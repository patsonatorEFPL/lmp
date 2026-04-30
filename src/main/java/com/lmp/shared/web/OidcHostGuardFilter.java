package com.lmp.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Bloque les endpoints OIDC sur tout host autre que l'host auth configuré.
 *
 * <p>Avec un seul backend qui sert plusieurs domaines (mode monolithique),
 * Spring Authorization Server expose ses endpoints sur tous les hosts.
 * Cela duplique la surface OIDC sur {@code dev.lmp-services.ca},
 * {@code lmp-services.ca}, etc. — alors que seul {@code auth.<host>}
 * doit y répondre.</p>
 *
 * <p>Ce filtre court-circuite la chaîne avec un 404 quand un path OIDC
 * est appelé hors de l'host auth, AVANT que Spring Security ne le traite.
 * Enregistré avec {@code Ordered.HIGHEST_PRECEDENCE} via
 * {@link OidcHostGuardFilterConfig}.</p>
 */
public class OidcHostGuardFilter extends OncePerRequestFilter {

    // Préfixes/paths gérés par Spring Authorization Server qu'on doit
    // restreindre à l'host auth.
    private static final List<String> OIDC_PATH_PREFIXES = List.of(
            "/oauth2/",
            "/.well-known/openid-configuration",
            "/.well-known/oauth-authorization-server",
            "/.well-known/jwks.json",
            "/userinfo",
            "/connect/",
            "/login/oauth2/"
    );

    private final AuthHostResolver authHostResolver;

    public OidcHostGuardFilter(AuthHostResolver authHostResolver) {
        this.authHostResolver = authHostResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isOidcPath(path) && !authHostResolver.isAuthHost(request.getServerName())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isOidcPath(String path) {
        if (path == null) {
            return false;
        }
        for (String prefix : OIDC_PATH_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
