import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Data Quality page
 * Provides methods to interact with all Data Quality features:
 * - Dashboard statistics
 * - Quality Rules management
 * - Issues tracking
 * - Dataset Profiles
 */
export class DataQualityPage {
  readonly page: Page;

  // Page elements
  readonly pageTitle: Locator;
  readonly refreshButton: Locator;
  readonly newRuleButton: Locator;
  readonly profileDatasetButton: Locator;

  // Dashboard Statistics Cards
  readonly overallScoreCard: Locator;
  readonly activeRulesCard: Locator;
  readonly openIssuesCard: Locator;
  readonly criticalIssuesCard: Locator;

  // Quality Dimensions
  readonly qualityDimensionsCard: Locator;

  // Tabs
  readonly rulesTab: Locator;
  readonly issuesTab: Locator;
  readonly profilesTab: Locator;

  // Rules table
  readonly rulesTable: Locator;
  readonly rulesDimensionFilter: Locator;
  readonly rulesSeverityFilter: Locator;
  readonly rulesRows: Locator;
  readonly createRuleDialog: Locator;
  readonly ruleNameInput: Locator;
  readonly ruleDescriptionInput: Locator;
  readonly ruleTypeSelect: Locator;
  readonly ruleDimensionSelect: Locator;
  readonly ruleSeveritySelect: Locator;
  readonly createRuleSubmitButton: Locator;
  readonly createRuleCancelButton: Locator;

  // Issues table
  readonly issuesTable: Locator;
  readonly issuesStatusFilter: Locator;
  readonly issuesSeverityFilter: Locator;
  readonly issuesRows: Locator;

  // Profiles table
  readonly profilesTable: Locator;
  readonly profilesRows: Locator;

  // Error alert
  readonly errorAlert: Locator;

  constructor(page: Page) {
    this.page = page;

    // Page elements
    this.pageTitle = page.locator('h4:has-text("Data Quality")');
    this.refreshButton = page.locator('button:has-text("Refresh")');
    this.newRuleButton = page.locator('button:has-text("New Rule")');
    this.profileDatasetButton = page.locator('button:has-text("Profile Dataset")');

    // Dashboard cards
    this.overallScoreCard = page.locator('text=Overall Score').locator('..');
    this.activeRulesCard = page.locator('text=Active Rules').locator('..');
    this.openIssuesCard = page.locator('text=Open Issues').locator('..');
    this.criticalIssuesCard = page.locator('text=Critical Issues').locator('..');

    // Quality dimensions
    this.qualityDimensionsCard = page.locator('text=Quality Dimensions').locator('..');

    // Tabs
    this.rulesTab = page.locator('button[role="tab"]:has-text("Rules")');
    this.issuesTab = page.locator('button[role="tab"]:has-text("Issues")');
    this.profilesTab = page.locator('button[role="tab"]:has-text("Profiles")');

    // Rules
    this.rulesTable = page.locator('table').first();
    this.rulesDimensionFilter = page.locator('label:has-text("Filter by Dimension")').locator('..');
    this.rulesSeverityFilter = page.locator('label:has-text("Filter by Severity")').first().locator('..');
    this.rulesRows = page.locator('table tbody tr');
    this.createRuleDialog = page.locator('[role="dialog"]:has-text("Create New Quality Rule")');
    this.ruleNameInput = this.createRuleDialog.locator('input[value]').first();
    this.ruleDescriptionInput = this.createRuleDialog.locator('textarea');
    this.ruleTypeSelect = this.createRuleDialog.locator('label:has-text("Rule Type")').locator('..');
    this.ruleDimensionSelect = this.createRuleDialog.locator('label:has-text("Dimension")').locator('..');
    this.ruleSeveritySelect = this.createRuleDialog.locator('label:has-text("Severity")').locator('..');
    this.createRuleSubmitButton = this.createRuleDialog.locator('button:has-text("Create")');
    this.createRuleCancelButton = this.createRuleDialog.locator('button:has-text("Cancel")');

    // Issues
    this.issuesTable = page.locator('table').first();
    this.issuesStatusFilter = page.locator('label:has-text("Filter by Status")').locator('..');
    this.issuesSeverityFilter = page.locator('label:has-text("Filter by Severity")').first().locator('..');
    this.issuesRows = page.locator('table tbody tr');

    // Profiles
    this.profilesTable = page.locator('table').first();
    this.profilesRows = page.locator('table tbody tr');

    // Error alert
    this.errorAlert = page.locator('[role="alert"]');
  }

  /**
   * Navigate to Data Quality page
   */
  async goto(): Promise<void> {
    await this.page.goto('/data-quality');
    await this.waitForLoad();
  }

  /**
   * Wait for page to load
   */
  async waitForLoad(): Promise<void> {
    await this.pageTitle.waitFor({ state: 'visible', timeout: 10000 });
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Verify all page elements are present
   */
  async verifyPageElements(): Promise<void> {
    await expect(this.pageTitle).toBeVisible();
    await expect(this.refreshButton).toBeVisible();
    await expect(this.newRuleButton).toBeVisible();
    await expect(this.profileDatasetButton).toBeVisible();
  }

  /**
   * Get overall quality score
   */
  async getOverallScore(): Promise<number> {
    const scoreText = await this.overallScoreCard.locator('h4').textContent();
    return parseFloat(scoreText?.replace('%', '') || '0');
  }

  /**
   * Get active rules count
   */
  async getActiveRulesCount(): Promise<number> {
    const countText = await this.activeRulesCard.locator('h4').textContent();
    return parseInt(countText || '0');
  }

  /**
   * Get open issues count
   */
  async getOpenIssuesCount(): Promise<number> {
    const countText = await this.openIssuesCard.locator('h4').textContent();
    return parseInt(countText || '0');
  }

  /**
   * Get critical issues count
   */
  async getCriticalIssuesCount(): Promise<number> {
    const countText = await this.criticalIssuesCard.locator('h4').textContent();
    return parseInt(countText || '0');
  }

  /**
   * Switch to Rules tab
   */
  async switchToRulesTab(): Promise<void> {
    await this.rulesTab.click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Switch to Issues tab
   */
  async switchToIssuesTab(): Promise<void> {
    await this.issuesTab.click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Switch to Profiles tab
   */
  async switchToProfilesTab(): Promise<void> {
    await this.profilesTab.click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Click New Rule button
   */
  async clickNewRule(): Promise<void> {
    await this.newRuleButton.click();
    await this.createRuleDialog.waitFor({ state: 'visible' });
  }

  /**
   * Fill create rule form
   */
  async fillCreateRuleForm(data: {
    name: string;
    description: string;
    ruleType?: string;
    dimension?: string;
    severity?: string;
  }): Promise<void> {
    // Clear and fill name
    await this.ruleNameInput.clear();
    await this.ruleNameInput.fill(data.name);

    // Clear and fill description
    await this.ruleDescriptionInput.clear();
    await this.ruleDescriptionInput.fill(data.description);

    // Select rule type if provided
    if (data.ruleType) {
      await this.ruleTypeSelect.click();
      await this.page.locator(`li[role="option"]:has-text("${data.ruleType}")`).click();
    }

    // Select dimension if provided
    if (data.dimension) {
      await this.ruleDimensionSelect.click();
      await this.page.locator(`li[role="option"]:has-text("${data.dimension}")`).click();
    }

    // Select severity if provided
    if (data.severity) {
      await this.ruleSeveritySelect.click();
      await this.page.locator(`li[role="option"]:has-text("${data.severity}")`).click();
    }
  }

  /**
   * Submit create rule form
   */
  async submitCreateRuleForm(): Promise<void> {
    await this.createRuleSubmitButton.click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Cancel create rule form
   */
  async cancelCreateRuleForm(): Promise<void> {
    await this.createRuleCancelButton.click();
    await this.createRuleDialog.waitFor({ state: 'hidden' });
  }

  /**
   * Create a new rule (convenience method)
   */
  async createRule(data: {
    name: string;
    description: string;
    ruleType?: string;
    dimension?: string;
    severity?: string;
  }): Promise<void> {
    await this.clickNewRule();
    await this.fillCreateRuleForm(data);
    await this.submitCreateRuleForm();
  }

  /**
   * Filter rules by dimension
   */
  async filterRulesByDimension(dimension: string): Promise<void> {
    await this.rulesDimensionFilter.click();
    await this.page.locator(`li[role="option"]:has-text("${dimension}")`).click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Filter rules by severity
   */
  async filterRulesBySeverity(severity: string): Promise<void> {
    await this.rulesSeverityFilter.click();
    await this.page.locator(`li[role="option"]:has-text("${severity}")`).first().click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Get rules table row count
   */
  async getRulesCount(): Promise<number> {
    const rows = await this.rulesRows.all();
    // Check if "No rules found" message is present
    if (rows.length === 1) {
      const text = await rows[0].textContent();
      if (text?.includes('No rules found')) return 0;
    }
    return rows.length;
  }

  /**
   * Find rule by name
   */
  async findRuleByName(name: string): Promise<Locator | null> {
    const rows = await this.rulesRows.all();
    for (const row of rows) {
      const text = await row.textContent();
      if (text?.includes(name)) {
        return row;
      }
    }
    return null;
  }

  /**
   * Check if rule exists
   */
  async ruleExists(name: string): Promise<boolean> {
    const row = await this.findRuleByName(name);
    return row !== null;
  }

  /**
   * Execute rule by name
   */
  async executeRule(name: string): Promise<void> {
    const row = await this.findRuleByName(name);
    if (!row) throw new Error(`Rule "${name}" not found`);
    await row.locator('button[title="Execute"]').click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Delete rule by name
   */
  async deleteRule(name: string): Promise<void> {
    const row = await this.findRuleByName(name);
    if (!row) throw new Error(`Rule "${name}" not found`);

    // Click delete button
    await row.locator('button[title="Delete"]').click();

    // Confirm deletion (browser dialog)
    this.page.on('dialog', (dialog) => dialog.accept());
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Filter issues by status
   */
  async filterIssuesByStatus(status: string): Promise<void> {
    await this.issuesStatusFilter.click();
    await this.page.locator(`li[role="option"]:has-text("${status}")`).click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Filter issues by severity
   */
  async filterIssuesBySeverity(severity: string): Promise<void> {
    await this.issuesSeverityFilter.click();
    await this.page.locator(`li[role="option"]:has-text("${severity}")`).first().click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Get issues table row count
   */
  async getIssuesCount(): Promise<number> {
    const rows = await this.issuesRows.all();
    // Check if "No issues found" message is present
    if (rows.length === 1) {
      const text = await rows[0].textContent();
      if (text?.includes('No issues found')) return 0;
    }
    return rows.length;
  }

  /**
   * Find issue by description
   */
  async findIssueByDescription(description: string): Promise<Locator | null> {
    const rows = await this.issuesRows.all();
    for (const row of rows) {
      const text = await row.textContent();
      if (text?.includes(description)) {
        return row;
      }
    }
    return null;
  }

  /**
   * Resolve issue by description
   */
  async resolveIssue(description: string): Promise<void> {
    const row = await this.findIssueByDescription(description);
    if (!row) throw new Error(`Issue "${description}" not found`);
    await row.locator('button[title="Resolve"]').click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Get profiles table row count
   */
  async getProfilesCount(): Promise<number> {
    const rows = await this.profilesRows.all();
    // Check if "No profiles found" message is present
    if (rows.length === 1) {
      const text = await rows[0].textContent();
      if (text?.includes('No profiles found')) return 0;
    }
    return rows.length;
  }

  /**
   * Find profile by dataset name
   */
  async findProfileByDataset(datasetName: string): Promise<Locator | null> {
    const rows = await this.profilesRows.all();
    for (const row of rows) {
      const text = await row.textContent();
      if (text?.includes(datasetName)) {
        return row;
      }
    }
    return null;
  }

  /**
   * Check if profile exists
   */
  async profileExists(datasetName: string): Promise<boolean> {
    const row = await this.findProfileByDataset(datasetName);
    return row !== null;
  }

  /**
   * Get profile score
   */
  async getProfileScore(datasetName: string): Promise<number> {
    const row = await this.findProfileByDataset(datasetName);
    if (!row) throw new Error(`Profile "${datasetName}" not found`);

    // Get the score cell (second column)
    const scoreCell = row.locator('td').nth(1);
    const scoreText = await scoreCell.textContent();
    return parseFloat(scoreText?.replace('%', '') || '0');
  }

  /**
   * Refresh dashboard
   */
  async refresh(): Promise<void> {
    await this.refreshButton.click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Check if error is displayed
   */
  async hasError(): Promise<boolean> {
    return await this.errorAlert.isVisible();
  }

  /**
   * Get error message
   */
  async getErrorMessage(): Promise<string> {
    return await this.errorAlert.textContent() || '';
  }
}
