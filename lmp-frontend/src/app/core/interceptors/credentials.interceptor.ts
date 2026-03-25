import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Attaches credentials (session cookies) to all requests targeting the API.
 */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('/api') || req.url.startsWith('/oauth2')) {
    return next(req.clone({ withCredentials: true }));
  }
  return next(req);
};
