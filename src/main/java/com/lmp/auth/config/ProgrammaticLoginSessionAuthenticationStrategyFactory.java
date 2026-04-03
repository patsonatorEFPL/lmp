package com.lmp.auth.config;

import java.util.List;

import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

/**
 * Construction unique de la {@link SessionAuthenticationStrategy} utilisée par le login HTTP
 * programmatique ({@code ProgrammaticHttpSessionLogin}).
 * <p>
 * <strong>Couplage :</strong> doit rester équivalent au résultat de
 * {@code HttpSecurity.sessionManagement().maximumSessions(n).maxSessionsPreventsLogin(...).sessionRegistry(...)}
 * sur la chaîne filtre {@code /api/**} (ordre Spring par défaut : contrôle concurrent, fixation
 * {@code changeSessionId}, enregistrement registry). Si vous ajoutez
 * {@code sessionAuthenticationStrategy(...)} ou des stratégies supplémentaires sur cette chaîne,
 * mettez à jour cette factory en conséquence.
 */
final class ProgrammaticLoginSessionAuthenticationStrategyFactory {

    private ProgrammaticLoginSessionAuthenticationStrategyFactory() {}

    static SessionAuthenticationStrategy create(
            SessionRegistry sessionRegistry, int maximumSessionsPerPrincipal, boolean maxSessionsPreventsLogin) {
        ConcurrentSessionControlAuthenticationStrategy concurrent =
                new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry);
        concurrent.setMaximumSessions(maximumSessionsPerPrincipal);
        concurrent.setExceptionIfMaximumExceeded(maxSessionsPreventsLogin);
        return new CompositeSessionAuthenticationStrategy(List.of(
                concurrent,
                new ChangeSessionIdAuthenticationStrategy(),
                new RegisterSessionAuthenticationStrategy(sessionRegistry)));
    }
}
