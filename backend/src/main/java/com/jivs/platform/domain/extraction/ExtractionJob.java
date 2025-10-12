package com.jivs.platform.domain.extraction;

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
 * Extraction Job entity
 */
@Entity
@Table(name = "extraction_jobs", indexes = {
        @Index(name = "idx_extraction_jobs_job_id", columnList = "job_id"),
        @Index(name = "idx_extraction_jobs_status", columnList = "status"),
        @Index(name = "idx_extraction_jobs_start_time", columnList = "start_time")
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true, length = 100)
    private String jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id")
    private DataSource dataSource;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "records_extracted")
    private Long recordsExtracted = 0L;

    @Column(name = "records_failed")
    private Long recordsFailed = 0L;

    @Column(name = "bytes_processed")
    private Long bytesProcessed = 0L;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @ElementCollection
    @CollectionTable(name = "extraction_job_params",
                     joinColumns = @JoinColumn(name = "extraction_job_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, String> extractionParams = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "extraction_job_context",
                     joinColumns = @JoinColumn(name = "extraction_job_id"))
    @MapKeyColumn(name = "context_key")
    @Column(name = "context_value")
    private Map<String, String> executionContext = new HashMap<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "triggered_by", length = 50)
    private String triggeredBy;

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

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

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

    public Map<String, String> getExtractionParams() {
        return extractionParams;
    }

    public void setExtractionParams(Map<String, String> extractionParams) {
        this.extractionParams = extractionParams;
    }

    public Map<String, String> getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(Map<String, String> executionContext) {
        this.executionContext = executionContext;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public enum JobStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }
}