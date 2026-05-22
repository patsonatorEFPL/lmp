package com.lmp.shared.config;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Initialise Sentry le plus tôt possible dans le cycle de démarrage Spring Boot
 * afin de capturer les erreurs survenant avant le chargement complet du contexte
 * (erreurs de configuration, de connexion DB, de validation de beans, etc.).
 */
public class SentryBootstrapListener implements SpringApplicationRunListener {

    private static final Logger log = LoggerFactory.getLogger(SentryBootstrapListener.class);

    public SentryBootstrapListener(SpringApplication application, String[] args) {
        // Constructeur requis par SpringApplicationRunListener
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext,
                                     ConfigurableEnvironment environment) {
        String dsn = environment.getProperty("lmp.sync.alert.sentry-dsn");
        if (dsn == null || dsn.isBlank()) {
            log.debug("[SENTRY] DSN non configuré — initialisation bootstrap sautée");
            return;
        }

        try {
            Sentry.init(options -> {
                options.setDsn(dsn);
                options.setEnvironment(environment.getProperty(
                        "lmp.sync.alert.sentry-environment", "production"));
                options.setRelease("lmp@" + environment.getProperty("app.version", "unknown"));
                options.setTracesSampleRate(0.0);
                options.setDebug(false);
            });
            log.info("[SENTRY] Initialisé en bootstrap — env={}",
                    environment.getProperty("lmp.sync.alert.sentry-environment", "production"));
        } catch (Exception e) {
            log.error("[SENTRY] Échec de l'initialisation bootstrap : {}", e.getMessage());
        }
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        if (!Sentry.isEnabled()) {
            log.debug("[SENTRY] Client non initialisé — impossible de rapporter l'erreur de démarrage");
            return;
        }
        log.error("[SENTRY] Application failed to start — capturing exception", exception);
        Sentry.captureException(exception);
        try {
            Sentry.flush(5000);
        } catch (Exception ignored) {
            // ignore
        }
    }
}
