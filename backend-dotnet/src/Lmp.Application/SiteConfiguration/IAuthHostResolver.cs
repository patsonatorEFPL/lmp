namespace Lmp.Application.SiteConfiguration;

/// <summary>
/// Source of truth for the OIDC issuer URL, derived from <c>app.oauth2.issuer-uri</c>.
/// Port of <c>com.lmp.shared.web.AuthHostResolver</c>. In single-host mode the
/// auth base URL equals the site base URL.
/// </summary>
public interface IAuthHostResolver
{
    /// <summary>Issuer host (e.g. <c>lmp-services.ca</c>), or null when unresolved.</summary>
    string? GetAuthHost();

    /// <summary>Full issuer URL (e.g. <c>https://lmp-services.ca</c>), or null when unresolved.</summary>
    string? GetAuthBaseUrl();
}
