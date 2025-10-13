# JiVS Platform - Operational Runbook

## Document Information
- **Version**: 1.0
- **Last Updated**: January 2025
- **Owner**: DevOps Team
- **On-Call Escalation**: See [On-Call Schedule](#on-call-schedule)

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Access & Credentials](#2-access--credentials)
3. [Daily Operations](#3-daily-operations)
4. [Incident Response](#4-incident-response)
5. [Common Issues & Solutions](#5-common-issues--solutions)
6. [Deployment Procedures](#6-deployment-procedures)
7. [Monitoring & Alerting](#7-monitoring--alerting)
8. [Maintenance Procedures](#8-maintenance-procedures)
9. [Emergency Contacts](#9-emergency-contacts)

---

## 1. System Overview

### 1.1 Architecture Summary

```
┌─────────────────┐
│   Load Balancer │
└────────┬────────┘
         │
    ┌────┴────┐
    │  Ingress │
    └────┬────┘
         │
    ┌────┴────────────────────────────────┐
    │    Kubernetes Cluster (3 nodes)     │
    │  ┌──────────┐  ┌──────────┐        │
    │  │ Backend  │  │ Frontend │        │
    │  │ (3 pods) │  │ (2 pods) │        │
    │  └─────┬────┘  └──────────┘        │
    │        │                             │
    │  ┌─────┴────────┬─────────┬────┐   │
    │  │              │         │    │   │
    └──┼──────────────┼─────────┼────┼───┘
       │              │         │    │
  ┌────┴───┐  ┌──────┴───┐  ┌──┴──┐ │
  │Postgres│  │Elasticsearch│ │Redis│ │
  │(Primary│  │  (3 nodes) │ │Cluster│
  │ + 2    │  └────────────┘ └─────┘ │
  │Replicas│                          │
  └────────┘                          │
                                 ┌────┴────┐
                                 │RabbitMQ │
                                 │Cluster  │
                                 └─────────┘
```

### 1.2 Key Metrics Dashboard

**Primary Dashboard**: `https://grafana.jivs.example.com/d/overview`

Key Metrics to Monitor:
- Request Rate: < 1000 req/s (normal)
- Error Rate: < 1% (healthy)
- Response Time (p99): < 2s (healthy)
- CPU Usage: < 70% (healthy)
- Memory Usage: < 80% (healthy)
- Database Connections: < 80% of pool (healthy)

---

## 2. Access & Credentials

### 2.1 System Access

#### Kubernetes Cluster
```bash
# Connect to cluster
kubectl config use-context jivs-production

# Verify connection
kubectl cluster-info

# Switch namespace
kubectl config set-context --current --namespace=jivs-platform
```

#### Database Access
```bash
# PostgreSQL Primary
kubectl exec -it postgres-0 -n jivs-platform -- psql -U jivs

# PostgreSQL Read Replica
kubectl exec -it postgres-1 -n jivs-platform -- psql -U jivs

# Connection string (for external tools)
postgresql://jivs:$DB_PASSWORD@postgres.jivs-platform.svc.cluster.local:5432/jivs
```

#### Redis Access
```bash
# Connect to Redis
kubectl exec -it redis-0 -n jivs-platform -- redis-cli

# View replication status
redis-cli info replication
```

#### Application Logs
```bash
# Stream backend logs
kubectl logs -f deployment/jivs-backend -n jivs-platform

# Search logs for errors
kubectl logs deployment/jivs-backend -n jivs-platform | grep ERROR

# View last 100 lines
kubectl logs --tail=100 deployment/jivs-backend -n jivs-platform
```

### 2.2 Monitoring & Alerting Access

- **Grafana**: `https://grafana.jivs.example.com`
  - Username: admin
  - Password: Stored in 1Password vault `JiVS-Production`

- **Prometheus**: `https://prometheus.jivs.example.com`
  - Internal access only (port-forward if needed)

- **Kibana**: `https://kibana.jivs.example.com`
  - SSO enabled

- **Jaeger**: `https://jaeger.jivs.example.com`
  - Distributed tracing UI

---

## 3. Daily Operations

### 3.1 Morning Health Check (30 minutes)

#### Step 1: Check System Status
```bash
#!/bin/bash
# daily-health-check.sh

echo "=== JiVS Platform Health Check ==="
echo "Date: $(date)"
echo ""

# 1. Check all pods are running
echo "1. Checking pod status..."
kubectl get pods -n jivs-platform

# Count pods not in Running state
NOT_RUNNING=$(kubectl get pods -n jivs-platform --field-selector=status.phase!=Running --no-headers | wc -l)
if [ $NOT_RUNNING -gt 0 ]; then
    echo "⚠️  WARNING: $NOT_RUNNING pods not running!"
else
    echo "✅ All pods running"
fi

# 2. Check service endpoints
echo ""
echo "2. Checking service endpoints..."
curl -f https://jivs.example.com/actuator/health || echo "❌ Backend unhealthy"
curl -f https://jivs.example.com/ || echo "❌ Frontend unhealthy"

# 3. Check database replication lag
echo ""
echo "3. Checking database replication..."
kubectl exec postgres-0 -n jivs-platform -- psql -U jivs -c "SELECT NOW() - pg_last_xact_replay_timestamp() AS replication_lag;"

# 4. Check Redis cluster status
echo ""
echo "4. Checking Redis cluster..."
kubectl exec redis-0 -n jivs-platform -- redis-cli cluster info

# 5. Check Elasticsearch cluster health
echo ""
echo "5. Checking Elasticsearch cluster..."
curl -s "http://elasticsearch.jivs-platform.svc.cluster.local:9200/_cluster/health?pretty"

# 6. Check recent errors
echo ""
echo "6. Checking for recent errors..."
ERROR_COUNT=$(kubectl logs --since=24h deployment/jivs-backend -n jivs-platform | grep -c ERROR)
echo "Error count (last 24h): $ERROR_COUNT"

# 7. Check disk space
echo ""
echo "7. Checking disk space..."
df -h /var/lib/postgresql/data
df -h /var/jivs/storage

# 8. Check certificate expiry
echo ""
echo "8. Checking SSL certificates..."
echo | openssl s_client -servername jivs.example.com -connect jivs.example.com:443 2>/dev/null | openssl x509 -noout -dates

echo ""
echo "=== Health Check Complete ==="
```

#### Step 2: Review Overnight Metrics
1. Open Grafana dashboard
2. Check for any anomalies in:
   - Request rate
   - Error rate
   - Response times
   - Resource usage
3. Review any alerts triggered overnight

#### Step 3: Check Failed Jobs
```sql
-- Connect to database
psql -U jivs -d jivs

-- Check failed extractions
SELECT id, name, status, error_message, updated_at
FROM extractions
WHERE status = 'FAILED'
  AND updated_at > NOW() - INTERVAL '24 hours'
ORDER BY updated_at DESC;

-- Check failed migrations
SELECT id, name, status, current_phase, updated_at
FROM migrations
WHERE status = 'FAILED'
  AND updated_at > NOW() - INTERVAL '24 hours'
ORDER BY updated_at DESC;

-- Check overdue GDPR requests
SELECT id, request_type, data_subject_email, due_date, status
FROM data_subject_requests
WHERE status != 'COMPLETED'
  AND due_date < NOW()
ORDER BY due_date ASC;
```

#### Step 4: Review Backups
```bash
# Check last backup timestamp
aws s3 ls s3://jivs-backups/$(date +%Y/%m/%d)/ | tail -5

# Verify backup size (should be > 100MB for database)
aws s3 ls --human-readable s3://jivs-backups/$(date +%Y/%m/%d)/db_backup_*
```

### 3.2 Weekly Maintenance Tasks

#### Monday: Capacity Planning Review
- Review resource utilization trends
- Check if auto-scaling triggered
- Plan for capacity increases if needed

#### Wednesday: Security Review
- Review access logs for suspicious activity
- Check for failed login attempts
- Verify SSL certificates are valid
- Review security scan results

#### Friday: Performance Optimization
- Review slow query log
- Identify and optimize N+1 queries
- Check cache hit rates
- Review and optimize indexes

---

## 4. Incident Response

### 4.1 Severity Levels

| Severity | Description | Response Time | Examples |
|----------|-------------|---------------|----------|
| **SEV1** | Critical - System down | 15 minutes | Complete outage, data loss |
| **SEV2** | High - Major functionality impaired | 1 hour | Authentication failure, database connectivity |
| **SEV3** | Medium - Partial functionality affected | 4 hours | Single feature broken, performance degradation |
| **SEV4** | Low - Minor issue | 24 hours | UI bug, logging issue |

### 4.2 Incident Response Procedure

#### Step 1: Acknowledge & Assess (5 minutes)
```bash
# 1. Acknowledge the alert
# In PagerDuty/Opsgenie, click "Acknowledge"

# 2. Check system status dashboard
open https://grafana.jivs.example.com/d/incident-overview

# 3. Identify affected components
kubectl get pods -n jivs-platform
kubectl get events -n jivs-platform --sort-by='.lastTimestamp' | tail -20

# 4. Check error rates
# In Grafana, review Error Rate panel

# 5. Determine severity
# Use severity matrix above
```

#### Step 2: Communicate (2 minutes)
```
# Post in #incidents Slack channel:
🚨 INCIDENT DETECTED
Severity: [SEV1/SEV2/SEV3/SEV4]
Impact: [Brief description]
Status: INVESTIGATING
Incident Commander: [Your name]
Started: [Timestamp]

# For SEV1/SEV2, also:
- Page on-call manager
- Update status page: https://status.jivs.example.com
```

#### Step 3: Investigate & Mitigate (varies)
See [Common Issues & Solutions](#5-common-issues--solutions) section

#### Step 4: Resolve & Document (15 minutes)
```
# Post resolution in #incidents:
✅ INCIDENT RESOLVED
Duration: [Time]
Root Cause: [Brief description]
Actions Taken: [List]
Follow-up Required: [Yes/No]

# Create post-mortem document (for SEV1/SEV2)
# Template: https://docs.jivs.internal/incident-postmortem-template
```

### 4.3 Incident War Room

For SEV1/SEV2 incidents, establish war room:

```bash
# Create Zoom meeting
zoom://meeting/incident-$(date +%Y%m%d-%H%M)

# Invite key personnel:
- Incident Commander (whoever acknowledged)
- On-call Engineer
- Engineering Manager
- Product Manager (if user-facing)
- Support Lead (if customer impact)
```

### 4.4 Rollback Procedure

```bash
# For deployment rollbacks

# 1. Identify last good version
kubectl rollout history deployment/jivs-backend -n jivs-platform

# 2. Rollback to previous version
kubectl rollout undo deployment/jivs-backend -n jivs-platform

# 3. Monitor rollback progress
kubectl rollout status deployment/jivs-backend -n jivs-platform

# 4. Verify health
curl https://jivs.example.com/actuator/health

# 5. Check logs for errors
kubectl logs deployment/jivs-backend -n jivs-platform | grep ERROR
```

---

## 5. Common Issues & Solutions

### 5.1 High Error Rate

**Symptoms:**
- Error rate > 5%
- Increased 5xx responses
- User reports of failures

**Investigation:**
```bash
# 1. Check application logs
kubectl logs deployment/jivs-backend -n jivs-platform | grep ERROR | tail -50

# 2. Check for database issues
kubectl exec postgres-0 -n jivs-platform -- psql -U jivs -c "SELECT state, COUNT(*) FROM pg_stat_activity GROUP BY state;"

# 3. Check connection pool
# In Grafana: hikaricp_connections_active / hikaricp_connections_max

# 4. Check external service status
curl -I https://external-service.example.com/health
```

**Solutions:**
```bash
# If database connection pool exhausted:
kubectl scale deployment jivs-backend --replicas=5 -n jivs-platform

# If external service down:
# Enable circuit breaker via feature flag
curl -X POST https://jivs.example.com/api/v1/admin/circuit-breaker/enable

# If application error:
# Check logs, fix code, deploy hotfix
```

---

### 5.2 Database Connection Issues

**Symptoms:**
- "Connection refused" errors
- "Too many connections" errors
- Slow query responses

**Investigation:**
```sql
-- Check active connections
SELECT count(*) as connections,
       state,
       usename
FROM pg_stat_activity
GROUP BY state, usename;

-- Check long-running queries
SELECT pid,
       now() - pg_stat_activity.query_start AS duration,
       query
FROM pg_stat_activity
WHERE state = 'active'
  AND now() - pg_stat_activity.query_start > interval '5 minutes'
ORDER BY duration DESC;

-- Check replication lag
SELECT NOW() - pg_last_xact_replay_timestamp() AS replication_lag;
```

**Solutions:**
```bash
# Kill long-running queries
psql -U jivs -d jivs -c "SELECT pg_terminate_backend(PID);"

# Restart application pods to release connections
kubectl rollout restart deployment/jivs-backend -n jivs-platform

# Increase connection pool size (if consistently hitting limit)
kubectl set env deployment/jivs-backend -n jivs-platform \
  HIKARI_MAX_POOL_SIZE=50

# Promote replica if primary is down
kubectl exec postgres-0 -n jivs-platform -- \
  pg_ctl promote -D /var/lib/postgresql/data
```

---

### 5.3 High Memory Usage

**Symptoms:**
- Memory usage > 90%
- OOM (Out of Memory) kills
- Slow application performance

**Investigation:**
```bash
# 1. Check pod memory usage
kubectl top pods -n jivs-platform

# 2. Get heap dump (if needed)
kubectl exec jivs-backend-xxx -n jivs-platform -- \
  jmap -dump:format=b,file=/tmp/heap.hprof 1

# Copy heap dump for analysis
kubectl cp jivs-platform/jivs-backend-xxx:/tmp/heap.hprof ./heap.hprof

# 3. Check for memory leaks in logs
kubectl logs deployment/jivs-backend -n jivs-platform | grep "OutOfMemoryError"
```

**Solutions:**
```bash
# Immediate: Restart affected pods
kubectl delete pod jivs-backend-xxx -n jivs-platform

# Short-term: Increase memory limits
kubectl set resources deployment jivs-backend -n jivs-platform \
  --limits=memory=8Gi \
  --requests=memory=4Gi

# Long-term: Optimize code based on heap dump analysis
```

---

### 5.4 Slow API Responses

**Symptoms:**
- p99 response time > 5s
- User complaints about slowness
- Timeout errors

**Investigation:**
```bash
# 1. Check distributed traces
# Open Jaeger: https://jaeger.jivs.example.com
# Search for slow traces (duration > 5s)

# 2. Check slow query log
tail -f /var/log/jivs/slow-queries.log

# 3. Check database performance
psql -U jivs -d jivs -c "SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10;"

# 4. Check Redis hit rate
redis-cli info stats | grep keyspace_hits
redis-cli info stats | grep keyspace_misses
```

**Solutions:**
```sql
-- Add missing indexes
CREATE INDEX CONCURRENTLY idx_extraction_status_created
ON extractions(status, created_at DESC);

-- Optimize slow queries
EXPLAIN ANALYZE SELECT * FROM extractions WHERE status = 'RUNNING';

-- Update statistics
ANALYZE extractions;
VACUUM ANALYZE extractions;
```

```bash
# Increase cache TTL if low hit rate
kubectl set env deployment/jivs-backend -n jivs-platform \
  CACHE_TTL=7200

# Scale up if CPU-bound
kubectl scale deployment jivs-backend --replicas=5 -n jivs-platform
```

---

### 5.5 Failed Data Migration

**Symptoms:**
- Migration stuck in RUNNING state
- Migration status shows FAILED
- Data inconsistency between source and target

**Investigation:**
```sql
-- Check migration status
SELECT id, name, status, current_phase, progress,
       error_message, updated_at
FROM migrations
WHERE id = 'MIGRATION_ID';

-- Check migration phases
SELECT phase_name, status, start_time, end_time, error_message
FROM migration_phases
WHERE migration_id = 'MIGRATION_ID'
ORDER BY phase_order;

-- Check extraction statistics
SELECT records_extracted, records_migrated, total_records
FROM migrations
WHERE id = 'MIGRATION_ID';
```

**Solutions:**
```bash
# Resume paused migration
curl -X POST https://jivs.example.com/api/v1/migrations/MIGRATION_ID/resume

# Rollback failed migration
curl -X POST https://jivs.example.com/api/v1/migrations/MIGRATION_ID/rollback

# Manual cleanup if rollback fails
psql -U jivs -d jivs <<EOF
BEGIN;
DELETE FROM target_table WHERE migration_id = 'MIGRATION_ID';
UPDATE migrations SET status = 'FAILED', error_message = 'Manual cleanup required' WHERE id = 'MIGRATION_ID';
COMMIT;
EOF
```

---

### 5.6 GDPR Request Overdue

**Symptoms:**
- Alert: "GDPR requests overdue"
- Request due date passed
- Compliance violation risk

**Investigation:**
```sql
-- Check overdue requests
SELECT id, request_type, data_subject_email,
       requested_at, due_date, status,
       EXTRACT(DAY FROM (NOW() - due_date)) as days_overdue
FROM data_subject_requests
WHERE status != 'COMPLETED'
  AND due_date < NOW()
ORDER BY due_date ASC;
```

**Solutions:**
```bash
# Process request immediately
curl -X POST https://jivs.example.com/api/v1/compliance/requests/REQUEST_ID/process

# If processing fails, escalate
# 1. Create high-priority ticket in JIRA
# 2. Notify compliance officer
# 3. Prepare manual response
```

---

## 6. Deployment Procedures

### 6.1 Standard Deployment

**Prerequisites:**
- Code merged to main branch
- CI/CD pipeline passed
- Staging deployment successful
- Change approved in change management system

**Procedure:**
```bash
# 1. Create deployment tag
git tag -a v1.2.3 -m "Release v1.2.3"
git push origin v1.2.3

# 2. Build and push Docker images
docker build -t jivs/backend:v1.2.3 ./backend
docker push jivs/backend:v1.2.3

# 3. Update Kubernetes manifests
kubectl set image deployment/jivs-backend \
  backend=jivs/backend:v1.2.3 \
  -n jivs-platform

# 4. Monitor rollout
kubectl rollout status deployment/jivs-backend -n jivs-platform

# 5. Verify deployment
curl https://jivs.example.com/actuator/health
kubectl logs deployment/jivs-backend -n jivs-platform | grep ERROR

# 6. Run smoke tests
./scripts/smoke-tests.sh

# 7. Monitor metrics for 30 minutes
# Watch Grafana dashboard for anomalies
```

### 6.2 Emergency Hotfix

```bash
# 1. Create hotfix branch
git checkout -b hotfix/critical-bug main

# 2. Make fix and commit
git commit -m "Fix critical bug"

# 3. Fast-track review (single approver)
gh pr create --title "HOTFIX: Critical bug" --base main

# 4. Deploy to staging
kubectl set image deployment/jivs-backend \
  backend=jivs/backend:hotfix-$(git rev-parse --short HEAD) \
  -n jivs-staging

# 5. Quick verification
./scripts/smoke-tests.sh staging

# 6. Deploy to production
kubectl set image deployment/jivs-backend \
  backend=jivs/backend:hotfix-$(git rev-parse --short HEAD) \
  -n jivs-platform

# 7. Monitor closely
# Watch Grafana for 1 hour

# 8. Merge to main
git checkout main
git merge hotfix/critical-bug
```

### 6.3 Database Migration

```bash
# 1. Backup database
pg_dump -h postgres -U jivs -F c -f backup-pre-migration.dump jivs

# 2. Upload backup to S3
aws s3 cp backup-pre-migration.dump s3://jivs-backups/migrations/

# 3. Review migration script
cat backend/src/main/resources/db/migration/V123__add_new_table.sql

# 4. Test in staging
psql -h staging-postgres -U jivs -d jivs -f V123__add_new_table.sql

# 5. Deploy to production (Flyway runs automatically)
kubectl rollout restart deployment/jivs-backend -n jivs-platform

# 6. Verify migration
psql -h postgres -U jivs -d jivs -c "SELECT * FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 5;"

# 7. Test application functionality
./scripts/integration-tests.sh
```

---

## 7. Monitoring & Alerting

### 7.1 Key Alerts

| Alert Name | Condition | Severity | Action |
|------------|-----------|----------|--------|
| Service Down | `up == 0` | SEV1 | Restart service immediately |
| High Error Rate | `error_rate > 5%` | SEV2 | Check logs, investigate |
| High Response Time | `p99 > 5s` | SEV2 | Check slow queries |
| Database Unavailable | `pg_up == 0` | SEV1 | Failover to replica |
| Disk Space Low | `disk_free < 10%` | SEV2 | Clean up old files |
| Memory High | `memory_usage > 90%` | SEV2 | Restart pods |
| GDPR Overdue | `overdue_requests > 0` | SEV2 | Process requests |

### 7.2 Alert Response Playbook

**Service Down Alert**
```bash
# 1. Check pod status
kubectl get pods -n jivs-platform

# 2. Check pod logs
kubectl logs -l app=jivs-backend -n jivs-platform --tail=100

# 3. Describe pod for events
kubectl describe pod jivs-backend-xxx -n jivs-platform

# 4. Restart if crashed
kubectl delete pod jivs-backend-xxx -n jivs-platform

# 5. If persistent, rollback
kubectl rollout undo deployment/jivs-backend -n jivs-platform
```

---

## 8. Maintenance Procedures

### 8.1 Scheduled Maintenance Window

**Schedule**: Every Sunday 2:00 AM - 4:00 AM UTC

**Procedure:**
```bash
#!/bin/bash
# maintenance.sh

# 1. Put system in maintenance mode
kubectl scale deployment jivs-frontend --replicas=1 -n jivs-platform
kubectl set image deployment/jivs-frontend frontend=jivs/maintenance-page:latest -n jivs-platform

# 2. Perform database maintenance
psql -U jivs -d jivs <<EOF
VACUUM FULL;
REINDEX DATABASE jivs;
ANALYZE;
EOF

# 3. Update system packages
kubectl set image daemonset/node-updater updater=jivs/system-updater:latest -n kube-system

# 4. Rotate logs
find /var/log/jivs -name "*.log" -mtime +30 -delete
logrotate /etc/logrotate.d/jivs

# 5. Clean up old Docker images
docker image prune -a --filter "until=720h" -f

# 6. Verify backups
./scripts/verify-backups.sh

# 7. Restore normal operation
kubectl scale deployment jivs-frontend --replicas=2 -n jivs-platform
kubectl set image deployment/jivs-frontend frontend=jivs/frontend:latest -n jivs-platform

# 8. Run health checks
./scripts/health-check.sh

# 9. Send completion notification
curl -X POST https://hooks.slack.com/services/YOUR/WEBHOOK \
  -H 'Content-Type: application/json' \
  -d '{"text":"Maintenance window completed successfully"}'
```

---

## 9. Emergency Contacts

### On-Call Schedule

| Role | Primary | Backup | Contact |
|------|---------|--------|---------|
| Platform Engineer | John Doe | Jane Smith | PagerDuty |
| Database Admin | Bob Johnson | Alice Brown | +1-555-0123 |
| Security | Eve Wilson | Charlie Davis | +1-555-0456 |
| Engineering Manager | David Lee | Sarah Chen | +1-555-0789 |

### Escalation Path

1. **Tier 1**: On-call Platform Engineer (0-15 min)
2. **Tier 2**: Engineering Manager (15-30 min)
3. **Tier 3**: CTO (30+ min)

### External Vendors

- **Cloud Provider**: AWS Support
  - Phone: +1-866-216-3396
  - Account: 123456789

- **Database Support**: PostgreSQL Enterprise
  - Email: support@postgresql-enterprise.com
  - SLA: 4 hour response

---

## Appendix

### A. Useful Commands Cheat Sheet

```bash
# Kubernetes
kubectl get pods -n jivs-platform -o wide
kubectl logs -f deployment/jivs-backend -n jivs-platform
kubectl exec -it jivs-backend-xxx -n jivs-platform -- /bin/bash
kubectl port-forward service/postgres 5432:5432 -n jivs-platform

# Database
psql -h postgres -U jivs -d jivs
\dt                          # List tables
\d+ table_name              # Describe table
SELECT * FROM pg_stat_activity;  # Show active queries

# Redis
redis-cli
KEYS pattern*               # Find keys
GET key                     # Get value
INFO                        # Server info

# Docker
docker ps                    # List containers
docker logs container_id     # View logs
docker exec -it container_id /bin/bash  # Shell into container
```

### B. Important File Locations

```
/var/log/jivs/                    # Application logs
/var/lib/postgresql/data/          # Database data
/var/jivs/storage/                # File storage
/etc/jivs/application.yml         # Application config
~/.kube/config                    # Kubernetes config
```

---

**Document Last Reviewed**: January 2025
**Next Review Date**: February 2025
