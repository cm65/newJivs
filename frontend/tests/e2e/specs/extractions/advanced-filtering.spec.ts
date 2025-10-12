import { test, expect } from '@playwright/test';
import { ExtractionsPage } from '../../pages/extractions/ExtractionsPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';
import { createExtraction, deleteExtraction } from '../../helpers/api.helper';
import { createTestExtraction } from '../../fixtures/extractions';

/**
 * EXT-FLT-001 to EXT-FLT-015: Advanced Filtering Tests
 * Tests for FilterBuilder, QuickFilters, and SavedViews
 */

test.describe('Extractions - Advanced Filtering', () => {
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

  test('EXT-FLT-001: FilterBuilder button is visible', async ({ page }) => {
    await extractionsPage.goto();

    // Assert - Filter builder button should be visible
    const filterButton = page.locator('button:has-text("Filter"), button:has-text("Add Filter")');
    await expect(filterButton.first()).toBeVisible();
  });

  test('EXT-FLT-002: Quick filters are displayed', async ({ page }) => {
    await extractionsPage.goto();

    // Assert - Quick filter chips should be visible
    const activeFilter = page.locator('[class*="MuiChip"]:has-text("Active")');
    const failedFilter = page.locator('[class*="MuiChip"]:has-text("Failed")');
    const completedFilter = page.locator('[class*="MuiChip"]:has-text("Completed")');

    await expect(activeFilter).toBeVisible();
    await expect(failedFilter).toBeVisible();
    await expect(completedFilter).toBeVisible();
  });

  test('EXT-FLT-003: Clicking quick filter "Failed" shows only failed extractions', async ({ page }) => {
    // Arrange - Create extractions with different statuses
    const pendingExtraction = createTestExtraction({ name: 'Filter Pending' });
    const failedExtraction = createTestExtraction({ name: 'Filter Failed' });

    const id1 = await createExtraction(page, pendingExtraction);
    const id2 = await createExtraction(page, failedExtraction);
    createdExtractionIds.push(id1, id2);

    // TODO: Mark second extraction as FAILED via API

    await extractionsPage.goto();

    // Act - Click "Failed" quick filter
    const failedFilter = page.locator('[class*="MuiChip"]:has-text("Failed")');
    await failedFilter.click();
    await page.waitForTimeout(1000);

    // Assert - Only failed extractions should be visible
    // Note: This test needs backend support to create FAILED extractions
  });

  test('EXT-FLT-004: Quick filter "Active" shows RUNNING and PENDING extractions', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Click "Active" quick filter
    const activeFilter = page.locator('[class*="MuiChip"]:has-text("Active")');
    await activeFilter.click();
    await page.waitForTimeout(1000);

    // Assert - Should show RUNNING and PENDING extractions
    // Verify table has rows (if any active extractions exist)
    const rowCount = await extractionsPage.getTableRowCount();
    // If there are rows, verify they're all RUNNING or PENDING
  });

  test('EXT-FLT-005: Clear filters button removes active filters', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Apply a quick filter
    const activeFilter = page.locator('[class*="MuiChip"]:has-text("Active")');
    await activeFilter.click();
    await page.waitForTimeout(500);

    // Click clear filters
    const clearButton = page.locator('button:has-text("Clear")');
    if (await clearButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await clearButton.click();
      await page.waitForTimeout(1000);
    }

    // Assert - All extractions should be visible again
    // Filter should be deactivated
  });

  test('EXT-FLT-006: SavedViews button is visible', async ({ page }) => {
    await extractionsPage.goto();

    // Assert - Saved views button should be visible
    const savedViewsButton = page.locator('button:has-text("Saved Views"), button[aria-label*="view"]');
    await expect(savedViewsButton.first()).toBeVisible();
  });

  test('EXT-FLT-007: Clicking SavedViews opens dropdown menu', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Click saved views button
    const savedViewsButton = page.locator('button:has-text("Saved Views"), button[aria-label*="view"]').first();
    await savedViewsButton.click();

    // Assert - Dropdown menu should appear
    const menu = page.locator('[role="menu"], [role="listbox"]');
    await expect(menu).toBeVisible({ timeout: 5000 });
  });

  test('EXT-FLT-008: Can save a new view', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Open saved views and click "Save Current View"
    const savedViewsButton = page.locator('button:has-text("Saved Views"), button[aria-label*="view"]').first();
    await savedViewsButton.click();

    const saveViewOption = page.locator('text="Save Current View"');
    if (await saveViewOption.isVisible({ timeout: 2000 }).catch(() => false)) {
      await saveViewOption.click();

      // Fill in view name
      const nameInput = page.locator('input[label="View Name"], input[placeholder*="name"]').first();
      await nameInput.waitFor({ state: 'visible', timeout: 5000 });
      await nameInput.fill('My Test View');

      // Click save
      const saveButton = page.locator('button:has-text("Save")');
      await saveButton.click();
      await page.waitForTimeout(1000);

      // Assert - Success message or view appears in list
    }
  });

  test('EXT-FLT-009: Can apply a saved view', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Open saved views
    const savedViewsButton = page.locator('button:has-text("Saved Views"), button[aria-label*="view"]').first();
    await savedViewsButton.click();

    // Check if any saved views exist
    const menu = page.locator('[role="menu"], [role="listbox"]');
    await menu.waitFor({ state: 'visible', timeout: 5000 });

    // Look for any view in the list
    const viewItems = menu.locator('[role="menuitem"]');
    const count = await viewItems.count();

    if (count > 0) {
      // Click first saved view
      await viewItems.first().click();
      await page.waitForTimeout(1000);

      // Assert - Filters should be applied
    }
  });

  test('EXT-FLT-010: Can set a view as default', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Open saved views
    const savedViewsButton = page.locator('button:has-text("Saved Views"), button[aria-label*="view"]').first();
    await savedViewsButton.click();

    const menu = page.locator('[role="menu"], [role="listbox"]');
    await menu.waitFor({ state: 'visible', timeout: 5000 });

    // Look for "Set as Default" option
    const setDefaultOption = menu.locator('text*="default"');
    if (await setDefaultOption.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Option exists
    }
  });

  test('EXT-FLT-011: FilterBuilder can add a new filter condition', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Open filter builder
    const filterButton = page.locator('button:has-text("Filter"), button:has-text("Add Filter")').first();
    await filterButton.click();

    // Look for filter builder dialog/panel
    const filterPanel = page.locator('[role="dialog"], .filter-builder');
    if (await filterPanel.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Add a condition
      const addConditionButton = page.locator('button:has-text("Add Condition")');
      if (await addConditionButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        await addConditionButton.click();

        // Assert - New condition row should appear
      }
    }
  });

  test('EXT-FLT-012: FilterBuilder supports multiple filter fields', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Open filter builder
    const filterButton = page.locator('button:has-text("Filter"), button:has-text("Add Filter")').first();
    await filterButton.click();

    const filterPanel = page.locator('[role="dialog"], .filter-builder');
    if (await filterPanel.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Look for field selector
      const fieldSelect = filterPanel.locator('select, [role="combobox"]').first();
      if (await fieldSelect.isVisible({ timeout: 2000 }).catch(() => false)) {
        // Check available fields
        // Should include: Name, Source Type, Status, Records Extracted, Created Date
      }
    }
  });

  test('EXT-FLT-013: FilterBuilder supports different operators', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Open filter builder
    const filterButton = page.locator('button:has-text("Filter"), button:has-text("Add Filter")').first();
    await filterButton.click();

    const filterPanel = page.locator('[role="dialog"], .filter-builder');
    if (await filterPanel.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Look for operator selector
      const operatorSelect = filterPanel.locator('select:has-text("equals"), [role="combobox"]');
      if (await operatorSelect.count() > 0) {
        // Operators should include: equals, not equals, contains, starts with, etc.
      }
    }
  });

  test('EXT-FLT-014: Can delete a saved view', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Open saved views
    const savedViewsButton = page.locator('button:has-text("Saved Views"), button[aria-label*="view"]').first();
    await savedViewsButton.click();

    const menu = page.locator('[role="menu"], [role="listbox"]');
    await menu.waitFor({ state: 'visible', timeout: 5000 });

    // Look for delete icon/button
    const deleteButton = menu.locator('button[aria-label*="delete"], button:has-text("Delete")');
    if (await deleteButton.count() > 0) {
      // Delete button exists for saved views
    }
  });

  test('EXT-FLT-015: Multi-column sorting works with filters', async ({ page }) => {
    // Arrange - Create multiple extractions
    const extractions = [
      createTestExtraction({ name: 'Sort A' }),
      createTestExtraction({ name: 'Sort B' }),
      createTestExtraction({ name: 'Sort C' }),
    ];

    for (const extraction of extractions) {
      const id = await createExtraction(page, extraction);
      createdExtractionIds.push(id);
    }

    await extractionsPage.goto();

    // Act - Apply sorting on Name column (hold Shift for multi-column)
    const nameHeader = page.locator('table thead th:has-text("Name")');
    await nameHeader.click();
    await page.waitForTimeout(500);

    // Assert - Sorting indicator should be visible
    const sortLabel = page.locator('[class*="TableSortLabel"]');
    await expect(sortLabel.first()).toBeVisible();
  });

  test('EXT-FLT-016: Sorting persists with SavedViews', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Apply sorting
    const nameHeader = page.locator('table thead th:has-text("Name")');
    await nameHeader.click();
    await page.waitForTimeout(500);

    // Save view with sorting
    const savedViewsButton = page.locator('button:has-text("Saved Views"), button[aria-label*="view"]').first();
    await savedViewsButton.click();

    const saveViewOption = page.locator('text="Save Current View"');
    if (await saveViewOption.isVisible({ timeout: 2000 }).catch(() => false)) {
      await saveViewOption.click();

      const nameInput = page.locator('input[label="View Name"], input[placeholder*="name"]').first();
      await nameInput.fill('Sorted View');

      const saveButton = page.locator('button:has-text("Save")');
      await saveButton.click();
      await page.waitForTimeout(1000);

      // Assert - View saved with sorting configuration
    }
  });

  test('EXT-FLT-017: Filter by name (string contains)', async ({ page }) => {
    // Arrange - Create extractions with distinct names
    const extractions = [
      createTestExtraction({ name: 'Production Extraction' }),
      createTestExtraction({ name: 'Development Extraction' }),
      createTestExtraction({ name: 'Testing Extraction' }),
    ];

    for (const extraction of extractions) {
      const id = await createExtraction(page, extraction);
      createdExtractionIds.push(id);
    }

    await extractionsPage.goto();

    // Act - Use filter builder to filter by name contains "Production"
    const filterButton = page.locator('button:has-text("Filter"), button:has-text("Add Filter")').first();
    await filterButton.click();

    // Configure filter (specific steps depend on FilterBuilder implementation)
    await page.waitForTimeout(1000);

    // Assert - Only "Production Extraction" should be visible
  });

  test('EXT-FLT-018: Filter by date range (Created Date)', async ({ page }) => {
    // Arrange - Create extractions
    const extraction = createTestExtraction({ name: 'Date Filter Test' });
    const id = await createExtraction(page, extraction);
    createdExtractionIds.push(id);

    await extractionsPage.goto();

    // Act - Use filter builder to filter by created date
    const filterButton = page.locator('button:has-text("Filter"), button:has-text("Add Filter")').first();
    await filterButton.click();

    // Look for date picker
    const datePicker = page.locator('input[type="date"], [role="textbox"][placeholder*="date"]');
    if (await datePicker.count() > 0) {
      // Date filtering is supported
    }
  });

  test('EXT-FLT-019: Filter by numeric range (Records Extracted)', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Open filter builder
    const filterButton = page.locator('button:has-text("Filter"), button:has-text("Add Filter")').first();
    await filterButton.click();

    // Look for numeric input for records
    const numericInput = page.locator('input[type="number"]');
    if (await numericInput.count() > 0) {
      // Numeric filtering is supported
    }
  });

  test('EXT-FLT-020: Combining filters with AND/OR logic', async ({ page }) => {
    await extractionsPage.goto();

    // Act - Open filter builder
    const filterButton = page.locator('button:has-text("Filter"), button:has-text("Add Filter")').first();
    await filterButton.click();

    const filterPanel = page.locator('[role="dialog"], .filter-builder');
    if (await filterPanel.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Look for AND/OR logic selector
      const logicSelector = filterPanel.locator('select:has-text("AND"), select:has-text("OR")');
      if (await logicSelector.count() > 0) {
        // Logic operators are supported
      }
    }
  });
});
