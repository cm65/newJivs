import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Extractions Page
 * Represents /extractions route
 */
export class ExtractionsPage {
  readonly page: Page;

  // Locators
  readonly pageTitle: Locator;
  readonly newExtractionButton: Locator;
  readonly statusFilter: Locator;
  readonly extractionsTable: Locator;
  readonly tableRows: Locator;
  readonly pagination: Locator;
  readonly totalExtractionsCard: Locator;
  readonly runningExtractionsCard: Locator;
  readonly completedExtractionsCard: Locator;
  readonly failedExtractionsCard: Locator;
  readonly createDialog: Locator;
  readonly dialogNameInput: Locator;
  readonly dialogSourceTypeSelect: Locator;
  readonly dialogQueryInput: Locator;
  readonly dialogCreateButton: Locator;
  readonly dialogCancelButton: Locator;
  readonly loadingSpinner: Locator;
  readonly errorAlert: Locator;
  readonly successAlert: Locator;

  constructor(page: Page) {
    this.page = page;

    // Initialize locators
    this.pageTitle = page.locator('text=Extractions');
    this.newExtractionButton = page.locator('button:has-text("New Extraction")');
    this.statusFilter = page.locator('select[name="status"], [role="combobox"]').first();
    this.extractionsTable = page.locator('table');
    this.tableRows = page.locator('table tbody tr');
    this.pagination = page.locator('[role="navigation"]').last();
    this.totalExtractionsCard = page.locator('text=Total').locator('..');
    this.runningExtractionsCard = page.locator('text=Running').locator('..');
    this.completedExtractionsCard = page.locator('text=Completed').locator('..');
    this.failedExtractionsCard = page.locator('text=Failed').locator('..');
    this.createDialog = page.locator('[role="dialog"]');
    this.dialogNameInput = this.createDialog.locator('[name="name"], input[label="Name"]').first();
    this.dialogSourceTypeSelect = this.createDialog.locator('[name="sourceType"], [label="Source Type"]').first();
    this.dialogQueryInput = this.createDialog.locator('[name="query"], textarea').first();
    this.dialogCreateButton = this.createDialog.locator('button:has-text("Create")');
    this.dialogCancelButton = this.createDialog.locator('button:has-text("Cancel")');
    this.loadingSpinner = page.locator('[role="progressbar"]');
    this.errorAlert = page.locator('[role="alert"][class*="error"]');
    this.successAlert = page.locator('[role="alert"][class*="success"]');
  }

  /**
   * Navigate to extractions page
   */
  async goto(): Promise<void> {
    await this.page.goto('/extractions');
    await this.waitForLoad();
  }

  /**
   * Wait for page to load
   */
  async waitForLoad(): Promise<void> {
    await this.pageTitle.waitFor({ state: 'visible', timeout: 15000 });
    await this.extractionsTable.waitFor({ state: 'visible', timeout: 15000 });
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Click new extraction button
   */
  async clickNewExtraction(): Promise<void> {
    await this.newExtractionButton.click();
    await this.createDialog.waitFor({ state: 'visible' });
  }

  /**
   * Fill create extraction form
   */
  async fillCreateForm(data: {
    name: string;
    sourceType: string;
    query?: string;
  }): Promise<void> {
    await this.dialogNameInput.fill(data.name);
    await this.dialogSourceTypeSelect.selectOption(data.sourceType);
    if (data.query) {
      await this.dialogQueryInput.fill(data.query);
    }
  }

  /**
   * Submit create form
   */
  async submitCreateForm(): Promise<void> {
    await this.dialogCreateButton.click();
    await this.createDialog.waitFor({ state: 'hidden', timeout: 10000 });
  }

  /**
   * Cancel create dialog
   */
  async cancelCreateDialog(): Promise<void> {
    await this.dialogCancelButton.click();
    await this.createDialog.waitFor({ state: 'hidden' });
  }

  /**
   * Create extraction with full flow
   */
  async createExtraction(data: {
    name: string;
    sourceType: string;
    query?: string;
  }): Promise<void> {
    await this.clickNewExtraction();
    await this.fillCreateForm(data);
    await this.submitCreateForm();
  }

  /**
   * Get extraction count from statistics
   */
  async getTotalCount(): Promise<number> {
    const text = await this.totalExtractionsCard.textContent();
    return parseInt(text?.match(/\d+/)?.[0] || '0');
  }

  /**
   * Get running extractions count
   */
  async getRunningCount(): Promise<number> {
    const text = await this.runningExtractionsCard.textContent();
    return parseInt(text?.match(/\d+/)?.[0] || '0');
  }

  /**
   * Filter by status
   */
  async filterByStatus(status: string): Promise<void> {
    await this.statusFilter.selectOption(status);
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Get table row count
   */
  async getTableRowCount(): Promise<number> {
    return await this.tableRows.count();
  }

  /**
   * Get extraction by name from table
   */
  async findExtractionByName(name: string): Promise<Locator | null> {
    const row = this.page.locator(`table tbody tr:has-text("${name}")`);
    if (await row.count() > 0) {
      return row.first();
    }
    return null;
  }

  /**
   * Click action button on extraction row
   */
  async clickActionOnExtraction(name: string, action: 'Start' | 'Stop' | 'Delete'): Promise<void> {
    const row = await this.findExtractionByName(name);
    if (!row) {
      throw new Error(`Extraction "${name}" not found in table`);
    }
    const actionButton = row.locator(`button:has-text("${action}")`);
    await actionButton.click();

    // Handle confirmation dialogs for Delete
    if (action === 'Delete') {
      const confirmButton = this.page.locator('button:has-text("Confirm"), button:has-text("Delete")').last();
      await confirmButton.waitFor({ state: 'visible', timeout: 5000 });
      await confirmButton.click();
    }

    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Start extraction
   */
  async startExtraction(name: string): Promise<void> {
    await this.clickActionOnExtraction(name, 'Start');
  }

  /**
   * Stop extraction
   */
  async stopExtraction(name: string): Promise<void> {
    await this.clickActionOnExtraction(name, 'Stop');
  }

  /**
   * Delete extraction
   */
  async deleteExtraction(name: string): Promise<void> {
    await this.clickActionOnExtraction(name, 'Delete');
  }

  /**
   * Get extraction status from row
   */
  async getExtractionStatus(name: string): Promise<string> {
    const row = await this.findExtractionByName(name);
    if (!row) {
      throw new Error(`Extraction "${name}" not found`);
    }
    const statusChip = row.locator('[class*="MuiChip"]');
    return await statusChip.textContent() || '';
  }

  /**
   * Check if extraction exists in table
   */
  async extractionExists(name: string): Promise<boolean> {
    const row = await this.findExtractionByName(name);
    return row !== null;
  }

  /**
   * Change page
   */
  async goToPage(pageNumber: number): Promise<void> {
    const pageButton = this.pagination.locator(`button:has-text("${pageNumber}")`);
    await pageButton.click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Change rows per page
   */
  async changeRowsPerPage(rows: number): Promise<void> {
    const rowsSelect = this.pagination.locator('select, [role="combobox"]').first();
    await rowsSelect.selectOption(rows.toString());
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Check if success message is shown
   */
  async hasSuccessMessage(): Promise<boolean> {
    return await this.successAlert.isVisible();
  }

  /**
   * Check if error message is shown
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
   * Verify page elements
   */
  async verifyPageElements(): Promise<void> {
    await expect(this.pageTitle).toBeVisible();
    await expect(this.newExtractionButton).toBeVisible();
    await expect(this.extractionsTable).toBeVisible();
    await expect(this.pagination).toBeVisible();
  }
}
