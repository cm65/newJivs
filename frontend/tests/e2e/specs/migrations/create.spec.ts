import { test, expect } from '@playwright/test';
import { MigrationsPage } from '../../pages/migrations/MigrationsPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';
import { createTestMigration } from '../../fixtures/migrations';
import { deleteMigration } from '../../helpers/api.helper';

/**
 * MIG-001 to MIG-003: Create Migration Tests
 */

test.describe('Migrations - Create', () => {
  let migrationsPage: MigrationsPage;
  const createdMigrationIds: string[] = [];

  test.beforeEach(async ({ page }) => {
    await setupAuthenticatedSession(page, 'admin');
    migrationsPage = new MigrationsPage(page);
    await migrationsPage.goto();
  });

  test.afterEach(async ({ page }) => {
    // Cleanup
    for (const id of createdMigrationIds) {
      await deleteMigration(page, id).catch(() => {});
    }
    createdMigrationIds.length = 0;
  });

  test('MIG-001: Create migration with valid data', async ({ page }) => {
    // Arrange
    const migrationData = createTestMigration({
      name: 'E2E Test Migration - Valid',
    });

    const initialCount = await migrationsPage.getTotalCount();

    // Act
    await migrationsPage.clickNewMigration();
    await migrationsPage.fillCreateForm({ name: migrationData.name });
    await migrationsPage.submitCreateForm();

    // Assert
    // 1. Dialog should close
    await expect(migrationsPage.createDialog).not.toBeVisible();

    // 2. New migration should appear in table
    await page.waitForTimeout(1000);
    const exists = await migrationsPage.migrationExists(migrationData.name);
    expect(exists).toBe(true);

    // 3. Count should increase
    const newCount = await migrationsPage.getTotalCount();
    expect(newCount).toBeGreaterThan(initialCount);
  });

  test('MIG-002: Create migration dialog cancellation', async ({ page }) => {
    // Arrange
    const initialCount = await migrationsPage.getTotalCount();
    const migrationData = createTestMigration({
      name: 'E2E Test - Cancelled Migration',
    });

    // Act
    await migrationsPage.clickNewMigration();
    await migrationsPage.fillCreateForm({ name: migrationData.name });
    await migrationsPage.cancelCreateDialog();

    // Assert
    // 1. Dialog should close
    await expect(migrationsPage.createDialog).not.toBeVisible();

    // 2. Migration should NOT be created
    await page.waitForTimeout(500);
    const exists = await migrationsPage.migrationExists(migrationData.name);
    expect(exists).toBe(false);

    // 3. Count should remain same
    const newCount = await migrationsPage.getTotalCount();
    expect(newCount).toBe(initialCount);
  });

  test('MIG-003: Create migration and verify in API', async ({ page }) => {
    // Arrange
    const migrationData = createTestMigration({
      name: 'E2E Test - API Verification',
    });

    // Act
    await migrationsPage.createMigration(migrationData.name);

    // Assert - Verify via API
    await page.waitForTimeout(1000);

    const { apiGet } = await import('../../helpers/api.helper');
    const response = await apiGet(page, '/migrations?size=100');
    expect(response.ok()).toBe(true);

    const data = await response.json();
    const createdMigration = data.data.content.find(
      (mig: any) => mig.name === migrationData.name
    );

    expect(createdMigration).toBeDefined();
    expect(createdMigration.status).toBe('PENDING');
    expect(createdMigration.progress).toBe(0);

    // Save ID for cleanup
    if (createdMigration?.id) {
      createdMigrationIds.push(createdMigration.id);
    }
  });
});
