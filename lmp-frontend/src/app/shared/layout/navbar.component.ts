import {
  Component,
  inject,
  signal,
  OnInit,
  OnDestroy,
  Inject,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser, NgClass } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { LucideAngularModule, Menu, X, Sun, Moon } from 'lucide-angular';
import { ThemeService } from '../../core/services';
import { AuthService } from '../../core/services/auth.service';
import { HlmButton } from '@spartan-ng/helm/button';

@Component({
  selector: 'lmp-navbar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    LucideAngularModule,
    HlmButton,
    NgClass,
  ],
  template: `
    <header
      class="fixed top-0 right-0 left-0 z-50 transition-colors duration-200"
      [ngClass]="{
        'bg-transparent': isAtTop(),
        'bg-(--background)/95 backdrop-blur-sm border-b border-(--border)':
          !isAtTop(),
      }"
    >
      <nav
      class="mx-auto flex h-14 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8 overflow-visible"
      >
        <!-- Logo -->
        <a routerLink="/" class="flex items-center" [ngClass]="isAtTop() ? 'self-start' : 'self-center'">
          <img
            src="/images/logo-lmp.webp"
            alt="LMP Logo"
            class="w-auto rounded-sm transition-all duration-300 ease-in-out"
            [ngClass]="isAtTop() ? 'h-20 drop-shadow-md' : 'h-9 shadow-xs'"
          />
        </a>

        <!-- Desktop Nav Links -->
        <div class="hidden items-center gap-1 md:flex">
          @for (link of navLinks; track link.path) {
            <a
              [routerLink]="link.path"
              routerLinkActive="text-(--foreground)"
              [routerLinkActiveOptions]="{ exact: link.path === '/' }"
              class="px-3 py-2 text-sm font-medium text-(--muted-foreground) transition-colors duration-150 hover:text-(--foreground)"
            >
              {{ link.label }}
            </a>
          }
        </div>

        <!-- Desktop Actions -->
        <div class="hidden items-center gap-2 md:flex">
          <!-- Theme Toggle -->
          <button
            hlmBtn
            variant="ghost"
            size="icon"
            (click)="toggleTheme()"
            class="cursor-pointer"
            [attr.aria-label]="themeService.isDark() ? 'Passer au thème clair' : 'Passer au thème sombre'"
          >
            @if (themeService.isDark()) {
              <lucide-icon [img]="SunIcon" [size]="16"></lucide-icon>
            } @else {
              <lucide-icon [img]="MoonIcon" [size]="16"></lucide-icon>
            }
          </button>

          <!-- Language Selector -->
          <div class="relative">
            <button
              class="flex cursor-pointer items-center gap-1.5 px-2 py-1.5 text-sm text-(--muted-foreground) transition-colors hover:text-(--foreground)"
              (click)="langMenuOpen.set(!langMenuOpen())"
            >
              <img
                src="https://flagcdn.com/w40/fr.png"
                alt="Drapeau français"
                class="h-3.5 w-5 rounded-xs object-cover"
              />
              <span class="font-medium text-xs">FR</span>
              <svg
                class="h-3 w-3"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M19 9l-7 7-7-7"
                />
              </svg>
            </button>
            @if (langMenuOpen()) {
              <div
                class="absolute right-0 mt-1 w-40 rounded-sm border border-(--border) bg-(--card) py-1 shadow-sm"
              >
                @for (lang of languages; track lang.code) {
                  <button
                    class="flex w-full cursor-pointer items-center gap-2 px-3 py-1.5 text-sm text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
                    (click)="langMenuOpen.set(false)"
                  >
                    <img
                      [src]="lang.flag"
                      [alt]="lang.label"
                      class="h-3.5 w-5 rounded-xs object-cover"
                    />
                    {{ lang.label }}
                  </button>
                }
              </div>
            }
          </div>

          <!-- Connexion Button -->
          @if (authService.loading()) {
            <div class="w-[90px] h-8 rounded-sm bg-(--muted) animate-pulse"></div>
          } @else if (authService.isAuthenticated()) {
            @if (authService.isAdmin()) {
              <a
                hlmBtn
                variant="ghost"
                routerLink="/admin"
                class="cursor-pointer gap-1 text-(--primary) text-sm"
              >
                Admin
              </a>
            }
            <a
              hlmBtn
              variant="ghost"
              routerLink="/dashboard"
              class="cursor-pointer text-sm"
            >
              Dashboard
            </a>
          } @else {
            <a
              routerLink="/login"
              class="cursor-pointer rounded-sm border border-(--border) px-4 py-1.5 text-sm font-medium text-(--foreground) transition-colors hover:bg-(--accent)"
            >
              Connexion
            </a>
          }
        </div>

        <!-- Mobile Menu Toggle -->
        <button
          hlmBtn
          variant="ghost"
          size="icon"
          class="cursor-pointer md:hidden"
          (click)="mobileMenuOpen.set(!mobileMenuOpen())"
          [attr.aria-label]="mobileMenuOpen() ? 'Fermer le menu' : 'Ouvrir le menu'"
          aria-controls="mobile-menu"
        >
          @if (mobileMenuOpen()) {
            <lucide-icon [img]="XIcon" [size]="18"></lucide-icon>
          } @else {
            <lucide-icon [img]="MenuIcon" [size]="18"></lucide-icon>
          }
        </button>
      </nav>

      <!-- Mobile Menu -->
      @if (mobileMenuOpen()) {
        <div
          class="border-t border-(--border) bg-(--background) px-4 pb-4 pt-2 md:hidden"
        >
          <div class="flex flex-col gap-1">
            @for (link of navLinks; track link.path) {
              <a
                [routerLink]="link.path"
                routerLinkActive="text-(--foreground)"
                [routerLinkActiveOptions]="{ exact: link.path === '/' }"
                class="px-3 py-2 text-sm font-medium text-(--muted-foreground) transition-colors duration-150 hover:text-(--foreground)"
                (click)="mobileMenuOpen.set(false)"
              >
                {{ link.label }}
              </a>
            }
          </div>

          <div
            class="mt-3 flex flex-col gap-2 border-t border-(--border) pt-3"
          >
            <div class="flex items-center justify-between px-3">
              <span class="text-sm text-(--muted-foreground)">Thème</span>
              <button
                hlmBtn
                variant="ghost"
                size="icon-sm"
                (click)="toggleTheme()"
                class="cursor-pointer"
              >
                @if (themeService.isDark()) {
                  <lucide-icon [img]="SunIcon" [size]="16"></lucide-icon>
                } @else {
                  <lucide-icon [img]="MoonIcon" [size]="16"></lucide-icon>
                }
              </button>
            </div>

            @if (authService.loading()) {
              <div class="w-full h-9 rounded-sm bg-(--muted) animate-pulse"></div>
            } @else if (!authService.isAuthenticated()) {
              <a
                routerLink="/login"
                class="w-full cursor-pointer rounded-sm border border-(--border) px-4 py-2 text-center text-sm font-medium text-(--foreground) transition-colors hover:bg-(--accent)"
                (click)="mobileMenuOpen.set(false)"
              >
                Connexion
              </a>
            }
          </div>
        </div>
      }
    </header>
  `,
})
export class NavbarComponent implements OnInit, OnDestroy {
  protected readonly themeService = inject(ThemeService);
  protected readonly authService = inject(AuthService);

  readonly mobileMenuOpen = signal(false);
  readonly langMenuOpen = signal(false);
  readonly isAtTop = signal(true);

  readonly MenuIcon = Menu;
  readonly XIcon = X;
  readonly SunIcon = Sun;
  readonly MoonIcon = Moon;

  private isBrowser: boolean;
  private scrollHandler: (() => void) | null = null;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  readonly navLinks = [
    { path: '/', label: 'Accueil' },
    { path: '/services', label: 'Services' },
    { path: '/map', label: 'Carte' },
    { path: '/about', label: 'À propos' },
    { path: '/contact', label: 'Contact' },
  ];

  readonly languages = [
    { code: 'fr', label: 'Français', flag: 'https://flagcdn.com/w40/fr.png' },
    { code: 'en', label: 'English', flag: 'https://flagcdn.com/w40/us.png' },
    { code: 'es', label: 'Español', flag: 'https://flagcdn.com/w40/es.png' },
    { code: 'de', label: 'Deutsch', flag: 'https://flagcdn.com/w40/de.png' },
    { code: 'it', label: 'Italiano', flag: 'https://flagcdn.com/w40/it.png' },
    {
      code: 'nl',
      label: 'Nederlands',
      flag: 'https://flagcdn.com/w40/nl.png',
    },
    {
      code: 'pt',
      label: 'Português',
      flag: 'https://flagcdn.com/w40/br.png',
    },
    {
      code: 'lb',
      label: 'Lëtzebuergesch',
      flag: 'https://flagcdn.com/w40/lu.png',
    },
  ];

  ngOnInit(): void {
    if (!this.isBrowser) return;

    this.scrollHandler = () => {
      this.isAtTop.set(window.scrollY <= 10);
    };

    window.addEventListener('scroll', this.scrollHandler, { passive: true });
    this.scrollHandler();
  }

  ngOnDestroy(): void {
    if (this.scrollHandler && this.isBrowser) {
      window.removeEventListener('scroll', this.scrollHandler);
    }
  }

  toggleTheme(): void {
    this.themeService.toggle();
  }
}
