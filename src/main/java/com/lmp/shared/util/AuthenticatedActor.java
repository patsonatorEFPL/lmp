package com.lmp.shared.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Résolution de l'acteur courant pour les champs d'audit (processedBy, changedBy…).
 */
public final class AuthenticatedActor {

    public static final String SYSTEM = "SYSTEM";

    private AuthenticatedActor() {
    }

    /**
     * Nom du principal authentifié (email), ou {@value #SYSTEM} hors contexte
     * authentifié (jobs, webhooks, listeners async).
     */
    public static String nameOrSystem() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return SYSTEM;
        }
        return auth.getName();
    }
}
