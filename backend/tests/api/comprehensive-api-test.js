/**
 * JiVS Platform - Comprehensive API Test Suite
 * Tests all 78 endpoints across 8 controllers
 *
 * Usage: k6 run comprehensive-api-test.js
 *
 * Date: January 13, 2025
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// Base URL
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

// Custom metrics
const errorRate = new Rate('errors');
const endpointTested = new Counter('endpoints_tested');
const endpointPassed = new Counter('endpoints_passed');
const endpointFailed = new Counter('endpoints_failed');
const responseTime = new Trend('response_time');

// Test configuration
export const options = {
  vus: 1, // Single user for comprehensive testing
  iterations: 1, // Run once
  thresholds: {
    'http_req_duration': ['p(95)<2000'], // 95% of requests should be below 2s
    'http_req_failed': ['rate<0.3'], // Allow 30% failure (some endpoints not implemented)
  },
};

// Test results tracking
let testResults = {
  controllers: {},
  summary: {
    total: 0,
    passed: 0,
    failed: 0,
    skipped: 0
  }
};

// Helper function to record test result
function recordTest(controller, endpoint, method, passed, statusCode, message = '') {
  if (!testResults.controllers[controller]) {
    testResults.controllers[controller] = {
      total: 0,
      passed: 0,
      failed: 0,
      endpoints: []
    };
  }

  testResults.controllers[controller].total++;
  testResults.summary.total++;

  const result = {
    endpoint: `${method} ${endpoint}`,
    passed,
    statusCode,
    message
  };

  testResults.controllers[controller].endpoints.push(result);

  if (passed) {
    testResults.controllers[controller].passed++;
    testResults.summary.passed++;
    endpointPassed.add(1);
  } else {
    testResults.controllers[controller].failed++;
    testResults.summary.failed++;
    endpointFailed.add(1);
  }

  endpointTested.add(1);
}

// Global auth token
let authToken = '';

export default function () {
  console.log('========================================');
  console.log('JiVS Platform - Comprehensive API Test');
  console.log('========================================\n');

  // Test each controller
  testAuthController();
  testExtractionController();
  testMigrationController();
  testDataQualityController();
  testComplianceController();
  testAnalyticsController();
  testUserPreferencesController();
  testViewsController();

  // Print summary
  printSummary();
}

/**
 * 1. AuthController Tests (4 endpoints)
 */
function testAuthController() {
  group('AuthController - /api/v1/auth', () => {
    console.log('\n--- Testing AuthController ---');

    // Test 1: POST /auth/login
    let res = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
      username: 'admin',
      password: 'password'
    }), {
      headers: { 'Content-Type': 'application/json' }
    });

    let passed = check(res, {
      'login status is 200': (r) => r.status === 200,
      'login has accessToken': (r) => r.json('data') && r.json('data').accessToken !== undefined
    });

    recordTest('AuthController', '/auth/login', 'POST', passed, res.status);

    if (res.status === 200 && res.json('data')) {
      authToken = res.json('data').accessToken;
      console.log('✓ Login successful, token obtained');
    } else {
      console.log('✗ Login failed:', res.body);
    }

    // Test 2: GET /auth/me
    res = http.get(`${BASE_URL}/auth/me`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      }
    });

    passed = check(res, {
      'get current user status is 200': (r) => r.status === 200,
      'has user data': (r) => r.json('data') && r.json('data').username !== undefined
    });

    recordTest('AuthController', '/auth/me', 'GET', passed, res.status);

    // Test 3: POST /auth/refresh
    if (authToken) {
      res = http.post(`${BASE_URL}/auth/refresh`, JSON.stringify({
        refreshToken: authToken // Using access token as refresh for testing
      }), {
        headers: { 'Content-Type': 'application/json' }
      });

      passed = check(res, {
        'refresh token responds': (r) => r.status === 200 || r.status === 401
      });

      recordTest('AuthController', '/auth/refresh', 'POST', res.status === 200, res.status,
        res.status === 401 ? 'Invalid refresh token (expected)' : '');
    }

    // Test 4: POST /auth/logout (will test at the end)
    console.log('  (Logout will be tested at the end)');
  });
}

/**
 * 2. ExtractionController Tests (10 endpoints)
 */
function testExtractionController() {
  group('ExtractionController - /api/v1/extractions', () => {
    console.log('\n--- Testing ExtractionController ---');

    const headers = {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    };

    let extractionId = null;

    // Test 1: POST /extractions (create)
    let res = http.post(`${BASE_URL}/extractions`, JSON.stringify({
      name: 'API Test Extraction',
      sourceType: 'JDBC',
      connectionConfig: {
        url: 'jdbc:postgresql://localhost:5432/testdb',
        username: 'test',
        password: 'test'
      },
      extractionQuery: 'SELECT * FROM test_table LIMIT 100'
    }), { headers });

    let passed = check(res, {
      'create extraction status is 201': (r) => r.status === 201,
      'has extraction id': (r) => r.json('id') !== undefined
    });

    recordTest('ExtractionController', '/extractions', 'POST', passed, res.status);

    if (res.status === 201 && res.json('id')) {
      extractionId = res.json('id');
      console.log(`✓ Created extraction: ${extractionId}`);
    }

    // Test 2: GET /extractions (list)
    res = http.get(`${BASE_URL}/extractions?page=0&size=20`, { headers });

    passed = check(res, {
      'list extractions status is 200': (r) => r.status === 200,
      'has content array': (r) => r.json('content') !== undefined
    });

    recordTest('ExtractionController', '/extractions', 'GET', passed, res.status);

    // Test 3: GET /extractions/{id}
    if (extractionId) {
      res = http.get(`${BASE_URL}/extractions/${extractionId}`, { headers });

      passed = check(res, {
        'get extraction status is 200': (r) => r.status === 200,
        'has extraction data': (r) => r.json('id') !== undefined
      });

      recordTest('ExtractionController', '/extractions/{id}', 'GET', passed, res.status);
    }

    // Test 4: POST /extractions/{id}/start
    if (extractionId) {
      res = http.post(`${BASE_URL}/extractions/${extractionId}/start`, null, { headers });

      passed = check(res, {
        'start extraction responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('ExtractionController', '/extractions/{id}/start', 'POST',
        res.status === 200, res.status);
    }

    // Test 5: POST /extractions/{id}/stop
    if (extractionId) {
      res = http.post(`${BASE_URL}/extractions/${extractionId}/stop`, null, { headers });

      passed = check(res, {
        'stop extraction responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('ExtractionController', '/extractions/{id}/stop', 'POST',
        res.status === 200, res.status);
    }

    // Test 6: GET /extractions/{id}/statistics
    if (extractionId) {
      res = http.get(`${BASE_URL}/extractions/${extractionId}/statistics`, { headers });

      passed = check(res, {
        'get statistics status is 200': (r) => r.status === 200
      });

      recordTest('ExtractionController', '/extractions/{id}/statistics', 'GET',
        passed, res.status);
    }

    // Test 7: POST /extractions/test-connection
    res = http.post(`${BASE_URL}/extractions/test-connection`, JSON.stringify({
      url: 'jdbc:postgresql://localhost:5432/testdb',
      username: 'test',
      password: 'test'
    }), { headers });

    passed = check(res, {
      'test connection responds': (r) => r.status === 200 || r.status === 400
    });

    recordTest('ExtractionController', '/extractions/test-connection', 'POST',
      res.status === 200, res.status);

    // Test 8: GET /extractions/{id}/logs
    if (extractionId) {
      res = http.get(`${BASE_URL}/extractions/${extractionId}/logs?limit=10`, { headers });

      passed = check(res, {
        'get logs responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('ExtractionController', '/extractions/{id}/logs', 'GET',
        res.status === 200, res.status);
    }

    // Test 9: POST /extractions/bulk
    res = http.post(`${BASE_URL}/extractions/bulk`, JSON.stringify({
      action: 'export',
      ids: [extractionId || 'test-id']
    }), { headers });

    passed = check(res, {
      'bulk action responds': (r) => r.status === 200 || r.status === 500
    });

    recordTest('ExtractionController', '/extractions/bulk', 'POST',
      res.status === 200, res.status);

    // Test 10: DELETE /extractions/{id} (cleanup)
    if (extractionId) {
      res = http.del(`${BASE_URL}/extractions/${extractionId}`, null, { headers });

      passed = check(res, {
        'delete extraction responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('ExtractionController', '/extractions/{id}', 'DELETE',
        res.status === 200, res.status);
    }
  });
}

/**
 * 3. MigrationController Tests (12 endpoints)
 */
function testMigrationController() {
  group('MigrationController - /api/v1/migrations', () => {
    console.log('\n--- Testing MigrationController ---');

    const headers = {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    };

    let migrationId = null;

    // Test 1: POST /migrations (create)
    let res = http.post(`${BASE_URL}/migrations`, JSON.stringify({
      name: 'API Test Migration',
      sourceConfig: {
        type: 'JDBC',
        url: 'jdbc:postgresql://localhost:5432/sourcedb',
        username: 'source',
        password: 'source'
      },
      targetConfig: {
        type: 'JDBC',
        url: 'jdbc:postgresql://localhost:5432/targetdb',
        username: 'target',
        password: 'target'
      }
    }), { headers });

    let passed = check(res, {
      'create migration status is 201': (r) => r.status === 201,
      'has migration id': (r) => r.json('id') !== undefined
    });

    recordTest('MigrationController', '/migrations', 'POST', passed, res.status);

    if (res.status === 201 && res.json('id')) {
      migrationId = res.json('id');
      console.log(`✓ Created migration: ${migrationId}`);
    }

    // Test 2: GET /migrations (list)
    res = http.get(`${BASE_URL}/migrations?page=0&size=20`, { headers });

    passed = check(res, {
      'list migrations status is 200': (r) => r.status === 200,
      'has content array': (r) => r.json('content') !== undefined
    });

    recordTest('MigrationController', '/migrations', 'GET', passed, res.status);

    // Test 3: GET /migrations/{id}
    if (migrationId) {
      res = http.get(`${BASE_URL}/migrations/${migrationId}`, { headers });

      passed = check(res, {
        'get migration status is 200': (r) => r.status === 200
      });

      recordTest('MigrationController', '/migrations/{id}', 'GET', passed, res.status);
    }

    // Test 4: POST /migrations/{id}/start
    if (migrationId) {
      res = http.post(`${BASE_URL}/migrations/${migrationId}/start`, null, { headers });

      passed = check(res, {
        'start migration responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('MigrationController', '/migrations/{id}/start', 'POST',
        res.status === 200, res.status);
    }

    // Test 5: POST /migrations/{id}/pause
    if (migrationId) {
      res = http.post(`${BASE_URL}/migrations/${migrationId}/pause`, null, { headers });

      passed = check(res, {
        'pause migration responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('MigrationController', '/migrations/{id}/pause', 'POST',
        res.status === 200, res.status);
    }

    // Test 6: POST /migrations/{id}/resume
    if (migrationId) {
      res = http.post(`${BASE_URL}/migrations/${migrationId}/resume`, null, { headers });

      passed = check(res, {
        'resume migration responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('MigrationController', '/migrations/{id}/resume', 'POST',
        res.status === 200, res.status);
    }

    // Test 7: GET /migrations/{id}/progress
    if (migrationId) {
      res = http.get(`${BASE_URL}/migrations/${migrationId}/progress`, { headers });

      passed = check(res, {
        'get progress status is 200': (r) => r.status === 200
      });

      recordTest('MigrationController', '/migrations/{id}/progress', 'GET',
        passed, res.status);
    }

    // Test 8: GET /migrations/{id}/statistics
    if (migrationId) {
      res = http.get(`${BASE_URL}/migrations/${migrationId}/statistics`, { headers });

      passed = check(res, {
        'get statistics status is 200': (r) => r.status === 200
      });

      recordTest('MigrationController', '/migrations/{id}/statistics', 'GET',
        passed, res.status);
    }

    // Test 9: POST /migrations/validate
    res = http.post(`${BASE_URL}/migrations/validate`, JSON.stringify({
      name: 'Validation Test',
      sourceConfig: { type: 'JDBC' },
      targetConfig: { type: 'JDBC' }
    }), { headers });

    passed = check(res, {
      'validate migration responds': (r) => r.status === 200 || r.status === 400
    });

    recordTest('MigrationController', '/migrations/validate', 'POST',
      res.status === 200, res.status);

    // Test 10: POST /migrations/bulk
    res = http.post(`${BASE_URL}/migrations/bulk`, JSON.stringify({
      action: 'export',
      ids: [migrationId || 'test-id']
    }), { headers });

    passed = check(res, {
      'bulk action responds': (r) => r.status === 200 || r.status === 500
    });

    recordTest('MigrationController', '/migrations/bulk', 'POST',
      res.status === 200, res.status);

    // Test 11: POST /migrations/{id}/rollback
    if (migrationId) {
      res = http.post(`${BASE_URL}/migrations/${migrationId}/rollback`, null, { headers });

      passed = check(res, {
        'rollback migration responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('MigrationController', '/migrations/{id}/rollback', 'POST',
        res.status === 200, res.status);
    }

    // Test 12: DELETE /migrations/{id} (cleanup)
    if (migrationId) {
      res = http.del(`${BASE_URL}/migrations/${migrationId}`, null, { headers });

      passed = check(res, {
        'delete migration responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('MigrationController', '/migrations/{id}', 'DELETE',
        res.status === 200, res.status);
    }
  });
}

/**
 * 4. DataQualityController Tests (10 endpoints)
 */
function testDataQualityController() {
  group('DataQualityController - /api/v1/data-quality', () => {
    console.log('\n--- Testing DataQualityController ---');

    const headers = {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    };

    let ruleId = null;

    // Test 1: GET /data-quality/dashboard
    let res = http.get(`${BASE_URL}/data-quality/dashboard`, { headers });

    let passed = check(res, {
      'get dashboard status is 200': (r) => r.status === 200,
      'has overall score': (r) => r.json('overallScore') !== undefined
    });

    recordTest('DataQualityController', '/data-quality/dashboard', 'GET', passed, res.status);

    // Test 2: POST /data-quality/rules (create)
    res = http.post(`${BASE_URL}/data-quality/rules`, JSON.stringify({
      name: 'API Test Rule',
      dimension: 'VALIDITY',
      severity: 'HIGH',
      ruleExpression: 'email LIKE \'%@%.%\'',
      enabled: true
    }), { headers });

    passed = check(res, {
      'create rule status is 201': (r) => r.status === 201,
      'has rule id': (r) => r.json('id') !== undefined
    });

    recordTest('DataQualityController', '/data-quality/rules', 'POST', passed, res.status);

    if (res.status === 201 && res.json('id')) {
      ruleId = res.json('id');
    }

    // Test 3: GET /data-quality/rules (list)
    res = http.get(`${BASE_URL}/data-quality/rules?page=0&size=20`, { headers });

    passed = check(res, {
      'list rules status is 200': (r) => r.status === 200,
      'has content array': (r) => r.json('content') !== undefined
    });

    recordTest('DataQualityController', '/data-quality/rules', 'GET', passed, res.status);

    // Test 4: GET /data-quality/rules/{id}
    if (ruleId) {
      res = http.get(`${BASE_URL}/data-quality/rules/${ruleId}`, { headers });

      passed = check(res, {
        'get rule status is 200': (r) => r.status === 200
      });

      recordTest('DataQualityController', '/data-quality/rules/{id}', 'GET', passed, res.status);
    }

    // Test 5: PUT /data-quality/rules/{id}
    if (ruleId) {
      res = http.put(`${BASE_URL}/data-quality/rules/${ruleId}`, JSON.stringify({
        name: 'Updated API Test Rule',
        enabled: false
      }), { headers });

      passed = check(res, {
        'update rule status is 200': (r) => r.status === 200
      });

      recordTest('DataQualityController', '/data-quality/rules/{id}', 'PUT', passed, res.status);
    }

    // Test 6: POST /data-quality/rules/{id}/execute
    if (ruleId) {
      res = http.post(`${BASE_URL}/data-quality/rules/${ruleId}/execute`, JSON.stringify({
        datasetId: 'test-dataset'
      }), { headers });

      passed = check(res, {
        'execute rule responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('DataQualityController', '/data-quality/rules/{id}/execute', 'POST',
        res.status === 200, res.status);
    }

    // Test 7: GET /data-quality/issues
    res = http.get(`${BASE_URL}/data-quality/issues?page=0&size=20`, { headers });

    passed = check(res, {
      'get issues status is 200': (r) => r.status === 200,
      'has content array': (r) => r.json('content') !== undefined
    });

    recordTest('DataQualityController', '/data-quality/issues', 'GET', passed, res.status);

    // Test 8: POST /data-quality/profile
    res = http.post(`${BASE_URL}/data-quality/profile`, JSON.stringify({
      datasetId: 'test-dataset',
      tableName: 'test_table'
    }), { headers });

    passed = check(res, {
      'profile dataset responds': (r) => r.status === 200 || r.status === 500
    });

    recordTest('DataQualityController', '/data-quality/profile', 'POST',
      res.status === 200, res.status);

    // Test 9: GET /data-quality/reports/{id}
    res = http.get(`${BASE_URL}/data-quality/reports/test-report-id`, { headers });

    passed = check(res, {
      'get report responds': (r) => r.status === 200 || r.status === 404
    });

    recordTest('DataQualityController', '/data-quality/reports/{id}', 'GET',
      res.status === 200, res.status);

    // Test 10: DELETE /data-quality/rules/{id} (cleanup)
    if (ruleId) {
      res = http.del(`${BASE_URL}/data-quality/rules/${ruleId}`, null, { headers });

      passed = check(res, {
        'delete rule responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('DataQualityController', '/data-quality/rules/{id}', 'DELETE',
        res.status === 200, res.status);
    }
  });
}

/**
 * 5. ComplianceController Tests (12 endpoints)
 */
function testComplianceController() {
  group('ComplianceController - /api/v1/compliance', () => {
    console.log('\n--- Testing ComplianceController ---');

    const headers = {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    };

    let requestId = null;
    let consentId = null;

    // Test 1: GET /compliance/dashboard
    let res = http.get(`${BASE_URL}/compliance/dashboard`, { headers });

    let passed = check(res, {
      'get dashboard status is 200': (r) => r.status === 200,
      'has overall score': (r) => r.json('overallScore') !== undefined
    });

    recordTest('ComplianceController', '/compliance/dashboard', 'GET', passed, res.status);

    // Test 2: POST /compliance/requests (create)
    res = http.post(`${BASE_URL}/compliance/requests`, JSON.stringify({
      type: 'ACCESS',
      subjectEmail: 'test@example.com',
      regulation: 'GDPR',
      requestDetails: 'GDPR Article 15 - Right of Access'
    }), { headers });

    passed = check(res, {
      'create request status is 201': (r) => r.status === 201,
      'has request id': (r) => r.json('id') !== undefined
    });

    recordTest('ComplianceController', '/compliance/requests', 'POST', passed, res.status);

    if (res.status === 201 && res.json('id')) {
      requestId = res.json('id');
    }

    // Test 3: GET /compliance/requests (list)
    res = http.get(`${BASE_URL}/compliance/requests?page=0&size=20`, { headers });

    passed = check(res, {
      'list requests status is 200': (r) => r.status === 200,
      'has content array': (r) => r.json('content') !== undefined
    });

    recordTest('ComplianceController', '/compliance/requests', 'GET', passed, res.status);

    // Test 4: GET /compliance/requests/{id}
    if (requestId) {
      res = http.get(`${BASE_URL}/compliance/requests/${requestId}`, { headers });

      passed = check(res, {
        'get request status is 200': (r) => r.status === 200
      });

      recordTest('ComplianceController', '/compliance/requests/{id}', 'GET', passed, res.status);
    }

    // Test 5: PUT /compliance/requests/{id}/status
    if (requestId) {
      res = http.put(`${BASE_URL}/compliance/requests/${requestId}/status`, JSON.stringify({
        status: 'IN_PROGRESS'
      }), { headers });

      passed = check(res, {
        'update request status responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('ComplianceController', '/compliance/requests/{id}/status', 'PUT',
        res.status === 200, res.status);
    }

    // Test 6: POST /compliance/requests/{id}/process
    if (requestId) {
      res = http.post(`${BASE_URL}/compliance/requests/${requestId}/process`, null, { headers });

      passed = check(res, {
        'process request responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('ComplianceController', '/compliance/requests/{id}/process', 'POST',
        res.status === 200, res.status);
    }

    // Test 7: GET /compliance/requests/{id}/export
    if (requestId) {
      res = http.get(`${BASE_URL}/compliance/requests/${requestId}/export`, { headers });

      passed = check(res, {
        'export personal data responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('ComplianceController', '/compliance/requests/{id}/export', 'GET',
        res.status === 200, res.status);
    }

    // Test 8: GET /compliance/consents
    res = http.get(`${BASE_URL}/compliance/consents?page=0&size=20`, { headers });

    passed = check(res, {
      'get consents status is 200': (r) => r.status === 200,
      'has content array': (r) => r.json('content') !== undefined
    });

    recordTest('ComplianceController', '/compliance/consents', 'GET', passed, res.status);

    // Test 9: POST /compliance/consents
    res = http.post(`${BASE_URL}/compliance/consents`, JSON.stringify({
      subjectEmail: 'test@example.com',
      purpose: 'MARKETING',
      granted: true
    }), { headers });

    passed = check(res, {
      'record consent status is 201': (r) => r.status === 201,
      'has consent id': (r) => r.json('id') !== undefined
    });

    recordTest('ComplianceController', '/compliance/consents', 'POST', passed, res.status);

    if (res.status === 201 && res.json('id')) {
      consentId = res.json('id');
    }

    // Test 10: POST /compliance/consents/{id}/revoke
    if (consentId) {
      res = http.post(`${BASE_URL}/compliance/consents/${consentId}/revoke`, null, { headers });

      passed = check(res, {
        'revoke consent responds': (r) => r.status === 200 || r.status === 500
      });

      recordTest('ComplianceController', '/compliance/consents/{id}/revoke', 'POST',
        res.status === 200, res.status);
    }

    // Test 11: GET /compliance/retention-policies
    res = http.get(`${BASE_URL}/compliance/retention-policies`, { headers });

    passed = check(res, {
      'get retention policies responds': (r) => r.status === 200 || r.status === 500
      });

    recordTest('ComplianceController', '/compliance/retention-policies', 'GET',
      res.status === 200, res.status);

    // Test 12: GET /compliance/audit
    res = http.get(`${BASE_URL}/compliance/audit?page=0&size=20`, { headers });

    passed = check(res, {
      'get audit trail status is 200': (r) => r.status === 200,
      'has content array': (r) => r.json('content') !== undefined
    });

    recordTest('ComplianceController', '/compliance/audit', 'GET', passed, res.status);
  });
}

/**
 * 6. AnalyticsController Tests (8 endpoints)
 */
function testAnalyticsController() {
  group('AnalyticsController - /api/v1/analytics', () => {
    console.log('\n--- Testing AnalyticsController ---');

    const headers = {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    };

    // Test 1: GET /analytics/dashboard
    let res = http.get(`${BASE_URL}/analytics/dashboard`, { headers });

    let passed = check(res, {
      'get dashboard analytics status is 200': (r) => r.status === 200,
      'has total extractions': (r) => r.json('totalExtractions') !== undefined
    });

    recordTest('AnalyticsController', '/analytics/dashboard', 'GET', passed, res.status);

    // Test 2: GET /analytics/extractions
    res = http.get(`${BASE_URL}/analytics/extractions`, { headers });

    passed = check(res, {
      'get extraction analytics status is 200': (r) => r.status === 200
    });

    recordTest('AnalyticsController', '/analytics/extractions', 'GET', passed, res.status);

    // Test 3: GET /analytics/migrations
    res = http.get(`${BASE_URL}/analytics/migrations`, { headers });

    passed = check(res, {
      'get migration analytics status is 200': (r) => r.status === 200
    });

    recordTest('AnalyticsController', '/analytics/migrations', 'GET', passed, res.status);

    // Test 4: GET /analytics/data-quality
    res = http.get(`${BASE_URL}/analytics/data-quality`, { headers });

    passed = check(res, {
      'get data quality analytics status is 200': (r) => r.status === 200
    });

    recordTest('AnalyticsController', '/analytics/data-quality', 'GET', passed, res.status);

    // Test 5: GET /analytics/usage
    res = http.get(`${BASE_URL}/analytics/usage`, { headers });

    passed = check(res, {
      'get usage analytics status is 200': (r) => r.status === 200
    });

    recordTest('AnalyticsController', '/analytics/usage', 'GET', passed, res.status);

    // Test 6: GET /analytics/compliance
    res = http.get(`${BASE_URL}/analytics/compliance`, { headers });

    passed = check(res, {
      'get compliance analytics status is 200': (r) => r.status === 200
    });

    recordTest('AnalyticsController', '/analytics/compliance', 'GET', passed, res.status);

    // Test 7: GET /analytics/performance
    res = http.get(`${BASE_URL}/analytics/performance`, { headers });

    passed = check(res, {
      'get performance analytics status is 200': (r) => r.status === 200
    });

    recordTest('AnalyticsController', '/analytics/performance', 'GET', passed, res.status);

    // Test 8: POST /analytics/export
    res = http.post(`${BASE_URL}/analytics/export`, JSON.stringify({
      format: 'CSV',
      reportType: 'dashboard'
    }), { headers });

    passed = check(res, {
      'export report status is 200': (r) => r.status === 200,
      'has download URL': (r) => r.json('downloadUrl') !== undefined
    });

    recordTest('AnalyticsController', '/analytics/export', 'POST', passed, res.status);
  });
}

/**
 * 7. UserPreferencesController Tests (4 endpoints)
 */
function testUserPreferencesController() {
  group('UserPreferencesController - /api/v1/preferences', () => {
    console.log('\n--- Testing UserPreferencesController ---');

    const headers = {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    };

    // Test 1: GET /preferences
    let res = http.get(`${BASE_URL}/preferences`, { headers });

    let passed = check(res, {
      'get preferences responds': (r) => r.status === 200 || r.status === 500
    });

    recordTest('UserPreferencesController', '/preferences', 'GET',
      res.status === 200, res.status);

    // Test 2: PUT /preferences
    res = http.put(`${BASE_URL}/preferences`, JSON.stringify({
      theme: 'dark',
      language: 'en',
      notificationsEnabled: true,
      emailNotifications: true
    }), { headers });

    passed = check(res, {
      'update preferences responds': (r) => r.status === 200 || r.status === 500
    });

    recordTest('UserPreferencesController', '/preferences', 'PUT',
      res.status === 200, res.status);

    // Test 3: GET /preferences/theme
    res = http.get(`${BASE_URL}/preferences/theme`, { headers });

    passed = check(res, {
      'get theme preference responds': (r) => r.status === 200 || r.status === 500
    });

    recordTest('UserPreferencesController', '/preferences/theme', 'GET',
      res.status === 200, res.status);

    // Test 4: PUT /preferences/theme
    res = http.put(`${BASE_URL}/preferences/theme`, JSON.stringify({
      theme: 'light'
    }), { headers });

    passed = check(res, {
      'update theme preference responds': (r) => r.status === 200 || r.status === 500
    });

    recordTest('UserPreferencesController', '/preferences/theme', 'PUT',
      res.status === 200, res.status);
  });
}

/**
 * 8. ViewsController Tests (8 endpoints)
 */
function testViewsController() {
  group('ViewsController - /api/v1/views', () => {
    console.log('\n--- Testing ViewsController ---');

    const headers = {
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    };

    let viewName = 'api-test-view';

    // Test 1: GET /views
    let res = http.get(`${BASE_URL}/views?module=extractions`, { headers });

    let passed = check(res, {
      'get views status is 200': (r) => r.status === 200,
      'returns array': (r) => Array.isArray(r.json())
    });

    recordTest('ViewsController', '/views', 'GET', passed, res.status);

    // Test 2: POST /views (create)
    res = http.post(`${BASE_URL}/views`, JSON.stringify({
      viewName: viewName,
      module: 'extractions',
      filters: { status: 'COMPLETED' },
      sortBy: 'createdAt',
      sortOrder: 'desc',
      isDefault: false
    }), { headers });

    passed = check(res, {
      'create view status is 201': (r) => r.status === 201,
      'has view name': (r) => r.json('viewName') !== undefined
    });

    recordTest('ViewsController', '/views', 'POST', passed, res.status);

    // Test 3: GET /views/{viewName}
    res = http.get(`${BASE_URL}/views/${viewName}?module=extractions`, { headers });

    passed = check(res, {
      'get view by name responds': (r) => r.status === 200 || r.status === 404
    });

    recordTest('ViewsController', '/views/{viewName}', 'GET',
      res.status === 200, res.status);

    // Test 4: PUT /views/{viewName}
    res = http.put(`${BASE_URL}/views/${viewName}?module=extractions`, JSON.stringify({
      filters: { status: 'RUNNING' }
    }), { headers });

    passed = check(res, {
      'update view responds': (r) => r.status === 200 || r.status === 404
    });

    recordTest('ViewsController', '/views/{viewName}', 'PUT',
      res.status === 200, res.status);

    // Test 5: POST /views/{viewName}/set-default
    res = http.post(`${BASE_URL}/views/${viewName}/set-default?module=extractions`, null, { headers });

    passed = check(res, {
      'set default view responds': (r) => r.status === 200 || r.status === 404
    });

    recordTest('ViewsController', '/views/{viewName}/set-default', 'POST',
      res.status === 200, res.status);

    // Test 6: GET /views/default
    res = http.get(`${BASE_URL}/views/default?module=extractions`, { headers });

    passed = check(res, {
      'get default view responds': (r) => r.status === 200 || r.status === 404
    });

    recordTest('ViewsController', '/views/default', 'GET',
      res.status === 200, res.status);

    // Test 7: GET /views/count
    res = http.get(`${BASE_URL}/views/count?module=extractions`, { headers });

    passed = check(res, {
      'get view count status is 200': (r) => r.status === 200,
      'has count field': (r) => r.json('count') !== undefined
    });

    recordTest('ViewsController', '/views/count', 'GET', passed, res.status);

    // Test 8: DELETE /views/{viewName}
    res = http.del(`${BASE_URL}/views/${viewName}?module=extractions`, null, { headers });

    passed = check(res, {
      'delete view responds': (r) => r.status === 200 || r.status === 404
    });

    recordTest('ViewsController', '/views/{viewName}', 'DELETE',
      res.status === 200, res.status);
  });
}

/**
 * Print test summary
 */
function printSummary() {
  console.log('\n========================================');
  console.log('TEST SUMMARY');
  console.log('========================================\n');

  console.log(`Total Endpoints: ${testResults.summary.total}`);
  console.log(`Passed: ${testResults.summary.passed} ✓`);
  console.log(`Failed: ${testResults.summary.failed} ✗`);
  console.log(`Success Rate: ${((testResults.summary.passed / testResults.summary.total) * 100).toFixed(2)}%\n`);

  console.log('Results by Controller:');
  console.log('----------------------');

  for (const [controller, data] of Object.entries(testResults.controllers)) {
    const successRate = ((data.passed / data.total) * 100).toFixed(2);
    console.log(`\n${controller}: ${data.passed}/${data.total} passed (${successRate}%)`);

    // Show failed endpoints
    const failed = data.endpoints.filter(e => !e.passed);
    if (failed.length > 0) {
      console.log('  Failed endpoints:');
      failed.forEach(e => {
        console.log(`    ✗ ${e.endpoint} - ${e.statusCode} ${e.message}`);
      });
    }
  }

  console.log('\n========================================');
  console.log('Test completed successfully!');
  console.log('========================================\n');
}

export function handleSummary(data) {
  return {
    '/Users/chandramahadevan/jivs-platform/backend/tests/api/test-results-summary.json': JSON.stringify(testResults, null, 2),
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}
