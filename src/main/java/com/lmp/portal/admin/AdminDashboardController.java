package com.lmp.portal.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Contrôleur pour le tableau de bord administrateur.
 * Redirige vers le frontend Angular. Les API REST admin sont dans les contrôleurs dédiés.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    @Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    private boolean isMonolithicMode() {
        return frontendUrl == null || frontendUrl.isBlank() || frontendUrl.equals(baseUrl);
    }

    @GetMapping("/dashboard")
    public String showAdminDashboard() {
        if (isMonolithicMode()) return "forward:/index.html";
        return "redirect:" + frontendUrl + "/admin";
    }

    @GetMapping
    public String adminRoot() {
        if (isMonolithicMode()) return "forward:/index.html";
        return "redirect:" + frontendUrl + "/admin";
    }

    @GetMapping("/statistics")
    public String showStatistics() {
        if (isMonolithicMode()) return "forward:/index.html";
        return "redirect:" + frontendUrl + "/admin";
    }
}
