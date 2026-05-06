package com.lmp.portal;

import com.lmp.shared.config.site.SiteConfigManager;
import com.lmp.shared.web.AuthHostResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint public exposant la configuration du site au frontend.
 *
 * <p>Inclut les URLs canoniques d'authentification (login, register, OAuth2)
 * pointant vers l'host auth, pour que le SPA puisse rediriger l'utilisateur
 * sans hardcoder ces URLs côté frontend.</p>
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final SiteConfigManager siteConfigManager;
    private final AuthHostResolver authHostResolver;

    public ConfigController(SiteConfigManager siteConfigManager,
                            AuthHostResolver authHostResolver) {
        this.siteConfigManager = siteConfigManager;
        this.authHostResolver = authHostResolver;
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("baseUrl", siteConfigManager.getBaseUrl());
        config.put("frontendUrl", siteConfigManager.getFrontendUrl());
        config.put("siteName", siteConfigManager.getSiteName());
        config.put("supportEmail", siteConfigManager.getSupportEmail());
        config.put("contactEmail", siteConfigManager.getContactEmail());
        config.put("noreplyEmail", siteConfigManager.getNoreplyEmail());

        // URLs auth canoniques (dérivées de app.oauth2.issuer-uri).
        // Le frontend utilise ces URLs pour toutes les redirections d'authentification —
        // pas de hardcode côté Angular.
        String authBase = authHostResolver.getAuthBaseUrl();
        if (authBase != null) {
            config.put("authBaseUrl", authBase);
            config.put("loginUrl", authBase + "/login");
            config.put("registerUrl", authBase + "/register");
            config.put("forgotPasswordUrl", authBase + "/forgot-password");
            config.put("resetPasswordUrl", authBase + "/reset-password");
            config.put("verifyEmailUrl", authBase + "/verify-email");
            config.put("oauth2GoogleAuthUrl", authBase + "/oauth2/authorization/google");
            config.put("oauth2MicrosoftAuthUrl", authBase + "/oauth2/authorization/microsoft");
        }
        return config;
    }
}
