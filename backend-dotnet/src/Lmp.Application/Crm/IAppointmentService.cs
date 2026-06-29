using Lmp.Domain.Crm;

namespace Lmp.Application.Crm;

/// <summary>Appointment booking use-cases. Port of <c>com.lmp.crm.service.AppointmentService</c>.</summary>
public interface IAppointmentService
{
    /// <summary>Available 1-hour slots for a date (09–11h, 13–16h), excluding conflicts and past/too-soon slots.</summary>
    Task<IReadOnlyList<DateTime>> GetAvailableTimeSlotsAsync(DateOnly date, CancellationToken ct = default);

    /// <summary>
    /// Create an appointment from a public request. Links an existing user (by email)
    /// or stores the client details for an anonymous visitor. Throws
    /// <see cref="ArgumentException"/> on validation or time-conflict failure.
    /// </summary>
    Task<Appointment> CreateAppointmentAsync(AppointmentRequest request, CancellationToken ct = default);
}
