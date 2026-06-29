using Lmp.Application.SiteConfiguration;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Portal;

/// <summary>
/// Public site-configuration endpoint consumed by the frontend on boot. Port of
/// <c>com.lmp.portal.ConfigController</c> (<c>/api/v1/config</c>). Returns a flat
/// string map; key order matches the Java <c>LinkedHashMap</c> insertion order.
/// </summary>
[ApiController]
[Route("api/v1/config")]
public sealed class ConfigController(
    ISiteConfigManager siteConfig,
    IAuthHostResolver authHostResolver) : ControllerBase
{
    [HttpGet]
    public ActionResult<IDictionary<string, string>> GetConfig()
    {
        var config = new Dictionary<string, string>(StringComparer.Ordinal)
        {
            ["baseUrl"] = siteConfig.GetBaseUrl(),
            ["frontendUrl"] = siteConfig.GetFrontendUrl(),
            ["siteName"] = siteConfig.GetSiteName(),
            ["supportEmail"] = siteConfig.GetSupportEmail(),
            ["contactEmail"] = siteConfig.GetContactEmail(),
            ["noreplyEmail"] = siteConfig.GetNoreplyEmail(),
        };

        var authBase = authHostResolver.GetAuthBaseUrl();
        if (authBase is not null)
        {
            config["authBaseUrl"] = authBase;
            config["loginUrl"] = authBase + "/login";
            config["registerUrl"] = authBase + "/register";
            config["forgotPasswordUrl"] = authBase + "/forgot-password";
            config["resetPasswordUrl"] = authBase + "/reset-password";
            config["verifyEmailUrl"] = authBase + "/verify-email";
            config["oauth2GoogleAuthUrl"] = authBase + "/oauth2/authorization/google";
            config["oauth2MicrosoftAuthUrl"] = authBase + "/oauth2/authorization/microsoft";
        }

        Response.Headers.CacheControl = "public, max-age=300";
        return Ok(config);
    }
}
