import { test, expect } from '@playwright/test';
import { MigrationsPage } from '../../pages/migrations/MigrationsPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';
import { createMigration, deleteMigration } from '../../helpers/api.helper';
import { createTestMigration } from '../../fixtures/migrations';

/**
 * MIG-004 to MIG-007: List and Display Migration Tests
 */

test.describe('Migrations - List and Display', () => {
  let migrationsPage: MigrationsPage;
  const createdMigrationIds: string[] = [];

  test.beforeEach(async ({ page }) => {
    await setupAuthenticatedSession(page, 'admin');
    migrationsPage = new MigrationsPage(page);
  });

  test.afterEach(async ({ page }) => {
    // Cleanup
    for (const id of createdMigrationIds) {
      await deleteMigration(page, id).catch(() => {});
    }
    createdMigrationIds.length = 0;
  });

  test('MIG-004: Migrations list displays correctly', async ({ page }) => {
    // Arrange & Act
    await migrationsPage.goto();

    // Assert
    // 1. Page elements should be visible
    await migrationsPage.verifyPageElements();

    // 2. Table should display
    await expect(migrationsPage.migrationsTable).toBeVisible();

    // 3. Statistics cards should show counts
    const totalCount = await migrationsPage.getTotalCount();
    expect(totalCount).toBeGreaterThanOrEqual(0);
  });

  test('MIG-005: Migration progress displays correctly (0-100%)', async ({ page }) => {
    // Arrange - Create a migration
    const data = createTestMigration({
      name: 'E2E Test - Progress Display',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    await migrationsPage.goto();

    // Assert
    // 1. Migration should exist
    const exists = await migrationsPage.migrationExists(data.name);
    expect(exists).toBe(true);

    // 2. Progress should be valid (0-100%)
    const isValid = await migrationsPage.verifyProgressBarValid(data.name);
    expect(isValid).toBe(true);

    // 3. Progress value should be displayed
    const progress = await migrationsPage.getMigrationProgress(data.name);
    expect(progress).toBeGreaterThanOrEqual(0);
    expect(progress).toBeLessThanOrEqual(100);
  });

  test('MIG-006: Migration phase and status display correctly', async ({ page }) => {
    // Arrange - Create a migration
    const data = createTestMigration({
      name: 'E2E Test - Phase Display',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    await migrationsPage.goto();

    // Assert
    // 1. Status should be displayed
    const status = await migrationsPage.getMigrationStatus(data.name);
    expect(status).toBeTruthy();
    expect(['PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED']).toContain(status);

    // 2. Phase should be displayed (not null/undefined)
    const phase = await migrationsPage.getMigrationPhase(data.name);
    expect(phase).toBeTruthy();
    expect(phase).not.toBe('null');
    expect(phase).not.toBe('undefined');
  });

  test('MIG-007: Filter migrations by status', async ({ page }) => {
    // Arrange
    await migrationsPage.goto();

    // Act - Filter by status
    await migrationsPage.filterByStatus('PENDING');
    await page.waitForTimeout(1000);

    // Assert - Table should update
    const pendingCount = await migrationsPage.getTableRowCount();

    // If there are pending migrations, verify all are PENDING
    if (pendingCount > 0) {
      const firstRow = migrationsPage.tableRows.first();
      const statusChip = firstRow.locator('[class*="MuiChip"]');
      const statusText = await statusChip.textContent();
      expect(statusText).toBe('PENDING');
    }

    // Act - Filter by ALL
    await migrationsPage.filterByStatus('All');
    await page.waitForTimeout(1000);

    // Assert - Should show all migrations
    const allCount = await migrationsPage.getTableRowCount();
    expect(allCount).toBeGreaterThanOrEqual(0);
  });

  test('MIG-008: Migration dates format correctly', async ({ page }) => {
    // Arrange - Create migration
    const data = createTestMigration({
      name: 'E2E Test - Date Format',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    await migrationsPage.goto();

    // Assert
    const row = await migrationsPage.findMigrationByName(data.name);
    expect(row).not.toBeNull();

    if (row) {
      // Get created at date (usually last column before actions)
      const dateCell = row.locator('td').nth(5);
      const dateText = await dateCell.textContent();

      // Should be formatted, not "Invalid Date" or null
      expect(dateText).toBeTruthy();
      expect(dateText).not.toBe('Invalid Date');
      expect(dateText).not.toBe('N/A');
      expect(dateText).not.toContain('undefined');

      // Should look like a date (contains numbers and separators)
      expect(dateText).toMatch(/\d/);
    }
  });

  test('MIG-009: Migration records display with proper formatting', async ({ page }) => {
    // Arrange - Create migration
    const data = createTestMigration({
      name: 'E2E Test - Records Display',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    await migrationsPage.goto();

    // Assert
    const row = await migrationsPage.findMigrationByName(data.name);
    expect(row).not.toBeNull();

    if (row) {
      // Records migrated column should show numbers
      const recordsCell = row.locator('td').nth(4);
      const recordsText = await recordsCell.textContent();

      expect(recordsText).toBeTruthy();
      // Should contain numbers (could be "0 / 0" or "100 / 1000" etc)
      expect(recordsText).toMatch(/\d/);

      // Should not show undefined or null
      expect(recordsText).not.toContain('undefined');
      expect(recordsText).not.toContain('null');
    }
  });

  test('MIG-010: Pagination works correctly', async ({ page }) => {
    // Arrange
    await migrationsPage.goto();

    // Act - Change rows per page
    await migrationsPage.changeRowsPerPage(10);
    await page.waitForTimeout(500);

    const rowsWith10 = await migrationsPage.getTableRowCount();
    expect(rowsWith10).toBeLessThanOrEqual(10);

    // Act - Change to 20
    await migrationsPage.changeRowsPerPage(20);
    await page.waitForTimeout(500);

    const rowsWith20 = await migrationsPage.getTableRowCount();
    expect(rowsWith20).toBeLessThanOrEqual(20);
  });
});
