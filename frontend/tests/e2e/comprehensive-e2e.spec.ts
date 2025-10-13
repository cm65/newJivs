import { test, expect, Page } from '@playwright/test';

/**
 * Comprehensive E2E Test Suite
 *
 * These tests validate complete user journeys across the JiVS platform,
 * including visual regression testing to catch UI bugs.
 *
 * Test Coverage:
 * 1. Complete data pipeline workflow
 * 2. GDPR compliance workflow
 * 3. Multi-user collaboration scenarios
 * 4. Error recovery flows
 * 5. Performance under realistic usage
 * 6. Visual regression for all pages
 */

// Test configuration
test.use({
  // Enable video recording for debugging
  video: 'on-first-retry',
  // Enable tracing for debugging
  trace: 'on-first-retry',
  // Set viewport for consistent screenshots
  viewport: { width: 1920, height: 1080 },
});

// Helper functions
async function login(page: Page, username: string = 'admin', password: string = 'password') {
  await page.goto('http://localhost:3001/login');
  await page.fill('[name="username"]', username);
  await page.fill('[name="password"]', password);
  await page.click('button[type="submit"]');
  await page.waitForURL('**/dashboard');
}

async function waitForToast(page: Page, message: string) {
  await page.waitForSelector(`text="${message}"`, { timeout: 5000 });
}

test.describe('Complete Data Pipeline Workflow', () => {
  test('should complete end-to-end data pipeline from extraction to migration', async ({ page }) => {
    await login(page);

    // Step 1: Create extraction
    await page.goto('http://localhost:3001/extractions');
    await page.click('button:has-text("New Extraction")');

    // Fill extraction form
    await page.fill('[name="name"]', 'E2E Test Extraction');
    await page.selectOption('[name="sourceType"]', 'JDBC');
    await page.fill('[name="connectionUrl"]', 'jdbc:postgresql://localhost:5432/source_db');
    await page.fill('[name="username"]', 'test_user');
    await page.fill('[name="password"]', 'test_pass');
    await page.fill('[name="query"]', 'SELECT * FROM customers WHERE created_at > NOW() - INTERVAL \'7 days\'');

    // Visual regression: Capture extraction dialog
    await expect(page.locator('.MuiDialog-root')).toHaveScreenshot('extraction-dialog.png');

    await page.click('button:has-text("Create")');
    await waitForToast(page, 'Extraction created successfully');

    // Start extraction
    await page.click('button[aria-label="Start extraction"]:first');
    await waitForToast(page, 'Extraction started');

    // Wait for extraction to complete
    await page.waitForSelector('text=COMPLETED', { timeout: 30000 });

    // Step 2: Review data quality
    await page.goto('http://localhost:3001/data-quality');

    // Visual regression: Capture data quality dashboard
    await expect(page).toHaveScreenshot('data-quality-dashboard.png', {
      fullPage: true,
      animations: 'disabled',
    });

    // Create quality rule
    await page.click('button:has-text("New Rule")');
    await page.fill('[name="name"]', 'Email Validation Rule');
    await page.selectOption('[name="dimension"]', 'VALIDITY');
    await page.fill('[name="expression"]', 'email LIKE \'%@%.%\'');
    await page.selectOption('[name="severity"]', 'HIGH');
    await page.click('button:has-text("Create")');

    // Execute rule
    await page.click('button[aria-label="Execute rule"]:first');
    await waitForToast(page, 'Rule execution started');

    // Step 3: Create migration
    await page.goto('http://localhost:3001/migrations');
    await page.click('button:has-text("New Migration")');

    // Fill migration form
    await page.fill('[name="name"]', 'E2E Test Migration');
    await page.selectOption('[name="sourceExtraction"]', 'E2E Test Extraction');
    await page.fill('[name="targetDatabase"]', 'jdbc:postgresql://localhost:5432/target_db');
    await page.fill('[name="targetTable"]', 'customers_migrated');

    // Add transformation rule
    await page.click('button:has-text("Add Transformation")');
    await page.fill('[name="transformations[0].field"]', 'email');
    await page.fill('[name="transformations[0].expression"]', 'LOWER(email)');

    await page.click('button:has-text("Create")');
    await waitForToast(page, 'Migration created successfully');

    // Start migration
    await page.click('button[aria-label="Start migration"]:first');

    // Monitor progress
    await page.waitForSelector('[role="progressbar"][aria-valuenow="100"]', { timeout: 60000 });

    // Visual regression: Capture completed migration
    await expect(page.locator('tr:first-child')).toHaveScreenshot('completed-migration-row.png');

    // Step 4: Verify analytics
    await page.goto('http://localhost:3001/analytics');

    // Check metrics updated
    await expect(page.locator('text=Total Extractions')).toBeVisible();
    await expect(page.locator('text=Total Migrations')).toBeVisible();

    // Visual regression: Capture analytics dashboard
    await expect(page).toHaveScreenshot('analytics-dashboard-after-pipeline.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });
});

test.describe('GDPR Compliance Workflow', () => {
  test('should process GDPR data subject request end-to-end', async ({ page }) => {
    await login(page);

    await page.goto('http://localhost:3001/compliance');

    // Create data subject request
    await page.click('button:has-text("New Request")');

    // Fill request form
    await page.fill('[name="subjectEmail"]', 'gdpr.test@example.com');
    await page.selectOption('[name="requestType"]', 'ERASURE');
    await page.selectOption('[name="regulation"]', 'GDPR');
    await page.fill('[name="description"]', 'Please delete all my personal data as per GDPR Article 17');

    // Visual regression: Capture GDPR request form
    await expect(page.locator('.MuiDialog-root')).toHaveScreenshot('gdpr-request-form.png');

    await page.click('button:has-text("Submit")');
    await waitForToast(page, 'Request submitted successfully');

    // Verify request appears in list
    await expect(page.locator('td:has-text("gdpr.test@example.com")')).toBeVisible();

    // Process request
    await page.click('button[aria-label="Process request"]:first');

    // Confirm processing
    await page.click('button:has-text("Confirm")');
    await waitForToast(page, 'Processing started');

    // Wait for processing to complete
    await page.waitForSelector('text=COMPLETED', { timeout: 30000 });

    // Download compliance report
    await page.click('button[aria-label="Download report"]:first');

    // Verify download started
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.click('button:has-text("Export PDF")'),
    ]);

    expect(download.suggestedFilename()).toContain('gdpr-compliance-report');
  });

  test('should handle CCPA consumer request with data portability', async ({ page }) => {
    await login(page);

    await page.goto('http://localhost:3001/compliance');

    // Create CCPA request
    await page.click('button:has-text("New Request")');
    await page.fill('[name="subjectEmail"]', 'ccpa.test@example.com');
    await page.selectOption('[name="requestType"]', 'PORTABILITY');
    await page.selectOption('[name="regulation"]', 'CCPA');
    await page.fill('[name="description"]', 'Export all my data in machine-readable format');
    await page.click('button:has-text("Submit")');

    // Process and export
    await page.click('button[aria-label="Process request"]:first');
    await page.click('button:has-text("Confirm")');

    // Wait for export to be ready
    await page.waitForSelector('button[aria-label="Download export"]', { timeout: 30000 });

    // Download exported data
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.click('button[aria-label="Download export"]:first'),
    ]);

    expect(download.suggestedFilename()).toContain('.json');
  });
});

test.describe('Multi-User Collaboration', () => {
  test('should handle concurrent edits from multiple users', async ({ browser }) => {
    // Create two browser contexts (simulating two users)
    const context1 = await browser.newContext();
    const context2 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();

    // User 1 logs in
    await login(page1, 'user1', 'password');

    // User 2 logs in
    await login(page2, 'user2', 'password');

    // Both users navigate to extractions
    await page1.goto('http://localhost:3001/extractions');
    await page2.goto('http://localhost:3001/extractions');

    // User 1 creates an extraction
    await page1.click('button:has-text("New Extraction")');
    await page1.fill('[name="name"]', 'User 1 Extraction');
    await page1.selectOption('[name="sourceType"]', 'JDBC');
    await page1.click('button:has-text("Create")');

    // User 2 should see the new extraction appear
    await page2.waitForSelector('text=User 1 Extraction', { timeout: 5000 });

    // User 2 creates an extraction
    await page2.click('button:has-text("New Extraction")');
    await page2.fill('[name="name"]', 'User 2 Extraction');
    await page2.selectOption('[name="sourceType"]', 'SAP');
    await page2.click('button:has-text("Create")');

    // User 1 should see User 2's extraction
    await page1.waitForSelector('text=User 2 Extraction', { timeout: 5000 });

    // Both users should see both extractions
    await expect(page1.locator('text=User 1 Extraction')).toBeVisible();
    await expect(page1.locator('text=User 2 Extraction')).toBeVisible();
    await expect(page2.locator('text=User 1 Extraction')).toBeVisible();
    await expect(page2.locator('text=User 2 Extraction')).toBeVisible();

    await context1.close();
    await context2.close();
  });

  test('should show real-time updates when another user modifies data', async ({ browser }) => {
    const context1 = await browser.newContext();
    const context2 = await browser.newContext();

    const page1 = await context1.newPage();
    const page2 = await context2.newPage();

    await login(page1, 'user1', 'password');
    await login(page2, 'user2', 'password');

    // Both view migrations
    await page1.goto('http://localhost:3001/migrations');
    await page2.goto('http://localhost:3001/migrations');

    // User 1 starts a migration
    const migrationName = `Migration ${Date.now()}`;
    await page1.click('button:has-text("New Migration")');
    await page1.fill('[name="name"]', migrationName);
    await page1.click('button:has-text("Create")');

    // User 2 sees the migration
    await page2.waitForSelector(`text=${migrationName}`);

    // User 1 starts the migration
    await page1.click(`tr:has-text("${migrationName}") button[aria-label="Start migration"]`);

    // User 2 should see status change
    await page2.waitForSelector(`tr:has-text("${migrationName}") text=RUNNING`, { timeout: 10000 });

    await context1.close();
    await context2.close();
  });
});

test.describe('Error Recovery Flows', () => {
  test('should gracefully handle network disconnection', async ({ page, context }) => {
    await login(page);
    await page.goto('http://localhost:3001/extractions');

    // Simulate network disconnection
    await context.setOffline(true);

    // Try to create extraction (should fail gracefully)
    await page.click('button:has-text("New Extraction")');
    await page.fill('[name="name"]', 'Offline Test');
    await page.click('button:has-text("Create")');

    // Should show error message
    await expect(page.locator('text=Network error')).toBeVisible({ timeout: 5000 });

    // Restore network
    await context.setOffline(false);

    // Retry should work
    await page.click('button:has-text("Retry")');
    await waitForToast(page, 'Extraction created successfully');
  });

  test('should handle server errors gracefully', async ({ page }) => {
    await login(page);

    // Navigate to page that will trigger server error
    // (Assuming we have a way to trigger this for testing)
    await page.goto('http://localhost:3001/migrations');

    // Create migration with invalid configuration to trigger error
    await page.click('button:has-text("New Migration")');
    await page.fill('[name="name"]', 'Error Test Migration');
    await page.fill('[name="sourceExtraction"]', 'INVALID_ID_TRIGGER_ERROR');
    await page.click('button:has-text("Create")');

    // Should show user-friendly error
    await expect(page.locator('text=Something went wrong')).toBeVisible();

    // Error details should be available
    await page.click('button:has-text("Show details")');
    await expect(page.locator('text=400')).toBeVisible();
  });

  test('should auto-save form data on connection loss', async ({ page, context }) => {
    await login(page);
    await page.goto('http://localhost:3001/migrations');

    // Start filling form
    await page.click('button:has-text("New Migration")');
    await page.fill('[name="name"]', 'Auto-save Test Migration');
    await page.selectOption('[name="sourceExtraction"]', { index: 1 });
    await page.fill('[name="targetDatabase"]', 'jdbc:postgresql://localhost:5432/target');

    // Simulate connection loss
    await context.setOffline(true);

    // Wait a moment for auto-save
    await page.waitForTimeout(2000);

    // Restore connection
    await context.setOffline(false);

    // Reload page
    await page.reload();

    // Open new migration dialog
    await page.click('button:has-text("New Migration")');

    // Check if data was restored
    await expect(page.locator('[name="name"]')).toHaveValue('Auto-save Test Migration');
    await expect(page.locator('[name="targetDatabase"]')).toHaveValue('jdbc:postgresql://localhost:5432/target');
  });
});

test.describe('Visual Regression Testing', () => {
  test('should match visual snapshot for all main pages', async ({ page }) => {
    await login(page);

    // Dashboard
    await page.goto('http://localhost:3001/dashboard');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('dashboard-full.png', {
      fullPage: true,
      animations: 'disabled',
      mask: [page.locator('.timestamp')], // Mask dynamic timestamps
    });

    // Extractions
    await page.goto('http://localhost:3001/extractions');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('extractions-full.png', {
      fullPage: true,
      animations: 'disabled',
    });

    // Migrations
    await page.goto('http://localhost:3001/migrations');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('migrations-full.png', {
      fullPage: true,
      animations: 'disabled',
    });

    // Data Quality
    await page.goto('http://localhost:3001/data-quality');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('data-quality-full.png', {
      fullPage: true,
      animations: 'disabled',
    });

    // Compliance
    await page.goto('http://localhost:3001/compliance');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('compliance-full.png', {
      fullPage: true,
      animations: 'disabled',
    });

    // Analytics
    await page.goto('http://localhost:3001/analytics');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveScreenshot('analytics-full.png', {
      fullPage: true,
      animations: 'disabled',
      mask: [page.locator('.chart')], // Mask dynamic charts
    });
  });

  test('should detect visual changes in components', async ({ page }) => {
    await login(page);
    await page.goto('http://localhost:3001/extractions');

    // Test table component
    const table = page.locator('table');
    await expect(table).toHaveScreenshot('extraction-table.png');

    // Test status chip variations
    const statusChips = page.locator('[data-testid="status-chip"]');
    for (let i = 0; i < await statusChips.count(); i++) {
      await expect(statusChips.nth(i)).toHaveScreenshot(`status-chip-${i}.png`);
    }

    // Test action buttons
    const actionButtons = page.locator('[data-testid="action-button"]');
    for (let i = 0; i < Math.min(3, await actionButtons.count()); i++) {
      await expect(actionButtons.nth(i)).toHaveScreenshot(`action-button-${i}.png`);
    }
  });

  test('should maintain consistent styling across themes', async ({ page }) => {
    await login(page);

    // Light theme
    await page.goto('http://localhost:3001/settings');
    await page.click('text=Light theme');
    await page.goto('http://localhost:3001/dashboard');
    await expect(page).toHaveScreenshot('dashboard-light-theme.png');

    // Dark theme (if implemented)
    await page.goto('http://localhost:3001/settings');
    await page.click('text=Dark theme');
    await page.goto('http://localhost:3001/dashboard');
    await expect(page).toHaveScreenshot('dashboard-dark-theme.png');
  });
});

test.describe('Performance Testing', () => {
  test('should load pages within performance budget', async ({ page }) => {
    await login(page);

    const pages = [
      '/dashboard',
      '/extractions',
      '/migrations',
      '/data-quality',
      '/compliance',
      '/analytics',
    ];

    for (const path of pages) {
      const startTime = Date.now();
      await page.goto(`http://localhost:3001${path}`);
      await page.waitForLoadState('networkidle');
      const loadTime = Date.now() - startTime;

      // Pages should load within 3 seconds
      expect(loadTime).toBeLessThan(3000);

      // Log performance metrics
      const metrics = await page.evaluate(() => {
        const perf = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
        return {
          domContentLoaded: perf.domContentLoadedEventEnd - perf.domContentLoadedEventStart,
          loadComplete: perf.loadEventEnd - perf.loadEventStart,
          firstContentfulPaint: performance.getEntriesByName('first-contentful-paint')[0]?.startTime,
        };
      });

      console.log(`Performance metrics for ${path}:`, metrics);

      // Assert performance budgets
      expect(metrics.firstContentfulPaint).toBeLessThan(1500);
    }
  });

  test('should handle large datasets efficiently', async ({ page }) => {
    await login(page);

    // Navigate to page with pagination
    await page.goto('http://localhost:3001/extractions');

    // Change to show 100 rows (if available)
    await page.click('[aria-label="Rows per page"]');
    await page.click('text=100');

    // Measure render time
    const startTime = Date.now();
    await page.waitForSelector('table tbody tr:nth-child(50)', { timeout: 5000 });
    const renderTime = Date.now() - startTime;

    // Should render large tables quickly
    expect(renderTime).toBeLessThan(2000);

    // Test smooth scrolling
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.evaluate(() => window.scrollTo(0, 0));

    // No visual glitches
    await expect(page).toHaveScreenshot('large-dataset-scroll.png');
  });
});

test.describe('Accessibility Testing', () => {
  test('should support keyboard navigation', async ({ page }) => {
    await login(page);
    await page.goto('http://localhost:3001/dashboard');

    // Tab through interactive elements
    await page.keyboard.press('Tab');
    await expect(page.locator(':focus')).toBeVisible();

    // Navigate to extractions using keyboard
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');

    await page.waitForURL('**/extractions');

    // Open dialog with keyboard
    await page.keyboard.press('Tab');
    await page.keyboard.press('Enter');

    // Dialog should be open
    await expect(page.locator('.MuiDialog-root')).toBeVisible();

    // Escape should close dialog
    await page.keyboard.press('Escape');
    await expect(page.locator('.MuiDialog-root')).not.toBeVisible();
  });

  test('should have proper ARIA labels', async ({ page }) => {
    await login(page);
    await page.goto('http://localhost:3001/extractions');

    // Check for ARIA labels
    await expect(page.locator('[aria-label="Create new extraction"]')).toBeVisible();
    await expect(page.locator('[aria-label="Refresh"]')).toBeVisible();
    await expect(page.locator('[role="table"]')).toBeVisible();

    // Check for screen reader announcements
    const liveRegion = page.locator('[aria-live="polite"]');
    await page.click('button:has-text("New Extraction")');
    await expect(liveRegion).toContainText(/dialog opened/i);
  });
});