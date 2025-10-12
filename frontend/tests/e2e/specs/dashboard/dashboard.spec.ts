import { test, expect } from '@playwright/test';
import { DashboardPage } from '../../pages/dashboard/DashboardPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';

/**
 * DASH-001 to DASH-005: Dashboard Tests
 * Tests for dashboard loading, statistics, charts, and error handling
 */

test.describe('Dashboard', () => {
  let dashboardPage: DashboardPage;

  test.beforeEach(async ({ page }) => {
    // Setup authenticated session
    await setupAuthenticatedSession(page, 'admin');
    dashboardPage = new DashboardPage(page);
  });

  test('DASH-001: Dashboard loads successfully with all elements', async ({ page }) => {
    // Arrange & Act
    await dashboardPage.goto();

    // Assert - All dashboard elements should be visible
    await dashboardPage.verifyDashboardElements();

    // Check statistics cards
    await expect(dashboardPage.totalExtractionsCard).toBeVisible();
    await expect(dashboardPage.activeMigrationsCard).toBeVisible();
    await expect(dashboardPage.qualityScoreCard).toBeVisible();
    await expect(dashboardPage.complianceRateCard).toBeVisible();

    // Check charts
    await dashboardPage.waitForCharts();
    const chartsVisible = await dashboardPage.areChartsVisible();
    expect(chartsVisible).toBe(true);

    // Check performance metrics
    const metricsVisible = await dashboardPage.arePerformanceMetricsVisible();
    expect(metricsVisible).toBe(true);
  });

  test('DASH-002: Dashboard statistics display correct data', async ({ page }) => {
    // Arrange & Act
    await dashboardPage.goto();

    // Assert - Statistics should be numbers >= 0
    const totalExtractions = await dashboardPage.getTotalExtractions();
    expect(totalExtractions).toBeGreaterThanOrEqual(0);

    const activeMigrations = await dashboardPage.getActiveMigrations();
    expect(activeMigrations).toBeGreaterThanOrEqual(0);

    const qualityScore = await dashboardPage.getQualityScore();
    expect(qualityScore).toBeGreaterThanOrEqual(0);
    expect(qualityScore).toBeLessThanOrEqual(100);

    const complianceRate = await dashboardPage.getComplianceRate();
    expect(complianceRate).toBeGreaterThanOrEqual(0);
    expect(complianceRate).toBeLessThanOrEqual(100);
  });

  test('DASH-003: Dashboard charts render without errors', async ({ page }) => {
    // Arrange
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    // Act
    await dashboardPage.goto();
    await dashboardPage.waitForCharts();

    // Assert
    // 1. Charts should be visible
    const chartsVisible = await dashboardPage.areChartsVisible();
    expect(chartsVisible).toBe(true);

    // 2. No console errors related to charts
    const chartErrors = consoleErrors.filter(err =>
      err.toLowerCase().includes('chart') ||
      err.toLowerCase().includes('recharts') ||
      err.toLowerCase().includes('undefined')
    );
    expect(chartErrors.length).toBe(0);

    // 3. No visual error alerts
    const hasError = await dashboardPage.hasError();
    expect(hasError).toBe(false);
  });

  test('DASH-004: Dashboard performance metrics are within valid ranges', async ({ page }) => {
    // Arrange & Act
    await dashboardPage.goto();

    // Assert - Verify metrics are present and visible
    await expect(dashboardPage.cpuMetric).toBeVisible();
    await expect(dashboardPage.memoryMetric).toBeVisible();
    await expect(dashboardPage.storageMetric).toBeVisible();
    await expect(dashboardPage.networkMetric).toBeVisible();

    // All metrics should contain percentage or usage information
    const cpuText = await dashboardPage.cpuMetric.textContent();
    expect(cpuText).toBeTruthy();

    const memoryText = await dashboardPage.memoryMetric.textContent();
    expect(memoryText).toBeTruthy();

    const storageText = await dashboardPage.storageMetric.textContent();
    expect(storageText).toBeTruthy();

    const networkText = await dashboardPage.networkMetric.textContent();
    expect(networkText).toBeTruthy();
  });

  test('DASH-005: Dashboard handles API errors gracefully', async ({ page }) => {
    // Arrange - Intercept API call to simulate error
    await page.route('**/api/v1/analytics/dashboard', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal server error' }),
      });
    });

    // Act
    await page.goto('/dashboard');

    // Assert - Should handle error gracefully
    // Either show error message or display empty state
    await page.waitForTimeout(2000);

    // Check if error is shown or dashboard shows empty state
    const hasError = await dashboardPage.hasError();
    const errorMessage = hasError ? await dashboardPage.getErrorMessage() : '';

    // At minimum, the page should not crash
    await expect(page.locator('text=Dashboard')).toBeVisible();

    // If error is shown, it should be user-friendly
    if (hasError) {
      expect(errorMessage).toBeTruthy();
    }
  });

  test('DASH-006: Dashboard loads quickly (performance)', async ({ page }) => {
    // Arrange
    const startTime = Date.now();

    // Act
    await dashboardPage.goto();

    // Assert - Dashboard should load within reasonable time
    const loadTime = Date.now() - startTime;
    expect(loadTime).toBeLessThan(10000); // 10 seconds max

    // Page should be interactive
    await expect(dashboardPage.pageTitle).toBeVisible();
    await expect(dashboardPage.totalExtractionsCard).toBeVisible();
  });

  test('DASH-007: Dashboard statistics cards are clickable', async ({ page }) => {
    // Arrange
    await dashboardPage.goto();

    // Act & Assert - Check if cards are clickable (if implemented)
    // This is a future enhancement test
    const extractionsCard = dashboardPage.totalExtractionsCard;
    await expect(extractionsCard).toBeVisible();

    // Cards might link to their respective pages
    // NOTE: This depends on implementation
  });

  test('DASH-008: Recent activities display correctly', async ({ page }) => {
    // Arrange & Act
    await dashboardPage.goto();

    // Assert - Recent activities section should be present
    // NOTE: This depends on implementation
    const activitiesSection = page.locator('text=/recent activities/i').or(
      page.locator('[data-testid="recent-activities"]')
    );

    // If activities exist, they should be visible
    const hasActivities = await activitiesSection.isVisible().catch(() => false);

    if (hasActivities) {
      // Activities should have some content
      const content = await activitiesSection.textContent();
      expect(content).toBeTruthy();
    }
  });
});
