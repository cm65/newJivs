package com.jivs.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * Service for blacklisting JWT tokens to prevent their use after logout or revocation.
 * Uses Redis for distributed token blacklist storage with automatic expiration.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${jivs.security.jwt.secret}")
    private String jwtSecret;

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * Blacklist a token by its JTI (JWT ID) with automatic expiration
     *
     * @param token The JWT token to blacklist
     */
    public void blacklistToken(String token) {
        try {
            String jti = extractJti(token);
            long expirationMs = getExpirationTime(token);

            if (expirationMs > 0) {
                redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    "revoked",
                    Duration.ofMillis(expirationMs)
                );
                log.info("Token blacklisted successfully: {}", jti);
            } else {
                log.warn("Attempted to blacklist already expired token: {}", jti);
            }
        } catch (Exception e) {
            log.error("Error blacklisting token", e);
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }

    /**
     * Check if a token is blacklisted
     *
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isBlacklisted(String token) {
        try {
            String jti = extractJti(token);
            Boolean hasKey = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
            return hasKey != null && hasKey;
        } catch (Exception e) {
            log.error("Error checking token blacklist status", e);
            // Fail secure - if we can't check, consider it blacklisted
            return true;
        }
    }

    /**
     * Blacklist all tokens for a specific user
     *
     * @param userId The user ID whose tokens should be blacklisted
     */
    public void blacklistAllUserTokens(String userId) {
        try {
            // Store a user-level blacklist entry with a long expiration (7 days)
            redisTemplate.opsForValue().set(
                "user:blacklist:" + userId,
                String.valueOf(System.currentTimeMillis()),
                Duration.ofDays(7)
            );
            log.info("All tokens blacklisted for user: {}", userId);
        } catch (Exception e) {
            log.error("Error blacklisting all user tokens for user: {}", userId, e);
            throw new RuntimeException("Failed to blacklist user tokens", e);
        }
    }

    /**
     * Check if a user's tokens are blacklisted
     *
     * @param userId The user ID to check
     * @param tokenIssuedAt The timestamp when the token was issued
     * @return true if user tokens are blacklisted after the token issue time
     */
    public boolean isUserBlacklisted(String userId, Date tokenIssuedAt) {
        try {
            String blacklistTime = redisTemplate.opsForValue().get("user:blacklist:" + userId);
            if (blacklistTime != null) {
                long blacklistTimestamp = Long.parseLong(blacklistTime);
                return tokenIssuedAt.getTime() < blacklistTimestamp;
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking user blacklist status for user: {}", userId, e);
            return true; // Fail secure
        }
    }

    /**
     * Extract JTI (JWT ID) from token
     */
    private String extractJti(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(jwtSecret.getBytes())
            .build()
            .parseClaimsJws(token)
            .getBody();

        String jti = claims.getId();
        if (jti == null) {
            // If no JTI exists, generate one from the token content
            jti = UUID.nameUUIDFromBytes(token.getBytes()).toString();
        }
        return jti;
    }

    /**
     * Get remaining time until token expiration in milliseconds
     */
    private long getExpirationTime(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(jwtSecret.getBytes())
            .build()
            .parseClaimsJws(token)
            .getBody();

        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();
        return expiration.getTime() - now;
    }

    /**
     * Clear all blacklist entries (use with caution - for testing only)
     */
    public void clearBlacklist() {
        try {
            redisTemplate.keys(BLACKLIST_PREFIX + "*").forEach(key ->
                redisTemplate.delete(key)
            );
            log.warn("Token blacklist cleared");
        } catch (Exception e) {
            log.error("Error clearing blacklist", e);
        }
    }
}
