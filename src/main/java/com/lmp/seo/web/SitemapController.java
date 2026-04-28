package com.lmp.seo.web;

import com.lmp.catalog.domain.Service;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.content.domain.BlogPost;
import com.lmp.content.persistence.BlogPostRepository;

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

    private static final String BASE_URL = "https://lmp-services.ca";
    private static final DateTimeFormatter W3C_DATE = DateTimeFormatter.ISO_DATE;

    private final ServiceRepository serviceRepository;
    private final BlogPostRepository blogPostRepository;

    public SitemapController(ServiceRepository serviceRepository,
                             BlogPostRepository blogPostRepository) {
        this.serviceRepository = serviceRepository;
        this.blogPostRepository = blogPostRepository;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String generateSitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Pages statiques
        addUrl(xml, BASE_URL + "/", "1.0", LocalDate.now());
        addUrl(xml, BASE_URL + "/services", "0.9", LocalDate.now());
        addUrl(xml, BASE_URL + "/about", "0.8", LocalDate.now());
        addUrl(xml, BASE_URL + "/contact", "0.8", LocalDate.now());
        addUrl(xml, BASE_URL + "/map", "0.6", LocalDate.now());
        addUrl(xml, BASE_URL + "/privacy", "0.3", LocalDate.now());
        addUrl(xml, BASE_URL + "/terms", "0.3", LocalDate.now());

        // Services actifs
        List<Service> services = serviceRepository.findByActiveTrue();
        for (Service service : services) {
            addUrl(xml, BASE_URL + "/services/" + service.getSlug(), "0.7",
                    service.getUpdatedAt() != null
                            ? service.getUpdatedAt().toLocalDate()
                            : LocalDate.now());
        }

        // Articles de blog publiés
        List<BlogPost> posts = blogPostRepository.findAllByPublishedTrueOrderByPublishedAtDesc(null)
                .getContent();
        for (BlogPost post : posts) {
            addUrl(xml, BASE_URL + "/blog/" + post.getSlug(), "0.6",
                    post.getUpdatedAt() != null
                            ? post.getUpdatedAt().toLocalDate()
                            : post.getPublishedAt() != null
                                    ? post.getPublishedAt().toLocalDate()
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
