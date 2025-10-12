# JiVS Platform - API Quick Reference

Quick copy-paste examples for testing all endpoints.

## Authentication

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

**Save token**:
```bash
export TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | \
  jq -r '.data.accessToken')
```

### Get Current User
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/auth/me
```

### Refresh Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

### Logout
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/auth/logout
```

---

## Extractions

### Create Extraction
```bash
curl -X POST http://localhost:8080/api/v1/extractions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Data Extract",
    "sourceType": "JDBC",
    "connectionConfig": {
      "url": "jdbc:postgresql://localhost:5432/sourcedb",
      "username": "source_user",
      "password": "source_pass"
    },
    "extractionQuery": "SELECT * FROM customers LIMIT 1000"
  }'
```

### List Extractions
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/extractions?page=0&size=20&status=COMPLETED"
```

### Get Extraction
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/extractions/{id}
```

### Start Extraction
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/extractions/{id}/start
```

### Stop Extraction
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/extractions/{id}/stop
```

### Get Statistics
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/extractions/{id}/statistics
```

### Test Connection
```bash
curl -X POST http://localhost:8080/api/v1/extractions/test-connection \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "url": "jdbc:postgresql://localhost:5432/testdb",
    "username": "test",
    "password": "test"
  }'
```

### Get Logs
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/extractions/{id}/logs?limit=100"
```

### Delete Extraction
```bash
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/extractions/{id}
```

---

## Migrations

### Create Migration
```bash
curl -X POST http://localhost:8080/api/v1/migrations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Migration",
    "sourceConfig": {
      "type": "JDBC",
      "url": "jdbc:postgresql://source:5432/sourcedb",
      "username": "source_user",
      "password": "source_pass"
    },
    "targetConfig": {
      "type": "JDBC",
      "url": "jdbc:postgresql://target:5432/targetdb",
      "username": "target_user",
      "password": "target_pass"
    },
    "transformationRules": [
      {
        "sourceField": "customer_id",
        "targetField": "id",
        "transformation": "DIRECT"
      }
    ]
  }'
```

### List Migrations
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/migrations?page=0&size=20&status=RUNNING"
```

### Get Migration
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/migrations/{id}
```

### Start Migration
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/migrations/{id}/start
```

### Pause Migration
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/migrations/{id}/pause
```

### Resume Migration
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/migrations/{id}/resume
```

### Get Progress
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/migrations/{id}/progress
```

### Get Statistics
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/migrations/{id}/statistics
```

### Validate Migration
```bash
curl -X POST http://localhost:8080/api/v1/migrations/validate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Migration",
    "sourceConfig": {"type": "JDBC"},
    "targetConfig": {"type": "JDBC"}
  }'
```

### Rollback Migration
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/migrations/{id}/rollback
```

### Delete Migration
```bash
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/migrations/{id}
```

---

## Data Quality

### Get Dashboard
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/data-quality/dashboard
```

### Create Quality Rule
```bash
curl -X POST http://localhost:8080/api/v1/data-quality/rules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Email Validation",
    "dimension": "VALIDITY",
    "severity": "HIGH",
    "ruleExpression": "email LIKE '\''%@%.%'\''",
    "enabled": true
  }'
```

### List Quality Rules
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/data-quality/rules?page=0&size=20&dimension=VALIDITY"
```

### Get Quality Rule
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/data-quality/rules/{id}
```

### Update Quality Rule
```bash
curl -X PUT http://localhost:8080/api/v1/data-quality/rules/{id} \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Email Validation",
    "enabled": false
  }'
```

### Execute Quality Rule
```bash
curl -X POST http://localhost:8080/api/v1/data-quality/rules/{id}/execute \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "datasetId": "customers_dataset",
    "tableName": "customers"
  }'
```

### Get Quality Issues
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/data-quality/issues?page=0&size=20&severity=HIGH"
```

### Profile Dataset
```bash
curl -X POST http://localhost:8080/api/v1/data-quality/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "datasetId": "customers_dataset",
    "tableName": "customers"
  }'
```

### Delete Quality Rule
```bash
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/data-quality/rules/{id}
```

---

## Compliance

### Get Dashboard
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/compliance/dashboard
```

### Create Data Subject Request
```bash
curl -X POST http://localhost:8080/api/v1/compliance/requests \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ACCESS",
    "subjectEmail": "john.doe@example.com",
    "regulation": "GDPR",
    "requestDetails": "GDPR Article 15 - Right of Access"
  }'
```

**Request Types**: ACCESS, ERASURE, RECTIFICATION, PORTABILITY

### List Requests
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/compliance/requests?page=0&size=20&type=ACCESS&status=PENDING"
```

### Get Request
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/compliance/requests/{id}
```

### Update Request Status
```bash
curl -X PUT http://localhost:8080/api/v1/compliance/requests/{id}/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS"
  }'
```

### Process Request
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/compliance/requests/{id}/process
```

### Export Personal Data
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/compliance/requests/{id}/export
```

### Get Consents
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/compliance/consents?page=0&size=20&subjectEmail=john@example.com"
```

### Record Consent
```bash
curl -X POST http://localhost:8080/api/v1/compliance/consents \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "subjectEmail": "john.doe@example.com",
    "purpose": "MARKETING",
    "granted": true
  }'
```

**Consent Purposes**: MARKETING, ANALYTICS, PROFILING

### Revoke Consent
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/compliance/consents/{id}/revoke
```

### Get Retention Policies
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/compliance/retention-policies
```

### Get Audit Trail
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/compliance/audit?page=0&size=50&eventType=DATA_ACCESS"
```

---

## Analytics

### Dashboard Analytics
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/analytics/dashboard?from=2024-01-01&to=2024-12-31"
```

### Extraction Analytics
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/analytics/extractions?from=2024-01-01&to=2024-12-31"
```

### Migration Analytics
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/analytics/migrations?from=2024-01-01&to=2024-12-31"
```

### Data Quality Analytics
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/analytics/data-quality
```

### Usage Analytics
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/analytics/usage?from=2024-01-01&to=2024-12-31"
```

### Compliance Analytics
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/analytics/compliance
```

### Performance Analytics
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/analytics/performance?from=2024-01-01&to=2024-12-31"
```

### Export Report
```bash
curl -X POST http://localhost:8080/api/v1/analytics/export \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "format": "CSV",
    "reportType": "dashboard",
    "dateRange": {
      "from": "2024-01-01",
      "to": "2024-12-31"
    }
  }'
```

**Export Formats**: CSV, EXCEL, PDF

---

## User Preferences

### Get All Preferences
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/preferences
```

### Update Preferences
```bash
curl -X PUT http://localhost:8080/api/v1/preferences \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "theme": "dark",
    "language": "en",
    "notificationsEnabled": true,
    "emailNotifications": true
  }'
```

### Get Theme Preference
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/preferences/theme
```

### Update Theme
```bash
curl -X PUT http://localhost:8080/api/v1/preferences/theme \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "theme": "light"
  }'
```

---

## Saved Views

### Get All Views
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/views?module=extractions"
```

**Modules**: extractions, migrations, data-quality, compliance

### Create View
```bash
curl -X POST http://localhost:8080/api/v1/views \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "viewName": "My Completed Extractions",
    "module": "extractions",
    "filters": {
      "status": "COMPLETED",
      "dateFrom": "2024-01-01"
    },
    "sortBy": "createdAt",
    "sortOrder": "desc",
    "isDefault": false
  }'
```

### Get View by Name
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/views/My%20Completed%20Extractions?module=extractions"
```

### Update View
```bash
curl -X PUT "http://localhost:8080/api/v1/views/My%20Completed%20Extractions?module=extractions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "status": "COMPLETED",
      "dateFrom": "2024-06-01"
    }
  }'
```

### Set Default View
```bash
curl -X POST "http://localhost:8080/api/v1/views/My%20Completed%20Extractions/set-default?module=extractions" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Default View
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/views/default?module=extractions"
```

### Get View Count
```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/views/count?module=extractions"
```

### Delete View
```bash
curl -X DELETE "http://localhost:8080/api/v1/views/My%20Completed%20Extractions?module=extractions" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Common Query Parameters

- `page` - Page number (default: 0)
- `size` - Page size (default: 20)
- `status` - Filter by status
- `from` - Start date (format: yyyy-MM-dd)
- `to` - End date (format: yyyy-MM-dd)

---

## HTTP Status Codes

- **200 OK** - Request successful
- **201 Created** - Resource created
- **204 No Content** - Successful deletion
- **400 Bad Request** - Validation error
- **401 Unauthorized** - Not authenticated
- **403 Forbidden** - Insufficient permissions
- **404 Not Found** - Resource not found
- **500 Internal Server Error** - Server error

---

## Testing Tips

**Pretty print JSON output**:
```bash
curl ... | jq '.'
```

**Save response to file**:
```bash
curl ... -o response.json
```

**Show response headers**:
```bash
curl -i ...
```

**Verbose output (debug)**:
```bash
curl -v ...
```

**Follow redirects**:
```bash
curl -L ...
```

---

**Last Updated**: January 13, 2025
