#!/bin/bash
#
# JiVS Platform - Extraction Load Test Runner
#
# This script orchestrates all extraction performance tests:
# 1. Baseline test (current performance)
# 2. Performance test (validate optimizations)
# 3. Stress test (find breaking point)
# 4. Soak test (long-term stability) - optional
# 5. Spike test (sudden load changes)
#
# Usage:
#   ./run-extraction-tests.sh                    # Run all tests except soak
#   ./run-extraction-tests.sh --baseline         # Run only baseline
#   ./run-extraction-tests.sh --performance      # Run only performance
#   ./run-extraction-tests.sh --all              # Run all tests including soak
#   ./run-extraction-tests.sh --compare          # Compare baseline vs performance
#

set -euo pipefail

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
RESULTS_DIR="./load-test-results"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
REPORT_FILE="${RESULTS_DIR}/test-report-${TIMESTAMP}.md"

# Load environment if exists
if [ -f "./load-tests/.env.test" ]; then
    echo -e "${BLUE}Loading test environment...${NC}"
    set -a
    source ./load-tests/.env.test
    set +a
fi

# Create results directory
mkdir -p "$RESULTS_DIR"

# Function: Print header
print_header() {
    echo ""
    echo -e "${BLUE}=========================================="
    echo "$1"
    echo -e "==========================================${NC}"
    echo ""
}

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

# Function: Check if k6 is installed
check_k6() {
    if ! command -v k6 >/dev/null 2>&1; then
        print_error "k6 is not installed"
        echo ""
        echo "Install k6:"
        echo "  macOS:   brew install k6"
        echo "  Linux:   sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69"
        echo "           echo 'deb https://dl.k6.io/deb stable main' | sudo tee /etc/apt/sources.list.d/k6.list"
        echo "           sudo apt-get update && sudo apt-get install k6"
        echo "  Windows: choco install k6"
        echo ""
        echo "Or visit: https://k6.io/docs/getting-started/installation/"
        exit 1
    fi
}

# Function: Run a test
run_test() {
    local test_name=$1
    local test_file=$2
    local test_description=$3

    print_header "Running $test_name"
    echo "$test_description"
    echo ""

    local start_time=$(date +%s)

    if k6 run "$test_file"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        print_success "$test_name completed in ${duration}s"
        return 0
    else
        print_error "$test_name failed"
        return 1
    fi
}

# Function: Generate comparison report
generate_comparison() {
    print_header "Generating Comparison Report"

    if [ ! -f "$RESULTS_DIR/baseline-latest.json" ] || [ ! -f "$RESULTS_DIR/performance-latest.json" ]; then
        print_error "Cannot generate comparison: missing baseline or performance results"
        return 1
    fi

    # Extract metrics using jq or grep
    if command -v jq >/dev/null 2>&1; then
        BASELINE_THROUGHPUT=$(jq -r '.metrics.extraction.records_per_minute // 0' "$RESULTS_DIR/baseline-latest.json")
        PERF_THROUGHPUT=$(jq -r '.metrics.extraction.records_per_minute // 0' "$RESULTS_DIR/performance-latest.json")
        BASELINE_LATENCY=$(jq -r '.metrics.extraction.creation_latency_p95 // 0' "$RESULTS_DIR/baseline-latest.json")
        PERF_LATENCY=$(jq -r '.metrics.extraction.creation_latency_p95 // 0' "$RESULTS_DIR/performance-latest.json")
    else
        echo "  Note: jq not found, using grep for parsing"
        BASELINE_THROUGHPUT=$(grep -o '"records_per_minute"[^,]*' "$RESULTS_DIR/baseline-latest.json" | grep -o '[0-9.]*')
        PERF_THROUGHPUT=$(grep -o '"records_per_minute"[^,]*' "$RESULTS_DIR/performance-latest.json" | grep -o '[0-9.]*')
        BASELINE_LATENCY=$(grep -o '"creation_latency_p95"[^,]*' "$RESULTS_DIR/baseline-latest.json" | grep -o '[0-9.]*')
        PERF_LATENCY=$(grep -o '"creation_latency_p95"[^,]*' "$RESULTS_DIR/performance-latest.json" | grep -o '[0-9.]*')
    fi

    # Calculate improvements
    if [ "${BASELINE_THROUGHPUT:-0}" != "0" ]; then
        THROUGHPUT_IMPROVEMENT=$(echo "scale=2; (($PERF_THROUGHPUT - $BASELINE_THROUGHPUT) / $BASELINE_THROUGHPUT) * 100" | bc)
    else
        THROUGHPUT_IMPROVEMENT="N/A"
    fi

    if [ "${BASELINE_LATENCY:-0}" != "0" ]; then
        LATENCY_IMPROVEMENT=$(echo "scale=2; (($BASELINE_LATENCY - $PERF_LATENCY) / $BASELINE_LATENCY) * 100" | bc)
    else
        LATENCY_IMPROVEMENT="N/A"
    fi

    echo ""
    echo -e "${CYAN}PERFORMANCE COMPARISON${NC}"
    echo "======================================"
    echo ""
    echo "THROUGHPUT (records/min):"
    echo "  Baseline:    $BASELINE_THROUGHPUT"
    echo "  Optimized:   $PERF_THROUGHPUT"
    echo "  Improvement: ${THROUGHPUT_IMPROVEMENT}%"
    echo ""
    echo "LATENCY p95 (ms):"
    echo "  Baseline:    $BASELINE_LATENCY"
    echo "  Optimized:   $PERF_LATENCY"
    echo "  Improvement: ${LATENCY_IMPROVEMENT}%"
    echo ""

    # Check if targets met
    TARGET_THROUGHPUT=20000
    TARGET_LATENCY=200

    echo "TARGET ACHIEVEMENT:"
    if (( $(echo "$PERF_THROUGHPUT >= $TARGET_THROUGHPUT" | bc -l) )); then
        echo -e "  ${GREEN}✓${NC} Throughput target met (>= ${TARGET_THROUGHPUT} records/min)"
    else
        echo -e "  ${RED}✗${NC} Throughput target NOT met (< ${TARGET_THROUGHPUT} records/min)"
    fi

    if (( $(echo "$PERF_LATENCY <= $TARGET_LATENCY" | bc -l) )); then
        echo -e "  ${GREEN}✓${NC} Latency target met (<= ${TARGET_LATENCY}ms p95)"
    else
        echo -e "  ${RED}✗${NC} Latency target NOT met (> ${TARGET_LATENCY}ms p95)"
    fi

    echo ""
}

# Function: Generate markdown report
generate_report() {
    print_step "Generating test report..."

    cat > "$REPORT_FILE" <<EOF
# JiVS Platform - Extraction Performance Test Report

**Generated:** $(date)
**Test Run ID:** ${TIMESTAMP}

## Test Summary

This report contains the results of extraction performance load testing.

### Tests Executed

EOF

    if [ -f "$RESULTS_DIR/baseline-latest.json" ]; then
        echo "- ✓ Baseline Test" >> "$REPORT_FILE"
    fi

    if [ -f "$RESULTS_DIR/performance-latest.json" ]; then
        echo "- ✓ Performance Test" >> "$REPORT_FILE"
    fi

    if [ -f "$RESULTS_DIR/stress-latest.json" ]; then
        echo "- ✓ Stress Test" >> "$REPORT_FILE"
    fi

    if [ -f "$RESULTS_DIR/soak-latest.json" ]; then
        echo "- ✓ Soak Test" >> "$REPORT_FILE"
    fi

    if [ -f "$RESULTS_DIR/spike-latest.json" ]; then
        echo "- ✓ Spike Test" >> "$REPORT_FILE"
    fi

    cat >> "$REPORT_FILE" <<EOF

## Performance Targets

| Metric | Target | Status |
|--------|--------|--------|
| Throughput | >= 20,000 records/min | TBD |
| Latency (p95) | <= 200ms | TBD |
| Latency (p99) | <= 500ms | TBD |
| Error Rate | < 1% | TBD |

## Detailed Results

See JSON files in \`${RESULTS_DIR}\` for detailed metrics.

EOF

    print_success "Report generated: $REPORT_FILE"
}

# Main script
print_header "JiVS Platform - Extraction Load Test Suite"

# Check prerequisites
check_k6

# Parse arguments
RUN_BASELINE=false
RUN_PERFORMANCE=false
RUN_STRESS=false
RUN_SOAK=false
RUN_SPIKE=false
RUN_COMPARE=false
RUN_ALL=false

if [ $# -eq 0 ]; then
    # Default: run all except soak
    RUN_BASELINE=true
    RUN_PERFORMANCE=true
    RUN_STRESS=true
    RUN_SPIKE=true
else
    case "$1" in
        --baseline)
            RUN_BASELINE=true
            ;;
        --performance)
            RUN_PERFORMANCE=true
            ;;
        --stress)
            RUN_STRESS=true
            ;;
        --soak)
            RUN_SOAK=true
            ;;
        --spike)
            RUN_SPIKE=true
            ;;
        --compare)
            RUN_COMPARE=true
            ;;
        --all)
            RUN_ALL=true
            RUN_BASELINE=true
            RUN_PERFORMANCE=true
            RUN_STRESS=true
            RUN_SOAK=true
            RUN_SPIKE=true
            ;;
        *)
            echo "Usage: $0 [--baseline|--performance|--stress|--soak|--spike|--compare|--all]"
            exit 1
            ;;
    esac
fi

# Track test results
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Run baseline test
if [ "$RUN_BASELINE" = true ]; then
    if run_test "Baseline Test" \
        "./load-tests/extraction-baseline-test.js" \
        "Measuring current system performance before optimizations"; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        # Copy latest result
        LATEST_BASELINE=$(ls -t ${RESULTS_DIR}/baseline-*.json 2>/dev/null | head -n1)
        if [ -n "$LATEST_BASELINE" ]; then
            cp "$LATEST_BASELINE" "${RESULTS_DIR}/baseline-latest.json"
        fi
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
    TESTS_RUN=$((TESTS_RUN + 1))
fi

# Run performance test
if [ "$RUN_PERFORMANCE" = true ]; then
    if run_test "Performance Test" \
        "./load-tests/extraction-performance-test.js" \
        "Validating optimized system performance (100 concurrent users)"; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        # Copy latest result
        LATEST_PERF=$(ls -t ${RESULTS_DIR}/performance-*.json 2>/dev/null | head -n1)
        if [ -n "$LATEST_PERF" ]; then
            cp "$LATEST_PERF" "${RESULTS_DIR}/performance-latest.json"
        fi
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
    TESTS_RUN=$((TESTS_RUN + 1))
fi

# Run stress test
if [ "$RUN_STRESS" = true ]; then
    if run_test "Stress Test" \
        "./load-tests/extraction-stress-test.js" \
        "Finding system breaking point (gradually increasing to 500 VUs)"; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        # Copy latest result
        LATEST_STRESS=$(ls -t ${RESULTS_DIR}/stress-*.json 2>/dev/null | head -n1)
        if [ -n "$LATEST_STRESS" ]; then
            cp "$LATEST_STRESS" "${RESULTS_DIR}/stress-latest.json"
        fi
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
    TESTS_RUN=$((TESTS_RUN + 1))
fi

# Run soak test
if [ "$RUN_SOAK" = true ]; then
    echo -e "${YELLOW}WARNING: Soak test will run for ${SOAK_DURATION:-2h}${NC}"
    read -p "Continue? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if run_test "Soak Test" \
            "./load-tests/extraction-soak-test.js" \
            "Long-term stability test (${SOAK_DURATION:-2h} at ${SOAK_VUS:-50} VUs)"; then
            TESTS_PASSED=$((TESTS_PASSED + 1))
            # Copy latest result
            LATEST_SOAK=$(ls -t ${RESULTS_DIR}/soak-*.json 2>/dev/null | head -n1)
            if [ -n "$LATEST_SOAK" ]; then
                cp "$LATEST_SOAK" "${RESULTS_DIR}/soak-latest.json"
            fi
        else
            TESTS_FAILED=$((TESTS_FAILED + 1))
        fi
        TESTS_RUN=$((TESTS_RUN + 1))
    fi
fi

# Run spike test
if [ "$RUN_SPIKE" = true ]; then
    if run_test "Spike Test" \
        "./load-tests/extraction-spike-test.js" \
        "Testing system behavior under sudden load spikes"; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
        # Copy latest result
        LATEST_SPIKE=$(ls -t ${RESULTS_DIR}/spike-*.json 2>/dev/null | head -n1)
        if [ -n "$LATEST_SPIKE" ]; then
            cp "$LATEST_SPIKE" "${RESULTS_DIR}/spike-latest.json"
        fi
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
    TESTS_RUN=$((TESTS_RUN + 1))
fi

# Generate comparison if requested or if both baseline and performance ran
if [ "$RUN_COMPARE" = true ] || ([ "$RUN_BASELINE" = true ] && [ "$RUN_PERFORMANCE" = true ]); then
    generate_comparison
fi

# Generate report
generate_report

# Final summary
print_header "Test Suite Complete"

echo "Tests Run: $TESTS_RUN"
echo "Tests Passed: $TESTS_PASSED"
echo "Tests Failed: $TESTS_FAILED"
echo ""
echo "Results Directory: $RESULTS_DIR"
echo "Test Report: $REPORT_FILE"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    print_success "All tests completed successfully!"
    exit 0
else
    print_error "Some tests failed. Review the results above."
    exit 1
fi
