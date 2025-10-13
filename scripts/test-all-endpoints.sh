#!/bin/bash

# Comprehensive API endpoint testing script
# Tests CRUD operations for all major entities to catch issues before users do

set -e

BASE_URL="${1:-http://localhost:8080}"
RESULTS_FILE="${2:-/tmp/api-test-results.json}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Results array
declare -a FAILURES=()

# Helper functions
log_test() {
    echo -e "${YELLOW}[TEST]${NC} $1"
}

log_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((PASSED_TESTS++))
}

log_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((FAILED_TESTS++))
    FAILURES+=("$1")
}

# Get JWT token
echo "=========================================="
echo "JiVS Platform - Comprehensive API Tests"
echo "=========================================="
echo ""

log_test "Authenticating..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}')

TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}[FATAL]${NC} Authentication failed. Cannot proceed with tests."
    exit 1
fi

log_pass "Authentication successful"
echo ""

# Test helper function
test_endpoint() {
    local name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_status="${5:-200}"

    ((TOTAL_TESTS++))
    log_test "$name"

    if [ -z "$data" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json")
    else
        RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data")
    fi

    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "$expected_status" ]; then
        # Check if response contains error
        if echo "$BODY" | grep -q '"error"'; then
            log_fail "$name - Returned $HTTP_CODE but contains error: $(echo "$BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('error', 'Unknown error'))" 2>/dev/null || echo "$BODY")"
            return 1
        fi
        log_pass "$name - HTTP $HTTP_CODE"
        echo "$BODY"
        return 0
    else
        log_fail "$name - Expected $expected_status, got $HTTP_CODE"
        echo "Response: $BODY" | head -c 200
        return 1
    fi
}

# ==========================================
# EXTRACTION TESTS
# ==========================================
echo "==========================================
echo "Testing Extractions API"
echo "=========================================="

# List extractions
test_endpoint "List extractions" "GET" "/api/v1/extractions?page=0&size=10" "" "200" > /dev/null

# Create extraction
EXTRACTION_DATA='{
  "name": "API Test Extraction",
  "sourceType": "POSTGRESQL",
  "connectionConfig": {
    "host": "localhost",
    "port": "5432",
    "database": "test_db"
  },
  "extractionQuery": "SELECT * FROM test_table"
}'

EXTRACTION_RESPONSE=$(test_endpoint "Create extraction" "POST" "/api/v1/extractions" "$EXTRACTION_DATA" "201")
EXTRACTION_ID=$(echo "$EXTRACTION_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ -n "$EXTRACTION_ID" ]; then
    log_pass "Extraction created with ID: $EXTRACTION_ID"

    # Get extraction by ID
    test_endpoint "Get extraction by ID" "GET" "/api/v1/extractions/$EXTRACTION_ID" "" "200" > /dev/null

    # Delete extraction
    test_endpoint "Delete extraction" "DELETE" "/api/v1/extractions/$EXTRACTION_ID" "" "200" > /dev/null
else
    log_fail "Create extraction - Failed to extract ID from response"
fi

echo ""

# ==========================================
# MIGRATION TESTS
# ==========================================
echo "=========================================="
echo "Testing Migrations API"
echo "=========================================="

# List migrations
test_endpoint "List migrations" "GET" "/api/v1/migrations?page=0&size=10" "" "200" > /dev/null

# Create migration
MIGRATION_DATA='{
  "name": "API Test Migration",
  "description": "Automated test migration",
  "sourceSystem": "Oracle Database",
  "targetSystem": "PostgreSQL 15",
  "migrationType": "FULL_MIGRATION"
}'

MIGRATION_RESPONSE=$(test_endpoint "Create migration" "POST" "/api/v1/migrations" "$MIGRATION_DATA" "201")
MIGRATION_ID=$(echo "$MIGRATION_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ -n "$MIGRATION_ID" ]; then
    log_pass "Migration created with ID: $MIGRATION_ID"

    # Get migration by ID
    test_endpoint "Get migration by ID" "GET" "/api/v1/migrations/$MIGRATION_ID" "" "200" > /dev/null

    # Get migration progress
    test_endpoint "Get migration progress" "GET" "/api/v1/migrations/$MIGRATION_ID/progress" "" "200" > /dev/null

    # Delete migration
    test_endpoint "Delete migration" "DELETE" "/api/v1/migrations/$MIGRATION_ID" "" "200" > /dev/null
else
    log_fail "Create migration - Failed to extract ID from response"
fi

echo ""

# ==========================================
# DATA QUALITY TESTS
# ==========================================
echo "=========================================="
echo "Testing Data Quality API"
echo "=========================================="

# Get data quality dashboard
test_endpoint "Get quality dashboard" "GET" "/api/v1/data-quality/dashboard" "" "200" > /dev/null

# List quality rules
test_endpoint "List quality rules" "GET" "/api/v1/data-quality/rules" "" "200" > /dev/null

# Create quality rule
QUALITY_RULE_DATA='{
  "name": "API Test Rule",
  "description": "Automated test quality rule",
  "ruleType": "NOT_NULL",
  "fieldName": "email",
  "severity": "HIGH"
}'

RULE_RESPONSE=$(test_endpoint "Create quality rule" "POST" "/api/v1/data-quality/rules" "$QUALITY_RULE_DATA" "201")
RULE_ID=$(echo "$RULE_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null)

if [ -n "$RULE_ID" ]; then
    log_pass "Quality rule created with ID: $RULE_ID"

    # Get rule by ID
    test_endpoint "Get quality rule by ID" "GET" "/api/v1/data-quality/rules/$RULE_ID" "" "200" > /dev/null

    # Delete rule
    test_endpoint "Delete quality rule" "DELETE" "/api/v1/data-quality/rules/$RULE_ID" "" "200" > /dev/null
else
    log_fail "Create quality rule - Failed to extract ID from response"
fi

echo ""

# ==========================================
# COMPLIANCE TESTS
# ==========================================
echo "=========================================="
echo "Testing Compliance API"
echo "=========================================="

# Get compliance dashboard
test_endpoint "Get compliance dashboard" "GET" "/api/v1/compliance/dashboard" "" "200" > /dev/null

# List compliance requests
test_endpoint "List compliance requests" "GET" "/api/v1/compliance/requests" "" "200" > /dev/null

echo ""

# ==========================================
# ANALYTICS TESTS
# ==========================================
echo "=========================================="
echo "Testing Analytics API"
echo "=========================================="

# Get dashboard analytics
test_endpoint "Get dashboard analytics" "GET" "/api/v1/analytics/dashboard" "" "200" > /dev/null

# Get extraction analytics
test_endpoint "Get extraction analytics" "GET" "/api/v1/analytics/extractions" "" "200" > /dev/null

# Get migration analytics
test_endpoint "Get migration analytics" "GET" "/api/v1/analytics/migrations" "" "200" > /dev/null

# Get data quality analytics
test_endpoint "Get quality analytics" "GET" "/api/v1/analytics/data-quality" "" "200" > /dev/null

echo ""

# ==========================================
# SUMMARY
# ==========================================
echo "=========================================="
echo "TEST SUMMARY"
echo "=========================================="
echo "Total Tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -gt 0 ]; then
    echo -e "${RED}FAILURES:${NC}"
    for failure in "${FAILURES[@]}"; do
        echo "  - $failure"
    done
    echo ""
    exit 1
else
    echo -e "${GREEN}✅ ALL TESTS PASSED${NC}"
    exit 0
fi
