#!/bin/bash

################################################################################
# JiVS Continuous Tester - Real-time testing and validation
#
# Usage:
#   ./continuous-tester.sh --watch      Start continuous monitoring
#   ./continuous-tester.sh --pre-commit Run pre-commit validation
#   ./continuous-tester.sh --full       Run complete test suite
#   ./continuous-tester.sh --quick      Run quick smoke tests
################################################################################

set -e

MODE="${1:---watch}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_REPORT_LOG="${TEST_REPORT_LOG:-/tmp/continuous-test-report.log}"
TEST_ALERT_LOG="${TEST_ALERT_LOG:-/tmp/test-alerts.log}"

# Colors for terminal output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Initialize logs on first run
init_logs() {
    if [ ! -f "$TEST_REPORT_LOG" ] || [ "$MODE" = "--watch" ]; then
        echo "# JiVS Continuous Test Report - Started $(date '+%Y-%m-%d %H:%M:%S')" > "$TEST_REPORT_LOG"
        echo "# Backend: http://localhost:8080" >> "$TEST_REPORT_LOG"
        echo "# Frontend: http://localhost:3001" >> "$TEST_REPORT_LOG"
        echo "" >> "$TEST_REPORT_LOG"
    fi
    if [ ! -f "$TEST_ALERT_LOG" ]; then
        echo "# JiVS Test Alerts - Started $(date '+%Y-%m-%d %H:%M:%S')" > "$TEST_ALERT_LOG"
        echo "" >> "$TEST_ALERT_LOG"
    fi
}

# Logging functions with file output
log_info() {
    local msg="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${BLUE}[$(date +%H:%M:%S)]${NC} $msg"
    echo "[$timestamp] INFO: $msg" >> "$TEST_REPORT_LOG"
}

log_success() {
    local msg="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${GREEN}[$(date +%H:%M:%S)] ✅${NC} $msg"
    echo "[$timestamp] SUCCESS: $msg" >> "$TEST_REPORT_LOG"
}

log_warning() {
    local msg="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${YELLOW}[$(date +%H:%M:%S)] ⚠️${NC} $msg"
    echo "[$timestamp] WARNING: $msg" >> "$TEST_REPORT_LOG"
}

log_error() {
    local msg="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${RED}[$(date +%H:%M:%S)] ❌${NC} $msg"
    echo "[$timestamp] ERROR: $msg" >> "$TEST_REPORT_LOG"
    echo "[$timestamp] ERROR: $msg" >> "$TEST_ALERT_LOG"
}

log_test() {
    local msg="$1"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${CYAN}[$(date +%H:%M:%S)] 🧪${NC} $msg"
    echo "[$timestamp] TEST: $msg" >> "$TEST_REPORT_LOG"
}

log_metric() {
    local test_name="$1"
    local status="$2"
    local response_time="${3:-N/A}"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] METRIC: $test_name | Status: $status | ResponseTime: ${response_time}ms" >> "$TEST_REPORT_LOG"
}

# Check if services are running
check_services() {
    local all_up=true

    # Check frontend with timing
    local start_time=$(gdate +%s%3N 2>/dev/null || date +%s000)
    local frontend_code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3001 --max-time 5 2>/dev/null || echo "000")
    local end_time=$(gdate +%s%3N 2>/dev/null || date +%s000)
    local frontend_time=$((end_time - start_time))

    if [ "$frontend_code" = "200" ]; then
        log_success "Frontend running (port 3001, ${frontend_time}ms)"
        log_metric "frontend_health" "UP" "$frontend_time"
    else
        log_warning "Frontend not running (port 3001)"
        log_metric "frontend_health" "DOWN" "0"
        all_up=false
    fi

    # Check backend with timing
    start_time=$(gdate +%s%3N 2>/dev/null || date +%s000)
    local backend_code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health --max-time 5 2>/dev/null || echo "000")
    end_time=$(gdate +%s%3N 2>/dev/null || date +%s000)
    local backend_time=$((end_time - start_time))

    if [ "$backend_code" = "200" ]; then
        log_success "Backend running (port 8080, ${backend_time}ms)"
        log_metric "backend_health" "UP" "$backend_time"
    else
        log_warning "Backend not running (port 8080)"
        log_metric "backend_health" "DOWN" "0"
        all_up=false
    fi

    if [ "$all_up" = false ]; then
        log_warning "Some services are not running. Start them first:"
        echo "  Terminal 1: cd backend && mvn spring-boot:run"
        echo "  Terminal 2: cd frontend && npm run dev"
    fi

    return 0
}

# Test build
test_build() {
    log_test "Testing build..."
    local start_time=$(date +%s)
    local failed=false

    # Frontend build
    cd "$PROJECT_ROOT/frontend"
    if npm run build > /tmp/frontend-build.log 2>&1; then
        log_success "  Frontend build passed"
    else
        log_error "  Frontend build FAILED"
        echo "    See: /tmp/frontend-build.log"
        failed=true
    fi

    # Backend compile
    cd "$PROJECT_ROOT/backend"
    if mvn compile -q > /tmp/backend-build.log 2>&1; then
        log_success "  Backend build passed"
    else
        log_error "  Backend build FAILED"
        echo "    See: /tmp/backend-build.log"
        failed=true
    fi

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    if [ "$failed" = true ]; then
        return 1
    else
        log_success "Build completed in ${duration}s"
        return 0
    fi
}

# Test critical API endpoints
test_critical_apis() {
    log_test "Testing critical APIs..."

    # Get auth token with timing
    local start_time=$(gdate +%s%3N 2>/dev/null || date +%s000)
    local token_response=$(curl -s http://localhost:8080/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"password"}' \
        --max-time 10)
    local end_time=$(gdate +%s%3N 2>/dev/null || date +%s000)
    local auth_time=$((end_time - start_time))

    local token=$(echo "$token_response" | jq -r '.data.accessToken // empty' 2>/dev/null)

    if [ -z "$token" ]; then
        log_error "  Authentication FAILED (${auth_time}ms)"
        log_metric "api_authentication" "FAILED" "$auth_time"
        return 1
    fi

    log_success "  Authentication passed (${auth_time}ms)"
    log_metric "api_authentication" "SUCCESS" "$auth_time"

    # Test critical endpoints
    local endpoints=(
        "/analytics/dashboard:Dashboard analytics"
        "/extractions:Extractions list"
        "/migrations:Migrations list"
        "/data-quality/dashboard:Data quality dashboard"
    )

    local failed=false

    for endpoint_info in "${endpoints[@]}"; do
        IFS=':' read -r endpoint description <<< "$endpoint_info"

        start_time=$(gdate +%s%3N 2>/dev/null || date +%s000)
        local status=$(curl -s -o /dev/null -w "%{http_code}" \
            -H "Authorization: Bearer $token" \
            "http://localhost:8080/api/v1${endpoint}" \
            --max-time 10)
        end_time=$(gdate +%s%3N 2>/dev/null || date +%s000)
        local endpoint_time=$((end_time - start_time))

        local endpoint_name=$(echo "$endpoint" | sed 's/[^a-zA-Z0-9]/_/g')

        if [ "$status" = "200" ]; then
            log_success "  $description → 200 OK (${endpoint_time}ms)"
            log_metric "api${endpoint_name}" "SUCCESS" "$endpoint_time"
        else
            log_error "  $description → $status FAILED (${endpoint_time}ms)"
            log_metric "api${endpoint_name}" "FAILED" "$endpoint_time"
            failed=true
        fi
    done

    if [ "$failed" = true ]; then
        return 1
    else
        return 0
    fi
}

# Check UI for console errors (simple version - checks if page loads)
test_ui_health() {
    log_test "Testing UI health..."

    # Check frontend loads without errors
    local status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3001)

    if [ "$status" = "200" ]; then
        log_success "  Frontend loads successfully"
        return 0
    else
        log_error "  Frontend failed to load (HTTP $status)"
        return 1
    fi
}

# Run affected tests based on changed files
run_affected_tests() {
    log_test "Running affected tests..."

    # Get changed files (uncommitted changes)
    local changed_files=$(git diff --name-only HEAD 2>/dev/null || echo "")

    if [ -z "$changed_files" ]; then
        log_info "  No uncommitted changes detected"
        return 0
    fi

    local ran_tests=false

    # Backend tests
    if echo "$changed_files" | grep -q "backend/"; then
        log_info "  Running backend tests..."
        cd "$PROJECT_ROOT/backend"

        if mvn test -q > /tmp/backend-tests.log 2>&1; then
            log_success "  Backend tests passed"
        else
            log_error "  Backend tests FAILED"
            echo "    See: /tmp/backend-tests.log"
            return 1
        fi
        ran_tests=true
    fi

    # Frontend tests
    if echo "$changed_files" | grep -q "frontend/"; then
        log_info "  Running frontend tests..."
        cd "$PROJECT_ROOT/frontend"

        if npm test --silent --passWithNoTests > /tmp/frontend-tests.log 2>&1; then
            log_success "  Frontend tests passed"
        else
            log_error "  Frontend tests FAILED"
            echo "    See: /tmp/frontend-tests.log"
            return 1
        fi
        ran_tests=true
    fi

    if [ "$ran_tests" = false ]; then
        log_info "  No tests needed for changed files"
    fi

    return 0
}

# Display status dashboard
show_status() {
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║           JiVS Continuous Tester - Real-time Status           ║"
    echo "╠═══════════════════════════════════════════════════════════════╣"

    # Services status
    local frontend_status="❌ Down"
    local backend_status="❌ Down"

    if curl -s -o /dev/null -w "%{http_code}" http://localhost:3001 | grep -q "200"; then
        frontend_status="✅ Running"
    fi

    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200"; then
        backend_status="✅ Running"
    fi

    printf "║ Frontend:          %-44s ║\n" "$frontend_status"
    printf "║ Backend:           %-44s ║\n" "$backend_status"
    echo "╠═══════════════════════════════════════════════════════════════╣"
    printf "║ Last Check:        %-44s ║\n" "$(date '+%Y-%m-%d %H:%M:%S')"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo ""
}

# Watch mode - continuous monitoring
watch_mode() {
    log_info "🔄 Starting continuous monitoring..."
    log_info "   Project: $PROJECT_ROOT"
    log_info "   Watching: backend/src, frontend/src"
    log_info "   Press Ctrl+C to stop"
    echo ""

    # Check services are running
    check_services

    # Initial tests
    show_status
    test_build
    test_critical_apis
    test_ui_health

    log_info ""
    log_info "👀 Monitoring file changes..."

    # Check if fswatch is available
    if command -v fswatch &> /dev/null; then
        # Use fswatch for real-time monitoring (Mac/Linux)
        fswatch -o "$PROJECT_ROOT/backend/src" "$PROJECT_ROOT/frontend/src" 2>/dev/null | while read -r num_changes; do
            log_info "🔔 Files changed detected ($num_changes changes)"
            echo ""
            test_build && test_critical_apis && test_ui_health
            echo ""
            log_info "👀 Watching for changes..."
        done
    else
        log_warning "fswatch not installed - using polling mode (slower)"
        log_info "   Install: brew install fswatch (Mac) or apt-get install inotify-tools (Linux)"
        echo ""

        # Fallback: polling mode
        while true; do
            sleep 30
            show_status
            test_build && test_critical_apis && test_ui_health
        done
    fi
}

# Pre-commit mode - validate before allowing commit
precommit_mode() {
    log_info "🧪 Running pre-commit validation..."
    echo ""

    local failed_checks=0
    local total_checks=0

    # Check 1: Build
    ((total_checks++))
    log_test "Check 1/$total_checks: Build compilation"
    if ! test_build; then
        ((failed_checks++))
    fi
    echo ""

    # Check 2: Critical APIs
    ((total_checks++))
    log_test "Check 2/$total_checks: Critical API endpoints"
    if ! test_critical_apis; then
        ((failed_checks++))
    fi
    echo ""

    # Check 3: UI Health
    ((total_checks++))
    log_test "Check 3/$total_checks: UI health"
    if ! test_ui_health; then
        ((failed_checks++))
    fi
    echo ""

    # Check 4: Affected tests
    ((total_checks++))
    log_test "Check 4/$total_checks: Affected tests"
    if ! run_affected_tests; then
        ((failed_checks++))
    fi
    echo ""

    # Results
    if [ $failed_checks -gt 0 ]; then
        log_error "❌ PRE-COMMIT VALIDATION FAILED"
        log_error "   $failed_checks of $total_checks checks failed"
        log_error "   COMMIT BLOCKED - Fix issues before committing"
        echo ""
        exit 1
    else
        log_success "✅ ALL PRE-COMMIT CHECKS PASSED"
        log_success "   All $total_checks checks passed"
        log_success "   Commit allowed to proceed"
        echo ""
        exit 0
    fi
}

# Full test mode - comprehensive testing
full_mode() {
    log_info "🚀 Running full test suite (this may take 15-20 minutes)..."
    echo ""

    local start_time=$(date +%s)
    local failed=false

    # 1. Build
    log_test "Phase 1/5: Build"
    if ! test_build; then
        failed=true
    fi
    echo ""

    # 2. Unit tests
    log_test "Phase 2/5: Unit tests"

    cd "$PROJECT_ROOT/backend"
    if mvn test > /tmp/backend-unit-tests.log 2>&1; then
        log_success "  Backend unit tests passed"
    else
        log_error "  Backend unit tests FAILED"
        failed=true
    fi

    cd "$PROJECT_ROOT/frontend"
    if npm test --passWithNoTests > /tmp/frontend-unit-tests.log 2>&1; then
        log_success "  Frontend unit tests passed"
    else
        log_error "  Frontend unit tests FAILED"
        failed=true
    fi
    echo ""

    # 3. API tests
    log_test "Phase 3/5: API tests (all 78 endpoints)"
    if [ -f "$PROJECT_ROOT/backend/tests/api/test-all-endpoints.sh" ]; then
        if bash "$PROJECT_ROOT/backend/tests/api/test-all-endpoints.sh" > /tmp/api-tests.log 2>&1; then
            log_success "  API tests passed"
        else
            log_warning "  Some API tests failed (see backend/API_TEST_REPORT.md)"
        fi
    else
        log_warning "  API test script not found - skipping"
    fi
    echo ""

    # 4. UI tests
    log_test "Phase 4/5: UI health check"
    if ! test_ui_health; then
        failed=true
    fi
    echo ""

    # 5. Critical endpoints
    log_test "Phase 5/5: Critical API endpoints"
    if ! test_critical_apis; then
        failed=true
    fi
    echo ""

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    local minutes=$((duration / 60))
    local seconds=$((duration % 60))

    if [ "$failed" = true ]; then
        log_error "❌ FULL TEST SUITE FAILED"
        log_error "   Duration: ${minutes}m ${seconds}s"
        exit 1
    else
        log_success "✅ FULL TEST SUITE PASSED"
        log_success "   Duration: ${minutes}m ${seconds}s"
        exit 0
    fi
}

# Quick smoke test mode - fast validation
quick_mode() {
    log_info "⚡ Running quick smoke tests (< 2 minutes)..."
    echo ""

    local start_time=$(date +%s)

    # Check services
    check_services
    echo ""

    # Quick build check (just compile, no tests)
    log_test "Quick build check..."
    cd "$PROJECT_ROOT/backend"
    if mvn compile -q > /dev/null 2>&1; then
        log_success "  Backend compiles"
    else
        log_error "  Backend compilation failed"
        exit 1
    fi

    # Critical APIs
    if ! test_critical_apis; then
        exit 1
    fi
    echo ""

    # UI health
    if ! test_ui_health; then
        exit 1
    fi
    echo ""

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    log_success "✅ QUICK SMOKE TESTS PASSED (${duration}s)"
    exit 0
}

# Initialize logs
init_logs

# Main execution
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
    --status)
        # Show current status
        check_services
        echo ""
        show_status
        echo ""
        log_info "Report log: $TEST_REPORT_LOG"
        log_info "Alert log: $TEST_ALERT_LOG"
        exit 0
        ;;
    --help|-h)
        echo "JiVS Continuous Tester"
        echo ""
        echo "Usage:"
        echo "  $0 --watch       Start continuous monitoring (watches file changes)"
        echo "  $0 --pre-commit  Run pre-commit validation (blocks bad commits)"
        echo "  $0 --full        Run complete test suite (15-20 min)"
        echo "  $0 --quick       Run quick smoke tests (< 2 min)"
        echo "  $0 --status      Show current health status"
        echo "  $0 --help        Show this help message"
        echo ""
        echo "Logs:"
        echo "  Test Report: $TEST_REPORT_LOG"
        echo "  Test Alerts: $TEST_ALERT_LOG"
        echo ""
        exit 0
        ;;
    *)
        log_error "Unknown mode: $MODE"
        echo "Use --help for usage information"
        exit 1
        ;;
esac
