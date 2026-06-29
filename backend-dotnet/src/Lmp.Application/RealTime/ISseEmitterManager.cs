namespace Lmp.Application.RealTime;

/// <summary>
/// Registry of live SSE connections, keyed by user plus an admin-broadcast group.
/// Port of the Java <c>SseEmitterManager</c> in single-replica mode: the optional
/// Redis pub/sub fan-out (multi-replica) and the business-event producers are
/// later increments. Today the streams stay open (heartbeat) and the
/// <c>SendTo*</c> methods are the seam those producers will call.
/// </summary>
public interface ISseEmitterManager
{
    /// <summary>Registers a new per-user stream (one browser tab).</summary>
    SseConnection OpenUserStream(string userId);

    /// <summary>Registers a new admin-broadcast stream.</summary>
    SseConnection OpenAdminStream();

    /// <summary>Deregisters and completes a stream previously opened by either <c>Open*</c> method.</summary>
    void CloseStream(SseConnection connection);

    /// <summary>Pushes an event to every open stream of one user.</summary>
    void SendToUser(string userId, string eventName, object data);

    /// <summary>Pushes an event to every open admin stream.</summary>
    void SendToAdmins(string eventName, object data);

    /// <summary>
    /// Forcibly completes all of a user's streams (e.g. session invalidation after a
    /// soft-delete or suspension). Returns the number of streams closed.
    /// </summary>
    int CloseUserStreams(string userId);
}
