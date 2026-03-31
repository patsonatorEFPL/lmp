import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * URLs that should NOT trigger a redirect to /login on 401.
 * - /auth/me : probe de session (déjà gérée par AuthService + garde).
 * - /services : catalogue public.
 * - Espace utilisateur (dashboard, commandes, notifications) : un 401 isolé ne doit pas
 *   expulser l’utilisateur comme un échec global ; les composants gèrent l’erreur.
 *   Sans cela, un 401 sur /dashboard/stats ou /notifications après F5 peut envoyer vers /login
 *   alors que /admin (autres endpoints) ne déclenche pas ce chemin.
 */
const SKIP_401_REDIRECT = [
  '/api/v1/auth/me',
  '/api/v1/services',
  '/api/v1/dashboard',
  '/api/v1/notifications',
  '/api/v1/orders',
];

/**
 * Global error interceptor: handles 401/403 redirects
 * and standardizes error responses.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const injector = inject(Injector);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        const shouldSkip = SKIP_401_REDIRECT.some((url) =>
          req.url.includes(url),
        );
        if (!shouldSkip) {
          injector.get(AuthService).clearUser();
          router.navigate(['/login']);
        }
      } else if (error.status === 403) {
        // Don't redirect if already on an admin page (avoid losing context)
        const currentUrl = router.url;
        if (!currentUrl.startsWith('/admin')) {
          router.navigate(['/dashboard'], {
            queryParams: { forbidden: true },
          });
        }
      }
      return throwError(() => error);
    }),
  );
};
