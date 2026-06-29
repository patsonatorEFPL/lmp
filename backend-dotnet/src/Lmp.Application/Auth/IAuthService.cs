using Lmp.Domain.Auth;

namespace Lmp.Application.Auth;

/// <summary>
/// Authentication/registration use-cases. Subset of
/// <c>com.lmp.auth.service.AuthService</c> covering the registration and
/// email-verification flow; password-reset is a later increment.
/// </summary>
public interface IAuthService
{
    Task<bool> ExistsByEmailAsync(string email, CancellationToken ct = default);

    bool IsDisposableEmail(string? email);

    /// <summary>Validates registration input; throws <see cref="ArgumentException"/> on failure.</summary>
    void ValidateRegistrationData(RegisterDto dto);

    /// <summary>Creates the user (Argon2id password, USER role, verification token).</summary>
    Task<User> RegisterUserAsync(RegisterDto dto, CancellationToken ct = default);

    /// <summary>Verifies an email-verification token; returns whether it matched.</summary>
    Task<bool> VerifyEmailAsync(string? token, CancellationToken ct = default);

    /// <summary>Issues a fresh verification token and re-sends the email.</summary>
    Task ResendVerificationEmailAsync(string email, CancellationToken ct = default);
}
