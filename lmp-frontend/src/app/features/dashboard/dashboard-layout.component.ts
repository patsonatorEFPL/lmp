import { isPlatformBrowser, NgClass } from '@angular/common';
import { Component, inject, OnInit, PLATFORM_ID, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs/operators';

import {
  LucideAngularModule,
  LayoutDashboard,
  ShoppingCart,
  Calendar,
  Settings,
  Menu,
  X,
  Bell,
  ChevronDown,
  PanelLeftClose,
  PanelLeftOpen,
  FileText,
  Receipt,
  FolderKanban,
  LifeBuoy,
  MapPin,
  Search,
  Gift,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationPanelComponent } from '../../shared/layout/notification-panel.component';
import { ShellAccountMenuComponent } from '../../shared/layout/shell-account-menu.component';

@Component({
  selector: 'lmp-dashboard-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    NgClass,
    LucideAngularModule,
    HlmButton,
    NotificationPanelComponent,
    ShellAccountMenuComponent,
  ],
  template: `
    <!-- Même squelette que lmp-admin-layout : bandeau desktop 220px + titre/outils, sidebar « Vues », colonne main -->
    <div
      class="flex min-h-screen flex-col bg-surface-white dark:bg-black lg:h-screen lg:overflow-hidden"
    >
      <div
        class="hidden h-14 shrink-0 border-b border-zinc-200/90 dark:border-zinc-800 lg:flex lg:items-stretch"
      >
        <div
          class="flex shrink-0 items-center border-r border-zinc-200/90 bg-surface-menu-bar transition-[width] duration-200 ease-out dark:border-zinc-800 dark:bg-black"
          [ngClass]="
            sidebarCollapsed()
              ? 'w-16 justify-center px-1'
              : 'w-[220px] justify-between gap-2 px-2'
          "
        >
          <a
            routerLink="/"
            class="flex items-center gap-2 rounded-sm px-1 py-0.5 focus-visible:ring-2 focus-visible:ring-zinc-400"
            [class.min-w-0]="!sidebarCollapsed()"
            [class.flex-1]="!sidebarCollapsed()"
            [class.justify-center]="sidebarCollapsed()"
            [title]="sidebarCollapsed() ? 'LMP Digital Services — Espace client' : ''"
          >
            <img src="/images/logo-lmp.webp" alt="LMP" class="h-7 w-auto shrink-0 rounded-sm" />
            @if (!sidebarCollapsed()) {
              <div class="min-w-0 text-left leading-tight">
                <span class="block truncate text-[13px] font-medium text-zinc-900 dark:text-zinc-100"
                  >LMP Digital Services</span
                >
                <span class="mt-0.5 block truncate text-[11px] text-zinc-500 dark:text-zinc-400"
                  >Espace client</span
                >
              </div>
            }
          </a>
          @if (!sidebarCollapsed()) {
            <span
              class="flex h-7 shrink-0 items-center rounded p-0.5 text-zinc-400 dark:text-zinc-500"
              aria-hidden="true"
            >
              <lucide-icon [img]="ChevronDownIcon" [size]="16"></lucide-icon>
            </span>
          }
        </div>
        <div
          class="flex min-w-0 flex-1 items-center gap-3 bg-white px-3 sm:pl-5 sm:pr-6 dark:bg-black"
        >
          <div class="flex min-w-0 items-center gap-2 sm:gap-3">
            <div class="flex min-w-0 items-center gap-2 text-sm font-medium">
              <span class="shrink-0 text-zinc-400 dark:text-zinc-500">Espace client</span>
              <span class="text-zinc-300 dark:text-zinc-600">/</span>
              <span class="truncate text-zinc-700 dark:text-zinc-200">{{ dashboardPageTitle() }}</span>
            </div>
          </div>
          <div
            class="ml-auto hidden items-center gap-2 rounded-lg border border-zinc-200 bg-white px-2.5 py-1 text-[12.5px] text-zinc-400 lg:flex dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-500"
            style="min-width: 220px; cursor: text"
          >
            <lucide-icon [img]="SearchIcon" [size]="13" class="shrink-0"></lucide-icon>
            <span class="truncate">Rechercher une commande, facture, projet…</span>
            <kbd
              class="ml-auto shrink-0 rounded border border-zinc-200 bg-zinc-100/60 px-1.5 py-0.5 text-[10.5px] text-zinc-400 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-500"
            >⌘K</kbd>
          </div>
          <div class="flex shrink-0 items-center gap-1.5 sm:gap-2">
            <div class="relative hidden lg:block" (click)="$event.stopPropagation()">
              <button
                hlmBtn
                variant="ghost"
                size="icon"
                type="button"
                class="relative cursor-pointer rounded-full"
                (click)="onNotificationButtonClick()"
                aria-label="Notifications"
              >
                <lucide-icon [img]="BellIcon" [size]="18"></lucide-icon>
                @if (notificationService.unreadCount() > 0) {
                  <span
                    class="absolute -top-0.5 -right-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white ring-2 ring-white dark:ring-zinc-950"
                  >
                    {{ notificationService.unreadCount() > 9 ? '9+' : notificationService.unreadCount() }}
                  </span>
                }
              </button>
              <lmp-notification-panel
                [isOpen]="showNotificationPanel()"
                (panelClosed)="showNotificationPanel.set(false)"
              />
            </div>
            <lmp-shell-account-menu
              variant="user"
              (menuOpenChange)="onAccountMenuOpenChange($event)"
              (logoutRequest)="onLogout()"
            />
          </div>
        </div>
      </div>

      <div class="flex min-h-0 flex-1 flex-col overflow-hidden lg:flex-row">
        <aside
          class="z-30 hidden shrink-0 flex-col overflow-hidden border-r border-zinc-200/90 bg-surface-menu-bar transition-[width] duration-200 ease-out dark:border-zinc-800 dark:bg-black lg:z-auto lg:flex"
          [ngClass]="sidebarCollapsed() ? 'w-16' : 'w-[220px]'"
        >
          <nav
            class="flex min-h-0 flex-1 flex-col gap-0 overflow-y-auto overflow-x-hidden pb-2 pt-1"
            [ngClass]="sidebarCollapsed() ? '[scrollbar-gutter:auto]' : '[scrollbar-gutter:stable]'"
            [class.px-2]="!sidebarCollapsed()"
            [class.px-0]="sidebarCollapsed()"
          >
            @if (!sidebarCollapsed()) {
              <p
                class="px-4 pb-2 pt-3 text-[10.5px] font-semibold uppercase tracking-[0.08em] text-zinc-400 dark:text-zinc-500"
              >
                Navigation
              </p>
            }
            @for (item of navigationItems; track item.route) {
              <a
                [routerLink]="item.route"
                [routerLinkActive]="sidebarLinkActive"
                [routerLinkActiveOptions]="{ exact: item.exact }"
                (click)="onSidebarNav(item.route)"
                class="relative my-[1.5px] flex min-h-[30px] cursor-pointer items-center rounded py-[7px] text-sm text-zinc-700 transition-colors duration-200 ease-in-out hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
                [class.justify-center]="sidebarCollapsed()"
                [class.gap-2]="!sidebarCollapsed()"
                [class.px-2]="!sidebarCollapsed()"
                [class.mx-0.5]="!sidebarCollapsed()"
                [class.w-full]="sidebarCollapsed()"
                [attr.title]="item.label"
                [attr.aria-label]="sidebarCollapsed() ? item.label : undefined"
              >
                <lucide-icon [img]="item.icon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
                @if (!sidebarCollapsed()) {
                  <span class="truncate">{{ item.label }}</span>
                  @if (item.badge) {
                    <span class="ml-auto text-[11px] font-medium rounded-full bg-zinc-200/70 px-1.5 text-zinc-500 dark:bg-zinc-700/50 dark:text-zinc-400">{{ item.badge }}</span>
                  }
                }
                @if (item.route === '/dashboard/orders' && notificationService.liveOrderHint() > 0) {
                  <span
                    class="absolute flex h-4 min-w-4 items-center justify-center rounded bg-red-500 px-1 text-[10px] font-bold text-white"
                    [ngClass]="sidebarCollapsed() ? 'right-1 top-1' : 'right-2 top-1.5'"
                    >{{ notificationService.liveOrderHint() > 9 ? '9+' : notificationService.liveOrderHint() }}</span
                  >
                }
                @if (item.route === '/dashboard/appointments' && notificationService.liveAppointmentHint() > 0) {
                  <span
                    class="absolute flex h-4 min-w-4 items-center justify-center rounded bg-red-500 px-1 text-[10px] font-bold text-white"
                    [ngClass]="sidebarCollapsed() ? 'right-1 top-1' : 'right-2 top-1.5'"
                    >{{
                      notificationService.liveAppointmentHint() > 9
                        ? '9+'
                        : notificationService.liveAppointmentHint()
                    }}</span
                  >
                }
              </a>
            }

            @if (!sidebarCollapsed()) {
              <p
                class="px-4 pb-2 pt-4 text-[10.5px] font-semibold uppercase tracking-[0.08em] text-zinc-400 dark:text-zinc-500"
              >
                Compte
              </p>
            }
            @for (item of compteItems; track item.route) {
              <a
                [routerLink]="item.route"
                [routerLinkActive]="sidebarLinkActive"
                [routerLinkActiveOptions]="{ exact: item.exact }"
                class="my-[1.5px] flex min-h-[30px] cursor-pointer items-center rounded py-[7px] text-sm text-zinc-700 transition-colors duration-200 ease-in-out hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
                [class.justify-center]="sidebarCollapsed()"
                [class.gap-2]="!sidebarCollapsed()"
                [class.px-2]="!sidebarCollapsed()"
                [class.mx-0.5]="!sidebarCollapsed()"
                [class.w-full]="sidebarCollapsed()"
                [attr.title]="item.label"
                [attr.aria-label]="sidebarCollapsed() ? item.label : undefined"
              >
                <lucide-icon [img]="item.icon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
                @if (!sidebarCollapsed()) {
                  <span class="truncate">{{ item.label }}</span>
                }
              </a>
            }
          </nav>

          <div class="shrink-0 border-t border-zinc-200/80 dark:border-zinc-800">
            <button
              type="button"
              hlmBtn
              variant="ghost"
              size="sm"
              class="my-1 flex w-full cursor-pointer text-zinc-600 hover:bg-zinc-200/70 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-100"
              [class.gap-2]="!sidebarCollapsed()"
              [class.gap-0]="sidebarCollapsed()"
              [class.justify-center]="sidebarCollapsed()"
              [class.justify-start]="!sidebarCollapsed()"
              [class.px-2]="!sidebarCollapsed()"
              [class.px-0]="sidebarCollapsed()"
              (click)="toggleSidebarCollapsed()"
              [attr.aria-expanded]="!sidebarCollapsed()"
              [attr.aria-label]="sidebarCollapsed() ? 'Développer le menu latéral' : 'Réduire le menu latéral'"
            >
              @if (sidebarCollapsed()) {
                <lucide-icon [img]="PanelLeftOpenIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
                <span class="sr-only">Développer le menu</span>
              } @else {
                <lucide-icon [img]="PanelLeftCloseIcon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
                <span>Réduire</span>
              }
            </button>
          </div>
        </aside>

        @if (mobileMenuOpen()) {
          <button
            type="button"
            tabindex="-1"
            class="fixed inset-0 z-40 cursor-default touch-none bg-black/40 lg:hidden"
            aria-label="Fermer le menu"
            (click)="mobileMenuOpen.set(false)"
          ></button>
          <aside
            class="fixed inset-y-0 left-0 z-50 flex w-[min(18rem,calc(100vw-2.5rem))] flex-col border-r border-zinc-200/90 bg-surface-menu-bar shadow-xl dark:border-zinc-800 dark:bg-black lg:hidden"
          >
            <div
              class="box-border flex h-14 min-h-14 shrink-0 items-center justify-between gap-2 border-b border-zinc-200/90 px-2 dark:border-zinc-800"
            >
              <a
                routerLink="/"
                class="flex min-w-0 flex-1 items-center gap-2 rounded-sm px-1 py-0.5 focus-visible:ring-2 focus-visible:ring-zinc-400"
                (click)="mobileMenuOpen.set(false)"
              >
                <img src="/images/logo-lmp.webp" alt="LMP" class="h-7 w-auto shrink-0 rounded-sm" />
                <div class="min-w-0 text-left leading-tight">
                  <span class="block truncate text-[13px] font-medium text-zinc-900 dark:text-zinc-100"
                    >LMP Digital Services</span
                  >
                  <span class="mt-0.5 block truncate text-[11px] text-zinc-500 dark:text-zinc-400"
                    >Espace client</span
                  >
                </div>
              </a>
              <button
                hlmBtn
                variant="ghost"
                size="icon"
                type="button"
                class="shrink-0 cursor-pointer"
                (click)="mobileMenuOpen.set(false)"
                aria-label="Fermer"
              >
                <lucide-icon [img]="XIcon" [size]="18"></lucide-icon>
              </button>
            </div>
            <nav class="flex flex-1 flex-col gap-0 overflow-y-auto px-2 pb-2 pt-1">
              <p class="px-4 pb-2 pt-3 text-[10.5px] font-semibold uppercase tracking-[0.08em] text-zinc-400">Navigation</p>
              @for (item of navigationItems; track item.route) {
                <a
                  [routerLink]="item.route"
                  [routerLinkActive]="sidebarLinkActive"
                  [routerLinkActiveOptions]="{ exact: item.exact }"
                  class="relative mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
                  (click)="mobileMenuOpen.set(false); onSidebarNav(item.route)"
                >
                  <lucide-icon [img]="item.icon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
                  <span class="truncate">{{ item.label }}</span>
                  @if (item.badge) {
                    <span class="ml-auto text-[11px] font-medium rounded-full bg-zinc-200/70 px-1.5 text-zinc-500 dark:bg-zinc-700/50 dark:text-zinc-400">{{ item.badge }}</span>
                  }
                  @if (item.route === '/dashboard/orders' && notificationService.liveOrderHint() > 0) {
                    <span
                      class="absolute right-2 top-1.5 flex h-4 min-w-4 items-center justify-center rounded bg-red-500 px-1 text-[10px] font-bold text-white"
                      >{{ notificationService.liveOrderHint() > 9 ? '9+' : notificationService.liveOrderHint() }}</span
                    >
                  }
                  @if (item.route === '/dashboard/appointments' && notificationService.liveAppointmentHint() > 0) {
                    <span
                      class="absolute right-2 top-1.5 flex h-4 min-w-4 items-center justify-center rounded bg-red-500 px-1 text-[10px] font-bold text-white"
                      >{{
                        notificationService.liveAppointmentHint() > 9
                          ? '9+'
                          : notificationService.liveAppointmentHint()
                      }}</span
                    >
                  }
                </a>
              }
              <p class="px-4 pb-2 pt-4 text-[10.5px] font-semibold uppercase tracking-[0.08em] text-zinc-400">Compte</p>
              @for (item of compteItems; track item.route) {
                <a
                  [routerLink]="item.route"
                  [routerLinkActive]="sidebarLinkActive"
                  [routerLinkActiveOptions]="{ exact: item.exact }"
                  class="mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
                  (click)="mobileMenuOpen.set(false)"
                >
                  <lucide-icon [img]="item.icon" [size]="16" class="inline-flex shrink-0"></lucide-icon>
                  <span class="truncate">{{ item.label }}</span>
                </a>
              }
            </nav>
          </aside>
        }

        <div
          class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden bg-white dark:bg-black lg:min-h-0"
        >
          <header
            class="sticky top-0 z-20 shrink-0 border-b border-zinc-200/90 bg-white lg:hidden dark:border-zinc-800 dark:bg-black"
          >
            <div
              class="box-border flex h-14 min-h-14 shrink-0 items-center justify-between gap-3 px-3 sm:pl-5 sm:pr-6"
            >
              <div class="flex min-w-0 flex-1 items-center gap-3">
                <button
                  hlmBtn
                  variant="ghost"
                  size="icon"
                  type="button"
                  class="shrink-0 cursor-pointer lg:hidden"
                  (click)="mobileMenuOpen.set(!mobileMenuOpen())"
                  [attr.aria-expanded]="mobileMenuOpen()"
                  aria-label="Menu de navigation"
                >
                  <lucide-icon [img]="MenuIcon" [size]="18"></lucide-icon>
                </button>
                <div class="flex min-w-0 items-center gap-2 text-sm font-medium">
                  <span class="shrink-0 text-zinc-400 dark:text-zinc-500">Espace client</span>
                  <span class="text-zinc-300 dark:text-zinc-600">/</span>
                  <span class="truncate text-zinc-700 dark:text-zinc-200">{{ dashboardPageTitle() }}</span>
                </div>
              </div>

              <div class="flex shrink-0 items-center gap-1.5 sm:gap-2">
                <div class="relative lg:hidden" (click)="$event.stopPropagation()">
                  <button
                    hlmBtn
                    variant="ghost"
                    size="icon"
                    type="button"
                    class="relative cursor-pointer rounded-full"
                    (click)="onNotificationButtonClick()"
                    aria-label="Notifications"
                  >
                    <lucide-icon [img]="BellIcon" [size]="18"></lucide-icon>
                    @if (notificationService.unreadCount() > 0) {
                      <span
                        class="absolute -top-0.5 -right-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white ring-2 ring-white dark:ring-zinc-950"
                      >
                        {{ notificationService.unreadCount() > 9 ? '9+' : notificationService.unreadCount() }}
                      </span>
                    }
                  </button>
                  <lmp-notification-panel
                    [isOpen]="showNotificationPanel()"
                    (panelClosed)="showNotificationPanel.set(false)"
                  />
                </div>

                <lmp-shell-account-menu
                  variant="user"
                  (menuOpenChange)="onAccountMenuOpenChange($event)"
                  (logoutRequest)="onLogout()"
                />
              </div>
            </div>
          </header>

          <main
            class="lmp-dashboard-theme min-h-0 flex-1 overflow-x-hidden overflow-y-auto bg-white p-4 sm:p-5 dark:bg-black"
          >
            <router-outlet />
          </main>
        </div>
      </div>
    </div>
  `,
})
export class DashboardLayoutComponent implements OnInit {
  private static readonly SIDEBAR_STORAGE_KEY = 'lmp-user-sidebar-collapsed';

  readonly authService = inject(AuthService);
  readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  /** Desktop : sidebar étroite (icônes seules). */
  readonly sidebarCollapsed = signal(false);

  /** Titre de la vue courante (bandeau desktop + mobile). */
  readonly dashboardPageTitle = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.titleForDashboardUrl(this.router.url)),
    ),
    { initialValue: this.titleForDashboardUrl(this.router.url) },
  );

  private titleForDashboardUrl(rawUrl: string): string {
    const path = rawUrl.split('?')[0].replace(/\/+$/, '') || '/';
    const parts = path.split('/').filter(Boolean);
    if (parts[0] !== 'dashboard') {
      return 'Espace client';
    }
    if (parts.length === 1) {
      return 'Tableau de bord';
    }
    switch (parts[1]) {
      case 'orders':
        return 'Commandes';
      case 'appointments':
        return 'Rendez-vous';
      case 'quotations':
        return 'Devis';
      case 'invoices':
        return 'Factures';
      case 'projects':
        return parts[2] ? 'Détail projet' : 'Projets';
      case 'tickets':
        return parts[2] ? 'Détail ticket' : 'Tickets';
      case 'addresses':
        return 'Adresses';
      case 'settings':
        return 'Paramètres du compte';
      default:
        return 'Espace client';
    }
  }

  private readonly accountMenu = viewChild(ShellAccountMenuComponent);

  readonly showNotificationPanel = signal(false);
  readonly mobileMenuOpen = signal(false);

  readonly sidebarLinkActive =
    '!bg-white font-medium text-zinc-900 shadow-sm ring-1 ring-zinc-200/70 dark:!bg-zinc-800 dark:!text-white dark:ring-zinc-600';

  readonly BellIcon = Bell;
  readonly MenuIcon = Menu;
  readonly XIcon = X;
  readonly ChevronDownIcon = ChevronDown;
  readonly PanelLeftCloseIcon = PanelLeftClose;
  readonly PanelLeftOpenIcon = PanelLeftOpen;
  readonly SearchIcon = Search;
  readonly GiftIcon = Gift;

  readonly navigationItems = [
    { label: "Tableau de bord", route: '/dashboard', icon: LayoutDashboard, exact: true, badge: null as string | null },
    { label: 'Commandes', route: '/dashboard/orders', icon: ShoppingCart, exact: false, badge: '14' },
    { label: 'Devis', route: '/dashboard/quotations', icon: FileText, exact: false, badge: '2' },
    { label: 'Factures', route: '/dashboard/invoices', icon: Receipt, exact: false, badge: null },
    { label: 'Projets', route: '/dashboard/projects', icon: FolderKanban, exact: false, badge: '3' },
    { label: 'Tickets', route: '/dashboard/tickets', icon: LifeBuoy, exact: false, badge: '1' },
    { label: 'Rendez-vous', route: '/dashboard/appointments', icon: Calendar, exact: false, badge: '2' },
    { label: 'Adresses', route: '/dashboard/addresses', icon: MapPin, exact: false, badge: null },
  ];

  readonly compteItems = [
    { label: 'Paramètres', route: '/dashboard/settings', icon: Settings, exact: false, badge: null as string | null },
    { label: 'Parrainer & gagner', route: '/dashboard/referral', icon: Gift, exact: false, badge: null },
  ];

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      try {
        if (localStorage.getItem(DashboardLayoutComponent.SIDEBAR_STORAGE_KEY) === '1') {
          this.sidebarCollapsed.set(true);
        }
      } catch {
        /* private mode */
      }
    }
  }

  toggleSidebarCollapsed(): void {
    this.sidebarCollapsed.update((c) => {
      const next = !c;
      if (isPlatformBrowser(this.platformId)) {
        try {
          localStorage.setItem(
            DashboardLayoutComponent.SIDEBAR_STORAGE_KEY,
            next ? '1' : '0',
          );
        } catch {
          /* ignore */
        }
      }
      return next;
    });
  }

  onAccountMenuOpenChange(open: boolean): void {
    if (open) {
      this.showNotificationPanel.set(false);
    }
  }

  onNotificationButtonClick(): void {
    this.accountMenu()?.closeMenu();
    this.showNotificationPanel.update((v) => !v);
  }

  onSidebarNav(route: string): void {
    if (route === '/dashboard/orders') {
      this.notificationService.liveOrderHint.set(0);
    }
    if (route === '/dashboard/appointments') {
      this.notificationService.liveAppointmentHint.set(0);
    }
  }

  onLogout(): void {
    this.notificationService.reset();
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
