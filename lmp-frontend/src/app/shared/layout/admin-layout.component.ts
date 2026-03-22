import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import {
  LucideAngularModule,
  LayoutDashboard,
  Package,
  Users,
  ShoppingCart,
  Calendar,
  Settings,
  LogOut,
  ChevronLeft,
  Moon,
  Sun,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'lmp-admin-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    LucideAngularModule,
    HlmButton,
  ],
  template: `
    <div class="flex min-h-screen bg-(--background)">
      <!-- Sidebar -->
      <aside
        class="fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-(--border) bg-(--card)"
      >
        <!-- Logo -->
        <div
          class="flex h-16 items-center gap-3 border-b border-(--border) px-6"
        >
          <a routerLink="/" class="flex items-center gap-3">
            <img
              src="/images/logo-lmp.webp"
              alt="LMP Logo"
              class="h-9 w-auto"
            />
            <div>
              <span class="text-sm font-bold text-(--foreground)">LMP</span>
              <span
                class="ml-1 rounded bg-(--primary)/10 px-1.5 py-0.5 text-[10px] font-semibold text-(--primary)"
                >ADMIN</span
              >
            </div>
          </a>
        </div>

        <!-- Navigation -->
        <nav class="flex-1 space-y-1 px-3 py-4">
          <a
            routerLink="/admin"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            [routerLinkActiveOptions]="{ exact: true }"
            class="flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="DashboardIcon" [size]="18"></lucide-icon>
            Tableau de bord
          </a>
          <a
            routerLink="/admin/services"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            class="flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="PackageIcon" [size]="18"></lucide-icon>
            Services
          </a>
          <a
            routerLink="/admin/users"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            class="flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="UsersIcon" [size]="18"></lucide-icon>
            Utilisateurs
          </a>
          <a
            routerLink="/admin/orders"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            class="flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="OrdersIcon" [size]="18"></lucide-icon>
            Commandes
          </a>
          <a
            routerLink="/admin/appointments"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            class="flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="CalendarIcon" [size]="18"></lucide-icon>
            Rendez-vous
          </a>
          <a
            routerLink="/admin/settings"
            routerLinkActive="bg-(--primary)/10 text-(--primary)"
            class="flex items-center gap-3 rounded-sm px-3 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground)"
          >
            <lucide-icon [img]="SettingsIcon" [size]="18"></lucide-icon>
            Paramètres
          </a>
        </nav>

        <!-- Bottom -->
        <div class="border-t border-(--border) p-4">
          <div class="flex items-center justify-between">
            <button
              hlmBtn
              variant="ghost"
              size="sm"
              class="cursor-pointer gap-2 text-xs text-(--muted-foreground)"
              (click)="toggleTheme()"
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
              hlmBtn
              variant="ghost"
              size="icon"
              class="h-8 w-8 cursor-pointer text-(--muted-foreground) hover:text-(--destructive)"
              (click)="onLogout()"
            >
              <lucide-icon [img]="LogOutIcon" [size]="16"></lucide-icon>
            </button>
          </div>
        </div>
      </aside>

      <!-- Main content -->
      <div class="ml-64 flex-1">
        <!-- Top bar -->
        <header
          class="sticky top-0 z-40 flex h-14 items-center justify-between border-b border-(--border) bg-(--card) px-6"
        >
          <div class="flex items-center gap-3">
            <a
              routerLink="/"
              class="flex items-center gap-1 text-xs text-(--muted-foreground) hover:text-(--foreground)"
            >
              <lucide-icon [img]="BackIcon" [size]="14"></lucide-icon>
              Retour au site
            </a>
          </div>
          <div
            class="flex items-center gap-2 rounded-sm border border-(--border) px-3 py-1.5"
          >
            <div
              class="flex h-7 w-7 items-center justify-center rounded-full bg-(--muted) text-(--primary)"
            >
              <lucide-icon [img]="UsersIcon" [size]="14"></lucide-icon>
            </div>
            <span class="text-sm font-medium text-(--foreground)">
              {{ authService.user()?.displayName || authService.user()?.email }}
            </span>
          </div>
        </header>

        <!-- Page content -->
        <main class="p-6">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
})
export class AdminLayoutComponent {
  readonly authService = inject(AuthService);
  readonly themeService = inject(ThemeService);
  private readonly router = inject(Router);

  readonly DashboardIcon = LayoutDashboard;
  readonly PackageIcon = Package;
  readonly UsersIcon = Users;
  readonly OrdersIcon = ShoppingCart;
  readonly CalendarIcon = Calendar;
  readonly SettingsIcon = Settings;
  readonly LogOutIcon = LogOut;
  readonly BackIcon = ChevronLeft;
  readonly MoonIcon = Moon;
  readonly SunIcon = Sun;

  toggleTheme(): void {
    this.themeService.toggle();
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
