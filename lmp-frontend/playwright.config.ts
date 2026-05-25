import { defineConfig, devices } from '@playwright/test';

/**
 * LMP E2E Test Configuration
 * Headful mode by default as requested for visual validation at each step.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'e2e-report' }],
    ['json', { outputFile: 'e2e-report/results.json' }],
  ],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    headless: false,
    slowMo: 200,
    viewport: { width: 1400, height: 900 },
    launchOptions: {
      args: ['--disable-web-security'],
    },
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  globalSetup: require.resolve('./e2e/global-setup.ts'),
});
