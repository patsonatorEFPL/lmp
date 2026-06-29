using System.Collections.Concurrent;
using Lmp.Application.RealTime;

namespace Lmp.Infrastructure.RealTime;

/// <summary>
/// In-memory, thread-safe <see cref="ISseEmitterManager"/>. Registered as a
/// singleton so producers and the streaming controller share one registry.
/// Single-replica port of the Java <c>SseEmitterManager</c> (no Redis pub/sub).
/// </summary>
public sealed class SseEmitterManager : ISseEmitterManager
{
    private readonly ConcurrentDictionary<string, ConcurrentDictionary<SseConnection, byte>> _userStreams =
        new(StringComparer.Ordinal);

    private readonly ConcurrentDictionary<SseConnection, byte> _adminStreams = new();

    // Reverse lookup so CloseStream is O(1) and knows which group a connection
    // belongs to (value = userId, or null for an admin-broadcast stream).
    private readonly ConcurrentDictionary<SseConnection, string?> _owners = new();

    public SseConnection OpenUserStream(string userId)
    {
        var connection = new SseConnection();
        _userStreams.GetOrAdd(userId, _ => new ConcurrentDictionary<SseConnection, byte>()).TryAdd(connection, 0);
        _owners[connection] = userId;
        return connection;
    }

    public SseConnection OpenAdminStream()
    {
        var connection = new SseConnection();
        _adminStreams.TryAdd(connection, 0);
        _owners[connection] = null;
        return connection;
    }

    public void CloseStream(SseConnection connection)
    {
        if (_owners.TryRemove(connection, out var userId))
        {
            if (userId is null)
            {
                _adminStreams.TryRemove(connection, out _);
            }
            else if (_userStreams.TryGetValue(userId, out var set))
            {
                set.TryRemove(connection, out _);
                if (set.IsEmpty)
                {
                    _userStreams.TryRemove(userId, out _);
                }
            }
        }

        connection.Complete();
    }

    public void SendToUser(string userId, string eventName, object data)
    {
        if (!_userStreams.TryGetValue(userId, out var set))
        {
            return;
        }

        var ev = new SseEvent(eventName, data);
        foreach (var connection in set.Keys)
        {
            connection.TryEnqueue(ev);
        }
    }

    public void SendToAdmins(string eventName, object data)
    {
        var ev = new SseEvent(eventName, data);
        foreach (var connection in _adminStreams.Keys)
        {
            connection.TryEnqueue(ev);
        }
    }

    public int CloseUserStreams(string userId)
    {
        if (!_userStreams.TryRemove(userId, out var set))
        {
            return 0;
        }

        var closed = 0;
        foreach (var connection in set.Keys)
        {
            _owners.TryRemove(connection, out _);
            connection.Complete();
            closed++;
        }

        return closed;
    }
}
