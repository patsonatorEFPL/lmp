package com.lmp.seo.web;

import com.lmp.catalog.domain.Service;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.content.SitemapBlogQuery;
import com.lmp.content.SitemapEntry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Génère dynamiquement le sitemap.xml pour les moteurs de recherche.
 * Inclut les pages statiques et les contenus dynamiques (services, blog).
 */
@RestController
public class SitemapController {

    private static final DateTimeFormatter W3C_DATE = DateTimeFormatter.ISO_DATE;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    private final ServiceRepository serviceRepository;
    private final SitemapBlogQuery sitemapBlogQuery;

    public SitemapController(ServiceRepository serviceRepository,
                             SitemapBlogQuery sitemapBlogQuery) {
        this.serviceRepository = serviceRepository;
        this.sitemapBlogQuery = sitemapBlogQuery;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String generateSitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Pages statiques
        addUrl(xml, baseUrl + "/", "1.0", LocalDate.now());
        addUrl(xml, baseUrl + "/services", "0.9", LocalDate.now());
        addUrl(xml, baseUrl + "/blog", "0.8", LocalDate.now());
        addUrl(xml, baseUrl + "/about", "0.8", LocalDate.now());
        addUrl(xml, baseUrl + "/contact", "0.8", LocalDate.now());
        addUrl(xml, baseUrl + "/map", "0.6", LocalDate.now());
        addUrl(xml, baseUrl + "/privacy", "0.3", LocalDate.now());
        addUrl(xml, baseUrl + "/terms", "0.3", LocalDate.now());

        // Services actifs
        List<Service> services = serviceRepository.findByActiveTrue();
        for (Service service : services) {
            addUrl(xml, baseUrl + "/services/" + service.getSlug(), "0.7",
                    service.getUpdatedAt() != null
                            ? service.getUpdatedAt().toLocalDate()
                            : LocalDate.now());
        }

        // Articles de blog publiés
        List<SitemapEntry> posts = sitemapBlogQuery.findPublishedForSitemap();
        for (SitemapEntry post : posts) {
            addUrl(xml, baseUrl + "/blog/" + post.slug(), "0.6",
                    post.updatedAt() != null
                            ? post.updatedAt().toLocalDate()
                            : post.publishedAt() != null
                                    ? post.publishedAt().toLocalDate()
                                    : LocalDate.now());
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private void addUrl(StringBuilder xml, String loc, String priority, LocalDate lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod.format(W3C_DATE)).append("</lastmod>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String escapeXml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
