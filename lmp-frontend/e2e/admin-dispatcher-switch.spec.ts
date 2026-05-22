import { test, expect } from './fixtures/auth.fixture';
import { AdminSettingsPage } from './pages/admin-settings.page';

test.describe('Étape 5 — Dispatcher Switchable', () => {
  test('affiche le switcher de dispatcher et permet le test', async ({ adminPage }) => {
    const settingsPage = new AdminSettingsPage(adminPage);

    await settingsPage.goto();

    // Vérifier que les boutons de stratégie sont visibles
    const smtpBtn = adminPage.locator('button:has-text("SMTP (Mailtrap)")');
    const erpnextBtn = adminPage.locator('button:has-text("ERPNext")');
    await expect(smtpBtn).toBeVisible();
    await expect(erpnextBtn).toBeVisible();

    // Screenshot pour validation visuelle
    await adminPage.screenshot({ path: 'e2e-report/screenshots/admin-dispatcher-switch.png', fullPage: false });
  });
});
