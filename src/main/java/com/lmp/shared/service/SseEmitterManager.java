package com.lmp.shared.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;

/**
 * Thread-safe registry of SSE emitters keyed by userId.
 * Supports multiple tabs per user (list of emitters per userId).
 * Sends a heartbeat comment every 15 seconds to keep connections alive through proxies/Nginx.
 */
@Component
public class SseEmitterManager {

    private static final Logger logger = LoggerFactory.getLogger(SseEmitterManager.class);
    private static final long HEARTBEAT_INTERVAL_SECONDS = 15;

    /** userId → list of active emitters (one per browser tab) */
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /** Admin broadcast emitters (admin dashboard SSE connections) */
    private final CopyOnWriteArrayList<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    /**
     * Broadcaster Redis pub/sub. Lazy car circular dep (Broadcaster injects
     * Manager pour dispatcher localement les messages reçus de Redis). Si
     * Redis indisponible, fallback fanout local seulement (single-replica mode).
     */
    private final ObjectProvider<SseRedisBroadcaster> broadcasterProvider;

    public SseEmitterManager(@Lazy ObjectProvider<SseRedisBroadcaster> broadcasterProvider) {
        this.broadcasterProvider = broadcasterProvider;
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeats,
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
    }

    /**
     * Creates and registers a new SSE emitter for the given user.
     * Timeout is set to 0 (infinite); cleaned up on completion/error/timeout.
     */
    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(0L); // infinite timeout

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> {
            logger.debug("SSE emitter error for user {}: {}", userId, e.getMessage());
            cleanup.run();
        });

        logger.info("SSE emitter created for user {} (total: {})",
                userId, userEmitters.getOrDefault(userId, new CopyOnWriteArrayList<>()).size());

        return emitter;
    }

    /**
     * Creates and registers a new SSE emitter for admin broadcast events.
     */
    public SseEmitter createAdminEmitter() {
        SseEmitter emitter = new SseEmitter(0L);

        adminEmitters.add(emitter);

        Runnable cleanup = () -> removeAdminEmitter(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> {
            logger.debug("Admin SSE emitter error: {}", e.getMessage());
            cleanup.run();
        });

        logger.info("Admin SSE emitter created (total: {})", adminEmitters.size());
        return emitter;
    }

    /**
     * Sends an SSE event to all emitters of a specific user.
     * <p>Si {@link SseRedisBroadcaster} disponible : publie sur Redis pub/sub →
     * toutes les replicas (incluant celle-ci par loopback) reçoivent et appellent
     * {@link #dispatchLocalUser}. Sinon fallback local-only.</p>
     */
    public void sendToUser(String userId, String eventName, Object data) {
        SseRedisBroadcaster broadcaster = broadcasterProvider.getIfAvailable();
        if (broadcaster != null) {
            broadcaster.publishToUser(userId, eventName, data);
        } else {
            dispatchLocalUser(userId, eventName, data);
        }
    }

    /**
     * Sends an SSE event to all admin emitters (broadcast).
     * <p>Voir {@link #sendToUser} pour le pattern Redis pub/sub multi-replica.</p>
     */
    public void sendToAdmins(String eventName, Object data) {
        SseRedisBroadcaster broadcaster = broadcasterProvider.getIfAvailable();
        if (broadcaster != null) {
            broadcaster.publishToAdmins(eventName, data);
        } else {
            dispatchLocalAdmin(eventName, data);
        }
    }

    /**
     * Fan-out vers les emitters locaux pour un userId. Appelé par
     * {@link SseRedisBroadcaster} quand un message Redis pub/sub arrive.
     */
    public void dispatchLocalUser(String userId, String eventName, Object data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            logger.debug("No local SSE emitters for user {}, event {} dropped on this replica", userId, eventName);
            return;
        }

        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(eventName)
                .data(data);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event);
            } catch (IOException | IllegalStateException e) {
                logger.debug("Failed to send SSE to user {}: {}", userId, e.getMessage());
                safeComplete(emitter);
                removeEmitter(userId, emitter);
            } catch (Exception e) {
                logger.debug("Failed to send SSE to user {}: {}", userId, e.getMessage());
                safeComplete(emitter);
                removeEmitter(userId, emitter);
            }
        }
    }

    /**
     * Fan-out vers les admin emitters locaux. Appelé par
     * {@link SseRedisBroadcaster} sur message Redis pub/sub admin channel.
     */
    public void dispatchLocalAdmin(String eventName, Object data) {
        if (adminEmitters.isEmpty()) {
            logger.debug("No local admin SSE emitters, event {} dropped on this replica", eventName);
            return;
        }

        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(eventName)
                .data(data);

        for (SseEmitter emitter : adminEmitters) {
            try {
                emitter.send(event);
            } catch (IOException | IllegalStateException e) {
                logger.debug("Failed to send SSE to admin: {}", e.getMessage());
                safeComplete(emitter);
                removeAdminEmitter(emitter);
            } catch (Exception e) {
                logger.debug("Failed to send SSE to admin: {}", e.getMessage());
                safeComplete(emitter);
                removeAdminEmitter(emitter);
            }
        }
    }

    /**
     * SECURITY (auth audit related) : ferme immédiatement tous les emitters SSE
     * actifs d'un utilisateur. Appelé après soft-delete, suspension admin, ou
     * autre invalidation de session pour éviter qu'un client continue de
     * recevoir des events après que le backend ait coupé son auth.
     *
     * Le heartbeat 15s rattrappe normalement les sessions timeout, mais un
     * soft-delete intervenu entre deux heartbeats laisserait une fenêtre de
     * 15s pendant laquelle le client reçoit encore des données. Cet appel
     * ferme proprement (emitter.complete()) → le client EventSource reconnecte
     * automatiquement et le prochain handshake fail sur 401.
     *
     * @param userId UUID utilisateur (même string que createEmitter)
     * @return nombre d'emitters fermés
     */
    public int closeUserEmitters(String userId) {
        List<SseEmitter> emitters = userEmitters.remove(userId);
        if (emitters == null || emitters.isEmpty()) {
            return 0;
        }
        int closed = 0;
        for (SseEmitter emitter : emitters) {
            safeComplete(emitter);
            closed++;
        }
        logger.info("SSE: {} emitter(s) fermé(s) pour user {} (invalidation session)", closed, userId);
        return closed;
    }

    private void removeAdminEmitter(SseEmitter emitter) {
        adminEmitters.remove(emitter);
        logger.debug("Admin SSE emitter removed (remaining: {})", adminEmitters.size());
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    /**
     * Sends heartbeat comments to all active emitters to keep connections alive.
     */
    private void sendHeartbeats() {
        // User emitters
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : userEmitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException | IllegalStateException e) {
                    safeComplete(emitter);
                    removeEmitter(entry.getKey(), emitter);
                } catch (Exception e) {
                    safeComplete(emitter);
                    removeEmitter(entry.getKey(), emitter);
                }
            }
        }

        // Admin emitters
        for (SseEmitter emitter : adminEmitters) {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                safeComplete(emitter);
                removeAdminEmitter(emitter);
            } catch (Exception e) {
                safeComplete(emitter);
                removeAdminEmitter(emitter);
            }
        }
    }

    private static void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // already completed or broken
        }
    }

    @PreDestroy
    public void shutdown() {
        heartbeatScheduler.shutdown();
        // Complete all emitters
        userEmitters.values().forEach(list -> list.forEach(SseEmitter::complete));
        userEmitters.clear();
        adminEmitters.forEach(SseEmitter::complete);
        adminEmitters.clear();
        logger.info("SseEmitterManager shut down");
    }
}
