package com.jivs.platform.domain.retention;

/**
 * Enum representing retention actions
 */
public enum RetentionAction {
    /**
     * Delete the record permanently
     */
    DELETE,

    /**
     * Move record to archive storage
     */
    ARCHIVE,

    /**
     * Move record to cold storage
     */
    COLD_STORAGE,

    /**
     * Anonymize sensitive data
     */
    ANONYMIZE,

    /**
     * Soft delete (mark as deleted)
     */
    SOFT_DELETE,

    /**
     * Send notification about upcoming action
     */
    NOTIFY,

    /**
     * No action, just track
     */
    NONE
}
