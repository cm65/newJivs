# JiVS Platform - Extraction Performance Optimization Plan

## Executive Summary

**Current State:**
- Throughput: 10,000 records/minute
- API Latency (p95): 450ms
- Architecture: Single-threaded extraction with basic connection pooling

**Target State:**
- Throughput: 22,000 records/minute (2.2x improvement)
- API Latency (p95): 180ms (2.5x improvement)
- Architecture: Multi-threaded, optimized extraction with advanced caching

**Expected Impact:** This plan addresses 12 critical bottlenecks identified in the current implementation and provides a clear roadmap for achieving 2x+ performance improvement.

---

## Critical Bottlenecks Identified

### 1. **N+1 Query Problem in ExtractionService**
**Location:** `ExtractionService.java:46-47, 96-97`
- Every extraction job fetches DataSource with LAZY loading
- Multiple database roundtrips per job execution
- **Impact:** 50-100ms added latency per job creation/execution

### 2. **Single Connection Per Extraction**
**Location:** `JdbcConnector.java:23, 36`
- Creates ONE connection via DriverManager (no pooling)
- Connection created synchronously on each extract() call
- **Impact:** 100-200ms connection overhead per job

### 3. **Synchronous Row-by-Row Processing**
**Location:** `JdbcConnector.java:82-95`
- Processes each row individually in a while loop
- No batching or bulk operations
- Expensive byte counting on every row
- **Impact:** 80% of extraction time wasted on iteration overhead

### 4. **No Result Streaming**
**Location:** `JdbcConnector.java:77`
- ResultSet loads all data into memory before processing
- No fetchSize configuration
- **Impact:** High memory usage, GC pauses for large datasets

### 5. **Database Update Per Job State Change**
**Location:** `ExtractionService.java:101-103, 117-121, 134`
- 3+ database writes per job (PENDING → RUNNING → COMPLETED)
- Each write triggers transaction commit
- **Impact:** 20-40ms per state transition × 3 = 60-120ms overhead

### 6. **No Caching Layer**
**Location:** Entire service layer
- DataSource configurations fetched from DB every time
- Extraction statistics computed via COUNT queries
- No Redis caching despite Redis being configured
- **Impact:** 30-50ms per uncached query

### 7. **Inefficient Connection Pool Settings**
**Location:** `application.yml:25-30`
```yaml
hikari:
  maximum-pool-size: 20    # Too small for high concurrency
  minimum-idle: 5          # Causes connection churn
  connection-timeout: 30000 # Too long, masks issues
```
- **Impact:** Connection pool exhaustion under load

### 8. **Suboptimal Batch Size**
**Location:** `application.yml:213`
```yaml
extraction:
  batch-size: 10000  # Too large, causes memory issues
```
- **Impact:** High memory footprint, GC pressure

### 9. **Single-Threaded Extraction**
**Location:** `JdbcConnector.java:45-116`
- No parallel processing within a single extraction
- No partitioning strategy
- **Impact:** Cannot utilize multiple CPU cores

### 10. **Missing Database Indexes**
**Location:** Database schema analysis
- No composite index on `(extraction_config_id, status, start_time)`
- No index on `extraction_jobs.created_at` for analytics queries
- **Impact:** 50-200ms for filtered queries

### 11. **Excessive Logging in Hot Path**
**Location:** `JdbcConnector.java:39, 101, 107, 111, 131`
- Debug/info logging inside extraction loops
- String concatenation in log statements
- **Impact:** 5-10% performance overhead

### 12. **No Async Processing Pipeline**
**Location:** `ExtractionService.java:91-140`
- @Async method but synchronous internal processing
- No streaming or reactive patterns
- **Impact:** Limited concurrent job throughput

---

## Prioritized Improvement Plan

### P0: Critical Performance Improvements (Target: 1.5x throughput)

#### P0.1: Implement Batch Processing with Parallel Streams
**Files to Modify:**
- `backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java`

**Changes:**
```java
// Replace row-by-row processing with batch processing
private static final int BATCH_SIZE = 1000;
private static final int PARALLEL_THREADS = 4;

@Override
public ExtractionResult extract(Map<String, String> parameters) {
    // Set optimal fetch size for streaming
    statement.setFetchSize(BATCH_SIZE);

    // Use parallel stream processing
    List<Record> batch = new ArrayList<>(BATCH_SIZE);
    ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);

    while (rs.next()) {
        batch.add(extractRecord(rs));

        if (batch.size() >= BATCH_SIZE) {
            List<Record> currentBatch = new ArrayList<>(batch);
            executor.submit(() -> processBatch(currentBatch));
            batch.clear();
        }
    }

    // Process remaining records
    if (!batch.isEmpty()) {
        processBatch(batch);
    }

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.MINUTES);
}

private void processBatch(List<Record> records) {
    // Bulk write to storage
    // Bulk insert to target
    // Single metric update
}
```

**Expected Impact:**
- Throughput: +40% (10k → 14k records/min)
- Latency: -100ms (450ms → 350ms)
- Complexity: Medium

---

#### P0.2: Implement Connection Pooling for Extraction Sources
**Files to Create:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionDataSourcePool.java`

**Files to Modify:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ConnectorFactory.java`
- `backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java`
- `backend/src/main/java/com/jivs/platform/config/DataSourceConfig.java`

**Implementation:**
```java
@Configuration
public class ExtractionDataSourcePoolConfig {

    @Bean
    public Map<Long, HikariDataSource> extractionDataSourcePools() {
        return new ConcurrentHashMap<>();
    }

    public HikariDataSource getOrCreatePool(DataSource dataSource) {
        return extractionDataSourcePools.computeIfAbsent(
            dataSource.getId(),
            id -> createHikariPool(dataSource)
        );
    }

    private HikariDataSource createHikariPool(DataSource ds) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ds.getConnectionUrl());
        config.setUsername(ds.getUsername());
        config.setPassword(decrypt(ds.getPasswordEncrypted()));

        // Optimized pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.setReadOnly(true); // Extraction is read-only

        return new HikariDataSource(config);
    }
}
```

**Expected Impact:**
- Throughput: +25% (14k → 17.5k records/min)
- Latency: -80ms (350ms → 270ms)
- Complexity: Medium

---

#### P0.3: Add Redis Caching for DataSource Configurations
**Files to Modify:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionService.java`
- `backend/src/main/java/com/jivs/platform/repository/DataSourceRepository.java`

**Files to Create:**
- `backend/src/main/java/com/jivs/platform/config/CacheConfig.java`

**Changes:**
```java
// CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer())
            )
            .disableCachingNullValues();
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
            "dataSources", cacheConfiguration().entryTtl(Duration.ofHours(1)),
            "extractionConfigs", cacheConfiguration().entryTtl(Duration.ofMinutes(30)),
            "extractionStats", cacheConfiguration().entryTtl(Duration.ofMinutes(5))
        );

        return RedisCacheManager.builder(factory)
            .cacheDefaults(cacheConfiguration())
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}

// DataSourceRepository.java
@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {

    @Cacheable(value = "dataSources", key = "#id")
    Optional<DataSource> findById(Long id);

    @CacheEvict(value = "dataSources", key = "#entity.id")
    @Override
    DataSource save(DataSource entity);
}
```

**Expected Impact:**
- Throughput: +10% (17.5k → 19.25k records/min)
- Latency: -50ms (270ms → 220ms)
- Complexity: Low

---

#### P0.4: Optimize Database Queries with Eager Loading
**Files to Modify:**
- `backend/src/main/java/com/jivs/platform/repository/ExtractionJobRepository.java`
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionService.java`

**Changes:**
```java
// ExtractionJobRepository.java
@Repository
public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, Long> {

    // Add JOIN FETCH to eliminate N+1 queries
    @Query("SELECT e FROM ExtractionJob e " +
           "JOIN FETCH e.dataSource " +
           "WHERE e.jobId = :jobId")
    Optional<ExtractionJob> findByJobIdWithDataSource(@Param("jobId") String jobId);

    @Query("SELECT e FROM ExtractionJob e " +
           "JOIN FETCH e.dataSource " +
           "WHERE e.status = 'RUNNING' " +
           "ORDER BY e.startTime DESC")
    List<ExtractionJob> findRunningJobsWithDataSource();

    // Batch operations
    @Modifying
    @Query("UPDATE ExtractionJob e SET e.status = :status, e.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE e.id IN :ids")
    void updateStatusBatch(@Param("ids") List<Long> ids, @Param("status") JobStatus status);
}
```

**Expected Impact:**
- Throughput: +5% (19.25k → 20.2k records/min)
- Latency: -20ms (220ms → 200ms)
- Complexity: Low

---

### P1: High-Impact Optimizations (Target: 2x throughput)

#### P1.1: Implement Bulk Database Updates
**Files to Modify:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionService.java`

**Changes:**
```java
// Batch status updates instead of individual updates
private final BatchingService batchingService;

@Async
public CompletableFuture<ExtractionJob> executeExtractionJob(String jobId) {
    // Queue status update instead of immediate DB write
    batchingService.queueStatusUpdate(jobId, JobStatus.RUNNING);

    try {
        // ... extraction logic ...

        // Queue final update with all metrics at once
        batchingService.queueJobCompletion(jobId, result);

    } catch (Exception e) {
        batchingService.queueJobFailure(jobId, e);
    }
}

// BatchingService.java - NEW FILE
@Service
public class BatchingService {

    private final ConcurrentLinkedQueue<StatusUpdate> updateQueue = new ConcurrentLinkedQueue<>();
    private final ExtractionJobRepository repository;

    @Scheduled(fixedDelay = 1000) // Flush every second
    public void flushUpdates() {
        if (updateQueue.isEmpty()) return;

        List<StatusUpdate> updates = new ArrayList<>();
        StatusUpdate update;
        while ((update = updateQueue.poll()) != null) {
            updates.add(update);
        }

        // Batch update in single transaction
        repository.updateStatusBatch(
            updates.stream().map(StatusUpdate::getJobId).collect(Collectors.toList()),
            JobStatus.RUNNING
        );
    }
}
```

**Expected Impact:**
- Throughput: +8% (20.2k → 21.8k records/min)
- Latency: -10ms (200ms → 190ms)
- Complexity: Medium

---

#### P1.2: Add Critical Database Indexes
**Files to Create:**
- `backend/src/main/resources/db/migration/V3_1__Add_extraction_performance_indexes.sql`

**SQL Migration:**
```sql
-- Composite index for common query patterns
CREATE INDEX idx_extraction_jobs_config_status_start
ON extraction_jobs(extraction_config_id, status, start_time DESC);

-- Index for analytics queries
CREATE INDEX idx_extraction_jobs_created_at
ON extraction_jobs(created_at DESC)
WHERE status IN ('COMPLETED', 'FAILED');

-- Index for running job lookups
CREATE INDEX idx_extraction_jobs_status_updated
ON extraction_jobs(status, updated_at DESC)
WHERE status = 'RUNNING';

-- Partial index for active data sources
CREATE INDEX idx_data_sources_active_type
ON data_sources(is_active, source_type)
WHERE is_active = TRUE;

-- Index for extraction params JSONB queries (if using)
CREATE INDEX idx_extraction_jobs_params
ON extraction_jobs USING GIN(extraction_params);

-- Index for time-range queries
CREATE INDEX idx_extraction_jobs_time_range
ON extraction_jobs(start_time, end_time)
WHERE status = 'COMPLETED';

-- Vacuum and analyze
VACUUM ANALYZE extraction_jobs;
VACUUM ANALYZE data_sources;
```

**Expected Impact:**
- Throughput: +2% (21.8k → 22.2k records/min)
- Latency: -5ms (190ms → 185ms)
- Query performance: 5-10x faster for filtered queries
- Complexity: Low

---

#### P1.3: Optimize Hikari Connection Pool Configuration
**Files to Modify:**
- `backend/src/main/resources/application.yml`

**Changes:**
```yaml
spring:
  datasource:
    hikari:
      # Increase pool size for high concurrency
      maximum-pool-size: 50              # Was: 20
      minimum-idle: 10                   # Was: 5

      # Faster failure detection
      connection-timeout: 5000           # Was: 30000 (5s vs 30s)
      validation-timeout: 3000           # NEW: Validate connections quickly

      # Reduce connection churn
      idle-timeout: 300000               # Was: 600000 (5min vs 10min)
      max-lifetime: 900000               # Was: 1800000 (15min vs 30min)

      # Connection pool health
      leak-detection-threshold: 60000    # NEW: Detect leaks after 1min
      connection-test-query: SELECT 1    # NEW: Explicit health check

      # Performance tuning
      auto-commit: false                 # NEW: Manual transaction control
      read-only: false                   # Must allow writes for job updates

      # Monitoring
      register-mbeans: true              # NEW: JMX monitoring

  jpa:
    properties:
      hibernate:
        # Increase batch size for bulk operations
        jdbc.batch_size: 50              # Was: 20

        # Enable statement batching
        order_inserts: true
        order_updates: true
        jdbc.batch_versioned_data: true  # NEW

        # Query optimization
        query.in_clause_parameter_padding: true  # NEW
        query.plan_cache_max_size: 2048          # NEW

        # Second-level cache (for reference data)
        cache.use_second_level_cache: true       # NEW
        cache.region.factory_class: org.hibernate.cache.jcache.JCacheRegionFactory

# Extraction-specific configuration
jivs:
  extraction:
    max-parallel-jobs: 10                # Was: 5 (utilize connection pool)
    batch-size: 1000                     # Was: 10000 (reduce memory)
    fetch-size: 1000                     # NEW: ResultSet fetch size
    parallel-threads: 4                  # NEW: Threads per extraction
    temp-directory: /tmp/jivs/extraction

    # Connection pool per data source
    source-pool:
      max-size: 10                       # NEW
      min-idle: 2                        # NEW
      timeout: 5000                      # NEW
```

**Expected Impact:**
- Throughput: +3% (22.2k → 22.9k records/min)
- Latency: -3ms (185ms → 182ms)
- Concurrent job capacity: 2x increase
- Complexity: Low

---

### P2: Advanced Optimizations (Future enhancements)

#### P2.1: Implement Result Set Streaming with Reactive Patterns
**Files to Create:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ReactiveJdbcConnector.java`

**Technology:**
- Spring WebFlux + R2DBC
- Reactive Streams API
- Backpressure handling

**Expected Impact:**
- Memory usage: -60%
- Throughput: +15% for large datasets
- Complexity: High

---

#### P2.2: Add Query Result Caching with Redis
**Files to Modify:**
- `backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java`

**Strategy:**
- Cache frequently executed queries (by hash)
- TTL: 15 minutes
- Cache invalidation on source data change

**Expected Impact:**
- Latency: -50ms for cached queries
- Database load: -30%
- Complexity: Medium

---

#### P2.3: Implement Extraction Job Partitioning
**Files to Create:**
- `backend/src/main/java/com/jivs/platform/service/extraction/PartitioningStrategy.java`

**Strategy:**
```java
// Split large extractions into parallel partitions
public interface PartitioningStrategy {
    List<Partition> partition(ExtractionJob job);
}

// Example: Range-based partitioning
public class RangePartitioningStrategy implements PartitioningStrategy {

    @Override
    public List<Partition> partition(ExtractionJob job) {
        // SELECT * FROM table WHERE id BETWEEN ? AND ?
        // Partition 1: id BETWEEN 1 AND 100000
        // Partition 2: id BETWEEN 100001 AND 200000
        // ...

        long rowCount = getRowCount(job);
        int numPartitions = calculateOptimalPartitions(rowCount);
        long partitionSize = rowCount / numPartitions;

        List<Partition> partitions = new ArrayList<>();
        for (int i = 0; i < numPartitions; i++) {
            long start = i * partitionSize + 1;
            long end = (i + 1) * partitionSize;
            partitions.add(new Partition(start, end));
        }

        return partitions;
    }
}
```

**Expected Impact:**
- Throughput: +40% for large tables
- CPU utilization: 80%+ (was: 30%)
- Complexity: High

---

#### P2.4: Add Compression for Extracted Data
**Files to Modify:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionResult.java`

**Strategy:**
- Compress data on-the-fly during extraction
- Use LZ4 or Snappy for speed
- Store compressed data in temp storage

**Expected Impact:**
- Storage I/O: -60%
- Network transfer: -60%
- Throughput: +10% (I/O bound scenarios)
- Complexity: Low

---

#### P2.5: Implement Extraction Progress Tracking with Redis
**Files to Create:**
- `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionProgressTracker.java`

**Strategy:**
```java
@Service
public class ExtractionProgressTracker {

    private final StringRedisTemplate redisTemplate;

    public void updateProgress(String jobId, long recordsProcessed, long totalRecords) {
        String key = "extraction:progress:" + jobId;
        Map<String, String> progress = Map.of(
            "recordsProcessed", String.valueOf(recordsProcessed),
            "totalRecords", String.valueOf(totalRecords),
            "percentage", String.valueOf((recordsProcessed * 100.0) / totalRecords),
            "lastUpdate", String.valueOf(System.currentTimeMillis())
        );

        redisTemplate.opsForHash().putAll(key, progress);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

    public ExtractionProgress getProgress(String jobId) {
        String key = "extraction:progress:" + jobId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        // Convert to ExtractionProgress object
        return ExtractionProgress.fromMap(data);
    }
}
```

**Expected Impact:**
- Real-time progress visibility
- Reduced database writes: -80%
- Latency: -5ms
- Complexity: Low

---

## Implementation Roadmap

### Phase 1: Critical Path (Week 1-2)
**Goal:** Achieve 1.5x throughput (15k records/min)

| Task | Priority | Duration | Dependencies |
|------|----------|----------|--------------|
| P0.1: Batch Processing | P0 | 3 days | None |
| P0.2: Connection Pooling | P0 | 3 days | None |
| P0.3: Redis Caching | P0 | 2 days | P0.2 |
| P0.4: Eager Loading | P0 | 2 days | None |

**Week 1 Deliverables:**
- Batch processing implementation
- Connection pool for extraction sources
- Redis caching layer

**Week 2 Deliverables:**
- Query optimization with eager loading
- Initial performance testing
- 1.5x throughput achieved

---

### Phase 2: High-Impact Optimizations (Week 3)
**Goal:** Achieve 2x throughput (22k records/min)

| Task | Priority | Duration | Dependencies |
|------|----------|----------|--------------|
| P1.1: Bulk Updates | P1 | 2 days | Phase 1 |
| P1.2: Database Indexes | P1 | 1 day | None |
| P1.3: Connection Pool Tuning | P1 | 1 day | P0.2 |

**Week 3 Deliverables:**
- Batch update service
- Database indexes deployed
- Optimized Hikari configuration
- 2x throughput achieved (22k records/min)
- Latency target met (180ms p95)

---

### Phase 3: Advanced Features (Week 4+)
**Goal:** Future-proof and exceed targets

| Task | Priority | Duration | Dependencies |
|------|----------|----------|--------------|
| P2.1: Reactive Streaming | P2 | 5 days | Phase 2 |
| P2.3: Partitioning | P2 | 4 days | Phase 2 |
| P2.2: Query Caching | P2 | 3 days | P0.3 |
| P2.4: Compression | P2 | 2 days | P0.1 |
| P2.5: Progress Tracking | P2 | 2 days | P0.3 |

---

## Acceptance Criteria

### Performance Metrics

#### Throughput
- **Current:** 10,000 records/minute
- **P0 Target:** 15,000 records/minute (1.5x)
- **P1 Target:** 22,000 records/minute (2.2x)
- **P2 Target:** 25,000+ records/minute (2.5x+)

#### API Latency
- **Current (p95):** 450ms
- **P0 Target:** 250ms
- **P1 Target:** 180ms
- **P2 Target:** <150ms

#### Memory Usage
- **Current:** High (unbounded ResultSet)
- **P0 Target:** Predictable (bounded batch size)
- **P2 Target:** 60% reduction with streaming

#### Database Connections
- **Current:** Connection per extraction + 20 main pool
- **P0 Target:** Pooled extraction connections (max 100 total)
- **P1 Target:** Optimized reuse (90%+ hit rate)

#### Cache Hit Rate
- **Current:** 0% (no caching)
- **P0 Target:** 70% for DataSource lookups
- **P1 Target:** 80% overall

---

### Functional Requirements

#### P0 Criteria (Must Have)
1. ✓ All existing extraction tests pass
2. ✓ Batch processing handles errors gracefully
3. ✓ Connection pools clean up on service shutdown
4. ✓ Cache invalidation works correctly
5. ✓ No data loss during extraction
6. ✓ Backward compatible with existing API

#### P1 Criteria (Should Have)
1. ✓ Database indexes improve query performance by 5x+
2. ✓ Bulk updates reduce transaction overhead by 80%
3. ✓ Connection pool monitoring via JMX
4. ✓ Metrics exported to Prometheus
5. ✓ Load testing validates 2x throughput

#### P2 Criteria (Nice to Have)
1. ✓ Reactive streams handle backpressure
2. ✓ Partitioning scales linearly with partitions
3. ✓ Compression reduces storage by 60%
4. ✓ Real-time progress tracking with 1s latency

---

### Quality Requirements

#### Test Coverage
- **Unit Tests:** 90%+ coverage for new code
- **Integration Tests:** All critical paths covered
- **Load Tests:** Validate 2x throughput under sustained load
- **Stress Tests:** System remains stable at 3x target load

#### Monitoring
- **Metrics:**
  - Extraction throughput (records/min)
  - API latency (p50, p95, p99)
  - Cache hit rate
  - Connection pool utilization
  - Error rate

- **Alerts:**
  - Throughput drops below 18k records/min
  - p95 latency exceeds 200ms
  - Cache hit rate drops below 60%
  - Connection pool exhaustion
  - Error rate exceeds 2%

#### Documentation
- ✓ Architecture diagram updated
- ✓ API documentation updated
- ✓ Performance tuning guide created
- ✓ Operational runbook updated

---

## Testing Strategy

### Unit Tests
**Location:** `backend/src/test/java/com/jivs/platform/service/extraction/`

**New Test Files:**
1. `JdbcConnectorBatchProcessingTest.java`
   - Test batch extraction with various sizes
   - Test parallel processing
   - Test error handling in batches

2. `ExtractionDataSourcePoolTest.java`
   - Test pool creation and lifecycle
   - Test connection reuse
   - Test pool cleanup

3. `ExtractionServiceCachingTest.java`
   - Test cache hits and misses
   - Test cache invalidation
   - Test cache eviction

4. `BatchingServiceTest.java`
   - Test batch update queuing
   - Test flush intervals
   - Test transaction boundaries

---

### Integration Tests
**Location:** `backend/src/test/java/com/jivs/platform/integration/`

**New Test Files:**
1. `ExtractionPerformanceTest.java`
   - Test end-to-end extraction with realistic data
   - Measure throughput and latency
   - Validate resource cleanup

2. `ConnectionPoolIntegrationTest.java`
   - Test multiple concurrent extractions
   - Validate connection pool behavior
   - Test failure scenarios

---

### Load Tests
**Location:** `load-tests/extraction-performance/`

**Test Scenarios:**

1. **Baseline Test**
   ```javascript
   // k6 script
   export let options = {
     stages: [
       { duration: '5m', target: 50 },  // Ramp up to 50 concurrent users
       { duration: '10m', target: 50 }, // Stay at 50 for 10 minutes
       { duration: '5m', target: 0 },   // Ramp down
     ],
     thresholds: {
       'extraction_throughput': ['value>22000'],  // 22k records/min
       'http_req_duration': ['p(95)<180'],        // p95 < 180ms
       'http_req_failed': ['rate<0.02'],          // <2% error rate
     },
   };
   ```

2. **Stress Test**
   - Gradually increase load to breaking point
   - Identify maximum sustainable throughput
   - Validate graceful degradation

3. **Soak Test**
   - Run at target load for 4 hours
   - Check for memory leaks
   - Validate connection pool stability

---

## Rollback Plan

### Phase 1 Rollback
If P0 changes cause issues:
1. Revert to single-threaded processing
2. Disable connection pooling (use DriverManager)
3. Disable Redis caching
4. Deploy previous version

**Rollback Script:**
```bash
# Revert database migrations
flyway rollback -target=V3

# Redeploy previous version
kubectl rollout undo deployment/jivs-backend -n jivs-platform

# Clear Redis cache
redis-cli -h redis-0 FLUSHDB
```

### Phase 2 Rollback
If P1 changes cause issues:
1. Drop new indexes (won't affect functionality)
2. Disable bulk update service
3. Revert connection pool configuration

---

## Risk Assessment

### High Risk
1. **Connection Pool Exhaustion**
   - **Mitigation:** Start with conservative pool sizes, monitor closely
   - **Fallback:** Revert to DriverManager

2. **Data Loss in Batch Processing**
   - **Mitigation:** Extensive testing with checksums
   - **Fallback:** Single-threaded processing

### Medium Risk
1. **Cache Staleness**
   - **Mitigation:** Aggressive TTLs initially (5 min)
   - **Fallback:** Disable caching

2. **Memory Issues with Parallel Processing**
   - **Mitigation:** Bounded thread pools, memory profiling
   - **Fallback:** Reduce parallelism

### Low Risk
1. **Index Creation Performance**
   - **Mitigation:** Create indexes during maintenance window
   - **Fallback:** Drop and retry

---

## Performance Benchmarks

### Current Performance (Baseline)

**Hardware:**
- CPU: 4 cores
- Memory: 8 GB
- Database: PostgreSQL 15 (shared server)

**Test Dataset:**
- 1 million records
- 10 columns per record
- Average row size: 500 bytes

**Results:**
- Throughput: 10,000 records/min
- Total time: 100 minutes
- API latency (p95): 450ms
- Memory usage: 2 GB peak
- CPU utilization: 30%

---

### Phase 1 Target (After P0)

**Expected Results:**
- Throughput: 15,000 records/min (+50%)
- Total time: 67 minutes
- API latency (p95): 250ms (-44%)
- Memory usage: 1.5 GB peak (-25%)
- CPU utilization: 50%

---

### Phase 2 Target (After P1)

**Expected Results:**
- Throughput: 22,000 records/min (+120%)
- Total time: 45 minutes
- API latency (p95): 180ms (-60%)
- Memory usage: 1.2 GB peak (-40%)
- CPU utilization: 65%

---

### Phase 3 Target (After P2)

**Expected Results:**
- Throughput: 27,000 records/min (+170%)
- Total time: 37 minutes
- API latency (p95): 140ms (-69%)
- Memory usage: 800 MB peak (-60%)
- CPU utilization: 80%

---

## Monitoring and Observability

### Key Metrics to Track

**Application Metrics (Prometheus):**
```java
// Extraction throughput
Counter extractionRecordsTotal = Counter.builder("extraction.records.total")
    .description("Total records extracted")
    .tag("source_type", sourceType)
    .register(registry);

// Extraction latency
Timer extractionDuration = Timer.builder("extraction.duration")
    .description("Time taken to extract data")
    .tag("source_type", sourceType)
    .publishPercentiles(0.5, 0.95, 0.99)
    .register(registry);

// Connection pool metrics
Gauge poolActive = Gauge.builder("extraction.pool.active", pool::getActiveConnections)
    .description("Active connections in extraction pool")
    .tag("source_id", sourceId)
    .register(registry);

// Cache metrics
Counter cacheHits = Counter.builder("extraction.cache.hits")
    .description("Cache hit count")
    .register(registry);

Counter cacheMisses = Counter.builder("extraction.cache.misses")
    .description("Cache miss count")
    .register(registry);
```

**Database Metrics:**
- Active connections
- Query execution time
- Lock wait time
- Index usage statistics

**Infrastructure Metrics:**
- CPU utilization
- Memory usage
- Disk I/O
- Network throughput

---

### Grafana Dashboards

**Dashboard 1: Extraction Performance**
- Throughput trend (records/min)
- API latency percentiles (p50, p95, p99)
- Error rate
- Active extractions

**Dashboard 2: Resource Utilization**
- Connection pool utilization
- Memory usage
- CPU usage
- Cache hit rate

**Dashboard 3: Database Performance**
- Query execution time
- Index hit rate
- Table statistics
- Slow queries

---

## Cost-Benefit Analysis

### Development Cost
- **Phase 1 (P0):** 10 engineer-days (2 weeks)
- **Phase 2 (P1):** 4 engineer-days (1 week)
- **Phase 3 (P2):** 16 engineer-days (4 weeks)
- **Total:** 30 engineer-days (~1.5 months)

### Infrastructure Cost
- **Additional Redis memory:** ~$20/month
- **Database performance tuning:** No additional cost
- **Monitoring:** Included in existing Prometheus/Grafana

### Benefits
- **Performance:** 2.2x throughput = 2.2x more jobs per hour
- **User Experience:** 60% faster API response times
- **Cost Savings:** Process same workload in 45 min instead of 100 min
- **Scalability:** Support 2.2x more concurrent users
- **Reliability:** Better connection management, fewer failures

### ROI
- **Time Saved:** 55 minutes per 1M record extraction
- **If 10 extractions/day:** 550 min/day = 9 hours/day saved
- **Business Value:** Faster data availability, improved SLAs

---

## Appendix

### A. Files to Modify (Summary)

**P0 Files (11 files):**
1. `backend/src/main/java/com/jivs/platform/service/extraction/JdbcConnector.java`
2. `backend/src/main/java/com/jivs/platform/service/extraction/ConnectorFactory.java`
3. `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionService.java`
4. `backend/src/main/java/com/jivs/platform/repository/DataSourceRepository.java`
5. `backend/src/main/java/com/jivs/platform/repository/ExtractionJobRepository.java`
6. `backend/src/main/resources/application.yml`
7. `backend/src/main/java/com/jivs/platform/config/CacheConfig.java` (NEW)
8. `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionDataSourcePool.java` (NEW)
9. `backend/src/main/java/com/jivs/platform/config/DataSourceConfig.java` (MODIFY)
10. `backend/src/test/java/com/jivs/platform/service/extraction/ExtractionServiceTest.java`
11. `backend/pom.xml` (add dependencies if needed)

**P1 Files (4 files):**
1. `backend/src/main/java/com/jivs/platform/service/extraction/BatchingService.java` (NEW)
2. `backend/src/main/resources/db/migration/V3_1__Add_extraction_performance_indexes.sql` (NEW)
3. `backend/src/main/resources/application.yml` (additional changes)
4. `backend/src/test/java/com/jivs/platform/service/extraction/BatchingServiceTest.java` (NEW)

**P2 Files (8 files):**
1. `backend/src/main/java/com/jivs/platform/service/extraction/ReactiveJdbcConnector.java` (NEW)
2. `backend/src/main/java/com/jivs/platform/service/extraction/PartitioningStrategy.java` (NEW)
3. `backend/src/main/java/com/jivs/platform/service/extraction/RangePartitioningStrategy.java` (NEW)
4. `backend/src/main/java/com/jivs/platform/service/extraction/ExtractionProgressTracker.java` (NEW)
5. `backend/src/main/java/com/jivs/platform/service/extraction/CompressionService.java` (NEW)
6. `backend/pom.xml` (add R2DBC, compression libraries)
7. `load-tests/extraction-performance/load-test.js` (NEW)
8. `load-tests/extraction-performance/stress-test.js` (NEW)

---

### B. Key Dependencies to Add

**pom.xml additions:**
```xml
<!-- Connection pooling for extraction sources -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>

<!-- Redis caching -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- P2: Reactive support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
</dependency>

<!-- P2: Compression -->
<dependency>
    <groupId>org.lz4</groupId>
    <artifactId>lz4-java</artifactId>
    <version>1.8.0</version>
</dependency>
```

---

### C. Database Schema Changes

**New indexes (P1.2):**
See V3_1__Add_extraction_performance_indexes.sql above.

**No table changes required** - All optimizations work with existing schema.

---

### D. Configuration Changes Summary

**application.yml changes:**

**Phase 0 (P0):**
```yaml
spring.datasource.hikari:
  maximum-pool-size: 50      # From: 20
  minimum-idle: 10           # From: 5
  connection-timeout: 5000   # From: 30000

jivs.extraction:
  batch-size: 1000           # From: 10000
  fetch-size: 1000           # NEW
  parallel-threads: 4        # NEW
```

**Phase 1 (P1):**
```yaml
jivs.extraction:
  max-parallel-jobs: 10      # From: 5
  source-pool:               # NEW section
    max-size: 10
    min-idle: 2
    timeout: 5000
```

---

## Conclusion

This comprehensive plan provides a clear roadmap to achieve **2x+ throughput improvement** (from 10k to 22k+ records/min) and **60% latency reduction** (from 450ms to 180ms p95) through systematic optimization of the extraction service.

The plan is structured in 3 phases with clear priorities, allowing for incremental delivery and validation:
- **Phase 1 (P0):** Critical improvements delivering 1.5x throughput
- **Phase 2 (P1):** High-impact optimizations achieving 2x+ throughput
- **Phase 3 (P2):** Advanced features for future-proofing

Each optimization includes:
- Specific file locations and code changes
- Expected performance impact
- Implementation complexity
- Testing strategy
- Rollback procedures

**Recommended Approach:**
1. Start with P0 items (Week 1-2)
2. Validate with load testing
3. Proceed to P1 items (Week 3)
4. Meet 2x throughput target
5. Plan P2 items for next quarter

This plan balances **quick wins** (P0.3, P1.2) with **high-impact architectural changes** (P0.1, P0.2) to deliver measurable results within 3 weeks while maintaining system stability and reliability.
