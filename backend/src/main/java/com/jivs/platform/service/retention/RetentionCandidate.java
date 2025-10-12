package com.jivs.platform.service.retention;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a record candidate for retention action
 */
@NoArgsConstructor
@AllArgsConstructor
public class RetentionCandidate {

    /**
     * Entity type (table name or business object type)
     */
    private String entityType;

    /**
     * Record ID
     */
    private Long recordId;

    /**
     * Record creation date
     */
    private LocalDateTime createdDate;

    /**
     * Record size in bytes
     */
    private long sizeBytes;

    /**
     * Category or classification of the record
     */
    private String category;

    /**
     * Age of record in days
     */
    private long ageInDays;

    /**
     * Additional metadata
     */
    private java.util.Map<String, Object> metadata;

    // Getters and Setters
    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getAgeInDays() {
        return ageInDays;
    }

    public void setAgeInDays(long ageInDays) {
        this.ageInDays = ageInDays;
    }

    public java.util.Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(java.util.Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
