package com.lmp.shared.web;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Source de vérité du nom d'host "auth" à partir de {@code app.oauth2.issuer-uri}.
 *
 * <p>Évite tout hardcode {@code "auth."} dans le code applicatif : le sous-domaine
 * peut changer (ex. {@code auth.dev.lmp-services.ca} en staging, {@code localhost}
 * en local). Tout ce qui veut savoir "suis-je sur l'host auth ?" passe par ici.</p>
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
            // Normaliser : sans trailing slash
            this.authBaseUrl = issuerUri.replaceAll("/+$", "");
        } catch (Exception e) {
            this.authHost = null;
            this.authBaseUrl = null;
        }
    }

    /**
     * @return host configuré pour l'issuer OIDC (ex. {@code auth.lmp-services.ca}),
     *         ou {@code null} si non résolu.
     */
    public String getAuthHost() {
        return authHost;
    }

    /**
     * @return URL complète de l'host auth (ex. {@code https://auth.lmp-services.ca}),
     *         ou {@code null} si non résolu.
     */
    public String getAuthBaseUrl() {
        return authBaseUrl;
    }

    /**
     * @param requestHost host extrait de la requête HTTP courante
     * @return true si {@code requestHost} correspond à l'host auth configuré (case-insensitive)
     */
    public boolean isAuthHost(String requestHost) {
        if (authHost == null || requestHost == null) {
            return false;
        }
        return authHost.equalsIgnoreCase(requestHost);
    }
}
