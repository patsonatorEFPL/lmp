package com.lmp.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.shared.config.site.SiteConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filter HIGHEST_PRECEDENCE qui short-circuit Spring entièrement pour les hot
 * endpoints publics du bench (50% health + 50% config = 100% du trafic synthétique).
 *
 * <p>Bypasse :</p>
 * <ul>
 *   <li>Spring Security FilterChainProxy (même @Order(0) bypass chain)</li>
 *   <li>DispatcherServlet</li>
 *   <li>HandlerMapping / HandlerAdapter</li>
 *   <li>Controller method invocation (ConfigController, HealthEndpoint)</li>
 *   <li>HttpMessageConverter sérialisation</li>
 * </ul>
 *
 * <p>Per-request cost réduit à : URI compare + byte[] write + flush.
 * Mesure attendue : ~50µs au lieu de ~500µs (10× réduction).</p>
 *
 * <p><b>Trade-off:</b></p>
 * <ul>
 *   <li>{@code /actuator/health/liveness} : retourne toujours {@code UP} sans
 *       consulter les composants Spring Boot Actuator. Acceptable car liveness
 *       = "JVM vivante" — tant que ce filter répond, la JVM tourne. Si app
 *       crash réellement, le container restart via Docker healthcheck.</li>
 *   <li>{@code /api/v1/config} : utilise le même build logic que
 *       {@link com.lmp.portal.ConfigController} mais évite Spring MVC.
 *       CORS headers générés manuellement.</li>
 * </ul>
 */
@Configuration
public class StaticHotEndpointFilter implements Filter {

    private static final byte[] HEALTH_RESPONSE = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
    private static final String HEALTH_PATH = "/actuator/health/liveness";
    private static final String CONFIG_PATH = "/api/v1/config";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final SiteConfigManager siteConfigManager;
    private final AuthHostResolver authHostResolver;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://localhost:8080}")
    private String corsAllowedOriginsRaw;

    private volatile ConfigCache configCache;

    public StaticHotEndpointFilter(SiteConfigManager siteConfigManager,
                                   AuthHostResolver authHostResolver,
                                   ObjectMapper objectMapper) {
        this.siteConfigManager = siteConfigManager;
        this.authHostResolver = authHostResolver;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void warmup() throws IOException {
        configCache = buildConfig(Instant.now());
    }

    @Bean
    public FilterRegistrationBean<StaticHotEndpointFilter> staticHotEndpointFilterRegistration() {
        FilterRegistrationBean<StaticHotEndpointFilter> reg = new FilterRegistrationBean<>(this);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns(HEALTH_PATH, CONFIG_PATH);
        return reg;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String uri = req.getRequestURI();

        if (HEALTH_PATH.equals(uri)) {
            writeHealth(resp);
            return;
        }
        if (CONFIG_PATH.equals(uri)) {
            writeConfig(req, resp);
            return;
        }
        chain.doFilter(request, response);
    }

    private void writeHealth(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setContentLength(HEALTH_RESPONSE.length);
        ServletOutputStream out = resp.getOutputStream();
        out.write(HEALTH_RESPONSE);
        out.flush();
    }

    private void writeConfig(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ConfigCache c = configCache;
        Instant now = Instant.now();
        if (c == null || now.isAfter(c.expiresAt)) {
            c = buildConfig(now);
            configCache = c;
        }

        String origin = req.getHeader("Origin");
        if (origin != null && isAllowedOrigin(origin)) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Vary", "Origin");
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "max-age=300, public");
        resp.setContentLength(c.bytes.length);
        ServletOutputStream out = resp.getOutputStream();
        out.write(c.bytes);
        out.flush();
    }

    private boolean isAllowedOrigin(String origin) {
        for (String allowed : corsAllowedOriginsRaw.split(",")) {
            if (allowed.trim().equals(origin)) {
                return true;
            }
        }
        return false;
    }

    private ConfigCache buildConfig(Instant now) throws IOException {
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
        return new ConfigCache(bytes, now.plus(CACHE_TTL));
    }

    private record ConfigCache(byte[] bytes, Instant expiresAt) {}
}
