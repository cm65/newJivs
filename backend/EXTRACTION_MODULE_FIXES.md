# JiVS Extraction Module - Critical Fixes Implementation Guide

**Date:** 2025-10-26
**Module:** Data Extraction
**Status:** 🔴 REQUIRES IMMEDIATE ATTENTION

---

## 🚨 CRITICAL FIX #1: Enable SQL Injection Validation

### Current State
SQL injection validation is **DISABLED** in production code, leaving the entire database vulnerable.

### File: `JdbcConnector.java`

**Step 1: Update Constructor**

```java
// CURRENT (Line 49-55)
public JdbcConnector(String connectionUrl, String username, String password, String dbType) {
    this.connectionUrl = connectionUrl;
    this.username = username;
    this.password = password;
    this.dbType = dbType;
    // this.sqlValidator = null; // Temporarily disabled
}

// FIX: Add SqlInjectionValidator parameter
public JdbcConnector(String connectionUrl, String username, String password,
                     String dbType, SqlInjectionValidator sqlValidator) {
    this.connectionUrl = connectionUrl;
    this.username = username;
    this.password = password;
    this.dbType = dbType;
    this.sqlValidator = sqlValidator;  // ENABLE THIS

    if (sqlValidator == null) {
        throw new IllegalArgumentException("SqlInjectionValidator cannot be null");
    }
}
```

**Step 2: Uncomment Validation Logic**

```java
// CURRENT (Lines 90-98)
// CRITICAL: Validate query for SQL injection (temporarily disabled)
// TODO: Re-enable SQL injection validation when security module is restored
// if (!sqlValidator.isQuerySafe(query)) {
//     String errorMsg = "Query failed security validation. Query may contain SQL injection attempts.";
//     log.error("SQL Injection attempt detected: {}", query);
//     result.getErrors().add(errorMsg);
//     result.setRecordsFailed(1L);
//     throw new SecurityException(errorMsg);
// }

// FIX: Uncomment and enable
if (!sqlValidator.isQuerySafe(query)) {
    String errorMsg = "Query failed security validation. Query may contain SQL injection attempts.";
    log.error("SQL Injection attempt detected in extraction. Query hash: {}",
              Integer.toHexString(query.hashCode()));
    result.getErrors().add(errorMsg);
    result.setRecordsFailed(1L);
    throw new SecurityException(errorMsg);
}
```

**Step 3: Update ConnectorFactory**

```java
// CURRENT (Lines 69-82 in ConnectorFactory.java)
@Deprecated
public DataConnector getLegacyConnector(DataSource dataSource) {
    log.warn("Using legacy non-pooled connector for data source: {}", dataSource.getName());

    String decryptedPassword = null;
    if (dataSource.getPasswordEncrypted() != null) {
        decryptedPassword = cryptoUtil.decrypt(dataSource.getPasswordEncrypted());
    }

    return new JdbcConnector(
            dataSource.getConnectionUrl(),
            dataSource.getUsername(),
            decryptedPassword,
            dataSource.getSourceType().name()
    );
}

// FIX: Add sqlInjectionValidator parameter
@Deprecated
public DataConnector getLegacyConnector(DataSource dataSource) {
    log.warn("Using legacy non-pooled connector for data source: {}", dataSource.getName());

    String decryptedPassword = null;
    if (dataSource.getPasswordEncrypted() != null) {
        decryptedPassword = cryptoUtil.decrypt(dataSource.getPasswordEncrypted());
    }

    return new JdbcConnector(
            dataSource.getConnectionUrl(),
            dataSource.getUsername(),
            decryptedPassword,
            dataSource.getSourceType().name(),
            sqlInjectionValidator  // ADD THIS
    );
}
```

### Verification Test

```java
@Test
void testSqlInjectionPrevention() {
    // Arrange
    SqlInjectionValidator validator = new SqlInjectionValidator();
    JdbcConnector connector = new JdbcConnector(
        "jdbc:postgresql://localhost/test",
        "user",
        "pass",
        "POSTGRESQL",
        validator
    );

    // Act & Assert - DROP TABLE attack
    Map<String, String> params = Map.of(
        "query", "SELECT * FROM users; DROP TABLE users;--"
    );
    assertThrows(SecurityException.class, () -> connector.extract(params));

    // Act & Assert - UNION injection
    params = Map.of(
        "query", "SELECT * FROM users UNION SELECT password FROM admin_users"
    );
    assertThrows(SecurityException.class, () -> connector.extract(params));

    // Act & Assert - Valid query should work
    params = Map.of(
        "query", "SELECT id, name, email FROM users WHERE created_at > '2024-01-01'"
    );
    assertDoesNotThrow(() -> connector.extract(params));
}
```

**Effort:** 2 hours
**Risk:** Low (SqlInjectionValidator already exists and is tested)
**Impact:** CRITICAL - Prevents SQL injection attacks

---

## 🚨 CRITICAL FIX #2: Encrypt Passwords Before Storage

### Current State
Passwords are stored in plaintext despite field being named `passwordEncrypted`.

### File: `ExtractionConfigService.java`

**Step 1: Inject CryptoUtil**

```java
// CURRENT (Lines 21-28)
@Service
@RequiredArgsConstructor
public class ExtractionConfigService {
    private static final org.slf4j.Logger log = ...;

    private final ExtractionConfigRepository extractionConfigRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ExtractionService extractionService;
}

// FIX: Add CryptoUtil
@Service
@RequiredArgsConstructor
public class ExtractionConfigService {
    private static final org.slf4j.Logger log = ...;

    private final ExtractionConfigRepository extractionConfigRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ExtractionService extractionService;
    private final CryptoUtil cryptoUtil;  // ADD THIS
}
```

**Step 2: Encrypt Password in findOrCreateDataSource**

```java
// CURRENT (Lines 160-164)
if (connectionConfig != null) {
    dataSource.setUsername(connectionConfig.get("username"));
    dataSource.setPasswordEncrypted(connectionConfig.get("password")); // TODO: Encrypt properly
}

// FIX: Encrypt password
if (connectionConfig != null) {
    dataSource.setUsername(connectionConfig.get("username"));

    // Encrypt password before storing
    String plainPassword = connectionConfig.get("password");
    if (plainPassword != null && !plainPassword.trim().isEmpty()) {
        try {
            String encryptedPassword = cryptoUtil.encrypt(plainPassword);
            dataSource.setPasswordEncrypted(encryptedPassword);
            log.debug("Password encrypted for data source");
        } catch (Exception e) {
            log.error("Failed to encrypt password", e);
            throw new BusinessException("Failed to secure credentials");
        }
    } else {
        log.warn("No password provided for data source");
    }
}
```

**Step 3: Add Validation**

```java
// Add before setting password
private void validatePassword(String password) {
    if (password == null || password.trim().isEmpty()) {
        throw new BusinessException("Password cannot be empty");
    }

    if (password.length() < 8) {
        throw new BusinessException("Password must be at least 8 characters");
    }

    if (password.length() > 256) {
        throw new BusinessException("Password too long (max 256 characters)");
    }
}
```

### Database Migration Script

Create: `backend/src/main/resources/db/migration/V111__Encrypt_existing_passwords.sql`

```sql
-- Migration to encrypt existing plaintext passwords
-- WARNING: This requires manual intervention for production

-- Step 1: Add temporary column for plaintext
ALTER TABLE data_sources ADD COLUMN password_plaintext_temp VARCHAR(500);

-- Step 2: Copy current passwords to temp column
UPDATE data_sources
SET password_plaintext_temp = password_encrypted
WHERE password_encrypted IS NOT NULL;

-- Step 3: Mark table for manual encryption
-- APPLICATION MUST encrypt these passwords on startup
-- See: DataSourcePasswordMigration.java

-- Step 4: Add migration status column
ALTER TABLE data_sources ADD COLUMN password_migration_status VARCHAR(20) DEFAULT 'PENDING';

UPDATE data_sources
SET password_migration_status = 'PENDING'
WHERE password_plaintext_temp IS NOT NULL;

-- Step 5: Create audit log
CREATE TABLE IF NOT EXISTS password_migration_audit (
    id BIGSERIAL PRIMARY KEY,
    data_source_id BIGINT NOT NULL,
    migration_status VARCHAR(20),
    migrated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT
);
```

**Step 4: Create Migration Runner**

Create: `backend/src/main/java/com/jivs/platform/migration/DataSourcePasswordMigration.java`

```java
package com.jivs.platform.migration;

import com.jivs.platform.common.util.CryptoUtil;
import com.jivs.platform.repository.DataSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time migration to encrypt existing plaintext passwords
 * Runs on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSourcePasswordMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final CryptoUtil cryptoUtil;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting DataSource password migration...");

        String checkSql = "SELECT COUNT(*) FROM data_sources WHERE password_migration_status = 'PENDING'";
        Integer pendingCount = jdbcTemplate.queryForObject(checkSql, Integer.class);

        if (pendingCount == null || pendingCount == 0) {
            log.info("No passwords to migrate");
            return;
        }

        log.warn("Found {} data sources with plaintext passwords - encrypting...", pendingCount);

        String selectSql = "SELECT id, password_plaintext_temp FROM data_sources " +
                          "WHERE password_migration_status = 'PENDING'";

        jdbcTemplate.query(selectSql, rs -> {
            Long id = rs.getLong("id");
            String plaintextPassword = rs.getString("password_plaintext_temp");

            try {
                String encrypted = cryptoUtil.encrypt(plaintextPassword);

                String updateSql = "UPDATE data_sources SET " +
                                  "password_encrypted = ?, " +
                                  "password_plaintext_temp = NULL, " +
                                  "password_migration_status = 'COMPLETED' " +
                                  "WHERE id = ?";

                jdbcTemplate.update(updateSql, encrypted, id);

                // Audit log
                String auditSql = "INSERT INTO password_migration_audit " +
                                 "(data_source_id, migration_status) VALUES (?, 'SUCCESS')";
                jdbcTemplate.update(auditSql, id);

                log.debug("Encrypted password for data source ID: {}", id);

            } catch (Exception e) {
                log.error("Failed to encrypt password for data source ID: {}", id, e);

                String updateSql = "UPDATE data_sources SET " +
                                  "password_migration_status = 'FAILED' " +
                                  "WHERE id = ?";
                jdbcTemplate.update(updateSql, id);

                String auditSql = "INSERT INTO password_migration_audit " +
                                 "(data_source_id, migration_status, error_message) VALUES (?, 'FAILED', ?)";
                jdbcTemplate.update(auditSql, id, e.getMessage());
            }
        });

        log.info("DataSource password migration completed");

        // Verify no pending migrations
        pendingCount = jdbcTemplate.queryForObject(checkSql, Integer.class);
        if (pendingCount != null && pendingCount > 0) {
            log.error("WARNING: {} data sources still have pending password migration", pendingCount);
        }
    }
}
```

### Verification Test

```java
@Test
void testPasswordEncryption() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("name", "Test Source");
    request.put("sourceType", "POSTGRESQL");

    Map<String, String> connectionConfig = new HashMap<>();
    connectionConfig.put("url", "jdbc:postgresql://localhost/test");
    connectionConfig.put("username", "testuser");
    connectionConfig.put("password", "PlaintextPassword123");
    request.put("connectionConfig", connectionConfig);

    // Act
    ExtractionConfig config = extractionConfigService.createExtractionConfig(request, "admin");

    // Assert
    DataSource dataSource = config.getDataSource();
    assertNotNull(dataSource.getPasswordEncrypted());
    assertNotEquals("PlaintextPassword123", dataSource.getPasswordEncrypted());
    assertTrue(dataSource.getPasswordEncrypted().length() > 50, "Encrypted password should be longer");

    // Verify decryption works
    String decrypted = cryptoUtil.decrypt(dataSource.getPasswordEncrypted());
    assertEquals("PlaintextPassword123", decrypted);
}
```

**Effort:** 4 hours (includes migration)
**Risk:** Medium (requires database migration)
**Impact:** CRITICAL - Protects credentials

---

## 🚨 CRITICAL FIX #3: Thread-Safe ExtractionResult

### Current State
ExtractionResult is modified by multiple threads without synchronization.

### File: Create new `ExtractionResult.java`

```java
package com.jivs.platform.service.extraction;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe result of data extraction
 *
 * FIXED: Uses AtomicLong and CopyOnWriteArrayList for concurrent access
 * Used in multi-threaded extraction with parallel batch processing
 */
public class ExtractionResult {

    // Thread-safe atomic counters
    private final AtomicLong recordsExtracted = new AtomicLong(0);
    private final AtomicLong recordsFailed = new AtomicLong(0);
    private final AtomicLong bytesProcessed = new AtomicLong(0);

    // Thread-safe error collection
    private final List<String> errors = new CopyOnWriteArrayList<>();

    // Single-thread fields (set once, read many)
    @Getter
    private volatile String outputPath;

    @Getter
    private volatile String metadata;

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
        recordsExtracted.set(count);
    }

    public void setRecordsFailed(Long count) {
        recordsFailed.set(count);
    }

    public void setBytesProcessed(Long bytes) {
        bytesProcessed.set(bytes);
    }

    public void setOutputPath(String path) {
        this.outputPath = path;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    // Thread-safe increment operations
    public void addRecordsExtracted(long count) {
        recordsExtracted.addAndGet(count);
    }

    public void addRecordsFailed(long count) {
        recordsFailed.addAndGet(count);
    }

    public void addBytesProcessed(long bytes) {
        bytesProcessed.addAndGet(bytes);
    }

    public void addError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            errors.add(error);
        }
    }

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
            "ExtractionResult{extracted=%d, failed=%d, bytes=%d, errors=%d, successRate=%.2f%%}",
            getRecordsExtracted(),
            getRecordsFailed(),
            getBytesProcessed(),
            errors.size(),
            getSuccessRate()
        );
    }
}
```

### Update Usage in JdbcConnector and PooledJdbcConnector

```java
// OLD USAGE (unsafe)
result.setRecordsExtracted(recordCount.get());
result.getErrors().add(e.getMessage());

// NEW USAGE (thread-safe)
result.setRecordsExtracted(recordCount.get());
result.addError(e.getMessage());
```

### Verification Test

```java
@Test
void testConcurrentModification() throws InterruptedException {
    ExtractionResult result = new ExtractionResult();
    int threadCount = 20;
    int iterationsPerThread = 100;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        executor.submit(() -> {
            try {
                for (int i = 0; i < iterationsPerThread; i++) {
                    result.addRecordsExtracted(1);
                    result.addBytesProcessed(100);
                    result.addError("Error from thread " + threadId + " iteration " + i);
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    // Verify counts
    assertEquals(threadCount * iterationsPerThread, result.getRecordsExtracted());
    assertEquals(threadCount * iterationsPerThread * 100, result.getBytesProcessed());
    assertEquals(threadCount * iterationsPerThread, result.getErrors().size());

    // Verify no ConcurrentModificationException occurred
    assertTrue(executor.isTerminated());
}

@Test
void testMergeResults() {
    ExtractionResult result1 = new ExtractionResult();
    result1.setRecordsExtracted(100L);
    result1.addError("Error 1");

    ExtractionResult result2 = new ExtractionResult();
    result2.setRecordsExtracted(200L);
    result2.addError("Error 2");

    result1.merge(result2);

    assertEquals(300L, result1.getRecordsExtracted());
    assertEquals(2, result1.getErrors().size());
}
```

**Effort:** 2 hours
**Risk:** Low (backward compatible API)
**Impact:** HIGH - Prevents data corruption in concurrent extractions

---

## 📋 IMPLEMENTATION PLAN

### Phase 1: Security Fixes (Week 1)
**Total Effort:** 8 hours

| Task | Priority | Effort | Risk | Assigned To |
|------|----------|--------|------|-------------|
| Enable SQL injection validation | P0 | 2h | Low | Backend Dev |
| Implement password encryption | P0 | 4h | Medium | Backend Dev |
| Run password migration | P0 | 2h | Medium | DevOps |

### Phase 2: Correctness Fixes (Week 1)
**Total Effort:** 6 hours

| Task | Priority | Effort | Risk | Assigned To |
|------|----------|--------|------|-------------|
| Thread-safe ExtractionResult | P1 | 2h | Low | Backend Dev |
| Fix resource leaks | P1 | 2h | Low | Backend Dev |
| Fix inefficient query | P1 | 1h | Low | Backend Dev |
| Add connection timeout | P1 | 1h | Low | Backend Dev |

### Phase 3: Functionality Fixes (Week 2)
**Total Effort:** 16 hours

| Task | Priority | Effort | Risk | Assigned To |
|------|----------|--------|------|-------------|
| Implement batch processing (Parquet) | P0 | 6h | Medium | Backend Dev |
| Implement batch processing (CSV) | P1 | 2h | Low | Backend Dev |
| Fix broken test suite | P0 | 4h | Low | QA/Backend Dev |
| Add input validation | P2 | 4h | Low | Backend Dev |

### Phase 4: Technical Debt (Week 3)
**Total Effort:** 12 hours

| Task | Priority | Effort | Risk | Assigned To |
|------|----------|--------|------|-------------|
| Complete schema migration | P2 | 6h | Medium | Backend Dev |
| Handle stub connectors | P2 | 4h | Low | Backend Dev |
| Improve logging security | P2 | 2h | Low | Backend Dev |

**Total Estimated Effort:** 42 hours (approximately 1 week for 1 developer)

---

## ✅ ACCEPTANCE CRITERIA

### Security Fixes
- [ ] All extraction queries validated for SQL injection
- [ ] 0% plaintext passwords in database
- [ ] Password migration script executed successfully
- [ ] Security audit passes

### Correctness Fixes
- [ ] No ConcurrentModificationException in concurrent extractions
- [ ] No connection leaks (verified with load test)
- [ ] Database queries use indexes (no findAll())
- [ ] All executor timeouts configurable

### Functionality Fixes
- [ ] Batch processing writes actual output files
- [ ] Test suite compiles and passes (>95% pass rate)
- [ ] All API inputs validated
- [ ] Stub connectors either implemented or throw UnsupportedOperationException

### Testing
- [ ] Unit test coverage >80%
- [ ] Integration tests pass
- [ ] Load test: 100 concurrent extractions complete successfully
- [ ] Memory leak test: 1000 extractions without memory growth

---

## 🔍 VERIFICATION CHECKLIST

Before marking fixes as complete:

```bash
# 1. Run unit tests
cd backend
mvn test -Dtest=*Extraction*

# 2. Run integration tests
mvn verify -P integration-tests

# 3. Check for SQL injection vulnerability
curl -X POST http://localhost:8080/api/v1/extractions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test",
    "sourceType": "POSTGRESQL",
    "extractionQuery": "SELECT * FROM users; DROP TABLE users;--"
  }'
# Expected: 400 Bad Request with "SQL injection" error

# 4. Verify password encryption
psql -U jivs_user -d jivs -c \
  "SELECT id, name, length(password_encrypted) as pwd_length FROM data_sources;"
# Expected: All passwords >50 characters (encrypted)

# 5. Load test
k6 run backend/src/test/k6/extraction-load-test.js
# Expected: 0 errors, <500ms p95 latency

# 6. Memory leak test
jcmd <PID> GC.heap_dump /tmp/before.hprof
# Run 1000 extractions
jcmd <PID> GC.heap_dump /tmp/after.hprof
# Compare heap dumps - no significant growth
```

---

**Next Steps:** See `EXTRACTION_MODULE_BATCH_PROCESSING_IMPL.md` for batch processing implementation details.
