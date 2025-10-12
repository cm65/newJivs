#!/bin/bash
#
# JiVS Platform - Test Data Setup Script
#
# This script prepares the environment for extraction load testing:
# - Creates test database and tables
# - Populates test data
# - Creates test user with proper roles
# - Configures test data sources
# - Validates environment
#
# Usage:
#   ./setup-test-data.sh
#   ./setup-test-data.sh --clean  # Clean and recreate
#

set -euo pipefail

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-jivs}"
POSTGRES_USER="${POSTGRES_USER:-jivs_user}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-jivs_password}"

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"
TEST_USERNAME="${TEST_USERNAME:-testuser}"
TEST_PASSWORD="${TEST_PASSWORD:-TestPassword123!}"

# Test data configuration
TEST_TABLE_NAME="test_table"
TEST_DATA_SIZES=(1000 10000 50000 100000)

echo -e "${BLUE}=========================================="
echo "JiVS Platform - Test Data Setup"
echo -e "==========================================${NC}"
echo ""

# Function: Print step
print_step() {
    echo -e "${YELLOW}→${NC} $1"
}

# Function: Print success
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

# Function: Print error
print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Function: Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Step 1: Validate prerequisites
print_step "Checking prerequisites..."

if ! command_exists psql; then
    print_error "psql (PostgreSQL client) is not installed"
    echo "Install: brew install postgresql (macOS) or apt-get install postgresql-client (Linux)"
    exit 1
fi

if ! command_exists curl; then
    print_error "curl is not installed"
    exit 1
fi

if ! command_exists jq; then
    print_error "jq is not installed (optional but recommended)"
    echo "Install: brew install jq (macOS) or apt-get install jq (Linux)"
fi

print_success "Prerequisites validated"
echo ""

# Step 2: Test database connection
print_step "Testing database connection..."

export PGPASSWORD="$POSTGRES_PASSWORD"

if psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT 1" >/dev/null 2>&1; then
    print_success "Database connection successful"
else
    print_error "Cannot connect to database"
    echo "  Host: $POSTGRES_HOST:$POSTGRES_PORT"
    echo "  Database: $POSTGRES_DB"
    echo "  User: $POSTGRES_USER"
    exit 1
fi
echo ""

# Step 3: Clean existing test data (if --clean flag)
if [[ "${1:-}" == "--clean" ]]; then
    print_step "Cleaning existing test data..."

    psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<EOF
DROP TABLE IF EXISTS ${TEST_TABLE_NAME} CASCADE;
DROP TABLE IF EXISTS test_table_1k CASCADE;
DROP TABLE IF EXISTS test_table_10k CASCADE;
DROP TABLE IF EXISTS test_table_50k CASCADE;
DROP TABLE IF EXISTS test_table_100k CASCADE;
EOF

    print_success "Existing test data cleaned"
    echo ""
fi

# Step 4: Create test tables
print_step "Creating test tables..."

psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<EOF
-- Main test table
CREATE TABLE IF NOT EXISTS ${TEST_TABLE_NAME} (
    id SERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    amount DECIMAL(10, 2),
    category VARCHAR(100),
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_test_table_status ON ${TEST_TABLE_NAME}(status);
CREATE INDEX IF NOT EXISTS idx_test_table_category ON ${TEST_TABLE_NAME}(category);
CREATE INDEX IF NOT EXISTS idx_test_table_created_at ON ${TEST_TABLE_NAME}(created_at);
EOF

print_success "Test tables created"
echo ""

# Step 5: Populate test data
print_step "Populating test data (this may take a few minutes)..."

for SIZE in "${TEST_DATA_SIZES[@]}"; do
    echo "  Generating $SIZE test records..."

    psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<EOF
-- Delete existing data for this size range (if re-running)
DELETE FROM ${TEST_TABLE_NAME} WHERE id <= ${SIZE};

-- Insert test data
INSERT INTO ${TEST_TABLE_NAME} (id, uuid, name, email, status, amount, category, description, metadata, created_at)
SELECT
    i,
    gen_random_uuid()::text,
    'Test User ' || i,
    'user' || i || '@test.com',
    CASE (i % 4)
        WHEN 0 THEN 'ACTIVE'
        WHEN 1 THEN 'INACTIVE'
        WHEN 2 THEN 'PENDING'
        ELSE 'ARCHIVED'
    END,
    (random() * 10000)::decimal(10, 2),
    CASE (i % 5)
        WHEN 0 THEN 'Category A'
        WHEN 1 THEN 'Category B'
        WHEN 2 THEN 'Category C'
        WHEN 3 THEN 'Category D'
        ELSE 'Category E'
    END,
    'Test description for record ' || i,
    jsonb_build_object(
        'key1', 'value' || i,
        'key2', random()::text,
        'key3', (i % 100)::text
    ),
    CURRENT_TIMESTAMP - (random() * interval '365 days')
FROM generate_series(1, ${SIZE}) AS i
ON CONFLICT (id) DO NOTHING;
EOF

    echo "    ✓ $SIZE records inserted"
done

# Analyze tables for query optimization
psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "ANALYZE ${TEST_TABLE_NAME};" >/dev/null

print_success "Test data populated"
echo ""

# Step 6: Verify data
print_step "Verifying test data..."

RECORD_COUNT=$(psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -c "SELECT COUNT(*) FROM ${TEST_TABLE_NAME};")
RECORD_COUNT=$(echo "$RECORD_COUNT" | xargs) # Trim whitespace

echo "  Total records in ${TEST_TABLE_NAME}: $RECORD_COUNT"

if [ "$RECORD_COUNT" -ge "${TEST_DATA_SIZES[-1]}" ]; then
    print_success "Test data verified"
else
    print_error "Expected at least ${TEST_DATA_SIZES[-1]} records, found $RECORD_COUNT"
    exit 1
fi
echo ""

# Step 7: Create/verify test user via API
print_step "Creating test user via API..."

# First, try to register the user (may fail if already exists)
REGISTER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${API_BASE_URL}/auth/register" \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"${TEST_USERNAME}\",
        \"password\": \"${TEST_PASSWORD}\",
        \"email\": \"${TEST_USERNAME}@test.com\",
        \"firstName\": \"Test\",
        \"lastName\": \"User\"
    }" 2>/dev/null || echo -e "\n400")

HTTP_CODE=$(echo "$REGISTER_RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
    print_success "Test user created"
elif [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "409" ]; then
    print_success "Test user already exists"
else
    echo "  Warning: Could not create test user (HTTP $HTTP_CODE)"
    echo "  You may need to create it manually"
fi
echo ""

# Step 8: Test authentication
print_step "Testing authentication..."

AUTH_RESPONSE=$(curl -s -X POST "${API_BASE_URL}/auth/login" \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"${TEST_USERNAME}\",
        \"password\": \"${TEST_PASSWORD}\"
    }" 2>/dev/null)

if command_exists jq; then
    ACCESS_TOKEN=$(echo "$AUTH_RESPONSE" | jq -r '.accessToken // .data.accessToken // empty')
else
    ACCESS_TOKEN=$(echo "$AUTH_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
fi

if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
    print_success "Authentication successful"
    echo "  Access Token: ${ACCESS_TOKEN:0:20}..."
else
    print_error "Authentication failed"
    echo "  Response: $AUTH_RESPONSE"
    exit 1
fi
echo ""

# Step 9: Create test environment file
print_step "Creating test environment file..."

ENV_FILE="./load-tests/.env.test"

cat > "$ENV_FILE" <<EOF
# JiVS Platform Load Test Environment Configuration
# Generated: $(date)

# API Configuration
BASE_URL=${API_BASE_URL}

# Authentication
TEST_USERNAME=${TEST_USERNAME}
TEST_PASSWORD=${TEST_PASSWORD}

# Database Configuration
POSTGRES_HOST=${POSTGRES_HOST}
POSTGRES_PORT=${POSTGRES_PORT}
POSTGRES_DB=${POSTGRES_DB}
POSTGRES_USER=${POSTGRES_USER}

# Test Data Configuration
TEST_TABLE=${TEST_TABLE_NAME}
EXTRACTION_RECORDS=10000

# Load Test Configuration
BASELINE_VUS=10
PERFORMANCE_VUS=100
STRESS_MAX_VUS=500
SOAK_VUS=50
SOAK_DURATION=2h
SPIKE_NORMAL_LOAD=20
SPIKE_MULTIPLIER=10
EOF

print_success "Environment file created: $ENV_FILE"
echo ""

# Step 10: Summary
echo -e "${BLUE}=========================================="
echo "Setup Complete!"
echo -e "==========================================${NC}"
echo ""
echo "Test Environment Summary:"
echo "  Database: $POSTGRES_HOST:$POSTGRES_PORT/$POSTGRES_DB"
echo "  Test Table: $TEST_TABLE_NAME"
echo "  Total Records: $RECORD_COUNT"
echo "  Test User: $TEST_USERNAME"
echo "  API URL: $API_BASE_URL"
echo ""
echo "Next Steps:"
echo "  1. Review test environment: cat $ENV_FILE"
echo "  2. Run baseline test: k6 run load-tests/extraction-baseline-test.js"
echo "  3. Run all tests: ./load-tests/run-extraction-tests.sh"
echo ""
echo "Test Data Sizes Available:"
for SIZE in "${TEST_DATA_SIZES[@]}"; do
    echo "  - $SIZE records (SELECT * FROM $TEST_TABLE_NAME LIMIT $SIZE)"
done
echo ""
echo -e "${GREEN}Setup successful! Ready for load testing.${NC}"
echo ""
