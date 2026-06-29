using System.Security.Claims;
using Lmp.Application.Auth;
using Lmp.Application.Common;
using Lmp.Application.Portal;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Portal;

/// <summary>
/// User dashboard API. Port of
/// <c>com.lmp.portal.api.UserDashboardRestController</c> (<c>/api/v1/dashboard</c>).
/// All endpoints require authentication.
/// </summary>
[ApiController]
[Route("api/v1/dashboard")]
public sealed class DashboardController(IUserDashboardService dashboard) : ControllerBase
{
    [HttpGet("stats")]
    public async Task<ActionResult<ApiResponse<DashboardStatsResponse>>> GetStats(CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(ApiResponse<DashboardStatsResponse>.Error("Not authenticated"));
        }

        return Ok(ApiResponse<DashboardStatsResponse>.Ok(await dashboard.GetStatsAsync(userId, ct)));
    }

    [HttpPut("profile")]
    public async Task<ActionResult<ApiResponse<UserResponse>>> UpdateProfile(
        [FromBody] UpdateProfileRequest request,
        CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(ApiResponse<UserResponse>.Error("Not authenticated"));
        }

        try
        {
            var user = await dashboard.UpdateProfileAsync(userId, request, ct);
            return Ok(ApiResponse<UserResponse>.Ok("Profile updated successfully", user));
        }
        catch (ArgumentException e)
        {
            return BadRequest(ApiResponse<UserResponse>.Error(e.Message));
        }
    }

    [HttpPut("password")]
    public async Task<ActionResult<ApiResponse<object>>> ChangePassword(
        [FromBody] ChangePasswordRequest request,
        CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(ApiResponse<object>.Error("Not authenticated"));
        }

        if (!request.IsPasswordMatching())
        {
            return BadRequest(ApiResponse<object>.Error("Les mots de passe ne correspondent pas"));
        }

        if (!PasswordPolicy.IsStrong(request.NewPassword))
        {
            return BadRequest(ApiResponse<object>.Error(
                "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule et un chiffre"));
        }

        try
        {
            await dashboard.ChangePasswordAsync(userId, request.CurrentPassword, request.NewPassword, ct);
            return Ok(new ApiResponse<object>(true, "Mot de passe modifié avec succès", null));
        }
        catch (InvalidOperationException e)
        {
            return BadRequest(ApiResponse<object>.Error(e.Message));
        }
    }

    [HttpGet("test-notification")]
    public ActionResult<ApiResponse<string>> TestNotification()
    {
        // The SSE/event-bus broadcast belongs to the realtime notification module
        // (not yet ported); the endpoint contract is preserved.
        if (CurrentUserId() is null)
        {
            return Unauthorized(ApiResponse<string>.Error("Not authenticated"));
        }

        return Ok(ApiResponse<string>.Ok("Notification SSE envoyée", "OK"));
    }

    private Guid? CurrentUserId()
    {
        if (User.Identity?.IsAuthenticated != true)
        {
            return null;
        }

        return Guid.TryParse(User.FindFirstValue(ClaimTypes.NameIdentifier), out var id) ? id : null;
    }
}
