package com.jivs.platform.domain.quality;

/**
 * Enum representing different types of data quality rules
 */
public enum RuleType {
    /**
     * Check if required fields are populated
     */
    COMPLETENESS,

    /**
     * Check if data matches expected formats and reference data
     */
    ACCURACY,

    /**
     * Check if related fields are consistent
     */
    CONSISTENCY,

    /**
     * Check if data types, ranges, and allowed values are valid
     */
    VALIDITY,

    /**
     * Check for duplicate values
     */
    UNIQUENESS,

    /**
     * Check if data is current and up-to-date
     */
    TIMELINESS,

    /**
     * Check if foreign key references exist
     */
    REFERENTIAL_INTEGRITY,

    /**
     * Check custom business rules
     */
    BUSINESS_RULE
}
