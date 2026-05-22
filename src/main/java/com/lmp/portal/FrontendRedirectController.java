package com.lmp.portal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Sert l'application Angular SPA en mode monolithique (forward → index.html)
 * ou redirige vers le dev server Angular en mode split.
 *
 * Pattern JHipster : capture tout chemin sans point dans le dernier segment
 * ({@code /{path:[^\\.]*}}) — les assets statiques (*.js, *.css…) contiennent
 * un point et sont servis par le resource handler de Spring Boot avant d'atteindre
 * ce contrôleur.
 *
 * Mode single-host : un seul domaine par environnement (dev = dev.lmp-services.ca,
 * prod = lmp-services.ca). Pas de séparation auth/marketing par sous-domaine.
 * Les pages d'auth (/login, /register…) sont servies sur le même host que le
 * site marketing — Angular gère le routage côté client.
 */
@Controller
public class FrontendRedirectController {

    private static final List<String> BACKEND_PREFIXES = List.of(
            "/api/",
            "/actuator/",
            "/stripe/"
    );

    @Value("${app.frontend.url:${app.base.url:http://localhost:8080}}")
    private String frontendUrl;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    @GetMapping("/")
    public String root() {
        return forwardOrRedirect("/");
    }

    @GetMapping({"/{path:[^\\.]*}", "/**/{path:[^\\.]*}"})
    public String forward(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : BACKEND_PREFIXES) {
            if (uri.startsWith(prefix)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        return forwardOrRedirect(uri);
    }

    private String forwardOrRedirect(String path) {
        if (frontendUrl != null && !frontendUrl.isBlank() && !frontendUrl.equals(baseUrl)) {
            return "redirect:" + frontendUrl + path;
        }
        return "forward:/index.html";
    }
}
