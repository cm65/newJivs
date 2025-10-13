/**
 * JiVS Platform - Comprehensive k6 Load Testing Suite
 *
 * Layer 5: Performance Testing
 *
 * This suite validates that the JiVS platform meets performance requirements:
 * - Throughput: 1000+ requests/second
 * - Latency: p95 < 200ms, p99 < 500ms
 * - Error Rate: < 1%
 * - Concurrent Users: 100-500
 *
 * Test Scenarios:
 * 1. Load Test - Normal expected load
 * 2. Stress Test - Push to breaking point
 * 3. Spike Test - Sudden traffic surge
 * 4. Soak Test - Extended duration
 *
 * @since Day 10 of Continuous Testing Implementation
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

// Custom metrics
const errorRate = new Rate('errors');
const apiLatency = new Trend('api_latency');
const extractionDuration = new Trend('extraction_duration');
const migrationDuration = new Trend('migration_duration');
const dataQualityDuration = new Trend('data_quality_duration');
const complianceRequests = new Counter('compliance_requests');
const concurrentExtractions = new Gauge('concurrent_extractions');
const concurrentMigrations = new Gauge('concurrent_migrations');

// Test data
const testUsers = [
  { username: 'admin', password: 'password', role: 'ADMIN' },
  { username: 'engineer1', password: 'password', role: 'DATA_ENGINEER' },
  { username: 'compliance1', password: 'password', role: 'COMPLIANCE_OFFICER' },
  { username: 'viewer1', password: 'password', role: 'VIEWER' }
];

const sourceTypes = ['JDBC', 'SAP', 'FILE', 'API'];
const migrationPhases = ['PLANNING', 'VALIDATION', 'EXTRACTION', 'TRANSFORMATION', 'LOADING', 'VERIFICATION', 'CLEANUP'];
const qualityDimensions = ['COMPLETENESS', 'ACCURACY', 'CONSISTENCY', 'VALIDITY', 'UNIQUENESS', 'TIMELINESS'];
const complianceTypes = ['ACCESS', 'ERASURE', 'RECTIFICATION', 'PORTABILITY'];

// Test scenarios configuration
export const options = {
  // Scenario 1: Load Test (normal expected load)
  scenarios: {
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 },   // Ramp up to 50 users
        { duration: '5m', target: 50 },   // Stay at 50 users
        { duration: '2m', target: 100 },  // Ramp up to 100 users
        { duration: '5m', target: 100 },  // Stay at 100 users
        { duration: '2m', target: 0 },    // Ramp down to 0
      ],
      gracefulRampDown: '30s',
      exec: 'loadTest',
    },

    // Scenario 2: Stress Test (find breaking point)
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 100 },  // Ramp to 100 users
        { duration: '2m', target: 200 },  // Increase to 200
        { duration: '2m', target: 300 },  // Push to 300
        { duration: '2m', target: 400 },  // Push harder to 400
        { duration: '2m', target: 500 },  // Maximum load
        { duration: '3m', target: 0 },    // Recovery
      ],
      startTime: '17m',  // Start after load test
      exec: 'stressTest',
    },

    // Scenario 3: Spike Test (sudden surge)
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },   // Baseline load
        { duration: '5s', target: 200 },   // Sudden spike
        { duration: '2m', target: 200 },   // Hold spike
        { duration: '10s', target: 10 },   // Quick recovery
        { duration: '1m', target: 10 },    // Stable baseline
      ],
      startTime: '30m',  // Start after stress test
      exec: 'spikeTest',
    },

    // Scenario 4: Soak Test (extended duration)
    soak_test: {
      executor: 'constant-vus',
      vus: 50,
      duration: '10m',
      startTime: '37m',  // Start after spike test
      exec: 'soakTest',
    }
  },

  // Thresholds (Quality Gates)
  thresholds: {
    // General thresholds
    http_req_duration: ['p(95)<200', 'p(99)<500'],  // 95% of requests under 200ms
    http_req_failed: ['rate<0.01'],                  // Error rate under 1%
    errors: ['rate<0.01'],                           // Custom error rate under 1%

    // API-specific thresholds
    'http_req_duration{api:auth}': ['p(95)<100'],
    'http_req_duration{api:extraction}': ['p(95)<300'],
    'http_req_duration{api:migration}': ['p(95)<500'],
    'http_req_duration{api:quality}': ['p(95)<400'],
    'http_req_duration{api:compliance}': ['p(95)<600'],
    'http_req_duration{api:analytics}': ['p(95)<800'],

    // Custom metric thresholds
    api_latency: ['p(95)<250', 'p(99)<500'],
    extraction_duration: ['p(95)<5000'],
    migration_duration: ['p(95)<10000'],
    data_quality_duration: ['p(95)<3000'],
  },
};

// Setup function - runs once per VU
export function setup() {
  console.log('🚀 Starting JiVS Performance Test Suite');
  console.log(`📍 Target: ${BASE_URL}`);
  console.log(`👥 Max VUs: 500`);
  console.log(`⏱️  Total Duration: ~47 minutes`);

  // Verify backend is accessible
  const healthCheck = http.get(`${BASE_URL}/actuator/health`);
  check(healthCheck, {
    'Backend is healthy': (r) => r.status === 200,
  });

  if (healthCheck.status !== 200) {
    throw new Error(`Backend not healthy: ${healthCheck.status}`);
  }

  return { startTime: Date.now() };
}

// Helper function to authenticate
function authenticate(user) {
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({
      username: user.username,
      password: user.password,
    }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { api: 'auth' },
    }
  );

  check(loginRes, {
    'Login successful': (r) => r.status === 200,
    'Token received': (r) => r.json('accessToken') !== '',
  });

  errorRate.add(loginRes.status !== 200);

  return loginRes.json('accessToken') || '';
}

// Helper function to create headers with auth
function getHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
}

// Scenario 1: Load Test
export function loadTest() {
  const user = randomItem(testUsers);
  const token = authenticate(user);
  const headers = getHeaders(token);

  group('Extraction Operations', () => {
    // Create extraction
    const extractionData = {
      name: `LoadTest_Extract_${Date.now()}`,
      sourceType: randomItem(sourceTypes),
      connectionConfig: {
        url: 'jdbc:postgresql://localhost:5432/test',
        username: 'test_user',
        password: 'test_pass',
      },
      extractionQuery: 'SELECT * FROM customers LIMIT 1000',
    };

    const createRes = http.post(
      `${BASE_URL}/api/v1/extractions`,
      JSON.stringify(extractionData),
      { headers, tags: { api: 'extraction' } }
    );

    check(createRes, {
      'Extraction created': (r) => r.status === 201,
    });
    errorRate.add(createRes.status !== 201);
    apiLatency.add(createRes.timings.duration);

    if (createRes.status === 201) {
      const extractionId = createRes.json('id');
      concurrentExtractions.add(1);

      // Start extraction
      const startRes = http.post(
        `${BASE_URL}/api/v1/extractions/${extractionId}/start`,
        null,
        { headers, tags: { api: 'extraction' } }
      );

      check(startRes, {
        'Extraction started': (r) => r.status === 200,
      });
      extractionDuration.add(startRes.timings.duration);

      // Get extraction status
      sleep(1);
      const statusRes = http.get(
        `${BASE_URL}/api/v1/extractions/${extractionId}`,
        { headers, tags: { api: 'extraction' } }
      );

      check(statusRes, {
        'Status retrieved': (r) => r.status === 200,
      });

      concurrentExtractions.add(-1);
    }
  });

  group('Migration Operations', () => {
    // Create migration
    const migrationData = {
      name: `LoadTest_Migration_${Date.now()}`,
      sourceSystem: 'Oracle Database 12c',
      targetSystem: 'PostgreSQL 15',
      sourceConfig: {
        connectionUrl: 'jdbc:oracle:thin:@localhost:1521:ORCL',
        schema: 'PROD',
      },
      targetConfig: {
        connectionUrl: 'jdbc:postgresql://localhost:5432/target',
        schema: 'public',
      },
    };

    const createRes = http.post(
      `${BASE_URL}/api/v1/migrations`,
      JSON.stringify(migrationData),
      { headers, tags: { api: 'migration' } }
    );

    check(createRes, {
      'Migration created': (r) => r.status === 201,
    });
    errorRate.add(createRes.status !== 201);
    apiLatency.add(createRes.timings.duration);

    if (createRes.status === 201) {
      const migrationId = createRes.json('id');
      concurrentMigrations.add(1);

      // Start migration
      const startRes = http.post(
        `${BASE_URL}/api/v1/migrations/${migrationId}/start`,
        null,
        { headers, tags: { api: 'migration' } }
      );

      check(startRes, {
        'Migration started': (r) => r.status === 200,
      });
      migrationDuration.add(startRes.timings.duration);

      // Get progress
      sleep(2);
      const progressRes = http.get(
        `${BASE_URL}/api/v1/migrations/${migrationId}/progress`,
        { headers, tags: { api: 'migration' } }
      );

      check(progressRes, {
        'Progress retrieved': (r) => r.status === 200,
      });

      concurrentMigrations.add(-1);
    }
  });

  group('Data Quality Operations', () => {
    // Create quality rule
    const ruleData = {
      name: `LoadTest_Rule_${Date.now()}`,
      dimension: randomItem(qualityDimensions),
      ruleType: 'NULL_CHECK',
      configuration: {
        table: 'customers',
        column: 'email',
        allowNull: false,
      },
      enabled: true,
      severity: 3,
    };

    const createRes = http.post(
      `${BASE_URL}/api/v1/data-quality/rules`,
      JSON.stringify(ruleData),
      { headers, tags: { api: 'quality' } }
    );

    check(createRes, {
      'Quality rule created': (r) => r.status === 201,
    });
    errorRate.add(createRes.status !== 201);
    dataQualityDuration.add(createRes.timings.duration);

    // Get quality dashboard
    const dashboardRes = http.get(
      `${BASE_URL}/api/v1/data-quality/dashboard`,
      { headers, tags: { api: 'quality' } }
    );

    check(dashboardRes, {
      'Quality dashboard loaded': (r) => r.status === 200,
    });
  });

  // Only compliance officers can create compliance requests
  if (user.role === 'COMPLIANCE_OFFICER' || user.role === 'ADMIN') {
    group('Compliance Operations', () => {
      // Create compliance request
      const requestData = {
        requestType: randomItem(complianceTypes),
        regulation: Math.random() > 0.5 ? 'GDPR' : 'CCPA',
        subjectEmail: `user${Date.now()}@example.com`,
        subjectName: `Test User ${Date.now()}`,
        requestData: {
          reason: 'Performance test request',
          identityVerified: true,
        },
      };

      const createRes = http.post(
        `${BASE_URL}/api/v1/compliance/requests`,
        JSON.stringify(requestData),
        { headers, tags: { api: 'compliance' } }
      );

      check(createRes, {
        'Compliance request created': (r) => r.status === 201,
      });
      errorRate.add(createRes.status !== 201);
      complianceRequests.add(1);

      // Get compliance dashboard
      const dashboardRes = http.get(
        `${BASE_URL}/api/v1/compliance/dashboard`,
        { headers, tags: { api: 'compliance' } }
      );

      check(dashboardRes, {
        'Compliance dashboard loaded': (r) => r.status === 200,
      });
    });
  }

  group('Analytics Operations', () => {
    // Get various analytics endpoints
    const endpoints = [
      '/api/v1/analytics/dashboard',
      '/api/v1/analytics/extractions',
      '/api/v1/analytics/migrations',
      '/api/v1/analytics/data-quality',
      '/api/v1/analytics/compliance',
    ];

    const endpoint = randomItem(endpoints);
    const analyticsRes = http.get(
      `${BASE_URL}${endpoint}`,
      { headers, tags: { api: 'analytics' } }
    );

    check(analyticsRes, {
      'Analytics data retrieved': (r) => r.status === 200,
    });
    errorRate.add(analyticsRes.status !== 200);
    apiLatency.add(analyticsRes.timings.duration);
  });

  sleep(Math.random() * 2 + 1);  // Random think time between 1-3 seconds
}

// Scenario 2: Stress Test
export function stressTest() {
  // Similar to load test but with more aggressive operations
  const user = randomItem(testUsers);
  const token = authenticate(user);
  const headers = getHeaders(token);

  // Perform multiple operations in parallel
  const responses = http.batch([
    ['GET', `${BASE_URL}/api/v1/extractions?page=0&size=100`, null, { headers, tags: { api: 'extraction' } }],
    ['GET', `${BASE_URL}/api/v1/migrations?page=0&size=100`, null, { headers, tags: { api: 'migration' } }],
    ['GET', `${BASE_URL}/api/v1/data-quality/rules?page=0&size=100`, null, { headers, tags: { api: 'quality' } }],
    ['GET', `${BASE_URL}/api/v1/compliance/requests?page=0&size=100`, null, { headers, tags: { api: 'compliance' } }],
    ['GET', `${BASE_URL}/api/v1/analytics/performance`, null, { headers, tags: { api: 'analytics' } }],
  ]);

  responses.forEach(res => {
    check(res, {
      'Batch request successful': (r) => r.status === 200,
    });
    errorRate.add(res.status !== 200);
    apiLatency.add(res.timings.duration);
  });

  sleep(0.5);  // Minimal think time for stress
}

// Scenario 3: Spike Test
export function spikeTest() {
  // Rapid-fire requests with minimal processing
  const user = testUsers[0];  // Use same user to simulate spike
  const token = authenticate(user);
  const headers = getHeaders(token);

  // Hammer the most critical endpoints
  for (let i = 0; i < 5; i++) {
    const res = http.get(
      `${BASE_URL}/api/v1/analytics/dashboard`,
      { headers, tags: { api: 'analytics', test: 'spike' } }
    );

    check(res, {
      'Spike request handled': (r) => r.status === 200,
    });
    errorRate.add(res.status !== 200);

    // No sleep - maximum pressure
  }
}

// Scenario 4: Soak Test
export function soakTest() {
  // Steady load for extended duration
  const user = randomItem(testUsers);
  const token = authenticate(user);
  const headers = getHeaders(token);

  // Rotate through different operations
  const operations = [
    () => http.get(`${BASE_URL}/api/v1/extractions`, { headers, tags: { api: 'extraction', test: 'soak' } }),
    () => http.get(`${BASE_URL}/api/v1/migrations`, { headers, tags: { api: 'migration', test: 'soak' } }),
    () => http.get(`${BASE_URL}/api/v1/data-quality/dashboard`, { headers, tags: { api: 'quality', test: 'soak' } }),
    () => http.get(`${BASE_URL}/api/v1/compliance/dashboard`, { headers, tags: { api: 'compliance', test: 'soak' } }),
    () => http.get(`${BASE_URL}/api/v1/analytics/dashboard`, { headers, tags: { api: 'analytics', test: 'soak' } }),
  ];

  const operation = randomItem(operations);
  const res = operation();

  check(res, {
    'Soak request successful': (r) => r.status === 200,
    'Response time acceptable': (r) => r.timings.duration < 1000,
  });
  errorRate.add(res.status !== 200);
  apiLatency.add(res.timings.duration);

  sleep(2);  // Steady pace
}

// Teardown function - runs once after all tests
export function handleSummary(data) {
  const duration = (Date.now() - data.setup.startTime) / 1000;

  console.log('');
  console.log('================================================================================');
  console.log('                     PERFORMANCE TEST SUMMARY                                  ');
  console.log('================================================================================');
  console.log(`Total Duration: ${duration}s`);
  console.log(`Total Requests: ${data.metrics.http_reqs.values.count}`);
  console.log(`Request Rate: ${data.metrics.http_reqs.values.rate} req/s`);
  console.log('');
  console.log('Latency Metrics:');
  console.log(`  p50: ${data.metrics.http_req_duration.values['p(50)']}ms`);
  console.log(`  p95: ${data.metrics.http_req_duration.values['p(95)']}ms`);
  console.log(`  p99: ${data.metrics.http_req_duration.values['p(99)']}ms`);
  console.log('');
  console.log('Error Metrics:');
  console.log(`  HTTP Errors: ${data.metrics.http_req_failed.values.rate * 100}%`);
  console.log(`  Custom Errors: ${data.metrics.errors.values.rate * 100}%`);
  console.log('');
  console.log('Business Metrics:');
  console.log(`  Total Extractions: ${data.metrics.concurrent_extractions.values.value}`);
  console.log(`  Total Migrations: ${data.metrics.concurrent_migrations.values.value}`);
  console.log(`  Compliance Requests: ${data.metrics.compliance_requests.values.count}`);
  console.log('================================================================================');

  // Generate HTML and JSON reports
  return {
    'test-reports/performance/summary.html': htmlReport(data),
    'test-reports/performance/summary.json': JSON.stringify(data, null, 2),
  };
}

function htmlReport(data) {
  return `
<!DOCTYPE html>
<html>
<head>
  <title>JiVS Performance Test Report</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 20px; }
    h1 { color: #333; }
    .metric { margin: 10px 0; }
    .passed { color: green; }
    .failed { color: red; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
    th { background-color: #f2f2f2; }
  </style>
</head>
<body>
  <h1>JiVS Platform - Performance Test Results</h1>
  <h2>Test Configuration</h2>
  <table>
    <tr><th>Scenario</th><th>Max VUs</th><th>Duration</th><th>Status</th></tr>
    <tr><td>Load Test</td><td>100</td><td>16m</td><td class="${data.metrics.errors.values.rate < 0.01 ? 'passed' : 'failed'}">
      ${data.metrics.errors.values.rate < 0.01 ? '✅ Passed' : '❌ Failed'}</td></tr>
    <tr><td>Stress Test</td><td>500</td><td>12m</td><td>Completed</td></tr>
    <tr><td>Spike Test</td><td>200</td><td>4m</td><td>Completed</td></tr>
    <tr><td>Soak Test</td><td>50</td><td>10m</td><td>Completed</td></tr>
  </table>

  <h2>Performance Metrics</h2>
  <table>
    <tr><th>Metric</th><th>Value</th><th>Target</th><th>Status</th></tr>
    <tr><td>p95 Latency</td><td>${Math.round(data.metrics.http_req_duration.values['p(95)'])}ms</td>
        <td>&lt;200ms</td><td class="${data.metrics.http_req_duration.values['p(95)'] < 200 ? 'passed' : 'failed'}">
        ${data.metrics.http_req_duration.values['p(95)'] < 200 ? '✅' : '❌'}</td></tr>
    <tr><td>p99 Latency</td><td>${Math.round(data.metrics.http_req_duration.values['p(99)'])}ms</td>
        <td>&lt;500ms</td><td class="${data.metrics.http_req_duration.values['p(99)'] < 500 ? 'passed' : 'failed'}">
        ${data.metrics.http_req_duration.values['p(99)'] < 500 ? '✅' : '❌'}</td></tr>
    <tr><td>Error Rate</td><td>${(data.metrics.errors.values.rate * 100).toFixed(2)}%</td>
        <td>&lt;1%</td><td class="${data.metrics.errors.values.rate < 0.01 ? 'passed' : 'failed'}">
        ${data.metrics.errors.values.rate < 0.01 ? '✅' : '❌'}</td></tr>
    <tr><td>Throughput</td><td>${Math.round(data.metrics.http_reqs.values.rate)} req/s</td>
        <td>&gt;1000 req/s</td><td class="${data.metrics.http_reqs.values.rate > 1000 ? 'passed' : 'failed'}">
        ${data.metrics.http_reqs.values.rate > 1000 ? '✅' : '❌'}</td></tr>
  </table>

  <h2>API Performance Breakdown</h2>
  <table>
    <tr><th>API</th><th>Requests</th><th>p95 Latency</th><th>Error Rate</th></tr>
    <tr><td>Authentication</td><td>${data.metrics.http_reqs.values.count}</td><td>-</td><td>-</td></tr>
    <tr><td>Extractions</td><td>-</td><td>-</td><td>-</td></tr>
    <tr><td>Migrations</td><td>-</td><td>-</td><td>-</td></tr>
    <tr><td>Data Quality</td><td>-</td><td>-</td><td>-</td></tr>
    <tr><td>Compliance</td><td>-</td><td>-</td><td>-</td></tr>
    <tr><td>Analytics</td><td>-</td><td>-</td><td>-</td></tr>
  </table>

  <p><i>Generated: ${new Date().toISOString()}</i></p>
</body>
</html>
  `;
}