package com.jivs.platform.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * P0.3: Redis Cache Configuration for Performance Optimization
 *
 * Implements caching strategy for:
 * - DataSource configurations (1 hour TTL)
 * - ExtractionConfig lookups (30 minutes TTL)
 * - ExtractionStatistics (5 minutes TTL)
 *
 * Expected Impact:
 * - Throughput: +10% (17.5k → 19.25k records/min)
 * - Latency: -50ms (270ms → 220ms)
 * - Cache hit rate target: 70%+
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CacheConfig.class);

    /**
     * Configure default cache settings with Redis
     */
    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        // Create ObjectMapper with polymorphic type handling
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )
            .disableCachingNullValues();
    }

    /**
     * Configure cache manager with specific cache configurations
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("Configuring Redis Cache Manager for extraction performance optimization");

        // Define cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // DataSource cache - long TTL (1 hour) as configs rarely change
        cacheConfigurations.put("dataSources",
            defaultCacheConfiguration()
                .entryTtl(Duration.ofHours(1))
                .prefixCacheNameWith("jivs:datasource:")
        );

        // ExtractionConfig cache - medium TTL (30 minutes)
        cacheConfigurations.put("extractionConfigs",
            defaultCacheConfiguration()
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("jivs:extraction:config:")
        );

        // ExtractionStatistics cache - short TTL (5 minutes) for near real-time data
        cacheConfigurations.put("extractionStats",
            defaultCacheConfiguration()
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("jivs:extraction:stats:")
        );

        // Running jobs cache - very short TTL (1 minute) for active monitoring
        cacheConfigurations.put("runningJobs",
            defaultCacheConfiguration()
                .entryTtl(Duration.ofMinutes(1))
                .prefixCacheNameWith("jivs:extraction:running:")
        );

        // Connection pool metadata - medium TTL (15 minutes)
        cacheConfigurations.put("connectionPools",
            defaultCacheConfiguration()
                .entryTtl(Duration.ofMinutes(15))
                .prefixCacheNameWith("jivs:pool:")
        );

        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfiguration())
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();

        log.info("Redis Cache Manager configured with {} custom cache configurations",
            cacheConfigurations.size());

        return cacheManager;
    }
}
