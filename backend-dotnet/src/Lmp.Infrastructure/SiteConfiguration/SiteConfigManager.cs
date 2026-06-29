using System.Collections.Concurrent;
using System.Text.Json;
using Lmp.Application.SiteConfiguration;
using Lmp.Domain.SiteConfiguration;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace Lmp.Infrastructure.SiteConfiguration;

/// <summary>
/// Central site-configuration manager. Faithful port of
/// <c>com.lmp.shared.config.site.SiteConfigManager</c>: a hierarchical resolver
/// (env/config → <c>site-config.json</c> → <c>site_config</c> table → values
/// derived from <c>lmp.site.url</c>) behind a memory cache. Registered as a
/// singleton; database reads use a scoped <see cref="LmpDbContext"/>.
/// </summary>
public sealed class SiteConfigManager : ISiteConfigManager
{
    private readonly IConfiguration _configuration;
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<SiteConfigManager> _logger;

    private readonly ConcurrentDictionary<string, string?> _cache = new(StringComparer.Ordinal);
    private readonly IReadOnlyDictionary<string, string> _fileConfig;
    private readonly IReadOnlyDictionary<string, string> _derivedConfig;
    private readonly string _siteUrl;

    public SiteConfigManager(
        IConfiguration configuration,
        IServiceScopeFactory scopeFactory,
        IOptions<SiteConfigOptions> options,
        ILogger<SiteConfigManager> logger)
    {
        _configuration = configuration;
        _scopeFactory = scopeFactory;
        _logger = logger;

        var url = options.Value.Url ?? "http://localhost:8080";
        _siteUrl = url.TrimEnd('/');
        _derivedConfig = ComputeDerivedValues(_siteUrl);
        _fileConfig = LoadFileConfig(options.Value.ConfigPath);
        _logger.LogInformation("SiteConfigManager initialised - siteUrl={SiteUrl}", _siteUrl);
    }

    public string? GetString(string key) => _cache.GetOrAdd(key, Resolve);

    public string GetString(string key, string defaultValue) => GetString(key) ?? defaultValue;

    public bool GetBoolean(string key, bool defaultValue)
    {
        try
        {
            var value = GetString(key);
            return value is null ? defaultValue : bool.TryParse(value.Trim(), out var b) && b;
        }
        catch (Exception e)
        {
            _logger.LogWarning(e, "[SITE-CONFIG] getBoolean({Key}) failed - fallback {Default}", key, defaultValue);
            return defaultValue;
        }
    }

    public string GetBaseUrl() => GetString("app.base.url", _siteUrl);

    public string GetFrontendUrl() => GetString("app.frontend.url", GetBaseUrl());

    public string GetSiteName() => GetString("app.name", "LMP Digital Services");

    public string GetSupportEmail() => GetString("mail.from.support", "support@" + ExtractHost(GetBaseUrl()));

    public string GetContactEmail() => GetString("mail.from.contact", "info@" + ExtractHost(GetBaseUrl()));

    public string GetNoreplyEmail() => GetString("mail.from.noreply", "noreply@" + ExtractHost(GetBaseUrl()));

    public string GetCompanyWebsite() => GetString("company.website", GetBaseUrl());

    public string GetOauth2IssuerUri() => GetString("app.oauth2.issuer-uri", GetBaseUrl());

    public string GetCorsAllowedOrigins() => GetString("app.cors.allowed-origins", GetBaseUrl());

    public async Task UpdateAsync(string key, string value, string? description, CancellationToken ct = default)
    {
        await using var scope = _scopeFactory.CreateAsyncScope();
        var db = scope.ServiceProvider.GetRequiredService<LmpDbContext>();

        var entry = await db.SiteConfig.FindAsync([key], ct);
        if (entry is null)
        {
            entry = new SiteConfigEntry(key, value, description);
            db.SiteConfig.Add(entry);
        }
        else
        {
            entry.Value = value;
            if (description is not null)
            {
                entry.Description = description;
            }
        }

        entry.UpdatedAt = DateTime.UtcNow;
        await db.SaveChangesAsync(ct);

        _cache.TryRemove(key, out _);
        _logger.LogInformation("SiteConfig updated - {Key} = {Value}", key, value);
    }

    public async Task<IReadOnlyDictionary<string, string?>> GetAllFromDbAsync(CancellationToken ct = default)
    {
        await using var scope = _scopeFactory.CreateAsyncScope();
        var db = scope.ServiceProvider.GetRequiredService<LmpDbContext>();
        var entries = await db.SiteConfig.AsNoTracking().OrderBy(e => e.Key).ToListAsync(ct);
        return entries.ToDictionary(e => e.Key, e => e.Value, StringComparer.Ordinal);
    }

    private string? Resolve(string key)
    {
        // 1. Environment / configuration providers.
        var envValue = _configuration[key];
        if (!string.IsNullOrWhiteSpace(envValue))
        {
            return envValue;
        }

        // 2. site-config.json.
        if (_fileConfig.TryGetValue(key, out var fileValue))
        {
            return fileValue;
        }

        // 3. Database — fail-soft: a not-yet-ready DB must not crash resolution.
        try
        {
            using var scope = _scopeFactory.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<LmpDbContext>();
            var dbValue = db.SiteConfig.AsNoTracking()
                .Where(e => e.Key == key)
                .Select(e => e.Value)
                .FirstOrDefault();
            if (dbValue is not null)
            {
                return dbValue;
            }
        }
        catch (Exception e)
        {
            _logger.LogWarning("[SITE-CONFIG] DB read unavailable for '{Key}' - derived/env fallback: {Message}", key, e.Message);
        }

        // 4. Values derived from lmp.site.url.
        return _derivedConfig.GetValueOrDefault(key);
    }

    private static Dictionary<string, string> ComputeDerivedValues(string siteUrl)
    {
        var derived = new Dictionary<string, string>(StringComparer.Ordinal);
        if (string.IsNullOrWhiteSpace(siteUrl))
        {
            return derived;
        }

        var host = ExtractHost(siteUrl);
        var isLocal = host is "localhost" or "127.0.0.1";
        var hostNoWww = host.StartsWith("www.", StringComparison.Ordinal) ? host[4..] : host;

        derived["app.base.url"] = siteUrl;
        derived["app.frontend.url"] = siteUrl;
        derived["company.website"] = siteUrl;
        // Single-host monolith: issuer = site URL (auth pages on the same host).
        derived["app.oauth2.issuer-uri"] = siteUrl;
        derived["app.cors.allowed-origins"] = isLocal
            ? "http://localhost:*"
            : $"https://{hostNoWww},https://www.{hostNoWww}";

        derived["mail.from.noreply"] = "noreply@" + host;
        derived["mail.from.support"] = "support@" + host;
        derived["mail.replyto.support"] = "support@" + host;
        derived["company.email"] = "support@" + host;
        derived["company.team.email"] = "support@" + host;
        derived["company.admin.email"] = "admin@" + host;
        derived["lmp.sync.alert.admin-email"] = "admin@" + host;

        return derived;
    }

    private Dictionary<string, string> LoadFileConfig(string? configPath)
    {
        var path = !string.IsNullOrWhiteSpace(configPath)
            ? configPath
            : Path.Combine(AppContext.BaseDirectory, "site-config.json");

        if (!File.Exists(path))
        {
            return new Dictionary<string, string>(StringComparer.Ordinal);
        }

        try
        {
            using var doc = JsonDocument.Parse(File.ReadAllText(path));
            var flattened = new Dictionary<string, string>(StringComparer.Ordinal);
            Flatten(doc.RootElement, string.Empty, flattened);
            return flattened;
        }
        catch (Exception e)
        {
            _logger.LogWarning("Unable to load site-config.json: {Message}", e.Message);
            return new Dictionary<string, string>(StringComparer.Ordinal);
        }
    }

    private static void Flatten(JsonElement element, string prefix, Dictionary<string, string> target)
    {
        if (element.ValueKind == JsonValueKind.Object)
        {
            foreach (var property in element.EnumerateObject())
            {
                var key = prefix.Length == 0 ? property.Name : $"{prefix}.{property.Name}";
                Flatten(property.Value, key, target);
            }
        }
        else
        {
            target[prefix] = element.ToString();
        }
    }

    private static string ExtractHost(string url)
    {
        if (Uri.TryCreate(url, UriKind.Absolute, out var uri) && !string.IsNullOrEmpty(uri.Host))
        {
            return uri.Host;
        }

        var stripped = url.Replace("https://", string.Empty, StringComparison.OrdinalIgnoreCase)
            .Replace("http://", string.Empty, StringComparison.OrdinalIgnoreCase);
        var slash = stripped.IndexOf('/', StringComparison.Ordinal);
        if (slash > 0)
        {
            stripped = stripped[..slash];
        }

        var colon = stripped.IndexOf(':', StringComparison.Ordinal);
        if (colon > 0)
        {
            stripped = stripped[..colon];
        }

        return stripped;
    }
}
