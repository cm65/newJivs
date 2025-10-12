import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/auth/LoginPage';
import { testUsers } from '../../fixtures/users';
import { loginAsRole, isAuthenticated, getAuthTokens } from '../../helpers/auth.helper';

/**
 * AUTH-009 to AUTH-011: Logout Tests
 * Tests for logout functionality
 */

test.describe('Authentication - Logout', () => {
  test.beforeEach(async ({ page }) => {
    // Setup: Login before each test
    await loginAsRole(page, 'admin');
  });

  test('AUTH-009: Successful logout clears session', async ({ page }) => {
    // Arrange - Already logged in from beforeEach
    await expect(page).toHaveURL('/dashboard');
    const authenticatedBefore = await isAuthenticated(page);
    expect(authenticatedBefore).toBe(true);

    // Act - Logout
    // Click user menu
    await page.click('[aria-label="account of current user"], button:has([data-testid="AccountCircle"])');
    // Wait for menu to open
    await page.waitForTimeout(500);
    // Click logout option
    await page.click('text=Logout');

    // Assert
    // 1. Should redirect to login page
    await expect(page).toHaveURL('/login', { timeout: 10000 });

    // 2. localStorage should be cleared
    const tokens = await getAuthTokens(page);
    expect(tokens.accessToken).toBeNull();
    expect(tokens.refreshToken).toBeNull();

    // 3. User should not be authenticated
    const authenticatedAfter = await isAuthenticated(page);
    expect(authenticatedAfter).toBe(false);
  });

  test('AUTH-010: Logout and try to access protected route', async ({ page }) => {
    // Arrange - Logged in
    await expect(page).toHaveURL('/dashboard');

    // Act - Logout
    await page.click('[aria-label="account of current user"], button:has([data-testid="AccountCircle"])');
    await page.waitForTimeout(500);
    await page.click('text=Logout');
    await expect(page).toHaveURL('/login', { timeout: 10000 });

    // Try to access protected route
    await page.goto('/extractions');

    // Assert - Should be redirected back to login
    await expect(page).toHaveURL('/login');
  });

  test('AUTH-011: Logout from different pages works', async ({ page }) => {
    // Test logout from Extractions page
    await page.goto('/extractions');
    await page.waitForLoadState('networkidle');

    await page.click('[aria-label="account of current user"], button:has([data-testid="AccountCircle"])');
    await page.waitForTimeout(500);
    await page.click('text=Logout');

    // Should redirect to login
    await expect(page).toHaveURL('/login', { timeout: 10000 });

    // Login again and test from another page
    const loginPage = new LoginPage(page);
    const user = testUsers.admin;
    await loginPage.login(user.username, user.password);

    // Go to Migrations page
    await page.goto('/migrations');
    await page.waitForLoadState('networkidle');

    // Logout again
    await page.click('[aria-label="account of current user"], button:has([data-testid="AccountCircle"])');
    await page.waitForTimeout(500);
    await page.click('text=Logout');

    // Should redirect to login
    await expect(page).toHaveURL('/login', { timeout: 10000 });
  });
});
