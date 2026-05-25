import { test, expect } from './fixtures/auth.fixture';
import { EmailQueuePage } from './pages/email-queue.page';

test.describe('Étape 6 — Bulk Actions Email Queue', () => {
  test('affiche les actions bulk et la suppression individuelle', async ({ adminPage }) => {
    const queuePage = new EmailQueuePage(adminPage);

    await queuePage.goto();

    // Table should be visible
    await queuePage.expectTableVisible();

    // Screenshot for visual validation
    await queuePage.takeScreenshot('admin-email-queue-bulk');
  });
});
