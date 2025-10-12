/**
 * Test user data fixtures
 * These users should exist in the test database
 */

export interface TestUser {
  username: string;
  password: string;
  role: string;
  email: string;
}

export const testUsers = {
  admin: {
    username: 'admin',
    password: 'password',
    role: 'ROLE_ADMIN',
    email: 'admin@jivs.com',
  },
  dataEngineer: {
    username: 'engineer',
    password: 'password123',
    role: 'ROLE_DATA_ENGINEER',
    email: 'engineer@jivs.com',
  },
  complianceOfficer: {
    username: 'compliance',
    password: 'password123',
    role: 'ROLE_COMPLIANCE_OFFICER',
    email: 'compliance@jivs.com',
  },
  viewer: {
    username: 'viewer',
    password: 'password123',
    role: 'ROLE_VIEWER',
    email: 'viewer@jivs.com',
  },
} as const;

export type TestUserRole = keyof typeof testUsers;

export function getTestUser(role: TestUserRole = 'admin'): TestUser {
  return testUsers[role];
}
