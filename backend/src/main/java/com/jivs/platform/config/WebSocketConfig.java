package com.jivs.platform.config;

import com.jivs.platform.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time updates
 * Enables STOMP over WebSocket for extraction and migration status updates
 * Secured with JWT authentication via WebSocketAuthInterceptor
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${jivs.security.websocket.allowed-origins}")
    private String[] allowedOrigins;

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
     * Allowed origins configured via jivs.security.websocket.allowed-origins property
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS(); // Enable SockJS fallback
    }

    /**
     * Configure client inbound channel interceptors
     * Adds JWT authentication for WebSocket connections
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
