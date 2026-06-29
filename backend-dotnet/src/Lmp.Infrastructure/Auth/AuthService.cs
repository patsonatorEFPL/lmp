using Lmp.Application.Auth;
using Lmp.Domain.Auth;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

namespace Lmp.Infrastructure.Auth;

/// <summary>
/// Registration and email-verification use-cases. Faithful port of the
/// corresponding methods in <c>com.lmp.auth.service.AuthServiceImpl</c>.
/// </summary>
public sealed class AuthService(
    LmpDbContext db,
    IPasswordEncoder passwordEncoder,
    IDisposableEmailBlocklist disposableEmailBlocklist,
    IAuthEmailService emailService,
    ILogger<AuthService> logger) : IAuthService
{
    public Task<bool> ExistsByEmailAsync(string email, CancellationToken ct = default)
        => db.Users.AnyAsync(u => u.Email == email, ct);

    public bool IsDisposableEmail(string? email) => disposableEmailBlocklist.IsDisposable(email);

    public void ValidateRegistrationData(RegisterDto dto)
    {
        ArgumentNullException.ThrowIfNull(dto);

        if (!dto.IsPasswordMatching())
        {
            throw new ArgumentException("Les mots de passe ne correspondent pas");
        }

        if (!dto.AcceptTerms)
        {
            throw new ArgumentException("Vous devez accepter les conditions d'utilisation");
        }

        if (dto.Email is null || !dto.Email.Contains('@', StringComparison.Ordinal))
        {
            throw new ArgumentException("Format d'email invalide");
        }

        if (dto.Password is null || dto.Password.Length < 6)
        {
            throw new ArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }
    }

    public async Task<User> RegisterUserAsync(RegisterDto dto, CancellationToken ct = default)
    {
        ValidateRegistrationData(dto);

        if (await ExistsByEmailAsync(dto.Email, ct))
        {
            throw new ArgumentException("Un utilisateur avec cet email existe déjà");
        }

        if (IsDisposableEmail(dto.Email))
        {
            throw new ArgumentException(
                "Les adresses email temporaires/jetables ne sont pas acceptées. Veuillez utiliser une adresse email permanente.");
        }

        var userRole = await db.Roles.FirstOrDefaultAsync(r => r.Name == "USER", ct)
            ?? throw new InvalidOperationException("Rôle USER non trouvé");

        var user = new User
        {
            Email = dto.Email,
            Password = passwordEncoder.Encode(dto.Password),
            FirstName = DeriveFirstName(dto.FirstName, dto.Email),
            LastName = dto.LastName,
            Phone = dto.Phone,
            Address = dto.Address,
            City = dto.City,
            PostalCode = dto.PostalCode,
            Country = dto.Country,
            CompanyName = dto.CompanyName,
            RegistrationDate = DateTime.UtcNow,
            Status = UserStatus.Active,
            AccountLocked = false,
            EmailVerified = false,
            VerificationToken = Guid.NewGuid().ToString(),
            Roles = [userRole],
        };

        db.Users.Add(user);
        await db.SaveChangesAsync(ct);
        logger.LogInformation("User registered: {Email}", user.Email);
        return user;
    }

    public async Task<bool> VerifyEmailAsync(string? token, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(token))
        {
            return false;
        }

        var user = await db.Users.FirstOrDefaultAsync(u => u.VerificationToken == token, ct);
        if (user is null)
        {
            return false;
        }

        // Capture verification state before flipping it: distinguishes "INACTIVE
        // because email pending" from "INACTIVE because admin-suspended".
        var wasUnverified = !user.EmailVerified;

        user.EmailVerified = true;
        user.VerificationToken = null;

        if (user.Status == UserStatus.Inactive && wasUnverified)
        {
            user.Status = UserStatus.Active;
            logger.LogInformation("Account reactivated after email verification: {Email}", user.Email);
        }

        await db.SaveChangesAsync(ct);
        logger.LogInformation("Email verified for: {Email}", user.Email);
        return true;
    }

    public async Task ResendVerificationEmailAsync(string email, CancellationToken ct = default)
    {
        var user = await db.Users.FirstOrDefaultAsync(u => u.Email == email, ct)
            ?? throw new InvalidOperationException("Utilisateur non trouvé");

        if (user.EmailVerified)
        {
            throw new InvalidOperationException("Votre email est déjà vérifié");
        }

        user.VerificationToken = Guid.NewGuid().ToString();
        await db.SaveChangesAsync(ct);
        await emailService.SendVerificationEmailAsync(user, ct);
    }

    /// <summary>Mirrors <c>AuthServiceImpl.deriveFirstName</c>.</summary>
    private static string DeriveFirstName(string? provided, string? email)
    {
        if (!string.IsNullOrWhiteSpace(provided))
        {
            return provided.Trim();
        }

        if (string.IsNullOrWhiteSpace(email) || !email.Contains('@', StringComparison.Ordinal))
        {
            return string.Empty;
        }

        var local = email[..email.IndexOf('@', StringComparison.Ordinal)];
        var token = local.Split('.', '_', '+', '-')[0];
        if (token.Length == 0)
        {
            return string.Empty;
        }

        return char.ToUpperInvariant(token[0]) + token[1..].ToLowerInvariant();
    }
}
