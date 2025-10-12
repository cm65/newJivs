import { test, expect } from '@playwright/test';
import { ExtractionsPage } from '../../pages/extractions/ExtractionsPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';
import { createTestExtraction } from '../../fixtures/extractions';
import { deleteExtraction } from '../../helpers/api.helper';

/**
 * EXT-001: Create Extraction Tests
 * Tests for creating new extractions
 */

test.describe('Extractions - Create', () => {
  let extractionsPage: ExtractionsPage;
  const createdExtractionIds: string[] = [];

  test.beforeEach(async ({ page }) => {
    await setupAuthenticatedSession(page, 'admin');
    extractionsPage = new ExtractionsPage(page);
    await extractionsPage.goto();
  });

  test.afterEach(async ({ page }) => {
    // Cleanup created extractions
    for (const id of createdExtractionIds) {
      await deleteExtraction(page, id).catch(() => {});
    }
    createdExtractionIds.length = 0;
  });

  test('EXT-001: Create extraction with valid data', async ({ page }) => {
    // Arrange
    const extractionData = createTestExtraction({
      name: 'E2E Test Extraction - Valid',
      sourceType: 'JDBC',
      query: 'SELECT * FROM customers LIMIT 100',
    });

    const initialCount = await extractionsPage.getTotalCount();

    // Act
    await extractionsPage.clickNewExtraction();
    await extractionsPage.fillCreateForm({
      name: extractionData.name,
      sourceType: extractionData.sourceType,
      query: extractionData.extractionQuery,
    });
    await extractionsPage.submitCreateForm();

    // Assert
    // 1. Dialog should close
    await expect(extractionsPage.createDialog).not.toBeVisible();

    // 2. Success message may appear
    await page.waitForTimeout(1000);

    // 3. New extraction should appear in table
    const exists = await extractionsPage.extractionExists(extractionData.name);
    expect(exists).toBe(true);

    // 4. Count should increase
    const newCount = await extractionsPage.getTotalCount();
    expect(newCount).toBeGreaterThan(initialCount);
  });

  test('EXT-002: Create extraction with different source types', async ({ page }) => {
    const sourceTypes = ['JDBC', 'SAP', 'FILE', 'API'];

    for (const sourceType of sourceTypes) {
      const extractionData = createTestExtraction({
        name: `E2E Test - ${sourceType} Extraction`,
        sourceType: sourceType as any,
      });

      await extractionsPage.clickNewExtraction();
      await extractionsPage.fillCreateForm({
        name: extractionData.name,
        sourceType: extractionData.sourceType,
        query: extractionData.extractionQuery,
      });
      await extractionsPage.submitCreateForm();

      // Verify created
      await page.waitForTimeout(500);
      const exists = await extractionsPage.extractionExists(extractionData.name);
      expect(exists).toBe(true);
    }
  });

  test('EXT-003: Create extraction dialog cancellation', async ({ page }) => {
    // Arrange
    const initialCount = await extractionsPage.getTotalCount();
    const extractionData = createTestExtraction();

    // Act
    await extractionsPage.clickNewExtraction();
    await extractionsPage.fillCreateForm({
      name: extractionData.name,
      sourceType: extractionData.sourceType,
      query: extractionData.extractionQuery,
    });
    await extractionsPage.cancelCreateDialog();

    // Assert
    // 1. Dialog should close
    await expect(extractionsPage.createDialog).not.toBeVisible();

    // 2. Extraction should NOT be created
    await page.waitForTimeout(500);
    const exists = await extractionsPage.extractionExists(extractionData.name);
    expect(exists).toBe(false);

    // 3. Count should remain same
    const newCount = await extractionsPage.getTotalCount();
    expect(newCount).toBe(initialCount);
  });

  test('EXT-004: Create extraction form validation', async ({ page }) => {
    // Act - Try to create with empty name
    await extractionsPage.clickNewExtraction();

    // Try to submit without filling
    const isCreateButtonDisabled = await extractionsPage.dialogCreateButton.isDisabled();

    // Assert - Button should be disabled or show validation errors
    // Note: Exact behavior depends on form implementation
    if (!isCreateButtonDisabled) {
      // If button is enabled, submitting should show validation error
      await extractionsPage.dialogCreateButton.click();
      // Dialog should remain open
      await expect(extractionsPage.createDialog).toBeVisible();
    }
  });

  test('EXT-005: Create extraction and verify in API', async ({ page }) => {
    // Arrange
    const extractionData = createTestExtraction({
      name: 'E2E Test - API Verification',
    });

    // Act
    await extractionsPage.createExtraction({
      name: extractionData.name,
      sourceType: extractionData.sourceType,
      query: extractionData.extractionQuery,
    });

    // Assert - Verify via API
    await page.waitForTimeout(1000);

    const { apiGet } = await import('../../helpers/api.helper');
    const response = await apiGet(page, '/extractions?size=100');
    expect(response.ok()).toBe(true);

    const data = await response.json();
    const createdExtraction = data.data.content.find(
      (ext: any) => ext.name === extractionData.name
    );

    expect(createdExtraction).toBeDefined();
    expect(createdExtraction.sourceType).toBe(extractionData.sourceType);
    expect(createdExtraction.status).toBe('PENDING');

    // Save ID for cleanup
    if (createdExtraction?.id) {
      createdExtractionIds.push(createdExtraction.id);
    }
  });
});
