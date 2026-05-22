import { test, expect } from '@playwright/test';

test.describe('Smoke tests', () => {
  test('homepage loads', async ({ page }) => {
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});

    // Basic assertion: page has a title or some visible content
    const body = page.locator('body');
    await expect(body).toBeVisible();

    // Ensure we're not on an error page
    await expect(page.locator('text=404')).not.toBeVisible();
    await expect(page.locator('text=Error')).not.toBeVisible();

    // Screenshot for visual validation (headfull requirement)
    await page.screenshot({ path: 'e2e-report/screenshots/smoke-homepage.png', fullPage: true });
  });

  test('login page loads', async ({ page }) => {
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});

    await expect(page.locator('input[type="email"]').first()).toBeVisible();
    await expect(page.locator('input[type="password"]').first()).toBeVisible();
    await expect(page.locator('button[type="submit"]').first()).toBeVisible();

    await page.screenshot({ path: 'e2e-report/screenshots/smoke-login.png', fullPage: true });
  });
});
