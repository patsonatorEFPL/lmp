import { RenderMode, ServerRoute } from '@angular/ssr';

/**
 * Server-side route configuration for Angular SSR.
 *
 * - Public pages are server-rendered for SEO.
 * - Protected pages (dashboard, admin, settings) are client-rendered
 *   because they require authentication (no cookies during SSR).
 * - Auth pages are client-rendered to avoid flashing issues.
 */
export const serverRoutes: ServerRoute[] = [
  // Public pages — SSR for SEO
  { path: '', renderMode: RenderMode.Server },
  { path: 'services', renderMode: RenderMode.Server },
  { path: 'about', renderMode: RenderMode.Server },
  { path: 'contact', renderMode: RenderMode.Server },
  { path: 'map', renderMode: RenderMode.Server },
  { path: 'privacy', renderMode: RenderMode.Server },
  { path: 'terms', renderMode: RenderMode.Server },
  { path: 'blog', renderMode: RenderMode.Server },
  { path: 'blog/**', renderMode: RenderMode.Server },

  // Auth pages — client-only
  { path: 'login', renderMode: RenderMode.Client },
  { path: 'register', renderMode: RenderMode.Client },
  { path: 'forgot-password', renderMode: RenderMode.Client },
  { path: 'reset-password', renderMode: RenderMode.Client },

  // Payment / checkout pages — client-only
  { path: 'checkout/**', renderMode: RenderMode.Client },
  { path: 'payment/success', renderMode: RenderMode.Client },
  { path: 'payment/cancel', renderMode: RenderMode.Client },
  { path: 'payment/guest', renderMode: RenderMode.Client },

  // Protected pages — client-only (require auth cookies)
  { path: 'dashboard', renderMode: RenderMode.Client },
  { path: 'dashboard/**', renderMode: RenderMode.Client },
  { path: 'settings', renderMode: RenderMode.Client },

  // Admin pages — client-only
  { path: 'admin', renderMode: RenderMode.Client },
  { path: 'admin/**', renderMode: RenderMode.Client },

  // Fallback — server-render
  { path: '**', renderMode: RenderMode.Server },
];
