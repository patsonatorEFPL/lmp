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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Démarre TOUS les {@link RedisMessageListenerContainer} (rendus
 * {@code autoStartup=false} par {@link RedisListenerDeferralPostProcessor}) APRÈS
 * que le contexte soit complètement levé ({@code ApplicationReadyEvent}), avec
 * retry borné. Découple le pub/sub Redis (SSE, dispatcher, expiration de session
 * indexée Spring Session) de la phase {@code finishRefresh} : une indisponibilité
 * Redis transitoire au boot dégrade le realtime au lieu de tuer l'application.
 *
 * <p>Une fois démarré, chaque container gère lui-même la reconnexion sur coupure
 * via son {@code recoveryInterval} natif. Ce starter ne couvre que le trou de la
 * première connexion (fenêtre où la résolution DNS de redis-cache n'est pas encore
 * prête pour le container fraîchement démarré).</p>
 *
 * <p>Observabilité (le mode dégradé silencieux serait pire que le crash) : INFO au
 * premier succès de chaque container, ERROR à l'abandon définitif.
 * {@link RedisListenerHealthIndicator} expose l'état sur {@code /actuator/health}.</p>
 */
@Component
public class RedisListenerStarter {

    private static final Logger log = LoggerFactory.getLogger(RedisListenerStarter.class);

    static final int MAX_ATTEMPTS = 10;
    static final Duration RETRY_DELAY = Duration.ofSeconds(3);

    private final Map<String, RedisMessageListenerContainer> containers;
    private final TaskScheduler scheduler;

    private final Set<String> startedNames = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean gaveUp = new AtomicBoolean(false);
    private final AtomicInteger attempts = new AtomicInteger(0);

    public RedisListenerStarter(Map<String, RedisMessageListenerContainer> containers,
                                TaskScheduler scheduler) {
        this.containers = containers;
        this.scheduler = scheduler;
    }

    /** Schedule le premier essai hors du thread de l'event (ne pas bloquer la chaîne ApplicationReadyEvent). */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        scheduler.schedule(this::tryStart, Instant.now());
    }

    /** Un essai de démarrage de tous les containers non encore démarrés. Reschedule sur échec jusqu'à {@link #MAX_ATTEMPTS}. */
    void tryStart() {
        if (started.get()) {
            return;
        }
        int n = attempts.incrementAndGet();
        for (Map.Entry<String, RedisMessageListenerContainer> e : containers.entrySet()) {
            if (startedNames.contains(e.getKey())) {
                continue;
            }
            try {
                e.getValue().start();
                startedNames.add(e.getKey());
                log.info("[REDIS-LISTENER] '{}' started on attempt {}", e.getKey(), n);
            } catch (Exception ex) {
                log.warn("[REDIS-LISTENER] '{}' start attempt {}/{} failed, retrying in {}s: {}",
                        e.getKey(), n, MAX_ATTEMPTS, RETRY_DELAY.toSeconds(), ex.getMessage());
            }
        }
        if (startedNames.size() == containers.size()) {
            started.set(true);
            log.info("[REDIS-LISTENER] all {} container(s) live — SSE + dispatcher + session pub/sub", containers.size());
            return;
        }
        if (n >= MAX_ATTEMPTS) {
            gaveUp.set(true);
            log.error("[REDIS-LISTENER] Gave up after {} attempts — {} of {} container(s) DOWN: realtime/session pub/sub DISABLED until app restart",
                    n, containers.size() - startedNames.size(), containers.size());
            return;
        }
        scheduler.schedule(this::tryStart, Instant.now().plus(RETRY_DELAY));
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
