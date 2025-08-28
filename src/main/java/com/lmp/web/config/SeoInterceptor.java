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
        
        // Log pour diagnostic SEO
        logger.debug("SEO Interceptor - Request: {}, URI: {}, Canonical base: {}", 
                    requestUrl, requestUri, canonicalBaseUrl);
        
        // Ajouter des headers SEO
        response.setHeader("X-Robots-Tag", "index, follow");
        
        // Gérer les redirections pour contenu dupliqué seulement pour les pages principales
        if (shouldRedirectToCanonical(request, requestUrl)) {
            String canonicalUrl = canonicalBaseUrl + requestUri;
            logger.info("SEO Redirect 301 - From: {} To: {}", requestUrl, canonicalUrl);
            response.sendRedirect(canonicalUrl);
            return false;
        }
        
        return true;
    }
    
    /**
     * Détermine si une redirection canonique est nécessaire
     */
    private boolean shouldRedirectToCanonical(HttpServletRequest request, String requestUrl) {
        String uri = request.getRequestURI();
        
        // Exclure les APIs, admin, et ressources statiques
        if (uri.startsWith("/api/") || 
            uri.startsWith("/admin/") || 
            uri.startsWith("/css/") || 
            uri.startsWith("/js/") || 
            uri.startsWith("/images/") ||
            uri.startsWith("/favicon.ico")) {
            return false;
        }
        
        // Seulement pour les pages principales qui ne correspondent pas au domaine canonique
        return !requestUrl.startsWith(canonicalBaseUrl) && 
               (uri.equals("/") || uri.equals("/services") || uri.equals("/contact") || 
                uri.equals("/about") || uri.equals("/privacy") || uri.equals("/terms"));
    }
}