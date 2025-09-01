package com.lmp.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Contrôleur SEO pour la gestion du sitemap.xml et robots.txt
 * Optimisé pour le référencement Google et autres moteurs de recherche
 */
@Controller
public class SeoController {
    
    private static final Logger logger = LoggerFactory.getLogger(SeoController.class);
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    @Value("${app.base.url}")
    private String baseUrl;
    
    /**
     * Génère le sitemap.xml dynamique avec toutes les pages importantes
     * Accessible via : https://lmp.run.place/sitemap.xml
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap(HttpServletResponse response) {
        logger.info("Génération du sitemap.xml pour SEO");
        
        response.setContentType("application/xml");
        response.setCharacterEncoding("UTF-8");
        
        String currentDate = LocalDate.now().format(ISO_DATE_FORMATTER);
        
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        // Page d'accueil - priorité maximale
        addUrl(sitemap, baseUrl + "/", "1.0", "daily", currentDate);
        
        // Pages principales - haute priorité
        addUrl(sitemap, baseUrl + "/services", "0.9", "weekly", currentDate);
        addUrl(sitemap, baseUrl + "/contact", "0.9", "weekly", currentDate);
        addUrl(sitemap, baseUrl + "/about", "0.8", "monthly", currentDate);
        
        // Pages légales - priorité moyenne
        addUrl(sitemap, baseUrl + "/privacy", "0.6", "monthly", currentDate);
        addUrl(sitemap, baseUrl + "/terms", "0.6", "monthly", currentDate);
        
        // Pages utilisateur - priorité moyenne
        addUrl(sitemap, baseUrl + "/auth/login", "0.7", "weekly", currentDate);
        addUrl(sitemap, baseUrl + "/auth/register", "0.7", "weekly", currentDate);
        
        // Pages de paiement - basse priorité (pas indexées mais déclarées)
        addUrl(sitemap, baseUrl + "/payment/success", "0.3", "yearly", currentDate);
        addUrl(sitemap, baseUrl + "/payment/cancel", "0.3", "yearly", currentDate);
        
        sitemap.append("</urlset>");
        
        logger.info("Sitemap.xml généré avec succès - {} URLs", countUrls(sitemap.toString()));
        return sitemap.toString();
    }
    
    /**
     * Génère le robots.txt optimisé pour SEO
     * Accessible via : https://lmp.run.place/robots.txt
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
    
    /**
     * Ajoute une URL au sitemap avec ses métadonnées SEO
     */
    private void addUrl(StringBuilder sitemap, String url, String priority, String changeFreq, String lastMod) {
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(url).append("</loc>\n");
        sitemap.append("    <lastmod>").append(lastMod).append("</lastmod>\n");
        sitemap.append("    <changefreq>").append(changeFreq).append("</changefreq>\n");
        sitemap.append("    <priority>").append(priority).append("</priority>\n");
        sitemap.append("  </url>\n");
    }
    
    /**
     * Compte le nombre d'URLs dans le sitemap pour les logs
     */
    private int countUrls(String sitemap) {
        return sitemap.split("<url>").length - 1;
    }
}