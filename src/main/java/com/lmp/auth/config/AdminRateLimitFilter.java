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

/**
 * Rate limiting pour les endpoints admin {@code /api/v1/admin/**} via Redis
 * (partagé entre replicas). 100 req/min par client (IP + user authentifié),
 * burst à 20.
 *
 * <p>Migration depuis Caffeine in-memory : avec N replicas, l'ancien filtre
 * permettait N × 100 req/min effective (un compteur par replica). Le INCR
 * Redis avec EXPIRE 60s donne un compteur global → limite réelle 100/min
 * peu importe le nombre de replicas / sur quel replica le LB Traefik
 * route la requête.</p>
 *
 * <p>Pattern Redis : {@code rate:admin:<ip>[:<user>]} → INCR + EXPIRE 60s
 * sur la première incrémentation. TTL natif évite le besoin de cleanup.</p>
 */
@Component
@Order(2)
public class AdminRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminRateLimitFilter.class);

    private static final int LIMIT_PER_MINUTE = 100;
    private static final int BURST = 20;
    private static final String KEY_PREFIX = "rate:admin:";
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;

    /**
     * SECURITY (M6) : comportement quand Redis est down.
     * {@code false} (default) = fail-open historique (laisse passer, log warn).
     * {@code true} = fail-closed (503), à activer en prod après vérification
     * que la dispo Redis (≥99.9%) ne génère pas de blackouts admin.
     * Override via env {@code LMP_RATE_LIMIT_FAIL_CLOSED} (mapping
     * Spring : {@code lmp.rate-limit.fail-closed}).
     */
    @Value("${lmp.rate-limit.fail-closed:false}")
    private boolean failClosedOnRedisDown;

    public AdminRateLimitFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith("/api/v1/admin/") || path.startsWith("/api/v1/admin/health/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = KEY_PREFIX + buildKey(request);
        long current;
        try {
            Long incr = redis.opsForValue().increment(key);
            current = incr != null ? incr : 0L;
            if (current == 1L) {
                redis.expire(key, WINDOW);
            }
        } catch (RuntimeException e) {
            // SECURITY (M6) : Redis down — comportement configurable.
            // Default = fail-open (compat historique, évite blackout admin si Redis flappe).
            // Prod recommandée = fail-closed via LMP_RATE_LIMIT_FAIL_CLOSED=true
            // pour empêcher attaquant de désactiver le rate-limit en down-ant Redis.
            if (failClosedOnRedisDown) {
                log.error("[RATE-LIMIT] Redis indisponible — fail-CLOSED (503): {}", e.getMessage());
                response.setStatus(503);
                response.setContentType("application/json");
                response.setHeader("Retry-After", "30");
                response.getWriter().write("{\"error\":\"SERVICE_UNAVAILABLE\",\"message\":\"Rate-limit backend indisponible. Retry après 30s.\",\"status\":503}");
                return;
            }
            log.warn("[RATE-LIMIT] Redis indisponible — fail-open: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (current > LIMIT_PER_MINUTE) {
            log.warn("⛔ [RATE-LIMIT] Admin endpoint blocked for {} ({} requests/min)", key, current);
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Rate limit exceeded. Retry after 60s.\",\"status\":429}");
            return;
        }

        if (current == BURST + 1) {
            log.warn("⚠️ [RATE-LIMIT] Admin endpoint burst threshold reached for {} ({} req/min)", key, current);
        }

        filterChain.doFilter(request, response);
    }

    private String buildKey(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String user = request.getRemoteUser();
        if (user != null && !user.isBlank()) {
            return ip + ":" + user;
        }
        return ip;
    }
}
