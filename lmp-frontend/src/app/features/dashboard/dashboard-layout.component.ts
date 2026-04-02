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
    <div class="flex min-h-screen bg-(--background)">
      <!-- Sidebar desktop -->
      <aside
        class="fixed inset-y-0 left-0 z-30 hidden w-60 flex-col border-r border-(--border) bg-(--card) lg:flex"
      >
        <div class="flex h-16 items-center gap-3 border-b border-(--border) px-5">
          <a routerLink="/" class="flex items-center rounded-sm focus-visible:ring-2 focus-visible:ring-(--ring)">
            <img src="/images/logo-lmp.webp" alt="LMP Logo" class="h-9 w-auto" />
          </a>
        </div>

        <nav class="flex-1 space-y-1 overflow-y-auto px-3 py-4">
          @for (item of sidebarItems; track item.route) {
            <a
              [routerLink]="item.route"
              routerLinkActive="bg-(--primary)/10 text-(--primary)"
              [routerLinkActiveOptions]="{ exact: item.exact }"
              (click)="onSidebarNav(item.route)"
              class="relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
            >
              <lucide-icon [img]="item.icon" [size]="18"></lucide-icon>
              {{ item.label }}
              @if (item.route === '/dashboard/orders' && notificationService.liveOrderHint() > 0) {
                <span
                  class="absolute right-2 top-2 flex h-4 min-w-4 items-center justify-center rounded-sm bg-red-500 px-1 text-[10px] font-bold text-white"
                  >{{ notificationService.liveOrderHint() > 9 ? '9+' : notificationService.liveOrderHint() }}</span
                >
              }
              @if (item.route === '/dashboard/appointments' && notificationService.liveAppointmentHint() > 0) {
                <span
                  class="absolute right-2 top-2 flex h-4 min-w-4 items-center justify-center rounded-sm bg-red-500 px-1 text-[10px] font-bold text-white"
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

        <div class="shrink-0 border-t border-(--border) px-4 py-3">
          <p class="text-center text-[10px] font-medium tracking-wide text-(--muted-foreground)">
            LMP Digital Services
          </p>
        </div>
      </aside>

      <!-- Mobile drawer -->
      @if (mobileMenuOpen()) {
        <button
          type="button"
          tabindex="-1"
          class="fixed inset-0 z-40 cursor-default touch-none bg-black/40 lg:hidden"
          aria-label="Fermer le menu"
          (click)="mobileMenuOpen.set(false)"
        ></button>
        <aside
          class="fixed inset-y-0 left-0 z-50 flex w-[min(17rem,calc(100vw-2.5rem))] flex-col border-r border-(--border) bg-(--card) shadow-xl lg:hidden"
        >
          <div class="flex h-16 items-center justify-between gap-2 border-b border-(--border) px-4">
            <a
              routerLink="/"
              class="flex min-w-0 items-center rounded-sm focus-visible:ring-2 focus-visible:ring-(--ring)"
              (click)="mobileMenuOpen.set(false)"
            >
              <img src="/images/logo-lmp.webp" alt="LMP Logo" class="h-9 w-auto" />
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
          <nav class="flex-1 space-y-1 overflow-y-auto px-3 py-4">
            @for (item of sidebarItems; track item.route) {
              <a
                [routerLink]="item.route"
                routerLinkActive="bg-(--primary)/10 text-(--primary)"
                [routerLinkActiveOptions]="{ exact: item.exact }"
                class="relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
                (click)="mobileMenuOpen.set(false); onSidebarNav(item.route)"
              >
                <lucide-icon [img]="item.icon" [size]="18"></lucide-icon>
                {{ item.label }}
                @if (item.route === '/dashboard/orders' && notificationService.liveOrderHint() > 0) {
                  <span
                    class="absolute right-2 top-2 flex h-4 min-w-4 items-center justify-center rounded-sm bg-red-500 px-1 text-[10px] font-bold text-white"
                    >{{ notificationService.liveOrderHint() > 9 ? '9+' : notificationService.liveOrderHint() }}</span
                  >
                }
                @if (item.route === '/dashboard/appointments' && notificationService.liveAppointmentHint() > 0) {
                  <span
                    class="absolute right-2 top-2 flex h-4 min-w-4 items-center justify-center rounded-sm bg-red-500 px-1 text-[10px] font-bold text-white"
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

      <div class="flex flex-1 flex-col lg:ml-60">
        <header
          class="sticky top-0 z-20 border-b border-(--border) bg-(--card)/95 backdrop-blur-sm supports-[backdrop-filter]:bg-(--card)/80"
        >
          <div class="flex h-14 items-center justify-between gap-3 px-4 sm:h-16 sm:px-6">
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
                class="min-w-0 truncate text-sm font-semibold tracking-tight text-(--foreground) sm:text-base"
              >
                Espace client
              </h1>
            </div>

            <div class="flex shrink-0 items-center gap-1.5 sm:gap-2">
              <div class="relative" (click)="$event.stopPropagation()">
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
                      class="absolute -top-0.5 -right-0.5 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white ring-2 ring-(--card)"
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

        <main class="flex-1 p-4 sm:p-6 lg:p-8">
          <router-outlet />
        </main>
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

  readonly BellIcon = Bell;
  readonly MenuIcon = Menu;
  readonly XIcon = X;

  readonly sidebarItems = [
    { label: "Vue d'ensemble", route: '/dashboard', icon: LayoutDashboard, exact: true },
    { label: 'Mes commandes', route: '/dashboard/orders', icon: ShoppingCart, exact: false },
    { label: 'Mes rendez-vous', route: '/dashboard/appointments', icon: Calendar, exact: false },
    { label: 'Paramètres', route: '/settings', icon: Settings, exact: false },
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
