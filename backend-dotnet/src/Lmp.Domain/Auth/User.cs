namespace Lmp.Domain.Auth;

/// <summary>Application user — maps to <c>users</c> (port of <c>com.lmp.auth.domain.User</c>).</summary>
public class User
{
    public Guid Id { get; set; }
    public string? Email { get; set; }
    public string? Username { get; set; }
    public string Password { get; set; } = null!;
    public string? FirstName { get; set; }
    public string? LastName { get; set; }
    public string? Phone { get; set; }
    public string? Address { get; set; }
    public string? City { get; set; }
    public string? PostalCode { get; set; }
    public string? Country { get; set; }
    public string? CompanyName { get; set; }
    public DateTime RegistrationDate { get; set; }
    public DateTime? LastLoginDate { get; set; }
    public UserStatus Status { get; set; } = UserStatus.Active;
    public bool AccountLocked { get; set; }
    public bool EmailVerified { get; set; }
    public string? VerificationToken { get; set; }
    public string? ResetToken { get; set; }
    public DateTime? ResetTokenExpiry { get; set; }
    public string? Gender { get; set; }
    public string? OauthProvider { get; set; }
    public string? OauthProviderId { get; set; }
    public bool VatReverseCharge { get; set; }
    public string? VatNumber { get; set; }

    public ICollection<Role> Roles { get; set; } = new List<Role>();

    /// <summary>Best available human-readable name. Faithful port of <c>User.getDisplayName()</c>.</summary>
    public string GetDisplayName()
    {
        var first = FirstName?.Trim();
        var last = LastName?.Trim();
        if (!string.IsNullOrEmpty(first) && !string.IsNullOrEmpty(last))
        {
            return $"{first} {last}";
        }

        if (!string.IsNullOrEmpty(first))
        {
            return first;
        }

        if (!string.IsNullOrEmpty(last))
        {
            return last;
        }

        if (Email is not null && Email.Contains('@', StringComparison.Ordinal))
        {
            var localPart = Email[..Email.IndexOf('@', StringComparison.Ordinal)];
            var words = localPart.Replace('.', ' ').Replace('-', ' ').Replace('_', ' ')
                .Split(' ', StringSplitOptions.RemoveEmptyEntries);
            var titled = words.Select(w => char.ToUpperInvariant(w[0]) + w[1..].ToLowerInvariant());
            return string.Join(' ', titled);
        }

        return "Utilisateur";
    }

    public bool IsAdmin() => Roles.Any(r => r.Name == "ADMIN");
}
