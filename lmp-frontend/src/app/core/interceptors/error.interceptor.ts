import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * 401 sur ces URLs ne doit jamais déclencher une redirection globale :
 * - /auth/me : son erreur est gérée par {@link AuthService#checkSession}.
 * - /services : catalogue public ; un 401 ne doit pas envoyer un visiteur anonyme vers /login.
 */
const SKIP_401_REDIRECT_URL_PARTS = ['/api/v1/auth/me', '/api/v1/services'];

function shouldSkip401Redirect(req: { url: string }, authService: AuthService): boolean {
  if (SKIP_401_REDIRECT_URL_PARTS.some((part) => req.url.includes(part))) {
    return true;
  }
  if (authService.loading()) {
    return true;
  }
  if (authService.isLoggedIn()) {
    return true;
  }
  return false;
}

/**
 * Global error interceptor: handles 401/403 redirects
 * and standardizes error responses.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        if (!shouldSkip401Redirect(req, authService)) {
          authService.clearUser();
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
