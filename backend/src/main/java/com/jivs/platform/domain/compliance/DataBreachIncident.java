package com.jivs.platform.domain.compliance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a data breach incident
 */
@Entity
@Table(name = "data_breach_incidents", indexes = {
    @Index(name = "idx_breach_date", columnList = "breach_date"),
    @Index(name = "idx_severity", columnList = "severity"),
    @Index(name = "idx_status", columnList = "status")
})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DataBreachIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String incidentId;

    @Column(nullable = false)
    private LocalDateTime breachDate;

    @Column(nullable = false)
    private LocalDateTime discoveryDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BreachSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BreachStatus status;

    @Column(columnDefinition = "TEXT")
    private String affectedDataTypes;

    @Column(nullable = false)
    private Integer affectedRecordsCount;

    @Column(nullable = false)
    private Integer affectedDataSubjectsCount;

    @Column(columnDefinition = "TEXT")
    private String breachCause;

    @Column(columnDefinition = "TEXT")
    private String containmentActions;

    @Column
    private LocalDateTime containmentDate;

    @Column(columnDefinition = "TEXT")
    private String remediationActions;

    @Column
    private LocalDateTime remediationDate;

    @Column(nullable = false)
    private Boolean regulatoryNotificationRequired;

    @Column
    private LocalDateTime regulatoryNotificationDate;

    @Column(nullable = false)
    private Boolean dataSubjectNotificationRequired;

    @Column
    private LocalDateTime dataSubjectNotificationDate;

    @Column(length = 500)
    private String regulatoryBody;

    @Column(length = 200)
    private String caseNumber;

    @Column(columnDefinition = "TEXT")
    private String lessonsLearned;

    @Column(columnDefinition = "TEXT")
    private String preventiveMeasures;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Enum for breach severity
     */
    public enum BreachSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Enum for breach status
     */
    public enum BreachStatus {
        DETECTED,
        INVESTIGATING,
        CONTAINED,
        REMEDIATED,
        CLOSED
    }

    // Manual getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public LocalDateTime getBreachDate() {
        return breachDate;
    }

    public void setBreachDate(LocalDateTime breachDate) {
        this.breachDate = breachDate;
    }

    public LocalDateTime getDiscoveryDate() {
        return discoveryDate;
    }

    public void setDiscoveryDate(LocalDateTime discoveryDate) {
        this.discoveryDate = discoveryDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BreachSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(BreachSeverity severity) {
        this.severity = severity;
    }

    public BreachStatus getStatus() {
        return status;
    }

    public void setStatus(BreachStatus status) {
        this.status = status;
    }

    public String getAffectedDataTypes() {
        return affectedDataTypes;
    }

    public void setAffectedDataTypes(String affectedDataTypes) {
        this.affectedDataTypes = affectedDataTypes;
    }

    public Integer getAffectedRecordsCount() {
        return affectedRecordsCount;
    }

    public void setAffectedRecordsCount(Integer affectedRecordsCount) {
        this.affectedRecordsCount = affectedRecordsCount;
    }

    public Integer getAffectedDataSubjectsCount() {
        return affectedDataSubjectsCount;
    }

    public void setAffectedDataSubjectsCount(Integer affectedDataSubjectsCount) {
        this.affectedDataSubjectsCount = affectedDataSubjectsCount;
    }

    public String getBreachCause() {
        return breachCause;
    }

    public void setBreachCause(String breachCause) {
        this.breachCause = breachCause;
    }

    public String getContainmentActions() {
        return containmentActions;
    }

    public void setContainmentActions(String containmentActions) {
        this.containmentActions = containmentActions;
    }

    public LocalDateTime getContainmentDate() {
        return containmentDate;
    }

    public void setContainmentDate(LocalDateTime containmentDate) {
        this.containmentDate = containmentDate;
    }

    public String getRemediationActions() {
        return remediationActions;
    }

    public void setRemediationActions(String remediationActions) {
        this.remediationActions = remediationActions;
    }

    public LocalDateTime getRemediationDate() {
        return remediationDate;
    }

    public void setRemediationDate(LocalDateTime remediationDate) {
        this.remediationDate = remediationDate;
    }

    public Boolean getRegulatoryNotificationRequired() {
        return regulatoryNotificationRequired;
    }

    public void setRegulatoryNotificationRequired(Boolean regulatoryNotificationRequired) {
        this.regulatoryNotificationRequired = regulatoryNotificationRequired;
    }

    public LocalDateTime getRegulatoryNotificationDate() {
        return regulatoryNotificationDate;
    }

    public void setRegulatoryNotificationDate(LocalDateTime regulatoryNotificationDate) {
        this.regulatoryNotificationDate = regulatoryNotificationDate;
    }

    public Boolean getDataSubjectNotificationRequired() {
        return dataSubjectNotificationRequired;
    }

    public void setDataSubjectNotificationRequired(Boolean dataSubjectNotificationRequired) {
        this.dataSubjectNotificationRequired = dataSubjectNotificationRequired;
    }

    public LocalDateTime getDataSubjectNotificationDate() {
        return dataSubjectNotificationDate;
    }

    public void setDataSubjectNotificationDate(LocalDateTime dataSubjectNotificationDate) {
        this.dataSubjectNotificationDate = dataSubjectNotificationDate;
    }

    public String getRegulatoryBody() {
        return regulatoryBody;
    }

    public void setRegulatoryBody(String regulatoryBody) {
        this.regulatoryBody = regulatoryBody;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getLessonsLearned() {
        return lessonsLearned;
    }

    public void setLessonsLearned(String lessonsLearned) {
        this.lessonsLearned = lessonsLearned;
    }

    public String getPreventiveMeasures() {
        return preventiveMeasures;
    }

    public void setPreventiveMeasures(String preventiveMeasures) {
        this.preventiveMeasures = preventiveMeasures;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
