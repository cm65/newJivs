import { test, expect } from '@playwright/test';
import { ExtractionsPage } from '../../pages/extractions/ExtractionsPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';
import { createExtraction, deleteExtraction, apiPost } from '../../helpers/api.helper';
import { createTestExtraction } from '../../fixtures/extractions';

/**
 * EXT-BLK-001 to EXT-BLK-010: Bulk Operations Tests
 * Tests for bulk Start, Stop, Delete operations
 */

test.describe('Extractions - Bulk Operations', () => {
  let extractionsPage: ExtractionsPage;
  const createdExtractionIds: string[] = [];

  test.beforeEach(async ({ page }) => {
    await setupAuthenticatedSession(page, 'admin');
    extractionsPage = new ExtractionsPage(page);
  });

  test.afterEach(async ({ page }) => {
    // Cleanup
    for (const id of createdExtractionIds) {
      await deleteExtraction(page, id).catch(() => {});
    }
    createdExtractionIds.length = 0;
  });

  test('EXT-BLK-001: Bulk operations toolbar appears when selecting extractions', async ({ page }) => {
    // Arrange - Create multiple extractions
    const extraction1 = createTestExtraction({ name: 'Bulk Test 1' });
    const extraction2 = createTestExtraction({ name: 'Bulk Test 2' });
    const id1 = await createExtraction(page, extraction1);
    const id2 = await createExtraction(page, extraction2);
    createdExtractionIds.push(id1, id2);

    await extractionsPage.goto();

    // Toolbar should not be visible initially
    const visibleBefore = await extractionsPage.isBulkToolbarVisible();
    expect(visibleBefore).toBe(false);

    // Act - Select one extraction
    await extractionsPage.selectExtraction(extraction1.name);

    // Assert - Toolbar should appear
    const visibleAfter = await extractionsPage.isBulkToolbarVisible();
    expect(visibleAfter).toBe(true);

    // Selected count should be 1
    const count = await extractionsPage.getSelectedCount();
    expect(count).toBe(1);
  });

  test('EXT-BLK-002: Select all checkbox selects all visible extractions', async ({ page }) => {
    // Arrange - Create multiple extractions
    const extractions = [
      createTestExtraction({ name: 'Bulk SelectAll 1' }),
      createTestExtraction({ name: 'Bulk SelectAll 2' }),
      createTestExtraction({ name: 'Bulk SelectAll 3' }),
    ];

    for (const extraction of extractions) {
      const id = await createExtraction(page, extraction);
      createdExtractionIds.push(id);
    }

    await extractionsPage.goto();

    // Act - Click select all
    await extractionsPage.selectAll();

    // Assert - All should be selected
    const selectedCount = await extractionsPage.getSelectedCount();
    expect(selectedCount).toBe(3);
  });

  test('EXT-BLK-003: Bulk start multiple pending extractions', async ({ page }) => {
    // Arrange - Create 3 pending extractions
    const extractions = [
      createTestExtraction({ name: 'Bulk Start 1' }),
      createTestExtraction({ name: 'Bulk Start 2' }),
      createTestExtraction({ name: 'Bulk Start 3' }),
    ];

    for (const extraction of extractions) {
      const id = await createExtraction(page, extraction);
      createdExtractionIds.push(id);
    }

    await extractionsPage.goto();

    // Verify all are PENDING
    for (const extraction of extractions) {
      const status = await extractionsPage.getExtractionStatus(extraction.name);
      expect(status).toBe('PENDING');
    }

    // Act - Select all and bulk start
    await extractionsPage.selectAll();
    await extractionsPage.clickBulkStart();

    // Assert - All should be RUNNING
    await page.waitForTimeout(2000); // Wait for status updates
    for (const extraction of extractions) {
      const status = await extractionsPage.getExtractionStatus(extraction.name);
      expect(status).toBe('RUNNING');
    }
  });

  test('EXT-BLK-004: Bulk stop multiple running extractions', async ({ page }) => {
    // Arrange - Create and start 3 extractions
    const extractions = [
      createTestExtraction({ name: 'Bulk Stop 1' }),
      createTestExtraction({ name: 'Bulk Stop 2' }),
      createTestExtraction({ name: 'Bulk Stop 3' }),
    ];

    for (const extraction of extractions) {
      const id = await createExtraction(page, extraction);
      createdExtractionIds.push(id);
      // Start via API
      await apiPost(page, `/extractions/${id}/start`);
    }

    await extractionsPage.goto();

    // Verify all are RUNNING
    for (const extraction of extractions) {
      const status = await extractionsPage.getExtractionStatus(extraction.name);
      expect(status).toBe('RUNNING');
    }

    // Act - Select all and bulk stop
    await extractionsPage.selectAll();
    await extractionsPage.clickBulkStop();

    // Assert - All should be STOPPED
    await page.waitForTimeout(2000);
    for (const extraction of extractions) {
      const status = await extractionsPage.getExtractionStatus(extraction.name);
      expect(status).toBe('STOPPED');
    }
  });

  test('EXT-BLK-005: Bulk delete multiple extractions with confirmation', async ({ page }) => {
    // Arrange - Create 3 extractions
    const extractions = [
      createTestExtraction({ name: 'Bulk Delete 1' }),
      createTestExtraction({ name: 'Bulk Delete 2' }),
      createTestExtraction({ name: 'Bulk Delete 3' }),
    ];

    const ids: string[] = [];
    for (const extraction of extractions) {
      const id = await createExtraction(page, extraction);
      createdExtractionIds.push(id);
      ids.push(id);
    }

    await extractionsPage.goto();

    // Verify all exist
    for (const extraction of extractions) {
      const exists = await extractionsPage.extractionExists(extraction.name);
      expect(exists).toBe(true);
    }

    // Act - Select all and bulk delete
    await extractionsPage.selectAll();
    await extractionsPage.clickBulkDelete();

    // Assert - All should be deleted
    await page.waitForTimeout(2000);
    for (const extraction of extractions) {
      const exists = await extractionsPage.extractionExists(extraction.name);
      expect(exists).toBe(false);
    }

    // Remove from cleanup array
    createdExtractionIds.length = 0;
  });

  test('EXT-BLK-006: Select individual extractions (not select all)', async ({ page }) => {
    // Arrange - Create 4 extractions
    const extractions = [
      createTestExtraction({ name: 'Individual 1' }),
      createTestExtraction({ name: 'Individual 2' }),
      createTestExtraction({ name: 'Individual 3' }),
      createTestExtraction({ name: 'Individual 4' }),
    ];

    for (const extraction of extractions) {
      const id = await createExtraction(page, extraction);
      createdExtractionIds.push(id);
    }

    await extractionsPage.goto();

    // Act - Select only 2 of them
    await extractionsPage.selectExtraction(extractions[0].name);
    await extractionsPage.selectExtraction(extractions[2].name);

    // Assert - Selected count should be 2
    const count = await extractionsPage.getSelectedCount();
    expect(count).toBe(2);
  });

  test('EXT-BLK-007: Bulk operations only affect selected extractions', async ({ page }) => {
    // Arrange - Create 3 pending extractions
    const extractions = [
      createTestExtraction({ name: 'Selective 1' }),
      createTestExtraction({ name: 'Selective 2' }),
      createTestExtraction({ name: 'Selective 3' }),
    ];

    for (const extraction of extractions) {
      const id = await createExtraction(page, extraction);
      createdExtractionIds.push(id);
    }

    await extractionsPage.goto();

    // Act - Select only first 2 and start them
    await extractionsPage.selectExtraction(extractions[0].name);
    await extractionsPage.selectExtraction(extractions[1].name);
    await extractionsPage.clickBulkStart();

    // Assert - First 2 should be RUNNING, third should be PENDING
    await page.waitForTimeout(2000);
    const status1 = await extractionsPage.getExtractionStatus(extractions[0].name);
    const status2 = await extractionsPage.getExtractionStatus(extractions[1].name);
    const status3 = await extractionsPage.getExtractionStatus(extractions[2].name);

    expect(status1).toBe('RUNNING');
    expect(status2).toBe('RUNNING');
    expect(status3).toBe('PENDING');
  });

  test('EXT-BLK-008: Clear selection hides bulk operations toolbar', async ({ page }) => {
    // Arrange - Create extractions
    const extraction = createTestExtraction({ name: 'Clear Selection Test' });
    const id = await createExtraction(page, extraction);
    createdExtractionIds.push(id);

    await extractionsPage.goto();

    // Select extraction
    await extractionsPage.selectExtraction(extraction.name);

    // Toolbar should be visible
    const visibleBefore = await extractionsPage.isBulkToolbarVisible();
    expect(visibleBefore).toBe(true);

    // Act - Clear selection
    await extractionsPage.clearSelection();

    // Assert - Toolbar should be hidden
    await page.waitForTimeout(500);
    const visibleAfter = await extractionsPage.isBulkToolbarVisible();
    expect(visibleAfter).toBe(false);
  });

  test('EXT-BLK-009: Bulk operations toolbar shows correct action buttons', async ({ page }) => {
    // Arrange
    const extraction = createTestExtraction({ name: 'Toolbar Actions Test' });
    const id = await createExtraction(page, extraction);
    createdExtractionIds.push(id);

    await extractionsPage.goto();
    await extractionsPage.selectExtraction(extraction.name);

    // Assert - Toolbar should have Start, Stop, Delete buttons
    const hasStart = await extractionsPage.bulkStartButton.isVisible();
    const hasStop = await extractionsPage.bulkStopButton.isVisible();
    const hasDelete = await extractionsPage.bulkDeleteButton.isVisible();

    expect(hasStart).toBe(true);
    expect(hasStop).toBe(true);
    expect(hasDelete).toBe(true);
  });

  test('EXT-BLK-010: Bulk delete shows confirmation dialog', async ({ page }) => {
    // Arrange
    const extraction = createTestExtraction({ name: 'Delete Confirm Test' });
    const id = await createExtraction(page, extraction);
    createdExtractionIds.push(id);

    await extractionsPage.goto();
    await extractionsPage.selectExtraction(extraction.name);

    // Act - Click bulk delete
    await extractionsPage.bulkDeleteButton.click();

    // Assert - Confirmation dialog should appear
    const confirmDialog = page.locator('[role="dialog"]:has-text("sure")');
    await expect(confirmDialog).toBeVisible({ timeout: 5000 });

    // Verify message mentions multiple extractions
    const dialogText = await confirmDialog.textContent();
    expect(dialogText).toContain('cannot be undone');

    // Cancel to avoid deletion
    const cancelButton = confirmDialog.locator('button:has-text("Cancel")');
    await cancelButton.click();

    // Verify extraction still exists
    const exists = await extractionsPage.extractionExists(extraction.name);
    expect(exists).toBe(true);
  });
});
