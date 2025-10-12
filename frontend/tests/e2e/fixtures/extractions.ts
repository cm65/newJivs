/**
 * Extraction test data factories
 */

export interface ExtractionConfig {
  name: string;
  sourceType: 'JDBC' | 'SAP' | 'FILE' | 'API';
  extractionQuery?: string;
  connectionConfig?: Record<string, any>;
  schedule?: string;
}

/**
 * Create test extraction data with unique timestamp
 */
export function createTestExtraction(overrides: Partial<ExtractionConfig> = {}): ExtractionConfig {
  const timestamp = Date.now();
  return {
    name: `E2E Test Extraction ${timestamp}`,
    sourceType: 'JDBC',
    extractionQuery: 'SELECT * FROM test_table LIMIT 100',
    connectionConfig: {
      host: 'localhost',
      port: 5432,
      database: 'test_db',
      username: 'test_user',
      password: 'test_pass',
    },
    ...overrides,
  };
}

/**
 * Create multiple test extractions
 */
export function createTestExtractions(count: number, overrides: Partial<ExtractionConfig> = {}): ExtractionConfig[] {
  return Array.from({ length: count }, (_, i) =>
    createTestExtraction({
      name: `E2E Test Extraction ${Date.now()}-${i}`,
      ...overrides
    })
  );
}

/**
 * Predefined test extraction configurations
 */
export const testExtractionConfigs = {
  jdbc: {
    name: 'JDBC Test Extraction',
    sourceType: 'JDBC' as const,
    extractionQuery: 'SELECT * FROM customers WHERE active = true',
    connectionConfig: {
      host: 'localhost',
      port: 5432,
      database: 'production',
    },
  },
  sap: {
    name: 'SAP Test Extraction',
    sourceType: 'SAP' as const,
    extractionQuery: 'SELECT * FROM MARA',
    connectionConfig: {
      host: 'sap-server',
      client: '100',
      systemNumber: '00',
    },
  },
  file: {
    name: 'File Test Extraction',
    sourceType: 'FILE' as const,
    connectionConfig: {
      path: '/data/input/customers.csv',
      delimiter: ',',
      hasHeader: true,
    },
  },
  api: {
    name: 'API Test Extraction',
    sourceType: 'API' as const,
    connectionConfig: {
      url: 'https://api.example.com/customers',
      method: 'GET',
      headers: {
        'Authorization': 'Bearer test-token',
      },
    },
  },
};
