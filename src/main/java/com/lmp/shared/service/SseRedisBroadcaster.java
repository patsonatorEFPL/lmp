package com.lmp.shared.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Redis pub/sub broker pour les événements SSE multi-replica.
 *
 * <p>Sans ce composant, un user connecté à la replica A ne reçoit jamais les
 * événements publiés depuis la replica B (les emitters SSE sont per-replica
 * en mémoire). Pattern externalCrm : Redis pub/sub channel = realtime broker.</p>
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>Service métier appelle {@code sseEmitterManager.sendToUser(userId, ...)}</li>
 *   <li>Manager délégue ici → publish JSON sur {@code lmp:sse:user:<userId>}</li>
 *   <li>Toutes les replicas (incluant celle qui publie, par loopback) reçoivent</li>
 *   <li>Chaque replica fan-out vers ses emitters SSE locaux pour ce userId</li>
 * </ol>
 *
 * <h3>Channels</h3>
 * <ul>
 *   <li>{@code lmp:sse:user:<userId>} — événement pour un user spécifique</li>
 *   <li>{@code lmp:sse:admin} — broadcast admin dashboards</li>
 * </ul>
 *
 * <p>Format message : JSON {@code {"name":"<eventName>","data":<payload>}}</p>
 */
@Component
public class SseRedisBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(SseRedisBroadcaster.class);
    private static final String USER_CHANNEL_PREFIX = "lmp:sse:user:";
    private static final String ADMIN_CHANNEL = "lmp:sse:admin";

    private final StringRedisTemplate redis;
    private final RedisMessageListenerContainer listenerContainer;
    private final SseEmitterManager emitterManager;
    private final ObjectMapper json = new ObjectMapper();

    public SseRedisBroadcaster(StringRedisTemplate redis,
                                RedisMessageListenerContainer listenerContainer,
                                SseEmitterManager emitterManager) {
        this.redis = redis;
        this.listenerContainer = listenerContainer;
        this.emitterManager = emitterManager;
    }

    @PostConstruct
    void subscribeChannels() {
        MessageListener userListener = (message, pattern) -> {
            String channel = new String(message.getChannel());
            String userId = channel.substring(USER_CHANNEL_PREFIX.length());
            handleMessage(userId, false, new String(message.getBody()));
        };
        MessageListener adminListener = (message, pattern) -> {
            handleMessage(null, true, new String(message.getBody()));
        };
        listenerContainer.addMessageListener(userListener,
                new PatternTopic(USER_CHANNEL_PREFIX + "*"));
        listenerContainer.addMessageListener(adminListener,
                new PatternTopic(ADMIN_CHANNEL));
        logger.info("[SSE-PUBSUB] Subscribed to {}{} and {}",
                USER_CHANNEL_PREFIX, "*", ADMIN_CHANNEL);
    }

    /**
     * Publie un événement à destination d'un user spécifique. Toutes les
     * replicas reçoivent et fan-out vers leurs emitters locaux.
     */
    public void publishToUser(String userId, String eventName, Object data) {
        try {
            String payload = json.writeValueAsString(new SseEnvelope(eventName, data));
            redis.convertAndSend(USER_CHANNEL_PREFIX + userId, payload);
        } catch (JsonProcessingException e) {
            logger.warn("[SSE-PUBSUB] Failed to serialize event {} for user {}: {}",
                    eventName, userId, e.getMessage());
        }
    }

    /**
     * Publie un événement broadcast admin. Toutes les replicas fan-out vers
     * leurs admin emitters locaux.
     */
    public void publishToAdmins(String eventName, Object data) {
        try {
            String payload = json.writeValueAsString(new SseEnvelope(eventName, data));
            redis.convertAndSend(ADMIN_CHANNEL, payload);
        } catch (JsonProcessingException e) {
            logger.warn("[SSE-PUBSUB] Failed to serialize admin event {}: {}",
                    eventName, e.getMessage());
        }
    }

    private void handleMessage(String userId, boolean isAdmin, String body) {
        try {
            SseEnvelope env = json.readValue(body, SseEnvelope.class);
            if (isAdmin) {
                emitterManager.dispatchLocalAdmin(env.name(), env.data());
            } else {
                emitterManager.dispatchLocalUser(userId, env.name(), env.data());
            }
        } catch (JsonProcessingException e) {
            logger.warn("[SSE-PUBSUB] Failed to deserialize message: {}", e.getMessage());
        }
    }

    /** Enveloppe sérialisée sur le channel Redis. */
    public record SseEnvelope(String name, Object data) {}
}
