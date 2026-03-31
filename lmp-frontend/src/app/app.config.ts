import {
  ApplicationConfig,
  APP_INITIALIZER,
  LOCALE_ID,
  inject,
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

function initializeTheme(): () => void {
  const themeService = inject(ThemeService);
  return () => themeService.init();
}

function initializeAuth(): () => Promise<void> {
  const authService = inject(AuthService);
  return () => authService.checkSession();
}

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
    {
      provide: APP_INITIALIZER,
      useFactory: initializeTheme,
      multi: true,
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAuth,
      multi: true,
    },
    { provide: LOCALE_ID, useValue: 'fr' },
  ],
};
