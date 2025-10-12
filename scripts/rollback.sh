#!/bin/bash
#
# Emergency Rollback Script for JiVS Platform
# Quickly rollback to previous version
# Usage: ./rollback.sh [--environment prod|staging] [--revision <n>]
#

set -euo pipefail

ENVIRONMENT="staging"
REVISION=""
NAMESPACE=""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --environment|-e)
            ENVIRONMENT="$2"
            shift 2
            ;;
        --revision|-r)
            REVISION="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Set namespace
case $ENVIRONMENT in
    prod|production)
        NAMESPACE="jivs-platform"
        ;;
    staging|stg)
        NAMESPACE="jivs-staging"
        ;;
    *)
        echo -e "${RED}Invalid environment: $ENVIRONMENT${NC}"
        exit 1
        ;;
esac

echo "=========================================="
echo "JiVS Platform Emergency Rollback"
echo "=========================================="
echo ""
echo -e "${RED}WARNING: This will rollback the deployment!${NC}"
echo "Environment: $ENVIRONMENT"
echo "Namespace:   $NAMESPACE"
echo ""

read -p "Continue with rollback? (yes/no): " -r
if [[ ! $REPLY =~ ^yes$ ]]; then
    echo "Rollback cancelled."
    exit 0
fi

echo ""
echo "Rolling back..."

if [ -n "$REVISION" ]; then
    kubectl rollout undo deployment/jivs-backend -n $NAMESPACE --to-revision=$REVISION
    kubectl rollout undo deployment/jivs-frontend -n $NAMESPACE --to-revision=$REVISION
else
    kubectl rollout undo deployment/jivs-backend -n $NAMESPACE
    kubectl rollout undo deployment/jivs-frontend -n $NAMESPACE
fi

echo "Waiting for rollout to complete..."
kubectl rollout status deployment/jivs-backend -n $NAMESPACE --timeout=5m
kubectl rollout status deployment/jivs-frontend -n $NAMESPACE --timeout=5m

echo ""
echo -e "${GREEN}✓ Rollback completed successfully!${NC}"
echo ""
echo "Current versions:"
kubectl get deployment jivs-backend jivs-frontend -n $NAMESPACE \
    -o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[0].image
echo ""
