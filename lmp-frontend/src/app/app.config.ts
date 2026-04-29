import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
} from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import {
  provideRouter,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { credentialsInterceptor, csrfInterceptor, errorInterceptor } from './core/interceptors';
import { ThemeService, AuthService } from './core/services';
import { SiteConfigService, initSiteConfig } from './core/services/site-config.service';

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
    { provide: LOCALE_ID, useValue: 'fr' },
  ],
};
