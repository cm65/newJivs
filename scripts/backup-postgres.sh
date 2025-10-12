#!/bin/bash
#
# PostgreSQL Backup Script for JiVS Platform
# Performs full database backup and uploads to S3
# Usage: ./backup-postgres.sh
#

set -euo pipefail

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/tmp/backups}"
S3_BUCKET="${S3_BUCKET:-jivs-backups}"
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-jivs}"
POSTGRES_DB="${POSTGRES_DB:-jivs}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

# Derived variables
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="postgres-backup-${TIMESTAMP}.sql.gz"
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILE}"
LOG_FILE="/var/log/jivs-backup.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}" | tee -a "$LOG_FILE"
}

success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}" | tee -a "$LOG_FILE"
}

warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}" | tee -a "$LOG_FILE"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."

    if ! command -v pg_dump &> /dev/null; then
        error "pg_dump not found. Please install PostgreSQL client."
        exit 1
    fi

    if ! command -v aws &> /dev/null; then
        error "AWS CLI not found. Please install AWS CLI."
        exit 1
    fi

    if [ ! -d "$BACKUP_DIR" ]; then
        log "Creating backup directory: $BACKUP_DIR"
        mkdir -p "$BACKUP_DIR"
    fi

    success "Prerequisites check passed"
}

# Test database connection
test_connection() {
    log "Testing database connection..."

    if PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" \
        -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT version();" > /dev/null 2>&1; then
        success "Database connection successful"
    else
        error "Failed to connect to database"
        exit 1
    fi
}

# Perform backup
perform_backup() {
    log "Starting PostgreSQL backup..."
    log "Database: $POSTGRES_DB@$POSTGRES_HOST:$POSTGRES_PORT"
    log "Backup file: $BACKUP_PATH"

    # Run pg_dump with compression
    if PGPASSWORD="$POSTGRES_PASSWORD" pg_dump \
        -h "$POSTGRES_HOST" \
        -p "$POSTGRES_PORT" \
        -U "$POSTGRES_USER" \
        -d "$POSTGRES_DB" \
        --format=plain \
        --no-owner \
        --no-acl \
        --verbose \
        2>> "$LOG_FILE" | gzip > "$BACKUP_PATH"; then

        BACKUP_SIZE=$(du -h "$BACKUP_PATH" | cut -f1)
        success "Backup completed successfully. Size: $BACKUP_SIZE"
    else
        error "Backup failed"
        exit 1
    fi
}

# Upload to S3
upload_to_s3() {
    log "Uploading backup to S3..."

    S3_PATH="s3://${S3_BUCKET}/postgres/${BACKUP_FILE}"

    if aws s3 cp "$BACKUP_PATH" "$S3_PATH" \
        --storage-class STANDARD_IA \
        --server-side-encryption AES256; then
        success "Backup uploaded to: $S3_PATH"
    else
        error "Failed to upload backup to S3"
        exit 1
    fi
}

# Verify backup integrity
verify_backup() {
    log "Verifying backup integrity..."

    if gzip -t "$BACKUP_PATH" 2>> "$LOG_FILE"; then
        success "Backup file integrity verified"
    else
        error "Backup file is corrupted"
        exit 1
    fi
}

# Clean up old backups
cleanup_old_backups() {
    log "Cleaning up old local backups (older than $RETENTION_DAYS days)..."

    find "$BACKUP_DIR" -name "postgres-backup-*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete

    success "Old local backups cleaned up"

    log "Cleaning up old S3 backups..."

    # S3 lifecycle policy should handle this, but we can also clean manually
    CUTOFF_DATE=$(date -d "$RETENTION_DAYS days ago" +%Y-%m-%d)

    aws s3 ls "s3://${S3_BUCKET}/postgres/" | while read -r line; do
        BACKUP_DATE=$(echo "$line" | awk '{print $1}')
        BACKUP_NAME=$(echo "$line" | awk '{print $4}')

        if [[ "$BACKUP_DATE" < "$CUTOFF_DATE" ]]; then
            log "Deleting old S3 backup: $BACKUP_NAME"
            aws s3 rm "s3://${S3_BUCKET}/postgres/$BACKUP_NAME"
        fi
    done

    success "Old S3 backups cleaned up"
}

# Send notification
send_notification() {
    local STATUS=$1
    local MESSAGE=$2

    log "Sending notification..."

    # Send to SNS (if configured)
    if [ -n "${SNS_TOPIC_ARN:-}" ]; then
        aws sns publish \
            --topic-arn "$SNS_TOPIC_ARN" \
            --subject "JiVS Backup: $STATUS" \
            --message "$MESSAGE" \
            2>> "$LOG_FILE" || warning "Failed to send SNS notification"
    fi

    # Send to Slack (if configured)
    if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
        COLOR=$([[ "$STATUS" == "SUCCESS" ]] && echo "good" || echo "danger")

        curl -X POST "$SLACK_WEBHOOK_URL" \
            -H 'Content-Type: application/json' \
            -d "{
                \"attachments\": [{
                    \"color\": \"$COLOR\",
                    \"title\": \"JiVS PostgreSQL Backup: $STATUS\",
                    \"text\": \"$MESSAGE\",
                    \"footer\": \"Backup System\",
                    \"ts\": $(date +%s)
                }]
            }" \
            2>> "$LOG_FILE" || warning "Failed to send Slack notification"
    fi
}

# Main execution
main() {
    log "========================================="
    log "Starting JiVS PostgreSQL Backup Process"
    log "========================================="

    START_TIME=$(date +%s)

    check_prerequisites
    test_connection
    perform_backup
    verify_backup
    upload_to_s3
    cleanup_old_backups

    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))

    success "Backup process completed in ${DURATION}s"

    # Send success notification
    send_notification "SUCCESS" "PostgreSQL backup completed successfully in ${DURATION}s. Backup: $BACKUP_FILE"

    log "========================================="
    log "Backup Process Finished"
    log "========================================="
}

# Trap errors
trap 'error "Backup failed with error on line $LINENO"; \
      send_notification "FAILED" "PostgreSQL backup failed on line $LINENO"; \
      exit 1' ERR

# Run main function
main "$@"
