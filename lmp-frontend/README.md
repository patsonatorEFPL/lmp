# LMP Frontend

Angular 21 application with Server-Side Rendering (SSR) — public interface and dashboard for LMP Digital Services.

## Stack

| Tool | Version |
|---|---|
| Angular | 21.2 |
| Angular SSR | 21.2 |
| Tailwind CSS | 4.x |
| Spartan UI / Helm | 0.0.1-alpha.648 |
| Lucide Angular | 0.577 |
| TypeScript | 5.9 |
| Node.js | 20+ |

## Getting started

```bash
npm install
npm start        # dev server → http://localhost:4200
```

The proxy (`proxy.conf.json`) forwards `/api/**` calls to `http://localhost:8080`.

## Commands

```bash
npm start              # Development server (hot reload)
npm run build          # Production build (SSR)
npm run watch          # Dev build in watch mode
npm test               # Unit tests (Vitest)
npm run codegen        # Generate API clients from OpenAPI
```

## Structure

```
src/app/
├── core/
│   ├── guards/        # auth.guard, admin.guard
│   ├── interceptors/  # CSRF, error handling
│   └── services/      # auth, catalog, dashboard, seo, theme…
├── features/
│   ├── home/          # Landing page
│   ├── services/      # Service catalog
│   ├── auth/          # Login, Register
│   ├── dashboard/     # Customer area (orders, appointments)
│   ├── admin/         # Back office (users, orders, appointments…)
│   ├── contact/       # Contact form
│   ├── about/         # About page
│   ├── map/           # Interactive map
│   ├── privacy/       # Privacy policy
│   └── terms/         # Terms of service
├── shared/
│   ├── layout/        # Navbar, Footer, PublicLayout, AdminLayout
│   └── modals/        # AppointmentModal, OrderModal
└── libs/ui/           # Spartan/Helm components (button, card, input…)
```

## Theme

CSS variables are defined in `src/styles.css`:

- Font: **DM Sans** (Google Fonts)
- Primary color: `#7c3aed` (violet, light) / `#a78bfa` (dark)
- Light background: `#fafafa` — Dark background: `#111113`
- Border radius: `0.5rem` (`rounded-sm` everywhere)
- Supports light and dark mode via the `.dark` class on `<html>`
