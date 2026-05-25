import { test as base, expect, BrowserContext, Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const AUTH_DIR = path.join(__dirname, '..', '..', '.playwright-auth');

export type UserRole = 'admin' | 'user';

interface AuthFixture {
  adminPage: Page;
  userPage: Page;
}

/**
 * Logs in as the given role and returns a new browser context with session storage
 * already populated. Reuses existing session files when fresh (under 30 min).
 */
async function getAuthenticatedContext(
  browser: BrowserContext['browser'],
  role: UserRole,
  baseURL: string
): Promise<BrowserContext> {
  if (!browser) throw new Error('Browser instance required');

  const sessionFile = path.join(AUTH_DIR, `${role}-session.json`);

  // Reuse session if less than 30 minutes old
  if (fs.existsSync(sessionFile)) {
    const stat = fs.statSync(sessionFile);
    const ageMin = (Date.now() - stat.mtimeMs) / 60000;
    if (ageMin < 30) {
      const ctx = await browser.newContext({
        storageState: sessionFile,
        viewport: { width: 1400, height: 900 },
      });
      return ctx;
    }
  }

  // Fresh login
  const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
  const page = await ctx.newPage();

  const required = (key: string): string => {
    const v = process.env[key];
    if (!v) throw new Error(`Missing env var ${key} — set it before running E2E tests`);
    return v;
  };
  const credentials = {
    admin: {
      email: required('E2E_ADMIN_EMAIL'),
      password: required('E2E_ADMIN_PASSWORD'),
    },
    user: {
      email: required('E2E_USER_EMAIL'),
      password: required('E2E_USER_PASSWORD'),
    },
  }[role];

  await page.goto(`${baseURL}/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});

  await page.locator('input[type="email"], input[name="email"]').first().fill(credentials.email);
  await page.locator('input[type="password"]').first().fill(credentials.password);
  await page.locator('button[type="submit"]').first().click();

  await page.waitForURL(/\/|admin/, { timeout: 20000 });
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});

  // Save session
  if (!fs.existsSync(AUTH_DIR)) {
    fs.mkdirSync(AUTH_DIR, { recursive: true });
  }
  await ctx.storageState({ path: sessionFile });

  return ctx;
}

export const test = base.extend<AuthFixture>({
  adminPage: async ({ browser, baseURL }, use) => {
    const ctx = await getAuthenticatedContext(browser, 'admin', baseURL ?? 'http://localhost:4200');
    const page = await ctx.newPage();
    await use(page);
    await ctx.close();
  },
  userPage: async ({ browser, baseURL }, use) => {
    const ctx = await getAuthenticatedContext(browser, 'user', baseURL ?? 'http://localhost:4200');
    const page = await ctx.newPage();
    await use(page);
    await ctx.close();
  },
});

export { expect };
