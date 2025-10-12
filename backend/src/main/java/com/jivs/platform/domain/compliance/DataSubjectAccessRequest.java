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
 * Entity representing a GDPR Article 15 - Right of Access request
 * Data subjects have the right to obtain confirmation about whether their
 * personal data is being processed and access to that data
 */
@Entity
@Table(name = "data_subject_access_requests", indexes = {
    @Index(name = "idx_dsar_user", columnList = "user_id"),
    @Index(name = "idx_dsar_status", columnList = "status")
})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DataSubjectAccessRequest {

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
    private Regulation regulation = Regulation.GDPR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String requestDetails;

    @ElementCollection
    @CollectionTable(name = "dsar_data_categories", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "category")
    private List<String> requestedDataCategories = new ArrayList<>();

    @Column
    private LocalDateTime dataCollectedFrom;

    @Column
    private LocalDateTime dataCollectedTo;

    @Column
    private String exportFormat = "JSON";  // JSON, CSV, PDF, XML

    @Column
    private String exportFilePath;

    @Column
    private Long exportFileSize;

    @Column
    private String downloadToken;

    @Column
    private LocalDateTime downloadExpiresAt;

    @Column
    private Integer downloadCount = 0;

    @Column
    private Integer maxDownloads = 3;

    @Column(columnDefinition = "TEXT")
    private String processingLog;

    @Column
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public Regulation getRegulation() { return regulation; }
    public void setRegulation(Regulation regulation) { this.regulation = regulation; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getRequestDetails() { return requestDetails; }
    public void setRequestDetails(String requestDetails) { this.requestDetails = requestDetails; }

    public List<String> getRequestedDataCategories() { return requestedDataCategories; }
    public void setRequestedDataCategories(List<String> requestedDataCategories) { this.requestedDataCategories = requestedDataCategories; }

    public LocalDateTime getDataCollectedFrom() { return dataCollectedFrom; }
    public void setDataCollectedFrom(LocalDateTime dataCollectedFrom) { this.dataCollectedFrom = dataCollectedFrom; }

    public LocalDateTime getDataCollectedTo() { return dataCollectedTo; }
    public void setDataCollectedTo(LocalDateTime dataCollectedTo) { this.dataCollectedTo = dataCollectedTo; }

    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }

    public String getExportFilePath() { return exportFilePath; }
    public void setExportFilePath(String exportFilePath) { this.exportFilePath = exportFilePath; }

    public Long getExportFileSize() { return exportFileSize; }
    public void setExportFileSize(Long exportFileSize) { this.exportFileSize = exportFileSize; }

    public String getDownloadToken() { return downloadToken; }
    public void setDownloadToken(String downloadToken) { this.downloadToken = downloadToken; }

    public LocalDateTime getDownloadExpiresAt() { return downloadExpiresAt; }
    public void setDownloadExpiresAt(LocalDateTime downloadExpiresAt) { this.downloadExpiresAt = downloadExpiresAt; }

    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }

    public Integer getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(Integer maxDownloads) { this.maxDownloads = maxDownloads; }

    public String getProcessingLog() { return processingLog; }
    public void setProcessingLog(String processingLog) { this.processingLog = processingLog; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

    /**
     * Helper method to check if download is available and not expired
     */
    @Transient
    public boolean isDownloadAvailable() {
        return status == RequestStatus.COMPLETED
               && downloadToken != null
               && downloadExpiresAt != null
               && LocalDateTime.now().isBefore(downloadExpiresAt)
               && downloadCount < maxDownloads;
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
    public String getFormat() {
        return exportFormat;
    }
}
