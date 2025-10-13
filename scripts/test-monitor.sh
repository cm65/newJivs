#!/bin/bash

################################################################################
# JiVS Platform - Real-time Test Monitor
#
# Provides a live dashboard showing test execution status, coverage metrics,
# and system health during development.
#
# Features:
# - Real-time test status updates
# - Coverage metrics tracking
# - Performance monitoring
# - API health checks
# - Alert notifications
#
# Usage:
#   ./test-monitor.sh [options]
#
# Options:
#   --refresh <seconds>  Refresh interval (default: 5)
#   --sound             Enable sound alerts
#   --compact           Compact display mode
################################################################################

set -euo pipefail

# Configuration
REFRESH_INTERVAL=5
SOUND_ALERTS=false
COMPACT_MODE=false
MONITOR_PID=$$

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

# Paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
TEST_REPORTS="$PROJECT_ROOT/test-reports"

# Test metrics
LAST_CONTRACT_PASS=0
LAST_CONTRACT_FAIL=0
LAST_UNIT_PASS=0
LAST_UNIT_FAIL=0
LAST_E2E_PASS=0
LAST_E2E_FAIL=0
BACKEND_COVERAGE=0
FRONTEND_COVERAGE=0

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --refresh)
            REFRESH_INTERVAL="$2"
            shift 2
            ;;
        --sound)
            SOUND_ALERTS=true
            shift
            ;;
        --compact)
            COMPACT_MODE=true
            shift
            ;;
        *)
            shift
            ;;
    esac
done

# Helper functions
clear_screen() {
    printf '\033[2J\033[H'
}

move_cursor() {
    printf '\033[%s;%sH' "$1" "$2"
}

draw_box() {
    local width=$1
    local title=$2
    local color=$3

    echo -e "${color}┌─${BOLD} $title ${NC}${color}$( printf '─%.0s' $(seq 1 $((width - ${#title} - 4))) )┐${NC}"
}

draw_box_bottom() {
    local width=$1
    echo -e "${CYAN}└$( printf '─%.0s' $(seq 1 $((width - 2))) )┘${NC}"
}

play_sound() {
    if [ "$SOUND_ALERTS" = true ] && [[ "$OSTYPE" == "darwin"* ]]; then
        afplay /System/Library/Sounds/Glass.aiff 2>/dev/null &
    fi
}

format_duration() {
    local seconds=$1
    if [ $seconds -lt 60 ]; then
        echo "${seconds}s"
    elif [ $seconds -lt 3600 ]; then
        echo "$((seconds / 60))m $((seconds % 60))s"
    else
        echo "$((seconds / 3600))h $((seconds % 3600 / 60))m"
    fi
}

get_service_status() {
    local service=$1
    local port=$2

    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/actuator/health" 2>/dev/null | grep -q "200"; then
        echo -e "${GREEN}● Running${NC}"
    else
        echo -e "${RED}● Stopped${NC}"
    fi
}

get_test_stats() {
    # Check for recent test results
    if [ -d "$TEST_REPORTS" ]; then
        # Contract tests
        local contract_log=$(find "$TEST_REPORTS/contract" -name "*.log" -mmin -5 2>/dev/null | head -1)
        if [ -n "$contract_log" ]; then
            LAST_CONTRACT_PASS=$(grep -c "✅" "$contract_log" 2>/dev/null || echo 0)
            LAST_CONTRACT_FAIL=$(grep -c "❌" "$contract_log" 2>/dev/null || echo 0)
        fi

        # Unit tests
        local unit_log=$(find "$TEST_REPORTS/unit" -name "*.log" -mmin -5 2>/dev/null | head -1)
        if [ -n "$unit_log" ]; then
            LAST_UNIT_PASS=$(grep -c "Tests run.*Failures: 0" "$unit_log" 2>/dev/null || echo 0)
            LAST_UNIT_FAIL=$(grep -c "Tests run.*Failures: [1-9]" "$unit_log" 2>/dev/null || echo 0)
        fi
    fi
}

get_coverage_metrics() {
    # Backend coverage
    if [ -f "$BACKEND_DIR/target/site/jacoco/index.html" ]; then
        BACKEND_COVERAGE=$(grep -oP 'Total.*?([0-9]+)%' "$BACKEND_DIR/target/site/jacoco/index.html" | head -1 | grep -oP '[0-9]+' || echo 0)
    fi

    # Frontend coverage
    if [ -f "$FRONTEND_DIR/coverage/coverage-summary.json" ]; then
        FRONTEND_COVERAGE=$(grep -oP '"pct":[0-9.]+' "$FRONTEND_DIR/coverage/coverage-summary.json" | head -1 | cut -d: -f2 | cut -d. -f1 || echo 0)
    fi
}

draw_dashboard() {
    clear_screen

    # Header
    echo -e "${BOLD}${PURPLE}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BOLD}${PURPLE}║                    🧪 JiVS CONTINUOUS TESTING MONITOR 🧪                     ║${NC}"
    echo -e "${BOLD}${PURPLE}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    # Get current stats
    get_test_stats
    get_coverage_metrics

    # Service Status Row
    draw_box 78 "SERVICE STATUS" "$CYAN"
    echo -e "${CYAN}│${NC} Backend API:  $(get_service_status "backend" "8080")    ${CYAN}│${NC} Frontend:     $(get_service_status "frontend" "3001")    ${CYAN}│${NC} Database:     $(get_service_status "postgres" "5432")    ${CYAN}│${NC}"
    draw_box_bottom 78
    echo ""

    # Test Status Row
    draw_box 78 "TEST EXECUTION STATUS" "$CYAN"

    # Contract Tests
    echo -ne "${CYAN}│${NC} ${BOLD}Contract Tests:${NC}  "
    if [ $LAST_CONTRACT_PASS -gt 0 ]; then
        echo -ne "${GREEN}✅ $LAST_CONTRACT_PASS passed${NC}  "
    fi
    if [ $LAST_CONTRACT_FAIL -gt 0 ]; then
        echo -ne "${RED}❌ $LAST_CONTRACT_FAIL failed${NC}"
    fi
    if [ $LAST_CONTRACT_PASS -eq 0 ] && [ $LAST_CONTRACT_FAIL -eq 0 ]; then
        echo -ne "${DIM}Not run recently${NC}"
    fi
    echo -e "                              ${CYAN}│${NC}"

    # Unit Tests
    echo -ne "${CYAN}│${NC} ${BOLD}Unit Tests:${NC}      "
    if [ $LAST_UNIT_PASS -gt 0 ]; then
        echo -ne "${GREEN}✅ Passing${NC}  "
    elif [ $LAST_UNIT_FAIL -gt 0 ]; then
        echo -ne "${RED}❌ Failing${NC}  "
        play_sound
    else
        echo -ne "${DIM}Not run recently${NC}"
    fi
    echo -e "                                       ${CYAN}│${NC}"

    # E2E Tests
    echo -ne "${CYAN}│${NC} ${BOLD}E2E Tests:${NC}       "
    if [ -f "$FRONTEND_DIR/playwright-report/index.html" ]; then
        local e2e_time=$(stat -f "%m" "$FRONTEND_DIR/playwright-report/index.html" 2>/dev/null || stat -c "%Y" "$FRONTEND_DIR/playwright-report/index.html" 2>/dev/null)
        local current_time=$(date +%s)
        local age=$((current_time - e2e_time))
        if [ $age -lt 300 ]; then
            echo -ne "${GREEN}✅ Recent run${NC}"
        else
            echo -ne "${YELLOW}⚠️  Last run: $(format_duration $age) ago${NC}"
        fi
    else
        echo -ne "${DIM}Not run${NC}"
    fi
    echo -e "                                  ${CYAN}│${NC}"

    draw_box_bottom 78
    echo ""

    # Coverage Metrics Row
    draw_box 78 "COVERAGE METRICS" "$CYAN"

    # Backend Coverage
    echo -ne "${CYAN}│${NC} ${BOLD}Backend Coverage:${NC}  "
    if [ $BACKEND_COVERAGE -ge 80 ]; then
        echo -ne "${GREEN}${BACKEND_COVERAGE}%${NC}"
    elif [ $BACKEND_COVERAGE -ge 60 ]; then
        echo -ne "${YELLOW}${BACKEND_COVERAGE}%${NC}"
    else
        echo -ne "${RED}${BACKEND_COVERAGE}%${NC}"
    fi
    echo -ne " (target: 80%)                                           ${CYAN}│${NC}\n"

    # Frontend Coverage
    echo -ne "${CYAN}│${NC} ${BOLD}Frontend Coverage:${NC} "
    if [ $FRONTEND_COVERAGE -ge 75 ]; then
        echo -ne "${GREEN}${FRONTEND_COVERAGE}%${NC}"
    elif [ $FRONTEND_COVERAGE -ge 50 ]; then
        echo -ne "${YELLOW}${FRONTEND_COVERAGE}%${NC}"
    else
        echo -ne "${RED}${FRONTEND_COVERAGE}%${NC}"
    fi
    echo -ne " (target: 75%)                                           ${CYAN}│${NC}\n"

    draw_box_bottom 78
    echo ""

    # Active Processes Row
    draw_box 78 "ACTIVE TEST PROCESSES" "$CYAN"

    # Check for running test processes
    local test_processes=$(ps aux | grep -E "(mvn test|npm test|playwright|k6)" | grep -v grep | wc -l)
    if [ $test_processes -gt 0 ]; then
        echo -e "${CYAN}│${NC} ${YELLOW}⚡ $test_processes test process(es) running${NC}                                                   ${CYAN}│${NC}"
        ps aux | grep -E "(mvn test|npm test|playwright|k6)" | grep -v grep | head -3 | while read line; do
            local proc=$(echo "$line" | awk '{print $11}' | xargs basename)
            echo -e "${CYAN}│${NC}   ${DIM}→ $proc${NC}                                                              ${CYAN}│${NC}"
        done
    else
        echo -e "${CYAN}│${NC} ${DIM}No test processes running${NC}                                                  ${CYAN}│${NC}"
    fi

    draw_box_bottom 78
    echo ""

    # Recent File Changes
    draw_box 78 "RECENT FILE CHANGES" "$CYAN"

    # Get recently modified source files
    local recent_changes=$(find "$BACKEND_DIR/src" "$FRONTEND_DIR/src" -type f \( -name "*.java" -o -name "*.ts" -o -name "*.tsx" \) -mmin -5 2>/dev/null | head -3)
    if [ -n "$recent_changes" ]; then
        echo "$recent_changes" | while read file; do
            local basename=$(basename "$file")
            local age=$(( ($(date +%s) - $(stat -f "%m" "$file" 2>/dev/null || stat -c "%Y" "$file")) / 60 ))
            echo -e "${CYAN}│${NC}  ${YELLOW}●${NC} $basename ${DIM}(${age}m ago)${NC}                                                ${CYAN}│${NC}"
        done
    else
        echo -e "${CYAN}│${NC} ${DIM}No recent changes${NC}                                                          ${CYAN}│${NC}"
    fi

    draw_box_bottom 78
    echo ""

    # Alerts Row
    if [ $LAST_CONTRACT_FAIL -gt 0 ] || [ $LAST_UNIT_FAIL -gt 0 ] || [ $BACKEND_COVERAGE -lt 80 ] || [ $FRONTEND_COVERAGE -lt 75 ]; then
        draw_box 78 "⚠️  ALERTS" "$RED"

        if [ $LAST_CONTRACT_FAIL -gt 0 ]; then
            echo -e "${CYAN}│${NC} ${RED}● Contract tests failing - API contracts broken!${NC}                           ${CYAN}│${NC}"
        fi

        if [ $LAST_UNIT_FAIL -gt 0 ]; then
            echo -e "${CYAN}│${NC} ${RED}● Unit tests failing - Check recent changes${NC}                               ${CYAN}│${NC}"
        fi

        if [ $BACKEND_COVERAGE -lt 80 ]; then
            echo -e "${CYAN}│${NC} ${YELLOW}● Backend coverage below 80% threshold${NC}                                    ${CYAN}│${NC}"
        fi

        if [ $FRONTEND_COVERAGE -lt 75 ]; then
            echo -e "${CYAN}│${NC} ${YELLOW}● Frontend coverage below 75% threshold${NC}                                   ${CYAN}│${NC}"
        fi

        draw_box_bottom 78
        echo ""
    fi

    # Footer
    echo -e "${DIM}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${DIM}Refreshing every ${REFRESH_INTERVAL}s | Press Ctrl+C to exit | $(date +'%H:%M:%S')${NC}"
    echo -e "${DIM}Run './test-orchestrator.sh watch' for continuous testing${NC}"
}

# Trap to clean up on exit
trap "echo -e '\n${CYAN}Test monitor stopped.${NC}'; exit 0" INT TERM

# Main loop
echo -e "${CYAN}Starting JiVS Test Monitor...${NC}"
echo -e "${DIM}Refresh interval: ${REFRESH_INTERVAL}s | Sound alerts: $SOUND_ALERTS${NC}\n"

while true; do
    draw_dashboard
    sleep $REFRESH_INTERVAL
done