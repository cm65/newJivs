package com.jivs.platform.domain.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivs.platform.service.migration.MigrationModels.*;
import com.jivs.platform.service.migration.ValidationService.ValidationResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Migration entity representing a data migration project
 * Maps to migration_projects table
 * Note: This entity will need refactoring - should be split into MigrationProject and MigrationJob entities
 */
@Entity
@Table(name = "migration_projects", indexes = {
        @Index(name = "idx_migration_projects_status", columnList = "status"),
        @Index(name = "idx_migration_projects_code", columnList = "project_code")
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public class Migration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_code", nullable = false, unique = true, length = 50)
    private String projectCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "target_system", nullable = false, length = 100)
    private String targetSystem;

    @Column(name = "project_type", nullable = false, length = 50)
    private String projectType;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MigrationStatus status = MigrationStatus.INITIALIZED;

    @Column(length = 20)
    private String priority = "MEDIUM";

    @Column(name = "start_date")
    private java.time.LocalDate startDate;

    @Column(name = "end_date")
    private java.time.LocalDate endDate;

    @Column(name = "planned_cutover_date")
    private java.time.LocalDate plannedCutoverDate;

    @Column(name = "actual_cutover_date")
    private java.time.LocalDate actualCutoverDate;

    @Column(name = "estimated_records")
    private Long estimatedRecords;

    @Column(name = "estimated_size_gb")
    private java.math.BigDecimal estimatedSizeGb;

    @Column(name = "project_metadata", columnDefinition = "jsonb")
    private String projectMetadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // Fields below are NOT in database - marked as @Transient
    @Transient
    private String migrationType;

    @Transient
    private MigrationPhase phase = MigrationPhase.PLANNING;

    @Transient
    private MigrationMetrics metrics = new MigrationMetrics();

    @Transient
    private Map<String, String> parameters = new HashMap<>();

    @Transient
    private Integer batchSize = 1000;

    @Transient
    private Integer parallelism = 4;

    @Transient
    private Integer retryAttempts = 3;

    @Transient
    private boolean strictValidation = false;

    @Transient
    private boolean rollbackEnabled = true;

    @Transient
    private boolean rollbackOnCancel = false;

    @Transient
    private boolean archiveEnabled = false;

    @Transient
    private LocalDateTime startTime;

    @Transient
    private LocalDateTime completionTime;

    @Transient
    private LocalDateTime pausedTime;

    @Transient
    private LocalDateTime resumedTime;

    @Transient
    private LocalDateTime cancelledTime;

    @Transient
    private LocalDateTime rollbackTime;

    @Transient
    private boolean rollbackExecuted = false;

    @Transient
    private boolean rollbackFailed = false;

    @Transient
    private String rollbackError;

    @Transient
    private String errorMessage;

    @Transient
    private String errorStackTrace;

    @Transient
    private String sourceAnalysisJson;

    @Transient
    private String targetAnalysisJson;

    @Transient
    private String migrationPlanJson;

    @Transient
    private String resourceEstimationJson;

    @Transient
    private String validationResultJson;

    @Transient
    private String verificationResultJson;

    // Static ObjectMapper for JSON serialization/deserialization
    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Getters and Setters for DB fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public MigrationStatus getStatus() { return status; }
    public void setStatus(MigrationStatus status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public java.time.LocalDate getStartDate() { return startDate; }
    public void setStartDate(java.time.LocalDate startDate) { this.startDate = startDate; }

    public java.time.LocalDate getEndDate() { return endDate; }
    public void setEndDate(java.time.LocalDate endDate) { this.endDate = endDate; }

    public java.time.LocalDate getPlannedCutoverDate() { return plannedCutoverDate; }
    public void setPlannedCutoverDate(java.time.LocalDate plannedCutoverDate) { this.plannedCutoverDate = plannedCutoverDate; }

    public java.time.LocalDate getActualCutoverDate() { return actualCutoverDate; }
    public void setActualCutoverDate(java.time.LocalDate actualCutoverDate) { this.actualCutoverDate = actualCutoverDate; }

    public Long getEstimatedRecords() { return estimatedRecords; }
    public void setEstimatedRecords(Long estimatedRecords) { this.estimatedRecords = estimatedRecords; }

    public java.math.BigDecimal getEstimatedSizeGb() { return estimatedSizeGb; }
    public void setEstimatedSizeGb(java.math.BigDecimal estimatedSizeGb) { this.estimatedSizeGb = estimatedSizeGb; }

    public String getProjectMetadata() { return projectMetadata; }
    public void setProjectMetadata(String projectMetadata) { this.projectMetadata = projectMetadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    // Getters and Setters for @Transient fields (for backward compatibility)
    public String getMigrationType() { return migrationType; }
    public void setMigrationType(String migrationType) { this.migrationType = migrationType; }

    public MigrationPhase getPhase() { return phase; }
    public void setPhase(MigrationPhase phase) { this.phase = phase; }

    public MigrationMetrics getMetrics() { return metrics; }
    public void setMetrics(MigrationMetrics metrics) { this.metrics = metrics; }

    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }

    public Integer getBatchSize() { return batchSize; }
    public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }

    public Integer getParallelism() { return parallelism; }
    public void setParallelism(Integer parallelism) { this.parallelism = parallelism; }

    public Integer getRetryAttempts() { return retryAttempts; }
    public void setRetryAttempts(Integer retryAttempts) { this.retryAttempts = retryAttempts; }

    public boolean isStrictValidation() { return strictValidation; }
    public void setStrictValidation(boolean strictValidation) { this.strictValidation = strictValidation; }

    public boolean isRollbackEnabled() { return rollbackEnabled; }
    public void setRollbackEnabled(boolean rollbackEnabled) { this.rollbackEnabled = rollbackEnabled; }

    public boolean isRollbackOnCancel() { return rollbackOnCancel; }
    public void setRollbackOnCancel(boolean rollbackOnCancel) { this.rollbackOnCancel = rollbackOnCancel; }

    public boolean isArchiveEnabled() { return archiveEnabled; }
    public void setArchiveEnabled(boolean archiveEnabled) { this.archiveEnabled = archiveEnabled; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getCompletionTime() { return completionTime; }
    public void setCompletionTime(LocalDateTime completionTime) { this.completionTime = completionTime; }

    public LocalDateTime getPausedTime() { return pausedTime; }
    public void setPausedTime(LocalDateTime pausedTime) { this.pausedTime = pausedTime; }

    public LocalDateTime getResumedTime() { return resumedTime; }
    public void setResumedTime(LocalDateTime resumedTime) { this.resumedTime = resumedTime; }

    public LocalDateTime getCancelledTime() { return cancelledTime; }
    public void setCancelledTime(LocalDateTime cancelledTime) { this.cancelledTime = cancelledTime; }

    public LocalDateTime getRollbackTime() { return rollbackTime; }
    public void setRollbackTime(LocalDateTime rollbackTime) { this.rollbackTime = rollbackTime; }

    public boolean isRollbackExecuted() { return rollbackExecuted; }
    public void setRollbackExecuted(boolean rollbackExecuted) { this.rollbackExecuted = rollbackExecuted; }

    public boolean isRollbackFailed() { return rollbackFailed; }
    public void setRollbackFailed(boolean rollbackFailed) { this.rollbackFailed = rollbackFailed; }

    public String getRollbackError() { return rollbackError; }
    public void setRollbackError(String rollbackError) { this.rollbackError = rollbackError; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getErrorStackTrace() { return errorStackTrace; }
    public void setErrorStackTrace(String errorStackTrace) { this.errorStackTrace = errorStackTrace; }

    // Backward compatibility for old field names
    public LocalDateTime getCreatedDate() { return createdAt; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdAt = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedAt; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedAt = updatedDate; }

    // Helper methods to work with JSON fields
    @Transient
    public void setSourceAnalysis(SourceAnalysis sourceAnalysis) {
        this.sourceAnalysisJson = toJson(sourceAnalysis);
    }

    @Transient
    public SourceAnalysis getSourceAnalysis() {
        return fromJson(sourceAnalysisJson, SourceAnalysis.class);
    }

    @Transient
    public void setTargetAnalysis(TargetAnalysis targetAnalysis) {
        this.targetAnalysisJson = toJson(targetAnalysis);
    }

    @Transient
    public TargetAnalysis getTargetAnalysis() {
        return fromJson(targetAnalysisJson, TargetAnalysis.class);
    }

    @Transient
    public void setPlan(MigrationPlan plan) {
        this.migrationPlanJson = toJson(plan);
    }

    @Transient
    public MigrationPlan getPlan() {
        return fromJson(migrationPlanJson, MigrationPlan.class);
    }

    @Transient
    public void setResourceEstimation(ResourceEstimation estimation) {
        this.resourceEstimationJson = toJson(estimation);
    }

    @Transient
    public ResourceEstimation getResourceEstimation() {
        return fromJson(resourceEstimationJson, ResourceEstimation.class);
    }

    @Transient
    public void setValidationResult(ValidationResult result) {
        this.validationResultJson = toJson(result);
    }

    @Transient
    public ValidationResult getValidationResult() {
        return fromJson(validationResultJson, ValidationResult.class);
    }

    @Transient
    public void setVerificationResult(VerificationResult result) {
        this.verificationResultJson = toJson(result);
    }

    @Transient
    public VerificationResult getVerificationResult() {
        return fromJson(verificationResultJson, VerificationResult.class);
    }

    // JSON serialization helper methods
    private String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + clazz.getSimpleName(), e);
        }
    }
}
