using System.Globalization;
using Lmp.Application.Notification;
using Lmp.Domain.Notification;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Notification;

/// <summary>EF Core in-app notification use-cases. Faithful port of <c>InAppNotificationService</c>.</summary>
public sealed class InAppNotificationService(LmpDbContext db) : IInAppNotificationService
{
    public async Task<IReadOnlyList<InAppNotificationDto>> GetUserNotificationsAsync(Guid userId, CancellationToken ct = default)
    {
        var notifications = await db.InAppNotifications
            .AsNoTracking()
            .Where(n => n.UserId == userId)
            .OrderByDescending(n => n.CreatedAt)
            .Take(50)
            .ToListAsync(ct);

        return notifications.Select(ToDto).ToList();
    }

    public async Task<long> GetUnreadCountAsync(Guid userId, CancellationToken ct = default)
        => await db.InAppNotifications.CountAsync(n => n.UserId == userId && !n.Read, ct);

    public async Task MarkAsReadAsync(Guid notificationId, CancellationToken ct = default)
        => await db.InAppNotifications
            .Where(n => n.Id == notificationId)
            .ExecuteUpdateAsync(s => s.SetProperty(n => n.Read, true), ct);

    public async Task MarkAllAsReadAsync(Guid userId, CancellationToken ct = default)
        => await db.InAppNotifications
            .Where(n => n.UserId == userId && !n.Read)
            .ExecuteUpdateAsync(s => s.SetProperty(n => n.Read, true), ct);

    public async Task DeleteNotificationAsync(Guid notificationId, CancellationToken ct = default)
        => await db.InAppNotifications.Where(n => n.Id == notificationId).ExecuteDeleteAsync(ct);

    public async Task DeleteAllForUserAsync(Guid userId, CancellationToken ct = default)
        => await db.InAppNotifications.Where(n => n.UserId == userId).ExecuteDeleteAsync(ct);

    private static InAppNotificationDto ToDto(InAppNotification n)
    {
        // ISO-8601 instant with 'Z' (createdAt is stored tz-less but represents UTC).
        var timestamp = DateTime.SpecifyKind(n.CreatedAt, DateTimeKind.Utc)
            .ToString("yyyy-MM-dd'T'HH:mm:ss.ffffff'Z'", CultureInfo.InvariantCulture);

        return new InAppNotificationDto(
            n.Id.ToString(),
            n.Type,
            n.Message,
            n.OrderId,
            n.ServiceName,
            n.Amount is { } amount ? (double)amount : null,
            n.Read,
            timestamp);
    }
}
