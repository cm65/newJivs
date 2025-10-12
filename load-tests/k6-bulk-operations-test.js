/**
 * JiVS Platform - Bulk Operations Load Testing Script (k6)
 *
 * This script performs comprehensive load testing of bulk operations endpoints
 * Testing bulk start, stop, pause, resume, and delete operations
 *
 * Usage:
 *   k6 run load-tests/k6-bulk-operations-test.js
 *   k6 run --vus 50 --duration 30s load-tests/k6-bulk-operations-test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 load-tests/k6-bulk-operations-test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const bulkOperationDuration = new Trend('bulk_operation_duration');
const bulkOperationSuccessRate = new Rate('bulk_operation_success_rate');
const bulkItemsProcessed = new Counter('bulk_items_processed');
const bulkBatchSize = new Gauge('bulk_batch_size');
const apiCallCounter = new Counter('api_calls');

// Test configuration
export const options = {
    scenarios: {
        // Scenario 1: Steady load on bulk extractions
        bulk_extractions: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 10 },  // Ramp up to 10 users
                { duration: '3m', target: 20 },  // Ramp up to 20 users
                { duration: '5m', target: 20 },  // Stay at 20 users
                { duration: '1m', target: 0 },   // Ramp down
            ],
            gracefulRampDown: '30s',
            exec: 'testBulkExtractions',
        },
        // Scenario 2: Steady load on bulk migrations
        bulk_migrations: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 5 },   // Ramp up to 5 users
                { duration: '3m', target: 10 },  // Ramp up to 10 users
                { duration: '5m', target: 10 },  // Stay at 10 users
                { duration: '1m', target: 0 },   // Ramp down
            ],
            gracefulRampDown: '30s',
            exec: 'testBulkMigrations',
        },
        // Scenario 3: Spike test for bulk operations
        bulk_spike_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 5 },   // Ramp up normally
                { duration: '10s', target: 50 },  // Spike to 50 users
                { duration: '1m', target: 50 },   // Stay at spike
                { duration: '30s', target: 5 },   // Return to normal
                { duration: '30s', target: 0 },   // Ramp down
            ],
            gracefulRampDown: '30s',
            exec: 'testBulkSpikeLoad',
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<2000', 'p(99)<5000'],         // Bulk ops can take longer
        'http_req_failed': ['rate<0.02'],                           // Error rate < 2% (more lenient)
        'bulk_operation_duration': ['p(95)<3000', 'p(99)<6000'],   // Bulk operation specific
        'bulk_operation_success_rate': ['rate>0.95'],              // 95%+ success rate
        'errors': ['rate<0.05'],                                    // Custom error rate < 5%
    },
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const USERNAME = __ENV.TEST_USERNAME || 'admin';
const PASSWORD = __ENV.TEST_PASSWORD || 'password';

// Test data sizes
const SMALL_BATCH = 5;
const MEDIUM_BATCH = 10;
const LARGE_BATCH = 20;

/**
 * Setup function - runs once before tests
 */
export function setup() {
    console.log('Setting up bulk operations load test...');
    console.log(`Target: ${BASE_URL}`);

    // Authenticate once during setup
    const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
        username: USERNAME,
        password: PASSWORD,
    }), {
        headers: { 'Content-Type': 'application/json' },
    });

    if (loginRes.status !== 200) {
        throw new Error('Setup failed: Unable to authenticate');
    }

    const authToken = loginRes.json('accessToken');
    console.log('Setup complete - authenticated successfully');

    return {
        baseUrl: BASE_URL,
        authToken: authToken,
    };
}

/**
 * Test bulk extraction operations
 */
export function testBulkExtractions(data) {
    const headers = {
        'Authorization': `Bearer ${data.authToken}`,
        'Content-Type': 'application/json',
    };

    group('Bulk Extractions - Start', function () {
        // Create test extraction IDs (simulated)
        const extractionIds = generateIds(MEDIUM_BATCH);

        const bulkStartPayload = {
            ids: extractionIds,
            action: 'start',
        };

        const startTime = Date.now();
        const bulkStartRes = http.post(
            `${data.baseUrl}/extractions/bulk`,
            JSON.stringify(bulkStartPayload),
            {
                headers: headers,
                tags: { name: 'BulkStartExtractions' },
            }
        );

        const duration = Date.now() - startTime;
        bulkOperationDuration.add(duration);
        bulkBatchSize.add(MEDIUM_BATCH);
        apiCallCounter.add(1);

        const success = check(bulkStartRes, {
            'bulk start status is 200': (r) => r.status === 200,
            'bulk start returns success count': (r) => r.json('successCount') !== undefined,
            'bulk start processes all items': (r) => r.json('totalProcessed') === extractionIds.length,
        });

        bulkOperationSuccessRate.add(success ? 1 : 0);
        errorRate.add(success ? 0 : 1);

        if (success) {
            const responseData = bulkStartRes.json();
            bulkItemsProcessed.add(responseData.successCount || 0);

            check(responseData, {
                'bulk start success rate > 80%': (r) => (r.successCount / r.totalProcessed) > 0.8,
                'bulk start processing time < 3s': () => duration < 3000,
            });
        }
    });

    sleep(1);

    group('Bulk Extractions - Stop', function () {
        const extractionIds = generateIds(MEDIUM_BATCH);

        const bulkStopPayload = {
            ids: extractionIds,
            action: 'stop',
        };

        const startTime = Date.now();
        const bulkStopRes = http.post(
            `${data.baseUrl}/extractions/bulk`,
            JSON.stringify(bulkStopPayload),
            {
                headers: headers,
                tags: { name: 'BulkStopExtractions' },
            }
        );

        const duration = Date.now() - startTime;
        bulkOperationDuration.add(duration);
        apiCallCounter.add(1);

        const success = check(bulkStopRes, {
            'bulk stop status is 200': (r) => r.status === 200,
            'bulk stop returns success count': (r) => r.json('successCount') !== undefined,
        });

        bulkOperationSuccessRate.add(success ? 1 : 0);
        errorRate.add(success ? 0 : 1);

        if (success) {
            const responseData = bulkStopRes.json();
            bulkItemsProcessed.add(responseData.successCount || 0);
        }
    });

    sleep(1);

    group('Bulk Extractions - Delete', function () {
        const extractionIds = generateIds(SMALL_BATCH); // Smaller batch for deletes

        const bulkDeletePayload = {
            ids: extractionIds,
            action: 'delete',
        };

        const startTime = Date.now();
        const bulkDeleteRes = http.post(
            `${data.baseUrl}/extractions/bulk`,
            JSON.stringify(bulkDeletePayload),
            {
                headers: headers,
                tags: { name: 'BulkDeleteExtractions' },
            }
        );

        const duration = Date.now() - startTime;
        bulkOperationDuration.add(duration);
        apiCallCounter.add(1);

        const success = check(bulkDeleteRes, {
            'bulk delete status is 200': (r) => r.status === 200,
            'bulk delete returns success count': (r) => r.json('successCount') !== undefined,
        });

        bulkOperationSuccessRate.add(success ? 1 : 0);
        errorRate.add(success ? 0 : 1);

        if (success) {
            const responseData = bulkDeleteRes.json();
            bulkItemsProcessed.add(responseData.successCount || 0);
        }
    });

    sleep(2); // Think time
}

/**
 * Test bulk migration operations
 */
export function testBulkMigrations(data) {
    const headers = {
        'Authorization': `Bearer ${data.authToken}`,
        'Content-Type': 'application/json',
    };

    group('Bulk Migrations - Start', function () {
        const migrationIds = generateIds(SMALL_BATCH);

        const bulkStartPayload = {
            ids: migrationIds,
            action: 'start',
        };

        const startTime = Date.now();
        const bulkStartRes = http.post(
            `${data.baseUrl}/migrations/bulk`,
            JSON.stringify(bulkStartPayload),
            {
                headers: headers,
                tags: { name: 'BulkStartMigrations' },
            }
        );

        const duration = Date.now() - startTime;
        bulkOperationDuration.add(duration);
        bulkBatchSize.add(SMALL_BATCH);
        apiCallCounter.add(1);

        const success = check(bulkStartRes, {
            'bulk start migrations status is 200': (r) => r.status === 200,
            'bulk start migrations returns success count': (r) => r.json('successCount') !== undefined,
        });

        bulkOperationSuccessRate.add(success ? 1 : 0);
        errorRate.add(success ? 0 : 1);

        if (success) {
            const responseData = bulkStartRes.json();
            bulkItemsProcessed.add(responseData.successCount || 0);
        }
    });

    sleep(1);

    group('Bulk Migrations - Pause', function () {
        const migrationIds = generateIds(SMALL_BATCH);

        const bulkPausePayload = {
            ids: migrationIds,
            action: 'pause',
        };

        const bulkPauseRes = http.post(
            `${data.baseUrl}/migrations/bulk`,
            JSON.stringify(bulkPausePayload),
            {
                headers: headers,
                tags: { name: 'BulkPauseMigrations' },
            }
        );

        apiCallCounter.add(1);

        const success = check(bulkPauseRes, {
            'bulk pause status is 200': (r) => r.status === 200,
        });

        bulkOperationSuccessRate.add(success ? 1 : 0);
        errorRate.add(success ? 0 : 1);
    });

    sleep(1);

    group('Bulk Migrations - Resume', function () {
        const migrationIds = generateIds(SMALL_BATCH);

        const bulkResumePayload = {
            ids: migrationIds,
            action: 'resume',
        };

        const bulkResumeRes = http.post(
            `${data.baseUrl}/migrations/bulk`,
            JSON.stringify(bulkResumePayload),
            {
                headers: headers,
                tags: { name: 'BulkResumeMigrations' },
            }
        );

        apiCallCounter.add(1);

        const success = check(bulkResumeRes, {
            'bulk resume status is 200': (r) => r.status === 200,
        });

        bulkOperationSuccessRate.add(success ? 1 : 0);
        errorRate.add(success ? 0 : 1);
    });

    sleep(2); // Think time
}

/**
 * Spike load test for bulk operations
 */
export function testBulkSpikeLoad(data) {
    const headers = {
        'Authorization': `Bearer ${data.authToken}`,
        'Content-Type': 'application/json',
    };

    // Alternate between extractions and migrations
    const useExtractions = Math.random() > 0.5;
    const endpoint = useExtractions ? 'extractions' : 'migrations';
    const batchSize = LARGE_BATCH;

    group(`Bulk ${endpoint} - Spike Test`, function () {
        const ids = generateIds(batchSize);

        const bulkPayload = {
            ids: ids,
            action: 'start',
        };

        const startTime = Date.now();
        const bulkRes = http.post(
            `${data.baseUrl}/${endpoint}/bulk`,
            JSON.stringify(bulkPayload),
            {
                headers: headers,
                tags: { name: `BulkSpike_${endpoint}` },
            }
        );

        const duration = Date.now() - startTime;
        bulkOperationDuration.add(duration);
        bulkBatchSize.add(batchSize);
        apiCallCounter.add(1);

        const success = check(bulkRes, {
            'spike bulk operation status is 200': (r) => r.status === 200,
            'spike bulk operation completes < 5s': () => duration < 5000,
        });

        bulkOperationSuccessRate.add(success ? 1 : 0);
        errorRate.add(success ? 0 : 1);

        if (success) {
            const responseData = bulkRes.json();
            bulkItemsProcessed.add(responseData.successCount || 0);
        }
    });

    sleep(0.5); // Shorter think time during spike
}

/**
 * Helper function to generate random IDs
 */
function generateIds(count) {
    const ids = [];
    for (let i = 0; i < count; i++) {
        // Generate UUIDs or numeric IDs
        ids.push(`test-id-${Math.floor(Math.random() * 100000)}`);
    }
    return ids;
}

/**
 * Teardown function - runs once after tests
 */
export function teardown(data) {
    console.log('Bulk operations load test completed');
}

/**
 * Handle summary - custom summary formatting
 */
export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'bulk-operations-test-results.json': JSON.stringify(data),
        'bulk-operations-test-summary.html': htmlReport(data),
    };
}

function textSummary(data, options) {
    const indent = options.indent || '';
    let summary = '\n' + indent + '='.repeat(70) + '\n';
    summary += indent + 'JiVS Platform - Bulk Operations Load Test Summary\n';
    summary += indent + '='.repeat(70) + '\n\n';

    summary += indent + `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
    summary += indent + `Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n`;
    summary += indent + `Failed Requests: ${data.metrics.http_req_failed.values.passes || 0}\n`;
    summary += indent + `Average Duration: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
    summary += indent + `p95 Duration: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
    summary += indent + `p99 Duration: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n\n`;

    summary += indent + 'Bulk Operations Metrics:\n';
    summary += indent + `  Total Items Processed: ${data.metrics.bulk_items_processed.values.count}\n`;
    summary += indent + `  Bulk Operation Success Rate: ${(data.metrics.bulk_operation_success_rate.values.rate * 100).toFixed(2)}%\n`;
    summary += indent + `  Avg Bulk Operation Duration: ${data.metrics.bulk_operation_duration.values.avg.toFixed(2)}ms\n`;
    summary += indent + `  p95 Bulk Operation Duration: ${data.metrics.bulk_operation_duration.values['p(95)'].toFixed(2)}ms\n`;
    summary += indent + `  p99 Bulk Operation Duration: ${data.metrics.bulk_operation_duration.values['p(99)'].toFixed(2)}ms\n`;

    summary += '\n' + indent + '-'.repeat(70) + '\n';

    return summary;
}

function htmlReport(data) {
    return `
<!DOCTYPE html>
<html>
<head>
    <title>JiVS Bulk Operations Load Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }
        h2 { color: #34495e; margin-top: 30px; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #3498db; color: white; }
        tr:nth-child(even) { background-color: #f8f9fa; }
        .metric { margin: 15px 0; padding: 10px; background: #ecf0f1; border-radius: 4px; }
        .metric-label { font-weight: bold; color: #2c3e50; }
        .metric-value { color: #27ae60; font-size: 1.2em; }
        .pass { color: #27ae60; font-weight: bold; }
        .fail { color: #e74c3c; font-weight: bold; }
        .warning { color: #f39c12; font-weight: bold; }
    </style>
</head>
<body>
    <div class="container">
        <h1>JiVS Platform - Bulk Operations Load Test Report</h1>
        <p><strong>Test completed:</strong> ${new Date().toISOString()}</p>

        <h2>Summary Statistics</h2>
        <table>
            <tr><th>Metric</th><th>Value</th></tr>
            <tr><td>Total Requests</td><td>${data.metrics.http_reqs.values.count}</td></tr>
            <tr><td>Request Rate</td><td>${data.metrics.http_reqs.values.rate.toFixed(2)}/s</td></tr>
            <tr><td>Failed Requests</td><td class="${(data.metrics.http_req_failed.values.passes || 0) > 10 ? 'fail' : 'pass'}">${data.metrics.http_req_failed.values.passes || 0}</td></tr>
            <tr><td>Average Duration</td><td>${data.metrics.http_req_duration.values.avg.toFixed(2)}ms</td></tr>
            <tr><td>p95 Duration</td><td class="${data.metrics.http_req_duration.values['p(95)'] > 2000 ? 'warning' : 'pass'}">${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms</td></tr>
            <tr><td>p99 Duration</td><td class="${data.metrics.http_req_duration.values['p(99)'] > 5000 ? 'fail' : 'pass'}">${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms</td></tr>
        </table>

        <h2>Bulk Operations Metrics</h2>
        <div class="metric">
            <span class="metric-label">Total Items Processed:</span>
            <span class="metric-value">${data.metrics.bulk_items_processed.values.count}</span>
        </div>
        <div class="metric">
            <span class="metric-label">Bulk Operation Success Rate:</span>
            <span class="metric-value ${data.metrics.bulk_operation_success_rate.values.rate < 0.95 ? 'fail' : 'pass'}">
                ${(data.metrics.bulk_operation_success_rate.values.rate * 100).toFixed(2)}%
            </span>
        </div>
        <div class="metric">
            <span class="metric-label">Avg Bulk Operation Duration:</span>
            <span class="metric-value">${data.metrics.bulk_operation_duration.values.avg.toFixed(2)}ms</span>
        </div>
        <div class="metric">
            <span class="metric-label">p95 Bulk Operation Duration:</span>
            <span class="metric-value ${data.metrics.bulk_operation_duration.values['p(95)'] > 3000 ? 'warning' : 'pass'}">
                ${data.metrics.bulk_operation_duration.values['p(95)'].toFixed(2)}ms
            </span>
        </div>
        <div class="metric">
            <span class="metric-label">p99 Bulk Operation Duration:</span>
            <span class="metric-value ${data.metrics.bulk_operation_duration.values['p(99)'] > 6000 ? 'fail' : 'pass'}">
                ${data.metrics.bulk_operation_duration.values['p(99)'].toFixed(2)}ms
            </span>
        </div>

        <h2>Performance Thresholds</h2>
        <div class="metric">
            ${data.metrics.http_req_duration.thresholds['p(95)<2000'].ok ? '<span class="pass">✓</span>' : '<span class="fail">✗</span>'}
            p95 request duration &lt; 2000ms
        </div>
        <div class="metric">
            ${data.metrics.http_req_duration.thresholds['p(99)<5000'].ok ? '<span class="pass">✓</span>' : '<span class="fail">✗</span>'}
            p99 request duration &lt; 5000ms
        </div>
        <div class="metric">
            ${data.metrics.http_req_failed.thresholds['rate<0.02'].ok ? '<span class="pass">✓</span>' : '<span class="fail">✗</span>'}
            HTTP error rate &lt; 2%
        </div>
        <div class="metric">
            ${data.metrics.bulk_operation_success_rate.thresholds['rate>0.95'].ok ? '<span class="pass">✓</span>' : '<span class="fail">✗</span>'}
            Bulk operation success rate &gt; 95%
        </div>
    </div>
</body>
</html>
    `;
}
