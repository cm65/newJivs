package com.jivs.platform.domain.compliance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of processing a data portability request (GDPR Article 20)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortabilityRequestResult {

    /**
     * Request ID
     */
    private Long requestId;

    /**
     * Data subject identifier
     */
    private String dataSubjectId;

    /**
     * Processing start time
     */
    private LocalDateTime startTime;

    /**
     * Processing end time
     */
    private LocalDateTime endTime;

    /**
     * Duration in milliseconds
     */
    private Long durationMs;

    /**
     * Success status
     */
    private Boolean success;

    /**
     * Error message if failed
     */
    private String errorMessage;

    /**
     * Total records exported
     */
    @Builder.Default
    private Integer totalRecords = 0;

    /**
     * Records exported per source
     */
    @Builder.Default
    private Map<String, Integer> recordsBySource = new HashMap<>();

    /**
     * Export file path
     */
    private String exportFilePath;

    /**
     * Export format (JSON, XML, CSV, etc.)
     */
    private String exportFormat;

    /**
     * File size in bytes
     */
    private Long fileSizeBytes;

    /**
     * Data categories included
     */
    @Builder.Default
    private List<String> dataCategories = new ArrayList<>();

    /**
     * Sources included
     */
    @Builder.Default
    private List<String> sourcesIncluded = new ArrayList<>();

    /**
     * Data structured in machine-readable format
     */
    private Boolean machineReadable;

    /**
     * Download URL if available
     */
    private String downloadUrl;

    /**
     * URL expiration date
     */
    private LocalDateTime urlExpirationDate;

    /**
     * Processing notes
     */
    @Builder.Default
    private List<String> processingNotes = new ArrayList<>();

    /**
     * Add records from a source
     */
    public void addRecords(String source, int count) {
        recordsBySource.put(source, recordsBySource.getOrDefault(source, 0) + count);
        totalRecords += count;
    }

    /**
     * Add data category
     */
    public void addDataCategory(String category) {
        if (!dataCategories.contains(category)) {
            dataCategories.add(category);
        }
    }

    /**
     * Add source
     */
    public void addSource(String source) {
        if (!sourcesIncluded.contains(source)) {
            sourcesIncluded.add(source);
        }
    }

    /**
     * Add processing note
     */
    public void addProcessingNote(String note) {
        processingNotes.add(note);
    }
}
