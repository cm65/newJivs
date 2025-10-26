# JiVS Extraction Module - Production Monitoring Guide

**Purpose:** Comprehensive monitoring setup to detect extraction module issues in production
**Audience:** DevOps, SRE, Backend Engineers
**Updated:** 2025-10-26

---

## 🎯 MONITORING OBJECTIVES

**Critical Issues to Detect:**
1. SQL injection attempts (P0 security)
2. Password encryption failures (P0 security)
3. Batch processing failures (P0 data loss)
4. Thread-safety violations (P1 data corruption)
5. Resource leaks (P1 performance degradation)
6. Connection pool exhaustion (P1 availability)

**Key Metrics:**
- Extraction job success/failure rates
- Processing throughput (records/sec)
- Response times (p50, p95, p99)
- Error rates by type
- Resource utilization (CPU, memory, connections)

---

## 📊 PROMETHEUS METRICS

### 1. Custom Application Metrics

Create: `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionMetrics.java`

```java
package com.jivs.platform.service.extraction;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prometheus metrics for extraction module monitoring.
 * Tracks critical operations identified in security audit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExtractionMetrics {

    private final MeterRegistry meterRegistry;

    // Counters for security events
    private Counter sqlInjectionAttempts;
    private Counter passwordEncryptionFailures;
    private Counter passwordDecryptionFailures;

    // Counters for data processing
    private Counter extractionJobsStarted;
    private Counter extractionJobsCompleted;
    private Counter extractionJobsFailed;
    private Counter recordsExtracted;
    private Counter recordsFailed;
    private Counter batchWriteFailures;

    // Gauges for resource monitoring
    private AtomicInteger activeExtractionJobs;
    private AtomicInteger connectionPoolActive;
    private AtomicInteger connectionPoolIdle;

    // Timers for performance
    private Timer extractionJobDuration;
    private Timer batchWriteDuration;
    private Timer sqlValidationDuration;

    // Distribution summaries
    private DistributionSummary batchSizeDistribution;

    /**
     * Initialize metrics on bean creation
     */
    @jakarta.annotation.PostConstruct
    public void initMetrics() {
        // Security metrics
        sqlInjectionAttempts = Counter.builder("jivs.extraction.security.sql_injection_attempts")
                .description("Number of SQL injection attempts detected")
                .tag("module", "extraction")
                .register(meterRegistry);

        passwordEncryptionFailures = Counter.builder("jivs.extraction.security.password_encryption_failures")
                .description("Number of password encryption failures")
                .tag("module", "extraction")
                .register(meterRegistry);

        passwordDecryptionFailures = Counter.builder("jivs.extraction.security.password_decryption_failures")
                .description("Number of password decryption failures")
                .tag("module", "extraction")
                .register(meterRegistry);

        // Job lifecycle metrics
        extractionJobsStarted = Counter.builder("jivs.extraction.jobs.started")
                .description("Total extraction jobs started")
                .tag("module", "extraction")
                .register(meterRegistry);

        extractionJobsCompleted = Counter.builder("jivs.extraction.jobs.completed")
                .description("Total extraction jobs completed successfully")
                .tag("module", "extraction")
                .register(meterRegistry);

        extractionJobsFailed = Counter.builder("jivs.extraction.jobs.failed")
                .description("Total extraction jobs failed")
                .tag("module", "extraction")
                .register(meterRegistry);

        // Data processing metrics
        recordsExtracted = Counter.builder("jivs.extraction.records.extracted")
                .description("Total records extracted")
                .tag("module", "extraction")
                .register(meterRegistry);

        recordsFailed = Counter.builder("jivs.extraction.records.failed")
                .description("Total records failed")
                .tag("module", "extraction")
                .register(meterRegistry);

        batchWriteFailures = Counter.builder("jivs.extraction.batch.write_failures")
                .description("Number of batch write failures")
                .tag("module", "extraction")
                .register(meterRegistry);

        // Resource gauges
        activeExtractionJobs = meterRegistry.gauge(
                "jivs.extraction.jobs.active",
                Tags.of("module", "extraction"),
                new AtomicInteger(0)
        );

        connectionPoolActive = meterRegistry.gauge(
                "jivs.extraction.connection_pool.active",
                Tags.of("module", "extraction"),
                new AtomicInteger(0)
        );

        connectionPoolIdle = meterRegistry.gauge(
                "jivs.extraction.connection_pool.idle",
                Tags.of("module", "extraction"),
                new AtomicInteger(0)
        );

        // Timers
        extractionJobDuration = Timer.builder("jivs.extraction.jobs.duration")
                .description("Extraction job duration")
                .tag("module", "extraction")
                .register(meterRegistry);

        batchWriteDuration = Timer.builder("jivs.extraction.batch.write_duration")
                .description("Batch write duration")
                .tag("module", "extraction")
                .register(meterRegistry);

        sqlValidationDuration = Timer.builder("jivs.extraction.security.sql_validation_duration")
                .description("SQL validation duration")
                .tag("module", "extraction")
                .register(meterRegistry);

        // Distribution summaries
        batchSizeDistribution = DistributionSummary.builder("jivs.extraction.batch.size")
                .description("Distribution of batch sizes")
                .tag("module", "extraction")
                .register(meterRegistry);

        log.info("Extraction module Prometheus metrics initialized");
    }

    // Security event tracking
    public void recordSqlInjectionAttempt() {
        sqlInjectionAttempts.increment();
        log.warn("SQL injection attempt recorded in metrics");
    }

    public void recordPasswordEncryptionFailure() {
        passwordEncryptionFailures.increment();
    }

    public void recordPasswordDecryptionFailure() {
        passwordDecryptionFailures.increment();
    }

    // Job lifecycle tracking
    public void recordJobStarted() {
        extractionJobsStarted.increment();
        activeExtractionJobs.incrementAndGet();
    }

    public void recordJobCompleted(long durationMillis) {
        extractionJobsCompleted.increment();
        activeExtractionJobs.decrementAndGet();
        extractionJobDuration.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void recordJobFailed(long durationMillis) {
        extractionJobsFailed.increment();
        activeExtractionJobs.decrementAndGet();
        extractionJobDuration.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    // Data processing tracking
    public void recordRecordsExtracted(long count) {
        recordsExtracted.increment(count);
    }

    public void recordRecordsFailed(long count) {
        recordsFailed.increment(count);
    }

    public void recordBatchWriteFailure() {
        batchWriteFailures.increment();
    }

    public void recordBatchWriteDuration(long durationMillis, int batchSize) {
        batchWriteDuration.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        batchSizeDistribution.record(batchSize);
    }

    // Connection pool tracking
    public void updateConnectionPoolStats(int active, int idle) {
        connectionPoolActive.set(active);
        connectionPoolIdle.set(idle);
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordSqlValidation(Timer.Sample sample) {
        sample.stop(sqlValidationDuration);
    }
}
```

### 2. Integrate Metrics into Services

**Update ExtractionService.java** to record metrics:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractionService {

    private final ExtractionMetrics metrics;  // ADD THIS

    @Async
    public void runExtraction(Long jobId) {
        long startTime = System.currentTimeMillis();
        metrics.recordJobStarted();  // ADD THIS

        try {
            // ... existing extraction logic ...

            long duration = System.currentTimeMillis() - startTime;
            metrics.recordJobCompleted(duration);  // ADD THIS
            metrics.recordRecordsExtracted(result.getRecordsExtracted());  // ADD THIS

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordJobFailed(duration);  // ADD THIS
            log.error("Extraction failed", e);
            throw e;
        }
    }
}
```

**Update JdbcConnector.java** for SQL injection metrics:

```java
public class JdbcConnector implements DataConnector {

    private final ExtractionMetrics metrics;  // ADD THIS

    @Override
    public ExtractionResult extractData(String query) {
        // SQL injection validation
        Timer.Sample validationTimer = metrics.startTimer();
        if (!sqlValidator.isQuerySafe(query)) {
            metrics.recordSqlInjectionAttempt();  // ADD THIS
            metrics.recordSqlValidation(validationTimer);
            throw new SecurityException("SQL injection detected");
        }
        metrics.recordSqlValidation(validationTimer);

        // ... rest of extraction ...
    }

    private void processBatch(List<Map<String, Object>> batch, ExtractionResult result) {
        long startTime = System.currentTimeMillis();

        try {
            // Write batch to file
            batchWriter.writeBatch(batch);

            long duration = System.currentTimeMillis() - startTime;
            metrics.recordBatchWriteDuration(duration, batch.size());  // ADD THIS

        } catch (Exception e) {
            metrics.recordBatchWriteFailure();  // ADD THIS
            log.error("Batch write failed", e);
        }
    }
}
```

### 3. Prometheus Configuration

Create: `kubernetes/prometheus/extraction-servicemonitor.yaml`

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: jivs-extraction-metrics
  namespace: jivs
  labels:
    app: jivs-backend
    module: extraction
spec:
  selector:
    matchLabels:
      app: jivs-backend
  endpoints:
    - port: metrics
      path: /actuator/prometheus
      interval: 15s
      scrapeTimeout: 10s
```

---

## 📈 GRAFANA DASHBOARD

### Dashboard JSON Configuration

Create: `kubernetes/grafana/dashboards/extraction-module-dashboard.json`

```json
{
  "dashboard": {
    "title": "JiVS Extraction Module Monitoring",
    "tags": ["jivs", "extraction", "data-integration"],
    "timezone": "browser",
    "panels": [
      {
        "title": "🚨 Security Alerts",
        "type": "stat",
        "gridPos": {"h": 4, "w": 8, "x": 0, "y": 0},
        "targets": [
          {
            "expr": "rate(jivs_extraction_security_sql_injection_attempts_total[5m])",
            "legendFormat": "SQL Injection Attempts/sec"
          }
        ],
        "options": {
          "colorMode": "background",
          "graphMode": "area"
        },
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "mode": "absolute",
              "steps": [
                {"value": 0, "color": "green"},
                {"value": 0.1, "color": "red"}
              ]
            }
          }
        }
      },
      {
        "title": "Password Encryption Failures",
        "type": "stat",
        "gridPos": {"h": 4, "w": 8, "x": 8, "y": 0},
        "targets": [
          {
            "expr": "increase(jivs_extraction_security_password_encryption_failures_total[1h])",
            "legendFormat": "Last Hour"
          }
        ],
        "options": {
          "colorMode": "background"
        }
      },
      {
        "title": "Active Extraction Jobs",
        "type": "gauge",
        "gridPos": {"h": 4, "w": 8, "x": 16, "y": 0},
        "targets": [
          {
            "expr": "jivs_extraction_jobs_active",
            "legendFormat": "Active Jobs"
          }
        ],
        "options": {
          "max": 10,
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {"value": 0, "color": "green"},
              {"value": 5, "color": "yellow"},
              {"value": 8, "color": "red"}
            ]
          }
        }
      },
      {
        "title": "Extraction Job Success Rate",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 4},
        "targets": [
          {
            "expr": "rate(jivs_extraction_jobs_completed_total[5m]) / (rate(jivs_extraction_jobs_completed_total[5m]) + rate(jivs_extraction_jobs_failed_total[5m])) * 100",
            "legendFormat": "Success Rate %"
          }
        ]
      },
      {
        "title": "Records Processing Rate",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 4},
        "targets": [
          {
            "expr": "rate(jivs_extraction_records_extracted_total[1m])",
            "legendFormat": "Extracted/sec"
          },
          {
            "expr": "rate(jivs_extraction_records_failed_total[1m])",
            "legendFormat": "Failed/sec"
          }
        ]
      },
      {
        "title": "Extraction Job Duration (p95)",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 12},
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(jivs_extraction_jobs_duration_seconds_bucket[5m]))",
            "legendFormat": "p95 Duration"
          },
          {
            "expr": "histogram_quantile(0.50, rate(jivs_extraction_jobs_duration_seconds_bucket[5m]))",
            "legendFormat": "p50 Duration"
          }
        ]
      },
      {
        "title": "Connection Pool Utilization",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 12},
        "targets": [
          {
            "expr": "jivs_extraction_connection_pool_active",
            "legendFormat": "Active Connections"
          },
          {
            "expr": "jivs_extraction_connection_pool_idle",
            "legendFormat": "Idle Connections"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "custom": {
              "fillOpacity": 10,
              "lineWidth": 2
            }
          }
        }
      },
      {
        "title": "Batch Write Performance",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 20},
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(jivs_extraction_batch_write_duration_seconds_bucket[5m]))",
            "legendFormat": "p95 Write Duration"
          },
          {
            "expr": "rate(jivs_extraction_batch_write_failures_total[5m])",
            "legendFormat": "Write Failures/sec"
          }
        ]
      },
      {
        "title": "Batch Size Distribution",
        "type": "heatmap",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 20},
        "targets": [
          {
            "expr": "rate(jivs_extraction_batch_size_bucket[5m])",
            "format": "heatmap",
            "legendFormat": "{{le}}"
          }
        ]
      }
    ],
    "refresh": "30s"
  }
}
```

---

## 🚨 ALERTING RULES

### Prometheus Alert Rules

Create: `kubernetes/prometheus/alerts/extraction-alerts.yaml`

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: jivs-extraction-alerts
  namespace: jivs
  labels:
    prometheus: kube-prometheus
spec:
  groups:
    - name: extraction.security
      interval: 30s
      rules:
        - alert: SQLInjectionAttackDetected
          expr: rate(jivs_extraction_security_sql_injection_attempts_total[5m]) > 0
          for: 1m
          labels:
            severity: critical
            module: extraction
            category: security
          annotations:
            summary: "SQL injection attack detected on extraction module"
            description: "{{ $value }} SQL injection attempts per second detected. Immediate investigation required."
            runbook_url: "https://wiki.company.com/runbooks/extraction-sql-injection"

        - alert: PasswordEncryptionFailuresHigh
          expr: increase(jivs_extraction_security_password_encryption_failures_total[1h]) > 5
          for: 5m
          labels:
            severity: critical
            module: extraction
            category: security
          annotations:
            summary: "High rate of password encryption failures"
            description: "{{ $value }} password encryption failures in the last hour. Credentials may be at risk."

        - alert: PasswordDecryptionFailuresHigh
          expr: increase(jivs_extraction_security_password_decryption_failures_total[1h]) > 10
          for: 5m
          labels:
            severity: warning
            module: extraction
            category: security
          annotations:
            summary: "High rate of password decryption failures"
            description: "{{ $value }} password decryption failures in the last hour. Check encryption key rotation."

    - name: extraction.availability
      interval: 30s
      rules:
        - alert: ExtractionJobFailureRateHigh
          expr: |
            (
              rate(jivs_extraction_jobs_failed_total[5m]) /
              (rate(jivs_extraction_jobs_completed_total[5m]) + rate(jivs_extraction_jobs_failed_total[5m]))
            ) > 0.1
          for: 10m
          labels:
            severity: warning
            module: extraction
            category: availability
          annotations:
            summary: "Extraction job failure rate above 10%"
            description: "{{ $value | humanizePercentage }} of extraction jobs are failing. Check logs for errors."

        - alert: ExtractionJobFailureRateCritical
          expr: |
            (
              rate(jivs_extraction_jobs_failed_total[5m]) /
              (rate(jivs_extraction_jobs_completed_total[5m]) + rate(jivs_extraction_jobs_failed_total[5m]))
            ) > 0.5
          for: 5m
          labels:
            severity: critical
            module: extraction
            category: availability
          annotations:
            summary: "CRITICAL: Extraction job failure rate above 50%"
            description: "{{ $value | humanizePercentage }} of extraction jobs are failing. System degraded."

        - alert: NoExtractionJobsRunning
          expr: sum(increase(jivs_extraction_jobs_started_total[1h])) == 0
          for: 2h
          labels:
            severity: warning
            module: extraction
            category: availability
          annotations:
            summary: "No extraction jobs started in last 2 hours"
            description: "Extraction module may be stuck or disabled. Check scheduler and job queue."

    - name: extraction.performance
      interval: 30s
      rules:
        - alert: ExtractionJobDurationHigh
          expr: histogram_quantile(0.95, rate(jivs_extraction_jobs_duration_seconds_bucket[5m])) > 600
          for: 15m
          labels:
            severity: warning
            module: extraction
            category: performance
          annotations:
            summary: "Extraction job duration p95 above 10 minutes"
            description: "p95 extraction duration: {{ $value }}s. Performance degradation detected."

        - alert: BatchWriteFailuresHigh
          expr: rate(jivs_extraction_batch_write_failures_total[5m]) > 0.1
          for: 10m
          labels:
            severity: critical
            module: extraction
            category: data-loss
          annotations:
            summary: "High rate of batch write failures (DATA LOSS RISK)"
            description: "{{ $value }} batch write failures/sec. Extracted data may be lost."

        - alert: RecordFailureRateHigh
          expr: |
            (
              rate(jivs_extraction_records_failed_total[5m]) /
              (rate(jivs_extraction_records_extracted_total[5m]) + rate(jivs_extraction_records_failed_total[5m]))
            ) > 0.05
          for: 10m
          labels:
            severity: warning
            module: extraction
            category: data-quality
          annotations:
            summary: "Record failure rate above 5%"
            description: "{{ $value | humanizePercentage }} of records are failing extraction."

    - name: extraction.resources
      interval: 30s
      rules:
        - alert: ConnectionPoolNearExhaustion
          expr: |
            jivs_extraction_connection_pool_active /
            (jivs_extraction_connection_pool_active + jivs_extraction_connection_pool_idle) > 0.9
          for: 5m
          labels:
            severity: warning
            module: extraction
            category: resources
          annotations:
            summary: "Database connection pool near exhaustion"
            description: "{{ $value | humanizePercentage }} of connections in use. Risk of connection timeout."

        - alert: ConnectionPoolExhausted
          expr: jivs_extraction_connection_pool_idle == 0
          for: 2m
          labels:
            severity: critical
            module: extraction
            category: resources
          annotations:
            summary: "CRITICAL: Database connection pool exhausted"
            description: "No idle connections available. New extraction jobs will fail."

        - alert: TooManyActiveJobs
          expr: jivs_extraction_jobs_active > 10
          for: 10m
          labels:
            severity: warning
            module: extraction
            category: resources
          annotations:
            summary: "Unusually high number of active extraction jobs"
            description: "{{ $value }} active jobs. Check for stuck jobs or excessive load."
```

---

## 🏥 HEALTH CHECKS

### Custom Health Indicator

Create: `backend/src/main/java/com/jivs/platform/health/ExtractionHealthIndicator.java`

```java
package com.jivs.platform.health;

import com.jivs.platform.repository.ExtractionJobRepository;
import com.jivs.platform.service.extraction.ExtractionDataSourcePool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Health indicator for extraction module.
 * Checks critical components identified in audit.
 */
@Component("extraction")
@RequiredArgsConstructor
@Slf4j
public class ExtractionHealthIndicator implements HealthIndicator {

    private final ExtractionJobRepository jobRepository;
    private final ExtractionDataSourcePool dataSourcePool;

    @Override
    public Health health() {
        try {
            // Check 1: Database connectivity
            long jobCount = jobRepository.count();

            // Check 2: Connection pool health
            int activeConnections = dataSourcePool.getActiveConnectionCount();
            int idleConnections = dataSourcePool.getIdleConnectionCount();
            int totalConnections = activeConnections + idleConnections;

            // Check 3: Stuck jobs (running > 1 hour)
            long stuckJobs = jobRepository.countByStatusAndStartedAtBefore(
                ExtractionJob.JobStatus.RUNNING,
                java.time.LocalDateTime.now().minusHours(1)
            );

            Health.Builder builder = Health.up();

            builder.withDetail("totalJobs", jobCount)
                   .withDetail("connectionPool", totalConnections + " total (" +
                               activeConnections + " active, " +
                               idleConnections + " idle)")
                   .withDetail("stuckJobs", stuckJobs);

            // Warning conditions
            if (stuckJobs > 0) {
                builder.status("WARNING")
                       .withDetail("warning", stuckJobs + " jobs stuck in RUNNING state");
            }

            if (idleConnections == 0) {
                builder.status("WARNING")
                       .withDetail("warning", "Connection pool exhausted");
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                        .withException(e)
                        .build();
        }
    }
}
```

### Liveness and Readiness Probes

Update: `kubernetes/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jivs-backend
spec:
  template:
    spec:
      containers:
        - name: jivs-backend
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3

          # Startup probe for slow initialization
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 0
            periodSeconds: 10
            timeoutSeconds: 3
            failureThreshold: 30  # 5 minutes max startup time
```

---

## 📝 LOG AGGREGATION

### Structured Logging Configuration

Update: `backend/src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- JSON format for log aggregation -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app":"jivs","module":"extraction"}</customFields>
            <fieldNames>
                <timestamp>timestamp</timestamp>
                <message>message</message>
                <logger>logger</logger>
                <thread>thread</thread>
                <level>level</level>
            </fieldNames>
        </encoder>
    </appender>

    <!-- Extraction-specific logger -->
    <logger name="com.jivs.platform.service.extraction" level="INFO" additivity="false">
        <appender-ref ref="JSON"/>
    </logger>

    <!-- Security events logger (for SIEM integration) -->
    <logger name="SECURITY_EVENTS" level="WARN" additivity="false">
        <appender-ref ref="JSON"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

### Critical Log Events

**In JdbcConnector.java**, add structured logging for security events:

```java
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class JdbcConnector implements DataConnector {

    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_EVENTS");

    @Override
    public ExtractionResult extractData(String query) {
        if (!sqlValidator.isQuerySafe(query)) {
            securityLogger.warn("SQL_INJECTION_ATTEMPT detected. " +
                "Query hash: {}, User: {}, IP: {}",
                Integer.toHexString(query.hashCode()),
                SecurityContextHolder.getContext().getAuthentication().getName(),
                RequestContextHolder.currentRequestAttributes()
            );

            metrics.recordSqlInjectionAttempt();
            throw new SecurityException("Query failed security validation");
        }

        // ... rest of logic
    }
}
```

### ELK Stack Query Examples

**Find SQL injection attempts (last 24 hours):**
```
module:extraction AND message:"SQL_INJECTION_ATTEMPT" AND @timestamp:[now-24h TO now]
```

**Find password encryption failures:**
```
module:extraction AND (message:"password encryption failed" OR message:"CryptoUtil.encrypt failed")
```

**Find batch write failures (data loss risk):**
```
module:extraction AND message:"Batch write failed" AND level:ERROR
```

**Find stuck jobs:**
```
module:extraction AND message:"Extraction job timeout" AND @timestamp:[now-1h TO now]
```

---

## 🔔 NOTIFICATION CHANNELS

### Slack Integration

Create: `kubernetes/alertmanager/config.yaml`

```yaml
global:
  slack_api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'

route:
  group_by: ['alertname', 'module']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'slack-extraction'
  routes:
    - match:
        severity: critical
        module: extraction
      receiver: 'slack-extraction-critical'
      continue: true

receivers:
  - name: 'slack-extraction'
    slack_configs:
      - channel: '#jivs-extraction-alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: >-
          {{ range .Alerts }}
            *Alert:* {{ .Annotations.summary }}
            *Description:* {{ .Annotations.description }}
            *Severity:* {{ .Labels.severity }}
            *Module:* {{ .Labels.module }}
          {{ end }}
        send_resolved: true

  - name: 'slack-extraction-critical'
    slack_configs:
      - channel: '#jivs-extraction-critical'
        title: '🚨 CRITICAL: {{ .GroupLabels.alertname }}'
        text: >-
          @channel CRITICAL ALERT
          {{ range .Alerts }}
            *Alert:* {{ .Annotations.summary }}
            *Description:* {{ .Annotations.description }}
            *Runbook:* {{ .Annotations.runbook_url }}
          {{ end }}
        send_resolved: true
```

### PagerDuty Integration (for P0 alerts)

```yaml
  - name: 'pagerduty-extraction'
    pagerduty_configs:
      - service_key: 'YOUR_PAGERDUTY_SERVICE_KEY'
        description: '{{ .GroupLabels.alertname }}: {{ .Annotations.summary }}'
        severity: '{{ .Labels.severity }}'
        client: 'JiVS Extraction Module'
        client_url: 'https://grafana.company.com/d/extraction-dashboard'
```

---

## 📊 RUNBOOK: RESPONDING TO ALERTS

### 1. SQLInjectionAttackDetected

**Severity:** CRITICAL (P0)

**Immediate Actions:**
1. Check Grafana dashboard for attack source
2. Review logs for user/IP address:
   ```bash
   kubectl logs -l app=jivs-backend --tail=100 | grep "SQL_INJECTION_ATTEMPT"
   ```
3. If attack is ongoing, temporarily block IP at ingress:
   ```bash
   kubectl apply -f kubernetes/ingress-block-ip.yaml
   ```
4. Notify security team
5. Review extraction job queue for suspicious jobs

**Investigation:**
- Check if SQL validation is enabled (should be after audit fixes)
- Review recent code changes to JdbcConnector
- Audit user accounts that created suspicious extraction jobs

**Resolution:**
- Keep IP block in place pending investigation
- Consider rate limiting extraction API
- Review SQL injection patterns in SqlInjectionValidator

### 2. BatchWriteFailuresHigh (DATA LOSS RISK)

**Severity:** CRITICAL (P0)

**Immediate Actions:**
1. **STOP NEW EXTRACTION JOBS** to prevent further data loss:
   ```bash
   kubectl scale deployment jivs-extraction-scheduler --replicas=0
   ```
2. Check disk space on storage backend:
   ```bash
   kubectl exec -it jivs-backend-pod -- df -h
   ```
3. Review batch write errors in logs:
   ```bash
   kubectl logs -l app=jivs-backend | grep "Batch write failed"
   ```
4. Check if batch writer implementation is deployed (should be after audit fixes)

**Investigation:**
- Verify ParquetBatchWriter/CsvBatchWriter classes exist
- Check file permissions on output directory
- Review storage backend connectivity

**Resolution:**
- Fix root cause (disk space, permissions, etc.)
- Re-run failed extraction jobs
- Monitor batch write success rate

### 3. PasswordEncryptionFailuresHigh

**Severity:** CRITICAL (P0)

**Immediate Actions:**
1. Check if CryptoUtil is properly configured:
   ```bash
   kubectl exec -it jivs-backend-pod -- env | grep ENCRYPTION_KEY
   ```
2. Review password encryption failures in logs:
   ```bash
   kubectl logs -l app=jivs-backend | grep "password encryption failed"
   ```
3. **DO NOT allow new data source creation** until fixed

**Investigation:**
- Verify CryptoUtil bean is initialized
- Check encryption key rotation status
- Review recent config changes

**Resolution:**
- Fix CryptoUtil configuration
- Re-encrypt failed passwords manually if needed
- Test with new data source creation

### 4. ConnectionPoolExhausted

**Severity:** CRITICAL (P1)

**Immediate Actions:**
1. Check for connection leaks:
   ```bash
   kubectl exec -it jivs-backend-pod -- jstack <PID> | grep "JdbcConnector"
   ```
2. Review active extraction jobs:
   ```bash
   curl http://localhost:8080/actuator/health/extraction
   ```
3. Increase connection pool size temporarily:
   ```yaml
   spring.datasource.hikari.maximum-pool-size: 20  # increase from 10
   ```

**Investigation:**
- Check if connections are properly closed in finally blocks (should be after audit fixes)
- Look for stuck extraction jobs
- Review connection pool metrics in Grafana

**Resolution:**
- Apply resource leak fixes from audit
- Kill stuck jobs if found
- Restart pods to reclaim connections

---

## ✅ DEPLOYMENT CHECKLIST

Before deploying monitoring to production:

- [ ] ExtractionMetrics class created and integrated
- [ ] Prometheus ServiceMonitor deployed
- [ ] Grafana dashboard imported
- [ ] Alert rules configured in Prometheus
- [ ] Slack/PagerDuty webhooks tested
- [ ] Health check endpoints tested
- [ ] Log aggregation verified (ELK stack)
- [ ] Runbook reviewed by on-call team
- [ ] Alert thresholds tuned based on baseline metrics

---

## 📚 ADDITIONAL RESOURCES

**Related Documentation:**
- [EXTRACTION_MODULE_FIXES.md](EXTRACTION_MODULE_FIXES.md) - Issues being monitored
- [EXTRACTION_MODULE_DEPLOYMENT_RUNBOOK.md](EXTRACTION_MODULE_DEPLOYMENT_RUNBOOK.md) - Deployment procedures
- [EXTRACTION_QUICK_START.md](../EXTRACTION_QUICK_START.md) - Developer guide

**External Resources:**
- [Prometheus Best Practices](https://prometheus.io/docs/practices/)
- [Grafana Dashboard Design](https://grafana.com/docs/grafana/latest/best-practices/)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

**Last Updated:** 2025-10-26
**Maintained By:** DevOps + Extraction Module Team
**Review Frequency:** Monthly or after major incidents
