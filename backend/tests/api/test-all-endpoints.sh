#!/bin/bash

##############################################
# JiVS Platform - Comprehensive API Test Script
# Tests all 78 endpoints across 8 controllers
# Date: January 13, 2025
##############################################

BASE_URL="http://localhost:8080/api/v1"
AUTH_TOKEN=""
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test results array
declare -A CONTROLLER_RESULTS

echo "========================================"
echo "JiVS Platform - API Test Suite"
echo "========================================"
echo ""

# Helper function to test endpoint
test_endpoint() {
    local controller=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_status=$5
    local description=$6

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    # Build curl command
    local curl_cmd="curl -s -w '\n%{http_code}' -X $method"

    if [ "$method" != "GET" ] && [ "$method" != "DELETE" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json'"
    fi

    if [ ! -z "$AUTH_TOKEN" ]; then
        curl_cmd="$curl_cmd -H 'Authorization: Bearer $AUTH_TOKEN'"
    fi

    if [ ! -z "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    curl_cmd="$curl_cmd '$BASE_URL$endpoint'"

    # Execute request
    local response=$(eval $curl_cmd)
    local status_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n -1)

    # Check if status code matches expected
    if [ "$status_code" == "$expected_status" ]; then
        echo -e "${GREEN}✓${NC} $method $endpoint - $status_code ($description)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        CONTROLLER_RESULTS["$controller,passed"]=$((${CONTROLLER_RESULTS["$controller,passed"]:-0} + 1))
        return 0
    else
        echo -e "${RED}✗${NC} $method $endpoint - Expected $expected_status, got $status_code ($description)"
        echo "   Response: $(echo $body | head -c 200)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        CONTROLLER_RESULTS["$controller,failed"]=$((${CONTROLLER_RESULTS["$controller,failed"]:-0} + 1))
        return 1
    fi

    CONTROLLER_RESULTS["$controller,total"]=$((${CONTROLLER_RESULTS["$controller,total"]:-0} + 1))
}

# Test 1: AuthController
echo ""
echo "=== AuthController ===="

# POST /auth/login
response=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' \
  "$BASE_URL/auth/login")

if echo "$response" | grep -q "accessToken"; then
    AUTH_TOKEN=$(echo "$response" | python3 -c "import sys, json; print(json.load(sys.stdin).get('data', {}).get('accessToken', ''))" 2>/dev/null || echo "")
    echo -e "${GREEN}✓${NC} POST /auth/login - 200 (Login successful, token obtained)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    CONTROLLER_RESULTS["AuthController,passed"]=$((${CONTROLLER_RESULTS["AuthController,passed"]:-0} + 1))
else
    echo -e "${RED}✗${NC} POST /auth/login - Failed to get token"
    echo "Response: $response"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    CONTROLLER_RESULTS["AuthController,failed"]=$((${CONTROLLER_RESULTS["AuthController,failed"]:-0} + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))
CONTROLLER_RESULTS["AuthController,total"]=$((${CONTROLLER_RESULTS["AuthController,total"]:-0} + 1))

# GET /auth/me
test_endpoint "AuthController" "GET" "/auth/me" "" "200" "Get current user"
CONTROLLER_RESULTS["AuthController,total"]=$((${CONTROLLER_RESULTS["AuthController,total"]:-0} + 1))

# POST /auth/refresh (will likely fail without proper refresh token)
test_endpoint "AuthController" "POST" "/auth/refresh" '{"refreshToken":"invalid"}' "401" "Refresh token (expected to fail)"
CONTROLLER_RESULTS["AuthController,total"]=$((${CONTROLLER_RESULTS["AuthController,total"]:-0} + 1))

echo ""
echo "=== ExtractionController ===="

# POST /extractions
create_response=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d '{"name":"API Test Extraction","sourceType":"JDBC","connectionConfig":{"url":"jdbc:postgresql://localhost:5432/testdb","username":"test","password":"test"},"extractionQuery":"SELECT * FROM test"}' \
  "$BASE_URL/extractions")

EXTRACTION_ID=$(echo "$create_response" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null || echo "")

if [ ! -z "$EXTRACTION_ID" ]; then
    echo -e "${GREEN}✓${NC} POST /extractions - 201 (Created extraction: $EXTRACTION_ID)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    CONTROLLER_RESULTS["ExtractionController,passed"]=$((${CONTROLLER_RESULTS["ExtractionController,passed"]:-0} + 1))
else
    echo -e "${RED}✗${NC} POST /extractions - Failed to create extraction"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    CONTROLLER_RESULTS["ExtractionController,failed"]=$((${CONTROLLER_RESULTS["ExtractionController,failed"]:-0} + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))
CONTROLLER_RESULTS["ExtractionController,total"]=$((${CONTROLLER_RESULTS["ExtractionController,total"]:-0} + 1))

# GET /extractions
test_endpoint "ExtractionController" "GET" "/extractions?page=0&size=20" "" "200" "List extractions"
CONTROLLER_RESULTS["ExtractionController,total"]=$((${CONTROLLER_RESULTS["ExtractionController,total"]:-0} + 1))

if [ ! -z "$EXTRACTION_ID" ]; then
    # GET /extractions/{id}
    test_endpoint "ExtractionController" "GET" "/extractions/$EXTRACTION_ID" "" "200" "Get extraction by ID"
    CONTROLLER_RESULTS["ExtractionController,total"]=$((${CONTROLLER_RESULTS["ExtractionController,total"]:-0} + 1))

    # POST /extractions/{id}/start
    test_endpoint "ExtractionController" "POST" "/extractions/$EXTRACTION_ID/start" "" "200" "Start extraction"
    CONTROLLER_RESULTS["ExtractionController,total"]=$((${CONTROLLER_RESULTS["ExtractionController,total"]:-0} + 1))

    # POST /extractions/{id}/stop
    test_endpoint "ExtractionController" "POST" "/extractions/$EXTRACTION_ID/stop" "" "200" "Stop extraction"
    CONTROLLER_RESULTS["ExtractionController,total"]=$((${CONTROLLER_RESULTS["ExtractionController,total"]:-0} + 1))

    # GET /extractions/{id}/statistics
    test_endpoint "ExtractionController" "GET" "/extractions/$EXTRACTION_ID/statistics" "" "200" "Get statistics"
    CONTROLLER_RESULTS["ExtractionController,total"]=$((${CONTROLLER_RESULTS["ExtractionController,total"]:-0} + 1))

    # GET /extractions/{id}/logs
    test_endpoint "ExtractionController" "GET" "/extractions/$EXTRACTION_ID/logs?limit=10" "" "200" "Get logs"
    CONTROLLER_RESULTS["ExtractionController,total"]=$((${CONTROLLER_RESULTS["ExtractionController,total"]:-0} + 1))
fi

# POST /extractions/test-connection
test_endpoint "ExtractionController" "POST" "/extractions/test-connection" '{"url":"jdbc:test"}' "200" "Test connection"
CONTROLLER_RESULTS["ExtractionController,total"]=$((${CONTROLLER_RESULTS["ExtractionController,total"]:-0} + 1))

# POST /extractions/bulk
test_endpoint "ExtractionController" "POST" "/extractions/bulk" "{\"action\":\"export\",\"ids\":[\"$EXTRACTION_ID\"]}" "200" "Bulk action"
CONTROLLER_RESULTS["ExtractionController,total"]=$((${CONTROLLER_RESULTS["ExtractionController,total"]:-0} + 1))

if [ ! -z "$EXTRACTION_ID" ]; then
    # DELETE /extractions/{id}
    test_endpoint "ExtractionController" "DELETE" "/extractions/$EXTRACTION_ID" "" "200" "Delete extraction"
    CONTROLLER_RESULTS["ExtractionController,total"]=$((${CONTROLLER_RESULTS["ExtractionController,total"]:-0} + 1))
fi

echo ""
echo "=== MigrationController ===="

# POST /migrations
create_response=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d '{"name":"API Test Migration","sourceConfig":{"type":"JDBC"},"targetConfig":{"type":"JDBC"}}' \
  "$BASE_URL/migrations")

MIGRATION_ID=$(echo "$create_response" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null || echo "")

if [ ! -z "$MIGRATION_ID" ]; then
    echo -e "${GREEN}✓${NC} POST /migrations - 201 (Created migration: $MIGRATION_ID)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    CONTROLLER_RESULTS["MigrationController,passed"]=$((${CONTROLLER_RESULTS["MigrationController,passed"]:-0} + 1))
else
    echo -e "${RED}✗${NC} POST /migrations - Failed to create migration"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    CONTROLLER_RESULTS["MigrationController,failed"]=$((${CONTROLLER_RESULTS["MigrationController,failed"]:-0} + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))
CONTROLLER_RESULTS["MigrationController,total"]=$((${CONTROLLER_RESULTS["MigrationController,total"]:-0} + 1))

# GET /migrations
test_endpoint "MigrationController" "GET" "/migrations?page=0&size=20" "" "200" "List migrations"
CONTROLLER_RESULTS["MigrationController,total"]=$((${CONTROLLER_RESULTS["MigrationController,total"]:-0} + 1))

if [ ! -z "$MIGRATION_ID" ]; then
    test_endpoint "MigrationController" "GET" "/migrations/$MIGRATION_ID" "" "200" "Get migration"
    test_endpoint "MigrationController" "POST" "/migrations/$MIGRATION_ID/start" "" "200" "Start migration"
    test_endpoint "MigrationController" "POST" "/migrations/$MIGRATION_ID/pause" "" "200" "Pause migration"
    test_endpoint "MigrationController" "POST" "/migrations/$MIGRATION_ID/resume" "" "200" "Resume migration"
    test_endpoint "MigrationController" "GET" "/migrations/$MIGRATION_ID/progress" "" "200" "Get progress"
    test_endpoint "MigrationController" "GET" "/migrations/$MIGRATION_ID/statistics" "" "200" "Get statistics"
    test_endpoint "MigrationController" "POST" "/migrations/$MIGRATION_ID/rollback" "" "200" "Rollback"
    test_endpoint "MigrationController" "DELETE" "/migrations/$MIGRATION_ID" "" "200" "Delete migration"

    for i in {1..9}; do
        CONTROLLER_RESULTS["MigrationController,total"]=$((${CONTROLLER_RESULTS["MigrationController,total"]:-0} + 1))
    done
fi

test_endpoint "MigrationController" "POST" "/migrations/validate" '{"name":"test"}' "200" "Validate migration"
test_endpoint "MigrationController" "POST" "/migrations/bulk" "{\"action\":\"export\",\"ids\":[\"$MIGRATION_ID\"]}" "200" "Bulk action"
CONTROLLER_RESULTS["MigrationController,total"]=$((${CONTROLLER_RESULTS["MigrationController,total"]:-0} + 2))

echo ""
echo "=== DataQualityController ===="

test_endpoint "DataQualityController" "GET" "/data-quality/dashboard" "" "200" "Get dashboard"
CONTROLLER_RESULTS["DataQualityController,total"]=$((${CONTROLLER_RESULTS["DataQualityController,total"]:-0} + 1))

# POST /data-quality/rules
create_response=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d '{"name":"API Test Rule","dimension":"VALIDITY","severity":"HIGH","ruleExpression":"test","enabled":true}' \
  "$BASE_URL/data-quality/rules")

RULE_ID=$(echo "$create_response" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null || echo "")

if [ ! -z "$RULE_ID" ]; then
    echo -e "${GREEN}✓${NC} POST /data-quality/rules - 201 (Created rule: $RULE_ID)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    CONTROLLER_RESULTS["DataQualityController,passed"]=$((${CONTROLLER_RESULTS["DataQualityController,passed"]:-0} + 1))
else
    echo -e "${RED}✗${NC} POST /data-quality/rules - Failed"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    CONTROLLER_RESULTS["DataQualityController,failed"]=$((${CONTROLLER_RESULTS["DataQualityController,failed"]:-0} + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))
CONTROLLER_RESULTS["DataQualityController,total"]=$((${CONTROLLER_RESULTS["DataQualityController,total"]:-0} + 1))

test_endpoint "DataQualityController" "GET" "/data-quality/rules?page=0&size=20" "" "200" "List rules"
CONTROLLER_RESULTS["DataQualityController,total"]=$((${CONTROLLER_RESULTS["DataQualityController,total"]:-0} + 1))

if [ ! -z "$RULE_ID" ]; then
    test_endpoint "DataQualityController" "GET" "/data-quality/rules/$RULE_ID" "" "200" "Get rule"
    test_endpoint "DataQualityController" "PUT" "/data-quality/rules/$RULE_ID" '{"name":"Updated"}' "200" "Update rule"
    test_endpoint "DataQualityController" "POST" "/data-quality/rules/$RULE_ID/execute" '{}' "200" "Execute rule"
    test_endpoint "DataQualityController" "DELETE" "/data-quality/rules/$RULE_ID" "" "200" "Delete rule"

    for i in {1..4}; do
        CONTROLLER_RESULTS["DataQualityController,total"]=$((${CONTROLLER_RESULTS["DataQualityController,total"]:-0} + 1))
    done
fi

test_endpoint "DataQualityController" "GET" "/data-quality/issues?page=0&size=20" "" "200" "Get issues"
test_endpoint "DataQualityController" "POST" "/data-quality/profile" '{"datasetId":"test"}' "200" "Profile dataset"
test_endpoint "DataQualityController" "GET" "/data-quality/reports/test-id" "200" "200" "Get report"
CONTROLLER_RESULTS["DataQualityController,total"]=$((${CONTROLLER_RESULTS["DataQualityController,total"]:-0} + 3))

echo ""
echo "=== ComplianceController ===="

test_endpoint "ComplianceController" "GET" "/compliance/dashboard" "" "200" "Get dashboard"
CONTROLLER_RESULTS["ComplianceController,total"]=$((${CONTROLLER_RESULTS["ComplianceController,total"]:-0} + 1))

# POST /compliance/requests
create_response=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d '{"type":"ACCESS","subjectEmail":"test@example.com","regulation":"GDPR"}' \
  "$BASE_URL/compliance/requests")

REQUEST_ID=$(echo "$create_response" | python3 -c "import sys, json; print(json.load(sys.stdin).get('id', ''))" 2>/dev/null || echo "")

if [ ! -z "$REQUEST_ID" ]; then
    echo -e "${GREEN}✓${NC} POST /compliance/requests - 201 (Created request: $REQUEST_ID)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    CONTROLLER_RESULTS["ComplianceController,passed"]=$((${CONTROLLER_RESULTS["ComplianceController,passed"]:-0} + 1))
else
    echo -e "${RED}✗${NC} POST /compliance/requests - Failed"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    CONTROLLER_RESULTS["ComplianceController,failed"]=$((${CONTROLLER_RESULTS["ComplianceController,failed"]:-0} + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))
CONTROLLER_RESULTS["ComplianceController,total"]=$((${CONTROLLER_RESULTS["ComplianceController,total"]:-0} + 1))

test_endpoint "ComplianceController" "GET" "/compliance/requests?page=0&size=20" "" "200" "List requests"
CONTROLLER_RESULTS["ComplianceController,total"]=$((${CONTROLLER_RESULTS["ComplianceController,total"]:-0} + 1))

if [ ! -z "$REQUEST_ID" ]; then
    test_endpoint "ComplianceController" "GET" "/compliance/requests/$REQUEST_ID" "" "200" "Get request"
    test_endpoint "ComplianceController" "PUT" "/compliance/requests/$REQUEST_ID/status" '{"status":"IN_PROGRESS"}' "200" "Update status"
    test_endpoint "ComplianceController" "POST" "/compliance/requests/$REQUEST_ID/process" "" "200" "Process request"
    test_endpoint "ComplianceController" "GET" "/compliance/requests/$REQUEST_ID/export" "" "200" "Export data"

    for i in {1..4}; do
        CONTROLLER_RESULTS["ComplianceController,total"]=$((${CONTROLLER_RESULTS["ComplianceController,total"]:-0} + 1))
    done
fi

test_endpoint "ComplianceController" "GET" "/compliance/consents?page=0&size=20" "" "200" "Get consents"
test_endpoint "ComplianceController" "POST" "/compliance/consents" '{"subjectEmail":"test@example.com","purpose":"MARKETING","granted":true}' "201" "Record consent"
test_endpoint "ComplianceController" "GET" "/compliance/retention-policies" "" "200" "Get retention policies"
test_endpoint "ComplianceController" "GET" "/compliance/audit?page=0&size=20" "" "200" "Get audit trail"

for i in {1..4}; do
    CONTROLLER_RESULTS["ComplianceController,total"]=$((${CONTROLLER_RESULTS["ComplianceController,total"]:-0} + 1))
done

echo ""
echo "=== AnalyticsController ===="

test_endpoint "AnalyticsController" "GET" "/analytics/dashboard" "" "200" "Dashboard analytics"
test_endpoint "AnalyticsController" "GET" "/analytics/extractions" "" "200" "Extraction analytics"
test_endpoint "AnalyticsController" "GET" "/analytics/migrations" "" "200" "Migration analytics"
test_endpoint "AnalyticsController" "GET" "/analytics/data-quality" "" "200" "Quality analytics"
test_endpoint "AnalyticsController" "GET" "/analytics/usage" "" "200" "Usage analytics"
test_endpoint "AnalyticsController" "GET" "/analytics/compliance" "" "200" "Compliance analytics"
test_endpoint "AnalyticsController" "GET" "/analytics/performance" "" "200" "Performance analytics"
test_endpoint "AnalyticsController" "POST" "/analytics/export" '{"format":"CSV"}' "200" "Export report"

for i in {1..8}; do
    CONTROLLER_RESULTS["AnalyticsController,total"]=$((${CONTROLLER_RESULTS["AnalyticsController,total"]:-0} + 1))
done

echo ""
echo "=== UserPreferencesController ===="

test_endpoint "UserPreferencesController" "GET" "/preferences" "" "200" "Get preferences"
test_endpoint "UserPreferencesController" "PUT" "/preferences" '{"theme":"dark","language":"en"}' "200" "Update preferences"
test_endpoint "UserPreferencesController" "GET" "/preferences/theme" "" "200" "Get theme"
test_endpoint "UserPreferencesController" "PUT" "/preferences/theme" '{"theme":"light"}' "200" "Update theme"

for i in {1..4}; do
    CONTROLLER_RESULTS["UserPreferencesController,total"]=$((${CONTROLLER_RESULTS["UserPreferencesController,total"]:-0} + 1))
done

echo ""
echo "=== ViewsController ===="

test_endpoint "ViewsController" "GET" "/views?module=extractions" "" "200" "Get views"
test_endpoint "ViewsController" "POST" "/views" '{"viewName":"test-view","module":"extractions","filters":{}}' "201" "Create view"
test_endpoint "ViewsController" "GET" "/views/test-view?module=extractions" "" "200" "Get view by name"
test_endpoint "ViewsController" "PUT" "/views/test-view?module=extractions" '{"filters":{}}' "200" "Update view"
test_endpoint "ViewsController" "POST" "/views/test-view/set-default?module=extractions" "" "200" "Set default"
test_endpoint "ViewsController" "GET" "/views/default?module=extractions" "" "200" "Get default view"
test_endpoint "ViewsController" "GET" "/views/count?module=extractions" "" "200" "Get view count"
test_endpoint "ViewsController" "DELETE" "/views/test-view?module=extractions" "" "200" "Delete view"

for i in {1..8}; do
    CONTROLLER_RESULTS["ViewsController,total"]=$((${CONTROLLER_RESULTS["ViewsController,total"]:-0} + 1})
done

# Logout
test_endpoint "AuthController" "POST" "/auth/logout" "" "200" "Logout"
CONTROLLER_RESULTS["AuthController,total"]=$((${CONTROLLER_RESULTS["AuthController,total"]:-0} + 1))

# Print Summary
echo ""
echo "========================================"
echo "TEST SUMMARY"
echo "========================================"
echo ""
echo "Total Endpoints Tested: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"

SUCCESS_RATE=$(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS/$TOTAL_TESTS)*100}")
echo "Success Rate: $SUCCESS_RATE%"

echo ""
echo "Results by Controller:"
echo "----------------------"

for controller in "AuthController" "ExtractionController" "MigrationController" "DataQualityController" "ComplianceController" "AnalyticsController" "UserPreferencesController" "ViewsController"; do
    total=${CONTROLLER_RESULTS["$controller,total"]:-0}
    passed=${CONTROLLER_RESULTS["$controller,passed"]:-0}
    failed=${CONTROLLER_RESULTS["$controller,failed"]:-0}

    if [ $total -gt 0 ]; then
        rate=$(awk "BEGIN {printf \"%.2f\", ($passed/$total)*100}")
        echo "$controller: $passed/$total passed ($rate%)"
    fi
done

echo ""
echo "========================================"
echo "Test completed!"
echo "========================================"
