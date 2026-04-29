package com.lmp.portal;

import com.lmp.shared.config.site.SiteConfigManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint public exposant la configuration du site au frontend.
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final SiteConfigManager siteConfigManager;

    public ConfigController(SiteConfigManager siteConfigManager) {
        this.siteConfigManager = siteConfigManager;
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public Map<String, String> getConfig() {
        return Map.of(
            "baseUrl", siteConfigManager.getBaseUrl(),
            "frontendUrl", siteConfigManager.getFrontendUrl(),
            "siteName", siteConfigManager.getSiteName(),
            "supportEmail", siteConfigManager.getSupportEmail(),
            "noreplyEmail", siteConfigManager.getNoreplyEmail()
        );
    }
}
