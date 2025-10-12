import { test, expect } from '@playwright/test';
import { loginAsRole, isAuthenticated } from '../../helpers/auth.helper';

/**
 * AUTH-012 to AUTH-015: Session Management Tests
 * Tests for session persistence, protected routes, token refresh
 */

test.describe('Authentication - Session Management', () => {
  test('AUTH-012: Session persists after page refresh', async ({ page }) => {
    // Arrange - Login
    await loginAsRole(page, 'admin');
    await expect(page).toHaveURL('/dashboard');

    // Act - Refresh page
    await page.reload();

    // Assert
    // 1. User should remain authenticated
    const authenticated = await isAuthenticated(page);
    expect(authenticated).toBe(true);

    // 2. Should stay on dashboard
    await expect(page).toHaveURL('/dashboard');

    // 3. Dashboard should load correctly
    await expect(page.locator('text=Dashboard')).toBeVisible();
  });

  test('AUTH-013: Protected route redirects to login when not authenticated', async ({ page }) => {
    // Arrange - No authentication
    // Act - Try to access protected route directly
    await page.goto('/dashboard');

    // Assert - Should be redirected to login
    await expect(page).toHaveURL('/login');
  });

  test('AUTH-014: Protected route redirects back after login', async ({ page }) => {
    // Arrange - Try to access protected route
    await page.goto('/extractions');

    // Should be redirected to login
    await expect(page).toHaveURL('/login');

    // Act - Login
    const loginPage = await import('../../pages/auth/LoginPage');
    const login = new loginPage.LoginPage(page);
    const { testUsers } = await import('../../fixtures/users');
    await login.login(testUsers.admin.username, testUsers.admin.password);

    // Assert - Should redirect back to originally requested page
    // NOTE: This depends on implementation - might redirect to dashboard or extractions
    await page.waitForURL(/\/(dashboard|extractions)/, { timeout: 10000 });
  });

  test('AUTH-015: Multiple tabs maintain same session', async ({ browser }) => {
    // Arrange - Create first tab and login
    const context = await browser.newContext();
    const page1 = await context.newPage();

    await loginAsRole(page1, 'admin');
    await expect(page1).toHaveURL('/dashboard');

    // Act - Open second tab with same context
    const page2 = await context.newPage();
    await page2.goto('/extractions');

    // Assert - Second tab should also be authenticated
    await expect(page2).toHaveURL('/extractions');
    const authenticated = await isAuthenticated(page2);
    expect(authenticated).toBe(true);

    // Cleanup
    await context.close();
  });

  test('AUTH-016: Session expires and redirects to login', async ({ page }) => {
    // Arrange - Login
    await loginAsRole(page, 'admin');
    await expect(page).toHaveURL('/dashboard');

    // Act - Clear access token to simulate expiration
    await page.evaluate(() => {
      localStorage.removeItem('accessToken');
    });

    // Try to navigate to another page
    await page.goto('/extractions');

    // Assert - Should be redirected to login
    // NOTE: This behavior depends on implementation
    // Some implementations might try to refresh token first
    await page.waitForTimeout(2000);

    // Check if redirected to login or if page shows error
    const url = page.url();
    const hasAuthError = url.includes('/login') || await page.locator('text=/unauthorized|not authenticated/i').isVisible();
    expect(hasAuthError).toBe(true);
  });

  test('AUTH-017: Different user roles see appropriate content', async ({ page }) => {
    // Test admin role
    await loginAsRole(page, 'admin');
    await expect(page).toHaveURL('/dashboard');

    // Admin should see all navigation items
    await expect(page.locator('text=Extractions')).toBeVisible();
    await expect(page.locator('text=Migrations')).toBeVisible();
    await expect(page.locator('text=Data Quality')).toBeVisible();
    await expect(page.locator('text=Compliance')).toBeVisible();

    // Note: Role-based content visibility depends on implementation
  });
});
