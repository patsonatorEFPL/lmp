namespace Lmp.Application.Notification;

/// <summary>In-app notification DTO. Faithful port of <c>com.lmp.notification.dto.InAppNotificationDto</c>.</summary>
public sealed record InAppNotificationDto(
    string Id,
    string Type,
    string Message,
    string? OrderId,
    string? ServiceName,
    double? Amount,
    bool Read,
    string Timestamp);
