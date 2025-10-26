#!/bin/bash

##############################################################################
# Railway Production Deployment Test Script
# Tests all critical API endpoints against Railway deployment
##############################################################################

FRONTEND_URL="https://jivs-frontend-production.up.railway.app"
BACKEND_URL="https://jivs-backend-production.up.railway.app"
API_URL="$BACKEND_URL/api/v1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Store access token
ACCESS_TOKEN=""

##############################################################################
# Helper Functions
##############################################################################

test_start() {
    echo -e "\n${YELLOW}Testing: $1${NC}"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

test_pass() {
    echo -e "${GREEN}✓ PASS: $1${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
}

test_fail() {
    echo -e "${RED}✗ FAIL: $1${NC}"
    echo -e "${RED}  Error: $2${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
}

##############################################################################
# Infrastructure Tests
##############################################################################

test_frontend_accessible() {
    test_start "Frontend is accessible"

    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$FRONTEND_URL")

    if [ "$STATUS" -eq 200 ]; then
        test_pass "Frontend returns HTTP 200"
    else
        test_fail "Frontend accessibility" "Expected HTTP 200, got $STATUS"
    fi
}

test_backend_health() {
    test_start "Backend health endpoint"

    RESPONSE=$(curl -s "$BACKEND_URL/actuator/health")
    STATUS=$(echo "$RESPONSE" | jq -r '.status // empty')
    DB_STATUS=$(echo "$RESPONSE" | jq -r '.components.db.status // empty')

    if [ "$STATUS" = "UP" ]; then
        test_pass "Backend health is UP"
    else
        test_fail "Backend health" "Health status is $STATUS"
        return
    fi

    if [ "$DB_STATUS" = "UP" ]; then
        test_pass "Database connection is UP"
    else
        test_fail "Database connection" "Database status is $DB_STATUS"
    fi
}

##############################################################################
# Authentication Tests
##############################################################################

test_login() {
    test_start "Authentication - Login"

    RESPONSE=$(curl -s -X POST "$API_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"password"}')

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')
    ACCESS_TOKEN=$(echo "$RESPONSE" | jq -r '.data.accessToken // empty')

    if [ "$SUCCESS" = "true" ] && [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
        test_pass "Login successful, token received"
    else
        test_fail "Login" "Login failed or no token received"
        echo "$RESPONSE" | jq '.'
    fi
}

test_get_current_user() {
    test_start "Authentication - Get Current User"

    if [ -z "$ACCESS_TOKEN" ]; then
        test_fail "Get current user" "No access token available"
        return
    fi

    RESPONSE=$(curl -s -X GET "$API_URL/auth/me" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')
    USERNAME=$(echo "$RESPONSE" | jq -r '.data.username // empty')

    if [ "$SUCCESS" = "true" ] && [ "$USERNAME" = "admin" ]; then
        test_pass "Current user retrieved successfully"
    else
        test_fail "Get current user" "Failed to retrieve current user"
    fi
}

##############################################################################
# Extraction Tests
##############################################################################

test_get_extractions() {
    test_start "Extractions - List extractions"

    RESPONSE=$(curl -s -X GET "$API_URL/extractions?page=0&size=10" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')

    if [ "$SUCCESS" = "true" ]; then
        test_pass "Extractions list retrieved successfully"
    else
        test_fail "List extractions" "Failed to retrieve extractions"
    fi
}

test_create_extraction() {
    test_start "Extractions - Create extraction"

    RESPONSE=$(curl -s -X POST "$API_URL/extractions" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Railway Test Extraction",
            "description": "Test extraction from automated Railway test",
            "sourceType": "JDBC",
            "configuration": {
                "url": "jdbc:postgresql://test:5432/testdb",
                "username": "test",
                "password": "test",
                "query": "SELECT * FROM test_table"
            }
        }')

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')
    EXTRACTION_ID=$(echo "$RESPONSE" | jq -r '.data.id // empty')

    if [ "$SUCCESS" = "true" ] && [ -n "$EXTRACTION_ID" ] && [ "$EXTRACTION_ID" != "null" ]; then
        test_pass "Extraction created successfully (ID: $EXTRACTION_ID)"

        # Clean up - delete the extraction
        curl -s -X DELETE "$API_URL/extractions/$EXTRACTION_ID" \
            -H "Authorization: Bearer $ACCESS_TOKEN" > /dev/null
    else
        test_fail "Create extraction" "Failed to create extraction"
    fi
}

##############################################################################
# Migration Tests
##############################################################################

test_get_migrations() {
    test_start "Migrations - List migrations"

    RESPONSE=$(curl -s -X GET "$API_URL/migrations?page=0&size=10" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')

    if [ "$SUCCESS" = "true" ]; then
        test_pass "Migrations list retrieved successfully"
    else
        test_fail "List migrations" "Failed to retrieve migrations"
    fi
}

##############################################################################
# Data Quality Tests
##############################################################################

test_get_quality_rules() {
    test_start "Data Quality - List rules"

    RESPONSE=$(curl -s -X GET "$API_URL/data-quality/rules?page=0&size=10" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')

    if [ "$SUCCESS" = "true" ]; then
        test_pass "Quality rules list retrieved successfully"
    else
        test_fail "List quality rules" "Failed to retrieve quality rules"
    fi
}

test_get_quality_dashboard() {
    test_start "Data Quality - Dashboard"

    RESPONSE=$(curl -s -X GET "$API_URL/data-quality/dashboard" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')

    if [ "$SUCCESS" = "true" ]; then
        test_pass "Quality dashboard retrieved successfully"
    else
        test_fail "Quality dashboard" "Failed to retrieve quality dashboard"
    fi
}

##############################################################################
# Compliance Tests
##############################################################################

test_get_compliance_requests() {
    test_start "Compliance - List requests"

    RESPONSE=$(curl -s -X GET "$API_URL/compliance/requests?page=0&size=10" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')

    if [ "$SUCCESS" = "true" ]; then
        test_pass "Compliance requests list retrieved successfully"
    else
        test_fail "List compliance requests" "Failed to retrieve compliance requests"
    fi
}

test_get_retention_policies() {
    test_start "Compliance - List retention policies"

    RESPONSE=$(curl -s -X GET "$API_URL/compliance/retention-policies?page=0&size=10" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')

    if [ "$SUCCESS" = "true" ]; then
        test_pass "Retention policies list retrieved successfully"
    else
        test_fail "List retention policies" "Failed to retrieve retention policies"
    fi
}

##############################################################################
# Analytics Tests
##############################################################################

test_get_analytics_dashboard() {
    test_start "Analytics - Dashboard"

    RESPONSE=$(curl -s -X GET "$API_URL/analytics/dashboard" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')

    if [ "$SUCCESS" = "true" ]; then
        test_pass "Analytics dashboard retrieved successfully"
    else
        test_fail "Analytics dashboard" "Failed to retrieve analytics dashboard"
    fi
}

##############################################################################
# User Management Tests
##############################################################################

test_get_users() {
    test_start "Users - List users (Admin only)"

    RESPONSE=$(curl -s -X GET "$API_URL/users?page=0&size=10" \
        -H "Authorization: Bearer $ACCESS_TOKEN")

    SUCCESS=$(echo "$RESPONSE" | jq -r '.success // empty')

    if [ "$SUCCESS" = "true" ]; then
        test_pass "Users list retrieved successfully"
    else
        test_fail "List users" "Failed to retrieve users list"
    fi
}

##############################################################################
# Run All Tests
##############################################################################

echo "=============================================="
echo "Railway Production Deployment Test Suite"
echo "=============================================="
echo "Frontend: $FRONTEND_URL"
echo "Backend:  $BACKEND_URL"
echo "API:      $API_URL"
echo "=============================================="

# Infrastructure Tests
test_frontend_accessible
test_backend_health

# Authentication Tests
test_login
test_get_current_user

# Feature Tests (only if authentication succeeded)
if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
    test_get_extractions
    test_create_extraction
    test_get_migrations
    test_get_quality_rules
    test_get_quality_dashboard
    test_get_compliance_requests
    test_get_retention_policies
    test_get_analytics_dashboard
    test_get_users
else
    echo -e "\n${RED}Skipping feature tests - authentication failed${NC}"
fi

##############################################################################
# Summary
##############################################################################

echo ""
echo "=============================================="
echo "Test Summary"
echo "=============================================="
echo "Total Tests:  $TOTAL_TESTS"
echo -e "${GREEN}Passed:       $PASSED_TESTS${NC}"
echo -e "${RED}Failed:       $FAILED_TESTS${NC}"
echo "=============================================="

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}All tests passed! ✓${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed! ✗${NC}"
    exit 1
fi
