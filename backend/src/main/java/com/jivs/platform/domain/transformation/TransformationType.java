package com.jivs.platform.domain.transformation;

/**
 * Enum representing types of data transformations
 */
public enum TransformationType {
    /**
     * Map field from source to target
     */
    FIELD_MAPPING,

    /**
     * Apply a function to transform data
     */
    VALUE_TRANSFORMATION,

    /**
     * Combine multiple fields
     */
    AGGREGATION,

    /**
     * Split a field into multiple fields
     */
    SPLIT,

    /**
     * Filter records based on conditions
     */
    FILTER,

    /**
     * Enrich data with additional information
     */
    ENRICHMENT,

    /**
     * Convert data types
     */
    TYPE_CONVERSION,

    /**
     * Apply custom logic
     */
    CUSTOM
}
