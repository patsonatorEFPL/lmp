/**
 * Polyfill: sockjs-client expects `global` to be defined (Node.js env).
 * In browser environments we alias it to `window`.
 */
(window as any).global = window;

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
