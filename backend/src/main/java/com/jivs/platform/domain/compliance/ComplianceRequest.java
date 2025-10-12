package com.jivs.platform.domain.compliance;

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
 * Entity representing a compliance request (GDPR, CCPA, etc.)
 */
@Entity
@Table(name = "compliance_requests", indexes = {
    @Index(name = "idx_compliance_request_user", columnList = "user_id"),
    @Index(name = "idx_compliance_request_status", columnList = "status"),
    @Index(name = "idx_compliance_request_type", columnList = "request_type"),
    @Index(name = "idx_compliance_request_date", columnList = "created_date")
})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ComplianceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "request_type")
    private ComplianceRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Regulation regulation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceStatus status = ComplianceStatus.SUBMITTED;

    @Column(nullable = false)
    private String subjectEmail;

    @Column
    private String subjectIdentifier;

    @Column
    private String requesterEmail;

    @Column
    private String requesterName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String verificationToken;

    @Column
    private LocalDateTime verifiedAt;

    @Column
    private LocalDateTime submittedDate;

    @Column
    private LocalDateTime dueDate;

    @Column
    private LocalDateTime processingStarted;

    @Column
    private LocalDateTime completedDate;

    @Column(columnDefinition = "TEXT")
    private String processingNotes;

    @Column(columnDefinition = "TEXT")
    private String resultSummary;

    @Column
    private String resultPath;

    @Column
    private String exportFormat;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column
    private String processedBy;

    @ElementCollection
    @CollectionTable(name = "compliance_request_corrections", joinColumns = @JoinColumn(name = "request_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "field_value", columnDefinition = "TEXT")
    private Map<String, Object> corrections = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "compliance_request_metadata", joinColumns = @JoinColumn(name = "request_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", columnDefinition = "TEXT")
    private Map<String, String> metadata = new HashMap<>();

    @Column(nullable = false)
    private Integer retentionDays = 90;  // How long to keep the request record

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_date")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public ComplianceRequestType getRequestType() { return requestType; }
    public void setRequestType(ComplianceRequestType requestType) { this.requestType = requestType; }

    public Regulation getRegulation() { return regulation; }
    public void setRegulation(Regulation regulation) { this.regulation = regulation; }

    public ComplianceStatus getStatus() { return status; }
    public void setStatus(ComplianceStatus status) { this.status = status; }

    public String getSubjectEmail() { return subjectEmail; }
    public void setSubjectEmail(String subjectEmail) { this.subjectEmail = subjectEmail; }

    public String getSubjectIdentifier() { return subjectIdentifier; }
    public void setSubjectIdentifier(String subjectIdentifier) { this.subjectIdentifier = subjectIdentifier; }

    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(LocalDateTime submittedDate) { this.submittedDate = submittedDate; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getProcessingStarted() { return processingStarted; }
    public void setProcessingStarted(LocalDateTime processingStarted) { this.processingStarted = processingStarted; }

    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }

    public String getProcessingNotes() { return processingNotes; }
    public void setProcessingNotes(String processingNotes) { this.processingNotes = processingNotes; }

    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }

    public String getResultPath() { return resultPath; }
    public void setResultPath(String resultPath) { this.resultPath = resultPath; }

    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public Map<String, Object> getCorrections() { return corrections; }
    public void setCorrections(Map<String, Object> corrections) { this.corrections = corrections; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public Integer getRetentionDays() { return retentionDays; }
    public void setRetentionDays(Integer retentionDays) { this.retentionDays = retentionDays; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    /**
     * Helper method to check if request is overdue
     */
    @Transient
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate)
               && status != ComplianceStatus.COMPLETED && status != ComplianceStatus.CANCELLED;
    }

    /**
     * Helper method to check if request requires verification
     */
    @Transient
    public boolean requiresVerification() {
        return verifiedAt == null && status == ComplianceStatus.PENDING;
    }
}
