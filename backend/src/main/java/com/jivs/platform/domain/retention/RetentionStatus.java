package com.jivs.platform.domain.retention;

/**
 * Enum representing retention status
 */
public enum RetentionStatus {
    /**
     * Retention policy is pending execution
     */
    PENDING,

    /**
     * Retention action is in progress
     */
    IN_PROGRESS,

    /**
     * Retention action completed successfully
     */
    COMPLETED,

    /**
     * Retention action failed
     */
    FAILED,

    /**
     * Retention action is on hold (e.g., legal hold)
     */
    ON_HOLD,

    /**
     * Retention action was skipped
     */
    SKIPPED,

    /**
     * Retention action is scheduled for future
     */
    SCHEDULED
}
