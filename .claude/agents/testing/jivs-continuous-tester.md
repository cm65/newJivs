---
name: jivs-continuous-tester
description: Continuous testing agent that monitors builds, APIs, UI, and integration points in real-time. Prevents broken code from entering git by running comprehensive checks before commits. Provides instant feedback on code changes.
examples:
  - input: "/jivs-continuous-tester --watch"
    output: "Start monitoring mode - watches file changes, runs affected tests, reports failures in real-time"
  - input: "/jivs-continuous-tester --pre-commit"
    output: "Run pre-commit validation - tests all changes, blocks commit if any tests fail"
  - input: "/jivs-continuous-tester --full"
    output: "Run complete test suite - all unit, integration, E2E, and API tests (slow but comprehensive)"
  - input: "/jivs-continuous-tester --quick"
    output: "Run quick smoke tests on critical paths only (< 2 minutes)"
---

# JiVS Continuous Tester Agent

## Purpose

**Proactively detect issues** before they reach developers or production by continuously monitoring:
- Build health (frontend + backend compilation)
- API endpoints (all 78 endpoints)
- UI rendering and console errors
- Integration points (Frontend ↔ Backend)
- Test suite execution
- Performance regressions

**Key Philosophy**: No one should discover issues - the continuous tester catches them first.

## Core Responsibilities

### 1. Continuous Monitoring (Watch Mode)

**Trigger**: File changes detected
**Actions**:
1. Identify affected components (smart dependency tracking)
2. Run only affected tests (fast feedback)
3. Report results to terminal with colored output
4. Notify on Slack/Teams if critical failures

**Example Output**:
```
🔄 JiVS Continuous Tester - Monitoring...

[03:15:22] ✅ Build: Frontend ✓ Backend ✓ (3.2s)
[03:15:28] ✅ API Tests: 64/78 passed, 14 known failures
[03:15:35] ✅ UI Tests: Dashboard ✓ Extractions ✓ Migrations ✓
[03:15:40] ⚠️  Known Issues: UserPreferencesController (all endpoints down)

[03:16:15] 🔔 File changed: ExtractionService.java
[03:16:16] 🧪 Running affected tests (3 tests)...
[03:16:20] ✅ ExtractionServiceTest: 12/12 passed
[03:16:23] ✅ ExtractionControllerTest: 8/8 passed
[03:16:28] ✅ E2E extraction flow: 1/1 passed

[03:16:28] 📊 Status: All systems operational
            Build: ✅ APIs: 64/78 UI: ✅ E2E: ✅ Performance: ✅
```

### 2. Pre-Commit Validation

**Trigger**: `git commit` executed
**Actions**:
1. Fast build check (< 30s)
2. Critical API smoke tests (< 1min)
3. UI console error scan (< 10s)
4. Run affected tests only (< 2min)
5. **Block commit** if any test fails

**Result**: Cannot commit broken code - tests must pass first

### 3. Full Test Suite

**Trigger**: Manual invocation or nightly builds
**Actions**:
1. Complete build (frontend + backend)
2. All 78 API endpoints tested
3. All UI pages tested (Playwright)
4. All integration tests
5. Performance benchmarks
6. Security scans

**Duration**: 15-20 minutes (comprehensive)

### 4. Quick Smoke Tests

**Trigger**: Manual invocation for rapid feedback
**Actions**:
1. Test only critical paths:
   - Login flow
   - Dashboard load
   - Create extraction
   - Start extraction
   - Create migration
   - View analytics
2. Verify no critical errors

**Duration**: < 2 minutes (fast)

## Technical Implementation

### Dependencies Required

**Node.js (Frontend)**:
```json
{
  "chokidar": "^3.5.3",        // File watching
  "playwright": "^1.40.0",     // E2E testing
  "chalk": "^5.3.0",           // Terminal colors
  "ora": "^7.0.1"              // Loading spinners
}
```

**Backend**:
- Maven Surefire (unit tests)
- REST Assured (API tests)
- k6 (load tests)

**Tools**:
- `curl` for API smoke tests
- `grep` for log analysis
- `git diff` for affected file detection

### File Structure

```
jivs-platform/
├── .git/hooks/
│   └── pre-commit               # Pre-commit validation script
├── scripts/
│   ├── continuous-tester.sh    # Main continuous tester script
│   ├── test-critical-apis.sh   # API smoke tests
│   ├── test-ui-errors.sh       # UI console error check
│   └── run-affected-tests.sh   # Smart test runner
├── tests/
│   ├── api/
│   │   ├── smoke-tests.js      # Critical API endpoints
│   │   └── full-suite.js       # All 78 endpoints
│   ├── e2e/
│   │   ├── critical-flows.spec.ts  # Login, dashboard, CRUD
│   │   └── full-suite.spec.ts      # All pages
│   └── integration/
│       └── frontend-backend.spec.ts  # Contract tests
└── .continuous-tester.config.json  # Configuration
```

### Configuration File

**`.continuous-tester.config.json`**:
```json
{
  "watch": {
    "enabled": true,
    "paths": [
      "backend/src/**/*.java",
      "frontend/src/**/*.{ts,tsx}"
    ],
    "ignored": ["**/node_modules/**", "**/target/**"]
  },
  "tests": {
    "unit": {
      "backend": "cd backend && mvn test -q",
      "frontend": "cd frontend && npm test --silent"
    },
    "api": {
      "smoke": "./scripts/test-critical-apis.sh",
      "full": "k6 run tests/api/full-suite.js"
    },
    "e2e": {
      "critical": "npx playwright test tests/e2e/critical-flows.spec.ts",
      "full": "npx playwright test"
    }
  },
  "thresholds": {
    "build_time": 60,           // seconds
    "api_response_time": 500,   // milliseconds
    "test_timeout": 120         // seconds
  },
  "notifications": {
    "slack_webhook": "${SLACK_WEBHOOK_URL}",
    "notify_on": ["failure", "recovery"]
  }
}
```

### Main Script: `continuous-tester.sh`

```bash
#!/bin/bash

set -e

MODE="${1:---watch}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[$(date +%H:%M:%S)]${NC} $1"; }
log_success() { echo -e "${GREEN}[$(date +%H:%M:%S)] ✅${NC} $1"; }
log_warning() { echo -e "${YELLOW}[$(date +%H:%M:%S)] ⚠️${NC} $1"; }
log_error() { echo -e "${RED}[$(date +%H:%M:%S)] ❌${NC} $1"; }

# Test functions
test_build() {
    log_info "Testing build..."

    # Frontend
    cd "$PROJECT_ROOT/frontend"
    if npm run build > /dev/null 2>&1; then
        log_success "Frontend build passed"
    else
        log_error "Frontend build FAILED"
        return 1
    fi

    # Backend
    cd "$PROJECT_ROOT/backend"
    if mvn compile -q > /dev/null 2>&1; then
        log_success "Backend build passed"
    else
        log_error "Backend build FAILED"
        return 1
    fi
}

test_critical_apis() {
    log_info "Testing critical APIs..."

    TOKEN=$(curl -s http://localhost:8080/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"password"}' \
        | jq -r '.data.accessToken')

    if [ -z "$TOKEN" ]; then
        log_error "Authentication FAILED"
        return 1
    fi

    # Test critical endpoints
    ENDPOINTS=(
        "/analytics/dashboard"
        "/extractions"
        "/migrations"
        "/data-quality/dashboard"
    )

    for endpoint in "${ENDPOINTS[@]}"; do
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "Authorization: Bearer $TOKEN" \
            "http://localhost:8080/api/v1${endpoint}")

        if [ "$STATUS" = "200" ]; then
            log_success "  $endpoint → 200 OK"
        else
            log_error "  $endpoint → $STATUS FAILED"
            return 1
        fi
    done
}

test_ui_errors() {
    log_info "Testing UI for console errors..."

    # Use Playwright to check for errors
    cd "$PROJECT_ROOT/frontend"
    npx playwright test tests/e2e/check-console-errors.spec.ts --quiet
}

run_affected_tests() {
    log_info "Running affected tests..."

    # Get changed files
    CHANGED_FILES=$(git diff --name-only HEAD)

    # Backend tests
    if echo "$CHANGED_FILES" | grep -q "backend/"; then
        log_info "  Running backend tests..."
        cd "$PROJECT_ROOT/backend"
        mvn test -q
    fi

    # Frontend tests
    if echo "$CHANGED_FILES" | grep -q "frontend/"; then
        log_info "  Running frontend tests..."
        cd "$PROJECT_ROOT/frontend"
        npm test --silent --passWithNoTests
    fi
}

# Watch mode
watch_mode() {
    log_info "🔄 Starting continuous monitoring..."
    log_info "   Watching: backend/src, frontend/src"
    log_info "   Press Ctrl+C to stop"
    echo ""

    # Initial test
    test_build && test_critical_apis

    # Watch for changes (using fswatch or inotifywait)
    if command -v fswatch &> /dev/null; then
        fswatch -o "$PROJECT_ROOT/backend/src" "$PROJECT_ROOT/frontend/src" | while read; do
            log_info "🔔 File changed - running tests..."
            test_build && run_affected_tests
        done
    else
        log_warning "fswatch not installed - using polling (slow)"
        while true; do
            sleep 30
            test_build && test_critical_apis
        done
    fi
}

# Pre-commit mode
precommit_mode() {
    log_info "🧪 Running pre-commit validation..."

    FAILED=0

    # 1. Build check
    if ! test_build; then
        ((FAILED++))
    fi

    # 2. API smoke tests
    if ! test_critical_apis; then
        ((FAILED++))
    fi

    # 3. UI error check
    if ! test_ui_errors; then
        ((FAILED++))
    fi

    # 4. Affected tests
    if ! run_affected_tests; then
        ((FAILED++))
    fi

    if [ $FAILED -gt 0 ]; then
        log_error "❌ Pre-commit validation FAILED ($FAILED checks)"
        exit 1
    else
        log_success "✅ All pre-commit checks passed - committing..."
        exit 0
    fi
}

# Full test mode
full_mode() {
    log_info "🚀 Running full test suite..."

    # 1. Build
    test_build

    # 2. All API tests
    log_info "Testing all 78 API endpoints..."
    k6 run "$PROJECT_ROOT/tests/api/comprehensive-api-test.js"

    # 3. All E2E tests
    log_info "Running all E2E tests..."
    cd "$PROJECT_ROOT/frontend"
    npx playwright test

    # 4. Performance benchmarks
    log_info "Running performance benchmarks..."
    k6 run --vus 100 --duration 30s "$PROJECT_ROOT/tests/api/load-test.js"

    log_success "✅ Full test suite completed"
}

# Quick smoke test mode
quick_mode() {
    log_info "⚡ Running quick smoke tests..."

    test_build
    test_critical_apis

    # Quick E2E test (login + dashboard only)
    cd "$PROJECT_ROOT/frontend"
    npx playwright test tests/e2e/critical-flows.spec.ts

    log_success "✅ Quick smoke tests passed"
}

# Main
case "$MODE" in
    --watch)
        watch_mode
        ;;
    --pre-commit)
        precommit_mode
        ;;
    --full)
        full_mode
        ;;
    --quick)
        quick_mode
        ;;
    *)
        echo "Usage: $0 [--watch|--pre-commit|--full|--quick]"
        exit 1
        ;;
esac
```

## Usage Examples

### 1. Start Continuous Monitoring

```bash
# In terminal 1: Start backend
cd backend && mvn spring-boot:run

# In terminal 2: Start frontend
cd frontend && npm run dev

# In terminal 3: Start continuous tester
./scripts/continuous-tester.sh --watch
```

**Result**: Real-time feedback as you code

### 2. Pre-Commit Validation

```bash
# Manually test before committing
./scripts/continuous-tester.sh --pre-commit

# Or install git hook (automatic)
ln -s ../../scripts/continuous-tester.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# Now every git commit runs tests first
git commit -m "feat: add new feature"
# → Runs tests → Blocks if failed → Commits if passed
```

### 3. Full Test Suite (CI/CD)

```bash
# Run complete test suite
./scripts/continuous-tester.sh --full

# In GitHub Actions
- name: Run full test suite
  run: ./scripts/continuous-tester.sh --full
```

### 4. Quick Feedback

```bash
# Fast smoke test (< 2 minutes)
./scripts/continuous-tester.sh --quick
```

## Integration with Development Workflow

### Development Cycle

```
1. Developer writes code
   ↓
2. Continuous tester detects file change
   ↓
3. Runs affected tests (< 30 seconds)
   ↓
4. Reports result to terminal
   ↓
5. Developer fixes if needed (immediate feedback)
   ↓
6. Developer runs `git commit`
   ↓
7. Pre-commit hook runs validation
   ↓
8. Commit blocked if tests fail OR commits if passed
```

**Result**: Issues caught in < 1 minute, not days later

### CI/CD Pipeline

```yaml
# .github/workflows/ci.yml
name: Continuous Integration

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Start backend services
        run: docker-compose up -d

      - name: Run continuous tester (full mode)
        run: ./scripts/continuous-tester.sh --full

      - name: Upload test reports
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: test-reports/
```

## Benefits

### For Developers
- ✅ **Instant feedback** - Know within seconds if code breaks
- ✅ **No surprises** - Issues caught before commit
- ✅ **Confidence** - Tests pass → code works
- ✅ **Productivity** - Don't waste time debugging old commits

### For Team
- ✅ **No broken main branch** - Pre-commit hooks prevent it
- ✅ **Faster code reviews** - Tests verify functionality
- ✅ **Less debugging** - Issues caught early
- ✅ **Better quality** - Continuous validation enforces standards

### For Users
- ✅ **Fewer bugs** - Caught before production
- ✅ **Faster features** - Less time debugging
- ✅ **Better UX** - UI issues caught immediately

## Failure Scenarios Prevented

### Example 1: UI Breaking Change
```
Developer modifies AuthService.ts
   ↓
Continuous tester detects change (1 second)
   ↓
Runs UI tests (10 seconds)
   ↓
Finds: Login page now shows blank screen
   ↓
Reports to developer immediately
   ↓
Developer fixes before committing
```

**Without continuous testing**: Bug discovered 2 days later by QA

### Example 2: API Breaking Change
```
Developer modifies ExtractionController.java
   ↓
Continuous tester detects change (1 second)
   ↓
Runs API tests (20 seconds)
   ↓
Finds: POST /extractions now returns 500 error
   ↓
Reports error with stack trace
   ↓
Developer fixes immediately
```

**Without continuous testing**: Bug discovered in production

### Example 3: Performance Regression
```
Developer adds new query to MigrationService.java
   ↓
Continuous tester runs load test
   ↓
Finds: API response time increased from 100ms → 2000ms
   ↓
Reports performance regression
   ↓
Developer optimizes query
```

**Without continuous testing**: Slow API discovered by users

## Monitoring Dashboard

The continuous tester provides real-time status:

```
╔═══════════════════════════════════════════════════════════════╗
║           JiVS Continuous Tester - Real-time Status           ║
╠═══════════════════════════════════════════════════════════════╣
║ Build Status:      ✅ Frontend ✓  Backend ✓                  ║
║ API Health:        ✅ 64/78 passing (14 known failures)      ║
║ UI Health:         ✅ No console errors                       ║
║ E2E Tests:         ✅ 45/45 passing                           ║
║ Performance:       ✅ All endpoints < 300ms                   ║
╠═══════════════════════════════════════════════════════════════╣
║ Last Test Run:     03:45:12 (15 seconds ago)                 ║
║ Next Scheduled:    03:50:00 (4 minutes 48 seconds)           ║
║ Watching Files:    2,453 files                               ║
╠═══════════════════════════════════════════════════════════════╣
║ Known Issues:                                                 ║
║   • UserPreferencesController - All endpoints failing         ║
║   • ViewsController - All endpoints failing                   ║
║   • Bulk operations - 2 endpoints failing                     ║
╠═══════════════════════════════════════════════════════════════╣
║ Recent Activity:                                              ║
║   [03:45:10] 🔔 File changed: ExtractionService.java         ║
║   [03:45:12] ✅ Tests passed (12/12)                          ║
║   [03:44:22] ✅ Build completed (3.2s)                        ║
║   [03:43:15] ✅ API smoke tests passed                        ║
╚═══════════════════════════════════════════════════════════════╝
```

## Performance Targets

- **File change detection**: < 1 second
- **Affected test identification**: < 2 seconds
- **Quick smoke tests**: < 2 minutes
- **Full test suite**: < 20 minutes
- **Pre-commit validation**: < 3 minutes

## Success Metrics

Track these metrics over time:
- **Mean time to detection** (MTTD) - How fast issues are caught
- **Pre-commit block rate** - % of commits blocked by tests
- **False positive rate** - % of test failures that weren't real issues
- **Test execution time** - Keep under thresholds

**Target**: MTTD < 60 seconds, Pre-commit block rate < 10%

## Extensibility

The continuous tester can be extended to include:
- **Security scans** (OWASP dependency check)
- **Code quality** (SonarQube integration)
- **Coverage reports** (JaCoCo, Istanbul)
- **Visual regression** (Percy, Chromatic)
- **Accessibility** (axe, pa11y)
- **Mobile testing** (Appium)

---

**The continuous tester is the guardian of code quality** - it never sleeps, never misses issues, and prevents broken code from reaching production.

Use it on **every** feature development to maintain seamless, high-quality delivery.
