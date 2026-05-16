package com.lmp.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.shared.config.site.SiteConfigManager;
import com.lmp.shared.web.AuthHostResolver;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint public exposant la configuration du site au frontend.
 *
 * <p>Inclut les URLs canoniques d'authentification (login, register, OAuth2)
 * pointant vers l'host auth, pour que le SPA puisse rediriger l'utilisateur
 * sans hardcoder ces URLs côté frontend.</p>
 *
 * <p><b>Optim 2026-05-16:</b> response cached en {@code byte[]} pour éviter
 * construction Map + Jackson serialize per request. Bench 50% du mix sous
 * 7k VU mono container 4-core ARM. Le contenu change uniquement quand
 * {@link SiteConfigManager} ou {@link AuthHostResolver} runtime config
 * change — TTL 5 min sur cache local (aligné avec Cache-Control client).</p>
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final SiteConfigManager siteConfigManager;
    private final AuthHostResolver authHostResolver;
    private final ObjectMapper objectMapper;

    private volatile CachedResponse cached;

    public ConfigController(SiteConfigManager siteConfigManager,
                            AuthHostResolver authHostResolver,
                            ObjectMapper objectMapper) {
        this.siteConfigManager = siteConfigManager;
        this.authHostResolver = authHostResolver;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<byte[]> getConfig() throws IOException {
        CachedResponse c = cached;
        Instant now = Instant.now();
        if (c == null || now.isAfter(c.expiresAt)) {
            c = buildResponse(now);
            cached = c;
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CACHE_TTL).cachePublic())
                .contentType(MediaType.APPLICATION_JSON)
                .body(c.bytes);
    }

    private CachedResponse buildResponse(Instant now) throws IOException {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("baseUrl", siteConfigManager.getBaseUrl());
        config.put("frontendUrl", siteConfigManager.getFrontendUrl());
        config.put("siteName", siteConfigManager.getSiteName());
        config.put("supportEmail", siteConfigManager.getSupportEmail());
        config.put("contactEmail", siteConfigManager.getContactEmail());
        config.put("noreplyEmail", siteConfigManager.getNoreplyEmail());

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
        byte[] bytes = objectMapper.writeValueAsBytes(config);
        return new CachedResponse(bytes, now.plus(CACHE_TTL));
    }

    private record CachedResponse(byte[] bytes, Instant expiresAt) {}
}
