package com.jivs.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time updates
 * Enables STOMP over WebSocket for extraction and migration status updates
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker for pub/sub messaging
     * - In-memory broker for /topic destinations
     * - Redis broker can be added for multi-instance scalability
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for /topic destinations
        registry.enableSimpleBroker("/topic");

        // Set application destination prefix for @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");

        // Optional: For multi-instance deployments, use Redis broker
        // registry.enableStompBrokerRelay("/topic")
        //     .setRelayHost("localhost")
        //     .setRelayPort(61613)
        //     .setClientLogin("guest")
        //     .setClientPasscode("guest");
    }

    /**
     * Register STOMP endpoints for WebSocket connections
     * SockJS fallback enabled for browsers without WebSocket support
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure based on environment
                .withSockJS(); // Enable SockJS fallback
    }
}
