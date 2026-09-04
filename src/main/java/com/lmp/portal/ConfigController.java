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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, PrecompressedResponse> cacheByBaseUrl = new ConcurrentHashMap<>();

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
        String effectiveBaseUrl = resolveBaseUrl(httpRequest);
        Instant now = Instant.now();
        PrecompressedResponse r = cacheByBaseUrl.get(effectiveBaseUrl);
        if (r == null || now.isAfter(r.expiresAt())) {
            r = build(effectiveBaseUrl, now);
            cacheByBaseUrl.put(effectiveBaseUrl, r);
        }
        String ifNoneMatch = httpRequest.getHeader("If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.contains(r.etag())) {
            return ResponseEntity.status(304)
                    .cacheControl(PUBLIC_CACHE)
                    .header("ETag", r.etag())
                    .header("Vary", "Host, Accept-Encoding")
                    .build();
        }
        boolean gz = false;
        String ae = httpRequest.getHeader("Accept-Encoding");
        if (ae != null && ae.contains("gzip")) {
            gz = true;
        }
        ResponseEntity.BodyBuilder b = ResponseEntity.ok()
                .cacheControl(PUBLIC_CACHE)
                .header("ETag", r.etag())
                .header("Vary", "Host, Accept-Encoding")
                .contentType(MediaType.APPLICATION_JSON);
        if (gz) {
            b.header("Content-Encoding", "gzip");
            return b.body(r.gzip());
        }
        return b.body(r.raw());
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        try {
            String contextUrl = ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
            if (contextUrl != null && !contextUrl.isBlank()) {
                return contextUrl.replaceAll("/+$", "");
            }
        } catch (Exception ignored) {
        }
        return siteConfigManager.getBaseUrl();
    }

    private PrecompressedResponse build(String effectiveBaseUrl, Instant now) throws IOException {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("baseUrl", effectiveBaseUrl);
        config.put("frontendUrl", effectiveBaseUrl);
        config.put("siteName", siteConfigManager.getSiteName());
        config.put("supportEmail", siteConfigManager.getSupportEmail());
        config.put("contactEmail", siteConfigManager.getContactEmail());
        config.put("noreplyEmail", siteConfigManager.getNoreplyEmail());

        config.put("authBaseUrl", effectiveBaseUrl);
        config.put("loginUrl", effectiveBaseUrl + "/login");
        config.put("registerUrl", effectiveBaseUrl + "/register");
        config.put("forgotPasswordUrl", effectiveBaseUrl + "/forgot-password");
        config.put("resetPasswordUrl", effectiveBaseUrl + "/reset-password");
        config.put("verifyEmailUrl", effectiveBaseUrl + "/verify-email");
        config.put("oauth2GoogleAuthUrl", effectiveBaseUrl + "/oauth2/authorization/google");
        config.put("oauth2MicrosoftAuthUrl", effectiveBaseUrl + "/oauth2/authorization/microsoft");

        byte[] bytes = objectMapper.writeValueAsBytes(config);
        return PrecompressedResponse.build(bytes, now.plus(CACHE_TTL));
    }
}
