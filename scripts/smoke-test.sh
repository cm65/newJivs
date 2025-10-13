#!/bin/bash

# Smoke Test Script
# Runs after every deployment to verify critical user flows work
# WOULD HAVE CAUGHT: Migration creation error immediately after deployment

set -e

BASE_URL="${1:-http://localhost:8080}"

echo "🔥 Running Smoke Tests..."
echo "Target: $BASE_URL"
echo ""

# Get auth token
echo "[1/5] Testing authentication..."
TOKEN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | \
  python3 -c "import sys, json; print(json.load(sys.stdin)['data']['accessToken'])")

if [ -z "$TOKEN" ]; then
    echo "❌ SMOKE TEST FAILED: Authentication"
    exit 1
fi
echo "✅ Authentication works"

# Test migration creation (THE KEY TEST THAT WAS MISSING)
echo "[2/5] Testing migration creation..."
MIGRATION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/migrations" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke Test Migration","sourceSystem":"A","targetSystem":"B","migrationType":"FULL"}')

if echo "$MIGRATION_RESPONSE" | grep -q '"error"'; then
    echo "❌ SMOKE TEST FAILED: Migration creation"
    echo "Error: $(echo "$MIGRATION_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('error', 'Unknown'))")"
    exit 1
fi
echo "✅ Migration creation works"

# Test extraction creation
echo "[3/5] Testing extraction creation..."
EXTRACTION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/extractions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke Test","sourceType":"POSTGRESQL","connectionConfig":{},"extractionQuery":"SELECT 1"}')

if echo "$EXTRACTION_RESPONSE" | grep -q '"error"'; then
    echo "❌ SMOKE TEST FAILED: Extraction creation"
    exit 1
fi
echo "✅ Extraction creation works"

# Test data quality rule creation
echo "[4/5] Testing quality rule creation..."
RULE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/data-quality/rules" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke Test Rule","ruleType":"NOT_NULL","fieldName":"test","severity":"HIGH"}')

if echo "$RULE_RESPONSE" | grep -q '"error"'; then
    echo "❌ SMOKE TEST FAILED: Quality rule creation"
    exit 1
fi
echo "✅ Quality rule creation works"

# Test analytics endpoints
echo "[5/5] Testing analytics..."
ANALYTICS_RESPONSE=$(curl -s "$BASE_URL/api/v1/analytics/dashboard" \
  -H "Authorization: Bearer $TOKEN")

if echo "$ANALYTICS_RESPONSE" | grep -q '"error"'; then
    echo "❌ SMOKE TEST FAILED: Analytics"
    exit 1
fi
echo "✅ Analytics works"

echo ""
echo "✅ ALL SMOKE TESTS PASSED"
echo "🎉 Deployment verification successful"
