package com.jivs.platform.event;

import com.jivs.platform.domain.extraction.ExtractionJob;

import java.util.UUID;

/**
 * Builder for Extraction module events
 */
public class ExtractionEventBuilder {

    public static PlatformEvent extractionStarted(ExtractionJob job, String userId) {
        return PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("EXTRACTION_STARTED")
                .source(PlatformEvent.EventSource.EXTRACTION)
                .entityId(job.getId())
                .entityType("ExtractionJob")
                .userId(userId)
                .build();
    }

    public static PlatformEvent extractionCompleted(ExtractionJob job, String userId) {
        return PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("EXTRACTION_COMPLETED")
                .source(PlatformEvent.EventSource.EXTRACTION)
                .entityId(job.getId())
                .entityType("ExtractionJob")
                .userId(userId)
                .build();
    }

    public static PlatformEvent extractionFailed(Long jobId, String errorMessage, String userId) {
        PlatformEvent event = PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("EXTRACTION_FAILED")
                .source(PlatformEvent.EventSource.EXTRACTION)
                .entityId(jobId)
                .entityType("ExtractionJob")
                .userId(userId)
                .build();
        
        event.getPayload().put("errorMessage", errorMessage);
        return event;
    }

    public static PlatformEvent extractionProgress(Long jobId, int progress, String userId) {
        PlatformEvent event = PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("EXTRACTION_PROGRESS")
                .source(PlatformEvent.EventSource.EXTRACTION)
                .entityId(jobId)
                .entityType("ExtractionJob")
                .userId(userId)
                .build();
        
        event.getPayload().put("progress", progress);
        return event;
    }
}
