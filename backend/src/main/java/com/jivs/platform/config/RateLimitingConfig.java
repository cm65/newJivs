package com.jivs.platform.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for rate limiting using Resilience4j
 * Prevents API abuse and ensures fair resource utilization
 */
@Configuration
public class RateLimitingConfig {

    /**
     * Default rate limiter for general API endpoints
     * 100 requests per minute per user
     */
    @Bean
    public RateLimiter defaultRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(100)
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return rateLimiterRegistry.rateLimiter("default", config);
    }

    /**
     * Strict rate limiter for authentication endpoints
     * 5 login attempts per minute to prevent brute force attacks
     */
    @Bean
    public RateLimiter authRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(5)
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return rateLimiterRegistry.rateLimiter("auth", config);
    }

    /**
     * Rate limiter for data extraction endpoints
     * 10 extraction jobs per hour per user
     */
    @Bean
    public RateLimiter extractionRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofHours(1))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return rateLimiterRegistry.rateLimiter("extraction", config);
    }

    /**
     * Rate limiter for migration endpoints
     * 5 migration jobs per hour per user
     */
    @Bean
    public RateLimiter migrationRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofHours(1))
            .limitForPeriod(5)
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return rateLimiterRegistry.rateLimiter("migration", config);
    }

    /**
     * Rate limiter for data quality rule execution
     * 50 rule executions per minute
     */
    @Bean
    public RateLimiter dataQualityRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(50)
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return rateLimiterRegistry.rateLimiter("data-quality", config);
    }

    /**
     * Rate limiter for compliance requests
     * 10 GDPR/CCPA requests per day per user
     */
    @Bean
    public RateLimiter complianceRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofDays(1))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        return rateLimiterRegistry.rateLimiter("compliance", config);
    }

    /**
     * Lenient rate limiter for read-only operations
     * 500 requests per minute
     */
    @Bean
    public RateLimiter readOnlyRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(500)
            .timeoutDuration(Duration.ofSeconds(1))
            .build();

        return rateLimiterRegistry.rateLimiter("read-only", config);
    }
}
