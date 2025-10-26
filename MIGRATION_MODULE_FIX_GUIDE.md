# JiVS Migration Module - Fix Implementation Guide

**Created**: 2025-10-26
**Purpose**: Step-by-step code fixes for critical migration module issues
**Status**: 🚧 Implementation In Progress

---

## Quick Start - Critical Fixes (Week 1)

### Fix 1: Database Schema Migration for @Transient Fields

**Issue**: 18 fields marked @Transient are never persisted, causing data loss

**Impact**: HIGH - Migration state lost on restart

**Files**:
- `backend/src/main/resources/db/migration/V111__Add_migration_execution_fields.sql` (NEW)
- `backend/src/main/java/com/jivs/platform/domain/migration/Migration.java`

#### Step 1: Create Database Migration

```sql
-- File: backend/src/main/resources/db/migration/V111__Add_migration_execution_fields.sql

-- Add execution tracking fields
ALTER TABLE migration_projects
ADD COLUMN migration_phase VARCHAR(20),
ADD COLUMN batch_size INTEGER DEFAULT 1000,
ADD COLUMN parallelism INTEGER DEFAULT 4,
ADD COLUMN retry_attempts INTEGER DEFAULT 3,
ADD COLUMN strict_validation BOOLEAN DEFAULT false,
ADD COLUMN rollback_enabled BOOLEAN DEFAULT true,
ADD COLUMN rollback_on_cancel BOOLEAN DEFAULT false,
ADD COLUMN archive_enabled BOOLEAN DEFAULT false;

-- Add timestamp fields
ALTER TABLE migration_projects
ADD COLUMN start_time TIMESTAMP,
ADD COLUMN completion_time TIMESTAMP,
ADD COLUMN paused_time TIMESTAMP,
ADD COLUMN resumed_time TIMESTAMP,
ADD COLUMN cancelled_time TIMESTAMP,
ADD COLUMN rollback_time TIMESTAMP;

-- Add rollback tracking
ALTER TABLE migration_projects
ADD COLUMN rollback_executed BOOLEAN DEFAULT false,
ADD COLUMN rollback_failed BOOLEAN DEFAULT false,
ADD COLUMN rollback_error TEXT;

-- Add error tracking
ALTER TABLE migration_projects
ADD COLUMN error_message TEXT,
ADD COLUMN error_stack_trace TEXT;

-- Add JSON fields for complex objects
ALTER TABLE migration_projects
ADD COLUMN source_analysis JSONB,
ADD COLUMN target_analysis JSONB,
ADD COLUMN migration_plan JSONB,
ADD COLUMN resource_estimation JSONB,
ADD COLUMN validation_result JSONB,
ADD COLUMN verification_result JSONB,
ADD COLUMN migration_parameters JSONB;

-- Add metrics as embedded fields (denormalized for performance)
ALTER TABLE migration_projects
ADD COLUMN total_records INTEGER DEFAULT 0,
ADD COLUMN processed_records INTEGER DEFAULT 0,
ADD COLUMN successful_records INTEGER DEFAULT 0,
ADD COLUMN failed_records INTEGER DEFAULT 0,
ADD COLUMN extracted_records INTEGER DEFAULT 0,
ADD COLUMN transformed_records INTEGER DEFAULT 0,
ADD COLUMN loaded_records INTEGER DEFAULT 0,
ADD COLUMN validation_score DOUBLE PRECISION,
ADD COLUMN validation_errors INTEGER DEFAULT 0,
ADD COLUMN bytes_processed BIGINT DEFAULT 0,
ADD COLUMN duration_seconds BIGINT;

-- Add indexes for performance
CREATE INDEX idx_migration_phase ON migration_projects(migration_phase);
CREATE INDEX idx_migration_start_time ON migration_projects(start_time);
CREATE INDEX idx_migration_completion_time ON migration_projects(completion_time);

-- Add comments
COMMENT ON COLUMN migration_projects.migration_phase IS 'Current execution phase (PLANNING, EXTRACTION, etc.)';
COMMENT ON COLUMN migration_projects.source_analysis IS 'JSON: Source system analysis results';
COMMENT ON COLUMN migration_projects.migration_plan IS 'JSON: Generated migration execution plan';
```

#### Step 2: Update Migration Entity

```java
// File: backend/src/main/java/com/jivs/platform/domain/migration/Migration.java

package com.jivs.platform.domain.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivs.platform.service.migration.MigrationModels.*;
import com.jivs.platform.service.migration.ValidationService.ValidationResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "migration_projects", indexes = {
        @Index(name = "idx_migration_projects_status", columnList = "status"),
        @Index(name = "idx_migration_projects_code", columnList = "project_code"),
        @Index(name = "idx_migration_phase", columnList = "migration_phase"),
        @Index(name = "idx_migration_start_time", columnList = "start_time")
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

    // ✅ FIXED: Removed @Transient, now persisted to database
    @Column(name = "migration_phase", length = 20)
    @Enumerated(EnumType.STRING)
    private MigrationPhase phase = MigrationPhase.PLANNING;

    // ✅ FIXED: Execution parameters now persisted
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

    // ✅ FIXED: Timestamps now persisted
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

    // ✅ FIXED: Rollback tracking now persisted
    @Column(name = "rollback_executed")
    private boolean rollbackExecuted = false;

    @Column(name = "rollback_failed")
    private boolean rollbackFailed = false;

    @Column(name = "rollback_error", columnDefinition = "TEXT")
    private String rollbackError;

    // ✅ FIXED: Error tracking now persisted
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    // ✅ FIXED: JSON fields for complex objects
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_analysis", columnDefinition = "jsonb")
    private String sourceAnalysisJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_analysis", columnDefinition = "jsonb")
    private String targetAnalysisJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "migration_plan", columnDefinition = "jsonb")
    private String migrationPlanJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_estimation", columnDefinition = "jsonb")
    private String resourceEstimationJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_result", columnDefinition = "jsonb")
    private String validationResultJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verification_result", columnDefinition = "jsonb")
    private String verificationResultJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "migration_parameters", columnDefinition = "jsonb")
    private String parametersJson;

    // ✅ FIXED: Metrics embedded directly (denormalized for performance)
    @Column(name = "total_records")
    private Integer totalRecords = 0;

    @Column(name = "processed_records")
    private Integer processedRecords = 0;

    @Column(name = "successful_records")
    private Integer successfulRecords = 0;

    @Column(name = "failed_records")
    private Integer failedRecords = 0;

    @Column(name = "extracted_records")
    private Integer extractedRecords = 0;

    @Column(name = "transformed_records")
    private Integer transformedRecords = 0;

    @Column(name = "loaded_records")
    private Integer loadedRecords = 0;

    @Column(name = "validation_score")
    private Double validationScore;

    @Column(name = "validation_errors")
    private Integer validationErrors = 0;

    @Column(name = "bytes_processed")
    private Long bytesProcessed = 0L;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    // Audit fields
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

    // For backward compatibility - keep as @Transient
    @Transient
    private String migrationType;

    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ FIXED: Metrics getter returns object assembled from individual fields
    @Transient
    public MigrationMetrics getMetrics() {
        MigrationMetrics metrics = new MigrationMetrics();
        metrics.setTotalRecords(this.totalRecords);
        metrics.setProcessedRecords(this.processedRecords);
        metrics.setSuccessfulRecords(this.successfulRecords);
        metrics.setFailedRecords(this.failedRecords);
        metrics.setExtractedRecords(this.extractedRecords);
        metrics.setTransformedRecords(this.transformedRecords);
        metrics.setLoadedRecords(this.loadedRecords);
        metrics.setValidationScore(this.validationScore);
        metrics.setValidationErrors(this.validationErrors);
        metrics.setBytesProcessed(this.bytesProcessed);
        metrics.setDurationSeconds(this.durationSeconds);
        return metrics;
    }

    // ✅ FIXED: Metrics setter updates individual fields
    @Transient
    public void setMetrics(MigrationMetrics metrics) {
        if (metrics != null) {
            this.totalRecords = metrics.getTotalRecords();
            this.processedRecords = metrics.getProcessedRecords();
            this.successfulRecords = metrics.getSuccessfulRecords();
            this.failedRecords = metrics.getFailedRecords();
            this.extractedRecords = metrics.getExtractedRecords();
            this.transformedRecords = metrics.getTransformedRecords();
            this.loadedRecords = metrics.getLoadedRecords();
            this.validationScore = metrics.getValidationScore();
            this.validationErrors = metrics.getValidationErrors();
            this.bytesProcessed = metrics.getBytesProcessed();
            this.durationSeconds = metrics.getDurationSeconds();
        }
    }

    // ✅ FIXED: Parameters stored as JSON
    @Transient
    public Map<String, String> getParameters() {
        if (parametersJson == null || parametersJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(parametersJson,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    @Transient
    public void setParameters(Map<String, String> parameters) {
        try {
            this.parametersJson = objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize parameters", e);
        }
    }

    // JSON helper methods (same as before)
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

    private String toJson(Object object) {
        if (object == null) return null;
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null; // Return null instead of throwing, more forgiving
        }
    }

    // Standard getters/setters for all fields
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

    public MigrationPhase getPhase() { return phase; }
    public void setPhase(MigrationPhase phase) { this.phase = phase; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    // Backward compatibility
    public LocalDateTime getCreatedDate() { return createdAt; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdAt = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedAt; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedAt = updatedDate; }

    public String getMigrationType() { return migrationType; }
    public void setMigrationType(String migrationType) { this.migrationType = migrationType; }
}
```

**Testing**:
```bash
# Run migration
cd backend
mvn flyway:migrate

# Verify schema
psql -U jivs_user -d jivs -c "\d migration_projects"

# Test that fields are persisted
mvn test -Dtest=MigrationPersistenceTest
```

---

### Fix 2: Separate @Async from @Transactional

**Issue**: `executeMigration()` method has both `@Async` and `@Transactional`

**Impact**: CRITICAL - Transaction closes before async work completes

**Files**:
- `backend/src/main/java/com/jivs/platform/service/migration/MigrationOrchestrator.java`
- `backend/src/main/java/com/jivs/platform/event/MigrationExecutionEvent.java` (NEW)

#### Step 1: Create Event for Async Execution

```java
// File: backend/src/main/java/com/jivs/platform/event/MigrationExecutionEvent.java

package com.jivs.platform.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a migration is ready for async execution
 */
@Getter
public class MigrationExecutionEvent extends ApplicationEvent {

    private final Long migrationId;
    private final String migrationName;

    public MigrationExecutionEvent(Object source, Long migrationId, String migrationName) {
        super(source);
        this.migrationId = migrationId;
        this.migrationName = migrationName;
    }
}
```

#### Step 2: Refactor MigrationOrchestrator

```java
// File: backend/src/main/java/com/jivs/platform/service/migration/MigrationOrchestrator.java

package com.jivs.platform.service.migration;

import com.jivs.platform.domain.migration.Migration;
import com.jivs.platform.domain.migration.MigrationStatus;
import com.jivs.platform.domain.migration.MigrationPhase;
import com.jivs.platform.event.MigrationExecutionEvent;
import com.jivs.platform.repository.MigrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import jakarta.annotation.PreDestroy;

@Service
@RequiredArgsConstructor
public class MigrationOrchestrator {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MigrationOrchestrator.class);

    private final MigrationRepository migrationRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ✅ FIXED: Use configured executor bean instead of direct instantiation
    private final ThreadPoolTaskExecutor migrationExecutor;

    /**
     * Initiate a new migration (transactional)
     * ✅ FIXED: No longer sends RabbitMQ message directly
     */
    @Transactional
    public Migration initiateMigration(MigrationModels.MigrationRequest request) {
        log.info("Initiating migration from {} to {}",
            request.getSourceSystem(), request.getTargetSystem());

        Migration migration = new Migration();
        migration.setName(request.getName());
        migration.setDescription(request.getDescription());
        migration.setSourceSystem(request.getSourceSystem());
        migration.setTargetSystem(request.getTargetSystem());
        migration.setMigrationType(request.getMigrationType());
        migration.setStatus(MigrationStatus.INITIALIZED);
        migration.setPhase(MigrationPhase.PLANNING);
        migration.setProjectCode(generateProjectCode(request.getName()));
        migration.setProjectType(request.getMigrationType() != null ?
            request.getMigrationType() : "DATA_MIGRATION");
        migration.setCreatedBy(request.getUserId() != null ?
            request.getUserId().toString() : null);

        if (request.getParameters() != null) {
            Map<String, String> stringParams = new HashMap<>();
            request.getParameters().forEach((k, v) ->
                stringParams.put(k, v != null ? v.toString() : null)
            );
            migration.setParameters(stringParams);
        }

        migration.setBatchSize(request.getBatchSize());
        migration.setParallelism(request.getParallelism());
        migration.setRetryAttempts(request.getRetryAttempts());

        Migration savedMigration = migrationRepository.save(migration);

        // ✅ FIXED: Publish event instead of direct RabbitMQ send
        // Event listener will send message AFTER transaction commits
        eventPublisher.publishEvent(
            new MigrationExecutionEvent(this, savedMigration.getId(), savedMigration.getName())
        );

        return savedMigration;
    }

    /**
     * ✅ FIXED: Listen to event AFTER transaction commits
     * This ensures migration is persisted before RabbitMQ message is sent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMigrationCreated(MigrationExecutionEvent event) {
        log.info("Migration {} created, sending to planning queue", event.getMigrationId());

        // Now safe to send RabbitMQ message
        if (rabbitTemplate != null) {
            rabbitTemplate.convertAndSend("migration.planning", event.getMigrationId());
        }
    }

    /**
     * Execute migration workflow
     * ✅ FIXED: Removed @Transactional, each phase manages its own transaction
     */
    @Async("migrationExecutor")
    public CompletableFuture<Migration> executeMigration(Long migrationId) {
        log.info("Starting migration execution: {}", migrationId);

        try {
            // Each phase is transactional independently
            updateMigrationStatus(migrationId, MigrationStatus.IN_PROGRESS,
                MigrationPhase.PLANNING, LocalDateTime.now());

            executePlanningPhase(migrationId);
            executeExtractionPhase(migrationId);
            executeTransformationPhase(migrationId);
            executeValidationPhase(migrationId);
            executeLoadingPhase(migrationId);
            executeVerificationPhase(migrationId);
            executeCleanupPhase(migrationId);

            Migration completed = updateMigrationStatus(migrationId,
                MigrationStatus.COMPLETED, MigrationPhase.COMPLETED, null);
            completed.setCompletionTime(LocalDateTime.now());
            migrationRepository.save(completed);

            log.info("Migration {} completed successfully", migrationId);
            return CompletableFuture.completedFuture(completed);

        } catch (Exception e) {
            log.error("Migration {} failed", migrationId, e);

            Migration failed = updateMigrationStatus(migrationId,
                MigrationStatus.FAILED, MigrationPhase.FAILED, null);
            failed.setErrorMessage(e.getMessage());
            failed.setCompletionTime(LocalDateTime.now());
            Migration savedFailed = migrationRepository.save(failed);

            // Execute rollback if needed
            if (savedFailed.isRollbackEnabled()) {
                executeRollback(migrationId);
            }

            return CompletableFuture.completedFuture(savedFailed);
        }
    }

    /**
     * ✅ FIXED: Helper method to update status in separate transaction
     */
    @Transactional
    protected Migration updateMigrationStatus(Long migrationId, MigrationStatus status,
                                             MigrationPhase phase, LocalDateTime startTime) {
        Migration migration = migrationRepository.findById(migrationId)
            .orElseThrow(() -> new IllegalArgumentException("Migration not found: " + migrationId));

        migration.setStatus(status);
        if (phase != null) {
            migration.setPhase(phase);
        }
        if (startTime != null) {
            migration.setStartTime(startTime);
        }

        return migrationRepository.save(migration);
    }

    /**
     * Planning phase
     * ✅ FIXED: Now transactional and saves state
     */
    @Transactional
    protected void executePlanningPhase(Long migrationId) {
        log.info("Executing planning phase for migration: {}", migrationId);

        Migration migration = migrationRepository.findById(migrationId)
            .orElseThrow(() -> new IllegalArgumentException("Migration not found: " + migrationId));

        migration.setPhase(MigrationPhase.PLANNING);

        // Analyze source system
        MigrationModels.SourceAnalysis sourceAnalysis = analyzeSourceSystem(migration);
        migration.setSourceAnalysis(sourceAnalysis);

        // Analyze target system
        MigrationModels.TargetAnalysis targetAnalysis = analyzeTargetSystem(migration);
        migration.setTargetAnalysis(targetAnalysis);

        // Generate migration plan
        MigrationModels.MigrationPlan plan = generateMigrationPlan(
            sourceAnalysis, targetAnalysis, migration);
        migration.setPlan(plan);

        // Estimate resources and time
        MigrationModels.ResourceEstimation estimation = estimateResources(plan);
        migration.setResourceEstimation(estimation);

        // Update metrics
        migration.setTotalRecords(sourceAnalysis.getTotalRecords());

        // ✅ FIXED: Save after phase execution
        migrationRepository.save(migration);

        log.info("Planning phase completed. Total records to migrate: {}",
            sourceAnalysis.getTotalRecords());
    }

    /**
     * Extraction phase
     * ✅ FIXED: Now transactional and saves state
     */
    @Transactional
    protected void executeExtractionPhase(Long migrationId) {
        log.info("Executing extraction phase for migration: {}", migrationId);

        Migration migration = migrationRepository.findById(migrationId)
            .orElseThrow(() -> new IllegalArgumentException("Migration not found: " + migrationId));

        migration.setPhase(MigrationPhase.EXTRACTION);
        migrationRepository.save(migration);

        MigrationModels.MigrationPlan plan = migration.getPlan();
        List<CompletableFuture<MigrationModels.ExtractionResult>> futures = new ArrayList<>();

        // Extract data in batches
        for (MigrationModels.ExtractionTask task : plan.getExtractionTasks()) {
            CompletableFuture<MigrationModels.ExtractionResult> future =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return extractBatch(task, migration);
                    } catch (Exception e) {
                        log.error("Extraction failed for task: {}", task.getId(), e);
                        return new MigrationModels.ExtractionResult(task.getId(), false, e.getMessage());
                    }
                }, migrationExecutor)
                .orTimeout(30, TimeUnit.MINUTES); // ✅ FIXED: Added timeout

            futures.add(future);
        }

        // ✅ FIXED: Use allOf instead of blocking stream
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));

        try {
            allFutures.join(); // Wait for all to complete

            List<MigrationModels.ExtractionResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

            // Update metrics
            updateExtractionMetrics(migration, results);
            migrationRepository.save(migration);

            log.info("Extraction phase completed. Extracted {} records",
                migration.getExtractedRecords());

        } catch (CompletionException e) {
            throw new RuntimeException("Extraction phase failed", e.getCause());
        }
    }

    // Similar fixes for other phase methods...
    // (transformation, validation, loading, verification, cleanup)

    /**
     * ✅ FIXED: Rollback now transactional
     */
    @Transactional
    protected void executeRollback(Long migrationId) {
        log.info("Executing rollback for migration: {}", migrationId);

        Migration migration = migrationRepository.findById(migrationId)
            .orElseThrow(() -> new IllegalArgumentException("Migration not found: " + migrationId));

        try {
            migration.setStatus(MigrationStatus.ROLLING_BACK);
            migrationRepository.save(migration);

            // Identify rollback points
            List<MigrationModels.RollbackPoint> rollbackPoints = identifyRollbackPoints(migration);

            // Execute rollback in reverse order
            Collections.reverse(rollbackPoints);
            for (MigrationModels.RollbackPoint point : rollbackPoints) {
                executeRollbackPoint(point, migration);
            }

            migration.setRollbackExecuted(true);
            migration.setRollbackTime(LocalDateTime.now());
            migration.setStatus(MigrationStatus.ROLLED_BACK);

            log.info("Rollback completed successfully");

        } catch (Exception e) {
            log.error("Rollback failed", e);
            migration.setRollbackFailed(true);
            migration.setRollbackError(e.getMessage());
        }

        migrationRepository.save(migration);
    }

    /**
     * ✅ FIXED: Resource cleanup
     */
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down migration executor");
        migrationExecutor.shutdown();
        try {
            if (!migrationExecutor.getThreadPoolExecutor().awaitTermination(60, TimeUnit.SECONDS)) {
                migrationExecutor.getThreadPoolExecutor().shutdownNow();
            }
        } catch (InterruptedException e) {
            migrationExecutor.getThreadPoolExecutor().shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Helper methods (same as before, but now called from transactional methods)
    private String generateProjectCode(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "MIGRATION";
        }
        String dateStr = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);
        return String.format("MIG-%s-%05d", dateStr, random);
    }

    // ... other helper methods
}
```

#### Step 3: Configure Thread Pool

```java
// File: backend/src/main/java/com/jivs/platform/config/AsyncConfig.java

package com.jivs.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool for migration execution
     * ✅ Properly configured with monitoring
     */
    @Bean(name = "migrationExecutor")
    public ThreadPoolTaskExecutor migrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("migration-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool for async I/O operations
     */
    @Bean(name = "ioExecutor")
    public ThreadPoolTaskExecutor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("io-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
```

**Testing**:
```java
// File: backend/src/test/java/com/jivs/platform/service/migration/MigrationOrchestratorTransactionTest.java

@SpringBootTest
@Transactional
class MigrationOrchestratorTransactionTest {

    @Autowired
    private MigrationOrchestrator orchestrator;

    @Autowired
    private MigrationRepository repository;

    @Test
    void testInitiateMigration_SavesBeforeEventPublished() {
        // Given
        MigrationModels.MigrationRequest request = new MigrationModels.MigrationRequest();
        request.setName("Test Migration");
        request.setSourceSystem("MySQL");
        request.setTargetSystem("PostgreSQL");

        // When
        Migration migration = orchestrator.initiateMigration(request);

        // Then
        assertNotNull(migration.getId());

        // Verify migration is in database before event fires
        Migration found = repository.findById(migration.getId()).orElseThrow();
        assertEquals(MigrationStatus.INITIALIZED, found.getStatus());
    }

    @Test
    void testExecuteMigration_PhasesArePersisted() throws Exception {
        // Given
        Migration migration = createTestMigration();
        repository.save(migration);

        // When
        CompletableFuture<Migration> future = orchestrator.executeMigration(migration.getId());
        Migration result = future.get(5, TimeUnit.MINUTES);

        // Then
        Migration persisted = repository.findById(migration.getId()).orElseThrow();
        assertNotNull(persisted.getStartTime());
        assertNotNull(persisted.getCompletionTime());
        assertEquals(MigrationPhase.COMPLETED, persisted.getPhase());
    }
}
```

---

### Fix 3: Restrict CORS Origins

**Issue**: `@CrossOrigin(origins = "*")` allows all origins

**Impact**: HIGH - CSRF vulnerability

**File**: `backend/src/main/java/com/jivs/platform/controller/MigrationController.java`

```java
// BEFORE (INSECURE):
@CrossOrigin(origins = "*", maxAge = 3600)
public class MigrationController {

// ✅ AFTER (SECURE):
@CrossOrigin(
    origins = "${jivs.cors.allowed-origins}",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE},
    allowedHeaders = {"Authorization", "Content-Type"},
    maxAge = 3600
)
public class MigrationController {
```

**Configuration**:
```yaml
# File: backend/src/main/resources/application.yml

jivs:
  cors:
    allowed-origins:
      - http://localhost:3001
      - http://localhost:3000

---
# Production profile
spring:
  config:
    activate:
      on-profile: production

jivs:
  cors:
    allowed-origins:
      - https://app.jivs.com
      - https://jivs-app.company.com
```

**Better Approach - Global CORS Configuration**:
```java
// File: backend/src/main/java/com/jivs/platform/config/WebSecurityConfig.java

@Configuration
public class WebSecurityConfig {

    @Value("${jivs.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("X-Total-Count", "X-Page-Number"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## Continue to Part 2...

This guide has covered the first 3 critical fixes:
1. ✅ Database schema migration for @Transient fields
2. ✅ Separating @Async from @Transactional
3. ✅ Restricting CORS origins

**Next**: Would you like me to continue with:
- Fix 4: SQL Injection Prevention
- Fix 5: Resource Leak Fixes
- Fix 6: Input Validation DTOs
- Fix 7: Error Handling Improvements
- Complete test implementation examples

Let me know which fixes you'd like to see next!
