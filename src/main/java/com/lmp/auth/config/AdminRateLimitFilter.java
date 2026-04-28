package com.lmp.auth.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting pour les endpoints admin {@code /api/v1/admin/**}.
 * <p>
 * 100 requêtes/minute par client (IP + utilisateur authentifié),
 * avec un burst de 20. Si dépassé → 429 Too Many Requests.
 * Les healthchecks admin sont exclus.
 */
@Component
@Order(2)
public class AdminRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminRateLimitFilter.class);

    private static final int LIMIT_PER_MINUTE = 100;
    private static final int BURST = 20;

    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Ne s'applique qu'aux endpoints admin (pas healthchecks)
        if (!path.startsWith("/api/v1/admin/") || path.startsWith("/api/v1/admin/health/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildKey(request);
        AtomicInteger counter = requestCounts.get(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

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
