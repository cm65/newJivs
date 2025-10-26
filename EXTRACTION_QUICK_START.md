# Extraction Module Fixes - Quick Start Guide

**For:** Developers implementing audit fixes
**Time to First Fix:** 30 minutes
**Updated:** 2025-10-26

This guide gets you from audit findings to working code in the shortest time possible.

---

## 🎯 WHAT YOU NEED TO KNOW

**The Problem:**
Extraction module has 13 critical issues preventing production deployment.

**The Solution:**
All fixes are documented with executable code. Follow this guide to implement them in priority order.

**What's Already Done:** ✅
- Thread-safe ExtractionResult implemented
- Database migration script created
- Unit tests written
- JIRA tickets created

**What You Need to Do:** ⚠️
- Enable SQL injection validation (30 min)
- Add password encryption (1 hour)
- Fix broken test (2 hours)
- Implement batch processing (6 hours)

---

## ⚡ QUICK WINS (Do These First)

### Quick Win #1: Fix Thread-Safety ✅ DONE
**Time:** Already completed
**Status:** ✅ Implemented

**What was done:**
- Replaced `ExtractionResult.java` with thread-safe version
- Created comprehensive unit tests
- Backward compatible API

**Verify it works:**
```bash
cd backend
mvn test -Dtest=ExtractionResultTest

# Expected: All tests passing
```

**Files changed:**
- ✅ `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionResult.java`
- ✅ `backend/src/test/java/com/jivs/platform/service/extraction/ExtractionResultTest.java`

---

### Quick Win #2: Enable SQL Injection Validation
**Time:** 30 minutes
**Impact:** CRITICAL security fix

**Step 1: Update JdbcConnector.java**

Open: `backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java`

**Change 1 - Line 49 (Constructor):**
```java
// BEFORE
public JdbcConnector(String connectionUrl, String username, String password, String dbType) {
    this.connectionUrl = connectionUrl;
    this.username = username;
    this.password = password;
    this.dbType = dbType;
    // this.sqlValidator = null; // Temporarily disabled
}

// AFTER
public JdbcConnector(String connectionUrl, String username, String password,
                     String dbType, SqlInjectionValidator sqlValidator) {
    this.connectionUrl = connectionUrl;
    this.username = username;
    this.password = password;
    this.dbType = dbType;
    this.sqlValidator = sqlValidator;

    if (sqlValidator == null) {
        throw new IllegalArgumentException("SqlInjectionValidator cannot be null");
    }
}
```

**Change 2 - Line 46 (Field):**
```java
// BEFORE
// private final SqlInjectionValidator sqlValidator; // Temporarily disabled

// AFTER
private final SqlInjectionValidator sqlValidator;
```

**Change 3 - Lines 90-98 (Uncomment validation):**
```java
// BEFORE (commented out)
// CRITICAL: Validate query for SQL injection (temporarily disabled)
// TODO: Re-enable SQL injection validation when security module is restored
// if (!sqlValidator.isQuerySafe(query)) {
//     String errorMsg = "Query failed security validation...";
//     ...
// }

// AFTER (uncommented)
// CRITICAL: Validate query for SQL injection
if (!sqlValidator.isQuerySafe(query)) {
    String errorMsg = "Query failed security validation. Query may contain SQL injection attempts.";
    log.error("SQL Injection attempt detected in extraction. Query hash: {}",
              Integer.toHexString(query.hashCode()));
    result.getErrors().add(errorMsg);
    result.setRecordsFailed(1L);
    throw new SecurityException(errorMsg);
}
```

**Step 2: Update ConnectorFactory.java**

Open: `backend/src/main/java/com/jivs/platform/service/extraction/ConnectorFactory.java`

**Line 82 - Update legacy connector creation:**
```java
// BEFORE
return new JdbcConnector(
        dataSource.getConnectionUrl(),
        dataSource.getUsername(),
        decryptedPassword,
        dataSource.getSourceType().name()
);

// AFTER
return new JdbcConnector(
        dataSource.getConnectionUrl(),
        dataSource.getUsername(),
        decryptedPassword,
        dataSource.getSourceType().name(),
        sqlInjectionValidator  // ADD THIS
);
```

**Step 3: Test it**
```bash
cd backend
mvn test -Dtest=SqlInjectionValidatorTest

# Expected: All tests passing

# Integration test
mvn spring-boot:run

# In another terminal:
curl -X POST http://localhost:8080/api/v1/extractions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "SQL Injection Test",
    "sourceType": "POSTGRESQL",
    "extractionQuery": "SELECT * FROM users; DROP TABLE users;--"
  }'

# Expected: 400 Bad Request with "SQL injection" error
```

**Done!** ✅ SQL injection validation now active.

---

### Quick Win #3: Add Password Encryption
**Time:** 1 hour (code) + migration
**Impact:** CRITICAL security fix

**Step 1: Update ExtractionConfigService.java**

Open: `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionConfigService.java`

**Add CryptoUtil injection (line 28):**
```java
// BEFORE
@Service
@RequiredArgsConstructor
public class ExtractionConfigService {
    private final ExtractionConfigRepository extractionConfigRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ExtractionService extractionService;
}

// AFTER
@Service
@RequiredArgsConstructor
public class ExtractionConfigService {
    private final ExtractionConfigRepository extractionConfigRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ExtractionService extractionService;
    private final CryptoUtil cryptoUtil;  // ADD THIS
}
```

**Update findOrCreateDataSource method (lines 160-164):**
```java
// BEFORE
if (connectionConfig != null) {
    dataSource.setUsername(connectionConfig.get("username"));
    dataSource.setPasswordEncrypted(connectionConfig.get("password")); // TODO: Encrypt properly
}

// AFTER
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

**Step 2: Database Migration**

File already created: `backend/src/main/resources/db/migration/V111__Encrypt_existing_passwords.sql`

**Step 3: Create Password Migration Runner**

Create file: `backend/src/main/java/com/jivs/platform/migration/DataSourcePasswordMigration.java`

```java
package com.jivs.platform.migration;

import com.jivs.platform.common.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

                String auditSql = "INSERT INTO password_migration_audit " +
                                 "(data_source_id, migration_status) VALUES (?, 'SUCCESS')";
                jdbcTemplate.update(auditSql, id);

                log.debug("Encrypted password for data source ID: {}", id);

            } catch (Exception e) {
                log.error("Failed to encrypt password for data source ID: {}", id, e);

                String updateSql = "UPDATE data_sources SET password_migration_status = 'FAILED' WHERE id = ?";
                jdbcTemplate.update(updateSql, id);

                String auditSql = "INSERT INTO password_migration_audit " +
                                 "(data_source_id, migration_status, error_message) VALUES (?, 'FAILED', ?)";
                jdbcTemplate.update(auditSql, id, e.getMessage());
            }
        });

        log.info("DataSource password migration completed");
    }
}
```

**Step 4: Test It**
```bash
# Run migration (will execute on app startup)
mvn spring-boot:run

# Check logs for:
# "Starting DataSource password migration..."
# "Found X data sources with plaintext passwords - encrypting..."
# "DataSource password migration completed"

# Verify in database
psql -U jivs_user -d jivs -c "
  SELECT id, name,
         length(password_encrypted) as pwd_len,
         password_migration_status
  FROM data_sources;
"

# Expected: All pwd_len > 50, status = COMPLETED
```

**Done!** ✅ Passwords now encrypted.

---

## 📋 REMAINING FIXES

### Fix #4: Implement Batch Processing
**Time:** 6-8 hours
**Priority:** P0 - CRITICAL

**Full guide:** `backend/EXTRACTION_MODULE_BATCH_PROCESSING_IMPL.md`

**Quick summary:**
1. Add Maven dependencies (Parquet, Avro, OpenCSV)
2. Create `BatchWriter` interface
3. Implement `ParquetBatchWriter`
4. Implement `CsvBatchWriter`
5. Implement `JsonLinesBatchWriter`
6. Create `BatchWriterFactory`
7. Update `JdbcConnector.processBatch()` to use writers

**Start here:**
```bash
# Add dependencies to pom.xml
# See EXTRACTION_MODULE_BATCH_PROCESSING_IMPL.md

# Create batch writer package
mkdir -p backend/src/main/java/com/jivs/platform/service/extraction/batch

# Copy implementations from documentation
```

---

### Fix #5: Fix Broken Test
**Time:** 2-4 hours
**Priority:** P0 - CRITICAL

**Problem:**
`ExtractionContractTest.java` won't compile - uses wrong entities

**Solution:**
Rewrite test to use correct entities:
- `Extraction` → `ExtractionConfig` + `ExtractionJob`
- `ExtractionStatus` → `ExtractionJob.JobStatus`
- `SourceType` → `DataSource.SourceType`

**Full guide:** `backend/EXTRACTION_MODULE_TEST_SUITE.md`

---

## 🧪 TESTING CHECKLIST

After implementing fixes, run:

```bash
cd backend

# 1. Unit tests
mvn test

# 2. Integration tests
mvn verify -P integration-tests

# 3. Security tests
mvn test -Dtest=*SecurityTest

# 4. Load test
cd ..
k6 run backend/src/test/k6/extraction-load-test.js

# 5. Manual verification
curl -X POST http://localhost:8080/api/v1/extractions/1/start
# Check output file created
```

**All should pass before production deployment.**

---

## 📊 PROGRESS TRACKING

**Completed:** ✅
- [✅] Thread-safe ExtractionResult
- [✅] Database migration script
- [✅] Unit test suite
- [✅] JIRA tickets

**In Progress:** ⚠️
- [ ] SQL injection validation (30 min)
- [ ] Password encryption (1 hour)
- [ ] Batch processing (6 hours)
- [ ] Fix broken test (2 hours)

**Remaining P1/P2:**
- [ ] Fix resource leaks (1 hour)
- [ ] Fix inefficient query (1 hour)
- [ ] Add input validation (4 hours)
- [ ] Complete schema migration (6 hours)

**Total Time:** ~25 hours for all P0 fixes

---

## 🚀 DEPLOYMENT

Once P0 fixes complete:

**Follow deployment runbook:**
```bash
open backend/EXTRACTION_MODULE_DEPLOYMENT_RUNBOOK.md
```

**Key steps:**
1. Backup production database
2. Apply migration V111
3. Deploy application (runs password encryption)
4. Verify all passwords encrypted
5. Run verification tests
6. Monitor for 24 hours

---

## 📞 GETTING HELP

**Documentation:**
- Full fixes: `backend/EXTRACTION_MODULE_FIXES.md`
- Batch processing: `backend/EXTRACTION_MODULE_BATCH_PROCESSING_IMPL.md`
- Deployment: `backend/EXTRACTION_MODULE_DEPLOYMENT_RUNBOOK.md`
- Tests: `backend/EXTRACTION_MODULE_TEST_SUITE.md`
- Summary: `EXTRACTION_AUDIT_SUMMARY.md`

**Tickets:**
- JIRA export: `EXTRACTION_JIRA_TICKETS.md`

**Questions?**
- Check documentation first
- Review code comments
- Search for related tests
- Ask in team Slack channel

---

## ✅ DEFINITION OF DONE

**For each fix:**
- [ ] Code implemented as documented
- [ ] Unit tests passing
- [ ] Integration test passing
- [ ] Security audit clean (for security fixes)
- [ ] Code reviewed
- [ ] Documentation updated
- [ ] JIRA ticket closed

**For module:**
- [ ] All P0 issues resolved
- [ ] Test coverage >80%
- [ ] Load test passing
- [ ] Security audit passing
- [ ] Production deployment successful

---

**Ready to start?** Begin with Quick Win #2 (SQL injection validation - 30 minutes)

**Stuck?** Check the detailed guides or ask for help.

**Good luck!** 🚀
