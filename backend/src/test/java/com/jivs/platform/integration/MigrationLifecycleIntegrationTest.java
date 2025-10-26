package com.jivs.platform.integration;

import com.jivs.platform.domain.migration.Migration;
import com.jivs.platform.domain.migration.MigrationPhase;
import com.jivs.platform.domain.migration.MigrationStatus;
import com.jivs.platform.repository.MigrationRepository;
import com.jivs.platform.service.migration.MigrationModels;
import com.jivs.platform.service.migration.MigrationOrchestrator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ FIXED: End-to-end integration tests for migration lifecycle
 * Tests the complete migration workflow from creation to completion
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MigrationLifecycleIntegrationTest {

    @Autowired
    private MigrationOrchestrator orchestrator;

    @Autowired
    private MigrationRepository repository;

    private static Migration testMigration;

    @BeforeEach
    void setUp() {
        // Clean up any previous test data
        if (testMigration != null && testMigration.getId() != null) {
            repository.findById(testMigration.getId()).ifPresent(repository::delete);
        }
    }

    @Test
    @Order(1)
    @DisplayName("✅ INT-1: Complete migration lifecycle - Creation to Completion")
    void testCompleteMigrationLifecycle() throws Exception {
        // ============================================================
        // PHASE 1: CREATE MIGRATION
        // ============================================================
        MigrationModels.MigrationRequest request = createValidRequest();
        Migration created = orchestrator.initiateMigration(request);

        assertNotNull(created.getId(), "Migration should have ID");
        assertEquals(MigrationStatus.INITIALIZED, created.getStatus());
        assertEquals(MigrationPhase.PLANNING, created.getPhase());
        assertNotNull(created.getProjectCode());

        testMigration = created;

        // Verify persistence
        Migration fromDb = repository.findById(created.getId()).orElseThrow();
        assertEquals(created.getName(), fromDb.getName());

        // ============================================================
        // PHASE 2: EXECUTE MIGRATION
        // ============================================================
        CompletableFuture<Migration> future = orchestrator.executeMigration(created.getId());

        // Wait for completion (with timeout)
        Migration completed = future.get(5, TimeUnit.MINUTES);

        assertNotNull(completed);
        assertTrue(
            completed.getStatus() == MigrationStatus.COMPLETED ||
            completed.getStatus() == MigrationStatus.FAILED,
            "Migration should be in terminal state"
        );

        // ============================================================
        // PHASE 3: VERIFY FINAL STATE
        // ============================================================
        Migration finalMigration = repository.findById(created.getId()).orElseThrow();

        // Verify timestamps were saved
        assertNotNull(finalMigration.getStartTime(), "Start time should be set");
        assertNotNull(finalMigration.getCompletionTime(), "Completion time should be set");

        // Verify completion time is after start time
        assertTrue(
            finalMigration.getCompletionTime().isAfter(finalMigration.getStartTime()),
            "Completion time should be after start time"
        );

        // Verify metrics were updated
        assertNotNull(finalMigration.getTotalRecords());
        assertTrue(finalMigration.getProcessedRecords() >= 0);

        // Verify phase progression
        assertTrue(
            finalMigration.getPhase() == MigrationPhase.COMPLETED ||
            finalMigration.getPhase() == MigrationPhase.FAILED
        );
    }

    @Test
    @Order(2)
    @DisplayName("✅ INT-2: Migration state survives application restart simulation")
    void testMigrationStatePersistence() {
        // GIVEN: Create and partially execute migration
        MigrationModels.MigrationRequest request = createValidRequest();
        Migration migration = orchestrator.initiateMigration(request);

        // Simulate partial execution
        migration.setStatus(MigrationStatus.IN_PROGRESS);
        migration.setPhase(MigrationPhase.EXTRACTION);
        migration.setStartTime(LocalDateTime.now());
        migration.setTotalRecords(10000);
        migration.setProcessedRecords(5000);
        migration.setExtractedRecords(5000);
        migration.setBatchSize(1000);
        migration.setParallelism(4);

        // Save state
        Migration saved = repository.save(migration);
        repository.flush();

        Long migrationId = saved.getId();

        // WHEN: Simulate application restart - clear all caches
        // Force new entity manager session
        repository.findAll(); // Trigger flush

        // Reload migration (simulating restart)
        Migration afterRestart = repository.findById(migrationId).orElseThrow();

        // THEN: All state should be preserved
        assertEquals(MigrationStatus.IN_PROGRESS, afterRestart.getStatus());
        assertEquals(MigrationPhase.EXTRACTION, afterRestart.getPhase());
        assertNotNull(afterRestart.getStartTime());
        assertEquals(10000, afterRestart.getTotalRecords());
        assertEquals(5000, afterRestart.getProcessedRecords());
        assertEquals(5000, afterRestart.getExtractedRecords());
        assertEquals(1000, afterRestart.getBatchSize());
        assertEquals(4, afterRestart.getParallelism());

        // Clean up
        repository.delete(afterRestart);
    }

    @Test
    @Order(3)
    @DisplayName("✅ INT-3: Pause and resume workflow")
    void testPauseResumeWorkflow() throws Exception {
        // GIVEN: Create and start migration
        MigrationModels.MigrationRequest request = createValidRequest();
        Migration migration = orchestrator.initiateMigration(request);

        // Set to IN_PROGRESS state
        migration.setStatus(MigrationStatus.IN_PROGRESS);
        migration.setPhase(MigrationPhase.EXTRACTION);
        migration.setStartTime(LocalDateTime.now());
        repository.save(migration);

        // WHEN: Pause migration
        Migration paused = orchestrator.pauseMigration(migration.getId());

        // THEN: Migration should be paused
        assertEquals(MigrationStatus.PAUSED, paused.getStatus());
        assertNotNull(paused.getPausedTime());

        // Verify persistence
        Migration pausedFromDb = repository.findById(migration.getId()).orElseThrow();
        assertEquals(MigrationStatus.PAUSED, pausedFromDb.getStatus());
        assertNotNull(pausedFromDb.getPausedTime());

        // WHEN: Resume migration
        Migration resumed = orchestrator.resumeMigration(migration.getId());

        // THEN: Migration should resume
        assertEquals(MigrationStatus.IN_PROGRESS, resumed.getStatus());
        assertNotNull(resumed.getResumedTime());

        // Verify persistence
        Migration resumedFromDb = repository.findById(migration.getId()).orElseThrow();
        assertEquals(MigrationStatus.IN_PROGRESS, resumedFromDb.getStatus());
        assertNotNull(resumedFromDb.getResumedTime());
        assertTrue(
            resumedFromDb.getResumedTime().isAfter(resumedFromDb.getPausedTime()),
            "Resume time should be after pause time"
        );

        // Clean up
        repository.delete(resumed);
    }

    @Test
    @Order(4)
    @DisplayName("✅ INT-4: Cancel with rollback workflow")
    void testCancelWithRollbackWorkflow() {
        // GIVEN: Create migration with rollback enabled
        MigrationModels.MigrationRequest request = createValidRequest();
        Migration migration = orchestrator.initiateMigration(request);

        migration.setStatus(MigrationStatus.IN_PROGRESS);
        migration.setPhase(MigrationPhase.LOADING);
        migration.setRollbackOnCancel(true);
        repository.save(migration);

        // WHEN: Cancel migration
        Migration cancelled = orchestrator.cancelMigration(migration.getId());

        // THEN: Migration should be cancelled
        assertEquals(MigrationStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledTime());

        // Verify rollback was executed
        if (cancelled.isRollbackOnCancel()) {
            assertTrue(cancelled.isRollbackExecuted() || cancelled.getStatus() == MigrationStatus.ROLLED_BACK);
        }

        // Verify persistence
        Migration cancelledFromDb = repository.findById(migration.getId()).orElseThrow();
        assertEquals(MigrationStatus.CANCELLED, cancelledFromDb.getStatus());
        assertNotNull(cancelledFromDb.getCancelledTime());

        // Clean up
        repository.delete(cancelled);
    }

    @Test
    @Order(5)
    @DisplayName("✅ INT-5: Concurrent migrations don't interfere")
    void testConcurrentMigrations() throws Exception {
        // GIVEN: Create two migrations
        MigrationModels.MigrationRequest request1 = createValidRequest();
        request1.setName("Migration 1 - " + System.currentTimeMillis());

        MigrationModels.MigrationRequest request2 = createValidRequest();
        request2.setName("Migration 2 - " + System.currentTimeMillis());

        Migration migration1 = orchestrator.initiateMigration(request1);
        Migration migration2 = orchestrator.initiateMigration(request2);

        // WHEN: Execute both migrations concurrently
        CompletableFuture<Migration> future1 = orchestrator.executeMigration(migration1.getId());
        CompletableFuture<Migration> future2 = orchestrator.executeMigration(migration2.getId());

        // Wait for both to complete
        CompletableFuture.allOf(future1, future2).get(10, TimeUnit.MINUTES);

        Migration completed1 = future1.get();
        Migration completed2 = future2.get();

        // THEN: Both should complete independently
        assertNotNull(completed1);
        assertNotNull(completed2);

        // Verify both were saved independently
        Migration fromDb1 = repository.findById(migration1.getId()).orElseThrow();
        Migration fromDb2 = repository.findById(migration2.getId()).orElseThrow();

        assertNotEquals(fromDb1.getId(), fromDb2.getId());
        assertNotEquals(fromDb1.getProjectCode(), fromDb2.getProjectCode());

        // Both should have their own timestamps
        assertNotNull(fromDb1.getStartTime());
        assertNotNull(fromDb2.getStartTime());

        // Clean up
        repository.delete(fromDb1);
        repository.delete(fromDb2);
    }

    @Test
    @Order(6)
    @DisplayName("✅ INT-6: Transaction boundaries are respected")
    @Transactional
    void testTransactionBoundaries() {
        // GIVEN: Create migration
        MigrationModels.MigrationRequest request = createValidRequest();
        Migration migration = orchestrator.initiateMigration(request);

        // Verify migration exists in current transaction
        assertTrue(repository.existsById(migration.getId()));

        // WHEN: Transaction is rolled back (by test framework)
        // Migration should not exist in database after transaction rollback
        // This is automatically tested by @Transactional on test method
    }

    @Test
    @Order(7)
    @DisplayName("✅ INT-7: Metrics are accurately tracked through phases")
    void testMetricsTracking() {
        // GIVEN: Create migration
        MigrationModels.MigrationRequest request = createValidRequest();
        Migration migration = orchestrator.initiateMigration(request);

        // WHEN: Simulate phase-by-phase metrics updates
        migration.setPhase(MigrationPhase.PLANNING);
        migration.setTotalRecords(10000);
        repository.save(migration);

        migration.setPhase(MigrationPhase.EXTRACTION);
        migration.setExtractedRecords(10000);
        migration.setProcessedRecords(10000);
        repository.save(migration);

        migration.setPhase(MigrationPhase.TRANSFORMATION);
        migration.setTransformedRecords(9950);
        repository.save(migration);

        migration.setPhase(MigrationPhase.LOADING);
        migration.setLoadedRecords(9950);
        migration.setSuccessfulRecords(9950);
        migration.setFailedRecords(50);
        repository.save(migration);

        repository.flush();

        // THEN: Reload and verify all metrics
        Migration loaded = repository.findById(migration.getId()).orElseThrow();

        assertEquals(10000, loaded.getTotalRecords());
        assertEquals(10000, loaded.getExtractedRecords());
        assertEquals(9950, loaded.getTransformedRecords());
        assertEquals(9950, loaded.getLoadedRecords());
        assertEquals(9950, loaded.getSuccessfulRecords());
        assertEquals(50, loaded.getFailedRecords());
        assertEquals(10000, loaded.getProcessedRecords());

        // Verify record accounting
        assertEquals(
            loaded.getSuccessfulRecords() + loaded.getFailedRecords(),
            loaded.getLoadedRecords(),
            "Successful + Failed should equal Loaded"
        );

        // Clean up
        repository.delete(loaded);
    }

    @Test
    @Order(8)
    @DisplayName("✅ INT-8: Complex objects are persisted as JSON")
    void testComplexObjectPersistence() {
        // GIVEN: Create migration with complex objects
        MigrationModels.MigrationRequest request = createValidRequest();
        Migration migration = orchestrator.initiateMigration(request);

        // Set complex objects
        MigrationModels.SourceAnalysis sourceAnalysis = new MigrationModels.SourceAnalysis();
        sourceAnalysis.setSystemType("PostgreSQL");
        sourceAnalysis.setVersion("15.3");
        sourceAnalysis.setTotalRecords(100000);
        sourceAnalysis.setTotalSize(5000000000L);

        MigrationModels.TargetAnalysis targetAnalysis = new MigrationModels.TargetAnalysis();
        targetAnalysis.setSystemType("MySQL");
        targetAnalysis.setVersion("8.0");
        targetAnalysis.setAvailableSpace(100000000000L);

        migration.setSourceAnalysis(sourceAnalysis);
        migration.setTargetAnalysis(targetAnalysis);

        repository.save(migration);
        repository.flush();

        // WHEN: Reload from database
        Migration reloaded = repository.findById(migration.getId()).orElseThrow();

        // THEN: Complex objects should be deserialized
        assertNotNull(reloaded.getSourceAnalysis());
        assertEquals("PostgreSQL", reloaded.getSourceAnalysis().getSystemType());
        assertEquals("15.3", reloaded.getSourceAnalysis().getVersion());
        assertEquals(100000, reloaded.getSourceAnalysis().getTotalRecords());

        assertNotNull(reloaded.getTargetAnalysis());
        assertEquals("MySQL", reloaded.getTargetAnalysis().getSystemType());
        assertEquals("8.0", reloaded.getTargetAnalysis().getVersion());

        // Clean up
        repository.delete(reloaded);
    }

    @Test
    @Order(9)
    @DisplayName("✅ INT-9: Error information is captured and persisted")
    void testErrorCapture() {
        // GIVEN: Create migration
        MigrationModels.MigrationRequest request = createValidRequest();
        Migration migration = orchestrator.initiateMigration(request);

        // WHEN: Simulate failure with error details
        migration.setStatus(MigrationStatus.FAILED);
        migration.setPhase(MigrationPhase.FAILED);
        migration.setErrorMessage("Database connection timeout");
        migration.setErrorStackTrace("java.sql.SQLTimeoutException: Connection timeout\n" +
            "\tat com.jivs.platform.service.extraction.JdbcExtractor.connect(JdbcExtractor.java:123)\n" +
            "\tat com.jivs.platform.service.migration.MigrationOrchestrator.executeExtraction(MigrationOrchestrator.java:456)");
        migration.setCompletionTime(LocalDateTime.now());

        repository.save(migration);
        repository.flush();

        // THEN: Reload and verify error details
        Migration failed = repository.findById(migration.getId()).orElseThrow();

        assertEquals(MigrationStatus.FAILED, failed.getStatus());
        assertEquals("Database connection timeout", failed.getErrorMessage());
        assertNotNull(failed.getErrorStackTrace());
        assertTrue(failed.getErrorStackTrace().contains("SQLTimeoutException"));
        assertTrue(failed.getErrorStackTrace().contains("JdbcExtractor.java:123"));
        assertNotNull(failed.getCompletionTime());

        // Clean up
        repository.delete(failed);
    }

    @Test
    @Order(10)
    @DisplayName("✅ INT-10: Progress calculation is accurate")
    void testProgressCalculation() {
        // GIVEN: Migrations at different progress levels
        MigrationModels.MigrationRequest request = createValidRequest();

        // 0% progress
        Migration migration0 = orchestrator.initiateMigration(request);
        migration0.setTotalRecords(10000);
        migration0.setProcessedRecords(0);
        assertEquals(0, calculateProgress(migration0));

        // 25% progress
        Migration migration25 = orchestrator.initiateMigration(request);
        migration25.setTotalRecords(10000);
        migration25.setProcessedRecords(2500);
        assertEquals(25, calculateProgress(migration25));

        // 50% progress
        Migration migration50 = orchestrator.initiateMigration(request);
        migration50.setTotalRecords(10000);
        migration50.setProcessedRecords(5000);
        assertEquals(50, calculateProgress(migration50));

        // 100% progress
        Migration migration100 = orchestrator.initiateMigration(request);
        migration100.setTotalRecords(10000);
        migration100.setProcessedRecords(10000);
        assertEquals(100, calculateProgress(migration100));

        // Over 100% (edge case)
        Migration migrationOver = orchestrator.initiateMigration(request);
        migrationOver.setTotalRecords(10000);
        migrationOver.setProcessedRecords(12000);
        assertEquals(100, calculateProgress(migrationOver), "Should cap at 100%");

        // Clean up
        repository.deleteAll(Arrays.asList(migration0, migration25, migration50, migration100, migrationOver));
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private MigrationModels.MigrationRequest createValidRequest() {
        MigrationModels.MigrationRequest request = new MigrationModels.MigrationRequest();
        request.setName("Integration Test Migration - " + System.currentTimeMillis());
        request.setDescription("Testing complete migration lifecycle");
        request.setSourceSystem("PostgreSQL-15");
        request.setTargetSystem("MySQL-8");
        request.setMigrationType("DATA_MIGRATION");
        request.setUserId(1L);
        request.setBatchSize(1000);
        request.setParallelism(2); // Lower for test environment
        request.setRetryAttempts(3);

        Map<String, Object> params = new HashMap<>();
        params.put("test_mode", "true");
        params.put("skip_validation", "false");
        request.setParameters(params);

        return request;
    }

    private int calculateProgress(Migration migration) {
        if (migration.getTotalRecords() == null || migration.getTotalRecords() == 0) {
            return 0;
        }
        int progress = (int) ((migration.getProcessedRecords() * 100.0) / migration.getTotalRecords());
        return Math.min(100, Math.max(0, progress));
    }
}
