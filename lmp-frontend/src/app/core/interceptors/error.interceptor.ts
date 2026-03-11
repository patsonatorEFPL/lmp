import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * URLs that should NOT trigger a redirect on 401.
 * These are "silent" auth checks or public endpoints.
 */
const SKIP_401_REDIRECT = ['/api/v1/auth/me', '/api/v1/services'];

/**
 * Global error interceptor: handles 401/403 redirects
 * and standardizes error responses.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        const shouldSkip = SKIP_401_REDIRECT.some((url) =>
          req.url.includes(url),
        );
        if (!shouldSkip) {
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
