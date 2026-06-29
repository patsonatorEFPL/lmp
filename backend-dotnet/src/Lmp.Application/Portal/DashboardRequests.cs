using System.ComponentModel.DataAnnotations;

namespace Lmp.Application.Portal;

/// <summary>Profile update request. Port of <c>UpdateProfileRequest</c>.</summary>
public sealed record UpdateProfileRequest(
    [MaxLength(50)] string? FirstName,
    [MaxLength(50)] string? LastName,
    [MaxLength(20)] string? Phone,
    [MaxLength(100)] string? CompanyName,
    [MaxLength(100)] string? City,
    [MaxLength(100)] string? Country,
    [MaxLength(200)] string? Address,
    [MaxLength(10)] string? PostalCode,
    bool? VatReverseCharge,
    [MaxLength(64)] string? VatNumber);

/// <summary>Password change request. Port of <c>ChangePasswordRequest</c>.</summary>
public sealed record ChangePasswordRequest(
    [Required] string CurrentPassword,
    [Required][StringLength(128, MinimumLength = 8)] string NewPassword,
    [Required] string ConfirmPassword)
{
    public bool IsPasswordMatching() => NewPassword is not null && NewPassword == ConfirmPassword;
}
