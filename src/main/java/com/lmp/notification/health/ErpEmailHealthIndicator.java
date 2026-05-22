package com.lmp.notification.health;

import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring Boot HealthIndicator exposed at {@code /actuator/health/erpEmail}.
 * Caches the last result for {@value #CACHE_TTL_SECONDS}s to shield ERPNext from
 * thundering-herd polling by multiple admin tabs.
 */
@Component("erpEmail")
public class ErpEmailHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ErpEmailHealthIndicator.class);
    private static final long CACHE_TTL_SECONDS = 30;

    private final ExternalSystemClient externalClient;
    private final AtomicReference<CachedHealth> cache = new AtomicReference<>();

    public ErpEmailHealthIndicator(ExternalSystemClient externalClient) {
        this.externalClient = externalClient;
    }

    @Override
    public Health health() {
        CachedHealth cached = cache.get();
        if (cached != null && Duration.between(cached.at, Instant.now()).getSeconds() < CACHE_TTL_SECONDS) {
            return cached.health;
        }
        Health fresh = performCheck();
        cache.set(new CachedHealth(fresh, Instant.now()));
        return fresh;
    }

    private Health performCheck() {
        long start = System.currentTimeMillis();
        try {
            ExternalResponse response = externalClient.callMethod("frappe.ping", Map.of());
            long latency = System.currentTimeMillis() - start;

            if (response != null && response.success()) {
                return Health.up()
                        .withDetail("latencyMs", latency)
                        .withDetail("message", "ERPNext email relay reachable")
                        .build();
            }

            String error = response != null ? response.errorMessage() : "null response";
            return Health.down()
                    .withDetail("latencyMs", latency)
                    .withDetail("error", "ERPNext email relay unavailable: " + error)
                    .build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("ERPNext health check failed after {}ms: {}", latency, e.getMessage());
            return Health.down()
                    .withDetail("latencyMs", latency)
                    .withDetail("error", "ERPNext email relay unavailable: " + e.getMessage())
                    .build();
        }
    }

    private record CachedHealth(Health health, Instant at) {}
}
