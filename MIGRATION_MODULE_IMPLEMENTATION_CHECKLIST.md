# Migration Module Implementation Checklist

**Purpose**: Step-by-step guide to implement all fixes from the migration module audit
**Audience**: Backend developers, DevOps engineers
**Prerequisites**: Review MIGRATION_MODULE_AUDIT_REPORT.md and MIGRATION_MODULE_FIX_GUIDE.md
**Estimated Time**: 8 weeks (2 engineers)

---

## Quick Navigation

- [Phase 1: Critical Fixes (Week 1-3)](#phase-1-critical-fixes)
- [Phase 2: High-Priority Fixes (Week 4-5)](#phase-2-high-priority-fixes)
- [Phase 3: Medium-Priority Fixes (Week 6-7)](#phase-3-medium-priority-fixes)
- [Phase 4: Testing & Validation (Week 8)](#phase-4-testing--validation)
- [Rollback Procedures](#rollback-procedures)

---

## Phase 1: Critical Fixes (Week 1-3)

### Week 1: Data Persistence (32 hours)

#### ✅ Task 1.1: Apply Database Migration (4 hours)

**Files Involved**:
- `backend/src/main/resources/db/migration/V111__Add_migration_execution_fields.sql`

**Steps**:
```bash
# 1. Review the migration script
cat backend/src/main/resources/db/migration/V111__Add_migration_execution_fields.sql

# 2. Backup production database (CRITICAL!)
pg_dump -U jivs_user -d jivs > backup_before_v111_$(date +%Y%m%d_%H%M%S).sql

# 3. Test migration in dev environment first
cd backend
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/jivs_dev

# 4. Verify columns added
psql -U jivs_user -d jivs_dev -c "\d+ migration_projects"

# Expected: 40+ new columns including:
# - migration_phase, start_time, completion_time
# - batch_size, parallelism, retry_attempts
# - source_analysis (jsonb), target_analysis (jsonb)
# - total_records, processed_records, etc.

# 5. Check indexes created
psql -U jivs_user -d jivs_dev -c "\di+ idx_migration_*"

# Expected: 8+ indexes including:
# - idx_migration_phase
# - idx_migration_start_time
# - idx_migration_active

# 6. Apply to production (during maintenance window)
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://prod-db:5432/jivs \
    -Dflyway.user=jivs_user \
    -Dflyway.password=$PROD_DB_PASSWORD
```

**Validation**:
```sql
-- Check column count
SELECT COUNT(*) FROM information_schema.columns
WHERE table_name = 'migration_projects';
-- Should return ~60+ columns

-- Check materialized view
SELECT COUNT(*) FROM migration_analytics;
-- Should return row count

-- Check audit trigger
SELECT tgname FROM pg_trigger WHERE tgrelid = 'migration_projects'::regclass;
-- Should show: migration_state_change_trigger
```

**Rollback Plan** (if issues):
```sql
-- See V111 migration script for detailed rollback instructions
DROP TRIGGER migration_state_change_trigger ON migration_projects;
DROP FUNCTION migration_audit_trigger();
DROP TABLE migration_audit_log;
-- ... (full rollback in V111 footer)
```

---

#### ✅ Task 1.2: Refactor Migration Entity (8 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/domain/migration/RefactoredMigration.java` (NEW)
- `backend/src/main/java/com/jivs/platform/domain/migration/Migration.java` (REPLACE or MERGE)

**Steps**:

1. **Add Hypersistence dependency** (for JSONB support):
```xml
<!-- backend/pom.xml -->
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
    <version>3.7.0</version>
</dependency>
```

2. **Option A: Replace Migration.java** (Clean slate):
```bash
# Backup original
cp backend/src/main/java/com/jivs/platform/domain/migration/Migration.java \
   backend/src/main/java/com/jivs/platform/domain/migration/Migration.java.bak

# Replace with refactored version
cp backend/src/main/java/com/jivs/platform/domain/migration/RefactoredMigration.java \
   backend/src/main/java/com/jivs/platform/domain/migration/Migration.java
```

3. **Option B: Merge changes** (Preserve existing code):
   - Compare `Migration.java` with `RefactoredMigration.java`
   - Remove all `@Transient` annotations from the 18+ fields
   - Add `@Column` mappings for all fields
   - Add `@Type(JsonBinaryType.class)` for complex objects
   - Add helper methods: `markStarted()`, `markCompleted()`, etc.

4. **Update imports**:
```java
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
```

5. **Compile and check for errors**:
```bash
cd backend
mvn clean compile
```

**Key Changes**:
```java
// BEFORE (BROKEN):
@Transient
private LocalDateTime startTime;

@Transient
private Integer totalRecords;

@Transient
private SourceAnalysis sourceAnalysis;

// AFTER (FIXED):
@Column(name = "start_time")
private LocalDateTime startTime;

@Column(name = "total_records")
private Integer totalRecords;

@Type(JsonBinaryType.class)
@Column(name = "source_analysis", columnDefinition = "jsonb")
private SourceAnalysis sourceAnalysis;
```

**Validation**:
```bash
# Run unit tests
mvn test -Dtest=MigrationOrchestratorFixedTest

# Check entity can be persisted
mvn test -Dtest=MigrationRepositoryTest#testPersistAllFields
```

---

#### ✅ Task 1.3: Implement Executor Configuration (6 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/config/MigrationExecutorConfig.java` (NEW)
- `backend/src/main/resources/application.yml` (UPDATE)

**Steps**:

1. **Add configuration file**:
```bash
# Copy the provided config
cp MigrationExecutorConfig.java backend/src/main/java/com/jivs/platform/config/
```

2. **Update application.yml**:
```yaml
# backend/src/main/resources/application.yml
jivs:
  migration:
    executor:
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 100
      thread-name-prefix: migration-executor-
      await-termination-seconds: 60
    validation:
      timeout-seconds: 300
      max-errors: 1000
    load:
      batch-size: 1000
      max-retry-attempts: 3
      timeout-seconds: 600
      parallelism: 4
```

3. **Update MigrationOrchestrator.java**:
```java
// BEFORE (BROKEN):
private final ExecutorService executorService = Executors.newFixedThreadPool(4);

// AFTER (FIXED):
@Autowired
@Qualifier("migrationExecutor")
private Executor migrationExecutor;

// Remove @PreDestroy with executorService.shutdown()
// (now handled by MigrationExecutorConfig)
```

4. **Update LoadService.java**:
```java
// BEFORE (BROKEN):
private final ExecutorService executorService = Executors.newFixedThreadPool(8);

// AFTER (FIXED):
@Autowired
@Qualifier("loadExecutor")
private Executor loadExecutor;
```

5. **Update all async calls**:
```java
// BEFORE:
CompletableFuture.supplyAsync(() -> { ... }, executorService)

// AFTER:
CompletableFuture.supplyAsync(() -> { ... }, migrationExecutor)
```

**Validation**:
```bash
# Start application
mvn spring-boot:run

# Check logs for executor initialization
grep "Initializing Migration Executor" logs/application.log
# Should show: "Initializing Migration Executor with corePoolSize=4, maxPoolSize=8"

# Trigger a migration and check thread names
grep "migration-executor-" logs/application.log
# Should show threads with correct prefix

# Stop application and verify graceful shutdown
grep "Shutting down Migration Executors" logs/application.log
# Should show: "Migration Executor shut down gracefully"
```

---

#### ✅ Task 1.4: Implement Event-Driven Async (8 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/service/migration/MigrationEventListener.java` (NEW)
- `backend/src/main/java/com/jivs/platform/service/migration/MigrationOrchestrator.java` (UPDATE)

**Steps**:

1. **Add event listener**:
```bash
cp MigrationEventListener.java backend/src/main/java/com/jivs/platform/service/migration/
```

2. **Enable async support**:
```java
// backend/src/main/java/com/jivs/platform/JivsApplication.java
@SpringBootApplication
@EnableAsync  // Add this annotation
public class JivsApplication {
    public static void main(String[] args) {
        SpringApplication.run(JivsApplication.class, args);
    }
}
```

3. **Refactor MigrationOrchestrator.executeMigration()**:

**BEFORE (BROKEN)**:
```java
@Async
@Transactional
public CompletableFuture<Migration> executeMigration(Long migrationId) {
    // This causes transaction to close before async work completes!
    Migration migration = migrationRepository.findById(migrationId).orElseThrow();
    // ... execute phases
    return CompletableFuture.completedFuture(migration);
}
```

**AFTER (FIXED)**:
```java
// Remove @Async and @Transactional from executeMigration

// New method: Synchronous initialization
@Transactional
public Migration initiateMigration(MigrationRequest request) {
    Migration migration = new Migration();
    migration.setName(request.getName());
    migration.setSourceSystem(request.getSourceSystem());
    migration.setTargetSystem(request.getTargetSystem());
    migration.setStatus(MigrationStatus.INITIALIZED);

    // CRITICAL: Save before publishing event
    migration = migrationRepository.save(migration);

    // Publish event (will fire AFTER transaction commits)
    applicationEventPublisher.publishEvent(
        new MigrationExecutionEvent(this, migration.getId())
    );

    return migration;
}

// Add new method for phase execution (called by event listener)
public Migration executeAllPhases(Migration migration) {
    try {
        migration = executePlanningPhase(migration);
        migration = executeExtractionPhase(migration);
        migration = executeTransformationPhase(migration);
        migration = executeValidationPhase(migration);
        migration = executeLoadPhase(migration);
        migration = executeVerificationPhase(migration);
        migration = executeCleanupPhase(migration);
        migration.markCompleted();
        return migrationRepository.save(migration);
    } catch (Exception e) {
        migration.markFailed(e.getMessage(), getStackTrace(e));
        return migrationRepository.save(migration);
    }
}
```

4. **Add ApplicationEventPublisher**:
```java
@Service
public class MigrationOrchestrator {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;  // Add this

    // ... rest of code
}
```

5. **Update controller**:
```java
// BEFORE:
CompletableFuture<Migration> future = orchestrator.executeMigration(id);
return ResponseEntity.accepted().body(future.get());  // Blocks!

// AFTER:
Migration migration = orchestrator.initiateMigration(request);
return ResponseEntity.accepted()
    .header("Location", "/api/migrations/" + migration.getId())
    .body(migration);  // Returns immediately
```

**Validation**:
```bash
# Start application
mvn spring-boot:run

# Test migration creation
curl -X POST http://localhost:8080/api/migrations \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Migration","sourceSystem":"postgresql","targetSystem":"postgresql"}'

# Response should return immediately with status=INITIALIZED
# Check logs for event processing:
grep "Migration execution event received" logs/application.log
# Should show: "Migration execution event received for migrationId=123 (AFTER_COMMIT)"

# Verify migration status changes asynchronously
curl http://localhost:8080/api/migrations/123
# Status should progress: INITIALIZED -> IN_PROGRESS -> COMPLETED
```

---

#### ✅ Task 1.5: Update All Phase Methods to Save State (6 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/service/migration/MigrationOrchestrator.java`

**Required Changes**:

Each phase method must call `migrationRepository.save()` to persist state:

```java
private Migration executePlanningPhase(Migration migration) {
    log.info("Starting PLANNING phase for migration {}", migration.getId());

    migration.setCurrentPhase(MigrationPhase.PLANNING);
    migration = migrationRepository.save(migration);  // ✅ ADD THIS

    try {
        // Analyze source
        SourceAnalysis sourceAnalysis = analyzeSource(migration);
        migration.setSourceAnalysis(sourceAnalysis);

        // Analyze target
        TargetAnalysis targetAnalysis = analyzeTarget(migration);
        migration.setTargetAnalysis(targetAnalysis);

        // Generate plan
        MigrationPlan plan = generatePlan(migration, sourceAnalysis, targetAnalysis);
        migration.setMigrationPlan(plan);

        // Estimate resources
        ResourceEstimation estimation = estimateResources(plan);
        migration.setResourceEstimation(estimation);

        log.info("PLANNING phase completed for migration {}", migration.getId());
        return migrationRepository.save(migration);  // ✅ SAVE AT END

    } catch (Exception e) {
        log.error("PLANNING phase failed for migration {}", migration.getId(), e);
        throw new MigrationException("Planning failed", e);
    }
}
```

**Apply to all phases**:
- `executePlanningPhase()` ✅
- `executeExtractionPhase()` ✅
- `executeTransformationPhase()` ✅
- `executeValidationPhase()` ✅
- `executeLoadPhase()` ✅
- `executeVerificationPhase()` ✅
- `executeCleanupPhase()` ✅

**Pattern to follow**:
```java
private Migration executeXXXPhase(Migration migration) {
    migration.setCurrentPhase(MigrationPhase.XXX);
    migration = migrationRepository.save(migration);  // ← Save on entry

    try {
        // ... phase logic ...

        return migrationRepository.save(migration);  // ← Save on success
    } catch (Exception e) {
        migration.markFailed(e.getMessage(), stackTrace);
        return migrationRepository.save(migration);  // ← Save on failure
    }
}
```

**Validation**:
```bash
# Run integration test
mvn test -Dtest=MigrationLifecycleIntegrationTest#testCompleteMigrationLifecycle

# Check that migration state persists between phases
# (The test will restart the application mid-migration)
```

---

### Week 2: Security Hardening (36 hours)

#### ✅ Task 2.1: Implement SQL Injection Prevention (8 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/service/migration/LoadService.java`

**Steps**:

1. **Add validation method**:
```java
// Add to LoadService.java

private static final Pattern SQL_IDENTIFIER_PATTERN =
    Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

private static final Set<String> SQL_RESERVED_KEYWORDS = Set.of(
    "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
    "TABLE", "WHERE", "FROM", "JOIN", "UNION", "ORDER", "GROUP", "HAVING",
    "INTO", "VALUES", "SET", "AND", "OR", "NOT", "NULL", "IS", "AS"
);

/**
 * ✅ FIX: Validate SQL identifiers to prevent injection
 *
 * Prevents attacks like:
 * - "users; DROP TABLE users--"
 * - "users' OR '1'='1"
 * - "users/*comment* /UNION SELECT password"
 *
 * @param identifier Table name, column name, or other SQL identifier
 * @param type Identifier type for error message (e.g., "Table", "Column")
 * @throws IllegalArgumentException if identifier is invalid
 */
public void validateSqlIdentifier(String identifier, String type) {
    // Null/empty check
    if (identifier == null || identifier.trim().isEmpty()) {
        throw new IllegalArgumentException(type + " name cannot be null or empty");
    }

    String trimmed = identifier.trim();

    // Check format (alphanumeric + underscore, must start with letter or underscore)
    if (!SQL_IDENTIFIER_PATTERN.matcher(trimmed).matches()) {
        throw new IllegalArgumentException(
            "Invalid " + type + " name format: " + trimmed +
            " (must match ^[a-zA-Z_][a-zA-Z0-9_]*$)"
        );
    }

    // Check reserved keywords
    if (SQL_RESERVED_KEYWORDS.contains(trimmed.toUpperCase())) {
        throw new IllegalArgumentException(
            type + " name cannot be a reserved SQL keyword: " + trimmed
        );
    }

    // Optional: Check max length (PostgreSQL limit is 63 chars)
    if (trimmed.length() > 63) {
        throw new IllegalArgumentException(
            type + " name exceeds maximum length of 63 characters: " + trimmed
        );
    }
}
```

2. **Update batchLoad method**:
```java
public LoadResult batchLoad(LoadContext context) {
    // ✅ Validate all identifiers BEFORE using them
    validateSqlIdentifier(context.getTargetTable(), "Table");

    for (String column : context.getColumns()) {
        validateSqlIdentifier(column, "Column");
    }

    for (String keyColumn : context.getKeyColumns()) {
        validateSqlIdentifier(keyColumn, "Key column");
    }

    // ... rest of load logic
}
```

3. **Update SQL building methods**:
```java
// Identifiers are now validated, but still use parameterized queries for values

public String buildPostgresUpsertSql(String table, List<String> columns,
                                     List<String> keyColumns) {
    // Validation already done by caller

    String columnList = String.join(", ", columns);
    String placeholders = columns.stream()
        .map(c -> "?")
        .collect(Collectors.joining(", "));

    String sql = String.format(
        "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (%s) DO UPDATE SET %s",
        table,  // Already validated
        columnList,  // Already validated
        placeholders,  // Parameterized
        String.join(", ", keyColumns),  // Already validated
        columns.stream()
            .filter(c -> !keyColumns.contains(c))
            .map(c -> c + " = EXCLUDED." + c)
            .collect(Collectors.joining(", "))
    );

    return sql;
}
```

**Validation**:
```bash
# Run security tests
mvn test -Dtest=LoadServiceSecurityTest

# Should pass all 18 security tests:
# ✅ SEC-1: Should reject SQL injection in table name
# ✅ SEC-2: Should reject SQL injection in column name
# ✅ SEC-3: Should reject various SQL injection patterns
# ... (all 18 tests)
```

---

#### ✅ Task 2.2: Restrict CORS Origins (2 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/controller/MigrationController.java`
- `backend/src/main/resources/application.yml`

**Steps**:

1. **Update application.yml**:
```yaml
jivs:
  security:
    cors:
      allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3001,http://localhost:3000}
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      allowed-headers: "*"
      allow-credentials: true
      max-age: 3600
```

2. **Update controller**:
```java
// BEFORE (INSECURE):
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/migrations")
public class MigrationController {
    // ...
}

// AFTER (SECURE):
@RestController
@RequestMapping("/api/migrations")
public class MigrationController {
    // Remove @CrossOrigin annotation
    // CORS now handled by WebSecurityConfig
}
```

3. **Create CORS configuration**:
```java
// backend/src/main/java/com/jivs/platform/config/WebSecurityConfig.java

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Value("${jivs.security.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

**Validation**:
```bash
# Test CORS from allowed origin
curl -H "Origin: http://localhost:3001" \
     -H "Access-Control-Request-Method: POST" \
     -X OPTIONS http://localhost:8080/api/migrations

# Should return:
# Access-Control-Allow-Origin: http://localhost:3001
# Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS

# Test CORS from disallowed origin
curl -H "Origin: http://evil.com" \
     -H "Access-Control-Request-Method: POST" \
     -X OPTIONS http://localhost:8080/api/migrations

# Should NOT return Access-Control-Allow-Origin header
```

---

#### ✅ Task 2.3: Add Input Validation DTOs (12 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/dto/MigrationCreateRequest.java` (NEW)
- `backend/src/main/java/com/jivs/platform/dto/MigrationUpdateRequest.java` (NEW)
- `backend/src/main/java/com/jivs/platform/controller/MigrationController.java` (UPDATE)

**Steps**:

1. **Create MigrationCreateRequest DTO**:
```java
package com.jivs.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MigrationCreateRequest {

    @NotBlank(message = "Migration name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 _-]+$",
             message = "Name can only contain letters, numbers, spaces, hyphens, and underscores")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Source system is required")
    @Pattern(regexp = "^(postgresql|mysql|oracle|sqlserver|mongodb|sap|csv|json|xml)$",
             message = "Invalid source system type")
    private String sourceSystem;

    @NotBlank(message = "Target system is required")
    @Pattern(regexp = "^(postgresql|mysql|oracle|sqlserver|mongodb)$",
             message = "Invalid target system type")
    private String targetSystem;

    @Pattern(regexp = "^jdbc:[a-z]+://[a-zA-Z0-9.-]+(:\\d+)?/[a-zA-Z0-9_]+.*$",
             message = "Invalid source connection string format")
    private String sourceConnectionString;

    @Pattern(regexp = "^jdbc:[a-z]+://[a-zA-Z0-9.-]+(:\\d+)?/[a-zA-Z0-9_]+.*$",
             message = "Invalid target connection string format")
    private String targetConnectionString;

    @Min(value = 1, message = "Batch size must be at least 1")
    @Max(value = 10000, message = "Batch size cannot exceed 10,000")
    private Integer batchSize = 1000;

    @Min(value = 1, message = "Parallelism must be at least 1")
    @Max(value = 20, message = "Parallelism cannot exceed 20")
    private Integer parallelism = 4;

    @Min(value = 0, message = "Retry attempts cannot be negative")
    @Max(value = 10, message = "Retry attempts cannot exceed 10")
    private Integer retryAttempts = 3;

    private Boolean strictValidation = false;
    private Boolean rollbackEnabled = true;
    private Boolean rollbackOnCancel = false;
    private Boolean archiveEnabled = false;
}
```

2. **Update controller to use DTOs**:
```java
// BEFORE (INSECURE):
@PostMapping
public ResponseEntity<Migration> createMigration(@RequestBody Map<String, Object> request) {
    // No validation!
    String name = (String) request.get("name");
    String sourceSystem = (String) request.get("sourceSystem");
    // ... potential NPE, type mismatches, injection
}

// AFTER (SECURE):
@PostMapping
public ResponseEntity<Migration> createMigration(
        @Valid @RequestBody MigrationCreateRequest request) {
    // Validation happens automatically via @Valid
    Migration migration = orchestrator.initiateMigration(request);
    return ResponseEntity.accepted()
        .header("Location", "/api/migrations/" + migration.getId())
        .body(migration);
}
```

3. **Add global exception handler for validation**:
```java
package com.jivs.platform.exception;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse response = new ErrorResponse(
            "VALIDATION_ERROR",
            "Invalid request parameters",
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        ErrorResponse response = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            null
        );

        return ResponseEntity.badRequest().body(response);
    }
}
```

**Validation**:
```bash
# Test with invalid request (empty name)
curl -X POST http://localhost:8080/api/migrations \
  -H "Content-Type: application/json" \
  -d '{"name":"","sourceSystem":"postgresql","targetSystem":"postgresql"}'

# Should return 400 Bad Request:
# {
#   "code": "VALIDATION_ERROR",
#   "message": "Invalid request parameters",
#   "errors": {
#     "name": "Migration name is required"
#   }
# }

# Test with invalid source system
curl -X POST http://localhost:8080/api/migrations \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","sourceSystem":"INVALID","targetSystem":"postgresql"}'

# Should return 400 Bad Request:
# {
#   "code": "VALIDATION_ERROR",
#   "message": "Invalid request parameters",
#   "errors": {
#     "sourceSystem": "Invalid source system type"
#   }
# }

# Test with valid request
curl -X POST http://localhost:8080/api/migrations \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Production User Migration",
    "sourceSystem":"postgresql",
    "targetSystem":"postgresql",
    "batchSize":1000
  }'

# Should return 202 Accepted with Migration object
```

---

### Week 3: Resource Management (44 hours)

#### ✅ Task 3.1: Implement Missing Exception Types (6 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/exception/MigrationException.java` (NEW)
- `backend/src/main/java/com/jivs/platform/exception/ValidationException.java` (NEW)
- `backend/src/main/java/com/jivs/platform/exception/LoadException.java` (NEW)

**Steps**:

1. **Create base MigrationException**:
```java
package com.jivs.platform.exception;

public class MigrationException extends RuntimeException {
    private final String migrationId;
    private final String phase;

    public MigrationException(String message, String migrationId, String phase) {
        super(message);
        this.migrationId = migrationId;
        this.phase = phase;
    }

    public MigrationException(String message, Throwable cause, String migrationId, String phase) {
        super(message, cause);
        this.migrationId = migrationId;
        this.phase = phase;
    }

    public String getMigrationId() { return migrationId; }
    public String getPhase() { return phase; }
}
```

2. **Create ValidationException**:
```java
package com.jivs.platform.exception;

import java.util.List;

public class ValidationException extends MigrationException {
    private final List<ValidationError> errors;

    public ValidationException(String message, List<ValidationError> errors,
                              String migrationId) {
        super(message, migrationId, "VALIDATION");
        this.errors = errors;
    }

    public List<ValidationError> getErrors() { return errors; }

    public static class ValidationError {
        private final String field;
        private final String rule;
        private final String message;
        private final Object actualValue;

        public ValidationError(String field, String rule, String message, Object actualValue) {
            this.field = field;
            this.rule = rule;
            this.message = message;
            this.actualValue = actualValue;
        }

        // Getters omitted for brevity
    }
}
```

3. **Update GlobalExceptionHandler**:
```java
@ExceptionHandler(MigrationException.class)
public ResponseEntity<ErrorResponse> handleMigrationException(MigrationException ex) {
    ErrorResponse response = new ErrorResponse(
        "MIGRATION_ERROR",
        ex.getMessage(),
        Map.of(
            "migrationId", ex.getMigrationId(),
            "phase", ex.getPhase()
        )
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
}

@ExceptionHandler(ValidationException.class)
public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
    Map<String, Object> details = new HashMap<>();
    details.put("migrationId", ex.getMigrationId());
    details.put("validationErrors", ex.getErrors());

    ErrorResponse response = new ErrorResponse(
        "VALIDATION_FAILED",
        ex.getMessage(),
        details
    );
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
}
```

**Validation**:
```bash
# Test migration exception handling
curl -X POST http://localhost:8080/api/migrations/999/execute
# Should return 500 with proper error structure

# Test validation exception
curl -X POST http://localhost:8080/api/migrations \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","sourceSystem":"invalid"}'
# Should return 422 with validation errors
```

---

#### ✅ Task 3.2: Add Connection Pool Management (8 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/service/migration/ConnectionPoolManager.java` (NEW)
- `backend/src/main/resources/application.yml` (UPDATE)

**Steps**:

1. **Create ConnectionPoolManager**:
```java
package com.jivs.platform.service.migration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

/**
 * ✅ FIX: Manages dynamic database connections for migrations
 *
 * Fixes Issue #15: Connection not closed in try-with-resources
 *
 * This service:
 * 1. Creates connection pools on-demand for source/target systems
 * 2. Reuses pools for same connection strings
 * 3. Properly closes all pools on shutdown
 * 4. Monitors pool health
 */
@Slf4j
@Service
public class ConnectionPoolManager {

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    /**
     * Get or create connection pool for given connection string
     */
    public DataSource getDataSource(String connectionString, String username, String password) {
        String poolKey = connectionString + ":" + username;

        return pools.computeIfAbsent(poolKey, key -> {
            log.info("Creating new connection pool for {}", connectionString);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(connectionString);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setPoolName("migration-pool-" + System.currentTimeMillis());

            return new HikariDataSource(config);
        });
    }

    /**
     * Close specific connection pool
     */
    public void closePool(String connectionString, String username) {
        String poolKey = connectionString + ":" + username;
        HikariDataSource pool = pools.remove(poolKey);

        if (pool != null && !pool.isClosed()) {
            log.info("Closing connection pool for {}", connectionString);
            pool.close();
        }
    }

    /**
     * Cleanup all pools on shutdown
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down all connection pools ({})", pools.size());

        pools.forEach((key, pool) -> {
            try {
                if (!pool.isClosed()) {
                    pool.close();
                }
            } catch (Exception e) {
                log.error("Error closing connection pool {}", key, e);
            }
        });

        pools.clear();
        log.info("All connection pools closed");
    }

    /**
     * Get pool statistics for monitoring
     */
    public Map<String, PoolStats> getPoolStats() {
        Map<String, PoolStats> stats = new HashMap<>();

        pools.forEach((key, pool) -> {
            stats.put(key, new PoolStats(
                pool.getHikariPoolMXBean().getActiveConnections(),
                pool.getHikariPoolMXBean().getIdleConnections(),
                pool.getHikariPoolMXBean().getTotalConnections(),
                pool.getHikariPoolMXBean().getThreadsAwaitingConnection()
            ));
        });

        return stats;
    }

    public record PoolStats(int active, int idle, int total, int waiting) {}
}
```

2. **Update LoadService to use ConnectionPoolManager**:
```java
@Service
public class LoadService {

    @Autowired
    private ConnectionPoolManager connectionPoolManager;

    public LoadResult batchLoad(LoadContext context) {
        // Get managed DataSource instead of creating connection directly
        DataSource dataSource = connectionPoolManager.getDataSource(
            context.getTargetConnectionString(),
            context.getTargetUsername(),
            context.getTargetPassword()
        );

        try (Connection conn = dataSource.getConnection()) {
            // Use connection (pool will manage it)
            // ...
        } catch (SQLException e) {
            throw new LoadException("Failed to load data", e);
        }
        // Pool stays open for reuse
    }
}
```

**Validation**:
```bash
# Start application
mvn spring-boot:run

# Trigger migration (creates pools)
curl -X POST http://localhost:8080/api/migrations \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","sourceSystem":"postgresql","targetSystem":"postgresql"}'

# Check pool stats
curl http://localhost:8080/actuator/metrics/hikari.connections.active

# Stop application and verify all pools closed
grep "All connection pools closed" logs/application.log
```

---

#### ✅ Task 3.3: Add Proper Logging with MDC (6 hours)

**Files Involved**:
- `backend/src/main/resources/logback-spring.xml` (UPDATE)
- All migration service classes (UPDATE)

**Steps**:

1. **Update logback configuration**:
```xml
<!-- backend/src/main/resources/logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [migrationId=%X{migrationId}] [userId=%X{userId}] - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/migration.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [migrationId=%X{migrationId}] [userId=%X{userId}] [phase=%X{phase}] - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/migration-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <logger name="com.jivs.platform.service.migration" level="DEBUG" additivity="false">
        <appender-ref ref="FILE" />
        <appender-ref ref="CONSOLE" />
    </logger>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

2. **Add MDC filter**:
```java
package com.jivs.platform.filter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // Add request ID
            String requestId = UUID.randomUUID().toString();
            MDC.put("requestId", requestId);

            // Add user ID from security context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                MDC.put("userId", auth.getName());
            }

            // Add migration ID from path if present
            String path = httpRequest.getRequestURI();
            if (path.contains("/migrations/")) {
                String[] parts = path.split("/");
                for (int i = 0; i < parts.length - 1; i++) {
                    if ("migrations".equals(parts[i]) && i + 1 < parts.length) {
                        MDC.put("migrationId", parts[i + 1]);
                        break;
                    }
                }
            }

            chain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }
}
```

**Validation**:
```bash
# Trigger migration and check logs
curl -X POST http://localhost:8080/api/migrations/123/execute

# Check log file for MDC context
grep "migrationId=123" logs/migration.log
# Should show: [migrationId=123] [userId=admin] [phase=EXTRACTION]
```

---

#### ✅ Task 3.4: Add Metrics and Monitoring (10 hours)

**Files Involved**:
- `backend/pom.xml` (UPDATE - add Micrometer)
- `backend/src/main/java/com/jivs/platform/service/migration/MigrationMetricsService.java` (NEW)

**Steps**:

1. **Add Micrometer dependencies**:
```xml
<!-- backend/pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

2. **Create MigrationMetricsService**:
```java
package com.jivs.platform.service.migration;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Service;

@Service
public class MigrationMetricsService {

    private final Counter migrationsStarted;
    private final Counter migrationsCompleted;
    private final Counter migrationsFailed;
    private final Timer migrationDuration;
    private final Gauge activeMigrations;

    public MigrationMetricsService(MeterRegistry registry) {
        this.migrationsStarted = Counter.builder("jivs.migrations.started")
            .description("Total migrations started")
            .register(registry);

        this.migrationsCompleted = Counter.builder("jivs.migrations.completed")
            .description("Total migrations completed successfully")
            .register(registry);

        this.migrationsFailed = Counter.builder("jivs.migrations.failed")
            .description("Total migrations failed")
            .tag("reason", "error")
            .register(registry);

        this.migrationDuration = Timer.builder("jivs.migrations.duration")
            .description("Migration execution time")
            .register(registry);

        // Active migrations gauge updated by orchestrator
        this.activeMigrations = Gauge.builder("jivs.migrations.active", () -> {
            // This will be updated by querying repository
            return 0; // Placeholder
        }).register(registry);
    }

    public void recordMigrationStarted() {
        migrationsStarted.increment();
    }

    public void recordMigrationCompleted(long durationMillis) {
        migrationsCompleted.increment();
        migrationDuration.record(durationMillis, TimeUnit.MILLISECONDS);
    }

    public void recordMigrationFailed(String reason) {
        migrationsFailed.increment();
    }
}
```

3. **Update MigrationOrchestrator to record metrics**:
```java
@Service
public class MigrationOrchestrator {

    @Autowired
    private MigrationMetricsService metrics;

    public Migration executeAllPhases(Migration migration) {
        long startTime = System.currentTimeMillis();
        metrics.recordMigrationStarted();

        try {
            // Execute phases...
            migration.markCompleted();

            long duration = System.currentTimeMillis() - startTime;
            metrics.recordMigrationCompleted(duration);

            return migration;
        } catch (Exception e) {
            metrics.recordMigrationFailed(e.getClass().getSimpleName());
            throw e;
        }
    }
}
```

**Validation**:
```bash
# Start application
mvn spring-boot:run

# Check Prometheus metrics endpoint
curl http://localhost:8080/actuator/prometheus | grep jivs_migrations

# Expected output:
# jivs_migrations_started_total 5.0
# jivs_migrations_completed_total 4.0
# jivs_migrations_failed_total{reason="ValidationException"} 1.0
# jivs_migrations_duration_seconds_sum 120.5
```

---

#### ✅ Task 3.5: Add Health Checks (6 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/health/MigrationHealthIndicator.java` (NEW)

**Steps**:

1. **Create custom health indicator**:
```java
package com.jivs.platform.health;

import com.jivs.platform.config.MigrationExecutorConfig;
import com.jivs.platform.service.migration.ConnectionPoolManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MigrationHealthIndicator implements HealthIndicator {

    @Autowired
    private MigrationExecutorConfig.MigrationExecutorMetrics executorMetrics;

    @Autowired
    private ConnectionPoolManager connectionPoolManager;

    @Override
    public Health health() {
        try {
            // Check executor health
            var migrationStats = executorMetrics.getMigrationExecutorStats();
            var loadStats = executorMetrics.getLoadExecutorStats();

            // Check if thread pools are healthy
            boolean executorHealthy = migrationStats.activeThreads() < migrationStats.maxPoolSize();
            boolean loadHealthy = loadStats.activeThreads() < loadStats.maxPoolSize();

            // Check connection pools
            var poolStats = connectionPoolManager.getPoolStats();
            boolean poolsHealthy = poolStats.values().stream()
                .allMatch(stats -> stats.waiting() == 0);

            if (executorHealthy && loadHealthy && poolsHealthy) {
                return Health.up()
                    .withDetail("migrationExecutor", migrationStats)
                    .withDetail("loadExecutor", loadStats)
                    .withDetail("connectionPools", poolStats.size())
                    .build();
            } else {
                return Health.down()
                    .withDetail("executorHealthy", executorHealthy)
                    .withDetail("loadHealthy", loadHealthy)
                    .withDetail("poolsHealthy", poolsHealthy)
                    .build();
            }

        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

**Validation**:
```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected output:
# {
#   "status": "UP",
#   "components": {
#     "migration": {
#       "status": "UP",
#       "details": {
#         "migrationExecutor": {...},
#         "loadExecutor": {...},
#         "connectionPools": 2
#       }
#     }
#   }
# }
```

---

#### ✅ Task 3.6: Implement Graceful Shutdown (8 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/config/GracefulShutdownConfig.java` (NEW)
- `backend/src/main/resources/application.yml` (UPDATE)

**Steps**:

1. **Enable graceful shutdown in application.yml**:
```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 60s
```

2. **Create shutdown handler**:
```java
package com.jivs.platform.config;

@Configuration
public class GracefulShutdownConfig {

    @Autowired
    private MigrationRepository migrationRepository;

    @PreDestroy
    public void onShutdown() {
        log.info("Application shutdown initiated - pausing active migrations");

        // Find all in-progress migrations
        List<Migration> activeMigrations = migrationRepository
            .findByStatus(MigrationStatus.IN_PROGRESS);

        for (Migration migration : activeMigrations) {
            try {
                migration.markPaused();
                migration.setUpdatedBy("system-shutdown");
                migrationRepository.save(migration);

                log.info("Migration {} paused for graceful shutdown", migration.getId());
            } catch (Exception e) {
                log.error("Failed to pause migration {}", migration.getId(), e);
            }
        }

        log.info("Graceful shutdown: {} migrations paused", activeMigrations.size());
    }
}
```

**Validation**:
```bash
# Start migration
curl -X POST http://localhost:8080/api/migrations/123/execute

# Stop application gracefully (SIGTERM not SIGKILL)
kill -15 <PID>

# Check logs
grep "migrations paused" logs/application.log
# Should show: "Graceful shutdown: 1 migrations paused"

# Restart and verify migration can be resumed
mvn spring-boot:run
curl -X POST http://localhost:8080/api/migrations/123/resume
```

---

## Phase 2: High-Priority Fixes (Week 4-5)

### Week 4: Error Handling & Validation (40 hours)

#### ✅ Task 4.1: Refactor Validation Service (12 hours)

**Issue**: Large switch statement, business rules stubbed out

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/service/migration/ValidationService.java`

**Steps**:

1. **Replace switch with strategy pattern**:
```java
// Create interface
public interface ValidationRule {
    ValidationResult validate(Object value, RuleConfig config);
}

// Create implementations
@Component("notNullRule")
public class NotNullValidationRule implements ValidationRule {
    @Override
    public ValidationResult validate(Object value, RuleConfig config) {
        if (value == null) {
            return ValidationResult.failed("Value cannot be null");
        }
        return ValidationResult.success();
    }
}

// Update ValidationService
@Service
public class ValidationService {

    @Autowired
    private Map<String, ValidationRule> validationRules;  // Spring auto-wires by bean name

    public ValidationResult validate(Object value, String ruleType, RuleConfig config) {
        ValidationRule rule = validationRules.get(ruleType + "Rule");
        if (rule == null) {
            throw new IllegalArgumentException("Unknown rule type: " + ruleType);
        }
        return rule.validate(value, config);
    }
}
```

2. **Implement business rules**:
```java
@Component("businessRule")
public class BusinessValidationRule implements ValidationRule {
    @Override
    public ValidationResult validate(Object value, RuleConfig config) {
        // Implement actual business logic
        String expression = config.getExpression();

        // Use expression evaluator (e.g., Spring Expression Language)
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(expression);

        Boolean result = exp.getValue(new StandardEvaluationContext(value), Boolean.class);

        if (Boolean.FALSE.equals(result)) {
            return ValidationResult.failed("Business rule violated: " + expression);
        }

        return ValidationResult.success();
    }
}
```

**Validation**:
```bash
mvn test -Dtest=ValidationServiceTest
# All tests should pass with new strategy pattern
```

---

#### ✅ Task 4.2: Add Retry Logic with Exponential Backoff (8 hours)

**Files Involved**:
- `backend/pom.xml` (ADD Spring Retry)
- `backend/src/main/java/com/jivs/platform/service/migration/LoadService.java`

**Steps**:

1. **Add Spring Retry dependency**:
```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

2. **Enable retry**:
```java
@SpringBootApplication
@EnableRetry  // Add this
public class JivsApplication {
    // ...
}
```

3. **Add retry to load methods**:
```java
@Service
public class LoadService {

    @Retryable(
        value = {SQLException.class, DataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public LoadResult batchLoad(LoadContext context) {
        log.info("Attempting batch load (retry if fails)");
        // ... existing logic
    }

    @Recover
    public LoadResult recover(SQLException e, LoadContext context) {
        log.error("All retry attempts exhausted for batch load", e);
        throw new LoadException("Failed after 3 retry attempts", e);
    }
}
```

**Validation**:
```bash
# Test with unreliable connection (simulate network issues)
# Should see retry attempts in logs
grep "Attempting batch load (retry if fails)" logs/application.log
# Should show 3 attempts before giving up
```

---

#### ✅ Task 4.3: Implement Circuit Breaker (10 hours)

**Files Involved**:
- `backend/pom.xml` (ADD Resilience4j)
- `backend/src/main/java/com/jivs/platform/config/CircuitBreakerConfig.java`

**Steps**:

1. **Add Resilience4j**:
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

2. **Configure circuit breaker**:
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      migrationLoad:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
        slowCallRateThreshold: 100
        slowCallDurationThreshold: 5s
```

3. **Apply circuit breaker**:
```java
@Service
public class LoadService {

    @CircuitBreaker(name = "migrationLoad", fallbackMethod = "fallbackLoad")
    @Retryable(...)
    public LoadResult batchLoad(LoadContext context) {
        // ... existing logic
    }

    public LoadResult fallbackLoad(LoadContext context, Exception e) {
        log.error("Circuit breaker triggered for batch load", e);
        throw new LoadException("Load service unavailable (circuit open)", e);
    }
}
```

**Validation**:
```bash
# Trigger circuit breaker (make 5 failing requests)
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/migrations/999/load
done

# Check circuit breaker state
curl http://localhost:8080/actuator/health/circuitBreakers
# Should show: "state": "OPEN"

# Wait 30 seconds, should transition to HALF_OPEN
sleep 30
curl http://localhost:8080/actuator/health/circuitBreakers
# Should show: "state": "HALF_OPEN"
```

---

#### ✅ Task 4.4: Add Rate Limiting (10 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/config/RateLimitConfig.java`
- `backend/src/main/java/com/jivs/platform/filter/RateLimitFilter.java`

**Steps**:

1. **Configure rate limiter**:
```yaml
# application.yml
resilience4j:
  ratelimiter:
    instances:
      migrationApi:
        registerHealthIndicator: true
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 0s
```

2. **Apply to controller**:
```java
@RestController
@RequestMapping("/api/migrations")
public class MigrationController {

    @RateLimiter(name = "migrationApi")
    @PostMapping
    public ResponseEntity<Migration> createMigration(@Valid @RequestBody MigrationCreateRequest request) {
        // ... existing logic
    }
}
```

3. **Add rate limit exception handler**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RequestNotPermitted ex) {
        ErrorResponse response = new ErrorResponse(
            "RATE_LIMIT_EXCEEDED",
            "Too many requests. Please try again later.",
            Map.of("retryAfter", "1s")
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }
}
```

**Validation**:
```bash
# Send 150 requests in 1 second
for i in {1..150}; do
  curl -X POST http://localhost:8080/api/migrations \
    -H "Content-Type: application/json" \
    -d '{"name":"Test"}' &
done
wait

# Should see 100 succeed, 50 fail with 429 Too Many Requests
```

---

### Week 5: Missing Features & Enhancements (40 hours)

#### ✅ Task 5.1: Implement Pause/Resume Functionality (12 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/service/migration/MigrationOrchestrator.java`
- `backend/src/main/java/com/jivs/platform/controller/MigrationController.java`

**Steps**:

1. **Add pause method**:
```java
@Service
public class MigrationOrchestrator {

    private final Map<Long, AtomicBoolean> pauseFlags = new ConcurrentHashMap<>();

    @Transactional
    public Migration pauseMigration(Long migrationId) {
        Migration migration = migrationRepository.findById(migrationId)
            .orElseThrow(() -> new MigrationException("Migration not found"));

        if (migration.getStatus() != MigrationStatus.IN_PROGRESS) {
            throw new IllegalStateException("Can only pause migrations that are in progress");
        }

        // Set pause flag (checked during execution)
        pauseFlags.computeIfAbsent(migrationId, k -> new AtomicBoolean()).set(true);

        migration.markPaused();
        return migrationRepository.save(migration);
    }

    // Check pause flag during execution
    public Migration executeAllPhases(Migration migration) {
        Long migrationId = migration.getId();

        for (MigrationPhase phase : MigrationPhase.values()) {
            // Check if paused before each phase
            if (isPaused(migrationId)) {
                log.info("Migration {} paused before {} phase", migrationId, phase);
                return migration;
            }

            migration = executePhase(migration, phase);
        }

        return migration;
    }

    private boolean isPaused(Long migrationId) {
        AtomicBoolean flag = pauseFlags.get(migrationId);
        return flag != null && flag.get();
    }
}
```

2. **Add controller endpoints**:
```java
@PostMapping("/{id}/pause")
public ResponseEntity<Migration> pauseMigration(@PathVariable Long id) {
    Migration migration = orchestrator.pauseMigration(id);
    return ResponseEntity.ok(migration);
}

@PostMapping("/{id}/resume")
public ResponseEntity<Migration> resumeMigration(@PathVariable Long id) {
    Migration migration = migrationRepository.findById(id).orElseThrow();

    if (!migration.canResume()) {
        return ResponseEntity.badRequest().build();
    }

    // Publish resume event
    applicationEventPublisher.publishEvent(new MigrationResumeEvent(this, id));

    return ResponseEntity.accepted().body(migration);
}
```

**Validation**:
```bash
# Start migration
curl -X POST http://localhost:8080/api/migrations/123/execute

# Pause it
curl -X POST http://localhost:8080/api/migrations/123/pause

# Check status
curl http://localhost:8080/api/migrations/123
# Should show: "status": "PAUSED", "pausedTime": "2025-01-13T..."

# Resume
curl -X POST http://localhost:8080/api/migrations/123/resume

# Check status again
curl http://localhost:8080/api/migrations/123
# Should show: "status": "IN_PROGRESS", "resumedTime": "2025-01-13T..."
```

---

#### ✅ Task 5.2: Implement Cancellation with Rollback (14 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/service/migration/MigrationOrchestrator.java`
- `backend/src/main/java/com/jivs/platform/service/migration/RollbackService.java` (NEW)

**Steps**:

1. **Create RollbackService**:
```java
package com.jivs.platform.service.migration;

@Slf4j
@Service
public class RollbackService {

    @Autowired
    private LoadService loadService;

    /**
     * Rollback migration by deleting loaded data
     */
    public void rollback(Migration migration) {
        log.info("Starting rollback for migration {}", migration.getId());

        try {
            // Get loaded record IDs from migration metadata
            List<Object> loadedIds = getLoadedRecordIds(migration);

            // Delete in reverse order
            String targetTable = migration.getTargetSystem();
            String targetConnection = migration.getTargetConnectionString();

            for (Object id : loadedIds) {
                deleteRecord(targetConnection, targetTable, id);
            }

            migration.recordRollback(true, null);
            log.info("Rollback completed for migration {}", migration.getId());

        } catch (Exception e) {
            log.error("Rollback failed for migration {}", migration.getId(), e);
            migration.recordRollback(false, e.getMessage());
            throw new MigrationException("Rollback failed", e);
        }
    }

    private List<Object> getLoadedRecordIds(Migration migration) {
        // Parse migration metadata to get IDs of records that were loaded
        // This assumes we track IDs during load phase
        return new ArrayList<>();  // Placeholder
    }

    private void deleteRecord(String connection, String table, Object id) {
        // Execute DELETE statement
        // Use parameterized query to prevent SQL injection
    }
}
```

2. **Add cancel endpoint**:
```java
@PostMapping("/{id}/cancel")
public ResponseEntity<Migration> cancelMigration(
        @PathVariable Long id,
        @RequestParam(defaultValue = "false") boolean rollback) {

    Migration migration = orchestrator.cancelMigration(id, rollback);
    return ResponseEntity.ok(migration);
}
```

**Validation**:
```bash
# Start migration
curl -X POST http://localhost:8080/api/migrations/123/execute

# Cancel with rollback
curl -X POST "http://localhost:8080/api/migrations/123/cancel?rollback=true"

# Check status
curl http://localhost:8080/api/migrations/123
# Should show: "status": "CANCELLED", "rollbackExecuted": true
```

---

#### ✅ Task 5.3: Add Progress Tracking (8 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/service/migration/MigrationOrchestrator.java`
- `backend/src/main/java/com/jivs/platform/controller/MigrationController.java`

**Steps**:

1. **Update progress during execution**:
```java
public Migration executeLoadPhase(Migration migration) {
    migration.setCurrentPhase(MigrationPhase.LOADING);
    migration = migrationRepository.save(migration);

    int totalRecords = migration.getTotalRecords();
    int batchSize = migration.getBatchSize();
    int processed = 0;

    while (processed < totalRecords) {
        int batchEnd = Math.min(processed + batchSize, totalRecords);

        // Load batch
        loadBatch(migration, processed, batchEnd);

        processed = batchEnd;

        // Update progress (save to DB frequently)
        migration.updateProgress(processed, migration.getSuccessfulRecords(), migration.getFailedRecords());
        migration = migrationRepository.save(migration);

        log.info("Migration {} progress: {}/{}  ({}%)",
            migration.getId(), processed, totalRecords, migration.getProgressPercentage());
    }

    return migration;
}
```

2. **Add SSE endpoint for real-time progress**:
```java
@GetMapping(value = "/{id}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<MigrationProgress>> streamProgress(@PathVariable Long id) {
    return Flux.interval(Duration.ofSeconds(1))
        .map(seq -> {
            Migration migration = migrationRepository.findById(id).orElseThrow();

            MigrationProgress progress = new MigrationProgress(
                migration.getId(),
                migration.getStatus(),
                migration.getCurrentPhase(),
                migration.getProgressPercentage(),
                migration.getProcessedRecords(),
                migration.getTotalRecords()
            );

            return ServerSentEvent.<MigrationProgress>builder()
                .id(String.valueOf(seq))
                .event("progress")
                .data(progress)
                .build();
        })
        .takeWhile(event -> !event.data().status().equals(MigrationStatus.COMPLETED));
}
```

**Validation**:
```bash
# Start migration
curl -X POST http://localhost:8080/api/migrations/123/execute

# Stream progress (SSE)
curl -N http://localhost:8080/api/migrations/123/progress

# Should see updates like:
# event: progress
# data: {"id":123,"status":"IN_PROGRESS","phase":"LOADING","percentage":45,"processed":4500,"total":10000}
#
# event: progress
# data: {"id":123,"status":"IN_PROGRESS","phase":"LOADING","percentage":67,"processed":6700,"total":10000}
```

---

#### ✅ Task 5.4: Add Audit Logging (6 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/service/migration/MigrationAuditService.java` (NEW)

**Steps**:

1. **Create audit service**:
```java
package com.jivs.platform.service.migration;

@Service
public class MigrationAuditService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void logMigrationEvent(Long migrationId, String eventType,
                                   String description, String userId) {
        String sql = """
            INSERT INTO migration_audit_log
                (migration_id, event_type, description, user_id, created_at)
            VALUES (?, ?, ?, ?, NOW())
            """;

        jdbcTemplate.update(sql, migrationId, eventType, description, userId);
    }

    public List<AuditEvent> getMigrationAuditLog(Long migrationId) {
        String sql = """
            SELECT id, migration_id, event_type, description, user_id, created_at
            FROM migration_audit_log
            WHERE migration_id = ?
            ORDER BY created_at DESC
            """;

        return jdbcTemplate.query(sql, this::mapAuditEvent, migrationId);
    }

    private AuditEvent mapAuditEvent(ResultSet rs, int rowNum) throws SQLException {
        return new AuditEvent(
            rs.getLong("id"),
            rs.getLong("migration_id"),
            rs.getString("event_type"),
            rs.getString("description"),
            rs.getString("user_id"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}

record AuditEvent(Long id, Long migrationId, String eventType,
                  String description, String userId, LocalDateTime createdAt) {}
```

2. **Add audit logging to orchestrator**:
```java
@Service
public class MigrationOrchestrator {

    @Autowired
    private MigrationAuditService auditService;

    @Transactional
    public Migration initiateMigration(MigrationCreateRequest request) {
        Migration migration = createMigration(request);
        migration = migrationRepository.save(migration);

        // Audit log
        auditService.logMigrationEvent(
            migration.getId(),
            "MIGRATION_CREATED",
            "Migration created: " + migration.getName(),
            getCurrentUserId()
        );

        return migration;
    }

    public Migration executeAllPhases(Migration migration) {
        auditService.logMigrationEvent(
            migration.getId(),
            "EXECUTION_STARTED",
            "Migration execution started",
            "system"
        );

        try {
            // Execute phases...

            auditService.logMigrationEvent(
                migration.getId(),
                "EXECUTION_COMPLETED",
                "Migration completed successfully",
                "system"
            );

            return migration;
        } catch (Exception e) {
            auditService.logMigrationEvent(
                migration.getId(),
                "EXECUTION_FAILED",
                "Migration failed: " + e.getMessage(),
                "system"
            );
            throw e;
        }
    }
}
```

**Validation**:
```bash
# Create and execute migration
curl -X POST http://localhost:8080/api/migrations \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","sourceSystem":"postgresql","targetSystem":"postgresql"}'

# Check audit log
psql -U jivs_user -d jivs -c \
  "SELECT * FROM migration_audit_log WHERE migration_id = 123 ORDER BY created_at"

# Should show:
# id | migration_id | event_type          | description                | user_id | created_at
# 1  | 123          | MIGRATION_CREATED   | Migration created: Test    | admin   | 2025-01-13 ...
# 2  | 123          | EXECUTION_STARTED   | Migration execution started| system  | 2025-01-13 ...
# 3  | 123          | EXECUTION_COMPLETED | Migration completed ...    | system  | 2025-01-13 ...
```

---

## Phase 3: Medium-Priority Fixes (Week 6-7)

### Week 6: Code Organization & Performance (42 hours)

#### ✅ Task 6.1: Split MigrationModels.java (8 hours)

**Issue**: 483-line file with 20+ inner classes

**Steps**:

1. **Create separate files for each model**:
```bash
mkdir -p backend/src/main/java/com/jivs/platform/service/migration/model

# Create individual model files:
# - SourceAnalysis.java
# - TargetAnalysis.java
# - MigrationPlan.java
# - ResourceEstimation.java
# - ValidationResult.java
# - VerificationResult.java
# - LoadContext.java
# - TransformContext.java
# ... (20+ files)
```

2. **Update imports** in all migration service classes

**Validation**:
```bash
mvn clean compile
# Should compile without errors
```

---

#### ✅ Task 6.2: Add Database Indexes (6 hours)

**Files Involved**:
- `backend/src/main/resources/db/migration/V112__Add_performance_indexes.sql` (NEW)

**Steps**:

1. **Create index migration**:
```sql
-- V112__Add_performance_indexes.sql

-- Index for common status queries
CREATE INDEX IF NOT EXISTS idx_migration_status_created
ON migration_projects(status, created_at DESC);

-- Index for user queries (find my migrations)
CREATE INDEX IF NOT EXISTS idx_migration_created_by
ON migration_projects(created_by, created_at DESC);

-- Composite index for filtering
CREATE INDEX IF NOT EXISTS idx_migration_filter
ON migration_projects(status, source_system, target_system, created_at DESC);

-- Partial index for active migrations only
CREATE INDEX IF NOT EXISTS idx_migration_active_phase
ON migration_projects(migration_phase, updated_at DESC)
WHERE status IN ('IN_PROGRESS', 'PAUSED');
```

**Validation**:
```bash
mvn flyway:migrate

psql -U jivs_user -d jivs -c "\d+ migration_projects"
# Should show all new indexes
```

---

#### ✅ Task 6.3: Optimize N+1 Queries (12 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/repository/MigrationRepository.java`

**Steps**:

1. **Add fetch joins to repository queries**:
```java
@Repository
public interface MigrationRepository extends JpaRepository<Migration, Long> {

    @Query("""
        SELECT DISTINCT m FROM Migration m
        LEFT JOIN FETCH m.sourceAnalysis
        LEFT JOIN FETCH m.targetAnalysis
        WHERE m.status = :status
        """)
    List<Migration> findByStatusWithAnalysis(@Param("status") MigrationStatus status);

    @Query("""
        SELECT m FROM Migration m
        WHERE m.id = :id
        """)
    @EntityGraph(attributePaths = {
        "sourceAnalysis",
        "targetAnalysis",
        "migrationPlan",
        "validationResult"
    })
    Optional<Migration> findByIdWithDetails(@Param("id") Long id);
}
```

**Validation**:
```bash
# Enable SQL logging
# application.yml: spring.jpa.show-sql: true

# Run query and check logs
curl http://localhost:8080/api/migrations/123

# Should see SINGLE SELECT with JOIN, not N+1 SELECTs
```

---

#### ✅ Task 6.4: Add Caching (10 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/config/CacheConfig.java` (NEW)
- `backend/src/main/java/com/jivs/platform/service/migration/MigrationService.java`

**Steps**:

1. **Configure Redis caching**:
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
    }
}
```

2. **Add caching to repository**:
```java
@Cacheable(value = "migrations", key = "#id")
public Optional<Migration> findById(Long id) {
    return super.findById(id);
}

@CacheEvict(value = "migrations", key = "#migration.id")
public Migration save(Migration migration) {
    return super.save(migration);
}
```

**Validation**:
```bash
# First request (cache miss)
time curl http://localhost:8080/api/migrations/123
# Response time: ~100ms

# Second request (cache hit)
time curl http://localhost:8080/api/migrations/123
# Response time: ~5ms (20x faster)

# Check Redis
redis-cli
> KEYS migrations::*
> GET migrations::123
```

---

#### ✅ Task 6.6: Add Pagination (6 hours)

**Files Involved**:
- `backend/src/main/java/com/jivs/platform/controller/MigrationController.java`
- `backend/src/main/java/com/jivs/platform/repository/MigrationRepository.java`

**Steps**:

1. **Update repository for pagination**:
```java
public interface MigrationRepository extends JpaRepository<Migration, Long> {
    Page<Migration> findByStatus(MigrationStatus status, Pageable pageable);
    Page<Migration> findByCreatedBy(String userId, Pageable pageable);
}
```

2. **Update controller**:
```java
@GetMapping
public ResponseEntity<Page<Migration>> getAllMigrations(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrders(sort)));
    Page<Migration> migrations = migrationRepository.findAll(pageable);

    return ResponseEntity.ok(migrations);
}
```

**Validation**:
```bash
# Get first page
curl "http://localhost:8080/api/migrations?page=0&size=20"

# Get second page
curl "http://localhost:8080/api/migrations?page=1&size=20"

# Sort by name
curl "http://localhost:8080/api/migrations?sort=name,asc"
```

---

### Week 7: Final Optimizations & Documentation (40 hours)

#### ✅ Task 7.1: Performance Profiling & Optimization (16 hours)

**Tools**:
- JProfiler / YourKit
- JMH Benchmarking
- k6 Load Testing

**Steps**:

1. **Run profiler** and identify hotspots
2. **Optimize critical paths** (top 3 bottlenecks)
3. **Re-run load tests** and compare metrics
4. **Document optimizations** in performance.md

---

#### ✅ Task 7.2: Code Review & Cleanup (12 hours)

- Remove unused imports
- Fix PMD/SpotBugs warnings
- Update JavaDocs
- Format code consistently

---

#### ✅ Task 7.3: Update Documentation (12 hours)

- Update README.md
- Create migration-module-README.md
- Update API documentation
- Create troubleshooting guide

---

## Phase 4: Testing & Validation (Week 8)

### ✅ Task 4.1: Run All Test Suites

**Unit Tests**:
```bash
cd backend

# Migration Orchestrator Tests (13 tests)
mvn test -Dtest=MigrationOrchestratorFixedTest
# Expected: All 13 tests pass ✅

# Load Service Security Tests (18 tests)
mvn test -Dtest=LoadServiceSecurityTest
# Expected: All 18 tests pass ✅

# Validation Service Tests
mvn test -Dtest=ValidationServiceTest
# Expected: All tests pass ✅

# All migration module unit tests
mvn test -Dtest="com.jivs.platform.service.migration.*Test"
```

**Integration Tests**:
```bash
# Lifecycle Integration Tests (10 tests)
mvn verify -Dtest=MigrationLifecycleIntegrationTest -P integration-tests
# Expected: All 10 tests pass ✅

# Full integration test suite
mvn verify -P integration-tests
```

**E2E Tests**:
```bash
cd frontend

# Migration workflow E2E tests
npx playwright test specs/migration-workflow.spec.ts

# All E2E tests
npx playwright test
```

---

### ✅ Task 4.2: Performance Testing

**Load Test**:
```bash
# Start application
mvn spring-boot:run &

# Run k6 load test
k6 run backend/src/test/k6/migration-load-test.js

# Expected metrics:
# - Throughput: ~1000 req/sec
# - P95 latency: <500ms
# - Error rate: <1%
```

---

### ✅ Task 4.3: Security Audit

**OWASP Dependency Check**:
```bash
mvn org.owasp:dependency-check-maven:check
# Review report: target/dependency-check-report.html
```

**SQL Injection Tests**:
```bash
# Run security test suite
mvn test -Dtest=LoadServiceSecurityTest

# Manual penetration testing
# Try various injection patterns in API requests
```

---

### ✅ Task 4.4: Code Quality Gates

**SonarQube Analysis**:
```bash
mvn sonar:sonar \
  -Dsonar.projectKey=jivs-migration \
  -Dsonar.host.url=http://localhost:9000

# Quality Gates:
# - Code coverage: ≥80% ✅
# - Duplications: <3% ✅
# - Maintainability: A rating ✅
# - Reliability: A rating ✅
# - Security: A rating ✅
```

---

## Rollback Procedures

### Emergency Rollback (Production Issues)

**Symptoms requiring rollback**:
- Migration data not persisting
- High error rates (>5%)
- Performance degradation (>2x latency)
- Security vulnerabilities exploited

**Rollback Steps**:

1. **Restore code**:
```bash
# Rollback to previous Git tag
git checkout v1.0.0-before-migration-fixes
mvn clean package -DskipTests
# Deploy previous JAR
```

2. **Rollback database** (if V111 migration applied):
```sql
-- Connect to production database
psql -U jivs_user -d jivs

-- Drop new objects
DROP TRIGGER IF EXISTS migration_state_change_trigger ON migration_projects;
DROP FUNCTION IF EXISTS migration_audit_trigger();
DROP TABLE IF EXISTS migration_audit_log;
DROP MATERIALIZED VIEW IF EXISTS migration_analytics;
DROP FUNCTION IF EXISTS calculate_migration_progress;

-- Drop indexes
DROP INDEX IF EXISTS idx_migration_phase;
DROP INDEX IF EXISTS idx_migration_start_time;
-- ... (drop all 8+ indexes from V111)

-- Drop columns (WARNING: Data loss!)
ALTER TABLE migration_projects
    DROP COLUMN IF EXISTS migration_phase,
    DROP COLUMN IF EXISTS batch_size,
    -- ... (drop all 40+ columns)

-- Restore from backup if needed
psql -U jivs_user -d jivs < backup_before_v111_YYYYMMDD_HHMMSS.sql
```

3. **Verify rollback**:
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Test migration creation (old API)
curl -X POST http://localhost:8080/api/migrations \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","sourceSystem":"postgresql","targetSystem":"postgresql"}'
```

---

## Success Criteria

### Week 1 Completion Checklist:
- [ ] V111 migration applied successfully
- [ ] All 40+ columns present in migration_projects table
- [ ] RefactoredMigration entity compiles and passes tests
- [ ] MigrationExecutorConfig initialized (check logs)
- [ ] Event-driven async working (test with API)
- [ ] All phase methods save state
- [ ] MigrationOrchestratorFixedTest: 13/13 tests pass

### Week 2 Completion Checklist:
- [ ] LoadServiceSecurityTest: 18/18 tests pass
- [ ] SQL injection attempts blocked (manual testing)
- [ ] CORS restricted to allowed origins
- [ ] Input validation rejects invalid requests
- [ ] GlobalExceptionHandler returns proper error responses

### Week 3 Completion Checklist:
- [ ] Executor pools shut down gracefully (check logs)
- [ ] No resource leaks under load (monitor with JConsole)
- [ ] MDC context propagated to async threads
- [ ] Executor metrics exposed via /actuator/metrics

### Final Acceptance Criteria:
- [ ] All 41 tests passing (13 + 18 + 10)
- [ ] Code coverage ≥80%
- [ ] SonarQube: All ratings A
- [ ] Load test: 1000 req/sec, <500ms P95
- [ ] Security scan: 0 critical vulnerabilities
- [ ] Documentation updated
- [ ] Production deployment successful
- [ ] Post-deployment monitoring: 24hr observation period

---

## Support & Contacts

**For implementation questions**:
- Review: `MIGRATION_MODULE_FIX_GUIDE.md`
- Review: `MIGRATION_MODULE_AUDIT_REPORT.md`
- Check: `MIGRATION_MODULE_COMPLETE_SUMMARY.md`

**For production issues**:
- Check: Application logs (`logs/application.log`)
- Check: Migration audit log table
- Monitor: `/actuator/health` endpoint
- Monitor: Executor metrics

---

**Document Version**: 1.0
**Last Updated**: 2025-01-13
**Status**: ✅ Ready for Implementation
**Estimated Completion**: 8 weeks (2 engineers)
