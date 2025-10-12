/**
 * Test fixtures for Compliance tests
 * Provides factory functions for creating test compliance data
 */

export interface DataSubjectRequestConfig {
  requestType: 'ACCESS' | 'ERASURE' | 'RECTIFICATION' | 'PORTABILITY' | 'RESTRICTION' | 'OBJECTION';
  regulation: 'GDPR' | 'CCPA';
  dataSubjectId: string;
  dataSubjectEmail: string;
  requestDetails: string;
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
}

export interface RetentionPolicyConfig {
  name: string;
  description: string;
  dataType: string;
  retentionPeriod: number;
  retentionUnit: 'DAYS' | 'MONTHS' | 'YEARS';
  action: 'DELETE' | 'ARCHIVE' | 'COLD_STORAGE' | 'ANONYMIZE' | 'SOFT_DELETE' | 'NOTIFY';
  enabled: boolean;
}

export function createTestDataSubjectRequest(overrides: Partial<DataSubjectRequestConfig> = {}): DataSubjectRequestConfig {
  const timestamp = Date.now();
  return {
    requestType: 'ACCESS',
    regulation: 'GDPR',
    dataSubjectId: `TEST_USER_${timestamp}`,
    dataSubjectEmail: `test_${timestamp}@example.com`,
    requestDetails: `E2E test request created at ${new Date().toISOString()}`,
    priority: 'MEDIUM',
    ...overrides,
  };
}

export function createTestRetentionPolicy(overrides: Partial<RetentionPolicyConfig> = {}): RetentionPolicyConfig {
  const timestamp = Date.now();
  return {
    name: `E2E Test Policy ${timestamp}`,
    description: `Test retention policy created at ${new Date().toISOString()}`,
    dataType: 'TEST_DATA',
    retentionPeriod: 90,
    retentionUnit: 'DAYS',
    action: 'ARCHIVE',
    enabled: true,
    ...overrides,
  };
}

export const requestTypes = ['ACCESS', 'ERASURE', 'RECTIFICATION', 'PORTABILITY', 'RESTRICTION', 'OBJECTION'] as const;
export const regulations = ['GDPR', 'CCPA'] as const;
export const priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const;
export const retentionActions = ['DELETE', 'ARCHIVE', 'COLD_STORAGE', 'ANONYMIZE', 'SOFT_DELETE', 'NOTIFY'] as const;
