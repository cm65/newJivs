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
 * Entity representing a GDPR Article 20 - Right to Data Portability request
 * Data subjects have the right to receive their personal data in a structured,
 * commonly used, and machine-readable format
 */
@Entity
@Table(name = "data_portability_requests", indexes = {
    @Index(name = "idx_portability_user", columnList = "user_id"),
    @Index(name = "idx_portability_status", columnList = "status")
})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DataPortabilityRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String requesterEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String requestDetails;

    @ElementCollection
    @CollectionTable(name = "portability_data_categories", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "category")
    private List<String> dataCategories = new ArrayList<>();

    @Column(nullable = false)
    private String exportFormat = "JSON";  // JSON, CSV, XML, PDF

    @Column
    private Boolean structuredFormat = true;

    @Column
    private Boolean machineReadable = true;

    @Column
    private String targetSystem;  // If transmitting directly to another controller

    @Column
    private String exportFilePath;

    @Column
    private Long exportFileSize;

    @Column
    private String fileChecksum;

    @Column
    private String downloadToken;

    @Column
    private LocalDateTime downloadExpiresAt;

    @Column
    private Integer downloadCount = 0;

    @Column
    private Integer maxDownloads = 5;

    @Column
    private Integer recordsExported = 0;

    @Column(columnDefinition = "TEXT")
    private String exportMetadata;

    @Column(columnDefinition = "TEXT")
    private String processingLog;

    @Column
    private LocalDateTime completedAt;

    @Column
    private String processedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedDate;

    /**
     * Helper method to check if download is available
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
     * Helper method to increment download count
     */
    public void incrementDownloadCount() {
        this.downloadCount++;
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

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getRequestDetails() {
        return requestDetails;
    }

    public void setRequestDetails(String requestDetails) {
        this.requestDetails = requestDetails;
    }

    public List<String> getDataCategories() {
        return dataCategories;
    }

    public void setDataCategories(List<String> dataCategories) {
        this.dataCategories = dataCategories;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public Boolean getStructuredFormat() {
        return structuredFormat;
    }

    public void setStructuredFormat(Boolean structuredFormat) {
        this.structuredFormat = structuredFormat;
    }

    public Boolean getMachineReadable() {
        return machineReadable;
    }

    public void setMachineReadable(Boolean machineReadable) {
        this.machineReadable = machineReadable;
    }

    public String getTargetSystem() {
        return targetSystem;
    }

    public void setTargetSystem(String targetSystem) {
        this.targetSystem = targetSystem;
    }

    public String getExportFilePath() {
        return exportFilePath;
    }

    public void setExportFilePath(String exportFilePath) {
        this.exportFilePath = exportFilePath;
    }

    public Long getExportFileSize() {
        return exportFileSize;
    }

    public void setExportFileSize(Long exportFileSize) {
        this.exportFileSize = exportFileSize;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(String fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    public String getDownloadToken() {
        return downloadToken;
    }

    public void setDownloadToken(String downloadToken) {
        this.downloadToken = downloadToken;
    }

    public LocalDateTime getDownloadExpiresAt() {
        return downloadExpiresAt;
    }

    public void setDownloadExpiresAt(LocalDateTime downloadExpiresAt) {
        this.downloadExpiresAt = downloadExpiresAt;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public Integer getRecordsExported() {
        return recordsExported;
    }

    public void setRecordsExported(Integer recordsExported) {
        this.recordsExported = recordsExported;
    }

    public String getExportMetadata() {
        return exportMetadata;
    }

    public void setExportMetadata(String exportMetadata) {
        this.exportMetadata = exportMetadata;
    }

    public String getProcessingLog() {
        return processingLog;
    }

    public void setProcessingLog(String processingLog) {
        this.processingLog = processingLog;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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
