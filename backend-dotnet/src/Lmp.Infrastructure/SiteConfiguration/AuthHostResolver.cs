using Lmp.Application.SiteConfiguration;

namespace Lmp.Infrastructure.SiteConfiguration;

/// <summary>
/// Resolves the OIDC issuer URL from <c>app.oauth2.issuer-uri</c>. Faithful port
/// of <c>com.lmp.shared.web.AuthHostResolver</c>. In single-host mode the issuer
/// is derived from <c>lmp.site.url</c>, so <c>authBaseUrl == siteBaseUrl</c>.
/// </summary>
public sealed class AuthHostResolver : IAuthHostResolver
{
    private readonly string? _authHost;
    private readonly string? _authBaseUrl;

    public AuthHostResolver(ISiteConfigManager siteConfig)
    {
        var issuerUri = siteConfig.GetOauth2IssuerUri();
        if (string.IsNullOrWhiteSpace(issuerUri))
        {
            return;
        }

        if (Uri.TryCreate(issuerUri, UriKind.Absolute, out var uri))
        {
            _authHost = uri.Host;
            _authBaseUrl = issuerUri.TrimEnd('/');
        }
    }

    public string? GetAuthHost() => _authHost;

    public string? GetAuthBaseUrl() => _authBaseUrl;
}
