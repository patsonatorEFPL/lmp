using System.Security.Claims;
using System.Text;
using System.Text.Json;
using Lmp.Application.RealTime;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http.Features;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.RealTime;

/// <summary>
/// Server-Sent Events streams. Port of <c>com.lmp.shared.web.api.SseNotificationController</c>
/// (<c>/api/v1/sse</c>): a per-user notification stream and an admin broadcast stream.
/// <c>EventSource</c> carries the session cookie automatically, so these rely on the
/// standard cookie auth. Each stream stays open with a 15s heartbeat comment so
/// proxies (Nginx) keep it alive.
/// </summary>
[ApiController]
[Route("api/v1/sse")]
public sealed class SseController(ISseEmitterManager manager) : ControllerBase
{
    private static readonly TimeSpan HeartbeatInterval = TimeSpan.FromSeconds(15);
    private static readonly UTF8Encoding Utf8NoBom = new(encoderShouldEmitUTF8Identifier: false);
    private static readonly JsonSerializerOptions SseJson = new(JsonSerializerDefaults.Web);

    /// <summary>Per-user notification stream (event name <c>notification</c>).</summary>
    [HttpGet("notifications")]
    [Authorize]
    public Task StreamNotifications(CancellationToken ct)
    {
        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrEmpty(userId))
        {
            Response.StatusCode = StatusCodes.Status401Unauthorized;
            return Task.CompletedTask;
        }

        return StreamAsync(manager.OpenUserStream(userId), ct);
    }

    /// <summary>Admin broadcast stream (unified event name <c>lmp-admin</c>).</summary>
    [HttpGet("admin/events")]
    [Authorize(Roles = "ADMIN")]
    public Task StreamAdminEvents(CancellationToken ct)
        => StreamAsync(manager.OpenAdminStream(), ct);

    private async Task StreamAsync(SseConnection connection, CancellationToken ct)
    {
        Response.ContentType = "text/event-stream";
        Response.Headers.CacheControl = "no-cache,no-store,must-revalidate";
        Response.Headers["X-Accel-Buffering"] = "no";
        HttpContext.Features.Get<IHttpResponseBodyFeature>()?.DisableBuffering();

        var writer = new StreamWriter(Response.Body, Utf8NoBom, bufferSize: 1024, leaveOpen: true)
        {
            AutoFlush = false,
        };

        try
        {
            // Prime the stream so EventSource fires `onopen` immediately and any
            // buffering proxy starts flushing.
            await writer.WriteAsync(": connected\n\n");
            await writer.FlushAsync(ct);

            while (!ct.IsCancellationRequested)
            {
                using var beat = CancellationTokenSource.CreateLinkedTokenSource(ct);
                beat.CancelAfter(HeartbeatInterval);
                try
                {
                    if (!await connection.Reader.WaitToReadAsync(beat.Token))
                    {
                        break; // stream completed by the manager (e.g. session invalidated)
                    }

                    while (connection.Reader.TryRead(out var ev))
                    {
                        await WriteEventAsync(writer, ev);
                    }

                    await writer.FlushAsync(ct);
                }
                catch (OperationCanceledException) when (!ct.IsCancellationRequested)
                {
                    // No event within the heartbeat window — send a keep-alive comment.
                    await writer.WriteAsync(": heartbeat\n\n");
                    await writer.FlushAsync(ct);
                }
            }
        }
        catch (OperationCanceledException)
        {
            // Client closed the EventSource — normal teardown.
        }
        catch (IOException)
        {
            // Client dropped the socket mid-write — normal teardown.
        }
        finally
        {
            manager.CloseStream(connection);
            try
            {
                writer.Dispose();
            }
            catch (IOException)
            {
                // Connection already gone; nothing left to flush.
            }
        }
    }

    private static async Task WriteEventAsync(TextWriter writer, SseEvent ev)
    {
        await writer.WriteAsync("event: ");
        await writer.WriteAsync(ev.EventName);
        await writer.WriteAsync('\n');

        // Split on newlines so a multi-line JSON payload is emitted as the spec
        // requires (one `data:` line per physical line).
        var json = JsonSerializer.Serialize(ev.Data, SseJson);
        foreach (var line in json.Split('\n'))
        {
            await writer.WriteAsync("data: ");
            await writer.WriteAsync(line);
            await writer.WriteAsync('\n');
        }

        await writer.WriteAsync('\n');
    }
}
