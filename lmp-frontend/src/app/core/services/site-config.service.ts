import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface SiteConfig {
  baseUrl: string;
  frontendUrl: string;
  siteName: string;
  supportEmail: string;
}

/**
 * Configuration du site chargée dynamiquement depuis le backend.
 * Permet de ne jamais coder le domaine en dur dans le frontend.
 */
@Injectable({ providedIn: 'root' })
export class SiteConfigService {
  private readonly http = inject(HttpClient);

  readonly config = signal<SiteConfig | null>(null);

  /**
   * Charge la configuration depuis le backend.
   * Appelé via APP_INITIALIZER au démarrage de l'application.
   */
  async load(): Promise<void> {
    try {
      const cfg = await firstValueFrom(
        this.http.get<SiteConfig>('/api/v1/config')
      );
      this.config.set(cfg);
    } catch (e) {
      // Fallback silencieux pour ne pas bloquer le démarrage
      console.warn('[SiteConfig] Impossible de charger la config backend, fallback localhost', e);
      this.config.set({
        baseUrl: typeof window !== 'undefined' ? window.location.origin : 'http://localhost:4200',
        frontendUrl: typeof window !== 'undefined' ? window.location.origin : 'http://localhost:4200',
        siteName: 'LMP Digital Services',
        supportEmail: 'support@localhost',
      });
    }
  }

  get baseUrl(): string {
    return this.config()?.baseUrl ?? 'http://localhost:4200';
  }

  get frontendUrl(): string {
    return this.config()?.frontendUrl ?? 'http://localhost:4200';
  }

  get siteName(): string {
    return this.config()?.siteName ?? 'LMP Digital Services';
  }

  get supportEmail(): string {
    return this.config()?.supportEmail ?? 'support@localhost';
  }
}

/**
 * Factory pour APP_INITIALIZER.
 */
export function initSiteConfig(siteConfig: SiteConfigService): () => Promise<void> {
  return () => siteConfig.load();
}
