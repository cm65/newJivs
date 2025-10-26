package com.jivs.platform.executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Performance tests for async executor improvements
 *
 * CRITICAL: Verifies thread pool performance and prevents thread starvation
 * - Tests parallel task execution
 * - Validates queue capacity and rejection handling
 * - Tests graceful shutdown
 * - Measures throughput and latency
 */
@SpringBootTest
@ActiveProfiles("test")
class AsyncExecutorPerformanceTest {

    @Autowired
    @Qualifier("migrationExecutor")
    private ThreadPoolTaskExecutor migrationExecutor;

    @Autowired
    @Qualifier("extractionExecutor")
    private ThreadPoolTaskExecutor extractionExecutor;

    @Autowired
    @Qualifier("loadExecutor")
    private ThreadPoolTaskExecutor loadExecutor;

    private ExecutorService testExecutor;

    @BeforeEach
    void setUp() {
        testExecutor = Executors.newFixedThreadPool(20);
    }

    // =====================================
    // Throughput Tests
    // =====================================

    @Test
    void migrationExecutor_HighThroughput_HandlesSuccessfully() throws Exception {
        // Given
        int taskCount = 1000;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger completedTasks = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // When - submit 1000 tasks
        for (int i = 0; i < taskCount; i++) {
            migrationExecutor.submit(() -> {
                try {
                    Thread.sleep(10); // Simulate work
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(completed).isTrue();
        assertThat(completedTasks.get()).isEqualTo(taskCount);
        assertThat(duration).isLessThan(60000); // Should complete within 60s

        double throughput = (taskCount * 1000.0) / duration;
        System.out.println("Throughput: " + throughput + " tasks/second");
        assertThat(throughput).isGreaterThan(50); // At least 50 tasks/sec
    }

    @Test
    void extractionExecutor_ParallelExtractions_NoThreadStarvation() throws Exception {
        // Given
        int parallelExtractions = 20;
        CountDownLatch latch = new CountDownLatch(parallelExtractions);
        List<Future<String>> futures = new ArrayList<>();

        // When - submit 20 parallel extraction tasks
        for (int i = 0; i < parallelExtractions; i++) {
            final int taskId = i;
            Future<String> future = extractionExecutor.submit(() -> {
                try {
                    Thread.sleep(100); // Simulate extraction work
                    return "Extraction-" + taskId;
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(futures).hasSize(parallelExtractions);

        // All futures should complete successfully
        for (int i = 0; i < parallelExtractions; i++) {
            assertThat(futures.get(i).get()).isEqualTo("Extraction-" + i);
        }
    }

    @Test
    void loadExecutor_BulkLoads_HandlesWithoutBlocking() throws Exception {
        // Given
        int bulkLoadCount = 15;
        CountDownLatch latch = new CountDownLatch(bulkLoadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - simulate bulk load operations
        for (int i = 0; i < bulkLoadCount; i++) {
            loadExecutor.submit(() -> {
                try {
                    Thread.sleep(200); // Simulate database load
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(20, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(bulkLoadCount);
    }

    // =====================================
    // Queue Capacity Tests
    // =====================================

    @Test
    void migrationExecutor_ExceedsQueueCapacity_RejectsGracefully() throws Exception {
        // Given - queue capacity is 500
        // Fill up executor with long-running tasks
        int longRunningTasks = 550; // Core pool + max pool + queue capacity
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch blockingLatch = new CountDownLatch(longRunningTasks);

        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger rejectedCount = new AtomicInteger(0);

        // When - submit more tasks than capacity
        for (int i = 0; i < longRunningTasks; i++) {
            try {
                Future<?> future = migrationExecutor.submit(() -> {
                    try {
                        startLatch.await(); // Block until released
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        blockingLatch.countDown();
                    }
                });
                futures.add(future);
            } catch (TaskRejectedException e) {
                rejectedCount.incrementAndGet();
            }
        }

        // Release all tasks
        startLatch.countDown();
        blockingLatch.await(5, TimeUnit.SECONDS);

        // Then - some tasks should be rejected
        System.out.println("Rejected tasks: " + rejectedCount.get());
        assertThat(rejectedCount.get()).isGreaterThan(0);
    }

    @Test
    void extractionExecutor_AtCapacity_QueuesProperly() throws Exception {
        // Given
        int corePoolSize = extractionExecutor.getCorePoolSize();
        int maxPoolSize = extractionExecutor.getMaxPoolSize();
        int queueCapacity = extractionExecutor.getQueueCapacity();

        int tasksToSubmit = corePoolSize + queueCapacity;

        CountDownLatch blockingLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(tasksToSubmit);

        // When - fill core pool and queue
        for (int i = 0; i < tasksToSubmit; i++) {
            extractionExecutor.submit(() -> {
                try {
                    blockingLatch.await(); // Block tasks
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Verify active and queued counts
        Thread.sleep(500); // Let tasks settle
        int activeCount = extractionExecutor.getActiveCount();
        int queueSize = extractionExecutor.getThreadPoolExecutor().getQueue().size();

        assertThat(activeCount).isEqualTo(corePoolSize);
        assertThat(queueSize).isEqualTo(queueCapacity);

        // Release and cleanup
        blockingLatch.countDown();
        completionLatch.await(5, TimeUnit.SECONDS);
    }

    // =====================================
    // CompletableFuture Performance Tests
    // =====================================

    @Test
    void completableFuture_NonBlocking_CompletesWithTimeout() throws Exception {
        // Given
        List<CompletableFuture<String>> futures = new ArrayList<>();

        // When - create multiple async operations
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Task-" + taskId;
            }, migrationExecutor.getThreadPoolExecutor());
            futures.add(future);
        }

        // Then - use allOf with timeout instead of join
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));

        assertThat(allOf.get(5, TimeUnit.SECONDS)).isNull(); // All complete

        // Verify all results
        for (int i = 0; i < 10; i++) {
            assertThat(futures.get(i).get()).isEqualTo("Task-" + i);
        }
    }

    @Test
    void completableFuture_WithTimeout_DoesNotBlockIndefinitely() {
        // Given
        CompletableFuture<String> slowFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(10000); // 10 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Should timeout";
        }, migrationExecutor.getThreadPoolExecutor());

        // When/Then - timeout after 2 seconds
        assertThatThrownBy(() -> slowFuture.get(2, TimeUnit.SECONDS))
            .isInstanceOf(TimeoutException.class);

        // Cancel the future
        slowFuture.cancel(true);
    }

    @Test
    void completableFuture_ExceptionalCompletion_HandlesGracefully() throws Exception {
        // Given
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("Simulated failure");
        }, migrationExecutor.getThreadPoolExecutor());

        CompletableFuture<String> handled = future.exceptionally(ex -> {
            return "Handled: " + ex.getMessage();
        });

        // When
        String result = handled.get(2, TimeUnit.SECONDS);

        // Then
        assertThat(result).startsWith("Handled:");
    }

    // =====================================
    // Graceful Shutdown Tests
    // =====================================

    @Test
    void executor_GracefulShutdown_WaitsForTaskCompletion() throws Exception {
        // Given - create new executor for testing shutdown
        ThreadPoolTaskExecutor testExecutor = new ThreadPoolTaskExecutor();
        testExecutor.setCorePoolSize(5);
        testExecutor.setMaxPoolSize(10);
        testExecutor.setQueueCapacity(50);
        testExecutor.setThreadNamePrefix("shutdown-test-");
        testExecutor.setWaitForTasksToCompleteOnShutdown(true);
        testExecutor.setAwaitTerminationSeconds(30);
        testExecutor.initialize();

        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskCompleted = new CountDownLatch(1);

        // When - submit task and shutdown
        testExecutor.submit(() -> {
            taskStarted.countDown();
            try {
                Thread.sleep(2000); // 2 second task
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                taskCompleted.countDown();
            }
        });

        taskStarted.await(1, TimeUnit.SECONDS);
        testExecutor.shutdown();

        // Then - task should complete before shutdown
        boolean completed = taskCompleted.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(testExecutor.getThreadPoolExecutor().isTerminated()).isTrue();
    }

    @Test
    void executor_ShutdownNow_InterruptsTasks() throws Exception {
        // Given
        ThreadPoolTaskExecutor testExecutor = new ThreadPoolTaskExecutor();
        testExecutor.setCorePoolSize(5);
        testExecutor.setMaxPoolSize(10);
        testExecutor.initialize();

        AtomicInteger interruptedCount = new AtomicInteger(0);
        CountDownLatch taskStarted = new CountDownLatch(5);

        // When - submit long-running tasks
        for (int i = 0; i < 5; i++) {
            testExecutor.submit(() -> {
                taskStarted.countDown();
                try {
                    Thread.sleep(60000); // Long task
                } catch (InterruptedException e) {
                    interruptedCount.incrementAndGet();
                }
            });
        }

        taskStarted.await(1, TimeUnit.SECONDS);

        // Immediate shutdown
        List<Runnable> remainingTasks = testExecutor.getThreadPoolExecutor().shutdownNow();

        Thread.sleep(1000); // Wait for interrupts to propagate

        // Then - tasks should be interrupted
        assertThat(interruptedCount.get()).isGreaterThan(0);
    }

    // =====================================
    // Resource Leak Tests
    // =====================================

    @Test
    void executor_RepeatedSubmissions_NoMemoryLeak() throws Exception {
        // Given
        int iterations = 100;
        long initialMemory = Runtime.getRuntime().totalMemory() -
                             Runtime.getRuntime().freeMemory();

        // When - repeatedly submit and complete tasks
        for (int iter = 0; iter < iterations; iter++) {
            CountDownLatch latch = new CountDownLatch(50);

            for (int i = 0; i < 50; i++) {
                migrationExecutor.submit(() -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);

            if (iter % 20 == 0) {
                System.gc(); // Suggest garbage collection
                Thread.sleep(100);
            }
        }

        // Then - memory should not grow significantly
        Thread.sleep(1000);
        System.gc();
        Thread.sleep(1000);

        long finalMemory = Runtime.getRuntime().totalMemory() -
                          Runtime.getRuntime().freeMemory();
        long memoryGrowth = finalMemory - initialMemory;

        System.out.println("Memory growth: " + (memoryGrowth / 1024 / 1024) + " MB");

        // Allow reasonable growth (JIT, caching, etc.) but not excessive
        assertThat(memoryGrowth).isLessThan(100 * 1024 * 1024); // 100 MB
    }

    // =====================================
    // Thread Pool Metrics Tests
    // =====================================

    @Test
    void executor_Metrics_ReflectCurrentState() throws Exception {
        // Given
        int tasksToSubmit = 20;
        CountDownLatch blockingLatch = new CountDownLatch(1);
        CountDownLatch taskStartLatch = new CountDownLatch(tasksToSubmit);

        // When - submit tasks that block
        for (int i = 0; i < tasksToSubmit; i++) {
            migrationExecutor.submit(() -> {
                taskStartLatch.countDown();
                try {
                    blockingLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        taskStartLatch.await(2, TimeUnit.SECONDS);

        // Then - verify metrics
        int activeCount = migrationExecutor.getActiveCount();
        int poolSize = migrationExecutor.getPoolSize();
        int queueSize = migrationExecutor.getThreadPoolExecutor().getQueue().size();

        assertThat(activeCount).isGreaterThan(0);
        assertThat(poolSize).isGreaterThan(0);
        assertThat(activeCount + queueSize).isEqualTo(tasksToSubmit);

        // Cleanup
        blockingLatch.countDown();
        Thread.sleep(500);
    }

    @Test
    void executor_CompletedTaskCount_Increments() throws Exception {
        // Given
        ThreadPoolExecutor underlying = migrationExecutor.getThreadPoolExecutor();
        long initialCompletedTasks = underlying.getCompletedTaskCount();

        // When
        int tasksToSubmit = 50;
        CountDownLatch latch = new CountDownLatch(tasksToSubmit);

        for (int i = 0; i < tasksToSubmit; i++) {
            migrationExecutor.submit(() -> {
                try {
                    Thread.sleep(10);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        Thread.sleep(500); // Let metrics update

        // Then
        long finalCompletedTasks = underlying.getCompletedTaskCount();
        assertThat(finalCompletedTasks - initialCompletedTasks).isGreaterThanOrEqualTo(tasksToSubmit);
    }
}
