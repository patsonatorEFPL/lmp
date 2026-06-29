using Lmp.Domain.Crm;

namespace Lmp.Application.Crm;

/// <summary>Appointment response DTO. Faithful port of <c>com.lmp.crm.dto.AppointmentResponse</c>.</summary>
public sealed record AppointmentResponse(
    Guid Id,
    string? ClientName,
    string? ClientEmail,
    string? ClientPhone,
    DateTime AppointmentDate,
    string Subject,
    string? Description,
    string? Status,
    int? DurationMinutes,
    int? Priority,
    DateTime CreatedAt,
    DateTime? ConfirmedAt)
{
    public static AppointmentResponse From(Appointment a)
        => new(
            a.Id,
            a.ClientName,
            a.ClientEmail,
            a.ClientPhone,
            a.AppointmentDate,
            a.Subject,
            a.Description,
            a.Status.ToWire(),
            a.DurationMinutes,
            a.Priority,
            a.CreatedAt,
            a.ConfirmedAt);
}
