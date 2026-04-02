import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CanActivateFn, Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError, filter, map, take, timeout } from 'rxjs/operators';

import { AuthService } from '../services/auth.service';
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
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (isPlatformServer(platformId)) {
    return true;
  }

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  return toObservable(authService.loading).pipe(
    filter((loading) => !loading),
    take(1),
    map(() =>
      authService.isLoggedIn()
        ? true
        : router.createUrlTree(['/login']),
    ),
    timeout(AUTH_GUARD_LOADING_TIMEOUT_MS),
    catchError(() => of(router.createUrlTree(['/login']))),
  );
};

export const moduleGuard = (requiredModule: string): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const platformId = inject(PLATFORM_ID);

    if (isPlatformServer(platformId) || !isPlatformBrowser(platformId)) {
      return true;
    }

    return toObservable(authService.loading).pipe(
      filter((loading) => !loading),
      take(1),
      map(() => {
        if (!authService.isLoggedIn()) {
          return router.createUrlTree(['/login']);
        }
        if (!authService.hasModule(requiredModule)) {
          return router.createUrlTree(['/dashboard'], {
            queryParams: { upgrade: true },
          });
        }
        return true;
      }),
      timeout(AUTH_GUARD_LOADING_TIMEOUT_MS),
      catchError(() => of(router.createUrlTree(['/login']))),
    );
  };
};
