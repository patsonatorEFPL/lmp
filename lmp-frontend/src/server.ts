import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createProxyMiddleware } from 'http-proxy-middleware';

const serverDistFolder = dirname(fileURLToPath(import.meta.url));
const browserDistFolder = resolve(serverDistFolder, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine();

// ── API / WebSocket proxy to the Spring Boot backend ──
const BACKEND_URL = process.env['BACKEND_URL'] || 'http://localhost:8080';

// Proxy API, OAuth, Actuator and WebSocket requests to the backend
for (const path of ['/api', '/oauth2', '/actuator']) {
  app.use(path, createProxyMiddleware({ target: BACKEND_URL, changeOrigin: true }));
}
app.use('/ws', createProxyMiddleware({ target: BACKEND_URL, changeOrigin: true, ws: true }));

// ── Serve static files from /browser ──
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
  }),
);

// ── All other routes: Angular SSR ──
app.use('/**', (req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

// ── Start the server ──
if (isMainModule(import.meta.url)) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, () => {
    console.log(`Node Express SSR server listening on http://localhost:${port}`);
  });
}

export const reqHandler = createNodeRequestHandler(app);
