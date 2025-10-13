#!/bin/bash

################################################################################
# JiVS Platform - Performance Testing Runner
#
# This script runs k6 performance tests with different profiles.
# Part of Layer 5: Performance Testing
#
# Usage:
#   ./run-performance-tests.sh [mode] [options]
#
# Modes:
#   quick    - 5-minute smoke test (10 VUs)
#   load     - 15-minute load test (100 VUs)
#   stress   - 30-minute stress test (500 VUs)
#   spike    - 10-minute spike test (200 VUs)
#   soak     - 1-hour endurance test (50 VUs)
#   full     - Complete test suite (47 minutes)
#
# Options:
#   --url <url>     - Target URL (default: http://localhost:8080)
#   --report        - Generate HTML report
#   --dashboard     - Open k6 dashboard
#   --cloud         - Run on k6 cloud (requires API key)
#
# Examples:
#   ./run-performance-tests.sh quick
#   ./run-performance-tests.sh load --url https://staging.jivs.com --report
#   ./run-performance-tests.sh full --dashboard
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
NC='\033[0m'

# Defaults
MODE="quick"
BASE_URL="http://localhost:8080"
GENERATE_REPORT=false
OPEN_DASHBOARD=false
USE_CLOUD=false
K6_SCRIPT="tests/performance/k6-load-test.js"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        quick|load|stress|spike|soak|full)
            MODE=$1
            shift
            ;;
        --url)
            BASE_URL=$2
            shift 2
            ;;
        --report)
            GENERATE_REPORT=true
            shift
            ;;
        --dashboard)
            OPEN_DASHBOARD=true
            shift
            ;;
        --cloud)
            USE_CLOUD=true
            shift
            ;;
        --help)
            echo "Usage: $0 [mode] [options]"
            echo "Modes: quick, load, stress, spike, soak, full"
            echo "Options:"
            echo "  --url <url>    - Target URL"
            echo "  --report       - Generate HTML report"
            echo "  --dashboard    - Open k6 dashboard"
            echo "  --cloud        - Run on k6 cloud"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo -e "${BOLD}${PURPLE}╔══════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${PURPLE}║               JiVS PLATFORM - PERFORMANCE TESTING                     ║${NC}"
echo -e "${BOLD}${PURPLE}╚══════════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}Mode: ${BOLD}$MODE${NC}"
echo -e "${CYAN}Target: ${BOLD}$BASE_URL${NC}"
echo -e "${CYAN}Dashboard: ${BOLD}$([ "$OPEN_DASHBOARD" = true ] && echo "Enabled" || echo "Disabled")${NC}"
echo -e "${CYAN}Report: ${BOLD}$([ "$GENERATE_REPORT" = true ] && echo "Yes" || echo "No")${NC}"
echo ""

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo -e "${YELLOW}⚠️  k6 is not installed. Installing...${NC}"

    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        brew install k6
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
        echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
        sudo apt-get update
        sudo apt-get install k6
    else
        echo -e "${RED}❌ Unsupported OS. Please install k6 manually: https://k6.io/docs/getting-started/installation/${NC}"
        exit 1
    fi
fi

# Create test reports directory
mkdir -p "$PROJECT_ROOT/test-reports/performance"

# Check if backend is running
echo -e "${CYAN}Checking backend availability...${NC}"
if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" | grep -q "200"; then
    echo -e "${GREEN}✅ Backend is healthy${NC}"
else
    echo -e "${YELLOW}⚠️  Backend not responding at $BASE_URL${NC}"
    echo -e "${YELLOW}Starting backend...${NC}"

    # Try to start backend
    cd "$PROJECT_ROOT/backend"
    mvn spring-boot:run > /tmp/backend-perf.log 2>&1 &
    BACKEND_PID=$!

    # Wait for backend to start
    echo -n "Waiting for backend to start"
    for i in {1..30}; do
        if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" | grep -q "200"; then
            echo -e "\n${GREEN}✅ Backend started${NC}"
            break
        fi
        echo -n "."
        sleep 2
    done

    if ! curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" | grep -q "200"; then
        echo -e "\n${RED}❌ Failed to start backend${NC}"
        exit 1
    fi
fi

# Prepare k6 options based on mode
case $MODE in
    quick)
        K6_OPTIONS="--vus 10 --duration 5m"
        echo -e "${CYAN}Running quick smoke test (5 minutes, 10 VUs)...${NC}"
        ;;
    load)
        K6_OPTIONS="--vus 100 --duration 15m --stage 2m:50,5m:50,2m:100,5m:100,1m:0"
        echo -e "${CYAN}Running load test (15 minutes, up to 100 VUs)...${NC}"
        ;;
    stress)
        K6_OPTIONS="--vus 500 --duration 30m --stage 2m:100,5m:200,5m:300,5m:400,5m:500,8m:0"
        echo -e "${CYAN}Running stress test (30 minutes, up to 500 VUs)...${NC}"
        ;;
    spike)
        K6_OPTIONS="--vus 200 --duration 10m --stage 30s:10,5s:200,3m:200,10s:10,6m:10"
        echo -e "${CYAN}Running spike test (10 minutes, spike to 200 VUs)...${NC}"
        ;;
    soak)
        K6_OPTIONS="--vus 50 --duration 60m"
        echo -e "${CYAN}Running soak test (1 hour, 50 VUs)...${NC}"
        ;;
    full)
        # Use the scenarios defined in the script
        K6_OPTIONS=""
        echo -e "${CYAN}Running full test suite (47 minutes, all scenarios)...${NC}"
        ;;
    *)
        echo -e "${RED}Unknown mode: $MODE${NC}"
        exit 1
        ;;
esac

# Build k6 command
K6_CMD="k6 run"

# Add dashboard if requested
if [ "$OPEN_DASHBOARD" = true ]; then
    K6_CMD="$K6_CMD --out web-dashboard"
fi

# Add cloud if requested
if [ "$USE_CLOUD" = true ]; then
    if [ -z "$K6_CLOUD_TOKEN" ]; then
        echo -e "${YELLOW}⚠️  K6_CLOUD_TOKEN not set. Please export your k6 cloud token.${NC}"
        exit 1
    fi
    K6_CMD="$K6_CMD --out cloud"
fi

# Add output formats
K6_CMD="$K6_CMD --out json=$PROJECT_ROOT/test-reports/performance/results-$(date +%Y%m%d-%H%M%S).json"

# Add options and script
if [ -n "$K6_OPTIONS" ]; then
    K6_CMD="$K6_CMD $K6_OPTIONS"
fi

K6_CMD="$K6_CMD -e BASE_URL=$BASE_URL $PROJECT_ROOT/$K6_SCRIPT"

# Show performance targets
echo ""
echo -e "${BOLD}${CYAN}Performance Targets:${NC}"
echo -e "  • Throughput: ${GREEN}>1000 req/s${NC}"
echo -e "  • p95 Latency: ${GREEN}<200ms${NC}"
echo -e "  • p99 Latency: ${GREEN}<500ms${NC}"
echo -e "  • Error Rate: ${GREEN}<1%${NC}"
echo ""

# Start time
START_TIME=$(date +%s)

# Run k6
echo -e "${BOLD}${CYAN}Starting performance test...${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

if eval $K6_CMD; then
    TEST_RESULT="PASSED"
    RESULT_COLOR=$GREEN
    RESULT_ICON="✅"
else
    TEST_RESULT="FAILED"
    RESULT_COLOR=$RED
    RESULT_ICON="❌"
fi

# End time
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Generate report if requested
if [ "$GENERATE_REPORT" = true ]; then
    echo -e "${CYAN}Generating HTML report...${NC}"

    # Convert JSON to HTML (simplified - in production use k6-reporter)
    REPORT_FILE="$PROJECT_ROOT/test-reports/performance/report-$(date +%Y%m%d-%H%M%S).html"

    cat > "$REPORT_FILE" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>JiVS Performance Test Report - $MODE</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 40px; background: #f5f5f5; }
        .container { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        h1 { color: #333; border-bottom: 3px solid #007bff; padding-bottom: 10px; }
        .status-passed { color: #28a745; font-weight: bold; }
        .status-failed { color: #dc3545; font-weight: bold; }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }
        .metric { background: #f8f9fa; padding: 15px; border-radius: 5px; border-left: 4px solid #007bff; }
        .metric-label { color: #666; font-size: 12px; text-transform: uppercase; }
        .metric-value { font-size: 24px; font-weight: bold; color: #333; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th { background: #007bff; color: white; padding: 10px; text-align: left; }
        td { padding: 10px; border-bottom: 1px solid #ddd; }
        tr:hover { background: #f8f9fa; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🚀 JiVS Performance Test Report</h1>

        <p><strong>Test Mode:</strong> $MODE</p>
        <p><strong>Target URL:</strong> $BASE_URL</p>
        <p><strong>Duration:</strong> ${DURATION}s</p>
        <p><strong>Status:</strong> <span class="status-${TEST_RESULT,,}">${RESULT_ICON} ${TEST_RESULT}</span></p>

        <div class="metrics">
            <div class="metric">
                <div class="metric-label">Target Throughput</div>
                <div class="metric-value">>1000 req/s</div>
            </div>
            <div class="metric">
                <div class="metric-label">Target p95</div>
                <div class="metric-value"><200ms</div>
            </div>
            <div class="metric">
                <div class="metric-label">Target p99</div>
                <div class="metric-value"><500ms</div>
            </div>
            <div class="metric">
                <div class="metric-label">Target Error Rate</div>
                <div class="metric-value"><1%</div>
            </div>
        </div>

        <h2>Test Configuration</h2>
        <table>
            <tr><th>Parameter</th><th>Value</th></tr>
            <tr><td>Test Mode</td><td>$MODE</td></tr>
            <tr><td>Virtual Users</td><td>$(echo "$K6_OPTIONS" | grep -oP '\-\-vus \K\d+' || echo "Variable")</td></tr>
            <tr><td>Duration</td><td>$(echo "$K6_OPTIONS" | grep -oP '\-\-duration \K\S+' || echo "Variable")</td></tr>
            <tr><td>Target URL</td><td>$BASE_URL</td></tr>
        </table>

        <h2>Results Summary</h2>
        <p>Detailed results available in: <code>test-reports/performance/results-*.json</code></p>

        <hr>
        <p><em>Generated: $(date)</em></p>
    </div>
</body>
</html>
EOF

    echo -e "${GREEN}✅ Report generated: $REPORT_FILE${NC}"

    # Open report if on macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        open "$REPORT_FILE"
    fi
fi

# Summary
echo -e "${BOLD}${PURPLE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}${PURPLE}                         PERFORMANCE TEST COMPLETE                              ${NC}"
echo -e "${BOLD}${PURPLE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BOLD}Mode:${NC}     $MODE"
echo -e "${BOLD}Duration:${NC} ${DURATION}s"
echo -e "${BOLD}Status:${NC}   ${RESULT_COLOR}${RESULT_ICON} ${TEST_RESULT}${NC}"
echo ""

if [ "$TEST_RESULT" = "FAILED" ]; then
    echo -e "${YELLOW}⚠️  Some performance thresholds were not met.${NC}"
    echo -e "${YELLOW}Review the detailed results to identify bottlenecks.${NC}"
else
    echo -e "${GREEN}🎉 All performance targets achieved!${NC}"
fi

echo ""
echo -e "${CYAN}Results saved to: test-reports/performance/${NC}"

# Clean up
if [ -n "$BACKEND_PID" ]; then
    echo -e "${CYAN}Stopping backend...${NC}"
    kill $BACKEND_PID 2>/dev/null || true
fi

# Exit with appropriate code
[ "$TEST_RESULT" = "PASSED" ] && exit 0 || exit 1