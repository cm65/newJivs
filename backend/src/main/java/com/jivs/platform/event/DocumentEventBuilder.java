package com.jivs.platform.event;

import com.jivs.platform.domain.Document;

import java.util.UUID;

/**
 * Builder for Document module events
 */
public class DocumentEventBuilder {

    public static PlatformEvent documentUploaded(Document document, String userId) {
        return PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DOCUMENT_UPLOADED")
                .source(PlatformEvent.EventSource.DOCUMENT)
                .entityId(document.getId())
                .entityType("Document")
                .userId(userId)
                .build();
    }

    public static PlatformEvent documentArchived(Long documentId, String userId) {
        return PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DOCUMENT_ARCHIVED")
                .source(PlatformEvent.EventSource.DOCUMENT)
                .entityId(documentId)
                .entityType("Document")
                .userId(userId)
                .build();}

    public static PlatformEvent documentDeleted(Long documentId, String userId) {
        return PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DOCUMENT_DELETED")
                .source(PlatformEvent.EventSource.DOCUMENT)
                .entityId(documentId)
                .entityType("Document")
                .userId(userId)
                .build();
    }

    public static PlatformEvent documentMetadataUpdated(Long documentId, String userId) {
        return PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DOCUMENT_METADATA_UPDATED")
                .source(PlatformEvent.EventSource.DOCUMENT)
                .entityId(documentId)
                .entityType("Document")
                .userId(userId)
                .build();
    }
}
