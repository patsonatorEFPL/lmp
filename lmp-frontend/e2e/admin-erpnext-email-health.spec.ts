import { test, expect } from './fixtures/auth.fixture';
import { AdminSettingsPage } from './pages/admin-settings.page';

test.describe('Étape 1 — Health Check ERPNext Email', () => {
  test('affiche le badge de statut ERPNext sur la page admin settings', async ({ adminPage }) => {
    const settingsPage = new AdminSettingsPage(adminPage);

    await settingsPage.goto();

    // Le badge doit être visible après le chargement
    await settingsPage.expectErpnextBadgeVisible();

    // Vérifier que le statut s'affiche (UP ou DOWN selon l'environnement)
    const badge = settingsPage.erpnextHealthBadge;
    const text = await badge.textContent();
    expect(text).toMatch(/En ligne|Hors ligne/);

    // Screenshot pour validation visuelle (headfull)
    await adminPage.screenshot({ path: 'e2e-report/screenshots/erpnext-health-badge.png', fullPage: false });
  });
});
