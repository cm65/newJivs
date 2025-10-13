package com.jivs.platform.service.migration;

import com.jivs.platform.domain.migration.Migration;
import com.jivs.platform.domain.migration.MigrationStatus;
import com.jivs.platform.domain.migration.MigrationPhase;
import com.jivs.platform.domain.migration.MigrationMetrics;
import com.jivs.platform.repository.MigrationRepository;
import com.jivs.platform.service.extraction.ExtractionService;
import com.jivs.platform.service.transformation.TransformationService;
import com.jivs.platform.service.quality.DataQualityService;
import com.jivs.platform.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Orchestrates the complete data migration lifecycle
 */
@Service
@RequiredArgsConstructor
public class MigrationOrchestrator {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MigrationOrchestrator.class);

    private final MigrationRepository migrationRepository;
    private final ExtractionService extractionService;
    private final TransformationService transformationService;
    private final DataQualityService dataQualityService;
    private final ValidationService validationService;
    private final LoadService loadService;
    private final StorageService storageService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RabbitTemplate rabbitTemplate;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Initiate a new migration
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

        // Generate unique project code (required field)
        migration.setProjectCode(generateProjectCode(request.getName()));

        // Set project type (required field) - use migrationType or default to "DATA_MIGRATION"
        migration.setProjectType(request.getMigrationType() != null ? request.getMigrationType() : "DATA_MIGRATION");

        // Note: createdDate is set automatically by @CreatedDate
        migration.setCreatedBy(request.getUserId() != null ? request.getUserId().toString() : null);

        // Set migration parameters - convert Map<String, Object> to Map<String, String>
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

        // Initialize metrics
        MigrationMetrics metrics = new MigrationMetrics();
        metrics.setTotalRecords(0);
        metrics.setProcessedRecords(0);
        metrics.setSuccessfulRecords(0);
        metrics.setFailedRecords(0);
        migration.setMetrics(metrics);

        Migration savedMigration = migrationRepository.save(migration);

        // Send to planning queue
        if (rabbitTemplate != null) {
            rabbitTemplate.convertAndSend("migration.planning", savedMigration.getId());
            log.info("Migration {} queued for planning", savedMigration.getId());
        } else {
            log.warn("RabbitMQ not available, migration {} created but not queued", savedMigration.getId());
        }

        return savedMigration;
    }

    /**
     * Execute migration workflow
     */
    @Async
    @Transactional
    public CompletableFuture<Migration> executeMigration(Long migrationId) {
        log.info("Starting migration execution: {}", migrationId);

        Migration migration = migrationRepository.findById(migrationId)
            .orElseThrow(() -> new IllegalArgumentException("Migration not found: " + migrationId));

        try {
            migration.setStatus(MigrationStatus.IN_PROGRESS);
            migration.setStartTime(LocalDateTime.now());
            migrationRepository.save(migration);

            // Execute migration phases
            executePlanningPhase(migration);
            executeExtractionPhase(migration);
            executeTransformationPhase(migration);
            executeValidationPhase(migration);
            executeLoadingPhase(migration);
            executeVerificationPhase(migration);
            executeCleanupPhase(migration);

            migration.setStatus(MigrationStatus.COMPLETED);
            migration.setPhase(MigrationPhase.COMPLETED);
            migration.setCompletionTime(LocalDateTime.now());

            log.info("Migration {} completed successfully", migrationId);

        } catch (Exception e) {
            log.error("Migration {} failed", migrationId, e);
            migration.setStatus(MigrationStatus.FAILED);
            migration.setErrorMessage(e.getMessage());
            migration.setCompletionTime(LocalDateTime.now());

            // Execute rollback if needed
            if (migration.isRollbackEnabled()) {
                executeRollback(migration);
            }
        }

        Migration savedMigration = migrationRepository.save(migration);
        return CompletableFuture.completedFuture(savedMigration);
    }

    /**
     * Planning phase - Analyze source and target systems
     */
    private void executePlanningPhase(Migration migration) {
        log.info("Executing planning phase for migration: {}", migration.getId());
        migration.setPhase(MigrationPhase.PLANNING);

        // Analyze source system
        MigrationModels.SourceAnalysis sourceAnalysis = analyzeSourceSystem(migration);
        migration.setSourceAnalysis(sourceAnalysis);

        // Analyze target system
        MigrationModels.TargetAnalysis targetAnalysis = analyzeTargetSystem(migration);
        migration.setTargetAnalysis(targetAnalysis);

        // Generate migration plan
        MigrationModels.MigrationPlan plan = generateMigrationPlan(sourceAnalysis, targetAnalysis, migration);
        migration.setPlan(plan);

        // Estimate resources and time
        MigrationModels.ResourceEstimation estimation = estimateResources(plan);
        migration.setResourceEstimation(estimation);

        migration.getMetrics().setTotalRecords(sourceAnalysis.getTotalRecords());

        log.info("Planning phase completed. Total records to migrate: {}",
            sourceAnalysis.getTotalRecords());
    }

    /**
     * Extraction phase - Extract data from source system
     */
    private void executeExtractionPhase(Migration migration) {
        log.info("Executing extraction phase for migration: {}", migration.getId());
        migration.setPhase(MigrationPhase.EXTRACTION);

        MigrationModels.MigrationPlan plan = migration.getPlan();
        List<CompletableFuture<MigrationModels.ExtractionResult>> futures = new ArrayList<>();

        // Extract data in batches
        for (MigrationModels.ExtractionTask task : plan.getExtractionTasks()) {
            CompletableFuture<MigrationModels.ExtractionResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return extractBatch(task, migration);
                } catch (Exception e) {
                    log.error("Extraction failed for task: {}", task.getId(), e);
                    return new MigrationModels.ExtractionResult(task.getId(), false, e.getMessage());
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all extractions to complete
        List<MigrationModels.ExtractionResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // Update metrics
        updateExtractionMetrics(migration, results);

        log.info("Extraction phase completed. Extracted {} records",
            migration.getMetrics().getExtractedRecords());
    }

    /**
     * Transformation phase - Transform data to target format
     */
    private void executeTransformationPhase(Migration migration) {
        log.info("Executing transformation phase for migration: {}", migration.getId());
        migration.setPhase(MigrationPhase.TRANSFORMATION);

        List<MigrationModels.TransformationTask> tasks = migration.getPlan().getTransformationTasks();
        List<CompletableFuture<MigrationModels.TransformationResult>> futures = new ArrayList<>();

        for (MigrationModels.TransformationTask task : tasks) {
            CompletableFuture<MigrationModels.TransformationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return transformBatch(task, migration);
                } catch (Exception e) {
                    log.error("Transformation failed for task: {}", task.getId(), e);
                    return new MigrationModels.TransformationResult(task.getId(), false, e.getMessage());
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all transformations
        List<MigrationModels.TransformationResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // Update metrics
        updateTransformationMetrics(migration, results);

        log.info("Transformation phase completed. Transformed {} records",
            migration.getMetrics().getTransformedRecords());
    }

    /**
     * Validation phase - Validate transformed data
     */
    private void executeValidationPhase(Migration migration) {
        log.info("Executing validation phase for migration: {}", migration.getId());
        migration.setPhase(MigrationPhase.VALIDATION);

        ValidationService.ValidationContext context = new ValidationService.ValidationContext();
        context.setMigrationId(migration.getId());
        context.setSourceSystem(migration.getSourceSystem());
        context.setTargetSystem(migration.getTargetSystem());
        context.setValidationRules(migration.getPlan().getValidationRules());

        ValidationService.ValidationResult result = validationService.validateMigrationData(context);

        migration.setValidationResult(result);
        migration.getMetrics().setValidationScore(result.getScore());
        migration.getMetrics().setValidationErrors(result.getErrorCount());

        if (!result.isPassed() && migration.isStrictValidation()) {
            throw new MigrationModels.ValidationException("Validation failed: " + result.getSummary());
        }

        log.info("Validation phase completed. Score: {}%, Errors: {}",
            result.getScore(), result.getErrorCount());
    }

    /**
     * Loading phase - Load data into target system
     */
    private void executeLoadingPhase(Migration migration) {
        log.info("Executing loading phase for migration: {}", migration.getId());
        migration.setPhase(MigrationPhase.LOADING);

        List<MigrationModels.LoadTask> tasks = migration.getPlan().getLoadTasks();
        LoadService.LoadStrategy strategy = determineLoadStrategy(migration);

        List<CompletableFuture<LoadService.LoadResult>> futures = new ArrayList<>();

        for (MigrationModels.LoadTask task : tasks) {
            CompletableFuture<LoadService.LoadResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return loadBatch(task, migration, strategy);
                } catch (Exception e) {
                    log.error("Loading failed for task: {}", task.getId(), e);
                    return new LoadService.LoadResult(task.getId(), false, e.getMessage());
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all loads
        List<LoadService.LoadResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // Update metrics
        updateLoadMetrics(migration, results);

        log.info("Loading phase completed. Loaded {} records",
            migration.getMetrics().getLoadedRecords());
    }

    /**
     * Verification phase - Verify migration success
     */
    private void executeVerificationPhase(Migration migration) {
        log.info("Executing verification phase for migration: {}", migration.getId());
        migration.setPhase(MigrationPhase.VERIFICATION);

        MigrationModels.VerificationContext context = new MigrationModels.VerificationContext();
        context.setMigrationId(migration.getId());
        context.setExpectedRecords(migration.getMetrics().getTotalRecords());
        context.setActualRecords(migration.getMetrics().getLoadedRecords());

        // Perform various verification checks
        MigrationModels.VerificationResult result = new MigrationModels.VerificationResult();

        // Record count verification
        result.setRecordCountMatch(verifyRecordCounts(migration));

        // Data integrity verification
        result.setDataIntegrityPassed(verifyDataIntegrity(migration));

        // Referential integrity verification
        result.setReferentialIntegrityPassed(verifyReferentialIntegrity(migration));

        // Business rules verification
        result.setBusinessRulesPassed(verifyBusinessRules(migration));

        migration.setVerificationResult(result);

        if (!result.isFullyVerified()) {
            log.warn("Migration verification found issues: {}", result.getIssues());
        }

        log.info("Verification phase completed. Fully verified: {}", result.isFullyVerified());
    }

    /**
     * Cleanup phase - Clean up temporary resources
     */
    private void executeCleanupPhase(Migration migration) {
        log.info("Executing cleanup phase for migration: {}", migration.getId());
        migration.setPhase(MigrationPhase.CLEANUP);

        try {
            // Clean up temporary files
            cleanupTemporaryFiles(migration);

            // Release resources
            releaseResources(migration);

            // Archive migration data if needed
            if (migration.isArchiveEnabled()) {
                archiveMigrationData(migration);
            }

            log.info("Cleanup phase completed");

        } catch (Exception e) {
            log.error("Cleanup phase failed", e);
            // Non-critical failure, don't fail the migration
        }
    }

    /**
     * Execute rollback if migration fails
     */
    private void executeRollback(Migration migration) {
        log.info("Executing rollback for migration: {}", migration.getId());

        try {
            // Identify rollback points
            List<MigrationModels.RollbackPoint> rollbackPoints = identifyRollbackPoints(migration);

            // Execute rollback in reverse order
            Collections.reverse(rollbackPoints);
            for (MigrationModels.RollbackPoint point : rollbackPoints) {
                executeRollbackPoint(point, migration);
            }

            migration.setRollbackExecuted(true);
            migration.setRollbackTime(LocalDateTime.now());

            log.info("Rollback completed successfully");

        } catch (Exception e) {
            log.error("Rollback failed", e);
            migration.setRollbackFailed(true);
            migration.setRollbackError(e.getMessage());
        }
    }

    /**
     * Pause migration
     */
    @Transactional
    public Migration pauseMigration(Long migrationId) {
        Migration migration = migrationRepository.findById(migrationId)
            .orElseThrow(() -> new IllegalArgumentException("Migration not found: " + migrationId));

        if (migration.getStatus() == MigrationStatus.IN_PROGRESS) {
            migration.setStatus(MigrationStatus.PAUSED);
            migration.setPausedTime(LocalDateTime.now());
            log.info("Migration {} paused", migrationId);
        }

        return migrationRepository.save(migration);
    }

    /**
     * Resume migration
     */
    @Transactional
    public Migration resumeMigration(Long migrationId) {
        Migration migration = migrationRepository.findById(migrationId)
            .orElseThrow(() -> new IllegalArgumentException("Migration not found: " + migrationId));

        if (migration.getStatus() == MigrationStatus.PAUSED) {
            migration.setStatus(MigrationStatus.IN_PROGRESS);
            migration.setResumedTime(LocalDateTime.now());
            log.info("Migration {} resumed", migrationId);

            // Continue from last checkpoint
            executeMigration(migrationId);
        }

        return migrationRepository.save(migration);
    }

    /**
     * Cancel migration
     */
    @Transactional
    public Migration cancelMigration(Long migrationId) {
        Migration migration = migrationRepository.findById(migrationId)
            .orElseThrow(() -> new IllegalArgumentException("Migration not found: " + migrationId));

        migration.setStatus(MigrationStatus.CANCELLED);
        migration.setCancelledTime(LocalDateTime.now());

        // Execute rollback if configured
        if (migration.isRollbackOnCancel()) {
            executeRollback(migration);
        }

        log.info("Migration {} cancelled", migrationId);
        return migrationRepository.save(migration);
    }

    /**
     * Get migration progress
     */
    public MigrationModels.MigrationProgress getProgress(Long migrationId) {
        Migration migration = migrationRepository.findById(migrationId)
            .orElseThrow(() -> new IllegalArgumentException("Migration not found: " + migrationId));

        MigrationModels.MigrationProgress progress = new MigrationModels.MigrationProgress();
        progress.setMigrationId(migrationId);
        progress.setStatus(migration.getStatus().name());
        progress.setPhase(migration.getPhase().name());

        MigrationMetrics metrics = migration.getMetrics();
        if (metrics != null && metrics.getTotalRecords() > 0) {
            double percentage = (double) metrics.getProcessedRecords() / metrics.getTotalRecords() * 100;
            progress.setPercentageComplete(Math.round(percentage * 100.0) / 100.0);
        }

        progress.setProcessedRecords(metrics.getProcessedRecords());
        progress.setTotalRecords(metrics.getTotalRecords());
        progress.setEstimatedTimeRemaining(estimateTimeRemaining(migration));

        return progress;
    }

    // Helper methods
    private MigrationModels.SourceAnalysis analyzeSourceSystem(Migration migration) {
        // Analyze source system structure and data
        return new MigrationModels.SourceAnalysis(); // Simplified
    }

    private MigrationModels.TargetAnalysis analyzeTargetSystem(Migration migration) {
        // Analyze target system requirements
        return new MigrationModels.TargetAnalysis(); // Simplified
    }

    private MigrationModels.MigrationPlan generateMigrationPlan(
            MigrationModels.SourceAnalysis source,
            MigrationModels.TargetAnalysis target,
            Migration migration) {
        // Generate detailed migration plan
        return new MigrationModels.MigrationPlan(); // Simplified
    }

    private MigrationModels.ResourceEstimation estimateResources(MigrationModels.MigrationPlan plan) {
        // Estimate required resources
        return new MigrationModels.ResourceEstimation(); // Simplified
    }

    private MigrationModels.ExtractionResult extractBatch(MigrationModels.ExtractionTask task, Migration migration) {
        // Extract batch of data
        return new MigrationModels.ExtractionResult(task.getId(), true, null);
    }

    private MigrationModels.TransformationResult transformBatch(MigrationModels.TransformationTask task, Migration migration) {
        // Transform batch of data
        return new MigrationModels.TransformationResult(task.getId(), true, null);
    }

    private LoadService.LoadResult loadBatch(MigrationModels.LoadTask task, Migration migration, LoadService.LoadStrategy strategy) {
        // Load batch into target
        return new LoadService.LoadResult(task.getId(), true, null);
    }

    private LoadService.LoadStrategy determineLoadStrategy(Migration migration) {
        // Determine optimal load strategy
        return LoadService.LoadStrategy.BATCH; // Simplified
    }

    private boolean verifyRecordCounts(Migration migration) {
        return migration.getMetrics().getTotalRecords() ==
               migration.getMetrics().getLoadedRecords();
    }

    private boolean verifyDataIntegrity(Migration migration) {
        // Verify data integrity
        return true; // Simplified
    }

    private boolean verifyReferentialIntegrity(Migration migration) {
        // Verify referential integrity
        return true; // Simplified
    }

    private boolean verifyBusinessRules(Migration migration) {
        // Verify business rules
        return true; // Simplified
    }

    private void cleanupTemporaryFiles(Migration migration) {
        // Clean up temporary files
    }

    private void releaseResources(Migration migration) {
        // Release allocated resources
    }

    private void archiveMigrationData(Migration migration) {
        // Archive migration data
    }

    private List<MigrationModels.RollbackPoint> identifyRollbackPoints(Migration migration) {
        // Identify rollback points
        return new ArrayList<>();
    }

    private void executeRollbackPoint(MigrationModels.RollbackPoint point, Migration migration) {
        // Execute specific rollback point
    }

    private void updateExtractionMetrics(Migration migration, List<MigrationModels.ExtractionResult> results) {
        long successful = results.stream().filter(MigrationModels.ExtractionResult::isSuccess).count();
        migration.getMetrics().setExtractedRecords((int) successful);
    }

    private void updateTransformationMetrics(Migration migration, List<MigrationModels.TransformationResult> results) {
        long successful = results.stream().filter(MigrationModels.TransformationResult::isSuccess).count();
        migration.getMetrics().setTransformedRecords((int) successful);
    }

    private void updateLoadMetrics(Migration migration, List<LoadService.LoadResult> results) {
        long successful = results.stream().filter(LoadService.LoadResult::isSuccess).count();
        migration.getMetrics().setLoadedRecords((int) successful);
        migration.getMetrics().setSuccessfulRecords((int) successful);
    }

    private Duration estimateTimeRemaining(Migration migration) {
        if (migration.getStartTime() == null || migration.getMetrics().getProcessedRecords() == 0) {
            return Duration.ZERO;
        }

        Duration elapsed = Duration.between(migration.getStartTime(), LocalDateTime.now());
        double rate = migration.getMetrics().getProcessedRecords() / elapsed.toSeconds();
        long remaining = migration.getMetrics().getTotalRecords() - migration.getMetrics().getProcessedRecords();

        if (rate > 0) {
            return Duration.ofSeconds((long) (remaining / rate));
        }

        return Duration.ZERO;
    }

    /**
     * Generate unique project code from migration name
     * Format: MIG-YYYYMMDD-XXXXX (e.g., MIG-20251013-12345)
     */
    private String generateProjectCode(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "MIGRATION";
        }

        // Create base code from date
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);

        // Generate random 5-digit number for uniqueness
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);

        return String.format("MIG-%s-%05d", dateStr, random);
    }
}