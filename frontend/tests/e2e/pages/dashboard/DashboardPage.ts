import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Dashboard Page
 * Represents /dashboard route
 */
export class DashboardPage {
  readonly page: Page;

  // Locators
  readonly pageTitle: Locator;
  readonly totalExtractionsCard: Locator;
  readonly activeMigrationsCard: Locator;
  readonly qualityScoreCard: Locator;
  readonly complianceRateCard: Locator;
  readonly lineChart: Locator;
  readonly pieChart: Locator;
  readonly cpuMetric: Locator;
  readonly memoryMetric: Locator;
  readonly storageMetric: Locator;
  readonly networkMetric: Locator;
  readonly recentActivities: Locator;
  readonly loadingSpinner: Locator;
  readonly errorAlert: Locator;

  constructor(page: Page) {
    this.page = page;

    // Initialize locators
    this.pageTitle = page.locator('text=Dashboard');
    this.totalExtractionsCard = page.locator('[data-testid="total-extractions-card"]').or(
      page.locator('text=Total Extractions').locator('..')
    );
    this.activeMigrationsCard = page.locator('[data-testid="active-migrations-card"]').or(
      page.locator('text=Active Migrations').locator('..')
    );
    this.qualityScoreCard = page.locator('[data-testid="quality-score-card"]').or(
      page.locator('text=Quality Score').locator('..')
    );
    this.complianceRateCard = page.locator('[data-testid="compliance-rate-card"]').or(
      page.locator('text=Compliance Rate').locator('..')
    );
    this.lineChart = page.locator('.recharts-wrapper').first();
    this.pieChart = page.locator('.recharts-wrapper').nth(1);
    this.cpuMetric = page.locator('text=CPU Usage').locator('..');
    this.memoryMetric = page.locator('text=Memory Usage').locator('..');
    this.storageMetric = page.locator('text=Storage Usage').locator('..');
    this.networkMetric = page.locator('text=Network Activity').locator('..');
    this.recentActivities = page.locator('[data-testid="recent-activities"]');
    this.loadingSpinner = page.locator('[role="progressbar"]');
    this.errorAlert = page.locator('[role="alert"]');
  }

  /**
   * Navigate to dashboard
   */
  async goto(): Promise<void> {
    await this.page.goto('/dashboard');
    await this.waitForLoad();
  }

  /**
   * Wait for page to load
   */
  async waitForLoad(): Promise<void> {
    await this.pageTitle.waitFor({ state: 'visible', timeout: 15000 });
    // Wait for loading to complete
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Get statistic value from card
   */
  async getStatisticValue(cardName: string): Promise<string> {
    const card = this.page.locator(`text=${cardName}`).locator('..');
    const value = card.locator('h4, h3').first();
    return await value.textContent() || '0';
  }

  /**
   * Get total extractions count
   */
  async getTotalExtractions(): Promise<number> {
    const value = await this.getStatisticValue('Total Extractions');
    return parseInt(value.replace(/[^0-9]/g, '')) || 0;
  }

  /**
   * Get active migrations count
   */
  async getActiveMigrations(): Promise<number> {
    const value = await this.getStatisticValue('Active Migrations');
    return parseInt(value.replace(/[^0-9]/g, '')) || 0;
  }

  /**
   * Get quality score
   */
  async getQualityScore(): Promise<number> {
    const value = await this.getStatisticValue('Quality Score');
    return parseFloat(value.replace(/[^0-9.]/g, '')) || 0;
  }

  /**
   * Get compliance rate
   */
  async getComplianceRate(): Promise<number> {
    const value = await this.getStatisticValue('Compliance Rate');
    return parseFloat(value.replace(/[^0-9.]/g, '')) || 0;
  }

  /**
   * Check if charts are visible
   */
  async areChartsVisible(): Promise<boolean> {
    return (await this.lineChart.isVisible()) && (await this.pieChart.isVisible());
  }

  /**
   * Check if performance metrics are visible
   */
  async arePerformanceMetricsVisible(): Promise<boolean> {
    return (
      (await this.cpuMetric.isVisible()) &&
      (await this.memoryMetric.isVisible()) &&
      (await this.storageMetric.isVisible()) &&
      (await this.networkMetric.isVisible())
    );
  }

  /**
   * Check if page has errors
   */
  async hasError(): Promise<boolean> {
    return await this.errorAlert.isVisible();
  }

  /**
   * Get error message
   */
  async getErrorMessage(): Promise<string> {
    if (await this.hasError()) {
      return await this.errorAlert.textContent() || '';
    }
    return '';
  }

  /**
   * Check if loading
   */
  async isLoading(): Promise<boolean> {
    return await this.loadingSpinner.isVisible();
  }

  /**
   * Wait for charts to render
   */
  async waitForCharts(): Promise<void> {
    await this.lineChart.waitFor({ state: 'visible', timeout: 10000 });
    await this.pieChart.waitFor({ state: 'visible', timeout: 10000 });
  }

  /**
   * Verify all dashboard elements
   */
  async verifyDashboardElements(): Promise<void> {
    await expect(this.pageTitle).toBeVisible();
    await expect(this.totalExtractionsCard).toBeVisible();
    await expect(this.activeMigrationsCard).toBeVisible();
    await expect(this.qualityScoreCard).toBeVisible();
    await expect(this.complianceRateCard).toBeVisible();
  }

  /**
   * Check if dashboard loaded successfully (no console errors)
   */
  async hasConsoleErrors(): Promise<boolean> {
    const errors: string[] = [];
    this.page.on('console', (msg) => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });
    await this.page.waitForTimeout(1000);
    return errors.length > 0;
  }
}
