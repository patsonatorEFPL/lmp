import {
  Component,
  ElementRef,
  HostListener,
  inject,
  input,
  output,
  computed,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  User,
  LogOut,
  Monitor,
  Sun,
  Moon,
  ChevronRight,
  Check,
  Home,
  Shield,
  HelpCircle,
  LayoutDashboard,
} from 'lucide-angular';

import { AuthService } from '../../core/services/auth.service';
import { ThemeService, type ThemePreference } from '../../core/services/theme.service';

/**
 * Menu compte pour les shells dashboard / admin : même logique visuelle que la navbar publique
 * (panneau arrondi, apparence repliable, liens avec icônes).
 */
@Component({
  selector: 'lmp-shell-account-menu',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  host: { class: 'relative' },
  template: `
    <button
      type="button"
      class="inline-flex h-9 w-9 shrink-0 cursor-pointer items-center justify-center rounded-full border border-(--border) bg-(--muted)/35 text-(--muted-foreground) transition-colors hover:bg-(--muted)/55 hover:text-(--foreground) focus-visible:ring-2 focus-visible:ring-(--ring) focus-visible:outline-none"
      (click)="$event.stopPropagation(); toggleMenu()"
      (keydown.enter)="$event.preventDefault(); toggleMenu()"
      (keydown.space)="$event.preventDefault(); toggleMenu()"
      [attr.aria-expanded]="accountMenuOpen()"
      aria-haspopup="true"
      aria-label="Menu compte"
    >
      <lucide-icon [img]="UserIcon" [size]="18"></lucide-icon>
    </button>

    @if (accountMenuOpen()) {
      <div
        class="absolute right-0 z-[100] mt-2 flex w-max max-w-[min(calc(100vw-2rem),18.5rem)] max-h-[min(70dvh,calc(100dvh-5rem))] flex-col overflow-x-hidden overflow-y-auto overscroll-contain rounded-xl border border-(--border) bg-(--card) py-1 text-(--foreground) shadow-lg max-sm:right-1"
        role="menu"
      >
        <div class="w-full border-b border-(--border) px-3 pb-3 pt-2">
          <div class="flex gap-3">
            <div
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-(--primary)/15 text-(--primary)"
            >
              <lucide-icon [img]="UserIcon" [size]="20"></lucide-icon>
            </div>
            <div class="min-w-0 flex-1 text-left">
              <p class="truncate text-sm font-semibold">{{ accountTitle() }}</p>
              <p class="truncate text-xs text-(--muted-foreground)">{{ accountEmail() }}</p>
            </div>
          </div>
        </div>

        <div class="w-full py-1">
          @if (variant() === 'user') {
            <a
              routerLink="/"
              role="menuitem"
              class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
              (click)="closeMenu()"
            >
              <lucide-icon [img]="HomeIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
              <span>Site public</span>
            </a>
            @if (authService.isAdmin()) {
              <a
                routerLink="/admin"
                role="menuitem"
                class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
                (click)="closeMenu()"
              >
                <lucide-icon [img]="ShieldIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
                <span>Administration</span>
              </a>
            }
          } @else {
            <a
              routerLink="/"
              role="menuitem"
              class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
              (click)="closeMenu()"
            >
              <lucide-icon [img]="HomeIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
              <span>Site public</span>
            </a>
            <a
              routerLink="/dashboard"
              role="menuitem"
              class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
              (click)="closeMenu()"
            >
              <lucide-icon
                [img]="DashboardIcon"
                [size]="18"
                class="shrink-0 opacity-80"
              ></lucide-icon>
              <span>Espace client</span>
            </a>
          }
        </div>

        <div class="mx-3 h-px shrink-0 bg-(--border)"></div>

        <div class="w-full py-1">
          <button
            type="button"
            class="flex w-full min-w-0 items-center gap-3 px-3 py-2.5 text-left text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
            (click)="appearanceSubOpen.update((v) => !v)"
          >
            <lucide-icon [img]="MoonIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
            <span class="min-w-0 flex-1 truncate">Apparence&nbsp;: {{ appearanceSummary() }}</span>
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
        </div>

        <div class="mx-3 h-px shrink-0 bg-(--border)"></div>

        <div class="w-full py-1">
          <a
            routerLink="/contact"
            role="menuitem"
            class="flex items-center gap-3 whitespace-nowrap px-3 py-2.5 text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
            (click)="closeMenu()"
          >
            <lucide-icon [img]="HelpCircleIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
            <span>Aide</span>
          </a>
          <button
            type="button"
            role="menuitem"
            class="flex w-full items-center gap-3 px-3 py-2.5 text-left text-sm text-(--foreground) transition-colors hover:bg-(--accent)"
            (click)="onLogoutClick()"
          >
            <lucide-icon [img]="LogOutIcon" [size]="18" class="shrink-0 opacity-80"></lucide-icon>
            <span>Se déconnecter</span>
          </button>
        </div>
      </div>
    }
  `,
})
export class ShellAccountMenuComponent {
  readonly variant = input<'user' | 'admin'>('user');

  /** Émis quand le panneau s’ouvre ou se ferme (fermer d’autres overlays côté parent). */
  readonly menuOpenChange = output<boolean>();
  /** Déconnexion : le parent réinitialise SSE / notifs puis appelle AuthService. */
  readonly logoutRequest = output<void>();

  private readonly host = inject(ElementRef<HTMLElement>);
  readonly authService = inject(AuthService);
  readonly themeService = inject(ThemeService);

  readonly accountMenuOpen = signal(false);
  readonly appearanceSubOpen = signal(false);

  readonly UserIcon = User;
  readonly LogOutIcon = LogOut;
  readonly MoonIcon = Moon;
  readonly ChevronRightIcon = ChevronRight;
  readonly CheckIcon = Check;
  readonly HomeIcon = Home;
  readonly ShieldIcon = Shield;
  readonly HelpCircleIcon = HelpCircle;
  readonly DashboardIcon = LayoutDashboard;

  readonly themeOptions: {
    value: ThemePreference;
    label: string;
    icon: typeof Monitor;
  }[] = [
    { value: 'system', label: 'Système', icon: Monitor },
    { value: 'light', label: 'Clair', icon: Sun },
    { value: 'dark', label: 'Sombre', icon: Moon },
  ];

  readonly accountTitle = computed(() => {
    const u = this.authService.user();
    if (!u) return '';
    const d = u.displayName?.trim();
    if (d) return d;
    const name = [u.firstName, u.lastName].filter(Boolean).join(' ').trim();
    if (name) return name;
    return u.email;
  });

  readonly accountEmail = computed(() => this.authService.user()?.email ?? '');

  closeMenu(): void {
    this.accountMenuOpen.set(false);
    this.appearanceSubOpen.set(false);
    this.menuOpenChange.emit(false);
  }

  toggleMenu(): void {
    const next = !this.accountMenuOpen();
    this.accountMenuOpen.set(next);
    if (!next) {
      this.appearanceSubOpen.set(false);
    }
    this.menuOpenChange.emit(next);
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

  onLogoutClick(): void {
    this.closeMenu();
    this.logoutRequest.emit();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(ev: MouseEvent): void {
    queueMicrotask(() => {
      if (!this.accountMenuOpen()) return;
      if (!this.host.nativeElement.contains(ev.target as Node)) {
        this.closeMenu();
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.accountMenuOpen()) {
      this.closeMenu();
    }
  }
}
