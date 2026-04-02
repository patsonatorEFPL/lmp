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
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { credentialsInterceptor, csrfInterceptor, errorInterceptor } from './core/interceptors';
import { ThemeService, AuthService } from './core/services';

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
      withInterceptors([credentialsInterceptor, csrfInterceptor, errorInterceptor]),
    ),
    provideAppInitializer(() => {
      inject(ThemeService).init();
    }),
    provideAppInitializer(() => inject(AuthService).checkSession()),
    { provide: LOCALE_ID, useValue: 'fr' },
  ],
};
