package com.lmp.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Filtre diagnostic pour les headers X-Forwarded-* et CORS — utile pour debug
 * reverse proxy mais coûte 1 dispatch par requête.
 * <p>
 * Désactivé par défaut (le bean n'existe pas → le filter chain Tomcat n'a rien
 * à dispatcher). Activer via {@code lmp.diagnostic.request-logging.enabled=true}
 * (env var {@code LMP_DIAGNOSTIC_REQUEST_LOGGING_ENABLED=true}) en cas
 * d'investigation X-Forwarded-* / CORS.
 */
@Component
@ConditionalOnProperty(name = "lmp.diagnostic.request-logging.enabled", havingValue = "true")
public class RequestLoggingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Logger diagnosticLogger = LoggerFactory.getLogger("DIAGNOSTIC." + RequestLoggingFilter.class.getName());
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // Short-circuit: si DIAGNOSTIC est sous INFO (défaut staging/prod),
        // on évite getHeader() × ~7 + isWarnEnabled() qui coûtaient ~5-10%
        // de CPU sous load (mesuré bench 50 VUs avant/après).
        if (diagnosticLogger.isInfoEnabled() &&
            request instanceof HttpServletRequest httpRequest &&
            response instanceof HttpServletResponse httpResponse) {

            String requestURI = httpRequest.getRequestURI();
            if (shouldLogRequest(requestURI)) {
                logRequestDetails(httpRequest);
            }
        }

        chain.doFilter(request, response);
    }
    
    /**
     * Détermine si la requête doit être loggée
     */
    private boolean shouldLogRequest(String requestURI) {
        return requestURI.contains("/stripe/") || 
               requestURI.contains("/payment/") || 
               requestURI.contains("/api/") ||
               requestURI.contains("/checkout/") ||
               requestURI.contains("/diagnostic/");
    }
    
    /**
     * Log les détails de la requête pour le diagnostic
     */
    private void logRequestDetails(HttpServletRequest request) {
        
        // Headers X-Forwarded-* critiques pour le reverse proxy
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String forwardedPort = request.getHeader("X-Forwarded-Port");
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String forwardedPrefix = request.getHeader("X-Forwarded-Prefix");
        
        // Information de base sur la requête
        diagnosticLogger.info("REQUEST_HEADERS_DEBUG - URI: '{}', Method: '{}', RemoteAddr: '{}', ServerName: '{}', ServerPort: {}, Scheme: '{}'",
                             request.getRequestURI(), 
                             request.getMethod(),
                             request.getRemoteAddr(),
                             request.getServerName(),
                             request.getServerPort(),
                             request.getScheme());
        
        // Headers X-Forwarded-* (reverse proxy)
        if (hasForwardedHeaders(forwardedProto, forwardedHost, forwardedPort, forwardedFor, forwardedPrefix)) {
            diagnosticLogger.info("FORWARDED_HEADERS_DEBUG - Proto: '{}', Host: '{}', Port: '{}', For: '{}', Prefix: '{}'",
                                 forwardedProto, forwardedHost, forwardedPort, forwardedFor, forwardedPrefix);
        } else {
            diagnosticLogger.warn("FORWARDED_HEADERS_MISSING - No X-Forwarded-* headers detected for URI: '{}'", 
                                 request.getRequestURI());
        }
        
        // Headers Host et Origin pour debugging CORS/Security
        diagnosticLogger.info("HOST_ORIGIN_DEBUG - Host: '{}', Origin: '{}', Referer: '{}'",
                             request.getHeader("Host"),
                             request.getHeader("Origin"),
                             request.getHeader("Referer"));
        
        // User-Agent pour identifier les problèmes browser-specific
        diagnosticLogger.debug("USER_AGENT_DEBUG - User-Agent: '{}'", request.getHeader("User-Agent"));
        
        // Log complet des headers uniquement en mode TRACE
        if (logger.isTraceEnabled()) {
            StringBuilder allHeaders = new StringBuilder();
            Collections.list(request.getHeaderNames()).forEach(headerName -> 
                allHeaders.append(headerName).append(": ").append(request.getHeader(headerName)).append("; ")
            );
            logger.trace("ALL_HEADERS_TRACE - {}", allHeaders.toString());
        }
    }
    
    /**
     * Vérifie si au moins un header X-Forwarded-* est présent
     */
    private boolean hasForwardedHeaders(String... headers) {
        for (String header : headers) {
            if (header != null && !header.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}