package com.jivs.platform.domain.migration;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Embeddable migration metrics
 */
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class MigrationMetrics {

    @Column(name = "total_records")
    private Integer totalRecords = 0;

    @Column(name = "processed_records")
    private Integer processedRecords = 0;

    @Column(name = "successful_records")
    private Integer successfulRecords = 0;

    @Column(name = "failed_records")
    private Integer failedRecords = 0;

    @Column(name = "extracted_records")
    private Integer extractedRecords = 0;

    @Column(name = "transformed_records")
    private Integer transformedRecords = 0;

    @Column(name = "loaded_records")
    private Integer loadedRecords = 0;

    @Column(name = "validation_score")
    private Double validationScore;

    @Column(name = "validation_errors")
    private Integer validationErrors = 0;

    @Column(name = "bytes_processed")
    private Long bytesProcessed = 0L;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    // Getters and Setters
    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(Integer processedRecords) {
        this.processedRecords = processedRecords;
    }

    public Integer getSuccessfulRecords() {
        return successfulRecords;
    }

    public void setSuccessfulRecords(Integer successfulRecords) {
        this.successfulRecords = successfulRecords;
    }

    public Integer getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(Integer failedRecords) {
        this.failedRecords = failedRecords;
    }

    public Integer getExtractedRecords() {
        return extractedRecords;
    }

    public void setExtractedRecords(Integer extractedRecords) {
        this.extractedRecords = extractedRecords;
    }

    public Integer getTransformedRecords() {
        return transformedRecords;
    }

    public void setTransformedRecords(Integer transformedRecords) {
        this.transformedRecords = transformedRecords;
    }

    public Integer getLoadedRecords() {
        return loadedRecords;
    }

    public void setLoadedRecords(Integer loadedRecords) {
        this.loadedRecords = loadedRecords;
    }

    public Double getValidationScore() {
        return validationScore;
    }

    public void setValidationScore(Double validationScore) {
        this.validationScore = validationScore;
    }

    public Integer getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Integer validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Long getBytesProcessed() {
        return bytesProcessed;
    }

    public void setBytesProcessed(Long bytesProcessed) {
        this.bytesProcessed = bytesProcessed;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
