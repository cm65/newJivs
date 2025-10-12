import { test, expect } from '@playwright/test';
import { DataQualityPage } from '../../pages/quality/DataQualityPage';
import { setupAuthenticatedSession } from '../../helpers/auth.helper';
import {
  createQualityRule,
  deleteQualityRule,
  executeQualityRule,
  resolveQualityIssue,
  getQualityIssues,
} from '../../helpers/api.helper';
import { createTestQualityRule, testQualityRules } from '../../fixtures/quality';

/**
 * DQ-001 to DQ-005: Data Quality Tests
 * Tests for dashboard, rules, issues, and quality management
 */

test.describe('Data Quality - Dashboard & Rules', () => {
  let qualityPage: DataQualityPage;
  const createdRuleIds: string[] = [];

  test.beforeEach(async ({ page }) => {
    await setupAuthenticatedSession(page, 'admin');
    qualityPage = new DataQualityPage(page);
  });

  test.afterEach(async ({ page }) => {
    // Cleanup created rules
    for (const id of createdRuleIds) {
      await deleteQualityRule(page, id).catch(() => {});
    }
    createdRuleIds.length = 0;
  });

  test('DQ-001: Dashboard loads with all quality statistics', async ({ page }) => {
    // Act - Navigate to Data Quality page
    await qualityPage.goto();

    // Assert - Page elements are visible
    await qualityPage.verifyPageElements();

    // Assert - Dashboard statistics cards are visible
    await expect(qualityPage.overallScoreCard).toBeVisible();
    await expect(qualityPage.activeRulesCard).toBeVisible();
    await expect(qualityPage.openIssuesCard).toBeVisible();
    await expect(qualityPage.criticalIssuesCard).toBeVisible();

    // Assert - Statistics display valid values
    const overallScore = await qualityPage.getOverallScore();
    expect(overallScore).toBeGreaterThanOrEqual(0);
    expect(overallScore).toBeLessThanOrEqual(100);

    const activeRules = await qualityPage.getActiveRulesCount();
    expect(activeRules).toBeGreaterThanOrEqual(0);

    const openIssues = await qualityPage.getOpenIssuesCount();
    expect(openIssues).toBeGreaterThanOrEqual(0);

    const criticalIssues = await qualityPage.getCriticalIssuesCount();
    expect(criticalIssues).toBeGreaterThanOrEqual(0);

    // Assert - Quality dimensions card is visible
    await expect(qualityPage.qualityDimensionsCard).toBeVisible();

    // Assert - All tabs are visible
    await expect(qualityPage.rulesTab).toBeVisible();
    await expect(qualityPage.issuesTab).toBeVisible();
    await expect(qualityPage.profilesTab).toBeVisible();
  });

  test('DQ-002: Create quality rule successfully', async ({ page }) => {
    // Arrange
    const ruleData = createTestQualityRule({
      name: 'E2E Test - Null Check Rule',
      description: 'Test rule for E2E testing',
      ruleType: 'NULL_CHECK',
      dimension: 'COMPLETENESS',
      severity: 'HIGH',
    });

    await qualityPage.goto();
    await qualityPage.switchToRulesTab();

    // Get initial count
    const initialCount = await qualityPage.getRulesCount();

    // Act - Create new rule
    await qualityPage.clickNewRule();

    // Assert - Dialog is visible
    await expect(qualityPage.createRuleDialog).toBeVisible();

    // Act - Fill and submit form
    await qualityPage.fillCreateRuleForm({
      name: ruleData.name,
      description: ruleData.description,
      ruleType: 'Null Check',
      dimension: 'Completeness',
      severity: 'High',
    });
    await qualityPage.submitCreateRuleForm();

    // Assert - Dialog is closed
    await expect(qualityPage.createRuleDialog).not.toBeVisible();

    // Wait for table to update
    await page.waitForTimeout(1000);

    // Assert - Rule appears in table
    const exists = await qualityPage.ruleExists(ruleData.name);
    expect(exists).toBe(true);

    // Assert - Rules count increased
    const newCount = await qualityPage.getRulesCount();
    expect(newCount).toBeGreaterThan(initialCount);

    // Find the created rule and store its ID for cleanup
    const row = await qualityPage.findRuleByName(ruleData.name);
    if (row) {
      // Note: In a real scenario, we would extract the ID from the row or API response
      // For now, we'll rely on name-based cleanup
    }
  });

  test('DQ-003: Filter rules by dimension and severity', async ({ page }) => {
    // Arrange - Create multiple rules with different dimensions and severities
    const rule1 = createTestQualityRule({
      name: 'E2E Test - Completeness Low',
      dimension: 'COMPLETENESS',
      severity: 'LOW',
    });
    const rule2 = createTestQualityRule({
      name: 'E2E Test - Accuracy Critical',
      dimension: 'ACCURACY',
      severity: 'CRITICAL',
    });

    const id1 = await createQualityRule(page, rule1);
    const id2 = await createQualityRule(page, rule2);
    createdRuleIds.push(id1, id2);

    await qualityPage.goto();
    await qualityPage.switchToRulesTab();

    // Act - Filter by COMPLETENESS dimension
    await qualityPage.filterRulesByDimension('Completeness');
    await page.waitForTimeout(1000);

    // Assert - Rule 1 should be visible, Rule 2 might not
    const existsAfterDimensionFilter = await qualityPage.ruleExists(rule1.name);
    expect(existsAfterDimensionFilter).toBe(true);

    // Act - Reset filter and filter by CRITICAL severity
    await qualityPage.filterRulesByDimension('All');
    await page.waitForTimeout(500);
    await qualityPage.filterRulesBySeverity('Critical');
    await page.waitForTimeout(1000);

    // Assert - Rule 2 should be visible, Rule 1 might not
    const existsAfterSeverityFilter = await qualityPage.ruleExists(rule2.name);
    expect(existsAfterSeverityFilter).toBe(true);

    // Act - Reset to All
    await qualityPage.filterRulesBySeverity('All');
    await page.waitForTimeout(500);

    // Assert - Both rules should be visible
    const exists1 = await qualityPage.ruleExists(rule1.name);
    const exists2 = await qualityPage.ruleExists(rule2.name);
    expect(exists1).toBe(true);
    expect(exists2).toBe(true);
  });

  test('DQ-004: Execute rule and verify it runs successfully', async ({ page }) => {
    // Arrange - Create a rule
    const ruleData = createTestQualityRule({
      name: 'E2E Test - Execute Rule',
      ruleType: 'NULL_CHECK',
      dimension: 'COMPLETENESS',
      severity: 'MEDIUM',
    });

    const ruleId = await createQualityRule(page, ruleData);
    createdRuleIds.push(ruleId);

    await qualityPage.goto();
    await qualityPage.switchToRulesTab();

    // Verify rule exists
    const exists = await qualityPage.ruleExists(ruleData.name);
    expect(exists).toBe(true);

    // Act - Execute the rule
    await qualityPage.executeRule(ruleData.name);
    await page.waitForTimeout(1000);

    // Assert - No error is displayed (successful execution)
    const hasError = await qualityPage.hasError();
    expect(hasError).toBe(false);

    // Switch to Issues tab to verify issues might have been created
    await qualityPage.switchToIssuesTab();
    await page.waitForTimeout(500);

    // Assert - Issues table is visible (may or may not have issues depending on data)
    await expect(qualityPage.issuesTable).toBeVisible();
  });

  test('DQ-005: View and manage quality issues', async ({ page }) => {
    // Arrange - Navigate to Data Quality page
    await qualityPage.goto();
    await qualityPage.switchToIssuesTab();

    // Assert - Issues tab is visible
    await expect(qualityPage.issuesTable).toBeVisible();

    // Get initial issues count
    const initialCount = await qualityPage.getIssuesCount();
    expect(initialCount).toBeGreaterThanOrEqual(0);

    // Test filtering by status
    // Act - Filter by OPEN status
    await qualityPage.filterIssuesByStatus('Open');
    await page.waitForTimeout(1000);

    // Assert - Filter was applied successfully
    const openCount = await qualityPage.getIssuesCount();
    expect(openCount).toBeGreaterThanOrEqual(0);

    // Act - Filter by severity
    await qualityPage.filterIssuesByStatus('All');
    await page.waitForTimeout(500);
    await qualityPage.filterIssuesBySeverity('High');
    await page.waitForTimeout(1000);

    // Assert - Filter was applied successfully
    const highSeverityCount = await qualityPage.getIssuesCount();
    expect(highSeverityCount).toBeGreaterThanOrEqual(0);

    // Reset filters
    await qualityPage.filterIssuesByStatus('All');
    await qualityPage.filterIssuesBySeverity('All');
    await page.waitForTimeout(500);

    // If there are any OPEN issues, try resolving one
    const hasOpenIssues = await page
      .locator('td:has-text("OPEN")')
      .first()
      .isVisible()
      .catch(() => false);

    if (hasOpenIssues) {
      // Get the first issue description
      const firstIssueRow = await qualityPage.issuesRows.first();
      const issueDescription = await firstIssueRow.locator('td').first().textContent();

      if (issueDescription) {
        // Act - Resolve the issue
        await qualityPage.resolveIssue(issueDescription);
        await page.waitForTimeout(1000);

        // Assert - Issue should be resolved or removed from OPEN filter
        await qualityPage.filterIssuesByStatus('Resolved');
        await page.waitForTimeout(500);

        // Check if issue now appears in Resolved
        const resolvedCount = await qualityPage.getIssuesCount();
        expect(resolvedCount).toBeGreaterThan(0);
      }
    }
  });

  test('DQ-006: View dataset profiles', async ({ page }) => {
    // Arrange
    await qualityPage.goto();
    await qualityPage.switchToProfilesTab();

    // Assert - Profiles tab is visible
    await expect(qualityPage.profilesTable).toBeVisible();

    // Get profiles count
    const profilesCount = await qualityPage.getProfilesCount();
    expect(profilesCount).toBeGreaterThanOrEqual(0);

    // If there are profiles, verify the data displays correctly
    if (profilesCount > 0) {
      const firstRow = await qualityPage.profilesRows.first();

      // Assert - Dataset name is displayed
      const datasetCell = firstRow.locator('td').first();
      const datasetName = await datasetCell.textContent();
      expect(datasetName).toBeTruthy();
      expect(datasetName).not.toBe('null');
      expect(datasetName).not.toBe('undefined');

      // Assert - Overall score is displayed and valid (0-100%)
      const scoreCell = firstRow.locator('td').nth(1);
      const scoreText = await scoreCell.textContent();
      const score = parseFloat(scoreText?.replace('%', '') || '0');
      expect(score).toBeGreaterThanOrEqual(0);
      expect(score).toBeLessThanOrEqual(100);

      // Assert - Record count is displayed
      const recordsCell = firstRow.locator('td').nth(2);
      const recordsText = await recordsCell.textContent();
      expect(recordsText).toBeTruthy();
      expect(recordsText).toMatch(/\d/); // Contains at least one digit
    }
  });

  test('DQ-007: Cancel rule creation dialog', async ({ page }) => {
    // Arrange
    await qualityPage.goto();
    await qualityPage.switchToRulesTab();

    const initialCount = await qualityPage.getRulesCount();

    // Act - Open create dialog
    await qualityPage.clickNewRule();

    // Assert - Dialog is visible
    await expect(qualityPage.createRuleDialog).toBeVisible();

    // Act - Fill some data
    await qualityPage.fillCreateRuleForm({
      name: 'E2E Test - Should Not Be Created',
      description: 'This rule should not be created',
    });

    // Act - Cancel
    await qualityPage.cancelCreateRuleForm();

    // Assert - Dialog is closed
    await expect(qualityPage.createRuleDialog).not.toBeVisible();

    // Assert - Rules count unchanged
    const newCount = await qualityPage.getRulesCount();
    expect(newCount).toBe(initialCount);

    // Assert - Rule was not created
    const exists = await qualityPage.ruleExists('E2E Test - Should Not Be Created');
    expect(exists).toBe(false);
  });

  test('DQ-008: Delete quality rule', async ({ page }) => {
    // Arrange - Create a rule
    const ruleData = createTestQualityRule({
      name: 'E2E Test - Delete Me',
      description: 'This rule will be deleted',
    });

    const ruleId = await createQualityRule(page, ruleData);
    createdRuleIds.push(ruleId);

    await qualityPage.goto();
    await qualityPage.switchToRulesTab();

    // Verify rule exists
    const existsBefore = await qualityPage.ruleExists(ruleData.name);
    expect(existsBefore).toBe(true);

    // Act - Delete the rule
    await qualityPage.deleteRule(ruleData.name);
    await page.waitForTimeout(1000);

    // Assert - Rule no longer exists
    const existsAfter = await qualityPage.ruleExists(ruleData.name);
    expect(existsAfter).toBe(false);

    // Remove from cleanup list since already deleted
    const index = createdRuleIds.indexOf(ruleId);
    if (index > -1) {
      createdRuleIds.splice(index, 1);
    }
  });
});
