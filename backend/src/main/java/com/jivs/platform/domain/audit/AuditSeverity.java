package com.jivs.platform.domain.audit;

/**
 * Enum representing audit log severity levels
 */
public enum AuditSeverity {
    /**
     * Informational audit entry
     */
    INFO,

    /**
     * Warning level audit entry
     */
    WARNING,

    /**
     * High priority audit entry
     */
    HIGH,

    /**
     * Error level audit entry
     */
    ERROR,

    /**
     * Critical security event
     */
    CRITICAL
}
