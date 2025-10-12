package com.jivs.platform.domain.quality;

/**
 * Enum representing severity levels for data quality issues
 */
public enum Severity {
    /**
     * Critical issues that must be resolved immediately
     */
    CRITICAL,

    /**
     * Major issues that should be resolved soon
     */
    MAJOR,

    /**
     * Minor issues that can be resolved later
     */
    MINOR,

    /**
     * Informational issues for awareness
     */
    INFO
}
