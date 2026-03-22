import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import {
  LucideAngularModule,
  LayoutDashboard,
  ShoppingCart,
  Calendar,
  Settings,
  LogOut,
  User,
  Bell,
  Menu,
  X,
  Moon,
  Sun,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { ThemeService } from '../../core/services/theme.service';
import { NotificationPanelComponent } from '../../shared/layout/notification-panel.component';

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
  ],
  template: `
    <div class="flex min-h-screen bg-(--background)">
      <!-- Sidebar (desktop) -->
      <aside
        class="fixed inset-y-0 left-0 z-30 hidden w-60 flex-col border-r border-(--border) bg-(--card) lg:flex"
      >
        <!-- Logo -->
        <div class="flex h-16 items-center gap-3 border-b border-(--border) px-5">
          <a routerLink="/" class="flex items-center">
            <img src="/images/logo-lmp.webp" alt="LMP Logo" class="h-9 w-auto" />
          </a>
        </div>

        <!-- Navigation -->
        <nav class="flex-1 space-y-1 px-3 py-4">
          @for (item of sidebarItems; track item.route) {
            <a
              [routerLink]="item.route"
              routerLinkActive="bg-(--primary)/10 text-(--primary)"
              [routerLinkActiveOptions]="{ exact: item.exact }"
              class="flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
            >
              <lucide-icon [img]="item.icon" [size]="18"></lucide-icon>
              {{ item.label }}
            </a>
          }
        </nav>

        <!-- Bottom actions -->
        <div class="border-t border-(--border) p-4">
          <div class="flex items-center justify-between">
            <button
              hlmBtn variant="ghost" size="sm"
              class="cursor-pointer gap-2 text-xs text-(--muted-foreground)"
              (click)="themeService.toggle()"
            >
              @if (themeService.isDark()) {
                <lucide-icon [img]="SunIcon" [size]="14"></lucide-icon>
                Clair
              } @else {
                <lucide-icon [img]="MoonIcon" [size]="14"></lucide-icon>
                Sombre
              }
            </button>
            <button
              hlmBtn variant="ghost" size="icon"
              class="h-8 w-8 cursor-pointer text-(--muted-foreground) hover:text-(--destructive)"
              (click)="onLogout()"
            >
              <lucide-icon [img]="LogOutIcon" [size]="16"></lucide-icon>
            </button>
          </div>
        </div>
      </aside>

      <!-- Mobile sidebar overlay -->
      @if (mobileMenuOpen()) {
        <div
          class="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm lg:hidden"
          (click)="mobileMenuOpen.set(false)"
        ></div>
        <aside
          class="fixed inset-y-0 left-0 z-50 w-60 flex-col border-r border-(--border) bg-(--card) lg:hidden flex"
        >
          <div class="flex h-16 items-center justify-between border-b border-(--border) px-5">
            <a routerLink="/" class="flex items-center">
              <img src="/images/logo-lmp.webp" alt="LMP Logo" class="h-9 w-auto" />
            </a>
            <button
              hlmBtn variant="ghost" size="icon"
              class="h-8 w-8 cursor-pointer"
              (click)="mobileMenuOpen.set(false)"
            >
              <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
            </button>
          </div>
          <nav class="flex-1 space-y-1 px-3 py-4">
            @for (item of sidebarItems; track item.route) {
              <a
                [routerLink]="item.route"
                routerLinkActive="bg-(--primary)/10 text-(--primary)"
                [routerLinkActiveOptions]="{ exact: item.exact }"
                class="flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
                (click)="mobileMenuOpen.set(false)"
              >
                <lucide-icon [img]="item.icon" [size]="18"></lucide-icon>
                {{ item.label }}
              </a>
            }
          </nav>
        </aside>
      }

      <!-- Main content area -->
      <div class="flex flex-1 flex-col lg:ml-60">
        <!-- Top navbar -->
        <header
          class="sticky top-0 z-20 border-b border-(--border) bg-(--card)"
        >
          <div class="flex h-16 items-center justify-between px-4 sm:px-6">
            <div class="flex items-center gap-3">
              <!-- Mobile hamburger -->
              <button
                hlmBtn variant="ghost" size="icon"
                class="cursor-pointer lg:hidden"
                (click)="mobileMenuOpen.set(!mobileMenuOpen())"
              >
                <lucide-icon [img]="MenuIcon" [size]="18"></lucide-icon>
              </button>

              <span class="text-sm font-semibold text-(--foreground)">Dashboard</span>
              @if (authService.isAdmin()) {
                <a
                  routerLink="/admin"
                  class="inline-flex items-center gap-1.5 rounded-xs border border-(--border) px-2 py-0.5 text-xs font-medium text-(--primary) transition-colors hover:bg-(--accent)"
                >
                  🛡️ Admin
                </a>
              }
            </div>

            <div class="flex items-center gap-3">
              <!-- Bell + notification panel -->
              <div class="relative" (click)="$event.stopPropagation()">
                <button
                  hlmBtn variant="ghost" size="icon"
                  class="relative cursor-pointer"
                  (click)="toggleNotificationPanel()"
                >
                  <lucide-icon [img]="BellIcon" [size]="18"></lucide-icon>
                  @if (notificationService.unreadCount() > 0) {
                    <span
                      class="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-sm bg-red-500 text-[10px] font-bold text-white"
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

              <!-- User chip -->
              <div class="hidden items-center gap-2 rounded-sm border border-(--border) px-3 py-1.5 sm:flex">
                <div class="flex h-7 w-7 items-center justify-center rounded-sm bg-(--muted) text-(--primary)">
                  <lucide-icon [img]="UserIcon" [size]="14"></lucide-icon>
                </div>
                <span class="text-sm font-medium text-(--foreground)">
                  {{ authService.user()?.displayName || authService.user()?.email }}
                </span>
              </div>

              <button
                hlmBtn variant="ghost" size="icon"
                class="cursor-pointer text-(--muted-foreground) hover:text-(--destructive)"
                (click)="onLogout()"
              >
                <lucide-icon [img]="LogOutIcon" [size]="18"></lucide-icon>
              </button>
            </div>
          </div>
        </header>

        <!-- Page content -->
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
  readonly themeService = inject(ThemeService);
  private readonly router = inject(Router);

  readonly showNotificationPanel = signal(false);
  readonly mobileMenuOpen = signal(false);

  // Icons
  readonly BellIcon = Bell;
  readonly UserIcon = User;
  readonly LogOutIcon = LogOut;
  readonly MenuIcon = Menu;
  readonly XIcon = X;
  readonly MoonIcon = Moon;
  readonly SunIcon = Sun;

  readonly sidebarItems = [
    { label: "Vue d'ensemble", route: '/dashboard', icon: LayoutDashboard, exact: true },
    { label: 'Mes commandes', route: '/dashboard/orders', icon: ShoppingCart, exact: false },
    { label: 'Mes rendez-vous', route: '/dashboard/appointments', icon: Calendar, exact: false },
    { label: 'Paramètres', route: '/settings', icon: Settings, exact: false },
  ];

  toggleNotificationPanel(): void {
    this.showNotificationPanel.update((v) => !v);
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
