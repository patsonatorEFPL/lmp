import { Injectable, inject } from '@angular/core';
import { Title, Meta } from '@angular/platform-browser';
import { DOCUMENT } from '@angular/common';

export interface SeoConfig {
  title: string;
  description: string;
  url: string;
  keywords?: string;
  ogImage?: string;
  ogType?: string;
}

const BASE_URL = 'https://lmp-services.ca';
const DEFAULT_OG_IMAGE = `${BASE_URL}/images/logo-lmp.webp`;
const SITE_NAME = 'LMP Digital Services';

@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly titleService = inject(Title);
  private readonly metaService = inject(Meta);
  private readonly document = inject(DOCUMENT);

  /**
   * Update all SEO meta tags for the current page.
   * Call this in ngOnInit() of each routed component.
   */
  updateMeta(config: SeoConfig): void {
    const fullTitle = config.title.includes(SITE_NAME)
      ? config.title
      : `${config.title} | ${SITE_NAME}`;
    const fullUrl = config.url.startsWith('http')
      ? config.url
      : `${BASE_URL}${config.url}`;
    const ogImage = config.ogImage ?? DEFAULT_OG_IMAGE;

    // Title
    this.titleService.setTitle(fullTitle);

    // Standard meta
    this.metaService.updateTag({ name: 'description', content: config.description });
    if (config.keywords) {
      this.metaService.updateTag({ name: 'keywords', content: config.keywords });
    }

    // Open Graph
    this.metaService.updateTag({ property: 'og:title', content: fullTitle });
    this.metaService.updateTag({ property: 'og:description', content: config.description });
    this.metaService.updateTag({ property: 'og:url', content: fullUrl });
    this.metaService.updateTag({ property: 'og:type', content: config.ogType ?? 'website' });
    this.metaService.updateTag({ property: 'og:image', content: ogImage });

    // Twitter Card
    this.metaService.updateTag({ name: 'twitter:title', content: fullTitle });
    this.metaService.updateTag({ name: 'twitter:description', content: config.description });
    this.metaService.updateTag({ name: 'twitter:image', content: ogImage });

    // Canonical URL
    this.updateCanonical(fullUrl);
  }

  private updateCanonical(url: string): void {
    let link: HTMLLinkElement | null = this.document.querySelector('link[rel="canonical"]');
    if (link) {
      link.setAttribute('href', url);
    } else {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      link.setAttribute('href', url);
      this.document.head.appendChild(link);
    }
  }
}
