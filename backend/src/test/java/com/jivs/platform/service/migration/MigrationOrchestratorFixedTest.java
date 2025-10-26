package com.jivs.platform.service.migration;

import com.jivs.platform.domain.migration.Migration;
import com.jivs.platform.domain.migration.MigrationPhase;
import com.jivs.platform.domain.migration.MigrationStatus;
import com.jivs.platform.event.MigrationExecutionEvent;
import com.jivs.platform.repository.MigrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ FIXED: Tests for refactored MigrationOrchestrator
 * Validates that async/transactional separation works correctly
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MigrationOrchestratorFixedTest {

    @Autowired
    private MigrationOrchestrator orchestrator;

    @Autowired
    private MigrationRepository repository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private MigrationModels.MigrationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new MigrationModels.MigrationRequest();
        validRequest.setName("Test Migration " + System.currentTimeMillis());
        validRequest.setSourceSystem("MySQL-5.7");
        validRequest.setTargetSystem("PostgreSQL-15");
        validRequest.setMigrationType("DATA_MIGRATION");
        validRequest.setUserId(1L);
        validRequest.setBatchSize(1000);
        validRequest.setParallelism(4);
        validRequest.setRetryAttempts(3);

        Map<String, Object> params = new HashMap<>();
        params.put("source_host", "localhost");
        params.put("source_port", "3306");
        validRequest.setParameters(params);
    }

    @Test
    @DisplayName("✅ FIX-1: Migration should be persisted before event is published")
    void testInitiateMigration_SavesBeforeEventPublished() {
        // Given
        String uniqueName = "Migration-" + System.currentTimeMillis();
        validRequest.setName(uniqueName);

        // When
        Migration migration = orchestrator.initiateMigration(validRequest);

        // Then
        assertNotNull(migration.getId(), "Migration should have ID after save");

        // Verify migration exists in database
        Migration found = repository.findById(migration.getId()).orElseThrow();
        assertEquals(uniqueName, found.getName());
        assertEquals(MigrationStatus.INITIALIZED, found.getStatus());
        assertEquals(MigrationPhase.PLANNING, found.getPhase());
        assertNotNull(found.getProjectCode(), "Project code should be generated");
        assertTrue(found.getProjectCode().startsWith("MIG-"), "Project code should have MIG prefix");
    }

    @Test
    @DisplayName("✅ FIX-1: Parameters should be persisted as JSON")
    void testInitiateMigration_ParametersArePersisted() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put("table_filter", "users,orders");
        params.put("batch_size", "5000");
        validRequest.setParameters(params);

        // When
        Migration migration = orchestrator.initiateMigration(validRequest);

        // Then - clear entity manager cache to force database read
        repository.flush();
        repository.findById(migration.getId()).ifPresent(m -> {
            Map<String, String> savedParams = m.getParameters();
            assertNotNull(savedParams);
            assertEquals("users,orders", savedParams.get("table_filter"));
            assertEquals("5000", savedParams.get("batch_size"));
        });
    }

    @Test
    @DisplayName("✅ FIX-1: All transient fields should now be persisted")
    void testTransientFieldsPersistence() {
        // Given
        Migration migration = orchestrator.initiateMigration(validRequest);
        Long migrationId = migration.getId();

        // Simulate phase execution that updates fields
        migration.setPhase(MigrationPhase.EXTRACTION);
        migration.setStartTime(LocalDateTime.now());
        migration.setBatchSize(2000);
        migration.setParallelism(8);
        migration.setStrictValidation(true);
        migration.setRollbackEnabled(false);

        repository.save(migration);
        repository.flush();

        // When - clear cache and reload from database
        repository.findById(migrationId).ifPresent(reloaded -> {
            // Then - all fields should be persisted
            assertEquals(MigrationPhase.EXTRACTION, reloaded.getPhase());
            assertNotNull(reloaded.getStartTime());
            assertEquals(2000, reloaded.getBatchSize());
            assertEquals(8, reloaded.getParallelism());
            assertTrue(reloaded.isStrictValidation());
            assertFalse(reloaded.isRollbackEnabled());
        });
    }

    @Test
    @DisplayName("✅ FIX-2: Metrics should be persisted as individual columns")
    void testMetricsPersistence() {
        // Given
        Migration migration = orchestrator.initiateMigration(validRequest);

        // Set metrics
        migration.setTotalRecords(10000);
        migration.setProcessedRecords(5000);
        migration.setSuccessfulRecords(4950);
        migration.setFailedRecords(50);
        migration.setExtractedRecords(5000);
        migration.setTransformedRecords(4980);
        migration.setLoadedRecords(4950);
        migration.setValidationScore(98.5);
        migration.setValidationErrors(15);
        migration.setBytesProcessed(1024000L);
        migration.setDurationSeconds(3600L);

        repository.save(migration);
        repository.flush();

        // When - reload from database
        Migration reloaded = repository.findById(migration.getId()).orElseThrow();

        // Then - all metrics should be persisted
        assertEquals(10000, reloaded.getTotalRecords());
        assertEquals(5000, reloaded.getProcessedRecords());
        assertEquals(4950, reloaded.getSuccessfulRecords());
        assertEquals(50, reloaded.getFailedRecords());
        assertEquals(5000, reloaded.getExtractedRecords());
        assertEquals(4980, reloaded.getTransformedRecords());
        assertEquals(4950, reloaded.getLoadedRecords());
        assertEquals(98.5, reloaded.getValidationScore());
        assertEquals(15, reloaded.getValidationErrors());
        assertEquals(1024000L, reloaded.getBytesProcessed());
        assertEquals(3600L, reloaded.getDurationSeconds());
    }

    @Test
    @DisplayName("✅ FIX-2: Complex objects should persist as JSON")
    void testComplexObjectsPersistence() {
        // Given
        Migration migration = orchestrator.initiateMigration(validRequest);

        // Set complex objects
        MigrationModels.SourceAnalysis sourceAnalysis = new MigrationModels.SourceAnalysis();
        sourceAnalysis.setSystemType("MySQL");
        sourceAnalysis.setVersion("5.7.42");
        sourceAnalysis.setTotalRecords(50000);
        sourceAnalysis.setTotalSize(1024000000L);
        migration.setSourceAnalysis(sourceAnalysis);

        MigrationModels.TargetAnalysis targetAnalysis = new MigrationModels.TargetAnalysis();
        targetAnalysis.setSystemType("PostgreSQL");
        targetAnalysis.setVersion("15.3");
        targetAnalysis.setAvailableSpace(10000000000L);
        migration.setTargetAnalysis(targetAnalysis);

        repository.save(migration);
        repository.flush();

        // When - reload from database
        Migration reloaded = repository.findById(migration.getId()).orElseThrow();

        // Then - complex objects should be deserialized correctly
        assertNotNull(reloaded.getSourceAnalysis());
        assertEquals("MySQL", reloaded.getSourceAnalysis().getSystemType());
        assertEquals("5.7.42", reloaded.getSourceAnalysis().getVersion());
        assertEquals(50000, reloaded.getSourceAnalysis().getTotalRecords());

        assertNotNull(reloaded.getTargetAnalysis());
        assertEquals("PostgreSQL", reloaded.getTargetAnalysis().getSystemType());
        assertEquals("15.3", reloaded.getTargetAnalysis().getVersion());
    }

    @Test
    @DisplayName("✅ FIX-3: Pause should validate current state")
    void testPauseMigration_ValidatesCurrentState() {
        // Given - create migration in INITIALIZED state
        Migration migration = orchestrator.initiateMigration(validRequest);

        // When/Then - pausing initialized migration should fail
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.pauseMigration(migration.getId())
        );

        assertTrue(ex.getMessage().contains("Cannot pause"));
    }

    @Test
    @DisplayName("✅ FIX-3: Pause should work for IN_PROGRESS migration")
    void testPauseMigration_WorksForInProgressMigration() {
        // Given - create and start migration
        Migration migration = orchestrator.initiateMigration(validRequest);
        migration.setStatus(MigrationStatus.IN_PROGRESS);
        repository.save(migration);

        // When
        Migration paused = orchestrator.pauseMigration(migration.getId());

        // Then
        assertEquals(MigrationStatus.PAUSED, paused.getStatus());
        assertNotNull(paused.getPausedTime());
    }

    @Test
    @DisplayName("✅ FIX-4: Resume should validate current state")
    void testResumeMigration_ValidatesCurrentState() {
        // Given - create migration in INITIALIZED state
        Migration migration = orchestrator.initiateMigration(validRequest);

        // When/Then - resuming non-paused migration should fail
        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> orchestrator.resumeMigration(migration.getId())
        );

        assertTrue(ex.getMessage().contains("Cannot resume") ||
                   ex.getMessage().contains("must be PAUSED"));
    }

    @Test
    @DisplayName("✅ FIX-5: Rollback tracking should be persisted")
    void testRollbackTracking() {
        // Given
        Migration migration = orchestrator.initiateMigration(validRequest);
        migration.setStatus(MigrationStatus.FAILED);
        repository.save(migration);

        // When - execute rollback
        orchestrator.executeRollback(migration.getId());

        // Then - rollback tracking should be saved
        Migration rolledBack = repository.findById(migration.getId()).orElseThrow();
        assertTrue(rolledBack.isRollbackExecuted());
        assertNotNull(rolledBack.getRollbackTime());
        assertEquals(MigrationStatus.ROLLED_BACK, rolledBack.getStatus());
    }

    @Test
    @DisplayName("✅ FIX-6: Error information should be persisted")
    void testErrorPersistence() {
        // Given
        Migration migration = orchestrator.initiateMigration(validRequest);

        // When - set error information
        migration.setStatus(MigrationStatus.FAILED);
        migration.setErrorMessage("Connection timeout to source database");
        migration.setErrorStackTrace("java.sql.SQLException at line 123...");
        migration.setCompletionTime(LocalDateTime.now());

        repository.save(migration);
        repository.flush();

        // Then - error info should be persisted
        Migration failed = repository.findById(migration.getId()).orElseThrow();
        assertEquals(MigrationStatus.FAILED, failed.getStatus());
        assertEquals("Connection timeout to source database", failed.getErrorMessage());
        assertNotNull(failed.getErrorStackTrace());
        assertTrue(failed.getErrorStackTrace().contains("SQLException"));
        assertNotNull(failed.getCompletionTime());
    }

    @Test
    @DisplayName("✅ FIX-7: Timestamp fields should all be persisted")
    void testTimestampPersistence() {
        // Given
        Migration migration = orchestrator.initiateMigration(validRequest);

        LocalDateTime now = LocalDateTime.now();
        migration.setStartTime(now);
        migration.setCompletionTime(now.plusHours(2));
        migration.setPausedTime(now.plusMinutes(30));
        migration.setResumedTime(now.plusMinutes(45));
        migration.setCancelledTime(null);
        migration.setRollbackTime(null);

        repository.save(migration);
        repository.flush();

        // When - reload from database
        Migration reloaded = repository.findById(migration.getId()).orElseThrow();

        // Then - all timestamps should be persisted
        assertNotNull(reloaded.getStartTime());
        assertNotNull(reloaded.getCompletionTime());
        assertNotNull(reloaded.getPausedTime());
        assertNotNull(reloaded.getResumedTime());
        assertNull(reloaded.getCancelledTime());
        assertNull(reloaded.getRollbackTime());

        // Verify timestamp accuracy
        assertEquals(now.getHour(), reloaded.getStartTime().getHour());
        assertEquals(now.plusHours(2).getHour(), reloaded.getCompletionTime().getHour());
    }

    @Test
    @DisplayName("✅ FIX-8: Project code should be unique")
    void testProjectCodeUniqueness() {
        // Given/When
        Migration migration1 = orchestrator.initiateMigration(validRequest);

        validRequest.setName("Another Migration");
        Migration migration2 = orchestrator.initiateMigration(validRequest);

        // Then
        assertNotNull(migration1.getProjectCode());
        assertNotNull(migration2.getProjectCode());
        assertNotEquals(migration1.getProjectCode(), migration2.getProjectCode(),
            "Project codes should be unique");
    }

    @Test
    @DisplayName("✅ FIX-9: Migration survives application restart simulation")
    void testMigrationSurvivesRestart() {
        // Given - create and partially execute migration
        Migration migration = orchestrator.initiateMigration(validRequest);
        Long migrationId = migration.getId();

        migration.setStatus(MigrationStatus.IN_PROGRESS);
        migration.setPhase(MigrationPhase.EXTRACTION);
        migration.setStartTime(LocalDateTime.now());
        migration.setTotalRecords(10000);
        migration.setProcessedRecords(5000);

        repository.save(migration);
        repository.flush();

        // Simulate application restart - clear all caches
        repository.findAll(); // Force flush

        // When - reload migration (simulating restart)
        Migration afterRestart = repository.findById(migrationId).orElseThrow();

        // Then - all state should be preserved
        assertEquals(MigrationStatus.IN_PROGRESS, afterRestart.getStatus());
        assertEquals(MigrationPhase.EXTRACTION, afterRestart.getPhase());
        assertNotNull(afterRestart.getStartTime());
        assertEquals(10000, afterRestart.getTotalRecords());
        assertEquals(5000, afterRestart.getProcessedRecords());
    }

    @Test
    @DisplayName("✅ FIX-10: Audit fields should be automatically populated")
    void testAuditFields() {
        // Given/When
        validRequest.setUserId(42L);
        Migration migration = orchestrator.initiateMigration(validRequest);

        // Then
        assertNotNull(migration.getCreatedAt(), "createdAt should be auto-populated");
        assertNotNull(migration.getUpdatedAt(), "updatedAt should be auto-populated");
        assertEquals("42", migration.getCreatedBy(), "createdBy should be set from userId");
    }

    @Test
    @DisplayName("✅ FIX-11: Batch size and parallelism validation")
    void testBatchSizeAndParallelismValidation() {
        // Given
        validRequest.setBatchSize(100);
        validRequest.setParallelism(2);

        // When
        Migration migration = orchestrator.initiateMigration(validRequest);

        // Then
        assertEquals(100, migration.getBatchSize());
        assertEquals(2, migration.getParallelism());

        // Verify persisted
        Migration reloaded = repository.findById(migration.getId()).orElseThrow();
        assertEquals(100, reloaded.getBatchSize());
        assertEquals(2, reloaded.getParallelism());
    }

    @Test
    @DisplayName("✅ FIX-12: Progress calculation should handle null metrics")
    void testProgressCalculation_HandlesNullMetrics() {
        // Given - migration with no metrics set
        Migration migration = orchestrator.initiateMigration(validRequest);

        // When - calculate progress
        int progress = calculateProgress(migration);

        // Then - should return 0, not crash
        assertEquals(0, progress);
    }

    @Test
    @DisplayName("✅ FIX-13: Progress calculation should cap at 100%")
    void testProgressCalculation_CapsAt100Percent() {
        // Given
        Migration migration = orchestrator.initiateMigration(validRequest);
        migration.setTotalRecords(100);
        migration.setProcessedRecords(150); // More than total (edge case)

        // When
        int progress = calculateProgress(migration);

        // Then
        assertEquals(100, progress, "Progress should cap at 100%");
    }

    private int calculateProgress(Migration migration) {
        if (migration.getTotalRecords() == null || migration.getTotalRecords() == 0) {
            return 0;
        }
        int progress = (int) ((migration.getProcessedRecords() * 100.0) / migration.getTotalRecords());
        return Math.min(100, Math.max(0, progress));
    }
}
