package com.jivs.platform.domain.compliance;

/**
 * Enum representing compliance request status
 */
public enum ComplianceStatus {
    /**
     * Request has been submitted
     */
    SUBMITTED,

    /**
     * Request is awaiting review
     */
    PENDING,

    /**
     * Request is being processed
     */
    IN_PROGRESS,

    /**
     * Request requires additional information
     */
    AWAITING_INFO,

    /**
     * Request has been completed successfully
     */
    COMPLETED,

    /**
     * Request processing failed
     */
    FAILED,

    /**
     * Request was cancelled
     */
    CANCELLED,

    /**
     * Request was rejected
     */
    REJECTED,

    /**
     * Request is on hold
     */
    ON_HOLD
}
