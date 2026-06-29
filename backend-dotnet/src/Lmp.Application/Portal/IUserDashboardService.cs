using Lmp.Application.Auth;

namespace Lmp.Application.Portal;

/// <summary>User dashboard use-cases. Backs <c>UserDashboardRestController</c>.</summary>
public interface IUserDashboardService
{
    Task<DashboardStatsResponse> GetStatsAsync(Guid userId, CancellationToken ct = default);

    /// <summary>
    /// Apply a profile update (incl. VAT reverse-charge rules) and return the refreshed
    /// user. Throws <see cref="ArgumentException"/> with a user-facing message on a VAT
    /// validation failure.
    /// </summary>
    Task<UserResponse> UpdateProfileAsync(Guid userId, UpdateProfileRequest request, CancellationToken ct = default);

    /// <summary>
    /// Change the password after verifying the current one, that it differs, and strength.
    /// Throws <see cref="InvalidOperationException"/> with a user-facing message on failure.
    /// </summary>
    Task ChangePasswordAsync(Guid userId, string currentPassword, string newPassword, CancellationToken ct = default);
}
