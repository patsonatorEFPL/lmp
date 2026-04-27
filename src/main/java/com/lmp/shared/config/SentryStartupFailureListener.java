package com.lmp.shared.config;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;

/**
 * Capture les erreurs fatales de démarrage Spring Boot et les envoie à Sentry.
 * <p>
 * Spring publie {@link ApplicationFailedEvent} quand le contexte échoue à se charger.
 * Ce listener s'assure que l'exception est capturée même si l'application ne démarre pas.
 */
public class SentryStartupFailureListener implements ApplicationListener<ApplicationFailedEvent> {

    private static final Logger log = LoggerFactory.getLogger(SentryStartupFailureListener.class);

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        if (Sentry.getCurrentHub().getClient() == null) {
            log.debug("[SENTRY] Client non initialisé — impossible de rapporter l'erreur de démarrage");
            return;
        }

        Throwable exception = event.getException();
        log.error("[SENTRY] Application failed to start — capturing exception", exception);

        Sentry.captureException(exception);

        // Forcer le flush avant que le processus ne meure
        try {
            Sentry.flush(5000);
        } catch (Exception ignored) {
            // ignore
        }
    }
}
