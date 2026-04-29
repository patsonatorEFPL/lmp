package com.lmp.portal;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
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
 * Cela permet à Dokploy de n'avoir qu'une seule règle de routage
 * par domaine : Host=X, Path=/, Port=8080.
 */
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
public class SpaWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
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

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = super.getResource(resourcePath, location);
            if (resource != null && resource.exists() && resource.isReadable()) {
                return resource;
            }
            if (isBackendPath(resourcePath)) {
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
    }
}
