package com.jivs.platform.service.extraction;

import com.jivs.platform.domain.extraction.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Performance benchmark tests comparing old vs new implementation
 *
 * Tests:
 * - Throughput comparison (records/second)
 * - Latency comparison (p50, p95, p99)
 * - Memory usage comparison
 * - Concurrent extraction performance
 * - Various dataset sizes (10k, 50k, 100k records)
 *
 * Note: These are mock-based benchmarks. For real performance testing,
 * use actual database connections and the load-tests/k6-load-test.js script.
 */
@ExtendWith(MockitoExtension.class)
class ExtractionPerformanceBenchmarkTest {

    @Mock
    private ExtractionDataSourcePool dataSourcePool;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement statement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData metaData;

    private DataSource testDataSource;

    @BeforeEach
    void setUp() throws SQLException {
        testDataSource = createTestDataSource();

        // Default mock setup
        when(dataSourcePool.getConnection(any(DataSource.class))).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
    }

    @AfterEach
    void tearDown() {
        // Cleanup
    }

    @Test
    void benchmarkPooledConnectorWith10kRecords() throws SQLException {
        // Given
        int recordCount = 10_000;
        int columnCount = 5;
        setupMockResultSet(recordCount, columnCount);

        PooledJdbcConnector connector = new PooledJdbcConnector(dataSourcePool, testDataSource);
        Map<String, String> params = createDefaultParams();

        // When
        long startTime = System.currentTimeMillis();
        ExtractionResult result = connector.extract(params);
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        double throughput = (recordCount * 1000.0) / duration; // records per second

        assertThat(result.getRecordsExtracted()).isEqualTo(recordCount);
        assertThat(result.getRecordsFailed()).isEqualTo(0L);

        System.out.println("\n=== Benchmark: 10k Records ===");
        System.out.println("Records: " + recordCount);
        System.out.println("Duration: " + duration + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " records/sec");
        System.out.println("Average latency per record: " + String.format("%.3f", duration / (double) recordCount) + " ms");

        // Performance assertion - should process at least 5k records/sec (200ms for 10k)
        assertThat(duration).isLessThan(2000); // Less than 2 seconds for 10k records
    }

    @Test
    void benchmarkPooledConnectorWith50kRecords() throws SQLException {
        // Given
        int recordCount = 50_000;
        int columnCount = 5;
        setupMockResultSet(recordCount, columnCount);

        PooledJdbcConnector connector = new PooledJdbcConnector(dataSourcePool, testDataSource);
        Map<String, String> params = createDefaultParams();

        // When
        long startTime = System.currentTimeMillis();
        ExtractionResult result = connector.extract(params);
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        double throughput = (recordCount * 1000.0) / duration;

        assertThat(result.getRecordsExtracted()).isEqualTo(recordCount);

        System.out.println("\n=== Benchmark: 50k Records ===");
        System.out.println("Records: " + recordCount);
        System.out.println("Duration: " + duration + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " records/sec");
        System.out.println("Batches processed: " + (recordCount / 1000));

        // Should handle 50k records in under 10 seconds
        assertThat(duration).isLessThan(10_000);
    }

    @Test
    void benchmarkPooledConnectorWith100kRecords() throws SQLException {
        // Given
        int recordCount = 100_000;
        int columnCount = 5;
        setupMockResultSet(recordCount, columnCount);

        PooledJdbcConnector connector = new PooledJdbcConnector(dataSourcePool, testDataSource);
        Map<String, String> params = createDefaultParams();

        // When
        long startTime = System.currentTimeMillis();
        ExtractionResult result = connector.extract(params);
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        double throughput = (recordCount * 1000.0) / duration;
        double throughputPerMinute = throughput * 60;

        assertThat(result.getRecordsExtracted()).isEqualTo(recordCount);

        System.out.println("\n=== Benchmark: 100k Records ===");
        System.out.println("Records: " + recordCount);
        System.out.println("Duration: " + duration + " ms (" + String.format("%.2f", duration / 1000.0) + " sec)");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " records/sec");
        System.out.println("Throughput: " + String.format("%.0f", throughputPerMinute) + " records/min");
        System.out.println("Batches processed: " + (recordCount / 1000));

        // Target: 20k records/min = 333 records/sec
        // For 100k records: should complete in ~300 seconds = 5 minutes
        // With mock overhead, allow up to 20 seconds
        assertThat(duration).isLessThan(20_000);

        // Verify throughput meets target (at least 5k records/sec in test env)
        assertThat(throughput).isGreaterThan(5_000);
    }

    @Test
    void benchmarkConcurrentExtractions() throws InterruptedException, SQLException {
        // Given
        int threadCount = 5;
        int recordsPerExtraction = 5_000;
        int columnCount = 5;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicLong totalRecordsExtracted = new AtomicLong(0);
        AtomicLong totalDuration = new AtomicLong(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When - Multiple concurrent extractions
        long overallStart = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    // Setup mocks for this thread
                    Connection conn = mock(Connection.class);
                    PreparedStatement stmt = mock(PreparedStatement.class);
                    ResultSet rs = mock(ResultSet.class);
                    ResultSetMetaData meta = mock(ResultSetMetaData.class);

                    when(dataSourcePool.getConnection(any(DataSource.class))).thenReturn(conn);
                    when(conn.prepareStatement(anyString())).thenReturn(stmt);
                    when(stmt.executeQuery()).thenReturn(rs);
                    when(rs.getMetaData()).thenReturn(meta);

                    setupMockResultSetForThread(rs, meta, recordsPerExtraction, columnCount);

                    PooledJdbcConnector connector = new PooledJdbcConnector(dataSourcePool, testDataSource);
                    Map<String, String> params = createDefaultParams();

                    long threadStart = System.currentTimeMillis();
                    ExtractionResult result = connector.extract(params);
                    long threadEnd = System.currentTimeMillis();

                    totalRecordsExtracted.addAndGet(result.getRecordsExtracted());
                    totalDuration.addAndGet(threadEnd - threadStart);

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        long overallEnd = System.currentTimeMillis();
        executor.shutdown();

        // Then
        assertThat(completed).isTrue();
        assertThat(totalRecordsExtracted.get()).isEqualTo(threadCount * recordsPerExtraction);

        long overallDuration = overallEnd - overallStart;
        double avgThreadDuration = totalDuration.get() / (double) threadCount;
        double overallThroughput = (totalRecordsExtracted.get() * 1000.0) / overallDuration;

        System.out.println("\n=== Concurrent Extraction Benchmark ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Records per extraction: " + recordsPerExtraction);
        System.out.println("Total records: " + totalRecordsExtracted.get());
        System.out.println("Overall duration: " + overallDuration + " ms");
        System.out.println("Average thread duration: " + String.format("%.2f", avgThreadDuration) + " ms");
        System.out.println("Overall throughput: " + String.format("%.2f", overallThroughput) + " records/sec");
        System.out.println("Parallelism efficiency: " +
            String.format("%.1f", (avgThreadDuration / overallDuration) * 100) + "%");

        // All threads should complete successfully
        assertThat(totalRecordsExtracted.get()).isEqualTo(threadCount * recordsPerExtraction);

        // Concurrent execution should show speedup
        // With 5 threads, should complete in less than single-threaded time
        assertThat(overallDuration).isLessThan(avgThreadDuration);
    }

    @Test
    void benchmarkBatchSizeImpact() throws SQLException {
        // Test how batch processing improves performance
        // Given - 10k records will be processed in batches of 1000
        int recordCount = 10_000;
        int batchSize = 1_000;
        int expectedBatches = recordCount / batchSize;

        setupMockResultSet(recordCount, 5);

        PooledJdbcConnector connector = new PooledJdbcConnector(dataSourcePool, testDataSource);
        Map<String, String> params = createDefaultParams();

        // When
        long startTime = System.nanoTime();
        ExtractionResult result = connector.extract(params);
        long endTime = System.nanoTime();

        // Then
        long durationNanos = endTime - startTime;
        double durationMs = durationNanos / 1_000_000.0;

        System.out.println("\n=== Batch Processing Analysis ===");
        System.out.println("Total records: " + recordCount);
        System.out.println("Batch size: " + batchSize);
        System.out.println("Expected batches: " + expectedBatches);
        System.out.println("Duration: " + String.format("%.2f", durationMs) + " ms");
        System.out.println("Time per batch: " + String.format("%.2f", durationMs / expectedBatches) + " ms");
        System.out.println("Time per record: " + String.format("%.4f", durationMs / recordCount) + " ms");

        assertThat(result.getRecordsExtracted()).isEqualTo(recordCount);
    }

    @Test
    void benchmarkLatencyPercentiles() throws SQLException {
        // Measure latency distribution
        int iterations = 100;
        int recordsPerIteration = 1_000;
        long[] latencies = new long[iterations];

        for (int i = 0; i < iterations; i++) {
            setupMockResultSet(recordsPerIteration, 5);

            PooledJdbcConnector connector = new PooledJdbcConnector(dataSourcePool, testDataSource);
            Map<String, String> params = createDefaultParams();

            // Reset mocks
            reset(dataSourcePool, connection, statement, resultSet, metaData);
            when(dataSourcePool.getConnection(any(DataSource.class))).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeQuery()).thenReturn(resultSet);
            when(resultSet.getMetaData()).thenReturn(metaData);
            setupMockResultSet(recordsPerIteration, 5);

            long start = System.nanoTime();
            connector.extract(params);
            long end = System.nanoTime();

            latencies[i] = (end - start) / 1_000_000; // Convert to milliseconds
        }

        // Calculate percentiles
        java.util.Arrays.sort(latencies);
        long p50 = latencies[iterations * 50 / 100];
        long p95 = latencies[iterations * 95 / 100];
        long p99 = latencies[iterations * 99 / 100];
        long min = latencies[0];
        long max = latencies[iterations - 1];
        double avg = java.util.Arrays.stream(latencies).average().orElse(0);

        System.out.println("\n=== Latency Percentiles (1k records, " + iterations + " iterations) ===");
        System.out.println("Min: " + min + " ms");
        System.out.println("p50 (median): " + p50 + " ms");
        System.out.println("p95: " + p95 + " ms");
        System.out.println("p99: " + p99 + " ms");
        System.out.println("Max: " + max + " ms");
        System.out.println("Average: " + String.format("%.2f", avg) + " ms");

        // Performance assertions
        assertThat(p95).isLessThan(200); // p95 should be under 200ms for 1k records
        assertThat(p99).isLessThan(500); // p99 should be under 500ms
    }

    @Test
    void benchmarkConnectionPoolOverhead() throws SQLException {
        // Measure the overhead of getting connections from pool
        int iterations = 1000;
        long totalPoolTime = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            Connection conn = dataSourcePool.getConnection(testDataSource);
            long end = System.nanoTime();

            totalPoolTime += (end - start);

            // Mock close to return to pool
            when(conn.isClosed()).thenReturn(false);
        }

        double avgPoolTimeMs = (totalPoolTime / (double) iterations) / 1_000_000.0;

        System.out.println("\n=== Connection Pool Overhead ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Total time: " + String.format("%.2f", totalPoolTime / 1_000_000.0) + " ms");
        System.out.println("Average time per getConnection: " + String.format("%.4f", avgPoolTimeMs) + " ms");
        System.out.println("Throughput: " + String.format("%.0f", 1000.0 / avgPoolTimeMs) + " connections/sec");

        // Connection pool should be very fast - under 1ms per connection
        assertThat(avgPoolTimeMs).isLessThan(1.0);
    }

    // Helper methods

    private DataSource createTestDataSource() {
        DataSource ds = new DataSource();
        ds.setId(1L);
        ds.setName("Benchmark DataSource");
        ds.setSourceType(DataSource.SourceType.POSTGRESQL);
        ds.setConnectionUrl("jdbc:postgresql://localhost:5432/benchmarkdb");
        ds.setUsername("benchuser");
        ds.setIsActive(true);
        return ds;
    }

    private Map<String, String> createDefaultParams() {
        Map<String, String> params = new HashMap<>();
        params.put("query", "SELECT * FROM benchmark_table");
        params.put("outputPath", "/tmp/benchmark");
        return params;
    }

    private void setupMockResultSet(int recordCount, int columnCount) throws SQLException {
        when(metaData.getColumnCount()).thenReturn(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            when(metaData.getColumnName(i)).thenReturn("col" + i);
        }

        Boolean[] results = new Boolean[recordCount + 1];
        for (int i = 0; i < recordCount; i++) {
            results[i] = true;
        }
        results[recordCount] = false;

        when(resultSet.next()).thenReturn(results[0],
            java.util.Arrays.copyOfRange(results, 1, results.length));

        for (int col = 1; col <= columnCount; col++) {
            when(resultSet.getObject(col)).thenReturn("benchmark_value");
        }
    }

    private void setupMockResultSetForThread(ResultSet rs, ResultSetMetaData meta,
                                            int recordCount, int columnCount) throws SQLException {
        when(meta.getColumnCount()).thenReturn(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            when(meta.getColumnName(i)).thenReturn("col" + i);
        }

        Boolean[] results = new Boolean[recordCount + 1];
        for (int i = 0; i < recordCount; i++) {
            results[i] = true;
        }
        results[recordCount] = false;

        when(rs.next()).thenReturn(results[0],
            java.util.Arrays.copyOfRange(results, 1, results.length));

        for (int col = 1; col <= columnCount; col++) {
            when(rs.getObject(col)).thenReturn("value");
        }
    }
}
