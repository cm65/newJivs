# 🚀 JiVS Testing - Quick Reference Card

> **One-page cheat sheet for daily testing operations**

---

## ⚡ Essential Commands (Copy & Paste)

### Development (Daily Use)

```bash
# Start watch mode (auto-test on file changes)
./scripts/test-orchestrator.sh watch

# Quick test before commit (30 seconds)
./scripts/test-orchestrator.sh quick

# View real-time test dashboard
./scripts/test-monitor.sh

# Debug test failures
./scripts/test-debug-helper.sh --fix
```

### Testing Modes

```bash
# Contract tests only (5 seconds) - API shape validation
./scripts/test-orchestrator.sh contract-only

# Unit tests only (30 seconds) - Logic validation
./scripts/test-orchestrator.sh unit-only

# Standard tests (3 minutes) - Before PR
./scripts/test-orchestrator.sh standard

# Full test suite (10 minutes) - Before deployment
./scripts/test-orchestrator.sh full
```

### Performance Testing

```bash
# Quick smoke test (5 minutes, 10 VUs)
./scripts/run-performance-tests.sh quick

# Load test (15 minutes, 100 VUs)
./scripts/run-performance-tests.sh load --report

# Stress test (30 minutes, 500 VUs)
./scripts/run-performance-tests.sh stress

# Custom target URL
./scripts/run-performance-tests.sh load --url https://staging.jivs.com
```

### Specific Test Types

```bash
# Run contract tests only
cd frontend && npm run test:contracts

# Run unit tests with coverage
cd backend && mvn test jacoco:report
cd frontend && npm run test:coverage

# Run integration tests
cd backend && mvn verify -Pintegration-tests

# Run E2E tests
cd frontend && npx playwright test

# Run E2E in headed mode (see browser)
cd frontend && npx playwright test --headed

# Run specific E2E test
cd frontend && npx playwright test specs/auth/login.spec.ts
```

---

## 🔧 Troubleshooting

### Common Issues

```bash
# Backend not responding
cd backend && mvn spring-boot:run

# Frontend not responding
cd frontend && npm run dev

# Services not running
docker-compose -f docker-compose.test.yml up -d

# Dependencies missing
cd backend && mvn install -DskipTests
cd frontend && npm install

# Pre-commit hook not working
git config core.hooksPath .githooks
chmod +x .githooks/pre-commit

# Tests failing unexpectedly
./scripts/test-debug-helper.sh --fix --verbose
```

### Service Status Checks

```bash
# Check all services
docker-compose ps

# Check backend health
curl http://localhost:8080/actuator/health

# Check frontend
curl http://localhost:3001

# Check PostgreSQL
docker-compose exec postgres psql -U jivs_user -d jivs -c "SELECT 1;"

# Check Redis
docker-compose exec redis redis-cli ping

# View service logs
docker-compose logs -f [service-name]
```

---

## 📊 Test Coverage & Reports

### View Coverage Reports

```bash
# Backend coverage (JaCoCo)
cd backend && mvn jacoco:report
open backend/target/site/jacoco/index.html

# Frontend coverage (Istanbul)
cd frontend && npm run test:coverage
open frontend/coverage/index.html

# E2E test report (Playwright)
cd frontend && npx playwright show-report
```

### Generate Reports

```bash
# Performance test report
./scripts/run-performance-tests.sh load --report
# Opens HTML report automatically on macOS

# Security scan report
./scripts/security-scan.sh --report-dir ./reports

# Infrastructure verification
./scripts/verify-testing-infrastructure.sh > test-reports/infrastructure-status.txt
```

---

## 🎯 Pre-Commit Workflow

### Recommended Flow

```bash
# Terminal 1: Development
code .

# Terminal 2: Watch mode (auto-runs tests)
./scripts/test-orchestrator.sh watch

# Terminal 3: Monitor dashboard
./scripts/test-monitor.sh

# When ready to commit:
git add .
git commit -m "feat: your feature description"
# Pre-commit hook runs automatically (30 seconds)
# Commit blocked if tests fail
```

---

## 🔍 Debugging Failed Tests

### Step-by-Step Debugging

```bash
# 1. Run debug helper
./scripts/test-debug-helper.sh

# 2. Check specific test type
./scripts/test-debug-helper.sh contract
./scripts/test-debug-helper.sh unit
./scripts/test-debug-helper.sh integration
./scripts/test-debug-helper.sh e2e

# 3. Try auto-fix
./scripts/test-debug-helper.sh --fix

# 4. View detailed logs
tail -f test-reports/test-orchestrator.log
tail -f frontend/playwright-report/index.html
```

### Check Test Results

```bash
# View last test run
cat test-reports/test-orchestrator.log | tail -100

# Check contract test results
cat frontend/pacts/*.json | jq '.'

# Check unit test results
cat backend/target/surefire-reports/*.xml

# Check E2E test results
ls frontend/test-results/
ls frontend/playwright-report/
```

---

## 📦 Setup & Installation

### One-Command Setup

```bash
# Complete setup from scratch
./scripts/setup-continuous-testing.sh

# Verify setup
./scripts/verify-testing-infrastructure.sh
```

### Manual Setup Steps

```bash
# 1. Install dependencies
cd backend && mvn install -DskipTests
cd ../frontend && npm install

# 2. Install Playwright browsers
cd frontend && npx playwright install chromium

# 3. Start services
docker-compose -f docker-compose.test.yml up -d

# 4. Configure Git hooks
git config core.hooksPath .githooks

# 5. Run first test
./scripts/test-orchestrator.sh quick
```

---

## 🚨 Emergency Commands

### Stop Everything

```bash
# Stop all Docker services
docker-compose down

# Kill all Java processes
pkill -f "spring-boot"

# Kill all Node processes
pkill -f "node"

# Kill watch mode
pkill -f "fswatch"
```

### Reset Testing Environment

```bash
# Nuclear option - reset everything
docker-compose down -v
rm -rf frontend/node_modules backend/target
./scripts/setup-continuous-testing.sh
```

### Quick Health Check

```bash
# Run smoke test
./scripts/smoke-test.sh

# Or quick verification
./scripts/verify-testing-infrastructure.sh
```

---

## 🎓 Test Writing Guidelines

### Contract Test (Write First!)

```typescript
// frontend/src/services/__tests__/myService.pact.test.ts
import { pactWith } from 'jest-pact';

pactWith({ consumer: 'Frontend', provider: 'Backend' }, provider => {
  describe('POST /api/v1/my-endpoint', () => {
    beforeEach(() => {
      return provider.addInteraction({
        state: 'system is ready',
        uponReceiving: 'a request to create something',
        withRequest: {
          method: 'POST',
          path: '/api/v1/my-endpoint',
          headers: { 'Content-Type': 'application/json' },
          body: { name: 'Test' }
        },
        willRespondWith: {
          status: 201,
          body: { id: like('123'), name: 'Test' }
        }
      });
    });

    it('creates successfully', async () => {
      const result = await myService.create({ name: 'Test' });
      expect(result.name).toBe('Test');
    });
  });
});
```

### Unit Test

```typescript
// frontend/src/services/__tests__/myService.test.ts
describe('MyService', () => {
  it('should transform data correctly', () => {
    const result = myService.transform({ input: 'value' });
    expect(result).toEqual({ output: 'VALUE' });
  });
});
```

### E2E Test

```typescript
// frontend/tests/e2e/my-feature.spec.ts
import { test, expect } from '@playwright/test';

test('user can perform action', async ({ page }) => {
  await page.goto('http://localhost:3001');
  await page.click('text=My Feature');
  await expect(page.locator('h1')).toContainText('Success');
});
```

---

## 📈 Key Metrics

### Test Execution Times

| Test Type | Duration | When to Run |
|-----------|----------|-------------|
| Contract | 5 seconds | Every commit |
| Unit | 30 seconds | Every commit |
| Integration | 2 minutes | Every PR |
| E2E | 5 minutes | Every PR |
| Performance | 5-60 minutes | Before deployment |
| Security | 3 minutes | Every PR |

### Coverage Targets

| Component | Target | Current |
|-----------|--------|---------|
| Backend | 80% | 85% ✅ |
| Frontend | 80% | 82% ✅ |
| API Endpoints | 100% | 100% ✅ |

### Quality Gates

- ✅ All tests passing (100%)
- ✅ Coverage > 80%
- ✅ No CRITICAL security issues
- ✅ API latency p95 < 200ms
- ✅ Error rate < 1%

---

## 🔗 Useful Links

- **Documentation**: `docs/COMPREHENSIVE_TESTING_STRATEGY.md`
- **Quick Start**: `DEVELOPER_QUICK_START.md`
- **Troubleshooting**: Run `./scripts/test-debug-helper.sh`
- **Infrastructure Status**: Run `./scripts/verify-testing-infrastructure.sh`

---

## 💡 Pro Tips

1. **Always keep watch mode running** during development
2. **Run quick tests** every 10-15 minutes
3. **Use debug helper** instead of guessing
4. **Check monitor dashboard** for real-time feedback
5. **Trust the system** - if tests pass, your code is solid
6. **Never use `--no-verify`** to skip pre-commit hooks
7. **Fix broken tests immediately** - don't let them accumulate

---

## 🎯 The 5-Second Rule

**The most important concept**: Contract tests prevent the `sourceConfig` vs `sourceSystem` bug that used to take 2+ hours to find. Now it's caught in **5 seconds** at commit time.

```javascript
// This bug is now IMPOSSIBLE:
// Frontend: { sourceConfig: {...} }  ❌
// Backend:  { sourceSystem: "..." }   ❌

// Contract tests enforce:
// Both:     { sourceSystem: "..." }   ✅
```

---

## 📞 Getting Help

```bash
# Self-service debugging
./scripts/test-debug-helper.sh --verbose --fix

# Check infrastructure health
./scripts/verify-testing-infrastructure.sh

# View recent test failures
grep -r "FAIL" test-reports/ | tail -20

# View detailed logs
tail -f test-reports/test-orchestrator.log
```

---

**Last Updated**: January 2025
**Version**: 1.0.0
**Status**: Production-Ready ✅

*Print this page and keep it by your desk for quick reference!*
