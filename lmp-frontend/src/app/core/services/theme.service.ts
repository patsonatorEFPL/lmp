import { Injectable, signal, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, DOCUMENT } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly document = inject(DOCUMENT);
  private readonly darkMode = signal(true); // Dark by default
  readonly isDark = this.darkMode.asReadonly();

  toggle(): void {
    this.darkMode.update((v) => !v);
    if (isPlatformBrowser(this.platformId)) {
      this.document.documentElement.classList.toggle('dark');
      localStorage.setItem('lmp-theme', this.darkMode() ? 'dark' : 'light');
    }
  }

  init(): void {
    if (!isPlatformBrowser(this.platformId)) {
      // During SSR/prerender, keep dark mode default (matches class="dark" on <html>)
      return;
    }

    const saved = localStorage.getItem('lmp-theme');
    const prefersDark = window.matchMedia(
      '(prefers-color-scheme: dark)',
    ).matches;
    const useDark = saved ? saved === 'dark' : prefersDark;
    this.darkMode.set(useDark);
    this.document.documentElement.classList.toggle('dark', useDark);
  }
}
