package com.jivs.platform.domain.migration;

/**
 * Migration phase enumeration
 */
public enum MigrationPhase {
    PLANNING,
    EXTRACTION,
    TRANSFORMATION,
    VALIDATION,
    LOADING,
    VERIFICATION,
    CLEANUP,
    COMPLETED,
    FAILED
}
