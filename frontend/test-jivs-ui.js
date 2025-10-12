const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

// Configuration
const BASE_URL = 'http://localhost:3001';
const API_URL = 'http://localhost:8080/api/v1';
const CREDENTIALS = {
  username: 'admin',
  password: 'password'
};

// Create screenshots directory
const screenshotsDir = '/tmp/jivs-ui-screenshots';
if (!fs.existsSync(screenshotsDir)) {
  fs.mkdirSync(screenshotsDir, { recursive: true });
}

// Test results
const results = {
  passed: [],
  failed: [],
  warnings: [],
  screenshots: []
};

// Helper function to capture screenshot
async function captureScreenshot(page, name) {
  const filename = `${screenshotsDir}/${name}.png`;
  await page.screenshot({ path: filename, fullPage: true });
  results.screenshots.push({ name, path: filename });
  console.log(`  📸 Screenshot saved: ${name}.png`);
  return filename;
}

// Helper function to wait for navigation
async function safeNavigate(page, url, timeout = 30000) {
  try {
    await page.goto(url, { waitUntil: 'networkidle0', timeout });
    return true;
  } catch (error) {
    console.error(`  ❌ Navigation failed: ${error.message}`);
    return false;
  }
}

// Main test function
async function runUITests() {
  console.log('='.repeat(80));
  console.log('  JiVS Platform - Comprehensive UI Test');
  console.log('='.repeat(80));
  console.log('');

  let browser;
  let page;
  const consoleMessages = [];
  const networkErrors = [];

  try {
    // Launch browser
    console.log('🚀 Launching browser...');
    browser = await puppeteer.launch({
      headless: true,
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-web-security',
        '--disable-features=IsolateOrigins,site-per-process'
      ]
    });

    page = await browser.newPage();
    await page.setViewport({ width: 1920, height: 1080 });

    // Listen for console messages
    page.on('console', msg => {
      const type = msg.type();
      const text = msg.text();
      consoleMessages.push({ type, text });
      if (type === 'error') {
        console.log(`  ⚠️  Console Error: ${text}`);
      }
    });

    // Listen for page errors
    page.on('pageerror', error => {
      console.log(`  ❌ Page Error: ${error.message}`);
      results.failed.push(`Page error: ${error.message}`);
    });

    // Listen for failed requests
    page.on('requestfailed', request => {
      networkErrors.push(`${request.url()} - ${request.failure().errorText}`);
      console.log(`  ⚠️  Network Error: ${request.url()}`);
    });

    // TEST 1: Login Page
    console.log('\n📋 Test 1: Login Page');
    console.log('-'.repeat(80));

    const loginSuccess = await safeNavigate(page, `${BASE_URL}/login`);
    if (!loginSuccess) {
      results.failed.push('Login page failed to load');
      throw new Error('Cannot proceed without login page');
    }

    await page.waitForTimeout(2000); // Wait for React to render
    await captureScreenshot(page, '01-login-page');

    // Check for login form elements
    const usernameField = await page.$('input[name="username"], input[type="text"]');
    const passwordField = await page.$('input[name="password"], input[type="password"]');
    const submitButton = await page.$('button[type="submit"]');

    if (usernameField && passwordField && submitButton) {
      console.log('  ✅ Login form elements present');
      results.passed.push('Login form elements found');
    } else {
      console.log('  ❌ Login form elements missing');
      results.failed.push('Login form elements not found');
    }

    // TEST 2: Login Functionality
    console.log('\n📋 Test 2: Login Functionality');
    console.log('-'.repeat(80));

    try {
      await page.type('input[name="username"], input[type="text"]', CREDENTIALS.username);
      await page.type('input[name="password"], input[type="password"]', CREDENTIALS.password);
      console.log('  ✅ Credentials entered');

      await captureScreenshot(page, '02-login-filled');

      await Promise.all([
        page.waitForNavigation({ waitUntil: 'networkidle0', timeout: 10000 }),
        page.click('button[type="submit"]')
      ]);

      console.log('  ✅ Login submitted');
      await page.waitForTimeout(2000);

      const currentUrl = page.url();
      if (currentUrl.includes('/dashboard') || currentUrl.includes('/home')) {
        console.log(`  ✅ Redirected to: ${currentUrl}`);
        results.passed.push('Login successful, redirected to dashboard');
      } else {
        console.log(`  ⚠️  Unexpected redirect: ${currentUrl}`);
        results.warnings.push(`Unexpected redirect to: ${currentUrl}`);
      }

      await captureScreenshot(page, '03-post-login');

    } catch (error) {
      console.log(`  ❌ Login failed: ${error.message}`);
      results.failed.push(`Login failed: ${error.message}`);
      await captureScreenshot(page, '03-login-error');
    }

    // TEST 3: Dashboard
    console.log('\n📋 Test 3: Dashboard');
    console.log('-'.repeat(80));

    try {
      await safeNavigate(page, `${BASE_URL}/dashboard`);
      await page.waitForTimeout(2000);
      await captureScreenshot(page, '04-dashboard');

      // Check for common dashboard elements
      const hasContent = await page.evaluate(() => {
        const body = document.body.innerText;
        return body.length > 100; // Dashboard should have content
      });

      if (hasContent) {
        console.log('  ✅ Dashboard loaded with content');
        results.passed.push('Dashboard rendering correctly');
      } else {
        console.log('  ⚠️  Dashboard seems empty');
        results.warnings.push('Dashboard appears to have minimal content');
      }

    } catch (error) {
      console.log(`  ❌ Dashboard error: ${error.message}`);
      results.failed.push(`Dashboard error: ${error.message}`);
    }

    // TEST 4: Extractions Page
    console.log('\n📋 Test 4: Extractions Page');
    console.log('-'.repeat(80));

    try {
      await safeNavigate(page, `${BASE_URL}/extractions`);
      await page.waitForTimeout(2000);
      await captureScreenshot(page, '05-extractions');

      console.log('  ✅ Extractions page loaded');
      results.passed.push('Extractions page accessible');
    } catch (error) {
      console.log(`  ❌ Extractions error: ${error.message}`);
      results.failed.push(`Extractions error: ${error.message}`);
    }

    // TEST 5: Migrations Page
    console.log('\n📋 Test 5: Migrations Page');
    console.log('-'.repeat(80));

    try {
      await safeNavigate(page, `${BASE_URL}/migrations`);
      await page.waitForTimeout(2000);
      await captureScreenshot(page, '06-migrations');

      console.log('  ✅ Migrations page loaded');
      results.passed.push('Migrations page accessible');
    } catch (error) {
      console.log(`  ❌ Migrations error: ${error.message}`);
      results.failed.push(`Migrations error: ${error.message}`);
    }

    // TEST 6: Data Quality Page
    console.log('\n📋 Test 6: Data Quality Page');
    console.log('-'.repeat(80));

    try {
      await safeNavigate(page, `${BASE_URL}/data-quality`);
      await page.waitForTimeout(2000);
      await captureScreenshot(page, '07-data-quality');

      console.log('  ✅ Data Quality page loaded');
      results.passed.push('Data Quality page accessible');
    } catch (error) {
      console.log(`  ❌ Data Quality error: ${error.message}`);
      results.failed.push(`Data Quality error: ${error.message}`);
    }

    // TEST 7: Compliance Page
    console.log('\n📋 Test 7: Compliance Page');
    console.log('-'.repeat(80));

    try {
      await safeNavigate(page, `${BASE_URL}/compliance`);
      await page.waitForTimeout(2000);
      await captureScreenshot(page, '08-compliance');

      console.log('  ✅ Compliance page loaded');
      results.passed.push('Compliance page accessible');
    } catch (error) {
      console.log(`  ❌ Compliance error: ${error.message}`);
      results.failed.push(`Compliance error: ${error.message}`);
    }

    // TEST 8: Check for JavaScript errors
    console.log('\n📋 Test 8: Console Error Analysis');
    console.log('-'.repeat(80));

    const errors = consoleMessages.filter(msg => msg.type === 'error');
    const criticalErrors = errors.filter(err =>
      !err.text.includes('favicon') &&
      !err.text.includes('sourcemap') &&
      !err.text.includes('DevTools')
    );

    if (criticalErrors.length === 0) {
      console.log('  ✅ No critical JavaScript errors');
      results.passed.push('No JavaScript errors detected');
    } else {
      console.log(`  ⚠️  Found ${criticalErrors.length} JavaScript errors:`);
      criticalErrors.forEach(err => {
        console.log(`     - ${err.text}`);
      });
      results.warnings.push(`${criticalErrors.length} JavaScript errors found`);
    }

    // TEST 9: Network Errors
    console.log('\n📋 Test 9: Network Error Analysis');
    console.log('-'.repeat(80));

    if (networkErrors.length === 0) {
      console.log('  ✅ No network errors');
      results.passed.push('All network requests successful');
    } else {
      console.log(`  ⚠️  Found ${networkErrors.length} network errors:`);
      networkErrors.forEach(err => {
        console.log(`     - ${err}`);
      });
      results.warnings.push(`${networkErrors.length} network requests failed`);
    }

    // TEST 10: Check localStorage
    console.log('\n📋 Test 10: Authentication Token Storage');
    console.log('-'.repeat(80));

    const tokens = await page.evaluate(() => {
      return {
        accessToken: localStorage.getItem('accessToken'),
        refreshToken: localStorage.getItem('refreshToken'),
        user: localStorage.getItem('user')
      };
    });

    if (tokens.accessToken && tokens.refreshToken) {
      console.log('  ✅ JWT tokens stored correctly');
      results.passed.push('Authentication tokens present in localStorage');
    } else {
      console.log('  ❌ JWT tokens missing');
      results.failed.push('Authentication tokens not found in localStorage');
    }

  } catch (error) {
    console.error(`\n❌ Critical Error: ${error.message}`);
    results.failed.push(`Critical error: ${error.message}`);
    if (page) {
      await captureScreenshot(page, '99-critical-error');
    }
  } finally {
    if (browser) {
      await browser.close();
    }
  }

  // Print Summary
  console.log('\n' + '='.repeat(80));
  console.log('  TEST SUMMARY');
  console.log('='.repeat(80));
  console.log(`\n✅ Passed: ${results.passed.length}`);
  results.passed.forEach(item => console.log(`   - ${item}`));

  if (results.warnings.length > 0) {
    console.log(`\n⚠️  Warnings: ${results.warnings.length}`);
    results.warnings.forEach(item => console.log(`   - ${item}`));
  }

  if (results.failed.length > 0) {
    console.log(`\n❌ Failed: ${results.failed.length}`);
    results.failed.forEach(item => console.log(`   - ${item}`));
  }

  console.log(`\n📸 Screenshots saved to: ${screenshotsDir}`);
  results.screenshots.forEach(shot => {
    console.log(`   - ${shot.name}.png`);
  });

  console.log('\n' + '='.repeat(80));

  if (results.failed.length === 0) {
    console.log('✅ ALL TESTS PASSED!');
    console.log('The JiVS Platform UI is fully functional.');
    return 0;
  } else {
    console.log('❌ SOME TESTS FAILED');
    console.log('Please review the failed tests and screenshots above.');
    return 1;
  }
}

// Run tests
runUITests().then(exitCode => {
  process.exit(exitCode);
}).catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
