/**
 * Views API Contract Tests
 *
 * These tests define the contract between the frontend and backend
 * for custom views and saved filters management.
 *
 * Coverage: 2 endpoints (LOW priority)
 */

import { Pact } from '@pact-foundation/pact';
import { MatchersV3 } from '@pact-foundation/pact';
import path from 'path';
import { viewsService } from '../viewsService';

const {
  like,
  eachLike,
  boolean,
  string,
  integer,
  regex,
  iso8601DateTime
} = MatchersV3;

// Create pact provider
const provider = new Pact({
  consumer: 'JiVS Frontend',
  provider: 'JiVS Backend',
  port: 9097,
  dir: path.resolve(process.cwd(), 'pacts'),
  logLevel: 'info',
});

describe('Views API Contract Tests', () => {
  beforeAll(() => provider.setup());
  afterAll(() => provider.finalize());
  afterEach(() => provider.verify());

  describe('Get Custom Views', () => {
    it('should fetch all custom views for the user', async () => {
      const expectedResponse = eachLike({
        id: string('VIEW-001'),
        name: string('My Extraction Dashboard'),
        description: string('Custom view for extraction monitoring'),
        type: string('EXTRACTION'),
        userId: string('USER-001'),
        isPublic: boolean(false),
        isDefault: boolean(false),
        configuration: like({
          filters: like({
            status: eachLike(string('RUNNING'), { min: 1 }),
            sourceType: eachLike(string('JDBC'), { min: 1 }),
            dateRange: like({
              from: iso8601DateTime(),
              to: iso8601DateTime()
            })
          }),
          columns: eachLike({
            field: string('name'),
            label: string('Name'),
            visible: boolean(true),
            width: integer(200),
            sortable: boolean(true),
            order: integer(1)
          }, { min: 5 }),
          sorting: like({
            field: string('createdAt'),
            order: string('DESC')
          }),
          grouping: like({
            enabled: boolean(false),
            field: string('sourceType')
          }),
          aggregations: eachLike({
            field: string('recordsExtracted'),
            function: string('SUM'),
            label: string('Total Records')
          }, { min: 0 }),
          chartConfig: like({
            type: string('line'),
            xAxis: string('date'),
            yAxis: string('count'),
            showLegend: boolean(true),
            showGrid: boolean(true)
          })
        }),
        permissions: like({
          canEdit: boolean(true),
          canDelete: boolean(true),
          canShare: boolean(true),
          canDuplicate: boolean(true)
        }),
        metadata: like({
          createdBy: string('USER-001'),
          createdByName: string('John Doe'),
          createdAt: iso8601DateTime(),
          updatedAt: iso8601DateTime(),
          lastAccessedAt: iso8601DateTime(),
          accessCount: integer(42),
          sharedWith: eachLike(string('USER-002'), { min: 0 })
        }),
        tags: eachLike(string('extraction'), { min: 0 })
      }, { min: 3 });

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and has custom views' }],
        uponReceiving: 'a request to get custom views',
        withRequest: {
          method: 'GET',
          path: '/api/v1/views',
          headers: {
            'Authorization': regex(/^Bearer .+/, 'Bearer valid-token')
          }
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: expectedResponse
        }
      });

      const result = await viewsService.getViews();
      expect(result).toBeDefined();
      expect(result.length).toBeGreaterThan(0);
    });
  });

  describe('Create Custom View', () => {
    it('should create a new custom view', async () => {
      const createRequest = {
        name: 'Failed Migrations Report',
        description: 'View showing all failed migrations for investigation',
        type: 'MIGRATION',
        isPublic: false,
        isDefault: false,
        configuration: {
          filters: {
            status: ['FAILED', 'ERROR'],
            dateRange: {
              from: '2025-01-01T00:00:00Z',
              to: '2025-01-31T23:59:59Z'
            }
          },
          columns: [
            { field: 'name', label: 'Migration Name', visible: true, width: 250, sortable: true, order: 1 },
            { field: 'status', label: 'Status', visible: true, width: 100, sortable: true, order: 2 },
            { field: 'errorMessage', label: 'Error', visible: true, width: 400, sortable: false, order: 3 },
            { field: 'failedAt', label: 'Failed At', visible: true, width: 180, sortable: true, order: 4 },
            { field: 'recordsMigrated', label: 'Records', visible: true, width: 120, sortable: true, order: 5 }
          ],
          sorting: {
            field: 'failedAt',
            order: 'DESC'
          },
          grouping: {
            enabled: true,
            field: 'errorType'
          },
          aggregations: [
            { field: 'recordsMigrated', function: 'SUM', label: 'Total Records Attempted' },
            { field: 'id', function: 'COUNT', label: 'Total Failures' }
          ],
          chartConfig: {
            type: 'bar',
            xAxis: 'date',
            yAxis: 'failureCount',
            showLegend: true,
            showGrid: true
          }
        },
        tags: ['migration', 'errors', 'investigation']
      };

      const expectedResponse = like({
        ...createRequest,
        id: string('VIEW-004'),
        userId: string('USER-001'),
        permissions: like({
          canEdit: boolean(true),
          canDelete: boolean(true),
          canShare: boolean(true),
          canDuplicate: boolean(true)
        }),
        metadata: like({
          createdBy: string('USER-001'),
          createdByName: string('John Doe'),
          createdAt: iso8601DateTime(),
          updatedAt: iso8601DateTime(),
          lastAccessedAt: iso8601DateTime(),
          accessCount: integer(0),
          sharedWith: eachLike(string(''), { min: 0 })
        }),
        message: string('View created successfully')
      });

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and can create views' }],
        uponReceiving: 'a request to create a custom view',
        withRequest: {
          method: 'POST',
          path: '/api/v1/views',
          headers: {
            'Authorization': regex(/^Bearer .+/, 'Bearer valid-token'),
            'Content-Type': 'application/json'
          },
          body: createRequest
        },
        willRespondWith: {
          status: 201,
          headers: {
            'Content-Type': 'application/json',
            'Location': regex(/^\/api\/v1\/views\/.+/, '/api/v1/views/VIEW-004')
          },
          body: expectedResponse
        }
      });

      const result = await viewsService.createView(createRequest);
      expect(result).toBeDefined();
      expect(result.id).toBeDefined();
      expect(result.message).toContain('successfully');
    });
  });
});

/**
 * WHY VIEWS CONTRACT TESTS MATTER:
 *
 * 1. Custom views save user productivity time
 * 2. View configurations must persist accurately
 * 3. Shared views enable team collaboration
 * 4. Filters and aggregations drive insights
 * 5. View permissions control data access
 *
 * These tests ensure:
 * - View configuration structure consistency
 * - Filter and sorting logic agreement
 * - Column visibility and ordering
 * - Aggregation function compatibility
 * - Permission model alignment
 *
 * User productivity impact:
 * - Saved views eliminate repetitive filtering
 * - Custom columns show relevant data
 * - Aggregations provide quick insights
 * - Shared views enable team consistency
 * - Default views streamline onboarding
 */