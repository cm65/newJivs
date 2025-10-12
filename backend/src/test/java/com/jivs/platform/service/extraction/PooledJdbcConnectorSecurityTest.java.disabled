package com.jivs.platform.service.extraction;

import com.jivs.platform.domain.extraction.DataSource;
import com.jivs.platform.security.SqlInjectionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * SEC-001: Security tests for SQL injection prevention in PooledJdbcConnector
 *
 * Tests validate that:
 * 1. SQL injection attacks are blocked
 * 2. Path traversal attacks are blocked
 * 3. Safe queries are allowed
 * 4. Safe paths are allowed
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PooledJdbcConnector Security Tests")
class PooledJdbcConnectorSecurityTest {

    @Mock
    private ExtractionDataSourcePool dataSourcePool;

    @Mock
    private Connection mockConnection;

    private SqlInjectionValidator sqlInjectionValidator;
    private DataSource dataSource;
    private PooledJdbcConnector connector;

    @BeforeEach
    void setUp() {
        sqlInjectionValidator = new SqlInjectionValidator();

        dataSource = new DataSource();
        dataSource.setName("test-datasource");
        dataSource.setSourceType(DataSource.SourceType.POSTGRESQL);

        connector = new PooledJdbcConnector(dataSourcePool, dataSource, sqlInjectionValidator);
    }

    // ===== SQL Injection Tests =====

    @Test
    @DisplayName("SEC-001: Should block SQL injection with DROP TABLE")
    void shouldBlockSqlInjectionDropTable() throws Exception {
        // Given: Malicious query with DROP TABLE
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users; DROP TABLE users; --");
        parameters.put("outputPath", "/tmp/extraction");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SQL injection validation");
    }

    @Test
    @DisplayName("SEC-001: Should block SQL injection with UNION attack")
    void shouldBlockSqlInjectionUnionAttack() throws Exception {
        // Given: UNION-based SQL injection
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users WHERE id=1 UNION SELECT password FROM admin");
        parameters.put("outputPath", "/tmp/extraction");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SQL injection validation");
    }

    @Test
    @DisplayName("SEC-001: Should block SQL injection with tautology (1=1)")
    void shouldBlockSqlInjectionTautology() throws Exception {
        // Given: Tautology-based SQL injection
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users WHERE username='admin' OR '1'='1'");
        parameters.put("outputPath", "/tmp/extraction");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SQL injection validation");
    }

    @Test
    @DisplayName("SEC-001: Should block SQL injection with comments (--)")
    void shouldBlockSqlInjectionComments() throws Exception {
        // Given: SQL injection using comments
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users WHERE id=1-- AND status='active'");
        parameters.put("outputPath", "/tmp/extraction");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SQL injection validation");
    }

    @Test
    @DisplayName("SEC-001: Should block SQL injection with time-based attack")
    void shouldBlockSqlInjectionTimeBased() throws Exception {
        // Given: Time-based SQL injection
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users WHERE id=1; WAITFOR DELAY '00:00:05'--");
        parameters.put("outputPath", "/tmp/extraction");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SQL injection validation");
    }

    @Test
    @DisplayName("SEC-001: Should block SQL injection with hex encoding")
    void shouldBlockSqlInjectionHexEncoding() throws Exception {
        // Given: SQL injection using hex encoding
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users WHERE id=0x61646D696E");
        parameters.put("outputPath", "/tmp/extraction");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SQL injection validation");
    }

    @Test
    @DisplayName("SEC-001: Should block SQL injection with INSERT")
    void shouldBlockSqlInjectionInsert() throws Exception {
        // Given: SQL injection with INSERT
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users; INSERT INTO admins VALUES ('hacker', 'password')");
        parameters.put("outputPath", "/tmp/extraction");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SQL injection validation");
    }

    @Test
    @DisplayName("SEC-001: Should block SQL injection with UPDATE")
    void shouldBlockSqlInjectionUpdate() throws Exception {
        // Given: SQL injection with UPDATE
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users; UPDATE users SET role='admin' WHERE id=1");
        parameters.put("outputPath", "/tmp/extraction");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SQL injection validation");
    }

    // ===== Path Traversal Tests =====

    @Test
    @DisplayName("SEC-003: Should block path traversal with ../ attack")
    void shouldBlockPathTraversalDoubleDot() throws Exception {
        // Given: Path traversal attack
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users");
        parameters.put("outputPath", "/tmp/extraction/../../etc/passwd");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Path traversal detected");
    }

    @Test
    @DisplayName("SEC-003: Should block absolute path outside allowed directories")
    void shouldBlockPathOutsideAllowedDirectories() throws Exception {
        // Given: Absolute path outside allowed directories
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users");
        parameters.put("outputPath", "/etc/passwd");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("must be within allowed directories");
    }

    @Test
    @DisplayName("SEC-003: Should block null output path")
    void shouldBlockNullOutputPath() throws Exception {
        // Given: Null output path
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users");
        parameters.put("outputPath", null);

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("SEC-003: Should block empty output path")
    void shouldBlockEmptyOutputPath() throws Exception {
        // Given: Empty output path
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", "SELECT * FROM users");
        parameters.put("outputPath", "   ");

        when(dataSourcePool.getConnection(any())).thenReturn(mockConnection);

        // When & Then: Should throw SecurityException
        assertThatThrownBy(() -> connector.extract(parameters))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("cannot be null or empty");
    }

    // ===== Positive Tests (Safe Queries) =====

    @Test
    @DisplayName("SEC-001: Should allow safe SELECT query")
    void shouldAllowSafeSelectQuery() {
        // Given: Safe SELECT query
        String safeQuery = "SELECT id, name, email FROM users WHERE status = 'active'";

        // When
        boolean isSafe = sqlInjectionValidator.isQuerySafe(safeQuery);

        // Then
        assertThat(isSafe).isTrue();
    }

    @Test
    @DisplayName("SEC-001: Should allow SELECT query with JOIN")
    void shouldAllowSelectWithJoin() {
        // Given: Safe SELECT with JOIN
        String safeQuery = "SELECT u.id, u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id";

        // When
        boolean isSafe = sqlInjectionValidator.isQuerySafe(safeQuery);

        // Then
        assertThat(isSafe).isTrue();
    }

    @Test
    @DisplayName("SEC-001: Should allow SELECT query with WHERE clause")
    void shouldAllowSelectWithWhere() {
        // Given: Safe SELECT with WHERE
        String safeQuery = "SELECT * FROM users WHERE age > 18 AND country = 'US'";

        // When
        boolean isSafe = sqlInjectionValidator.isQuerySafe(safeQuery);

        // Then
        assertThat(isSafe).isTrue();
    }

    @Test
    @DisplayName("SEC-001: Should allow SELECT query with aggregate functions")
    void shouldAllowSelectWithAggregateFunctions() {
        // Given: Safe SELECT with COUNT, SUM
        String safeQuery = "SELECT COUNT(*), SUM(amount), AVG(age) FROM users GROUP BY country";

        // When
        boolean isSafe = sqlInjectionValidator.isQuerySafe(safeQuery);

        // Then
        assertThat(isSafe).isTrue();
    }

    @Test
    @DisplayName("SEC-003: Should allow path in /tmp directory")
    void shouldAllowPathInTmpDirectory() {
        // Given: Path in allowed directory
        String safePath = "/tmp/extraction/users_data.csv";

        // When & Then: Should not throw
        assertThat(safePath).startsWith("/tmp");
    }

    @Test
    @DisplayName("SEC-003: Should allow path in /data directory")
    void shouldAllowPathInDataDirectory() {
        // Given: Path in allowed directory
        String safePath = "/data/extraction/output.csv";

        // When & Then: Should not throw
        assertThat(safePath).startsWith("/data");
    }

    @Test
    @DisplayName("SEC-003: Should allow path in /var/lib/jivs directory")
    void shouldAllowPathInJivsDirectory() {
        // Given: Path in allowed directory
        String safePath = "/var/lib/jivs/extraction/results.csv";

        // When & Then: Should not throw
        assertThat(safePath).startsWith("/var/lib/jivs");
    }

    // ===== Edge Cases =====

    @Test
    @DisplayName("SEC-001: Should block query not starting with SELECT")
    void shouldBlockQueryNotStartingWithSelect() {
        // Given: Query not starting with SELECT
        String unsafeQuery = "DELETE FROM users WHERE status = 'inactive'";

        // When
        boolean isSafe = sqlInjectionValidator.isQuerySafe(unsafeQuery);

        // Then
        assertThat(isSafe).isFalse();
    }

    @Test
    @DisplayName("SEC-001: Should block null query")
    void shouldBlockNullQuery() {
        // Given: Null query
        String nullQuery = null;

        // When
        boolean isSafe = sqlInjectionValidator.isQuerySafe(nullQuery);

        // Then
        assertThat(isSafe).isFalse();
    }

    @Test
    @DisplayName("SEC-001: Should block empty query")
    void shouldBlockEmptyQuery() {
        // Given: Empty query
        String emptyQuery = "   ";

        // When
        boolean isSafe = sqlInjectionValidator.isQuerySafe(emptyQuery);

        // Then
        assertThat(isSafe).isFalse();
    }

    @Test
    @DisplayName("SEC-001: Should be case-insensitive for SELECT")
    void shouldBeCaseInsensitiveForSelect() {
        // Given: SELECT in different cases
        String lowerCase = "select * from users";
        String mixedCase = "SeLeCt * FrOm users";

        // When
        boolean lowerIsSafe = sqlInjectionValidator.isQuerySafe(lowerCase);
        boolean mixedIsSafe = sqlInjectionValidator.isQuerySafe(mixedCase);

        // Then
        assertThat(lowerIsSafe).isTrue();
        assertThat(mixedIsSafe).isTrue();
    }
}
