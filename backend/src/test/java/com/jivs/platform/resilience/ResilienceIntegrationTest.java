package com.jivs.platform.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Resilience4j patterns
 *
 * CRITICAL: Verifies retry, circuit breaker, and timeout logic
 * - Tests exponential backoff retry
 * - Validates circuit breaker state transitions
 * - Tests timeout enforcement
 * - Verifies exception handling
 */
@SpringBootTest
@ActiveProfiles("test")
class ResilienceIntegrationTest {

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private TimeLimiterRegistry timeLimiterRegistry;

    private Retry migrationRetry;
    private CircuitBreaker loadCircuitBreaker;
    private CircuitBreaker extractionCircuitBreaker;
    private TimeLimiter migrationTimeLimiter;

    @BeforeEach
    void setUp() {
        migrationRetry = retryRegistry.retry("migration-operations");
        loadCircuitBreaker = circuitBreakerRegistry.circuitBreaker("migration-load");
        extractionCircuitBreaker = circuitBreakerRegistry.circuitBreaker("extraction-service");
        migrationTimeLimiter = timeLimiterRegistry.timeLimiter("migration-timeout");

        // Reset circuit breakers
        loadCircuitBreaker.reset();
        extractionCircuitBreaker.reset();
    }

    @AfterEach
    void tearDown() {
        // Clean up
        loadCircuitBreaker.reset();
        extractionCircuitBreaker.reset();
    }

    // =====================================
    // Retry Tests
    // =====================================

    @Test
    void retry_TransientFailure_EventuallySucceeds() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException(new IOException("Connection timeout"));
            }
            return "Success";
        };

        // When
        Supplier<String> decorated = Retry.decorateSupplier(migrationRetry, operation);
        String result = decorated.get();

        // Then
        assertThat(result).isEqualTo("Success");
        assertThat(attemptCount.get()).isEqualTo(3);
    }

    @Test
    void retry_PermanentFailure_ExhaustsRetries() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            attemptCount.incrementAndGet();
            throw new RuntimeException(new SQLException("Database unavailable"));
        };

        // When/Then
        Supplier<String> decorated = Retry.decorateSupplier(migrationRetry, operation);
        assertThatThrownBy(decorated::get)
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(SQLException.class);

        assertThat(attemptCount.get()).isEqualTo(3); // Max attempts
    }

    @Test
    void retry_NonRetryableException_FailsImmediately() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            attemptCount.incrementAndGet();
            throw new IllegalArgumentException("Invalid migration ID");
        };

        // When/Then
        Supplier<String> decorated = Retry.decorateSupplier(migrationRetry, operation);
        assertThatThrownBy(decorated::get)
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(attemptCount.get()).isEqualTo(1); // No retry
    }

    @Test
    void retry_ExponentialBackoff_IncreasesWaitTime() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        long[] attemptTimes = new long[3];

        Supplier<String> operation = () -> {
            int attempt = attemptCount.getAndIncrement();
            attemptTimes[attempt] = System.currentTimeMillis();
            throw new RuntimeException(new IOException("Timeout"));
        };

        // When
        Supplier<String> decorated = Retry.decorateSupplier(migrationRetry, operation);
        assertThatThrownBy(decorated::get);

        // Then - verify exponential backoff (2s, 4s intervals)
        long firstDelay = attemptTimes[1] - attemptTimes[0];
        long secondDelay = attemptTimes[2] - attemptTimes[1];

        assertThat(firstDelay).isGreaterThanOrEqualTo(2000); // 2 seconds
        assertThat(secondDelay).isGreaterThanOrEqualTo(4000); // 4 seconds (2s * 2)
    }

    // =====================================
    // Circuit Breaker Tests
    // =====================================

    @Test
    void circuitBreaker_HighFailureRate_OpensCircuit() {
        // Given - need minimum 5 calls to calculate failure rate
        Supplier<String> failingOperation = () -> {
            throw new RuntimeException(new SQLException("Database error"));
        };

        Supplier<String> decorated = CircuitBreaker.decorateSupplier(
            loadCircuitBreaker, failingOperation);

        // When - execute 10 failing calls
        for (int i = 0; i < 10; i++) {
            try {
                decorated.get();
            } catch (Exception ignored) {
            }
        }

        // Then - circuit should be OPEN (failure rate > 50%)
        assertThat(loadCircuitBreaker.getState())
            .isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.FORCED_OPEN);
    }

    @Test
    void circuitBreaker_OpenState_RejectsCalls() {
        // Given - force circuit open
        loadCircuitBreaker.transitionToOpenState();

        Supplier<String> operation = () -> "Should not execute";
        Supplier<String> decorated = CircuitBreaker.decorateSupplier(
            loadCircuitBreaker, operation);

        // When/Then - calls rejected immediately
        assertThatThrownBy(decorated::get)
            .isInstanceOf(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class);
    }

    @Test
    void circuitBreaker_HalfOpenState_AllowsTestCalls() throws InterruptedException {
        // Given - open circuit
        loadCircuitBreaker.transitionToOpenState();
        assertThat(loadCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Wait for circuit to transition to HALF_OPEN
        // (In real config: waitDurationInOpenState = 60s, but for testing we force it)
        loadCircuitBreaker.transitionToHalfOpenState();

        // When - successful call in HALF_OPEN
        Supplier<String> operation = () -> "Success";
        Supplier<String> decorated = CircuitBreaker.decorateSupplier(
            loadCircuitBreaker, operation);

        // Execute permitted test calls (3 configured)
        for (int i = 0; i < 3; i++) {
            String result = decorated.get();
            assertThat(result).isEqualTo("Success");
        }

        // Then - circuit closes after successful test calls
        assertThat(loadCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void circuitBreaker_IgnoredException_DoesNotCountAsFailure() {
        // Given
        AtomicInteger callCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            if (callCount.incrementAndGet() <= 10) {
                throw new IllegalArgumentException("Invalid input");
            }
            return "Success";
        };

        Supplier<String> decorated = CircuitBreaker.decorateSupplier(
            loadCircuitBreaker, operation);

        // When - 10 ignored exceptions
        for (int i = 0; i < 10; i++) {
            try {
                decorated.get();
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Then - circuit remains CLOSED (ignored exceptions don't count)
        assertThat(loadCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void circuitBreaker_SlowCalls_OpensCircuit() throws Exception {
        // Given - slow call threshold is 30 seconds
        Supplier<String> slowOperation = () -> {
            try {
                Thread.sleep(31000); // Exceed slow call threshold
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Success";
        };

        Supplier<String> decorated = CircuitBreaker.decorateSupplier(
            loadCircuitBreaker, slowOperation);

        // When - execute slow calls
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    decorated.get();
                } catch (Exception ignored) {
                }
            });
        }

        Thread.sleep(35000); // Wait for slow calls to complete

        // Then - circuit opens due to slow call rate > 50%
        assertThat(loadCircuitBreaker.getState())
            .isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.FORCED_OPEN);

        executor.shutdownNow();
    }

    // =====================================
    // Time Limiter Tests
    // =====================================

    @Test
    void timeLimiter_OperationWithinTimeout_Succeeds() throws Exception {
        // Given
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000); // 1 second (within 30 min timeout)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Success";
        });

        // When
        Supplier<CompletableFuture<String>> supplier = () -> future;
        Callable<String> decorated = TimeLimiter.decorateFutureSupplier(
            migrationTimeLimiter, supplier);

        String result = decorated.call();

        // Then
        assertThat(result).isEqualTo("Success");
    }

    @Test
    void timeLimiter_OperationExceedsTimeout_Throws() {
        // Given - create time limiter with short timeout for testing
        TimeLimiter shortTimeLimiter = timeLimiterRegistry.timeLimiter("test-timeout",
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(2))
                .cancelRunningFuture(true)
                .build());

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(5000); // 5 seconds (exceeds timeout)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted");
            }
            return "Should not complete";
        });

        // When/Then
        Supplier<CompletableFuture<String>> supplier = () -> future;
        Callable<String> decorated = TimeLimiter.decorateFutureSupplier(
            shortTimeLimiter, supplier);

        assertThatThrownBy(decorated::call)
            .isInstanceOf(TimeoutException.class);
    }

    @Test
    void timeLimiter_TimeoutWithCancellation_CancelsFuture() throws Exception {
        // Given
        TimeLimiter shortTimeLimiter = timeLimiterRegistry.timeLimiter("test-cancel",
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(1))
                .cancelRunningFuture(true)
                .build());

        AtomicInteger executionCount = new AtomicInteger(0);
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            executionCount.incrementAndGet();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Cancelled");
            }
            return "Should not complete";
        });

        // When
        Supplier<CompletableFuture<String>> supplier = () -> future;
        Callable<String> decorated = TimeLimiter.decorateFutureSupplier(
            shortTimeLimiter, supplier);

        try {
            decorated.call();
        } catch (TimeoutException ignored) {
        }

        Thread.sleep(1500);

        // Then - future should be cancelled
        assertThat(future.isCancelled()).isTrue();
        assertThat(executionCount.get()).isEqualTo(1);
    }

    // =====================================
    // Combined Patterns Tests
    // =====================================

    @Test
    void combinedPattern_RetryWithCircuitBreaker_WorksTogether() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new RuntimeException(new IOException("Connection timeout"));
            }
            return "Success";
        };

        // When - decorate with both retry and circuit breaker
        Supplier<String> withRetry = Retry.decorateSupplier(migrationRetry, operation);
        Supplier<String> withCircuitBreaker = CircuitBreaker.decorateSupplier(
            loadCircuitBreaker, withRetry);

        String result = withCircuitBreaker.get();

        // Then
        assertThat(result).isEqualTo("Success");
        assertThat(attemptCount.get()).isEqualTo(2);
        assertThat(loadCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void combinedPattern_RetryExhausted_CircuitBreakerRecordsFailure() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            attemptCount.incrementAndGet();
            throw new RuntimeException(new SQLException("Database unavailable"));
        };

        // When - all retries fail
        Supplier<String> withRetry = Retry.decorateSupplier(migrationRetry, operation);
        Supplier<String> withCircuitBreaker = CircuitBreaker.decorateSupplier(
            loadCircuitBreaker, withRetry);

        assertThatThrownBy(withCircuitBreaker::get);

        // Then
        assertThat(attemptCount.get()).isEqualTo(3); // Retry attempts
        // Circuit breaker recorded the failure
        assertThat(loadCircuitBreaker.getMetrics().getNumberOfFailedCalls()).isGreaterThan(0);
    }
}
