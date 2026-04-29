package com.lmp.portal.admin;

import com.lmp.shared.config.site.SiteConfigManager;
import com.lmp.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API d'administration de la configuration du site.
 * Permet de modifier la config à chaud sans redémarrage.
 */
@RestController
@RequestMapping("/api/v1/admin/site-config")
@PreAuthorize("hasRole('ADMIN')")
public class SiteConfigAdminController {

    private final SiteConfigManager siteConfigManager;

    public SiteConfigAdminController(SiteConfigManager siteConfigManager) {
        this.siteConfigManager = siteConfigManager;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(siteConfigManager.getAllFromDb()));
    }

    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable String key,
            @RequestBody UpdateSiteConfigRequest request) {
        siteConfigManager.update(key, request.value(), request.description());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/reload-file")
    public ResponseEntity<ApiResponse<Void>> reloadFile() {
        siteConfigManager.reloadFileConfig();
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/reload-all")
    public ResponseEntity<ApiResponse<Void>> reloadAll() {
        siteConfigManager.reloadAll();
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record UpdateSiteConfigRequest(String value, String description) {}
}
