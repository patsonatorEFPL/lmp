import { Page, Locator, expect } from '@playwright/test';

export class AdminSettingsPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly erpnextHealthBadge: Locator;
  readonly erpnextLatencyText: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.locator('h1, h2').filter({ hasText: /paramètres|settings|configuration/i });
    this.erpnextHealthBadge = page.locator('[data-testid="erpnext-health-badge"], .erpnext-health-badge').first();
    this.erpnextLatencyText = page.locator('[data-testid="erpnext-latency"], .erpnext-latency').first();
  }

  async goto() {
    await this.page.goto('/admin/settings', { waitUntil: 'domcontentloaded' });
    await this.page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
  }

  async expectErpnextBadgeVisible() {
    await expect(this.erpnextHealthBadge).toBeVisible({ timeout: 10000 });
  }

  async expectErpnextBadgeStatus(status: 'up' | 'down' | 'unknown') {
    const badge = this.erpnextHealthBadge;
    await expect(badge).toBeVisible();
    const classAttr = await badge.getAttribute('class');
    switch (status) {
      case 'up':
        expect(classAttr).toMatch(/green|success|up/i);
        break;
      case 'down':
        expect(classAttr).toMatch(/red|danger|down|error/i);
        break;
      default:
        expect(classAttr).toMatch(/gray|neutral|unknown/i);
    }
  }

  async takeScreenshot(name: string) {
    await this.page.screenshot({ path: `e2e-report/screenshots/${name}.png`, fullPage: false });
  }
}
