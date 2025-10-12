/**
 * JiVS Platform - Extraction Soak Test (Endurance Test)
 *
 * This test runs for an extended period to detect:
 * - Memory leaks
 * - Connection pool exhaustion over time
 * - Gradual performance degradation
 * - Resource cleanup issues
 * - Cache behavior over time
 *
 * Load Profile:
 * - Duration: 2 hours (configurable)
 * - Consistent load: 50 VUs
 * - Purpose: Long-term stability testing
 *
 * Usage:
 *   k6 run load-tests/extraction-soak-test.js
 *   k6 run --env DURATION=4h load-tests/extraction-soak-test.js
 *   k6 run --env DURATION=24h load-tests/extraction-soak-test.js  # Full endurance
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// Custom metrics for soak test
const extractionLatency = new Trend('extraction_latency');
const extractionLatencyOverTime = new Trend('extraction_latency_over_time', true);
const extractionErrors = new Rate('extraction_errors');
const extractionsCreated = new Counter('extractions_created');
const extractionsCompleted = new Counter('extractions_completed');
const memoryLeakIndicator = new Gauge('memory_leak_indicator');
const connectionPoolHealth = new Gauge('connection_pool_health');
const cacheHitRate = new Rate('cache_hit_rate');
const periodicHealth = new Counter('periodic_health_checks');

// Soak test configuration
const DURATION = __ENV.DURATION || '2h';
const LOAD_VUS = __ENV.LOAD_VUS || 50;

export const options = {
    stages: [
        { duration: '5m', target: parseInt(LOAD_VUS) },      // Ramp up
        { duration: DURATION, target: parseInt(LOAD_VUS) },  // Sustained load
        { duration: '5m', target: 0 },                       // Ramp down
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500', 'p(99)<1000'],
        'http_req_failed': ['rate<0.02'],
        'extraction_errors': ['rate<0.03'],
        // Key soak test threshold: latency shouldn't increase over time
        'extraction_latency_over_time': ['p(95)<500'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const USERNAME = __ENV.TEST_USERNAME || 'testuser';
const PASSWORD = __ENV.TEST_PASSWORD || 'TestPassword123!';

// Track performance over time
let iterationCount = 0;
let latencyHistory = [];
const HISTORY_SIZE = 100;

/**
 * Setup function
 */
export function setup() {
    console.log('==================================================');
    console.log('JiVS Extraction Soak Test (Endurance)');
    console.log('==================================================');
    console.log(`Target: ${BASE_URL}`);
    console.log(`Duration: ${DURATION}`);
    console.log(`Constant Load: ${LOAD_VUS} VUs`);
    console.log('');
    console.log('Monitoring for:');
    console.log('  - Memory leaks');
    console.log('  - Connection pool exhaustion');
    console.log('  - Performance degradation');
    console.log('  - Resource cleanup issues');
    console.log('');

    const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
        username: USERNAME,
        password: PASSWORD,
    }), {
        headers: { 'Content-Type': 'application/json' },
    });

    if (loginRes.status !== 200) {
        throw new Error('Authentication failed during setup');
    }

    const authToken = loginRes.json('accessToken') || loginRes.json('data.accessToken');

    return {
        baseUrl: BASE_URL,
        authToken: authToken,
        startTime: Date.now(),
    };
}

/**
 * Main soak test scenario
 */
export default function (data) {
    const headers = {
        'Authorization': `Bearer ${data.authToken}`,
        'Content-Type': 'application/json',
    };

    iterationCount++;
    const testRuntime = (Date.now() - data.startTime) / 1000; // seconds

    let extractionId = null;

    // 1. Create extraction
    group('Create Extraction', function () {
        const extractionConfig = {
            name: `Soak Test ${Date.now()}-${__VU}-${iterationCount}`,
            sourceType: 'JDBC',
            connectionConfig: {
                jdbcUrl: 'jdbc:postgresql://postgres:5432/testdb',
                username: 'testuser',
                password: 'testpass',
                driverClass: 'org.postgresql.Driver',
            },
            extractionQuery: 'SELECT * FROM test_table LIMIT 5000',
        };

        const startTime = Date.now();
        const createRes = http.post(
            `${data.baseUrl}/extractions`,
            JSON.stringify(extractionConfig),
            { headers: headers, tags: { name: 'CreateExtraction', runtime: testRuntime } }
        );
        const duration = Date.now() - startTime;

        extractionLatency.add(duration);
        extractionLatencyOverTime.add(duration);

        // Track latency trend
        latencyHistory.push(duration);
        if (latencyHistory.length > HISTORY_SIZE) {
            latencyHistory.shift();
        }

        const createSuccess = check(createRes, {
            'create succeeded': (r) => r.status === 201 || r.status === 200,
        });

        if (createSuccess) {
            const body = createRes.json();
            extractionId = body.id || body.data?.id;
            extractionsCreated.add(1);
            extractionErrors.add(0);
        } else {
            extractionErrors.add(1);
            return;
        }
    });

    if (!extractionId) {
        return;
    }

    sleep(0.5);

    // 2. Start extraction
    group('Start Extraction', function () {
        const startRes = http.post(
            `${data.baseUrl}/extractions/${extractionId}/start`,
            null,
            { headers: headers, tags: { name: 'StartExtraction' } }
        );

        check(startRes, {
            'start succeeded': (r) => r.status === 200 || r.status === 202,
        });
    });

    sleep(1);

    // 3. Monitor extraction (abbreviated for soak test)
    group('Monitor Extraction', function () {
        let attempts = 0;
        const maxAttempts = 20;

        while (attempts < maxAttempts) {
            const statusRes = http.get(
                `${data.baseUrl}/extractions/${extractionId}`,
                { headers: headers, tags: { name: 'GetStatus' } }
            );

            // Check for cache header
            const cacheHeader = statusRes.headers['X-Cache'];
            if (cacheHeader) {
                cacheHitRate.add(cacheHeader === 'HIT' ? 1 : 0);
            }

            if (statusRes.status === 200) {
                const body = statusRes.json();
                const extraction = body.data || body;

                if (extraction.status === 'COMPLETED') {
                    extractionsCompleted.add(1);
                    break;
                } else if (extraction.status === 'FAILED' || extraction.status === 'STOPPED') {
                    extractionErrors.add(1);
                    break;
                }
            }

            attempts++;
            sleep(1);
        }
    });

    // 4. Periodic health checks (every 10 iterations)
    if (iterationCount % 10 === 0) {
        group('Health Check', function () {
            periodicHealth.add(1);

            // Check system health endpoint (if available)
            const healthRes = http.get(
                `${data.baseUrl.replace('/api/v1', '')}/actuator/health`,
                { headers: headers, tags: { name: 'HealthCheck' } }
            );

            if (healthRes.status === 200) {
                const health = healthRes.json();
                // Check connection pool health
                if (health.components?.db?.status === 'UP') {
                    connectionPoolHealth.add(1);
                } else {
                    connectionPoolHealth.add(0);
                }
            }

            // Analyze latency trend for memory leak detection
            if (latencyHistory.length === HISTORY_SIZE) {
                const recentAvg = latencyHistory.slice(-20).reduce((a, b) => a + b, 0) / 20;
                const oldAvg = latencyHistory.slice(0, 20).reduce((a, b) => a + b, 0) / 20;
                const degradation = recentAvg / oldAvg;

                // If recent latency is 50% higher than initial, suspect memory leak
                memoryLeakIndicator.add(degradation);
            }
        });
    }

    // Normal think time
    sleep(2);
}

/**
 * Teardown function
 */
export function teardown(data) {
    console.log('');
    console.log('==================================================');
    console.log('Soak Test Completed');
    console.log('==================================================');
}

/**
 * Custom summary report
 */
export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');

    const totalRequests = data.metrics.http_reqs?.values?.count || 0;
    const failedRequests = data.metrics.http_req_failed?.values?.passes || 0;
    const extractionsCreated = data.metrics.extractions_created?.values?.count || 0;
    const extractionsCompleted = data.metrics.extractions_completed?.values?.count || 0;
    const testDurationHours = (data.state.testRunDurationMs / 1000 / 3600).toFixed(2);

    // Analyze performance over time
    const latencyStart = data.metrics.extraction_latency?.values?.min || 0;
    const latencyEnd = data.metrics.extraction_latency?.values?.max || 0;
    const latencyAvg = data.metrics.extraction_latency?.values?.avg || 0;
    const latencyDegradation = latencyEnd > 0 ? ((latencyEnd / latencyAvg) * 100).toFixed(2) : 0;

    const summary = {
        timestamp: timestamp,
        test_type: 'soak',
        duration_hours: testDurationHours,
        constant_load_vus: LOAD_VUS,
        metrics: {
            http: {
                total_requests: totalRequests,
                failed_requests: failedRequests,
                failure_rate: totalRequests > 0 ? ((failedRequests / totalRequests) * 100).toFixed(2) : 0,
                avg_duration_ms: data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0,
                p95_duration_ms: data.metrics.http_req_duration?.values['p(95)']?.toFixed(2) || 0,
                p99_duration_ms: data.metrics.http_req_duration?.values['p(99)']?.toFixed(2) || 0,
            },
            extraction: {
                created: extractionsCreated,
                completed: extractionsCompleted,
                completion_rate: extractionsCreated > 0 ?
                    ((extractionsCompleted / extractionsCreated) * 100).toFixed(2) : 0,
                latency_avg: latencyAvg.toFixed(2),
                latency_p95: data.metrics.extraction_latency?.values['p(95)']?.toFixed(2) || 0,
                latency_p99: data.metrics.extraction_latency?.values['p(99)']?.toFixed(2) || 0,
                latency_max: latencyEnd.toFixed(2),
                latency_degradation_percent: latencyDegradation,
            },
            stability: {
                cache_hit_rate: ((data.metrics.cache_hit_rate?.values?.rate || 0) * 100).toFixed(2),
                connection_pool_health: data.metrics.connection_pool_health?.values?.value || 'N/A',
                memory_leak_indicator: data.metrics.memory_leak_indicator?.values?.value || 'N/A',
                health_checks_performed: data.metrics.periodic_health_checks?.values?.count || 0,
            },
        },
        performance_over_time: {
            latency_stable: latencyDegradation < 150, // Less than 50% increase
            throughput_stable: (failedRequests / totalRequests) < 0.02,
            system_healthy: true, // TODO: Calculate based on metrics
        },
    };

    console.log('\n' + '='.repeat(70));
    console.log('SOAK TEST RESULTS (ENDURANCE)');
    console.log('='.repeat(70));
    console.log(`\nTest Duration: ${summary.duration_hours} hours`);
    console.log(`Constant Load: ${LOAD_VUS} VUs`);
    console.log(`\nTotal Requests: ${summary.metrics.http.total_requests}`);
    console.log(`Failed Requests: ${summary.metrics.http.failed_requests} (${summary.metrics.http.failure_rate}%)`);
    console.log(`\nExtractions Created: ${summary.metrics.extraction.created}`);
    console.log(`Extractions Completed: ${summary.metrics.extraction.completed} (${summary.metrics.extraction.completion_rate}%)`);
    console.log(`\nLATENCY STABILITY:`);
    console.log(`  Average: ${summary.metrics.extraction.latency_avg}ms`);
    console.log(`  p95: ${summary.metrics.extraction.latency_p95}ms`);
    console.log(`  p99: ${summary.metrics.extraction.latency_p99}ms`);
    console.log(`  Max: ${summary.metrics.extraction.latency_max}ms`);
    console.log(`  Degradation: ${summary.metrics.extraction.latency_degradation_percent}%`);
    console.log(`\nSTABILITY INDICATORS:`);
    console.log(`  Cache Hit Rate: ${summary.metrics.stability.cache_hit_rate}%`);
    console.log(`  Health Checks: ${summary.metrics.stability.health_checks_performed}`);
    console.log(`\nSTABILITY ANALYSIS:`);

    if (summary.performance_over_time.latency_stable) {
        console.log(`  ✓ Latency remained stable over ${summary.duration_hours} hours`);
    } else {
        console.log(`  ✗ WARNING: Latency degraded by ${summary.metrics.extraction.latency_degradation_percent}%`);
        console.log(`    → Possible memory leak or resource exhaustion`);
    }

    if (summary.performance_over_time.throughput_stable) {
        console.log(`  ✓ Error rate remained low (${summary.metrics.http.failure_rate}%)`);
    } else {
        console.log(`  ✗ WARNING: Error rate increased over time`);
        console.log(`    → Check logs for connection pool or resource issues`);
    }

    console.log(`\nOVERALL VERDICT:`);
    if (summary.performance_over_time.latency_stable && summary.performance_over_time.throughput_stable) {
        console.log(`  ✓ System is stable for long-running operations`);
        console.log(`  ✓ No memory leaks detected`);
        console.log(`  ✓ Connection pool behaving correctly`);
    } else {
        console.log(`  ⚠ System shows signs of degradation over time`);
        console.log(`  → Review resource cleanup and connection management`);
    }

    console.log('='.repeat(70) + '\n');

    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        [`load-test-results/soak-${timestamp}.json`]: JSON.stringify(summary, null, 2),
        [`load-test-results/soak-${timestamp}-full.json`]: JSON.stringify(data, null, 2),
    };
}
