package com.jivs.platform.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Base event class for cross-module communication
 * Published to Kafka for async processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformEvent {

    /**
     * Unique event ID for tracking
     */
    private String eventId;

    /**
     * Event type (e.g., "DOCUMENT_UPLOADED", "EXTRACTION_STARTED")
     */
    private String eventType;

    /**
     * Source module that published the event
     */
    private EventSource source;

    /**
     * Entity ID related to this event
     */
    private Long entityId;

    /**
     * Entity type (e.g., "Document", "Extraction", "Migration")
     */
    private String entityType;

    /**
     * Timestamp when event was created
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Event payload (flexible for different event types)
     */
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();

    /**
     * User who triggered the event (if applicable)
     */
    private String userId;

    /**
     * Correlation ID for tracing related events
     */
    private String correlationId;

    public enum EventSource {
        DOCUMENT,
        EXTRACTION,
        MIGRATION,
        WORKFLOW,
        COMPLIANCE,
        QUALITY
    }
}
