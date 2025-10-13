import { Page } from '@playwright/test';
import { testUsers, TestUserRole } from '../fixtures/users';

/**
 * Authentication helper utilities
 */

const API_BASE_URL = 'http://localhost:8080/api/v1';

/**
 * Login via UI
 */
export async function loginViaUI(
  page: Page,
  username: string,
  password: string
): Promise<void> {
  await page.goto('/login');
  await page.fill('[name="username"]', username);
  await page.fill('[name="password"]', password);
  await page.click('button[type="submit"]');
  await page.waitForURL('/dashboard', { timeout: 10000 });
}

/**
 * Login with test user role
 */
export async function loginAsRole(
  page: Page,
  role: TestUserRole = 'admin'
): Promise<void> {
  const user = testUsers[role];
  await loginViaUI(page, user.username, user.password);
}

/**
 * Login via API (faster for setup)
 */
export async function loginViaAPI(
  page: Page,
  username: string,
  password: string
): Promise<{ accessToken: string; refreshToken: string }> {
  const response = await page.request.post(`${API_BASE_URL}/auth/login`, {
    data: {
      username,
      password,
    },
  });

  if (!response.ok()) {
    throw new Error(`Login failed: ${response.status()} ${response.statusText()}`);
  }

  const data = await response.json();
  const loginData = data.data;

  // Store tokens in localStorage
  await page.addInitScript(({ accessToken, refreshToken }) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
  }, loginData);

  return loginData;
}

/**
 * Setup authenticated session for tests
 * This is faster than logging in via UI for every test
 */
export async function setupAuthenticatedSession(
  page: Page,
  role: TestUserRole = 'admin'
): Promise<void> {
  const user = testUsers[role];
  await loginViaAPI(page, user.username, user.password);
  await page.goto('/dashboard');
}

/**
 * Logout via UI
 */
export async function logoutViaUI(page: Page): Promise<void> {
  // Click user menu
  await page.click('[aria-label="account of current user"]');
  // Click logout
  await page.click('text=Logout');
  await page.waitForURL('/login', { timeout: 10000 });
}

/**
 * Logout via API
 */
export async function logoutViaAPI(page: Page): Promise<void> {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));

  if (accessToken) {
    await page.request.post(`${API_BASE_URL}/auth/logout`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
  }

  // Clear localStorage
  await page.evaluate(() => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  });
}

/**
 * Check if user is authenticated
 */
export async function isAuthenticated(page: Page): Promise<boolean> {
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
  return !!accessToken;
}

/**
 * Get authentication tokens from page
 */
export async function getAuthTokens(page: Page): Promise<{
  accessToken: string | null;
  refreshToken: string | null;
}> {
  return await page.evaluate(() => ({
    accessToken: localStorage.getItem('accessToken'),
    refreshToken: localStorage.getItem('refreshToken'),
  }));
}

/**
 * Save authentication state to file
 * Useful for reusing authentication across tests
 */
export async function saveAuthState(page: Page, path: string): Promise<void> {
  await page.context().storageState({ path });
}

/**
 * Clear authentication
 */
export async function clearAuth(page: Page): Promise<void> {
  // Navigate to a valid origin first to avoid "Access is denied" error
  // This ensures localStorage is accessible before clearing
  try {
    await page.goto('http://localhost:3001', { waitUntil: 'domcontentloaded', timeout: 5000 });
  } catch (error) {
    // If navigation fails, continue anyway - localStorage might already be accessible
  }

  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
}
