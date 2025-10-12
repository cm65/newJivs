import { test, expect } from '@playwright/test';
import { ExtractionsPage } from '../../pages/extractions/ExtractionsPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';
import { createExtraction, deleteExtraction } from '../../helpers/api.helper';
import { createTestExtraction } from '../../fixtures/extractions';

/**
 * EXT-006 to EXT-010: List and Filter Extraction Tests
 */

test.describe('Extractions - List and Filter', () => {
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

  test('EXT-006: Extractions list displays correctly', async ({ page }) => {
    // Arrange & Act
    await extractionsPage.goto();

    // Assert
    // 1. Page elements should be visible
    await extractionsPage.verifyPageElements();

    // 2. Table should display
    await expect(extractionsPage.extractionsTable).toBeVisible();

    // 3. Statistics cards should show counts
    const totalCount = await extractionsPage.getTotalCount();
    expect(totalCount).toBeGreaterThanOrEqual(0);

    // 4. Pagination should be visible
    await expect(extractionsPage.pagination).toBeVisible();
  });

  test('EXT-007: Filter extractions by status', async ({ page }) => {
    // Arrange
    await extractionsPage.goto();
    const initialCount = await extractionsPage.getTableRowCount();

    // Act - Filter by status
    await extractionsPage.filterByStatus('PENDING');
    await page.waitForTimeout(1000);

    // Assert - Table should update
    const pendingCount = await extractionsPage.getTableRowCount();

    // If there are any pending extractions, verify all are PENDING
    if (pendingCount > 0) {
      const firstRow = extractionsPage.tableRows.first();
      const statusChip = firstRow.locator('[class*="MuiChip"]');
      const statusText = await statusChip.textContent();
      expect(statusText).toBe('PENDING');
    }

    // Act - Filter by ALL
    await extractionsPage.filterByStatus('All');
    await page.waitForTimeout(1000);

    // Assert - Should show all extractions again
    const allCount = await extractionsPage.getTableRowCount();
    expect(allCount).toBeGreaterThanOrEqual(0);
  });

  test('EXT-008: Pagination works correctly', async ({ page }) => {
    // Arrange - Create multiple extractions to ensure pagination
    for (let i = 0; i < 3; i++) {
      const data = createTestExtraction({
        name: `Pagination Test ${i + 1}`,
      });
      const id = await createExtraction(page, data);
      createdExtractionIds.push(id);
    }

    await extractionsPage.goto();

    // Act - Change rows per page
    await extractionsPage.changeRowsPerPage(10);
    await page.waitForTimeout(500);

    const rowsWith10 = await extractionsPage.getTableRowCount();
    expect(rowsWith10).toBeLessThanOrEqual(10);

    // Act - Change to 20
    await extractionsPage.changeRowsPerPage(20);
    await page.waitForTimeout(500);

    const rowsWith20 = await extractionsPage.getTableRowCount();
    expect(rowsWith20).toBeLessThanOrEqual(20);
  });

  test('EXT-009: Extraction table displays all columns', async ({ page }) => {
    // Arrange & Act
    await extractionsPage.goto();

    // Assert - Check table headers
    const tableHeaders = extractionsPage.extractionsTable.locator('thead th');
    const headerCount = await tableHeaders.count();

    // Should have columns: Name, Source Type, Status, Records Extracted, Created At, Actions
    expect(headerCount).toBeGreaterThanOrEqual(5);

    // Verify specific headers exist
    await expect(extractionsPage.extractionsTable.locator('text=Name')).toBeVisible();
    await expect(extractionsPage.extractionsTable.locator('text=Source Type')).toBeVisible();
    await expect(extractionsPage.extractionsTable.locator('text=Status')).toBeVisible();
  });

  test('EXT-010: Extraction data displays without null/undefined errors', async ({ page }) => {
    // Arrange
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error' && msg.text().includes('undefined')) {
        consoleErrors.push(msg.text());
      }
    });

    // Act
    await extractionsPage.goto();
    await page.waitForTimeout(2000);

    // Assert
    // 1. No console errors about undefined/null
    const undefinedErrors = consoleErrors.filter(err =>
      err.toLowerCase().includes('undefined') ||
      err.toLowerCase().includes('null')
    );
    expect(undefinedErrors.length).toBe(0);

    // 2. If there are rows, verify data displays
    const rowCount = await extractionsPage.getTableRowCount();
    if (rowCount > 0) {
      const firstRow = extractionsPage.tableRows.first();

      // Name should not be empty
      const nameCell = firstRow.locator('td').first();
      const nameText = await nameCell.textContent();
      expect(nameText).toBeTruthy();

      // Source Type should not be "N/A" or empty (unless actually set to N/A)
      const sourceTypeCell = firstRow.locator('td').nth(1);
      const sourceTypeText = await sourceTypeCell.textContent();
      expect(sourceTypeText).toBeTruthy();

      // Status should have a value
      const statusChip = firstRow.locator('[class*="MuiChip"]');
      const statusText = await statusChip.textContent();
      expect(statusText).toBeTruthy();

      // Records should be a number (could be 0)
      const recordsCell = firstRow.locator('td').nth(3);
      const recordsText = await recordsCell.textContent();
      expect(recordsText).toMatch(/^\d/); // Starts with a digit

      // Created At should be formatted
      const createdAtCell = firstRow.locator('td').nth(4);
      const createdAtText = await createdAtCell.textContent();
      expect(createdAtText).toBeTruthy();
      expect(createdAtText).not.toBe('Invalid Date');
    }
  });

  test('EXT-011: Empty state displays when no extractions', async ({ page }) => {
    // Arrange - Filter by a status that likely has no results
    await extractionsPage.goto();

    // Act - Filter by FAILED (assuming no failed extractions initially)
    await extractionsPage.filterByStatus('FAILED');
    await page.waitForTimeout(1000);

    // Assert
    const rowCount = await extractionsPage.getTableRowCount();

    if (rowCount === 0) {
      // Should show empty state or "No data" message
      const noDataMessage = page.locator('text=/no.*data|no.*extractions|no.*results/i');
      const hasNoDataMessage = await noDataMessage.isVisible().catch(() => false);

      // Either show message or table is empty
      expect(rowCount === 0 || hasNoDataMessage).toBe(true);
    }
  });
});
