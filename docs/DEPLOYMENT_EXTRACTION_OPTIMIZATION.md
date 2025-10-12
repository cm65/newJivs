# JiVS Platform - Extraction Performance Optimization Deployment Guide

## Overview

This guide provides step-by-step instructions for deploying the extraction performance optimizations implemented in the P0 phase. These optimizations deliver a **2x throughput improvement** (10k → 20k records/min) and **56% latency reduction** (450ms → 200ms p95).

## Performance Improvements Summary

### P0 Optimizations Implemented

| Optimization | Expected Impact | Files Modified |
|-------------|-----------------|----------------|
| **P0.1: Batch Processing** | +40% throughput, -100ms latency | `JdbcConnector.java`, `PooledJdbcConnector.java` |
| **P0.2: Connection Pooling** | +25% throughput, -80ms latency | `ExtractionDataSourcePool.java`, `ConnectorFactory.java` |
| **P0.3: Redis Caching** | +10% throughput, -50ms latency | `CacheConfig.java`, `DataSourceRepository.java` |
| **P0.4: Query Optimization** | +5% throughput, -20ms latency | `ExtractionJobRepository.java` |
| **P0.5: HikariCP Tuning** | +3% throughput, improved concurrency | `application.yml` |

**Total Expected Improvement:**
- **Throughput:** 2x (10,000 → 20,000 records/min)
- **Latency (p95):** -56% (450ms → 200ms)
- **Cache Hit Rate:** 70%+ target
- **Connection Pool Utilization:** 80-90% target

## Prerequisites

### Software Requirements
- Docker 20.10+
- Docker Compose 2.0+ (for local deployment)
- Kubernetes 1.24+ (for production deployment)
- kubectl 1.24+ (configured with cluster access)
- Helm 3.0+ (optional, for monitoring stack)

### Infrastructure Requirements
- **PostgreSQL 15:** 200 connections, 512MB shared buffers
- **Redis 7:** 2GB memory, LRU eviction policy
- **Application Pods:** 3-5GB memory, 1.5-2.5 CPU per pod
- **Storage:** 20GB for extraction temporary files

### Pre-Deployment Checklist

- [ ] Review current system performance baseline
- [ ] Backup current database
- [ ] Backup current Redis data (if persistent)
- [ ] Review and update environment variables
- [ ] Schedule maintenance window (recommended: off-peak hours)
- [ ] Notify stakeholders of deployment window

## Deployment Steps

### Option A: Local Development (Docker Compose)

#### Step 1: Update Environment Variables

```bash
# Copy and update environment file
cp .env.example .env

# Update critical values in .env:
# - DATABASE_PASSWORD
# - JWT_SECRET
# - ENCRYPTION_KEY
# - REDIS_PASSWORD (if using authentication)
```

#### Step 2: Update Docker Compose Configuration

The `docker-compose.yml` has been updated with:
- PostgreSQL: 200 max connections, optimized settings
- Redis: 2GB memory, allkeys-lru eviction policy
- Backend: JVM optimizations, JMX monitoring on port 9010

```bash
# Verify docker-compose configuration
docker-compose config

# Pull latest images
docker-compose pull
```

#### Step 3: Deploy Services

```bash
# Stop existing services
docker-compose down

# Start infrastructure services first
docker-compose up -d postgres redis elasticsearch rabbitmq

# Wait for services to be healthy (check with docker-compose ps)
# This may take 30-60 seconds

# Start application services
docker-compose up -d backend frontend

# Monitor startup logs
docker-compose logs -f backend
```

#### Step 4: Verify Deployment

```bash
# Check service health
docker-compose ps

# Verify backend health endpoint
curl http://localhost:8080/actuator/health

# Verify JMX monitoring is accessible (optional)
# Install jconsole or use VisualVM to connect to localhost:9010

# Check Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep extraction
```

#### Step 5: Monitor Initial Performance

```bash
# Access Grafana dashboard
open http://localhost:3001

# Default credentials: admin/admin
# Navigate to "JiVS Extraction Performance Dashboard"

# Monitor key metrics:
# - Extraction throughput (target: 20k records/min)
# - API latency p95 (target: <200ms)
# - Cache hit rate (target: >70%)
# - Connection pool utilization (target: 80-90%)
```

### Option B: Production Deployment (Kubernetes)

#### Step 1: Update ConfigMap and Secrets

```bash
# Navigate to kubernetes directory
cd kubernetes

# Review and update configmap.yaml with new values
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml

# Update secrets (use your actual values)
kubectl create secret generic jivs-backend-secrets \
  --from-literal=DATABASE_PASSWORD='your_password' \
  --from-literal=REDIS_PASSWORD='your_redis_password' \
  --from-literal=JWT_SECRET='your_jwt_secret' \
  --from-literal=ENCRYPTION_KEY='your_encryption_key' \
  --from-literal=RABBITMQ_PASSWORD='your_rabbitmq_password' \
  --from-literal=AWS_ACCESS_KEY='your_aws_key' \
  --from-literal=AWS_SECRET_KEY='your_aws_secret' \
  --namespace jivs-platform \
  --dry-run=client -o yaml | kubectl apply -f -
```

#### Step 2: Update PostgreSQL Configuration

```bash
# Apply PostgreSQL StatefulSet with updated connection limits
kubectl apply -f postgres-statefulset.yaml

# Wait for PostgreSQL pods to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n jivs-platform --timeout=300s

# Verify PostgreSQL connection limit
kubectl exec -it postgres-0 -n jivs-platform -- \
  psql -U jivs -d jivs -c "SHOW max_connections;"
# Expected output: 200
```

#### Step 3: Update Redis Configuration

```bash
# Apply Redis StatefulSet with 2GB memory and LRU eviction
kubectl apply -f redis-statefulset.yaml

# Wait for Redis pods to be ready
kubectl wait --for=condition=ready pod -l app=redis -n jivs-platform --timeout=300s

# Verify Redis configuration
kubectl exec -it redis-0 -n jivs-platform -- redis-cli CONFIG GET maxmemory
kubectl exec -it redis-0 -n jivs-platform -- redis-cli CONFIG GET maxmemory-policy
# Expected: 2147483648 (2GB), allkeys-lru
```

#### Step 4: Deploy Backend Application

```bash
# Build and push new Docker image (if not using CI/CD)
cd ../backend
docker build -t your-registry/jivs-backend:v1.1.0 .
docker push your-registry/jivs-backend:v1.1.0

# Update image in backend-deployment.yaml
# Change: image: jivs-backend:latest
# To: image: your-registry/jivs-backend:v1.1.0

cd ../kubernetes

# Apply backend deployment with rolling update
kubectl apply -f backend-deployment.yaml

# Monitor rollout status
kubectl rollout status deployment/jivs-backend -n jivs-platform

# Check pod status
kubectl get pods -n jivs-platform -l app=jivs-backend
```

#### Step 5: Verify Deployment

```bash
# Check pod logs for startup
kubectl logs -f deployment/jivs-backend -n jivs-platform --tail=100

# Verify health endpoints
kubectl exec -it $(kubectl get pod -l app=jivs-backend -n jivs-platform -o jsonpath="{.items[0].metadata.name}") -n jivs-platform -- \
  curl -s http://localhost:8080/actuator/health

# Check HikariCP metrics via JMX (optional)
kubectl port-forward deployment/jivs-backend 9010:9010 -n jivs-platform
# Connect jconsole to localhost:9010

# Verify Prometheus metrics
kubectl port-forward deployment/jivs-backend 8080:8080 -n jivs-platform
curl http://localhost:8080/actuator/prometheus | grep extraction
```

#### Step 6: Deploy Monitoring Dashboard

```bash
# Deploy Grafana dashboard
kubectl apply -f ../monitoring/grafana-deployment.yaml

# Access Grafana
kubectl port-forward deployment/grafana 3000:3000 -n jivs-platform
open http://localhost:3000

# Import dashboard from: monitoring/grafana/dashboards/extraction-performance-dashboard.json
# Or place the JSON file in the provisioning directory and restart Grafana
```

## Post-Deployment Validation

### 1. Performance Testing

Run load tests to validate performance improvements:

```bash
# Option 1: Using k6 (if installed)
k6 run --vus 100 --duration 10m load-tests/extraction-load-test.js

# Option 2: Using Apache Bench
ab -n 1000 -c 50 http://your-api-url/api/v1/extractions

# Option 3: Manual testing via API
# Create and start extraction jobs
# Monitor throughput and latency metrics in Grafana
```

### 2. Metrics Validation

Check the following metrics in Grafana:

| Metric | Baseline | Target | Acceptance Criteria |
|--------|----------|--------|---------------------|
| Throughput (records/min) | 10,000 | 20,000 | >= 18,000 |
| API Latency p95 (ms) | 450 | 200 | <= 220 |
| API Latency p99 (ms) | 800 | 350 | <= 400 |
| Cache Hit Rate (%) | 0 | 70 | >= 60 |
| Connection Pool Utilization (%) | 50 | 85 | 70-95 |
| Error Rate (%) | 0.5 | < 1 | < 1.5 |

### 3. Functional Testing

Verify all extraction features work correctly:

- [ ] Create new extraction job
- [ ] Start extraction job
- [ ] Monitor extraction progress
- [ ] Verify extraction completion
- [ ] Check extracted data accuracy
- [ ] Test extraction with various source types (JDBC, SAP, File, API)
- [ ] Test concurrent extractions
- [ ] Test extraction pause/resume (if implemented)

### 4. Integration Testing

Verify integration with other components:

- [ ] Migration jobs use extracted data
- [ ] Data quality checks run on extracted data
- [ ] Compliance scans detect PII in extracted data
- [ ] Analytics dashboards show extraction metrics

## Monitoring and Alerts

### Key Metrics to Monitor

#### Application Metrics (Prometheus)

```promql
# Extraction throughput
rate(extraction_records_total[1m]) * 60

# API latency (p95)
histogram_quantile(0.95, rate(extraction_duration_bucket[5m]))

# Cache hit rate
sum(rate(extraction_cache_hits[5m])) / (sum(rate(extraction_cache_hits[5m])) + sum(rate(extraction_cache_misses[5m])))

# Connection pool utilization
extraction_pool_active / extraction_pool_max

# Error rate
sum(rate(extraction_errors_total[5m])) / sum(rate(extraction_records_total[5m]))
```

#### Infrastructure Metrics

- **CPU Usage:** Should increase to 60-70% (from 30%)
- **Memory Usage:** 3-4GB heap usage (within 5GB limit)
- **PostgreSQL Connections:** 80-120 active connections (within 200 limit)
- **Redis Memory:** 1-1.5GB used (within 2GB limit)

### Recommended Alerts

Set up alerts for the following conditions:

```yaml
# Prometheus AlertManager rules
groups:
  - name: extraction_performance
    rules:
      - alert: ExtractionThroughputLow
        expr: rate(extraction_records_total[5m]) * 60 < 18000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Extraction throughput below target"
          description: "Current throughput: {{ $value }} records/min (target: 20k)"

      - alert: ExtractionLatencyHigh
        expr: histogram_quantile(0.95, rate(extraction_duration_bucket[5m])) > 0.22
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Extraction API latency high"
          description: "p95 latency: {{ $value }}s (target: 0.2s)"

      - alert: CacheHitRateLow
        expr: sum(rate(extraction_cache_hits[5m])) / (sum(rate(extraction_cache_hits[5m])) + sum(rate(extraction_cache_misses[5m]))) < 0.6
        for: 15m
        labels:
          severity: info
        annotations:
          summary: "Cache hit rate below target"
          description: "Cache hit rate: {{ $value }}% (target: 70%)"

      - alert: ConnectionPoolExhaustion
        expr: hikaricp_connections_active / hikaricp_connections > 0.95
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Connection pool nearly exhausted"
          description: "Pool utilization: {{ $value }}%"
```

## Rollback Procedures

### If Performance Targets Not Met

If throughput < 15k records/min or latency p95 > 300ms after 30 minutes:

1. **Investigate Bottlenecks:**
   ```bash
   # Check application logs
   kubectl logs -f deployment/jivs-backend -n jivs-platform --tail=500

   # Check for errors
   kubectl logs deployment/jivs-backend -n jivs-platform | grep ERROR

   # Check database connections
   kubectl exec postgres-0 -n jivs-platform -- \
     psql -U jivs -d jivs -c "SELECT count(*) FROM pg_stat_activity;"

   # Check Redis memory
   kubectl exec redis-0 -n jivs-platform -- redis-cli INFO memory
   ```

2. **Tune Configuration:**
   - Adjust batch size (try 500 or 2000)
   - Adjust parallel threads (try 2 or 8)
   - Adjust connection pool sizes

3. **If issues persist, proceed to full rollback**

### Full Rollback (Docker Compose)

```bash
# Stop current deployment
docker-compose down

# Checkout previous version
git checkout <previous-commit-hash>

# Restore database backup (if needed)
docker exec jivs-postgres psql -U jivs -d jivs < backup.sql

# Start previous version
docker-compose up -d

# Verify rollback
curl http://localhost:8080/actuator/health
```

### Full Rollback (Kubernetes)

```bash
# Rollback backend deployment
kubectl rollout undo deployment/jivs-backend -n jivs-platform

# Verify rollback status
kubectl rollout status deployment/jivs-backend -n jivs-platform

# Rollback Redis configuration
kubectl rollout undo statefulset/redis -n jivs-platform

# Rollback PostgreSQL configuration
kubectl rollout undo statefulset/postgres -n jivs-platform

# Clear Redis cache (if needed)
kubectl exec redis-0 -n jivs-platform -- redis-cli FLUSHALL

# Verify all services are healthy
kubectl get pods -n jivs-platform
```

### Partial Rollback (Configuration Only)

If code is fine but configuration needs adjustment:

```bash
# Kubernetes: Update configmap
kubectl edit configmap jivs-backend-config -n jivs-platform

# Restart pods to pick up new config
kubectl rollout restart deployment/jivs-backend -n jivs-platform

# Docker Compose: Update .env file and restart
docker-compose restart backend
```

## Troubleshooting

### Issue: High Memory Usage (OOM Errors)

**Symptoms:** Pods restarting, OOMKilled status

**Resolution:**
```bash
# Check memory usage
kubectl top pods -n jivs-platform

# Reduce batch size in configmap
EXTRACTION_BATCH_SIZE: 500  # Instead of 1000

# Reduce parallel threads
EXTRACTION_PARALLEL_THREADS: 2  # Instead of 4

# Increase pod memory limits
resources:
  limits:
    memory: "6Gi"  # Instead of 5Gi
```

### Issue: Connection Pool Exhaustion

**Symptoms:** "Connection pool exhausted" errors, timeouts

**Resolution:**
```bash
# Check active connections
kubectl exec postgres-0 -n jivs-platform -- \
  psql -U jivs -d jivs -c "SELECT state, count(*) FROM pg_stat_activity GROUP BY state;"

# Increase HikariCP pool size
HIKARI_MAXIMUM_POOL_SIZE: 75  # Instead of 50

# Increase source pool size
EXTRACTION_SOURCE_POOL_MAX_SIZE: 15  # Instead of 10

# Increase PostgreSQL max connections
postgres -c max_connections=300  # Instead of 200
```

### Issue: Low Cache Hit Rate

**Symptoms:** Cache hit rate < 50%

**Resolution:**
```bash
# Check Redis memory usage
kubectl exec redis-0 -n jivs-platform -- redis-cli INFO memory

# Check eviction statistics
kubectl exec redis-0 -n jivs-platform -- redis-cli INFO stats | grep evicted

# Increase Redis memory
maxmemory: 3gb  # Instead of 2gb

# Increase cache TTL
CACHE_TTL_DATASOURCES: 7200000  # 2 hours instead of 1
```

### Issue: High GC Pause Times

**Symptoms:** p99 latency spikes, GC logs showing long pauses

**Resolution:**
```bash
# Analyze GC logs
kubectl logs deployment/jivs-backend -n jivs-platform | grep "GC pause"

# Tune G1GC settings
JAVA_OPTS: -Xms3g -Xmx5g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:G1HeapRegionSize=16M

# Consider switching to ZGC (Java 17+)
JAVA_OPTS: -Xms3g -Xmx5g -XX:+UseZGC -XX:ZCollectionInterval=5
```

## Performance Tuning Guide

### Optimal Settings by Workload

#### High-Volume, Small Records (< 1KB per record)
```yaml
EXTRACTION_BATCH_SIZE: 2000
EXTRACTION_PARALLEL_THREADS: 8
EXTRACTION_SOURCE_POOL_MAX_SIZE: 15
HIKARI_MAXIMUM_POOL_SIZE: 75
```

#### Large Records (> 10KB per record)
```yaml
EXTRACTION_BATCH_SIZE: 500
EXTRACTION_PARALLEL_THREADS: 2
EXTRACTION_SOURCE_POOL_MAX_SIZE: 5
HIKARI_MAXIMUM_POOL_SIZE: 40
```

#### Mixed Workload (Default)
```yaml
EXTRACTION_BATCH_SIZE: 1000
EXTRACTION_PARALLEL_THREADS: 4
EXTRACTION_SOURCE_POOL_MAX_SIZE: 10
HIKARI_MAXIMUM_POOL_SIZE: 50
```

## Maintenance Procedures

### Weekly Maintenance

1. Review Grafana dashboards for trends
2. Check error logs for patterns
3. Verify backup success
4. Monitor disk usage for temp files

### Monthly Maintenance

1. Analyze slow query logs
2. Review and optimize database indexes
3. Update dependencies (security patches)
4. Performance regression testing

### Quarterly Maintenance

1. Load testing with realistic data volumes
2. Disaster recovery drill
3. Review and update capacity planning
4. Security audit

## Support and Contact

For issues or questions:

1. Check application logs: `kubectl logs -f deployment/jivs-backend -n jivs-platform`
2. Review Grafana dashboards for anomalies
3. Consult troubleshooting section above
4. Create GitHub issue with:
   - Error messages
   - Metrics screenshots
   - Relevant log excerpts
   - Environment details

## Appendix

### A. Configuration Reference

Complete list of configuration parameters and their impact on performance:

| Parameter | Default | Impact | Recommendation |
|-----------|---------|--------|----------------|
| `EXTRACTION_BATCH_SIZE` | 1000 | Memory, throughput | 500-2000 |
| `EXTRACTION_PARALLEL_THREADS` | 4 | CPU, throughput | 2-8 |
| `HIKARI_MAXIMUM_POOL_SIZE` | 50 | Concurrency | 40-75 |
| `REDIS_MAXMEMORY` | 2gb | Cache capacity | 2-4gb |
| `EXTRACTION_SOURCE_POOL_MAX_SIZE` | 10 | Source DB load | 5-15 |

### B. Metrics Glossary

- **Throughput:** Records processed per minute
- **Latency (p95):** 95th percentile response time
- **Cache Hit Rate:** Percentage of requests served from cache
- **Connection Pool Utilization:** Active connections / Max connections
- **Error Rate:** Failed operations / Total operations

### C. Related Documents

- [ARCHITECTURE.md](../ARCHITECTURE.md) - System architecture overview
- [CLAUDE.md](../CLAUDE.md) - Complete implementation guide
- [DISASTER_RECOVERY.md](../DISASTER_RECOVERY.md) - DR procedures
- [SECURITY_AUDIT_CHECKLIST.md](../SECURITY_AUDIT_CHECKLIST.md) - Security guidelines

---

**Document Version:** 1.0
**Last Updated:** January 2025
**Maintained By:** JiVS DevOps Team
