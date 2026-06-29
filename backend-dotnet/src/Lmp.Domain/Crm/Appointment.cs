using Lmp.Domain.Auth;

namespace Lmp.Domain.Crm;

/// <summary>An appointment booking — maps to <c>appointments</c>.</summary>
public class Appointment
{
    public Guid Id { get; set; }
    public Guid? UserId { get; set; }
    public User? User { get; set; }

    public string? ClientName { get; set; }
    public string? ClientEmail { get; set; }
    public string? ClientPhone { get; set; }

    public DateTime AppointmentDate { get; set; }
    public string Subject { get; set; } = null!;
    public string? Description { get; set; }
    public AppointmentStatus Status { get; set; } = AppointmentStatus.Pending;

    public string? AdminNotes { get; set; }
    public string? ClientNotes { get; set; }
    public int? DurationMinutes { get; set; } = 60;
    public int? Priority { get; set; } = 5;

    public bool ReminderSent { get; set; }
    public DateTime? ReminderSentAt { get; set; }
    public bool ConfirmationSent { get; set; }
    public DateTime? ConfirmationSentAt { get; set; }
    public string? CancellationReason { get; set; }

    public DateTime CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }
    public DateTime? ConfirmedAt { get; set; }
    public DateTime? CancelledAt { get; set; }
    public DateTime? StartedAt { get; set; }
    public DateTime? CompletedAt { get; set; }
}
