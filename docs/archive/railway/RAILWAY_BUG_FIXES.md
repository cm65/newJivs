# Railway Deployment Bug Fixes

**Date**: October 21, 2025
**Issues Found**: 2 HTTP 500 errors in Railway production deployment

---

## BUG #1: Missing User Management Endpoint

### Issue
`GET /api/v1/users` returns HTTP 500 Internal Server Error

### Root Cause
No `UserController.java` exists - user management endpoint not implemented

### Fix: Create UserController.java

**File**: `/backend/src/main/java/com/jivs/platform/controller/UserController.java`

```java
package com.jivs.platform.controller;

import com.jivs.platform.common.constant.Constants;
import com.jivs.platform.dto.UserDTO;
import com.jivs.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * User Management Controller
 * Admin-only endpoints for managing users
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_V1 + "/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin user management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    /**
     * Get all users with pagination
     */
    @GetMapping
    @Operation(summary = "List all users", description = "Get paginated list of all users (admin only)")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        try {
            Sort.Direction direction = Sort.Direction.fromString(sortDirection);
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<UserDTO> users = search != null && !search.trim().isEmpty()
                    ? userService.searchUsers(search, pageable)
                    : userService.getAllUsers(pageable);

            return ResponseEntity.ok(users);

        } catch (Exception e) {
            log.error("Error retrieving users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Get a specific user by ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        try {
            UserDTO user = userService.getUserById(id);
            if (user != null) {
                return ResponseEntity.ok(user);
            }
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error retrieving user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new user
     */
    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user (admin only)")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        try {
            UserDTO created = userService.createUser(userDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (IllegalArgumentException e) {
            log.error("Invalid user data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update existing user
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {

        try {
            UserDTO updated = userService.updateUser(id, userDTO);
            if (updated != null) {
                return ResponseEntity.ok(updated);
            }
            return ResponseEntity.notFound().build();

        } catch (IllegalArgumentException e) {
            log.error("Invalid user data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Soft-delete a user (deactivate)")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        try {
            boolean deleted = userService.deleteUser(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("message", deleted ? "User deactivated successfully" : "User not found");

            return deleted ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error deleting user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get user statistics", description = "Get user activity and statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        try {
            Map<String, Object> stats = userService.getUserStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error retrieving user statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Activate user
     */
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate user", description = "Activate a deactivated user")
    public ResponseEntity<Map<String, Object>> activateUser(@PathVariable Long id) {
        try {
            boolean activated = userService.activateUser(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", activated);
            response.put("message", activated ? "User activated successfully" : "User not found");

            return activated ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error activating user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reset user password
     */
    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset password", description = "Reset user password (admin only)")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            boolean reset = userService.resetPassword(id, newPassword);
            Map<String, Object> response = new HashMap<>();
            response.put("success", reset);
            response.put("message", reset ? "Password reset successfully" : "User not found");

            return reset ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error resetting password for user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
```

### Additional Required Changes

**1. Update UserService.java** to add missing methods:

```java
// Add these methods to UserService interface and implementation

Page<UserDTO> getAllUsers(Pageable pageable);

Page<UserDTO> searchUsers(String search, Pageable pageable);

UserDTO getUserById(Long id);

UserDTO createUser(UserDTO userDTO);

UserDTO updateUser(Long id, UserDTO userDTO);

boolean deleteUser(Long id);

Map<String, Object> getUserStatistics();

boolean activateUser(Long id);

boolean resetPassword(Long id, String newPassword);
```

**2. Create UserDTO.java** if it doesn't exist:

```java
package com.jivs.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean active;
    private List<String> roles;
    private LocalDateTime createdDate;
    private LocalDateTime lastLogin;
    // Exclude password for security
}
```

---

## BUG #2: Missing Document Archiving Rules Endpoint

### Issue
`GET /api/v1/documents/archiving/rules` returns HTTP 500 Internal Server Error

### Root Cause
DocumentController has DocumentArchivingService injected but no endpoint for listing archiving rules

### Fix: Add Archiving Rules Endpoints to DocumentController.java

**Add these methods to**: `/backend/src/main/java/com/jivs/platform/controller/DocumentController.java`

```java
/**
 * Get all archiving rules
 */
@GetMapping("/archiving/rules")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Operation(summary = "Get archiving rules", description = "Get all document archiving rules with pagination")
public ResponseEntity<Page<ArchivingRuleDTO>> getArchivingRules(
        @Parameter(description = "Pagination parameters") Pageable pageable,
        @RequestParam(value = "enabled", required = false) Boolean enabled) {

    try {
        Page<ArchivingRuleDTO> rules = archivingService.getArchivingRules(pageable, enabled);
        return ResponseEntity.ok(rules);

    } catch (Exception e) {
        log.error("Error retrieving archiving rules: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

/**
 * Get archiving rule by ID
 */
@GetMapping("/archiving/rules/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Operation(summary = "Get archiving rule", description = "Get a specific archiving rule by ID")
public ResponseEntity<ArchivingRuleDTO> getArchivingRule(@PathVariable Long id) {
    try {
        ArchivingRuleDTO rule = archivingService.getArchivingRule(id);
        if (rule != null) {
            return ResponseEntity.ok(rule);
        }
        return ResponseEntity.notFound().build();

    } catch (Exception e) {
        log.error("Error retrieving archiving rule {}: {}", id, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

/**
 * Create archiving rule
 */
@PostMapping("/archiving/rules")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "Create archiving rule", description = "Create a new document archiving rule")
public ResponseEntity<ArchivingRuleDTO> createArchivingRule(
        @Valid @RequestBody ArchivingRuleDTO ruleDTO) {

    try {
        ArchivingRuleDTO created = archivingService.createArchivingRule(ruleDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);

    } catch (IllegalArgumentException e) {
        log.error("Invalid archiving rule: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    } catch (Exception e) {
        log.error("Error creating archiving rule: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

/**
 * Update archiving rule
 */
@PutMapping("/archiving/rules/{id}")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "Update archiving rule", description = "Update an existing archiving rule")
public ResponseEntity<ArchivingRuleDTO> updateArchivingRule(
        @PathVariable Long id,
        @Valid @RequestBody ArchivingRuleDTO ruleDTO) {

    try {
        ArchivingRuleDTO updated = archivingService.updateArchivingRule(id, ruleDTO);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();

    } catch (IllegalArgumentException e) {
        log.error("Invalid archiving rule: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    } catch (Exception e) {
        log.error("Error updating archiving rule {}: {}", id, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

/**
 * Delete archiving rule
 */
@DeleteMapping("/archiving/rules/{id}")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "Delete archiving rule", description = "Delete an archiving rule")
public ResponseEntity<Void> deleteArchivingRule(@PathVariable Long id) {
    try {
        boolean deleted = archivingService.deleteArchivingRule(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();

    } catch (Exception e) {
        log.error("Error deleting archiving rule {}: {}", id, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

/**
 * Execute archiving rule (apply to matching documents)
 */
@PostMapping("/archiving/rules/{id}/execute")
@PreAuthorize("hasRole('ADMIN')")
@Operation(summary = "Execute archiving rule", description = "Execute an archiving rule on matching documents")
public ResponseEntity<Map<String, Object>> executeArchivingRule(@PathVariable Long id) {
    try {
        Map<String, Object> result = archivingService.executeArchivingRule(id);
        return ResponseEntity.ok(result);

    } catch (Exception e) {
        log.error("Error executing archiving rule {}: {}", id, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
```

### Additional Required Changes

**1. Update DocumentArchivingService.java** to add these methods:

```java
// Add these methods to DocumentArchivingService

Page<ArchivingRuleDTO> getArchivingRules(Pageable pageable, Boolean enabled);

ArchivingRuleDTO getArchivingRule(Long id);

ArchivingRuleDTO createArchivingRule(ArchivingRuleDTO ruleDTO);

ArchivingRuleDTO updateArchivingRule(Long id, ArchivingRuleDTO ruleDTO);

boolean deleteArchivingRule(Long id);

Map<String, Object> executeArchivingRule(Long id);
```

**2. Create ArchivingRuleDTO.java**:

```java
package com.jivs.platform.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArchivingRuleDTO {
    private Long id;
    private String name;
    private String description;
    private String fileTypePattern;
    private Integer ageInDays;
    private String action; // ARCHIVE, DELETE, MOVE_TO_COLD_STORAGE
    private String storageTier; // HOT, WARM, COLD
    private boolean enabled;
    private Integer priority;
    private LocalDateTime createdDate;
    private LocalDateTime lastExecutedDate;
    private Integer documentsProcessed;
}
```

**3. Create ArchivingRule entity** (if it doesn't exist):

```java
package com.jivs.platform.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "archiving_rules")
public class ArchivingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String fileTypePattern;

    private Integer ageInDays;

    private String action;

    private String storageTier;

    private boolean enabled = true;

    private Integer priority = 0;

    private LocalDateTime createdDate;

    private LocalDateTime lastExecutedDate;

    private Integer documentsProcessed = 0;
}
```

**4. Create ArchivingRuleRepository**:

```java
package com.jivs.platform.repository;

import com.jivs.platform.domain.ArchivingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchivingRuleRepository extends JpaRepository<ArchivingRule, Long> {
    Page<ArchivingRule> findByEnabled(Boolean enabled, Pageable pageable);
}
```

**5. Add Flyway migration** (if table doesn't exist):

**File**: `/backend/src/main/resources/db/migration/V1.7__create_archiving_rules.sql`

```sql
CREATE TABLE IF NOT EXISTS archiving_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    file_type_pattern VARCHAR(100),
    age_in_days INTEGER,
    action VARCHAR(50),
    storage_tier VARCHAR(50),
    enabled BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 0,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_executed_date TIMESTAMP,
    documents_processed INTEGER DEFAULT 0
);

CREATE INDEX idx_archiving_rules_enabled ON archiving_rules(enabled);
CREATE INDEX idx_archiving_rules_priority ON archiving_rules(priority DESC);
```

---

## Deployment Steps

### Step 1: Create Missing Files

1. Create `UserController.java`
2. Update `UserService.java` with new methods
3. Create or update `UserDTO.java`
4. Add archiving endpoints to `DocumentController.java`
5. Update `DocumentArchivingService.java`
6. Create `ArchivingRuleDTO.java`
7. Create `ArchivingRule.java` entity
8. Create `ArchivingRuleRepository.java`
9. Add Flyway migration `V1.7__create_archiving_rules.sql`

### Step 2: Test Locally

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Test endpoints:
```bash
# Test users endpoint
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/users?page=0&size=10

# Test archiving rules endpoint
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/documents/archiving/rules?page=0&size=10
```

### Step 3: Deploy to Railway

```bash
git add .
git commit -m "fix: Add missing User Management and Document Archiving Rules endpoints"
git push origin main
```

Railway will automatically detect the push and redeploy.

### Step 4: Verify on Railway

```bash
# Wait for deployment to complete (~2-3 minutes)
# Then test endpoints

TOKEN=$(curl -s -X POST https://jivs-backend-production.up.railway.app/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | jq -r '.data.accessToken')

# Test users endpoint
curl -s -H "Authorization: Bearer $TOKEN" \
  https://jivs-backend-production.up.railway.app/api/v1/users?page=0&size=10 | jq '.'

# Test archiving rules endpoint
curl -s -H "Authorization: Bearer $TOKEN" \
  https://jivs-backend-production.up.railway.app/api/v1/documents/archiving/rules?page=0&size=10 | jq '.'
```

### Expected Results

Both endpoints should return HTTP 200 with paginated data:

**Users endpoint**:
```json
{
  "content": [
    {
      "id": 3,
      "username": "admin",
      "email": "admin@jivs.com",
      "firstName": "Admin",
      "lastName": "User",
      "active": true,
      "roles": ["ROLE_ADMIN"]
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

**Archiving rules endpoint**:
```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0
}
```

---

## Testing Checklist

- [ ] Users list endpoint returns HTTP 200
- [ ] Users list contains admin user
- [ ] Users create endpoint works
- [ ] Users update endpoint works
- [ ] Users delete endpoint works (soft delete)
- [ ] Archiving rules list endpoint returns HTTP 200
- [ ] Archiving rules create endpoint works
- [ ] Archiving rules update endpoint works
- [ ] Archiving rules delete endpoint works
- [ ] Archiving rules execute endpoint works
- [ ] All endpoints require proper authentication
- [ ] Admin-only endpoints reject non-admin users
- [ ] Error responses are consistent
- [ ] Database migrations run successfully on Railway

---

## Estimated Time to Fix

- **Coding**: 2-3 hours
- **Testing**: 1 hour
- **Deployment**: 30 minutes
- **Total**: 3.5-4.5 hours

---

**Priority**: HIGH (blocks admin functionality)
**Impact**: User management and document archiving features unavailable
**Risk**: LOW (new code, no existing functionality affected)
