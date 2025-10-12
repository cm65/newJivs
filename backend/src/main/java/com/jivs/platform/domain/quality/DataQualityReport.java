package com.jivs.platform.domain.quality;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a data quality report
 */
@Entity
@Table(name = "data_quality_reports")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DataQualityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long datasetId;

    @Column(nullable = false)
    private String datasetType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime checkDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QualityCheckStatus status;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DataQualityCheck> checks = new ArrayList<>();

    @Embedded
    private QualityMetrics metrics;

    @Column(nullable = false)
    private Double qualityScore = 0.0;

    @ElementCollection
    @CollectionTable(name = "quality_report_issues", joinColumns = @JoinColumn(name = "report_id"))
    private List<QualityIssue> issues = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "quality_report_recommendations", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "recommendation", columnDefinition = "TEXT")
    private List<String> recommendations = new ArrayList<>();

    private LocalDateTime completionTime;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private String executedBy;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public LocalDateTime getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(LocalDateTime checkDate) {
        this.checkDate = checkDate;
    }

    public QualityCheckStatus getStatus() {
        return status;
    }

    public void setStatus(QualityCheckStatus status) {
        this.status = status;
    }

    public List<DataQualityCheck> getChecks() {
        return checks;
    }

    public void setChecks(List<DataQualityCheck> checks) {
        this.checks = checks;
    }

    public QualityMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(QualityMetrics metrics) {
        this.metrics = metrics;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public List<QualityIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<QualityIssue> issues) {
        this.issues = issues;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }
}
