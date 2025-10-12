import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Migrations Page
 * Represents /migrations route
 */
export class MigrationsPage {
  readonly page: Page;

  // Locators
  readonly pageTitle: Locator;
  readonly newMigrationButton: Locator;
  readonly statusFilter: Locator;
  readonly migrationsTable: Locator;
  readonly tableRows: Locator;
  readonly pagination: Locator;
  readonly totalMigrationsCard: Locator;
  readonly runningMigrationsCard: Locator;
  readonly completedMigrationsCard: Locator;
  readonly failedMigrationsCard: Locator;
  readonly createDialog: Locator;
  readonly dialogNameInput: Locator;
  readonly dialogCreateButton: Locator;
  readonly dialogCancelButton: Locator;
  readonly loadingSpinner: Locator;
  readonly errorAlert: Locator;
  readonly successAlert: Locator;

  constructor(page: Page) {
    this.page = page;

    // Initialize locators
    this.pageTitle = page.locator('text=Migrations');
    this.newMigrationButton = page.locator('button:has-text("New Migration")');
    this.statusFilter = page.locator('select[name="status"], [role="combobox"]').first();
    this.migrationsTable = page.locator('table');
    this.tableRows = page.locator('table tbody tr');
    this.pagination = page.locator('[role="navigation"]').last();
    this.totalMigrationsCard = page.locator('text=Total').locator('..');
    this.runningMigrationsCard = page.locator('text=Running').locator('..');
    this.completedMigrationsCard = page.locator('text=Completed').locator('..');
    this.failedMigrationsCard = page.locator('text=Failed').locator('..');
    this.createDialog = page.locator('[role="dialog"]');
    this.dialogNameInput = this.createDialog.locator('[name="name"], input').first();
    this.dialogCreateButton = this.createDialog.locator('button:has-text("Create")');
    this.dialogCancelButton = this.createDialog.locator('button:has-text("Cancel")');
    this.loadingSpinner = page.locator('[role="progressbar"]');
    this.errorAlert = page.locator('[role="alert"][class*="error"]');
    this.successAlert = page.locator('[role="alert"][class*="success"]');
  }

  /**
   * Navigate to migrations page
   */
  async goto(): Promise<void> {
    await this.page.goto('/migrations');
    await this.waitForLoad();
  }

  /**
   * Wait for page to load
   */
  async waitForLoad(): Promise<void> {
    await this.pageTitle.waitFor({ state: 'visible', timeout: 15000 });
    await this.migrationsTable.waitFor({ state: 'visible', timeout: 15000 });
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Click new migration button
   */
  async clickNewMigration(): Promise<void> {
    await this.newMigrationButton.click();
    await this.createDialog.waitFor({ state: 'visible' });
  }

  /**
   * Fill create migration form
   */
  async fillCreateForm(data: { name: string }): Promise<void> {
    await this.dialogNameInput.fill(data.name);
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
   * Create migration with full flow
   */
  async createMigration(name: string): Promise<void> {
    await this.clickNewMigration();
    await this.fillCreateForm({ name });
    await this.submitCreateForm();
  }

  /**
   * Get migration count from statistics
   */
  async getTotalCount(): Promise<number> {
    const text = await this.totalMigrationsCard.textContent();
    return parseInt(text?.match(/\d+/)?.[0] || '0');
  }

  /**
   * Get running migrations count
   */
  async getRunningCount(): Promise<number> {
    const text = await this.runningMigrationsCard.textContent();
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
   * Get migration by name from table
   */
  async findMigrationByName(name: string): Promise<Locator | null> {
    const row = this.page.locator(`table tbody tr:has-text("${name}")`);
    if (await row.count() > 0) {
      return row.first();
    }
    return null;
  }

  /**
   * Click action button on migration row
   */
  async clickActionOnMigration(
    name: string,
    action: 'Start' | 'Pause' | 'Resume' | 'Rollback' | 'Delete'
  ): Promise<void> {
    const row = await this.findMigrationByName(name);
    if (!row) {
      throw new Error(`Migration "${name}" not found in table`);
    }
    const actionButton = row.locator(`button:has-text("${action}")`);
    await actionButton.click();

    // Handle confirmation dialogs for Rollback and Delete
    if (action === 'Rollback' || action === 'Delete') {
      const confirmButton = this.page.locator('button:has-text("Confirm"), button:has-text("Yes")').last();
      await confirmButton.waitFor({ state: 'visible', timeout: 5000 });
      await confirmButton.click();
    }

    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Start migration
   */
  async startMigration(name: string): Promise<void> {
    await this.clickActionOnMigration(name, 'Start');
  }

  /**
   * Pause migration
   */
  async pauseMigration(name: string): Promise<void> {
    await this.clickActionOnMigration(name, 'Pause');
  }

  /**
   * Resume migration
   */
  async resumeMigration(name: string): Promise<void> {
    await this.clickActionOnMigration(name, 'Resume');
  }

  /**
   * Rollback migration
   */
  async rollbackMigration(name: string): Promise<void> {
    await this.clickActionOnMigration(name, 'Rollback');
  }

  /**
   * Delete migration
   */
  async deleteMigration(name: string): Promise<void> {
    await this.clickActionOnMigration(name, 'Delete');
  }

  /**
   * Get migration status from row
   */
  async getMigrationStatus(name: string): Promise<string> {
    const row = await this.findMigrationByName(name);
    if (!row) {
      throw new Error(`Migration "${name}" not found`);
    }
    const statusChip = row.locator('[class*="MuiChip"]');
    return await statusChip.textContent() || '';
  }

  /**
   * Get migration phase from row
   */
  async getMigrationPhase(name: string): Promise<string> {
    const row = await this.findMigrationByName(name);
    if (!row) {
      throw new Error(`Migration "${name}" not found`);
    }
    // Phase is typically in the 4th column (index 3)
    const phaseCell = row.locator('td').nth(2);
    return await phaseCell.textContent() || '';
  }

  /**
   * Get migration progress from row
   */
  async getMigrationProgress(name: string): Promise<number> {
    const row = await this.findMigrationByName(name);
    if (!row) {
      throw new Error(`Migration "${name}" not found`);
    }
    // Progress is typically shown in progress bar or text
    const progressText = await row.locator('text=/\\d+\\.?\\d*%/').textContent();
    return parseFloat(progressText?.replace('%', '') || '0');
  }

  /**
   * Check if migration exists in table
   */
  async migrationExists(name: string): Promise<boolean> {
    const row = await this.findMigrationByName(name);
    return row !== null;
  }

  /**
   * Verify progress bar is displayed correctly (0-100%)
   */
  async verifyProgressBarValid(name: string): Promise<boolean> {
    const progress = await this.getMigrationProgress(name);
    return progress >= 0 && progress <= 100;
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
    await expect(this.newMigrationButton).toBeVisible();
    await expect(this.migrationsTable).toBeVisible();
    await expect(this.pagination).toBeVisible();
  }
}
