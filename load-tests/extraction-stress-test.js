/**
 * JiVS Platform - Extraction Stress Test
 *
 * This test gradually increases load to find the system's breaking point.
 *
 * Purpose:
 * - Identify maximum sustainable throughput
 * - Find connection pool limits
 * - Detect memory leaks under high load
 * - Identify resource exhaustion points
 * - Measure graceful degradation behavior
 *
 * Load Profile:
 * - Start: 10 VUs
 * - Ramp to: 500 VUs over 20 minutes
 * - Stages: 10 → 50 → 100 → 200 → 500 VUs
 *
 * Usage:
 *   k6 run load-tests/extraction-stress-test.js
 *   k6 run --env MAX_VUS=1000 load-tests/extraction-stress-test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// Custom metrics
const extractionLatency = new Trend('extraction_latency');
const extractionErrors = new Rate('extraction_errors');
const extractionsCreated = new Counter('extractions_created');
const extractionsFailed = new Counter('extractions_failed');
const activeExtractions = new Gauge('active_extractions');
const connectionErrors = new Counter('connection_errors');
const timeoutErrors = new Counter('timeout_errors');
const serverErrors = new Counter('server_errors');
const currentLoad = new Gauge('current_load_vus');

// Stress test configuration
const MAX_VUS = __ENV.MAX_VUS || 500;

export const options = {
    stages: [
        { duration: '2m', target: 10 },    // Warm up
        { duration: '3m', target: 50 },    // Stage 1
        { duration: '3m', target: 100 },   // Stage 2
        { duration: '3m', target: 200 },   // Stage 3
        { duration: '3m', target: 350 },   // Stage 4
        { duration: '3m', target: 500 },   // Stage 5 - Breaking point
        { duration: '2m', target: 500 },   // Sustain peak
        { duration: '2m', target: 0 },     // Recovery
    ],
    thresholds: {
        // Relaxed thresholds - we expect degradation
        'http_req_duration': ['p(95)<2000'],
        'extraction_errors': ['rate<0.25'], // Allow up to 25% errors at breaking point
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const USERNAME = __ENV.TEST_USERNAME || 'testuser';
const PASSWORD = __ENV.TEST_PASSWORD || 'TestPassword123!';

/**
 * Setup function
 */
export function setup() {
    console.log('==================================================');
    console.log('JiVS Extraction Stress Test');
    console.log('==================================================');
    console.log(`Target: ${BASE_URL}`);
    console.log(`Max Load: ${MAX_VUS} VUs`);
    console.log('Goal: Find breaking point');
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
    };
}

/**
 * Main stress test scenario
 */
export default function (data) {
    const headers = {
        'Authorization': `Bearer ${data.authToken}`,
        'Content-Type': 'application/json',
    };

    // Track current load
    currentLoad.add(__VU);

    // Simplified extraction for stress testing
    const dataSize = 5000; // Fixed smaller size for stress testing
    let extractionId = null;

    // 1. Create extraction
    group('Create Extraction', function () {
        const extractionConfig = {
            name: `Stress Test ${Date.now()}-${__VU}`,
            sourceType: 'JDBC',
            connectionConfig: {
                jdbcUrl: 'jdbc:postgresql://postgres:5432/testdb',
                username: 'testuser',
                password: 'testpass',
                driverClass: 'org.postgresql.Driver',
            },
            extractionQuery: `SELECT * FROM test_table LIMIT ${dataSize}`,
        };

        const startTime = Date.now();
        const createRes = http.post(
            `${data.baseUrl}/extractions`,
            JSON.stringify(extractionConfig),
            {
                headers: headers,
                tags: { name: 'CreateExtraction' },
                timeout: '30s', // Increased timeout for high load
            }
        );
        const duration = Date.now() - startTime;

        extractionLatency.add(duration);

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
            extractionsFailed.add(1);

            // Categorize errors
            if (createRes.status === 0) {
                connectionErrors.add(1);
            } else if (createRes.status === 408 || createRes.status === 504) {
                timeoutErrors.add(1);
            } else if (createRes.status >= 500) {
                serverErrors.add(1);
            }

            // Log first few errors for analysis
            if (extractionsFailed.value < 10) {
                console.error(`VU ${__VU}: Failed to create extraction: ${createRes.status}`);
            }
            return;
        }
    });

    if (!extractionId) {
        return;
    }

    // 2. Start extraction (skip monitoring to increase load)
    group('Start Extraction', function () {
        const startRes = http.post(
            `${data.baseUrl}/extractions/${extractionId}/start`,
            null,
            {
                headers: headers,
                tags: { name: 'StartExtraction' },
                timeout: '30s',
            }
        );

        const startSuccess = check(startRes, {
            'start succeeded': (r) => r.status === 200 || r.status === 202,
        });

        if (startSuccess) {
            activeExtractions.add(1);
        } else {
            extractionErrors.add(1);

            if (startRes.status === 0) {
                connectionErrors.add(1);
            } else if (startRes.status === 408 || startRes.status === 504) {
                timeoutErrors.add(1);
            } else if (startRes.status >= 500) {
                serverErrors.add(1);
            }
        }
    });

    // 3. Quick status check (no polling)
    group('Check Status', function () {
        const statusRes = http.get(
            `${data.baseUrl}/extractions/${extractionId}`,
            {
                headers: headers,
                tags: { name: 'GetStatus' },
                timeout: '10s',
            }
        );

        if (statusRes.status === 200) {
            const body = statusRes.json();
            const extraction = body.data || body;
            if (extraction.status === 'COMPLETED' || extraction.status === 'FAILED') {
                activeExtractions.add(-1);
            }
        }
    });

    // Minimal sleep to maximize load
    sleep(0.5);
}

/**
 * Teardown function
 */
export function teardown(data) {
    console.log('');
    console.log('==================================================');
    console.log('Stress Test Completed');
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
    const extractionsFailed = data.metrics.extractions_failed?.values?.count || 0;
    const connectionErrors = data.metrics.connection_errors?.values?.count || 0;
    const timeoutErrors = data.metrics.timeout_errors?.values?.count || 0;
    const serverErrors = data.metrics.server_errors?.values?.count || 0;

    const summary = {
        timestamp: timestamp,
        test_type: 'stress',
        max_vus: MAX_VUS,
        duration_seconds: data.state.testRunDurationMs / 1000,
        metrics: {
            http: {
                total_requests: totalRequests,
                failed_requests: failedRequests,
                failure_rate: totalRequests > 0 ? ((failedRequests / totalRequests) * 100).toFixed(2) : 0,
                avg_duration_ms: data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0,
                p95_duration_ms: data.metrics.http_req_duration?.values['p(95)']?.toFixed(2) || 0,
                p99_duration_ms: data.metrics.http_req_duration?.values['p(99)']?.toFixed(2) || 0,
                max_duration_ms: data.metrics.http_req_duration?.values?.max?.toFixed(2) || 0,
            },
            extraction: {
                created: extractionsCreated,
                failed: extractionsFailed,
                success_rate: extractionsCreated > 0 ?
                    ((extractionsCreated / (extractionsCreated + extractionsFailed)) * 100).toFixed(2) : 0,
                latency_avg: data.metrics.extraction_latency?.values?.avg?.toFixed(2) || 0,
                latency_p95: data.metrics.extraction_latency?.values['p(95)']?.toFixed(2) || 0,
                latency_max: data.metrics.extraction_latency?.values?.max?.toFixed(2) || 0,
            },
            errors: {
                connection_errors: connectionErrors,
                timeout_errors: timeoutErrors,
                server_errors: serverErrors,
                total_errors: extractionsFailed,
            },
        },
        breaking_point_analysis: {
            // Estimate breaking point based on error rates
            error_threshold_exceeded: extractionsFailed > (extractionsCreated * 0.1),
            estimated_max_vus: 'See detailed analysis below',
        },
    };

    console.log('\n' + '='.repeat(70));
    console.log('STRESS TEST RESULTS');
    console.log('='.repeat(70));
    console.log(`\nMax VUs: ${MAX_VUS}`);
    console.log(`Total Requests: ${summary.metrics.http.total_requests}`);
    console.log(`Failed Requests: ${summary.metrics.http.failed_requests} (${summary.metrics.http.failure_rate}%)`);
    console.log(`\nExtractions Created: ${summary.metrics.extraction.created}`);
    console.log(`Extractions Failed: ${summary.metrics.extraction.failed}`);
    console.log(`Success Rate: ${summary.metrics.extraction.success_rate}%`);
    console.log(`\nLATENCY UNDER STRESS:`);
    console.log(`  Average: ${summary.metrics.extraction.latency_avg}ms`);
    console.log(`  p95: ${summary.metrics.extraction.latency_p95}ms`);
    console.log(`  Max: ${summary.metrics.extraction.latency_max}ms`);
    console.log(`\nERROR BREAKDOWN:`);
    console.log(`  Connection Errors: ${summary.metrics.errors.connection_errors}`);
    console.log(`  Timeout Errors: ${summary.metrics.errors.timeout_errors}`);
    console.log(`  Server Errors: ${summary.metrics.errors.server_errors}`);
    console.log(`\nBREAKING POINT ANALYSIS:`);

    if (summary.metrics.extraction.success_rate > 95) {
        console.log(`  ✓ System handled ${MAX_VUS} VUs successfully (>95% success rate)`);
        console.log(`  → Recommendation: System can handle current load. Consider testing higher VUs.`);
    } else if (summary.metrics.extraction.success_rate > 80) {
        console.log(`  ⚠ System degraded at ${MAX_VUS} VUs (${summary.metrics.extraction.success_rate}% success rate)`);
        console.log(`  → Recommendation: Max sustainable load is around ${Math.floor(MAX_VUS * 0.8)} VUs`);
    } else {
        console.log(`  ✗ System breaking point reached at ${MAX_VUS} VUs (${summary.metrics.extraction.success_rate}% success rate)`);
        console.log(`  → Recommendation: Max sustainable load is around ${Math.floor(MAX_VUS * 0.5)} VUs`);
    }

    console.log('='.repeat(70) + '\n');

    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        [`load-test-results/stress-${timestamp}.json`]: JSON.stringify(summary, null, 2),
        [`load-test-results/stress-${timestamp}-full.json`]: JSON.stringify(data, null, 2),
    };
}
