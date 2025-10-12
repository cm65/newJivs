package com.jivs.platform.domain.audit;

/**
 * Enum representing audit actions
 */
public enum AuditAction {
    /**
     * Create operation
     */
    CREATE,

    /**
     * Read/View operation
     */
    READ,

    /**
     * Update operation
     */
    UPDATE,

    /**
     * Delete operation
     */
    DELETE,

    /**
     * Login operation
     */
    LOGIN,

    /**
     * Logout operation
     */
    LOGOUT,

    /**
     * Authentication failure
     */
    LOGIN_FAILED,

    /**
     * Authorization failure
     */
    ACCESS_DENIED,

    /**
     * Export operation
     */
    EXPORT,

    /**
     * Import operation
     */
    IMPORT,

    /**
     * Configuration change
     */
    CONFIG_CHANGE,

    /**
     * Approval operation
     */
    APPROVE,

    /**
     * Rejection operation
     */
    REJECT,

    /**
     * Data access
     */
    DATA_ACCESS,

    /**
     * Authentication operation
     */
    AUTHENTICATE,

    /**
     * Other operation
     */
    OTHER
}
