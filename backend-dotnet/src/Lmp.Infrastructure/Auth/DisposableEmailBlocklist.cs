using Lmp.Application.Auth;
using Microsoft.Extensions.Logging;

namespace Lmp.Infrastructure.Auth;

/// <summary>
/// Disposable-email blocklist. Faithful port of
/// <c>com.lmp.auth.service.DisposableEmailBlocklist</c>: loads the bundled local
/// list and applies a safelist of legitimate providers that must never be blocked.
/// The 24h remote refresh (GitHub, ETag) is intentionally omitted here — egress is
/// restricted and the local list is the authoritative fallback — and can be added
/// as a hosted service later.
/// </summary>
public sealed class DisposableEmailBlocklist : IDisposableEmailBlocklist
{
    private const string LocalFallback = "Auth/data/disposable_email_blocklist.conf";

    private static readonly HashSet<string> Safelist = new(StringComparer.OrdinalIgnoreCase)
    {
        "gmail.com", "googlemail.com",
        "yahoo.com", "yahoo.fr", "yahoo.ca",
        "hotmail.com", "hotmail.fr", "hotmail.ca",
        "outlook.com", "outlook.fr",
        "live.com", "live.fr", "live.ca",
        "msn.com",
        "icloud.com", "me.com", "mac.com",
        "protonmail.com", "proton.me", "pm.me",
        "aol.com",
        "zoho.com",
        "yandex.com", "yandex.ru",
        "mail.com",
        "gmx.com", "gmx.fr",
        "fastmail.com",
        "tutanota.com", "tuta.io",
        "hey.com",
        "videotron.ca", "bell.net", "rogers.com", "shaw.ca", "telus.net",
        "sympatico.ca", "cogeco.ca",
        "orange.fr", "free.fr", "sfr.fr", "laposte.net", "wanadoo.fr",
        "bluewin.ch",
        "gmx.de", "web.de", "t-online.de",
    };

    private readonly HashSet<string> _blockedDomains;

    public DisposableEmailBlocklist(ILogger<DisposableEmailBlocklist> logger)
    {
        _blockedDomains = LoadLocalFallback(logger);
        logger.LogInformation("DisposableEmailBlocklist initialised with {Count} blocked domains", _blockedDomains.Count);
    }

    public int Size => _blockedDomains.Count;

    public bool IsDisposable(string? email)
    {
        if (string.IsNullOrEmpty(email) || !email.Contains('@', StringComparison.Ordinal))
        {
            return false;
        }

        var domain = email[(email.LastIndexOf('@') + 1)..].Trim().ToLowerInvariant();
        if (Safelist.Contains(domain))
        {
            return false;
        }

        return _blockedDomains.Contains(domain);
    }

    private static HashSet<string> LoadLocalFallback(ILogger logger)
    {
        var path = Path.Combine(AppContext.BaseDirectory, LocalFallback);
        if (!File.Exists(path))
        {
            logger.LogWarning("Local blocklist file not found: {Path}", path);
            return new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        }

        var domains = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        foreach (var raw in File.ReadLines(path))
        {
            var line = raw.Trim();
            if (line.Length == 0 || line.StartsWith('#'))
            {
                continue;
            }

            domains.Add(line.ToLowerInvariant());
        }

        logger.LogInformation("Local blocklist loaded: {Count} domains", domains.Count);
        return domains;
    }
}
