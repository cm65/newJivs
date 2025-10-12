#!/bin/bash
#
# Redis Backup Script for JiVS Platform
# Performs RDB snapshot and uploads to S3
# Usage: ./backup-redis.sh
#

set -euo pipefail

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/tmp/backups}"
S3_BUCKET="${S3_BUCKET:-jivs-backups}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

# Derived variables
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="redis-backup-${TIMESTAMP}.rdb"
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILE}"
LOG_FILE="/var/log/jivs-backup.log"

# Logging function
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."

    if ! command -v redis-cli &> /dev/null; then
        echo "ERROR: redis-cli not found"
        exit 1
    fi

    if ! command -v aws &> /dev/null; then
        echo "ERROR: AWS CLI not found"
        exit 1
    fi

    mkdir -p "$BACKUP_DIR"
    log "Prerequisites check passed"
}

# Test Redis connection
test_connection() {
    log "Testing Redis connection..."

    if [ -n "$REDIS_PASSWORD" ]; then
        AUTH_CMD="-a $REDIS_PASSWORD"
    else
        AUTH_CMD=""
    fi

    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" $AUTH_CMD PING | grep -q "PONG"; then
        log "Redis connection successful"
    else
        echo "ERROR: Failed to connect to Redis"
        exit 1
    fi
}

# Perform backup
perform_backup() {
    log "Starting Redis backup..."

    if [ -n "$REDIS_PASSWORD" ]; then
        AUTH_CMD="-a $REDIS_PASSWORD"
    else
        AUTH_CMD=""
    fi

    # Trigger BGSAVE
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" $AUTH_CMD BGSAVE

    # Wait for BGSAVE to complete
    log "Waiting for BGSAVE to complete..."
    while true; do
        LASTSAVE=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" $AUTH_CMD LASTSAVE)
        sleep 2
        CURRENT=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" $AUTH_CMD LASTSAVE)

        if [ "$CURRENT" != "$LASTSAVE" ]; then
            log "BGSAVE completed"
            break
        fi
    done

    # Copy RDB file
    log "Copying RDB file..."
    if [ "$REDIS_HOST" == "localhost" ]; then
        cp /var/lib/redis/dump.rdb "$BACKUP_PATH"
    else
        # For remote Redis, use redis-cli with --rdb option
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" $AUTH_CMD --rdb "$BACKUP_PATH"
    fi

    # Compress the backup
    gzip "$BACKUP_PATH"
    BACKUP_PATH="${BACKUP_PATH}.gz"

    BACKUP_SIZE=$(du -h "$BACKUP_PATH" | cut -f1)
    log "Backup completed successfully. Size: $BACKUP_SIZE"
}

# Upload to S3
upload_to_s3() {
    log "Uploading backup to S3..."

    S3_PATH="s3://${S3_BUCKET}/redis/$(basename $BACKUP_PATH)"

    aws s3 cp "$BACKUP_PATH" "$S3_PATH" \
        --storage-class STANDARD_IA \
        --server-side-encryption AES256

    log "Backup uploaded to: $S3_PATH"
}

# Cleanup old backups
cleanup_old_backups() {
    log "Cleaning up old backups..."

    find "$BACKUP_DIR" -name "redis-backup-*.rdb.gz" -type f -mtime +$RETENTION_DAYS -delete

    log "Old backups cleaned up"
}

# Main execution
main() {
    log "Starting Redis backup process..."

    check_prerequisites
    test_connection
    perform_backup
    upload_to_s3
    cleanup_old_backups

    log "Redis backup process completed successfully"
}

trap 'echo "ERROR: Backup failed"; exit 1' ERR

main "$@"
