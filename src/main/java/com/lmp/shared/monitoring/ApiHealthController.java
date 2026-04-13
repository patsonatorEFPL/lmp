package com.lmp.shared.monitoring;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lmp.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoint admin pour le monitoring des APIs externes.
 *
 * <p>Retourne un snapshot agrégé des métriques de santé de toutes les APIs
 * intégrées, ainsi que l'état de l'infrastructure (DB, disque) via Actuator.
 */
@RestController
@RequestMapping("/api/v1/admin/api-health")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – Monitoring", description = "API health monitoring")
public class ApiHealthController {

    private static final Logger logger = LoggerFactory.getLogger(ApiHealthController.class);

    private final ApiHealthRecorder recorder;
    private final HealthEndpoint healthEndpoint;
    private final ApiHealthProbeService probeService;

    public ApiHealthController(ApiHealthRecorder recorder,
                               HealthEndpoint healthEndpoint,
                               ApiHealthProbeService probeService) {
        this.recorder = recorder;
        this.healthEndpoint = healthEndpoint;
        this.probeService = probeService;
    }

    @GetMapping
    @Operation(summary = "Snapshot de santé de toutes les APIs externes + infrastructure")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthSnapshot() {
        Map<String, ApiHealthEntry> apis = recorder.getSnapshot();

        // Infrastructure via Actuator
        Map<String, String> infra = new HashMap<>();
        try {
            HealthComponent health = healthEndpoint.health();
            if (health instanceof CompositeHealth composite) {
                var components = composite.getComponents();
                infra.put("db", extractComponentStatus(components, "db"));
                infra.put("diskSpace", extractComponentStatus(components, "diskSpace"));
            } else if (health instanceof Health h) {
                infra.put("db", h.getStatus().getCode());
                infra.put("diskSpace", "UNKNOWN");
            } else {
                infra.put("db", "UNKNOWN");
                infra.put("diskSpace", "UNKNOWN");
            }
        } catch (Exception e) {
            logger.warn("[API-HEALTH] Could not read Actuator health: {}", e.getMessage());
            infra.put("db", "UNKNOWN");
            infra.put("diskSpace", "UNKNOWN");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("apis", List.copyOf(apis.values()));
        payload.put("infra", infra);
        payload.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.ok(payload));
    }

    @PostMapping("/probe")
    @Operation(summary = "Lance un probe léger vers toutes les APIs externes")
    public ResponseEntity<ApiResponse<Map<String, String>>> probeAll() {
        Map<String, String> results = probeService.probeAll();
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    private String extractComponentStatus(Map<String, HealthComponent> components, String key) {
        if (components == null) return "UNKNOWN";
        HealthComponent component = components.get(key);
        if (component instanceof Health h) {
            return h.getStatus().getCode();
        }
        if (component instanceof CompositeHealth c) {
            return c.getStatus().getCode();
        }
        return "UNKNOWN";
    }
}
