package com.jivs.platform.service.migration;

import com.jivs.platform.domain.migration.Migration;
import com.jivs.platform.domain.migration.MigrationStatus;
import com.jivs.platform.repository.MigrationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for distributed locking with Redisson
 *
 * CRITICAL: Verifies concurrent execution prevention
 * - Tests that only one instance can acquire lock
 * - Verifies lock timeout and automatic release
 * - Tests race condition prevention
 * - Validates fair lock acquisition
 */
@SpringBootTest
@ActiveProfiles("test")
class DistributedLockingIntegrationTest {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private MigrationRepository migrationRepository;

    @Autowired
    private MigrationOrchestrator orchestrator;

    private Migration testMigration;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        // Create test migration
        testMigration = new Migration();
        testMigration.setName("Lock Test Migration");
        testMigration.setStatus(MigrationStatus.PAUSED);
        testMigration = migrationRepository.save(testMigration);

        executorService = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void tearDown() {
        if (testMigration != null && testMigration.getId() != null) {
            migrationRepository.deleteById(testMigration.getId());
        }

        if (executorService != null) {
            executorService.shutdownNow();
        }

        // Clean up any remaining locks
        String lockKey = "migration:lock:" + (testMigration != null ? testMigration.getId() : "test");
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isLocked()) {
            lock.forceUnlock();
        }
    }

    @Test
    void resumeMigration_SingleThread_ShouldAcquireLock() throws Exception {
        // When
        Migration result = orchestrator.resumeMigration(testMigration.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(MigrationStatus.IN_PROGRESS);
    }

    @Test
    void resumeMigration_ConcurrentAttempts_OnlyOneSucceeds() throws Exception {
        // Given
        int concurrentAttempts = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentAttempts);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        // When - simulate 10 concurrent resume attempts
        for (int i = 0; i < concurrentAttempts; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    startLatch.await(); // All threads start simultaneously
                    orchestrator.resumeMigration(testMigration.getId());
                    successCount.incrementAndGet();
                } catch (ConcurrentMigrationException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // Unexpected exception
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        startLatch.countDown(); // Release all threads
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(1); // Only ONE succeeds
        assertThat(failureCount.get()).isEqualTo(9); // Other 9 fail gracefully
        assertThat(successCount.get() + failureCount.get()).isEqualTo(concurrentAttempts);

        // Verify migration is in progress
        Migration updated = migrationRepository.findById(testMigration.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(MigrationStatus.IN_PROGRESS);
    }

    @Test
    void lock_AcquireTwice_SecondAttemptFails() {
        // Given
        String lockKey = "migration:lock:" + testMigration.getId();
        RLock lock1 = redissonClient.getLock(lockKey);

        // When - first acquisition succeeds
        boolean acquired1 = lock1.tryLock();
        assertThat(acquired1).isTrue();

        // Then - second acquisition fails (different lock instance)
        RLock lock2 = redissonClient.getLock(lockKey);
        boolean acquired2 = lock2.tryLock();
        assertThat(acquired2).isFalse();

        // Cleanup
        lock1.unlock();
    }

    @Test
    void lock_WithTimeout_AutomaticallyReleased() throws Exception {
        // Given
        String lockKey = "migration:lock:timeout-test";
        RLock lock = redissonClient.getLock(lockKey);

        // When - acquire with 2 second lease
        boolean acquired = lock.tryLock(0, 2, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();
        assertThat(lock.isLocked()).isTrue();

        // Wait for lease to expire
        Thread.sleep(2500);

        // Then - lock automatically released
        assertThat(lock.isLocked()).isFalse();

        // Another instance can acquire it
        RLock lock2 = redissonClient.getLock(lockKey);
        boolean acquired2 = lock2.tryLock();
        assertThat(acquired2).isTrue();

        lock2.unlock();
    }

    @Test
    void lock_ManualUnlock_ReleasesImmediately() {
        // Given
        String lockKey = "migration:lock:manual-test";
        RLock lock = redissonClient.getLock(lockKey);

        // When
        boolean acquired = lock.tryLock();
        assertThat(acquired).isTrue();
        assertThat(lock.isLocked()).isTrue();

        lock.unlock();

        // Then
        assertThat(lock.isLocked()).isFalse();

        // Another instance can acquire
        RLock lock2 = redissonClient.getLock(lockKey);
        assertThat(lock2.tryLock()).isTrue();
        lock2.unlock();
    }

    @Test
    void lock_WaitTime_EventuallyAcquires() throws Exception {
        // Given
        String lockKey = "migration:lock:wait-test";
        RLock lock1 = redissonClient.getLock(lockKey);
        lock1.tryLock(0, 5, TimeUnit.SECONDS); // Hold for 5 seconds

        // When - second thread waits up to 10 seconds
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                RLock lock2 = redissonClient.getLock(lockKey);
                // Wait up to 10 seconds, lease for 5 minutes if acquired
                return lock2.tryLock(10, 300, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        });

        // Release first lock after 2 seconds
        Thread.sleep(2000);
        lock1.unlock();

        // Then - second thread should acquire within wait time
        Boolean acquired = future.get(15, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();
    }

    @Test
    void lock_FairLock_FIFOAcquisition() throws Exception {
        // Given
        String lockKey = "migration:lock:fair-test";
        RLock fairLock = redissonClient.getFairLock(lockKey);

        List<Integer> acquisitionOrder = new CopyOnWriteArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);

        // When - 5 threads try to acquire in order
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    RLock lock = redissonClient.getFairLock(lockKey);
                    if (lock.tryLock(30, 1, TimeUnit.SECONDS)) {
                        acquisitionOrder.add(threadId);
                        Thread.sleep(100); // Hold briefly
                        lock.unlock();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
            Thread.sleep(10); // Slight delay to ensure order
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(45, TimeUnit.SECONDS);

        // Then - should acquire in FIFO order
        assertThat(completed).isTrue();
        assertThat(acquisitionOrder).hasSize(5);
        assertThat(acquisitionOrder).containsExactly(0, 1, 2, 3, 4);
    }

    @Test
    void lock_Reentrant_SameThreadCanAcquireMultipleTimes() {
        // Given
        String lockKey = "migration:lock:reentrant-test";
        RLock lock = redissonClient.getLock(lockKey);

        // When - same thread acquires twice
        boolean acquired1 = lock.tryLock();
        boolean acquired2 = lock.tryLock(); // Reentrant

        // Then
        assertThat(acquired1).isTrue();
        assertThat(acquired2).isTrue();
        assertThat(lock.getHoldCount()).isEqualTo(2);

        // Must unlock twice
        lock.unlock();
        assertThat(lock.isHeldByCurrentThread()).isTrue();

        lock.unlock();
        assertThat(lock.isHeldByCurrentThread()).isFalse();
    }

    @Test
    void lock_IsLocked_ReflectsCurrentState() throws Exception {
        // Given
        String lockKey = "migration:lock:state-test";
        RLock lock = redissonClient.getLock(lockKey);

        // Initially unlocked
        assertThat(lock.isLocked()).isFalse();
        assertThat(lock.isHeldByCurrentThread()).isFalse();

        // After acquisition
        lock.tryLock(0, 10, TimeUnit.SECONDS);
        assertThat(lock.isLocked()).isTrue();
        assertThat(lock.isHeldByCurrentThread()).isTrue();

        // After unlock
        lock.unlock();
        assertThat(lock.isLocked()).isFalse();
        assertThat(lock.isHeldByCurrentThread()).isFalse();
    }

    @Test
    void lock_ForceUnlock_ReleasesEvenIfHeldByOtherThread() throws Exception {
        // Given
        String lockKey = "migration:lock:force-test";
        RLock lock1 = redissonClient.getLock(lockKey);

        // Thread 1 acquires lock
        CompletableFuture.runAsync(() -> {
            try {
                lock1.tryLock(0, 60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).get(1, TimeUnit.SECONDS);

        assertThat(lock1.isLocked()).isTrue();

        // When - force unlock from different thread
        RLock lock2 = redissonClient.getLock(lockKey);
        lock2.forceUnlock();

        // Then
        assertThat(lock1.isLocked()).isFalse();

        // New acquisition succeeds
        RLock lock3 = redissonClient.getLock(lockKey);
        assertThat(lock3.tryLock()).isTrue();
        lock3.unlock();
    }
}
