import { test, expect } from './fixtures/auth.fixture';
import { EmailQueuePage } from './pages/email-queue.page';

test.describe('Étape 4 — Dashboard Admin Email Queue', () => {
  test('affiche les stats et la table des emails en queue', async ({ adminPage }) => {
    const queuePage = new EmailQueuePage(adminPage);

    await queuePage.goto();

    // Stats cards should be visible
    await queuePage.expectStatsVisible();

    // Table should be visible
    await queuePage.expectTableVisible();

    // Screenshot for visual validation
    await queuePage.takeScreenshot('admin-email-queue-dashboard');
  });
});
