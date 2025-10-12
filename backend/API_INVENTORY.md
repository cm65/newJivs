# JiVS Platform - Complete API Inventory

**Date**: January 13, 2025
**Total Endpoints**: 78
**Controllers**: 8

---

## 1. AuthController - `/api/v1/auth`

Authentication and authorization endpoints.

| Method | Endpoint | Auth Required | Description | Request Body | Response |
|--------|----------|---------------|-------------|--------------|----------|
| POST | `/auth/login` | No | User login | `LoginRequest` | `LoginResponse` with JWT tokens |
| GET | `/auth/me` | Yes | Get current user info | - | `LoginResponse` without tokens |
| POST | `/auth/refresh` | No | Refresh access token | `RefreshTokenRequest` | `LoginResponse` with new tokens |
| POST | `/auth/logout` | Yes | Logout and blacklist token | - | Success message |

**Request Schemas:**
```json
// LoginRequest
{
  "username": "string",
  "password": "string"
}

// RefreshTokenRequest
{
  "refreshToken": "string"
}
```

**Response Schema:**
```json
// LoginResponse
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": 1,
  "username": "string",
  "email": "string",
  "roles": ["ROLE_ADMIN"]
}
```

---

## 2. ExtractionController - `/api/v1/extractions`

Data extraction operations.

| Method | Endpoint | Auth Required | Roles | Description | Parameters |
|--------|----------|---------------|-------|-------------|------------|
| POST | `/extractions` | Yes | ADMIN, DATA_ENGINEER | Create extraction | Body: extraction config |
| GET | `/extractions` | Yes | ADMIN, DATA_ENGINEER, VIEWER | List extractions | page, size, status |
| GET | `/extractions/{id}` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get extraction by ID | Path: id |
| POST | `/extractions/{id}/start` | Yes | ADMIN, DATA_ENGINEER | Start extraction | Path: id |
| POST | `/extractions/{id}/stop` | Yes | ADMIN, DATA_ENGINEER | Stop extraction | Path: id |
| DELETE | `/extractions/{id}` | Yes | ADMIN | Delete extraction | Path: id |
| POST | `/extractions/bulk` | Yes | ADMIN, DATA_ENGINEER | Bulk action on extractions | Body: bulk action request |
| GET | `/extractions/{id}/statistics` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get extraction statistics | Path: id |
| POST | `/extractions/test-connection` | Yes | ADMIN, DATA_ENGINEER | Test extraction connection | Body: connection config |
| GET | `/extractions/{id}/logs` | Yes | ADMIN, DATA_ENGINEER | Get extraction logs | Path: id, Query: limit |

**Request Schemas:**
```json
// Create Extraction
{
  "name": "string",
  "sourceType": "JDBC|SAP|FILE|API",
  "connectionConfig": {
    "url": "string",
    "username": "string",
    "password": "string"
  },
  "extractionQuery": "string"
}

// Bulk Action Request
{
  "action": "start|stop|delete|export",
  "ids": ["string"]
}
```

---

## 3. MigrationController - `/api/v1/migrations`

Data migration orchestration with 7-phase lifecycle.

| Method | Endpoint | Auth Required | Roles | Description | Parameters |
|--------|----------|---------------|-------|-------------|------------|
| POST | `/migrations` | Yes | ADMIN, DATA_ENGINEER | Create migration | Body: migration config |
| GET | `/migrations` | Yes | ADMIN, DATA_ENGINEER, VIEWER | List migrations | page, size, status |
| GET | `/migrations/{id}` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get migration by ID | Path: id |
| POST | `/migrations/{id}/start` | Yes | ADMIN, DATA_ENGINEER | Start migration | Path: id |
| POST | `/migrations/{id}/pause` | Yes | ADMIN, DATA_ENGINEER | Pause migration | Path: id |
| POST | `/migrations/{id}/resume` | Yes | ADMIN, DATA_ENGINEER | Resume migration | Path: id |
| POST | `/migrations/{id}/rollback` | Yes | ADMIN | Rollback migration | Path: id |
| DELETE | `/migrations/{id}` | Yes | ADMIN | Delete migration | Path: id |
| POST | `/migrations/bulk` | Yes | ADMIN, DATA_ENGINEER | Bulk action on migrations | Body: bulk action request |
| GET | `/migrations/{id}/progress` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get migration progress | Path: id |
| GET | `/migrations/{id}/statistics` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get migration statistics | Path: id |
| POST | `/migrations/validate` | Yes | ADMIN, DATA_ENGINEER | Validate migration config | Body: migration config |

**Request Schemas:**
```json
// Create Migration
{
  "name": "string",
  "sourceConfig": {
    "type": "JDBC",
    "url": "string",
    "username": "string",
    "password": "string"
  },
  "targetConfig": {
    "type": "JDBC",
    "url": "string",
    "username": "string",
    "password": "string"
  },
  "transformationRules": []
}
```

**Migration Phases:**
1. PLANNING
2. VALIDATION
3. EXTRACTION
4. TRANSFORMATION
5. LOADING
6. VERIFICATION
7. CLEANUP

---

## 4. DataQualityController - `/api/v1/data-quality`

Data quality management with 6 dimensions.

| Method | Endpoint | Auth Required | Roles | Description | Parameters |
|--------|----------|---------------|-------|-------------|------------|
| GET | `/data-quality/dashboard` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get quality dashboard | - |
| POST | `/data-quality/rules` | Yes | ADMIN, DATA_ENGINEER | Create quality rule | Body: rule config |
| GET | `/data-quality/rules` | Yes | ADMIN, DATA_ENGINEER, VIEWER | List quality rules | page, size, dimension |
| GET | `/data-quality/rules/{id}` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get rule by ID | Path: id |
| PUT | `/data-quality/rules/{id}` | Yes | ADMIN, DATA_ENGINEER | Update quality rule | Path: id, Body: rule |
| DELETE | `/data-quality/rules/{id}` | Yes | ADMIN | Delete quality rule | Path: id |
| POST | `/data-quality/rules/{id}/execute` | Yes | ADMIN, DATA_ENGINEER | Execute quality rule | Path: id, Body: data |
| GET | `/data-quality/issues` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get quality issues | page, size, severity |
| POST | `/data-quality/profile` | Yes | ADMIN, DATA_ENGINEER | Profile dataset | Body: dataset config |
| GET | `/data-quality/reports/{id}` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get quality report | Path: id |

**Quality Dimensions:**
1. COMPLETENESS - Null checks, missing data
2. ACCURACY - Value correctness
3. CONSISTENCY - Cross-field validation
4. VALIDITY - Format, range validation
5. UNIQUENESS - Duplicate detection
6. TIMELINESS - Data freshness

**Request Schemas:**
```json
// Create Quality Rule
{
  "name": "string",
  "dimension": "COMPLETENESS|ACCURACY|CONSISTENCY|VALIDITY|UNIQUENESS|TIMELINESS",
  "severity": "CRITICAL|HIGH|MEDIUM|LOW",
  "ruleExpression": "string",
  "enabled": true
}
```

---

## 5. ComplianceController - `/api/v1/compliance`

GDPR/CCPA compliance management.

| Method | Endpoint | Auth Required | Roles | Description | Parameters |
|--------|----------|---------------|-------|-------------|------------|
| GET | `/compliance/dashboard` | Yes | ADMIN, COMPLIANCE_OFFICER, VIEWER | Get compliance dashboard | - |
| POST | `/compliance/requests` | Yes | ADMIN, COMPLIANCE_OFFICER | Create data subject request | Body: request details |
| GET | `/compliance/requests` | Yes | ADMIN, COMPLIANCE_OFFICER, VIEWER | List requests | page, size, status, type |
| GET | `/compliance/requests/{id}` | Yes | ADMIN, COMPLIANCE_OFFICER, VIEWER | Get request by ID | Path: id |
| PUT | `/compliance/requests/{id}/status` | Yes | ADMIN, COMPLIANCE_OFFICER | Update request status | Path: id, Body: status |
| POST | `/compliance/requests/{id}/process` | Yes | ADMIN, COMPLIANCE_OFFICER | Process request | Path: id |
| GET | `/compliance/requests/{id}/export` | Yes | ADMIN, COMPLIANCE_OFFICER | Export personal data | Path: id |
| GET | `/compliance/consents` | Yes | ADMIN, COMPLIANCE_OFFICER, VIEWER | Get consent records | page, size, subjectEmail |
| POST | `/compliance/consents` | Yes | ADMIN, COMPLIANCE_OFFICER | Record consent | Body: consent details |
| POST | `/compliance/consents/{id}/revoke` | Yes | ADMIN, COMPLIANCE_OFFICER | Revoke consent | Path: id |
| GET | `/compliance/retention-policies` | Yes | ADMIN, COMPLIANCE_OFFICER, VIEWER | Get retention policies | - |
| GET | `/compliance/audit` | Yes | ADMIN | Get audit trail | page, size, eventType |

**Request Types (GDPR Articles):**
- ACCESS (Article 15) - Right of access
- ERASURE (Article 17) - Right to be forgotten
- RECTIFICATION (Article 16) - Right to rectification
- PORTABILITY (Article 20) - Right to data portability

**Request Schemas:**
```json
// Create Data Subject Request
{
  "type": "ACCESS|ERASURE|RECTIFICATION|PORTABILITY",
  "subjectEmail": "string",
  "regulation": "GDPR|CCPA",
  "requestDetails": "string"
}

// Record Consent
{
  "subjectEmail": "string",
  "purpose": "MARKETING|ANALYTICS|PROFILING",
  "granted": true
}
```

---

## 6. AnalyticsController - `/api/v1/analytics`

Analytics and reporting endpoints.

| Method | Endpoint | Auth Required | Roles | Description | Parameters |
|--------|----------|---------------|-------|-------------|------------|
| GET | `/analytics/dashboard` | Yes | ADMIN, VIEWER | Get dashboard analytics | from, to (dates) |
| GET | `/analytics/extractions` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get extraction analytics | from, to |
| GET | `/analytics/migrations` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get migration analytics | from, to |
| GET | `/analytics/data-quality` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get quality analytics | - |
| GET | `/analytics/usage` | Yes | ADMIN | Get usage analytics | from, to |
| GET | `/analytics/compliance` | Yes | ADMIN, COMPLIANCE_OFFICER, VIEWER | Get compliance analytics | - |
| GET | `/analytics/performance` | Yes | ADMIN | Get performance analytics | from, to |
| POST | `/analytics/export` | Yes | ADMIN, VIEWER | Export analytics report | Body: export config |

**Analytics Metrics:**
- Dashboard: Overall system metrics, success rates
- Extractions: Volume, throughput, by source type
- Migrations: Duration, success rate, by destination
- Data Quality: Overall score, dimension scores, issues by severity
- Usage: Active users, feature usage
- Compliance: Request processing times, consent rates
- Performance: Response times, throughput, CPU usage

---

## 7. UserPreferencesController - `/api/v1/preferences`

User preferences management.

| Method | Endpoint | Auth Required | Roles | Description | Parameters |
|--------|----------|---------------|-------|-------------|------------|
| GET | `/preferences/theme` | Yes | All authenticated | Get theme preference | - |
| PUT | `/preferences/theme` | Yes | All authenticated | Update theme preference | Body: theme |
| GET | `/preferences` | Yes | All authenticated | Get all preferences | - |
| PUT | `/preferences` | Yes | All authenticated | Update preferences | Body: preferences |

**Request Schemas:**
```json
// User Preferences
{
  "theme": "light|dark",
  "language": "en|es|fr",
  "notificationsEnabled": true,
  "emailNotifications": true
}
```

---

## 8. ViewsController - `/api/v1/views`

Saved filter views management.

| Method | Endpoint | Auth Required | Roles | Description | Parameters |
|--------|----------|---------------|-------|-------------|------------|
| GET | `/views` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get all views | Query: module |
| GET | `/views/{viewName}` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get view by name | Path: viewName, Query: module |
| GET | `/views/default` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get default view | Query: module |
| POST | `/views` | Yes | ADMIN, DATA_ENGINEER | Create view | Body: view config |
| PUT | `/views/{viewName}` | Yes | ADMIN, DATA_ENGINEER | Update view | Path: viewName, Body: view |
| DELETE | `/views/{viewName}` | Yes | ADMIN, DATA_ENGINEER | Delete view | Path: viewName, Query: module |
| POST | `/views/{viewName}/set-default` | Yes | ADMIN, DATA_ENGINEER | Set default view | Path: viewName, Query: module |
| GET | `/views/count` | Yes | ADMIN, DATA_ENGINEER, VIEWER | Get view count | Query: module |

**Supported Modules:**
- extractions
- migrations
- data-quality
- compliance

**Request Schemas:**
```json
// Saved View
{
  "viewName": "string",
  "module": "extractions|migrations|data-quality|compliance",
  "filters": {
    "status": "COMPLETED",
    "dateFrom": "2024-01-01",
    "dateTo": "2024-01-31"
  },
  "sortBy": "createdAt",
  "sortOrder": "desc",
  "isDefault": false
}
```

---

## Summary by Controller

| Controller | Endpoint Count | Auth Protected | Public |
|------------|----------------|----------------|--------|
| AuthController | 4 | 2 | 2 |
| ExtractionController | 10 | 10 | 0 |
| MigrationController | 12 | 12 | 0 |
| DataQualityController | 10 | 10 | 0 |
| ComplianceController | 12 | 12 | 0 |
| AnalyticsController | 8 | 8 | 0 |
| UserPreferencesController | 4 | 4 | 0 |
| ViewsController | 8 | 8 | 0 |
| **TOTAL** | **78** | **66** | **2** |

---

## Authentication Flow

1. **Login**: POST `/api/v1/auth/login` → Get access & refresh tokens
2. **API Calls**: Include `Authorization: Bearer <access_token>` header
3. **Token Refresh**: POST `/api/v1/auth/refresh` when access token expires
4. **Logout**: POST `/api/v1/auth/logout` → Blacklist token

---

## Role-Based Access Control (RBAC)

| Role | Description | Access Level |
|------|-------------|--------------|
| ROLE_ADMIN | Full system access | All endpoints |
| ROLE_DATA_ENGINEER | Data operations | Extractions, Migrations, Data Quality |
| ROLE_COMPLIANCE_OFFICER | Compliance management | Compliance, Audit |
| ROLE_VIEWER | Read-only access | GET endpoints only |
| ROLE_USER | Basic authenticated user | Preferences only |

---

## Error Responses

All endpoints return standard error responses:

```json
// 400 Bad Request
{
  "status": 400,
  "message": "Validation error",
  "errors": ["Field 'name' is required"]
}

// 401 Unauthorized
{
  "status": 401,
  "message": "Not authenticated"
}

// 403 Forbidden
{
  "status": 403,
  "message": "Insufficient permissions"
}

// 404 Not Found
{
  "status": 404,
  "message": "Resource not found"
}

// 500 Internal Server Error
{
  "status": 500,
  "message": "Internal server error",
  "error": "Detailed error message"
}
```

---

## Pagination

All list endpoints support pagination:

**Query Parameters:**
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)

**Response:**
```json
{
  "content": [],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0,
  "pageSize": 20
}
```

---

## Filtering

Endpoints with filtering support:

- `/extractions?status=COMPLETED`
- `/migrations?status=RUNNING&page=0&size=20`
- `/data-quality/rules?dimension=VALIDITY`
- `/compliance/requests?type=ACCESS&status=PENDING`

---

## Performance Targets

| Endpoint Type | p95 Target | p99 Target |
|---------------|------------|------------|
| Simple GET | < 100ms | < 200ms |
| Complex POST | < 500ms | < 1s |
| Job Start/Stop | < 200ms | < 500ms |
| Analytics | < 300ms | < 600ms |
| Export | < 2s | < 5s |

---

**Last Updated**: January 13, 2025
**API Version**: v1
**Base URL**: http://localhost:8080/api/v1
