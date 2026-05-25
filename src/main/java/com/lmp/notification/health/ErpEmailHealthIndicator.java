package com.lmp.notification.health;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Spring Boot HealthIndicator exposed at {@code /actuator/health/erpEmail}.
 *
 * <p>Result cached in Redis under {@value #CACHE_KEY} for {@value #CACHE_TTL_SECONDS}s,
 * so N admin tabs across N replicas trigger at most one external Frappe ping per
 * TTL window. Stateless : no per-pod cache, no per-pod cron job — first
 * concurrent caller wins, others read the shared cache.</p>
 */
@Component("erpEmail")
public class ErpEmailHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ErpEmailHealthIndicator.class);
    private static final String CACHE_KEY = "lmp:erpnext-health:cache";
    private static final long CACHE_TTL_SECONDS = 30L;

    private final ExternalSystemClient externalClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper json = new ObjectMapper();

    public ErpEmailHealthIndicator(ExternalSystemClient externalClient, StringRedisTemplate redis) {
        this.externalClient = externalClient;
        this.redis = redis;
    }

    @Override
    public Health health() {
        var cached = readCache();
        if (cached != null) {
            return cached.toHealth();
        }
        var fresh = performCheck();
        writeCache(fresh);
        return fresh.toHealth();
    }

    private CachedHealth readCache() {
        try {
            String raw = redis.opsForValue().get(CACHE_KEY);
            if (raw == null) return null;
            return json.readValue(raw, CachedHealth.class);
        } catch (Exception e) {
            log.debug("Health cache read miss/error: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(CachedHealth value) {
        try {
            redis.opsForValue().set(CACHE_KEY, json.writeValueAsString(value),
                    Duration.ofSeconds(CACHE_TTL_SECONDS));
        } catch (JsonProcessingException e) {
            log.debug("Health cache write skipped: {}", e.getMessage());
        }
    }

    private CachedHealth performCheck() {
        long start = System.currentTimeMillis();
        try {
            ExternalResponse response = externalClient.callMethod("frappe.ping", Map.of());
            long latency = System.currentTimeMillis() - start;
            if (response != null && response.success()) {
                return new CachedHealth("UP", latency, "ERPNext email relay reachable", null);
            }
            String error = response != null ? response.errorMessage() : "null response";
            return new CachedHealth("DOWN", latency, null,
                    "ERPNext email relay unavailable: " + error);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("ERPNext health check failed after {}ms: {}", latency, e.getMessage());
            return new CachedHealth("DOWN", latency, null,
                    "ERPNext email relay unavailable: " + e.getMessage());
        }
    }

    /** Serializable DTO stored in Redis. {@code null} fields are tolerated. */
    public record CachedHealth(
            @JsonProperty("status") String status,
            @JsonProperty("latencyMs") long latencyMs,
            @JsonProperty("message") String message,
            @JsonProperty("error") String error
    ) {
        @JsonCreator
        public CachedHealth {}

        Health toHealth() {
            var builder = "UP".equals(status) ? Health.up() : Health.down();
            builder.withDetail("latencyMs", latencyMs);
            if (message != null) builder.withDetail("message", message);
            if (error != null) builder.withDetail("error", error);
            return builder.build();
        }

        public Status springStatus() {
            return "UP".equals(status) ? Status.UP : Status.DOWN;
        }
    }
}
