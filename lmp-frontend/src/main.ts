import { bootstrapApplication } from '@angular/platform-browser';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { provideBrowserGlobalErrorListeners } from '@angular/core';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Umami self-hosted analytics (privacy-first, GDPR-compliant).
// Injected via DOM API at browser bootstrap because Angular SSR + beasties
// strip external <script> tags from index.html during the build pipeline.
if (typeof document !== 'undefined' && !document.querySelector('script[data-website-id="01e9693f-ee5c-468d-87d6-39116466fd55"]')) {
  const umamiScript = document.createElement('script');
  umamiScript.defer = true;
  umamiScript.src = 'https://analytics.lmp-services.ca/script.js';
  umamiScript.setAttribute('data-website-id', '01e9693f-ee5c-468d-87d6-39116466fd55');
  document.head.appendChild(umamiScript);
}

bootstrapApplication(App, {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideClientHydration(withEventReplay()),
    ...appConfig.providers,
  ],
}).catch((err) => console.error(err));
