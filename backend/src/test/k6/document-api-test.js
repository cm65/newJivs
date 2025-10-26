import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate } from 'k6/metrics';

/**
 * k6 Load Test for Document Archiving API
 *
 * Usage:
 *   k6 run --vus 10 --duration 30s document-api-test.js
 *   k6 run --vus 50 --duration 2m document-api-test.js
 */

// Custom metrics
const errorRate = new Rate('errors');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },  // Ramp up to 10 users
    { duration: '1m', target: 10 },   // Stay at 10 users
    { duration: '30s', target: 0 },   // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% < 500ms, 99% < 1s
    http_req_failed: ['rate<0.01'],                  // Error rate < 1%
    errors: ['rate<0.05'],                           // Custom error rate < 5%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_PREFIX = '/api/v1/documents';

// Authentication token (obtain via login first)
let authToken = '';

export function setup() {
  // Login to get auth token
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    username: 'admin',
    password: 'password',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (loginRes.status === 200) {
    const body = JSON.parse(loginRes.body);
    authToken = body.accessToken;
    console.log('Authentication successful');
  } else {
    console.error('Authentication failed');
  }

  return { authToken };
}

export default function (data) {
  const token = data.authToken;
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // Test 1: Upload Document
  group('Upload Document', () => {
    const formData = {
      file: http.file('test-doc.txt', 'This is test document content', 'text/plain'),
    };

    const uploadRes = http.post(
      `${BASE_URL}${API_PREFIX}/upload`,
      formData,
      { headers: { 'Authorization': `Bearer ${token}` } }
    );

    const uploadSuccess = check(uploadRes, {
      'upload status is 200': (r) => r.status === 200,
      'upload response has id': (r) => JSON.parse(r.body).id !== undefined,
    });

    errorRate.add(!uploadSuccess);

    if (uploadSuccess) {
      const uploadedDoc = JSON.parse(uploadRes.body);

      // Test 2: Archive Document
      group('Archive Document', () => {
        const archiveRes = http.post(
          `${BASE_URL}${API_PREFIX}/${uploadedDoc.id}/archive?compress=true&storageTier=WARM`,
          null,
          { headers }
        );

        const archiveSuccess = check(archiveRes, {
          'archive status is 200': (r) => r.status === 200,
          'archive response has archiveId': (r) => JSON.parse(r.body).archiveId !== undefined,
        });

        errorRate.add(!archiveSuccess);
      });

      // Test 3: Restore Document
      group('Restore Document', () => {
        const restoreRes = http.post(
          `${BASE_URL}${API_PREFIX}/${uploadedDoc.id}/restore`,
          null,
          { headers }
        );

        const restoreSuccess = check(restoreRes, {
          'restore status is 200': (r) => r.status === 200,
          'restore response has success': (r) => JSON.parse(r.body).success === true,
        });

        errorRate.add(!restoreSuccess);
      });

      // Test 4: Get Document
      group('Get Document', () => {
        const getRes = http.get(
          `${BASE_URL}${API_PREFIX}/${uploadedDoc.id}`,
          { headers }
        );

        const getSuccess = check(getRes, {
          'get status is 200': (r) => r.status === 200,
          'get response has filename': (r) => JSON.parse(r.body).filename !== undefined,
        });

        errorRate.add(!getSuccess);
      });

      // Test 5: Delete Document
      group('Delete Document', () => {
        const deleteRes = http.del(
          `${BASE_URL}${API_PREFIX}/${uploadedDoc.id}`,
          null,
          { headers }
        );

        const deleteSuccess = check(deleteRes, {
          'delete status is 204 or 200': (r) => r.status === 204 || r.status === 200,
        });

        errorRate.add(!deleteSuccess);
      });
    }
  });

  // Test 6: Search Documents
  group('Search Documents', () => {
    const searchPayload = JSON.stringify({
      query: 'test',
      from: 0,
      size: 10,
    });

    const searchRes = http.post(
      `${BASE_URL}${API_PREFIX}/search`,
      searchPayload,
      { headers }
    );

    const searchSuccess = check(searchRes, {
      'search status is 200': (r) => r.status === 200,
      'search response has documents': (r) => JSON.parse(r.body).documents !== undefined,
    });

    errorRate.add(!searchSuccess);
  });

  // Test 7: Get All Documents
  group('Get All Documents', () => {
    const getAllRes = http.get(
      `${BASE_URL}${API_PREFIX}?page=0&size=20`,
      { headers }
    );

    const getAllSuccess = check(getAllRes, {
      'get all status is 200': (r) => r.status === 200,
      'get all response has content': (r) => JSON.parse(r.body).content !== undefined,
    });

    errorRate.add(!getAllSuccess);
  });

  // Test 8: Get Statistics
  group('Get Statistics', () => {
    const statsRes = http.get(
      `${BASE_URL}${API_PREFIX}/statistics`,
      { headers }
    );

    const statsSuccess = check(statsRes, {
      'statistics status is 200': (r) => r.status === 200,
      'statistics has totalDocuments': (r) => JSON.parse(r.body).totalDocuments !== undefined,
    });

    errorRate.add(!statsSuccess);
  });

  sleep(1);
}

export function teardown(data) {
  console.log('Test completed');
}
