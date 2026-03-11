package com.lmp.shared.web.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Contrôleur de diagnostic temporaire pour débugger les problèmes de domaine et reverse proxy
 * 
 * ⚠️ ATTENTION: Ce contrôleur est temporaire et doit être supprimé en production
 * Il expose des informations de configuration sensibles
 */
@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {
    
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticController.class);
    
    @Value("${app.base.url}")
    private String baseUrl;
    
    @Value("${stripe.checkout.success.url:/stripe/checkout/success}")
    private String defaultSuccessUrl;
    
    @Value("${stripe.checkout.cancel.url:/stripe/checkout/cancel}")
    private String defaultCancelUrl;
    
    @Value("${server.forward-headers-strategy:NONE}")
    private String forwardHeadersStrategy;
    
    /**
     * Endpoint de diagnostic pour valider la configuration du reverse proxy
     * 
     * URL: GET /api/diagnostic/proxy-headers
     * 
     * ⚠️ TEMPORAIRE - À SUPPRIMER EN PRODUCTION
     */
    @GetMapping("/proxy-headers")
    public ResponseEntity<Map<String, Object>> diagnosticProxyHeaders(HttpServletRequest request) {
        
        logger.info("DIAGNOSTIC_ACCESS - Proxy headers diagnostic accessed from IP: {}", 
                   request.getRemoteAddr());
        
        Map<String, Object> diagnostic = new HashMap<>();
        
        // Informations de base
        diagnostic.put("timestamp", LocalDateTime.now().toString());
        diagnostic.put("request_url", request.getRequestURL().toString());
        diagnostic.put("request_uri", request.getRequestURI());
        diagnostic.put("server_name", request.getServerName());
        diagnostic.put("server_port", request.getServerPort());
        diagnostic.put("scheme", request.getScheme());
        diagnostic.put("remote_addr", request.getRemoteAddr());
        
        // Configuration de l'application
        Map<String, Object> appConfig = new HashMap<>();
        appConfig.put("base_url", baseUrl);
        appConfig.put("forward_headers_strategy", forwardHeadersStrategy);
        appConfig.put("success_url_template", defaultSuccessUrl);
        appConfig.put("cancel_url_template", defaultCancelUrl);
        diagnostic.put("app_config", appConfig);
        
        // Headers X-Forwarded-* (reverse proxy)
        Map<String, String> forwardedHeaders = new HashMap<>();
        forwardedHeaders.put("X-Forwarded-Proto", request.getHeader("X-Forwarded-Proto"));
        forwardedHeaders.put("X-Forwarded-Host", request.getHeader("X-Forwarded-Host"));
        forwardedHeaders.put("X-Forwarded-Port", request.getHeader("X-Forwarded-Port"));
        forwardedHeaders.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        forwardedHeaders.put("X-Forwarded-Prefix", request.getHeader("X-Forwarded-Prefix"));
        diagnostic.put("forwarded_headers", forwardedHeaders);
        
        // Tous les headers pour debugging complet
        Map<String, String> allHeaders = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(headerName -> 
            allHeaders.put(headerName, request.getHeader(headerName))
        );
        diagnostic.put("all_headers", allHeaders);
        
        // Génération des URLs de callback Stripe simulées
        Map<String, String> stripeUrls = new HashMap<>();
        long testOrderId = 12345L;
        
        String simulatedSuccessUrl = String.format("%s%s?order_id=%d&session_id={CHECKOUT_SESSION_ID}&type=%s",
                                    baseUrl, defaultSuccessUrl, testOrderId, "success");
        String simulatedCancelUrl = String.format("%s%s?order_id=%d&session_id={CHECKOUT_SESSION_ID}&type=%s",
                                   baseUrl, defaultCancelUrl, testOrderId, "cancel");
        
        stripeUrls.put("simulated_success_url", simulatedSuccessUrl);
        stripeUrls.put("simulated_cancel_url", simulatedCancelUrl);
        stripeUrls.put("urls_are_https",
                      String.valueOf(simulatedSuccessUrl.startsWith("https://") && simulatedCancelUrl.startsWith("https://")));
        diagnostic.put("stripe_callback_urls", stripeUrls);
        
        // Détection des problèmes potentiels
        Map<String, Object> issues = new HashMap<>();
        
        // Vérification du reverse proxy
        boolean hasForwardedHeaders = forwardedHeaders.values().stream()
            .anyMatch(header -> header != null && !header.trim().isEmpty());
        issues.put("reverse_proxy_detected", hasForwardedHeaders);
        
        // Vérification HTTPS
        boolean isHttps = "https".equals(request.getScheme()) || 
                         "https".equals(request.getHeader("X-Forwarded-Proto"));
        issues.put("https_detected", isHttps);
        
        // Vérification de la configuration de base
        issues.put("base_url_configured", baseUrl != null && !baseUrl.trim().isEmpty());
        issues.put("base_url_is_https", baseUrl != null && baseUrl.startsWith("https://"));
        issues.put("forward_headers_strategy_enabled", 
                  !"NONE".equals(forwardHeadersStrategy));
        
        diagnostic.put("potential_issues", issues);
        
        // Log complet pour les fichiers de log
        logger.info("DIAGNOSTIC_FULL - Complete diagnostic info: {}", diagnostic);
        
        return ResponseEntity.ok(diagnostic);
    }
    
    /**
     * Endpoint simple pour vérifier la connectivité de base
     * 
     * URL: GET /api/diagnostic/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("base_url", baseUrl);
        
        logger.info("DIAGNOSTIC_HEALTH - Health check accessed");
        
        return ResponseEntity.ok(health);
    }
}