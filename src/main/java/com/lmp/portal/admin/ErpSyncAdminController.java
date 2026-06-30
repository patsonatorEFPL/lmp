package com.lmp.portal.admin;

import com.lmp.integration.sync.SyncProperties;
import com.lmp.integration.sync.service.SyncOutboundService;
import com.lmp.shared.config.site.SiteConfigManager;
import com.lmp.shared.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Toggle runtime de la synchronisation ERP (LMP → Frappe).
 * OFF ⇒ SyncOutboundService.enqueue() n'écrit plus rien (ni sync_event_log,
 * ni dispatch Frappe). Usage principal : couper la sync pendant les benchs.
 * Spec : docs/superpowers/specs/2026-05-29-erp-sync-toggle-design.md
 */
@RestController
@RequestMapping("/api/v1/admin/integrations/erp-sync")
@PreAuthorize("hasRole('ADMIN')")
public class ErpSyncAdminController {

    private static final Logger log = LoggerFactory.getLogger(ErpSyncAdminController.class);

    private final SiteConfigManager siteConfigManager;
    private final SyncProperties syncProperties;

    public ErpSyncAdminController(SiteConfigManager siteConfigManager, SyncProperties syncProperties) {
        this.siteConfigManager = siteConfigManager;
        this.syncProperties = syncProperties;
    }

    /** source : "config" = valeur présente (env/file/DB), "default" = clé absente ⇒ fail-open true. */
    public record ErpSyncStatus(boolean enabled, boolean staticEnabled, String source) {}
    public record UpdateErpSyncRequest(Boolean enabled) {}

    @GetMapping
    public ResponseEntity<ApiResponse<ErpSyncStatus>> get() {
        return ResponseEntity.ok(ApiResponse.ok(currentStatus()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ErpSyncStatus>> update(
            @RequestBody UpdateErpSyncRequest request, Authentication authentication) {
        if (request.enabled() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Le champ 'enabled' est obligatoire"));
        }
        boolean previous = siteConfigManager.getBoolean(SyncOutboundService.RUNTIME_ENABLED_KEY, true);
        siteConfigManager.update(SyncOutboundService.RUNTIME_ENABLED_KEY,
                String.valueOf(request.enabled()), "ERP sync runtime toggle");
        log.info("[ERP-SYNC] Toggle runtime {} -> {} par {}", previous, request.enabled(),
                authentication != null ? authentication.getName() : "inconnu");
        return ResponseEntity.ok(ApiResponse.ok(currentStatus()));
    }

    private ErpSyncStatus currentStatus() {
        boolean enabled = siteConfigManager.getBoolean(SyncOutboundService.RUNTIME_ENABLED_KEY, true);
        String raw = siteConfigManager.getString(SyncOutboundService.RUNTIME_ENABLED_KEY);
        return new ErpSyncStatus(enabled, syncProperties.isEnabled(), raw == null ? "default" : "config");
    }
}
