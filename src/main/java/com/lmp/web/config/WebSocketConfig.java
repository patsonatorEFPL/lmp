package com.lmp.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket pour les mises à jour temps réel
 * Utilisé pour notifier les administrateurs des changements de commandes
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Configuration du broker pour les messages sortants
        config.enableSimpleBroker("/topic", "/queue");
        // Préfixe pour les messages entrants
        config.setApplicationDestinationPrefixes("/app");
        // Configuration spécifique pour les utilisateurs
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket pour les connexions admin
        registry.addEndpoint("/ws/admin")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // Endpoint WebSocket pour les notifications générales
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}