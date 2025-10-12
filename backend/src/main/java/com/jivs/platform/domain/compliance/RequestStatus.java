package com.jivs.platform.domain.compliance;

/**
 * Enum representing compliance request status
 */
public enum RequestStatus {
    PENDING,        // Request received and pending processing
    IN_PROGRESS,    // Request is being processed
    COMPLETED,      // Request successfully completed
    FAILED,         // Request processing failed
    REJECTED,       // Request was rejected (e.g., identity verification failed)
    CANCELLED,      // Request was cancelled by requester
    PARTIALLY_COMPLETED  // Request partially completed
}
