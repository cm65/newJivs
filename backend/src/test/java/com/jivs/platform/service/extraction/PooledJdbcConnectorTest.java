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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PooledJdbcConnector
 *
 * Tests:
 * - Batch processing with various batch sizes
 * - Parallel stream processing
 * - Progress tracking
 * - Error handling and connection cleanup
 * - Fetch size configuration
 */
@ExtendWith(MockitoExtension.class)
class PooledJdbcConnectorTest {

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

    private DataSource dataSource;
    private PooledJdbcConnector connector;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = createTestDataSource();
        connector = new PooledJdbcConnector(dataSourcePool, dataSource);

        // Default mock setup
        when(dataSourcePool.getConnection(any(DataSource.class))).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
    }

    @AfterEach
    void tearDown() {
        connector.close();
    }

    @Test
    void testTestConnectionSuccess() throws SQLException {
        // Given
        when(connection.isClosed()).thenReturn(false);

        // When
        boolean result = connector.testConnection();

        // Then
        assertThat(result).isTrue();
        verify(dataSourcePool).getConnection(dataSource);
        verify(connection).isClosed();
    }

    @Test
    void testTestConnectionFailure() throws SQLException {
        // Given
        when(dataSourcePool.getConnection(any(DataSource.class)))
                .thenThrow(new SQLException("Connection failed"));

        // When
        boolean result = connector.testConnection();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void testExtractWithSmallDataset() throws SQLException {
        // Given - 100 records (less than batch size of 1000)
        setupResultSet(100, 3);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM test_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getRecordsExtracted()).isEqualTo(100L);
        assertThat(result.getRecordsFailed()).isEqualTo(0L);
        assertThat(result.getBytesProcessed()).isGreaterThan(0L);
        assertThat(result.getOutputPath()).isEqualTo("/tmp/test");

        // Verify fetch size was set
        verify(statement).setFetchSize(1000);
        verify(statement).setQueryTimeout(300);
    }

    @Test
    void testExtractWithLargeDataset() throws SQLException {
        // Given - 5000 records (5 batches of 1000)
        setupResultSet(5000, 3);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM large_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getRecordsExtracted()).isEqualTo(5000L);
        assertThat(result.getRecordsFailed()).isEqualTo(0L);
        assertThat(result.getBytesProcessed()).isGreaterThan(0L);
    }

    @Test
    void testExtractWithBatchBoundary() throws SQLException {
        // Given - exactly 1000 records (1 full batch, no remainder)
        setupResultSet(1000, 3);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM test_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getRecordsExtracted()).isEqualTo(1000L);
        assertThat(result.getRecordsFailed()).isEqualTo(0L);
    }

    @Test
    void testExtractWithBatchPlusOne() throws SQLException {
        // Given - 1001 records (1 full batch + 1 record)
        setupResultSet(1001, 3);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM test_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getRecordsExtracted()).isEqualTo(1001L);
        assertThat(result.getRecordsFailed()).isEqualTo(0L);
    }

    @Test
    void testExtractWithEmptyResultSet() throws SQLException {
        // Given - 0 records
        setupResultSet(0, 3);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM empty_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getRecordsExtracted()).isEqualTo(0L);
        assertThat(result.getRecordsFailed()).isEqualTo(0L);
        assertThat(result.getBytesProcessed()).isEqualTo(0L);
    }

    @Test
    void testExtractWithNullValues() throws SQLException {
        // Given
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnName(2)).thenReturn("name");
        when(metaData.getColumnName(3)).thenReturn("description");

        // First record with nulls
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(1);
        when(resultSet.getObject(2)).thenReturn(null); // NULL value
        when(resultSet.getObject(3)).thenReturn(null); // NULL value

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM test_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getRecordsExtracted()).isEqualTo(1L);
        assertThat(result.getRecordsFailed()).isEqualTo(0L);
    }

    @Test
    void testExtractWithDatabaseError() throws SQLException {
        // Given
        when(dataSourcePool.getConnection(any(DataSource.class)))
                .thenThrow(new SQLException("Database connection failed"));

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM test_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getRecordsExtracted()).isEqualTo(0L);
        assertThat(result.getRecordsFailed()).isEqualTo(0L);
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0)).contains("Database connection failed");
    }

    @Test
    void testExtractWithQueryExecutionError() throws SQLException {
        // Given
        when(statement.executeQuery()).thenThrow(new SQLException("Query execution failed"));

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM invalid_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getRecordsExtracted()).isEqualTo(0L);
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0)).contains("Query execution failed");
    }

    @Test
    void testExtractWithMultipleColumns() throws SQLException {
        // Given - 10 columns
        setupResultSet(100, 10);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM wide_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getRecordsExtracted()).isEqualTo(100L);
        assertThat(result.getBytesProcessed()).isGreaterThan(0L);
    }

    @Test
    void testExtractBytesProcessedCalculation() throws SQLException {
        // Given
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnName(2)).thenReturn("text");

        // Single record with known byte size
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(1);
        when(resultSet.getObject(2)).thenReturn("Hello World"); // 11 bytes

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM test_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getRecordsExtracted()).isEqualTo(1L);
        // Should have bytes for "1" (1 byte) + "Hello World" (11 bytes) = 12 bytes
        assertThat(result.getBytesProcessed()).isGreaterThanOrEqualTo(12L);
    }

    @Test
    void testGetConnectorType() {
        // When
        String connectorType = connector.getConnectorType();

        // Then
        assertThat(connectorType).isEqualTo("POOLED-JDBC-POSTGRESQL");
    }

    @Test
    void testCloseConnector() {
        // When
        connector.close();

        // Then - should not throw exception and can be called multiple times
        assertThatCode(() -> connector.close()).doesNotThrowAnyException();
    }

    @Test
    void testDefaultQueryParameter() throws SQLException {
        // Given - no query provided
        setupResultSet(1, 1);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("outputPath", "/tmp/test");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        verify(connection).prepareStatement("SELECT 1");
    }

    @Test
    void testDefaultOutputPathParameter() throws SQLException {
        // Given - no outputPath provided
        setupResultSet(10, 3);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM test_table");

        // When
        ExtractionResult result = connector.extract(parameters);

        // Then
        assertThat(result.getOutputPath()).isEqualTo("/tmp/extraction");
    }

    @Test
    void testStatementClosedAfterExtraction() throws SQLException {
        // Given
        setupResultSet(10, 3);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM test_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        connector.extract(parameters);

        // Then
        verify(resultSet).close();
        verify(statement).close();
    }

    @Test
    void testConnectionReturnedToPoolAfterExtraction() throws SQLException {
        // Given
        setupResultSet(10, 3);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM test_table");
        parameters.put("outputPath", "/tmp/test");

        // When
        connector.extract(parameters);

        // Then
        verify(connection).close(); // Try-with-resources closes connection, returning it to pool
    }

    @Test
    void testConcurrentExtractions() throws InterruptedException, SQLException {
        // Given
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When - multiple threads perform extractions simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Create new connector for each thread
                    Connection conn = mock(Connection.class);
                    PreparedStatement stmt = mock(PreparedStatement.class);
                    ResultSet rs = mock(ResultSet.class);
                    ResultSetMetaData meta = mock(ResultSetMetaData.class);

                    when(dataSourcePool.getConnection(any(DataSource.class))).thenReturn(conn);
                    when(conn.prepareStatement(anyString())).thenReturn(stmt);
                    when(stmt.executeQuery()).thenReturn(rs);
                    when(rs.getMetaData()).thenReturn(meta);
                    when(meta.getColumnCount()).thenReturn(2);
                    when(meta.getColumnName(anyInt())).thenReturn("col");
                    when(rs.next()).thenReturn(true, false); // 1 record
                    when(rs.getObject(anyInt())).thenReturn("value");

                    PooledJdbcConnector threadConnector = new PooledJdbcConnector(dataSourcePool, dataSource);

                    Map<String, String> params = new HashMap<>();
                    params.put("query", "SELECT * FROM test_table");
                    params.put("outputPath", "/tmp/test" + threadId);

                    ExtractionResult result = threadConnector.extract(params);

                    if (result.getRecordsExtracted() > 0) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    // Helper methods

    private DataSource createTestDataSource() {
        DataSource ds = new DataSource();
        ds.setId(1L);
        ds.setName("Test DataSource");
        ds.setSourceType(DataSource.SourceType.POSTGRESQL);
        ds.setConnectionUrl("jdbc:postgresql://localhost:5432/testdb");
        ds.setUsername("testuser");
        ds.setIsActive(true);
        return ds;
    }

    private void setupResultSet(int recordCount, int columnCount) throws SQLException {
        when(metaData.getColumnCount()).thenReturn(columnCount);

        // Setup column names
        for (int i = 1; i <= columnCount; i++) {
            when(metaData.getColumnName(i)).thenReturn("column" + i);
        }

        // Setup result set with recordCount records
        Boolean[] results = new Boolean[recordCount + 1];
        for (int i = 0; i < recordCount; i++) {
            results[i] = true;
        }
        results[recordCount] = false;

        when(resultSet.next()).thenReturn(results[0], java.util.Arrays.copyOfRange(results, 1, results.length));

        // Setup column values - return test data
        for (int col = 1; col <= columnCount; col++) {
            when(resultSet.getObject(col)).thenAnswer(invocation -> {
                return "value_col" + col;
            });
        }
    }
}
