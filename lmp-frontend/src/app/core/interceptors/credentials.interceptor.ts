import { HttpInterceptorFn } from '@angular/common/http';

/**
 * True if the request targets API or OAuth paths (relative or absolute URL).
 * HttpClient / withFetch() may normalize URLs to absolute; session cookies must
 * still be sent (withCredentials).
 */
function requestNeedsCredentials(url: string): boolean {
  if (url.startsWith('/api') || url.startsWith('/oauth2')) {
    return true;
  }
  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      const path = new URL(url).pathname;
      return path.startsWith('/api') || path.startsWith('/oauth2');
    } catch {
      return false;
    }
  }
  return false;
}

/**
 * Attaches credentials (session cookies) to all requests targeting the API.
 */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  if (requestNeedsCredentials(req.url)) {
    return next(req.clone({ withCredentials: true }));
  }
  return next(req);
};
