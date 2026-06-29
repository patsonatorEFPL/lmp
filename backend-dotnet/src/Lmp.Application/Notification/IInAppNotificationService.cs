namespace Lmp.Application.Notification;

/// <summary>In-app notification use-cases. Port of <c>com.lmp.notification.service.InAppNotificationService</c>.</summary>
public interface IInAppNotificationService
{
    /// <summary>User's notifications, newest first, capped at 50.</summary>
    Task<IReadOnlyList<InAppNotificationDto>> GetUserNotificationsAsync(Guid userId, CancellationToken ct = default);

    Task<long> GetUnreadCountAsync(Guid userId, CancellationToken ct = default);

    Task MarkAsReadAsync(Guid notificationId, CancellationToken ct = default);

    Task MarkAllAsReadAsync(Guid userId, CancellationToken ct = default);

    Task DeleteNotificationAsync(Guid notificationId, CancellationToken ct = default);

    Task DeleteAllForUserAsync(Guid userId, CancellationToken ct = default);
}
