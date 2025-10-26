package com.jivs.platform.service.extraction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for thread-safe ExtractionResult
 *
 * CRITICAL: These tests verify the concurrency fixes
 * that prevent data corruption in multi-threaded extractions
 *
 * Background:
 * - JdbcConnector uses ExecutorService with 4 threads
 * - Multiple threads increment counters and add errors concurrently
 * - Previous ArrayList implementation was NOT thread-safe
 * - Caused ConcurrentModificationException and lost updates
 *
 * Fix:
 * - AtomicLong for counters (lock-free atomic operations)
 * - CopyOnWriteArrayList for errors (thread-safe list)
 *
 * @see com.jivs.platform.service.extraction.ExtractionResult
 * @author jivs-extraction-expert
 */
@DisplayName("ExtractionResult Thread-Safety Tests")
class ExtractionResultTest {

    @Test
    @DisplayName("Should handle concurrent record increments without data loss")
    @Timeout(30)
    void shouldHandleConcurrentRecordIncrement() throws InterruptedException {
        // Arrange
        ExtractionResult result = new ExtractionResult();
        int threadCount = 20;
        int incrementsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act - Concurrent increments from multiple threads
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        result.addRecordsExtracted(1);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Test should complete within 30 seconds");
        executor.shutdown();

        // Assert - No lost updates
        long expected = (long) threadCount * incrementsPerThread;
        assertEquals(expected, result.getRecordsExtracted(),
            "All increments should be counted (no lost updates)");
    }

    @Test
    @DisplayName("Should handle concurrent error additions without ConcurrentModificationException")
    @Timeout(30)
    void shouldHandleConcurrentErrorAdditions() throws InterruptedException {
        // Arrange
        ExtractionResult result = new ExtractionResult();
        int threadCount = 10;
        int errorsPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act - Concurrent error additions
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < errorsPerThread; i++) {
                        result.addError("Error from thread " + threadId + " #" + i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert - All errors recorded
        assertEquals(threadCount * errorsPerThread, result.getErrors().size(),
            "All errors should be recorded");

        // Assert - No ConcurrentModificationException when iterating
        assertDoesNotThrow(() -> {
            int count = 0;
            for (String error : result.getErrors()) {
                assertNotNull(error);
                count++;
            }
            assertEquals(threadCount * errorsPerThread, count);
        }, "Should be able to iterate errors without ConcurrentModificationException");
    }

    @Test
    @DisplayName("Should handle mixed concurrent operations (increments + errors)")
    @Timeout(30)
    void shouldHandleMixedConcurrentOperations() throws InterruptedException {
        // Arrange
        ExtractionResult result = new ExtractionResult();
        int threadCount = 15;
        int operationsPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act - Mix of different operations from multiple threads
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        // Increment different counters
                        result.addRecordsExtracted(1);
                        result.addBytesProcessed(100);

                        // Add error occasionally
                        if (i % 10 == 0) {
                            result.addError("Error " + threadId + "-" + i);
                            result.addRecordsFailed(1);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert - All operations counted correctly
        assertEquals(threadCount * operationsPerThread, result.getRecordsExtracted());
        assertEquals(threadCount * operationsPerThread * 100L, result.getBytesProcessed());
        assertEquals(threadCount * (operationsPerThread / 10), result.getErrors().size());
        assertEquals(threadCount * (operationsPerThread / 10), result.getRecordsFailed());
    }

    @Test
    @DisplayName("Should calculate success rate correctly")
    void shouldCalculateSuccessRateCorrectly() {
        ExtractionResult result = new ExtractionResult();
        result.setRecordsExtracted(80L);
        result.setRecordsFailed(20L);

        assertEquals(80.0, result.getSuccessRate(), 0.01,
            "Success rate should be 80%");

        // Edge case: No records
        ExtractionResult emptyResult = new ExtractionResult();
        assertEquals(0.0, emptyResult.getSuccessRate(), 0.01,
            "Empty result should have 0% success rate");

        // Edge case: All failed
        ExtractionResult failedResult = new ExtractionResult();
        failedResult.setRecordsFailed(100L);
        assertEquals(0.0, failedResult.getSuccessRate(), 0.01,
            "All failed should be 0% success rate");

        // Edge case: All succeeded
        ExtractionResult successResult = new ExtractionResult();
        successResult.setRecordsExtracted(100L);
        assertEquals(100.0, successResult.getSuccessRate(), 0.01,
            "All succeeded should be 100% success rate");
    }

    @Test
    @DisplayName("Should merge results correctly")
    void shouldMergeResultsCorrectly() {
        // Arrange
        ExtractionResult result1 = new ExtractionResult();
        result1.setRecordsExtracted(100L);
        result1.setBytesProcessed(1000L);
        result1.addError("Error 1");

        ExtractionResult result2 = new ExtractionResult();
        result2.setRecordsExtracted(200L);
        result2.setBytesProcessed(2000L);
        result2.addError("Error 2");
        result2.addError("Error 3");

        // Act
        result1.merge(result2);

        // Assert
        assertEquals(300L, result1.getRecordsExtracted(),
            "Records should be summed");
        assertEquals(3000L, result1.getBytesProcessed(),
            "Bytes should be summed");
        assertEquals(3, result1.getErrors().size(),
            "Errors should be combined");
    }

    @Test
    @DisplayName("Should handle null error additions gracefully")
    void shouldHandleNullErrorAdditions() {
        ExtractionResult result = new ExtractionResult();

        result.addError(null);
        result.addError("");
        result.addError("   ");
        result.addError("Valid error");

        // Only valid error should be added
        assertEquals(1, result.getErrors().size());
        assertEquals("Valid error", result.getErrors().get(0));
    }

    @Test
    @DisplayName("Should support backward-compatible constructor")
    void shouldSupportBackwardCompatibleConstructor() {
        // Arrange
        List<String> errors = new ArrayList<>();
        errors.add("Error 1");
        errors.add("Error 2");

        // Act
        ExtractionResult result = new ExtractionResult(
            100L, 10L, 5000L, errors, "/tmp/output.parquet", "metadata"
        );

        // Assert
        assertEquals(100L, result.getRecordsExtracted());
        assertEquals(10L, result.getRecordsFailed());
        assertEquals(5000L, result.getBytesProcessed());
        assertEquals(2, result.getErrors().size());
        assertEquals("/tmp/output.parquet", result.getOutputPath());
        assertEquals("metadata", result.getMetadata());
    }

    @Test
    @DisplayName("Should provide helpful toString output")
    void shouldProvideHelpfulToStringOutput() {
        ExtractionResult result = new ExtractionResult();
        result.setRecordsExtracted(100L);
        result.setRecordsFailed(25L);
        result.setBytesProcessed(5000L);
        result.addError("Test error");
        result.setOutputPath("/tmp/test.parquet");

        String output = result.toString();

        assertTrue(output.contains("extracted=100"));
        assertTrue(output.contains("failed=25"));
        assertTrue(output.contains("bytes=5000"));
        assertTrue(output.contains("errors=1"));
        assertTrue(output.contains("successRate=80.00"));
        assertTrue(output.contains("/tmp/test.parquet"));
    }

    @Test
    @DisplayName("Should handle concurrent read and write operations")
    @Timeout(30)
    void shouldHandleConcurrentReadAndWrite() throws InterruptedException {
        // Arrange
        ExtractionResult result = new ExtractionResult();
        int writerThreads = 10;
        int readerThreads = 5;
        int operations = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(writerThreads + readerThreads);
        CountDownLatch latch = new CountDownLatch(writerThreads + readerThreads);
        AtomicInteger readExceptions = new AtomicInteger(0);

        // Writers
        for (int i = 0; i < writerThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operations; j++) {
                        result.addRecordsExtracted(1);
                        result.addError("Error " + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Readers (reading while writers are modifying)
        for (int i = 0; i < readerThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operations; j++) {
                        // Read operations
                        long extracted = result.getRecordsExtracted();
                        double rate = result.getSuccessRate();
                        int errorCount = result.getErrors().size();

                        // Try to iterate errors (most likely to fail with ArrayList)
                        try {
                            for (String error : result.getErrors()) {
                                assertNotNull(error);
                            }
                        } catch (ConcurrentModificationException e) {
                            readExceptions.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert - No ConcurrentModificationException during reads
        assertEquals(0, readExceptions.get(),
            "Should not have any ConcurrentModificationException");

        // Assert - All writes completed
        assertEquals(writerThreads * operations, result.getRecordsExtracted());
        assertEquals(writerThreads * operations, result.getErrors().size());
    }

    @Test
    @DisplayName("Should handle high-frequency concurrent updates")
    @Timeout(30)
    void shouldHandleHighFrequencyConcurrentUpdates() throws InterruptedException {
        // Simulate real extraction workload: 4 threads, 10,000 records each
        ExtractionResult result = new ExtractionResult();
        int threads = 4; // Same as JdbcConnector PARALLEL_THREADS
        int recordsPerThread = 10000;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < recordsPerThread; i++) {
                        result.addRecordsExtracted(1);
                        result.addBytesProcessed(128); // Average record size

                        // Simulate occasional errors
                        if (i % 1000 == 0) {
                            result.addRecordsFailed(1);
                            result.addError("Connection timeout at record " + i);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify results
        assertEquals(threads * recordsPerThread, result.getRecordsExtracted());
        assertEquals(threads * recordsPerThread * 128L, result.getBytesProcessed());
        assertEquals(threads * 10, result.getRecordsFailed()); // 10 errors per thread
        assertEquals(threads * 10, result.getErrors().size());
    }
}
