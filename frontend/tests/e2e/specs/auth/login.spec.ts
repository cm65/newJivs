import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/auth/LoginPage';
import { testUsers } from '../../fixtures/users';
import { clearAuth, isAuthenticated, getAuthTokens } from '../../helpers/auth.helper';

/**
 * AUTH-001 to AUTH-006: Authentication Tests
 * Tests for login, logout, session management
 */

test.describe('Authentication - Login', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await clearAuth(page);
  });

  test('AUTH-001: Successful login with valid credentials', async ({ page }) => {
    // Arrange
    const user = testUsers.admin;
    await loginPage.goto();

    // Act
    await loginPage.login(user.username, user.password);

    // Assert
    // 1. User should be redirected to dashboard
    await expect(page).toHaveURL('/dashboard', { timeout: 10000 });

    // 2. Access token should be stored in localStorage
    const tokens = await getAuthTokens(page);
    expect(tokens.accessToken).not.toBeNull();
    expect(tokens.refreshToken).not.toBeNull();

    // 3. User should be authenticated
    const authenticated = await isAuthenticated(page);
    expect(authenticated).toBe(true);

    // 4. Dashboard should load
    await expect(page.locator('text=Dashboard')).toBeVisible();
  });

  test('AUTH-002: Failed login with invalid username', async ({ page }) => {
    // Arrange
    await loginPage.goto();

    // Act
    await loginPage.login('invalid_user', 'password');

    // Assert
    // 1. Error message should be displayed
    await expect(loginPage.errorAlert).toBeVisible({ timeout: 5000 });
    const errorMsg = await loginPage.getErrorMessage();
    expect(errorMsg.toLowerCase()).toContain('invalid');

    // 2. User should remain on login page
    await expect(page).toHaveURL('/login');

    // 3. No token should be stored
    const authenticated = await isAuthenticated(page);
    expect(authenticated).toBe(false);
  });

  test('AUTH-003: Failed login with invalid password', async ({ page }) => {
    // Arrange
    const user = testUsers.admin;
    await loginPage.goto();

    // Act
    await loginPage.login(user.username, 'wrong_password');

    // Assert
    // 1. Error message should be displayed
    await expect(loginPage.errorAlert).toBeVisible({ timeout: 5000 });

    // 2. User should remain on login page
    await expect(page).toHaveURL('/login');

    // 3. No token should be stored
    const authenticated = await isAuthenticated(page);
    expect(authenticated).toBe(false);
  });

  test('AUTH-004: Login form validation - empty fields', async ({ page }) => {
    // Arrange
    await loginPage.goto();

    // Act
    await loginPage.clickSubmit();

    // Assert
    // Submit button should be disabled or form should show validation errors
    // User should remain on login page
    await expect(page).toHaveURL('/login');
  });

  test('AUTH-005: Password visibility toggle works', async ({ page }) => {
    // Arrange
    await loginPage.goto();
    await loginPage.fillPassword('test123');

    // Act & Assert
    // 1. Password should be hidden by default
    let inputType = await loginPage.getPasswordInputType();
    expect(inputType).toBe('password');

    // 2. Toggle visibility
    await loginPage.togglePasswordVisibility();

    // 3. Password should be visible
    inputType = await loginPage.getPasswordInputType();
    expect(inputType).toBe('text');

    // 4. Toggle back
    await loginPage.togglePasswordVisibility();

    // 5. Password should be hidden again
    inputType = await loginPage.getPasswordInputType();
    expect(inputType).toBe('password');
  });

  test('AUTH-006: Remember me checkbox works', async ({ page }) => {
    // Arrange
    const user = testUsers.admin;
    await loginPage.goto();

    // Act
    await loginPage.login(user.username, user.password, true);

    // Assert
    // User should be logged in successfully
    await expect(page).toHaveURL('/dashboard', { timeout: 10000 });
    const authenticated = await isAuthenticated(page);
    expect(authenticated).toBe(true);
  });

  test('AUTH-007: Login page UI elements are present', async ({ page }) => {
    // Arrange & Act
    await loginPage.goto();

    // Assert - Verify all UI elements
    await loginPage.verifyPageElements();

    // Check page title
    await expect(loginPage.pageTitle).toContainText('JiVS Platform');

    // Check form elements
    await expect(loginPage.usernameInput).toBeVisible();
    await expect(loginPage.passwordInput).toBeVisible();
    await expect(loginPage.submitButton).toBeVisible();
    await expect(loginPage.rememberMeCheckbox).toBeVisible();

    // Check links
    await expect(loginPage.forgotPasswordLink).toBeVisible();
    await expect(loginPage.contactAdminLink).toBeVisible();
  });

  test('AUTH-008: Login with different user roles', async ({ page }) => {
    // Test login with data engineer role
    const engineer = testUsers.dataEngineer;
    await loginPage.goto();
    await loginPage.login(engineer.username, engineer.password);

    // Should redirect to dashboard
    await expect(page).toHaveURL('/dashboard', { timeout: 10000 });
    const authenticated = await isAuthenticated(page);
    expect(authenticated).toBe(true);
  });
});
