import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CanActivateFn, Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError, filter, map, take, timeout } from 'rxjs/operators';

import { AuthService } from '../services/auth.service';
import { SiteConfigService } from '../services/site-config.service';
import { AUTH_GUARD_LOADING_TIMEOUT_MS } from './guard-timeout';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const siteConfig = inject(SiteConfigService);
  const platformId = inject(PLATFORM_ID);

  if (isPlatformServer(platformId) || !isPlatformBrowser(platformId)) {
    return true;
  }

  const goToLogin = (): false => {
    siteConfig.goToLogin(window.location.pathname + window.location.search);
    return false;
  };

  return toObservable(authService.loading).pipe(
    filter((loading) => !loading),
    take(1),
    map(() => {
      if (!authService.isLoggedIn()) {
        return goToLogin();
      }
      if (!authService.isAdmin()) {
        return router.createUrlTree(['/dashboard']);
      }
      return true;
    }),
    timeout(AUTH_GUARD_LOADING_TIMEOUT_MS),
    catchError(() => of(goToLogin())),
  );
};
