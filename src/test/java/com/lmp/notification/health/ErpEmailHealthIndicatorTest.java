package com.lmp.notification.health;

import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpEmailHealthIndicatorTest {

    @Mock
    private ExternalSystemClient externalClient;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOps;

    private ErpEmailHealthIndicator indicator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // Default : Redis empty so the indicator falls through to performCheck().
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(any(String.class))).thenReturn(null);
        indicator = new ErpEmailHealthIndicator(externalClient, redis);
    }

    @Test
    void healthUpWhenErpnextRespondsSuccessfully() {
        when(externalClient.callMethod(eq("frappe.ping"), any(Map.class)))
                .thenReturn(ExternalResponse.success(null, Map.of("message", "pong")));

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.UP);
        assertThat(result.getDetails()).containsKey("latencyMs");
        assertThat(result.getDetails().get("message")).asString().contains("reachable");
    }

    @Test
    void healthDownWhenErpnextReturnsFailure() {
        when(externalClient.callMethod(eq("frappe.ping"), any(Map.class)))
                .thenReturn(ExternalResponse.failure("Connection timeout", 503));

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.DOWN);
        assertThat(result.getDetails().get("error")).asString().contains("Connection timeout");
    }

    @Test
    void healthDownWhenErpnextThrowsException() {
        when(externalClient.callMethod(eq("frappe.ping"), any(Map.class)))
                .thenThrow(new RuntimeException("Network unreachable"));

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.DOWN);
        assertThat(result.getDetails().get("error")).asString().contains("Network unreachable");
    }

    @Test
    void healthDownWhenNoOpClientAlwaysUnavailable() {
        when(externalClient.callMethod(eq("frappe.ping"), any(Map.class)))
                .thenReturn(ExternalResponse.unavailable());

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void usesCachedValueOnSecondCall() {
        when(externalClient.callMethod(eq("frappe.ping"), any(Map.class)))
                .thenReturn(ExternalResponse.success(null, Map.of("message", "pong")));

        // First call : cache miss → triggers performCheck + writeCache.
        Health first = indicator.health();
        assertThat(first.getStatus()).isEqualTo(Status.UP);

        // Simulate Redis SETEX persistence : next get() returns the JSON the indicator
        // would have written.
        String cachedJson = "{\"status\":\"UP\",\"latencyMs\":1,\"message\":\"ERPNext email relay reachable\",\"error\":null}";
        when(valueOps.get(any(String.class))).thenReturn(cachedJson);

        Health second = indicator.health();
        assertThat(second.getStatus()).isEqualTo(Status.UP);

        // External client called exactly once across the two health() calls.
        verify(externalClient, times(1)).callMethod(eq("frappe.ping"), any(Map.class));
        verify(valueOps).set(any(String.class), any(String.class), eq(Duration.ofSeconds(30)));
    }
}
