package com.jivs.platform.domain.compliance;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of processing a data access request (GDPR Article 15)
 */
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequestResult {

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
     * Total records found
     */
    private Integer totalRecords = 0;

    /**
     * Personal data found grouped by source
     */
    private Map<String, List<Map<String, Object>>> personalData = new HashMap<>();

    /**
     * Data sources searched
     */
    private List<String> sourcesSearched = new ArrayList<>();

    /**
     * Export file path if data was exported
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
     * Processing notes
     */
    private List<String> processingNotes = new ArrayList<>();

    // Getters and Setters
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public String getDataSubjectId() { return dataSubjectId; }
    public void setDataSubjectId(String dataSubjectId) { this.dataSubjectId = dataSubjectId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Integer totalRecords) { this.totalRecords = totalRecords; }

    public Map<String, List<Map<String, Object>>> getPersonalData() { return personalData; }
    public void setPersonalData(Map<String, List<Map<String, Object>>> personalData) { this.personalData = personalData; }

    public List<String> getSourcesSearched() { return sourcesSearched; }
    public void setSourcesSearched(List<String> sourcesSearched) { this.sourcesSearched = sourcesSearched; }

    public String getExportFilePath() { return exportFilePath; }
    public void setExportFilePath(String exportFilePath) { this.exportFilePath = exportFilePath; }

    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public List<String> getProcessingNotes() { return processingNotes; }
    public void setProcessingNotes(List<String> processingNotes) { this.processingNotes = processingNotes; }

    /**
     * Add personal data from a source
     */
    public void addPersonalData(String source, Map<String, Object> data) {
        personalData.computeIfAbsent(source, k -> new ArrayList<>()).add(data);
        totalRecords++;
    }

    /**
     * Add a source that was searched
     */
    public void addSourceSearched(String source) {
        if (!sourcesSearched.contains(source)) {
            sourcesSearched.add(source);
        }
    }

    /**
     * Add a processing note
     */
    public void addProcessingNote(String note) {
        processingNotes.add(note);
    }

    /**
     * Convenience methods for ComplianceService compatibility
     */
    public void setDataSources(List<String> sources) {
        this.sourcesSearched = sources;
    }

    public List<String> getDataSources() {
        return sourcesSearched;
    }

    public void setCollectedData(Map<String, Object> data) {
        this.personalData.clear();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Map) {
                this.personalData.put(entry.getKey(), new ArrayList<>());
                this.personalData.get(entry.getKey()).add((Map<String, Object>) entry.getValue());
            }
        }
    }

    public void setExportPath(String path) {
        this.exportFilePath = path;
    }

    public void setError(String error) {
        this.errorMessage = error;
        this.success = false;
    }
}
