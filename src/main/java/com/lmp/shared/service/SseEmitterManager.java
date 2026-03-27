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

    public SseEmitterManager() {
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

        Runnable cleanup = () -> {
            adminEmitters.remove(emitter);
            logger.debug("Admin SSE emitter removed (remaining: {})", adminEmitters.size());
        };
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
     */
    public void sendToUser(String userId, String eventName, Object data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            logger.debug("No SSE emitters found for user {}, event {} dropped", userId, eventName);
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
                removeEmitter(userId, emitter);
            }
        }
    }

    /**
     * Sends an SSE event to all admin emitters (broadcast).
     */
    public void sendToAdmins(String eventName, Object data) {
        if (adminEmitters.isEmpty()) {
            logger.debug("No admin SSE emitters, event {} dropped", eventName);
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
                adminEmitters.remove(emitter);
            }
        }
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
                    removeEmitter(entry.getKey(), emitter);
                }
            }
        }

        // Admin emitters
        for (SseEmitter emitter : adminEmitters) {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                adminEmitters.remove(emitter);
            }
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
