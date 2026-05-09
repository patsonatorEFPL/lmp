package com.lmp.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Bean {@link RedisMessageListenerContainer} — requis pour les listeners
 * pub/sub (SSE multi-replica). Spring Boot autoconfig ne le crée pas par
 * défaut, il faut le déclarer explicitement.
 *
 * <p>Le container démarre un thread pool qui dispatche les messages reçus
 * sur les channels souscrits aux {@link org.springframework.data.redis.connection.MessageListener}
 * enregistrés (par ex. {@code SseRedisBroadcaster}).</p>
 */
@Configuration
public class RedisListenerConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
