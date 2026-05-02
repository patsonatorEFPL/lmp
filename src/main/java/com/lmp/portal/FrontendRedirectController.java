package com.lmp.portal;

import com.lmp.shared.web.AuthHostResolver;
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
 * Sur l'host auth (staging/prod uniquement, pas localhost), seules les pages
 * d'authentification sont servies — les pages marketing retournent 404.
 */
@Controller
public class FrontendRedirectController {

    private static final List<String> BACKEND_PREFIXES = List.of(
            "/api/",
            "/actuator/",
            "/stripe/"
    );

    // Sur l'host auth, seuls ces préfixes de chemin sont autorisés.
    private static final List<String> AUTH_HOST_ALLOWED_PREFIXES = List.of(
            "/login",
            "/register",
            "/forgot-password",
            "/reset-password",
            "/verify-email",
            "/resend-verification"
    );

    @Value("${app.frontend.url:${app.base.url:http://localhost:8080}}")
    private String frontendUrl;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    private final AuthHostResolver authHostResolver;

    public FrontendRedirectController(AuthHostResolver authHostResolver) {
        this.authHostResolver = authHostResolver;
    }

    @GetMapping("/")
    public String root(HttpServletRequest request) {
        if (blockedOnAuthHost("/", request)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
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
        if (blockedOnAuthHost(uri, request)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return forwardOrRedirect(uri);
    }

    private String forwardOrRedirect(String path) {
        if (frontendUrl != null && !frontendUrl.isBlank() && !frontendUrl.equals(baseUrl)) {
            return "redirect:" + frontendUrl + path;
        }
        return "forward:/index.html";
    }

    private boolean blockedOnAuthHost(String uri, HttpServletRequest request) {
        if (!authHostResolver.isAuthSubdomainEnabled()) {
            return false;
        }
        if (!authHostResolver.isAuthHost(request.getServerName())) {
            return false;
        }
        for (String prefix : AUTH_HOST_ALLOWED_PREFIXES) {
            if (uri.equals(prefix) || uri.startsWith(prefix + "/")) {
                return false;
            }
        }
        return true;
    }
}
