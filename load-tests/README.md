# JiVS Platform - Extraction Performance Load Tests

This directory contains comprehensive k6 load tests for validating extraction performance optimizations.

## Overview

These tests validate the performance improvements implemented as part of the extraction optimization initiative, which aims to achieve:
- **2x throughput improvement** (10k → 20k records/min)
- **56% latency reduction** (450ms → 200ms p95)
- **< 1% error rate**

## Test Suite

### 1. Baseline Test (`extraction-baseline-test.js`)
**Purpose:** Establish current performance metrics before optimization

**Load Profile:**
- 5-10 concurrent users
- 5 minute duration
- Moderate data sizes (10k records)

**Thresholds:**
- p95 latency < 600ms
- Error rate < 5%

**Usage:**
```bash
k6 run load-tests/extraction-baseline-test.js
```

### 2. Performance Test (`extraction-performance-test.js`)
**Purpose:** Validate optimized system performance

**Load Profile:**
- 100 concurrent users (peak)
- 18 minute duration with ramp up/down
- Various data sizes: 1k, 10k, 50k, 100k records
- Multiple source types: JDBC, File, API

**Thresholds:**
- p95 latency < 250ms (target: 200ms)
- p99 latency < 500ms
- Throughput > 20k records/min
- Error rate < 1%

**Usage:**
```bash
k6 run load-tests/extraction-performance-test.js
```

### 3. Stress Test (`extraction-stress-test.js`)
**Purpose:** Find system breaking point

**Load Profile:**
- Gradual increase: 10 → 50 → 100 → 200 → 500 VUs
- 21 minute duration
- Goal: Identify maximum sustainable load

**Thresholds:**
- Relaxed (expects degradation at limits)
- p95 latency < 2000ms
- Error rate < 25% (at peak)

**Usage:**
```bash
k6 run load-tests/extraction-stress-test.js
k6 run --env MAX_VUS=1000 load-tests/extraction-stress-test.js  # Custom max
```

### 4. Soak Test (`extraction-soak-test.js`)
**Purpose:** Long-term stability and memory leak detection

**Load Profile:**
- Constant 50 VUs
- 2 hour duration (default)
- Monitors: memory leaks, connection pools, cache behavior

**Thresholds:**
- Latency should remain stable over time
- Error rate < 3%
- No performance degradation

**Usage:**
```bash
k6 run load-tests/extraction-soak-test.js                    # 2 hours
k6 run --env DURATION=4h load-tests/extraction-soak-test.js  # 4 hours
k6 run --env DURATION=24h load-tests/extraction-soak-test.js # Full endurance
```

### 5. Spike Test (`extraction-spike-test.js`)
**Purpose:** Validate system recovery from sudden load spikes

**Load Profile:**
- Normal: 20 VUs
- Spike: 200 VUs (10x increase)
- 3 spike cycles with recovery periods

**Thresholds:**
- Latency during spike < 2000ms
- Latency after recovery < 500ms
- Error rate < 5%

**Usage:**
```bash
k6 run load-tests/extraction-spike-test.js
k6 run --env SPIKE_MULTIPLIER=15 load-tests/extraction-spike-test.js  # 15x spike
```

## Quick Start

### Prerequisites

1. **Install k6:**
```bash
# macOS
brew install k6

# Linux (Debian/Ubuntu)
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6

# Windows
choco install k6
```

2. **Verify k6 installation:**
```bash
k6 version
```

### Setup Test Environment

1. **Run the setup script:**
```bash
./load-tests/setup-test-data.sh
```

This script will:
- Create test database tables
- Populate test data (1k, 10k, 50k, 100k records)
- Create test user account
- Validate API connectivity
- Generate `.env.test` configuration file

2. **Review test configuration:**
```bash
cat ./load-tests/.env.test
```

### Run Tests

**Option 1: Run all tests (recommended)**
```bash
./load-tests/run-extraction-tests.sh
```

**Option 2: Run specific test**
```bash
./load-tests/run-extraction-tests.sh --baseline      # Baseline only
./load-tests/run-extraction-tests.sh --performance   # Performance only
./load-tests/run-extraction-tests.sh --stress        # Stress only
./load-tests/run-extraction-tests.sh --spike         # Spike only
./load-tests/run-extraction-tests.sh --soak          # Soak only (long)
```

**Option 3: Run all including soak test**
```bash
./load-tests/run-extraction-tests.sh --all
```

**Option 4: Manual k6 execution**
```bash
k6 run --vus 100 --duration 10m load-tests/extraction-performance-test.js
```

### Compare Results

After running baseline and performance tests:

```bash
# Automatic comparison (if both tests ran)
./load-tests/run-extraction-tests.sh --compare

# Or use the comparison tool directly
node load-tests/compare-results.js

# Custom file comparison
node load-tests/compare-results.js \
  --baseline load-test-results/baseline-20250112.json \
  --optimized load-test-results/performance-20250112.json \
  --output my-comparison.md
```

## Environment Variables

All tests support the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `BASE_URL` | API base URL | `http://localhost:8080/api/v1` |
| `TEST_USERNAME` | Test user username | `testuser` |
| `TEST_PASSWORD` | Test user password | `TestPassword123!` |
| `EXTRACTION_RECORDS` | Records per extraction | `10000` |

**Example:**
```bash
BASE_URL=http://staging.jivs.com/api/v1 \
TEST_USERNAME=perftest \
k6 run load-tests/extraction-performance-test.js
```

## Test Results

### Results Directory

All test results are saved to `./load-test-results/` with timestamps:

```
load-test-results/
├── baseline-20250112-143022.json
├── baseline-20250112-143022-full.json
├── baseline-latest.json
├── performance-20250112-150145.json
├── performance-20250112-150145-full.json
├── performance-latest.json
├── stress-20250112-152300.json
├── spike-20250112-154500.json
├── soak-20250112-160000.json
├── comparison-report.md
└── test-report-20250112-154500.md
```

### Result Files

- `*-<timestamp>.json` - Summary metrics
- `*-<timestamp>-full.json` - Complete k6 output
- `*-latest.json` - Symlink to most recent test
- `comparison-report.md` - Baseline vs. optimized comparison
- `test-report-<timestamp>.md` - Overall test suite report

### Reading Results

**View summary:**
```bash
cat load-test-results/performance-latest.json | jq '.metrics'
```

**View comparison:**
```bash
cat load-test-results/comparison-report.md
```

**Key metrics to check:**
- `metrics.extraction.records_per_minute` - Throughput
- `metrics.extraction.creation_latency_p95` - p95 latency
- `metrics.extraction.error_rate` - Error rate
- `thresholds.*` - Pass/fail status

## Performance Targets

Based on the optimization plan (`extraction_performance_plan.md`):

| Metric | Baseline | Target | Status |
|--------|----------|--------|--------|
| Throughput | 10,000 rec/min | 20,000+ rec/min | ⏳ Testing |
| Latency (p95) | 450ms | 200ms | ⏳ Testing |
| Latency (p99) | 1000ms | 500ms | ⏳ Testing |
| Error Rate | <2% | <1% | ⏳ Testing |

### Optimization Breakdown

| Phase | Optimizations | Expected Impact |
|-------|---------------|-----------------|
| **P0** | Batch processing, connection pooling, Redis caching, query optimization | **2x throughput, 56% latency reduction** |
| P1 | Bulk updates, database indexes, pool tuning | Additional 10% improvement |
| P2 | Reactive streams, partitioning, compression | Future enhancements |

## Interpreting Results

### Success Criteria

**✅ Optimization Successful:**
- Throughput ≥ 20,000 records/min
- p95 latency ≤ 200ms
- p99 latency ≤ 500ms
- Error rate < 1%
- System stable under stress
- Clean recovery from spikes

**⚠️ Needs Review:**
- Throughput 15,000-19,999 records/min (partial improvement)
- p95 latency 200-300ms
- Error rate 1-2%
- Degradation during soak test

**❌ Optimization Failed:**
- Throughput < 15,000 records/min
- p95 latency > 300ms
- Error rate > 2%
- System crashes under stress
- Memory leaks detected

### Common Issues

**High latency:**
- Check cache hit rates (target: 70%+)
- Review database query performance
- Monitor connection pool utilization
- Check network latency

**Low throughput:**
- Increase connection pool sizes
- Verify parallel thread configuration
- Check batch size settings
- Review database indexes

**High error rate:**
- Check connection pool exhaustion
- Review timeout settings
- Monitor database locks
- Check resource limits (CPU, memory)

**Failed stress test:**
- Increase connection pool max size
- Implement rate limiting
- Add circuit breakers
- Scale infrastructure

## Advanced Usage

### Custom Test Scenarios

**Test specific data source:**
```bash
# Modify extractionConfig in the test file
# Or create custom test variant
```

**Test with production-like load:**
```bash
# Scale VUs and duration to match production patterns
k6 run --vus 500 --duration 30m load-tests/extraction-performance-test.js
```

### Output to InfluxDB/Grafana

```bash
# Send results to InfluxDB
k6 run --out influxdb=http://localhost:8086/k6 \
  load-tests/extraction-performance-test.js

# View in Grafana dashboard
```

### Continuous Integration

```bash
# CI/CD pipeline example
#!/bin/bash
set -e

# Setup
./load-tests/setup-test-data.sh --clean

# Run tests
./load-tests/run-extraction-tests.sh --performance

# Check results
if node load-tests/compare-results.js; then
  echo "Performance targets met!"
  exit 0
else
  echo "Performance targets NOT met!"
  exit 1
fi
```

## Troubleshooting

### Test Failures

**"Authentication failed during setup"**
- Verify API is running: `curl http://localhost:8080/actuator/health`
- Check credentials in `.env.test`
- Ensure test user exists

**"Cannot connect to database"**
- Verify PostgreSQL is running: `pg_isready -h localhost -p 5432`
- Check database credentials
- Ensure test data is loaded

**"k6: command not found"**
- Install k6 (see Prerequisites section)
- Verify installation: `k6 version`

**"Extraction creation failed"**
- Check extraction service logs
- Verify data source configuration
- Check connection pool availability

### Performance Issues

**Tests running too slowly:**
- Reduce data sizes in test
- Decrease number of VUs
- Shorten test duration

**Inconsistent results:**
- Run multiple iterations
- Check system resource availability
- Eliminate other background processes
- Use dedicated test environment

## Contributing

When adding new tests:

1. Follow naming convention: `extraction-*-test.js`
2. Include comprehensive thresholds
3. Add custom metrics for specific scenarios
4. Generate summary reports
5. Update this README
6. Add test to `run-extraction-tests.sh`

## Support

For issues or questions:
1. Check test logs in `./load-test-results/`
2. Review application logs
3. Consult `extraction_performance_plan.md`
4. Check k6 documentation: https://k6.io/docs/

---

**Last Updated:** January 2025
**Test Suite Version:** 1.0.0
**k6 Version Required:** >= 0.40.0
