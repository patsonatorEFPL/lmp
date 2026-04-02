import { Component, inject, OnDestroy, OnInit, signal, viewChild } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  LucideAngularModule,
  LayoutDashboard,
  Package,
  Users,
  ShoppingCart,
  Calendar,
  Settings,
  Menu,
  X,
  Bell,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AdminSseService } from '../../core/services/admin-sse.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationPanelComponent } from './notification-panel.component';
import { ShellAccountMenuComponent } from './shell-account-menu.component';

@Component({
  selector: 'lmp-admin-layout',
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
    @if (adminSse.lastToast(); as toast) {
      <div
        class="fixed bottom-4 right-4 z-[260] max-w-sm rounded-xl border border-(--border) bg-(--card) p-4 shadow-lg ring-1 ring-(--foreground)/5"
        role="alert"
      >
        <p class="text-xs font-semibold text-(--primary)">{{ toast.title }}</p>
        <p class="mt-1 text-sm text-(--foreground)">{{ toast.message }}</p>
      </div>
    }
    <div class="flex min-h-screen bg-(--background)">
      <!-- Sidebar desktop -->
      <aside
        class="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-(--border) bg-(--card) box-border lg:flex"
      >
        <div
          class="flex h-16 min-h-16 shrink-0 items-center gap-3 border-b border-(--border) px-5 box-border bg-(--card)"
        >
          <a routerLink="/" class="flex min-w-0 items-center gap-3 rounded-sm focus-visible:ring-2 focus-visible:ring-(--ring)">
            <img src="/images/logo-lmp.webp" alt="LMP Logo" class="h-9 w-auto shrink-0" />
            <div class="min-w-0">
              <span class="block truncate text-sm font-bold text-(--foreground)">LMP</span>
              <span
                class="mt-0.5 inline-block rounded-md bg-(--primary)/12 px-1.5 py-0.5 text-[10px] font-semibold text-(--primary)"
                >Admin</span
              >
            </div>
          </a>
        </div>

        <nav class="flex-1 space-y-1 overflow-y-auto px-3 py-4">
          <a
            routerLink="/admin"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            [routerLinkActiveOptions]="{ exact: true }"
            class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="DashboardIcon" [size]="18"></lucide-icon>
            Tableau de bord
          </a>
          <a
            routerLink="/admin/services"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="PackageIcon" [size]="18"></lucide-icon>
            Services
          </a>
          <a
            routerLink="/admin/users"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            (click)="adminSse.badgeUsers.set(0)"
            class="relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="UsersIcon" [size]="18"></lucide-icon>
            Utilisateurs
            @if (adminSse.badgeUsers() > 0) {
              <span
                class="absolute right-2 top-2 flex h-4 min-w-4 items-center justify-center rounded-sm bg-red-500 px-1 text-[10px] font-bold text-white"
                >{{ adminSse.badgeUsers() > 9 ? '9+' : adminSse.badgeUsers() }}</span
              >
            }
          </a>
          <a
            routerLink="/admin/orders"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            (click)="adminSse.badgeOrders.set(0)"
            class="relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="OrdersIcon" [size]="18"></lucide-icon>
            Commandes
            @if (adminSse.badgeOrders() > 0) {
              <span
                class="absolute right-2 top-2 flex h-4 min-w-4 items-center justify-center rounded-sm bg-red-500 px-1 text-[10px] font-bold text-white"
                >{{ adminSse.badgeOrders() > 9 ? '9+' : adminSse.badgeOrders() }}</span
              >
            }
          </a>
          <a
            routerLink="/admin/appointments"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            (click)="adminSse.badgeAppointments.set(0)"
            class="relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="CalendarIcon" [size]="18"></lucide-icon>
            Rendez-vous
            @if (adminSse.badgeAppointments() > 0) {
              <span
                class="absolute right-2 top-2 flex h-4 min-w-4 items-center justify-center rounded-sm bg-red-500 px-1 text-[10px] font-bold text-white"
                >{{ adminSse.badgeAppointments() > 9 ? '9+' : adminSse.badgeAppointments() }}</span
              >
            }
          </a>
          <a
            routerLink="/admin/settings"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="SettingsIcon" [size]="18"></lucide-icon>
            Paramètres
          </a>
        </nav>

        <div class="shrink-0 border-t border-(--border) px-4 py-3">
          <p class="text-center text-[10px] font-medium tracking-wide text-(--muted-foreground)">
            Administration
          </p>
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
          class="fixed inset-y-0 left-0 z-50 flex w-[min(18rem,calc(100vw-2.5rem))] flex-col border-r border-(--border) bg-(--card) shadow-xl lg:hidden"
        >
          <div class="flex h-16 items-center justify-between gap-2 border-b border-(--border) px-4">
            <a
              routerLink="/"
              class="flex min-w-0 items-center gap-2 rounded-sm focus-visible:ring-2 focus-visible:ring-(--ring)"
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
            <a
              routerLink="/admin"
              routerLinkActive="bg-(--primary)/10 text-(--primary)"
              [routerLinkActiveOptions]="{ exact: true }"
              class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
              (click)="mobileMenuOpen.set(false)"
            >
              <lucide-icon [img]="DashboardIcon" [size]="18"></lucide-icon>
              Tableau de bord
            </a>
            <a
              routerLink="/admin/services"
              routerLinkActive="bg-(--primary)/10 text-(--primary)"
              class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
              (click)="mobileMenuOpen.set(false)"
            >
              <lucide-icon [img]="PackageIcon" [size]="18"></lucide-icon>
              Services
            </a>
            <a
              routerLink="/admin/users"
              routerLinkActive="bg-(--primary)/10 text-(--primary)"
              class="relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
              (click)="mobileMenuOpen.set(false); adminSse.badgeUsers.set(0)"
            >
              <lucide-icon [img]="UsersIcon" [size]="18"></lucide-icon>
              Utilisateurs
              @if (adminSse.badgeUsers() > 0) {
                <span
                  class="absolute right-2 top-2 flex h-4 min-w-4 items-center justify-center rounded-sm bg-red-500 px-1 text-[10px] font-bold text-white"
                  >{{ adminSse.badgeUsers() > 9 ? '9+' : adminSse.badgeUsers() }}</span
                >
              }
            </a>
            <a
              routerLink="/admin/orders"
              routerLinkActive="bg-(--primary)/10 text-(--primary)"
              class="relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
              (click)="mobileMenuOpen.set(false); adminSse.badgeOrders.set(0)"
            >
              <lucide-icon [img]="OrdersIcon" [size]="18"></lucide-icon>
              Commandes
              @if (adminSse.badgeOrders() > 0) {
                <span
                  class="absolute right-2 top-2 flex h-4 min-w-4 items-center justify-center rounded-sm bg-red-500 px-1 text-[10px] font-bold text-white"
                  >{{ adminSse.badgeOrders() > 9 ? '9+' : adminSse.badgeOrders() }}</span
                >
              }
            </a>
            <a
              routerLink="/admin/appointments"
              routerLinkActive="bg-(--primary)/10 text-(--primary)"
              class="relative flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
              (click)="mobileMenuOpen.set(false); adminSse.badgeAppointments.set(0)"
            >
              <lucide-icon [img]="CalendarIcon" [size]="18"></lucide-icon>
              Rendez-vous
              @if (adminSse.badgeAppointments() > 0) {
                <span
                  class="absolute right-2 top-2 flex h-4 min-w-4 items-center justify-center rounded-sm bg-red-500 px-1 text-[10px] font-bold text-white"
                  >{{ adminSse.badgeAppointments() > 9 ? '9+' : adminSse.badgeAppointments() }}</span
                >
              }
            </a>
            <a
              routerLink="/admin/settings"
              routerLinkActive="bg-(--primary)/10 text-(--primary)"
              class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
              (click)="mobileMenuOpen.set(false)"
            >
              <lucide-icon [img]="SettingsIcon" [size]="18"></lucide-icon>
              Paramètres
            </a>
          </nav>
        </aside>
      }

      <div class="flex flex-1 flex-col lg:ml-64">
        <header
          class="sticky top-0 z-20 bg-(--card)/95 backdrop-blur-sm supports-[backdrop-filter]:bg-(--card)/80"
        >
          <div
            class="flex h-16 min-h-16 shrink-0 items-center justify-between gap-3 border-b border-(--border) px-4 box-border sm:px-6"
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
              <div class="flex min-w-0 items-center gap-2 sm:gap-3">
                <h1
                  class="min-w-0 truncate text-sm font-semibold tracking-tight text-(--foreground) sm:text-base"
                >
                  Administration
                </h1>
                <span
                  class="hidden shrink-0 items-center gap-1.5 text-[11px] text-(--muted-foreground) sm:inline-flex"
                  title="Flux temps réel (SSE)"
                >
                  <span
                    class="h-2 w-2 rounded-full"
                    [class.bg-emerald-500]="adminSse.connected()"
                    [class.bg-red-500]="!adminSse.connected()"
                  ></span>
                  Live
                </span>
              </div>
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
                variant="admin"
                (menuOpenChange)="onAccountMenuOpenChange($event)"
                (logoutRequest)="onLogout()"
              />
            </div>
          </div>
        </header>

        <main class="p-4 sm:p-6">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  readonly authService = inject(AuthService);
  readonly adminSse = inject(AdminSseService);
  readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  private readonly accountMenu = viewChild(ShellAccountMenuComponent);

  readonly showNotificationPanel = signal(false);
  readonly mobileMenuOpen = signal(false);

  readonly DashboardIcon = LayoutDashboard;
  readonly PackageIcon = Package;
  readonly UsersIcon = Users;
  readonly OrdersIcon = ShoppingCart;
  readonly CalendarIcon = Calendar;
  readonly SettingsIcon = Settings;
  readonly MenuIcon = Menu;
  readonly XIcon = X;
  readonly BellIcon = Bell;

  ngOnInit(): void {
    this.adminSse.connect();
  }

  ngOnDestroy(): void {
    this.adminSse.disconnect();
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

  onLogout(): void {
    this.adminSse.disconnect();
    this.notificationService.reset();
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
