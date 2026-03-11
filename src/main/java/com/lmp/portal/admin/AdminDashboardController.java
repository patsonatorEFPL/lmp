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

    @GetMapping("/dashboard")
    public String showAdminDashboard() {
        return "redirect:" + frontendUrl + "/admin";
    }

    @GetMapping
    public String adminRoot() {
        return "redirect:" + frontendUrl + "/admin";
    }

    @GetMapping("/statistics")
    public String showStatistics() {
        return "redirect:" + frontendUrl + "/admin";
    }
}
