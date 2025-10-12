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
      type: 'JDBC',
      host: 'source-db',
      port: 5432,
      database: 'source_db',
      username: 'source_user',
      password: 'source_pass',
    },
    targetConfig: {
      type: 'JDBC',
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
  jdbcToJdbc: {
    name: 'JDBC to JDBC Migration',
    sourceConfig: {
      type: 'JDBC',
      host: 'postgres-source',
      port: 5432,
      database: 'legacy_db',
    },
    targetConfig: {
      type: 'JDBC',
      host: 'postgres-target',
      port: 5432,
      database: 'modern_db',
    },
  },
  sapToJdbc: {
    name: 'SAP to JDBC Migration',
    sourceConfig: {
      type: 'SAP',
      host: 'sap-server',
      client: '100',
      systemNumber: '00',
    },
    targetConfig: {
      type: 'JDBC',
      host: 'postgres-target',
      port: 5432,
      database: 'target_db',
    },
  },
  fileToJdbc: {
    name: 'File to JDBC Migration',
    sourceConfig: {
      type: 'FILE',
      path: '/data/exports/customers.csv',
      format: 'CSV',
    },
    targetConfig: {
      type: 'JDBC',
      host: 'postgres-target',
      port: 5432,
      database: 'target_db',
    },
  },
};
