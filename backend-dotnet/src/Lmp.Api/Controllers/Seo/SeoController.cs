using Lmp.Application.Seo;
using Microsoft.AspNetCore.Mvc;

namespace Lmp.Api.Controllers.Seo;

/// <summary>
/// Public SEO endpoints. Port of <c>SitemapController</c> + <c>SeoController</c>:
/// dynamically generated <c>/sitemap.xml</c> and a cached <c>/robots.txt</c>.
/// </summary>
[ApiController]
public sealed class SeoController(ISeoService seo) : ControllerBase
{
    [HttpGet("/sitemap.xml")]
    [Produces("application/xml")]
    public async Task<IActionResult> Sitemap(CancellationToken ct)
    {
        var xml = await seo.GenerateSitemapXmlAsync(ct);
        return Content(xml, "application/xml");
    }

    [HttpGet("/robots.txt")]
    [Produces("text/plain")]
    public IActionResult Robots()
    {
        Response.Headers.CacheControl = "public, max-age=3600";
        return Content(seo.GetRobotsTxt(), "text/plain");
    }
}
