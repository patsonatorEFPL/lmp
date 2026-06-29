using System.Security.Claims;
using Lmp.Application.Common;
using Lmp.Application.Notification;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Notification;

/// <summary>
/// In-app notifications. Port of
/// <c>com.lmp.notification.web.api.InAppNotificationRestController</c>
/// (<c>/api/v1/notifications</c>). All endpoints require an authenticated user.
/// </summary>
[ApiController]
[Route("api/v1/notifications")]
public sealed class NotificationController(IInAppNotificationService notifications) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<ApiResponse<IReadOnlyList<InAppNotificationDto>>>> GetNotifications(CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(ApiResponse<IReadOnlyList<InAppNotificationDto>>.Error("Non authentifié"));
        }

        var result = await notifications.GetUserNotificationsAsync(userId, ct);
        return Ok(ApiResponse<IReadOnlyList<InAppNotificationDto>>.Ok("Notifications récupérées", result));
    }

    [HttpGet("unread-count")]
    public async Task<ActionResult<ApiResponse<IDictionary<string, long>>>> GetUnreadCount(CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(ApiResponse<IDictionary<string, long>>.Error("Non authentifié"));
        }

        var count = await notifications.GetUnreadCountAsync(userId, ct);
        return Ok(ApiResponse<IDictionary<string, long>>.Ok(
            "Compteur récupéré",
            new Dictionary<string, long> { ["count"] = count }));
    }

    [HttpPatch("{id:guid}/read")]
    public async Task<ActionResult<ApiResponse<object>>> MarkAsRead(Guid id, CancellationToken ct)
    {
        if (CurrentUserId() is null)
        {
            return Unauthorized(ApiResponse<object>.Error("Non authentifié"));
        }

        await notifications.MarkAsReadAsync(id, ct);
        return Ok(new ApiResponse<object>(true, "Notification marquée comme lue", null));
    }

    [HttpPatch("read-all")]
    public async Task<ActionResult<ApiResponse<object>>> MarkAllAsRead(CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(ApiResponse<object>.Error("Non authentifié"));
        }

        await notifications.MarkAllAsReadAsync(userId, ct);
        return Ok(new ApiResponse<object>(true, "Toutes les notifications marquées comme lues", null));
    }

    [HttpDelete("{id:guid}")]
    public async Task<ActionResult<ApiResponse<object>>> DeleteNotification(Guid id, CancellationToken ct)
    {
        if (CurrentUserId() is null)
        {
            return Unauthorized(ApiResponse<object>.Error("Non authentifié"));
        }

        await notifications.DeleteNotificationAsync(id, ct);
        return Ok(new ApiResponse<object>(true, "Notification supprimée", null));
    }

    [HttpDelete("all")]
    public async Task<ActionResult<ApiResponse<object>>> DeleteAll(CancellationToken ct)
    {
        if (CurrentUserId() is not { } userId)
        {
            return Unauthorized(ApiResponse<object>.Error("Non authentifié"));
        }

        await notifications.DeleteAllForUserAsync(userId, ct);
        return Ok(new ApiResponse<object>(true, "Toutes les notifications supprimées", null));
    }

    private Guid? CurrentUserId()
    {
        if (User.Identity?.IsAuthenticated != true)
        {
            return null;
        }

        return Guid.TryParse(User.FindFirstValue(ClaimTypes.NameIdentifier), out var id) ? id : null;
    }
}
