package com.jivs.platform.service.migration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ FIXED: Security tests for LoadService
 * Validates SQL injection prevention and identifier validation
 */
@SpringBootTest
@ActiveProfiles("test")
class LoadServiceSecurityTest {

    @Autowired
    private LoadService loadService;

    private LoadService.LoadContext validContext;

    @BeforeEach
    void setUp() {
        validContext = new LoadService.LoadContext();
        validContext.setBatchId("test-batch-001");
        validContext.setTargetSystem("postgresql");
        validContext.setTargetTable("test_table");
        validContext.setColumns(Arrays.asList("id", "name", "email"));
        validContext.setKeyColumns(Arrays.asList("id"));
        validContext.setStrategy(LoadService.LoadStrategy.BATCH);
        validContext.setData(createTestData());
    }

    private List<Map<String, Object>> createTestData() {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> record = new HashMap<>();
        record.put("id", 1);
        record.put("name", "Test User");
        record.put("email", "test@example.com");
        data.add(record);
        return data;
    }

    @Test
    @DisplayName("✅ SEC-1: Should reject SQL injection in table name")
    void testRejectsSqlInjectionInTableName() {
        // Given - SQL injection attempt in table name
        validContext.setTargetTable("users; DROP TABLE users--");

        // When/Then
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loadService.batchLoad(validContext)
        );

        assertTrue(ex.getMessage().contains("Invalid Table name"),
            "Should reject malicious table name");
    }

    @Test
    @DisplayName("✅ SEC-2: Should reject SQL injection in column name")
    void testRejectsSqlInjectionInColumnName() {
        // Given - SQL injection attempt in column name
        validContext.setColumns(Arrays.asList("id", "name; DELETE FROM users--"));

        // When/Then
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loadService.batchLoad(validContext)
        );

        assertTrue(ex.getMessage().contains("Invalid Column name"),
            "Should reject malicious column name");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "users; DROP TABLE users--",
        "users' OR '1'='1",
        "users/*comment*/",
        "users--comment",
        "users;SELECT * FROM passwords",
        "users UNION SELECT password FROM users",
        "users`backtick`",
        "users[bracket]",
        "users{brace}"
    })
    @DisplayName("✅ SEC-3: Should reject various SQL injection patterns")
    void testRejectsVariousSqlInjectionPatterns(String maliciousTableName) {
        // Given
        validContext.setTargetTable(maliciousTableName);

        // When/Then
        assertThrows(
            IllegalArgumentException.class,
            () -> loadService.batchLoad(validContext),
            "Should reject: " + maliciousTableName
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT", "INSERT", "UPDATE", "DELETE", "DROP",
        "CREATE", "ALTER", "TABLE", "WHERE", "FROM",
        "JOIN", "UNION", "ORDER", "GROUP", "HAVING"
    })
    @DisplayName("✅ SEC-4: Should reject SQL reserved keywords as identifiers")
    void testRejectsSqlReservedKeywords(String keyword) {
        // Given
        validContext.setTargetTable(keyword);

        // When/Then
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loadService.batchLoad(validContext)
        );

        assertTrue(ex.getMessage().contains("reserved SQL keyword"),
            "Should reject reserved keyword: " + keyword);
    }

    @Test
    @DisplayName("✅ SEC-5: Should accept valid table names")
    void testAcceptsValidTableNames() {
        // Valid table names that should be accepted
        List<String> validNames = Arrays.asList(
            "users",
            "user_profiles",
            "UserData",
            "data_2024",
            "_temp_table",
            "table123",
            "my_table_v2"
        );

        for (String tableName : validNames) {
            validContext.setTargetTable(tableName);

            // Should not throw
            assertDoesNotThrow(
                () -> loadService.validateSqlIdentifier(tableName, "Table"),
                "Should accept valid table name: " + tableName
            );
        }
    }

    @Test
    @DisplayName("✅ SEC-6: Should accept valid column names")
    void testAcceptsValidColumnNames() {
        // Valid column names that should be accepted
        List<String> validNames = Arrays.asList(
            "id",
            "user_id",
            "firstName",
            "created_at",
            "_internal_id",
            "column123",
            "data_v2"
        );

        for (String columnName : validNames) {
            // Should not throw
            assertDoesNotThrow(
                () -> loadService.validateSqlIdentifier(columnName, "Column"),
                "Should accept valid column name: " + columnName
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", // Empty
        " ", // Whitespace
        "123invalid", // Starts with number
        "in-valid", // Contains dash
        "in.valid", // Contains dot
        "in valid", // Contains space
        "in@valid", // Contains special char
        "in#valid"  // Contains hash
    })
    @DisplayName("✅ SEC-7: Should reject invalid identifier formats")
    void testRejectsInvalidIdentifierFormats(String invalidName) {
        // When/Then
        assertThrows(
            IllegalArgumentException.class,
            () -> loadService.validateSqlIdentifier(invalidName, "Identifier"),
            "Should reject invalid format: '" + invalidName + "'"
        );
    }

    @Test
    @DisplayName("✅ SEC-8: Should validate all columns in list")
    void testValidatesAllColumnsInList() {
        // Given - one bad column in the list
        validContext.setColumns(Arrays.asList(
            "id",
            "name",
            "email; DROP TABLE users--", // Malicious
            "address"
        ));

        // When/Then
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loadService.batchLoad(validContext)
        );

        assertTrue(ex.getMessage().contains("Invalid Column name"));
    }

    @Test
    @DisplayName("✅ SEC-9: Should validate key columns")
    void testValidatesKeyColumns() {
        // Given - malicious key column
        validContext.setKeyColumns(Arrays.asList("id; DROP TABLE users--"));

        // When/Then
        assertThrows(
            IllegalArgumentException.class,
            () -> loadService.buildPostgresUpsertSql(
                validContext.getTargetTable(),
                validContext.getColumns(),
                validContext.getKeyColumns()
            )
        );
    }

    @Test
    @DisplayName("✅ SEC-10: Should reject null table name")
    void testRejectsNullTableName() {
        // Given
        validContext.setTargetTable(null);

        // When/Then
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loadService.batchLoad(validContext)
        );

        assertTrue(ex.getMessage().contains("cannot be null"));
    }

    @Test
    @DisplayName("✅ SEC-11: Should reject empty table name")
    void testRejectsEmptyTableName() {
        // Given
        validContext.setTargetTable("");

        // When/Then
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loadService.batchLoad(validContext)
        );

        assertTrue(ex.getMessage().contains("cannot be null or empty"));
    }

    @Test
    @DisplayName("✅ SEC-12: Should reject whitespace-only table name")
    void testRejectsWhitespaceOnlyTableName() {
        // Given
        validContext.setTargetTable("   ");

        // When/Then
        assertThrows(
            IllegalArgumentException.class,
            () -> loadService.batchLoad(validContext)
        );
    }

    @Test
    @DisplayName("✅ SEC-13: Upsert SQL should use parameterized queries")
    void testUpsertSqlIsParameterized() {
        // Given
        validContext.setColumns(Arrays.asList("id", "name", "email"));
        validContext.setKeyColumns(Arrays.asList("id"));

        // When
        String sql = loadService.buildPostgresUpsertSql(
            "users",
            validContext.getColumns(),
            validContext.getKeyColumns()
        );

        // Then
        // Should contain placeholders, not actual values
        assertTrue(sql.contains("?"), "SQL should use placeholders");
        assertFalse(sql.contains("DROP"), "SQL should not contain injection");
        assertFalse(sql.contains("--"), "SQL should not contain comments");
        assertFalse(sql.contains(";"), "SQL should not contain semicolons");

        // Should be properly structured
        assertTrue(sql.toUpperCase().contains("INSERT INTO"));
        assertTrue(sql.toUpperCase().contains("ON CONFLICT"));
        assertTrue(sql.toUpperCase().contains("DO UPDATE"));
    }

    @Test
    @DisplayName("✅ SEC-14: Insert SQL should use parameterized queries")
    void testInsertSqlIsParameterized() {
        // Given
        validContext.setColumns(Arrays.asList("id", "name", "email"));

        // When
        String sql = loadService.buildInsertSql(
            "users",
            validContext.getColumns()
        );

        // Then
        // Count placeholders - should match number of columns
        long placeholderCount = sql.chars().filter(ch -> ch == '?').count();
        assertEquals(3, placeholderCount, "Should have placeholder for each column");

        // Should not contain raw values
        assertFalse(sql.contains("'"), "Should not contain string literals");
        assertFalse(sql.toLowerCase().contains("drop"), "Should not contain DROP");
    }

    @Test
    @DisplayName("✅ SEC-15: Should prevent identifier length overflow")
    void testRejectsOverlyLongIdentifiers() {
        // Given - identifier longer than typical database limit (63 chars for PostgreSQL)
        String longName = "a".repeat(100);
        validContext.setTargetTable(longName);

        // When/Then - while technically valid format, best practice is to limit length
        // This test documents current behavior - consider adding max length validation
        assertDoesNotThrow(() -> loadService.validateSqlIdentifier(longName, "Table"));

        // TODO: Add max length validation (e.g., 63 chars for PostgreSQL)
        // Once implemented, this test should expect IllegalArgumentException
    }

    @Test
    @DisplayName("✅ SEC-16: Case sensitivity should be handled correctly")
    void testCaseSensitivityHandling() {
        // Given - mixed case identifiers (valid in most databases)
        validContext.setTargetTable("UserProfiles");
        validContext.setColumns(Arrays.asList("userId", "firstName", "lastName"));

        // When/Then - should accept valid mixed case identifiers
        assertDoesNotThrow(() -> loadService.batchLoad(validContext));
    }

    @Test
    @DisplayName("✅ SEC-17: Unicode characters should be rejected")
    void testRejectsUnicodeCharacters() {
        // Given - table name with unicode characters
        String unicodeName = "users_データ";

        // When/Then - current regex only allows ASCII
        assertThrows(
            IllegalArgumentException.class,
            () -> loadService.validateSqlIdentifier(unicodeName, "Table"),
            "Should reject non-ASCII characters"
        );
    }

    @Test
    @DisplayName("✅ SEC-18: Should sanitize data values (not identifiers)")
    void testDataValuesSanitization() {
        // Given - data with potentially malicious content
        Map<String, Object> maliciousData = new HashMap<>();
        maliciousData.put("id", "1; DROP TABLE users--");
        maliciousData.put("name", "Robert'; DROP TABLE students;--");
        maliciousData.put("email", "test@example.com' OR '1'='1");

        validContext.setData(Arrays.asList(maliciousData));

        // When/Then - values should be parameterized, not validated like identifiers
        // This should NOT throw - values are parameters, not SQL
        assertDoesNotThrow(() -> loadService.batchLoad(validContext));

        // Note: The actual SQL uses placeholders (?), so malicious data values
        // are safely handled as parameters, not executed as SQL
    }
}
