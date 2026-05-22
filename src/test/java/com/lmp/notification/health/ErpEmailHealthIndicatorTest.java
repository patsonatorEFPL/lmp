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

    private ErpEmailHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new ErpEmailHealthIndicator(externalClient);
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
    void cachesResultBetweenCallsWithinTtl() {
        when(externalClient.callMethod(eq("frappe.ping"), any(Map.class)))
                .thenReturn(ExternalResponse.success(null, Map.of("message", "pong")));

        Health first = indicator.health();
        Health second = indicator.health();

        assertThat(first.getStatus()).isEqualTo(Status.UP);
        assertThat(second.getStatus()).isEqualTo(Status.UP);
        verify(externalClient, times(1)).callMethod(eq("frappe.ping"), any(Map.class));
    }
}
