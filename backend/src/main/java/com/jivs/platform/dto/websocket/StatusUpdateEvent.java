package com.jivs.platform.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket event for real-time status updates
 * Used for extraction and migration progress notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateEvent {

    /**
     * Event type (status_changed, progress_updated, completed, failed)
     */
    private String eventType;

    /**
     * Entity type (extraction, migration, data_quality)
     */
    private String entityType;

    /**
     * Entity ID (job ID)
     */
    private String entityId;

    /**
     * Current status (PENDING, RUNNING, COMPLETED, FAILED, etc.)
     */
    private String status;

    /**
     * Progress percentage (0-100)
     */
    private Integer progress;

    /**
     * Records processed
     */
    private Long recordsProcessed;

    /**
     * Total records
     */
    private Long totalRecords;

    /**
     * Current phase (for migrations)
     */
    private String phase;

    /**
     * Message or error description
     */
    private String message;

    /**
     * Timestamp of the event
     */
    private LocalDateTime timestamp;

    /**
     * Additional metadata
     */
    private Object metadata;

    /**
     * Create a status changed event
     */
    public static StatusUpdateEvent statusChanged(String entityType, String entityId, String status, String message) {
        return StatusUpdateEvent.builder()
                .eventType("status_changed")
                .entityType(entityType)
                .entityId(entityId)
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a progress updated event
     */
    public static StatusUpdateEvent progressUpdated(String entityType, String entityId,
                                                     Integer progress, Long recordsProcessed, Long totalRecords) {
        return StatusUpdateEvent.builder()
                .eventType("progress_updated")
                .entityType(entityType)
                .entityId(entityId)
                .progress(progress)
                .recordsProcessed(recordsProcessed)
                .totalRecords(totalRecords)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a completed event
     */
    public static StatusUpdateEvent completed(String entityType, String entityId, Long recordsProcessed) {
        return StatusUpdateEvent.builder()
                .eventType("completed")
                .entityType(entityType)
                .entityId(entityId)
                .status("COMPLETED")
                .progress(100)
                .recordsProcessed(recordsProcessed)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create a failed event
     */
    public static StatusUpdateEvent failed(String entityType, String entityId, String errorMessage) {
        return StatusUpdateEvent.builder()
                .eventType("failed")
                .entityType(entityType)
                .entityId(entityId)
                .status("FAILED")
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
