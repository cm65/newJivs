package com.jivs.platform.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * Consumer for platform events from Kafka
 * Processes cross-module events and triggers appropriate actions
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlatformEventConsumer {

    /**
     * Listen to platform events topic
     */
    @KafkaListener(topics = "jivs-platform-events", groupId = "jivs-platform")
    public void consumeEvent(PlatformEvent event, Acknowledgment acknowledgment) {
        try {
            log.debug("Received event: {} for entity {}:{}", 
                    event.getEventType(), event.getEntityType(), event.getEntityId());

            // Route event to appropriate handler based on source
            switch (event.getSource()) {
                case DOCUMENT:
                    handleDocumentEvent(event);
                    break;
                case EXTRACTION:
                    handleExtractionEvent(event);
                    break;
                case MIGRATION:
                    handleMigrationEvent(event);
                    break;
                case WORKFLOW:
                    handleWorkflowEvent(event);
                    break;
                case QUALITY:
                    handleQualityEvent(event);
                    break;
                case COMPLIANCE:
                    handleComplianceEvent(event);
                    break;
                default:
                    log.warn("Unknown event source: {}", event.getSource());
            }

            // Manually acknowledge message after successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing event: {}", event.getEventType(), e);
            // Don't acknowledge - message will be retried
        }
    }

    private void handleDocumentEvent(PlatformEvent event) {
        log.debug("Handling document event: {}", event.getEventType());
        
        switch (event.getEventType()) {
            case "DOCUMENT_UPLOADED":
                // Trigger data quality checks
                // Update search index
                break;
            case "DOCUMENT_ARCHIVED":
                // Update retention policies
                break;
            case "DOCUMENT_DELETED":
                // Clean up references
                break;
            default:
                log.debug("Unhandled document event: {}", event.getEventType());
        }
    }

    private void handleExtractionEvent(PlatformEvent event) {
        log.debug("Handling extraction event: {}", event.getEventType());
        
        switch (event.getEventType()) {
            case "EXTRACTION_COMPLETED":
                // Trigger migration workflow
                // Update metrics
                break;
            case "EXTRACTION_FAILED":
                // Send notifications
                // Update failure metrics
                break;
            default:
                log.debug("Unhandled extraction event: {}", event.getEventType());
        }
    }

    private void handleMigrationEvent(PlatformEvent event) {
        log.debug("Handling migration event: {}", event.getEventType());
        
        switch (event.getEventType()) {
            case "MIGRATION_COMPLETED":
                // Trigger data quality checks
                // Archive source data
                break;
            case "MIGRATION_FAILED":
                // Trigger rollback if needed
                // Send notifications
                break;
            case "MIGRATION_ROLLED_BACK":
                // Clean up target data
                // Restore source data
                break;
            default:
                log.debug("Unhandled migration event: {}", event.getEventType());
        }
    }

    private void handleWorkflowEvent(PlatformEvent event) {
        log.debug("Handling workflow event: {}", event.getEventType());
        
        switch (event.getEventType()) {
            case "WORKFLOW_COMPLETED":
                // Update workflow metrics
                break;
            case "WORKFLOW_FAILED":
                // Trigger error handling
                break;
            default:
                log.debug("Unhandled workflow event: {}", event.getEventType());
        }
    }

    private void handleQualityEvent(PlatformEvent event) {
        log.debug("Handling quality event: {}", event.getEventType());
    }

    private void handleComplianceEvent(PlatformEvent event) {
        log.debug("Handling compliance event: {}", event.getEventType());
    }
}
