package com.jivs.platform.monitoring;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Metrics for migration workflow monitoring
 * Integrates with Micrometer for Prometheus export
 *
 * CRITICAL FIX: Provides full observability into migration workflows
 * - Real-time active migration count
 * - Success/failure rates
 * - Duration percentiles (p50, p95, p99)
 * - Phase-level metrics
 */
@Component
@Slf4j
public class MigrationMetrics {

    private final Counter migrationsStarted;
    private final Counter migrationsCompleted;
    private final Counter migrationsFailed;
    private final Counter migrationsPaused;
    private final Counter migrationsResumed;
    private final Counter migrationsCancelled;

    private final Timer migrationDuration;
    private final Timer extractionDuration;
    private final Timer transformationDuration;
    private final Timer loadingDuration;
    private final Timer validationDuration;

    private final DistributionSummary recordsProcessed;
    private final DistributionSummary recordsFailed;

    private final AtomicInteger activeMigrations;
    private final Gauge activeMigrationsGauge;

    private final Counter retryAttempts;
    private final Counter circuitBreakerOpened;

    public MigrationMetrics(MeterRegistry meterRegistry) {
        // Counters
        this.migrationsStarted = Counter.builder("migrations.started")
            .description("Total number of migrations started")
            .tag("component", "migration")
            .register(meterRegistry);

        this.migrationsCompleted = Counter.builder("migrations.completed")
            .description("Total number of migrations completed successfully")
            .tag("component", "migration")
            .register(meterRegistry);

        this.migrationsFailed = Counter.builder("migrations.failed")
            .description("Total number of migrations that failed")
            .tag("component", "migration")
            .register(meterRegistry);

        this.migrationsPaused = Counter.builder("migrations.paused")
            .description("Total number of migrations paused")
            .tag("component", "migration")
            .register(meterRegistry);

        this.migrationsResumed = Counter.builder("migrations.resumed")
            .description("Total number of migrations resumed")
            .tag("component", "migration")
            .register(meterRegistry);

        this.migrationsCancelled = Counter.builder("migrations.cancelled")
            .description("Total number of migrations cancelled")
            .tag("component", "migration")
            .register(meterRegistry);

        // Timers
        this.migrationDuration = Timer.builder("migration.duration")
            .description("Duration of complete migration workflow")
            .tag("component", "migration")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(meterRegistry);

        this.extractionDuration = Timer.builder("migration.phase.extraction.duration")
            .description("Duration of extraction phase")
            .tag("phase", "extraction")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);

        this.transformationDuration = Timer.builder("migration.phase.transformation.duration")
            .description("Duration of transformation phase")
            .tag("phase", "transformation")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);

        this.loadingDuration = Timer.builder("migration.phase.loading.duration")
            .description("Duration of loading phase")
            .tag("phase", "loading")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);

        this.validationDuration = Timer.builder("migration.phase.validation.duration")
            .description("Duration of validation phase")
            .tag("phase", "validation")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);

        // Distribution summaries
        this.recordsProcessed = DistributionSummary.builder("migration.records.processed")
            .description("Number of records processed per migration")
            .baseUnit("records")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);

        this.recordsFailed = DistributionSummary.builder("migration.records.failed")
            .description("Number of records that failed processing")
            .baseUnit("records")
            .register(meterRegistry);

        // Gauge for active migrations
        this.activeMigrations = new AtomicInteger(0);
        this.activeMigrationsGauge = Gauge.builder("migrations.active",
                activeMigrations, AtomicInteger::get)
            .description("Number of currently active migrations")
            .register(meterRegistry);

        // Resilience metrics
        this.retryAttempts = Counter.builder("migration.retry.attempts")
            .description("Total number of retry attempts")
            .register(meterRegistry);

        this.circuitBreakerOpened = Counter.builder("migration.circuitbreaker.opened")
            .description("Number of times circuit breaker opened")
            .register(meterRegistry);
    }

    // =====================================
    // Counter Methods
    // =====================================

    public void recordMigrationStarted() {
        migrationsStarted.increment();
        activeMigrations.incrementAndGet();
        log.debug("Migration started. Active count: {}", activeMigrations.get());
    }

    public void recordMigrationCompleted() {
        migrationsCompleted.increment();
        activeMigrations.decrementAndGet();
        log.debug("Migration completed. Active count: {}", activeMigrations.get());
    }

    public void recordMigrationFailed() {
        migrationsFailed.increment();
        activeMigrations.decrementAndGet();
        log.debug("Migration failed. Active count: {}", activeMigrations.get());
    }

    public void recordMigrationPaused() {
        migrationsPaused.increment();
        log.debug("Migration paused");
    }

    public void recordMigrationResumed() {
        migrationsResumed.increment();
        log.debug("Migration resumed");
    }

    public void recordMigrationCancelled() {
        migrationsCancelled.increment();
        activeMigrations.decrementAndGet();
        log.debug("Migration cancelled. Active count: {}", activeMigrations.get());
    }

    public void recordRetryAttempt() {
        retryAttempts.increment();
    }

    public void recordCircuitBreakerOpened() {
        circuitBreakerOpened.increment();
    }

    // =====================================
    // Timer Methods
    // =====================================

    public Timer.Sample startMigrationTimer() {
        return Timer.start();
    }

    public void recordMigrationDuration(Timer.Sample sample) {
        sample.stop(migrationDuration);
    }

    public void recordExtractionDuration(long durationMillis) {
        extractionDuration.record(java.time.Duration.ofMillis(durationMillis));
    }

    public void recordTransformationDuration(long durationMillis) {
        transformationDuration.record(java.time.Duration.ofMillis(durationMillis));
    }

    public void recordLoadingDuration(long durationMillis) {
        loadingDuration.record(java.time.Duration.ofMillis(durationMillis));
    }

    public void recordValidationDuration(long durationMillis) {
        validationDuration.record(java.time.Duration.ofMillis(durationMillis));
    }

    // =====================================
    // Distribution Summary Methods
    // =====================================

    public void recordRecordsProcessed(long count) {
        recordsProcessed.record(count);
    }

    public void recordRecordsFailed(long count) {
        recordsFailed.record(count);
    }

    // =====================================
    // Gauge Methods
    // =====================================

    public int getActiveMigrations() {
        return activeMigrations.get();
    }

    /**
     * Get failure rate (percentage)
     */
    public double getFailureRate() {
        double total = migrationsStarted.count();
        if (total == 0) {
            return 0.0;
        }
        return (migrationsFailed.count() / total) * 100;
    }

    /**
     * Get success rate (percentage)
     */
    public double getSuccessRate() {
        double total = migrationsStarted.count();
        if (total == 0) {
            return 0.0;
        }
        return (migrationsCompleted.count() / total) * 100;
    }
}
