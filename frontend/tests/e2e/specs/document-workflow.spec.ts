import { test, expect } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs/promises';

/**
 * E2E Test: Complete Document Workflow
 * Tests: Upload → Archive → Search → Download
 *
 * This test verifies the complete document lifecycle in the JiVS platform,
 * including file upload, archiving with compression, searching, and downloading.
 */

const TEST_FILES_DIR = path.join(__dirname, '../../test-files');
const DOWNLOAD_DIR = path.join(__dirname, '../../../.playwright-mcp');

test.describe('Document Workflow', () => {

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await page.goto('http://localhost:3001/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    // Wait for redirect to dashboard
    await page.waitForURL('**/dashboard');

    // Navigate to Documents tab
    await page.click('text=Documents');
    await page.waitForURL('**/documents');
  });

  test('Complete document workflow: Upload → Archive → Search → Download', async ({ page }) => {
    const testFilename = 'test-document.pdf';
    const testTitle = 'E2E Test Document';
    const testDescription = 'This is a test document for E2E testing';
    const testTags = 'test,e2e,automation';

    // ==========================================
    // Step 1: Upload Document
    // ==========================================
    console.log('Step 1: Uploading document...');

    await page.click('button:has-text("Upload")');

    // Wait for upload dialog
    await expect(page.locator('text=Upload Document')).toBeVisible();

    // Select file
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles(path.join(TEST_FILES_DIR, testFilename));

    // Fill metadata
    await page.fill('input[name="title"]', testTitle);
    await page.fill('textarea[name="description"]', testDescription);
    await page.fill('input[name="tags"]', testTags);

    // Click upload button
    await page.click('button:has-text("Upload"):visible');

    // Wait for success message
    await expect(page.locator('text=Document uploaded successfully')).toBeVisible({ timeout: 30000 });

    console.log('✅ Document uploaded successfully');

    // ==========================================
    // Step 2: Verify Document in Active Tab
    // ==========================================
    console.log('Step 2: Verifying document appears in active list...');

    // Should be on Active tab by default
    await expect(page.locator(`text="${testTitle}"`)).toBeVisible();

    // Verify metadata is displayed
    await expect(page.locator(`text="${testDescription}"`)).toBeVisible();

    console.log('✅ Document visible in active list');

    // ==========================================
    // Step 3: Archive Document
    // ==========================================
    console.log('Step 3: Archiving document...');

    // Find the document row and click archive button
    const documentRow = page.locator(`tr:has-text("${testTitle}")`);
    await documentRow.locator('button[aria-label="Archive"]').click();

    // Wait for archive dialog
    await expect(page.locator('text=Archive Document')).toBeVisible();

    // Select compression and storage tier
    await page.check('input[name="compress"]');
    await page.selectOption('select[name="storageTier"]', 'WARM');

    // Confirm archiving
    await page.click('button:has-text("Archive"):visible');

    // Wait for success message
    await expect(page.locator('text=archived successfully')).toBeVisible({ timeout: 30000 });

    console.log('✅ Document archived successfully');

    // ==========================================
    // Step 4: Verify Document in Archived Tab
    // ==========================================
    console.log('Step 4: Verifying document in archived tab...');

    // Switch to Archived tab
    await page.click('button[role="tab"]:has-text("Archived")');

    // Wait for tab content to load
    await page.waitForTimeout(1000);

    // Verify document appears in archived list
    await expect(page.locator(`text="${testTitle}"`)).toBeVisible();

    // Verify it has archive indicator (compressed badge, storage tier, etc.)
    await expect(documentRow.locator('text=WARM')).toBeVisible();
    await expect(documentRow.locator('text=Compressed')).toBeVisible();

    console.log('✅ Document visible in archived list with correct metadata');

    // ==========================================
    // Step 5: Search for Document
    // ==========================================
    console.log('Step 5: Searching for document...');

    // Enter search query
    const searchInput = page.locator('input[placeholder*="Search"]');
    await searchInput.fill('E2E Test');
    await searchInput.press('Enter');

    // Wait for search results
    await page.waitForTimeout(1000);

    // Verify document appears in search results
    await expect(page.locator(`text="${testTitle}"`)).toBeVisible();

    console.log('✅ Document found in search results');

    // ==========================================
    // Step 6: Download Document
    // ==========================================
    console.log('Step 6: Downloading document...');

    // Find download button and click it
    const downloadPromise = page.waitForEvent('download');
    await documentRow.locator('button[aria-label="Download"]').click();
    const download = await downloadPromise;

    // Verify filename
    expect(download.suggestedFilename()).toBe(testFilename);

    // Save download
    const downloadPath = path.join(DOWNLOAD_DIR, download.suggestedFilename());
    await download.saveAs(downloadPath);

    // Verify file was downloaded and has content
    const stats = await fs.stat(downloadPath);
    expect(stats.size).toBeGreaterThan(0);

    console.log(`✅ Document downloaded successfully (${stats.size} bytes)`);

    // ==========================================
    // Step 7: Verify Downloaded File Integrity
    // ==========================================
    console.log('Step 7: Verifying file integrity...');

    // Read original and downloaded files
    const originalContent = await fs.readFile(path.join(TEST_FILES_DIR, testFilename));
    const downloadedContent = await fs.readFile(downloadPath);

    // Compare file sizes
    expect(downloadedContent.length).toBe(originalContent.length);

    // Compare file contents (checksum)
    const crypto = require('crypto');
    const originalChecksum = crypto.createHash('sha256').update(originalContent).digest('hex');
    const downloadedChecksum = crypto.createHash('sha256').update(downloadedContent).digest('hex');
    expect(downloadedChecksum).toBe(originalChecksum);

    console.log('✅ File integrity verified (checksums match)');

    // ==========================================
    // Step 8: Restore Document from Archive
    // ==========================================
    console.log('Step 8: Restoring document from archive...');

    // Navigate back to Archived tab
    await page.click('button[role="tab"]:has-text("Archived")');
    await page.waitForTimeout(1000);

    // Click restore button
    await documentRow.locator('button[aria-label="Restore"]').click();

    // Wait for confirmation dialog
    await expect(page.locator('text=Restore Document')).toBeVisible();
    await page.click('button:has-text("Restore"):visible');

    // Wait for success message
    await expect(page.locator('text=restored successfully')).toBeVisible({ timeout: 30000 });

    console.log('✅ Document restored successfully');

    // ==========================================
    // Step 9: Verify Document Back in Active Tab
    // ==========================================
    console.log('Step 9: Verifying document restored to active list...');

    // Switch to Active tab
    await page.click('button[role="tab"]:has-text("Active")');
    await page.waitForTimeout(1000);

    // Verify document appears
    await expect(page.locator(`text="${testTitle}"`)).toBeVisible();

    // Verify it no longer shows as compressed (decompressed on restore)
    await expect(documentRow.locator('text=HOT')).toBeVisible();

    console.log('✅ Document visible in active list (unarchived)');

    // ==========================================
    // Step 10: Delete Document (Cleanup)
    // ==========================================
    console.log('Step 10: Deleting document (cleanup)...');

    // Click delete button
    await documentRow.locator('button[aria-label="Delete"]').click();

    // Confirm deletion
    await expect(page.locator('text=Delete Document')).toBeVisible();
    await page.click('button:has-text("Delete"):visible');

    // Wait for success message
    await expect(page.locator('text=deleted successfully')).toBeVisible({ timeout: 30000 });

    // Verify document no longer appears in list
    await expect(page.locator(`text="${testTitle}"`)).not.toBeVisible();

    console.log('✅ Document deleted successfully');

    // Clean up downloaded file
    try {
      await fs.unlink(downloadPath);
    } catch (error) {
      console.warn('Could not delete downloaded file:', error);
    }
  });

  test('Bulk archive workflow', async ({ page }) => {
    console.log('Testing bulk archive operation...');

    // Upload 3 test files
    const testFiles = ['test1.pdf', 'test2.docx', 'test3.png'];

    for (const filename of testFiles) {
      await page.click('button:has-text("Upload")');
      await page.locator('input[type="file"]').setInputFiles(path.join(TEST_FILES_DIR, filename));
      await page.fill('input[name="title"]', `Bulk Test ${filename}`);
      await page.click('button:has-text("Upload"):visible');
      await expect(page.locator('text=uploaded successfully')).toBeVisible({ timeout: 30000 });
      await page.waitForTimeout(500);
    }

    console.log('✅ 3 documents uploaded');

    // Select all documents
    await page.check('input[type="checkbox"][aria-label="Select all"]');

    // Click bulk archive
    await page.click('button:has-text("Archive Selected")');

    // Configure bulk archive
    await expect(page.locator('text=Archive Documents')).toBeVisible();
    await page.check('input[name="compress"]');
    await page.selectOption('select[name="storageTier"]', 'WARM');
    await page.click('button:has-text("Archive All"):visible');

    // Wait for completion
    await expect(page.locator('text=3 document(s) archived successfully')).toBeVisible({ timeout: 60000 });

    console.log('✅ Bulk archive completed');

    // Verify all in Archived tab
    await page.click('button[role="tab"]:has-text("Archived")');
    await page.waitForTimeout(1000);

    for (const filename of testFiles) {
      await expect(page.locator(`text="${filename}"`)).toBeVisible();
    }

    console.log('✅ All documents appear in archived list');

    // Cleanup: Delete all
    await page.check('input[type="checkbox"][aria-label="Select all"]');
    await page.click('button:has-text("Delete Selected")');
    await page.click('button:has-text("Delete All"):visible');
    await expect(page.locator('text=deleted successfully')).toBeVisible({ timeout: 30000 });

    console.log('✅ Cleanup completed');
  });

  test('Large file upload with progress tracking', async ({ page }) => {
    console.log('Testing large file upload...');

    const largeFilename = 'large-document.pdf'; // 50MB+ test file

    await page.click('button:has-text("Upload")');
    await page.locator('input[type="file"]').setInputFiles(path.join(TEST_FILES_DIR, largeFilename));
    await page.fill('input[name="title"]', 'Large File Test');

    // Click upload
    await page.click('button:has-text("Upload"):visible');

    // Verify progress bar appears
    await expect(page.locator('[role="progressbar"]')).toBeVisible();

    // Wait for completion (longer timeout for large file)
    await expect(page.locator('text=uploaded successfully')).toBeVisible({ timeout: 120000 });

    console.log('✅ Large file uploaded successfully');

    // Cleanup
    const documentRow = page.locator('tr:has-text("Large File Test")');
    await documentRow.locator('button[aria-label="Delete"]').click();
    await page.click('button:has-text("Delete"):visible');
    await expect(page.locator('text=deleted successfully')).toBeVisible({ timeout: 30000 });
  });

  test('Search with advanced filters', async ({ page }) => {
    console.log('Testing advanced search filters...');

    // Open advanced search
    await page.click('button[aria-label="Advanced Search"]');

    // Fill advanced filters
    await page.fill('input[name="query"]', 'test');
    await page.selectOption('select[name="fileType"]', 'pdf');
    await page.fill('input[name="author"]', 'admin');
    await page.fill('input[name="tags"]', 'test,automation');
    await page.selectOption('select[name="storageTier"]', 'WARM');

    // Execute search
    await page.click('button:has-text("Search")');

    // Verify results filtered correctly
    await page.waitForTimeout(1000);
    const results = page.locator('tbody tr');
    const count = await results.count();

    if (count > 0) {
      // Verify all results match filters
      for (let i = 0; i < count; i++) {
        const row = results.nth(i);
        await expect(row.locator('text=PDF')).toBeVisible();
        await expect(row.locator('text=WARM')).toBeVisible();
      }
      console.log(`✅ Search returned ${count} filtered results`);
    } else {
      console.log('✅ Search returned no results (filters too restrictive)');
    }
  });
});
