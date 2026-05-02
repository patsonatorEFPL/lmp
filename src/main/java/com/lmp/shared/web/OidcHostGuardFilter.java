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

    // Endpoints OIDC purs (Spring Authorization Server) — préfixes à bloquer
    // hors host auth. Match par préfixe (path commence par X).
    private static final List<String> OIDC_PATH_PREFIXES = List.of(
            "/oauth2/",
            "/.well-known/openid-configuration",
            "/.well-known/oauth-authorization-server",
            "/.well-known/jwks.json",
            "/userinfo",
            "/connect/",
            "/login/oauth2/"
    );

    // Pages d'authentification — accessibles UNIQUEMENT sur host auth.
    // Match exact OU début "/path/..." (pas startsWith greedy qui matcherait "/login123").
    private static final List<String> AUTH_EXACT_PATHS = List.of(
            "/login",
            "/register",
            "/perform-login",
            "/perform-logout",
            "/logout",
            "/forgot-password",
            "/reset-password",
            "/verify-email",
            "/resend-verification"
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
        if (shouldBlock(path) && authHostResolver.isAuthSubdomainEnabled()
                && !authHostResolver.isAuthHost(request.getServerName())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldBlock(String path) {
        if (path == null) {
            return false;
        }
        // Préfixes OIDC : startsWith
        for (String prefix : OIDC_PATH_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix)) {
                return true;
            }
        }
        // Paths exact : equals OU "/path/..." (pas "/path123")
        for (String authPath : AUTH_EXACT_PATHS) {
            if (path.equals(authPath) || path.startsWith(authPath + "/")) {
                return true;
            }
        }
        return false;
    }
}
