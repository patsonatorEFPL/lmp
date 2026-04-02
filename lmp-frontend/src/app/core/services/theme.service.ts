import {
  Injectable,
  signal,
  inject,
  PLATFORM_ID,
  computed,
  effect,
  untracked,
} from '@angular/core';
import { isPlatformBrowser, DOCUMENT } from '@angular/common';

export type ThemePreference = 'light' | 'dark' | 'system';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly document = inject(DOCUMENT);

  private readonly preference = signal<ThemePreference>('system');
  private readonly systemIsDark = signal(false);

  private mql: MediaQueryList | null = null;
  private mqlListener?: (e: MediaQueryListEvent) => void;

  readonly themePreference = this.preference.asReadonly();

  /** Thème effectif (inclut le choix « système »). */
  readonly isDark = computed(() => {
    const p = this.preference();
    if (p === 'dark') return true;
    if (p === 'light') return false;
    return this.systemIsDark();
  });

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      effect(() => {
        const dark = this.isDark();
        untracked(() => {
          this.document.documentElement.classList.toggle('dark', dark);
        });
      });
    }
  }

  setPreference(p: ThemePreference): void {
    this.preference.set(p);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('lmp-theme', p);
    }
  }

  /**
   * Compat : cycle clair → sombre → système (ex. ancien bouton seul dans la barre).
   */
  toggle(): void {
    const cur = this.preference();
    if (cur === 'light') this.setPreference('dark');
    else if (cur === 'dark') this.setPreference('system');
    else this.setPreference('light');
  }

  init(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const saved = localStorage.getItem('lmp-theme');
    if (saved === 'light' || saved === 'dark' || saved === 'system') {
      this.preference.set(saved);
    } else {
      this.preference.set('system');
    }

    this.mql = window.matchMedia('(prefers-color-scheme: dark)');
    this.systemIsDark.set(this.mql.matches);
    this.mqlListener = (e: MediaQueryListEvent) => this.systemIsDark.set(e.matches);
    this.mql.addEventListener('change', this.mqlListener);
  }
}
