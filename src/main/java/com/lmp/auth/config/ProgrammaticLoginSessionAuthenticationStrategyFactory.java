package com.lmp.auth.config;

import java.util.List;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;

/**
 * Construction unique de la {@link SessionAuthenticationStrategy} utilisée par le login HTTP
 * programmatique ({@code ProgrammaticHttpSessionLogin}).
 * <p>
 * <strong>Couplage :</strong> doit rester équivalent au résultat de
 * {@code HttpSecurity.sessionManagement().maximumSessions(n).maxSessionsPreventsLogin(...).sessionRegistry(...)}
 * sur la chaîne filtre {@code /api/**} (contrôle concurrent, fixation, enregistrement registry).
 * <p>
 * <strong>Fixation strategy :</strong> {@link SessionFixationProtectionStrategy} avec
 * {@code migrateSessionAttributes=true}. Le default Spring Security
 * {@code ChangeSessionIdAuthenticationStrategy} appelle {@code HttpServletRequest.changeSessionId()}
 * dont l'implémentation Spring Session 4.x Redis perd les attributs custom non-Security
 * (notamment SPRING_SECURITY_SAVED_REQUEST) lors de la rotation → POST /api/v1/auth/login depuis
 * /login retourne {@code redirectUrl=/admin} au lieu de l'URL /oauth2/authorize originale.
 * MigrateSession copie explicitement chaque attribut via Enumeration → fixation-safe + préserve
 * la saved request. Cf. {@code SecurityConfig.sessionManagement.sessionFixation(migrateSession)}.
 */
final class ProgrammaticLoginSessionAuthenticationStrategyFactory {

    private ProgrammaticLoginSessionAuthenticationStrategyFactory() {}

    static SessionAuthenticationStrategy create(
            SessionRegistry sessionRegistry, int maximumSessionsPerPrincipal, boolean maxSessionsPreventsLogin) {
        ConcurrentSessionControlAuthenticationStrategy concurrent =
                new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry);
        concurrent.setMaximumSessions(maximumSessionsPerPrincipal);
        concurrent.setExceptionIfMaximumExceeded(maxSessionsPreventsLogin);

        SessionFixationProtectionStrategy fixation = new SessionFixationProtectionStrategy();
        fixation.setMigrateSessionAttributes(true);

        return new CompositeSessionAuthenticationStrategy(List.of(
                concurrent,
                fixation,
                new RegisterSessionAuthenticationStrategy(sessionRegistry)));
    }
}
