namespace Lmp.Infrastructure.SiteConfiguration;

/// <summary>
/// Options backing <see cref="SiteConfigManager"/>. Mirrors the Spring
/// <c>lmp.site.*</c> properties.
/// </summary>
public sealed class SiteConfigOptions
{
    public const string SectionName = "Lmp:Site";

    /// <summary>Canonical site URL; derived config values are computed from it.</summary>
    public string Url { get; set; } = "http://localhost:8080";

    /// <summary>Optional path to an external <c>site-config.json</c> file.</summary>
    public string? ConfigPath { get; set; }
}
