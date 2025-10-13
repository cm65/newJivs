#!/bin/bash

################################################################################
# JiVS Platform - Test Failure Debug Helper
#
# This intelligent script helps developers debug test failures by:
# 1. Analyzing test logs for common issues
# 2. Checking system dependencies
# 3. Verifying environment configuration
# 4. Suggesting specific fixes
# 5. Providing relevant documentation links
#
# Usage:
#   ./test-debug-helper.sh [test-type] [options]
#
# Test Types:
#   contract    - Debug contract test failures
#   unit        - Debug unit test failures
#   integration - Debug integration test failures
#   e2e         - Debug E2E test failures
#   all         - Analyze all test types
#
# Options:
#   --log <file>    - Analyze specific log file
#   --verbose       - Show detailed debugging info
#   --fix           - Attempt automatic fixes
#
# Examples:
#   ./test-debug-helper.sh contract
#   ./test-debug-helper.sh unit --log test.log --verbose
#   ./test-debug-helper.sh all --fix
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
PURPLE='\033[0;35m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

# Configuration
TEST_TYPE="${1:-all}"
LOG_FILE=""
VERBOSE=false
AUTO_FIX=false
ISSUES_FOUND=0
FIXES_APPLIED=0

# Parse arguments
shift || true
while [[ $# -gt 0 ]]; do
    case $1 in
        --log)
            LOG_FILE=$2
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --fix)
            AUTO_FIX=true
            shift
            ;;
        --help)
            echo "Usage: $0 [test-type] [options]"
            echo "Test Types: contract, unit, integration, e2e, all"
            echo "Options:"
            echo "  --log <file>  - Analyze specific log file"
            echo "  --verbose     - Show detailed debugging info"
            echo "  --fix         - Attempt automatic fixes"
            exit 0
            ;;
        *)
            shift
            ;;
    esac
done

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo -e "${BOLD}${PURPLE}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${PURPLE}║                    JiVS TEST FAILURE DEBUG HELPER                             ║${NC}"
echo -e "${BOLD}${PURPLE}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}Test Type: ${BOLD}$TEST_TYPE${NC}"
echo -e "${CYAN}Verbose: ${BOLD}$([ "$VERBOSE" = true ] && echo "Yes" || echo "No")${NC}"
echo -e "${CYAN}Auto Fix: ${BOLD}$([ "$AUTO_FIX" = true ] && echo "Enabled" || echo "Disabled")${NC}"
echo ""

# =============================================================================
# Common Issue Patterns
# =============================================================================
declare -A ERROR_PATTERNS
ERROR_PATTERNS["Cannot find module"]="Missing dependency"
ERROR_PATTERNS["Connection refused"]="Service not running"
ERROR_PATTERNS["ECONNREFUSED"]="Backend not accessible"
ERROR_PATTERNS["Cannot read property .* of undefined"]="Null reference error"
ERROR_PATTERNS["Failed to compile"]="Compilation error"
ERROR_PATTERNS["Module not found"]="Missing import"
ERROR_PATTERNS["sourceConfig"]="Contract mismatch - sourceConfig vs sourceSystem"
ERROR_PATTERNS["targetConfig"]="Contract mismatch - targetConfig vs targetSystem"
ERROR_PATTERNS["401 Unauthorized"]="Authentication failure"
ERROR_PATTERNS["500 Internal Server Error"]="Backend error"
ERROR_PATTERNS["Timeout"]="Performance issue or deadlock"
ERROR_PATTERNS["out of memory"]="Memory allocation failure"

# =============================================================================
# Helper Functions
# =============================================================================

print_issue() {
    local severity=$1
    local issue=$2
    local fix=$3

    case $severity in
        critical)
            echo -e "${RED}❌ CRITICAL: $issue${NC}"
            ;;
        high)
            echo -e "${YELLOW}⚠️  HIGH: $issue${NC}"
            ;;
        medium)
            echo -e "${BLUE}ℹ️  MEDIUM: $issue${NC}"
            ;;
        low)
            echo -e "${DIM}📝 LOW: $issue${NC}"
            ;;
    esac

    if [ -n "$fix" ]; then
        echo -e "   ${GREEN}➜ Fix: $fix${NC}"
    fi

    ISSUES_FOUND=$((ISSUES_FOUND + 1))
}

check_service() {
    local service=$1
    local port=$2
    local name=$3

    if ! lsof -i:$port > /dev/null 2>&1; then
        print_issue "critical" "$name not running on port $port" \
            "Start $name: cd $PROJECT_ROOT && $4"
        return 1
    fi
    return 0
}

analyze_log() {
    local log=$1
    local context=$2

    if [ ! -f "$log" ]; then
        return
    fi

    echo -e "${CYAN}Analyzing $context log...${NC}"

    for pattern in "${!ERROR_PATTERNS[@]}"; do
        if grep -qE "$pattern" "$log" 2>/dev/null; then
            local issue="${ERROR_PATTERNS[$pattern]}"
            local count=$(grep -cE "$pattern" "$log")
            print_issue "high" "$issue (found $count times in $context)" ""

            # Show sample if verbose
            if [ "$VERBOSE" = true ]; then
                echo -e "${DIM}Sample:${NC}"
                grep -E "$pattern" "$log" | head -2 | sed 's/^/  /'
            fi
        fi
    done
}

# =============================================================================
# SECTION 1: System Dependencies Check
# =============================================================================
echo -e "${BOLD}${CYAN}1. System Dependencies Check${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    print_issue "critical" "Java not installed" "Install Java 21: brew install openjdk@21"
elif ! java -version 2>&1 | grep -q "version \"21"; then
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    print_issue "medium" "Wrong Java version: $JAVA_VERSION" "Install Java 21"
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    print_issue "critical" "Maven not installed" "Install Maven: brew install maven"
fi

# Check Node.js
if ! command -v node &> /dev/null; then
    print_issue "critical" "Node.js not installed" "Install Node.js: brew install node"
elif [ $(node -v | cut -d'.' -f1 | tr -d 'v') -lt 18 ]; then
    print_issue "medium" "Node.js version too old: $(node -v)" "Update Node.js to v18+"
fi

# Check npm
if ! command -v npm &> /dev/null; then
    print_issue "critical" "npm not installed" "Install npm: comes with Node.js"
fi

# Check Docker
if ! command -v docker &> /dev/null; then
    print_issue "high" "Docker not installed (needed for integration tests)" \
        "Install Docker Desktop: https://www.docker.com/products/docker-desktop"
elif ! docker ps > /dev/null 2>&1; then
    print_issue "high" "Docker daemon not running" "Start Docker Desktop"
fi

echo ""

# =============================================================================
# SECTION 2: Service Availability Check
# =============================================================================
echo -e "${BOLD}${CYAN}2. Service Availability Check${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

check_service "backend" 8080 "Backend API" "cd backend && mvn spring-boot:run"
check_service "frontend" 3001 "Frontend Dev Server" "cd frontend && npm run dev"
check_service "postgres" 5432 "PostgreSQL Database" "docker-compose up -d postgres"
check_service "redis" 6379 "Redis Cache" "docker-compose up -d redis"

# Check backend health
if curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"' 2>/dev/null; then
    echo -e "${GREEN}✅ Backend is healthy${NC}"
else
    print_issue "high" "Backend unhealthy or not responding" \
        "Check backend logs: tail -f backend/logs/application.log"
fi

echo ""

# =============================================================================
# SECTION 3: Test-Specific Debugging
# =============================================================================
echo -e "${BOLD}${CYAN}3. Test-Specific Analysis${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

case $TEST_TYPE in
    contract|all)
        echo -e "${BOLD}Contract Tests:${NC}"

        # Check Pact files
        if [ ! -d "$PROJECT_ROOT/frontend/pacts" ]; then
            print_issue "high" "No Pact files generated" \
                "Run frontend tests first: cd frontend && npm run test:contracts"
        fi

        # Check for sourceConfig/sourceSystem issue
        if grep -r "sourceConfig" "$PROJECT_ROOT/frontend/src" 2>/dev/null | grep -v "test" > /dev/null; then
            print_issue "critical" "Found 'sourceConfig' in frontend code - should be 'sourceSystem'" \
                "Replace all instances of sourceConfig with sourceSystem"
        fi

        # Analyze contract test logs
        analyze_log "$PROJECT_ROOT/test-reports/contract/latest.log" "Contract tests"
        ;;&

    unit|all)
        echo -e "${BOLD}Unit Tests:${NC}"

        # Check Maven dependencies
        if [ ! -d "$PROJECT_ROOT/backend/target" ]; then
            print_issue "high" "Backend not compiled" "Run: cd backend && mvn compile"
        fi

        # Check npm dependencies
        if [ ! -d "$PROJECT_ROOT/frontend/node_modules" ]; then
            print_issue "critical" "Frontend dependencies not installed" \
                "Run: cd frontend && npm install"
        fi

        # Analyze unit test logs
        analyze_log "$PROJECT_ROOT/test-reports/unit/latest.log" "Unit tests"
        ;;&

    integration|all)
        echo -e "${BOLD}Integration Tests:${NC}"

        # Check test containers
        if ! docker ps | grep -q testcontainers > /dev/null 2>&1; then
            echo -e "${DIM}No Testcontainers running (normal if tests not active)${NC}"
        fi

        # Check database migrations
        if [ -d "$PROJECT_ROOT/backend/src/main/resources/db/migration" ]; then
            local migration_count=$(ls -1 "$PROJECT_ROOT/backend/src/main/resources/db/migration" | wc -l)
            echo -e "${GREEN}✅ Found $migration_count database migrations${NC}"
        else
            print_issue "high" "No database migrations found" \
                "Create migrations in backend/src/main/resources/db/migration/"
        fi

        analyze_log "$PROJECT_ROOT/test-reports/integration/latest.log" "Integration tests"
        ;;&

    e2e|all)
        echo -e "${BOLD}E2E Tests:${NC}"

        # Check Playwright installation
        if [ ! -d "$PROJECT_ROOT/frontend/node_modules/@playwright" ]; then
            print_issue "high" "Playwright not installed" \
                "Run: cd frontend && npx playwright install --with-deps"
        fi

        # Check screenshots directory
        if [ -d "$PROJECT_ROOT/frontend/test-results" ]; then
            local screenshot_count=$(find "$PROJECT_ROOT/frontend/test-results" -name "*.png" 2>/dev/null | wc -l)
            if [ $screenshot_count -gt 0 ]; then
                echo -e "${BLUE}ℹ️  Found $screenshot_count test screenshots for debugging${NC}"
            fi
        fi

        analyze_log "$PROJECT_ROOT/test-reports/e2e/latest.log" "E2E tests"
        ;;
esac

echo ""

# =============================================================================
# SECTION 4: Configuration Check
# =============================================================================
echo -e "${BOLD}${CYAN}4. Configuration Check${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Check environment variables
if [ ! -f "$PROJECT_ROOT/.env" ] && [ ! -f "$PROJECT_ROOT/backend/.env" ]; then
    print_issue "medium" "No .env file found" \
        "Create .env file with required variables (see .env.example)"
fi

# Check Git hooks
HOOKS_PATH=$(git config core.hooksPath 2>/dev/null || echo "")
if [ "$HOOKS_PATH" != ".githooks" ]; then
    print_issue "low" "Git hooks not configured" \
        "Run: git config core.hooksPath .githooks"
fi

# Check test reports directory
if [ ! -d "$PROJECT_ROOT/test-reports" ]; then
    print_issue "medium" "Test reports directory missing" \
        "Run: mkdir -p test-reports/{contract,unit,integration,e2e,performance,security}"

    if [ "$AUTO_FIX" = true ]; then
        mkdir -p "$PROJECT_ROOT/test-reports"/{contract,unit,integration,e2e,performance,security}
        echo -e "${GREEN}✅ Created test-reports directories${NC}"
        FIXES_APPLIED=$((FIXES_APPLIED + 1))
    fi
fi

echo ""

# =============================================================================
# SECTION 5: Common Fixes
# =============================================================================
if [ "$AUTO_FIX" = true ] && [ $ISSUES_FOUND -gt 0 ]; then
    echo -e "${BOLD}${CYAN}5. Applying Automatic Fixes${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    # Fix npm dependencies
    if [ ! -d "$PROJECT_ROOT/frontend/node_modules" ]; then
        echo -e "${CYAN}Installing frontend dependencies...${NC}"
        cd "$PROJECT_ROOT/frontend" && npm install
        FIXES_APPLIED=$((FIXES_APPLIED + 1))
    fi

    # Fix Maven dependencies
    if [ ! -d "$PROJECT_ROOT/backend/target" ]; then
        echo -e "${CYAN}Compiling backend...${NC}"
        cd "$PROJECT_ROOT/backend" && mvn compile
        FIXES_APPLIED=$((FIXES_APPLIED + 1))
    fi

    # Fix Git hooks
    if [ "$HOOKS_PATH" != ".githooks" ]; then
        echo -e "${CYAN}Configuring Git hooks...${NC}"
        git config core.hooksPath .githooks
        FIXES_APPLIED=$((FIXES_APPLIED + 1))
    fi

    echo -e "${GREEN}✅ Applied $FIXES_APPLIED automatic fixes${NC}"
    echo ""
fi

# =============================================================================
# SECTION 6: Recommended Actions
# =============================================================================
echo -e "${BOLD}${CYAN}6. Recommended Debugging Steps${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

if [ $ISSUES_FOUND -eq 0 ]; then
    echo -e "${GREEN}✅ No obvious issues found!${NC}"
    echo ""
    echo "If tests are still failing, try:"
    echo "  1. Check the detailed test logs in test-reports/"
    echo "  2. Run tests individually to isolate the problem"
    echo "  3. Enable verbose logging: export DEBUG=true"
    echo "  4. Check for timing issues or race conditions"
else
    echo "Based on the analysis, try these steps in order:"
    echo ""

    STEP=1
    if ! lsof -i:8080 > /dev/null 2>&1; then
        echo "  $STEP. Start the backend:"
        echo "     ${CYAN}cd backend && mvn spring-boot:run${NC}"
        STEP=$((STEP + 1))
    fi

    if ! lsof -i:3001 > /dev/null 2>&1; then
        echo "  $STEP. Start the frontend:"
        echo "     ${CYAN}cd frontend && npm run dev${NC}"
        STEP=$((STEP + 1))
    fi

    if ! lsof -i:5432 > /dev/null 2>&1; then
        echo "  $STEP. Start the database:"
        echo "     ${CYAN}docker-compose up -d postgres${NC}"
        STEP=$((STEP + 1))
    fi

    echo "  $STEP. Clear test cache and retry:"
    echo "     ${CYAN}rm -rf test-reports/* && ./scripts/test-orchestrator.sh quick${NC}"
    STEP=$((STEP + 1))

    echo "  $STEP. If contract tests fail, regenerate Pact files:"
    echo "     ${CYAN}cd frontend && rm -rf pacts && npm run test:contracts${NC}"
    STEP=$((STEP + 1))

    echo "  $STEP. Check recent code changes:"
    echo "     ${CYAN}git diff HEAD~1${NC}"
fi

echo ""

# =============================================================================
# SECTION 7: Quick Commands
# =============================================================================
echo -e "${BOLD}${CYAN}7. Quick Debug Commands${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

cat << EOF
# View recent test failures:
grep -r "FAIL\|ERROR\|❌" test-reports/ | tail -20

# Check backend logs:
tail -f backend/logs/application.log

# Check frontend console:
cd frontend && npm run dev

# Run single test file:
cd frontend && npm test -- MyComponent.test.tsx
cd backend && mvn test -Dtest=MyServiceTest

# Run tests with debug output:
DEBUG=* npm test
mvn test -X

# Check service ports:
lsof -i :8080,3001,5432,6379

# Docker status:
docker-compose ps

# Reset everything:
docker-compose down -v
rm -rf frontend/node_modules backend/target
npm install && mvn clean install
EOF

echo ""

# =============================================================================
# Summary
# =============================================================================
echo -e "${BOLD}${PURPLE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${PURPLE}                            DEBUG SUMMARY                                       ${NC}"
echo -e "${BOLD}${PURPLE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BOLD}Issues Found:${NC}     $ISSUES_FOUND"
echo -e "${BOLD}Fixes Applied:${NC}    $FIXES_APPLIED"
echo ""

if [ $ISSUES_FOUND -eq 0 ]; then
    echo -e "${GREEN}🎉 System appears healthy! If tests still fail, check the logs.${NC}"
elif [ $ISSUES_FOUND -le 3 ]; then
    echo -e "${YELLOW}⚠️  Found some issues. Follow the recommended steps above.${NC}"
else
    echo -e "${RED}❌ Multiple issues detected. Fix critical issues first.${NC}"
fi

echo ""
echo -e "${CYAN}For more help, see: docs/TROUBLESHOOTING.md${NC}"

# Exit with appropriate code
[ $ISSUES_FOUND -eq 0 ] && exit 0 || exit 1