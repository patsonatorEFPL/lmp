package com.lmp.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Rate limiting per-IP sur les endpoints d'authentification non-authentifiés :
 * login, forgot-password, reset-password, verify-email, register.
 *
 * <p>Limite + fenêtre dépendent de l'endpoint (login = plus serré pour ralentir
 * brute-force / credential stuffing ; register/forgot = limite anti-spam). Compteur
 * Redis avec EXPIRE TTL → fenêtre glissante simple, partagée entre replicas.</p>
 *
 * <p>Clé = IP source uniquement (pas d'auth disponible pré-login). En cas de
 * Redis indisponible, fail-open avec warn log — Cloudflare WAF en amont doit
 * couvrir le pire cas.</p>
 */
@Component
@Order(3)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    /** Per-endpoint policy: (max attempts, window duration). */
    private record Policy(int max, Duration window) {}

    private static final Policy LOGIN_POLICY = new Policy(10, Duration.ofMinutes(5));
    private static final Policy SENSITIVE_POLICY = new Policy(5, Duration.ofMinutes(15));
    private static final Policy REGISTER_POLICY = new Policy(8, Duration.ofMinutes(10));

    private static final Map<String, Policy> POLICIES = Map.of(
            "/api/v1/auth/login", LOGIN_POLICY,
            "/perform-login", LOGIN_POLICY,
            "/api/v1/auth/forgot-password", SENSITIVE_POLICY,
            "/api/v1/auth/reset-password", SENSITIVE_POLICY,
            "/api/v1/auth/verify-email", SENSITIVE_POLICY,
            "/api/v1/auth/register", REGISTER_POLICY
    );

    private static final Set<String> EXACT_PATHS = POLICIES.keySet();
    private static final String KEY_PREFIX = "rate:auth:";

    private final StringRedisTemplate redis;

    /**
     * SECURITY (M6) : comportement si Redis down.
     * Même flag que AdminRateLimitFilter ({@code lmp.rate-limit.fail-closed}).
     * Default false = fail-open. Prod recommandée = true.
     */
    @Value("${lmp.rate-limit.fail-closed:false}")
    private boolean failClosedOnRedisDown;

    public AuthRateLimitFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        Policy policy = POLICIES.get(path);
        if (policy == null) {
            // Fallback : prefix-match for /api/v1/auth/* not in exact list (no rate-limit).
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        String key = KEY_PREFIX + path + ":" + ip;
        long current;
        try {
            Long incr = redis.opsForValue().increment(key);
            current = incr != null ? incr : 0L;
            if (current == 1L) {
                redis.expire(key, policy.window());
            }
        } catch (RuntimeException e) {
            // SECURITY (M6) : Redis down — comportement configurable.
            // Fail-closed protège contre brute-force quand attaquant down-e Redis,
            // au prix de bloquer le login légitime. Fail-open inverse le trade-off.
            if (failClosedOnRedisDown) {
                log.error("[AUTH-RATE-LIMIT] Redis indisponible — fail-CLOSED (503): {}", e.getMessage());
                response.setStatus(503);
                response.setContentType("application/json");
                response.setHeader("Retry-After", "30");
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Service d'authentification temporairement indisponible. Réessayez dans quelques instants.\"}");
                return;
            }
            log.warn("[AUTH-RATE-LIMIT] Redis indisponible — fail-open: {}", e.getMessage());
            chain.doFilter(request, response);
            return;
        }

        if (current > policy.max()) {
            log.warn("[AUTH-RATE-LIMIT] {} blocked for IP {} ({} > {} per {})",
                    path, ip, current, policy.max(), policy.window());
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(policy.window().toSeconds()));
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Trop de tentatives. Réessayez plus tard.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Resolve client IP behind proxy. Spring Boot {@code server.forward-headers-strategy=NATIVE}
     * already updates {@code getRemoteAddr()} from {@code X-Forwarded-For}; we still fall back
     * to {@code CF-Connecting-IP} for Cloudflare → direct host scenarios.
     */
    private static String clientIp(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) return cf.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
