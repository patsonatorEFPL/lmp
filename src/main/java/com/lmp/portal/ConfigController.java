package com.lmp.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.shared.config.site.SiteConfigManager;
import com.lmp.shared.web.AuthHostResolver;
import com.lmp.shared.web.PrecompressedResponse;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint public exposant la configuration du site au frontend.
 *
 * <p>Response pre-cached + gzip pré-compressé via {@link PrecompressedResponse}.
 * Skip Jackson serialize + gzip compress per request. Bench iter16
 * confirme sub-5ms serve sur cache hit.</p>
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final CacheControl PUBLIC_CACHE = CacheControl
            .maxAge(CACHE_TTL)
            .cachePublic();

    private final SiteConfigManager siteConfigManager;
    private final AuthHostResolver authHostResolver;
    private final ObjectMapper objectMapper;

    private volatile PrecompressedResponse cached;

    public ConfigController(SiteConfigManager siteConfigManager,
                            AuthHostResolver authHostResolver,
                            ObjectMapper objectMapper) {
        this.siteConfigManager = siteConfigManager;
        this.authHostResolver = authHostResolver;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<byte[]> getConfig(HttpServletRequest httpRequest) throws IOException {
        PrecompressedResponse r = cached;
        Instant now = Instant.now();
        if (r == null || now.isAfter(r.expiresAt())) {
            r = build(now);
            cached = r;
        }
        boolean gz = false;
        String ae = httpRequest.getHeader("Accept-Encoding");
        if (ae != null && ae.contains("gzip")) {
            gz = true;
        }
        ResponseEntity.BodyBuilder b = ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .header("Vary", "Accept-Encoding")
                .contentType(MediaType.APPLICATION_JSON);
        if (gz) {
            b.header("Content-Encoding", "gzip");
            return b.body(r.gzip());
        }
        return b.body(r.raw());
    }

    private PrecompressedResponse build(Instant now) throws IOException {
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
        return PrecompressedResponse.build(bytes, now.plus(CACHE_TTL));
    }
}
