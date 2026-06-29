namespace Lmp.Domain.SiteConfiguration;

/// <summary>
/// A hot-editable site configuration entry — maps to <c>site_config</c>
/// (port of <c>com.lmp.shared.config.site.SiteConfigEntry</c>).
/// </summary>
public class SiteConfigEntry
{
    public string Key { get; set; } = null!;
    public string? Value { get; set; }
    public string? Description { get; set; }
    public DateTime UpdatedAt { get; set; }

    public SiteConfigEntry()
    {
    }

    public SiteConfigEntry(string key, string? value, string? description)
    {
        Key = key;
        Value = value;
        Description = description;
    }
}
