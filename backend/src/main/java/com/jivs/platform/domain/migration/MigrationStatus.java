package com.jivs.platform.domain.migration;

/**
 * Migration status enumeration
 */
public enum MigrationStatus {
    INITIALIZED,
    PENDING,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    ROLLING_BACK,
    ROLLED_BACK
}
