using System.Threading.Channels;

namespace Lmp.Application.RealTime;

/// <summary>
/// A single live SSE connection (one browser tab). The owning controller drains
/// <see cref="Reader"/> into the HTTP response; producers enqueue events via
/// <see cref="TryEnqueue"/>. Equivalent of one Spring MVC <c>SseEmitter</c>.
/// </summary>
public sealed class SseConnection
{
    // Bounded so a stalled or slow client can never grow memory without limit.
    // Under back-pressure the oldest event is dropped: a live dashboard cares
    // about the newest state, and the heartbeat keeps the socket honest.
    private readonly Channel<SseEvent> _channel = Channel.CreateBounded<SseEvent>(
        new BoundedChannelOptions(capacity: 256)
        {
            SingleReader = true,
            SingleWriter = false,
            FullMode = BoundedChannelFullMode.DropOldest,
        });

    /// <summary>Drained by the controller writing the HTTP response.</summary>
    public ChannelReader<SseEvent> Reader => _channel.Reader;

    /// <summary>Enqueues an event; returns false if the connection is already completed.</summary>
    public bool TryEnqueue(SseEvent ev) => _channel.Writer.TryWrite(ev);

    /// <summary>Completes the stream so the controller's read loop ends.</summary>
    public void Complete() => _channel.Writer.TryComplete();
}
