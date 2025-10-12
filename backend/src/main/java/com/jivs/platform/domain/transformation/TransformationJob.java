package com.jivs.platform.domain.transformation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Transformation job entity
 */
@Entity
@Table(name = "transformation_jobs", indexes = {
        @Index(name = "idx_transformation_jobs_status", columnList = "status"),
        @Index(name = "idx_transformation_jobs_source_system", columnList = "source_system"),
        @Index(name = "idx_transformation_jobs_target_system", columnList = "target_system"),
        @Index(name = "idx_transformation_jobs_start_time", columnList = "start_time")
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public class TransformationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true, length = 100)
    private String jobId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "target_system", nullable = false, length = 100)
    private String targetSystem;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TransformationStatus status = TransformationStatus.PENDING;

    @Column(name = "source_data", columnDefinition = "TEXT")
    private String sourceDataJson;

    @Column(name = "transformed_data", columnDefinition = "TEXT")
    private String transformedDataJson;

    @Column(name = "target_format", length = 50)
    private String targetFormat;

    @Column(name = "records_processed")
    private Integer recordsProcessed = 0;

    @Column(name = "records_transformed")
    private Integer recordsTransformed = 0;

    @Column(name = "records_failed")
    private Integer recordsFailed = 0;

    @ElementCollection
    @CollectionTable(name = "transformation_job_config",
                     joinColumns = @JoinColumn(name = "transformation_job_id"))
    @MapKeyColumn(name = "config_key")
    @Column(name = "config_value", columnDefinition = "TEXT")
    private Map<String, String> configuration = new HashMap<>();

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // Helper methods to work with JSON fields
    @Transient
    public void setSourceData(Map<String, Object> sourceData) {
        // This would convert to JSON in a real implementation
        this.sourceDataJson = sourceData != null ? sourceData.toString() : null;
    }

    @Transient
    public Map<String, Object> getSourceData() {
        // This would deserialize from JSON in a real implementation
        return new HashMap<>();
    }

    @Transient
    public void setTransformedData(Map<String, Object> transformedData) {
        this.transformedDataJson = transformedData != null ? transformedData.toString() : null;
    }

    @Transient
    public Map<String, Object> getTransformedData() {
        return new HashMap<>();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getTargetSystem() {
        return targetSystem;
    }

    public void setTargetSystem(String targetSystem) {
        this.targetSystem = targetSystem;
    }

    public TransformationStatus getStatus() {
        return status;
    }

    public void setStatus(TransformationStatus status) {
        this.status = status;
    }

    public String getSourceDataJson() {
        return sourceDataJson;
    }

    public void setSourceDataJson(String sourceDataJson) {
        this.sourceDataJson = sourceDataJson;
    }

    public String getTransformedDataJson() {
        return transformedDataJson;
    }

    public void setTransformedDataJson(String transformedDataJson) {
        this.transformedDataJson = transformedDataJson;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public Integer getRecordsTransformed() {
        return recordsTransformed;
    }

    public void setRecordsTransformed(Integer recordsTransformed) {
        this.recordsTransformed = recordsTransformed;
    }

    public Integer getRecordsFailed() {
        return recordsFailed;
    }

    public void setRecordsFailed(Integer recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
