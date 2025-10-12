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
 * Entity representing a compliance report
 * Generated periodically to demonstrate compliance with regulations
 */
@Entity
@Table(name = "compliance_reports", indexes = {
    @Index(name = "idx_compliance_report_regulation", columnList = "regulation"),
    @Index(name = "idx_compliance_report_date", columnList = "report_date"),
    @Index(name = "idx_compliance_report_period", columnList = "period_start, period_end")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ComplianceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Regulation regulation;

    @Column(nullable = false)
    private LocalDateTime periodStart;

    @Column(nullable = false)
    private LocalDateTime periodEnd;

    @Column(nullable = false)
    private LocalDateTime reportDate;

    @Column
    private String reportTitle;

    @Column(columnDefinition = "TEXT")
    private String executiveSummary;

    // Request statistics
    @Column
    private Integer totalRequests = 0;

    @Column
    private Integer completedRequests = 0;

    @Column
    private Integer pendingRequests = 0;

    @Column
    private Integer failedRequests = 0;

    @Column
    private Integer overdueRequests = 0;

    // Request type breakdown
    @Column
    private Integer accessRequests = 0;

    @Column
    private Integer erasureRequests = 0;

    @Column
    private Integer rectificationRequests = 0;

    @Column
    private Integer portabilityRequests = 0;

    // Consent statistics
    @Column
    private Integer totalConsents = 0;

    @Column
    private Integer activeConsents = 0;

    @Column
    private Integer withdrawnConsents = 0;

    @Column
    private Integer expiredConsents = 0;

    // Compliance metrics
    @Column
    private Double averageResponseTimeHours;

    @Column
    private Double complianceRate;  // Percentage of requests completed within SLA

    @Column
    private Integer dataBreachCount = 0;

    @Column
    private Integer privacyIncidents = 0;

    // Data subject metrics
    @Column
    private Integer uniqueDataSubjects = 0;

    @Column
    private Long totalRecordsProcessed = 0L;

    @Column
    private Long recordsErased = 0L;

    @Column
    private Long recordsRectified = 0L;

    @Column
    private Long recordsExported = 0L;

    @ElementCollection
    @CollectionTable(name = "compliance_report_metrics", joinColumns = @JoinColumn(name = "report_id"))
    @MapKeyColumn(name = "metric_name")
    @Column(name = "metric_value", columnDefinition = "TEXT")
    private Map<String, String> additionalMetrics = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "compliance_report_findings", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "finding", columnDefinition = "TEXT")
    private Map<Integer, String> keyFindings = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "compliance_report_recommendations", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "recommendation", columnDefinition = "TEXT")
    private Map<Integer, String> recommendations = new HashMap<>();

    @Column
    private String reportFilePath;

    @Column
    private String generatedBy;

    @Column(nullable = false)
    private Boolean isPublished = false;

    @Column
    private LocalDateTime publishedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * Helper method to calculate completion rate
     */
    @Transient
    public double getCompletionRate() {
        if (totalRequests == 0) {
            return 0.0;
        }
        return (completedRequests * 100.0) / totalRequests;
    }

    /**
     * Helper method to calculate consent withdrawal rate
     */
    @Transient
    public double getConsentWithdrawalRate() {
        if (totalConsents == 0) {
            return 0.0;
        }
        return (withdrawnConsents * 100.0) / totalConsents;
    }

    /**
     * Convenience methods for ComplianceService compatibility
     */
    public void setGeneratedDate(LocalDateTime date) {
        this.reportDate = date;
    }

    public LocalDateTime getGeneratedDate() {
        return reportDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.periodStart = startDate;
    }

    public LocalDateTime getStartDate() {
        return periodStart;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.periodEnd = endDate;
    }

    public LocalDateTime getEndDate() {
        return periodEnd;
    }

    public void setRequestsByType(java.util.Map<ComplianceRequestType, Long> requestsByType) {
        // Map the request type counts
        for (java.util.Map.Entry<ComplianceRequestType, Long> entry : requestsByType.entrySet()) {
            switch (entry.getKey()) {
                case ACCESS:
                    this.accessRequests = entry.getValue().intValue();
                    break;
                case ERASURE:
                    this.erasureRequests = entry.getValue().intValue();
                    break;
                case RECTIFICATION:
                    this.rectificationRequests = entry.getValue().intValue();
                    break;
                case PORTABILITY:
                    this.portabilityRequests = entry.getValue().intValue();
                    break;
            }
        }
    }

    public void setRequestsByStatus(java.util.Map<ComplianceStatus, Long> requestsByStatus) {
        // Map the status counts
        for (java.util.Map.Entry<ComplianceStatus, Long> entry : requestsByStatus.entrySet()) {
            switch (entry.getKey()) {
                case COMPLETED:
                    this.completedRequests = entry.getValue().intValue();
                    break;
                case SUBMITTED:
                case IN_PROGRESS:
                case PENDING:
                    this.pendingRequests = entry.getValue().intValue();
                    break;
                case FAILED:
                    this.failedRequests = entry.getValue().intValue();
                    break;
            }
        }
    }

    public void setSlaCompliance(double slaCompliance) {
        this.complianceRate = slaCompliance;
    }

    public Double getSlaCompliance() {
        return complianceRate;
    }

    public void setConsentsGiven(long count) {
        this.activeConsents = (int) count;
    }

    public void setConsentsWithdrawn(long count) {
        this.withdrawnConsents = (int) count;
    }

    public void setDataBreaches(java.util.List<DataBreachIncident> breaches) {
        this.dataBreachCount = breaches != null ? breaches.size() : 0;
    }

    public java.util.List<DataBreachIncident> getDataBreaches() {
        // This is a simplified implementation
        // In a real scenario, you might want to store breach references
        return new java.util.ArrayList<>();
    }

    public void setRecommendations(java.util.List<String> recommendationsList) {
        if (recommendationsList != null) {
            this.recommendations.clear();
            for (int i = 0; i < recommendationsList.size(); i++) {
                this.recommendations.put(i, recommendationsList.get(i));
            }
        }
    }
}
