/**
 * JiVS Platform Load Testing Script (k6)
 *
 * This script performs comprehensive load testing of the JiVS platform
 * Testing authentication, extractions, migrations, and data quality endpoints
 *
 * Usage:
 *   k6 run load-tests/k6-load-test.js
 *   k6 run --vus 100 --duration 30s load-tests/k6-load-test.js
 *   k6 run --out influxdb=http://localhost:8086/k6 load-tests/k6-load-test.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const loginDuration = new Trend('login_duration');
const extractionDuration = new Trend('extraction_duration');
const apiCallCounter = new Counter('api_calls');

// Test configuration
export const options = {
    stages: [
        { duration: '2m', target: 10 },   // Ramp up to 10 users
        { duration: '5m', target: 50 },   // Ramp up to 50 users
        { duration: '10m', target: 100 }, // Stay at 100 users
        { duration: '5m', target: 50 },   // Ramp down to 50 users
        { duration: '2m', target: 0 },    // Ramp down to 0 users
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500', 'p(99)<1000'], // 95% < 500ms, 99% < 1s
        'http_req_failed': ['rate<0.01'],                  // Error rate < 1%
        'errors': ['rate<0.05'],                           // Custom error rate < 5%
    },
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const USERNAME = __ENV.TEST_USERNAME || 'testuser';
const PASSWORD = __ENV.TEST_PASSWORD || 'TestPassword123!';

// Global state
let authToken = '';

/**
 * Setup function - runs once before tests
 */
export function setup() {
    console.log('Setting up load test...');
    console.log(`Target: ${BASE_URL}`);
    console.log(`VUs: ${__ENV.VUS || 'default'}`);
    return { baseUrl: BASE_URL };
}

/**
 * Main test function
 */
export default function (data) {
    // Login
    group('Authentication', function () {
        const loginRes = http.post(`${data.baseUrl}/auth/login`, JSON.stringify({
            username: USERNAME,
            password: PASSWORD,
        }), {
            headers: { 'Content-Type': 'application/json' },
            tags: { name: 'Login' },
        });

        const loginSuccess = check(loginRes, {
            'login status is 200': (r) => r.status === 200,
            'login has access token': (r) => r.json('data.accessToken') !== undefined,
        });

        loginDuration.add(loginRes.timings.duration);
        apiCallCounter.add(1);

        if (!loginSuccess) {
            errorRate.add(1);
            console.error('Login failed:', loginRes.status, loginRes.body);
            return;
        }

        authToken = loginRes.json('data.accessToken');
        errorRate.add(0);
    });

    // Test extractions endpoint
    group('Extractions', function () {
        const headers = {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json',
        };

        // List extractions
        const listRes = http.get(`${data.baseUrl}/extractions?page=0&size=20`, {
            headers: headers,
            tags: { name: 'ListExtractions' },
        });

        check(listRes, {
            'list extractions status is 200': (r) => r.status === 200,
            'list extractions has data': (r) => r.json('data') !== undefined,
        });

        extractionDuration.add(listRes.timings.duration);
        apiCallCounter.add(1);

        // Get extraction statistics
        const statsRes = http.get(`${data.baseUrl}/extractions/statistics`, {
            headers: headers,
            tags: { name: 'ExtractionStats' },
        });

        check(statsRes, {
            'stats status is 200': (r) => r.status === 200,
        });

        apiCallCounter.add(1);
    });

    // Test migrations endpoint
    group('Migrations', function () {
        const headers = {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json',
        };

        const listRes = http.get(`${data.baseUrl}/migrations?page=0&size=20`, {
            headers: headers,
            tags: { name: 'ListMigrations' },
        });

        check(listRes, {
            'list migrations status is 200': (r) => r.status === 200,
        });

        apiCallCounter.add(1);
    });

    // Test data quality endpoint
    group('Data Quality', function () {
        const headers = {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json',
        };

        const dashboardRes = http.get(`${data.baseUrl}/data-quality/dashboard`, {
            headers: headers,
            tags: { name: 'QualityDashboard' },
        });

        check(dashboardRes, {
            'quality dashboard status is 200': (r) => r.status === 200,
        });

        apiCallCounter.add(1);

        const rulesRes = http.get(`${data.baseUrl}/data-quality/rules?page=0&size=20`, {
            headers: headers,
            tags: { name: 'QualityRules' },
        });

        check(rulesRes, {
            'quality rules status is 200': (r) => r.status === 200,
        });

        apiCallCounter.add(1);
    });

    // Test analytics endpoint
    group('Analytics', function () {
        const headers = {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json',
        };

        const analyticsRes = http.get(`${data.baseUrl}/analytics/dashboard`, {
            headers: headers,
            tags: { name: 'AnalyticsDashboard' },
        });

        check(analyticsRes, {
            'analytics dashboard status is 200': (r) => r.status === 200,
        });

        apiCallCounter.add(1);
    });

    // Think time between iterations
    sleep(1);
}

/**
 * Teardown function - runs once after tests
 */
export function teardown(data) {
    console.log('Load test completed');
}

/**
 * Handle summary - custom summary formatting
 */
export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        'load-test-results.json': JSON.stringify(data),
        'load-test-summary.html': htmlReport(data),
    };
}

function textSummary(data, options) {
    const indent = options.indent || '';
    let summary = '\n' + indent + '='.repeat(60) + '\n';
    summary += indent + 'JiVS Platform Load Test Summary\n';
    summary += indent + '='.repeat(60) + '\n\n';

    summary += indent + `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
    summary += indent + `Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n`;
    summary += indent + `Failed Requests: ${data.metrics.http_req_failed.values.passes || 0}\n`;
    summary += indent + `Average Duration: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
    summary += indent + `p95 Duration: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
    summary += indent + `p99 Duration: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n`;

    summary += '\n' + indent + '-'.repeat(60) + '\n';

    return summary;
}

function htmlReport(data) {
    return `
<!DOCTYPE html>
<html>
<head>
    <title>JiVS Platform Load Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        tr:nth-child(even) { background-color: #f2f2f2; }
        .metric { margin: 10px 0; }
        .pass { color: green; }
        .fail { color: red; }
    </style>
</head>
<body>
    <h1>JiVS Platform Load Test Report</h1>
    <p>Test completed: ${new Date().toISOString()}</p>

    <h2>Summary</h2>
    <table>
        <tr><th>Metric</th><th>Value</th></tr>
        <tr><td>Total Requests</td><td>${data.metrics.http_reqs.values.count}</td></tr>
        <tr><td>Request Rate</td><td>${data.metrics.http_reqs.values.rate.toFixed(2)}/s</td></tr>
        <tr><td>Failed Requests</td><td>${data.metrics.http_req_failed.values.passes || 0}</td></tr>
        <tr><td>Average Duration</td><td>${data.metrics.http_req_duration.values.avg.toFixed(2)}ms</td></tr>
        <tr><td>p95 Duration</td><td>${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms</td></tr>
        <tr><td>p99 Duration</td><td>${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms</td></tr>
    </table>

    <h2>Thresholds</h2>
    <div class="metric">
        ${data.metrics.http_req_duration.thresholds['p(95)<500'].ok ? '<span class="pass">✓</span>' : '<span class="fail">✗</span>'}
        p95 &lt; 500ms
    </div>
    <div class="metric">
        ${data.metrics.http_req_duration.thresholds['p(99)<1000'].ok ? '<span class="pass">✓</span>' : '<span class="fail">✗</span>'}
        p99 &lt; 1000ms
    </div>
    <div class="metric">
        ${data.metrics.http_req_failed.thresholds['rate<0.01'].ok ? '<span class="pass">✓</span>' : '<span class="fail">✗</span>'}
        Error rate &lt; 1%
    </div>
</body>
</html>
    `;
}
