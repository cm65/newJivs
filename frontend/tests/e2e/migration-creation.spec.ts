import { test, expect } from '@playwright/test';
import { login } from './helpers/auth.helper';

/**
 * E2E Test: Migration Creation Flow
 * THIS TEST WOULD HAVE CAUGHT THE BUG BY SIMULATING REAL USER INTERACTION
 */

test.describe('Migration Creation', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto('http://localhost:3001/migrations');
  });

  test('should create a new migration successfully', async ({ page }) => {
    // Click "New Migration" button
    await page.click('button:has-text("New Migration")');

    // Fill in the migration form
    await page.fill('input[name="name"]', 'E2E Test Migration');
    await page.fill('input[name="description"]', 'Created by automated E2E test');
    await page.fill('input[name="sourceSystem"]', 'Oracle Database');
    await page.fill('input[name="targetSystem"]', 'PostgreSQL 15');
    await page.selectOption('select[name="migrationType"]', 'FULL');

    // Click Create button
    await page.click('button:has-text("Create")');

    // THIS IS WHERE THE BUG WOULD BE CAUGHT:
    // The test would fail with "failed to create new migration"
    // Instead of showing "Success" message

    // Verify success message appears
    await expect(page.locator('.MuiAlert-success')).toBeVisible({ timeout: 5000 });

    // Verify the migration appears in the table
    await expect(page.locator('table tbody tr:has-text("E2E Test Migration")')).toBeVisible();
  });

  test('should show error message when migration creation fails', async ({ page }) => {
    await page.click('button:has-text("New Migration")');

    // Submit form without required fields
    await page.click('button:has-text("Create")');

    // Should show validation error
    await expect(page.locator('.MuiAlert-error')).toBeVisible();
  });
});
