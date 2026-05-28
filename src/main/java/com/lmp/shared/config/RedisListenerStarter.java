package com.lmp.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Démarre le {@link RedisMessageListenerContainer} (autoStartup=false) APRÈS
 * que le contexte soit complètement levé ({@code ApplicationReadyEvent}),
 * avec retry borné. Découple le pub/sub SSE/dispatcher de la phase
 * {@code finishRefresh} : une indisponibilité Redis transitoire au boot
 * dégrade le realtime au lieu de tuer l'application entière.
 *
 * <p>Une fois démarré avec succès, le container gère lui-même la reconnexion
 * sur coupure via son {@code recoveryInterval} natif. Ce starter ne couvre
 * que le trou de la première connexion.</p>
 *
 * <p>Observabilité (le mode dégradé silencieux serait pire que le crash) :
 * INFO au premier succès, ERROR à l'abandon définitif. {@link
 * RedisListenerHealthIndicator} expose l'état sur {@code /actuator/health}.</p>
 */
@Component
public class RedisListenerStarter {

    private static final Logger log = LoggerFactory.getLogger(RedisListenerStarter.class);

    static final int MAX_ATTEMPTS = 10;
    static final Duration RETRY_DELAY = Duration.ofSeconds(3);

    private final RedisMessageListenerContainer container;
    private final TaskScheduler scheduler;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean gaveUp = new AtomicBoolean(false);
    private final AtomicInteger attempts = new AtomicInteger(0);

    public RedisListenerStarter(RedisMessageListenerContainer container, TaskScheduler scheduler) {
        this.container = container;
        this.scheduler = scheduler;
    }

    /** Schedule le premier essai hors du thread de l'event (ne pas bloquer la chaîne ApplicationReadyEvent). */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        scheduler.schedule(this::tryStart, Instant.now());
    }

    /** Un essai de démarrage. Reschedule lui-même sur échec jusqu'à {@link #MAX_ATTEMPTS}. */
    void tryStart() {
        if (started.get()) {
            return;
        }
        int n = attempts.incrementAndGet();
        try {
            container.start();
            started.set(true);
            log.info("[REDIS-LISTENER] Container started after {} attempt(s) — SSE + dispatcher pub/sub live", n);
        } catch (Exception e) {
            if (n >= MAX_ATTEMPTS) {
                gaveUp.set(true);
                log.error("[REDIS-LISTENER] Gave up after {} attempts — SSE realtime + cross-pod dispatcher sync DISABLED until app restart. Last error: {}",
                        n, e.getMessage());
                return;
            }
            log.warn("[REDIS-LISTENER] Start attempt {}/{} failed, retrying in {}s: {}",
                    n, MAX_ATTEMPTS, RETRY_DELAY.toSeconds(), e.getMessage());
            scheduler.schedule(this::tryStart, Instant.now().plus(RETRY_DELAY));
        }
    }

    public boolean isStarted() {
        return started.get();
    }

    public boolean hasGivenUp() {
        return gaveUp.get();
    }

    public int attempts() {
        return attempts.get();
    }
}
