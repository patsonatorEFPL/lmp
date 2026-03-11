package com.lmp.portal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur de redirection vers le frontend Angular.
 * 
 * Remplace les anciennes pages Thymeleaf en redirigeant toutes les requêtes
 * de navigation vers l'application Angular. Les endpoints API REST (/api/**)
 * ne sont pas affectés par ce contrôleur.
 */
@Controller
public class FrontendRedirectController {

    @Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

    // ===== Pages publiques =====

    @GetMapping("/")
    public String home() {
        return "redirect:" + frontendUrl;
    }

    @GetMapping("/about")
    public String about() {
        return "redirect:" + frontendUrl + "/about";
    }

    @GetMapping("/services")
    public String services() {
        return "redirect:" + frontendUrl + "/services";
    }

    @GetMapping("/contact")
    public String contact() {
        return "redirect:" + frontendUrl + "/contact";
    }

    @GetMapping("/contact/success")
    public String contactSuccess() {
        return "redirect:" + frontendUrl + "/contact";
    }

    @GetMapping("/map")
    public String map() {
        return "redirect:" + frontendUrl + "/map";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "redirect:" + frontendUrl + "/privacy";
    }

    @GetMapping("/terms")
    public String terms() {
        return "redirect:" + frontendUrl + "/terms";
    }

    // ===== Pages d'authentification =====

    @GetMapping("/login")
    public String login() {
        return "redirect:" + frontendUrl + "/login";
    }

    @GetMapping("/register")
    public String register() {
        return "redirect:" + frontendUrl + "/register";
    }

    // ===== Pages utilisateur =====

    @GetMapping("/profile")
    public String profile() {
        return "redirect:" + frontendUrl + "/settings";
    }
}
