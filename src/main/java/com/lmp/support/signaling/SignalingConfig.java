package com.lmp.support.signaling;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * STOMP broker config for HelpDesk WebRTC signaling.
 * Tech and runner each subscribe to per-session topics; the broker relays SDP
 * offers, answers and ICE candidates verbatim.
 */
@Configuration
@EnableWebSocketMessageBroker
public class SignalingConfig implements WebSocketMessageBrokerConfigurer {

    private final String corsAllowedOrigins;

    public SignalingConfig(
        @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://localhost:8080}")
        String corsAllowedOrigins
    ) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
        registry.addEndpoint("/ws/signaling")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }
}
