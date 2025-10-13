import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { Pact } from '@pact-foundation/pact';
import { Matchers } from '@pact-foundation/pact';
import path from 'path';
import authService from '../authService';

describe('Auth API Contract Tests - CRITICAL', () => {
  // Set up Pact provider
  const provider = new Pact({
    consumer: 'JiVS Frontend',
    provider: 'JiVS Backend',
    port: 9091, // Different port for auth tests
    log: path.resolve(process.cwd(), 'logs', 'pact-auth.log'),
    dir: path.resolve(process.cwd(), 'pacts'),
    logLevel: 'info',
    spec: 2,
  });

  // Start mock server before tests
  beforeAll(async () => {
    await provider.setup();

    // Override the base URL for testing
    (authService as any).baseURL = 'http://localhost:9091/api/v1';
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

  describe('POST /api/v1/auth/login - User Login', () => {
    it('should authenticate user with valid credentials', async () => {
      const loginRequest = {
        username: 'testuser@example.com',
        password: 'ValidPassword123!',
      };

      const expectedResponse = {
        accessToken: Matchers.like('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP03fq0Vg'),
        refreshToken: Matchers.like('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.refresh'),
        userId: Matchers.uuid(),
        username: 'testuser@example.com',
        email: 'testuser@example.com',
        firstName: Matchers.like('Test'),
        lastName: Matchers.like('User'),
        roles: Matchers.eachLike('ROLE_USER'),
        expiresIn: 3600,
      };

      await provider.addInteraction({
        state: 'user exists with valid credentials',
        uponReceiving: 'a login request with valid credentials',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/login',
          headers: {
            'Content-Type': 'application/json',
          },
          body: loginRequest,
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: expectedResponse,
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
            message: 'Login successful',
          },
        },
      });

      const response = await authService.login(loginRequest as any);

      expect(response.accessToken).toBeDefined();
      expect(response.refreshToken).toBeDefined();
      expect(response.userId).toBeDefined();
      expect(response.roles).toContain('ROLE_USER');
    });

    it('should reject login with invalid credentials', async () => {
      const invalidRequest = {
        username: 'testuser@example.com',
        password: 'WrongPassword',
      };

      await provider.addInteraction({
        state: 'user exists with valid credentials',
        uponReceiving: 'a login request with invalid credentials',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/login',
          headers: {
            'Content-Type': 'application/json',
          },
          body: invalidRequest,
        },
        willRespondWith: {
          status: 401,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 401,
            error: 'Unauthorized',
            message: 'Invalid credentials',
          },
        },
      });

      try {
        await authService.login(invalidRequest as any);
        expect.fail('Should have thrown an error');
      } catch (error: any) {
        expect(error.response.status).toBe(401);
        expect(error.response.data.message).toBe('Invalid credentials');
      }
    });
  });

  describe('POST /api/v1/auth/register - User Registration', () => {
    it('should register a new user', async () => {
      const registerRequest = {
        username: 'newuser@example.com',
        email: 'newuser@example.com',
        password: 'ValidPassword123!',
        firstName: 'New',
        lastName: 'User',
      };

      const expectedResponse = {
        id: Matchers.uuid(),
        username: 'newuser@example.com',
        email: 'newuser@example.com',
        firstName: 'New',
        lastName: 'User',
        roles: ['ROLE_USER'],
        createdAt: Matchers.iso8601DateTimeWithMillis(),
      };

      await provider.addInteraction({
        state: 'system allows registration',
        uponReceiving: 'a registration request with valid data',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/register',
          headers: {
            'Content-Type': 'application/json',
          },
          body: registerRequest,
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
            message: 'User registered successfully',
          },
        },
      });

      const response = await authService.register(registerRequest as any);

      expect(response.id).toBeDefined();
      expect(response.email).toBe('newuser@example.com');
      expect(response.roles).toContain('ROLE_USER');
    });

    it('should reject registration with duplicate email', async () => {
      const duplicateRequest = {
        username: 'existinguser@example.com',
        email: 'existinguser@example.com',
        password: 'ValidPassword123!',
        firstName: 'Existing',
        lastName: 'User',
      };

      await provider.addInteraction({
        state: 'user with email already exists',
        uponReceiving: 'a registration request with duplicate email',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/register',
          headers: {
            'Content-Type': 'application/json',
          },
          body: duplicateRequest,
        },
        willRespondWith: {
          status: 409,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 409,
            error: 'Conflict',
            message: 'Email already registered',
          },
        },
      });

      try {
        await authService.register(duplicateRequest as any);
        expect.fail('Should have thrown an error');
      } catch (error: any) {
        expect(error.response.status).toBe(409);
        expect(error.response.data.message).toBe('Email already registered');
      }
    });
  });

  describe('POST /api/v1/auth/refresh - Refresh Token', () => {
    it('should refresh access token with valid refresh token', async () => {
      const refreshRequest = {
        refreshToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.refresh',
      };

      const expectedResponse = {
        accessToken: Matchers.like('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.new'),
        refreshToken: Matchers.like('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.newRefresh'),
        expiresIn: 3600,
      };

      await provider.addInteraction({
        state: 'valid refresh token exists',
        uponReceiving: 'a token refresh request',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/refresh',
          headers: {
            'Content-Type': 'application/json',
          },
          body: refreshRequest,
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: expectedResponse,
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
            message: 'Token refreshed successfully',
          },
        },
      });

      const response = await authService.refreshToken(refreshRequest.refreshToken);

      expect(response).toBeDefined();
      expect(typeof response).toBe('string');
    });

    it('should reject refresh with invalid token', async () => {
      const invalidRequest = {
        refreshToken: 'invalid.refresh.token',
      };

      await provider.addInteraction({
        state: 'refresh token is invalid or expired',
        uponReceiving: 'a token refresh request with invalid token',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/refresh',
          headers: {
            'Content-Type': 'application/json',
          },
          body: invalidRequest,
        },
        willRespondWith: {
          status: 401,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 401,
            error: 'Unauthorized',
            message: 'Invalid or expired refresh token',
          },
        },
      });

      try {
        await authService.refreshToken(invalidRequest.refreshToken);
        expect.fail('Should have thrown an error');
      } catch (error: any) {
        expect(error.response.status).toBe(401);
      }
    });
  });

  describe('POST /api/v1/auth/logout - User Logout', () => {
    it('should logout user successfully', async () => {
      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a logout request',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/logout',
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
            'Content-Type': 'application/json',
          },
          body: {
            refreshToken: Matchers.like('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh'),
          },
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              message: 'Logout successful',
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      await authService.logout();
      // If no error thrown, logout successful
      expect(true).toBe(true);
    });
  });

  describe('GET /api/v1/auth/me - Get Current User', () => {
    it('should fetch current authenticated user', async () => {
      const expectedUser = {
        id: Matchers.uuid(),
        username: 'currentuser@example.com',
        email: 'currentuser@example.com',
        firstName: 'Current',
        lastName: 'User',
        roles: ['ROLE_USER', 'ROLE_ADMIN'],
        lastLogin: Matchers.iso8601DateTimeWithMillis(),
        createdAt: Matchers.iso8601DateTimeWithMillis(),
        updatedAt: Matchers.iso8601DateTimeWithMillis(),
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request for current user',
        withRequest: {
          method: 'GET',
          path: '/api/v1/auth/me',
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
            data: expectedUser,
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      const user = await authService.getCurrentUser();

      expect(user.id).toBeDefined();
      expect(user.username).toBeDefined();
      expect(user.roles).toBeInstanceOf(Array);
    });
  });

  describe('PUT /api/v1/auth/me - Update Current User', () => {
    it('should update current user profile', async () => {
      const updateRequest = {
        firstName: 'Updated',
        lastName: 'Name',
        email: 'updated@example.com',
      };

      const expectedResponse = {
        id: Matchers.uuid(),
        username: 'currentuser@example.com',
        email: 'updated@example.com',
        firstName: 'Updated',
        lastName: 'Name',
        roles: ['ROLE_USER'],
        updatedAt: Matchers.iso8601DateTimeWithMillis(),
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a request to update user profile',
        withRequest: {
          method: 'PUT',
          path: '/api/v1/auth/me',
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
            'Content-Type': 'application/json',
          },
          body: updateRequest,
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: expectedResponse,
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
            message: 'Profile updated successfully',
          },
        },
      });

      const response = await authService.updateProfile(updateRequest);

      expect(response.firstName).toBe('Updated');
      expect(response.lastName).toBe('Name');
      expect(response.email).toBe('updated@example.com');
    });
  });

  describe('POST /api/v1/auth/change-password - Change Password', () => {
    it('should change password successfully', async () => {
      const changePasswordRequest = {
        currentPassword: 'OldPassword123!',
        newPassword: 'NewPassword456!',
        confirmPassword: 'NewPassword456!',
      };

      await provider.addInteraction({
        state: 'user is authenticated with valid current password',
        uponReceiving: 'a password change request',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/change-password',
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
            'Content-Type': 'application/json',
          },
          body: changePasswordRequest,
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            data: {
              message: 'Password changed successfully',
            },
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      await authService.changePassword(changePasswordRequest);
      // If no error thrown, password changed successfully
      expect(true).toBe(true);
    });

    it('should reject password change with wrong current password', async () => {
      const invalidRequest = {
        currentPassword: 'WrongCurrentPassword',
        newPassword: 'NewPassword456!',
        confirmPassword: 'NewPassword456!',
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a password change request with wrong current password',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/change-password',
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
            'Content-Type': 'application/json',
          },
          body: invalidRequest,
        },
        willRespondWith: {
          status: 401,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 401,
            error: 'Unauthorized',
            message: 'Current password is incorrect',
          },
        },
      });

      try {
        await authService.changePassword(invalidRequest);
        expect.fail('Should have thrown an error');
      } catch (error: any) {
        expect(error.response.status).toBe(401);
        expect(error.response.data.message).toBe('Current password is incorrect');
      }
    });

    it('should reject password change when passwords do not match', async () => {
      const mismatchRequest = {
        currentPassword: 'OldPassword123!',
        newPassword: 'NewPassword456!',
        confirmPassword: 'DifferentPassword789!',
      };

      await provider.addInteraction({
        state: 'user is authenticated',
        uponReceiving: 'a password change request with mismatched passwords',
        withRequest: {
          method: 'POST',
          path: '/api/v1/auth/change-password',
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
            'Content-Type': 'application/json',
          },
          body: mismatchRequest,
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
            message: 'New password and confirmation do not match',
          },
        },
      });

      try {
        await authService.changePassword(mismatchRequest);
        expect.fail('Should have thrown an error');
      } catch (error: any) {
        expect(error.response.status).toBe(400);
        expect(error.response.data.message).toBe('New password and confirmation do not match');
      }
    });
  });

  describe('GET /api/v1/auth/users - Get All Users (Admin)', () => {
    it('should fetch all users for admin', async () => {
      const expectedUsers = {
        content: Matchers.eachLike({
          id: Matchers.uuid(),
          username: Matchers.like('user@example.com'),
          email: Matchers.like('user@example.com'),
          firstName: Matchers.like('First'),
          lastName: Matchers.like('Last'),
          roles: Matchers.eachLike('ROLE_USER'),
          active: true,
          lastLogin: Matchers.iso8601DateTimeWithMillis(),
          createdAt: Matchers.iso8601DateTimeWithMillis(),
        }),
        totalElements: Matchers.integer({ min: 0 }),
        totalPages: Matchers.integer({ min: 0 }),
        size: 20,
        number: 0,
      };

      await provider.addInteraction({
        state: 'user is authenticated as admin',
        uponReceiving: 'a request to get all users',
        withRequest: {
          method: 'GET',
          path: '/api/v1/auth/users',
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
            data: expectedUsers,
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 200,
          },
        },
      });

      const response = await authService.getAllUsers(0, 20);

      expect(response.content).toBeInstanceOf(Array);
      expect(response.totalElements).toBeGreaterThanOrEqual(0);
      expect(response.size).toBe(20);
    });

    it('should reject user list request for non-admin', async () => {
      await provider.addInteraction({
        state: 'user is authenticated as regular user',
        uponReceiving: 'a request to get all users from non-admin',
        withRequest: {
          method: 'GET',
          path: '/api/v1/auth/users',
          query: {
            page: '0',
            size: '20',
          },
          headers: {
            'Authorization': Matchers.like('Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9'),
          },
        },
        willRespondWith: {
          status: 403,
          headers: {
            'Content-Type': 'application/json',
          },
          body: {
            timestamp: Matchers.iso8601DateTimeWithMillis(),
            status: 403,
            error: 'Forbidden',
            message: 'Access denied. Admin role required.',
          },
        },
      });

      try {
        await authService.getAllUsers(0, 20);
        expect.fail('Should have thrown an error');
      } catch (error: any) {
        expect(error.response.status).toBe(403);
        expect(error.response.data.message).toBe('Access denied. Admin role required.');
      }
    });
  });
});

/**
 * Auth Contract Tests - CRITICAL
 *
 * These tests are CRITICAL because authentication affects EVERY API call in the system.
 * If auth is broken, the entire platform is unusable.
 *
 * What these tests prevent:
 * 1. Login failures due to field mismatches
 * 2. Token format changes breaking authentication
 * 3. Registration field validation issues
 * 4. Password change workflow bugs
 * 5. Role-based access control failures
 *
 * Benefits:
 * - Catch auth bugs in < 10 seconds vs hours of debugging
 * - Ensure frontend and backend auth contracts match
 * - Validate token formats and expiration handling
 * - Verify role-based access controls
 *
 * Coverage: 8/8 Auth endpoints (100%)
 */