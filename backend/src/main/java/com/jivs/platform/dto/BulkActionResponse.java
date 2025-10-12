package com.jivs.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO for bulk operation responses
 * Provides detailed results for each processed item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkActionResponse {

    /**
     * Overall status of the bulk operation
     */
    private String status; // "success", "partial", "failed"

    /**
     * Total number of items processed
     */
    private int totalProcessed;

    /**
     * Number of successful operations
     */
    private int successCount;

    /**
     * Number of failed operations
     */
    private int failureCount;

    /**
     * List of successfully processed IDs
     */
    @Builder.Default
    private List<String> successfulIds = new ArrayList<>();

    /**
     * Map of failed IDs to error messages
     */
    @Builder.Default
    private Map<String, String> failedIds = new java.util.HashMap<>();

    /**
     * Overall message
     */
    private String message;

    /**
     * Time taken to process (in milliseconds)
     */
    private Long processingTimeMs;
}
