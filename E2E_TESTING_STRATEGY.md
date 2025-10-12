# JiVS Platform - End-to-End Testing Strategy

**Document Version**: 1.0
**Created**: January 2025
**Author**: Claude AI
**Status**: Draft for Review

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Research Findings](#research-findings)
3. [Testing Framework Selection](#testing-framework-selection)
4. [Application Flow Inventory](#application-flow-inventory)
5. [Test Architecture & Design](#test-architecture--design)
6. [Comprehensive Test Plan](#comprehensive-test-plan)
7. [Implementation Roadmap](#implementation-roadmap)
8. [CI/CD Integration](#cicd-integration)
9. [Test Data Management](#test-data-management)
10. [Maintenance & Best Practices](#maintenance--best-practices)

---

## Executive Summary

### Purpose
This document outlines a comprehensive end-to-end (E2E) testing strategy for the JiVS Platform, ensuring every user flow from UI to backend is thoroughly tested and validated.

### Goals
1. **Comprehensive Coverage**: Test all critical user journeys and business flows
2. **Automated Execution**: Tests run automatically on every code change
3. **Fast Feedback**: Quick detection of bugs and regressions
4. **Maintainable**: Easy to update and extend as the application grows
5. **CI/CD Integration**: Seamless integration with GitHub Actions

### Key Metrics
- **Target Test Coverage**: 80%+ of critical user paths
- **Test Execution Time**: < 15 minutes for full suite
- **Flakiness Rate**: < 2% test failures due to instability
- **CI Integration**: 100% of pull requests tested before merge

---

## Research Findings

### Industry Best Practices (2025)

Based on comprehensive research of current E2E testing practices:

#### 1. Framework Selection
- **Playwright** emerges as the leading framework for 2025
- Supports TypeScript natively (perfect for our React TypeScript frontend)
- Cross-browser testing (Chrome, Firefox, Safari, Edge)
- Fast execution with parallel test running
- Excellent CI/CD integration
- Built-in test recording and debugging tools

#### 2. Test Organization Patterns
- **Page Object Model (POM)**: Industry standard for maintainable tests
- **Modular Test Suites**: Group tests by feature/functionality
- **Hierarchical Organization**: Mirror application structure
- **Test Isolation**: Each test runs independently
- **Test Data Factories**: Consistent, predictable test data

#### 3. CI/CD Integration Best Practices
- Run tests on every push/pull request
- Parallel execution with test sharding
- Artifact storage (screenshots, videos, reports)
- Fail fast on critical path failures
- Automatic retry for flaky tests (max 2 retries)

#### 4. Performance Optimization
- Use programmatic actions over UI actions where possible
- Reuse authentication state across tests
- Parallel test execution (4-8 workers)
- Docker container caching
- Strategic use of test fixtures

---

## Testing Framework Selection

### Selected Framework: **Playwright**

**Why Playwright?**

1. **Already Available**: Playwright MCP is already set up in this environment
2. **TypeScript Support**: Native TypeScript support matches our stack
3. **Cross-Browser**: Tests work across Chrome, Firefox, Safari, Edge
4. **Speed**: Faster than Cypress for parallel execution
5. **API Testing**: Can test both UI and API endpoints
6. **Advanced Features**: Network interception, multi-tab support, authentication state reuse
7. **Great Documentation**: Excellent docs and community support

**Playwright vs Alternatives**:

| Feature | Playwright | Cypress | Selenium |
|---------|-----------|---------|----------|
| TypeScript Support | ✅ Native | ✅ Good | ⚠️ Limited |
| Cross-Browser | ✅ All | ⚠️ Limited | ✅ All |
| Parallel Execution | ✅ Native | 💰 Paid | ⚠️ Complex |
| Speed | ✅ Fast | ✅ Fast | ❌ Slow |
| API Testing | ✅ Built-in | ✅ Plugin | ❌ No |
| CI/CD Integration | ✅ Excellent | ✅ Good | ⚠️ Complex |
| Learning Curve | ⚠️ Medium | ✅ Easy | ❌ Hard |
| Debugging Tools | ✅ Excellent | ✅ Excellent | ⚠️ Basic |

**Recommendation**: Playwright is the best choice for our needs.

---

## Application Flow Inventory

### Complete Application Analysis

Based on analysis of the JiVS Platform codebase, here are all flows that need testing:

#### 1. Authentication Flows
**Routes**: `/login`
**Backend**: `AuthController.java`

- User login with valid credentials
- User login with invalid credentials
- Token storage in localStorage
- Token refresh on expiration
- User logout (clear localStorage and session)
- Redirect after login to intended page
- Protected route access without authentication
- Session persistence across page refresh

#### 2. Dashboard Flow
**Routes**: `/dashboard`
**Backend**: `AnalyticsController.java`

- Dashboard loads with statistics cards
- Statistics display correct data (Extractions, Migrations, Quality, Compliance)
- Charts render correctly (Line chart, Pie chart)
- Performance metrics display (CPU, Memory, Storage, Network)
- Recent activities feed loads
- Error handling for failed analytics API calls
- Loading states display correctly

#### 3. Extractions Management Flow
**Routes**: `/extractions`
**Backend**: `ExtractionController.java`

**Create Extraction**:
- Open create extraction dialog
- Fill in extraction name
- Select source type (JDBC, SAP, File, API)
- Enter extraction query
- Submit and validate creation
- Verify extraction appears in list

**List Extractions**:
- View extractions table with data
- Pagination works correctly
- Filter by status (All, Pending, Running, Completed, Failed)
- Sort by columns
- Record count displays correctly
- Dates format properly

**Manage Extractions**:
- Start a pending extraction
- Stop a running extraction
- Delete an extraction with confirmation
- View extraction details
- View extraction statistics
- View extraction logs

#### 4. Migrations Management Flow
**Routes**: `/migrations`
**Backend**: `MigrationController.java`

**Create Migration**:
- Open create migration dialog
- Fill in migration name
- Configure source settings
- Configure target settings
- Submit and validate creation
- Verify migration appears in list

**List Migrations**:
- View migrations table with data
- Pagination works correctly
- Filter by status (All, Pending, Running, Paused, Completed, Failed)
- Progress bars display correctly (0-100%)
- Phase displayed correctly
- Dates format properly
- Records migrated count displays

**Manage Migrations**:
- Start a pending migration
- Pause a running migration
- Resume a paused migration
- Rollback a completed/failed migration
- Delete a migration with confirmation
- View migration progress
- View migration statistics

#### 5. Data Quality Flow
**Routes**: `/data-quality`
**Backend**: `DataQualityController.java`

**Dashboard View**:
- Overall quality score displays (no crash)
- Dimension scores display (Completeness, Accuracy, Consistency, Validity, Uniqueness, Timeliness)
- Quality issues summary
- Profile statistics

**Quality Rules**:
- List quality rules
- Create new quality rule
- Edit existing rule
- Delete rule with confirmation
- Execute rule manually
- View rule results

**Quality Issues**:
- List quality issues
- Filter by severity
- Filter by dimension
- View issue details
- Resolve issues

**Data Profiling**:
- Create profile for dataset
- View profile results
- Download profile report

#### 6. Compliance Flow
**Routes**: `/compliance`
**Backend**: `ComplianceController.java`

**Dashboard View**:
- Compliance score displays (no crash)
- GDPR compliance percentage
- CCPA compliance percentage
- Request statistics

**Data Subject Requests**:
- List compliance requests
- Create new request (Access, Erasure, Rectification, Portability)
- Process request
- Export request data
- View request status
- Filter requests by type/status

**Consent Management**:
- List consents
- Create new consent
- Revoke consent
- View consent history

**Audit Logs**:
- View audit trail
- Filter by date range
- Filter by user
- Filter by action type
- Export audit logs

#### 7. Business Objects Flow
**Routes**: `/business-objects`
**Backend**: TBD (placeholder page)

- Navigate to Business Objects page
- Verify placeholder displays
- (Future: Full implementation tests)

#### 8. Documents Flow
**Routes**: `/documents`
**Backend**: TBD (placeholder page)

- Navigate to Documents page
- Verify placeholder displays
- (Future: Full implementation tests)

#### 9. Settings Flow
**Routes**: `/settings`
**Backend**: TBD (placeholder page)

- Navigate to Settings page
- Verify placeholder displays
- (Future: Full implementation tests)

#### 10. Analytics Flow
**Routes**: `/analytics`
**Backend**: `AnalyticsController.java` (partial)

- Navigate to Analytics page
- Verify placeholder displays
- (Future: Full reporting tests)

#### 11. Navigation Flow
**Global**: All pages

- Sidebar navigation works
- All menu items accessible
- Breadcrumbs display correctly
- Active route highlighted
- User menu accessible
- Logout from menu works

---

## Test Architecture & Design

### Architecture Overview

```
tests/
├── e2e/                          # End-to-end tests
│   ├── specs/                    # Test specifications
│   │   ├── auth/                 # Authentication tests
│   │   │   ├── login.spec.ts
│   │   │   ├── logout.spec.ts
│   │   │   └── session.spec.ts
│   │   ├── dashboard/            # Dashboard tests
│   │   │   └── dashboard.spec.ts
│   │   ├── extractions/          # Extraction tests
│   │   │   ├── create.spec.ts
│   │   │   ├── list.spec.ts
│   │   │   └── manage.spec.ts
│   │   ├── migrations/           # Migration tests
│   │   │   ├── create.spec.ts
│   │   │   ├── list.spec.ts
│   │   │   └── manage.spec.ts
│   │   ├── data-quality/         # Data quality tests
│   │   │   ├── dashboard.spec.ts
│   │   │   ├── rules.spec.ts
│   │   │   └── issues.spec.ts
│   │   ├── compliance/           # Compliance tests
│   │   │   ├── dashboard.spec.ts
│   │   │   ├── requests.spec.ts
│   │   │   └── consents.spec.ts
│   │   └── navigation/           # Navigation tests
│   │       └── navigation.spec.ts
│   ├── pages/                    # Page Object Models
│   │   ├── auth/
│   │   │   └── LoginPage.ts
│   │   ├── dashboard/
│   │   │   └── DashboardPage.ts
│   │   ├── extractions/
│   │   │   └── ExtractionsPage.ts
│   │   ├── migrations/
│   │   │   └── MigrationsPage.ts
│   │   ├── data-quality/
│   │   │   └── DataQualityPage.ts
│   │   └── compliance/
│   │       └── CompliancePage.ts
│   ├── fixtures/                 # Test data fixtures
│   │   ├── users.ts
│   │   ├── extractions.ts
│   │   ├── migrations.ts
│   │   └── quality-rules.ts
│   ├── helpers/                  # Helper utilities
│   │   ├── auth.helper.ts
│   │   ├── api.helper.ts
│   │   └── database.helper.ts
│   └── config/                   # Test configuration
│       └── playwright.config.ts
├── api/                          # API integration tests
│   ├── auth.api.spec.ts
│   ├── extractions.api.spec.ts
│   ├── migrations.api.spec.ts
│   └── data-quality.api.spec.ts
└── README.md                     # Testing documentation
```

### Design Patterns

#### 1. Page Object Model (POM)

**Example: LoginPage.ts**
```typescript
export class LoginPage {
  constructor(private page: Page) {}

  // Selectors
  private readonly usernameInput = '[name="username"]';
  private readonly passwordInput = '[name="password"]';
  private readonly submitButton = 'button[type="submit"]';
  private readonly errorAlert = '[role="alert"]';

  // Actions
  async goto() {
    await this.page.goto('/login');
  }

  async login(username: string, password: string) {
    await this.page.fill(this.usernameInput, username);
    await this.page.fill(this.passwordInput, password);
    await this.page.click(this.submitButton);
  }

  async getErrorMessage() {
    return await this.page.textContent(this.errorAlert);
  }

  // Assertions
  async isLoggedIn() {
    await this.page.waitForURL('/dashboard');
    return this.page.url().includes('/dashboard');
  }
}
```

#### 2. Test Fixtures (Reusable Setup)

**Example: auth.fixture.ts**
```typescript
import { test as base } from '@playwright/test';
import { LoginPage } from '../pages/auth/LoginPage';

export const test = base.extend<{
  loginPage: LoginPage;
  authenticatedPage: Page;
}>({
  loginPage: async ({ page }, use) => {
    const loginPage = new LoginPage(page);
    await use(loginPage);
  },

  authenticatedPage: async ({ page }, use) => {
    // Reuse authentication state
    await page.goto('/login');
    const loginPage = new LoginPage(page);
    await loginPage.login('admin', 'password');
    await use(page);
  },
});
```

#### 3. Test Data Factories

**Example: users.factory.ts**
```typescript
export const testUsers = {
  admin: {
    username: 'admin',
    password: 'password',
    role: 'ROLE_ADMIN',
  },
  dataEngineer: {
    username: 'engineer',
    password: 'password123',
    role: 'ROLE_DATA_ENGINEER',
  },
  viewer: {
    username: 'viewer',
    password: 'password123',
    role: 'ROLE_VIEWER',
  },
};

export function createExtractionData(overrides = {}) {
  return {
    name: `Test Extraction ${Date.now()}`,
    sourceType: 'JDBC',
    extractionQuery: 'SELECT * FROM test_table',
    ...overrides,
  };
}
```

---

## Comprehensive Test Plan

### Test Case Structure

Each test case follows this structure:
- **Test ID**: Unique identifier
- **Test Name**: Descriptive name
- **Priority**: Critical / High / Medium / Low
- **Preconditions**: Required setup
- **Steps**: Test execution steps
- **Expected Result**: What should happen
- **API Validation**: Backend checks

### 1. Authentication Test Cases

#### AUTH-001: Successful Login
- **Priority**: Critical
- **Preconditions**: User exists in database
- **Steps**:
  1. Navigate to /login
  2. Enter valid username
  3. Enter valid password
  4. Click "Sign In"
- **Expected Result**:
  - User redirected to /dashboard
  - Access token stored in localStorage
  - User info available in AuthContext
  - Backend returns 200 OK
- **API Validation**:
  - POST /api/v1/auth/login returns accessToken and refreshToken

#### AUTH-002: Failed Login (Invalid Credentials)
- **Priority**: Critical
- **Steps**:
  1. Navigate to /login
  2. Enter invalid username
  3. Enter invalid password
  4. Click "Sign In"
- **Expected Result**:
  - Error message displays
  - User remains on /login
  - No token stored
  - Backend returns 401 Unauthorized

#### AUTH-003: Logout
- **Priority**: Critical
- **Preconditions**: User is authenticated
- **Steps**:
  1. Click user menu
  2. Click "Logout"
- **Expected Result**:
  - User redirected to /login
  - localStorage cleared
  - AuthContext cleared
  - Backend /logout endpoint called

#### AUTH-004: Protected Route Without Auth
- **Priority**: High
- **Steps**:
  1. Clear localStorage
  2. Navigate to /dashboard
- **Expected Result**:
  - User redirected to /login
  - Return URL stored for post-login redirect

#### AUTH-005: Session Persistence
- **Priority**: High
- **Preconditions**: User logged in
- **Steps**:
  1. Refresh page
- **Expected Result**:
  - User remains authenticated
  - Dashboard loads correctly

#### AUTH-006: Token Refresh
- **Priority**: High
- **Preconditions**: Access token expired
- **Steps**:
  1. Make API call with expired token
- **Expected Result**:
  - Token automatically refreshed
  - Request retried with new token
  - No visible error to user

### 2. Dashboard Test Cases

#### DASH-001: Dashboard Load
- **Priority**: Critical
- **Preconditions**: User authenticated
- **Steps**:
  1. Navigate to /dashboard
- **Expected Result**:
  - 4 statistics cards display
  - Line chart renders
  - Pie chart renders
  - Performance metrics display
  - No console errors
- **API Validation**:
  - GET /api/v1/analytics/dashboard returns data

#### DASH-002: Dashboard Statistics Accuracy
- **Priority**: High
- **Steps**:
  1. Load dashboard
  2. Compare displayed stats with API response
- **Expected Result**:
  - All numbers match API response
  - Percentages calculated correctly
  - Charts display correct data

#### DASH-003: Dashboard Error Handling
- **Priority**: Medium
- **Steps**:
  1. Mock API to return error
  2. Load dashboard
- **Expected Result**:
  - Error message displays
  - Retry option available
  - Page doesn't crash

### 3. Extractions Test Cases

#### EXT-001: Create Extraction
- **Priority**: Critical
- **Preconditions**: User authenticated with ADMIN or DATA_ENGINEER role
- **Steps**:
  1. Navigate to /extractions
  2. Click "New Extraction"
  3. Fill in name: "Test Extraction"
  4. Select source type: "JDBC"
  5. Enter query: "SELECT * FROM test"
  6. Click "Create"
- **Expected Result**:
  - Dialog closes
  - Success message displays
  - New extraction appears in table
  - Table refreshes
- **API Validation**:
  - POST /api/v1/extractions returns 201 Created
  - Response contains extraction ID

#### EXT-002: List Extractions
- **Priority**: Critical
- **Steps**:
  1. Navigate to /extractions
- **Expected Result**:
  - Table displays with columns: Name, Source Type, Status, Records, Created At, Actions
  - Pagination controls display
  - Statistics cards show counts
  - No null/undefined values
- **API Validation**:
  - GET /api/v1/extractions returns paginated data

#### EXT-003: Filter Extractions by Status
- **Priority**: High
- **Steps**:
  1. Navigate to /extractions
  2. Select "Running" from status filter
- **Expected Result**:
  - Table updates to show only running extractions
  - Statistics update
  - URL updates with filter param
- **API Validation**:
  - GET /api/v1/extractions?status=RUNNING called

#### EXT-004: Pagination
- **Priority**: High
- **Steps**:
  1. Navigate to /extractions
  2. Change rows per page to 50
  3. Navigate to page 2
- **Expected Result**:
  - Table updates with new page
  - Page indicator updates
  - Correct data displayed
- **API Validation**:
  - GET /api/v1/extractions?page=1&size=50 called

#### EXT-005: Start Extraction
- **Priority**: Critical
- **Preconditions**: Extraction with status PENDING exists
- **Steps**:
  1. Navigate to /extractions
  2. Click "Start" on pending extraction
- **Expected Result**:
  - Status changes to RUNNING
  - Start button disabled
  - Stop button enabled
- **API Validation**:
  - POST /api/v1/extractions/{id}/start returns 200 OK

#### EXT-006: Stop Extraction
- **Priority**: Critical
- **Preconditions**: Extraction with status RUNNING exists
- **Steps**:
  1. Navigate to /extractions
  2. Click "Stop" on running extraction
- **Expected Result**:
  - Status changes to STOPPED
  - Stop button disabled
- **API Validation**:
  - POST /api/v1/extractions/{id}/stop returns 200 OK

#### EXT-007: Delete Extraction
- **Priority**: High
- **Steps**:
  1. Navigate to /extractions
  2. Click "Delete" on extraction
  3. Confirm deletion
- **Expected Result**:
  - Confirmation dialog appears
  - After confirm, extraction removed from table
  - Success message displays
- **API Validation**:
  - DELETE /api/v1/extractions/{id} returns 200 OK

### 4. Migrations Test Cases

#### MIG-001: Create Migration
- **Priority**: Critical
- **Preconditions**: User authenticated
- **Steps**:
  1. Navigate to /migrations
  2. Click "New Migration"
  3. Fill in name: "Test Migration"
  4. Configure source
  5. Configure target
  6. Click "Create"
- **Expected Result**:
  - Dialog closes
  - Success message
  - New migration in table
- **API Validation**:
  - POST /api/v1/migrations returns 201 Created

#### MIG-002: List Migrations
- **Priority**: Critical
- **Steps**:
  1. Navigate to /migrations
- **Expected Result**:
  - Table displays: Name, Status, Phase, Progress, Records, Created At, Actions
  - Progress bars show 0-100%
  - Dates formatted correctly
  - Phase displayed (not null)
- **API Validation**:
  - GET /api/v1/migrations returns data

#### MIG-003: Migration Progress Display
- **Priority**: High
- **Preconditions**: Migration with progress exists
- **Steps**:
  1. View migration in table
- **Expected Result**:
  - Progress bar displays correctly
  - Progress percentage 0-100%
  - Records migrated/total displayed
  - Numbers formatted with commas

#### MIG-004: Start Migration
- **Priority**: Critical
- **Steps**:
  1. Click "Start" on pending migration
- **Expected Result**:
  - Status changes to RUNNING
  - Phase updates
  - Progress starts incrementing
- **API Validation**:
  - POST /api/v1/migrations/{id}/start returns 200

#### MIG-005: Pause Migration
- **Priority**: High
- **Steps**:
  1. Click "Pause" on running migration
- **Expected Result**:
  - Status changes to PAUSED
  - Progress stops
  - Resume button appears
- **API Validation**:
  - POST /api/v1/migrations/{id}/pause returns 200

#### MIG-006: Resume Migration
- **Priority**: High
- **Steps**:
  1. Click "Resume" on paused migration
- **Expected Result**:
  - Status changes to RUNNING
  - Progress continues
- **API Validation**:
  - POST /api/v1/migrations/{id}/resume returns 200

#### MIG-007: Rollback Migration
- **Priority**: Critical
- **Steps**:
  1. Click "Rollback" on completed migration
  2. Confirm rollback
- **Expected Result**:
  - Confirmation dialog appears
  - Status changes to ROLLING_BACK
  - After completion, data reverted
- **API Validation**:
  - POST /api/v1/migrations/{id}/rollback returns 200

#### MIG-008: Delete Migration
- **Priority**: High
- **Steps**:
  1. Click "Delete"
  2. Confirm
- **Expected Result**:
  - Migration removed
  - Table updates
- **API Validation**:
  - DELETE /api/v1/migrations/{id} returns 200

### 5. Data Quality Test Cases

#### DQ-001: Dashboard Load
- **Priority**: Critical
- **Steps**:
  1. Navigate to /data-quality
- **Expected Result**:
  - Overall score displays (no crash)
  - 6 dimension scores display
  - All scores have fallback to 0
  - No .toFixed() errors
- **API Validation**:
  - GET /api/v1/data-quality/dashboard returns data

#### DQ-002: Create Quality Rule
- **Priority**: High
- **Steps**:
  1. Click "New Rule"
  2. Fill in rule details
  3. Click "Create"
- **Expected Result**:
  - Rule created
  - Appears in rules table
- **API Validation**:
  - POST /api/v1/data-quality/rules returns 201

#### DQ-003: Execute Quality Rule
- **Priority**: Critical
- **Steps**:
  1. Click "Execute" on rule
- **Expected Result**:
  - Rule executes
  - Results display
  - Issues generated if violations found
- **API Validation**:
  - POST /api/v1/data-quality/rules/{id}/execute returns results

#### DQ-004: View Quality Issues
- **Priority**: High
- **Steps**:
  1. Navigate to issues tab
- **Expected Result**:
  - Issues table displays
  - Filter by severity works
  - Pagination works

#### DQ-005: Data Profiling
- **Priority**: Medium
- **Steps**:
  1. Click "Profile Dataset"
  2. Select dataset
  3. Execute
- **Expected Result**:
  - Profile runs
  - Statistics display
  - Report downloadable
- **API Validation**:
  - POST /api/v1/data-quality/profile returns profile data

### 6. Compliance Test Cases

#### COMP-001: Dashboard Load
- **Priority**: Critical
- **Steps**:
  1. Navigate to /compliance
- **Expected Result**:
  - Compliance score displays (no crash)
  - GDPR score displays with fallback
  - CCPA score displays with fallback
  - No .toFixed() errors
- **API Validation**:
  - GET /api/v1/compliance/dashboard returns data

#### COMP-002: Create Data Subject Request
- **Priority**: Critical
- **Steps**:
  1. Click "New Request"
  2. Select type (Access/Erasure/Rectification/Portability)
  3. Fill in subject details
  4. Click "Create"
- **Expected Result**:
  - Request created
  - Appears in requests table
  - Status is PENDING
- **API Validation**:
  - POST /api/v1/compliance/requests returns 201

#### COMP-003: Process Data Subject Request
- **Priority**: Critical
- **Steps**:
  1. Click "Process" on pending request
- **Expected Result**:
  - Processing starts
  - Status updates
  - Data collected/deleted based on type
- **API Validation**:
  - POST /api/v1/compliance/requests/{id}/process returns 200

#### COMP-004: Export Request Data
- **Priority**: High
- **Steps**:
  1. Click "Export" on access request
- **Expected Result**:
  - Data exported as JSON/CSV
  - File downloads
- **API Validation**:
  - GET /api/v1/compliance/requests/{id}/export returns file

#### COMP-005: Consent Management
- **Priority**: High
- **Steps**:
  1. Navigate to consents tab
  2. View consent list
  3. Click "Revoke" on consent
- **Expected Result**:
  - Consent revoked
  - Status updated
  - Audit log created
- **API Validation**:
  - POST /api/v1/compliance/consents/{id}/revoke returns 200

### 7. Navigation Test Cases

#### NAV-001: Sidebar Navigation
- **Priority**: Critical
- **Steps**:
  1. Click each menu item
- **Expected Result**:
  - All pages load
  - No 404 errors
  - Active item highlighted
  - Correct page displays

#### NAV-002: Breadcrumb Navigation
- **Priority**: Medium
- **Steps**:
  1. Navigate through pages
  2. Check breadcrumbs
- **Expected Result**:
  - Breadcrumbs update correctly
  - Clicking breadcrumb navigates correctly

#### NAV-003: User Menu
- **Priority**: High
- **Steps**:
  1. Click user avatar/menu
- **Expected Result**:
  - Menu opens
  - User info displays
  - Logout option available

### Test Execution Order

**Critical Path Tests (Run First)**:
1. Authentication (AUTH-001 to AUTH-006)
2. Dashboard Load (DASH-001)
3. Extractions Create/List (EXT-001, EXT-002)
4. Migrations Create/List (MIG-001, MIG-002)
5. Navigation (NAV-001)

**Secondary Tests**:
- All management operations (Start, Stop, Delete)
- Filtering and pagination
- Data Quality features
- Compliance features

**Tertiary Tests**:
- Error handling
- Edge cases
- Performance tests

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)
**Goal**: Set up testing infrastructure

**Tasks**:
1. Install Playwright dependencies
2. Configure playwright.config.ts
3. Set up test directory structure
4. Create base Page Object Models
5. Create authentication helpers
6. Write first test (AUTH-001: Login)
7. Verify test runs locally

**Deliverables**:
- Working Playwright setup
- 1 passing test
- Documentation for running tests

### Phase 2: Authentication Tests (Week 1)
**Goal**: Complete all authentication tests

**Tasks**:
1. Implement all AUTH test cases (AUTH-001 to AUTH-006)
2. Create reusable auth fixtures
3. Test in all browsers
4. Handle authentication state reuse

**Deliverables**:
- 6 passing authentication tests
- Reusable auth fixtures

### Phase 3: Core Feature Tests (Week 2)
**Goal**: Test main application features

**Tasks**:
1. Dashboard tests (DASH-001 to DASH-003)
2. Extractions tests (EXT-001 to EXT-007)
3. Migrations tests (MIG-001 to MIG-008)
4. Navigation tests (NAV-001 to NAV-003)

**Deliverables**:
- 18+ passing tests
- Page Object Models for all pages

### Phase 4: Advanced Feature Tests (Week 3)
**Goal**: Test Data Quality and Compliance

**Tasks**:
1. Data Quality tests (DQ-001 to DQ-005)
2. Compliance tests (COMP-001 to COMP-005)
3. Error handling tests
4. Edge case tests

**Deliverables**:
- 10+ passing tests
- Complete test coverage of existing features

### Phase 5: CI/CD Integration (Week 3)
**Goal**: Automate test execution

**Tasks**:
1. Create GitHub Actions workflow
2. Configure test sharding
3. Set up artifact storage
4. Configure failure notifications
5. Add badge to README

**Deliverables**:
- Working CI/CD pipeline
- Tests run on every PR
- Test reports in artifacts

### Phase 6: Optimization & Documentation (Week 4)
**Goal**: Optimize and document

**Tasks**:
1. Optimize test execution time
2. Reduce flakiness
3. Add test recordings
4. Write testing guidelines
5. Create maintenance procedures
6. Team training

**Deliverables**:
- Fast, stable tests
- Complete documentation
- Team knowledge transfer

---

## CI/CD Integration

### GitHub Actions Workflow

**File**: `.github/workflows/e2e-tests.yml`

```yaml
name: E2E Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test:
    timeout-minutes: 60
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        shardIndex: [1, 2, 3, 4]
        shardTotal: [4]

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '18'

      - name: Install dependencies
        run: |
          cd frontend
          npm ci

      - name: Install Playwright Browsers
        run: npx playwright install --with-deps

      - name: Start Backend
        run: |
          cd backend
          mvn clean install -DskipTests
          java -jar target/jivs-platform-1.0.0.jar &
          sleep 30  # Wait for backend to start

      - name: Start Frontend
        run: |
          cd frontend
          npm run dev &
          sleep 10  # Wait for frontend to start

      - name: Run E2E tests
        run: |
          cd frontend
          npx playwright test --shard=${{ matrix.shardIndex }}/${{ matrix.shardTotal }}

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report-${{ matrix.shardIndex }}
          path: frontend/playwright-report/
          retention-days: 30

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results-${{ matrix.shardIndex }}
          path: frontend/test-results/
          retention-days: 30
```

### Pre-commit Hook

**File**: `.husky/pre-commit`

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

# Run critical path tests before commit
cd frontend && npx playwright test tests/e2e/specs/auth --reporter=line
```

### Test Execution Triggers

1. **On every commit**: Run critical path tests
2. **On pull request**: Run full test suite
3. **On merge to main**: Run full test suite + deployment tests
4. **Nightly**: Run extended test suite with performance tests

---

## Test Data Management

### Strategy

1. **Database Seeding**: Use Flyway migrations to create test data
2. **Test Isolation**: Each test creates its own data
3. **Cleanup**: Tests clean up after themselves
4. **Factories**: Use factory functions for test data creation

### Test Database Setup

```sql
-- Create test user
INSERT INTO users (username, email, password, enabled)
VALUES ('admin', 'admin@jivs.com', '$2a$10$...', true);

-- Create test extraction
INSERT INTO extractions (name, source_type, status, created_by)
VALUES ('Test Extraction', 'JDBC', 'PENDING', 1);

-- Create test migration
INSERT INTO migrations (name, status, phase, progress, created_by)
VALUES ('Test Migration', 'PENDING', 'PLANNING', 0, 1);
```

### Data Factories

```typescript
// fixtures/extractions.ts
export function createTestExtraction(overrides = {}) {
  return {
    name: `E2E Test Extraction ${Date.now()}`,
    sourceType: 'JDBC',
    connectionConfig: {
      host: 'localhost',
      port: 5432,
      database: 'test',
    },
    extractionQuery: 'SELECT * FROM test_table',
    ...overrides,
  };
}

// fixtures/migrations.ts
export function createTestMigration(overrides = {}) {
  return {
    name: `E2E Test Migration ${Date.now()}`,
    sourceConfig: { type: 'JDBC', host: 'source' },
    targetConfig: { type: 'JDBC', host: 'target' },
    ...overrides,
  };
}
```

### State Management

**Authentication State Reuse**:
```typescript
// Save authentication state after first login
await page.context().storageState({ path: 'auth.json' });

// Reuse in other tests
const context = await browser.newContext({ storageState: 'auth.json' });
```

---

## Maintenance & Best Practices

### Best Practices

1. **Test Isolation**: Each test should be independent
2. **Descriptive Names**: Test names should describe what they test
3. **Arrange-Act-Assert**: Follow AAA pattern
4. **Avoid Sleep**: Use Playwright's built-in waiting
5. **Reuse Code**: Use Page Objects and fixtures
6. **Test Real Scenarios**: Test actual user workflows
7. **Handle Async**: Always await async operations
8. **Clean Data**: Clean up test data after tests
9. **Meaningful Assertions**: Assert actual business requirements
10. **Fast Tests**: Optimize for speed

### Code Quality

**Linting**:
```bash
npm run lint:tests
```

**Formatting**:
```bash
npm run format:tests
```

**Type Checking**:
```bash
npm run typecheck:tests
```

### Debugging

**Run single test**:
```bash
npx playwright test tests/e2e/specs/auth/login.spec.ts
```

**Run with UI**:
```bash
npx playwright test --ui
```

**Debug mode**:
```bash
npx playwright test --debug
```

**Generate test**:
```bash
npx playwright codegen http://localhost:3001
```

### Test Reporting

**HTML Report**:
```bash
npx playwright show-report
```

**JUnit Report** (for CI):
```typescript
// playwright.config.ts
reporter: [
  ['html'],
  ['junit', { outputFile: 'test-results/junit.xml' }],
  ['json', { outputFile: 'test-results/results.json' }],
]
```

### Monitoring Test Health

**Key Metrics**:
- Test execution time
- Pass/fail rate
- Flaky test rate
- Code coverage
- Time to fix failures

**Flaky Test Management**:
```typescript
// playwright.config.ts
retries: process.env.CI ? 2 : 0,  // Retry flaky tests in CI
```

### Documentation Requirements

1. **Test Plan**: This document
2. **Test Cases**: In test files as comments
3. **Page Objects**: JSDoc comments
4. **README**: How to run tests
5. **Troubleshooting Guide**: Common issues
6. **Release Notes**: Test coverage for each release

---

## Success Metrics

### Coverage Metrics
- **User Flows**: 100% of critical paths tested
- **API Endpoints**: 80%+ endpoints covered
- **UI Components**: 70%+ components tested
- **Code Coverage**: 60%+ (via Istanbul)

### Performance Metrics
- **Test Execution Time**: < 15 minutes full suite
- **Single Test Time**: < 30 seconds average
- **CI Pipeline Time**: < 20 minutes total
- **Feedback Loop**: < 5 minutes for critical tests

### Quality Metrics
- **Pass Rate**: > 95% on main branch
- **Flakiness Rate**: < 2% false failures
- **Bug Detection**: > 90% bugs caught before production
- **Time to Fix**: < 24 hours for test failures

---

## Next Steps

### Immediate Actions (This Week)
1. Review and approve this strategy document
2. Set up Playwright in the project
3. Write first authentication test
4. Verify test runs locally and in CI

### Short Term (Next 2 Weeks)
1. Implement all authentication tests
2. Implement core feature tests (Dashboard, Extractions, Migrations)
3. Set up GitHub Actions workflow
4. Train team on writing tests

### Long Term (Next Month)
1. Complete all test cases
2. Achieve 80% coverage target
3. Optimize test execution time
4. Document testing guidelines
5. Establish test maintenance procedures

---

## Conclusion

This comprehensive E2E testing strategy provides:
- **Clear Direction**: Detailed plan for implementation
- **Best Practices**: Industry-standard approaches
- **Complete Coverage**: All application flows tested
- **Automation**: CI/CD integration for continuous testing
- **Maintainability**: Page Object Model and fixtures
- **Quality Assurance**: Metrics and monitoring

By following this strategy, the JiVS Platform will have robust, reliable, and maintainable end-to-end tests that catch bugs early and ensure high quality releases.

**Ready to implement!** 🚀

---

**Document Status**: Ready for Review
**Next Review Date**: 2 weeks after implementation start
**Owner**: Development Team
**Approvers**: Tech Lead, QA Lead

