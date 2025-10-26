# Extraction Module Fixes - Deployment Runbook

**Version:** 1.0
**Last Updated:** 2025-10-26
**Owner:** Backend Engineering Team
**Estimated Downtime:** 30 minutes (for password migration)

---

## 🎯 OVERVIEW

This runbook guides deployment of critical fixes to the JiVS Extraction Module.

**What's Being Fixed:**
- ✅ SQL injection validation (security)
- ✅ Password encryption (security)
- ✅ Thread-safe ExtractionResult (correctness)
- ✅ Batch processing implementation (functionality)
- ✅ Resource leak fixes (stability)

**Risk Level:** MEDIUM
**Rollback Strategy:** Database migration must complete; application can roll back

---

## 📋 PRE-DEPLOYMENT CHECKLIST

### Infrastructure

- [ ] Backup production database
  ```bash
  pg_dump -U jivs_user -d jivs -F c -f /backups/jivs_pre_extraction_fix_$(date +%Y%m%d_%H%M%S).dump
  ```

- [ ] Verify disk space for extraction outputs
  ```bash
  df -h /data/jivs/extractions
  # Recommended: 100GB free minimum
  ```

- [ ] Verify Redis is running (for caching)
  ```bash
  redis-cli ping
  # Expected: PONG
  ```

- [ ] Verify RabbitMQ is running (for job queueing)
  ```bash
  sudo systemctl status rabbitmq-server
  # Expected: active (running)
  ```

### Code Review

- [ ] All fixes peer-reviewed
- [ ] Unit tests passing (>95%)
  ```bash
  mvn test -Dtest=*Extraction*
  ```

- [ ] Integration tests passing
  ```bash
  mvn verify -P integration-tests
  ```

- [ ] Security scan clean
  ```bash
  mvn org.owasp:dependency-check-maven:check
  ```

### Communication

- [ ] Notify users of 30-minute maintenance window
- [ ] Notify on-call team
- [ ] Create incident channel (Slack/Teams)
- [ ] Prepare rollback communication

---

## 🚀 DEPLOYMENT PROCEDURE

### Phase 1: Database Migration (15 minutes)

**Objective:** Prepare database for password encryption

**Step 1.1: Apply Migration Script**

```bash
cd backend/src/main/resources/db/migration

# Review migration
cat V111__Encrypt_existing_passwords.sql

# Apply to production (Flyway will run on app startup)
# OR apply manually:
psql -U jivs_user -d jivs -f V111__Encrypt_existing_passwords.sql
```

**Expected Output:**
```
ALTER TABLE
UPDATE 42
ALTER TABLE
UPDATE 42
CREATE TABLE
```

**Step 1.2: Verify Migration**

```sql
-- Check migration status
SELECT COUNT(*) FROM data_sources WHERE password_migration_status = 'PENDING';
-- Expected: Same count as total data_sources

-- Verify temp column exists
\d data_sources
-- Should show: password_plaintext_temp column
```

**⚠️ CRITICAL CHECKPOINT:**
- [ ] Migration script applied successfully
- [ ] All data_sources have password_migration_status = 'PENDING'
- [ ] password_plaintext_temp contains current passwords
- [ ] No errors in database logs

**Rollback (if needed):**
```sql
-- Undo migration
ALTER TABLE data_sources DROP COLUMN password_plaintext_temp;
ALTER TABLE data_sources DROP COLUMN password_migration_status;
DROP TABLE password_migration_audit;
```

---

### Phase 2: Deploy Application (10 minutes)

**Objective:** Deploy fixed code and run password encryption

**Step 2.1: Stop Application**

```bash
# Kubernetes
kubectl scale deployment jivs-backend --replicas=0 -n jivs

# OR Systemd
sudo systemctl stop jivs-backend

# Verify stopped
kubectl get pods -n jivs | grep jivs-backend
# Expected: No running pods
```

**Step 2.2: Deploy New Version**

```bash
# Kubernetes
kubectl set image deployment/jivs-backend \
  jivs-backend=jivs-backend:v1.2.0-extraction-fixes \
  -n jivs

kubectl scale deployment jivs-backend --replicas=3 -n jivs

# OR Docker Compose
docker-compose pull jivs-backend
docker-compose up -d jivs-backend

# OR JAR deployment
java -jar jivs-backend-1.2.0-extraction-fixes.jar
```

**Step 2.3: Monitor Password Encryption**

Application will automatically encrypt passwords on startup via `DataSourcePasswordMigration`.

```bash
# Watch application logs
kubectl logs -f deployment/jivs-backend -n jivs | grep -i "password migration"

# Expected log output:
# Starting DataSource password migration...
# Found 42 data sources with plaintext passwords - encrypting...
# Encrypted password for data source ID: 1
# Encrypted password for data source ID: 2
# ...
# DataSource password migration completed
```

**Step 2.4: Verify Password Encryption**

```sql
-- Check migration status
SELECT COUNT(*) FROM data_sources WHERE password_migration_status = 'COMPLETED';
-- Expected: Count should match total data_sources

-- Verify passwords are encrypted (long strings)
SELECT id, name, length(password_encrypted) as pwd_len, password_migration_status
FROM data_sources;
-- Expected: All pwd_len > 50 characters

-- Check for failures
SELECT * FROM password_migration_audit WHERE migration_status = 'FAILED';
-- Expected: 0 rows
```

**⚠️ CRITICAL CHECKPOINT:**
- [ ] All passwords encrypted (length > 50)
- [ ] Migration status = 'COMPLETED' for all data sources
- [ ] No FAILED entries in password_migration_audit
- [ ] Application started successfully

**Rollback (if needed):**
```bash
# Deploy previous version
kubectl set image deployment/jivs-backend \
  jivs-backend=jivs-backend:v1.1.0 \
  -n jivs

# Restore passwords from temp column
psql -U jivs_user -d jivs -c "
  UPDATE data_sources
  SET password_encrypted = password_plaintext_temp,
      password_migration_status = 'ROLLBACK'
  WHERE password_migration_status = 'FAILED';
"
```

---

### Phase 3: Verification (5 minutes)

**Objective:** Verify all fixes are working

**Test 1: SQL Injection Prevention**

```bash
curl -X POST http://localhost:8080/api/v1/extractions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "SQL Injection Test",
    "sourceType": "POSTGRESQL",
    "extractionQuery": "SELECT * FROM users; DROP TABLE users;--"
  }'

# Expected: 400 Bad Request
# Body: {"error": "Query failed security validation"}
```

**✅ PASS:** SQL injection blocked
**❌ FAIL:** Request succeeds → ROLLBACK IMMEDIATELY

**Test 2: Password Decryption**

```sql
-- Verify encrypted passwords can be decrypted
-- (Internally tested by connector factory when creating connections)

SELECT id, name, password_encrypted IS NOT NULL as has_password
FROM data_sources
WHERE is_active = true
LIMIT 5;
```

```bash
# Create test extraction to verify password decryption works
curl -X POST http://localhost:8080/api/v1/extractions/1/start \
  -H "Authorization: Bearer $TOKEN"

# Check logs for connection success
kubectl logs deployment/jivs-backend -n jivs | grep -i "connection"
# Expected: "Connection established successfully"
```

**✅ PASS:** Connections work with encrypted passwords
**❌ FAIL:** Connection errors → Check CryptoUtil key

**Test 3: Batch Processing Output**

```bash
# Start a small extraction
curl -X POST http://localhost:8080/api/v1/extractions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Batch Test",
    "sourceType": "POSTGRESQL",
    "connectionConfig": {...},
    "extractionQuery": "SELECT * FROM users LIMIT 100",
    "format": "parquet"
  }'

# Wait for completion (check job status)
# Then verify output file exists
ls -lh /data/jivs/extractions/
# Expected: .parquet file with size > 0
```

**✅ PASS:** Output file created
**❌ FAIL:** No output or size = 0 → Batch processing not working

**Test 4: Concurrency Test**

```bash
# Run load test with concurrent extractions
k6 run backend/src/test/k6/extraction-load-test.js

# Expected results:
# ✓ 95% requests successful
# ✓ No ConcurrentModificationException in logs
# ✓ p95 latency < 500ms
```

**✅ PASS:** No concurrency errors
**❌ FAIL:** ConcurrentModificationException in logs → Thread safety issue

**Test 5: Resource Leak Test**

```bash
# Check connection pool stats before
curl http://localhost:8080/actuator/metrics/hikari.connections.active

# Run 50 extractions
for i in {1..50}; do
  curl -X POST http://localhost:8080/api/v1/extractions/$i/start &
done
wait

# Check connection pool stats after
curl http://localhost:8080/actuator/metrics/hikari.connections.active

# Expected: Active connections should return to baseline
```

**✅ PASS:** No connection leak
**❌ FAIL:** Connections keep growing → Resource leak

---

### Phase 4: Cleanup (Post-Deployment)

**24 Hours After Deployment**

Once confident all is working:

```sql
-- Remove temporary password column
ALTER TABLE data_sources DROP COLUMN password_plaintext_temp;

-- Update migration status
UPDATE data_sources
SET password_migration_status = NULL;

-- Optionally archive migration audit table
CREATE TABLE password_migration_audit_archive AS
SELECT * FROM password_migration_audit;

DROP TABLE password_migration_audit;
```

---

## 🔄 ROLLBACK PROCEDURE

**If deployment fails after Phase 2:**

### Step 1: Restore Application

```bash
# Kubernetes
kubectl set image deployment/jivs-backend \
  jivs-backend=jivs-backend:v1.1.0 \
  -n jivs

# Wait for rollout
kubectl rollout status deployment/jivs-backend -n jivs
```

### Step 2: Restore Passwords (if encrypted passwords broken)

```sql
-- Restore from temp column
UPDATE data_sources
SET password_encrypted = password_plaintext_temp,
    password_migration_status = 'ROLLBACK'
WHERE password_plaintext_temp IS NOT NULL;

-- Verify
SELECT COUNT(*) FROM data_sources
WHERE password_encrypted IS NOT NULL;
```

### Step 3: Verify Old Version Working

```bash
# Test extraction creation
curl -X POST http://localhost:8080/api/v1/extractions/1/start \
  -H "Authorization: Bearer $TOKEN"

# Expected: Works (but without new security fixes)
```

### Step 4: Incident Report

Document in postmortem:
- What failed
- At what stage
- Error messages/logs
- Data impact (if any)
- Root cause
- Prevention measures

---

## 📊 MONITORING

### Key Metrics to Watch (First 24 Hours)

**Application Health:**
```bash
# Check application is running
curl http://localhost:8080/actuator/health

# Check extraction job success rate
curl http://localhost:8080/actuator/metrics/jivs.extraction.jobs.success.rate
# Target: >95%
```

**Database:**
```sql
-- Monitor extraction job statuses
SELECT status, COUNT(*)
FROM extraction_jobs
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY status;
-- Target: COMPLETED > 95% of total

-- Monitor for SQL injection attempts
SELECT COUNT(*)
FROM extraction_jobs
WHERE error_message LIKE '%SQL injection%'
AND created_at > NOW() - INTERVAL '24 hours';
-- Target: 0 (good!), >0 means attacks blocked
```

**Performance:**
```bash
# Extraction throughput
curl http://localhost:8080/actuator/metrics/jivs.extraction.throughput
# Target: >15,000 records/min (improved from 10,000)

# Latency (p95)
curl http://localhost:8080/actuator/metrics/jivs.extraction.latency.p95
# Target: <300ms (improved from 450ms)
```

**Errors to Alert On:**

```bash
# Critical errors
kubectl logs deployment/jivs-backend -n jivs | grep -i "ERROR\|CRITICAL\|FATAL"

# Watch for:
# - "Failed to decrypt password" → Encryption key issue
# - "SQL Injection attempt" → Good (blocked attacks)
# - "ConcurrentModificationException" → Thread safety issue
# - "Failed to write batch" → Batch processing issue
# - "Connection pool exhausted" → Resource leak
```

---

## 📞 CONTACT & ESCALATION

**Deployment Team:**
- On-Call Engineer: [NAME] - [PHONE]
- Backend Lead: [NAME] - [EMAIL]
- Database Admin: [NAME] - [EMAIL]

**Escalation Path:**
1. On-Call Engineer (immediate)
2. Backend Lead (if issue unresolved in 15 min)
3. CTO (if data loss or security breach)

**Rollback Authority:**
- On-Call Engineer: Can rollback independently
- Backend Lead: Required for data migrations
- CTO: Required for rollback >2 hours after deployment

---

## 📚 APPENDICES

### Appendix A: Full Test Suite

```bash
# Run complete test suite
cd backend

# Unit tests
mvn test

# Integration tests
mvn verify -P integration-tests

# Contract tests
cd ../frontend && npm run test:contract
cd ../backend && mvn test -Dtest=*ContractTest

# Load tests
k6 run src/test/k6/extraction-load-test.js

# Security tests
mvn org.owasp:dependency-check-maven:check
```

### Appendix B: Database Queries Reference

```sql
-- Count data sources
SELECT COUNT(*) FROM data_sources;

-- Check password encryption status
SELECT
  password_migration_status,
  COUNT(*) as count
FROM data_sources
GROUP BY password_migration_status;

-- View recent extraction jobs
SELECT
  job_id,
  status,
  records_extracted,
  error_message,
  created_at
FROM extraction_jobs
ORDER BY created_at DESC
LIMIT 20;

-- Check for running extractions
SELECT COUNT(*)
FROM extraction_jobs
WHERE status = 'RUNNING';
```

### Appendix C: Environment Variables

Required for new deployment:

```bash
# Encryption key (must be same as before)
export JIVS_ENCRYPTION_KEY=<existing-key>

# Extraction output directory
export JIVS_EXTRACTION_OUTPUT_DIR=/data/jivs/extractions

# Batch processing config
export JIVS_EXTRACTION_BATCH_SIZE=1000
export JIVS_EXTRACTION_PARALLEL_THREADS=4
export JIVS_EXTRACTION_BATCH_TIMEOUT_MINUTES=30

# Connection pool config
export JIVS_EXTRACTION_SOURCE_POOL_MAX_SIZE=10
export JIVS_EXTRACTION_SOURCE_POOL_MIN_IDLE=2
```

---

## ✅ DEPLOYMENT SUCCESS CRITERIA

Deployment is successful when:

- [ ] All data source passwords encrypted (100%)
- [ ] No password migration failures
- [ ] SQL injection validation blocking malicious queries
- [ ] Extraction jobs producing output files
- [ ] No ConcurrentModificationException errors
- [ ] No connection pool exhaustion
- [ ] Application health checks passing
- [ ] All tests passing (unit, integration, contract)
- [ ] Extraction throughput >15,000 records/min
- [ ] p95 latency <300ms
- [ ] No error spikes in logs
- [ ] Rollback plan tested and ready

**Sign-off Required:**
- [ ] Backend Engineer: _______________
- [ ] DevOps Engineer: _______________
- [ ] QA Lead: _______________
- [ ] Security Review (for password encryption): _______________

**Deployment Date:** _______________
**Deployment Time:** _______________
**Actual Downtime:** _______________

---

**Related Documents:**
- `EXTRACTION_MODULE_FIXES.md` - Fix details
- `EXTRACTION_MODULE_BATCH_PROCESSING_IMPL.md` - Batch processing guide
- `EXTRACTION_MODULE_AUDIT_REPORT.md` - Full audit
