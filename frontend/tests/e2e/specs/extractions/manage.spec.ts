import { test, expect } from '@playwright/test';
import { ExtractionsPage } from '../../pages/extractions/ExtractionsPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';
import { createExtraction, deleteExtraction, apiPost } from '../../helpers/api.helper';
import { createTestExtraction } from '../../fixtures/extractions';

/**
 * EXT-012 to EXT-016: Manage Extractions Tests
 * Tests for Start, Stop, Delete operations
 */

test.describe('Extractions - Manage', () => {
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

  test('EXT-012: Start a pending extraction', async ({ page }) => {
    // Arrange - Create a pending extraction
    const data = createTestExtraction({
      name: 'E2E Test - Start Extraction',
    });
    const extractionId = await createExtraction(page, data);
    createdExtractionIds.push(extractionId);

    await extractionsPage.goto();

    // Verify initial status
    const initialStatus = await extractionsPage.getExtractionStatus(data.name);
    expect(initialStatus).toBe('PENDING');

    // Act - Start the extraction
    await extractionsPage.startExtraction(data.name);
    await page.waitForTimeout(1000);

    // Assert - Status should change to RUNNING
    const newStatus = await extractionsPage.getExtractionStatus(data.name);
    expect(newStatus).toBe('RUNNING');
  });

  test('EXT-013: Stop a running extraction', async ({ page }) => {
    // Arrange - Create and start an extraction
    const data = createTestExtraction({
      name: 'E2E Test - Stop Extraction',
    });
    const extractionId = await createExtraction(page, data);
    createdExtractionIds.push(extractionId);

    // Start via API
    await apiPost(page, `/extractions/${extractionId}/start`);

    await extractionsPage.goto();

    // Verify it's running
    const initialStatus = await extractionsPage.getExtractionStatus(data.name);
    expect(initialStatus).toBe('RUNNING');

    // Act - Stop the extraction
    await extractionsPage.stopExtraction(data.name);
    await page.waitForTimeout(1000);

    // Assert - Status should change to STOPPED
    const newStatus = await extractionsPage.getExtractionStatus(data.name);
    expect(newStatus).toBe('STOPPED');
  });

  test('EXT-014: Delete extraction with confirmation', async ({ page }) => {
    // Arrange - Create an extraction
    const data = createTestExtraction({
      name: 'E2E Test - Delete Extraction',
    });
    const extractionId = await createExtraction(page, data);
    createdExtractionIds.push(extractionId);

    await extractionsPage.goto();

    // Verify it exists
    const existsBefore = await extractionsPage.extractionExists(data.name);
    expect(existsBefore).toBe(true);

    // Act - Delete the extraction
    await extractionsPage.deleteExtraction(data.name);
    await page.waitForTimeout(1000);

    // Assert - Should be removed from table
    const existsAfter = await extractionsPage.extractionExists(data.name);
    expect(existsAfter).toBe(false);

    // Remove from cleanup array since it's already deleted
    const index = createdExtractionIds.indexOf(extractionId);
    if (index > -1) {
      createdExtractionIds.splice(index, 1);
    }
  });

  test('EXT-015: Action buttons display based on status', async ({ page }) => {
    // Arrange - Create pending extraction
    const data = createTestExtraction({
      name: 'E2E Test - Action Buttons',
    });
    const extractionId = await createExtraction(page, data);
    createdExtractionIds.push(extractionId);

    await extractionsPage.goto();

    // Assert - For PENDING extraction
    const row = await extractionsPage.findExtractionByName(data.name);
    expect(row).not.toBeNull();

    if (row) {
      // Should have Start button
      const startButton = row.locator('button:has-text("Start")');
      await expect(startButton).toBeVisible();

      // Should have Delete button
      const deleteButton = row.locator('button:has-text("Delete")');
      await expect(deleteButton).toBeVisible();

      // Should NOT have Stop button
      const stopButton = row.locator('button:has-text("Stop")');
      const hasStopButton = await stopButton.isVisible().catch(() => false);
      expect(hasStopButton).toBe(false);
    }
  });

  test('EXT-016: Multiple extractions can be managed independently', async ({ page }) => {
    // Arrange - Create multiple extractions
    const extraction1Data = createTestExtraction({
      name: 'E2E Test - Multi 1',
    });
    const extraction2Data = createTestExtraction({
      name: 'E2E Test - Multi 2',
    });

    const id1 = await createExtraction(page, extraction1Data);
    const id2 = await createExtraction(page, extraction2Data);
    createdExtractionIds.push(id1, id2);

    await extractionsPage.goto();

    // Act - Start first extraction only
    await extractionsPage.startExtraction(extraction1Data.name);
    await page.waitForTimeout(1000);

    // Assert
    // Extraction 1 should be RUNNING
    const status1 = await extractionsPage.getExtractionStatus(extraction1Data.name);
    expect(status1).toBe('RUNNING');

    // Extraction 2 should still be PENDING
    const status2 = await extractionsPage.getExtractionStatus(extraction2Data.name);
    expect(status2).toBe('PENDING');

    // Act - Delete second extraction
    await extractionsPage.deleteExtraction(extraction2Data.name);
    await page.waitForTimeout(1000);

    // Assert - Only extraction 2 should be deleted
    const exists1 = await extractionsPage.extractionExists(extraction1Data.name);
    expect(exists1).toBe(true);

    const exists2 = await extractionsPage.extractionExists(extraction2Data.name);
    expect(exists2).toBe(false);

    // Remove deleted from cleanup
    const index = createdExtractionIds.indexOf(id2);
    if (index > -1) {
      createdExtractionIds.splice(index, 1);
    }
  });

  test('EXT-017: Error handling when operation fails', async ({ page }) => {
    // Arrange - Create extraction
    const data = createTestExtraction({
      name: 'E2E Test - Error Handling',
    });
    const extractionId = await createExtraction(page, data);
    createdExtractionIds.push(extractionId);

    // Delete via API to create inconsistent state
    await deleteExtraction(page, extractionId);

    await extractionsPage.goto();

    // Act - Try to perform operation on deleted extraction
    // This should show an error (404 or similar)
    await page.waitForTimeout(500);

    // The extraction may or may not appear in UI depending on cache
    // If it appears and we try to interact, should get error
    const exists = await extractionsPage.extractionExists(data.name);

    if (exists) {
      // Try to start (should fail)
      await extractionsPage.startExtraction(data.name).catch(() => {
        // Expected to fail
      });

      // Check if error message appears
      await page.waitForTimeout(1000);
      const hasError = await extractionsPage.hasError();

      // Should show error or extraction disappears from list
      if (!hasError) {
        const stillExists = await extractionsPage.extractionExists(data.name);
        expect(stillExists).toBe(false);
      }
    }

    // Remove from cleanup since already deleted
    const index = createdExtractionIds.indexOf(extractionId);
    if (index > -1) {
      createdExtractionIds.splice(index, 1);
    }
  });
});
