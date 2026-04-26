package com.lmp.integration.sync.monitoring;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoint REST pour l'état de santé de la synchronisation ERP.
 * <p>
 * Consommé par le frontend AdminMonitoringComponent et les outils
 * de surveillance externes (UptimeRobot, etc.).
 */
@RestController
@RequestMapping("/api/v1/admin/sync-health")
@PreAuthorize("hasRole('ADMIN')")
public class SyncHealthController {

    private final SyncHealthSnapshotRepository snapshotRepository;
    private final SyncErrorPatternRepository patternRepository;

    public SyncHealthController(SyncHealthSnapshotRepository snapshotRepository,
                                SyncErrorPatternRepository patternRepository) {
        this.snapshotRepository = snapshotRepository;
        this.patternRepository = patternRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSyncHealth() {
        Optional<SyncHealthSnapshot> latestOpt = snapshotRepository.findTopByOrderByRecordedAtDesc();

        Map<String, Object> response = new HashMap<>();

        if (latestOpt.isPresent()) {
            SyncHealthSnapshot s = latestOpt.get();
            response.put("status", s.getOverallStatus().name());
            response.put("lastCheck", s.getRecordedAt().toString());
            response.put("metrics", Map.of(
                    "queueDepth", s.getQueueDepth(),
                    "deadLast24h", s.getDeadCount(),
                    "failedStale", s.getFailedStaleCount(),
                    "unverifiedStale", s.getUnverifiedCount(),
                    "erpAvailable", s.isErpAvailable(),
                    "successRate24h", s.getSuccessRate24h()
            ));
        } else {
            response.put("status", "UNKNOWN");
            response.put("lastCheck", null);
            response.put("metrics", Map.of(
                    "queueDepth", 0,
                    "deadLast24h", 0,
                    "failedStale", 0,
                    "unverifiedStale", 0,
                    "erpAvailable", false,
                    "successRate24h", null
            ));
        }

        long unknownPatterns = patternRepository.countByCategoryAndAlerted(
                SyncErrorPattern.Category.UNKNOWN, false);
        response.put("unknownPatterns", unknownPatterns);

        return ResponseEntity.ok(response);
    }
}
