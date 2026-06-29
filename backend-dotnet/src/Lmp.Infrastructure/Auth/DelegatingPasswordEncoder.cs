using System.Security.Cryptography;
using System.Text;
using Konscious.Security.Cryptography;
using Lmp.Application.Auth;

namespace Lmp.Infrastructure.Auth;

/// <summary>
/// Password encoder equivalent to Spring Security's <c>DelegatingPasswordEncoder</c>
/// as configured in <c>SecurityConfig</c>: Argon2id for new hashes
/// (m=12&#160;288&#160;KB, t=2, p=1, 16-byte salt, 32-byte hash), with legacy BCrypt
/// hashes (prefixed or raw <c>$2a/$2b/$2y</c>) still verifying. The Argon2id PHC
/// string is byte-compatible with the Java encoder, so hashes round-trip between
/// the two backends.
/// </summary>
public sealed class DelegatingPasswordEncoder : IPasswordEncoder
{
    private const string Argon2IdPrefix = "{argon2id}";
    private const string BcryptPrefix = "{bcrypt}";

    // OWASP "fast tier" parameters, matching SecurityConfig.passwordEncoder().
    private const int SaltLength = 16;
    private const int HashLength = 32;
    private const int Parallelism = 1;
    private const int MemoryKb = 12 * 1024;
    private const int Iterations = 2;

    public string Encode(string rawPassword)
    {
        var salt = RandomNumberGenerator.GetBytes(SaltLength);
        var hash = Argon2Hash(rawPassword, salt, Parallelism, MemoryKb, Iterations, HashLength);
        return $"{Argon2IdPrefix}$argon2id$v=19$m={MemoryKb},t={Iterations},p={Parallelism}$" +
               $"{Base64NoPad(salt)}${Base64NoPad(hash)}";
    }

    public bool Matches(string rawPassword, string? encodedPassword)
    {
        if (string.IsNullOrEmpty(encodedPassword))
        {
            return false;
        }

        if (encodedPassword.StartsWith(Argon2IdPrefix, StringComparison.Ordinal))
        {
            return VerifyArgon2(rawPassword, encodedPassword[Argon2IdPrefix.Length..]);
        }

        if (encodedPassword.StartsWith(BcryptPrefix, StringComparison.Ordinal))
        {
            return BCrypt.Net.BCrypt.Verify(rawPassword, encodedPassword[BcryptPrefix.Length..]);
        }

        if (encodedPassword.StartsWith("$argon2", StringComparison.Ordinal))
        {
            return VerifyArgon2(rawPassword, encodedPassword);
        }

        // Default-for-matches encoder is BCrypt (legacy unprefixed hashes).
        if (encodedPassword.StartsWith("$2a$", StringComparison.Ordinal)
            || encodedPassword.StartsWith("$2b$", StringComparison.Ordinal)
            || encodedPassword.StartsWith("$2y$", StringComparison.Ordinal))
        {
            return BCrypt.Net.BCrypt.Verify(rawPassword, encodedPassword);
        }

        return false;
    }

    private static bool VerifyArgon2(string rawPassword, string phc)
    {
        // Format: $argon2id$v=19$m=12288,t=2,p=1$<saltB64>$<hashB64>
        var parts = phc.Split('$');
        if (parts.Length != 6 || parts[1] != "argon2id")
        {
            return false;
        }

        int memory = 0, iterations = 0, parallelism = 0;
        foreach (var token in parts[3].Split(','))
        {
            var kv = token.Split('=');
            if (kv.Length != 2 || !int.TryParse(kv[1], out var value))
            {
                return false;
            }

            switch (kv[0])
            {
                case "m": memory = value; break;
                case "t": iterations = value; break;
                case "p": parallelism = value; break;
                default: break;
            }
        }

        if (memory <= 0 || iterations <= 0 || parallelism <= 0)
        {
            return false;
        }

        byte[] salt, expected;
        try
        {
            salt = Base64Decode(parts[4]);
            expected = Base64Decode(parts[5]);
        }
        catch (FormatException)
        {
            return false;
        }

        var actual = Argon2Hash(rawPassword, salt, parallelism, memory, iterations, expected.Length);
        return CryptographicOperations.FixedTimeEquals(actual, expected);
    }

    private static byte[] Argon2Hash(string password, byte[] salt, int parallelism, int memoryKb, int iterations, int length)
    {
        using var argon2 = new Argon2id(Encoding.UTF8.GetBytes(password))
        {
            Salt = salt,
            DegreeOfParallelism = parallelism,
            MemorySize = memoryKb,
            Iterations = iterations,
        };
        return argon2.GetBytes(length);
    }

    private static string Base64NoPad(byte[] data) => Convert.ToBase64String(data).TrimEnd('=');

    private static byte[] Base64Decode(string value)
    {
        var padded = (value.Length % 4) switch
        {
            2 => value + "==",
            3 => value + "=",
            _ => value,
        };
        return Convert.FromBase64String(padded);
    }
}
