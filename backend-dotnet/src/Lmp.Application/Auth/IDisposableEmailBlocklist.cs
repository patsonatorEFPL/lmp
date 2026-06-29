namespace Lmp.Application.Auth;

/// <summary>
/// Disposable-email-domain blocklist. Port of
/// <c>com.lmp.auth.service.DisposableEmailBlocklist</c>.
/// </summary>
public interface IDisposableEmailBlocklist
{
    /// <summary>True when the email's domain is a known disposable provider (and not safelisted).</summary>
    bool IsDisposable(string? email);

    /// <summary>Number of blocked domains currently loaded.</summary>
    int Size { get; }
}
