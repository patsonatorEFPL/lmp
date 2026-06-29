using System.Globalization;
using Lmp.Application.Common;
using Lmp.Application.Crm;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Crm;

/// <summary>
/// Appointments API. Port of <c>com.lmp.crm.web.api.AppointmentRestController</c>
/// (<c>/api/v1/appointments</c>). Booking (POST) is public — it works for
/// anonymous visitors and links an existing user when the email matches.
/// </summary>
[ApiController]
[Route("api/v1/appointments")]
public sealed class AppointmentController(IAppointmentService appointments) : ControllerBase
{
    [HttpGet("available-slots")]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<DateTime>>>> GetAvailableSlots(
        [FromQuery] string date,
        CancellationToken ct)
    {
        if (!DateOnly.TryParseExact(date, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var localDate))
        {
            return BadRequest(ApiResponse<IReadOnlyList<DateTime>>.Error("Invalid date format. Use yyyy-MM-dd"));
        }

        var slots = await appointments.GetAvailableTimeSlotsAsync(localDate, ct);
        return Ok(ApiResponse<IReadOnlyList<DateTime>>.Ok(slots));
    }

    [HttpPost]
    public async Task<ActionResult<ApiResponse<AppointmentResponse>>> CreateAppointment(
        [FromBody] AppointmentRequest request,
        CancellationToken ct)
    {
        try
        {
            var appointment = await appointments.CreateAppointmentAsync(request, ct);
            return StatusCode(
                StatusCodes.Status201Created,
                ApiResponse<AppointmentResponse>.Ok("Rendez-vous créé avec succès", AppointmentResponse.From(appointment)));
        }
        catch (ArgumentException e)
        {
            return BadRequest(ApiResponse<AppointmentResponse>.Error(e.Message));
        }
    }
}
