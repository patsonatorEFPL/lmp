package com.lmp.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Intercepteur SEO pour gérer les redirections canoniques
 * et optimiser l'indexation Google Search Console
 */
@Component
public class SeoInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SeoInterceptor.class);
    
    @Value("${app.base.url}")
    private String canonicalBaseUrl;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUrl = request.getRequestURL().toString();
        String requestUri = request.getRequestURI();
        
        // DIAGNOSTIC LOGS DÉTAILLÉS pour troubleshooting indexation
        String scheme = request.getScheme();
        String host = request.getHeader("Host");
        String xfProto = request.getHeader("X-Forwarded-Proto");
        String xfHost = request.getHeader("X-Forwarded-Host");
        String xfPort = request.getHeader("X-Forwarded-Port");
        String userAgent = request.getHeader("User-Agent");
        
        logger.info("=== SEO INTERCEPTOR DEBUG ===");
        logger.info("Request URL: {}", requestUrl);
        logger.info("Request URI: {}", requestUri);
        logger.info("Canonical base: {}", canonicalBaseUrl);
        logger.info("Scheme: {}, Host: {}", scheme, host);
        logger.info("X-Forwarded-Proto: {}", xfProto);
        logger.info("X-Forwarded-Host: {}", xfHost);
        logger.info("X-Forwarded-Port: {}", xfPort);
        logger.info("User-Agent: {}", userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 100)) + "..." : "null");
        
        // Ajouter des headers SEO
        response.setHeader("X-Robots-Tag", "index, follow");
        
        // Gérer les redirections pour contenu dupliqué seulement pour les pages principales
        if (shouldRedirectToCanonical(request, requestUrl)) {
            String canonicalUrl = canonicalBaseUrl + requestUri;
            logger.info("SEO REDIRECT TRIGGERED:");
            logger.info("  From: {}", requestUrl);
            logger.info("  To: {}", canonicalUrl);
            logger.info("  Method: sendRedirect (302 temporary)");
            logger.info("  Reason: URL mismatch with canonical base");
            
            response.sendRedirect(canonicalUrl);
            return false;
        }
        
        logger.info("=== NO REDIRECT - REQUEST CONTINUES ===");
        return true;
    }
    
    /**
     * Détermine si une redirection canonique est nécessaire
     */
    private boolean shouldRedirectToCanonical(HttpServletRequest request, String requestUrl) {
        String uri = request.getRequestURI();
        
        // DÉSACTIVER TEMPORAIREMENT TOUTES LES REDIRECTIONS pour diagnostiquer la boucle
        logger.info("REDIRECT CHECK - URI: {}, RequestURL: {}, CanonicalBase: {}", uri, requestUrl, canonicalBaseUrl);
        logger.info("REDIRECT CHECK - StartsWith canonical: {}", requestUrl.startsWith(canonicalBaseUrl));
        
        // Exclure les APIs, admin, et ressources statiques
        if (uri.startsWith("/api/") ||
            uri.startsWith("/admin/") ||
            uri.startsWith("/css/") ||
            uri.startsWith("/js/") ||
            uri.startsWith("/images/") ||
            uri.startsWith("/favicon.ico")) {
            logger.info("REDIRECT CHECK - Excluded path: {}", uri);
            return false;
        }
        
        // DÉSACTIVER TOUTES LES REDIRECTIONS TEMPORAIREMENT
        logger.info("REDIRECT CHECK - Would redirect: {} (DISABLED)", !requestUrl.startsWith(canonicalBaseUrl));
        return false; // Temporairement désactivé pour éviter la boucle
        
        // Code original commenté:
        // return !requestUrl.startsWith(canonicalBaseUrl) &&
        //        (uri.equals("/") || uri.equals("/services") || uri.equals("/contact") ||
        //         uri.equals("/about") || uri.equals("/privacy") || uri.equals("/terms"));
    }
}