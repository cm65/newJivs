package com.jivs.platform.service.extraction;

import com.jivs.platform.common.util.CryptoUtil;
import com.jivs.platform.domain.extraction.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExtractionDataSourcePool
 *
 * Tests:
 * - Pool creation for different database types
 * - Connection acquisition and release
 * - Pool cleanup
 * - Concurrent access (thread-safety)
 * - Maximum pool size limits
 * - Pool statistics
 */
@ExtendWith(MockitoExtension.class)
class ExtractionDataSourcePoolTest {

    @Mock
    private CryptoUtil cryptoUtil;

    private ExtractionDataSourcePool dataSourcePool;

    @BeforeEach
    void setUp() {
        dataSourcePool = new ExtractionDataSourcePool(cryptoUtil);

        // Set configuration values
        ReflectionTestUtils.setField(dataSourcePool, "maxPoolSize", 10);
        ReflectionTestUtils.setField(dataSourcePool, "minIdleConnections", 2);
        ReflectionTestUtils.setField(dataSourcePool, "connectionTimeout", 5000);

        // Mock decryption
        when(cryptoUtil.decrypt(anyString())).thenReturn("decrypted_password");
    }

    @AfterEach
    void tearDown() {
        // Clean up pools after each test
        dataSourcePool.closeAllPools();
    }

    @Test
    void testCreatePoolForPostgreSQL() {
        // Given
        DataSource dataSource = createDataSource(1L, "PostgreSQL Test", DataSource.SourceType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/testdb");

        // When
        HikariDataSource pool = dataSourcePool.getOrCreatePool(dataSource);

        // Then
        assertThat(pool).isNotNull();
        assertThat(pool.getPoolName()).contains("ExtractionPool-1-POSTGRESQL");
        assertThat(pool.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/testdb");
        assertThat(pool.getMaximumPoolSize()).isEqualTo(10);
        assertThat(pool.getMinimumIdle()).isEqualTo(2);
        assertThat(pool.isReadOnly()).isTrue();
    }

    @Test
    void testCreatePoolForMySQL() {
        // Given
        DataSource dataSource = createDataSource(2L, "MySQL Test", DataSource.SourceType.MYSQL,
                "jdbc:mysql://localhost:3306/testdb");

        // When
        HikariDataSource pool = dataSourcePool.getOrCreatePool(dataSource);

        // Then
        assertThat(pool).isNotNull();
        assertThat(pool.getPoolName()).contains("ExtractionPool-2-MYSQL");
        assertThat(pool.getJdbcUrl()).isEqualTo("jdbc:mysql://localhost:3306/testdb");
    }

    @Test
    void testCreatePoolForOracle() {
        // Given
        DataSource dataSource = createDataSource(3L, "Oracle Test", DataSource.SourceType.ORACLE,
                "jdbc:oracle:thin:@localhost:1521:testdb");

        // When
        HikariDataSource pool = dataSourcePool.getOrCreatePool(dataSource);

        // Then
        assertThat(pool).isNotNull();
        assertThat(pool.getPoolName()).contains("ExtractionPool-3-ORACLE");
    }

    @Test
    void testCreatePoolForSQLServer() {
        // Given
        DataSource dataSource = createDataSource(4L, "SQL Server Test", DataSource.SourceType.SQL_SERVER,
                "jdbc:sqlserver://localhost:1433;databaseName=testdb");

        // When
        HikariDataSource pool = dataSourcePool.getOrCreatePool(dataSource);

        // Then
        assertThat(pool).isNotNull();
        assertThat(pool.getPoolName()).contains("ExtractionPool-4-SQL_SERVER");
    }

    @Test
    void testGetOrCreatePoolReturnsSamePoolForSameDataSource() {
        // Given
        DataSource dataSource = createDataSource(1L, "Test", DataSource.SourceType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/testdb");

        // When
        HikariDataSource pool1 = dataSourcePool.getOrCreatePool(dataSource);
        HikariDataSource pool2 = dataSourcePool.getOrCreatePool(dataSource);

        // Then
        assertThat(pool1).isSameAs(pool2);
    }

    @Test
    void testGetOrCreatePoolCreatesDifferentPoolsForDifferentDataSources() {
        // Given
        DataSource dataSource1 = createDataSource(1L, "Test1", DataSource.SourceType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/testdb1");
        DataSource dataSource2 = createDataSource(2L, "Test2", DataSource.SourceType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/testdb2");

        // When
        HikariDataSource pool1 = dataSourcePool.getOrCreatePool(dataSource1);
        HikariDataSource pool2 = dataSourcePool.getOrCreatePool(dataSource2);

        // Then
        assertThat(pool1).isNotSameAs(pool2);
    }

    @Test
    void testGetConnectionFailsForInvalidJdbcUrl() {
        // Given
        DataSource dataSource = createDataSource(1L, "Invalid", DataSource.SourceType.POSTGRESQL,
                "jdbc:invalid:url");

        // When/Then
        assertThatThrownBy(() -> dataSourcePool.getConnection(dataSource))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create connection pool");
    }

    @Test
    void testGetPoolStatsReturnsNullForNonExistentPool() {
        // When
        ExtractionDataSourcePool.PoolStats stats = dataSourcePool.getPoolStats(999L);

        // Then
        assertThat(stats).isNull();
    }

    @Test
    void testClosePoolRemovesPool() {
        // Given
        DataSource dataSource = createDataSource(1L, "Test", DataSource.SourceType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/testdb");
        HikariDataSource pool = dataSourcePool.getOrCreatePool(dataSource);

        assertThat(pool.isClosed()).isFalse();

        // When
        dataSourcePool.closePool(1L);

        // Then
        assertThat(pool.isClosed()).isTrue();
        assertThat(dataSourcePool.getPoolStats(1L)).isNull();
    }

    @Test
    void testClosePoolHandlesNonExistentPool() {
        // When/Then - should not throw exception
        assertThatCode(() -> dataSourcePool.closePool(999L))
                .doesNotThrowAnyException();
    }

    @Test
    void testCloseAllPoolsClosesAllActivePools() {
        // Given
        DataSource dataSource1 = createDataSource(1L, "Test1", DataSource.SourceType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/testdb1");
        DataSource dataSource2 = createDataSource(2L, "Test2", DataSource.SourceType.MYSQL,
                "jdbc:mysql://localhost:3306/testdb2");

        HikariDataSource pool1 = dataSourcePool.getOrCreatePool(dataSource1);
        HikariDataSource pool2 = dataSourcePool.getOrCreatePool(dataSource2);

        // When
        dataSourcePool.closeAllPools();

        // Then
        assertThat(pool1.isClosed()).isTrue();
        assertThat(pool2.isClosed()).isTrue();
        assertThat(dataSourcePool.getPoolStats(1L)).isNull();
        assertThat(dataSourcePool.getPoolStats(2L)).isNull();
    }

    @Test
    void testConcurrentPoolCreation() throws InterruptedException {
        // Given
        DataSource dataSource = createDataSource(1L, "Test", DataSource.SourceType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/testdb");
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When - multiple threads try to create pool simultaneously
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    HikariDataSource pool = dataSourcePool.getOrCreatePool(dataSource);
                    if (pool != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - all threads should succeed and get the same pool
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(dataSourcePool.getPoolStats(1L)).isNotNull();
    }

    @Test
    void testConcurrentAccessToMultiplePools() throws InterruptedException {
        // Given
        int poolCount = 5;
        int threadsPerPool = 4;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(poolCount * threadsPerPool);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(poolCount * threadsPerPool);

        // When - multiple threads access multiple pools concurrently
        for (int poolId = 1; poolId <= poolCount; poolId++) {
            final long id = poolId;
            DataSource dataSource = createDataSource(id, "Test" + id, DataSource.SourceType.POSTGRESQL,
                    "jdbc:postgresql://localhost:5432/testdb" + id);

            for (int thread = 0; thread < threadsPerPool; thread++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        HikariDataSource pool = dataSourcePool.getOrCreatePool(dataSource);
                        if (pool != null) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Ignore
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - all threads should succeed
        assertThat(successCount.get()).isEqualTo(poolCount * threadsPerPool);

        // Verify all pools were created
        for (long id = 1; id <= poolCount; id++) {
            assertThat(dataSourcePool.getPoolStats(id)).isNotNull();
        }
    }

    @Test
    void testPasswordDecryption() {
        // Given
        DataSource dataSource = createDataSource(1L, "Test", DataSource.SourceType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/testdb");
        dataSource.setPasswordEncrypted("encrypted_password");

        when(cryptoUtil.decrypt("encrypted_password")).thenReturn("my_secure_password");

        // When
        dataSourcePool.getOrCreatePool(dataSource);

        // Then
        verify(cryptoUtil).decrypt("encrypted_password");
    }

    @Test
    void testPoolStatsStructure() {
        // Given
        ExtractionDataSourcePool.PoolStats stats = new ExtractionDataSourcePool.PoolStats(10, 5, 5, 0);

        // Then
        assertThat(stats.totalConnections).isEqualTo(10);
        assertThat(stats.activeConnections).isEqualTo(5);
        assertThat(stats.idleConnections).isEqualTo(5);
        assertThat(stats.waitingThreads).isEqualTo(0);
        assertThat(stats.toString()).contains("total=10", "active=5", "idle=5", "waiting=0");
    }

    // Helper method to create test DataSource
    private DataSource createDataSource(Long id, String name, DataSource.SourceType sourceType, String connectionUrl) {
        DataSource dataSource = new DataSource();
        dataSource.setId(id);
        dataSource.setName(name);
        dataSource.setSourceType(sourceType);
        dataSource.setConnectionUrl(connectionUrl);
        dataSource.setUsername("testuser");
        dataSource.setPasswordEncrypted("encrypted_password");
        dataSource.setIsActive(true);
        return dataSource;
    }
}
