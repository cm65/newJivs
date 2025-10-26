package com.jivs.platform.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson configuration for distributed locking
 *
 * CRITICAL FIX: Provides distributed locks to prevent concurrent execution
 * - Prevents race conditions in pause/resume operations
 * - Ensures only one instance can modify a migration at a time
 * - Automatic lock expiration prevents deadlocks
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.redis.timeout:2000}")
    private int redisTimeout;

    /**
     * Create Redisson client for distributed operations
     *
     * @return Configured RedissonClient instance
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        // Single server configuration (use cluster config for production HA)
        String address = String.format("redis://%s:%d", redisHost, redisPort);

        config.useSingleServer()
            .setAddress(address)
            .setPassword(redisPassword.isEmpty() ? null : redisPassword)
            .setDatabase(redisDatabase)

            // Connection pool settings
            .setConnectionPoolSize(64)
            .setConnectionMinimumIdleSize(10)

            // Timeout settings
            .setConnectTimeout(10000)
            .setTimeout(redisTimeout)

            // Retry settings
            .setRetryAttempts(3)
            .setRetryInterval(1500)

            // Keep alive
            .setKeepAlive(true)
            .setPingConnectionInterval(30000);

        return Redisson.create(config);
    }

    /**
     * For production with Redis Cluster, use this configuration instead:
     *
     * @Bean(destroyMethod = "shutdown")
     * public RedissonClient redissonClientCluster() {
     *     Config config = new Config();
     *     config.useClusterServers()
     *         .addNodeAddress(
     *             "redis://node1:6379",
     *             "redis://node2:6379",
     *             "redis://node3:6379"
     *         )
     *         .setPassword(redisPassword.isEmpty() ? null : redisPassword)
     *         .setScanInterval(2000) // cluster state scan interval
     *         .setMasterConnectionPoolSize(64)
     *         .setSlaveConnectionPoolSize(64);
     *
     *     return Redisson.create(config);
     * }
     */
}
