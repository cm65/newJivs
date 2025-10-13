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
 * Maps to data_subject_requests table
 */
@Entity
@Table(name = "data_subject_requests", indexes = {
    @Index(name = "idx_data_subject_requests_status", columnList = "status"),
    @Index(name = "idx_data_subject_requests_request_id", columnList = "request_id"),
    @Index(name = "idx_data_subject_requests_due_date", columnList = "due_date")
})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ComplianceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 100)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "request_type", length = 50)
    private ComplianceRequestType requestType;

    @Column(nullable = false, name = "subject_email", length = 255)
    private String subjectEmail;

    @Column(name = "subject_name", length = 200)
    private String subjectName;

    @Column(name = "subject_identifier", length = 255)
    private String subjectIdentifier;

    @Column(name = "request_details", columnDefinition = "TEXT")
    private String requestDetails;

    @Column(name = "request_source", length = 50)
    private String requestSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplianceStatus status = ComplianceStatus.SUBMITTED;

    @Column(length = 20)
    private String priority = "MEDIUM";

    @Column(name = "due_date", nullable = false)
    private java.time.LocalDate dueDate;

    @Column(name = "completed_date")
    private java.time.LocalDate completedDate;

    @Column(name = "verification_status", length = 50)
    private String verificationStatus;

    @Column(name = "verification_method", length = 50)
    private String verificationMethod;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 50)
    private String verifiedBy;

    @Column(name = "assigned_to", length = 50)
    private String assignedTo;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Fields below are NOT in database - marked as @Transient
    @Transient
    private Long userId;

    @Transient
    private Regulation regulation;

    @Transient
    private String requesterEmail;

    @Transient
    private String requesterName;

    @Transient
    private String description;

    @Transient
    private String reason;

    @Transient
    private String verificationToken;

    @Transient
    private LocalDateTime submittedDate;

    @Transient
    private LocalDateTime processingStarted;

    @Transient
    private String processingNotes;

    @Transient
    private String resultSummary;

    @Transient
    private String resultPath;

    @Transient
    private String exportFormat;

    @Transient
    private String errorMessage;

    @Transient
    private String processedBy;

    @Transient
    private Map<String, Object> corrections = new HashMap<>();

    @Transient
    private Map<String, String> metadata = new HashMap<>();

    @Transient
    private Integer retentionDays = 90;

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

    public java.time.LocalDate getDueDate() { return dueDate; }
    public void setDueDate(java.time.LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getProcessingStarted() { return processingStarted; }
    public void setProcessingStarted(LocalDateTime processingStarted) { this.processingStarted = processingStarted; }

    public java.time.LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(java.time.LocalDate completedDate) { this.completedDate = completedDate; }

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

    // New DB field getters/setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getRequestDetails() { return requestDetails; }
    public void setRequestDetails(String requestDetails) { this.requestDetails = requestDetails; }

    public String getRequestSource() { return requestSource; }
    public void setRequestSource(String requestSource) { this.requestSource = requestSource; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getVerificationMethod() { return verificationMethod; }
    public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }

    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Backward compatibility for old field names
    public LocalDateTime getCreatedDate() { return createdAt; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdAt = createdDate; }

    public LocalDateTime getLastModifiedDate() { return updatedAt; }
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) { this.updatedAt = lastModifiedDate; }

    /**
     * Helper method to check if request is overdue
     */
    @Transient
    public boolean isOverdue() {
        return dueDate != null && java.time.LocalDate.now().isAfter(dueDate)
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
