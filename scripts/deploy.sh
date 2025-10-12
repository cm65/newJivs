#!/bin/bash
#
# Automated Deployment Script for JiVS Platform
# Performs zero-downtime deployment with health checks and rollback capability
# Usage: ./deploy.sh [--environment prod|staging] [--version <tag>] [--dry-run]
#

set -euo pipefail

# Default values
ENVIRONMENT="staging"
VERSION="latest"
DRY_RUN=false
NAMESPACE=""
TIMEOUT="5m"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --environment|-e)
            ENVIRONMENT="$2"
            shift 2
            ;;
        --version|-v)
            VERSION="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -e, --environment ENV    Environment (prod|staging) [default: staging]"
            echo "  -v, --version VERSION    Version/tag to deploy [default: latest]"
            echo "  --dry-run                Show what would be deployed without actually deploying"
            echo "  -h, --help               Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Set namespace based on environment
case $ENVIRONMENT in
    prod|production)
        NAMESPACE="jivs-platform"
        ENVIRONMENT="production"
        ;;
    staging|stg)
        NAMESPACE="jivs-staging"
        ENVIRONMENT="staging"
        ;;
    *)
        echo -e "${RED}Invalid environment: $ENVIRONMENT${NC}"
        echo "Valid environments: prod, staging"
        exit 1
        ;;
esac

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

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        error "kubectl not found. Please install kubectl."
        exit 1
    fi

    if ! kubectl cluster-info &> /dev/null; then
        error "Cannot connect to Kubernetes cluster."
        exit 1
    fi

    if ! kubectl get namespace $NAMESPACE &> /dev/null; then
        error "Namespace $NAMESPACE does not exist."
        exit 1
    fi

    success "Prerequisites check passed"
}

# Backup current deployment
backup_deployment() {
    log "Backing up current deployment..."

    local backup_dir="./deployment-backups/$ENVIRONMENT"
    mkdir -p "$backup_dir"

    kubectl get deployment jivs-backend -n $NAMESPACE -o yaml > "$backup_dir/backend-$(date +%Y%m%d-%H%M%S).yaml"
    kubectl get deployment jivs-frontend -n $NAMESPACE -o yaml > "$backup_dir/frontend-$(date +%Y%m%d-%H%M%S).yaml"

    success "Backup created in $backup_dir"
}

# Get current image versions
get_current_versions() {
    log "Getting current versions..."

    local backend_image=$(kubectl get deployment jivs-backend -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].image}')
    local frontend_image=$(kubectl get deployment jivs-frontend -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].image}')

    echo ""
    echo "Current versions:"
    echo "  Backend:  $backend_image"
    echo "  Frontend: $frontend_image"
    echo ""
}

# Deploy new version
deploy() {
    log "Deploying version $VERSION to $ENVIRONMENT..."

    if [ "$DRY_RUN" = true ]; then
        warning "DRY RUN MODE - No actual changes will be made"
        echo ""
        echo "Would deploy:"
        echo "  Backend:  ghcr.io/jivs-platform/jivs-backend:$VERSION"
        echo "  Frontend: ghcr.io/jivs-platform/jivs-frontend:$VERSION"
        echo "  Namespace: $NAMESPACE"
        return 0
    fi

    # Deploy backend
    log "Deploying backend..."
    kubectl set image deployment/jivs-backend \
        backend=ghcr.io/jivs-platform/jivs-backend:$VERSION \
        -n $NAMESPACE \
        --record

    # Wait for backend rollout
    if kubectl rollout status deployment/jivs-backend -n $NAMESPACE --timeout=$TIMEOUT; then
        success "Backend deployed successfully"
    else
        error "Backend deployment failed!"
        return 1
    fi

    # Deploy frontend
    log "Deploying frontend..."
    kubectl set image deployment/jivs-frontend \
        frontend=ghcr.io/jivs-platform/jivs-frontend:$VERSION \
        -n $NAMESPACE \
        --record

    # Wait for frontend rollout
    if kubectl rollout status deployment/jivs-frontend -n $NAMESPACE --timeout=$TIMEOUT; then
        success "Frontend deployed successfully"
    else
        error "Frontend deployment failed!"
        return 1
    fi
}

# Run health checks
health_check() {
    log "Running health checks..."

    sleep 10  # Wait for services to stabilize

    # Check backend health
    local backend_url=$(kubectl get ingress jivs-ingress -n $NAMESPACE -o jsonpath='{.spec.rules[1].host}')
    if [ -z "$backend_url" ]; then
        backend_url="localhost:8080"
    fi

    log "Checking backend health at $backend_url..."
    local retries=0
    local max_retries=30

    while [ $retries -lt $max_retries ]; do
        if curl -sf "https://$backend_url/actuator/health" > /dev/null 2>&1; then
            success "Backend health check passed"
            break
        fi
        retries=$((retries+1))
        sleep 2
    done

    if [ $retries -eq $max_retries ]; then
        error "Backend health check failed after $max_retries attempts"
        return 1
    fi

    # Check frontend
    local frontend_url=$(kubectl get ingress jivs-ingress -n $NAMESPACE -o jsonpath='{.spec.rules[0].host}')
    if [ -z "$frontend_url" ]; then
        frontend_url="localhost:3000"
    fi

    log "Checking frontend at $frontend_url..."
    if curl -sf "https://$frontend_url" > /dev/null 2>&1; then
        success "Frontend health check passed"
    else
        warning "Frontend health check failed (might be expected for some configurations)"
    fi

    # Check pod status
    log "Checking pod status..."
    local failing_pods=$(kubectl get pods -n $NAMESPACE --field-selector=status.phase!=Running --no-headers 2>/dev/null | wc -l)

    if [ "$failing_pods" -gt 0 ]; then
        error "$failing_pods pods are not in Running state"
        kubectl get pods -n $NAMESPACE --field-selector=status.phase!=Running
        return 1
    fi

    success "All pods are running"
    return 0
}

# Rollback deployment
rollback() {
    error "Deployment validation failed. Rolling back..."

    kubectl rollout undo deployment/jivs-backend -n $NAMESPACE
    kubectl rollout undo deployment/jivs-frontend -n $NAMESPACE

    kubectl rollout status deployment/jivs-backend -n $NAMESPACE --timeout=$TIMEOUT
    kubectl rollout status deployment/jivs-frontend -n $NAMESPACE --timeout=$TIMEOUT

    success "Rollback completed"
}

# Send notification
send_notification() {
    local status=$1
    local message=$2

    if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
        curl -X POST "$SLACK_WEBHOOK_URL" \
            -H 'Content-Type: application/json' \
            -d "{
                \"text\": \"JiVS Deployment: $status\",
                \"attachments\": [{
                    \"color\": \"$([ \"$status\" == \"SUCCESS\" ] && echo \"good\" || echo \"danger\")\",
                    \"fields\": [
                        {\"title\": \"Environment\", \"value\": \"$ENVIRONMENT\", \"short\": true},
                        {\"title\": \"Version\", \"value\": \"$VERSION\", \"short\": true},
                        {\"title\": \"Status\", \"value\": \"$status\", \"short\": true},
                        {\"title\": \"Message\", \"value\": \"$message\", \"short\": false}
                    ]
                }]
            }" \
            2>/dev/null || true
    fi
}

# Main execution
main() {
    echo "=========================================="
    echo "JiVS Platform Deployment"
    echo "=========================================="
    echo ""
    echo "Environment: $ENVIRONMENT"
    echo "Namespace:   $NAMESPACE"
    echo "Version:     $VERSION"
    echo "Dry Run:     $DRY_RUN"
    echo ""

    # Confirmation for production
    if [ "$ENVIRONMENT" == "production" ] && [ "$DRY_RUN" = false ]; then
        warning "You are about to deploy to PRODUCTION!"
        read -p "Are you sure you want to continue? (yes/no): " -r
        echo
        if [[ ! $REPLY =~ ^yes$ ]]; then
            echo "Deployment cancelled."
            exit 0
        fi
    fi

    local start_time=$(date +%s)

    check_prerequisites
    get_current_versions
    backup_deployment

    if deploy; then
        if health_check; then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))

            success "Deployment completed successfully in ${duration}s!"
            send_notification "SUCCESS" "Deployment completed in ${duration}s"

            echo ""
            echo "=========================================="
            echo "Deployment Summary"
            echo "=========================================="
            echo "Environment: $ENVIRONMENT"
            echo "Version:     $VERSION"
            echo "Duration:    ${duration}s"
            echo "Status:      SUCCESS"
            echo "=========================================="
        else
            error "Health checks failed!"
            rollback
            send_notification "FAILED" "Health checks failed, rolled back"
            exit 1
        fi
    else
        error "Deployment failed!"
        rollback
        send_notification "FAILED" "Deployment failed, rolled back"
        exit 1
    fi
}

# Trap errors
trap 'error "Deployment failed with error on line $LINENO"; rollback; exit 1' ERR

main "$@"
