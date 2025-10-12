package com.jivs.platform.domain.quality;

/**
 * Enum representing the status of a data quality check
 */
public enum QualityCheckStatus {
    /**
     * Check is waiting to be executed
     */
    PENDING,

    /**
     * Check is currently running
     */
    IN_PROGRESS,

    /**
     * Check completed successfully
     */
    COMPLETED,

    /**
     * Check failed with errors
     */
    FAILED,

    /**
     * Check was cancelled
     */
    CANCELLED
}
