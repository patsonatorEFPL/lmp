export const environment = {
  production: false,
  apiUrl: '',  // Empty = same-origin (proxied via proxy.conf.json in dev)
  apiBaseUrl: '/api/v1',
  appName: 'LMP Digital Services',
  /** Devise par défaut de l'application. Centralisée ici pour faciliter l'internationalisation future. */
  defaultCurrency: 'EUR',
  /** Locale par défaut pour les pipes de formatage. */
  defaultLocale: 'fr-FR',
  /** 0 = désactivé. Rafraîchissement silencieux des tableaux de bord / listes tant que l'onglet est visible. */
  dashboardPollIntervalMs: 45_000,
};
