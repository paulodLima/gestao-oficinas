import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { request as httpRequest } from 'node:http';
import { request as httpsRequest } from 'node:https';

const serverDistFolder = dirname(fileURLToPath(import.meta.url));
const browserDistFolder = resolve(serverDistFolder, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine();
const apiTarget = new URL(process.env['API_URL'] || 'http://localhost:8080');
if (!['http:', 'https:'].includes(apiTarget.protocol)) throw new Error('API_URL inválida');
app.use('/api', (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  const send = apiTarget.protocol === 'https:' ? httpsRequest : httpRequest;
  const upstream = send({
    hostname: apiTarget.hostname, port: apiTarget.port, protocol: apiTarget.protocol,
    method: req.method, path: req.originalUrl,
    headers: { ...req.headers, host: apiTarget.host,
      'x-oficinas-client-ip': req.socket.remoteAddress || 'unknown' }, timeout: 15000
  }, response => {
    res.writeHead(response.statusCode || 502, response.headers);
    response.pipe(res);
  });
  upstream.on('timeout', () => upstream.destroy(new Error('Timeout')));
  upstream.on('error', () => {
    if (!res.headersSent) res.status(502).json({ detail: 'Serviço indisponível. Tente novamente.' });
    else res.destroy();
  });
  req.on('aborted', () => upstream.destroy());
  req.pipe(upstream);
});

/**
 * Example Express Rest API endpoints can be defined here.
 * Uncomment and define endpoints as necessary.
 *
 * Example:
 * ```ts
 * app.get('/api/**', (req, res) => {
 *   // Handle API request
 * });
 * ```
 */

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use('/**', (req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

/**
 * Start the server if this module is the main entry point.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url)) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, () => {
    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * The request handler used by the Angular CLI (dev-server and during build).
 */
export const reqHandler = createNodeRequestHandler(app);
