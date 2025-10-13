package com.jivs.platform.domain.quality;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing an individual data quality check execution
 */
@Entity
@Table(name = "data_quality_checks")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DataQualityCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "check_id", nullable = false, unique = true, length = 100)
    private String checkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private DataQualityRule rule;

    @Column(name = "target_table", length = 100)
    private String targetTable;

    @Column(name = "target_schema", length = 100)
    private String targetSchema;

    @Column(name = "check_status", nullable = false, length = 50)
    private String checkStatus;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "records_checked")
    private Long recordsChecked = 0L;

    @Column(name = "records_passed")
    private Long recordsPassed = 0L;

    @Column(name = "records_failed")
    private Long recordsFailed = 0L;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "check_params", columnDefinition = "jsonb")
    private String checkParams;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "triggered_by", length = 50)
    private String triggeredBy;

    // Fields below are NOT in database - marked as @Transient
    @Transient
    private DataQualityReport report;

    @Transient
    private LocalDateTime executionTime;

    @Transient
    private boolean passed;

    @Transient
    private Severity severity;

    @Transient
    private String failureDetails;

    @Transient
    private String errorMessage;

    @Transient
    private Double passPercentage;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataQualityRule getRule() {
        return rule;
    }

    public void setRule(DataQualityRule rule) {
        this.rule = rule;
    }

    public DataQualityReport getReport() {
        return report;
    }

    public void setReport(DataQualityReport report) {
        this.report = report;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getFailureDetails() {
        return failureDetails;
    }

    public void setFailureDetails(String failureDetails) {
        this.failureDetails = failureDetails;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getRecordsChecked() {
        return recordsChecked;
    }

    public void setRecordsChecked(Long recordsChecked) {
        this.recordsChecked = recordsChecked;
    }

    public Long getRecordsFailed() {
        return recordsFailed;
    }

    public void setRecordsFailed(Long recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public Double getPassPercentage() {
        return passPercentage;
    }

    public void setPassPercentage(Double passPercentage) {
        this.passPercentage = passPercentage;
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getTargetSchema() {
        return targetSchema;
    }

    public void setTargetSchema(String targetSchema) {
        this.targetSchema = targetSchema;
    }

    public String getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(String checkStatus) {
        this.checkStatus = checkStatus;
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

    public Long getRecordsPassed() {
        return recordsPassed;
    }

    public void setRecordsPassed(Long recordsPassed) {
        this.recordsPassed = recordsPassed;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getCheckParams() {
        return checkParams;
    }

    public void setCheckParams(String checkParams) {
        this.checkParams = checkParams;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
}
