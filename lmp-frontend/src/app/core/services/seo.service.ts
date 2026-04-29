import { Injectable, inject } from '@angular/core';
import { Title, Meta } from '@angular/platform-browser';
import { DOCUMENT } from '@angular/common';
import { SiteConfigService } from './site-config.service';

export interface SeoConfig {
  title: string;
  description: string;
  url: string;
  keywords?: string;
  ogImage?: string;
  ogType?: string;
}

const SITE_NAME = 'LMP Digital Services';

@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly titleService = inject(Title);
  private readonly metaService = inject(Meta);
  private readonly document = inject(DOCUMENT);
  private readonly siteConfig = inject(SiteConfigService);

  readonly baseUrl = this.siteConfig.baseUrl;

  /**
   * Update all SEO meta tags for the current page.
   * Call this in ngOnInit() of each routed component.
   *
   * Uses removeTag + addTag instead of updateTag for SSR safety:
   * updateTag can fail silently during SSR if the selector doesn't match.
   */
  updateMeta(config: SeoConfig): void {
    const fullTitle = config.title.includes(SITE_NAME)
      ? config.title
      : `${config.title} | ${SITE_NAME}`;
    const fullUrl = config.url.startsWith('http')
      ? config.url
      : `${this.siteConfig.baseUrl}${config.url}`;
    const ogImage = config.ogImage ?? `${this.siteConfig.baseUrl}/images/logo-lmp.webp`;

    // Title
    this.titleService.setTitle(fullTitle);

    // Standard meta — remove then add for SSR safety
    this.removeAddMeta('name', 'description', config.description);
    if (config.keywords) {
      this.removeAddMeta('name', 'keywords', config.keywords);
    }

    // Open Graph — remove then add
    this.removeAddMeta('property', 'og:title', fullTitle);
    this.removeAddMeta('property', 'og:description', config.description);
    this.removeAddMeta('property', 'og:url', fullUrl);
    this.removeAddMeta('property', 'og:type', config.ogType ?? 'website');
    this.removeAddMeta('property', 'og:image', ogImage);

    // Twitter Card — remove then add
    this.removeAddMeta('name', 'twitter:title', fullTitle);
    this.removeAddMeta('name', 'twitter:description', config.description);
    this.removeAddMeta('name', 'twitter:image', ogImage);

    // Canonical URL
    this.updateCanonical(fullUrl);
  }

  /**
   * Add or update a JSON-LD structured data script tag.
   * Call this in ngOnInit() after updateMeta().
   */
  setJsonLd(data: object): void {
    const existing = this.document.querySelector('script[type="application/ld+json"]');
    if (existing) {
      existing.textContent = JSON.stringify(data);
    } else {
      const script = this.document.createElement('script');
      script.type = 'application/ld+json';
      script.textContent = JSON.stringify(data);
      this.document.head.appendChild(script);
    }
  }

  /**
   * Remove any existing JSON-LD script.
   * Call this in ngOnDestroy() if the component is unmounted.
   */
  removeJsonLd(): void {
    const existing = this.document.querySelector('script[type="application/ld+json"]');
    if (existing) {
      existing.remove();
    }
  }

  private removeAddMeta(
    attr: 'name' | 'property',
    key: string,
    content: string,
  ): void {
    // Remove existing tag to avoid duplicates in SSR
    try {
      this.metaService.removeTag(`${attr}='${key}'`);
    } catch {
      // ignore if not present
    }
    this.metaService.addTag({ [attr]: key, content });
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
