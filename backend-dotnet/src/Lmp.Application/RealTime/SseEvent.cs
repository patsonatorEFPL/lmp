namespace Lmp.Application.RealTime;

/// <summary>A queued Server-Sent Event: a named event carrying a JSON-serialisable payload.</summary>
public readonly record struct SseEvent(string EventName, object Data);
