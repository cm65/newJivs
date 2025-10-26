package com.jivs.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for migration workflow executors
 * Replaces hardcoded ExecutorService with Spring-managed beans
 *
 * CRITICAL FIX: Proper thread pool management
 * - Configurable pool sizes
 * - Graceful shutdown handling
 * - Bounded queues with rejection policy
 * - Monitoring integration
 */
@Configuration
public class WorkflowExecutorConfig {

    /**
     * Main executor for migration workflow orchestration
     * Handles phase execution and async coordination
     */
    @Bean(name = "migrationExecutor")
    public ThreadPoolTaskExecutor migrationExecutor(
            @Value("${jivs.migration.executor.core-pool-size:10}") int corePoolSize,
            @Value("${jivs.migration.executor.max-pool-size:50}") int maxPoolSize,
            @Value("${jivs.migration.executor.queue-capacity:500}") int queueCapacity,
            @Value("${jivs.migration.executor.await-termination-seconds:60}") int awaitTerminationSeconds) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("migration-exec-");
        executor.setThreadGroupName("migration-threads");

        // Caller runs policy - prevents task rejection
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);

        // Allow core threads to timeout when idle
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);

        executor.initialize();

        return executor;
    }

    /**
     * Dedicated executor for data loading operations
     * Higher parallelism for I/O bound tasks
     */
    @Bean(name = "loadExecutor")
    public ThreadPoolTaskExecutor loadExecutor(
            @Value("${jivs.load.executor.core-pool-size:20}") int corePoolSize,
            @Value("${jivs.load.executor.max-pool-size:100}") int maxPoolSize,
            @Value("${jivs.load.executor.queue-capacity:1000}") int queueCapacity) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("load-exec-");

        // Abort policy with logging - apply back-pressure
        executor.setRejectedExecutionHandler((r, exec) -> {
            throw new java.util.concurrent.RejectedExecutionException(
                "Load executor queue is full. Reduce parallelism or retry later.");
        });

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120); // Longer for data loading

        executor.initialize();

        return executor;
    }

    /**
     * Executor for extraction operations
     * Optimized for network I/O and external API calls
     */
    @Bean(name = "extractionExecutor")
    public ThreadPoolTaskExecutor extractionExecutor(
            @Value("${jivs.extraction.executor.core-pool-size:15}") int corePoolSize,
            @Value("${jivs.extraction.executor.max-pool-size:60}") int maxPoolSize,
            @Value("${jivs.extraction.executor.queue-capacity:500}") int queueCapacity) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("extract-exec-");

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(90);

        executor.initialize();

        return executor;
    }
}
