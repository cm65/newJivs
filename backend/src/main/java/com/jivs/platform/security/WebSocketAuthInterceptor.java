package com.jivs.platform.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * WebSocket security interceptor for JWT authentication
 * Validates JWT tokens in STOMP connection handshake
 * Part of Sprint 2 - Critical Security Fix
 */
@Component
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Autowired(required = false)
    private TokenBlacklistService tokenBlacklistService;

    public WebSocketAuthInterceptor(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT token from STOMP headers
            String jwt = getJwtFromStompHeaders(accessor);

            if (StringUtils.hasText(jwt)) {
                try {
                    // Validate token
                    if (tokenProvider.validateToken(jwt)) {
                        // Check if token is blacklisted (if TokenBlacklistService is available)
                        if (tokenBlacklistService != null && tokenBlacklistService.isBlacklisted(jwt)) {
                            log.warn("WebSocket connection attempt with blacklisted token");
                            throw new SecurityException("Token is blacklisted");
                        }

                        // Extract username and load user details
                        String username = tokenProvider.getUsernameFromToken(jwt);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        // Create authentication and set in context
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);

                        log.debug("WebSocket authenticated for user: {}", username);
                    } else {
                        log.warn("WebSocket connection attempt with invalid token");
                        throw new SecurityException("Invalid JWT token");
                    }
                } catch (Exception ex) {
                    log.error("WebSocket authentication failed: {}", ex.getMessage());
                    throw new SecurityException("WebSocket authentication failed", ex);
                }
            } else {
                log.warn("WebSocket connection attempt without JWT token");
                throw new SecurityException("Missing JWT token");
            }
        }

        return message;
    }

    /**
     * Extract JWT token from STOMP headers
     * Token can be passed in:
     * 1. "Authorization" header as "Bearer <token>"
     * 2. "token" header directly
     */
    private String getJwtFromStompHeaders(StompHeaderAccessor accessor) {
        // Try Authorization header first
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Try token header directly
        String token = accessor.getFirstNativeHeader("token");
        if (StringUtils.hasText(token)) {
            return token;
        }

        return null;
    }
}
