package com.jivs.platform.event;

import com.jivs.platform.domain.migration.Migration;
import com.jivs.platform.domain.migration.MigrationPhase;

import java.util.UUID;

/**
 * Builder for Migration module events
 */
public class MigrationEventBuilder {

    public static PlatformEvent migrationStarted(Migration migration, String userId) {
        return PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MIGRATION_STARTED")
                .source(PlatformEvent.EventSource.MIGRATION)
                .entityId(migration.getId())
                .entityType("Migration")
                .userId(userId)
                .correlationId(migration.getProjectCode())
                .build();
    }

    public static PlatformEvent migrationPhaseChanged(Long migrationId, MigrationPhase phase, String userId) {
        PlatformEvent event = PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MIGRATION_PHASE_CHANGED")
                .source(PlatformEvent.EventSource.MIGRATION)
                .entityId(migrationId)
                .entityType("Migration")
                .userId(userId)
                .build();
        
        event.getPayload().put("phase", phase.name());
        return event;
    }

    public static PlatformEvent migrationCompleted(Migration migration, String userId) {
        return PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MIGRATION_COMPLETED")
                .source(PlatformEvent.EventSource.MIGRATION)
                .entityId(migration.getId())
                .entityType("Migration")
                .userId(userId)
                .correlationId(migration.getProjectCode())
                .build();
    }

    public static PlatformEvent migrationFailed(Long migrationId, String errorMessage, String userId) {
        PlatformEvent event = PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MIGRATION_FAILED")
                .source(PlatformEvent.EventSource.MIGRATION)
                .entityId(migrationId)
                .entityType("Migration")
                .userId(userId)
                .build();
        
        event.getPayload().put("errorMessage", errorMessage);
        return event;
    }

    public static PlatformEvent migrationRolledBack(Long migrationId, String userId) {
        return PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MIGRATION_ROLLED_BACK")
                .source(PlatformEvent.EventSource.MIGRATION)
                .entityId(migrationId)
                .entityType("Migration")
                .userId(userId)
                .build();
    }

    public static PlatformEvent migrationProgress(Long migrationId, int progress, String userId) {
        PlatformEvent event = PlatformEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MIGRATION_PROGRESS")
                .source(PlatformEvent.EventSource.MIGRATION)
                .entityId(migrationId)
                .entityType("Migration")
                .userId(userId)
                .build();
        
        event.getPayload().put("progress", progress);
        return event;
    }
}
