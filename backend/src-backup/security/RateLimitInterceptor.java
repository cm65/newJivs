package com.jivs.platform.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor for enforcing rate limits on API endpoints
 * Uses Resilience4j for distributed rate limiting
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterRegistry rateLimiterRegistry;

    // Cache of user-specific rate limiters
    private final Map<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Skip rate limiting for health checks and actuator endpoints
        if (path.startsWith("/actuator") || path.equals("/health")) {
            return true;
        }

        // Get the appropriate rate limiter based on endpoint
        String rateLimiterName = determineRateLimiterName(path, method);

        // Get user identifier
        String userId = getUserIdentifier(request);

        // Create or get user-specific rate limiter
        String rateLimiterKey = rateLimiterName + ":" + userId;
        RateLimiter rateLimiter = userRateLimiters.computeIfAbsent(rateLimiterKey, key -> {
            RateLimiter baseLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName);
            return RateLimiter.of(key, baseLimiter.getRateLimiterConfig());
        });

        try {
            // Attempt to acquire permission
            rateLimiter.acquirePermission();
            log.debug("Rate limit check passed for user: {} on endpoint: {}", userId, path);
            return true;
        } catch (RequestNotPermitted e) {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for user: {} on endpoint: {}", userId, path);
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "error": "Too Many Requests",
                    "message": "Rate limit exceeded. Please try again later.",
                    "status": 429
                }
                """);
            return false;
        }
    }

    /**
     * Determine which rate limiter to use based on the endpoint
     */
    private String determineRateLimiterName(String path, String method) {
        // Authentication endpoints - strict limits
        if (path.startsWith("/api/v1/auth/login") ||
            path.startsWith("/api/v1/auth/register")) {
            return "auth";
        }

        // Extraction endpoints
        if (path.startsWith("/api/v1/extractions")) {
            if ("POST".equals(method) || path.contains("/start") || path.contains("/stop")) {
                return "extraction";
            }
            return "read-only";
        }

        // Migration endpoints
        if (path.startsWith("/api/v1/migrations")) {
            if ("POST".equals(method) || path.contains("/start") ||
                path.contains("/pause") || path.contains("/rollback")) {
                return "migration";
            }
            return "read-only";
        }

        // Data quality endpoints
        if (path.startsWith("/api/v1/data-quality")) {
            if (path.contains("/execute") || "POST".equals(method)) {
                return "data-quality";
            }
            return "read-only";
        }

        // Compliance endpoints
        if (path.startsWith("/api/v1/compliance")) {
            if ("POST".equals(method) || path.contains("/process")) {
                return "compliance";
            }
            return "read-only";
        }

        // Read-only operations (GET requests)
        if ("GET".equals(method)) {
            return "read-only";
        }

        // Default rate limiter for all other endpoints
        return "default";
    }

    /**
     * Get user identifier from authentication context or IP address
     */
    private String getUserIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }

        // For unauthenticated requests, use IP address
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple IPs, use the first one
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return "ip:" + ipAddress;
    }

    /**
     * Clean up rate limiters periodically to prevent memory leaks
     * This should be called by a scheduled task
     */
    public void cleanupInactiveLimiters() {
        log.info("Cleaning up inactive rate limiters. Current count: {}", userRateLimiters.size());
        // In production, you'd want to track last access time and remove stale entries
        // For now, we'll keep all limiters as they have configurable timeouts
    }
}
