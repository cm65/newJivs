package com.jivs.platform.domain.compliance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a GDPR Article 17 - Right to Erasure request
 * Also known as "Right to be Forgotten"
 */
@Entity
@Table(name = "data_erasure_requests", indexes = {
    @Index(name = "idx_erasure_user", columnList = "user_id"),
    @Index(name = "idx_erasure_status", columnList = "status")
})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DataErasureRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String requesterEmail;

    @Column
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ElementCollection
    @CollectionTable(name = "erasure_data_categories", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "category")
    private List<String> dataCategoriesToErase = new ArrayList<>();

    @Column
    private Boolean eraseAllData = true;

    @Column
    private Boolean anonymizeInsteadOfDelete = false;

    @ElementCollection
    @CollectionTable(name = "erasure_exceptions", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "exception_reason", columnDefinition = "TEXT")
    private List<String> erasureExceptions = new ArrayList<>();

    @Column
    private Integer recordsIdentified = 0;

    @Column
    private Integer recordsErased = 0;

    @Column
    private Integer recordsAnonymized = 0;

    @Column
    private Integer recordsRetained = 0;

    @Column(columnDefinition = "TEXT")
    private String retentionReason;

    @ElementCollection
    @CollectionTable(name = "erasure_affected_systems", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "system_name")
    private List<String> affectedSystems = new ArrayList<>();

    @Column
    private LocalDateTime scheduledErasureDate;

    @Column
    private LocalDateTime erasureCompletedAt;

    @Column(columnDefinition = "TEXT")
    private String processingLog;

    @Column(columnDefinition = "TEXT")
    private String completionReport;

    @Column
    private String processedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;

    /**
     * Helper method to calculate erasure completion percentage
     */
    @Transient
    public double getCompletionPercentage() {
        if (recordsIdentified == 0) {
            return 0.0;
        }
        int processed = recordsErased + recordsAnonymized + recordsRetained;
        return (processed * 100.0) / recordsIdentified;
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public String getEmail() {
        return requesterEmail;
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public void setEmail(String email) {
        this.requesterEmail = email;
    }

    /**
     * Convenience method for ComplianceService compatibility
     */
    public String getIdentifier() {
        return identifier;
    }

    // Manual getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getDataCategoriesToErase() {
        return dataCategoriesToErase;
    }

    public void setDataCategoriesToErase(List<String> dataCategoriesToErase) {
        this.dataCategoriesToErase = dataCategoriesToErase;
    }

    public Boolean getEraseAllData() {
        return eraseAllData;
    }

    public void setEraseAllData(Boolean eraseAllData) {
        this.eraseAllData = eraseAllData;
    }

    public Boolean getAnonymizeInsteadOfDelete() {
        return anonymizeInsteadOfDelete;
    }

    public void setAnonymizeInsteadOfDelete(Boolean anonymizeInsteadOfDelete) {
        this.anonymizeInsteadOfDelete = anonymizeInsteadOfDelete;
    }

    public List<String> getErasureExceptions() {
        return erasureExceptions;
    }

    public void setErasureExceptions(List<String> erasureExceptions) {
        this.erasureExceptions = erasureExceptions;
    }

    public Integer getRecordsIdentified() {
        return recordsIdentified;
    }

    public void setRecordsIdentified(Integer recordsIdentified) {
        this.recordsIdentified = recordsIdentified;
    }

    public Integer getRecordsErased() {
        return recordsErased;
    }

    public void setRecordsErased(Integer recordsErased) {
        this.recordsErased = recordsErased;
    }

    public Integer getRecordsAnonymized() {
        return recordsAnonymized;
    }

    public void setRecordsAnonymized(Integer recordsAnonymized) {
        this.recordsAnonymized = recordsAnonymized;
    }

    public Integer getRecordsRetained() {
        return recordsRetained;
    }

    public void setRecordsRetained(Integer recordsRetained) {
        this.recordsRetained = recordsRetained;
    }

    public String getRetentionReason() {
        return retentionReason;
    }

    public void setRetentionReason(String retentionReason) {
        this.retentionReason = retentionReason;
    }

    public List<String> getAffectedSystems() {
        return affectedSystems;
    }

    public void setAffectedSystems(List<String> affectedSystems) {
        this.affectedSystems = affectedSystems;
    }

    public LocalDateTime getScheduledErasureDate() {
        return scheduledErasureDate;
    }

    public void setScheduledErasureDate(LocalDateTime scheduledErasureDate) {
        this.scheduledErasureDate = scheduledErasureDate;
    }

    public LocalDateTime getErasureCompletedAt() {
        return erasureCompletedAt;
    }

    public void setErasureCompletedAt(LocalDateTime erasureCompletedAt) {
        this.erasureCompletedAt = erasureCompletedAt;
    }

    public String getProcessingLog() {
        return processingLog;
    }

    public void setProcessingLog(String processingLog) {
        this.processingLog = processingLog;
    }

    public String getCompletionReport() {
        return completionReport;
    }

    public void setCompletionReport(String completionReport) {
        this.completionReport = completionReport;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
