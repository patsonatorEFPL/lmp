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
  const commonEngine = new CommonEngine({
    allowedHosts: [
      'localhost',
      'lmp-services.ca',
      'dev.lmp-services.ca',
    ],
  });

  /**
   * Backend URL for SSR API proxy.
   * In Docker / production: BACKEND_URL = http://lmp-spring-app:8080
   * In local dev:           BACKEND_URL = http://localhost:8080
   */
  const backendUrl = process.env['BACKEND_URL'] || 'http://localhost:8080';

  // Proxy API, OAuth2 and Actuator requests to Spring Boot backend
  const proxyPaths = ['/api', '/oauth2', '/actuator'];
  for (const path of proxyPaths) {
    server.use(
      path,
      createProxyMiddleware({
        target: backendUrl,
        changeOrigin: true,
      }),
    );
  }

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
