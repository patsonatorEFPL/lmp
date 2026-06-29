namespace Lmp.Application.Seo;

/// <summary>SEO artifacts (sitemap.xml, robots.txt). Port of SitemapController + SeoController.</summary>
public interface ISeoService
{
    /// <summary>Build the sitemap XML: static pages, active services and published blog posts.</summary>
    Task<string> GenerateSitemapXmlAsync(CancellationToken ct = default);

    /// <summary>The robots.txt body (with the sitemap URL filled in).</summary>
    string GetRobotsTxt();
}
