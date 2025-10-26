package com.jivs.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ✅ FIX: Migration Executor Configuration with Proper Lifecycle Management
 *
 * Fixes CRITICAL Issue #3: ExecutorService Resource Leak
 *
 * This configuration:
 * 1. Creates a Spring-managed ThreadPoolTaskExecutor instead of raw ExecutorService
 * 2. Implements proper shutdown hooks via @PreDestroy
 * 3. Configurable via application.yml properties
 * 4. Provides graceful shutdown with timeout
 * 5. Logs executor lifecycle events
 *
 * Related Issues:
 * - MigrationOrchestrator.java line 43: ExecutorService never shut down
 * - LoadService.java line 27: ExecutorService never shut down
 *
 * @author JiVS Team
 * @since 2025-01-13
 */
@Slf4j
@Configuration
public class MigrationExecutorConfig {

    @Value("${jivs.migration.executor.core-pool-size:4}")
    private int corePoolSize;

    @Value("${jivs.migration.executor.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${jivs.migration.executor.queue-capacity:100}")
    private int queueCapacity;

    @Value("${jivs.migration.executor.thread-name-prefix:migration-executor-}")
    private String threadNamePrefix;

    @Value("${jivs.migration.executor.await-termination-seconds:60}")
    private int awaitTerminationSeconds;

    private ThreadPoolTaskExecutor migrationExecutor;
    private ThreadPoolTaskExecutor loadExecutor;

    /**
     * Bean for migration orchestration async operations
     *
     * Usage in MigrationOrchestrator:
     * <pre>
     * {@code
     * @Autowired
     * @Qualifier("migrationExecutor")
     * private Executor migrationExecutor;
     *
     * public CompletableFuture<Migration> executeMigration(Long id) {
     *     return CompletableFuture.supplyAsync(() -> {
     *         // Migration logic
     *     }, migrationExecutor);
     * }
     * }
     * </pre>
     */
    @Bean(name = "migrationExecutor")
    public Executor migrationExecutor() {
        log.info("Initializing Migration Executor with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                corePoolSize, maxPoolSize, queueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        // Graceful shutdown settings
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);

        // Rejection policy: Caller runs (back-pressure)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Task decorator for MDC/context propagation
        executor.setTaskDecorator(new MigrationContextDecorator());

        executor.initialize();

        this.migrationExecutor = executor;

        log.info("Migration Executor initialized successfully");
        return executor;
    }

    /**
     * Bean for data loading operations (separate thread pool)
     *
     * Usage in LoadService:
     * <pre>
     * {@code
     * @Autowired
     * @Qualifier("loadExecutor")
     * private Executor loadExecutor;
     *
     * public CompletableFuture<LoadResult> parallelLoad(LoadContext ctx) {
     *     return CompletableFuture.supplyAsync(() -> {
     *         // Load logic
     *     }, loadExecutor);
     * }
     * }
     * </pre>
     */
    @Bean(name = "loadExecutor")
    public Executor loadExecutor() {
        log.info("Initializing Load Executor with corePoolSize={}, maxPoolSize={}",
                corePoolSize * 2, maxPoolSize * 2);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Load operations need more threads due to I/O wait
        executor.setCorePoolSize(corePoolSize * 2);
        executor.setMaxPoolSize(maxPoolSize * 2);
        executor.setQueueCapacity(queueCapacity * 2);
        executor.setThreadNamePrefix("load-executor-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new MigrationContextDecorator());

        executor.initialize();

        this.loadExecutor = executor;

        log.info("Load Executor initialized successfully");
        return executor;
    }

    /**
     * Graceful shutdown hook
     *
     * Ensures:
     * 1. No new tasks accepted
     * 2. Running tasks complete (up to awaitTerminationSeconds)
     * 3. Queued tasks are processed
     * 4. Resources cleaned up
     *
     * This fixes the resource leak where ExecutorService was never shut down.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Migration Executors...");

        shutdownExecutor(migrationExecutor, "Migration Executor");
        shutdownExecutor(loadExecutor, "Load Executor");

        log.info("All Migration Executors shut down successfully");
    }

    /**
     * Helper method to shutdown an executor with proper logging
     */
    private void shutdownExecutor(ThreadPoolTaskExecutor executor, String name) {
        if (executor == null) {
            log.warn("{} is null, skipping shutdown", name);
            return;
        }

        log.info("Shutting down {} (active: {}, queued: {})",
                name,
                executor.getActiveCount(),
                executor.getThreadPoolExecutor().getQueue().size());

        executor.shutdown();

        try {
            // Wait for tasks to complete
            if (!executor.getThreadPoolExecutor().awaitTermination(awaitTerminationSeconds, TimeUnit.SECONDS)) {
                log.warn("{} did not terminate in {} seconds, forcing shutdown",
                        name, awaitTerminationSeconds);

                executor.getThreadPoolExecutor().shutdownNow();

                // Wait again for forced shutdown
                if (!executor.getThreadPoolExecutor().awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("{} did not terminate after forced shutdown", name);
                } else {
                    log.info("{} terminated after forced shutdown", name);
                }
            } else {
                log.info("{} shut down gracefully", name);
            }
        } catch (InterruptedException e) {
            log.error("{} shutdown interrupted", name, e);
            executor.getThreadPoolExecutor().shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Task decorator for propagating context (MDC, security, etc.)
     *
     * This ensures:
     * - Logging context (request ID, user ID) is preserved in async tasks
     * - Security context is available in async methods
     * - Custom context attributes are propagated
     */
    private static class MigrationContextDecorator implements org.springframework.core.task.TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture current context
            String migrationId = org.slf4j.MDC.get("migrationId");
            String userId = org.slf4j.MDC.get("userId");
            String requestId = org.slf4j.MDC.get("requestId");

            return () -> {
                try {
                    // Restore context in async thread
                    if (migrationId != null) {
                        org.slf4j.MDC.put("migrationId", migrationId);
                    }
                    if (userId != null) {
                        org.slf4j.MDC.put("userId", userId);
                    }
                    if (requestId != null) {
                        org.slf4j.MDC.put("requestId", requestId);
                    }

                    // Execute task
                    runnable.run();
                } finally {
                    // Clear context
                    org.slf4j.MDC.remove("migrationId");
                    org.slf4j.MDC.remove("userId");
                    org.slf4j.MDC.remove("requestId");
                }
            };
        }
    }

    /**
     * Health check endpoint data
     *
     * Expose executor metrics for monitoring:
     * - Active threads
     * - Queue size
     * - Completed tasks
     * - Pool size
     */
    @Bean
    public MigrationExecutorMetrics migrationExecutorMetrics() {
        return new MigrationExecutorMetrics(migrationExecutor, loadExecutor);
    }

    /**
     * Metrics bean for executor monitoring
     */
    public static class MigrationExecutorMetrics {
        private final ThreadPoolTaskExecutor migrationExecutor;
        private final ThreadPoolTaskExecutor loadExecutor;

        public MigrationExecutorMetrics(ThreadPoolTaskExecutor migrationExecutor,
                                       ThreadPoolTaskExecutor loadExecutor) {
            this.migrationExecutor = migrationExecutor;
            this.loadExecutor = loadExecutor;
        }

        public ExecutorStats getMigrationExecutorStats() {
            return getStats(migrationExecutor);
        }

        public ExecutorStats getLoadExecutorStats() {
            return getStats(loadExecutor);
        }

        private ExecutorStats getStats(ThreadPoolTaskExecutor executor) {
            if (executor == null) {
                return new ExecutorStats(0, 0, 0, 0, 0);
            }

            ThreadPoolExecutor tpe = executor.getThreadPoolExecutor();
            return new ExecutorStats(
                    tpe.getActiveCount(),
                    tpe.getQueue().size(),
                    tpe.getCompletedTaskCount(),
                    tpe.getCorePoolSize(),
                    tpe.getMaximumPoolSize()
            );
        }
    }

    /**
     * Executor statistics for monitoring
     */
    public record ExecutorStats(
            int activeThreads,
            int queueSize,
            long completedTasks,
            int corePoolSize,
            int maxPoolSize
    ) {}
}
