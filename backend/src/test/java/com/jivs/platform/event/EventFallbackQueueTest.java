package com.jivs.platform.event;

import com.jivs.platform.dto.websocket.StatusUpdateEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for EventFallbackQueue
 *
 * CRITICAL: Verifies event retry mechanism
 * - Tests event enqueueing on failure
 * - Validates retry scheduling
 * - Tests successful event recovery
 * - Verifies max retry limit
 * - Tests event expiration
 */
@SpringBootTest
@ActiveProfiles("test")
class EventFallbackQueueTest {

    @Autowired
    private EventFallbackQueue fallbackQueue;

    @MockBean
    private RedisTemplate<String, StatusUpdateEvent> eventRedisTemplate;

    @MockBean
    private MigrationEventPublisher eventPublisher;

    private ValueOperations<String, StatusUpdateEvent> valueOps;
    private SetOperations<String, String> setOps;
    private ValueOperations<String, String> stringValueOps;

    @BeforeEach
    void setUp() {
        valueOps = mock(ValueOperations.class);
        setOps = mock(SetOperations.class);
        stringValueOps = mock(ValueOperations.class);

        when(eventRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(eventRedisTemplate.opsForSet()).thenReturn(setOps);

        // For retry count tracking
        RedisTemplate<String, String> stringRedis = mock(RedisTemplate.class);
        when(stringRedis.opsForValue()).thenReturn(stringValueOps);
    }

    @AfterEach
    void tearDown() {
        reset(eventRedisTemplate, eventPublisher);
    }

    @Test
    void enqueue_NewEvent_StoresInRedis() {
        // Given
        StatusUpdateEvent event = createTestEvent(1L, "MIGRATION_STARTED");

        // When
        fallbackQueue.enqueue(event);

        // Then
        verify(valueOps).set(
            argThat(key -> key.startsWith("migration:fallback:event:")),
            eq(event),
            eq(Duration.ofHours(24))
        );

        verify(setOps).add(
            eq("migration:fallback:events"),
            argThat(key -> key.startsWith("migration:fallback:event:"))
        );
    }

    @Test
    void enqueue_MultipleEvents_StoresAllWithUniqueKeys() {
        // Given
        StatusUpdateEvent event1 = createTestEvent(1L, "MIGRATION_STARTED");
        StatusUpdateEvent event2 = createTestEvent(2L, "MIGRATION_COMPLETED");

        // When
        fallbackQueue.enqueue(event1);
        fallbackQueue.enqueue(event2);

        // Then
        verify(valueOps, times(2)).set(
            argThat(key -> key.startsWith("migration:fallback:event:")),
            any(StatusUpdateEvent.class),
            eq(Duration.ofHours(24))
        );

        verify(setOps, times(2)).add(
            eq("migration:fallback:events"),
            anyString()
        );
    }

    @Test
    void retryFailedEvents_EmptyQueue_DoesNothing() {
        // Given
        when(setOps.members("migration:fallback:events")).thenReturn(Set.of());

        // When
        fallbackQueue.retryFailedEvents();

        // Then
        verify(eventPublisher, never()).publishEventDirect(any());
    }

    @Test
    void retryFailedEvents_SuccessfulRetry_RemovesFromQueue() {
        // Given
        String eventKey = "migration:fallback:event:123:1:STARTED";
        StatusUpdateEvent event = createTestEvent(1L, "MIGRATION_STARTED");

        when(setOps.members("migration:fallback:events")).thenReturn(Set.of(eventKey));
        when(valueOps.get(eventKey)).thenReturn(event);
        when(stringValueOps.get(eventKey + ":retryCount")).thenReturn("0");
        doNothing().when(eventPublisher).publishEventDirect(event);

        // When
        fallbackQueue.retryFailedEvents();

        // Then
        verify(eventPublisher).publishEventDirect(event);
        verify(eventRedisTemplate).delete(eventKey);
        verify(setOps).remove("migration:fallback:events", eventKey);
    }

    @Test
    void retryFailedEvents_FailedRetry_IncrementsRetryCount() {
        // Given
        String eventKey = "migration:fallback:event:123:1:STARTED";
        StatusUpdateEvent event = createTestEvent(1L, "MIGRATION_STARTED");

        when(setOps.members("migration:fallback:events")).thenReturn(Set.of(eventKey));
        when(valueOps.get(eventKey)).thenReturn(event);
        when(stringValueOps.get(eventKey + ":retryCount")).thenReturn("0");
        doThrow(new RuntimeException("WebSocket unavailable"))
            .when(eventPublisher).publishEventDirect(event);

        // When
        fallbackQueue.retryFailedEvents();

        // Then
        verify(eventPublisher).publishEventDirect(event);
        verify(stringValueOps).increment(eventKey + ":retryCount");
        verify(eventRedisTemplate).expire(eventKey + ":retryCount", Duration.ofHours(24));

        // Event NOT removed from queue (stays for retry)
        verify(eventRedisTemplate, never()).delete(eventKey);
    }

    @Test
    void retryFailedEvents_MaxRetriesExceeded_RemovesEvent() {
        // Given
        String eventKey = "migration:fallback:event:123:1:STARTED";
        StatusUpdateEvent event = createTestEvent(1L, "MIGRATION_STARTED");

        when(setOps.members("migration:fallback:events")).thenReturn(Set.of(eventKey));
        when(valueOps.get(eventKey)).thenReturn(event);
        when(stringValueOps.get(eventKey + ":retryCount")).thenReturn("10"); // Max retries

        // When
        fallbackQueue.retryFailedEvents();

        // Then
        verify(eventPublisher, never()).publishEventDirect(any()); // Don't try again
        verify(eventRedisTemplate).delete(eventKey);
        verify(setOps).remove("migration:fallback:events", eventKey);
    }

    @Test
    void retryFailedEvents_ExpiredEvent_RemovesFromSet() {
        // Given
        String eventKey = "migration:fallback:event:123:1:STARTED";

        when(setOps.members("migration:fallback:events")).thenReturn(Set.of(eventKey));
        when(valueOps.get(eventKey)).thenReturn(null); // Event expired

        // When
        fallbackQueue.retryFailedEvents();

        // Then
        verify(setOps).remove("migration:fallback:events", eventKey);
        verify(eventPublisher, never()).publishEventDirect(any());
    }

    @Test
    void retryFailedEvents_MultipleEvents_ProcessesAll() {
        // Given
        String eventKey1 = "migration:fallback:event:123:1:STARTED";
        String eventKey2 = "migration:fallback:event:124:2:COMPLETED";

        StatusUpdateEvent event1 = createTestEvent(1L, "MIGRATION_STARTED");
        StatusUpdateEvent event2 = createTestEvent(2L, "MIGRATION_COMPLETED");

        when(setOps.members("migration:fallback:events"))
            .thenReturn(Set.of(eventKey1, eventKey2));
        when(valueOps.get(eventKey1)).thenReturn(event1);
        when(valueOps.get(eventKey2)).thenReturn(event2);
        when(stringValueOps.get(anyString())).thenReturn("0");
        doNothing().when(eventPublisher).publishEventDirect(any());

        // When
        fallbackQueue.retryFailedEvents();

        // Then
        verify(eventPublisher).publishEventDirect(event1);
        verify(eventPublisher).publishEventDirect(event2);
        verify(eventRedisTemplate, times(2)).delete(anyString());
    }

    @Test
    void retryFailedEvents_PartialSuccess_HandlesCorrectly() {
        // Given
        String eventKey1 = "migration:fallback:event:123:1:STARTED";
        String eventKey2 = "migration:fallback:event:124:2:COMPLETED";

        StatusUpdateEvent event1 = createTestEvent(1L, "MIGRATION_STARTED");
        StatusUpdateEvent event2 = createTestEvent(2L, "MIGRATION_COMPLETED");

        when(setOps.members("migration:fallback:events"))
            .thenReturn(Set.of(eventKey1, eventKey2));
        when(valueOps.get(eventKey1)).thenReturn(event1);
        when(valueOps.get(eventKey2)).thenReturn(event2);
        when(stringValueOps.get(anyString())).thenReturn("0");

        // First event succeeds, second fails
        doNothing().when(eventPublisher).publishEventDirect(event1);
        doThrow(new RuntimeException("WebSocket error"))
            .when(eventPublisher).publishEventDirect(event2);

        // When
        fallbackQueue.retryFailedEvents();

        // Then
        verify(eventPublisher).publishEventDirect(event1);
        verify(eventPublisher).publishEventDirect(event2);

        // First event removed, second stays
        verify(eventRedisTemplate).delete(eventKey1);
        verify(setOps).remove("migration:fallback:events", eventKey1);

        verify(stringValueOps).increment(eventKey2 + ":retryCount");
    }

    @Test
    void getPendingCount_EmptyQueue_ReturnsZero() {
        // Given
        when(setOps.size("migration:fallback:events")).thenReturn(0L);

        // When
        long count = fallbackQueue.getPendingCount();

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    void getPendingCount_WithEvents_ReturnsCorrectCount() {
        // Given
        when(setOps.size("migration:fallback:events")).thenReturn(5L);

        // When
        long count = fallbackQueue.getPendingCount();

        // Then
        assertThat(count).isEqualTo(5);
    }

    @Test
    void clearQueue_WithEvents_RemovesAll() {
        // Given
        String eventKey1 = "migration:fallback:event:123:1:STARTED";
        String eventKey2 = "migration:fallback:event:124:2:COMPLETED";

        when(setOps.members("migration:fallback:events"))
            .thenReturn(Set.of(eventKey1, eventKey2));

        // When
        fallbackQueue.clearQueue();

        // Then
        verify(eventRedisTemplate).delete(eventKey1);
        verify(eventRedisTemplate).delete(eventKey2);
        verify(setOps).remove("migration:fallback:events", eventKey1);
        verify(setOps).remove("migration:fallback:events", eventKey2);
        verify(eventRedisTemplate, times(2)).delete(argThat(key -> key.endsWith(":retryCount")));
    }

    @Test
    void clearQueue_EmptyQueue_DoesNotFail() {
        // Given
        when(setOps.members("migration:fallback:events")).thenReturn(Set.of());

        // When
        fallbackQueue.clearQueue();

        // Then
        verify(eventRedisTemplate, never()).delete(anyString());
    }

    @Test
    void enqueue_RedisFailure_DoesNotThrow() {
        // Given
        StatusUpdateEvent event = createTestEvent(1L, "MIGRATION_STARTED");
        doThrow(new RuntimeException("Redis unavailable"))
            .when(valueOps).set(anyString(), any(), any(Duration.class));

        // When/Then - should not throw
        fallbackQueue.enqueue(event);

        // Method completes without exception
    }

    @Test
    void retryFailedEvents_RedisError_ContinuesProcessing() {
        // Given
        when(setOps.members("migration:fallback:events"))
            .thenThrow(new RuntimeException("Redis connection lost"));

        // When/Then - should not throw
        fallbackQueue.retryFailedEvents();

        // Method completes without exception
    }

    // =====================================
    // Helper Methods
    // =====================================

    private StatusUpdateEvent createTestEvent(Long entityId, String eventType) {
        StatusUpdateEvent event = new StatusUpdateEvent();
        event.setEntityId(entityId);
        event.setEventType(eventType);
        event.setMessage("Test event");
        return event;
    }
}
