package com.lmp.portal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur de redirection vers le frontend Angular.
 *
 * En mode développement (app.frontend.url renseigné) :
 *   Redirige les requêtes de navigation vers l'application Angular
 *   externe (ex: http://localhost:4200).
 *
 * En mode production monolithique (app.frontend.url vide) :
 *   Fait un forward vers /index.html pour que Spring Boot serve
 *   directement le frontend embarqué dans le JAR.
 *
 * Les endpoints API REST (/api/**), OAuth2 (/oauth2/**), webhooks
 * et autres endpoints backend ne sont pas affectés par ce contrôleur.
 */
@Controller
public class FrontendRedirectController {

    /**
     * URL du frontend externe. Si vide ou non renseignée, le backend
     * sert directement le frontend embarqué (mode monolithique).
     */
    @Value("${app.frontend.url:}")
    private String frontendUrl;

    private String redirectOrForward(String path) {
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            return "redirect:" + frontendUrl + path;
        }
        return "forward:/index.html";
    }

    // ===== Pages publiques =====

    @GetMapping("/")
    public String home() {
        return redirectOrForward("/");
    }

    @GetMapping("/about")
    public String about() {
        return redirectOrForward("/about");
    }

    @GetMapping("/services")
    public String services() {
        return redirectOrForward("/services");
    }

    @GetMapping("/contact")
    public String contact() {
        return redirectOrForward("/contact");
    }

    @GetMapping("/contact/success")
    public String contactSuccess() {
        return redirectOrForward("/contact");
    }

    @GetMapping("/map")
    public String map() {
        return redirectOrForward("/map");
    }

    @GetMapping("/privacy")
    public String privacy() {
        return redirectOrForward("/privacy");
    }

    @GetMapping("/terms")
    public String terms() {
        return redirectOrForward("/terms");
    }

    // ===== Pages d'authentification =====

    @GetMapping("/login")
    public String login() {
        return redirectOrForward("/login");
    }

    @GetMapping("/register")
    public String register() {
        return redirectOrForward("/register");
    }

    // ===== Pages utilisateur =====

    @GetMapping("/profile")
    public String profile() {
        return redirectOrForward("/settings");
    }
}
