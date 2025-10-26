package com.jivs.platform.service.extraction;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe result of data extraction
 *
 * FIXED: Uses AtomicLong and CopyOnWriteArrayList for concurrent access
 * Used in multi-threaded extraction with parallel batch processing (4 threads)
 *
 * Changes from previous version:
 * - recordsExtracted: Long -> AtomicLong (thread-safe increments)
 * - recordsFailed: Long -> AtomicLong (thread-safe increments)
 * - bytesProcessed: Long -> AtomicLong (thread-safe increments)
 * - errors: ArrayList -> CopyOnWriteArrayList (thread-safe modifications)
 *
 * Why this matters:
 * - JdbcConnector and PooledJdbcConnector use ExecutorService with 4 threads
 * - Multiple threads increment counters and add errors concurrently
 * - Previous implementation caused race conditions and data loss
 *
 * @see com.jivs.platform.service.extraction.JdbcConnector
 * @see com.jivs.platform.service.extraction.PooledJdbcConnector
 */
public class ExtractionResult {

    // Thread-safe atomic counters
    private final AtomicLong recordsExtracted = new AtomicLong(0);
    private final AtomicLong recordsFailed = new AtomicLong(0);
    private final AtomicLong bytesProcessed = new AtomicLong(0);

    // Thread-safe error collection
    private final List<String> errors = new CopyOnWriteArrayList<>();

    // Single-thread fields (set once by main thread, read by many)
    @Getter
    private volatile String outputPath;

    @Getter
    private volatile String metadata;

    /**
     * Default constructor for backward compatibility
     */
    public ExtractionResult() {
        // Fields initialized inline
    }

    /**
     * Constructor for testing with initial values
     */
    public ExtractionResult(Long recordsExtracted, Long recordsFailed, Long bytesProcessed,
                           List<String> errors, String outputPath, String metadata) {
        if (recordsExtracted != null) {
            this.recordsExtracted.set(recordsExtracted);
        }
        if (recordsFailed != null) {
            this.recordsFailed.set(recordsFailed);
        }
        if (bytesProcessed != null) {
            this.bytesProcessed.set(bytesProcessed);
        }
        if (errors != null) {
            this.errors.addAll(errors);
        }
        this.outputPath = outputPath;
        this.metadata = metadata;
    }

    // Thread-safe getters
    public Long getRecordsExtracted() {
        return recordsExtracted.get();
    }

    public Long getRecordsFailed() {
        return recordsFailed.get();
    }

    public Long getBytesProcessed() {
        return bytesProcessed.get();
    }

    public List<String> getErrors() {
        return errors;
    }

    // Thread-safe setters (use atomic operations)
    public void setRecordsExtracted(Long count) {
        recordsExtracted.set(count != null ? count : 0L);
    }

    public void setRecordsFailed(Long count) {
        recordsFailed.set(count != null ? count : 0L);
    }

    public void setBytesProcessed(Long bytes) {
        bytesProcessed.set(bytes != null ? bytes : 0L);
    }

    public void setOutputPath(String path) {
        this.outputPath = path;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    // Thread-safe increment operations (NEW - for concurrent updates)
    public void addRecordsExtracted(long count) {
        recordsExtracted.addAndGet(count);
    }

    public void addRecordsFailed(long count) {
        recordsFailed.addAndGet(count);
    }

    public void addBytesProcessed(long bytes) {
        bytesProcessed.addAndGet(bytes);
    }

    /**
     * Thread-safe error addition
     * Null or empty errors are ignored
     */
    public void addError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            errors.add(error);
        }
    }

    /**
     * Replace entire error list (thread-safe)
     */
    public void setErrors(List<String> errorList) {
        errors.clear();
        if (errorList != null) {
            errors.addAll(errorList);
        }
    }

    /**
     * Get total records (extracted + failed)
     */
    public long getTotalRecordsProcessed() {
        return recordsExtracted.get() + recordsFailed.get();
    }

    /**
     * Get success rate as percentage
     */
    public double getSuccessRate() {
        long total = getTotalRecordsProcessed();
        if (total == 0) {
            return 0.0;
        }
        return (recordsExtracted.get() * 100.0) / total;
    }

    /**
     * Check if extraction has errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Merge another result into this one (thread-safe)
     * Useful for combining results from multiple batches
     */
    public void merge(ExtractionResult other) {
        if (other == null) {
            return;
        }

        addRecordsExtracted(other.getRecordsExtracted());
        addRecordsFailed(other.getRecordsFailed());
        addBytesProcessed(other.getBytesProcessed());

        if (other.hasErrors()) {
            errors.addAll(other.getErrors());
        }
    }

    @Override
    public String toString() {
        return String.format(
            "ExtractionResult{extracted=%d, failed=%d, bytes=%d, errors=%d, successRate=%.2f%%, outputPath='%s'}",
            getRecordsExtracted(),
            getRecordsFailed(),
            getBytesProcessed(),
            errors.size(),
            getSuccessRate(),
            outputPath != null ? outputPath : "null"
        );
    }
}
