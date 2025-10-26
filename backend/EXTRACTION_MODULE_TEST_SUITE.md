# Extraction Module - Comprehensive Test Suite

**Purpose:** Ensure all critical fixes are validated with automated tests
**Coverage Target:** >80% for extraction module
**Test Frameworks:** JUnit 5, Mockito, Testcontainers, k6

---

## 📋 TEST INVENTORY

| Test Type | Count | Status | Priority |
|-----------|-------|--------|----------|
| Unit Tests | 25 | ⚠️ To Create | P0 |
| Integration Tests | 8 | ⚠️ To Create | P0 |
| Contract Tests | 1 | ❌ Broken | P0 |
| Load Tests | 3 | ✅ Partial | P1 |
| Security Tests | 5 | ⚠️ To Create | P0 |

---

## 🧪 UNIT TESTS

### 1. SQL Injection Validation Tests

**File:** `backend/src/test/java/com/jivs/platform/security/SqlInjectionValidatorTest.java`

```java
package com.jivs.platform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SQL injection validator
 * CRITICAL: These tests verify the security boundary
 */
class SqlInjectionValidatorTest {

    private SqlInjectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SqlInjectionValidator();
    }

    // CRITICAL: Block SQL injection attacks
    @ParameterizedTest(name = "Should block injection: {0}")
    @ValueSource(strings = {
        "SELECT * FROM users; DROP TABLE users;--",
        "SELECT * FROM users WHERE id = 1 OR 1=1",
        "SELECT * FROM users UNION SELECT * FROM passwords",
        "SELECT * FROM users WHERE name = 'admin'--",
        "SELECT * FROM users; DELETE FROM users WHERE 1=1;",
        "SELECT * FROM users; EXEC sp_executesql N'DROP TABLE users'",
        "SELECT * FROM users WHERE id = 1; TRUNCATE TABLE users;",
        "SELECT * FROM users/*comment*/WHERE name='admin'",
        "SELECT * FROM users WHERE SLEEP(5)",
        "SELECT * FROM users WHERE BENCHMARK(1000000, MD5('test'))",
        "SELECT * FROM users WHERE pg_sleep(5)"
    })
    void shouldBlockSqlInjectionAttempts(String maliciousQuery) {
        assertFalse(validator.isQuerySafe(maliciousQuery),
            "Should block SQL injection: " + maliciousQuery);
    }

    // IMPORTANT: Allow legitimate queries
    @ParameterizedTest(name = "Should allow safe query: {0}")
    @ValueSource(strings = {
        "SELECT id, name, email FROM users",
        "SELECT * FROM users WHERE created_at > '2024-01-01'",
        "SELECT u.id, u.name, o.order_id FROM users u JOIN orders o ON u.id = o.user_id",
        "SELECT COUNT(*) FROM transactions WHERE amount > 1000",
        "SELECT DISTINCT department FROM employees ORDER BY department ASC",
        "SELECT * FROM products WHERE category IN ('electronics', 'furniture')"
    })
    void shouldAllowSafeQueries(String safeQuery) {
        assertTrue(validator.isQuerySafe(safeQuery),
            "Should allow safe query: " + safeQuery);
    }

    @Test
    void shouldRejectNonSelectQueries() {
        assertFalse(validator.isQuerySafe("UPDATE users SET role = 'admin'"));
        assertFalse(validator.isQuerySafe("DELETE FROM users"));
        assertFalse(validator.isQuerySafe("INSERT INTO users VALUES (...)"));
        assertFalse(validator.isQuerySafe("DROP TABLE users"));
    }

    @Test
    void shouldRejectNullOrEmptyQuery() {
        assertFalse(validator.isQuerySafe(null));
        assertFalse(validator.isQuerySafe(""));
        assertFalse(validator.isQuerySafe("   "));
    }

    @Test
    void shouldSanitizeIdentifiers() {
        assertEquals("valid_column", validator.sanitizeIdentifier("valid_column"));
        assertEquals("user_id", validator.sanitizeIdentifier("user_id"));

        // Invalid characters
        assertThrows(IllegalArgumentException.class, () ->
            validator.sanitizeIdentifier("users; DROP TABLE users;"));

        assertThrows(IllegalArgumentException.class, () ->
            validator.sanitizeIdentifier("user.id"));

        assertThrows(IllegalArgumentException.class, () ->
            validator.sanitizeIdentifier("users--comment"));
    }

    @Test
    void shouldValidateOrderByClause() {
        assertTrue(validator.isOrderBySafe("created_at DESC"));
        assertTrue(validator.isOrderBySafe("name ASC, id DESC"));

        assertFalse(validator.isOrderBySafe("created_at; DROP TABLE users"));
        assertFalse(validator.isOrderBySafe("(SELECT 1)"));
    }
}
```

### 2. Password Encryption Tests

**File:** `backend/src/test/java/com/jivs/platform/service/extraction/ExtractionConfigServiceTest.java`

```java
package com.jivs.platform.service.extraction;

import com.jivs.platform.common.util.CryptoUtil;
import com.jivs.platform.domain.extraction.DataSource;
import com.jivs.platform.domain.extraction.ExtractionConfig;
import com.jivs.platform.repository.DataSourceRepository;
import com.jivs.platform.repository.ExtractionConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractionConfigServiceTest {

    @Mock
    private ExtractionConfigRepository extractionConfigRepository;

    @Mock
    private DataSourceRepository dataSourceRepository;

    @Mock
    private ExtractionService extractionService;

    @Mock
    private CryptoUtil cryptoUtil;

    private ExtractionConfigService service;

    @BeforeEach
    void setUp() {
        service = new ExtractionConfigService(
            extractionConfigRepository,
            dataSourceRepository,
            extractionService,
            cryptoUtil
        );
    }

    @Test
    void shouldEncryptPasswordWhenCreatingExtractionConfig() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Test Extraction");
        request.put("sourceType", "POSTGRESQL");
        request.put("extractionQuery", "SELECT * FROM users");

        Map<String, String> connectionConfig = new HashMap<>();
        connectionConfig.put("url", "jdbc:postgresql://localhost/test");
        connectionConfig.put("username", "testuser");
        connectionConfig.put("password", "PlaintextPassword123");
        request.put("connectionConfig", connectionConfig);

        // Mock encryption
        when(cryptoUtil.encrypt("PlaintextPassword123"))
            .thenReturn("encrypted:AES256:base64encryptedvalue==");

        when(dataSourceRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        when(dataSourceRepository.save(any(DataSource.class)))
            .thenAnswer(invocation -> {
                DataSource ds = invocation.getArgument(0);
                ds.setId(1L);
                return ds;
            });

        when(extractionConfigRepository.findByName(any())).thenReturn(Optional.empty());
        when(extractionConfigRepository.save(any(ExtractionConfig.class)))
            .thenAnswer(invocation -> {
                ExtractionConfig ec = invocation.getArgument(0);
                ec.setId(1L);
                return ec;
            });

        // Act
        ExtractionConfig result = service.createExtractionConfig(request, "admin");

        // Assert - Verify encryption was called
        verify(cryptoUtil, times(1)).encrypt("PlaintextPassword123");

        // Capture saved DataSource
        ArgumentCaptor<DataSource> dataSourceCaptor = ArgumentCaptor.forClass(DataSource.class);
        verify(dataSourceRepository).save(dataSourceCaptor.capture());
        DataSource savedDataSource = dataSourceCaptor.getValue();

        // Verify password is encrypted
        assertNotNull(savedDataSource.getPasswordEncrypted());
        assertEquals("encrypted:AES256:base64encryptedvalue==",
            savedDataSource.getPasswordEncrypted());
        assertNotEquals("PlaintextPassword123",
            savedDataSource.getPasswordEncrypted());
    }

    @Test
    void shouldHandleEmptyPassword() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Test Extraction");
        request.put("sourceType", "POSTGRESQL");

        Map<String, String> connectionConfig = new HashMap<>();
        connectionConfig.put("url", "jdbc:postgresql://localhost/test");
        connectionConfig.put("username", "testuser");
        connectionConfig.put("password", ""); // Empty password
        request.put("connectionConfig", connectionConfig);

        when(dataSourceRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        when(dataSourceRepository.save(any(DataSource.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(extractionConfigRepository.findByName(any())).thenReturn(Optional.empty());
        when(extractionConfigRepository.save(any(ExtractionConfig.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ExtractionConfig result = service.createExtractionConfig(request, "admin");

        // Assert - Encryption should NOT be called for empty password
        verify(cryptoUtil, never()).encrypt(any());
    }

    @Test
    void shouldPropagateEncryptionErrors() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Test Extraction");
        request.put("sourceType", "POSTGRESQL");

        Map<String, String> connectionConfig = new HashMap<>();
        connectionConfig.put("url", "jdbc:postgresql://localhost/test");
        connectionConfig.put("password", "password123");
        request.put("connectionConfig", connectionConfig);

        // Mock encryption failure
        when(cryptoUtil.encrypt(any())).thenThrow(new RuntimeException("Encryption failed"));
        when(dataSourceRepository.findAll()).thenReturn(java.util.Collections.emptyList());

        // Act & Assert
        assertThrows(Exception.class, () ->
            service.createExtractionConfig(request, "admin"));
    }
}
```

### 3. Thread-Safety Tests

**File:** `backend/src/test/java/com/jivs/platform/service/extraction/ExtractionResultTest.java`

```java
package com.jivs.platform.service.extraction;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for thread-safe ExtractionResult
 * CRITICAL: Verifies concurrent access doesn't corrupt data
 */
class ExtractionResultTest {

    @Test
    void shouldHandleConcurrentRecordIncrement() throws InterruptedException {
        // Arrange
        ExtractionResult result = new ExtractionResult();
        int threadCount = 20;
        int incrementsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act - Concurrent increments
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

        // Wait for completion
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Test should complete within 30 seconds");
        executor.shutdown();

        // Assert - No lost updates
        long expected = (long) threadCount * incrementsPerThread;
        assertEquals(expected, result.getRecordsExtracted(),
            "All increments should be counted");
    }

    @Test
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
        assertEquals(threadCount * errorsPerThread, result.getErrors().size());

        // Assert - No ConcurrentModificationException when iterating
        assertDoesNotThrow(() -> {
            for (String error : result.getErrors()) {
                assertNotNull(error);
            }
        });
    }

    @Test
    void shouldCalculateSuccessRateCorrectly() {
        ExtractionResult result = new ExtractionResult();
        result.setRecordsExtracted(80L);
        result.setRecordsFailed(20L);

        assertEquals(80.0, result.getSuccessRate(), 0.01);

        // Edge case: No records
        ExtractionResult emptyResult = new ExtractionResult();
        assertEquals(0.0, emptyResult.getSuccessRate(), 0.01);
    }

    @Test
    void shouldMergeResultsCorrectly() {
        ExtractionResult result1 = new ExtractionResult();
        result1.setRecordsExtracted(100L);
        result1.setBytesProcessed(1000L);
        result1.addError("Error 1");

        ExtractionResult result2 = new ExtractionResult();
        result2.setRecordsExtracted(200L);
        result2.setBytesProcessed(2000L);
        result2.addError("Error 2");
        result2.addError("Error 3");

        // Merge result2 into result1
        result1.merge(result2);

        assertEquals(300L, result1.getRecordsExtracted());
        assertEquals(3000L, result1.getBytesProcessed());
        assertEquals(3, result1.getErrors().size());
    }
}
```

### 4. Batch Processing Tests

**File:** `backend/src/test/java/com/jivs/platform/service/extraction/batch/ParquetBatchWriterTest.java`

```java
package com.jivs.platform.service.extraction.batch;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ParquetBatchWriterTest {

    @TempDir
    File tempDir;

    private BatchWriter writer;

    @AfterEach
    void tearDown() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    @Test
    void shouldWriteParquetFileSuccessfully() throws IOException {
        // Arrange
        String outputPath = new File(tempDir, "test_extraction").getAbsolutePath();

        Map<String, Object> sampleRecord = Map.of(
            "id", 1L,
            "name", "John Doe",
            "email", "john@example.com",
            "age", 30
        );

        List<Map<String, Object>> batch = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            batch.add(Map.of(
                "id", (long) i,
                "name", "User " + i,
                "email", "user" + i + "@example.com",
                "age", 20 + (i % 50)
            ));
        }

        // Act
        writer = new ParquetBatchWriter(outputPath, sampleRecord);
        writer.writeBatch(batch, 0);
        writer.flush();

        // Assert
        assertEquals(100, writer.getRecordsWritten());
        assertTrue(writer.getBytesWritten() > 0);

        File parquetFile = new File(outputPath + ".parquet");
        assertTrue(parquetFile.exists());
        assertTrue(parquetFile.length() > 0);

        // Verify file is readable
        verifyParquetFileReadable(parquetFile, 100);
    }

    @Test
    void shouldHandleMultipleBatches() throws IOException {
        String outputPath = new File(tempDir, "multi_batch").getAbsolutePath();

        Map<String, Object> sampleRecord = Map.of("id", 1L, "value", "test");

        writer = new ParquetBatchWriter(outputPath, sampleRecord);

        // Write 5 batches
        for (int batchNum = 0; batchNum < 5; batchNum++) {
            List<Map<String, Object>> batch = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                batch.add(Map.of("id", (long) (batchNum * 20 + i), "value", "value_" + i));
            }
            writer.writeBatch(batch, batchNum);
        }

        writer.flush();

        // Assert
        assertEquals(100, writer.getRecordsWritten());
        File parquetFile = new File(outputPath + ".parquet");
        verifyParquetFileReadable(parquetFile, 100);
    }

    @Test
    void shouldCompressData() throws IOException {
        String outputPath = new File(tempDir, "compression_test").getAbsolutePath();

        // Create large batch with repetitive data (compresses well)
        Map<String, Object> sampleRecord = Map.of(
            "id", 1L,
            "category", "electronics",
            "description", "This is a test product description that repeats"
        );

        List<Map<String, Object>> batch = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            batch.add(Map.of(
                "id", (long) i,
                "category", "electronics",
                "description", "This is a test product description that repeats"
            ));
        }

        writer = new ParquetBatchWriter(outputPath, sampleRecord);
        writer.writeBatch(batch, 0);
        writer.flush();

        File parquetFile = new File(outputPath + ".parquet");

        // Uncompressed size estimate
        long uncompressedEstimate = 1000L * 100; // ~100 bytes per record

        // Parquet file should be smaller due to compression
        assertTrue(parquetFile.length() < uncompressedEstimate,
            "Parquet file should be compressed");
    }

    private void verifyParquetFileReadable(File parquetFile, int expectedRecords)
            throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());

        try (ParquetReader reader = AvroParquetReader.builder(new Path(parquetFile.getAbsolutePath()))
                .withConf(conf)
                .build()) {

            int count = 0;
            Object record;
            while ((record = reader.read()) != null) {
                count++;
            }

            assertEquals(expectedRecords, count, "Should read expected number of records");
        }
    }
}
```

---

## 🔗 INTEGRATION TESTS

### 5. End-to-End Extraction Test

**File:** `backend/src/test/java/com/jivs/platform/integration/ExtractionIntegrationTest.java`

```java
package com.jivs.platform.integration;

import com.jivs.platform.domain.extraction.ExtractionConfig;
import com.jivs.platform.domain.extraction.ExtractionJob;
import com.jivs.platform.repository.ExtractionConfigRepository;
import com.jivs.platform.repository.ExtractionJobRepository;
import com.jivs.platform.service.extraction.ExtractionConfigService;
import com.jivs.platform.service.extraction.ExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for extraction module
 * Uses Testcontainers for real PostgreSQL database
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ExtractionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test_db")
        .withUsername("test_user")
        .withPassword("test_pass")
        .withInitScript("test-data.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ExtractionConfigService extractionConfigService;

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private ExtractionConfigRepository extractionConfigRepository;

    @Autowired
    private ExtractionJobRepository extractionJobRepository;

    @Test
    void shouldPerformCompleteExtractionWorkflow() {
        // 1. Create extraction config
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Integration Test Extraction");
        request.put("sourceType", "POSTGRESQL");
        request.put("extractionQuery", "SELECT * FROM test_users LIMIT 10");

        Map<String, String> connectionConfig = new HashMap<>();
        connectionConfig.put("url", postgres.getJdbcUrl());
        connectionConfig.put("username", postgres.getUsername());
        connectionConfig.put("password", postgres.getPassword());
        request.put("connectionConfig", connectionConfig);

        ExtractionConfig config = extractionConfigService.createExtractionConfig(request, "test_user");

        assertNotNull(config.getId());
        assertEquals("Integration Test Extraction", config.getName());

        // 2. Start extraction
        extractionConfigService.startExtraction(config.getId(), "test_user");

        // 3. Wait for completion
        await().atMost(30, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> {
                ExtractionJob job = extractionJobRepository
                    .findByExtractionConfigId(config.getId(), org.springframework.data.domain.PageRequest.of(0, 1))
                    .getContent()
                    .stream()
                    .findFirst()
                    .orElse(null);

                return job != null && (
                    job.getStatus() == ExtractionJob.JobStatus.COMPLETED ||
                    job.getStatus() == ExtractionJob.JobStatus.FAILED
                );
            });

        // 4. Verify results
        ExtractionJob job = extractionJobRepository
            .findByExtractionConfigId(config.getId(), org.springframework.data.domain.PageRequest.of(0, 1))
            .getContent()
            .get(0);

        assertEquals(ExtractionJob.JobStatus.COMPLETED, job.getStatus());
        assertEquals(10L, job.getRecordsExtracted());
        assertTrue(job.getBytesProcessed() > 0);

        // 5. Verify output file exists
        assertNotNull(job.getOutputPath());
        File outputFile = new File(job.getOutputPath());
        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }
}
```

---

## 🔒 SECURITY TESTS

### 6. Security Penetration Tests

**File:** `backend/src/test/java/com/jivs/platform/security/ExtractionSecurityTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
class ExtractionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldBlockSqlInjectionInExtractionQuery() throws Exception {
        String maliciousPayload = """
        {
            "name": "Malicious Extraction",
            "sourceType": "POSTGRESQL",
            "extractionQuery": "SELECT * FROM users; DROP TABLE users;--"
        }
        """;

        mockMvc.perform(post("/api/v1/extractions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(containsString("SQL injection")));
    }

    @Test
    void shouldRequireAuthenticationForExtractionCreation() throws Exception {
        mockMvc.perform(post("/api/v1/extractions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void shouldDenyExtractionCreationForViewers() throws Exception {
        mockMvc.perform(post("/api/v1/extractions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }
}
```

---

## ⚡ LOAD TESTS

### 7. k6 Load Test Script

**File:** `backend/src/test/k6/extraction-load-test.js`

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '2m', target: 10 },  // Ramp up
    { duration: '5m', target: 50 },  // Sustained load
    { duration: '2m', target: 100 }, // Peak load
    { duration: '2m', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% requests <500ms
    errors: ['rate<0.05'],            // <5% error rate
  },
};

export default function () {
  const url = 'http://localhost:8080/api/v1/extractions';
  const payload = JSON.stringify({
    name: `Load Test Extraction ${__VU}-${__ITER}`,
    sourceType: 'POSTGRESQL',
    extractionQuery: 'SELECT * FROM users LIMIT 100',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + __ENV.JWT_TOKEN,
    },
  };

  const response = http.post(url, payload, params);

  const success = check(response, {
    'status is 201': (r) => r.status === 201,
    'has extraction id': (r) => r.json('id') !== null,
    'response time OK': (r) => r.timings.duration < 500,
  });

  errorRate.add(!success);

  sleep(1);
}
```

Run with:
```bash
export JWT_TOKEN=<your-test-token>
k6 run backend/src/test/k6/extraction-load-test.js
```

---

## ✅ TEST EXECUTION PLAN

### Phase 1: Unit Tests (Day 1-2)
```bash
# Run all unit tests
mvn test -Dtest=*Extraction*,*SqlInjection*

# Expected results:
# - 25+ tests passing
# - 0 failures
# - Coverage >80%
```

### Phase 2: Integration Tests (Day 3)
```bash
# Start Testcontainers
mvn verify -P integration-tests

# Expected results:
# - All integration tests passing
# - No container startup failures
```

### Phase 3: Security Tests (Day 4)
```bash
# Run security tests
mvn test -Dtest=*SecurityTest

# Run OWASP dependency check
mvn org.owasp:dependency-check-maven:check
```

### Phase 4: Load Tests (Day 5)
```bash
# Start application
mvn spring-boot:run

# Run load test
k6 run backend/src/test/k6/extraction-load-test.js

# Expected results:
# - p95 latency <500ms
# - Error rate <5%
# - No memory leaks
```

---

## 📊 COVERAGE REPORT

Target coverage for extraction module:

| Component | Target | Actual | Status |
|-----------|--------|--------|--------|
| Connectors | >80% | ⚠️ TBD | Pending |
| Services | >80% | ⚠️ TBD | Pending |
| Controllers | >70% | ⚠️ TBD | Pending |
| Security | 100% | ⚠️ TBD | Pending |
| Overall | >80% | ⚠️ TBD | Pending |

Generate coverage report:
```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

---

**Related Documents:**
- `EXTRACTION_MODULE_FIXES.md` - Implementation fixes
- `EXTRACTION_MODULE_DEPLOYMENT_RUNBOOK.md` - Deployment guide
