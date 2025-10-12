# JiVS Platform - E2E Testing Guide

This directory contains comprehensive end-to-end (E2E) tests for the JiVS Platform using Playwright.

## 📋 Table of Contents

- [Quick Start](#quick-start)
- [Test Structure](#test-structure)
- [Running Tests](#running-tests)
- [Writing Tests](#writing-tests)
- [CI/CD Integration](#cicd-integration)
- [Troubleshooting](#troubleshooting)

## 🚀 Quick Start

### Prerequisites

- Node.js 18+ and npm 9+
- Backend running on `http://localhost:8080`
- Frontend running on `http://localhost:3001`

### Installation

Playwright is already installed. If you need to reinstall:

```bash
npm install
npx playwright install chromium
```

### Run Your First Test

```bash
# Run all E2E tests
npm run test:e2e

# Run authentication tests only
npm run test:e2e:auth

# Run tests with UI (recommended for debugging)
npm run test:e2e:ui
```

## 📁 Test Structure

```
tests/
├── e2e/
│   ├── specs/                    # Test specifications
│   │   ├── auth/                 # Authentication tests
│   │   │   ├── login.spec.ts     # Login tests (AUTH-001 to AUTH-008)
│   │   │   ├── logout.spec.ts    # Logout tests (AUTH-009 to AUTH-011)
│   │   │   └── session.spec.ts   # Session tests (AUTH-012 to AUTH-017)
│   │   ├── dashboard/            # Dashboard tests
│   │   │   └── dashboard.spec.ts # Dashboard tests (DASH-001 to DASH-008)
│   │   ├── extractions/          # Extraction management tests
│   │   ├── migrations/           # Migration management tests
│   │   ├── data-quality/         # Data quality tests
│   │   ├── compliance/           # Compliance tests
│   │   └── navigation/           # Navigation tests
│   ├── pages/                    # Page Object Models (POM)
│   │   ├── auth/
│   │   │   └── LoginPage.ts      # Login page abstraction
│   │   ├── dashboard/
│   │   │   └── DashboardPage.ts  # Dashboard page abstraction
│   │   ├── extractions/
│   │   │   └── ExtractionsPage.ts
│   │   └── migrations/
│   │       └── MigrationsPage.ts
│   ├── fixtures/                 # Test data
│   │   ├── users.ts              # Test user credentials
│   │   ├── extractions.ts        # Extraction test data
│   │   └── migrations.ts         # Migration test data
│   ├── helpers/                  # Utility functions
│   │   ├── auth.helper.ts        # Authentication helpers
│   │   └── api.helper.ts         # API request helpers
│   └── config/                   # Test configuration
└── api/                          # API integration tests (future)
```

## 🏃 Running Tests

### Available Commands

| Command | Description |
|---------|-------------|
| `npm run test:e2e` | Run all E2E tests |
| `npm run test:e2e:ui` | Run tests with Playwright UI |
| `npm run test:e2e:headed` | Run tests in headed mode (see browser) |
| `npm run test:e2e:debug` | Run tests in debug mode |
| `npm run test:e2e:report` | Show HTML test report |
| `npm run test:e2e:codegen` | Record new tests with codegen |
| `npm run test:e2e:auth` | Run authentication tests only |
| `npm run test:e2e:critical` | Run critical path tests |

### Run Specific Tests

```bash
# Run single test file
npx playwright test tests/e2e/specs/auth/login.spec.ts

# Run tests matching pattern
npx playwright test --grep "successful login"

# Run tests in specific browser
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit

# Run tests in parallel
npx playwright test --workers=4
```

### Debug Tests

```bash
# Debug mode (step through tests)
npm run test:e2e:debug

# Debug specific test
npx playwright test tests/e2e/specs/auth/login.spec.ts --debug

# Record test execution
npx playwright test --trace on

# View trace
npx playwright show-trace trace.zip
```

## ✍️ Writing Tests

### Test Naming Convention

Tests follow the naming pattern: `TEST-ID: Description`

Example: `AUTH-001: Successful login with valid credentials`

### Using Page Objects

```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/auth/LoginPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';

test.describe('My Feature', () => {
  test('should do something', async ({ page }) => {
    // Setup
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    // Execute
    await loginPage.login('admin', 'password');

    // Verify
    await expect(page).toHaveURL('/dashboard');
  });
});
```

### Using Authentication Helpers

```typescript
import { loginAsRole, setupAuthenticatedSession } from '../../helpers/auth.helper';

// Quick login for tests
test.beforeEach(async ({ page }) => {
  await setupAuthenticatedSession(page, 'admin');
});

// Or login via UI
test('test name', async ({ page }) => {
  await loginAsRole(page, 'admin');
  // ... rest of test
});
```

### Using Test Fixtures

```typescript
import { testUsers } from '../../fixtures/users';
import { createTestExtraction } from '../../fixtures/extractions';

test('create extraction', async ({ page }) => {
  const user = testUsers.admin;
  const extractionData = createTestExtraction({
    name: 'My Test Extraction',
    sourceType: 'JDBC',
  });

  // Use the data...
});
```

## 🔧 Test Configuration

### Playwright Config

Configuration is in `playwright.config.ts`:

- **Base URL**: `http://localhost:3001`
- **Timeout**: 60 seconds per test
- **Retries**: 2 retries on CI, 0 locally
- **Workers**: 4 parallel workers on CI
- **Browsers**: Chromium, Firefox, WebKit
- **Reports**: HTML, JSON, JUnit

### Environment Variables

You can override configuration with environment variables:

```bash
# Set base URL
BASE_URL=http://localhost:3001 npm run test:e2e

# Set timeout
TIMEOUT=90000 npm run test:e2e

# Run on CI
CI=true npm run test:e2e
```

## 📊 Test Reports

### View HTML Report

After running tests:

```bash
npm run test:e2e:report
```

This opens an interactive HTML report showing:
- Test results with screenshots
- Failed test traces
- Execution timeline
- Test duration

### CI Reports

In CI, reports are generated in multiple formats:
- **HTML**: `playwright-report/`
- **JSON**: `test-results/results.json`
- **JUnit**: `test-results/junit.xml`

## 🐛 Troubleshooting

### Tests Failing?

#### 1. Check Prerequisites

```bash
# Backend running?
curl http://localhost:8080/api/v1/auth/login

# Frontend running?
curl http://localhost:3001
```

#### 2. Check Browser Installation

```bash
npx playwright install chromium
```

#### 3. Run with Headed Mode

See what's happening:

```bash
npm run test:e2e:headed
```

#### 4. Enable Debug Logs

```bash
DEBUG=pw:api npm run test:e2e
```

### Common Issues

#### "Timeout waiting for navigation"

**Cause**: Backend or frontend not running, or slow response

**Solution**:
- Ensure both servers are running
- Increase timeout in test
- Check network connectivity

#### "Element not found"

**Cause**: Page changed, selector wrong, or element not loaded

**Solution**:
- Use Playwright UI to inspect selectors
- Add proper wait conditions
- Check if element is actually on page

#### "Authentication failed"

**Cause**: Test user doesn't exist or password wrong

**Solution**:
- Verify test users exist in database
- Check credentials in `tests/e2e/fixtures/users.ts`
- Manually test login with credentials

#### "Flaky Tests"

**Cause**: Race conditions, timing issues

**Solution**:
- Use `waitForLoadState('networkidle')`
- Use explicit waits instead of `waitForTimeout`
- Check for proper test isolation

## 📈 Test Coverage

### Current Coverage

| Feature | Tests | Status |
|---------|-------|--------|
| Authentication | 17 tests | ✅ Complete |
| Dashboard | 8 tests | ✅ Complete |
| Extractions | - | 🚧 In Progress |
| Migrations | - | 📋 Planned |
| Data Quality | - | 📋 Planned |
| Compliance | - | 📋 Planned |

### Test Breakdown

- **Authentication**: Login, logout, session management, protected routes
- **Dashboard**: Load, statistics, charts, performance metrics, error handling
- **Extractions**: CRUD operations, filtering, pagination, status management
- **Migrations**: CRUD operations, lifecycle management, progress tracking
- **Data Quality**: Rules, issues, profiling, dashboards
- **Compliance**: Requests, consents, audit logs

## 🔄 CI/CD Integration

Tests automatically run in CI/CD pipeline:

### GitHub Actions

Tests run on:
- Every push to `main` or `develop`
- Every pull request
- With 4 parallel shards for speed

### Pre-commit Hook

Critical tests run before commit:

```bash
# Install hook
npm install husky -D
npx husky install
npx husky add .husky/pre-commit "cd frontend && npm run test:e2e:auth"
```

## 📚 Best Practices

### DO's

✅ Use Page Object Model pattern
✅ Write descriptive test names
✅ Use proper wait conditions
✅ Test real user workflows
✅ Keep tests independent
✅ Clean up test data
✅ Use fixtures for test data

### DON'Ts

❌ Don't use `waitForTimeout` (use proper waits)
❌ Don't hardcode credentials
❌ Don't share state between tests
❌ Don't test implementation details
❌ Don't write tests that depend on execution order
❌ Don't commit auth tokens

## 🆘 Getting Help

1. Check [Playwright Documentation](https://playwright.dev/)
2. Review [E2E_TESTING_STRATEGY.md](/E2E_TESTING_STRATEGY.md)
3. Check existing tests for examples
4. Ask team members

## 📝 Contributing

When adding new tests:

1. Follow the Page Object Model pattern
2. Add test to appropriate spec file
3. Update this README if needed
4. Ensure tests pass locally
5. Create pull request

---

**Happy Testing!** 🎉

For detailed testing strategy, see [E2E_TESTING_STRATEGY.md](/E2E_TESTING_STRATEGY.md)
