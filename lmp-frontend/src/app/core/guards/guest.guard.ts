import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CanActivateFn, Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError, filter, map, take, timeout } from 'rxjs/operators';

import { AuthService } from '../services/auth.service';
import { AUTH_GUARD_LOADING_TIMEOUT_MS } from './guard-timeout';

/**
 * Guard pour les pages publiques d'authentification (`/login`, `/register`,
 * `/forgot-password`). Si l'utilisateur est déjà authentifié, le renvoie vers
 * son dashboard (admin → `/admin`, sinon `/dashboard`) au lieu d'afficher
 * un formulaire d'inscription/login inutile.
 *
 * SSR : on rend la page côté serveur (pas de session disponible) puis le check
 * réel a lieu post-hydratation. Si l'utilisateur est connecté, le router
 * redirige avant l'instanciation du LoginComponent — pas de flash de formulaire.
 */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (isPlatformServer(platformId) || !isPlatformBrowser(platformId)) {
    return true;
  }

  return toObservable(auth.loading).pipe(
    filter((loading) => !loading),
    take(1),
    map(() => {
      if (!auth.isLoggedIn()) {
        return true;
      }
      const target = auth.isAdmin() ? '/admin' : '/dashboard';
      return router.createUrlTree([target]);
    }),
    timeout(AUTH_GUARD_LOADING_TIMEOUT_MS),
    catchError(() => of(true)),
  );
};
