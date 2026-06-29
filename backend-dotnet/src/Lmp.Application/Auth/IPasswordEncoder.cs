namespace Lmp.Application.Auth;

/// <summary>
/// Password hashing contract mirroring Spring Security's
/// <c>DelegatingPasswordEncoder</c>: Argon2id for new hashes, with legacy BCrypt
/// hashes still verifying. Hash strings are interoperable with the Java backend
/// so a password set by either backend validates on the other.
/// </summary>
public interface IPasswordEncoder
{
    /// <summary>Encode a raw password (produces an Argon2id hash, <c>{argon2id}</c>-prefixed).</summary>
    string Encode(string rawPassword);

    /// <summary>Verify a raw password against a stored hash (Argon2id, BCrypt, prefixed or raw).</summary>
    bool Matches(string rawPassword, string? encodedPassword);
}
