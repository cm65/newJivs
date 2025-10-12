package com.jivs.platform.event;

import com.jivs.platform.dto.websocket.StatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher for extraction job real-time events
 * Sends WebSocket messages to subscribed clients
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExtractionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String EXTRACTION_TOPIC = "/topic/extractions";

    /**
     * Publish extraction status changed event
     */
    public void publishStatusChanged(String jobId, String status, String message) {
        StatusUpdateEvent event = StatusUpdateEvent.statusChanged("extraction", jobId, status, message);
        publishEvent(event);
        log.debug("Published extraction status changed: jobId={}, status={}", jobId, status);
    }

    /**
     * Publish extraction progress update
     */
    public void publishProgressUpdate(String jobId, Integer progress, Long recordsProcessed, Long totalRecords) {
        StatusUpdateEvent event = StatusUpdateEvent.progressUpdated(
                "extraction", jobId, progress, recordsProcessed, totalRecords
        );
        publishEvent(event);
        log.debug("Published extraction progress: jobId={}, progress={}%", jobId, progress);
    }

    /**
     * Publish extraction completed event
     */
    public void publishCompleted(String jobId, Long recordsExtracted) {
        StatusUpdateEvent event = StatusUpdateEvent.completed("extraction", jobId, recordsExtracted);
        publishEvent(event);
        log.info("Published extraction completed: jobId={}, records={}", jobId, recordsExtracted);
    }

    /**
     * Publish extraction failed event
     */
    public void publishFailed(String jobId, String errorMessage) {
        StatusUpdateEvent event = StatusUpdateEvent.failed("extraction", jobId, errorMessage);
        publishEvent(event);
        log.warn("Published extraction failed: jobId={}, error={}", jobId, errorMessage);
    }

    /**
     * Publish extraction started event
     */
    public void publishStarted(String jobId) {
        StatusUpdateEvent event = StatusUpdateEvent.statusChanged("extraction", jobId, "RUNNING", "Extraction started");
        publishEvent(event);
        log.info("Published extraction started: jobId={}", jobId);
    }

    /**
     * Publish extraction cancelled event
     */
    public void publishCancelled(String jobId) {
        StatusUpdateEvent event = StatusUpdateEvent.statusChanged("extraction", jobId, "CANCELLED", "Extraction cancelled");
        publishEvent(event);
        log.info("Published extraction cancelled: jobId={}", jobId);
    }

    /**
     * Publish event to WebSocket topic
     */
    private void publishEvent(StatusUpdateEvent event) {
        try {
            messagingTemplate.convertAndSend(EXTRACTION_TOPIC, event);
        } catch (Exception e) {
            log.error("Failed to publish extraction event: {}", event, e);
        }
    }
}
