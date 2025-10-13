/**
 * Migration test data factories
 */

export interface MigrationConfig {
  name: string;
  sourceConfig: Record<string, any>;
  targetConfig: Record<string, any>;
  transformationRules?: any[];
  schedule?: string;
}

/**
 * Create test migration data with unique timestamp
 */
export function createTestMigration(overrides: Partial<MigrationConfig> = {}): MigrationConfig {
  const timestamp = Date.now();
  return {
    name: `E2E Test Migration ${timestamp}`,
    sourceConfig: {
      type: 'POSTGRESQL',
      host: 'source-db',
      port: 5432,
      database: 'source_db',
      username: 'source_user',
      password: 'source_pass',
    },
    targetConfig: {
      type: 'POSTGRESQL',
      host: 'target-db',
      port: 5432,
      database: 'target_db',
      username: 'target_user',
      password: 'target_pass',
    },
    transformationRules: [],
    ...overrides,
  };
}

/**
 * Create multiple test migrations
 */
export function createTestMigrations(count: number, overrides: Partial<MigrationConfig> = {}): MigrationConfig[] {
  return Array.from({ length: count }, (_, i) =>
    createTestMigration({
      name: `E2E Test Migration ${Date.now()}-${i}`,
      ...overrides,
    })
  );
}

/**
 * Predefined test migration configurations
 */
export const testMigrationConfigs = {
  postgresqlToPostgresql: {
    name: 'PostgreSQL to PostgreSQL Migration',
    sourceConfig: {
      type: 'POSTGRESQL',
      host: 'postgres-source',
      port: 5432,
      database: 'legacy_db',
    },
    targetConfig: {
      type: 'POSTGRESQL',
      host: 'postgres-target',
      port: 5432,
      database: 'modern_db',
    },
  },
  sapToPostgresql: {
    name: 'SAP to PostgreSQL Migration',
    sourceConfig: {
      type: 'SAP',
      host: 'sap-server',
      client: '100',
      systemNumber: '00',
    },
    targetConfig: {
      type: 'POSTGRESQL',
      host: 'postgres-target',
      port: 5432,
      database: 'target_db',
    },
  },
  fileToPostgresql: {
    name: 'File to PostgreSQL Migration',
    sourceConfig: {
      type: 'FILE',
      path: '/data/exports/customers.csv',
      format: 'CSV',
    },
    targetConfig: {
      type: 'POSTGRESQL',
      host: 'postgres-target',
      port: 5432,
      database: 'target_db',
    },
  },
};
