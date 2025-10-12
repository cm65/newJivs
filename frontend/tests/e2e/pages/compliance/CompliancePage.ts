import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for Compliance page
 * Provides methods to interact with all Compliance features:
 * - Dashboard statistics
 * - Data Subject Requests (GDPR/CCPA)
 * - Consent Records
 * - Retention Policies
 * - Audit Logs
 */
export class CompliancePage {
  readonly page: Page;

  // Page elements
  readonly pageTitle: Locator;
  readonly refreshButton: Locator;
  readonly newRequestButton: Locator;
  readonly newConsentButton: Locator;
  readonly newPolicyButton: Locator;

  // Dashboard Statistics Cards
  readonly complianceScoreCard: Locator;
  readonly pendingRequestsCard: Locator;
  readonly activeConsentsCard: Locator;
  readonly activePoliciesCard: Locator;

  // Tabs
  readonly requestsTab: Locator;
  readonly consentsTab: Locator;
  readonly policiesTab: Locator;
  readonly auditLogsTab: Locator;

  // Requests
  readonly requestsTable: Locator;
  readonly requestsStatusFilter: Locator;
  readonly requestsTypeFilter: Locator;
  readonly requestsRows: Locator;
  readonly createRequestDialog: Locator;

  // Consents
  readonly consentsTable: Locator;
  readonly consentsRows: Locator;

  // Policies
  readonly policiesTable: Locator;
  readonly policiesRows: Locator;
  readonly createPolicyDialog: Locator;

  // Audit Logs
  readonly auditLogsTable: Locator;
  readonly auditLogsRows: Locator;

  // Error alert
  readonly errorAlert: Locator;

  constructor(page: Page) {
    this.page = page;

    // Page elements
    this.pageTitle = page.locator('h4:has-text("Compliance Management")');
    this.refreshButton = page.locator('button:has-text("Refresh")');
    this.newRequestButton = page.locator('button:has-text("New Request")');
    this.newConsentButton = page.locator('button:has-text("New Consent")');
    this.newPolicyButton = page.locator('button:has-text("New Policy")');

    // Dashboard cards
    this.complianceScoreCard = page.locator('text=Compliance Score').locator('..');
    this.pendingRequestsCard = page.locator('text=Pending Requests').locator('..');
    this.activeConsentsCard = page.locator('text=Active Consents').locator('..');
    this.activePoliciesCard = page.locator('text=Active Policies').locator('..');

    // Tabs
    this.requestsTab = page.locator('button[role="tab"]:has-text("Data Subject Requests")');
    this.consentsTab = page.locator('button[role="tab"]:has-text("Consents")');
    this.policiesTab = page.locator('button[role="tab"]:has-text("Retention Policies")');
    this.auditLogsTab = page.locator('button[role="tab"]:has-text("Audit Logs")');

    // Requests
    this.requestsTable = page.locator('table').first();
    this.requestsStatusFilter = page.locator('label:has-text("Filter by Status")').locator('..');
    this.requestsTypeFilter = page.locator('label:has-text("Filter by Type")').locator('..');
    this.requestsRows = page.locator('table tbody tr');
    this.createRequestDialog = page.locator('[role="dialog"]:has-text("Create Data Subject Request")');

    // Consents
    this.consentsTable = page.locator('table').first();
    this.consentsRows = page.locator('table tbody tr');

    // Policies
    this.policiesTable = page.locator('table').first();
    this.policiesRows = page.locator('table tbody tr');
    this.createPolicyDialog = page.locator('[role="dialog"]:has-text("Create Retention Policy")');

    // Audit Logs
    this.auditLogsTable = page.locator('table').first();
    this.auditLogsRows = page.locator('table tbody tr');

    // Error alert
    this.errorAlert = page.locator('[role="alert"]');
  }

  async goto(): Promise<void> {
    await this.page.goto('/compliance');
    await this.waitForLoad();
  }

  async waitForLoad(): Promise<void> {
    await this.pageTitle.waitFor({ state: 'visible', timeout: 10000 });
    await this.page.waitForLoadState('networkidle');
  }

  async verifyPageElements(): Promise<void> {
    await expect(this.pageTitle).toBeVisible();
    await expect(this.refreshButton).toBeVisible();
  }

  async getComplianceScore(): Promise<number> {
    const scoreText = await this.complianceScoreCard.locator('h4').textContent();
    return parseFloat(scoreText?.replace('%', '') || '0');
  }

  async getPendingRequestsCount(): Promise<number> {
    const countText = await this.pendingRequestsCard.locator('h4').textContent();
    return parseInt(countText || '0');
  }

  async getActiveConsentsCount(): Promise<number> {
    const countText = await this.activeConsentsCard.locator('h4').textContent();
    return parseInt(countText || '0');
  }

  async getActivePoliciesCount(): Promise<number> {
    const countText = await this.activePoliciesCard.locator('h4').textContent();
    return parseInt(countText || '0');
  }

  async switchToRequestsTab(): Promise<void> {
    await this.requestsTab.click();
    await this.page.waitForLoadState('networkidle');
  }

  async switchToConsentsTab(): Promise<void> {
    await this.consentsTab.click();
    await this.page.waitForLoadState('networkidle');
  }

  async switchToPoliciesTab(): Promise<void> {
    await this.policiesTab.click();
    await this.page.waitForLoadState('networkidle');
  }

  async switchToAuditLogsTab(): Promise<void> {
    await this.auditLogsTab.click();
    await this.page.waitForLoadState('networkidle');
  }

  async clickNewRequest(): Promise<void> {
    await this.newRequestButton.click();
    await this.createRequestDialog.waitFor({ state: 'visible' });
  }

  async fillCreateRequestForm(data: {
    type: string;
    regulation: string;
    dataSubjectId: string;
    dataSubjectEmail: string;
    details: string;
    priority?: string;
  }): Promise<void> {
    // Select request type
    await this.createRequestDialog.locator('label:has-text("Request Type")').locator('..').click();
    await this.page.locator(`li[role="option"]:has-text("${data.type}")`).click();

    // Select regulation
    await this.createRequestDialog.locator('label:has-text("Regulation")').locator('..').click();
    await this.page.locator(`li[role="option"]:has-text("${data.regulation}")`).click();

    // Fill data subject ID
    await this.createRequestDialog.locator('input[value]').first().clear();
    await this.createRequestDialog.locator('input[value]').first().fill(data.dataSubjectId);

    // Fill email
    await this.createRequestDialog.locator('input[type="email"]').clear();
    await this.createRequestDialog.locator('input[type="email"]').fill(data.dataSubjectEmail);

    // Fill details
    await this.createRequestDialog.locator('textarea').clear();
    await this.createRequestDialog.locator('textarea').fill(data.details);

    // Select priority if provided
    if (data.priority) {
      await this.createRequestDialog.locator('label:has-text("Priority")').locator('..').click();
      await this.page.locator(`li[role="option"]:has-text("${data.priority}")`).click();
    }
  }

  async submitCreateRequestForm(): Promise<void> {
    await this.createRequestDialog.locator('button:has-text("Create")').click();
    await this.page.waitForLoadState('networkidle');
  }

  async createRequest(data: {
    type: string;
    regulation: string;
    dataSubjectId: string;
    dataSubjectEmail: string;
    details: string;
    priority?: string;
  }): Promise<void> {
    await this.clickNewRequest();
    await this.fillCreateRequestForm(data);
    await this.submitCreateRequestForm();
  }

  async filterRequestsByStatus(status: string): Promise<void> {
    await this.requestsStatusFilter.click();
    await this.page.locator(`li[role="option"]:has-text("${status}")`).click();
    await this.page.waitForLoadState('networkidle');
  }

  async filterRequestsByType(type: string): Promise<void> {
    await this.requestsTypeFilter.click();
    await this.page.locator(`li[role="option"]:has-text("${type}")`).click();
    await this.page.waitForLoadState('networkidle');
  }

  async getRequestsCount(): Promise<number> {
    const rows = await this.requestsRows.all();
    if (rows.length === 1) {
      const text = await rows[0].textContent();
      if (text?.includes('No requests found')) return 0;
    }
    return rows.length;
  }

  async findRequestByEmail(email: string): Promise<Locator | null> {
    const rows = await this.requestsRows.all();
    for (const row of rows) {
      const text = await row.textContent();
      if (text?.includes(email)) {
        return row;
      }
    }
    return null;
  }

  async requestExists(email: string): Promise<boolean> {
    const row = await this.findRequestByEmail(email);
    return row !== null;
  }

  async processRequest(email: string): Promise<void> {
    const row = await this.findRequestByEmail(email);
    if (!row) throw new Error(`Request for "${email}" not found`);
    await row.locator('button[title="Process"]').click();
    await this.page.waitForLoadState('networkidle');
  }

  async exportRequestData(email: string): Promise<void> {
    const row = await this.findRequestByEmail(email);
    if (!row) throw new Error(`Request for "${email}" not found`);
    await row.locator('button[title="Export Data"]').click();
    await this.page.waitForTimeout(1000);
  }

  async getConsentsCount(): Promise<number> {
    const rows = await this.consentsRows.all();
    if (rows.length === 1) {
      const text = await rows[0].textContent();
      if (text?.includes('No consents found')) return 0;
    }
    return rows.length;
  }

  async findConsentByEmail(email: string): Promise<Locator | null> {
    const rows = await this.consentsRows.all();
    for (const row of rows) {
      const text = await row.textContent();
      if (text?.includes(email)) {
        return row;
      }
    }
    return null;
  }

  async revokeConsent(email: string): Promise<void> {
    const row = await this.findConsentByEmail(email);
    if (!row) throw new Error(`Consent for "${email}" not found`);

    this.page.on('dialog', (dialog) => dialog.accept());
    await row.locator('button[title="Revoke"]').click();
    await this.page.waitForLoadState('networkidle');
  }

  async getPoliciesCount(): Promise<number> {
    const rows = await this.policiesRows.all();
    if (rows.length === 1) {
      const text = await rows[0].textContent();
      if (text?.includes('No policies found')) return 0;
    }
    return rows.length;
  }

  async findPolicyByName(name: string): Promise<Locator | null> {
    const rows = await this.policiesRows.all();
    for (const row of rows) {
      const text = await row.textContent();
      if (text?.includes(name)) {
        return row;
      }
    }
    return null;
  }

  async policyExists(name: string): Promise<boolean> {
    const row = await this.findPolicyByName(name);
    return row !== null;
  }

  async clickNewPolicy(): Promise<void> {
    await this.newPolicyButton.click();
    await this.createPolicyDialog.waitFor({ state: 'visible' });
  }

  async fillCreatePolicyForm(data: {
    name: string;
    description: string;
    dataType: string;
    retentionPeriod: number;
    retentionUnit: string;
    action: string;
  }): Promise<void> {
    // Fill name
    await this.createPolicyDialog.locator('input[value]').first().clear();
    await this.createPolicyDialog.locator('input[value]').first().fill(data.name);

    // Fill description
    await this.createPolicyDialog.locator('textarea').clear();
    await this.createPolicyDialog.locator('textarea').fill(data.description);

    // Fill data type
    const dataTypeInput = await this.createPolicyDialog.locator('input[value]').nth(1);
    await dataTypeInput.clear();
    await dataTypeInput.fill(data.dataType);

    // Fill retention period
    await this.createPolicyDialog.locator('input[type="number"]').clear();
    await this.createPolicyDialog.locator('input[type="number"]').fill(data.retentionPeriod.toString());

    // Select retention unit
    await this.createPolicyDialog.locator('label:has-text("Retention Unit")').locator('..').click();
    await this.page.locator(`li[role="option"]:has-text("${data.retentionUnit}")`).click();

    // Select action
    await this.createPolicyDialog.locator('label:has-text("Action")').locator('..').click();
    await this.page.locator(`li[role="option"]:has-text("${data.action}")`).click();
  }

  async submitCreatePolicyForm(): Promise<void> {
    await this.createPolicyDialog.locator('button:has-text("Create")').click();
    await this.page.waitForLoadState('networkidle');
  }

  async createPolicy(data: {
    name: string;
    description: string;
    dataType: string;
    retentionPeriod: number;
    retentionUnit: string;
    action: string;
  }): Promise<void> {
    await this.clickNewPolicy();
    await this.fillCreatePolicyForm(data);
    await this.submitCreatePolicyForm();
  }

  async executePolicy(name: string): Promise<void> {
    const row = await this.findPolicyByName(name);
    if (!row) throw new Error(`Policy "${name}" not found`);

    this.page.on('dialog', (dialog) => dialog.accept());
    await row.locator('button[title="Execute"]').click();
    await this.page.waitForLoadState('networkidle');
  }

  async deletePolicy(name: string): Promise<void> {
    const row = await this.findPolicyByName(name);
    if (!row) throw new Error(`Policy "${name}" not found`);

    this.page.on('dialog', (dialog) => dialog.accept());
    await row.locator('button[title="Delete"]').click();
    await this.page.waitForLoadState('networkidle');
  }

  async getAuditLogsCount(): Promise<number> {
    const rows = await this.auditLogsRows.all();
    if (rows.length === 1) {
      const text = await rows[0].textContent();
      if (text?.includes('No audit logs found')) return 0;
    }
    return rows.length;
  }

  async refresh(): Promise<void> {
    await this.refreshButton.click();
    await this.page.waitForLoadState('networkidle');
  }

  async hasError(): Promise<boolean> {
    return await this.errorAlert.isVisible();
  }

  async getErrorMessage(): Promise<string> {
    return await this.errorAlert.textContent() || '';
  }
}
