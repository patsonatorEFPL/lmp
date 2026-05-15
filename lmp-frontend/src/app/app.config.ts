import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
} from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import {
  NavigationEnd,
  NavigationError,
  Router,
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { filter } from 'rxjs';

import { routes } from './app.routes';
import { credentialsInterceptor, csrfInterceptor, errorInterceptor } from './core/interceptors';
import { ThemeService, AuthService } from './core/services';
import { SiteConfigService, initSiteConfig } from './core/services/site-config.service';

// Cache-poisoning recovery (post-deploy chunk hash mismatch) :
// Si Angular tente de lazy-load un chunk dont le hash n'existe plus (deploy
// rotate les hashes mais le user a un vieux main.js cached qui pointe vers
// l'ancien chunk), NavigationError remonte un "Failed to fetch dynamically
// imported module". On force un hard reload une seule fois (sentinel
// sessionStorage anti-boucle) pour re-télécharger index.html → bundles à jour.
const RELOAD_SENTINEL = 'lmp_chunk_reload_attempted';
const CHUNK_ERROR_PATTERN =
  /Failed to fetch dynamically imported module|Loading chunk [\w-]+ failed|ChunkLoadError|Importing a module script failed/i;

function setupChunkReloadRecovery(router: Router): void {
  if (typeof window === 'undefined' || typeof sessionStorage === 'undefined') return;
  router.events
    .pipe(filter((e): e is NavigationError => e instanceof NavigationError))
    .subscribe(event => {
      const msg = String(event.error?.message ?? event.error ?? '');
      if (!CHUNK_ERROR_PATTERN.test(msg)) return;
      if (sessionStorage.getItem(RELOAD_SENTINEL)) return;
      sessionStorage.setItem(RELOAD_SENTINEL, Date.now().toString());
      window.location.reload();
    });
  router.events
    .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
    .subscribe(() => sessionStorage.removeItem(RELOAD_SENTINEL));
}

registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({
        scrollPositionRestoration: 'enabled',
        anchorScrolling: 'enabled',
      }),
    ),
    provideHttpClient(
      withFetch(),
      withInterceptors([credentialsInterceptor, csrfInterceptor, errorInterceptor]),
    ),
    provideAppInitializer(() => inject(SiteConfigService).load()),
    provideAppInitializer(() => {
      inject(ThemeService).init();
    }),
    provideAppInitializer(() => inject(AuthService).checkSession()),
    provideAppInitializer(() => setupChunkReloadRecovery(inject(Router))),
    { provide: LOCALE_ID, useValue: 'fr' },
  ],
};
