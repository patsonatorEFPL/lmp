package com.lmp.portal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Contrôleur SEO pour la gestion du robots.txt.
 * Le sitemap.xml est géré par {@link com.lmp.seo.web.SitemapController}.
 */
@Controller
public class SeoController {

    private static final Logger logger = LoggerFactory.getLogger(SeoController.class);

    @Value("${app.base.url}")
    private String baseUrl;

    /**
     * Génère le robots.txt optimisé pour SEO.
     * Accessible via : https://lmp-services.ca/robots.txt
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots(HttpServletResponse response) {
        logger.info("Génération du robots.txt pour SEO");

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        StringBuilder robots = new StringBuilder();

        // Règles générales pour tous les robots
        robots.append("User-agent: *\n");
        robots.append("Allow: /\n");
        robots.append("Allow: /css/\n");
        robots.append("Allow: /js/\n");
        robots.append("Allow: /images/\n");
        robots.append("Allow: /favicon.ico\n");
        robots.append("\n");

        // Pages à ne pas indexer
        robots.append("# Pages privées et administratives\n");
        robots.append("Disallow: /admin/\n");
        robots.append("Disallow: /user/\n");
        robots.append("Disallow: /api/\n");
        robots.append("Disallow: /debug/\n");
        robots.append("\n");

        // Pages de processus (checkout, etc.)
        robots.append("# Pages de processus\n");
        robots.append("Disallow: /auth/\n");
        robots.append("Disallow: /payment/\n");
        robots.append("Disallow: /stripe/\n");
        robots.append("\n");

        // Règles spéciales pour Google
        robots.append("# Optimisations Google\n");
        robots.append("User-agent: Googlebot\n");
        robots.append("Allow: /\n");
        robots.append("Crawl-delay: 1\n");
        robots.append("\n");

        // Lien vers le sitemap
        robots.append("# Sitemap\n");
        robots.append("Sitemap: ").append(baseUrl).append("/sitemap.xml\n");

        logger.info("Robots.txt généré avec succès");
        return robots.toString();
    }
}
