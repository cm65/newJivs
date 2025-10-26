/**
 * JiVS Platform - Railway Production Load Test
 *
 * Usage:
 *   k6 run railway-production-load-test.js
 *
 * Environment Variables:
 *   BASE_URL - Backend URL (default: Railway production)
 *   TEST_USERNAME - Username (default: admin)
 *   TEST_PASSWORD - Password (default: password)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const apiResponseTime = new Trend('api_response_time');
const successfulRequests = new Counter('successful_requests');

// Test configuration
export const options = {
  stages: [
    { duration: '1m', target: 10 },   // Ramp up to 10 users
    { duration: '3m', target: 20 },   // Increase to 20 users
    { duration: '2m', target: 30 },   // Peak at 30 users
    { duration: '2m', target: 10 },   // Ramp down to 10
    { duration: '1m', target: 0 },    // Cool down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500', 'p(99)<1000'],
    'http_req_failed': ['rate<0.05'],
    'errors': ['rate<0.05'],
  },
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'https://jivs-backend-production.up.railway.app';
const TEST_USERNAME = __ENV.TEST_USERNAME || 'admin';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'password';

/**
 * Setup: Authenticate once per VU
 */
export function setup() {
  console.log(`Testing against: ${BASE_URL}`);

  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    username: TEST_USERNAME,
    password: TEST_PASSWORD
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  check(loginRes, {
    'login successful': (r) => r.status === 200,
  });

  if (loginRes.status !== 200) {
    console.error('Login failed:', loginRes.body);
    throw new Error('Authentication failed');
  }

  const token = loginRes.json('data.accessToken');
  return { token };
}

/**
 * Main test scenario
 */
export default function(data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  // Test 1: Dashboard Analytics (most common read operation)
  const dashboardStart = new Date();
  const dashboardRes = http.get(
    `${BASE_URL}/api/v1/analytics/dashboard`,
    { headers }
  );

  const dashboardDuration = new Date() - dashboardStart;
  apiResponseTime.add(dashboardDuration);

  check(dashboardRes, {
    'dashboard loaded': (r) => r.status === 200,
    'dashboard has data': (r) => r.json('extractionSuccessRate') !== undefined,
  }) || errorRate.add(1);

  if (dashboardRes.status === 200) {
    successfulRequests.add(1);
  }

  sleep(1);

  // Test 2: List Documents (common read operation)
  const docsStart = new Date();
  const docsRes = http.get(
    `${BASE_URL}/api/v1/documents?page=0&size=20`,
    { headers }
  );

  const docsDuration = new Date() - docsStart;
  apiResponseTime.add(docsDuration);

  check(docsRes, {
    'documents listed': (r) => r.status === 200,
    'documents paginated': (r) => r.json('content') !== undefined,
  }) || errorRate.add(1);

  if (docsRes.status === 200) {
    successfulRequests.add(1);
  }

  sleep(1);

  // Test 3: List Extractions
  const extractionsRes = http.get(
    `${BASE_URL}/api/v1/extractions?page=0&size=10`,
    { headers }
  );

  check(extractionsRes, {
    'extractions listed': (r) => r.status === 200,
  }) || errorRate.add(1);

  if (extractionsRes.status === 200) {
    successfulRequests.add(1);
  }

  sleep(1);

  // Test 4: Create Extraction (write operation)
  if (__VU % 5 === 0) {  // Only 20% of users create extractions
    const extractionPayload = JSON.stringify({
      name: `Load Test Extraction ${__VU}-${__ITER}`,
      description: `Created by k6 load test at ${new Date().toISOString()}`,
      sourceType: 'POSTGRESQL',
      connectionConfig: {
        url: 'jdbc:postgresql://localhost:5432/testdb',
        username: 'test',
        password: 'test'
      }
    });

    const createStart = new Date();
    const createRes = http.post(
      `${BASE_URL}/api/v1/extractions`,
      extractionPayload,
      { headers }
    );

    const createDuration = new Date() - createStart;
    apiResponseTime.add(createDuration);

    check(createRes, {
      'extraction created': (r) => r.status === 200 || r.status === 201,
      'extraction has ID': (r) => r.json('id') !== undefined,
    }) || errorRate.add(1);

    if (createRes.status === 200 || createRes.status === 201) {
      successfulRequests.add(1);
    }

    sleep(1);
  }

  // Test 5: List Migrations
  const migrationsRes = http.get(
    `${BASE_URL}/api/v1/migrations?page=0&size=10`,
    { headers }
  );

  check(migrationsRes, {
    'migrations listed': (r) => r.status === 200,
  }) || errorRate.add(1);

  if (migrationsRes.status === 200) {
    successfulRequests.add(1);
  }

  sleep(1);

  // Test 6: List Compliance Requests
  const complianceRes = http.get(
    `${BASE_URL}/api/v1/compliance/requests?page=0&size=10`,
    { headers }
  );

  check(complianceRes, {
    'compliance requests listed': (r) => r.status === 200,
  }) || errorRate.add(1);

  if (complianceRes.status === 200) {
    successfulRequests.add(1);
  }

  sleep(2);
}

/**
 * Teardown: Summary
 */
export function teardown(data) {
  console.log('Load test completed successfully');
}

/**
 * Test Scenarios:
 *
 * 1. Dashboard Analytics (High frequency)
 *    - GET /api/v1/analytics/dashboard
 *    - Expected: < 200ms (p95)
 *
 * 2. Document Management (Medium frequency)
 *    - GET /api/v1/documents
 *    - Expected: < 150ms (p95)
 *
 * 3. Extraction Management (Medium frequency)
 *    - GET /api/v1/extractions
 *    - POST /api/v1/extractions (20% of users)
 *    - Expected: < 300ms (p95)
 *
 * 4. Migration Management (Low frequency)
 *    - GET /api/v1/migrations
 *    - Expected: < 200ms (p95)
 *
 * 5. Compliance Management (Low frequency)
 *    - GET /api/v1/compliance/requests
 *    - Expected: < 200ms (p95)
 *
 * Expected Results:
 * - Throughput: > 100 req/sec (30 concurrent users)
 * - Error Rate: < 5%
 * - p95 Response Time: < 500ms
 * - p99 Response Time: < 1000ms
 *
 * Pass Criteria:
 * - All thresholds met
 * - No 500 errors
 * - Authentication working
 * - All endpoints responsive
 */
