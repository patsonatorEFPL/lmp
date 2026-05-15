import { APP_BASE_HREF } from '@angular/common';
import { CommonEngine } from '@angular/ssr/node';
import express from 'express';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createProxyMiddleware } from 'http-proxy-middleware';
import bootstrap from './main.server';

/**
 * Routes that should be client-side rendered (no SSR).
 * These require authentication or have no SEO benefit.
 */
const CLIENT_ONLY_ROUTES = [
  '/login',
  '/register',
  '/forgot-password',
  '/reset-password',
  '/dashboard',
  '/admin',
  '/settings',
  '/payment',
];

function isClientOnlyRoute(url: string): boolean {
  return CLIENT_ONLY_ROUTES.some(
    (route) => url === route || url.startsWith(route + '/'),
  );
}

export function app(): ReturnType<typeof express> {
  const serverDistFolder = dirname(fileURLToPath(import.meta.url));
  const browserDistFolder = resolve(serverDistFolder, '../browser');
  const indexHtml = join(serverDistFolder, 'index.server.html');

  const server = express();
  // Pour que req.ip reflète X-Forwarded-For / X-Real-IP derrière Traefik, Caddy, etc.
  server.set('trust proxy', true);

  // Request logger (JSON structuré, parseable par Loki/Datadog).
  // LOG_REQUESTS=false dans env si trop verbeux (default on).
  if (process.env['LOG_REQUESTS'] !== 'false') {
    server.use((req, res, next) => {
      const start = Date.now();
      res.on('finish', () => {
        console.log(JSON.stringify({
          ts: new Date().toISOString(),
          level: res.statusCode >= 500 ? 'error' : res.statusCode >= 400 ? 'warn' : 'info',
          method: req.method,
          url: req.originalUrl,
          status: res.statusCode,
          duration_ms: Date.now() - start,
          ip: req.ip,
          ua: req.get('user-agent')?.slice(0, 100),
          ref: req.get('referer'),
        }));
      });
      next();
    });
  }

  const allowedHosts = process.env['ALLOWED_HOSTS']
    ? process.env['ALLOWED_HOSTS'].split(',').map(h => h.trim())
    : ['localhost'];
  const commonEngine = new CommonEngine({
    allowedHosts,
  });

  /**
   * Backend URL for SSR API proxy.
   * In Docker / production: BACKEND_URL = http://lmp-spring-app:8080
   * In local dev:           BACKEND_URL = http://localhost:8080
   */
  const backendUrl = process.env['BACKEND_URL'] || 'http://localhost:8080';

  // Proxy API, OAuth2, Sitemap, and Actuator requests to Spring Boot backend
  const proxyPaths = ['/api', '/oauth2', '/actuator', '/sitemap.xml'];
  for (const path of proxyPaths) {
    server.use(
      path,
      createProxyMiddleware({
        target: backendUrl,
        changeOrigin: true,
      }),
    );
  }

  // Special handling for /login/oauth2 — Express strips the mount point from req.url,
  // so we append the path to the target URL to preserve the full path for Spring Security
  server.use(
    '/login/oauth2',
    createProxyMiddleware({
      target: backendUrl + '/login/oauth2',
      changeOrigin: true,
    }),
  );

  // Serve static files from /browser
  server.use(
    express.static(browserDistFolder, {
      maxAge: '1y',
      index: false,
    }),
  );

  // All regular routes: SSR for public pages, CSR fallback for protected pages
  server.get('/{*path}', (req, res, next) => {
    const { protocol, originalUrl, baseUrl, headers } = req;

    // For client-only routes, serve the CSR index.html
    if (isClientOnlyRoute(originalUrl)) {
      res.sendFile(join(browserDistFolder, 'index.csr.html'));
      return;
    }

    commonEngine
      .render({
        bootstrap,
        documentFilePath: indexHtml,
        url: `${protocol}://${headers.host}${originalUrl}`,
        publicPath: browserDistFolder,
        providers: [{ provide: APP_BASE_HREF, useValue: baseUrl }],
      })
      .then((html) => res.send(html))
      .catch((err) => next(err));
  });

  // Error handler — capture stack trace SSR fail (sinon Express silent).
  server.use((err: Error, req: express.Request, res: express.Response, _next: express.NextFunction) => {
    console.error(JSON.stringify({
      ts: new Date().toISOString(),
      level: 'error',
      msg: 'ssr_render_error',
      url: req.originalUrl,
      err: err.message,
      stack: err.stack?.split('\n').slice(0, 5).join(' | '),
    }));
    if (!res.headersSent) {
      res.status(500).sendFile(join(browserDistFolder, 'index.csr.html'));
    }
  });

  return server;
}

const server = app();

if (isMainModule(import.meta.url)) {
  const port = process.env['PORT'] || 80;
  server.listen(port, () => {
    console.log(`Node Express SSR server listening on http://localhost:${port}`);
  });
}

/**
 * Check if the current module is the main entry point.
 */
function isMainModule(url: string): boolean {
  return url.startsWith('file:') && process.argv[1] === fileURLToPath(url);
}

export const reqHandler = createNodeRequestHandler(server);

// Re-export for Angular CLI compatibility
import { createNodeRequestHandler } from '@angular/ssr/node';
