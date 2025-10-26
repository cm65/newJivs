import { defineConfig, devices } from '@playwright/test';

/**
 * Railway Production Test Configuration
 * Tests against deployed Railway environment
 */
export default defineConfig({
  testDir: './tests/e2e',

  /* Run tests in files in parallel */
  fullyParallel: true,

  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,

  /* Retry on CI only */
  retries: 2,

  /* Opt out of parallel tests on CI. */
  workers: 4,

  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [
    ['html', { outputFolder: 'playwright-report-railway' }],
    ['json', { outputFile: 'test-results-railway/results.json' }],
    ['junit', { outputFile: 'test-results-railway/junit.xml' }],
    ['list'],
  ],

  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: 'https://jivs-frontend-production.up.railway.app',

    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',

    /* Screenshot on failure */
    screenshot: 'only-on-failure',

    /* Video on failure */
    video: 'retain-on-failure',

    /* Maximum time each action can take */
    actionTimeout: 15000, // Increased for network latency

    /* Maximum navigation time */
    navigationTimeout: 45000, // Increased for Railway cold starts
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /* Do not start local dev server - testing against Railway */
  // webServer: undefined,

  /* Global timeout for each test */
  timeout: 90000, // Increased for Railway latency

  /* Expect timeout */
  expect: {
    timeout: 15000, // Increased for Railway
  },
});
