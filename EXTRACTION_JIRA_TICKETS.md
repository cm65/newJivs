# JiVS Extraction Module - JIRA Ticket Export

**Epic:** JIVS-EXTRACTION-AUDIT
**Component:** Data Extraction
**Created:** 2025-10-26
**Reporter:** jivs-extraction-expert

Import these tickets into your project management system.

---

## EPIC: Extraction Module Security & Functionality Fixes

**Epic Key:** JIVS-EXTRACTION-AUDIT
**Type:** Epic
**Priority:** Critical
**Labels:** security, extraction, technical-debt, production-blocker

**Description:**
Complete audit of JiVS extraction module revealed 13 critical issues preventing production deployment. This epic tracks all fixes required to make the module production-ready.

**Goals:**
- Fix 4 critical security vulnerabilities
- Implement missing functionality (batch processing)
- Resolve concurrency issues
- Achieve >80% test coverage

**Acceptance Criteria:**
- [ ] All P0 issues resolved
- [ ] Security audit passing
- [ ] Load test passing (100 concurrent users)
- [ ] Test coverage >80%

**Story Points:** 55
**Target Release:** Next Sprint

---

## P0 - CRITICAL BLOCKERS (Must fix before production)

### JIVS-EXTRACT-001: Enable SQL Injection Validation

**Type:** Bug - Security Vulnerability
**Priority:** Critical (P0)
**Labels:** security, sql-injection, blocker

**Summary:**
SQL injection validation is disabled in JdbcConnector, leaving database vulnerable to attacks

**Description:**
The SQL injection validator exists and is functional, but validation code is commented out in `JdbcConnector.java`. All extraction queries are executed without security validation.

**Impact:**
- Attackers can execute arbitrary SQL
- Database can be dropped or modified
- Data exfiltration possible
- CRITICAL security vulnerability

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java:46, 90-98`
- `backend/src/main/java/com/jivs/platform/service/extraction/ConnectorFactory.java:69-83`

**Steps to Reproduce:**
```bash
curl -X POST http://localhost:8080/api/v1/extractions \
  -d '{"extractionQuery": "SELECT * FROM users; DROP TABLE users;--"}'
# Currently succeeds - should FAIL with validation error
```

**Fix:**
1. Uncomment validation in `JdbcConnector.java` lines 90-98
2. Add `SqlInjectionValidator` parameter to constructor (line 49)
3. Update `ConnectorFactory.getLegacyConnector()` to pass validator (line 82)

**Acceptance Criteria:**
- [ ] SQL injection attempts blocked (return 400 error)
- [ ] Legitimate queries still work
- [ ] Unit tests for SQL injection patterns passing
- [ ] Security audit confirms fix

**Detailed Fix:**
See `backend/EXTRACTION_MODULE_FIXES.md` - Issue #1

**Estimate:** 2 hours
**Assignee:** Backend Team

---

### JIVS-EXTRACT-002: Encrypt Passwords in Database

**Type:** Bug - Security Vulnerability
**Priority:** Critical (P0)
**Labels:** security, encryption, compliance, blocker

**Summary:**
Data source passwords stored as plaintext in database despite field named `passwordEncrypted`

**Description:**
`ExtractionConfigService.java:163` stores passwords without encryption. CryptoUtil exists but is not used. This violates security best practices and compliance requirements (GDPR, SOC 2).

**Impact:**
- Database breach exposes all source system credentials
- Compliance violations
- Cannot pass security audit
- Credentials visible in database dumps

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionConfigService.java:163`
- `backend/src/main/resources/db/migration/V111__Encrypt_existing_passwords.sql` (to create)

**Fix Steps:**
1. Inject `CryptoUtil` into `ExtractionConfigService`
2. Update `findOrCreateDataSource()` to encrypt passwords before storage
3. Run database migration `V111` to prepare existing passwords
4. Deploy `DataSourcePasswordMigration.java` to encrypt existing data
5. Verify all passwords encrypted (length >50 characters)

**Migration Required:** YES (30 minutes downtime)

**Acceptance Criteria:**
- [ ] New passwords encrypted before storage
- [ ] Existing passwords migrated successfully
- [ ] Password decryption works (connections succeed)
- [ ] Zero plaintext passwords in database
- [ ] Migration audit table shows 100% success

**Detailed Fix:**
See `backend/EXTRACTION_MODULE_FIXES.md` - Issue #2
See `backend/EXTRACTION_MODULE_DEPLOYMENT_RUNBOOK.md` - Phase 1

**Estimate:** 4 hours (includes migration)
**Assignee:** Backend Team + DevOps

---

### JIVS-EXTRACT-003: Fix Broken Contract Test Suite

**Type:** Bug - Quality
**Priority:** Critical (P0)
**Labels:** testing, broken-build, blocker

**Summary:**
ExtractionContractTest won't compile - references non-existent entities

**Description:**
Test file imports entities that don't exist in codebase:
- `Extraction` entity (should be `ExtractionConfig` + `ExtractionJob`)
- `ExtractionStatus` enum (should be `ExtractionJob.JobStatus`)
- `SourceType` enum (should be `DataSource.SourceType`)
- `ExtractionRepository` (should be `ExtractionConfigRepository` + `ExtractionJobRepository`)

**Impact:**
- Test suite won't compile
- Zero API contract validation
- Frontend-backend integration can break
- No regression testing

**Files Affected:**
- `backend/src/test/java/com/jivs/platform/contract/ExtractionContractTest.java`

**Fix:**
Complete rewrite of test file to use correct entities and repositories.

**Acceptance Criteria:**
- [ ] Test compiles successfully
- [ ] All provider states work
- [ ] Contract verification passes
- [ ] Integrated into CI/CD pipeline

**Detailed Fix:**
See `backend/EXTRACTION_MODULE_TEST_SUITE.md` - Section on contract tests

**Estimate:** 4 hours
**Assignee:** QA Team

---

### JIVS-EXTRACT-004: Implement Batch Processing Output

**Type:** Bug - Functionality
**Priority:** Critical (P0)
**Labels:** data-loss, functionality, blocker

**Summary:**
Batch processing is placeholder - extracted data is DISCARDED, not saved

**Description:**
`processBatch()` method in JdbcConnector and PooledJdbcConnector contains only TODO comment. All extraction jobs report success but produce no output files. This is 100% data loss.

**Impact:**
- **All extractions silently fail to produce output**
- Users think data is extracted but it's lost
- Complete module failure
- Production deployment impossible

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java:212-229`
- `backend/src/main/java/com/jivs/platform/service/extraction/PooledJdbcConnector.java:203-213`

**Fix:**
Implement complete batch writing system:
1. Create `BatchWriter` interface
2. Implement `ParquetBatchWriter` (primary format)
3. Implement `CsvBatchWriter` (backward compatibility)
4. Implement `JsonLinesBatchWriter` (developer-friendly)
5. Create `BatchWriterFactory`
6. Update connectors to use writers

**Acceptance Criteria:**
- [ ] Parquet output files created successfully
- [ ] CSV and JSON formats working
- [ ] Output files verified non-empty
- [ ] End-to-end extraction test passing
- [ ] Files compressed with Snappy

**Detailed Fix:**
See `backend/EXTRACTION_MODULE_BATCH_PROCESSING_IMPL.md`

**Estimate:** 10 hours
**Assignee:** Backend Team

---

## P1 - HIGH PRIORITY

### JIVS-EXTRACT-005: Fix Thread-Safety in ExtractionResult

**Type:** Bug - Correctness
**Priority:** High (P1)
**Labels:** concurrency, race-condition

**Summary:**
ExtractionResult uses non-thread-safe collections in multi-threaded context

**Description:**
`ExtractionResult` uses plain `Long` fields and `ArrayList` for errors, but is modified by 4 concurrent threads in JdbcConnector. This causes:
- Lost updates to counters
- ConcurrentModificationException
- Data corruption

**Impact:**
- Incorrect extraction metrics
- Application crashes during concurrent extractions
- Unreliable reporting

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionResult.java`

**Fix:**
Replace with thread-safe alternatives:
- `Long` → `AtomicLong`
- `ArrayList` → `CopyOnWriteArrayList`

**Status:** ✅ **FIXED** (implemented)
**Files Updated:**
- `ExtractionResult.java` (replaced with thread-safe version)
- `ExtractionResultTest.java` (comprehensive concurrency tests added)

**Acceptance Criteria:**
- [✅] AtomicLong used for all counters
- [✅] CopyOnWriteArrayList used for errors
- [✅] Concurrency tests passing (20 threads, 1000 ops each)
- [✅] No ConcurrentModificationException in tests

**Estimate:** 2 hours (COMPLETED)
**Assignee:** Backend Team

---

### JIVS-EXTRACT-006: Fix Connection Resource Leak

**Type:** Bug - Stability
**Priority:** High (P1)
**Labels:** resource-leak, memory-leak

**Summary:**
JdbcConnector doesn't close database connections in error paths

**Description:**
`JdbcConnector.extract()` opens connection but doesn't close it in catch blocks. Only executor is shut down in finally block, not connection. Leads to connection pool exhaustion.

**Impact:**
- Connection pool exhaustion after failures
- Database connections leak
- Application instability
- Requires restart to recover

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java:82-203`

**Fix:**
Add connection close in finally block:
```java
finally {
    if (connection != null) {
        try { connection.close(); }
        catch (SQLException e) { log.error(...); }
    }
    // ... existing executor shutdown
}
```

**Acceptance Criteria:**
- [ ] Connection closed in all code paths
- [ ] Load test shows no connection leaks
- [ ] Pool metrics stable after 1000 extractions

**Estimate:** 1 hour
**Assignee:** Backend Team

---

### JIVS-EXTRACT-007: Fix Inefficient Database Query

**Type:** Bug - Performance
**Priority:** High (P1)
**Labels:** performance, n-plus-1

**Summary:**
ExtractionConfigService.findOrCreateDataSource() loads all DataSources into memory

**Description:**
Uses `dataSourceRepository.findAll()` then filters in Java. With 10,000+ DataSources, this causes:
- Memory overflow
- Slow response times
- O(n) complexity

**Impact:**
- Application crashes with many DataSources
- Slow extraction creation
- Poor scalability

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionConfigService.java:146-152`
- `backend/src/main/java/com/jivs/platform/repository/DataSourceRepository.java` (add method)

**Fix:**
1. Add `findByConnectionUrl()` to DataSourceRepository
2. Replace `findAll().stream().filter()` with database query

**Acceptance Criteria:**
- [ ] No findAll() calls
- [ ] Query uses index on connectionUrl
- [ ] Performance test with 10,000 DataSources passes

**Estimate:** 1 hour
**Assignee:** Backend Team

---

### JIVS-EXTRACT-008: Mark Stub Connectors as Unimplemented

**Type:** Task - Technical Debt
**Priority:** High (P1)
**Labels:** stub, documentation

**Summary:**
ApiConnector, FileConnector, SapConnector return fake data instead of throwing UnsupportedOperationException

**Description:**
These connectors always return success with hardcoded values:
- `ApiConnector.extract()` returns 250 records (fake)
- `FileConnector.extract()` returns 500 records (fake)
- `SapConnector.extract()` returns 1000 records (fake)

Users think extraction worked but got fake data.

**Impact:**
- Silent incorrect behavior
- Users confused by fake data
- Production deployment with broken features

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ApiConnector.java`
- `backend/src/main/java/com/jivs/platform/service/extraction/FileConnector.java`
- `backend/src/main/java/com/jivs/platform/service/extraction/SapConnector.java`

**Fix:**
Either implement properly OR throw UnsupportedOperationException

**Acceptance Criteria:**
- [ ] Connectors either work or fail explicitly
- [ ] Documentation updated to list supported connectors
- [ ] API returns clear error for unsupported types

**Estimate:** 4 hours (mark as unsupported) OR 40 hours (implement properly)
**Assignee:** Product + Backend Team (decide direction)

---

## P2 - MEDIUM PRIORITY

### JIVS-EXTRACT-009: Add Input Validation to API Endpoints

**Type:** Improvement - Security
**Priority:** Medium (P2)
**Labels:** validation, security

**Summary:**
ExtractionController and ExtractionConfigService lack input validation

**Description:**
No validation for:
- Null/empty required fields
- String length limits
- Invalid enum values
- SQL query safety
- Unchecked type casts

**Impact:**
- NullPointerException crashes
- ClassCastException errors
- Poor user experience
- Security risks

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionConfigService.java:35-63`
- `backend/src/main/java/com/jivs/platform/controller/ExtractionController.java:43-64`

**Fix:**
Add comprehensive validation with clear error messages

**Acceptance Criteria:**
- [ ] All required fields validated
- [ ] String lengths checked
- [ ] Enum values validated
- [ ] Helpful error messages returned

**Estimate:** 4 hours
**Assignee:** Backend Team

---

### JIVS-EXTRACT-010: Complete Schema Migration

**Type:** Task - Technical Debt
**Priority:** Medium (P2)
**Labels:** database, migration, technical-debt

**Summary:**
ExtractionJob has dual schema - old @Transient fields + new relationship

**Description:**
Code maintains backward compatibility with old schema using @Transient fields and complex fallback logic throughout ExtractionService. This increases complexity and bug risk.

**Impact:**
- Code complexity
- Maintenance burden
- Potential bugs in fallback logic

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/domain/extraction/ExtractionJob.java:82-89`
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionService.java` (multiple locations)

**Fix:**
1. Complete migration to new schema
2. Remove @Transient fields
3. Simplify service logic
4. Add data migration script

**Acceptance Criteria:**
- [ ] @Transient fields removed
- [ ] All code uses extractionConfig relationship
- [ ] Migration script tested
- [ ] Zero data loss

**Estimate:** 6 hours
**Assignee:** Backend Team

---

### JIVS-EXTRACT-011: Make Path Validation Configurable

**Type:** Improvement
**Priority:** Medium (P2)
**Labels:** configuration, cloud-storage

**Summary:**
Output path validation hardcoded to /tmp, /data, /var/lib/jivs - blocks cloud storage

**Description:**
`PooledJdbcConnector.validateOutputPath()` hardcodes allowed directories. Won't work for:
- Windows paths
- S3 (s3://...)
- Azure Blob (azblob://...)
- GCS (gs://...)

**Impact:**
- Cannot use cloud storage
- Windows deployment fails
- Not configurable per environment

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/service/extraction/PooledJdbcConnector.java:221-247`
- `backend/src/main/resources/application.yml` (add config)

**Fix:**
1. Make allowed paths configurable
2. Support cloud storage schemes
3. Document in application.yml

**Acceptance Criteria:**
- [ ] Paths configurable in application.yml
- [ ] S3/Azure/GCS schemes supported
- [ ] Works on Windows

**Estimate:** 2 hours
**Assignee:** Backend Team

---

### JIVS-EXTRACT-012: Make Executor Timeout Configurable

**Type:** Improvement
**Priority:** Medium (P2)
**Labels:** configuration, performance

**Summary:**
Batch processing timeout hardcoded to 5 minutes - insufficient for large datasets

**Description:**
`JdbcConnector` and `PooledJdbcConnector` use hardcoded 5-minute timeout for batch processing. Large extractions need more time.

**Impact:**
- Large extractions timeout
- No way to adjust per environment
- Data loss when timeout triggers

**Files Affected:**
- `backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java:172`
- `backend/src/main/java/com/jivs/platform/service/extraction/PooledJdbcConnector.java:166`

**Fix:**
Add configurable timeout in application.yml

**Acceptance Criteria:**
- [ ] Timeout configurable
- [ ] Default reasonable (30 minutes)
- [ ] Job fails explicitly on timeout

**Estimate:** 1 hour
**Assignee:** Backend Team

---

### JIVS-EXTRACT-013: Improve Logging Security

**Type:** Improvement - Security
**Priority:** Medium (P2)
**Labels:** logging, security, pii

**Summary:**
Sensitive data (queries, connection URLs) logged at INFO level

**Description:**
Connection strings and SQL queries logged without sanitization. May contain:
- Passwords in connection URLs
- PII in query parameters
- Sensitive data in error messages

**Impact:**
- Credentials exposed in logs
- PII compliance issues
- Security audit failures

**Files Affected:**
- Multiple files in extraction package

**Fix:**
1. Sanitize connection URLs (remove passwords)
2. Hash queries instead of logging full text
3. Move sensitive logs to DEBUG level
4. Add log scrubbing utility

**Acceptance Criteria:**
- [ ] No passwords in logs
- [ ] Queries hashed at INFO level
- [ ] PII scrubbed from error messages

**Estimate:** 2 hours
**Assignee:** Backend Team

---

## SUMMARY

**Total Issues:** 13
**Total Story Points:** 55 hours

**By Priority:**
- P0 (Critical): 4 issues, 20 hours
- P1 (High): 4 issues, 10 hours (1 completed)
- P2 (Medium): 5 issues, 15 hours

**By Type:**
- Security: 5 issues
- Functionality: 2 issues
- Performance: 2 issues
- Quality: 2 issues
- Technical Debt: 2 issues

**Completion Status:**
- ✅ JIVS-EXTRACT-005: Thread-safety (COMPLETED)
- ⚠️ All others: IN PROGRESS

**Target Sprint:** Next sprint (2 weeks)
**Blockers:** JIVS-EXTRACT-001, 002, 003, 004 (must complete before production)

---

## IMPORT INSTRUCTIONS

### For JIRA:
1. Create epic: JIVS-EXTRACTION-AUDIT
2. Import issues as subtasks or linked issues
3. Set sprint and assign to team
4. Link to documentation in each ticket

### For GitHub Issues:
```bash
# Use GitHub CLI to create issues
gh issue create --title "Enable SQL Injection Validation" \
  --body "See EXTRACTION_JIRA_TICKETS.md - JIVS-EXTRACT-001" \
  --label "P0,security,blocker"
```

### For Linear/Shortcut:
Copy issue descriptions and adjust formatting as needed.

---

**Related Documentation:**
- `backend/EXTRACTION_MODULE_FIXES.md` - Detailed fixes
- `backend/EXTRACTION_MODULE_DEPLOYMENT_RUNBOOK.md` - Deployment procedures
- `backend/EXTRACTION_MODULE_TEST_SUITE.md` - Test specifications
- `EXTRACTION_AUDIT_SUMMARY.md` - Executive summary
