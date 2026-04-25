import {
  Component,
  inject,
  signal,
  OnInit,
  OnDestroy,
  Inject,
  PLATFORM_ID,
  HostListener,
  computed,
  viewChild,
  ElementRef,
} from '@angular/core';
import { isPlatformBrowser, NgClass } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  LucideAngularModule,
  Menu,
  X,
  Sun,
  Moon,
  User,
  LogIn,
  UserPlus,
  Monitor,
  ChevronRight,
  Globe,
  HelpCircle,
  Check,
  LayoutDashboard,
  Shield,
  LogOut,
} from 'lucide-angular';
import { ThemeService, type ThemePreference } from '../../core/services/theme.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'lmp-navbar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    LucideAngularModule,
    NgClass,
  ],
  styles: `
    :host {
      display: block;
      overflow: visible;
    }
  `,
  template: `
    <header
      class="fixed top-0 right-0 left-0 overflow-visible transition-colors duration-200"
      [ngClass]="{
        'bg-transparent': isAtTop(),
        'bg-(--background)/95 backdrop-blur-sm border-b border-(--border)':
          !isAtTop() && !mobileMenuOpen(),
        'bg-(--background)/95 border-b border-(--border)':
          !isAtTop() && mobileMenuOpen(),
        'z-[1200]': mobileMenuOpen() || accountMenuOpen(),
        'z-50': !mobileMenuOpen() && !accountMenuOpen(),
      }"
    >
      <nav
        class="relative mx-auto flex min-h-14 max-w-7xl items-center gap-2 px-3 sm:gap-3 sm:px-6 lg:px-8 overflow-visible md:min-h-0"
        [ngClass]="{
          'max-md:items-start max-md:pt-2 md:items-center': isAtTop(),
          'items-center': !isAtTop(),
          'max-md:z-[85]': mobileMenuOpen(),
        }"
      >
        <!-- Logo (hauteur réduite sur mobile pour éviter de masquer le hero) -->
        <a
          routerLink="/"
          class="flex shrink-0 items-center"
          [ngClass]="isAtTop() ? 'max-md:self-start md:self-start' : 'self-center'"
        >
          <img
            src="/images/logo-lmp.webp"
            alt="LMP Logo"
            class="w-auto rounded-sm transition-all duration-300 ease-in-out"
            [ngClass]="
              isAtTop()
                ? 'h-12 max-h-[3rem] drop-shadow-md sm:h-14 md:h-20 md:max-h-none'
                : 'h-9 shadow-xs'
            "
          />
        </a>

        <!-- Liens centrés (desktop / tablette) — inert + aria-hidden sous breakpoint md (viewport réel) -->
        <div
          class="hidden min-w-0 flex-1 justify-center gap-0.5 px-1 md:flex lg:gap-1"
          [attr.aria-hidden]="isMdUp() ? null : 'true'"
          [attr.inert]="!isMdUp() ? '' : null"
        >
          @for (link of navLinks; track link.path) {
            <a
              [routerLink]="link.path"
              routerLinkActive="text-(--foreground)"
              [routerLinkActiveOptions]="{ exact: link.path === '/' }"
              class="shrink-0 px-2 py-2 text-xs font-medium text-(--muted-foreground) transition-colors duration-150 hover:text-(--foreground) lg:px-3 lg:text-sm"
            >
              {{ link.label }}
            </a>
          }
        </div>

        <!-- Actions : invité ou connecté = menu compte seul (tableau de bord, admin, thème, langue, aide, déconnexion dans le panneau) -->
        <div class="ml-auto flex shrink-0 items-center gap-1.5 sm:gap-2">
          @if (authService.loading()) {
            <div
              class="lmp-nav-auth-placeholder h-9 w-9 shrink-0 rounded-full animate-pulse bg-(--muted)"
              aria-hidden="true"
            ></div>
          } @else if (authService.isAuthenticated()) {
            <div class="relative" #accountMenuHost>
              <button
                type="button"
                class="inline-flex h-9 w-9 shrink-0 cursor-pointer items-center justify-center rounded-full text-[11px] font-semibold tracking-wide text-white transition-colors focus-visible:ring-2 focus-visible:ring-(--ring) focus-visible:outline-none"
                [style.background]="connectedAccountAvatarBg()"
                (click)="$event.stopPropagation(); toggleAccountMenu()"
                (keydown.enter)="$event.preventDefault(); openAccountMenuFromKeyboard()"
                (keydown.space)="$event.preventDefault(); openAccountMenuFromKeyboard()"
                [attr.aria-expanded]="accountMenuOpen()"
                aria-haspopup="true"
                aria-label="Menu compte"
              >
                {{ connectedAccountInitials() || '?' }}
              </button>

              @if (accountMenuOpen()) {
                <div
                  class="absolute right-0 z-[60] mt-2 flex w-max max-w-[min(calc(100vw-2rem),18.5rem)] max-h-[min(70dvh,calc(100dvh-5rem))] flex-col overflow-x-hidden overflow-y-auto overscroll-contain rounded-xl border border-(--border) bg-(--card) py-1 text-(--foreground) shadow-lg max-sm:right-1"
                  role="menu"
                >
                  <div class="w-full border-b border-(--border) px-3 pb-3 pt-2">
                    <div class="flex gap-3">
                      <div
                        class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-xs font-semibold text-white"
                        [style.background]="connectedAccountAvatarBg()"
                      >
                        {{ connectedAccountInitials() || '?' }}
                      </div>
                      <div class="min-w-0 flex-1 text-left">
                        <p class="truncate text-sm font-semibold">{{ connectedAccountTitle() }}</p>
                        <p class="truncate text-xs text-(--muted-foreground)">
                          {{ connectedAccountEmail() }}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div class="w-full py-1">
                    <a
                      routerLink="/dashboard"
                      role="menuitem"
                      class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                      (click)="closeAccountMenu()"
                    >
                      <lucide-icon
                        [img]="DashboardMenuIcon"
                        [size]="18"
                        class="shrink-0 opacity-80"
                      ></lucide-icon>
                      <span>Tableau de bord</span>
                    </a>
                    @if (authService.isAdmin()) {
                      <a
                        routerLink="/admin"
                        role="menuitem"
                        class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                        (click)="closeAccountMenu()"
                      >
                        <lucide-icon
                          [img]="AdminMenuIcon"
                          [size]="18"
                          class="shrink-0 opacity-80"
                        ></lucide-icon>
                        <span>Administration</span>
                      </a>
                    }
                  </div>

                  <div class="mx-3 h-px shrink-0 bg-(--border)"></div>

                  <div class="w-full py-1">
                    <button
                      type="button"
                      class="flex w-full min-w-0 items-center gap-3 px-3 py-2.5 text-left text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                      (click)="appearanceSubOpen.update((v) => !v); langSubOpen.set(false)"
                    >
                      <lucide-icon [img]="MoonIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
                      <span class="min-w-0 flex-1 truncate"
                        >Apparence&nbsp;: {{ appearanceSummary() }}</span
                      >
                      <lucide-icon
                        [img]="ChevronRightIcon"
                        [size]="16"
                        class="shrink-0 opacity-60 transition-transform"
                        [class.rotate-90]="appearanceSubOpen()"
                      ></lucide-icon>
                    </button>
                    @if (appearanceSubOpen()) {
                      <div class="border-t border-(--border) bg-(--muted)/15 px-2 py-1.5">
                        @for (opt of themeOptions; track opt.value) {
                          <button
                            type="button"
                            class="flex w-full items-center gap-2 rounded-md px-2 py-2 text-left text-sm transition-colors"
                            [class.bg-(--accent)]="themeService.themePreference() === opt.value"
                            (click)="setTheme(opt.value)"
                          >
                            <lucide-icon [img]="opt.icon" [size]="16" class="shrink-0 opacity-80"></lucide-icon>
                            <span class="flex-1">{{ opt.label }}</span>
                            @if (themeService.themePreference() === opt.value) {
                              <lucide-icon
                                [img]="CheckIcon"
                                [size]="16"
                                class="shrink-0 text-(--primary)"
                              ></lucide-icon>
                            }
                          </button>
                        }
                      </div>
                    }

                    <button
                      type="button"
                      class="flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                      (click)="langSubOpen.update((v) => !v); appearanceSubOpen.set(false)"
                    >
                      <lucide-icon [img]="GlobeIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
                      <span class="min-w-0 flex-1 truncate"
                        >Langue&nbsp;: {{ currentLangLabel() }}</span
                      >
                      <lucide-icon
                        [img]="ChevronRightIcon"
                        [size]="16"
                        class="shrink-0 opacity-60 transition-transform"
                        [class.rotate-90]="langSubOpen()"
                      ></lucide-icon>
                    </button>
                    @if (langSubOpen()) {
                      <div class="border-t border-(--border) bg-(--muted)/15 px-2 py-1.5">
                        @for (lang of languages; track lang.code) {
                          <button
                            type="button"
                            class="flex w-full items-center gap-2 rounded-md px-2 py-2 text-left text-sm transition-colors"
                            [class.bg-(--accent)]="selectedLangCode() === lang.code"
                            (click)="pickLanguageGuest(lang.code)"
                          >
                            <img
                              [src]="lang.flag"
                              [alt]="lang.label"
                              class="h-3.5 w-5 shrink-0 rounded-xs object-cover"
                            />
                            <span class="flex-1">{{ lang.label }}</span>
                            @if (selectedLangCode() === lang.code) {
                              <lucide-icon
                                [img]="CheckIcon"
                                [size]="16"
                                class="shrink-0 text-(--primary)"
                              ></lucide-icon>
                            }
                          </button>
                        }
                      </div>
                    }
                  </div>

                  <div class="mx-3 h-px shrink-0 bg-(--border)"></div>

                  <div class="w-full py-1">
                    <a
                      routerLink="/contact"
                      role="menuitem"
                      class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                      (click)="closeAccountMenu()"
                    >
                      <lucide-icon [img]="HelpCircleIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
                      <span>Aide</span>
                    </a>
                    <button
                      type="button"
                      role="menuitem"
                      class="flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                      (click)="logoutFromMenu()"
                    >
                      <lucide-icon [img]="LogOutMenuIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
                      <span>Se déconnecter</span>
                    </button>
                  </div>
                </div>
              }
            </div>
          } @else {
            <!-- Invité : menu compte (style pro, thème + langue regroupés) -->
            <div class="relative" #accountMenuHost>
              <button
                type="button"
                class="inline-flex h-9 w-9 shrink-0 cursor-pointer items-center justify-center rounded-full border border-(--border) bg-(--muted)/35 text-(--muted-foreground) transition-colors hover:bg-(--muted)/55 hover:text-(--foreground) focus-visible:ring-2 focus-visible:ring-(--ring) focus-visible:outline-none"
                (click)="$event.stopPropagation(); toggleAccountMenu()"
                (keydown.enter)="$event.preventDefault(); openAccountMenuFromKeyboard()"
                (keydown.space)="$event.preventDefault(); openAccountMenuFromKeyboard()"
                [attr.aria-expanded]="accountMenuOpen()"
                aria-haspopup="true"
                aria-label="Menu compte et paramètres"
              >
                <lucide-icon [img]="UserIcon" [size]="18"></lucide-icon>
              </button>

              @if (accountMenuOpen()) {
                <div
                  class="absolute right-0 z-[60] mt-2 flex w-max max-w-[min(calc(100vw-2rem),18.5rem)] max-h-[min(70dvh,calc(100dvh-5rem))] flex-col overflow-x-hidden overflow-y-auto overscroll-contain rounded-xl border border-(--border) bg-(--card) py-1 text-(--foreground) shadow-lg max-sm:right-1"
                  role="menu"
                >
                  <div class="w-full border-b border-(--border) px-3 pb-3 pt-2">
                    <div class="flex gap-3">
                      <div
                        class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-(--muted) text-(--muted-foreground)"
                      >
                        <lucide-icon [img]="UserIcon" [size]="20"></lucide-icon>
                      </div>
                      <div class="min-w-0 flex-1 text-left">
                        <p class="truncate text-sm font-semibold">LMP Digital</p>
                        <p class="truncate text-xs text-(--muted-foreground)">
                          Non connecté
                        </p>
                      </div>
                    </div>
                  </div>

                  <div class="w-full py-1">
                    <a
                      routerLink="/login"
                      role="menuitem"
                      class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                      (click)="closeAccountMenu()"
                    >
                      <lucide-icon [img]="LogInIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
                      <span>Se connecter</span>
                    </a>
                    <a
                      routerLink="/register"
                      role="menuitem"
                      class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                      (click)="closeAccountMenu()"
                    >
                      <lucide-icon [img]="UserPlusIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
                      <span>Créer un compte</span>
                    </a>
                  </div>

                  <div class="mx-3 h-px shrink-0 bg-(--border)"></div>

                  <div class="w-full py-1">
                    <button
                      type="button"
                      class="flex w-full min-w-0 items-center gap-3 px-3 py-2.5 text-left text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                      (click)="appearanceSubOpen.update((v) => !v); langSubOpen.set(false)"
                    >
                      <lucide-icon [img]="MoonIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
                      <span class="min-w-0 flex-1 truncate"
                        >Apparence&nbsp;: {{ appearanceSummary() }}</span
                      >
                      <lucide-icon
                        [img]="ChevronRightIcon"
                        [size]="16"
                        class="shrink-0 opacity-60 transition-transform"
                        [class.rotate-90]="appearanceSubOpen()"
                      ></lucide-icon>
                    </button>
                    @if (appearanceSubOpen()) {
                      <div class="border-t border-(--border) bg-(--muted)/15 px-2 py-1.5">
                        @for (opt of themeOptions; track opt.value) {
                          <button
                            type="button"
                            class="flex w-full items-center gap-2 rounded-md px-2 py-2 text-left text-sm transition-colors"
                            [class.bg-(--accent)]="themeService.themePreference() === opt.value"
                            (click)="setTheme(opt.value)"
                          >
                            <lucide-icon [img]="opt.icon" [size]="16" class="shrink-0 opacity-80"></lucide-icon>
                            <span class="flex-1">{{ opt.label }}</span>
                            @if (themeService.themePreference() === opt.value) {
                              <lucide-icon [img]="CheckIcon" [size]="16" class="shrink-0 text-(--primary)"></lucide-icon>
                            }
                          </button>
                        }
                      </div>
                    }

                    <button
                      type="button"
                      class="flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                      (click)="langSubOpen.update((v) => !v); appearanceSubOpen.set(false)"
                    >
                      <lucide-icon [img]="GlobeIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
                      <span class="min-w-0 flex-1 truncate"
                        >Langue&nbsp;: {{ currentLangLabel() }}</span
                      >
                      <lucide-icon
                        [img]="ChevronRightIcon"
                        [size]="16"
                        class="shrink-0 opacity-60 transition-transform"
                        [class.rotate-90]="langSubOpen()"
                      ></lucide-icon>
                    </button>
                    @if (langSubOpen()) {
                      <div class="border-t border-(--border) bg-(--muted)/15 px-2 py-1.5">
                        @for (lang of languages; track lang.code) {
                          <button
                            type="button"
                            class="flex w-full items-center gap-2 rounded-md px-2 py-2 text-left text-sm transition-colors"
                            [class.bg-(--accent)]="selectedLangCode() === lang.code"
                            (click)="pickLanguageGuest(lang.code)"
                          >
                            <img
                              [src]="lang.flag"
                              [alt]="lang.label"
                              class="h-3.5 w-5 shrink-0 rounded-xs object-cover"
                            />
                            <span class="flex-1">{{ lang.label }}</span>
                            @if (selectedLangCode() === lang.code) {
                              <lucide-icon [img]="CheckIcon" [size]="16" class="shrink-0 text-(--primary)"></lucide-icon>
                            }
                          </button>
                        }
                      </div>
                    }
                  </div>

                  <div class="mx-3 h-px shrink-0 bg-(--border)"></div>

                  <div class="w-full py-1">
                    <a
                      routerLink="/contact"
                      role="menuitem"
                      class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                      (click)="closeAccountMenu()"
                    >
                      <lucide-icon [img]="HelpCircleIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
                      <span>Aide</span>
                    </a>
                  </div>
                </div>
              }
            </div>
          }

          <!-- Bouton natif (pas hlmBtn) : meilleure expo a11y / automation sur petit écran -->
          <button
            type="button"
            class="inline-flex h-9 w-9 shrink-0 cursor-pointer items-center justify-center rounded-md md:hidden text-(--muted-foreground) transition-colors hover:bg-(--accent)/50 hover:text-(--foreground) focus-visible:ring-2 focus-visible:ring-(--ring) focus-visible:outline-none"
            (click)="toggleMobileMenu()"
            [attr.aria-expanded]="mobileMenuOpen()"
            [attr.aria-label]="mobileMenuOpen() ? 'Fermer le menu' : 'Ouvrir le menu'"
            aria-controls="mobile-menu"
          >
            <span class="sr-only">{{ mobileMenuOpen() ? 'Fermer le menu' : 'Ouvrir le menu' }}</span>
            @if (mobileMenuOpen()) {
              <lucide-icon [img]="XIcon" [size]="18" aria-hidden="true"></lucide-icon>
            } @else {
              <lucide-icon [img]="MenuIcon" [size]="18" aria-hidden="true"></lucide-icon>
            }
          </button>
        </div>
      </nav>

      <!-- Mobile : zone cliquable transparente (pas de voile ni flou — comme le menu compte) -->
      @if (mobileMenuOpen()) {
        <button
          type="button"
          tabindex="-1"
          class="fixed inset-0 z-[55] cursor-default touch-none overscroll-none bg-transparent md:hidden"
          aria-label="Fermer le menu"
          (click)="closeMobileMenu()"
        ></button>
        <div
          id="mobile-menu"
          role="navigation"
          aria-label="Menu principal"
          class="absolute right-2 top-full z-[60] mt-3 flex h-fit w-max max-w-[min(calc(100vw-2rem),18.5rem)] max-h-[min(65dvh,calc(100dvh-6rem))] flex-col overflow-y-auto overscroll-y-contain rounded-xl border border-(--border) bg-(--card) py-1.5 pl-2 pr-1 pb-[max(0.375rem,env(safe-area-inset-bottom,0px))] text-(--foreground) shadow-2xl ring-1 ring-(--foreground)/8 sm:right-3 md:hidden"
        >
          <!-- self-start : largeur au contenu ; section auth en w-full sous la même colonne -->
          <div class="flex flex-col self-start gap-0">
            @for (link of navLinks; track link.path) {
              <a
                [routerLink]="link.path"
                routerLinkActive="bg-(--accent) font-semibold text-(--foreground)"
                [routerLinkActiveOptions]="{ exact: link.path === '/' }"
                class="rounded-lg px-3 py-2 text-left text-sm font-medium whitespace-nowrap text-(--foreground) transition-colors hover:bg-(--accent)/80 focus-visible:ring-2 focus-visible:ring-(--ring) focus-visible:outline-none"
                (click)="closeMobileMenu()"
              >
                {{ link.label }}
              </a>
            }
          </div>

          @if (authService.loading()) {
            <div class="mt-3 flex w-full flex-col border-t border-(--border) pt-3">
              <div
                class="lmp-nav-auth-placeholder h-10 w-full shrink-0 rounded-sm animate-pulse bg-(--muted)"
                aria-hidden="true"
              ></div>
            </div>
          }
          <!-- Connecté : tableau de bord / admin uniquement dans le menu compte (icône profil), pas ici — parité invité -->
        </div>
      }
    </header>
  `,
})
export class NavbarComponent implements OnInit, OnDestroy {
  protected readonly themeService = inject(ThemeService);
  protected readonly authService = inject(AuthService);

  readonly accountMenuHost = viewChild<ElementRef<HTMLElement>>('accountMenuHost');

  readonly mobileMenuOpen = signal(false);
  readonly isAtTop = signal(true);

  readonly accountMenuOpen = signal(false);
  readonly appearanceSubOpen = signal(false);
  readonly langSubOpen = signal(false);

  readonly selectedLangCode = signal<string>('fr');

  readonly MenuIcon = Menu;
  readonly XIcon = X;
  readonly MoonIcon = Moon;
  readonly UserIcon = User;
  readonly LogInIcon = LogIn;
  readonly UserPlusIcon = UserPlus;
  readonly MonitorIcon = Monitor;
  readonly ChevronRightIcon = ChevronRight;
  readonly GlobeIcon = Globe;
  readonly HelpCircleIcon = HelpCircle;
  readonly CheckIcon = Check;
  readonly DashboardMenuIcon = LayoutDashboard;
  readonly AdminMenuIcon = Shield;
  readonly LogOutMenuIcon = LogOut;

  readonly themeOptions: {
    value: ThemePreference;
    label: string;
    icon: typeof Monitor;
  }[] = [
    { value: 'system', label: 'Système', icon: Monitor },
    { value: 'light', label: 'Clair', icon: Sun },
    { value: 'dark', label: 'Sombre', icon: Moon },
  ];

  private isBrowser: boolean;
  private scrollHandler: (() => void) | null = null;
  private mdMql: MediaQueryList | null = null;
  private mdMqlListener?: () => void;

  /** Aligné sur la breakpoint Tailwind `md` (768px). */
  readonly isMdUp = signal(false);

  readonly currentLangLabel = computed(() => {
    const code = this.selectedLangCode();
    return this.languages.find((l) => l.code === code)?.label ?? 'Français';
  });

  readonly connectedAccountTitle = computed(() => {
    const u = this.authService.user();
    if (!u) return '';
    const d = u.displayName?.trim();
    if (d) return d;
    const name = [u.firstName, u.lastName].filter(Boolean).join(' ').trim();
    if (name) return name;
    return u.email;
  });

  readonly connectedAccountEmail = computed(() => this.authService.user()?.email ?? '');

  readonly connectedAccountInitials = computed(() => {
    const title = this.connectedAccountTitle();
    if (!title) return '';
    return title
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('');
  });

  readonly connectedAccountAvatarBg = computed(() => {
    const name = this.connectedAccountTitle();
    if (!name) return 'linear-gradient(135deg, oklch(0.72 0.15 260), oklch(0.6 0.2 300))';
    let h = 0;
    for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0;
    const hues = [10, 30, 60, 150, 200, 230, 260, 290, 320, 350];
    const hue = hues[h % hues.length];
    return `linear-gradient(135deg, oklch(0.7 0.15 ${hue}), oklch(0.55 0.2 ${(hue + 40) % 360}))`;
  });

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

    this.mdMql = window.matchMedia('(min-width: 768px)');
    this.mdMqlListener = () => this.syncMdUp();
    this.syncMdUp();
    this.mdMql.addEventListener('change', this.mdMqlListener);
  }

  ngOnDestroy(): void {
    if (this.scrollHandler && this.isBrowser) {
      window.removeEventListener('scroll', this.scrollHandler);
    }
    if (this.mdMql && this.mdMqlListener && this.isBrowser) {
      this.mdMql.removeEventListener('change', this.mdMqlListener);
    }
  }

  private syncMdUp(): void {
    if (!this.isBrowser) return;
    this.isMdUp.set(window.matchMedia('(min-width: 768px)').matches);
  }

  appearanceSummary(): string {
    const p = this.themeService.themePreference();
    if (p === 'system') return 'système';
    if (p === 'light') return 'clair';
    return 'sombre';
  }

  setTheme(p: ThemePreference): void {
    this.themeService.setPreference(p);
  }

  toggleAccountMenu(): void {
    const next = !this.accountMenuOpen();
    this.accountMenuOpen.set(next);
    this.appearanceSubOpen.set(false);
    this.langSubOpen.set(false);
    if (next) {
      this.mobileMenuOpen.set(false);
    }
  }

  toggleMobileMenu(): void {
    const next = !this.mobileMenuOpen();
    if (next) {
      this.closeAccountMenu();
    }
    this.mobileMenuOpen.set(next);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  openAccountMenuFromKeyboard(): void {
    this.toggleAccountMenu();
  }

  closeAccountMenu(): void {
    this.accountMenuOpen.set(false);
    this.appearanceSubOpen.set(false);
    this.langSubOpen.set(false);
  }

  pickLanguageGuest(code: string): void {
    this.selectedLangCode.set(code);
    this.langSubOpen.set(false);
  }

  logoutFromMenu(): void {
    this.closeAccountMenu();
    this.authService.logout();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(ev: MouseEvent): void {
    queueMicrotask(() => {
      if (!this.accountMenuOpen()) return;
      const host = this.accountMenuHost()?.nativeElement;
      if (!host) return;
      if (!host.contains(ev.target as Node)) {
        this.closeAccountMenu();
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.accountMenuOpen()) {
      this.closeAccountMenu();
    }
    this.closeMobileMenu();
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    if (!this.isBrowser || typeof window === 'undefined') return;
    this.syncMdUp();
    if (this.isMdUp()) {
      this.closeMobileMenu();
      this.closeAccountMenu();
    }
  }
}
