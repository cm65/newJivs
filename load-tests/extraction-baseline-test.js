/**
 * JiVS Platform - Extraction Baseline Load Test
 *
 * This test establishes baseline performance metrics for extraction operations
 * before optimization implementation.
 *
 * Purpose:
 * - Measure current throughput (records/min)
 * - Measure current latency (p50, p95, p99)
 * - Identify baseline resource utilization
 * - Provide comparison metrics for post-optimization validation
 *
 * Usage:
 *   k6 run load-tests/extraction-baseline-test.js
 *   k6 run --env BASE_URL=http://staging.jivs.com/api/v1 load-tests/extraction-baseline-test.js
 *
 * Environment Variables:
 *   BASE_URL - API base URL (default: http://localhost:8080/api/v1)
 *   TEST_USERNAME - Test user (default: testuser)
 *   TEST_PASSWORD - Test password (default: TestPassword123!)
 *   EXTRACTION_RECORDS - Number of records to extract (default: 10000)
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// Custom metrics for extraction performance
const extractionCreationLatency = new Trend('extraction_creation_latency');
const extractionStartLatency = new Trend('extraction_start_latency');
const extractionStatusLatency = new Trend('extraction_status_latency');
const extractionStatsLatency = new Trend('extraction_stats_latency');
const extractionThroughput = new Rate('extraction_throughput_records_per_min');
const extractionErrors = new Rate('extraction_errors');
const extractionsCreated = new Counter('extractions_created');
const extractionsStarted = new Counter('extractions_started');
const extractionsCompleted = new Counter('extractions_completed');
const recordsExtracted = new Counter('records_extracted');
const activeExtractions = new Gauge('active_extractions');

// Test configuration - baseline test with moderate load
export const options = {
    stages: [
        { duration: '1m', target: 5 },    // Warm up to 5 users
        { duration: '5m', target: 10 },   // Steady state at 10 users
        { duration: '1m', target: 0 },    // Cool down
    ],
    thresholds: {
        // Baseline thresholds (current system)
        'http_req_duration': ['p(95)<500', 'p(99)<1000'],
        'http_req_failed': ['rate<0.02'],
        'extraction_creation_latency': ['p(95)<600'],
        'extraction_start_latency': ['p(95)<800'],
        'extraction_errors': ['rate<0.05'],
    },
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const USERNAME = __ENV.TEST_USERNAME || 'testuser';
const PASSWORD = __ENV.TEST_PASSWORD || 'TestPassword123!';
const EXTRACTION_RECORDS = __ENV.EXTRACTION_RECORDS || 10000;

/**
 * Setup function - authenticate and prepare test data
 */
export function setup() {
    console.log('==================================================');
    console.log('JiVS Extraction Baseline Test Setup');
    console.log('==================================================');
    console.log(`Target: ${BASE_URL}`);
    console.log(`Test User: ${USERNAME}`);
    console.log(`Extraction Records: ${EXTRACTION_RECORDS}`);
    console.log('');

    // Authenticate to get token
    const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
        username: USERNAME,
        password: PASSWORD,
    }), {
        headers: { 'Content-Type': 'application/json' },
    });

    if (loginRes.status !== 200) {
        console.error('Setup failed: Could not authenticate');
        console.error('Status:', loginRes.status);
        console.error('Body:', loginRes.body);
        throw new Error('Authentication failed during setup');
    }

    const authToken = loginRes.json('accessToken') || loginRes.json('data.accessToken');

    return {
        baseUrl: BASE_URL,
        authToken: authToken,
        extractionRecords: EXTRACTION_RECORDS,
    };
}

/**
 * Main test scenario
 */
export default function (data) {
    const headers = {
        'Authorization': `Bearer ${data.authToken}`,
        'Content-Type': 'application/json',
    };

    let extractionId = null;

    // 1. Create extraction job
    group('Create Extraction', function () {
        const extractionConfig = {
            name: `Baseline Test Extraction ${Date.now()}`,
            sourceType: 'JDBC',
            connectionConfig: {
                jdbcUrl: 'jdbc:postgresql://postgres:5432/testdb',
                username: 'testuser',
                password: 'testpass',
                driverClass: 'org.postgresql.Driver',
            },
            extractionQuery: `SELECT * FROM test_table LIMIT ${data.extractionRecords}`,
            schedule: null,
        };

        const startTime = Date.now();
        const createRes = http.post(
            `${data.baseUrl}/extractions`,
            JSON.stringify(extractionConfig),
            { headers: headers, tags: { name: 'CreateExtraction' } }
        );
        const createDuration = Date.now() - startTime;

        const createSuccess = check(createRes, {
            'create status is 201 or 200': (r) => r.status === 201 || r.status === 200,
            'create response has id': (r) => {
                const body = r.json();
                return body.id || body.data?.id;
            },
        });

        extractionCreationLatency.add(createDuration);

        if (createSuccess) {
            const body = createRes.json();
            extractionId = body.id || body.data?.id;
            extractionsCreated.add(1);
            extractionErrors.add(0);
        } else {
            extractionErrors.add(1);
            console.error('Failed to create extraction:', createRes.status, createRes.body);
            return;
        }
    });

    if (!extractionId) {
        return;
    }

    sleep(0.5);

    // 2. Start extraction job
    group('Start Extraction', function () {
        const startTime = Date.now();
        const startRes = http.post(
            `${data.baseUrl}/extractions/${extractionId}/start`,
            null,
            { headers: headers, tags: { name: 'StartExtraction' } }
        );
        const startDuration = Date.now() - startTime;

        const startSuccess = check(startRes, {
            'start status is 200 or 202': (r) => r.status === 200 || r.status === 202,
        });

        extractionStartLatency.add(startDuration);

        if (startSuccess) {
            extractionsStarted.add(1);
            activeExtractions.add(1);
            extractionErrors.add(0);
        } else {
            extractionErrors.add(1);
            console.error('Failed to start extraction:', startRes.status, startRes.body);
            return;
        }
    });

    sleep(1);

    // 3. Poll extraction status
    group('Poll Extraction Status', function () {
        let completed = false;
        let attempts = 0;
        const maxAttempts = 60; // 60 seconds max polling

        while (!completed && attempts < maxAttempts) {
            const startTime = Date.now();
            const statusRes = http.get(
                `${data.baseUrl}/extractions/${extractionId}`,
                { headers: headers, tags: { name: 'GetExtractionStatus' } }
            );
            const statusDuration = Date.now() - startTime;

            extractionStatusLatency.add(statusDuration);

            if (statusRes.status === 200) {
                const body = statusRes.json();
                const extraction = body.data || body;
                const status = extraction.status;

                if (status === 'COMPLETED') {
                    completed = true;
                    extractionsCompleted.add(1);
                    activeExtractions.add(-1);

                    const records = extraction.recordsExtracted || 0;
                    recordsExtracted.add(records);

                    // Calculate throughput (records per minute)
                    if (extraction.endTime && extraction.startTime) {
                        const startTime = new Date(extraction.startTime).getTime();
                        const endTime = new Date(extraction.endTime).getTime();
                        const durationMinutes = (endTime - startTime) / 60000;
                        if (durationMinutes > 0) {
                            const throughput = records / durationMinutes;
                            extractionThroughput.add(throughput > 0 ? 1 : 0);
                        }
                    }
                } else if (status === 'FAILED' || status === 'STOPPED') {
                    completed = true;
                    activeExtractions.add(-1);
                    extractionErrors.add(1);
                    console.error('Extraction failed:', status);
                }
            }

            attempts++;
            sleep(1); // Poll every second
        }

        if (!completed) {
            console.warn('Extraction did not complete within timeout');
            activeExtractions.add(-1);
        }
    });

    // 4. Get extraction statistics
    group('Get Extraction Statistics', function () {
        const startTime = Date.now();
        const statsRes = http.get(
            `${data.baseUrl}/extractions/${extractionId}/statistics`,
            { headers: headers, tags: { name: 'GetExtractionStats' } }
        );
        const statsDuration = Date.now() - startTime;

        check(statsRes, {
            'stats status is 200': (r) => r.status === 200,
            'stats has data': (r) => r.json('data') !== undefined,
        });

        extractionStatsLatency.add(statsDuration);
    });

    // Think time between iterations
    sleep(2);
}

/**
 * Teardown function
 */
export function teardown(data) {
    console.log('');
    console.log('==================================================');
    console.log('Baseline Test Completed');
    console.log('==================================================');
}

/**
 * Custom summary report
 */
export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');

    // Calculate custom metrics
    const totalRecords = data.metrics.records_extracted?.values?.count || 0;
    const totalExtractions = data.metrics.extractions_completed?.values?.count || 0;
    const avgRecordsPerExtraction = totalExtractions > 0 ? totalRecords / totalExtractions : 0;

    const summary = {
        timestamp: timestamp,
        test_type: 'baseline',
        duration_seconds: data.state.testRunDurationMs / 1000,
        metrics: {
            http: {
                total_requests: data.metrics.http_reqs?.values?.count || 0,
                request_rate: data.metrics.http_reqs?.values?.rate || 0,
                failed_requests: data.metrics.http_req_failed?.values?.passes || 0,
                avg_duration_ms: data.metrics.http_req_duration?.values?.avg || 0,
                p95_duration_ms: data.metrics.http_req_duration?.values['p(95)'] || 0,
                p99_duration_ms: data.metrics.http_req_duration?.values['p(99)'] || 0,
            },
            extraction: {
                created: data.metrics.extractions_created?.values?.count || 0,
                started: data.metrics.extractions_started?.values?.count || 0,
                completed: data.metrics.extractions_completed?.values?.count || 0,
                total_records: totalRecords,
                avg_records_per_extraction: avgRecordsPerExtraction,
                creation_latency_p95: data.metrics.extraction_creation_latency?.values['p(95)'] || 0,
                start_latency_p95: data.metrics.extraction_start_latency?.values['p(95)'] || 0,
                status_latency_p95: data.metrics.extraction_status_latency?.values['p(95)'] || 0,
                error_rate: data.metrics.extraction_errors?.values?.rate || 0,
            },
        },
        thresholds: {
            http_req_duration_p95: data.metrics.http_req_duration?.thresholds?.['p(95)<500']?.ok || false,
            http_req_failed: data.metrics.http_req_failed?.thresholds?.['rate<0.02']?.ok || false,
            extraction_creation_p95: data.metrics.extraction_creation_latency?.thresholds?.['p(95)<600']?.ok || false,
            extraction_start_p95: data.metrics.extraction_start_latency?.thresholds?.['p(95)<800']?.ok || false,
        },
    };

    console.log('\n' + '='.repeat(60));
    console.log('BASELINE PERFORMANCE METRICS');
    console.log('='.repeat(60));
    console.log(`\nExtractions Completed: ${summary.metrics.extraction.completed}`);
    console.log(`Total Records Extracted: ${summary.metrics.extraction.total_records}`);
    console.log(`Avg Records/Extraction: ${summary.metrics.extraction.avg_records_per_extraction.toFixed(0)}`);
    console.log(`\nCreation Latency (p95): ${summary.metrics.extraction.creation_latency_p95.toFixed(2)}ms`);
    console.log(`Start Latency (p95): ${summary.metrics.extraction.start_latency_p95.toFixed(2)}ms`);
    console.log(`Status Poll Latency (p95): ${summary.metrics.extraction.status_latency_p95.toFixed(2)}ms`);
    console.log(`\nError Rate: ${(summary.metrics.extraction.error_rate * 100).toFixed(2)}%`);
    console.log('='.repeat(60) + '\n');

    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        [`load-test-results/baseline-${timestamp}.json`]: JSON.stringify(summary, null, 2),
        [`load-test-results/baseline-${timestamp}-full.json`]: JSON.stringify(data, null, 2),
    };
}
