package com.jivs.platform.service.monitoring;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prometheus metrics for extraction module monitoring.
 *
 * Tracks critical operations identified in extraction module security audit:
 * - SQL injection attempts (P0 security)
 * - Password encryption failures (P0 security)
 * - Batch processing failures (P0 data loss)
 * - Job lifecycle and performance
 * - Resource utilization
 *
 * Integrates with Prometheus and Grafana for production monitoring.
 *
 * @see <a href="../../EXTRACTION_MODULE_MONITORING.md">Monitoring Guide</a>
 * @author JiVS Extraction Expert
 * @since 2025-10-26
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExtractionMetrics {

    private final MeterRegistry meterRegistry;

    // ========================================================================
    // SECURITY METRICS (P0 - Critical)
    // ========================================================================

    private Counter sqlInjectionAttempts;
    private Counter passwordEncryptionFailures;
    private Counter passwordDecryptionFailures;

    // ========================================================================
    // JOB LIFECYCLE METRICS
    // ========================================================================

    private Counter extractionJobsStarted;
    private Counter extractionJobsCompleted;
    private Counter extractionJobsFailed;

    // ========================================================================
    // DATA PROCESSING METRICS
    // ========================================================================

    private Counter recordsExtracted;
    private Counter recordsFailed;
    private Counter batchWriteFailures;

    // ========================================================================
    // RESOURCE UTILIZATION GAUGES
    // ========================================================================

    private AtomicInteger activeExtractionJobs;
    private AtomicInteger connectionPoolActive;
    private AtomicInteger connectionPoolIdle;

    // ========================================================================
    // PERFORMANCE TIMERS
    // ========================================================================

    private Timer extractionJobDuration;
    private Timer batchWriteDuration;
    private Timer sqlValidationDuration;

    // ========================================================================
    // DISTRIBUTION SUMMARIES
    // ========================================================================

    private DistributionSummary batchSizeDistribution;

    /**
     * Initialize all Prometheus metrics on bean creation.
     * Called automatically by Spring after dependency injection.
     */
    @jakarta.annotation.PostConstruct
    public void initMetrics() {
        log.info("Initializing Extraction Module Prometheus metrics...");

        initSecurityMetrics();
        initJobLifecycleMetrics();
        initDataProcessingMetrics();
        initResourceGauges();
        initPerformanceTimers();
        initDistributionSummaries();

        log.info("Extraction module Prometheus metrics initialized successfully");
    }

    // ========================================================================
    // INITIALIZATION METHODS
    // ========================================================================

    private void initSecurityMetrics() {
        sqlInjectionAttempts = Counter.builder("jivs.extraction.security.sql_injection_attempts")
                .description("Number of SQL injection attempts detected by SqlInjectionValidator")
                .tag("module", "extraction")
                .tag("severity", "critical")
                .register(meterRegistry);

        passwordEncryptionFailures = Counter.builder("jivs.extraction.security.password_encryption_failures")
                .description("Number of password encryption failures (CryptoUtil.encrypt)")
                .tag("module", "extraction")
                .tag("severity", "critical")
                .register(meterRegistry);

        passwordDecryptionFailures = Counter.builder("jivs.extraction.security.password_decryption_failures")
                .description("Number of password decryption failures (CryptoUtil.decrypt)")
                .tag("module", "extraction")
                .tag("severity", "warning")
                .register(meterRegistry);
    }

    private void initJobLifecycleMetrics() {
        extractionJobsStarted = Counter.builder("jivs.extraction.jobs.started")
                .description("Total number of extraction jobs started")
                .tag("module", "extraction")
                .register(meterRegistry);

        extractionJobsCompleted = Counter.builder("jivs.extraction.jobs.completed")
                .description("Total number of extraction jobs completed successfully")
                .tag("module", "extraction")
                .register(meterRegistry);

        extractionJobsFailed = Counter.builder("jivs.extraction.jobs.failed")
                .description("Total number of extraction jobs failed")
                .tag("module", "extraction")
                .register(meterRegistry);
    }

    private void initDataProcessingMetrics() {
        recordsExtracted = Counter.builder("jivs.extraction.records.extracted")
                .description("Total number of records successfully extracted")
                .tag("module", "extraction")
                .register(meterRegistry);

        recordsFailed = Counter.builder("jivs.extraction.records.failed")
                .description("Total number of records that failed extraction")
                .tag("module", "extraction")
                .register(meterRegistry);

        batchWriteFailures = Counter.builder("jivs.extraction.batch.write_failures")
                .description("Number of batch write failures (DATA LOSS RISK)")
                .tag("module", "extraction")
                .tag("severity", "critical")
                .register(meterRegistry);
    }

    private void initResourceGauges() {
        activeExtractionJobs = meterRegistry.gauge(
                "jivs.extraction.jobs.active",
                Tags.of("module", "extraction"),
                new AtomicInteger(0)
        );

        connectionPoolActive = meterRegistry.gauge(
                "jivs.extraction.connection_pool.active",
                Tags.of("module", "extraction", "type", "hikari"),
                new AtomicInteger(0)
        );

        connectionPoolIdle = meterRegistry.gauge(
                "jivs.extraction.connection_pool.idle",
                Tags.of("module", "extraction", "type", "hikari"),
                new AtomicInteger(0)
        );
    }

    private void initPerformanceTimers() {
        extractionJobDuration = Timer.builder("jivs.extraction.jobs.duration")
                .description("Duration of extraction jobs (start to completion)")
                .tag("module", "extraction")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        batchWriteDuration = Timer.builder("jivs.extraction.batch.write_duration")
                .description("Duration of batch write operations")
                .tag("module", "extraction")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        sqlValidationDuration = Timer.builder("jivs.extraction.security.sql_validation_duration")
                .description("Duration of SQL injection validation checks")
                .tag("module", "extraction")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    private void initDistributionSummaries() {
        batchSizeDistribution = DistributionSummary.builder("jivs.extraction.batch.size")
                .description("Distribution of batch sizes processed")
                .tag("module", "extraction")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    // ========================================================================
    // PUBLIC API - SECURITY EVENTS
    // ========================================================================

    /**
     * Record a SQL injection attempt detected by SqlInjectionValidator.
     * Triggers critical alert in production.
     */
    public void recordSqlInjectionAttempt() {
        sqlInjectionAttempts.increment();
        log.warn("SECURITY: SQL injection attempt recorded in metrics (triggers alert)");
    }

    /**
     * Record a password encryption failure.
     * Indicates issues with CryptoUtil or encryption key.
     */
    public void recordPasswordEncryptionFailure() {
        passwordEncryptionFailures.increment();
        log.error("SECURITY: Password encryption failure recorded in metrics");
    }

    /**
     * Record a password decryption failure.
     * May indicate encryption key rotation or corruption.
     */
    public void recordPasswordDecryptionFailure() {
        passwordDecryptionFailures.increment();
        log.warn("SECURITY: Password decryption failure recorded in metrics");
    }

    // ========================================================================
    // PUBLIC API - JOB LIFECYCLE
    // ========================================================================

    /**
     * Record extraction job start.
     * Increments active job counter.
     */
    public void recordJobStarted() {
        extractionJobsStarted.increment();
        activeExtractionJobs.incrementAndGet();
        log.debug("Job started - active jobs: {}", activeExtractionJobs.get());
    }

    /**
     * Record extraction job successful completion.
     *
     * @param durationMillis total job duration in milliseconds
     */
    public void recordJobCompleted(long durationMillis) {
        extractionJobsCompleted.increment();
        activeExtractionJobs.decrementAndGet();
        extractionJobDuration.record(durationMillis, TimeUnit.MILLISECONDS);
        log.debug("Job completed in {}ms - active jobs: {}", durationMillis, activeExtractionJobs.get());
    }

    /**
     * Record extraction job failure.
     *
     * @param durationMillis job duration before failure
     */
    public void recordJobFailed(long durationMillis) {
        extractionJobsFailed.increment();
        activeExtractionJobs.decrementAndGet();
        extractionJobDuration.record(durationMillis, TimeUnit.MILLISECONDS);
        log.warn("Job failed after {}ms - active jobs: {}", durationMillis, activeExtractionJobs.get());
    }

    // ========================================================================
    // PUBLIC API - DATA PROCESSING
    // ========================================================================

    /**
     * Record successfully extracted records.
     *
     * @param count number of records extracted
     */
    public void recordRecordsExtracted(long count) {
        recordsExtracted.increment(count);
    }

    /**
     * Record failed record extractions.
     *
     * @param count number of records that failed
     */
    public void recordRecordsFailed(long count) {
        recordsFailed.increment(count);
    }

    /**
     * Record a batch write failure.
     * CRITICAL: Indicates potential data loss.
     */
    public void recordBatchWriteFailure() {
        batchWriteFailures.increment();
        log.error("CRITICAL: Batch write failure recorded (DATA LOSS RISK)");
    }

    /**
     * Record batch write performance.
     *
     * @param durationMillis write duration in milliseconds
     * @param batchSize number of records in batch
     */
    public void recordBatchWriteDuration(long durationMillis, int batchSize) {
        batchWriteDuration.record(durationMillis, TimeUnit.MILLISECONDS);
        batchSizeDistribution.record(batchSize);
    }

    // ========================================================================
    // PUBLIC API - RESOURCE MONITORING
    // ========================================================================

    /**
     * Update connection pool statistics.
     * Should be called periodically by ExtractionDataSourcePool.
     *
     * @param active number of active connections
     * @param idle number of idle connections
     */
    public void updateConnectionPoolStats(int active, int idle) {
        connectionPoolActive.set(active);
        connectionPoolIdle.set(idle);

        if (idle == 0) {
            log.warn("WARNING: Connection pool exhausted (0 idle connections)");
        }
    }

    // ========================================================================
    // PUBLIC API - PERFORMANCE TIMING
    // ========================================================================

    /**
     * Start a timer for measuring operation duration.
     * Use with stopTimer() or recordSqlValidation().
     *
     * @return Timer.Sample to be stopped later
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Record SQL validation duration.
     *
     * @param sample timer started with startTimer()
     */
    public void recordSqlValidation(Timer.Sample sample) {
        sample.stop(sqlValidationDuration);
    }

    // ========================================================================
    // PUBLIC API - GETTERS (for testing and debugging)
    // ========================================================================

    /**
     * Get current number of active extraction jobs.
     *
     * @return active job count
     */
    public int getActiveJobCount() {
        return activeExtractionJobs.get();
    }

    /**
     * Get total SQL injection attempts detected.
     *
     * @return injection attempt count
     */
    public double getSqlInjectionAttempts() {
        return sqlInjectionAttempts.count();
    }

    /**
     * Get total password encryption failures.
     *
     * @return encryption failure count
     */
    public double getPasswordEncryptionFailures() {
        return passwordEncryptionFailures.count();
    }

    /**
     * Get total extraction jobs started.
     *
     * @return jobs started count
     */
    public double getJobsStarted() {
        return extractionJobsStarted.count();
    }

    /**
     * Get total extraction jobs completed.
     *
     * @return jobs completed count
     */
    public double getJobsCompleted() {
        return extractionJobsCompleted.count();
    }

    /**
     * Get total extraction jobs failed.
     *
     * @return jobs failed count
     */
    public double getJobsFailed() {
        return extractionJobsFailed.count();
    }

    /**
     * Get job success rate (completed / total).
     *
     * @return success rate between 0.0 and 1.0, or 0.0 if no jobs
     */
    public double getJobSuccessRate() {
        double total = getJobsCompleted() + getJobsFailed();
        return total > 0 ? getJobsCompleted() / total : 0.0;
    }
}
