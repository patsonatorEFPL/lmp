using System.Security.Claims;
using Lmp.Application.Auth;
using Lmp.Application.Common;
using Lmp.Domain.Auth;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Auth;

/// <summary>
/// Authentication API — cookie-based HTTP sessions (no JWT), SPA-compatible with
/// <c>withCredentials</c>. Port of <c>com.lmp.auth.web.api.AuthRestController</c>
/// (<c>/api/v1/auth</c>). Login/register and email-verification flows are ported
/// incrementally; this slice covers login, current-user and logout.
/// </summary>
[ApiController]
[Route("api/v1/auth")]
public sealed class AuthController(
    IUserService userService,
    IPasswordEncoder passwordEncoder) : ControllerBase
{
    public sealed record LoginData(UserResponse User, string RedirectUrl);

    [HttpPost("login")]
    public async Task<ActionResult<ApiResponse<LoginData>>> Login(
        [FromBody] LoginDto loginDto,
        CancellationToken ct)
    {
        var user = await userService.FindByLoginAsync(loginDto.Email, ct);

        // Constant message for any failure (bad login, wrong password, disabled or
        // locked account) — mirrors the Java 401 "Invalid email or password".
        if (user is null
            || user.Status != UserStatus.Active
            || user.AccountLocked
            || !passwordEncoder.Matches(loginDto.Password, user.Password))
        {
            return Unauthorized(ApiResponse<LoginData>.Error("Invalid email or password"));
        }

        var login = user.Email ?? user.Username ?? user.Id.ToString();
        var claims = new List<Claim>
        {
            new(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new(ClaimTypes.Name, login),
        };
        claims.AddRange(user.Roles.Select(r => new Claim(ClaimTypes.Role, r.Name)));

        var identity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
        var properties = new AuthenticationProperties { IsPersistent = loginDto.RememberMe };
        await HttpContext.SignInAsync(
            CookieAuthenticationDefaults.AuthenticationScheme,
            new ClaimsPrincipal(identity),
            properties);

        await userService.UpdateLastLoginDateAsync(user.Id, ct);

        var redirectUrl = user.IsAdmin() ? "/admin" : "/dashboard";
        var data = new LoginData(UserResponse.From(user), redirectUrl);
        return Ok(ApiResponse<LoginData>.Ok("Login successful", data));
    }

    [HttpGet("me")]
    public async Task<ActionResult<ApiResponse<UserResponse>>> GetCurrentUser(CancellationToken ct)
    {
        if (User.Identity?.IsAuthenticated != true)
        {
            return Unauthorized(ApiResponse<UserResponse>.Error("Not authenticated"));
        }

        var idClaim = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (!Guid.TryParse(idClaim, out var userId)
            || await userService.FindByIdAsync(userId, ct) is not { } user)
        {
            // Authenticated but the row is gone (hard-deleted) — clear the session.
            await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            return Unauthorized(ApiResponse<UserResponse>.Error("Session invalid — user no longer exists"));
        }

        return Ok(ApiResponse<UserResponse>.Ok(UserResponse.From(user)));
    }

    [HttpPost("logout")]
    public async Task<ActionResult<ApiResponse<object>>> Logout()
    {
        await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
        return Ok(new ApiResponse<object>(true, "Logged out successfully", null));
    }
}
