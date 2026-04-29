package com.lmp.auth.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sert la page de login backend pour le flow OAuth2 / SSO.
 * Nécessaire car les ressources statiques Spring Boot ne servent pas
 * les fichiers .html sans extension (ex: /backend-login -> backend-login.html).
 */
@Controller
public class BackendLoginController {

    @GetMapping("/backend-login")
    public String backendLogin() {
        return "forward:/backend-login.html";
    }
}
