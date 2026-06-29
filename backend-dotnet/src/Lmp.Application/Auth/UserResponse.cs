using Lmp.Domain.Auth;

namespace Lmp.Application.Auth;

/// <summary>User data without the password. Faithful port of <c>com.lmp.auth.dto.UserResponse</c>.</summary>
public sealed record UserResponse(
    Guid Id,
    string? Email,
    string? Username,
    string? FirstName,
    string? LastName,
    string DisplayName,
    string? Phone,
    string? City,
    string? Country,
    string? CompanyName,
    string Status,
    bool EmailVerified,
    bool AccountLocked,
    IReadOnlySet<string> Roles,
    DateTime RegistrationDate,
    DateTime? LastLoginDate,
    bool VatReverseCharge,
    string? VatNumber)
{
    public static UserResponse From(User user)
    {
        var roleNames = user.Roles.Select(r => r.Name).ToHashSet(StringComparer.Ordinal);
        return new UserResponse(
            user.Id,
            user.Email,
            user.Username,
            user.FirstName,
            user.LastName,
            user.GetDisplayName(),
            user.Phone,
            user.City,
            user.Country,
            user.CompanyName,
            StatusName(user.Status),
            user.EmailVerified,
            user.AccountLocked,
            roleNames,
            user.RegistrationDate,
            user.LastLoginDate,
            user.VatReverseCharge,
            user.VatNumber);
    }

    private static string StatusName(UserStatus status) => status switch
    {
        UserStatus.Active => "ACTIVE",
        UserStatus.Inactive => "INACTIVE",
        UserStatus.Deleted => "DELETED",
        _ => "ACTIVE",
    };
}
