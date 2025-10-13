/**
 * Analytics API Contract Tests
 *
 * These tests define the contract between the frontend and backend
 * for analytics and reporting functionality.
 *
 * Coverage: 7 endpoints (MEDIUM priority)
 */

import { Pact } from '@pact-foundation/pact';
import { MatchersV3 } from '@pact-foundation/pact';
import path from 'path';
import { analyticsService } from '../analyticsService';

const {
  like,
  eachLike,
  integer,
  decimal,
  boolean,
  string,
  regex,
  iso8601DateTime
} = MatchersV3;

// Create pact provider
const provider = new Pact({
  consumer: 'JiVS Frontend',
  provider: 'JiVS Backend',
  port: 9095,
  dir: path.resolve(process.cwd(), 'pacts'),
  logLevel: 'info',
});

describe('Analytics API Contract Tests', () => {
  beforeAll(() => provider.setup());
  afterAll(() => provider.finalize());
  afterEach(() => provider.verify());

  describe('Dashboard Analytics', () => {
    it('should fetch dashboard analytics', async () => {
      // Expected request with date range
      const expectedRequest = {
        from: '2025-01-01T00:00:00Z',
        to: '2025-01-13T23:59:59Z'
      };

      // Expected response structure
      const expectedResponse = {
        overview: like({
          totalExtractions: integer(1250),
          totalMigrations: integer(342),
          dataQualityScore: decimal(92.5),
          complianceRate: decimal(98.7),
          activeUsers: integer(45),
          dataVolume: integer(1048576000), // 1GB in bytes
          period: string('2025-01-01 to 2025-01-13')
        }),
        extractionTrends: eachLike({
          date: regex(/\d{4}-\d{2}-\d{2}/, '2025-01-13'),
          count: integer(25),
          volume: integer(104857600),
          successRate: decimal(95.5)
        }, { min: 7 }),
        migrationStatus: like({
          pending: integer(5),
          running: integer(3),
          completed: integer(320),
          failed: integer(14)
        }),
        topSources: eachLike({
          source: string('Oracle Database'),
          count: integer(450),
          percentage: decimal(36.0)
        }, { min: 5 }),
        performanceMetrics: like({
          avgExtractionTime: decimal(125.5), // seconds
          avgMigrationTime: decimal(450.2),  // seconds
          systemUptime: decimal(99.95),      // percentage
          errorRate: decimal(0.5)            // percentage
        })
      };

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and analytics data exists' }],
        uponReceiving: 'a request for dashboard analytics',
        withRequest: {
          method: 'GET',
          path: '/api/v1/analytics/dashboard',
          query: expectedRequest,
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

      const result = await analyticsService.getDashboardAnalytics(
        new Date('2025-01-01'),
        new Date('2025-01-13')
      );

      expect(result).toBeDefined();
      expect(result.overview.totalExtractions).toBeGreaterThan(0);
    });
  });

  describe('Extraction Analytics', () => {
    it('should fetch extraction analytics', async () => {
      const expectedResponse = {
        summary: like({
          totalExtractions: integer(1250),
          successfulExtractions: integer(1188),
          failedExtractions: integer(62),
          averageDuration: decimal(125.5),
          totalRecordsExtracted: integer(15000000),
          totalDataVolume: integer(5368709120) // 5GB
        }),
        bySourceType: eachLike({
          sourceType: string('JDBC'),
          count: integer(650),
          successRate: decimal(95.0),
          avgDuration: decimal(110.5)
        }, { min: 4 }),
        timeSeriesData: eachLike({
          timestamp: iso8601DateTime(),
          count: integer(42),
          successCount: integer(40),
          failureCount: integer(2),
          recordsExtracted: integer(500000)
        }, { min: 24 }), // 24 hours
        topExtractors: eachLike({
          userId: string('USER-001'),
          userName: string('John Doe'),
          extractionCount: integer(125),
          lastExtraction: iso8601DateTime()
        }, { min: 5 })
      };

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and extraction analytics exist' }],
        uponReceiving: 'a request for extraction analytics',
        withRequest: {
          method: 'GET',
          path: '/api/v1/analytics/extractions',
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

      const result = await analyticsService.getExtractionAnalytics();
      expect(result).toBeDefined();
      expect(result.summary.totalExtractions).toBeGreaterThan(0);
    });
  });

  describe('Migration Analytics', () => {
    it('should fetch migration analytics', async () => {
      const expectedResponse = {
        summary: like({
          totalMigrations: integer(342),
          successfulMigrations: integer(320),
          failedMigrations: integer(14),
          pendingMigrations: integer(8),
          averageDuration: decimal(450.2),
          totalRecordsMigrated: integer(25000000)
        }),
        phaseMetrics: eachLike({
          phase: string('VALIDATION'),
          avgDuration: decimal(45.5),
          successRate: decimal(98.5),
          errorCount: integer(3)
        }, { min: 7 }), // 7 phases
        performanceTrends: eachLike({
          date: regex(/\d{4}-\d{2}-\d{2}/, '2025-01-13'),
          throughput: decimal(50000.5), // records per minute
          latency: decimal(250.0),      // milliseconds
          errorRate: decimal(0.5)
        }, { min: 30 })
      };

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and migration analytics exist' }],
        uponReceiving: 'a request for migration analytics',
        withRequest: {
          method: 'GET',
          path: '/api/v1/analytics/migrations',
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

      const result = await analyticsService.getMigrationAnalytics();
      expect(result).toBeDefined();
      expect(result.summary.totalMigrations).toBeGreaterThan(0);
    });
  });

  describe('Data Quality Analytics', () => {
    it('should fetch data quality analytics', async () => {
      const expectedResponse = {
        overallScore: decimal(92.5),
        dimensionScores: like({
          completeness: decimal(95.0),
          accuracy: decimal(93.5),
          consistency: decimal(91.0),
          validity: decimal(94.0),
          uniqueness: decimal(96.5),
          timeliness: decimal(88.0)
        }),
        ruleExecutions: like({
          totalExecutions: integer(15000),
          passedExecutions: integer(13875),
          failedExecutions: integer(1125),
          avgExecutionTime: decimal(2.5) // seconds
        }),
        issuesTrend: eachLike({
          date: regex(/\d{4}-\d{2}-\d{2}/, '2025-01-13'),
          highSeverity: integer(5),
          mediumSeverity: integer(12),
          lowSeverity: integer(25),
          resolved: integer(35)
        }, { min: 7 }),
        topViolations: eachLike({
          ruleName: string('Email Format Validation'),
          violationCount: integer(125),
          severity: string('HIGH'),
          dimension: string('VALIDITY')
        }, { min: 10 })
      };

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and quality analytics exist' }],
        uponReceiving: 'a request for data quality analytics',
        withRequest: {
          method: 'GET',
          path: '/api/v1/analytics/data-quality',
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

      const result = await analyticsService.getDataQualityAnalytics();
      expect(result).toBeDefined();
      expect(result.overallScore).toBeGreaterThan(0);
    });
  });

  describe('Usage Analytics', () => {
    it('should fetch usage analytics', async () => {
      const expectedResponse = {
        activeUsers: like({
          daily: integer(45),
          weekly: integer(120),
          monthly: integer(250)
        }),
        apiUsage: eachLike({
          endpoint: string('/api/v1/extractions'),
          requestCount: integer(5000),
          avgResponseTime: decimal(125.5),
          errorRate: decimal(0.5)
        }, { min: 10 }),
        userActivity: eachLike({
          userId: string('USER-001'),
          userName: string('John Doe'),
          lastActive: iso8601DateTime(),
          actionsCount: integer(250),
          role: string('DATA_ENGINEER')
        }, { min: 10 }),
        storageUsage: like({
          totalStorage: integer(1099511627776), // 1TB
          usedStorage: integer(536870912000),  // 500GB
          availableStorage: integer(562640715776),
          trend: string('INCREASING')
        })
      };

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and usage analytics exist' }],
        uponReceiving: 'a request for usage analytics',
        withRequest: {
          method: 'GET',
          path: '/api/v1/analytics/usage',
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

      const result = await analyticsService.getUsageAnalytics();
      expect(result).toBeDefined();
      expect(result.activeUsers.daily).toBeGreaterThan(0);
    });
  });

  describe('Compliance Analytics', () => {
    it('should fetch compliance analytics', async () => {
      const expectedResponse = {
        complianceScore: decimal(98.7),
        gdprMetrics: like({
          totalRequests: integer(125),
          completedRequests: integer(118),
          pendingRequests: integer(7),
          avgCompletionTime: decimal(24.5), // hours
          complianceRate: decimal(99.2)
        }),
        ccpaMetrics: like({
          totalRequests: integer(45),
          completedRequests: integer(42),
          pendingRequests: integer(3),
          avgCompletionTime: decimal(36.0), // hours
          complianceRate: decimal(97.5)
        }),
        requestsByType: eachLike({
          type: string('ACCESS'),
          count: integer(55),
          avgProcessingTime: decimal(12.5),
          successRate: decimal(98.0)
        }, { min: 5 }),
        retentionCompliance: like({
          policiesActive: integer(12),
          dataDeleted: integer(1048576000), // 1GB
          dataArchived: integer(5368709120), // 5GB
          upcomingDeletions: integer(25)
        })
      };

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and compliance analytics exist' }],
        uponReceiving: 'a request for compliance analytics',
        withRequest: {
          method: 'GET',
          path: '/api/v1/analytics/compliance',
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

      const result = await analyticsService.getComplianceAnalytics();
      expect(result).toBeDefined();
      expect(result.complianceScore).toBeGreaterThan(0);
    });
  });

  describe('Performance Metrics', () => {
    it('should fetch performance metrics', async () => {
      const expectedResponse = {
        system: like({
          cpuUsage: decimal(45.5),
          memoryUsage: decimal(62.3),
          diskUsage: decimal(55.0),
          networkLatency: decimal(5.5) // ms
        }),
        database: like({
          connectionPoolUsage: decimal(35.0),
          queryResponseTime: decimal(45.5), // ms
          activeConnections: integer(25),
          slowQueries: integer(3)
        }),
        cache: like({
          hitRate: decimal(92.5),
          missRate: decimal(7.5),
          evictionRate: decimal(2.0),
          memoryUsage: decimal(45.0)
        }),
        queue: like({
          messageRate: decimal(125.5), // messages/sec
          processingTime: decimal(50.0), // ms
          queueDepth: integer(150),
          errorRate: decimal(0.1)
        })
      };

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and performance metrics exist' }],
        uponReceiving: 'a request for performance metrics',
        withRequest: {
          method: 'GET',
          path: '/api/v1/analytics/performance',
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

      const result = await analyticsService.getPerformanceMetrics();
      expect(result).toBeDefined();
      expect(result.system.cpuUsage).toBeGreaterThan(0);
    });
  });

  describe('Report Export', () => {
    it('should export analytics report', async () => {
      const expectedRequest = {
        type: 'monthly',
        format: 'pdf',
        filters: {
          startDate: '2025-01-01',
          endDate: '2025-01-31',
          includeCharts: true,
          includeTables: true
        }
      };

      await provider.addInteraction({
        states: [{ description: 'user is authenticated and can export reports' }],
        uponReceiving: 'a request to export analytics report',
        withRequest: {
          method: 'POST',
          path: '/api/v1/analytics/export',
          headers: {
            'Authorization': regex(/^Bearer .+/, 'Bearer valid-token'),
            'Content-Type': 'application/json'
          },
          body: expectedRequest
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/pdf',
            'Content-Disposition': regex(/attachment; filename=.+\.pdf/, 'attachment; filename=report.pdf')
          },
          body: string('PDF content here') // Binary content representation
        }
      });

      const result = await analyticsService.exportReport(
        'monthly',
        'pdf',
        {
          startDate: '2025-01-01',
          endDate: '2025-01-31',
          includeCharts: true,
          includeTables: true
        }
      );

      expect(result).toBeDefined();
    });
  });
});

/**
 * WHY ANALYTICS CONTRACT TESTS MATTER:
 *
 * 1. Analytics drive business decisions
 * 2. Metrics must be consistent across frontend/backend
 * 3. Performance metrics are critical for optimization
 * 4. Compliance analytics ensure regulatory adherence
 * 5. Usage analytics inform capacity planning
 *
 * These tests ensure:
 * - Dashboard metrics calculation consistency
 * - Time series data format alignment
 * - Aggregation logic agreement
 * - Export format compatibility
 * - Performance threshold definitions
 */