#!/bin/bash
#
# Stress Testing Script for JiVS Platform
# Performs stress testing to find system breaking points
# Usage: ./stress-test.sh [base-url]
#

set -euo pipefail

BASE_URL="${1:-http://localhost:8080/api/v1}"
RESULTS_DIR="./load-test-results"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "=========================================="
echo "JiVS Platform Stress Testing"
echo "=========================================="
echo ""
echo "Target: $BASE_URL"
echo "Results: $RESULTS_DIR"
echo ""

mkdir -p "$RESULTS_DIR"

# Stress Test Configuration
echo -e "${YELLOW}Running Stress Test...${NC}"
echo "This will gradually increase load until the system breaks"
echo ""

k6 run \
    --vus 1 \
    --duration 1m \
    --stage 1m:10 \
    --stage 2m:50 \
    --stage 3m:100 \
    --stage 2m:200 \
    --stage 2m:300 \
    --stage 1m:0 \
    --out json="$RESULTS_DIR/stress-test-${TIMESTAMP}.json" \
    --env BASE_URL="$BASE_URL" \
    k6-load-test.js

echo ""
echo -e "${GREEN}Stress test completed!${NC}"
echo "Results: $RESULTS_DIR/stress-test-${TIMESTAMP}.json"
echo ""

# Spike Test
echo -e "${YELLOW}Running Spike Test...${NC}"
echo "This will test system behavior under sudden traffic spikes"
echo ""

k6 run \
    --vus 1 \
    --duration 1m \
    --stage 10s:1 \
    --stage 30s:100 \
    --stage 1m:100 \
    --stage 30s:200 \
    --stage 1m:200 \
    --stage 30s:500 \
    --stage 1m:500 \
    --stage 10s:0 \
    --out json="$RESULTS_DIR/spike-test-${TIMESTAMP}.json" \
    --env BASE_URL="$BASE_URL" \
    k6-load-test.js

echo ""
echo -e "${GREEN}Spike test completed!${NC}"
echo "Results: $RESULTS_DIR/spike-test-${TIMESTAMP}.json"
echo ""

# Soak Test (Long Duration)
read -p "Run soak test? (24 hour endurance test) [y/N]: " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Running Soak Test...${NC}"
    echo "This will run for 24 hours at moderate load"
    echo ""

    k6 run \
        --vus 50 \
        --duration 24h \
        --out json="$RESULTS_DIR/soak-test-${TIMESTAMP}.json" \
        --env BASE_URL="$BASE_URL" \
        k6-load-test.js

    echo -e "${GREEN}Soak test completed!${NC}"
fi

echo ""
echo "=========================================="
echo "All stress tests completed!"
echo "=========================================="
echo ""
echo "Review results in: $RESULTS_DIR"
echo ""
echo "Next steps:"
echo "1. Analyze breaking points"
echo "2. Identify bottlenecks"
echo "3. Optimize identified issues"
echo "4. Adjust resource limits"
echo "5. Re-test to verify improvements"
echo ""
