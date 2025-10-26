# JiVS Migration Module - Fix Guide Part 2

## Critical Fixes Continued

### Fix 4: SQL Injection Prevention

**Issue**: LoadService builds SQL dynamically without escaping table/column names

**Impact**: CRITICAL - SQL injection vulnerability

**File**: `backend/src/main/java/com/jivs/platform/service/migration/LoadService.java`

#### Vulnerable Code:
```java
// VULNERABLE:
private String buildInsertSql(String table, List<String> columns) {
    String columnList = String.join(", ", columns);
    String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
    return String.format("INSERT INTO %s (%s) VALUES (%s)", table, columnList, placeholders);
    // ☠️ If table = "users; DROP TABLE users--", SQL injection occurs
}
```

#### Solution 1: Use JPA for Dynamic Inserts (Recommended)

```java
// File: backend/src/main/java/com/jivs/platform/service/migration/LoadService.java

package com.jivs.platform.service.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.annotation.PreDestroy;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LoadService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoadService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final ThreadPoolTaskExecutor ioExecutor;

    // ✅ FIXED: Regex to validate SQL identifiers
    private static final Pattern SQL_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * ✅ FIXED: Validate SQL identifier to prevent injection
     */
    private void validateSqlIdentifier(String identifier, String type) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException(type + " cannot be null or empty");
        }

        if (!SQL_IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                "Invalid " + type + ": " + identifier +
                ". Must start with letter/underscore and contain only alphanumeric/underscore characters"
            );
        }

        // Additional check for SQL keywords
        String upper = identifier.toUpperCase();
        if (isReservedKeyword(upper)) {
            throw new IllegalArgumentException(
                "Invalid " + type + ": " + identifier + " is a reserved SQL keyword"
            );
        }
    }

    /**
     * Check if identifier is a reserved SQL keyword
     */
    private boolean isReservedKeyword(String identifier) {
        Set<String> keywords = Set.of(
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
            "TABLE", "WHERE", "FROM", "JOIN", "UNION", "ORDER", "GROUP", "HAVING"
        );
        return keywords.contains(identifier);
    }

    /**
     * ✅ FIXED: Batch load using validated identifiers
     */
    @Transactional
    public LoadResult batchLoad(LoadContext context) {
        LoadResult result = new LoadResult();
        result.setBatchId(context.getBatchId());
        result.setStartTime(new Date());

        try {
            // Validate table and column names
            validateSqlIdentifier(context.getTargetTable(), "Table name");
            for (String column : context.getColumns()) {
                validateSqlIdentifier(column, "Column name");
            }

            // Build parameterized query
            StringBuilder sql = new StringBuilder("INSERT INTO ");
            sql.append(context.getTargetTable()).append(" (");
            sql.append(String.join(", ", context.getColumns()));
            sql.append(") VALUES (");
            sql.append(context.getColumns().stream()
                .map(c -> "?")
                .collect(Collectors.joining(", ")));
            sql.append(")");

            Query query = entityManager.createNativeQuery(sql.toString());

            int successCount = 0;
            int failedCount = 0;

            for (Map<String, Object> record : context.getData()) {
                try {
                    // Set parameters
                    int paramIndex = 1;
                    for (String column : context.getColumns()) {
                        query.setParameter(paramIndex++, record.get(column));
                    }

                    query.executeUpdate();
                    successCount++;

                    // Flush every 100 records
                    if (successCount % 100 == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    log.error("Failed to load record: {}", record, e);
                    failedCount++;

                    if (context.isFailFast()) {
                        throw new RuntimeException("Batch load failed", e);
                    }
                }
            }

            result.setRecordsLoaded(successCount);
            result.setFailedRecords(failedCount);
            result.setSuccess(true);

        } catch (Exception e) {
            log.error("Batch load failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setFailedRecords(context.getData().size());
        }

        result.setEndTime(new Date());
        result.setDuration(result.getEndTime().getTime() - result.getStartTime().getTime());

        return result;
    }

    /**
     * ✅ FIXED: PostgreSQL upsert with validated identifiers
     */
    private String buildPostgresUpsertSql(String table, List<String> columns, List<String> keyColumns) {
        // Validate all identifiers
        validateSqlIdentifier(table, "Table name");
        columns.forEach(c -> validateSqlIdentifier(c, "Column name"));
        keyColumns.forEach(c -> validateSqlIdentifier(c, "Key column name"));

        String columnList = String.join(", ", columns);
        String placeholders = columns.stream()
            .map(c -> "?")
            .collect(Collectors.joining(", "));

        String keyList = String.join(", ", keyColumns);

        String updateSet = columns.stream()
            .filter(c -> !keyColumns.contains(c))
            .map(c -> c + " = EXCLUDED." + c)
            .collect(Collectors.joining(", "));

        return String.format(
            "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (%s) DO UPDATE SET %s",
            table, columnList, placeholders, keyList, updateSet
        );
    }

    /**
     * ✅ FIXED: Resource cleanup
     */
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down IO executor");
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.getThreadPoolExecutor().awaitTermination(60, TimeUnit.SECONDS)) {
                ioExecutor.getThreadPoolExecutor().shutdownNow();
            }
        } catch (InterruptedException e) {
            ioExecutor.getThreadPoolExecutor().shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Supporting classes (unchanged)
    public static class LoadContext { /* ... */ }
    public static class LoadResult { /* ... */ }
    public static enum LoadStrategy { /* ... */ }
}
```

#### Solution 2: Whitelist Approach (Alternative)

```java
/**
 * ✅ FIXED: Whitelist-based table validation
 */
@Component
public class TableWhitelistValidator {

    private final Set<String> allowedTables;
    private final Map<String, Set<String>> allowedColumnsPerTable;

    public TableWhitelistValidator(
            @Value("${jivs.migration.allowed-tables}") List<String> tables) {
        this.allowedTables = new HashSet<>(tables);
        this.allowedColumnsPerTable = new HashMap<>();
        // Initialize from database metadata
        initializeColumnWhitelist();
    }

    @PostConstruct
    private void initializeColumnWhitelist() {
        // Query database metadata to populate allowed columns
        // This ensures we only accept actual table columns
    }

    public void validateTableAccess(String tableName, List<String> columns) {
        if (!allowedTables.contains(tableName)) {
            throw new SecurityException("Table not in whitelist: " + tableName);
        }

        Set<String> allowedColumns = allowedColumnsPerTable.get(tableName);
        if (allowedColumns == null) {
            throw new SecurityException("No column whitelist for table: " + tableName);
        }

        for (String column : columns) {
            if (!allowedColumns.contains(column)) {
                throw new SecurityException(
                    "Column not allowed: " + column + " for table: " + tableName
                );
            }
        }
    }
}
```

**Configuration**:
```yaml
# File: backend/src/main/resources/application.yml

jivs:
  migration:
    allowed-tables:
      - users
      - orders
      - products
      - customers
      - transactions
```

**Testing**:
```java
// File: backend/src/test/java/com/jivs/platform/service/migration/LoadServiceSecurityTest.java

@SpringBootTest
class LoadServiceSecurityTest {

    @Autowired
    private LoadService loadService;

    @Test
    void testBatchLoad_RejectsSqlInjectionInTableName() {
        // Given
        LoadService.LoadContext context = new LoadService.LoadContext();
        context.setTargetTable("users; DROP TABLE users--"); // SQL injection attempt
        context.setColumns(Arrays.asList("id", "name"));

        // When/Then
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loadService.batchLoad(context)
        );

        assertTrue(ex.getMessage().contains("Invalid Table name"));
    }

    @Test
    void testBatchLoad_RejectsReservedKeywords() {
        // Given
        LoadService.LoadContext context = new LoadService.LoadContext();
        context.setTargetTable("SELECT"); // Reserved keyword
        context.setColumns(Arrays.asList("id", "name"));

        // When/Then
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> loadService.batchLoad(context)
        );

        assertTrue(ex.getMessage().contains("reserved SQL keyword"));
    }

    @Test
    void testBatchLoad_AcceptsValidIdentifiers() {
        // Given
        LoadService.LoadContext context = new LoadService.LoadContext();
        context.setTargetTable("valid_table_name");
        context.setColumns(Arrays.asList("column_1", "column_2"));

        // Should not throw
        assertDoesNotThrow(() -> {
            loadService.validateSqlIdentifier("valid_table_name", "Table");
            loadService.validateSqlIdentifier("column_1", "Column");
        });
    }
}
```

---

### Fix 5: Input Validation with DTOs

**Issue**: Controller accepts raw Map, no validation on required fields

**Impact**: HIGH - Missing validation allows invalid data

**Files**:
- `backend/src/main/java/com/jivs/platform/dto/MigrationCreateRequest.java` (NEW)
- `backend/src/main/java/com/jivs/platform/dto/MigrationUpdateRequest.java` (NEW)
- `backend/src/main/java/com/jivs/platform/controller/MigrationController.java`

#### Step 1: Create Request DTOs

```java
// File: backend/src/main/java/com/jivs/platform/dto/MigrationCreateRequest.java

package com.jivs.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

/**
 * ✅ FIXED: Validated DTO for migration creation
 */
@Data
public class MigrationCreateRequest {

    @NotBlank(message = "Migration name is required")
    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Source system is required")
    @Size(max = 100, message = "Source system name cannot exceed 100 characters")
    private String sourceSystem;

    @NotBlank(message = "Target system is required")
    @Size(max = 100, message = "Target system name cannot exceed 100 characters")
    private String targetSystem;

    @NotBlank(message = "Migration type is required")
    @Pattern(
        regexp = "DATA_MIGRATION|APP_RETIREMENT|SYSTEM_CONSOLIDATION",
        message = "Invalid migration type"
    )
    private String migrationType;

    @Min(value = 1, message = "Batch size must be at least 1")
    @Max(value = 10000, message = "Batch size cannot exceed 10000")
    private Integer batchSize = 1000;

    @Min(value = 1, message = "Parallelism must be at least 1")
    @Max(value = 20, message = "Parallelism cannot exceed 20")
    private Integer parallelism = 4;

    @Min(value = 0, message = "Retry attempts cannot be negative")
    @Max(value = 10, message = "Retry attempts cannot exceed 10")
    private Integer retryAttempts = 3;

    private Map<String, String> parameters;

    private Boolean strictValidation = false;
    private Boolean rollbackEnabled = true;
    private Boolean rollbackOnCancel = false;
    private Boolean archiveEnabled = false;
}
```

```java
// File: backend/src/main/java/com/jivs/platform/dto/MigrationUpdateRequest.java

package com.jivs.platform.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * ✅ FIXED: Validated DTO for migration updates
 */
@Data
public class MigrationUpdateRequest {

    @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Pattern(
        regexp = "LOW|MEDIUM|HIGH|CRITICAL",
        message = "Invalid priority"
    )
    private String priority;

    @Min(value = 1, message = "Batch size must be at least 1")
    @Max(value = 10000, message = "Batch size cannot exceed 10000")
    private Integer batchSize;

    @Min(value = 1, message = "Parallelism must be at least 1")
    @Max(value = 20, message = "Parallelism cannot exceed 20")
    private Integer parallelism;
}
```

```java
// File: backend/src/main/java/com/jivs/platform/dto/MigrationResponse.java

package com.jivs.platform.dto;

import com.jivs.platform.domain.migration.MigrationStatus;
import com.jivs.platform.domain.migration.MigrationPhase;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ✅ FIXED: Consistent response DTO
 */
@Data
@Builder
public class MigrationResponse {

    private Long id;
    private String projectCode;
    private String name;
    private String description;
    private String sourceSystem;
    private String targetSystem;
    private String projectType;
    private MigrationStatus status;
    private MigrationPhase phase;
    private Integer progress;
    private Long recordsMigrated;
    private Long totalRecords;
    private LocalDateTime createdAt;
    private LocalDateTime startTime;
    private LocalDateTime completionTime;
    private String createdBy;

    public static MigrationResponse fromEntity(com.jivs.platform.domain.migration.Migration migration) {
        int progress = 0;
        if (migration.getTotalRecords() != null && migration.getTotalRecords() > 0) {
            progress = (int) ((migration.getProcessedRecords() * 100.0) / migration.getTotalRecords());
            progress = Math.min(100, Math.max(0, progress));
        }

        return MigrationResponse.builder()
            .id(migration.getId())
            .projectCode(migration.getProjectCode())
            .name(migration.getName())
            .description(migration.getDescription())
            .sourceSystem(migration.getSourceSystem())
            .targetSystem(migration.getTargetSystem())
            .projectType(migration.getProjectType())
            .status(migration.getStatus())
            .phase(migration.getPhase())
            .progress(progress)
            .recordsMigrated(migration.getProcessedRecords() != null ?
                migration.getProcessedRecords().longValue() : 0L)
            .totalRecords(migration.getTotalRecords() != null ?
                migration.getTotalRecords().longValue() : 0L)
            .createdAt(migration.getCreatedAt())
            .startTime(migration.getStartTime())
            .completionTime(migration.getCompletionTime())
            .createdBy(migration.getCreatedBy())
            .build();
    }
}
```

#### Step 2: Update Controller

```java
// File: backend/src/main/java/com/jivs/platform/controller/MigrationController.java

package com.jivs.platform.controller;

import com.jivs.platform.domain.migration.Migration;
import com.jivs.platform.dto.*;
import com.jivs.platform.repository.MigrationRepository;
import com.jivs.platform.security.UserPrincipal;
import com.jivs.platform.service.migration.MigrationModels;
import com.jivs.platform.service.migration.MigrationOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ FIXED: REST API controller with proper validation
 */
@RestController
@RequestMapping("/api/v1/migrations")
@RequiredArgsConstructor
@Validated
@CrossOrigin(
    origins = "${jivs.cors.allowed-origins}",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE},
    allowedHeaders = {"Authorization", "Content-Type"}
)
public class MigrationController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MigrationController.class);

    private final MigrationOrchestrator migrationOrchestrator;
    private final MigrationRepository migrationRepository;

    /**
     * ✅ FIXED: Create migration with validated DTO
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER')")
    public ResponseEntity<MigrationResponse> createMigration(
            @Valid @RequestBody MigrationCreateRequest request) {

        log.info("Creating new migration: {}", request.getName());

        try {
            // Convert DTO to internal request model
            MigrationModels.MigrationRequest migrationRequest = new MigrationModels.MigrationRequest();
            migrationRequest.setName(request.getName());
            migrationRequest.setDescription(request.getDescription());
            migrationRequest.setSourceSystem(request.getSourceSystem());
            migrationRequest.setTargetSystem(request.getTargetSystem());
            migrationRequest.setMigrationType(request.getMigrationType());
            migrationRequest.setUserId(getCurrentUserId());

            if (request.getParameters() != null) {
                Map<String, Object> params = new HashMap<>(request.getParameters());
                migrationRequest.setParameters(params);
            }

            migrationRequest.setBatchSize(request.getBatchSize());
            migrationRequest.setParallelism(request.getParallelism());
            migrationRequest.setRetryAttempts(request.getRetryAttempts());

            // Create migration
            Migration migration = migrationOrchestrator.initiateMigration(migrationRequest);

            // Return DTO response
            MigrationResponse response = MigrationResponse.fromEntity(migration);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid migration request: {}", e.getMessage());
            throw e; // Will be handled by @RestControllerAdvice
        } catch (Exception e) {
            log.error("Failed to create migration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create migration", e);
        }
    }

    /**
     * ✅ FIXED: Get migration with proper error handling
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER', 'VIEWER')")
    @Transactional(readOnly = true)
    public ResponseEntity<MigrationResponse> getMigration(@PathVariable Long id) {
        log.info("Getting migration: {}", id);

        Migration migration = migrationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Migration not found: " + id));

        MigrationResponse response = MigrationResponse.fromEntity(migration);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ FIXED: List migrations with status filtering
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER', 'VIEWER')")
    @Transactional(readOnly = true)
    public ResponseEntity<PagedResponse<MigrationResponse>> listMigrations(
            @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String status) {

        log.info("Listing migrations: page={}, size={}, status={}", page, size, status);

        Page<Migration> migrationPage;

        if (status != null && !status.trim().isEmpty()) {
            // ✅ FIXED: Now actually uses status parameter
            try {
                MigrationStatus migrationStatus = MigrationStatus.valueOf(status.toUpperCase());
                migrationPage = migrationRepository.findByStatus(
                    migrationStatus,
                    PageRequest.of(page, size)
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status value: " + status);
            }
        } else {
            migrationPage = migrationRepository.findAll(PageRequest.of(page, size));
        }

        List<MigrationResponse> migrations = migrationPage.getContent().stream()
            .map(MigrationResponse::fromEntity)
            .collect(Collectors.toList());

        PagedResponse<MigrationResponse> response = new PagedResponse<>(
            migrations,
            migrationPage.getTotalElements(),
            migrationPage.getTotalPages(),
            page,
            size
        );

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ FIXED: Pause with state validation
     */
    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENGINEER')")
    public ResponseEntity<MigrationResponse> pauseMigration(@PathVariable Long id) {
        log.info("Pausing migration: {}", id);

        // ✅ FIXED: Check current state before pausing
        Migration current = migrationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Migration not found: " + id));

        if (current.getStatus() != MigrationStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Cannot pause migration in status: " + current.getStatus() +
                ". Must be IN_PROGRESS"
            );
        }

        Migration migration = migrationOrchestrator.pauseMigration(id);
        MigrationResponse response = MigrationResponse.fromEntity(migration);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ FIXED: Get current user ID with fallback
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        // ✅ FIXED: Return system user ID instead of null
        return 0L; // System user
    }

    /**
     * ✅ FIXED: Get current username with fallback
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getUsername();
        }
        return "system";
    }
}
```

#### Step 3: Create Custom Exceptions

```java
// File: backend/src/main/java/com/jivs/platform/exception/ResourceNotFoundException.java

package com.jivs.platform.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

```java
// File: backend/src/main/java/com/jivs/platform/exception/GlobalExceptionHandler.java

package com.jivs.platform.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ✅ FIXED: Global exception handler for proper error responses
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Invalid state: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.CONFLICT.value())
            .error("Conflict")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse response = ValidationErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Invalid request parameters")
            .errors(errors)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        log.error("Security violation: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        ErrorResponse error = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

```java
// File: backend/src/main/java/com/jivs/platform/dto/ErrorResponse.java

package com.jivs.platform.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}
```

```java
// File: backend/src/main/java/com/jivs/platform/dto/ValidationErrorResponse.java

package com.jivs.platform.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ValidationErrorResponse {
    private int status;
    private String error;
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp;
}
```

```java
// File: backend/src/main/java/com/jivs/platform/dto/PagedResponse.java

package com.jivs.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
```

**Testing**:
```java
// File: backend/src/test/java/com/jivs/platform/controller/MigrationControllerValidationTest.java

@SpringBootTest
@AutoConfigureMockMvc
class MigrationControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateMigration_MissingRequiredFields() throws Exception {
        MigrationCreateRequest request = new MigrationCreateRequest();
        // Missing all required fields

        mockMvc.perform(post("/api/v1/migrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.errors.name").value("Migration name is required"))
            .andExpect(jsonPath("$.errors.sourceSystem").value("Source system is required"))
            .andExpect(jsonPath("$.errors.targetSystem").value("Target system is required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateMigration_InvalidBatchSize() throws Exception {
        MigrationCreateRequest request = new MigrationCreateRequest();
        request.setName("Test Migration");
        request.setSourceSystem("MySQL");
        request.setTargetSystem("PostgreSQL");
        request.setMigrationType("DATA_MIGRATION");
        request.setBatchSize(20000); // Exceeds max

        mockMvc.perform(post("/api/v1/migrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.batchSize")
                .value("Batch size cannot exceed 10000"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testPauseMigration_InvalidState() throws Exception {
        // Given a completed migration
        Migration migration = createCompletedMigration();

        // When trying to pause
        mockMvc.perform(post("/api/v1/migrations/{id}/pause", migration.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message")
                .value(containsString("Cannot pause migration")));
    }
}
```

---

**Continue to Part 3...**

This covers:
- ✅ Fix 4: SQL Injection Prevention
- ✅ Fix 5: Input Validation with DTOs

**Next sections**:
- Fix 6: Complete Test Suite Examples
- Fix 7: Performance Optimizations
- Fix 8: Monitoring & Observability

Would you like me to continue with the remaining fixes and test examples?