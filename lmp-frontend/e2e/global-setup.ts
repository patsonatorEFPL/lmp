import { chromium, FullConfig } from '@playwright/test';

/**
 * Global setup: ensure the auth state directory exists.
 * Actual login state is per-fixture to keep tests independent.
 */
async function globalSetup(config: FullConfig) {
  // Nothing required at global level for now.
  // Per-fixture auth in auth.fixture.ts handles session storage.
}

export default globalSetup;
