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
        Map<String, Object> infra = new HashMap<>();
        try {
            HealthComponent health = healthEndpoint.health();
            if (health instanceof CompositeHealth composite) {
                var components = composite.getComponents();
                infra.put("db", extractComponentStatus(components, "db"));
                infra.put("diskSpace", extractComponentStatus(components, "diskSpace"));
                infra.putAll(extractDiskSpaceDetails(components));
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

    /**
     * Extrait les détails d'espace disque depuis le composant Actuator diskSpace.
     * Retourne total, free, threshold en octets et usagePercent.
     */
    private Map<String, Object> extractDiskSpaceDetails(Map<String, HealthComponent> components) {
        Map<String, Object> details = new HashMap<>();
        if (components == null) return details;
        HealthComponent component = components.get("diskSpace");
        if (component instanceof Health h && h.getDetails() != null) {
            var d = h.getDetails();
            if (d.containsKey("total")) details.put("diskTotal", ((Number) d.get("total")).longValue());
            if (d.containsKey("free")) details.put("diskFree", ((Number) d.get("free")).longValue());
            if (d.containsKey("threshold")) details.put("diskThreshold", ((Number) d.get("threshold")).longValue());
            if (d.containsKey("total") && d.containsKey("free")) {
                long total = ((Number) d.get("total")).longValue();
                long free = ((Number) d.get("free")).longValue();
                if (total > 0) {
                    details.put("diskUsagePercent", Math.round((double)(total - free) / total * 1000.0) / 10.0);
                }
            }
        }
        return details;
    }
}
