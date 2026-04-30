package com.lmp.portal;

import com.lmp.shared.web.AuthHostResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;

/**
 * Configuration Spring MVC pour servir l'application Angular SPA
 * directement depuis le backend (mode monolithique).
 *
 * Le resource handler répond pour toute requête qui n'a pas été
 * matchée par un @Controller. Les chemins du backend (api, oauth2,
 * actuator, etc.) sont explicitement exclus du fallback : un path
 * inexistant sous ces préfixes retourne 404 plutôt qu'un index.html
 * (sinon le frontend tenterait de parser du HTML en JSON).
 *
 * Sur l'host auth (résolu depuis {@code app.oauth2.issuer-uri}, ex.
 * {@code auth.lmp-services.ca}), on ne sert le SPA QUE pour les pages
 * d'authentification ({@code /login}, {@code /register}, etc.). Les
 * pages marketing ({@code /services}, {@code /blog}, {@code /about},
 * etc.) retournent 404 pour éviter le duplicate-content SEO et garder
 * une séparation propre.
 *
 * Cela permet à Dokploy de n'avoir qu'une seule règle de routage
 * par domaine : Host=X, Path=/, Port=8080.
 */
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
public class SpaWebMvcConfigurer implements WebMvcConfigurer {

    private final AuthHostResolver authHostResolver;

    public SpaWebMvcConfigurer(AuthHostResolver authHostResolver) {
        this.authHostResolver = authHostResolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver(authHostResolver));
    }

    static class SpaResourceResolver extends PathResourceResolver {

        // Préfixes de paths gérés par un controller / filter chain backend.
        // Si la résolution arrive ici pour un de ces paths, c'est que rien
        // ne l'a matché — il faut retourner null (404), pas index.html.
        private static final List<String> BACKEND_PREFIXES = List.of(
                "api/",
                "oauth2/",
                "actuator/",
                "stripe/",
                ".well-known/",
                "userinfo",
                "connect/",
                "swagger-ui",
                "v3/api-docs",
                "perform-login",
                "logout",
                "login/oauth2/",
                "error"
        );

        // Sur l'host auth, on n'autorise le fallback SPA que pour ces
        // premiers segments (pages d'authentification nécessaires au flux OIDC).
        // Tout le reste retourne 404 sur l'host auth.
        private static final List<String> AUTH_HOST_ALLOWED_SPA_FIRST_SEGMENTS = List.of(
                "login",
                "register",
                "forgot-password",
                "reset-password",
                "verify-email"
        );

        private final AuthHostResolver authHostResolver;

        SpaResourceResolver(AuthHostResolver authHostResolver) {
            this.authHostResolver = authHostResolver;
        }

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = super.getResource(resourcePath, location);
            if (resource != null && resource.exists() && resource.isReadable()) {
                // Asset statique (JS, CSS, images, favicon, manifest) — toujours servi
                // peu importe le host, sinon les pages auth ne pourraient pas charger leurs bundles.
                return resource;
            }
            if (isBackendPath(resourcePath)) {
                return null;
            }
            if (isOnAuthHost() && !isAuthSpaPath(resourcePath)) {
                return null;
            }
            return super.getResource("index.html", location);
        }

        private static boolean isBackendPath(String resourcePath) {
            if (resourcePath == null || resourcePath.isEmpty()) {
                return false;
            }
            for (String prefix : BACKEND_PREFIXES) {
                if (resourcePath.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isOnAuthHost() {
            try {
                ServletRequestAttributes attrs =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs == null) {
                    return false;
                }
                return authHostResolver.isAuthHost(attrs.getRequest().getServerName());
            } catch (Exception e) {
                return false;
            }
        }

        private static boolean isAuthSpaPath(String resourcePath) {
            if (resourcePath == null || resourcePath.isEmpty()) {
                return false;
            }
            int slash = resourcePath.indexOf('/');
            String firstSegment = (slash < 0) ? resourcePath : resourcePath.substring(0, slash);
            return AUTH_HOST_ALLOWED_SPA_FIRST_SEGMENTS.contains(firstSegment);
        }
    }
}
