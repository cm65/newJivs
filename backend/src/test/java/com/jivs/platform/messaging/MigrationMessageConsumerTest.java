package com.jivs.platform.messaging;

import com.jivs.platform.service.migration.MigrationOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MigrationMessageConsumer
 *
 * CRITICAL: Verifies idempotent message processing
 * - Tests duplicate message detection
 * - Validates idempotency key handling
 * - Tests retry on failure
 * - Verifies key expiration
 */
@ExtendWith(MockitoExtension.class)
class MigrationMessageConsumerTest {

    @Mock
    private MigrationOrchestrator orchestrator;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private MigrationMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // =====================================
    // Planning Message Tests
    // =====================================

    @Test
    void handlePlanningMessage_FirstTime_ProcessesSuccessfully() {
        // Given
        Map<String, Object> message = createPlanningMessage(1L, "planning-key-123");
        when(valueOps.setIfAbsent(
            eq("migration:processed:planning-key-123"),
            eq("true"),
            eq(24L),
            eq(TimeUnit.HOURS)
        )).thenReturn(true); // First time seeing this key

        doNothing().when(orchestrator).executeMigration(1L);

        // When
        consumer.handlePlanningMessage(message);

        // Then
        verify(orchestrator).executeMigration(1L);
        verify(redisTemplate, never()).delete(anyString()); // Success - key stays
    }

    @Test
    void handlePlanningMessage_Duplicate_SkipsProcessing() {
        // Given
        Map<String, Object> message = createPlanningMessage(1L, "planning-key-123");
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(false); // Already processed

        // When
        consumer.handlePlanningMessage(message);

        // Then
        verify(orchestrator, never()).executeMigration(anyLong());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void handlePlanningMessage_ExecutionFails_RemovesKeyAndRethrows() {
        // Given
        Map<String, Object> message = createPlanningMessage(1L, "planning-key-123");
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        doThrow(new RuntimeException("Execution service unavailable"))
            .when(orchestrator).executeMigration(1L);

        // When/Then
        assertThatThrownBy(() -> consumer.handlePlanningMessage(message))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Migration planning failed");

        // Verify key removed to allow retry
        verify(redisTemplate).delete("migration:processed:planning-key-123");
    }

    @Test
    void handlePlanningMessage_IntegerMigrationId_HandlesCorrectly() {
        // Given - message with Integer instead of Long
        Map<String, Object> message = new HashMap<>();
        message.put("migrationId", 1); // Integer
        message.put("idempotencyKey", "planning-key-123");

        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
        doNothing().when(orchestrator).executeMigration(1L);

        // When
        consumer.handlePlanningMessage(message);

        // Then
        verify(orchestrator).executeMigration(1L);
    }

    @Test
    void handlePlanningMessage_LongMigrationId_HandlesCorrectly() {
        // Given - message with Long
        Map<String, Object> message = new HashMap<>();
        message.put("migrationId", 1L); // Long
        message.put("idempotencyKey", "planning-key-123");

        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
        doNothing().when(orchestrator).executeMigration(1L);

        // When
        consumer.handlePlanningMessage(message);

        // Then
        verify(orchestrator).executeMigration(1L);
    }

    // =====================================
    // Execution Message Tests
    // =====================================

    @Test
    void handleExecutionMessage_FirstTime_ProcessesSuccessfully() {
        // Given
        Map<String, Object> message = createExecutionMessage(1L, "extraction", "exec-key-456");
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        doNothing().when(orchestrator).executePhase(1L, "extraction");

        // When
        consumer.handleExecutionMessage(message);

        // Then
        verify(orchestrator).executePhase(1L, "extraction");
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void handleExecutionMessage_Duplicate_SkipsProcessing() {
        // Given
        Map<String, Object> message = createExecutionMessage(1L, "extraction", "exec-key-456");
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(false);

        // When
        consumer.handleExecutionMessage(message);

        // Then
        verify(orchestrator, never()).executePhase(anyLong(), anyString());
    }

    @Test
    void handleExecutionMessage_ExecutionFails_RemovesKeyAndRethrows() {
        // Given
        Map<String, Object> message = createExecutionMessage(1L, "transformation", "exec-key-789");
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        doThrow(new RuntimeException("Transformation service down"))
            .when(orchestrator).executePhase(1L, "transformation");

        // When/Then
        assertThatThrownBy(() -> consumer.handleExecutionMessage(message))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Phase execution failed");

        verify(redisTemplate).delete("migration:processed:exec-key-789");
    }

    @Test
    void handleExecutionMessage_DifferentPhases_ProcessedIndependently() {
        // Given
        Map<String, Object> message1 = createExecutionMessage(1L, "extraction", "exec-key-1");
        Map<String, Object> message2 = createExecutionMessage(1L, "transformation", "exec-key-2");

        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        doNothing().when(orchestrator).executePhase(anyLong(), anyString());

        // When
        consumer.handleExecutionMessage(message1);
        consumer.handleExecutionMessage(message2);

        // Then
        verify(orchestrator).executePhase(1L, "extraction");
        verify(orchestrator).executePhase(1L, "transformation");
    }

    // =====================================
    // Cancellation Message Tests
    // =====================================

    @Test
    void handleCancellationMessage_FirstTime_ProcessesSuccessfully() {
        // Given
        Map<String, Object> message = createCancellationMessage(1L, "cancel-key-999");
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        doNothing().when(orchestrator).cancelMigration(1L);

        // When
        consumer.handleCancellationMessage(message);

        // Then
        verify(orchestrator).cancelMigration(1L);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void handleCancellationMessage_Duplicate_SkipsProcessing() {
        // Given
        Map<String, Object> message = createCancellationMessage(1L, "cancel-key-999");
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(false);

        // When
        consumer.handleCancellationMessage(message);

        // Then
        verify(orchestrator, never()).cancelMigration(anyLong());
    }

    @Test
    void handleCancellationMessage_CancellationFails_RemovesKeyAndRethrows() {
        // Given
        Map<String, Object> message = createCancellationMessage(1L, "cancel-key-888");
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        doThrow(new RuntimeException("Cannot cancel - already completed"))
            .when(orchestrator).cancelMigration(1L);

        // When/Then
        assertThatThrownBy(() -> consumer.handleCancellationMessage(message))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Migration cancellation failed");

        verify(redisTemplate).delete("migration:processed:cancel-key-888");
    }

    // =====================================
    // Idempotency Key Tests
    // =====================================

    @Test
    void idempotencyKey_TTL_SetTo24Hours() {
        // Given
        Map<String, Object> message = createPlanningMessage(1L, "key-with-ttl");
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        // When
        consumer.handlePlanningMessage(message);

        // Then
        verify(valueOps).setIfAbsent(
            eq("migration:processed:key-with-ttl"),
            eq("true"),
            eq(24L),
            eq(TimeUnit.HOURS)
        );
    }

    @Test
    void idempotencyKey_UniquePerMessage_NoCrossContamination() {
        // Given
        Map<String, Object> message1 = createPlanningMessage(1L, "unique-key-1");
        Map<String, Object> message2 = createPlanningMessage(2L, "unique-key-2");

        when(valueOps.setIfAbsent(
            eq("migration:processed:unique-key-1"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);
        when(valueOps.setIfAbsent(
            eq("migration:processed:unique-key-2"), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        // When
        consumer.handlePlanningMessage(message1);
        consumer.handlePlanningMessage(message2);

        // Then
        verify(orchestrator).executeMigration(1L);
        verify(orchestrator).executeMigration(2L);
    }

    @Test
    void idempotencyKey_SameKeyDifferentMessageTypes_ProcessedIndependently() {
        // Given - same migration ID and key but different message types
        String sharedKey = "shared-idempotency-key";
        Map<String, Object> planningMsg = createPlanningMessage(1L, sharedKey);
        Map<String, Object> executionMsg = createExecutionMessage(1L, "extraction", sharedKey);

        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true);

        // When
        consumer.handlePlanningMessage(planningMsg);
        consumer.handleExecutionMessage(executionMsg);

        // Then - both processed because they use same Redis key prefix
        verify(orchestrator).executeMigration(1L);
        verify(orchestrator).executePhase(1L, "extraction");
    }

    // =====================================
    // Error Handling Tests
    // =====================================

    @Test
    void handleMessage_RedisUnavailable_AllowsRetry() {
        // Given
        Map<String, Object> message = createPlanningMessage(1L, "redis-down-key");
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenThrow(new RuntimeException("Redis connection failed"));

        // When/Then - exception propagates, message will be requeued
        assertThatThrownBy(() -> consumer.handlePlanningMessage(message))
            .isInstanceOf(RuntimeException.class);

        verify(orchestrator, never()).executeMigration(anyLong());
    }

    @Test
    void handleMessage_NullIdempotencyKey_HandlesGracefully() {
        // Given
        Map<String, Object> message = new HashMap<>();
        message.put("migrationId", 1L);
        message.put("idempotencyKey", null);

        // When/Then - will throw NullPointerException which triggers message requeue
        assertThatThrownBy(() -> consumer.handlePlanningMessage(message))
            .isInstanceOf(NullPointerException.class);
    }

    // =====================================
    // Concurrent Message Tests
    // =====================================

    @Test
    void handleMessage_ConcurrentDuplicates_OnlyOneProcesses() {
        // Given - simulate two threads processing same message
        Map<String, Object> message1 = createPlanningMessage(1L, "concurrent-key");
        Map<String, Object> message2 = createPlanningMessage(1L, "concurrent-key");

        // First call returns true (acquired), second returns false (already acquired)
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(true)
            .thenReturn(false);

        // When
        consumer.handlePlanningMessage(message1);
        consumer.handlePlanningMessage(message2);

        // Then - only first call processes
        verify(orchestrator, times(1)).executeMigration(1L);
    }

    // =====================================
    // Helper Methods
    // =====================================

    private Map<String, Object> createPlanningMessage(Long migrationId, String idempotencyKey) {
        Map<String, Object> message = new HashMap<>();
        message.put("migrationId", migrationId);
        message.put("idempotencyKey", idempotencyKey);
        return message;
    }

    private Map<String, Object> createExecutionMessage(Long migrationId, String phase, String idempotencyKey) {
        Map<String, Object> message = new HashMap<>();
        message.put("migrationId", migrationId);
        message.put("phase", phase);
        message.put("idempotencyKey", idempotencyKey);
        return message;
    }

    private Map<String, Object> createCancellationMessage(Long migrationId, String idempotencyKey) {
        Map<String, Object> message = new HashMap<>();
        message.put("migrationId", migrationId);
        message.put("idempotencyKey", idempotencyKey);
        return message;
    }
}
