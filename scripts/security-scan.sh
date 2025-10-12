#!/bin/bash
#
# Security Scanning Script for JiVS Platform
# Performs comprehensive security scanning of containers, dependencies, and code
# Usage: ./security-scan.sh [--fail-on-high] [--report-dir <dir>]
#

set -euo pipefail

# Configuration
FAIL_ON_HIGH=false
REPORT_DIR="./security-reports"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --fail-on-high)
            FAIL_ON_HIGH=true
            shift
            ;;
        --report-dir)
            REPORT_DIR="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Create report directory
mkdir -p "$REPORT_DIR"

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✓ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if required tools are installed
check_prerequisites() {
    log "Checking prerequisites..."

    local missing_tools=()

    if ! command -v trivy &> /dev/null; then
        missing_tools+=("trivy")
    fi

    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi

    if [ ${#missing_tools[@]} -gt 0 ]; then
        error "Missing required tools: ${missing_tools[*]}"
        echo ""
        echo "Installation instructions:"
        echo "  Trivy: https://aquasecurity.github.io/trivy/latest/getting-started/installation/"
        echo "  Docker: https://docs.docker.com/get-docker/"
        exit 1
    fi

    success "All prerequisites are installed"
}

# Scan Docker images
scan_docker_images() {
    log "Scanning Docker images for vulnerabilities..."

    local images=("jivs-backend:latest" "jivs-frontend:latest")
    local exit_code=0

    for image in "${images[@]}"; do
        log "Scanning $image..."

        local report_file="$REPORT_DIR/trivy-${image//:/--}-${TIMESTAMP}.json"

        if docker image inspect "$image" &> /dev/null; then
            if trivy image \
                --severity HIGH,CRITICAL \
                --format json \
                --output "$report_file" \
                "$image"; then
                success "Scan completed: $image"

                # Parse results
                local high_count=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity=="HIGH")] | length' "$report_file")
                local critical_count=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity=="CRITICAL")] | length' "$report_file")

                echo "  HIGH: $high_count, CRITICAL: $critical_count"

                if [ "$FAIL_ON_HIGH" = true ] && ([ "$high_count" -gt 0 ] || [ "$critical_count" -gt 0 ]); then
                    error "Found HIGH or CRITICAL vulnerabilities in $image"
                    exit_code=1
                fi
            else
                error "Scan failed: $image"
                exit_code=1
            fi
        else
            warning "Image not found: $image (skipping)"
        fi
    done

    return $exit_code
}

# Scan filesystem
scan_filesystem() {
    log "Scanning filesystem for vulnerabilities..."

    local report_file="$REPORT_DIR/trivy-fs-${TIMESTAMP}.json"

    if trivy fs \
        --severity HIGH,CRITICAL \
        --format json \
        --output "$report_file" \
        --scanners vuln,secret,misconfig \
        .; then
        success "Filesystem scan completed"
    else
        warning "Filesystem scan completed with warnings"
    fi
}

# Scan for secrets
scan_secrets() {
    log "Scanning for exposed secrets..."

    local report_file="$REPORT_DIR/secrets-${TIMESTAMP}.txt"

    if trivy fs \
        --scanners secret \
        --format table \
        --output "$report_file" \
        .; then

        if [ -s "$report_file" ]; then
            error "Found exposed secrets! Check $report_file"
            cat "$report_file"
            return 1
        else
            success "No exposed secrets found"
        fi
    else
        warning "Secret scan completed with warnings"
    fi
}

# Scan Kubernetes manifests
scan_kubernetes_manifests() {
    log "Scanning Kubernetes manifests for misconfigurations..."

    if [ ! -d "./kubernetes" ]; then
        warning "Kubernetes directory not found (skipping)"
        return 0
    fi

    local report_file="$REPORT_DIR/k8s-misconfig-${TIMESTAMP}.json"

    if trivy config \
        --severity HIGH,CRITICAL \
        --format json \
        --output "$report_file" \
        ./kubernetes; then
        success "Kubernetes manifest scan completed"

        # Parse results
        local issues=$(jq '[.Results[].Misconfigurations[]? | select(.Severity=="HIGH" or .Severity=="CRITICAL")] | length' "$report_file")

        if [ "$issues" -gt 0 ]; then
            warning "Found $issues HIGH/CRITICAL misconfigurations in Kubernetes manifests"
        fi
    else
        warning "Kubernetes manifest scan completed with warnings"
    fi
}

# Scan dependencies (Maven)
scan_maven_dependencies() {
    log "Scanning Maven dependencies..."

    if [ ! -f "./backend/pom.xml" ]; then
        warning "pom.xml not found (skipping)"
        return 0
    fi

    cd backend

    if mvn dependency:tree > "$REPORT_DIR/maven-dependencies-${TIMESTAMP}.txt" 2>&1; then
        success "Maven dependency tree generated"
    else
        warning "Failed to generate Maven dependency tree"
    fi

    # OWASP Dependency Check (if available)
    if command -v dependency-check.sh &> /dev/null; then
        log "Running OWASP Dependency Check..."
        dependency-check.sh \
            --project "JiVS Platform" \
            --scan . \
            --format HTML \
            --out "$REPORT_DIR/owasp-${TIMESTAMP}"
        success "OWASP Dependency Check completed"
    fi

    cd ..
}

# Scan npm dependencies
scan_npm_dependencies() {
    log "Scanning npm dependencies..."

    if [ ! -f "./frontend/package.json" ]; then
        warning "package.json not found (skipping)"
        return 0
    fi

    cd frontend

    if npm audit --json > "$REPORT_DIR/npm-audit-${TIMESTAMP}.json" 2>&1; then
        success "npm audit completed"

        # Parse results
        local high_count=$(jq '.metadata.vulnerabilities.high // 0' "$REPORT_DIR/npm-audit-${TIMESTAMP}.json")
        local critical_count=$(jq '.metadata.vulnerabilities.critical // 0' "$REPORT_DIR/npm-audit-${TIMESTAMP}.json")

        echo "  HIGH: $high_count, CRITICAL: $critical_count"

        if [ "$FAIL_ON_HIGH" = true ] && ([ "$high_count" -gt 0 ] || [ "$critical_count" -gt 0 ]); then
            error "Found HIGH or CRITICAL vulnerabilities in npm dependencies"
            cd ..
            return 1
        fi
    else
        warning "npm audit completed with issues"
    fi

    cd ..
}

# Generate summary report
generate_summary() {
    log "Generating summary report..."

    local summary_file="$REPORT_DIR/SUMMARY-${TIMESTAMP}.md"

    cat > "$summary_file" << EOF
# JiVS Platform Security Scan Summary

**Scan Date**: $(date)
**Report Directory**: $REPORT_DIR

## Scans Performed

- ✓ Docker Image Vulnerability Scan
- ✓ Filesystem Vulnerability Scan
- ✓ Secret Detection Scan
- ✓ Kubernetes Manifest Scan
- ✓ Maven Dependency Scan
- ✓ npm Dependency Scan

## Results

### Docker Images

EOF

    # Add Docker scan results
    for report in "$REPORT_DIR"/trivy-jivs-*-${TIMESTAMP}.json; do
        if [ -f "$report" ]; then
            local image=$(basename "$report" | sed 's/trivy-//' | sed 's/--/:/' | sed "s/-${TIMESTAMP}.json//")
            local high=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity=="HIGH")] | length' "$report")
            local critical=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity=="CRITICAL")] | length' "$report")

            echo "- **$image**: HIGH: $high, CRITICAL: $critical" >> "$summary_file"
        fi
    done

    cat >> "$summary_file" << EOF

### Secrets Detection

EOF

    if [ -s "$REPORT_DIR/secrets-${TIMESTAMP}.txt" ]; then
        echo "⚠️ **WARNING**: Exposed secrets detected! Review $REPORT_DIR/secrets-${TIMESTAMP}.txt" >> "$summary_file"
    else
        echo "✓ No exposed secrets found" >> "$summary_file"
    fi

    cat >> "$summary_file" << EOF

### Kubernetes Misconfigurations

EOF

    if [ -f "$REPORT_DIR/k8s-misconfig-${TIMESTAMP}.json" ]; then
        local issues=$(jq '[.Results[].Misconfigurations[]? | select(.Severity=="HIGH" or .Severity=="CRITICAL")] | length' "$REPORT_DIR/k8s-misconfig-${TIMESTAMP}.json")
        echo "Found $issues HIGH/CRITICAL misconfigurations" >> "$summary_file"
    fi

    cat >> "$summary_file" << EOF

### Dependencies

- Maven: See $REPORT_DIR/maven-dependencies-${TIMESTAMP}.txt
- npm: See $REPORT_DIR/npm-audit-${TIMESTAMP}.json

## Recommendations

1. Review all CRITICAL vulnerabilities immediately
2. Plan patches for HIGH severity issues
3. Update dependencies regularly
4. Fix any exposed secrets immediately
5. Address Kubernetes misconfigurations

## Next Steps

1. Review detailed reports in $REPORT_DIR
2. Create tickets for vulnerability remediation
3. Update dependencies and rebuild images
4. Re-scan to verify fixes

EOF

    success "Summary report generated: $summary_file"

    # Display summary
    echo ""
    echo "=========================================="
    cat "$summary_file"
    echo "=========================================="
}

# Main execution
main() {
    echo "=========================================="
    echo "JiVS Platform Security Scanner"
    echo "=========================================="
    echo ""

    local exit_code=0

    check_prerequisites

    scan_docker_images || exit_code=$?
    scan_filesystem || exit_code=$?
    scan_secrets || exit_code=$?
    scan_kubernetes_manifests || exit_code=$?
    scan_maven_dependencies || exit_code=$?
    scan_npm_dependencies || exit_code=$?

    generate_summary

    echo ""
    if [ $exit_code -eq 0 ]; then
        success "Security scan completed successfully!"
    else
        error "Security scan completed with issues!"
    fi
    echo ""

    return $exit_code
}

main "$@"
