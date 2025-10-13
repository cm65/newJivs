# JiVS Platform - Disaster Recovery Plan

## Table of Contents
1. [Overview](#overview)
2. [Recovery Time Objectives](#recovery-time-objectives)
3. [Disaster Scenarios](#disaster-scenarios)
4. [Recovery Procedures](#recovery-procedures)
5. [Backup Strategy](#backup-strategy)
6. [Contact Information](#contact-information)
7. [Testing Schedule](#testing-schedule)

## Overview

This document outlines the disaster recovery (DR) procedures for the JiVS Platform. It provides step-by-step instructions for recovering from various disaster scenarios to minimize downtime and data loss.

**Last Updated**: January 2025
**Document Owner**: Platform Team
**Review Frequency**: Quarterly

## Recovery Time Objectives

| Component | RTO (Recovery Time Objective) | RPO (Recovery Point Objective) |
|-----------|-------------------------------|--------------------------------|
| Backend Application | 15 minutes | 1 hour |
| Frontend Application | 10 minutes | 1 hour |
| PostgreSQL Database | 30 minutes | 5 minutes |
| Redis Cache | 15 minutes | Acceptable data loss |
| Elasticsearch | 1 hour | 1 hour |
| RabbitMQ | 15 minutes | Acceptable message loss |

## Disaster Scenarios

### Scenario 1: Complete Data Center Failure

**Impact**: Total system outage
**Probability**: Low
**Recovery Strategy**: Failover to secondary region

### Scenario 2: Database Corruption

**Impact**: Data unavailability
**Probability**: Medium
**Recovery Strategy**: Restore from backup

### Scenario 3: Kubernetes Cluster Failure

**Impact**: Application unavailability
**Probability**: Low
**Recovery Strategy**: Recreate cluster and redeploy

### Scenario 4: Accidental Data Deletion

**Impact**: Data loss
**Probability**: Medium
**Recovery Strategy**: Point-in-time restore

### Scenario 5: Ransomware Attack

**Impact**: Data encryption/corruption
**Probability**: Low
**Recovery Strategy**: Restore from offline backups

### Scenario 6: Network Partition

**Impact**: Partial system availability
**Probability**: Medium
**Recovery Strategy**: DNS failover, traffic rerouting

## Recovery Procedures

### Procedure 1: Complete System Recovery

**Use Case**: Catastrophic failure requiring full system rebuild

#### Step 1: Assess the Situation

```bash
# Check cluster status
kubectl cluster-info
kubectl get nodes
kubectl get pods --all-namespaces

# Check database connectivity
psql -h $DB_HOST -U jivs -d jivs -c "SELECT version();"

# Check backup status
aws s3 ls s3://jivs-backups/postgres/ --recursive | tail -10
```

#### Step 2: Restore Infrastructure

```bash
# Create Kubernetes namespace
kubectl apply -f kubernetes/namespace.yaml

# Create secrets
./scripts/generate-secrets.sh
kubectl create secret generic jivs-backend-secrets \
  --from-env-file=.env.production \
  --namespace=jivs-platform

# Deploy infrastructure components
kubectl apply -f kubernetes/postgres-statefulset.yaml
kubectl apply -f kubernetes/redis-statefulset.yaml
kubectl apply -f kubernetes/elasticsearch-statefulset.yaml
kubectl apply -f kubernetes/rabbitmq-statefulset.yaml

# Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=postgres --timeout=300s -n jivs-platform
kubectl wait --for=condition=ready pod -l app=redis --timeout=300s -n jivs-platform
```

#### Step 3: Restore Database

```bash
# Find latest backup
LATEST_BACKUP=$(aws s3 ls s3://jivs-backups/postgres/ | sort | tail -1 | awk '{print $4}')

# Download backup
aws s3 cp s3://jivs-backups/postgres/$LATEST_BACKUP /tmp/

# Restore database
gunzip /tmp/$LATEST_BACKUP
kubectl exec -i postgres-0 -n jivs-platform -- psql -U jivs jivs < /tmp/${LATEST_BACKUP%.gz}

# Verify restoration
kubectl exec postgres-0 -n jivs-platform -- psql -U jivs -d jivs -c "\dt"
kubectl exec postgres-0 -n jivs-platform -- psql -U jivs -d jivs -c "SELECT COUNT(*) FROM users;"
```

#### Step 4: Deploy Applications

```bash
# Deploy backend and frontend
kubectl apply -f kubernetes/configmap.yaml
kubectl apply -f kubernetes/backend-deployment.yaml
kubectl apply -f kubernetes/frontend-deployment.yaml
kubectl apply -f kubernetes/ingress.yaml

# Wait for deployment
kubectl wait --for=condition=available deployment/jivs-backend --timeout=300s -n jivs-platform
kubectl wait --for=condition=available deployment/jivs-frontend --timeout=300s -n jivs-platform
```

#### Step 5: Verify System Health

```bash
# Run health checks
./scripts/health-check.sh

# Check application logs
kubectl logs -f deployment/jivs-backend -n jivs-platform --tail=100

# Test critical endpoints
curl -f https://jivs.example.com/actuator/health
curl -f https://jivs.example.com/api/v1/auth/health
```

#### Step 6: Notify Stakeholders

```bash
# Send recovery notification
curl -X POST $SLACK_WEBHOOK_URL \
  -H 'Content-Type: application/json' \
  -d '{
    "text": "JiVS Platform Recovery Completed",
    "attachments": [{
      "color": "good",
      "fields": [
        {"title": "Status", "value": "System Recovered", "short": true},
        {"title": "Duration", "value": "45 minutes", "short": true}
      ]
    }]
  }'
```

### Procedure 2: Database Point-in-Time Recovery

**Use Case**: Recover database to specific point in time

#### Prerequisites
- WAL (Write-Ahead Logging) enabled
- Continuous archiving configured
- Base backup available

#### Steps

```bash
# 1. Stop all write operations
kubectl scale deployment jivs-backend --replicas=0 -n jivs-platform

# 2. Identify recovery target time
TARGET_TIME="2025-01-15 14:30:00 UTC"

# 3. Restore base backup
PGDATA=/var/lib/postgresql/data/pgdata
kubectl exec postgres-0 -n jivs-platform -- rm -rf $PGDATA/*

# Download and restore base backup
aws s3 cp s3://jivs-backups/postgres/base-backup-latest.tar.gz /tmp/
kubectl cp /tmp/base-backup-latest.tar.gz postgres-0:/tmp/ -n jivs-platform
kubectl exec postgres-0 -n jivs-platform -- tar -xzf /tmp/base-backup-latest.tar.gz -C $PGDATA

# 4. Create recovery configuration
cat > recovery.conf << EOF
restore_command = 'aws s3 cp s3://jivs-backups/postgres/wal/%f %p'
recovery_target_time = '$TARGET_TIME'
recovery_target_action = 'promote'
EOF

kubectl cp recovery.conf postgres-0:$PGDATA/ -n jivs-platform

# 5. Start PostgreSQL in recovery mode
kubectl exec postgres-0 -n jivs-platform -- pg_ctl start -D $PGDATA

# 6. Monitor recovery
kubectl logs -f postgres-0 -n jivs-platform | grep "recovery"

# 7. Verify recovery
kubectl exec postgres-0 -n jivs-platform -- psql -U jivs -d jivs -c "SELECT pg_is_in_recovery();"

# 8. Resume operations
kubectl scale deployment jivs-backend --replicas=3 -n jivs-platform
```

### Procedure 3: Regional Failover

**Use Case**: Primary region failure, switch to DR region

#### Prerequisites
- Multi-region setup configured
- Database replication active
- DNS failover configured

#### Steps

```bash
# 1. Verify DR region status
kubectl config use-context dr-cluster
kubectl get pods -n jivs-platform

# 2. Promote standby database to primary
kubectl exec postgres-0 -n jivs-platform -- \
  su - postgres -c "pg_ctl promote -D /var/lib/postgresql/data/pgdata"

# 3. Update DNS records
# Point jivs.example.com to DR region load balancer
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch file://failover-dns-change.json

# 4. Scale up applications in DR region
kubectl scale deployment jivs-backend --replicas=3 -n jivs-platform
kubectl scale deployment jivs-frontend --replicas=3 -n jivs-platform

# 5. Verify failover
curl -f https://jivs.example.com/actuator/health

# 6. Monitor metrics
kubectl top pods -n jivs-platform
```

### Procedure 4: Ransomware Recovery

**Use Case**: System compromised by ransomware

#### Immediate Actions

```bash
# 1. ISOLATE AFFECTED SYSTEMS IMMEDIATELY
kubectl cordon node-affected-1
kubectl cordon node-affected-2

# 2. Stop all pods on affected nodes
kubectl delete pods --all -n jivs-platform --grace-period=0 --force

# 3. Preserve evidence
kubectl logs deployment/jivs-backend -n jivs-platform --all-containers=true > incident-logs.txt
kubectl describe nodes > incident-nodes.txt

# 4. Change all credentials immediately
./scripts/generate-secrets.sh
kubectl delete secret jivs-backend-secrets -n jivs-platform
kubectl create secret generic jivs-backend-secrets --from-env-file=.env.production.new

# 5. Restore from offline backups (untouched by ransomware)
# Use backups stored in write-once storage or offline media
aws s3 cp s3://jivs-offline-backups/postgres-pre-incident.sql.gz /tmp/

# 6. Deploy to clean infrastructure
# Build new cluster in isolated network
# Deploy applications with new credentials
# Restore data from verified clean backups

# 7. Scan all systems for malware
trivy image --severity HIGH,CRITICAL jivs-backend:latest
```

## Backup Strategy

### Automated Backups

#### PostgreSQL
- **Frequency**: Every 4 hours
- **Retention**: 30 days
- **Type**: Full dump + WAL archiving
- **Location**: S3 with versioning enabled
- **Verification**: Daily restore test

```bash
# Backup schedule (Kubernetes CronJob)
# 0 */4 * * * - Every 4 hours
```

#### Redis
- **Frequency**: Daily
- **Retention**: 7 days
- **Type**: RDB snapshot
- **Location**: S3
- **Verification**: Weekly restore test

#### Application Data
- **Frequency**: Daily
- **Retention**: 90 days
- **Type**: Full file system backup
- **Location**: S3 Glacier
- **Verification**: Monthly restore test

### Backup Testing

```bash
#!/bin/bash
# Test backup restoration

# 1. Create test namespace
kubectl create namespace jivs-dr-test

# 2. Restore latest backup
LATEST_BACKUP=$(aws s3 ls s3://jivs-backups/postgres/ | sort | tail -1 | awk '{print $4}')
aws s3 cp s3://jivs-backups/postgres/$LATEST_BACKUP /tmp/

# 3. Deploy test database
kubectl apply -f kubernetes/postgres-test.yaml -n jivs-dr-test

# 4. Restore data
kubectl exec postgres-test-0 -n jivs-dr-test -- psql -U jivs jivs < /tmp/${LATEST_BACKUP%.gz}

# 5. Verify data integrity
kubectl exec postgres-test-0 -n jivs-dr-test -- psql -U jivs -d jivs -c "SELECT COUNT(*) FROM users;"

# 6. Cleanup
kubectl delete namespace jivs-dr-test

echo "Backup test completed successfully"
```

## Contact Information

### On-Call Rotation
- **Primary**: DevOps Team (on-call-devops@example.com)
- **Secondary**: Platform Team (platform-team@example.com)
- **Escalation**: CTO (cto@example.com)

### Emergency Contacts
| Name | Role | Phone | Email |
|------|------|-------|-------|
| John Doe | DevOps Lead | +1-555-0100 | john.doe@example.com |
| Jane Smith | Platform Architect | +1-555-0101 | jane.smith@example.com |
| Bob Johnson | DBA | +1-555-0102 | bob.johnson@example.com |

### External Vendors
| Vendor | Service | Support Contact |
|--------|---------|----------------|
| AWS | Infrastructure | +1-866-AWS-SUPPORT |
| Datadog | Monitoring | support@datadog.com |
| PagerDuty | Alerting | support@pagerduty.com |

## Testing Schedule

### Disaster Recovery Drills

#### Quarterly Tests
- **Q1**: Database restore test
- **Q2**: Regional failover test
- **Q3**: Complete system recovery test
- **Q4**: Ransomware response simulation

#### Monthly Tests
- Backup restoration verification
- Monitoring and alerting validation
- Documentation review

#### Continuous
- Automated backup integrity checks
- Health check monitoring
- Security scanning

### Test Checklist

- [ ] Backup restoration successful
- [ ] All services recovered
- [ ] Data integrity verified
- [ ] RTO/RPO objectives met
- [ ] Communication plan executed
- [ ] Lessons learned documented
- [ ] Runbook updated

## Post-Incident Review

After any disaster recovery event, conduct a post-incident review:

1. **Timeline Documentation**: Record exact timeline of events
2. **Root Cause Analysis**: Identify what caused the incident
3. **Impact Assessment**: Document business impact
4. **Response Evaluation**: Assess effectiveness of response
5. **Improvement Actions**: Identify areas for improvement
6. **Documentation Update**: Update runbooks and procedures
7. **Communication**: Share findings with stakeholders

### Template

```markdown
# Incident Review: [INCIDENT-ID]

**Date**: 2025-01-15
**Duration**: 45 minutes
**Impact**: Production outage

## Timeline
- 14:00 UTC: Incident detected
- 14:05 UTC: Incident response team activated
- 14:10 UTC: Root cause identified
- 14:30 UTC: Services restored
- 14:45 UTC: Incident resolved

## Root Cause
[Description]

## Resolution
[Steps taken]

## Lessons Learned
1. [Lesson 1]
2. [Lesson 2]

## Action Items
1. [ ] [Action 1] - Owner: [Name] - Due: [Date]
2. [ ] [Action 2] - Owner: [Name] - Due: [Date]
```

## Appendix

### Recovery Scripts Location
- Backup scripts: `/scripts/backup-*.sh`
- Deployment scripts: `/scripts/deploy.sh`
- Rollback scripts: `/scripts/rollback.sh`
- Health check scripts: `/scripts/health-check.sh`

### Important URLs
- Production: https://jivs.example.com
- Staging: https://staging.jivs.example.com
- Monitoring: https://grafana.jivs.example.com
- Status Page: https://status.jivs.example.com

### Compliance Requirements
- **GDPR**: 72-hour breach notification
- **SOC 2**: Incident response within 1 hour
- **HIPAA**: Immediate notification for PHI breaches
