package com.jivs.platform.domain.retention;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a retention record tracking
 */
@Entity
@Table(name = "retention_records")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RetentionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private RetentionPolicy policy;

    @Column(nullable = false)
    private Long recordId;

    @Column(nullable = false)
    private String datasetType;

    @Column(nullable = false)
    private String entityType;

    @Enumerated(EnumType.STRING)
    private RetentionAction action;

    private LocalDateTime executionDate;

    @Column(nullable = false)
    private LocalDateTime recordCreatedAt;

    @Column(nullable = false)
    private LocalDateTime eligibleForActionAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetentionStatus status = RetentionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetentionAction scheduledAction;

    @Column(nullable = false)
    private boolean onLegalHold = false;

    @Column(columnDefinition = "TEXT")
    private String legalHoldReason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime actionExecutedAt;

    private String actionExecutedBy;

    @Column(columnDefinition = "TEXT")
    private String executionNotes;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Integer retryCount = 0;

    private LocalDateTime lastRetryAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RetentionPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(RetentionPolicy policy) {
        this.policy = policy;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public RetentionAction getAction() {
        return action;
    }

    public void setAction(RetentionAction action) {
        this.action = action;
    }

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public LocalDateTime getRecordCreatedAt() {
        return recordCreatedAt;
    }

    public void setRecordCreatedAt(LocalDateTime recordCreatedAt) {
        this.recordCreatedAt = recordCreatedAt;
    }

    public LocalDateTime getEligibleForActionAt() {
        return eligibleForActionAt;
    }

    public void setEligibleForActionAt(LocalDateTime eligibleForActionAt) {
        this.eligibleForActionAt = eligibleForActionAt;
    }

    public RetentionStatus getStatus() {
        return status;
    }

    public void setStatus(RetentionStatus status) {
        this.status = status;
    }

    public RetentionAction getScheduledAction() {
        return scheduledAction;
    }

    public void setScheduledAction(RetentionAction scheduledAction) {
        this.scheduledAction = scheduledAction;
    }

    public boolean isOnLegalHold() {
        return onLegalHold;
    }

    public void setOnLegalHold(boolean onLegalHold) {
        this.onLegalHold = onLegalHold;
    }

    public String getLegalHoldReason() {
        return legalHoldReason;
    }

    public void setLegalHoldReason(String legalHoldReason) {
        this.legalHoldReason = legalHoldReason;
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

    public LocalDateTime getActionExecutedAt() {
        return actionExecutedAt;
    }

    public void setActionExecutedAt(LocalDateTime actionExecutedAt) {
        this.actionExecutedAt = actionExecutedAt;
    }

    public String getActionExecutedBy() {
        return actionExecutedBy;
    }

    public void setActionExecutedBy(String actionExecutedBy) {
        this.actionExecutedBy = actionExecutedBy;
    }

    public String getExecutionNotes() {
        return executionNotes;
    }

    public void setExecutionNotes(String executionNotes) {
        this.executionNotes = executionNotes;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getLastRetryAt() {
        return lastRetryAt;
    }

    public void setLastRetryAt(LocalDateTime lastRetryAt) {
        this.lastRetryAt = lastRetryAt;
    }
}
