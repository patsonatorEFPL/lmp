import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CanActivateFn } from '@angular/router';
import { of } from 'rxjs';
import { catchError, filter, map, take, timeout } from 'rxjs/operators';

import { AuthService } from '../services/auth.service';
import { SiteConfigService } from '../services/site-config.service';
import { AUTH_GUARD_LOADING_TIMEOUT_MS } from './guard-timeout';

/**
 * Session cookies are not available during SSR. Defer the real check to the
 * browser after hydration / APP_INITIALIZER (checkSession).
 *
 * Same pattern as {@link adminGuard}: wait for loading to finish, then read
 * {@link AuthService#isLoggedIn} synchronously. Do not use combineLatest(loading, user):
 * combineLatest can emit [false, null] if loading flips false before the user stream
 * has re-emitted, which wrongly sends users to /login on refresh.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const siteConfig = inject(SiteConfigService);
  const platformId = inject(PLATFORM_ID);

  if (isPlatformServer(platformId)) {
    return true;
  }

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const goToLogin = (): false => {
    siteConfig.goToLogin(window.location.pathname + window.location.search);
    return false;
  };

  return toObservable(authService.loading).pipe(
    filter((loading) => !loading),
    take(1),
    map(() => (authService.isLoggedIn() ? true : goToLogin())),
    timeout(AUTH_GUARD_LOADING_TIMEOUT_MS),
    catchError(() => of(goToLogin())),
  );
};

