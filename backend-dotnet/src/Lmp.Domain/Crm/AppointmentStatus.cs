namespace Lmp.Domain.Crm;

/// <summary>Appointment lifecycle status — persisted as the Java enum name.</summary>
public enum AppointmentStatus
{
    Pending,
    Confirmed,
    InProgress,
    Completed,
    Cancelled,
    NoShow,
}

public static class AppointmentStatusExtensions
{
    public static string ToWire(this AppointmentStatus status) => status switch
    {
        AppointmentStatus.Pending => "PENDING",
        AppointmentStatus.Confirmed => "CONFIRMED",
        AppointmentStatus.InProgress => "IN_PROGRESS",
        AppointmentStatus.Completed => "COMPLETED",
        AppointmentStatus.Cancelled => "CANCELLED",
        AppointmentStatus.NoShow => "NO_SHOW",
        _ => status.ToString(),
    };

    public static AppointmentStatus FromWire(string value) => value switch
    {
        "PENDING" => AppointmentStatus.Pending,
        "CONFIRMED" => AppointmentStatus.Confirmed,
        "IN_PROGRESS" => AppointmentStatus.InProgress,
        "COMPLETED" => AppointmentStatus.Completed,
        "CANCELLED" => AppointmentStatus.Cancelled,
        "NO_SHOW" => AppointmentStatus.NoShow,
        _ => throw new ArgumentOutOfRangeException(nameof(value), value, "Unknown appointment status"),
    };

    /// <summary>Active = not cancelled/completed (used for conflict detection).</summary>
    public static bool IsActive(this AppointmentStatus status)
        => status is AppointmentStatus.Pending or AppointmentStatus.Confirmed or AppointmentStatus.InProgress;
}
