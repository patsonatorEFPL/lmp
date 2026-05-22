package com.lmp.shared.web;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Source de vérité de l'URL d'issuer OIDC à partir de {@code app.oauth2.issuer-uri}.
 *
 * <p>Mode single-host (depuis 2026-05) : l'issuer URI est dérivé directement
 * de {@code lmp.site.url} dans {@link com.lmp.shared.config.site.SiteConfigManager}
 * — donc {@code authBaseUrl == siteBaseUrl} en pratique. Cette classe expose
 * une URL absolue stable utilisée par les emails de vérification, reset
 * password, et les redirections OAuth2 côté frontend.</p>
 */
@Component
public class AuthHostResolver {

    private final String issuerUri;
    private String authHost;
    private String authBaseUrl;

    public AuthHostResolver(@Value("${app.oauth2.issuer-uri:}") String issuerUri) {
        this.issuerUri = issuerUri;
    }

    @PostConstruct
    void init() {
        if (issuerUri == null || issuerUri.isBlank()) {
            this.authHost = null;
            this.authBaseUrl = null;
            return;
        }
        try {
            this.authHost = URI.create(issuerUri).getHost();
            this.authBaseUrl = issuerUri.replaceAll("/+$", "");
        } catch (Exception e) {
            this.authHost = null;
            this.authBaseUrl = null;
        }
    }

    /**
     * @return host de l'issuer OIDC (ex. {@code lmp-services.ca}),
     *         ou {@code null} si non résolu.
     */
    public String getAuthHost() {
        return authHost;
    }

    /**
     * @return URL complète de l'issuer (ex. {@code https://lmp-services.ca}),
     *         ou {@code null} si non résolu.
     */
    public String getAuthBaseUrl() {
        return authBaseUrl;
    }
}
