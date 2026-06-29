using System.Globalization;
using System.Text;
using Lmp.Application.Catalog;
using Lmp.Application.Seo;
using Lmp.Application.SiteConfiguration;
using Lmp.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace Lmp.Infrastructure.Seo;

/// <summary>
/// Builds sitemap.xml and robots.txt. Faithful port of
/// <c>com.lmp.seo.web.SitemapController</c> and <c>com.lmp.portal.SeoController</c>.
/// </summary>
public sealed class SeoService(
    IServiceCatalogService catalog,
    ISiteConfigManager siteConfig,
    LmpDbContext db) : ISeoService
{
    public async Task<string> GenerateSitemapXmlAsync(CancellationToken ct = default)
    {
        var baseUrl = siteConfig.GetBaseUrl();
        var today = DateOnly.FromDateTime(DateTime.Now);

        var xml = new StringBuilder();
        xml.Append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.Append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        AddUrl(xml, baseUrl + "/", "1.0", today);
        AddUrl(xml, baseUrl + "/services", "0.9", today);
        AddUrl(xml, baseUrl + "/blog", "0.8", today);
        AddUrl(xml, baseUrl + "/about", "0.8", today);
        AddUrl(xml, baseUrl + "/contact", "0.8", today);
        AddUrl(xml, baseUrl + "/map", "0.6", today);
        AddUrl(xml, baseUrl + "/privacy", "0.3", today);
        AddUrl(xml, baseUrl + "/terms", "0.3", today);

        foreach (var service in await catalog.GetActiveServicesAsync(ct))
        {
            var lastmod = service.UpdatedAt is { } u ? DateOnly.FromDateTime(u) : today;
            AddUrl(xml, $"{baseUrl}/services/{service.Slug}", "0.7", lastmod);
        }

        var posts = await db.BlogPosts.AsNoTracking()
            .Where(p => p.Published)
            .OrderByDescending(p => p.PublishedAt)
            .Select(p => new { p.Slug, p.UpdatedAt, p.PublishedAt })
            .ToListAsync(ct);

        foreach (var post in posts)
        {
            var lastmod = post.UpdatedAt is { } u
                ? DateOnly.FromDateTime(u)
                : post.PublishedAt is { } pub ? DateOnly.FromDateTime(pub) : today;
            AddUrl(xml, $"{baseUrl}/blog/{post.Slug}", "0.6", lastmod);
        }

        xml.Append("</urlset>");
        return xml.ToString();
    }

    public string GetRobotsTxt()
    {
        var baseUrl = siteConfig.GetBaseUrl();
        return $"""
            User-agent: *
            Allow: /
            Allow: /css/
            Allow: /js/
            Allow: /images/
            Allow: /favicon.ico

            # Pages privées et administratives
            Disallow: /admin/
            Disallow: /user/
            Disallow: /api/
            Disallow: /debug/

            # Pages de processus
            Disallow: /auth/
            Disallow: /payment/
            Disallow: /stripe/

            # Optimisations Google
            User-agent: Googlebot
            Allow: /
            Crawl-delay: 1

            # Sitemap
            Sitemap: {baseUrl}/sitemap.xml

            """;
    }

    private static void AddUrl(StringBuilder xml, string loc, string priority, DateOnly lastmod)
    {
        xml.Append("  <url>\n");
        xml.Append("    <loc>").Append(EscapeXml(loc)).Append("</loc>\n");
        xml.Append("    <lastmod>").Append(lastmod.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture)).Append("</lastmod>\n");
        xml.Append("    <priority>").Append(priority).Append("</priority>\n");
        xml.Append("  </url>\n");
    }

    private static string EscapeXml(string text) => text
        .Replace("&", "&amp;", StringComparison.Ordinal)
        .Replace("<", "&lt;", StringComparison.Ordinal)
        .Replace(">", "&gt;", StringComparison.Ordinal)
        .Replace("\"", "&quot;", StringComparison.Ordinal)
        .Replace("'", "&apos;", StringComparison.Ordinal);
}
