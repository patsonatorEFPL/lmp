import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CanActivateFn, Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError, filter, map, take, timeout } from 'rxjs/operators';

import { AuthService } from '../services/auth.service';

/**
 * Session cookies are not available during SSR. Defer the real check to the
 * browser after hydration / APP_INITIALIZER (checkSession).
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
    timeout(15_000),
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
      timeout(15_000),
      catchError(() => of(router.createUrlTree(['/login']))),
    );
  };
};
