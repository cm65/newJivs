package com.jivs.platform.domain.compliance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing the result of a GDPR Article 16 - Right to Rectification request
 * Data subjects have the right to have inaccurate personal data corrected
 */
@Entity
@Table(name = "rectification_request_results", indexes = {
    @Index(name = "idx_rectification_user", columnList = "user_id"),
    @Index(name = "idx_rectification_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RectificationRequestResult {

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
    private String requestDescription;

    @ElementCollection
    @CollectionTable(name = "rectification_fields", joinColumns = @JoinColumn(name = "result_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "old_value", columnDefinition = "TEXT")
    private Map<String, String> oldValues = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "rectification_new_values", joinColumns = @JoinColumn(name = "result_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "new_value", columnDefinition = "TEXT")
    private Map<String, String> newValues = new HashMap<>();

    @Column
    private Integer fieldsRequested = 0;

    @Column
    private Integer fieldsRectified = 0;

    @Column
    private Integer fieldsRejected = 0;

    @ElementCollection
    @CollectionTable(name = "rectification_rejections", joinColumns = @JoinColumn(name = "result_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private Map<String, String> rejectionReasons = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "rectification_affected_systems", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "system_name")
    private Map<String, Boolean> affectedSystemsUpdated = new HashMap<>();  // system -> success

    @Column(columnDefinition = "TEXT")
    private String verificationMethod;

    @Column
    private LocalDateTime verifiedAt;

    @Column
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String processingLog;

    @Column(columnDefinition = "TEXT")
    private String completionReport;

    @Column
    private String processedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime lastModifiedDate = LocalDateTime.now();

    /**
     * Helper method to calculate rectification success rate
     */
    @Transient
    public double getSuccessRate() {
        if (fieldsRequested == 0) {
            return 0.0;
        }
        return (fieldsRectified * 100.0) / fieldsRequested;
    }

    /**
     * Helper method to check if fully completed
     */
    @Transient
    public boolean isFullyCompleted() {
        return fieldsRequested > 0 && fieldsRectified == fieldsRequested;
    }

    /**
     * Convenience methods for ComplianceService compatibility
     */
    @Transient
    public void setRequestId(Long requestId) {
        // This is a result entity, so we don't store the parent request ID
        // This method is here for compatibility with the service layer
    }

    @Transient
    public void setUpdatedRecords(int count) {
        this.fieldsRectified = count;
    }

    @Transient
    public Integer getUpdatedRecords() {
        return fieldsRectified;
    }

    @Transient
    public void setError(String error) {
        this.status = RequestStatus.FAILED;
        this.processingLog = (processingLog != null ? processingLog + "\n" : "") + "ERROR: " + error;
    }

    @Transient
    public String getError() {
        if (status == RequestStatus.FAILED && processingLog != null && processingLog.contains("ERROR:")) {
            int start = processingLog.indexOf("ERROR:");
            return processingLog.substring(start + 7).trim();
        }
        return null;
    }
}
