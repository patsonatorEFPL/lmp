namespace Lmp.Application.SiteConfiguration;

/// <summary>
/// Central site-configuration accessor. Contract for
/// <c>com.lmp.shared.config.site.SiteConfigManager</c>. Reads resolve through a
/// hierarchy: environment/config → <c>site-config.json</c> → <c>site_config</c>
/// table → values derived from <c>lmp.site.url</c>, all behind a memory cache.
/// </summary>
public interface ISiteConfigManager
{
    string? GetString(string key);

    string GetString(string key, string defaultValue);

    bool GetBoolean(string key, bool defaultValue);

    string GetBaseUrl();

    string GetFrontendUrl();

    string GetSiteName();

    string GetSupportEmail();

    string GetContactEmail();

    string GetNoreplyEmail();

    string GetCompanyWebsite();

    string GetOauth2IssuerUri();

    string GetCorsAllowedOrigins();

    /// <summary>Persist a value to the database and invalidate the cache.</summary>
    Task UpdateAsync(string key, string value, string? description, CancellationToken ct = default);

    /// <summary>All entries currently stored in the database, ordered by key.</summary>
    Task<IReadOnlyDictionary<string, string?>> GetAllFromDbAsync(CancellationToken ct = default);
}
