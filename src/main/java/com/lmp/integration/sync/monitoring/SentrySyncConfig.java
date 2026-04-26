package com.lmp.integration.sync.monitoring;

import com.lmp.integration.sync.SyncProperties;
import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration conditionnelle de Sentry pour le monitoring de synchronisation.
 * <p>
 * Sentry offre un grouping intelligent des erreurs, du rate-limiting adaptatif,
 * et un dashboard riche — supérieur aux emails directs qui subissent l'"alert fatigue".
 * <p>
 * Activé uniquement si {@code lmp.sync.alert.sentry-dsn} est renseigné.
 */
@Configuration
public class SentrySyncConfig {

    private static final Logger log = LoggerFactory.getLogger(SentrySyncConfig.class);

    private final SyncProperties syncProperties;

    public SentrySyncConfig(SyncProperties syncProperties) {
        this.syncProperties = syncProperties;
    }

    @PostConstruct
    public void initSentry() {
        String dsn = syncProperties.getAlert().getSentryDsn();
        if (dsn == null || dsn.isBlank()) {
            log.info("[SENTRY] DSN non configuré — Sentry désactivé pour la synchronisation");
            return;
        }

        try {
            Sentry.init(options -> {
                options.setDsn(dsn);
                options.setEnvironment(syncProperties.getAlert().getSentryEnvironment());
                options.setRelease("lmp@" + getClass().getPackage().getImplementationVersion());
                options.setTracesSampleRate(0.0); // Pas de tracing APM pour le moment
                options.setDebug(false);
            });
            log.info("[SENTRY] Initialisé — env={}, DSN masqué", syncProperties.getAlert().getSentryEnvironment());
        } catch (Exception e) {
            log.error("[SENTRY] Échec de l'initialisation : {}", e.getMessage());
        }
    }

    @PreDestroy
    public void closeSentry() {
        try {
            Sentry.close();
            log.debug("[SENTRY] Client fermé");
        } catch (Exception ignored) {
            // ignore
        }
    }
}
