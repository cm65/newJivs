# JiVS Workflow Orchestration - Developer Quick Start Guide

**Version:** 1.0
**Last Updated:** October 26, 2025
**Target Audience:** Backend developers implementing workflow fixes

---

## 🚀 Quick Start (5 minutes)

### 1. Prerequisites Check

```bash
# Check Java version (need 21+)
java -version

# Check Maven version
mvn -version

# Check Docker
docker --version

# Check Redis
redis-cli ping

# Check PostgreSQL
psql --version
```

### 2. Pull Latest Code

```bash
cd /path/to/jivs-platform
git checkout main
git pull origin main
```

### 3. Install Dependencies

```bash
# Add to backend/pom.xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>

<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.25.0</version>
</dependency>

# Install
cd backend
mvn clean install -DskipTests
```

### 4. Start Infrastructure

```bash
# Start Redis, PostgreSQL, RabbitMQ
docker-compose up -d redis postgres rabbitmq

# Verify services
docker-compose ps
```

### 5. Run Database Migration

```bash
cd backend
mvn flyway:migrate
```

### 6. Start Application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=workflow
```

### 7. Verify Setup

```bash
# Check health
curl http://localhost:8080/actuator/health

# Check metrics
curl http://localhost:8080/actuator/metrics

# Check Prometheus endpoint
curl http://localhost:8080/actuator/prometheus | grep migration
```

---

## 📁 File Organization

### New Files (Already Created)
```
backend/src/main/java/com/jivs/platform/
├── service/migration/
│   └── MigrationExecutor.java              ✅ Created
├── config/
│   ├── WorkflowExecutorConfig.java         ✅ Created
│   ├── ResilienceConfig.java               (to create)
│   └── RedissonConfig.java                 (to create)
├── messaging/
│   └── MigrationMessageConsumer.java       (to create)
├── event/
│   └── EventFallbackQueue.java             (to create)
└── monitoring/
    └── MigrationMetrics.java               (to create)

backend/src/main/resources/
├── application-workflow.yml                ✅ Created
└── db/migration/
    └── V111__Add_workflow_improvements.sql ✅ Created
```

### Files to Modify
```
backend/src/main/java/com/jivs/platform/
├── service/migration/
│   ├── MigrationOrchestrator.java          ⚠️ Needs updates
│   └── LoadService.java                    ⚠️ Needs updates
├── event/
│   └── MigrationEventPublisher.java        ⚠️ Needs updates
└── controller/
    └── MigrationController.java            ⚠️ Needs updates
```

---

## 🔧 Implementation Checklist

### Phase 1: Transaction Management (Day 1)

- [ ] **Create MigrationExecutor.java** (✅ Already created)
  ```bash
  # File location
  backend/src/main/java/com/jivs/platform/service/migration/MigrationExecutor.java
  ```

- [ ] **Update MigrationOrchestrator.java**
  ```java
  // Add this field
  private final MigrationExecutor migrationExecutor;

  // Update executeMigration() method
  @Async("migrationExecutor")
  public CompletableFuture<Migration> executeMigration(Long migrationId) {
      return CompletableFuture.completedFuture(
          migrationExecutor.executeWithTransaction(migrationId)
      );
  }
  ```

- [ ] **Test transaction rollback**
  ```bash
  mvn test -Dtest=MigrationOrchestratorTransactionTest
  ```

### Phase 2: Distributed Locking (Day 2)

- [ ] **Create RedissonConfig.java**
  ```bash
  cp backend/src/main/java/com/jivs/platform/config/RedissonConfig.java.template \
     backend/src/main/java/com/jivs/platform/config/RedissonConfig.java
  ```

- [ ] **Update MigrationOrchestrator.resumeMigration()**
  ```java
  // Add distributed locking
  RLock lock = redissonClient.getLock("migration:lock:" + migrationId);
  try {
      if (lock.tryLock(30, 300, TimeUnit.SECONDS)) {
          // ... existing resume logic
      }
  } finally {
      lock.unlock();
  }
  ```

- [ ] **Test concurrent resume prevention**
  ```bash
  mvn test -Dtest=MigrationOrchestratorConcurrencyTest#testConcurrentResumePreventedByDistributedLock
  ```

### Phase 3: Retry Logic (Day 3)

- [ ] **Create ResilienceConfig.java**
  ```bash
  # Use provided template
  ```

- [ ] **Add @Retry annotations**
  ```java
  @Retry(name = "migration-operations")
  private MigrationModels.ExtractionResult extractBatch(...) {
      // existing logic
  }
  ```

- [ ] **Test retry behavior**
  ```bash
  mvn test -Dtest=RetryAndResilienceTest#testRetryOnTransientFailure
  ```

### Phase 4: Circuit Breakers (Day 4)

- [ ] **Add @CircuitBreaker to LoadService**
  ```java
  @CircuitBreaker(name = "migration-load", fallbackMethod = "loadFallback")
  @TimeLimiter(name = "migration-load")
  private LoadResult postgresqlBulkLoad(LoadContext context) {
      // existing logic
  }
  ```

- [ ] **Test circuit breaker**
  ```bash
  mvn test -Dtest=RetryAndResilienceTest#testCircuitBreakerOpensAfterThreshold
  ```

### Phase 5: Event Publishing Fallback (Day 5)

- [ ] **Create EventFallbackQueue.java**
- [ ] **Update MigrationEventPublisher.java**
- [ ] **Test event retry**
  ```bash
  mvn test -Dtest=ErrorHandlingTest#testEventPublishingRetry
  ```

---

## 🧪 Testing Guide

### Running All Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify -P integration-tests

# Specific test class
mvn test -Dtest=MigrationOrchestratorConcurrencyTest

# Specific test method
mvn test -Dtest=MigrationOrchestratorConcurrencyTest#testConcurrentResumePreventedByDistributedLock
```

### Manual Testing

```bash
# 1. Create a migration
curl -X POST http://localhost:8080/api/v1/migrations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Test Migration",
    "sourceSystem": "postgresql",
    "targetSystem": "postgresql",
    "migrationType": "FULL_MIGRATION"
  }'

# 2. Get migration status
curl http://localhost:8080/api/v1/migrations/1 \
  -H "Authorization: Bearer $TOKEN"

# 3. Pause migration
curl -X POST http://localhost:8080/api/v1/migrations/1/pause \
  -H "Authorization: Bearer $TOKEN"

# 4. Resume migration
curl -X POST http://localhost:8080/api/v1/migrations/1/resume \
  -H "Authorization: Bearer $TOKEN"

# 5. Check metrics
curl http://localhost:8080/actuator/metrics/migrations.active
curl http://localhost:8080/actuator/metrics/migration.duration
```

---

## 🐛 Troubleshooting

### Issue: "Could not acquire lock"

**Problem:** Distributed lock timeout

**Solution:**
```bash
# Check Redis connection
redis-cli -h localhost -p 6379 ping

# Check existing locks
redis-cli KEYS "migration:lock:*"

# Force release lock (emergency only)
redis-cli DEL "migration:lock:123"
```

### Issue: "Circuit breaker is OPEN"

**Problem:** Too many failures, circuit breaker protecting system

**Solution:**
```bash
# Check circuit breaker state
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# Wait for automatic recovery (60 seconds)
# Or restart the service to reset

# Check database connectivity
psql -h localhost -U jivs_user -d jivs -c "SELECT 1;"
```

### Issue: "Thread pool saturated"

**Problem:** Too many concurrent migrations

**Solution:**
```bash
# Check thread pool metrics
curl http://localhost:8080/actuator/metrics/executor.active | jq

# Increase pool size in application-workflow.yml
jivs:
  migration:
    executor:
      max-pool-size: 100  # Increase from 50

# Restart application
```

### Issue: "Event publishing failed"

**Problem:** WebSocket connection issues

**Solution:**
```bash
# Check fallback queue
curl http://localhost:8080/actuator/metrics/events.fallback.queue.size

# Events will retry automatically via EventFallbackQueue
# Check logs for retry attempts
tail -f logs/jivs-workflow.log | grep "Retrying.*event"
```

---

## 📊 Monitoring During Development

### Local Prometheus + Grafana

```bash
# Start monitoring stack
cd monitoring
docker-compose up -d prometheus grafana

# Access Grafana
open http://localhost:3000

# Default credentials
# Username: admin
# Password: admin

# Import dashboard
# Go to Dashboards > Import
# Upload: monitoring/grafana-dashboard-workflow.json
```

### Key Metrics to Watch

```bash
# Active migrations
curl http://localhost:8080/actuator/metrics/migrations.active

# Failure rate
curl http://localhost:8080/actuator/metrics/migrations.failed

# Thread pool utilization
curl http://localhost:8080/actuator/metrics/executor.active

# Circuit breaker state
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

---

## 🔍 Code Review Checklist

Before submitting PR, verify:

- [ ] All new files compile without errors
- [ ] No unused imports
- [ ] All methods have Javadoc comments
- [ ] All TODOs are resolved or have tickets
- [ ] Unit tests pass (mvn test)
- [ ] Integration tests pass (mvn verify)
- [ ] Code coverage > 80% for new code
- [ ] No Sonar violations
- [ ] Updated CHANGELOG.md
- [ ] Updated API documentation if endpoints changed

---

## 📚 Additional Resources

- **Full Audit Report:** `/WORKFLOW_AUDIT_SUMMARY.md`
- **Architecture Docs:** `/docs/architecture/WORKFLOW_ARCHITECTURE.md`
- **API Docs:** http://localhost:8080/swagger-ui.html
- **Resilience4j Docs:** https://resilience4j.readme.io/
- **Redisson Docs:** https://redisson.org/

---

## 🆘 Getting Help

- **Slack:** #jivs-workflow-dev
- **Email:** jivs-dev-team@company.com
- **On-call:** Check PagerDuty rotation

---

**Happy Coding! 🚀**
