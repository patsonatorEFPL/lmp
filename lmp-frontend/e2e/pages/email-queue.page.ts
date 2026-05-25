import { Page, Locator, expect } from '@playwright/test';

export class EmailQueuePage {
  readonly page: Page;
  readonly heading: Locator;
  readonly statsCards: Locator;
  readonly table: Locator;
  readonly refreshButton: Locator;
  readonly statusFilter: Locator;
  readonly searchInput: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.locator('h1').filter({ hasText: /file d'attente email/i });
    this.statsCards = page.locator('.rounded-sm.border');
    this.table = page.locator('table');
    this.refreshButton = page.locator('button:has-text("Actualiser")');
    this.statusFilter = page.locator('select');
    this.searchInput = page.locator('input[placeholder="Rechercher..."]');
  }

  async goto() {
    await this.page.goto('/admin/email-queue', { waitUntil: 'domcontentloaded' });
    await this.page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
  }

  async expectStatsVisible() {
    await expect(this.statsCards.first()).toBeVisible();
  }

  async expectTableVisible() {
    await expect(this.table).toBeVisible();
  }

  async filterByStatus(status: string) {
    await this.statusFilter.selectOption(status);
  }

  async takeScreenshot(name: string) {
    await this.page.screenshot({ path: `e2e-report/screenshots/${name}.png`, fullPage: false });
  }
}
