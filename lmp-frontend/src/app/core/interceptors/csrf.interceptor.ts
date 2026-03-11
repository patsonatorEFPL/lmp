import { HttpInterceptorFn } from '@angular/common/http';

/**
 * CSRF interceptor: reads XSRF-TOKEN cookie set by Spring Security
 * and sends it back as X-XSRF-TOKEN header.
 */
export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  const csrfToken = getCookie('XSRF-TOKEN');

  if (csrfToken && !['GET', 'HEAD', 'OPTIONS'].includes(req.method)) {
    const cloned = req.clone({
      setHeaders: { 'X-XSRF-TOKEN': csrfToken },
    });
    return next(cloned);
  }

  return next(req);
};

function getCookie(name: string): string | null {
  const match = document.cookie.match(
    new RegExp('(^| )' + name + '=([^;]+)'),
  );
  return match ? decodeURIComponent(match[2]) : null;
}
