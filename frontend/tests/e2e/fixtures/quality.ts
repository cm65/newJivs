/**
 * Test fixtures for Data Quality tests
 * Provides factory functions for creating test data quality rules and configurations
 */

export interface QualityRuleConfig {
  name: string;
  description: string;
  ruleType: 'NULL_CHECK' | 'FORMAT_VALIDATION' | 'RANGE_VALIDATION' | 'UNIQUENESS' | 'REFERENTIAL_INTEGRITY' | 'BUSINESS_RULE';
  dimension: 'COMPLETENESS' | 'ACCURACY' | 'CONSISTENCY' | 'VALIDITY' | 'UNIQUENESS' | 'TIMELINESS';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  enabled: boolean;
  parameters: Record<string, any>;
}

export interface ProfileRequest {
  datasetName: string;
  dataSource: string;
  tableName?: string;
  query?: string;
}

/**
 * Create test quality rule with default values
 */
export function createTestQualityRule(overrides: Partial<QualityRuleConfig> = {}): QualityRuleConfig {
  const timestamp = Date.now();
  return {
    name: `E2E Test Rule ${timestamp}`,
    description: `Test rule created by E2E tests at ${new Date().toISOString()}`,
    ruleType: 'NULL_CHECK',
    dimension: 'COMPLETENESS',
    severity: 'MEDIUM',
    enabled: true,
    parameters: {},
    ...overrides,
  };
}

/**
 * Create test profile request
 */
export function createTestProfileRequest(overrides: Partial<ProfileRequest> = {}): ProfileRequest {
  const timestamp = Date.now();
  return {
    datasetName: `E2E_TEST_DATASET_${timestamp}`,
    dataSource: 'JDBC',
    tableName: 'test_table',
    query: 'SELECT * FROM test_table LIMIT 1000',
    ...overrides,
  };
}

/**
 * Predefined quality rules for common scenarios
 */
export const testQualityRules = {
  nullCheck: {
    name: 'Null Check - Customer Email',
    description: 'Ensures customer email field is never null',
    ruleType: 'NULL_CHECK' as const,
    dimension: 'COMPLETENESS' as const,
    severity: 'HIGH' as const,
    enabled: true,
    parameters: {
      tableName: 'customers',
      columnName: 'email',
    },
  },
  formatValidation: {
    name: 'Format Validation - Phone Number',
    description: 'Validates phone number format matches pattern',
    ruleType: 'FORMAT_VALIDATION' as const,
    dimension: 'VALIDITY' as const,
    severity: 'MEDIUM' as const,
    enabled: true,
    parameters: {
      tableName: 'customers',
      columnName: 'phone',
      pattern: '^\\+?[1-9]\\d{1,14}$',
    },
  },
  rangeValidation: {
    name: 'Range Validation - Age',
    description: 'Validates age is between 0 and 150',
    ruleType: 'RANGE_VALIDATION' as const,
    dimension: 'VALIDITY' as const,
    severity: 'HIGH' as const,
    enabled: true,
    parameters: {
      tableName: 'customers',
      columnName: 'age',
      minValue: 0,
      maxValue: 150,
    },
  },
  uniquenessCheck: {
    name: 'Uniqueness Check - Customer ID',
    description: 'Ensures customer ID is unique across records',
    ruleType: 'UNIQUENESS' as const,
    dimension: 'UNIQUENESS' as const,
    severity: 'CRITICAL' as const,
    enabled: true,
    parameters: {
      tableName: 'customers',
      columnName: 'customer_id',
    },
  },
  referentialIntegrity: {
    name: 'Referential Integrity - Order Customer',
    description: 'Ensures every order references a valid customer',
    ruleType: 'REFERENTIAL_INTEGRITY' as const,
    dimension: 'CONSISTENCY' as const,
    severity: 'HIGH' as const,
    enabled: true,
    parameters: {
      sourceTable: 'orders',
      sourceColumn: 'customer_id',
      targetTable: 'customers',
      targetColumn: 'id',
    },
  },
  businessRule: {
    name: 'Business Rule - Order Total',
    description: 'Validates order total matches sum of line items',
    ruleType: 'BUSINESS_RULE' as const,
    dimension: 'ACCURACY' as const,
    severity: 'CRITICAL' as const,
    enabled: true,
    parameters: {
      tableName: 'orders',
      expression: 'order_total = SUM(line_items.price * line_items.quantity)',
    },
  },
};

/**
 * Quality dimensions for testing
 */
export const qualityDimensions = [
  'COMPLETENESS',
  'ACCURACY',
  'CONSISTENCY',
  'VALIDITY',
  'UNIQUENESS',
  'TIMELINESS',
] as const;

/**
 * Severity levels for testing
 */
export const severityLevels = [
  'LOW',
  'MEDIUM',
  'HIGH',
  'CRITICAL',
] as const;

/**
 * Issue statuses for testing
 */
export const issueStatuses = [
  'OPEN',
  'IN_PROGRESS',
  'RESOLVED',
  'IGNORED',
] as const;

/**
 * Rule types for testing
 */
export const ruleTypes = [
  'NULL_CHECK',
  'FORMAT_VALIDATION',
  'RANGE_VALIDATION',
  'UNIQUENESS',
  'REFERENTIAL_INTEGRITY',
  'BUSINESS_RULE',
] as const;
