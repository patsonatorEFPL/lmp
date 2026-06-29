using System.Text.Json;
using Lmp.Application.Admin;
using Lmp.Application.Auth;
using Lmp.Application.Billing;
using Lmp.Application.Common;
using Lmp.Application.Crm;
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
public sealed class AdminController(
    IAdminQueryService admin,
    IAdminAppointmentService adminAppointments) : ControllerBase
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

    [HttpGet("appointments")]
    public async Task<ActionResult<ApiResponse<PagedResponse<AppointmentResponse>>>> GetAppointments(
        [FromQuery] int page = 0,
        [FromQuery] int size = 20,
        [FromQuery] string? status = null,
        CancellationToken ct = default)
    {
        try
        {
            return Ok(ApiResponse<PagedResponse<AppointmentResponse>>.Ok(
                await adminAppointments.ListAsync(page, size, status, ct)));
        }
        catch (ArgumentException e)
        {
            return BadRequest(ApiResponse<PagedResponse<AppointmentResponse>>.Error(e.Message));
        }
    }

    [HttpGet("appointments/{id:guid}")]
    public async Task<ActionResult<ApiResponse<IDictionary<string, object?>>>> GetAppointmentDetail(Guid id, CancellationToken ct)
    {
        var detail = await adminAppointments.GetDetailAsync(id, ct);
        return detail is null
            ? NotFound()
            : Ok(ApiResponse<IDictionary<string, object?>>.Ok(detail));
    }

    [HttpPut("appointments/{id:guid}")]
    public async Task<ActionResult<ApiResponse<object>>> UpdateAppointment(
        Guid id,
        [FromBody] Dictionary<string, JsonElement> data,
        CancellationToken ct)
    {
        try
        {
            await adminAppointments.UpdateAsync(id, data, ct);
            return Ok(new ApiResponse<object>(true, "Rendez-vous mis à jour", null));
        }
        catch (ArgumentException e)
        {
            return BadRequest(ApiResponse<object>.Error(e.Message));
        }
    }

    [HttpDelete("appointments/{id:guid}")]
    public async Task<ActionResult<ApiResponse<object>>> DeleteAppointment(Guid id, CancellationToken ct)
    {
        await adminAppointments.DeleteAsync(id, ct);
        return Ok(new ApiResponse<object>(true, "Rendez-vous supprimé", null));
    }
}
