import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly darkMode = signal(true); // Dark by default
  readonly isDark = this.darkMode.asReadonly();

  toggle(): void {
    this.darkMode.update((v) => !v);
    document.documentElement.classList.toggle('dark');
    localStorage.setItem('lmp-theme', this.darkMode() ? 'dark' : 'light');
  }

  init(): void {
    const saved = localStorage.getItem('lmp-theme');
    const prefersDark = window.matchMedia(
      '(prefers-color-scheme: dark)',
    ).matches;
    const useDark = saved ? saved === 'dark' : prefersDark;
    this.darkMode.set(useDark);
    document.documentElement.classList.toggle('dark', useDark);
  }
}
