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
 * Migration entity representing a data migration job
 */
@Entity
@Table(name = "migrations", indexes = {
        @Index(name = "idx_migrations_status", columnList = "status"),
        @Index(name = "idx_migrations_phase", columnList = "phase"),
        @Index(name = "idx_migrations_source_system", columnList = "source_system"),
        @Index(name = "idx_migrations_target_system", columnList = "target_system"),
        @Index(name = "idx_migrations_created_date", columnList = "created_date")
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
public class Migration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "target_system", nullable = false, length = 100)
    private String targetSystem;

    @Column(name = "migration_type", length = 50)
    private String migrationType;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MigrationStatus status = MigrationStatus.INITIALIZED;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MigrationPhase phase = MigrationPhase.PLANNING;

    @Embedded
    private MigrationMetrics metrics = new MigrationMetrics();

    @ElementCollection
    @CollectionTable(name = "migration_parameters",
                     joinColumns = @JoinColumn(name = "migration_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value", columnDefinition = "TEXT")
    private Map<String, String> parameters = new HashMap<>();

    @Column(name = "batch_size")
    private Integer batchSize = 1000;

    @Column(name = "parallelism")
    private Integer parallelism = 4;

    @Column(name = "retry_attempts")
    private Integer retryAttempts = 3;

    @Column(name = "strict_validation")
    private boolean strictValidation = false;

    @Column(name = "rollback_enabled")
    private boolean rollbackEnabled = true;

    @Column(name = "rollback_on_cancel")
    private boolean rollbackOnCancel = false;

    @Column(name = "archive_enabled")
    private boolean archiveEnabled = false;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    @Column(name = "paused_time")
    private LocalDateTime pausedTime;

    @Column(name = "resumed_time")
    private LocalDateTime resumedTime;

    @Column(name = "cancelled_time")
    private LocalDateTime cancelledTime;

    @Column(name = "rollback_time")
    private LocalDateTime rollbackTime;

    @Column(name = "rollback_executed")
    private boolean rollbackExecuted = false;

    @Column(name = "rollback_failed")
    private boolean rollbackFailed = false;

    @Column(name = "rollback_error", columnDefinition = "TEXT")
    private String rollbackError;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    // Stored as JSON
    @Column(name = "source_analysis", columnDefinition = "TEXT")
    private String sourceAnalysisJson;

    @Column(name = "target_analysis", columnDefinition = "TEXT")
    private String targetAnalysisJson;

    @Column(name = "migration_plan", columnDefinition = "TEXT")
    private String migrationPlanJson;

    @Column(name = "resource_estimation", columnDefinition = "TEXT")
    private String resourceEstimationJson;

    @Column(name = "validation_result", columnDefinition = "TEXT")
    private String validationResultJson;

    @Column(name = "verification_result", columnDefinition = "TEXT")
    private String verificationResultJson;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // Static ObjectMapper for JSON serialization/deserialization
    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }

    public String getMigrationType() { return migrationType; }
    public void setMigrationType(String migrationType) { this.migrationType = migrationType; }

    public MigrationStatus getStatus() { return status; }
    public void setStatus(MigrationStatus status) { this.status = status; }

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

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

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
