/**
 * Comprehensive test data factory for frontend testing.
 * Provides consistent, realistic test data for all testing layers.
 *
 * Features:
 * - Type-safe builder patterns
 * - Faker.js for realistic data generation
 * - Preset scenarios for common test cases
 * - Support for bulk data generation
 * - Mock API responses
 *
 * @since Day 10 of Continuous Testing Implementation
 */

import { faker } from '@faker-js/faker';

// Domain Types (matching backend models)
export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  enabled: boolean;
  createdAt: string;
}

export interface Extraction {
  id: string;
  name: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'STOPPED';
  sourceType: 'JDBC' | 'SAP' | 'FILE' | 'API';
  connectionConfig: Record<string, any>;
  extractionQuery?: string;
  recordsExtracted: number;
  startTime?: string;
  endTime?: string;
  createdAt: string;
  createdBy: string;
}

export interface Migration {
  id: string;
  name: string;
  status: 'PENDING' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED' | 'ROLLING_BACK';
  phase: string;
  progress: number;
  sourceSystem: string;
  targetSystem: string;
  sourceConfig: Record<string, any>;
  targetConfig: Record<string, any>;
  recordsMigrated: number;
  totalRecords: number;
  startTime?: string;
  endTime?: string;
  createdAt: string;
  createdBy: string;
}

export interface DataQualityRule {
  id: string;
  name: string;
  dimension: 'COMPLETENESS' | 'ACCURACY' | 'CONSISTENCY' | 'VALIDITY' | 'UNIQUENESS' | 'TIMELINESS';
  ruleType: string;
  configuration: Record<string, any>;
  enabled: boolean;
  severity: number;
  createdAt: string;
}

export interface ComplianceRequest {
  id: string;
  requestType: 'ACCESS' | 'ERASURE' | 'RECTIFICATION' | 'PORTABILITY' | 'OBJECTION';
  regulation: 'GDPR' | 'CCPA';
  subjectEmail: string;
  subjectName: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'REJECTED';
  requestData: Record<string, any>;
  responseData?: Record<string, any>;
  requestDate: string;
  dueDate: string;
  completedDate?: string;
  assignedTo: string;
}

/**
 * User Builder
 */
export class UserBuilder {
  private user: Partial<User> = {
    id: faker.string.uuid(),
    username: faker.internet.userName(),
    email: faker.internet.email(),
    firstName: faker.person.firstName(),
    lastName: faker.person.lastName(),
    roles: ['ROLE_USER'],
    enabled: true,
    createdAt: faker.date.past().toISOString(),
  };

  withId(id: string): UserBuilder {
    this.user.id = id;
    return this;
  }

  withUsername(username: string): UserBuilder {
    this.user.username = username;
    return this;
  }

  withEmail(email: string): UserBuilder {
    this.user.email = email;
    return this;
  }

  withRoles(...roles: string[]): UserBuilder {
    this.user.roles = roles;
    return this;
  }

  asAdmin(): UserBuilder {
    this.user.roles = ['ROLE_ADMIN'];
    return this;
  }

  asDataEngineer(): UserBuilder {
    this.user.roles = ['ROLE_DATA_ENGINEER'];
    return this;
  }

  asComplianceOfficer(): UserBuilder {
    this.user.roles = ['ROLE_COMPLIANCE_OFFICER'];
    return this;
  }

  build(): User {
    return this.user as User;
  }
}

/**
 * Extraction Builder
 */
export class ExtractionBuilder {
  private extraction: Partial<Extraction> = {
    id: faker.string.uuid(),
    name: `Extraction_${faker.company.name().replace(/\s+/g, '_')}`,
    status: 'PENDING',
    sourceType: faker.helpers.arrayElement(['JDBC', 'SAP', 'FILE', 'API']),
    connectionConfig: {},
    recordsExtracted: 0,
    createdAt: faker.date.recent().toISOString(),
    createdBy: faker.internet.userName(),
  };

  constructor() {
    this.extraction.connectionConfig = this.getDefaultConnectionConfig(this.extraction.sourceType!);
  }

  private getDefaultConnectionConfig(sourceType: string): Record<string, any> {
    switch (sourceType) {
      case 'JDBC':
        return {
          url: `jdbc:postgresql://localhost:5432/${faker.lorem.word()}`,
          username: faker.internet.userName(),
          password: 'password',
          driver: 'org.postgresql.Driver',
        };
      case 'SAP':
        return {
          host: faker.internet.ip(),
          systemNumber: '00',
          client: '100',
          username: faker.internet.userName(),
        };
      case 'FILE':
        return {
          path: `/data/imports/${faker.system.fileName()}`,
          format: faker.helpers.arrayElement(['CSV', 'JSON', 'XML', 'PARQUET']),
          delimiter: ',',
        };
      case 'API':
        return {
          endpoint: `https://api.example.com/${faker.lorem.word()}`,
          method: 'GET',
          authType: 'Bearer',
        };
      default:
        return {};
    }
  }

  withId(id: string): ExtractionBuilder {
    this.extraction.id = id;
    return this;
  }

  withName(name: string): ExtractionBuilder {
    this.extraction.name = name;
    return this;
  }

  withStatus(status: Extraction['status']): ExtractionBuilder {
    this.extraction.status = status;
    return this;
  }

  withSourceType(sourceType: Extraction['sourceType']): ExtractionBuilder {
    this.extraction.sourceType = sourceType;
    this.extraction.connectionConfig = this.getDefaultConnectionConfig(sourceType);
    return this;
  }

  asRunning(): ExtractionBuilder {
    this.extraction.status = 'RUNNING';
    this.extraction.startTime = faker.date.recent({ days: 1 }).toISOString();
    this.extraction.recordsExtracted = faker.number.int({ min: 1000, max: 100000 });
    return this;
  }

  asCompleted(): ExtractionBuilder {
    const startTime = faker.date.recent({ days: 2 });
    this.extraction.status = 'COMPLETED';
    this.extraction.startTime = startTime.toISOString();
    this.extraction.endTime = faker.date.between({
      from: startTime,
      to: new Date()
    }).toISOString();
    this.extraction.recordsExtracted = faker.number.int({ min: 10000, max: 1000000 });
    return this;
  }

  asFailed(): ExtractionBuilder {
    const startTime = faker.date.recent({ days: 1 });
    this.extraction.status = 'FAILED';
    this.extraction.startTime = startTime.toISOString();
    this.extraction.endTime = faker.date.between({
      from: startTime,
      to: new Date()
    }).toISOString();
    return this;
  }

  build(): Extraction {
    return this.extraction as Extraction;
  }
}

/**
 * Migration Builder
 */
export class MigrationBuilder {
  private migration: Partial<Migration> = {
    id: faker.string.uuid(),
    name: `Migration_${faker.company.name().replace(/\s+/g, '_')}`,
    status: 'PENDING',
    phase: 'PLANNING',
    progress: 0,
    sourceSystem: faker.helpers.arrayElement([
      'Oracle Database 12c',
      'SQL Server 2019',
      'MySQL 8.0',
      'PostgreSQL 13'
    ]),
    targetSystem: faker.helpers.arrayElement([
      'PostgreSQL 15',
      'MySQL 8.0',
      'MongoDB 5.0',
      'Snowflake'
    ]),
    sourceConfig: {
      connectionUrl: 'jdbc:oracle:thin:@localhost:1521:ORCL',
      username: 'source_user',
      schema: 'PROD_SCHEMA',
      tables: ['customers', 'orders', 'products'],
    },
    targetConfig: {
      connectionUrl: 'jdbc:postgresql://localhost:5432/target_db',
      username: 'target_user',
      schema: 'public',
    },
    recordsMigrated: 0,
    totalRecords: faker.number.int({ min: 10000, max: 1000000 }),
    createdAt: faker.date.recent().toISOString(),
    createdBy: faker.internet.userName(),
  };

  withId(id: string): MigrationBuilder {
    this.migration.id = id;
    return this;
  }

  withName(name: string): MigrationBuilder {
    this.migration.name = name;
    return this;
  }

  withStatus(status: Migration['status']): MigrationBuilder {
    this.migration.status = status;
    return this;
  }

  inPhase(phase: string, progress: number): MigrationBuilder {
    this.migration.phase = phase;
    this.migration.progress = Math.min(100, Math.max(0, progress));
    return this;
  }

  asRunning(): MigrationBuilder {
    this.migration.status = 'RUNNING';
    this.migration.phase = faker.helpers.arrayElement(['EXTRACTION', 'TRANSFORMATION', 'LOADING']);
    this.migration.progress = faker.number.int({ min: 10, max: 90 });
    this.migration.startTime = faker.date.recent({ days: 1 }).toISOString();
    this.migration.recordsMigrated = Math.floor(
      (this.migration.totalRecords! * this.migration.progress) / 100
    );
    return this;
  }

  asCompleted(): MigrationBuilder {
    const startTime = faker.date.recent({ days: 3 });
    this.migration.status = 'COMPLETED';
    this.migration.phase = 'CLEANUP';
    this.migration.progress = 100;
    this.migration.startTime = startTime.toISOString();
    this.migration.endTime = faker.date.between({
      from: startTime,
      to: new Date()
    }).toISOString();
    this.migration.recordsMigrated = this.migration.totalRecords!;
    return this;
  }

  asFailed(): MigrationBuilder {
    const startTime = faker.date.recent({ days: 2 });
    this.migration.status = 'FAILED';
    this.migration.phase = faker.helpers.arrayElement(['VALIDATION', 'EXTRACTION', 'TRANSFORMATION']);
    this.migration.progress = faker.number.int({ min: 10, max: 60 });
    this.migration.startTime = startTime.toISOString();
    this.migration.endTime = faker.date.between({
      from: startTime,
      to: new Date()
    }).toISOString();
    this.migration.recordsMigrated = Math.floor(
      (this.migration.totalRecords! * this.migration.progress) / 100
    );
    return this;
  }

  build(): Migration {
    return this.migration as Migration;
  }
}

/**
 * Data Quality Rule Builder
 */
export class DataQualityRuleBuilder {
  private rule: Partial<DataQualityRule> = {
    id: faker.string.uuid(),
    name: `Rule_${faker.lorem.word()}`,
    dimension: faker.helpers.arrayElement([
      'COMPLETENESS',
      'ACCURACY',
      'CONSISTENCY',
      'VALIDITY',
      'UNIQUENESS',
      'TIMELINESS'
    ]),
    ruleType: faker.helpers.arrayElement([
      'NULL_CHECK',
      'FORMAT_VALIDATION',
      'RANGE_CHECK',
      'UNIQUENESS_CHECK',
      'REFERENTIAL_INTEGRITY',
      'BUSINESS_RULE'
    ]),
    configuration: {},
    enabled: true,
    severity: faker.number.int({ min: 1, max: 5 }),
    createdAt: faker.date.recent({ days: 30 }).toISOString(),
  };

  constructor() {
    this.rule.configuration = this.getDefaultConfiguration(this.rule.ruleType!);
  }

  private getDefaultConfiguration(ruleType: string): Record<string, any> {
    const config: Record<string, any> = {
      table: 'customers',
      column: faker.lorem.word(),
    };

    switch (ruleType) {
      case 'NULL_CHECK':
        config.allowNull = false;
        break;
      case 'FORMAT_VALIDATION':
        config.pattern = '^[A-Z]{2}[0-9]{6}$';
        break;
      case 'RANGE_CHECK':
        config.min = 0;
        config.max = 100;
        break;
      case 'UNIQUENESS_CHECK':
        config.scope = 'table';
        break;
      case 'REFERENTIAL_INTEGRITY':
        config.referenceTable = 'orders';
        config.referenceColumn = 'customer_id';
        break;
      case 'BUSINESS_RULE':
        config.expression = 'column1 + column2 <= 100';
        break;
    }
    return config;
  }

  withName(name: string): DataQualityRuleBuilder {
    this.rule.name = name;
    return this;
  }

  withDimension(dimension: DataQualityRule['dimension']): DataQualityRuleBuilder {
    this.rule.dimension = dimension;
    return this;
  }

  withRuleType(ruleType: string): DataQualityRuleBuilder {
    this.rule.ruleType = ruleType;
    this.rule.configuration = this.getDefaultConfiguration(ruleType);
    return this;
  }

  withSeverity(severity: number): DataQualityRuleBuilder {
    this.rule.severity = severity;
    return this;
  }

  build(): DataQualityRule {
    return this.rule as DataQualityRule;
  }
}

/**
 * Compliance Request Builder
 */
export class ComplianceRequestBuilder {
  private request: Partial<ComplianceRequest> = {
    id: faker.string.uuid(),
    requestType: faker.helpers.arrayElement(['ACCESS', 'ERASURE', 'RECTIFICATION', 'PORTABILITY', 'OBJECTION']),
    regulation: faker.helpers.arrayElement(['GDPR', 'CCPA']),
    subjectEmail: faker.internet.email(),
    subjectName: faker.person.fullName(),
    status: 'PENDING',
    requestData: {
      reason: faker.lorem.sentence(),
      identityVerified: true,
      dataSources: ['CRM', 'ERP', 'Analytics'],
    },
    requestDate: faker.date.recent({ days: 30 }).toISOString(),
    dueDate: faker.date.future({ days: 30 }).toISOString(),
    assignedTo: faker.internet.userName(),
  };

  constructor() {
    this.updateRequestData();
  }

  private updateRequestData(): void {
    if (this.request.requestType === 'ERASURE') {
      this.request.requestData!.confirmationToken = faker.string.uuid();
      this.request.requestData!.retentionException = false;
    } else if (this.request.requestType === 'PORTABILITY') {
      this.request.requestData!.format = faker.helpers.arrayElement(['JSON', 'CSV', 'XML']);
      this.request.requestData!.deliveryMethod = faker.helpers.arrayElement(['EMAIL', 'DOWNLOAD', 'API']);
    }
  }

  withRequestType(requestType: ComplianceRequest['requestType']): ComplianceRequestBuilder {
    this.request.requestType = requestType;
    this.updateRequestData();
    return this;
  }

  withRegulation(regulation: ComplianceRequest['regulation']): ComplianceRequestBuilder {
    this.request.regulation = regulation;
    return this;
  }

  asProcessing(): ComplianceRequestBuilder {
    this.request.status = 'PROCESSING';
    return this;
  }

  asCompleted(): ComplianceRequestBuilder {
    this.request.status = 'COMPLETED';
    this.request.completedDate = faker.date.recent({ days: 5 }).toISOString();
    this.request.responseData = {
      recordsFound: faker.number.int({ min: 100, max: 10000 }),
      systemsSearched: 12,
      processingTime: '2 hours 15 minutes',
    };
    return this;
  }

  asRejected(): ComplianceRequestBuilder {
    this.request.status = 'REJECTED';
    this.request.completedDate = faker.date.recent({ days: 1 }).toISOString();
    this.request.responseData = {
      rejectionReason: 'Identity verification failed',
    };
    return this;
  }

  build(): ComplianceRequest {
    return this.request as ComplianceRequest;
  }
}

/**
 * Factory functions for quick object creation
 */
export const TestDataFactory = {
  // User factories
  createUser: () => new UserBuilder().build(),
  createAdmin: () => new UserBuilder().asAdmin().build(),
  createDataEngineer: () => new UserBuilder().asDataEngineer().build(),
  createComplianceOfficer: () => new UserBuilder().asComplianceOfficer().build(),

  // Extraction factories
  createExtraction: () => new ExtractionBuilder().build(),
  createRunningExtraction: () => new ExtractionBuilder().asRunning().build(),
  createCompletedExtraction: () => new ExtractionBuilder().asCompleted().build(),
  createFailedExtraction: () => new ExtractionBuilder().asFailed().build(),

  // Migration factories
  createMigration: () => new MigrationBuilder().build(),
  createRunningMigration: () => new MigrationBuilder().asRunning().build(),
  createCompletedMigration: () => new MigrationBuilder().asCompleted().build(),
  createFailedMigration: () => new MigrationBuilder().asFailed().build(),

  // Data Quality factories
  createDataQualityRule: () => new DataQualityRuleBuilder().build(),

  // Compliance factories
  createComplianceRequest: () => new ComplianceRequestBuilder().build(),
  createProcessingComplianceRequest: () => new ComplianceRequestBuilder().asProcessing().build(),
  createCompletedComplianceRequest: () => new ComplianceRequestBuilder().asCompleted().build(),
  createRejectedComplianceRequest: () => new ComplianceRequestBuilder().asRejected().build(),

  // Bulk data generation
  createUsers: (count: number) => Array.from({ length: count }, () => TestDataFactory.createUser()),

  createExtractions: (count: number) =>
    Array.from({ length: count }, () => {
      const rand = Math.random();
      if (rand < 0.25) return TestDataFactory.createRunningExtraction();
      if (rand < 0.5) return TestDataFactory.createCompletedExtraction();
      if (rand < 0.75) return TestDataFactory.createFailedExtraction();
      return TestDataFactory.createExtraction();
    }),

  createMigrations: (count: number) =>
    Array.from({ length: count }, () => {
      const rand = Math.random();
      if (rand < 0.25) return TestDataFactory.createRunningMigration();
      if (rand < 0.5) return TestDataFactory.createCompletedMigration();
      if (rand < 0.75) return TestDataFactory.createFailedMigration();
      return TestDataFactory.createMigration();
    }),

  createDataQualityRules: (count: number) =>
    Array.from({ length: count }, () => TestDataFactory.createDataQualityRule()),

  createComplianceRequests: (count: number) =>
    Array.from({ length: count }, () => {
      const rand = Math.random();
      if (rand < 0.25) return TestDataFactory.createProcessingComplianceRequest();
      if (rand < 0.5) return TestDataFactory.createCompletedComplianceRequest();
      if (rand < 0.75) return TestDataFactory.createRejectedComplianceRequest();
      return TestDataFactory.createComplianceRequest();
    }),
};

/**
 * Mock API response generators
 */
export const MockAPIResponses = {
  // Login response
  createLoginResponse: (user?: Partial<User>) => ({
    accessToken: faker.string.alphanumeric(100),
    refreshToken: faker.string.alphanumeric(100),
    userId: user?.id || faker.string.uuid(),
    username: user?.username || faker.internet.userName(),
    email: user?.email || faker.internet.email(),
    roles: user?.roles || ['ROLE_USER'],
  }),

  // Paginated response
  createPageResponse: <T>(items: T[], page = 0, size = 20) => ({
    content: items.slice(page * size, (page + 1) * size),
    pageable: {
      sort: { sorted: false, unsorted: true },
      pageNumber: page,
      pageSize: size,
      offset: page * size,
      paged: true,
      unpaged: false,
    },
    totalElements: items.length,
    totalPages: Math.ceil(items.length / size),
    last: page >= Math.ceil(items.length / size) - 1,
    first: page === 0,
    number: page,
    size: size,
    numberOfElements: Math.min(size, items.length - page * size),
    empty: items.length === 0,
  }),

  // Analytics response
  createAnalyticsResponse: () => ({
    overview: {
      totalExtractions: faker.number.int({ min: 100, max: 10000 }),
      activeMigrations: faker.number.int({ min: 0, max: 20 }),
      dataQualityScore: faker.number.float({ min: 70, max: 100, fractionDigits: 1 }),
      complianceRate: faker.number.float({ min: 85, max: 100, fractionDigits: 1 }),
    },
    trends: {
      extractionJobs: Array.from({ length: 7 }, (_, i) => ({
        date: faker.date.recent({ days: 7 - i }).toISOString().split('T')[0],
        count: faker.number.int({ min: 5, max: 50 }),
      })),
      migrationStatus: {
        pending: faker.number.int({ min: 0, max: 10 }),
        running: faker.number.int({ min: 0, max: 5 }),
        completed: faker.number.int({ min: 20, max: 100 }),
        failed: faker.number.int({ min: 0, max: 10 }),
      },
    },
    performance: {
      cpuUsage: faker.number.float({ min: 20, max: 80, fractionDigits: 1 }),
      memoryUsage: faker.number.float({ min: 30, max: 85, fractionDigits: 1 }),
      storageUsage: faker.number.float({ min: 40, max: 90, fractionDigits: 1 }),
      networkLatency: faker.number.int({ min: 10, max: 100 }),
    },
    recentActivities: Array.from({ length: 10 }, () => ({
      id: faker.string.uuid(),
      type: faker.helpers.arrayElement(['extraction', 'migration', 'quality', 'compliance']),
      message: faker.lorem.sentence(),
      timestamp: faker.date.recent({ days: 1 }).toISOString(),
      severity: faker.helpers.arrayElement(['info', 'warning', 'error', 'success']),
    })),
  }),

  // Error response
  createErrorResponse: (status = 400, message?: string) => ({
    timestamp: new Date().toISOString(),
    status,
    error: status === 400 ? 'Bad Request' : status === 401 ? 'Unauthorized' : 'Internal Server Error',
    message: message || faker.lorem.sentence(),
    path: `/api/v1/${faker.lorem.word()}`,
  }),
};

/**
 * Test scenario generator
 */
export class TestScenario {
  admin: User;
  dataEngineer: User;
  complianceOfficer: User;
  extractions: Extraction[];
  migrations: Migration[];
  qualityRules: DataQualityRule[];
  complianceRequests: ComplianceRequest[];

  constructor() {
    // Create users with specific roles
    this.admin = new UserBuilder()
      .withUsername('admin')
      .withEmail('admin@jivs.com')
      .asAdmin()
      .build();

    this.dataEngineer = new UserBuilder()
      .withUsername('engineer1')
      .withEmail('engineer@jivs.com')
      .asDataEngineer()
      .build();

    this.complianceOfficer = new UserBuilder()
      .withUsername('compliance1')
      .withEmail('compliance@jivs.com')
      .asComplianceOfficer()
      .build();

    // Create mixed status extractions
    this.extractions = [
      new ExtractionBuilder().withName('Customer_Data_Extract').asCompleted().build(),
      new ExtractionBuilder().withName('Order_History_Extract').asRunning().build(),
      new ExtractionBuilder().withName('Product_Catalog_Extract').build(),
      new ExtractionBuilder().withName('Failed_Extract').asFailed().build(),
    ];

    // Create migrations in various phases
    this.migrations = [
      new MigrationBuilder()
        .withName('Oracle_to_Postgres')
        .asRunning()
        .inPhase('TRANSFORMATION', 65)
        .build(),
      new MigrationBuilder().withName('MySQL_to_MongoDB').asCompleted().build(),
      new MigrationBuilder().withName('SQLServer_to_Snowflake').inPhase('PLANNING', 10).build(),
    ];

    // Create quality rules for all dimensions
    this.qualityRules = [
      new DataQualityRuleBuilder()
        .withName('Email_Format_Check')
        .withDimension('VALIDITY')
        .withRuleType('FORMAT_VALIDATION')
        .build(),
      new DataQualityRuleBuilder()
        .withName('Customer_ID_Uniqueness')
        .withDimension('UNIQUENESS')
        .withRuleType('UNIQUENESS_CHECK')
        .build(),
      new DataQualityRuleBuilder()
        .withName('Order_Amount_Range')
        .withDimension('ACCURACY')
        .withRuleType('RANGE_CHECK')
        .build(),
    ];

    // Create GDPR/CCPA requests
    this.complianceRequests = [
      new ComplianceRequestBuilder()
        .withRequestType('ACCESS')
        .withRegulation('GDPR')
        .asCompleted()
        .build(),
      new ComplianceRequestBuilder()
        .withRequestType('ERASURE')
        .withRegulation('GDPR')
        .asProcessing()
        .build(),
      new ComplianceRequestBuilder()
        .withRequestType('PORTABILITY')
        .withRegulation('CCPA')
        .build(),
    ];
  }

  static createCompleteScenario(): TestScenario {
    return new TestScenario();
  }
}

/**
 * Performance test data generator
 */
export const PerformanceTestData = {
  generateBulkRecords: (count: number) =>
    Array.from({ length: count }, () => ({
      id: faker.string.uuid(),
      customerId: faker.number.int({ min: 1000, max: 99999 }),
      firstName: faker.person.firstName(),
      lastName: faker.person.lastName(),
      email: faker.internet.email(),
      phone: faker.phone.number(),
      address: faker.location.streetAddress(),
      city: faker.location.city(),
      country: faker.location.country(),
      orderCount: faker.number.int({ min: 0, max: 100 }),
      totalSpent: faker.number.float({ min: 0, max: 100000, fractionDigits: 2 }),
      lastOrderDate: faker.date.recent().toISOString(),
      accountStatus: faker.helpers.arrayElement(['ACTIVE', 'INACTIVE', 'SUSPENDED']),
      creditScore: faker.number.int({ min: 300, max: 850 }),
      preferredChannel: faker.helpers.arrayElement(['EMAIL', 'SMS', 'PHONE', 'MAIL']),
    })),

  generateLargeCSV: (rows: number): string => {
    let csv = 'id,customer_id,first_name,last_name,email,phone,order_count,total_spent\n';

    for (let i = 0; i < rows; i++) {
      csv += `${faker.string.uuid()},`;
      csv += `${faker.number.int({ min: 1000, max: 99999 })},`;
      csv += `${faker.person.firstName()},`;
      csv += `${faker.person.lastName()},`;
      csv += `${faker.internet.email()},`;
      csv += `${faker.phone.number()},`;
      csv += `${faker.number.int({ min: 0, max: 100 })},`;
      csv += `${faker.number.float({ min: 0, max: 100000, fractionDigits: 2 })}\n`;
    }

    return csv;
  },
};

// Export all builders for direct use
export {
  UserBuilder,
  ExtractionBuilder,
  MigrationBuilder,
  DataQualityRuleBuilder,
  ComplianceRequestBuilder,
};