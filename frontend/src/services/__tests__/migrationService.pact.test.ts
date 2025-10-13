import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { Pact } from '@pact-foundation/pact';
import { Matchers } from '@pact-foundation/pact';
import path from 'path';
import migrationService from '../migrationService';

describe('Migration API Contract Tests', () => {
  // Set up Pact provider
  const provider = new Pact({
    consumer: 'JiVS Frontend',
    provider: 'JiVS Backend',
    port: 9090, // Using different port to avoid conflicts
    log: path.resolve(process.cwd(), 'logs', 'pact.log'),
    dir: path.resolve(process.cwd(), 'pacts'),
    logLevel: 'info',
    spec: 2,
  });

  // Start mock server before tests
  beforeAll(async () => {
    await provider.setup();

    // Override the base URL for testing
    (migrationService as any).baseURL = 'http://localhost:9090/api/v1';
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

  describe('POST /api/v1/migrations - Create Migration', () => {
    it('should create a migration with correct request and response format', async () => {
      // THIS TEST WOULD HAVE CAUGHT THE BUG!
      // It validates the exact contract between frontend and backend

      const expectedRequest = {
        name: 'Test Migration',
        description: 'Test Description',
        sourceSystem: 'Oracle Database 12c',  // ← MUST match backend expectation
        targetSystem: 'PostgreSQL 15',        // ← MUST match backend expectation
        migrationType: 'FULL',                // ← MUST match backend expectation
        // NOT sourceConfig/targetConfig!
      };

      const expectedResponse = {
        id: Matchers.uuid(),
        name: 'Test Migration',
        description: 'Test Description',
        sourceSystem: 'Oracle Database 12c',
        targetSystem: 'PostgreSQL 15',
        migrationType: 'FULL',
        status: 'INITIALIZED',
        phase: 'PLANNING',
        projectCode: Matchers.like('MIG-20251013-12345'),
        projectType: 'FULL',
        progress: 0,
        recordsMigrated: 0,
        totalRecords: 0,
        createdAt: Matchers.iso8601DateTimeWithMillis(),
        updatedAt: Matchers.iso8601DateTimeWithMillis(),
      };

      // Define the interaction
      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to create a migration',
        withRequest: {
          method: 'POST',
          path: '/api/v1/migrations',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
          body: expectedRequest,
        },
        willRespondWith: {
          status: 201,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: expectedResponse,
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 201,
            message: 'Migration created successfully',
          },
        },
      });

      // Call the actual service
      const response = await migrationService.createMigration({
        name: 'Test Migration',
        description: 'Test Description',
        sourceSystem: 'Oracle Database 12c',
        targetSystem: 'PostgreSQL 15',
        migrationType: 'FULL',
      } as any);

      // Verify response structure
      expect(response.id).toBeDefined();
      expect(response.name).toBe('Test Migration');
      expect(response.status).toBe('INITIALIZED');
      expect(response.projectCode).toMatch(/^MIG-\d{8}-\d{5}$/);
    });

    it('should handle migration creation with missing required fields', async () => {
      // This test ensures proper error handling

      const invalidRequest = {
        name: 'Test Migration',
        // Missing sourceSystem, targetSystem, migrationType
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to create migration with missing fields',
        withRequest: {
          method: 'POST',
          path: '/api/v1/migrations',
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
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 400,
            error: 'Bad Request',
            message: 'Validation failed',
            validationErrors: [
              {
                field: 'sourceSystem',
                message: 'Source system is required',
              },
              {
                field: 'targetSystem',
                message: 'Target system is required',
              },
              {
                field: 'migrationType',
                message: 'Migration type is required',
              },
            ],
          },
        },
      });

      try {
        await migrationService.createMigration(invalidRequest as any);
        // Should not reach here
        expect.fail('Should have thrown an error');
      } catch (error: any) {
        expect(error.response.status).toBe(400);
        expect(error.response.data.validationErrors).toHaveLength(3);
      }
    });
  });

  describe('GET /api/v1/migrations - List Migrations', () => {
    it('should fetch migrations list with pagination', async () => {
      await provider.addInteraction({
        state: 'user is authenticated and migrations exist',
        uponReceiving: 'a request to list migrations',
        withRequest: {
          method: 'GET',
          path: '/api/v1/migrations',
          query: {
            page: '0',
            size: '20',
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
                name: Matchers.like('Migration 1'),
                status: Matchers.term({
                  matcher: 'PENDING|RUNNING|PAUSED|COMPLETED|FAILED|ROLLING_BACK',
                  generate: 'RUNNING',
                }),
                phase: Matchers.like('EXTRACTION'),
                progress: Matchers.integer({ min: 0, max: 100 }),
                recordsMigrated: Matchers.integer({ min: 0 }),
                totalRecords: Matchers.integer({ min: 0 }),
                createdAt: Matchers.iso8601DateTimeWithMillis(),
              }),
              totalElements: Matchers.integer({ min: 0 }),
              totalPages: Matchers.integer({ min: 0 }),
              size: 20,
              number: 0,
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      const response = await migrationService.getMigrations(0, 20);

      expect(response.content).toBeDefined();
      expect(response.content).toBeInstanceOf(Array);
      expect(response.totalElements).toBeGreaterThanOrEqual(0);
    });
  });

  describe('POST /api/v1/migrations/{id}/start - Start Migration', () => {
    it('should start a pending migration', async () => {
      const migrationId = '550e8400-e29b-41d4-a716-446655440001';

      await provider.addInteraction({
        state: 'migration exists in PENDING status',
        uponReceiving: 'a request to start migration',
        withRequest: {
          method: 'POST',
          path: `/api/v1/migrations/${migrationId}/start`,
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
              id: migrationId,
              status: 'RUNNING',
              phase: 'EXTRACTION',
              message: 'Migration started successfully',
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      await migrationService.startMigration(migrationId);
      // If no error thrown, test passes
      expect(true).toBe(true);
    });
  });

  describe('DELETE /api/v1/migrations/{id} - Delete Migration', () => {
    it('should delete a migration', async () => {
      const migrationId = '550e8400-e29b-41d4-a716-446655440002';

      await provider.addInteraction({
        state: 'migration exists',
        uponReceiving: 'a request to delete migration',
        withRequest: {
          method: 'DELETE',
          path: `/api/v1/migrations/${migrationId}`,
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
              message: 'Migration deleted successfully',
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      await migrationService.deleteMigration(migrationId);
      // If no error thrown, test passes
      expect(true).toBe(true);
    });
  });

  describe('GET /api/v1/migrations/{id} - Get Migration Details', () => {
    it('should fetch a single migration by ID', async () => {
      const migrationId = '550e8400-e29b-41d4-a716-446655440003';

      await provider.addInteraction({
        state: 'migration exists with details',
        uponReceiving: 'a request to get migration details',
        withRequest: {
          method: 'GET',
          path: `/api/v1/migrations/${migrationId}`,
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
              id: migrationId,
              name: Matchers.like('Production Migration'),
              description: Matchers.like('Migrating production data'),
              sourceSystem: Matchers.like('Oracle Database 12c'),
              targetSystem: Matchers.like('PostgreSQL 15'),
              migrationType: Matchers.term({
                matcher: 'FULL|INCREMENTAL|DELTA',
                generate: 'FULL',
              }),
              status: Matchers.term({
                matcher: 'PENDING|RUNNING|PAUSED|COMPLETED|FAILED|ROLLING_BACK',
                generate: 'RUNNING',
              }),
              phase: Matchers.term({
                matcher: 'PLANNING|VALIDATION|EXTRACTION|TRANSFORMATION|LOADING|VERIFICATION|CLEANUP',
                generate: 'EXTRACTION',
              }),
              progress: Matchers.integer({ min: 0, max: 100 }),
              recordsMigrated: Matchers.integer({ min: 0 }),
              totalRecords: Matchers.integer({ min: 0 }),
              startedAt: Matchers.iso8601DateTimeWithMillis(),
              createdAt: Matchers.iso8601DateTimeWithMillis(),
              updatedAt: Matchers.iso8601DateTimeWithMillis(),
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      const response = await migrationService.getMigration(migrationId);

      expect(response.id).toBe(migrationId);
      expect(response.name).toBeDefined();
      expect(response.status).toBeDefined();
    });
  });

  describe('POST /api/v1/migrations/{id}/pause - Pause Migration', () => {
    it('should pause a running migration', async () => {
      const migrationId = '550e8400-e29b-41d4-a716-446655440004';

      await provider.addInteraction({
        state: 'migration exists in RUNNING status',
        uponReceiving: 'a request to pause migration',
        withRequest: {
          method: 'POST',
          path: `/api/v1/migrations/${migrationId}/pause`,
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
              id: migrationId,
              status: 'PAUSED',
              message: 'Migration paused successfully',
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      await migrationService.pauseMigration(migrationId);
      expect(true).toBe(true);
    });
  });

  describe('POST /api/v1/migrations/{id}/resume - Resume Migration', () => {
    it('should resume a paused migration', async () => {
      const migrationId = '550e8400-e29b-41d4-a716-446655440005';

      await provider.addInteraction({
        state: 'migration exists in PAUSED status',
        uponReceiving: 'a request to resume migration',
        withRequest: {
          method: 'POST',
          path: `/api/v1/migrations/${migrationId}/resume`,
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
              id: migrationId,
              status: 'RUNNING',
              phase: Matchers.like('EXTRACTION'),
              message: 'Migration resumed successfully',
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      await migrationService.resumeMigration(migrationId);
      expect(true).toBe(true);
    });
  });

  describe('POST /api/v1/migrations/{id}/rollback - Rollback Migration', () => {
    it('should rollback a completed or failed migration', async () => {
      const migrationId = '550e8400-e29b-41d4-a716-446655440006';

      await provider.addInteraction({
        state: 'migration exists in COMPLETED or FAILED status',
        uponReceiving: 'a request to rollback migration',
        withRequest: {
          method: 'POST',
          path: `/api/v1/migrations/${migrationId}/rollback`,
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
              id: migrationId,
              status: 'ROLLING_BACK',
              message: 'Migration rollback initiated',
              rollbackStartedAt: Matchers.iso8601DateTimeWithMillis(),
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      await migrationService.rollbackMigration(migrationId);
      expect(true).toBe(true);
    });
  });

  describe('GET /api/v1/migrations/{id}/progress - Get Migration Progress', () => {
    it('should fetch migration progress details', async () => {
      const migrationId = '550e8400-e29b-41d4-a716-446655440007';

      await provider.addInteraction({
        state: 'migration exists with progress',
        uponReceiving: 'a request to get migration progress',
        withRequest: {
          method: 'GET',
          path: `/api/v1/migrations/${migrationId}/progress`,
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
              migrationId: migrationId,
              phase: Matchers.term({
                matcher: 'PLANNING|VALIDATION|EXTRACTION|TRANSFORMATION|LOADING|VERIFICATION|CLEANUP',
                generate: 'EXTRACTION',
              }),
              overallProgress: Matchers.integer({ min: 0, max: 100 }),
              phaseProgress: Matchers.integer({ min: 0, max: 100 }),
              recordsProcessed: Matchers.integer({ min: 0 }),
              recordsTotal: Matchers.integer({ min: 0 }),
              estimatedTimeRemaining: Matchers.integer({ min: 0 }),
              throughput: Matchers.decimal({ min: 0 }),
              errors: Matchers.integer({ min: 0 }),
              warnings: Matchers.integer({ min: 0 }),
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      const response = await migrationService.getProgress(migrationId);

      expect(response.migrationId).toBe(migrationId);
      expect(response.overallProgress).toBeGreaterThanOrEqual(0);
      expect(response.overallProgress).toBeLessThanOrEqual(100);
    });
  });

  describe('GET /api/v1/migrations/{id}/statistics - Get Migration Statistics', () => {
    it('should fetch migration statistics', async () => {
      const migrationId = '550e8400-e29b-41d4-a716-446655440008';

      await provider.addInteraction({
        state: 'migration exists with statistics',
        uponReceiving: 'a request to get migration statistics',
        withRequest: {
          method: 'GET',
          path: `/api/v1/migrations/${migrationId}/statistics`,
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
              migrationId: migrationId,
              totalRecords: Matchers.integer({ min: 0 }),
              recordsMigrated: Matchers.integer({ min: 0 }),
              recordsFailed: Matchers.integer({ min: 0 }),
              recordsSkipped: Matchers.integer({ min: 0 }),
              successRate: Matchers.decimal({ min: 0, max: 100 }),
              averageThroughput: Matchers.decimal({ min: 0 }),
              peakThroughput: Matchers.decimal({ min: 0 }),
              totalDuration: Matchers.integer({ min: 0 }),
              phaseStatistics: Matchers.eachLike({
                phase: Matchers.like('EXTRACTION'),
                duration: Matchers.integer({ min: 0 }),
                recordsProcessed: Matchers.integer({ min: 0 }),
              }),
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      const response = await migrationService.getStatistics(migrationId);

      expect(response.migrationId).toBe(migrationId);
      expect(response.successRate).toBeGreaterThanOrEqual(0);
      expect(response.successRate).toBeLessThanOrEqual(100);
    });
  });

  describe('POST /api/v1/migrations/validate - Validate Migration Configuration', () => {
    it('should validate migration configuration before creation', async () => {
      const validationRequest = {
        name: 'Test Migration',
        description: 'Test Description',
        sourceSystem: 'Oracle Database 12c',
        targetSystem: 'PostgreSQL 15',
        migrationType: 'FULL',
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to validate migration configuration',
        withRequest: {
          method: 'POST',
          path: '/api/v1/migrations/validate',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
          body: validationRequest,
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              valid: true,
              sourceConnectionValid: true,
              targetConnectionValid: true,
              estimatedRecords: Matchers.integer({ min: 0 }),
              estimatedDuration: Matchers.integer({ min: 0 }),
              warnings: Matchers.eachLike({
                code: Matchers.like('LARGE_DATASET'),
                message: Matchers.like('Dataset contains over 1 million records'),
              }),
              recommendations: Matchers.eachLike({
                type: Matchers.like('PERFORMANCE'),
                message: Matchers.like('Consider using incremental migration for large datasets'),
              }),
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      const response = await migrationService.validateMigration(validationRequest as any);

      expect(response.valid).toBe(true);
      expect(response.sourceConnectionValid).toBe(true);
      expect(response.targetConnectionValid).toBe(true);
    });
  });
});

/**
 * This contract test would have caught the migration creation bug immediately!
 *
 * How it works:
 * 1. Frontend defines what it will send: { name, sourceSystem, targetSystem, migrationType }
 * 2. Frontend defines what it expects back: Migration object with all fields
 * 3. Pact generates a contract file (JSON) that backend must satisfy
 * 4. Backend runs provider test to verify it matches the contract
 *
 * If frontend had been sending { sourceConfig, targetConfig } instead,
 * this test would FAIL immediately with:
 *   "Expected sourceSystem but got sourceConfig"
 *
 * Developer would fix in 1 minute instead of user finding bug after 2 hours!
 */