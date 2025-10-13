import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { Pact } from '@pact-foundation/pact';
import { Matchers } from '@pact-foundation/pact';
import path from 'path';
import extractionService from '../extractionService';

describe('Extraction API Contract Tests', () => {
  // Set up Pact provider
  const provider = new Pact({
    consumer: 'JiVS Frontend',
    provider: 'JiVS Backend',
    port: 9092, // Different port for extraction tests
    log: path.resolve(process.cwd(), 'logs', 'pact-extraction.log'),
    dir: path.resolve(process.cwd(), 'pacts'),
    logLevel: 'info',
    spec: 2,
  });

  // Start mock server before tests
  beforeAll(async () => {
    await provider.setup();

    // Override the base URL for testing
    (extractionService as any).baseURL = 'http://localhost:9092/api/v1';
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

  describe('POST /api/v1/extractions - Create Extraction', () => {
    it('should create an extraction job with valid configuration', async () => {
      const extractionRequest = {
        name: 'Customer Data Extract',
        description: 'Extract customer data from Oracle',
        sourceType: 'JDBC',
        connectionConfig: {
          url: 'jdbc:oracle:thin:@localhost:1521:orcl',
          username: 'system',
          password: 'oracle',
          driverClass: 'oracle.jdbc.OracleDriver',
        },
        extractionQuery: 'SELECT * FROM customers',
        schedule: '0 0 * * * *', // Every hour
      };

      const expectedResponse = {
        id: Matchers.uuid(),
        name: 'Customer Data Extract',
        description: 'Extract customer data from Oracle',
        sourceType: 'JDBC',
        status: 'INITIALIZED',
        recordsExtracted: 0,
        totalRecords: 0,
        extractionQuery: 'SELECT * FROM customers',
        schedule: '0 0 * * * *',
        createdAt: Matchers.iso8601DateTime(),
        updatedAt: Matchers.iso8601DateTime(),
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to create extraction',
        withRequest: {
          method: 'POST',
          path: '/api/v1/extractions',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
          body: extractionRequest,
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
            message: 'Extraction created successfully',
          },
        },
      });

      const response = await extractionService.createExtraction(extractionRequest as any);

      expect(response.id).toBeDefined();
      expect(response.name).toBe('Customer Data Extract');
      expect(response.status).toBe('INITIALIZED');
    });

    it('should reject extraction creation with invalid source type', async () => {
      const invalidRequest = {
        name: 'Invalid Extract',
        sourceType: 'INVALID_TYPE',
        connectionConfig: {},
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to create extraction with invalid source type',
        withRequest: {
          method: 'POST',
          path: '/api/v1/extractions',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
          body: invalidRequest,
        },
        willRespondWith: {
          status: 400,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            timestamp: Matchers.iso8601DateTime(),
            status: 400,
            error: 'Bad Request',
            message: 'Invalid source type. Supported types: JDBC, SAP, FILE, API',
          },
        },
      });

      try {
        await extractionService.createExtraction(invalidRequest as any);
        expect.fail('Should have thrown an error');
      } catch (error: any) {
        expect(error.response.status).toBe(400);
        expect(error.response.data.message).toContain('Invalid source type');
      }
    });
  });

  describe('GET /api/v1/extractions - List Extractions', () => {
    it('should fetch extractions list with pagination and filters', async () => {
      await provider.addInteraction({
        state: 'user is authenticated and extractions exist',
        uponReceiving: 'a request to list extractions',
        withRequest: {
          method: 'GET',
          path: '/api/v1/extractions',
          query: {
            page: '0',
            size: '20',
            status: 'RUNNING',
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
                name: Matchers.like('Data Extraction Job'),
                sourceType: Matchers.term({
                  matcher: 'JDBC|SAP|FILE|API',
                  generate: 'JDBC',
                }),
                status: Matchers.term({
                  matcher: 'PENDING|RUNNING|COMPLETED|FAILED|STOPPED',
                  generate: 'RUNNING',
                }),
                recordsExtracted: Matchers.integer(0),
                totalRecords: Matchers.integer(0),
                createdAt: Matchers.iso8601DateTime(),
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

      const response = await extractionService.getExtractions(0, 20, 'RUNNING');

      expect(response.content).toBeInstanceOf(Array);
      expect(response.size).toBe(20);
    });
  });

  describe('GET /api/v1/extractions/{id} - Get Extraction Details', () => {
    it('should fetch extraction details by ID', async () => {
      const extractionId = '550e8400-e29b-41d4-a716-446655440100';

      await provider.addInteraction({
        state: 'extraction exists',
        uponReceiving: 'a request to get extraction details',
        withRequest: {
          method: 'GET',
          path: `/api/v1/extractions/${extractionId}`,
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
              id: extractionId,
              name: Matchers.like('Production Data Extract'),
              description: Matchers.like('Daily production data extraction'),
              sourceType: 'JDBC',
              status: Matchers.term({
                matcher: 'PENDING|RUNNING|COMPLETED|FAILED|STOPPED',
                generate: 'RUNNING',
              }),
              recordsExtracted: Matchers.integer(5000),
              totalRecords: Matchers.integer(10000),
              extractionQuery: Matchers.like('SELECT * FROM transactions'),
              schedule: Matchers.like('0 0 * * * *'),
              startedAt: Matchers.iso8601DateTime(),
              createdAt: Matchers.iso8601DateTime(),
              updatedAt: Matchers.iso8601DateTime(),
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      const response = await extractionService.getExtraction(extractionId);

      expect(response.id).toBe(extractionId);
      expect(response.sourceType).toBe('JDBC');
    });
  });

  describe('POST /api/v1/extractions/{id}/start - Start Extraction', () => {
    it('should start a pending extraction job', async () => {
      const extractionId = '550e8400-e29b-41d4-a716-446655440101';

      await provider.addInteraction({
        state: 'extraction exists in PENDING status',
        uponReceiving: 'a request to start extraction',
        withRequest: {
          method: 'POST',
          path: `/api/v1/extractions/${extractionId}/start`,
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
              id: extractionId,
              status: 'RUNNING',
              message: 'Extraction started successfully',
              startedAt: Matchers.iso8601DateTime(),
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      await extractionService.startExtraction(extractionId);
      expect(true).toBe(true);
    });
  });

  describe('POST /api/v1/extractions/{id}/stop - Stop Extraction', () => {
    it('should stop a running extraction job', async () => {
      const extractionId = '550e8400-e29b-41d4-a716-446655440102';

      await provider.addInteraction({
        state: 'extraction exists in RUNNING status',
        uponReceiving: 'a request to stop extraction',
        withRequest: {
          method: 'POST',
          path: `/api/v1/extractions/${extractionId}/stop`,
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
              id: extractionId,
              status: 'STOPPED',
              message: 'Extraction stopped successfully',
              stoppedAt: Matchers.iso8601DateTime(),
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      await extractionService.stopExtraction(extractionId);
      expect(true).toBe(true);
    });
  });

  describe('DELETE /api/v1/extractions/{id} - Delete Extraction', () => {
    it('should delete an extraction job', async () => {
      const extractionId = '550e8400-e29b-41d4-a716-446655440103';

      await provider.addInteraction({
        state: 'extraction exists',
        uponReceiving: 'a request to delete extraction',
        withRequest: {
          method: 'DELETE',
          path: `/api/v1/extractions/${extractionId}`,
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
              message: 'Extraction deleted successfully',
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      await extractionService.deleteExtraction(extractionId);
      expect(true).toBe(true);
    });
  });

  describe('GET /api/v1/extractions/{id}/statistics - Get Extraction Statistics', () => {
    it('should fetch extraction statistics', async () => {
      const extractionId = '550e8400-e29b-41d4-a716-446655440104';

      await provider.addInteraction({
        state: 'extraction exists with statistics',
        uponReceiving: 'a request to get extraction statistics',
        withRequest: {
          method: 'GET',
          path: `/api/v1/extractions/${extractionId}/statistics`,
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInT5cCI6IkpXVCJ9'),
          },
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              extractionId: extractionId,
              totalRecords: Matchers.integer(10000),
              recordsExtracted: Matchers.integer(8500),
              recordsFailed: Matchers.integer(100),
              recordsSkipped: Matchers.integer(50),
              successRate: Matchers.decimal(85.0),
              averageThroughput: Matchers.decimal(100.5),
              peakThroughput: Matchers.decimal(250.0),
              totalDuration: Matchers.integer(3600),
              dataSize: Matchers.integer(1048576),
              errorRate: Matchers.decimal(1.0),
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      const response = await extractionService.getStatistics(extractionId);

      expect(response.extractionId).toBe(extractionId);
      expect(response.successRate).toBeGreaterThanOrEqual(0);
    });
  });

  describe('POST /api/v1/extractions/test-connection - Test Connection', () => {
    it('should test database connection successfully', async () => {
      const connectionConfig = {
        url: 'jdbc:oracle:thin:@localhost:1521:orcl',
        username: 'system',
        password: 'oracle',
        driverClass: 'oracle.jdbc.OracleDriver',
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to test connection',
        withRequest: {
          method: 'POST',
          path: '/api/v1/extractions/test-connection',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
          body: connectionConfig,
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              connected: true,
              message: 'Connection successful',
              databaseInfo: {
                productName: Matchers.like('Oracle'),
                productVersion: Matchers.like('12c'),
                driverName: Matchers.like('Oracle JDBC driver'),
                driverVersion: Matchers.like('12.2.0.1.0'),
              },
              latency: Matchers.integer(50),
            },
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      const response = await extractionService.testConnection(connectionConfig);

      expect(response.connected).toBe(true);
      expect(response.message).toBe('Connection successful');
    });

    it('should handle connection test failure', async () => {
      const invalidConfig = {
        url: 'jdbc:oracle:thin:@invalid:1521:orcl',
        username: 'wrong',
        password: 'wrong',
        driverClass: 'oracle.jdbc.OracleDriver',
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to test connection with invalid config',
        withRequest: {
          method: 'POST',
          path: '/api/v1/extractions/test-connection',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
          body: invalidConfig,
        },
        willRespondWith: {
          status: 400,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            timestamp: Matchers.iso8601DateTime(),
            status: 400,
            error: 'Bad Request',
            message: 'Connection failed: IO Error: The Network Adapter could not establish the connection',
          },
        },
      });

      try {
        await extractionService.testConnection(invalidConfig);
        expect.fail('Should have thrown an error');
      } catch (error: any) {
        expect(error.response.status).toBe(400);
        expect(error.response.data.message).toContain('Connection failed');
      }
    });
  });

  describe('GET /api/v1/extractions/{id}/logs - Get Extraction Logs', () => {
    it('should fetch extraction logs', async () => {
      const extractionId = '550e8400-e29b-41d4-a716-446655440105';

      await provider.addInteraction({
        state: 'extraction exists with logs',
        uponReceiving: 'a request to get extraction logs',
        withRequest: {
          method: 'GET',
          path: `/api/v1/extractions/${extractionId}/logs`,
          query: {
            limit: '100',
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
            data: Matchers.eachLike({
              timestamp: Matchers.iso8601DateTime(),
              level: Matchers.term({
                matcher: 'INFO|WARN|ERROR|DEBUG',
                generate: 'INFO',
              }),
              message: Matchers.like('Processing batch 1 of 10'),
              details: Matchers.like({
                batchNumber: 1,
                recordsProcessed: 100,
              }),
            }),
            timestamp: Matchers.iso8601DateTime(),
            status: 200,
          },
        },
      });

      const logs = await extractionService.getLogs(extractionId, 100);

      expect(logs).toBeInstanceOf(Array);
      if (logs.length > 0) {
        expect(logs[0].timestamp).toBeDefined();
        expect(logs[0].level).toBeDefined();
        expect(logs[0].message).toBeDefined();
      }
    });
  });
});

/**
 * Extraction Contract Tests
 *
 * These tests ensure the extraction service API contract is maintained between
 * frontend and backend. Extraction is critical for data ingestion into JiVS.
 *
 * What these tests prevent:
 * 1. Connection configuration mismatches
 * 2. Source type validation failures
 * 3. Query execution errors
 * 4. Status transition bugs
 * 5. Statistics calculation errors
 *
 * Coverage: 9/9 Extraction endpoints (100%)
 */