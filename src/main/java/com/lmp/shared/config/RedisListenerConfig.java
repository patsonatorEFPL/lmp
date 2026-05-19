package com.lmp.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
 *
 * <p>{@code @Primary} : Spring Session ajoute aussi un
 * {@code springSessionRedisMessageListenerContainer} (interne). Sans @Primary,
 * l'injection par type dans {@code SseRedisBroadcaster} échoue avec
 * "expected single matching bean but found 2". Notre container = celui
 * applicatif, le Spring Session = interne — donc on déclare le nôtre comme
 * primaire.</p>
 */
@Configuration
public class RedisListenerConfig {

    @Bean
    @Primary
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
