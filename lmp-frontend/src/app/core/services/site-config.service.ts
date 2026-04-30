import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface SiteConfig {
  baseUrl: string;
  frontendUrl: string;
  siteName: string;
  supportEmail: string;
  noreplyEmail?: string;

  // URLs canoniques d'authentification (host auth = OIDC issuer).
  // Nécessaires car les pages auth (/login, /register, /forgot-password, etc.)
  // ne sont accessibles QUE sur l'host auth (OidcHostGuardFilter bloque sur les autres).
  authBaseUrl?: string;
  loginUrl?: string;
  registerUrl?: string;
  forgotPasswordUrl?: string;
  resetPasswordUrl?: string;
  verifyEmailUrl?: string;
  oauth2GoogleAuthUrl?: string;
  oauth2MicrosoftAuthUrl?: string;
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
      const fallbackOrigin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:4200';
      this.config.set({
        baseUrl: fallbackOrigin,
        frontendUrl: fallbackOrigin,
        siteName: 'LMP Digital Services',
        supportEmail: 'support@localhost',
        // Sans config backend, on retombe sur le host courant pour l'auth — UX dégradée mais ne bloque pas le démarrage.
        authBaseUrl: fallbackOrigin,
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

  /**
   * URL de base de l'host auth (= app.oauth2.issuer-uri côté backend).
   * Toutes les URLs d'authentification doivent dériver de là.
   */
  get authBaseUrl(): string {
    return this.config()?.authBaseUrl ?? this.baseUrl;
  }

  // ─── Navigations auth (cross-host) ──────────────────────────────────
  // Ces méthodes utilisent window.location.href car elles peuvent traverser
  // l'host (dev.* → auth.*). Le router Angular ne peut pas faire ça.

  /** Ouvre la page login. {@code returnUrl} sera renvoyé après login. */
  goToLogin(returnUrl?: string): void {
    const target = this.appendReturnUrl(this.config()?.loginUrl ?? `${this.authBaseUrl}/login`, returnUrl);
    this.navigate(target);
  }

  goToRegister(returnUrl?: string): void {
    const target = this.appendReturnUrl(this.config()?.registerUrl ?? `${this.authBaseUrl}/register`, returnUrl);
    this.navigate(target);
  }

  goToForgotPassword(): void {
    this.navigate(this.config()?.forgotPasswordUrl ?? `${this.authBaseUrl}/forgot-password`);
  }

  goToOAuthGoogle(): void {
    this.navigate(this.config()?.oauth2GoogleAuthUrl ?? `${this.authBaseUrl}/oauth2/authorization/google`);
  }

  goToOAuthMicrosoft(): void {
    this.navigate(this.config()?.oauth2MicrosoftAuthUrl ?? `${this.authBaseUrl}/oauth2/authorization/microsoft`);
  }

  /** URL de logout (à utiliser comme href ou via window.location). */
  get logoutUrl(): string {
    return `${this.authBaseUrl}/logout`;
  }

  /** URL de la page login pour utilisation comme {@code href} de bouton. */
  get loginHref(): string {
    return this.config()?.loginUrl ?? `${this.authBaseUrl}/login`;
  }

  /** URL de la page register pour utilisation comme {@code href}. */
  get registerHref(): string {
    return this.config()?.registerUrl ?? `${this.authBaseUrl}/register`;
  }

  /** URL forgot-password pour utilisation comme {@code href}. */
  get forgotPasswordHref(): string {
    return this.config()?.forgotPasswordUrl ?? `${this.authBaseUrl}/forgot-password`;
  }

  /** URLs OAuth pour utilisation comme {@code href} sur les boutons social login. */
  get oauthGoogleHref(): string {
    return this.config()?.oauth2GoogleAuthUrl ?? `${this.authBaseUrl}/oauth2/authorization/google`;
  }

  get oauthMicrosoftHref(): string {
    return this.config()?.oauth2MicrosoftAuthUrl ?? `${this.authBaseUrl}/oauth2/authorization/microsoft`;
  }

  private appendReturnUrl(url: string, returnUrl?: string): string {
    if (!returnUrl) return url;
    const sep = url.includes('?') ? '&' : '?';
    return `${url}${sep}return_to=${encodeURIComponent(returnUrl)}`;
  }

  private navigate(url: string): void {
    if (typeof window !== 'undefined') {
      window.location.href = url;
    }
  }
}

/**
 * Factory pour APP_INITIALIZER.
 */
export function initSiteConfig(siteConfig: SiteConfigService): () => Promise<void> {
  return () => siteConfig.load();
}
