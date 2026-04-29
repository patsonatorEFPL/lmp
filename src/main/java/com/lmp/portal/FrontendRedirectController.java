package com.lmp.portal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

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
 * Sur les sous-domaines auth.* (réservés au flux OIDC), les pages
 * marketing retournent 404 — seules /login et /register restent
 * accessibles pour permettre le flux OAuth2 (Spring Auth Server
 * redirige vers /login si l'utilisateur n'est pas authentifié).
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

    /**
     * Pour les pages marketing : si on est sur l'host auth.*, retourner 404.
     * Sinon comportement normal (forward ou redirect).
     */
    private String marketingPage(String path) {
        if (isAuthSubdomain()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return redirectOrForward(path);
    }

    private static boolean isAuthSubdomain() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return false;
            }
            String host = attrs.getRequest().getServerName();
            return host != null && host.startsWith("auth.");
        } catch (Exception e) {
            return false;
        }
    }

    // ===== Pages publiques (404 sur auth.*) =====

    @GetMapping("/")
    public String home() {
        return marketingPage("/");
    }

    @GetMapping("/about")
    public String about() {
        return marketingPage("/about");
    }

    @GetMapping("/services")
    public String services() {
        return marketingPage("/services");
    }

    @GetMapping("/contact")
    public String contact() {
        return marketingPage("/contact");
    }

    @GetMapping("/contact/success")
    public String contactSuccess() {
        return marketingPage("/contact");
    }

    @GetMapping("/map")
    public String map() {
        return marketingPage("/map");
    }

    @GetMapping("/privacy")
    public String privacy() {
        return marketingPage("/privacy");
    }

    @GetMapping("/terms")
    public String terms() {
        return marketingPage("/terms");
    }

    // ===== Pages d'authentification (accessibles même sur auth.*) =====

    @GetMapping("/login")
    public String login() {
        return redirectOrForward("/login");
    }

    @GetMapping("/register")
    public String register() {
        return redirectOrForward("/register");
    }

    // ===== Pages utilisateur (404 sur auth.*) =====

    @GetMapping("/profile")
    public String profile() {
        return marketingPage("/settings");
    }
}
