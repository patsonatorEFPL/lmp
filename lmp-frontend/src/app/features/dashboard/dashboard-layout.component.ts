import { Component, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

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
    LucideAngularModule,
    HlmButton,
    NotificationPanelComponent,
    ShellAccountMenuComponent,
  ],
  template: `
    <!-- Même squelette que lmp-admin-layout : bandeau desktop 220px + titre/outils, sidebar « Vues », colonne main -->
    <div
      class="flex min-h-screen flex-col bg-[#eceff3] dark:bg-zinc-950 lg:h-screen lg:overflow-hidden"
    >
      <div
        class="hidden h-14 shrink-0 border-b border-zinc-200/90 dark:border-zinc-800 lg:flex lg:items-stretch"
      >
        <div
          class="flex w-[220px] shrink-0 items-center justify-between gap-2 border-r border-zinc-200/90 bg-[#f4f5f7] px-2 dark:border-zinc-800 dark:bg-zinc-900"
        >
          <a
            routerLink="/"
            class="flex min-w-0 flex-1 items-center gap-2 rounded-sm px-1 py-0.5 focus-visible:ring-2 focus-visible:ring-zinc-400"
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
          <span
            class="flex h-7 shrink-0 items-center rounded p-0.5 text-zinc-400 dark:text-zinc-500"
            aria-hidden="true"
          >
            <lucide-icon [img]="ChevronDownIcon" [size]="16"></lucide-icon>
          </span>
        </div>
        <div
          class="flex min-w-0 flex-1 items-center justify-between gap-3 bg-white px-3 sm:pl-5 sm:pr-6 dark:bg-zinc-950"
        >
          <div class="flex min-w-0 items-center gap-2 sm:gap-3">
            <h1
              class="min-w-0 truncate text-sm font-medium tracking-tight text-zinc-800 dark:text-zinc-100 sm:text-[15px]"
            >
              Espace client
            </h1>
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
          class="z-30 hidden w-[220px] shrink-0 flex-col overflow-hidden border-r border-zinc-200/90 bg-[#f4f5f7] dark:border-zinc-800 dark:bg-zinc-900 lg:z-auto lg:flex"
        >
          <nav
            class="flex min-h-0 flex-1 flex-col gap-0 overflow-y-auto overflow-x-hidden px-2 pb-2 pt-1 [scrollbar-gutter:stable]"
          >
            <p
              class="px-4 pb-2 pt-3 text-xs font-medium text-zinc-500 dark:text-zinc-500"
            >
              Vues
            </p>
            @for (item of sidebarItems; track item.route) {
              <a
                [routerLink]="item.route"
                [routerLinkActive]="sidebarLinkActive"
                [routerLinkActiveOptions]="{ exact: item.exact }"
                (click)="onSidebarNav(item.route)"
                class="relative mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors duration-200 ease-in-out hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
              >
                <lucide-icon [img]="item.icon" [size]="16" class="shrink-0"></lucide-icon>
                <span class="truncate">{{ item.label }}</span>
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
          </nav>

          <div class="shrink-0 border-t border-zinc-200/80 px-3 py-2.5 dark:border-zinc-800">
            <p class="text-center text-[10px] text-zinc-400 dark:text-zinc-500">LMP Digital Services</p>
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
            class="fixed inset-y-0 left-0 z-50 flex w-[min(18rem,calc(100vw-2.5rem))] flex-col border-r border-zinc-200/90 bg-[#f4f5f7] shadow-xl dark:border-zinc-800 dark:bg-zinc-900 lg:hidden"
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
              <p class="px-4 pb-2 pt-3 text-xs font-medium text-zinc-500">Vues</p>
              @for (item of sidebarItems; track item.route) {
                <a
                  [routerLink]="item.route"
                  [routerLinkActive]="sidebarLinkActive"
                  [routerLinkActiveOptions]="{ exact: item.exact }"
                  class="relative mx-0.5 my-[1.5px] flex min-h-[30px] cursor-pointer items-center gap-2 rounded px-2 py-[7px] text-sm text-zinc-700 transition-colors hover:bg-zinc-200/60 dark:text-zinc-300 dark:hover:bg-zinc-800/70"
                  (click)="mobileMenuOpen.set(false); onSidebarNav(item.route)"
                >
                  <lucide-icon [img]="item.icon" [size]="16" class="shrink-0"></lucide-icon>
                  <span class="truncate">{{ item.label }}</span>
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
            </nav>
          </aside>
        }

        <div
          class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden bg-white dark:bg-zinc-950 lg:min-h-0"
        >
          <header
            class="sticky top-0 z-20 shrink-0 border-b border-zinc-200/90 bg-white lg:hidden dark:border-zinc-800 dark:bg-zinc-950"
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
                <h1
                  class="min-w-0 truncate text-sm font-medium tracking-tight text-zinc-800 dark:text-zinc-100 sm:text-[15px]"
                >
                  Espace client
                </h1>
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
            class="lmp-dashboard-theme min-h-0 flex-1 overflow-x-hidden overflow-y-auto bg-white p-4 sm:p-5 dark:bg-zinc-950"
          >
            <router-outlet />
          </main>
        </div>
      </div>
    </div>
  `,
})
export class DashboardLayoutComponent {
  readonly authService = inject(AuthService);
  readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  private readonly accountMenu = viewChild(ShellAccountMenuComponent);

  readonly showNotificationPanel = signal(false);
  readonly mobileMenuOpen = signal(false);

  readonly sidebarLinkActive =
    '!bg-white font-medium text-zinc-900 shadow-sm ring-1 ring-zinc-200/70 dark:!bg-zinc-800 dark:!text-white dark:ring-zinc-600';

  readonly BellIcon = Bell;
  readonly MenuIcon = Menu;
  readonly XIcon = X;
  readonly ChevronDownIcon = ChevronDown;

  readonly sidebarItems = [
    { label: "Vue d'ensemble", route: '/dashboard', icon: LayoutDashboard, exact: true },
    { label: 'Mes commandes', route: '/dashboard/orders', icon: ShoppingCart, exact: false },
    { label: 'Mes rendez-vous', route: '/dashboard/appointments', icon: Calendar, exact: false },
    { label: 'Paramètres', route: '/dashboard/settings', icon: Settings, exact: false },
  ];

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
