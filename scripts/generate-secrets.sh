#!/bin/bash
#
# Generate Secure Secrets for JiVS Platform
# This script generates cryptographically secure passwords and keys
# Usage: ./generate-secrets.sh [output-file]
#

set -euo pipefail

OUTPUT_FILE="${1:-.env.production}"
BACKUP_FILE="${OUTPUT_FILE}.backup.$(date +%Y%m%d-%H%M%S)"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "=========================================="
echo "JiVS Platform - Secrets Generator"
echo "=========================================="
echo ""

# Function to generate random password
generate_password() {
    local length=${1:-32}
    openssl rand -base64 48 | tr -d "=+/" | cut -c1-${length}
}

# Function to generate hex key
generate_hex_key() {
    local length=${1:-32}
    openssl rand -hex ${length}
}

# Function to generate base64 key
generate_base64_key() {
    local length=${1:-64}
    openssl rand -base64 ${length} | tr -d "\n"
}

# Check if output file exists
if [ -f "$OUTPUT_FILE" ]; then
    echo -e "${YELLOW}Warning: $OUTPUT_FILE already exists!${NC}"
    read -p "Do you want to backup and overwrite it? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        echo "Aborted."
        exit 0
    fi
    cp "$OUTPUT_FILE" "$BACKUP_FILE"
    echo -e "${GREEN}Backup created: $BACKUP_FILE${NC}"
fi

echo ""
echo "Generating secure secrets..."
echo ""

# Generate all secrets
DB_PASSWORD=$(generate_password 32)
REDIS_PASSWORD=$(generate_password 32)
ES_PASSWORD=$(generate_password 32)
RABBITMQ_PASSWORD=$(generate_password 32)
JWT_SECRET=$(generate_base64_key 64)
ENCRYPTION_KEY=$(generate_base64_key 32)
CAMUNDA_PASSWORD=$(generate_password 24)
SSL_KEYSTORE_PASSWORD=$(generate_password 24)
SMTP_PASSWORD=$(generate_password 32)

# Create the environment file
cat > "$OUTPUT_FILE" << EOF
# JiVS Platform - Production Environment Variables
# Generated on: $(date)
# IMPORTANT: Keep this file secure and never commit to version control!

# ============================================
# Application Configuration
# ============================================
SPRING_PROFILE=prod
SERVER_PORT=8080
MANAGEMENT_PORT=8081

# ============================================
# Database Configuration (PostgreSQL)
# ============================================
DATABASE_URL=jdbc:postgresql://your-db-host:5432/jivs
DATABASE_USERNAME=jivs_user
DATABASE_PASSWORD=${DB_PASSWORD}

# ============================================
# Redis Configuration
# ============================================
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}
REDIS_SSL=true

# ============================================
# Elasticsearch Configuration
# ============================================
ELASTICSEARCH_URIS=https://your-es-host:9200
ELASTICSEARCH_USERNAME=elastic
ELASTICSEARCH_PASSWORD=${ES_PASSWORD}

# ============================================
# RabbitMQ Configuration
# ============================================
RABBITMQ_HOST=your-rabbitmq-host
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=jivs_user
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}

# ============================================
# Security Configuration
# ============================================
JWT_SECRET=${JWT_SECRET}
ENCRYPTION_KEY=${ENCRYPTION_KEY}

# ============================================
# CORS Configuration
# ============================================
CORS_ALLOWED_ORIGINS=https://jivs.example.com,https://app.jivs.example.com

# ============================================
# Storage Configuration
# ============================================
STORAGE_PROVIDER=s3
AWS_S3_BUCKET=jivs-production-data
AWS_REGION=us-east-1
AWS_ACCESS_KEY=CHANGE_ME_TO_ACTUAL_AWS_KEY
AWS_SECRET_KEY=CHANGE_ME_TO_ACTUAL_AWS_SECRET

# ============================================
# External Services
# ============================================
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=noreply@example.com
SMTP_PASSWORD=${SMTP_PASSWORD}
SMTP_FROM=noreply@example.com

TWILIO_ACCOUNT_SID=CHANGE_ME
TWILIO_AUTH_TOKEN=CHANGE_ME
TWILIO_PHONE_NUMBER=CHANGE_ME

SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
SNS_TOPIC_ARN=arn:aws:sns:us-east-1:123456789:jivs-alerts

# ============================================
# Monitoring & Logging
# ============================================
NEW_RELIC_LICENSE_KEY=CHANGE_ME
NEW_RELIC_APP_NAME=jivs-platform

# ============================================
# Feature Flags
# ============================================
FEATURE_OCR_ENABLED=false
FEATURE_VIRUS_SCAN_ENABLED=true
FEATURE_GDPR_ENABLED=true
FEATURE_CCPA_ENABLED=true

# ============================================
# Performance Tuning
# ============================================
JVM_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# ============================================
# Backup Configuration
# ============================================
BACKUP_S3_BUCKET=jivs-backups
BACKUP_RETENTION_DAYS=30

# ============================================
# Camunda BPM
# ============================================
CAMUNDA_ADMIN_USER=admin
CAMUNDA_ADMIN_PASSWORD=${CAMUNDA_PASSWORD}

# ============================================
# SSL/TLS Configuration
# ============================================
SSL_ENABLED=true
SSL_KEY_STORE=/etc/ssl/certs/keystore.p12
SSL_KEY_STORE_PASSWORD=${SSL_KEYSTORE_PASSWORD}
SSL_KEY_STORE_TYPE=PKCS12
SSL_KEY_ALIAS=jivs-platform

# ============================================
# Timezone
# ============================================
TZ=UTC
EOF

# Set proper permissions
chmod 600 "$OUTPUT_FILE"

echo -e "${GREEN}✓ Secrets generated successfully!${NC}"
echo ""
echo "Generated secrets file: $OUTPUT_FILE"
echo ""
echo -e "${YELLOW}IMPORTANT NEXT STEPS:${NC}"
echo "1. Review and update the following values:"
echo "   - DATABASE_URL (actual database host)"
echo "   - REDIS_HOST (actual Redis host)"
echo "   - ELASTICSEARCH_URIS (actual Elasticsearch URL)"
echo "   - RABBITMQ_HOST (actual RabbitMQ host)"
echo "   - AWS_ACCESS_KEY and AWS_SECRET_KEY (actual AWS credentials)"
echo "   - SMTP_* values (actual email server)"
echo "   - External service credentials (Twilio, Slack, etc.)"
echo ""
echo "2. Store these secrets securely:"
echo "   - Use AWS Secrets Manager, Azure Key Vault, or HashiCorp Vault"
echo "   - Never commit this file to version control"
echo "   - Add $OUTPUT_FILE to .gitignore"
echo ""
echo "3. For Kubernetes deployment, create secrets:"
echo "   kubectl create secret generic jivs-backend-secrets \\"
echo "     --from-env-file=$OUTPUT_FILE \\"
echo "     --namespace=jivs-platform"
echo ""

# Create a summary file
SUMMARY_FILE="${OUTPUT_FILE}.summary"
cat > "$SUMMARY_FILE" << EOF
JiVS Platform - Secrets Summary
Generated on: $(date)

DATABASE_PASSWORD: ${DB_PASSWORD:0:8}...
REDIS_PASSWORD: ${REDIS_PASSWORD:0:8}...
ELASTICSEARCH_PASSWORD: ${ES_PASSWORD:0:8}...
RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD:0:8}...
JWT_SECRET: ${JWT_SECRET:0:12}... (${#JWT_SECRET} chars)
ENCRYPTION_KEY: ${ENCRYPTION_KEY:0:12}... (${#ENCRYPTION_KEY} chars)
CAMUNDA_PASSWORD: ${CAMUNDA_PASSWORD:0:8}...
SSL_KEYSTORE_PASSWORD: ${SSL_KEYSTORE_PASSWORD:0:8}...
SMTP_PASSWORD: ${SMTP_PASSWORD:0:8}...

IMPORTANT: Keep this summary file secure or delete it after recording the secrets.
EOF

chmod 600 "$SUMMARY_FILE"

echo -e "${GREEN}✓ Summary file created: $SUMMARY_FILE${NC}"
echo ""
echo "=========================================="
echo "Secret generation completed successfully!"
echo "=========================================="
