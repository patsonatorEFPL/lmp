using Lmp.Application.Admin;
using Lmp.Application.Auth;
using Lmp.Application.Billing;
using Lmp.Application.Common;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Admin;

/// <summary>
/// Admin read endpoints. Port of the read methods of
/// <c>com.lmp.shared.web.api.AdminRestController</c> (<c>/api/v1/admin</c>).
/// Restricted to the ADMIN role (401 unauthenticated, 403 otherwise). Write
/// operations are a later increment.
/// </summary>
[ApiController]
[Route("api/v1/admin")]
[Authorize(Roles = "ADMIN")]
public sealed class AdminController(IAdminQueryService admin) : ControllerBase
{
    [HttpGet("stats")]
    public async Task<ActionResult<ApiResponse<IDictionary<string, object>>>> GetStats(CancellationToken ct)
        => Ok(ApiResponse<IDictionary<string, object>>.Ok(await admin.GetStatsAsync(ct)));

    [HttpGet("users")]
    public async Task<ActionResult<ApiResponse<PagedResponse<UserResponse>>>> GetUsers(
        [FromQuery] int page = 0,
        [FromQuery] int size = 20,
        [FromQuery] string? status = null,
        [FromQuery] string? search = null,
        CancellationToken ct = default)
    {
        try
        {
            return Ok(ApiResponse<PagedResponse<UserResponse>>.Ok(
                await admin.GetUsersAsync(page, size, status, search, ct)));
        }
        catch (ArgumentException e)
        {
            return BadRequest(ApiResponse<PagedResponse<UserResponse>>.Error(e.Message));
        }
    }

    [HttpGet("orders")]
    public async Task<ActionResult<ApiResponse<PagedResponse<OrderResponse>>>> GetOrders(
        [FromQuery] int page = 0,
        [FromQuery] int size = 20,
        [FromQuery] string? status = null,
        CancellationToken ct = default)
    {
        try
        {
            return Ok(ApiResponse<PagedResponse<OrderResponse>>.Ok(
                await admin.GetOrdersAsync(page, size, status, ct)));
        }
        catch (ArgumentException e)
        {
            return BadRequest(ApiResponse<PagedResponse<OrderResponse>>.Error(e.Message));
        }
    }
}
