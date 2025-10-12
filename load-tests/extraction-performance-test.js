/**
 * JiVS Platform - Extraction Performance Load Test
 *
 * This test validates performance improvements after optimization implementation.
 *
 * Target Performance (from optimization plan):
 * - Throughput: 20,000+ records/min (2x improvement from 10k baseline)
 * - p95 Latency: < 200ms (56% reduction from 450ms baseline)
 * - p99 Latency: < 500ms
 * - Error Rate: < 1%
 *
 * Test Scenarios:
 * - 100 concurrent users creating and executing extractions
 * - Various data sizes: 1k, 10k, 50k, 100k records
 * - Multiple source types: JDBC, File, API
 * - Concurrent extraction execution
 *
 * Usage:
 *   k6 run load-tests/extraction-performance-test.js
 *   k6 run --env BASE_URL=http://staging.jivs.com/api/v1 load-tests/extraction-performance-test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// Custom metrics
const extractionCreationLatency = new Trend('extraction_creation_latency');
const extractionStartLatency = new Trend('extraction_start_latency');
const extractionStatusLatency = new Trend('extraction_status_latency');
const extractionThroughput = new Trend('extraction_throughput_records_per_min');
const extractionErrors = new Rate('extraction_errors');
const extractionsCreated = new Counter('extractions_created');
const extractionsCompleted = new Counter('extractions_completed');
const recordsExtracted = new Counter('records_extracted');
const activeExtractions = new Gauge('active_extractions');
const cacheHits = new Counter('cache_hits');
const connectionPoolUtilization = new Gauge('connection_pool_utilization');

// Performance test configuration - high load
export const options = {
    stages: [
        { duration: '2m', target: 20 },    // Ramp up to 20 users
        { duration: '3m', target: 50 },    // Increase to 50 users
        { duration: '5m', target: 100 },   // Peak load at 100 users
        { duration: '5m', target: 100 },   // Sustain peak load
        { duration: '2m', target: 50 },    // Ramp down
        { duration: '1m', target: 0 },     // Cool down
    ],
    thresholds: {
        // Target thresholds (optimized system)
        'http_req_duration': ['p(95)<250', 'p(99)<500'],
        'http_req_failed': ['rate<0.01'],
        'extraction_creation_latency': ['p(95)<200'],
        'extraction_start_latency': ['p(95)<200'],
        'extraction_status_latency': ['p(95)<150'],
        'extraction_errors': ['rate<0.01'],
        'extraction_throughput_records_per_min': ['p(50)>20000'],
    },
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const USERNAME = __ENV.TEST_USERNAME || 'testuser';
const PASSWORD = __ENV.TEST_PASSWORD || 'TestPassword123!';

// Data size scenarios
const DATA_SIZES = [1000, 10000, 50000, 100000];
const SOURCE_TYPES = ['JDBC', 'FILE', 'API'];

/**
 * Setup function
 */
export function setup() {
    console.log('==================================================');
    console.log('JiVS Extraction Performance Test Setup');
    console.log('==================================================');
    console.log(`Target: ${BASE_URL}`);
    console.log(`Peak Load: 100 concurrent users`);
    console.log(`Data Sizes: ${DATA_SIZES.join(', ')} records`);
    console.log(`Source Types: ${SOURCE_TYPES.join(', ')}`);
    console.log('');

    // Authenticate
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
 * Main test scenario
 */
export default function (data) {
    const headers = {
        'Authorization': `Bearer ${data.authToken}`,
        'Content-Type': 'application/json',
    };

    // Randomly select data size and source type for variation
    const dataSize = DATA_SIZES[Math.floor(Math.random() * DATA_SIZES.length)];
    const sourceType = SOURCE_TYPES[Math.floor(Math.random() * SOURCE_TYPES.length)];

    let extractionId = null;

    // 1. Create extraction with varied configurations
    group('Create Extraction', function () {
        const extractionConfig = generateExtractionConfig(sourceType, dataSize);

        const startTime = Date.now();
        const createRes = http.post(
            `${data.baseUrl}/extractions`,
            JSON.stringify(extractionConfig),
            { headers: headers, tags: { name: 'CreateExtraction', source: sourceType } }
        );
        const createDuration = Date.now() - startTime;

        const createSuccess = check(createRes, {
            'create status is 201 or 200': (r) => r.status === 201 || r.status === 200,
            'create response has id': (r) => {
                const body = r.json();
                return body.id || body.data?.id;
            },
            'create latency < 250ms': () => createDuration < 250,
        });

        extractionCreationLatency.add(createDuration);

        if (createSuccess) {
            const body = createRes.json();
            extractionId = body.id || body.data?.id;
            extractionsCreated.add(1);
            extractionErrors.add(0);
        } else {
            extractionErrors.add(1);
            console.error(`Failed to create ${sourceType} extraction:`, createRes.status);
            return;
        }
    });

    if (!extractionId) {
        return;
    }

    sleep(0.2); // Minimal think time

    // 2. Start extraction
    group('Start Extraction', function () {
        const startTime = Date.now();
        const startRes = http.post(
            `${data.baseUrl}/extractions/${extractionId}/start`,
            null,
            { headers: headers, tags: { name: 'StartExtraction', source: sourceType } }
        );
        const startDuration = Date.now() - startTime;

        const startSuccess = check(startRes, {
            'start status is 200 or 202': (r) => r.status === 200 || r.status === 202,
            'start latency < 250ms': () => startDuration < 250,
        });

        extractionStartLatency.add(startDuration);

        if (startSuccess) {
            activeExtractions.add(1);
            extractionErrors.add(0);
        } else {
            extractionErrors.add(1);
            return;
        }
    });

    sleep(0.5);

    // 3. Monitor extraction progress
    group('Monitor Extraction', function () {
        let completed = false;
        let attempts = 0;
        const maxAttempts = 30; // 30 seconds max

        while (!completed && attempts < maxAttempts) {
            const startTime = Date.now();
            const statusRes = http.get(
                `${data.baseUrl}/extractions/${extractionId}`,
                { headers: headers, tags: { name: 'GetStatus', source: sourceType } }
            );
            const statusDuration = Date.now() - startTime;

            extractionStatusLatency.add(statusDuration);

            if (statusRes.status === 200) {
                const body = statusRes.json();
                const extraction = body.data || body;
                const status = extraction.status;

                // Check for cache indicators
                const cacheHeader = statusRes.headers['X-Cache'];
                if (cacheHeader === 'HIT') {
                    cacheHits.add(1);
                }

                if (status === 'COMPLETED') {
                    completed = true;
                    extractionsCompleted.add(1);
                    activeExtractions.add(-1);

                    const records = extraction.recordsExtracted || 0;
                    recordsExtracted.add(records);

                    // Calculate throughput
                    if (extraction.endTime && extraction.startTime) {
                        const startTime = new Date(extraction.startTime).getTime();
                        const endTime = new Date(extraction.endTime).getTime();
                        const durationMinutes = (endTime - startTime) / 60000;
                        if (durationMinutes > 0) {
                            const throughput = records / durationMinutes;
                            extractionThroughput.add(throughput);
                        }
                    }
                } else if (status === 'FAILED' || status === 'STOPPED') {
                    completed = true;
                    activeExtractions.add(-1);
                    extractionErrors.add(1);
                }
            }

            attempts++;
            sleep(1);
        }
    });

    // 4. Test concurrent operations
    group('Concurrent Operations', function () {
        const batch = http.batch([
            {
                method: 'GET',
                url: `${data.baseUrl}/extractions?page=0&size=20&status=RUNNING`,
                params: { headers: headers, tags: { name: 'ListRunning' } },
            },
            {
                method: 'GET',
                url: `${data.baseUrl}/extractions/${extractionId}/statistics`,
                params: { headers: headers, tags: { name: 'GetStats' } },
            },
            {
                method: 'GET',
                url: `${data.baseUrl}/extractions/${extractionId}/logs?limit=10`,
                params: { headers: headers, tags: { name: 'GetLogs' } },
            },
        ]);

        check(batch, {
            'all concurrent requests successful': (responses) =>
                responses.every(r => r.status === 200),
        });
    });

    // Minimal think time for high load
    sleep(1);
}

/**
 * Generate extraction configuration based on source type
 */
function generateExtractionConfig(sourceType, dataSize) {
    const timestamp = Date.now();

    const configs = {
        JDBC: {
            name: `JDBC Extraction ${timestamp}`,
            sourceType: 'JDBC',
            connectionConfig: {
                jdbcUrl: 'jdbc:postgresql://postgres:5432/testdb',
                username: 'testuser',
                password: 'testpass',
                driverClass: 'org.postgresql.Driver',
            },
            extractionQuery: `SELECT * FROM test_table LIMIT ${dataSize}`,
        },
        FILE: {
            name: `File Extraction ${timestamp}`,
            sourceType: 'FILE',
            connectionConfig: {
                filePath: `/data/test_${dataSize}.csv`,
                fileType: 'CSV',
                delimiter: ',',
                hasHeader: true,
            },
            extractionQuery: null,
        },
        API: {
            name: `API Extraction ${timestamp}`,
            sourceType: 'API',
            connectionConfig: {
                endpoint: `https://api.example.com/data?limit=${dataSize}`,
                method: 'GET',
                authType: 'BEARER',
                authToken: 'test-token',
            },
            extractionQuery: null,
        },
    };

    return configs[sourceType];
}

/**
 * Teardown function
 */
export function teardown(data) {
    console.log('');
    console.log('==================================================');
    console.log('Performance Test Completed');
    console.log('==================================================');
}

/**
 * Custom summary report
 */
export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');

    const totalRecords = data.metrics.records_extracted?.values?.count || 0;
    const totalExtractions = data.metrics.extractions_completed?.values?.count || 0;
    const testDurationMin = (data.state.testRunDurationMs / 1000) / 60;

    const summary = {
        timestamp: timestamp,
        test_type: 'performance',
        duration_seconds: data.state.testRunDurationMs / 1000,
        metrics: {
            http: {
                total_requests: data.metrics.http_reqs?.values?.count || 0,
                request_rate: data.metrics.http_reqs?.values?.rate?.toFixed(2) || 0,
                failed_requests: data.metrics.http_req_failed?.values?.passes || 0,
                avg_duration_ms: data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0,
                p95_duration_ms: data.metrics.http_req_duration?.values['p(95)']?.toFixed(2) || 0,
                p99_duration_ms: data.metrics.http_req_duration?.values['p(99)']?.toFixed(2) || 0,
            },
            extraction: {
                created: data.metrics.extractions_created?.values?.count || 0,
                completed: totalExtractions,
                total_records: totalRecords,
                records_per_minute: testDurationMin > 0 ? (totalRecords / testDurationMin).toFixed(0) : 0,
                creation_latency_avg: data.metrics.extraction_creation_latency?.values?.avg?.toFixed(2) || 0,
                creation_latency_p95: data.metrics.extraction_creation_latency?.values['p(95)']?.toFixed(2) || 0,
                start_latency_avg: data.metrics.extraction_start_latency?.values?.avg?.toFixed(2) || 0,
                start_latency_p95: data.metrics.extraction_start_latency?.values['p(95)']?.toFixed(2) || 0,
                status_latency_avg: data.metrics.extraction_status_latency?.values?.avg?.toFixed(2) || 0,
                status_latency_p95: data.metrics.extraction_status_latency?.values['p(95)']?.toFixed(2) || 0,
                throughput_p50: data.metrics.extraction_throughput_records_per_min?.values['p(50)']?.toFixed(0) || 0,
                throughput_p95: data.metrics.extraction_throughput_records_per_min?.values['p(95)']?.toFixed(0) || 0,
                error_rate: ((data.metrics.extraction_errors?.values?.rate || 0) * 100).toFixed(2),
                cache_hits: data.metrics.cache_hits?.values?.count || 0,
            },
        },
        thresholds: {
            http_req_duration_p95: data.metrics.http_req_duration?.thresholds?.['p(95)<250']?.ok || false,
            http_req_duration_p99: data.metrics.http_req_duration?.thresholds?.['p(99)<500']?.ok || false,
            http_req_failed: data.metrics.http_req_failed?.thresholds?.['rate<0.01']?.ok || false,
            extraction_creation_p95: data.metrics.extraction_creation_latency?.thresholds?.['p(95)<200']?.ok || false,
            extraction_start_p95: data.metrics.extraction_start_latency?.thresholds?.['p(95)<200']?.ok || false,
            throughput_target: data.metrics.extraction_throughput_records_per_min?.thresholds?.['p(50)>20000']?.ok || false,
        },
    };

    console.log('\n' + '='.repeat(70));
    console.log('PERFORMANCE TEST RESULTS');
    console.log('='.repeat(70));
    console.log(`\nExtractions Completed: ${summary.metrics.extraction.completed}`);
    console.log(`Total Records Extracted: ${summary.metrics.extraction.total_records}`);
    console.log(`Throughput: ${summary.metrics.extraction.records_per_minute} records/min`);
    console.log(`Throughput (p50): ${summary.metrics.extraction.throughput_p50} records/min`);
    console.log(`Throughput (p95): ${summary.metrics.extraction.throughput_p95} records/min`);
    console.log(`\nLATENCY METRICS:`);
    console.log(`  Creation (avg): ${summary.metrics.extraction.creation_latency_avg}ms`);
    console.log(`  Creation (p95): ${summary.metrics.extraction.creation_latency_p95}ms`);
    console.log(`  Start (avg): ${summary.metrics.extraction.start_latency_avg}ms`);
    console.log(`  Start (p95): ${summary.metrics.extraction.start_latency_p95}ms`);
    console.log(`  Status (avg): ${summary.metrics.extraction.status_latency_avg}ms`);
    console.log(`  Status (p95): ${summary.metrics.extraction.status_latency_p95}ms`);
    console.log(`\nERROR RATE: ${summary.metrics.extraction.error_rate}%`);
    console.log(`Cache Hits: ${summary.metrics.extraction.cache_hits}`);
    console.log(`\nTARGET ACHIEVEMENT:`);
    console.log(`  ✓ Throughput > 20k records/min: ${summary.thresholds.throughput_target ? 'PASS' : 'FAIL'}`);
    console.log(`  ✓ p95 Latency < 250ms: ${summary.thresholds.http_req_duration_p95 ? 'PASS' : 'FAIL'}`);
    console.log(`  ✓ p99 Latency < 500ms: ${summary.thresholds.http_req_duration_p99 ? 'PASS' : 'FAIL'}`);
    console.log(`  ✓ Error Rate < 1%: ${summary.thresholds.http_req_failed ? 'PASS' : 'FAIL'}`);
    console.log('='.repeat(70) + '\n');

    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        [`load-test-results/performance-${timestamp}.json`]: JSON.stringify(summary, null, 2),
        [`load-test-results/performance-${timestamp}-full.json`]: JSON.stringify(data, null, 2),
    };
}
