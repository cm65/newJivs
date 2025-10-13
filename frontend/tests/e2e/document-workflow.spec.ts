import { test, expect } from '@playwright/test';

/**
 * Document Workflow E2E Test
 * Tests: Upload → Archive → Search workflow
 */
test.describe('Document Management Workflow', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to login page
    await page.goto('http://localhost:3001/login');

    // Login
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    // Wait for navigation to dashboard
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Navigate to Documents page
    await page.goto('http://localhost:3001/documents');
    await page.waitForLoadState('networkidle');
  });

  test('Complete workflow: Upload → Archive → Search', async ({ page }) => {
    console.log('Starting document workflow test...');

    // Step 1: Verify Documents page loaded
    await expect(page.locator('h4:has-text("Document Management")')).toBeVisible();
    console.log('✓ Documents page loaded');

    // Wait a bit for user to see the page
    await page.waitForTimeout(1500);

    // Step 2: Click Upload Document button
    await page.click('button:has-text("Upload Document")');
    await expect(page.locator('h2:has-text("Upload Document")')).toBeVisible();
    console.log('✓ Upload dialog opened');

    await page.waitForTimeout(1000);

    // Step 3: Upload file
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('/tmp/test-document.txt');
    console.log('✓ File selected');

    await page.waitForTimeout(1000);

    // Step 4: Fill in document metadata
    // Use getByLabel for Material-UI TextFields
    await page.getByLabel('Title').fill('JiVS Test Document');
    await page.getByLabel('Description').fill('End-to-end test for document archiving feature');
    await page.getByPlaceholder('tag1, tag2, tag3').fill('test, jivs, e2e');
    console.log('✓ Metadata filled');

    await page.waitForTimeout(1000);

    // Step 5: Click Upload button (inside dialog, bypass Material-UI overlay)
    // Use getByRole within dialog to be specific, and force click to bypass overlay
    await page.getByRole('dialog').getByRole('button', { name: 'Upload' }).click({ force: true });
    console.log('✓ Upload button clicked');

    // Wait for upload to complete (look for success message)
    await page.waitForSelector('text=Document uploaded successfully', { timeout: 10000 });
    console.log('✓ Document uploaded successfully');

    await page.waitForTimeout(2000);

    // Step 6: Verify document appears in Active Documents list
    await expect(page.locator('text=test-document.txt').first()).toBeVisible();
    console.log('✓ Document visible in active list');

    await page.waitForTimeout(1500);

    // Step 7: Select the document for archiving
    const documentRow = page.locator('tr:has-text("test-document.txt")');
    await documentRow.locator('input[type="checkbox"]').first().check();
    console.log('✓ Document selected');

    await page.waitForTimeout(1000);

    // Step 8: Click Archive button
    await page.click('button:has-text("Archive Selected")');
    await expect(page.locator('h2:has-text("Archive Documents")')).toBeVisible();
    console.log('✓ Archive dialog opened');

    await page.waitForTimeout(1500);

    // Step 9: Configure archive settings
    // Click the Archive Tier combobox
    await page.locator('[role="combobox"]:has-text("Hot - Frequently accessed")').click();
    await page.waitForTimeout(500);
    await page.click('li:has-text("Cold - Rarely accessed")');
    await page.waitForTimeout(500);
    await page.getByLabel('Archive Reason').fill('Test archiving for E2E validation');
    console.log('✓ Archive settings configured');

    await page.waitForTimeout(1000);

    // Step 10: Click Archive button in dialog (use force to bypass overlay)
    await page.getByRole('dialog').getByRole('button', { name: 'Archive' }).click({ force: true });
    console.log('✓ Archive initiated');

    // Wait for archive success message
    await page.waitForSelector('text=archived successfully', { timeout: 10000 });
    console.log('✓ Document archived successfully');

    await page.waitForTimeout(2000);

    // Step 11: Switch to Archived Documents tab
    await page.click('button:has-text("Archived Documents")');
    console.log('✓ Switched to archived tab');

    await page.waitForTimeout(2000);

    // Step 12: Verify document appears in Archived list
    await expect(page.locator('text=test-document.txt')).toBeVisible();
    console.log('✓ Document visible in archived list');

    await page.waitForTimeout(1500);

    // Step 13: Test search functionality
    await page.fill('input[placeholder="Search documents..."]', 'test');
    console.log('✓ Search query entered');

    await page.waitForTimeout(1000);

    // Step 14: Click Search button
    await page.click('button:has-text("Search")');
    console.log('✓ Search initiated');

    await page.waitForTimeout(2000);

    // Step 15: Verify search results
    await expect(page.locator('text=test-document.txt')).toBeVisible();
    console.log('✓ Document found in search results');

    await page.waitForTimeout(2000);

    // Step 16: Verify document statistics updated
    const totalDocs = page.locator('text=Total Documents').locator('xpath=following-sibling::*');
    await expect(totalDocs).toContainText('1');
    console.log('✓ Statistics updated correctly');

    console.log('\n✅ Complete workflow test PASSED!');
    console.log('   - Document uploaded');
    console.log('   - Document archived');
    console.log('   - Document searchable');

    // Keep browser open for a few seconds so user can see final state
    await page.waitForTimeout(3000);
  });

  test('Verify document details and actions', async ({ page }) => {
    console.log('Starting document details test...');

    // Wait for page to load
    await page.waitForTimeout(1500);

    // If there are documents, test the action buttons
    const hasDocuments = await page.locator('table tr').count() > 1;

    if (hasDocuments) {
      console.log('✓ Documents found, testing actions...');

      // Check that action buttons are visible (IconButtons with SVG icons)
      await expect(page.getByTitle('Download').first()).toBeVisible();
      console.log('✓ Download button visible');

      await expect(page.getByTitle('Archive').first()).toBeVisible();
      console.log('✓ Archive button visible');

      await page.waitForTimeout(2000);
    } else {
      console.log('ℹ No documents to test actions on');
    }

    console.log('✅ Document details test PASSED!');

    await page.waitForTimeout(2000);
  });
});
