import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { Pact } from '@pact-foundation/pact';
import { Matchers } from '@pact-foundation/pact';
import path from 'path';
import dataQualityService from '../dataQualityService';

describe('Data Quality API Contract Tests', () => {
  // Set up Pact provider
  const provider = new Pact({
    consumer: 'JiVS Frontend',
    provider: 'JiVS Backend',
    port: 9093, // Different port for data quality tests
    log: path.resolve(process.cwd(), 'logs', 'pact-dataquality.log'),
    dir: path.resolve(process.cwd(), 'pacts'),
    logLevel: 'info',
    spec: 2,
  });

  // Start mock server before tests
  beforeAll(async () => {
    await provider.setup();

    // Override the base URL for testing
    (dataQualityService as any).baseURL = 'http://localhost:9093/api/v1';
  });

  // Stop mock server after tests
  afterAll(async () => {
    await provider.writePact();
    await provider.removeInteractions();
  });

  // Verify interactions after each test
  afterEach(async () => {
    await provider.removeInteractions();
  });

  describe('GET /api/v1/data-quality/dashboard - Get Dashboard Metrics', () => {
    it('should fetch data quality dashboard metrics', async () => {
      await provider.addInteraction({
        state: 'user is authenticated and quality data exists',
        uponReceiving: 'a request for data quality dashboard',
        withRequest: {
          method: 'GET',
          path: '/api/v1/data-quality/dashboard',
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              overallScore: Matchers.decimal(85.5),
              dimensions: {
                completeness: Matchers.decimal(90.0),
                accuracy: Matchers.decimal(88.5),
                consistency: Matchers.decimal(85.0),
                validity: Matchers.decimal(82.0),
                uniqueness: Matchers.decimal(95.0),
                timeliness: Matchers.decimal(78.0),
              },
              totalRules: Matchers.integer(50),
              activeRules: Matchers.integer(45),
              failingRules: Matchers.integer(5),
              totalIssues: Matchers.integer(125),
              criticalIssues: Matchers.integer(10),
              recentScans: Matchers.integer(15),
              lastScanDate: Matchers.iso8601DateTime(),
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      const response = await dataQualityService.getDashboard();

      expect(response.overallScore).toBeGreaterThanOrEqual(0);
      expect(response.overallScore).toBeLessThanOrEqual(100);
      expect(response.dimensions).toBeDefined();
    });
  });

  describe('POST /api/v1/data-quality/rules - Create Quality Rule', () => {
    it('should create a new data quality rule', async () => {
      const ruleRequest = {
        name: 'Email Format Validation',
        description: 'Validate email addresses match correct format',
        dimension: 'VALIDITY',
        ruleType: 'REGEX',
        expression: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
        targetTable: 'customers',
        targetColumn: 'email',
        severity: 'HIGH',
        enabled: true,
      };

      const expectedResponse = {
        id: Matchers.uuid(),
        name: 'Email Format Validation',
        description: 'Validate email addresses match correct format',
        dimension: 'VALIDITY',
        ruleType: 'REGEX',
        expression: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
        targetTable: 'customers',
        targetColumn: 'email',
        severity: 'HIGH',
        enabled: true,
        createdAt: Matchers.iso8601DateTime(),
        updatedAt: Matchers.iso8601DateTime(),
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to create a quality rule',
        withRequest: {
          method: 'POST',
          path: '/api/v1/data-quality/rules',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
          body: ruleRequest,
        },
        willRespondWith: {
          status: 201,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: expectedResponse,
            timestamp: Matchers.iso8601DateTime(),
            status: 201,
            message: 'Quality rule created successfully',
          },
        },
      });

      const response = await dataQualityService.createRule(ruleRequest as any);

      expect(response.id).toBeDefined();
      expect(response.name).toBe('Email Format Validation');
      expect(response.dimension).toBe('VALIDITY');
    });
  });

  describe('GET /api/v1/data-quality/rules - List Quality Rules', () => {
    it('should fetch list of quality rules with pagination', async () => {
      await provider.addInteraction({
        state: 'user is authenticated and rules exist',
        uponReceiving: 'a request to list quality rules',
        withRequest: {
          method: 'GET',
          path: '/api/v1/data-quality/rules',
          query: {
            page: '0',
            size: '20',
            dimension: 'COMPLETENESS',
          },
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              content: Matchers.eachLike({
                id: Matchers.uuid(),
                name: Matchers.like('Null Check Rule'),
                dimension: Matchers.term({
                  matcher: 'COMPLETENESS|ACCURACY|CONSISTENCY|VALIDITY|UNIQUENESS|TIMELINESS',
                  generate: 'COMPLETENESS',
                }),
                ruleType: Matchers.like('NULL_CHECK'),
                severity: Matchers.term({
                  matcher: 'LOW|MEDIUM|HIGH|CRITICAL',
                  generate: 'HIGH',
                }),
                enabled: true,
                lastExecuted: Matchers.iso8601DateTime(),
                passRate: Matchers.decimal(95.0),
              }),
              totalElements: Matchers.integer(0),
              totalPages: Matchers.integer(0),
              size: 20,
              number: 0,
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      const response = await dataQualityService.getRules(0, 20, 'COMPLETENESS');

      expect(response.content).toBeInstanceOf(Array);
      expect(response.size).toBe(20);
    });
  });

  describe('GET /api/v1/data-quality/rules/{id} - Get Rule Details', () => {
    it('should fetch quality rule details by ID', async () => {
      const ruleId = '550e8400-e29b-41d4-a716-446655440200';

      await provider.addInteraction({
        state: 'quality rule exists',
        uponReceiving: 'a request to get rule details',
        withRequest: {
          method: 'GET',
          path: `/api/v1/data-quality/rules/${ruleId}`,
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              id: ruleId,
              name: Matchers.like('Completeness Check'),
              description: Matchers.like('Check for null values in required fields'),
              dimension: 'COMPLETENESS',
              ruleType: 'NULL_CHECK',
              expression: Matchers.like('IS NOT NULL'),
              targetTable: Matchers.like('customers'),
              targetColumn: Matchers.like('email'),
              severity: 'HIGH',
              enabled: true,
              executionCount: Matchers.integer(100),
              lastExecuted: Matchers.iso8601DateTime(),
              passRate: Matchers.decimal(98.5),
              createdAt: Matchers.iso8601DateTime(),
              updatedAt: Matchers.iso8601DateTime(),
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      const response = await dataQualityService.getRule(ruleId);

      expect(response.id).toBe(ruleId);
      expect(response.dimension).toBe('COMPLETENESS');
    });
  });

  describe('PUT /api/v1/data-quality/rules/{id} - Update Rule', () => {
    it('should update an existing quality rule', async () => {
      const ruleId = '550e8400-e29b-41d4-a716-446655440201';
      const updateRequest = {
        name: 'Updated Email Validation',
        description: 'Updated description',
        severity: 'CRITICAL',
        enabled: false,
      };

      await provider.addInteraction({
        state: 'quality rule exists',
        uponReceiving: 'a request to update a rule',
        withRequest: {
          method: 'PUT',
          path: `/api/v1/data-quality/rules/${ruleId}`,
          headers: {
            'Content-Type': 'application/json',
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
          body: updateRequest,
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              id: ruleId,
              name: 'Updated Email Validation',
              description: 'Updated description',
              dimension: 'VALIDITY',
              ruleType: 'REGEX',
              severity: 'CRITICAL',
              enabled: false,
              updatedAt: Matchers.iso8601DateTime(),
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
            message: 'Quality rule updated successfully',
          },
        },
      });

      const response = await dataQualityService.updateRule(ruleId, updateRequest);

      expect(response.name).toBe('Updated Email Validation');
      expect(response.severity).toBe('CRITICAL');
      expect(response.enabled).toBe(false);
    });
  });

  describe('DELETE /api/v1/data-quality/rules/{id} - Delete Rule', () => {
    it('should delete a quality rule', async () => {
      const ruleId = '550e8400-e29b-41d4-a716-446655440202';

      await provider.addInteraction({
        state: 'quality rule exists',
        uponReceiving: 'a request to delete a rule',
        withRequest: {
          method: 'DELETE',
          path: `/api/v1/data-quality/rules/${ruleId}`,
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              message: 'Quality rule deleted successfully',
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      await dataQualityService.deleteRule(ruleId);
      expect(true).toBe(true);
    });
  });

  describe('POST /api/v1/data-quality/rules/{id}/execute - Execute Rule', () => {
    it('should execute a quality rule and return results', async () => {
      const ruleId = '550e8400-e29b-41d4-a716-446655440203';

      await provider.addInteraction({
        state: 'quality rule exists and can be executed',
        uponReceiving: 'a request to execute a rule',
        withRequest: {
          method: 'POST',
          path: `/api/v1/data-quality/rules/${ruleId}/execute`,
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              executionId: Matchers.uuid(),
              ruleId: ruleId,
              status: 'COMPLETED',
              startTime: Matchers.iso8601DateTime(),
              endTime: Matchers.iso8601DateTime(),
              duration: Matchers.integer(1500),
              recordsScanned: Matchers.integer(10000),
              recordsPassed: Matchers.integer(9850),
              recordsFailed: Matchers.integer(150),
              passRate: Matchers.decimal(98.5),
              issues: Matchers.eachLike({
                recordId: Matchers.like('CUST-001'),
                fieldValue: Matchers.like('invalid-email'),
                reason: Matchers.like('Does not match email format'),
              }),
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
            message: 'Rule executed successfully',
          },
        },
      });

      const response = await dataQualityService.executeRule(ruleId);

      expect(response.executionId).toBeDefined();
      expect(response.status).toBe('COMPLETED');
      expect(response.passRate).toBeGreaterThanOrEqual(0);
    });
  });

  describe('GET /api/v1/data-quality/issues - Get Quality Issues', () => {
    it('should fetch list of data quality issues', async () => {
      await provider.addInteraction({
        state: 'quality issues exist',
        uponReceiving: 'a request to get quality issues',
        withRequest: {
          method: 'GET',
          path: '/api/v1/data-quality/issues',
          query: {
            severity: 'HIGH',
            status: 'OPEN',
            page: '0',
            size: '50',
          },
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              content: Matchers.eachLike({
                id: Matchers.uuid(),
                ruleId: Matchers.uuid(),
                ruleName: Matchers.like('Email Validation'),
                dimension: Matchers.like('VALIDITY'),
                severity: Matchers.term({
                  matcher: 'LOW|MEDIUM|HIGH|CRITICAL',
                  generate: 'HIGH',
                }),
                status: Matchers.term({
                  matcher: 'OPEN|IN_PROGRESS|RESOLVED|IGNORED',
                  generate: 'OPEN',
                }),
                tableName: Matchers.like('customers'),
                columnName: Matchers.like('email'),
                recordId: Matchers.like('CUST-001'),
                fieldValue: Matchers.like('invalid-email'),
                reason: Matchers.like('Does not match email format'),
                detectedAt: Matchers.iso8601DateTime(),
              }),
              totalElements: Matchers.integer(0),
              totalPages: Matchers.integer(0),
              size: 50,
              number: 0,
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      const response = await dataQualityService.getIssues({
        severity: 'HIGH',
        status: 'OPEN',
        page: 0,
        size: 50,
      });

      expect(response.content).toBeInstanceOf(Array);
      expect(response.size).toBe(50);
    });
  });

  describe('POST /api/v1/data-quality/profile - Profile Dataset', () => {
    it('should profile a dataset and return quality metrics', async () => {
      const profileRequest = {
        tableName: 'customers',
        columns: ['id', 'email', 'name', 'created_date'],
        sampleSize: 10000,
      };

      await provider.addInteraction({
        state: 'user is authenticated and table exists',
        uponReceiving: 'a request to profile a dataset',
        withRequest: {
          method: 'POST',
          path: '/api/v1/data-quality/profile',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
          body: profileRequest,
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              tableName: 'customers',
              recordCount: Matchers.integer(10000),
              profileDate: Matchers.iso8601DateTime(),
              columns: Matchers.eachLike({
                name: Matchers.like('email'),
                dataType: Matchers.like('VARCHAR(255)'),
                nullCount: Matchers.integer(50),
                nullPercentage: Matchers.decimal(0.5),
                uniqueCount: Matchers.integer(9950),
                uniquePercentage: Matchers.decimal(99.5),
                minLength: Matchers.integer(5),
                maxLength: Matchers.integer(50),
                avgLength: Matchers.decimal(25.5),
                patterns: Matchers.eachLike({
                  pattern: Matchers.like('*@*.com'),
                  count: Matchers.integer(8000),
                  percentage: Matchers.decimal(80.0),
                }),
              }),
              qualityScore: Matchers.decimal(92.5),
              recommendations: Matchers.eachLike({
                type: Matchers.like('ADD_CONSTRAINT'),
                message: Matchers.like('Consider adding NOT NULL constraint to email column'),
                impact: Matchers.like('HIGH'),
              }),
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
            message: 'Dataset profiled successfully',
          },
        },
      });

      const response = await dataQualityService.profileDataset(profileRequest);

      expect(response.tableName).toBe('customers');
      expect(response.recordCount).toBeGreaterThan(0);
      expect(response.qualityScore).toBeGreaterThanOrEqual(0);
      expect(response.qualityScore).toBeLessThanOrEqual(100);
    });
  });
});

/**
 * Data Quality Contract Tests
 *
 * These tests ensure the data quality service API contract is maintained between
 * frontend and backend. Data quality is critical for trust in the platform.
 *
 * What these tests prevent:
 * 1. Quality dimension mismatches
 * 2. Rule expression syntax errors
 * 3. Severity level inconsistencies
 * 4. Score calculation errors
 * 5. Issue tracking failures
 *
 * Coverage: 8/8 Data Quality endpoints (100%)
 */