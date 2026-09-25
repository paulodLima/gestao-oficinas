import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e-qa', fullyParallel: false, workers: 1, timeout: 120_000,
  expect: { timeout: 15_000 },
  outputDir: './test-results-qa', reporter: [['list'], ['html', { outputFolder: 'playwright-report-qa', open: 'never' }]],
  use: { baseURL: process.env['PLAYWRIGHT_BASE_URL'] || 'http://localhost:14220',
    screenshot: 'only-on-failure', trace: 'off', timezoneId: 'America/Sao_Paulo' },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'android', use: { ...devices['Pixel 7'] } },
    { name: 'ios-webkit', use: { ...devices['iPhone 13'], defaultBrowserType: 'webkit' } },
    { name: 'narrow', use: { ...devices['Desktop Chrome'], viewport: { width: 320, height: 760 } } },
  ],
});
