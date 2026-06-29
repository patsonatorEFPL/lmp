namespace Lmp.Domain.Notification;

/// <summary>An in-app notification — maps to <c>in_app_notifications</c>.</summary>
public class InAppNotification
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public string Type { get; set; } = "INFO";
    public string Message { get; set; } = null!;
    public string? OrderId { get; set; }
    public string? ServiceName { get; set; }
    public decimal? Amount { get; set; }
    public bool Read { get; set; }
    public DateTime CreatedAt { get; set; }
}
