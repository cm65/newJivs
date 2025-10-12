package com.jivs.platform.event;

import com.jivs.platform.dto.websocket.StatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for migration job real-time events
 * Sends WebSocket messages to subscribed clients
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MigrationEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String MIGRATION_TOPIC = "/topic/migrations";

    /**
     * Publish migration status changed event
     */
    public void publishStatusChanged(String migrationId, String status, String message) {
        StatusUpdateEvent event = StatusUpdateEvent.statusChanged("migration", migrationId, status, message);
        publishEvent(event);
        log.debug("Published migration status changed: id={}, status={}", migrationId, status);
    }

    /**
     * Publish migration progress update
     */
    public void publishProgressUpdate(String migrationId, Integer progress, Long recordsProcessed,
                                       Long totalRecords, String phase) {
        StatusUpdateEvent event = StatusUpdateEvent.progressUpdated(
                "migration", migrationId, progress, recordsProcessed, totalRecords
        );
        event.setPhase(phase);
        publishEvent(event);
        log.debug("Published migration progress: id={}, phase={}, progress={}%", migrationId, phase, progress);
    }

    /**
     * Publish migration phase changed event
     */
    public void publishPhaseChanged(String migrationId, String phase) {
        StatusUpdateEvent event = StatusUpdateEvent.builder()
                .eventType("phase_changed")
                .entityType("migration")
                .entityId(migrationId)
                .phase(phase)
                .message("Migration phase: " + phase)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        publishEvent(event);
        log.info("Published migration phase changed: id={}, phase={}", migrationId, phase);
    }

    /**
     * Publish migration completed event
     */
    public void publishCompleted(String migrationId, Long recordsMigrated) {
        StatusUpdateEvent event = StatusUpdateEvent.completed("migration", migrationId, recordsMigrated);
        publishEvent(event);
        log.info("Published migration completed: id={}, records={}", migrationId, recordsMigrated);
    }

    /**
     * Publish migration failed event
     */
    public void publishFailed(String migrationId, String errorMessage) {
        StatusUpdateEvent event = StatusUpdateEvent.failed("migration", migrationId, errorMessage);
        publishEvent(event);
        log.warn("Published migration failed: id={}, error={}", migrationId, errorMessage);
    }

    /**
     * Publish migration started event
     */
    public void publishStarted(String migrationId) {
        StatusUpdateEvent event = StatusUpdateEvent.statusChanged("migration", migrationId, "RUNNING", "Migration started");
        publishEvent(event);
        log.info("Published migration started: id={}", migrationId);
    }

    /**
     * Publish migration paused event
     */
    public void publishPaused(String migrationId) {
        StatusUpdateEvent event = StatusUpdateEvent.statusChanged("migration", migrationId, "PAUSED", "Migration paused");
        publishEvent(event);
        log.info("Published migration paused: id={}", migrationId);
    }

    /**
     * Publish migration resumed event
     */
    public void publishResumed(String migrationId) {
        StatusUpdateEvent event = StatusUpdateEvent.statusChanged("migration", migrationId, "RUNNING", "Migration resumed");
        publishEvent(event);
        log.info("Published migration resumed: id={}", migrationId);
    }

    /**
     * Publish migration rollback started event
     */
    public void publishRollbackStarted(String migrationId) {
        StatusUpdateEvent event = StatusUpdateEvent.statusChanged("migration", migrationId, "ROLLING_BACK", "Migration rollback started");
        publishEvent(event);
        log.info("Published migration rollback started: id={}", migrationId);
    }

    /**
     * Publish event to WebSocket topic
     */
    private void publishEvent(StatusUpdateEvent event) {
        try {
            messagingTemplate.convertAndSend(MIGRATION_TOPIC, event);
        } catch (Exception e) {
            log.error("Failed to publish migration event: {}", event, e);
        }
    }
}
