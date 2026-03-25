# LMP Frontend

Application Angular 21 avec Server-Side Rendering (SSR) — interface publique et dashboard de LMP Digital Services.

## Stack

| Outil | Version |
|---|---|
| Angular | 21.2 |
| Angular SSR | 21.2 |
| Tailwind CSS | 4.x |
| Spartan UI / Helm | 0.0.1-alpha.648 |
| Lucide Angular | 0.577 |
| TypeScript | 5.9 |
| Node.js | 20+ |

## Démarrage

```bash
npm install
npm start        # dev server → http://localhost:4200
```

Le proxy (`proxy.conf.json`) redirige les appels `/api/**` vers `http://localhost:8080`.

## Commandes

```bash
npm start              # Serveur de développement (hot reload)
npm run build          # Build de production (SSR)
npm run watch          # Build dev en mode watch
npm test               # Tests unitaires (Vitest)
npm run codegen        # Génération des clients API depuis OpenAPI
```

## Structure

```
src/app/
├── core/
│   ├── guards/        # auth.guard, admin.guard
│   ├── interceptors/  # CSRF, gestion d'erreurs
│   └── services/      # auth, catalog, dashboard, seo, theme…
├── features/
│   ├── home/          # Landing page
│   ├── services/      # Catalogue des services
│   ├── auth/          # Login, Register
│   ├── dashboard/     # Espace client (commandes, RDV)
│   ├── admin/         # Back-office (users, orders, appointments…)
│   ├── contact/       # Formulaire de contact
│   ├── about/         # Page à propos
│   ├── map/           # Carte interactive
│   ├── privacy/       # Politique de confidentialité
│   └── terms/         # Conditions d'utilisation
├── shared/
│   ├── layout/        # Navbar, Footer, PublicLayout, AdminLayout
│   └── modals/        # AppointmentModal, OrderModal
└── libs/ui/           # Composants Spartan/Helm (button, card, input…)
```

## Thème

Les variables CSS sont définies dans `src/styles.css` :

- Police : **DM Sans** (Google Fonts)
- Couleur principale : `#7c3aed` (violet, light) / `#a78bfa` (dark)
- Fond light : `#fafafa` — Fond dark : `#111113`
- Border radius : `0.5rem` (`rounded-sm` partout)
- Supporte le mode clair et sombre via la classe `.dark` sur `<html>`
