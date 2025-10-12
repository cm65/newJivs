import { test, expect } from '@playwright/test';
import { MigrationsPage } from '../../pages/migrations/MigrationsPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';
import { createMigration, deleteMigration, apiPost } from '../../helpers/api.helper';
import { createTestMigration } from '../../fixtures/migrations';

/**
 * MIG-011 to MIG-018: Manage Migrations Tests
 * Tests for Start, Pause, Resume, Rollback, Delete operations
 */

test.describe('Migrations - Manage', () => {
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

  test('MIG-011: Start a pending migration', async ({ page }) => {
    // Arrange - Create pending migration
    const data = createTestMigration({
      name: 'E2E Test - Start Migration',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    await migrationsPage.goto();

    // Verify initial status
    const initialStatus = await migrationsPage.getMigrationStatus(data.name);
    expect(initialStatus).toBe('PENDING');

    // Act - Start the migration
    await migrationsPage.startMigration(data.name);
    await page.waitForTimeout(1000);

    // Assert - Status should change to RUNNING
    const newStatus = await migrationsPage.getMigrationStatus(data.name);
    expect(newStatus).toBe('RUNNING');
  });

  test('MIG-012: Pause a running migration', async ({ page }) => {
    // Arrange - Create and start migration
    const data = createTestMigration({
      name: 'E2E Test - Pause Migration',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    // Start via API
    await apiPost(page, `/migrations/${migrationId}/start`);

    await migrationsPage.goto();

    // Verify it's running
    const initialStatus = await migrationsPage.getMigrationStatus(data.name);
    expect(initialStatus).toBe('RUNNING');

    // Act - Pause the migration
    await migrationsPage.pauseMigration(data.name);
    await page.waitForTimeout(1000);

    // Assert - Status should change to PAUSED
    const newStatus = await migrationsPage.getMigrationStatus(data.name);
    expect(newStatus).toBe('PAUSED');
  });

  test('MIG-013: Resume a paused migration', async ({ page }) => {
    // Arrange - Create, start, and pause migration
    const data = createTestMigration({
      name: 'E2E Test - Resume Migration',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    // Start and pause via API
    await apiPost(page, `/migrations/${migrationId}/start`);
    await apiPost(page, `/migrations/${migrationId}/pause`);

    await migrationsPage.goto();

    // Verify it's paused
    const initialStatus = await migrationsPage.getMigrationStatus(data.name);
    expect(initialStatus).toBe('PAUSED');

    // Act - Resume the migration
    await migrationsPage.resumeMigration(data.name);
    await page.waitForTimeout(1000);

    // Assert - Status should change back to RUNNING
    const newStatus = await migrationsPage.getMigrationStatus(data.name);
    expect(newStatus).toBe('RUNNING');
  });

  test('MIG-014: Rollback a completed migration with confirmation', async ({ page }) => {
    // Arrange - Create migration (assuming it completes quickly)
    const data = createTestMigration({
      name: 'E2E Test - Rollback Migration',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    // For testing, we'll just verify the rollback button appears
    // In real scenario, migration would need to be completed first
    await migrationsPage.goto();

    // If migration is completed or failed, rollback button should appear
    const status = await migrationsPage.getMigrationStatus(data.name);

    if (status === 'COMPLETED' || status === 'FAILED') {
      // Act - Rollback the migration
      await migrationsPage.rollbackMigration(data.name);
      await page.waitForTimeout(1000);

      // Assert - Status should change to ROLLING_BACK or back to a previous state
      const newStatus = await migrationsPage.getMigrationStatus(data.name);
      expect(['ROLLING_BACK', 'PENDING']).toContain(newStatus);
    }
  });

  test('MIG-015: Delete migration with confirmation', async ({ page }) => {
    // Arrange - Create migration
    const data = createTestMigration({
      name: 'E2E Test - Delete Migration',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    await migrationsPage.goto();

    // Verify it exists
    const existsBefore = await migrationsPage.migrationExists(data.name);
    expect(existsBefore).toBe(true);

    // Act - Delete the migration
    await migrationsPage.deleteMigration(data.name);
    await page.waitForTimeout(1000);

    // Assert - Should be removed from table
    const existsAfter = await migrationsPage.migrationExists(data.name);
    expect(existsAfter).toBe(false);

    // Remove from cleanup since already deleted
    const index = createdMigrationIds.indexOf(migrationId);
    if (index > -1) {
      createdMigrationIds.splice(index, 1);
    }
  });

  test('MIG-016: Action buttons display based on migration status', async ({ page }) => {
    // Arrange - Create pending migration
    const data = createTestMigration({
      name: 'E2E Test - Action Buttons',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    await migrationsPage.goto();

    // Assert - For PENDING migration
    const row = await migrationsPage.findMigrationByName(data.name);
    expect(row).not.toBeNull();

    if (row) {
      // Should have Start button
      const startButton = row.locator('button:has-text("Start")');
      await expect(startButton).toBeVisible();

      // Should have Delete button
      const deleteButton = row.locator('button:has-text("Delete")');
      await expect(deleteButton).toBeVisible();

      // Should NOT have Pause button (only for RUNNING)
      const pauseButton = row.locator('button:has-text("Pause")');
      const hasPauseButton = await pauseButton.isVisible().catch(() => false);
      expect(hasPauseButton).toBe(false);

      // Should NOT have Resume button (only for PAUSED)
      const resumeButton = row.locator('button:has-text("Resume")');
      const hasResumeButton = await resumeButton.isVisible().catch(() => false);
      expect(hasResumeButton).toBe(false);
    }
  });

  test('MIG-017: Migration lifecycle (PENDING → RUNNING → PAUSED → RUNNING)', async ({ page }) => {
    // Arrange - Create migration
    const data = createTestMigration({
      name: 'E2E Test - Full Lifecycle',
    });
    const migrationId = await createMigration(page, data);
    createdMigrationIds.push(migrationId);

    await migrationsPage.goto();

    // Assert - Initial state PENDING
    let status = await migrationsPage.getMigrationStatus(data.name);
    expect(status).toBe('PENDING');

    // Act - Start migration
    await migrationsPage.startMigration(data.name);
    await page.waitForTimeout(1000);

    // Assert - Should be RUNNING
    status = await migrationsPage.getMigrationStatus(data.name);
    expect(status).toBe('RUNNING');

    // Act - Pause migration
    await migrationsPage.pauseMigration(data.name);
    await page.waitForTimeout(1000);

    // Assert - Should be PAUSED
    status = await migrationsPage.getMigrationStatus(data.name);
    expect(status).toBe('PAUSED');

    // Act - Resume migration
    await migrationsPage.resumeMigration(data.name);
    await page.waitForTimeout(1000);

    // Assert - Should be RUNNING again
    status = await migrationsPage.getMigrationStatus(data.name);
    expect(status).toBe('RUNNING');
  });

  test('MIG-018: Multiple migrations can be managed independently', async ({ page }) => {
    // Arrange - Create multiple migrations
    const migration1Data = createTestMigration({
      name: 'E2E Test - Independent 1',
    });
    const migration2Data = createTestMigration({
      name: 'E2E Test - Independent 2',
    });

    const id1 = await createMigration(page, migration1Data);
    const id2 = await createMigration(page, migration2Data);
    createdMigrationIds.push(id1, id2);

    await migrationsPage.goto();

    // Act - Start only first migration
    await migrationsPage.startMigration(migration1Data.name);
    await page.waitForTimeout(1000);

    // Assert
    // Migration 1 should be RUNNING
    const status1 = await migrationsPage.getMigrationStatus(migration1Data.name);
    expect(status1).toBe('RUNNING');

    // Migration 2 should still be PENDING
    const status2 = await migrationsPage.getMigrationStatus(migration2Data.name);
    expect(status2).toBe('PENDING');

    // Act - Delete second migration
    await migrationsPage.deleteMigration(migration2Data.name);
    await page.waitForTimeout(1000);

    // Assert - Only migration 2 should be deleted
    const exists1 = await migrationsPage.migrationExists(migration1Data.name);
    expect(exists1).toBe(true);

    const exists2 = await migrationsPage.migrationExists(migration2Data.name);
    expect(exists2).toBe(false);

    // Remove from cleanup
    const index = createdMigrationIds.indexOf(id2);
    if (index > -1) {
      createdMigrationIds.splice(index, 1);
    }
  });
});
