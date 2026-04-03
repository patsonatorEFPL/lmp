package com.lmp.auth.web.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

/** Session HTTP après authentification programmatique (même stratégie que la chaîne API). */
@Component
public class ProgrammaticHttpSessionLogin {

    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public ProgrammaticHttpSessionLogin(
            @Qualifier("programmaticLoginSessionAuthenticationStrategy")
                    SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    public void login(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = request.getSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    /**
     * Annule une session ouverte par {@link #login} (registry + invalidation + contexte thread-local).
     * À appeler si le login programmatique doit être défait (ex. utilisateur introuvable après auth).
     */
    public void revokeHttpSessionLogin(HttpServletRequest request, SessionRegistry sessionRegistry) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionRegistry.removeSessionInformation(session.getId());
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
