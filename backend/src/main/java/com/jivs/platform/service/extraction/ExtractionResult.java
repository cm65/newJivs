package com.jivs.platform.service.extraction;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of data extraction
 */
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {
    private Long recordsExtracted;
    private Long recordsFailed;
    private Long bytesProcessed;
    private List<String> errors = new ArrayList<>();
    private String outputPath;
    private String metadata;

    // Getters and Setters
    public Long getRecordsExtracted() {
        return recordsExtracted;
    }

    public void setRecordsExtracted(Long recordsExtracted) {
        this.recordsExtracted = recordsExtracted;
    }

    public Long getRecordsFailed() {
        return recordsFailed;
    }

    public void setRecordsFailed(Long recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public Long getBytesProcessed() {
        return bytesProcessed;
    }

    public void setBytesProcessed(Long bytesProcessed) {
        this.bytesProcessed = bytesProcessed;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}