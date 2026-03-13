import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgClass } from '@angular/common';
import {
  LucideAngularModule,
  Users,
  ShoppingCart,
  Package,
  Calendar,
  TrendingUp,
  RefreshCw,
  Loader2,
  ArrowRight,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AdminService, AdminDashboardStats, CatalogStats } from '../../core/services/admin.service';

@Component({
  selector: 'lmp-admin-dashboard',
  standalone: true,
  imports: [RouterLink, LucideAngularModule, HlmButton, NgClass],
  template: `
    <!-- Welcome Banner -->
    <div
      class="rounded-2xl border border-(--border) bg-gradient-to-br from-violet-500/10 to-transparent p-6 sm:p-8"
    >
      <div class="flex items-center justify-between">
        <div>
          <h1
            class="font-display text-2xl font-bold text-(--foreground) sm:text-3xl"
          >
            Administration 🛡️
          </h1>
          <p class="mt-2 text-sm text-(--muted-foreground)">
            Gérez les services, utilisateurs, commandes et plus encore.
          </p>
        </div>
        <button
          hlmBtn
          variant="ghost"
          size="icon"
          class="cursor-pointer"
          (click)="loadStats()"
        >
          <lucide-icon
            [img]="RefreshCwIcon"
            [size]="18"
            [ngClass]="{ 'animate-spin': loading() }"
          ></lucide-icon>
        </button>
      </div>
    </div>

    <!-- Loading -->
    @if (loading()) {
      <div class="mt-8 flex items-center justify-center py-16">
        <lucide-icon
          [img]="Loader2Icon"
          [size]="32"
          class="animate-spin text-(--primary)"
        ></lucide-icon>
      </div>
    }

    <!-- Stats Grid -->
    @if (!loading() && stats()) {
      <div class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <!-- Users -->
        <div
          class="rounded-xl border border-(--border) bg-(--card) p-5"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium uppercase tracking-wider text-(--muted-foreground)">Utilisateurs</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-500/10"
            >
              <lucide-icon
                [img]="UsersIcon"
                [size]="18"
                class="text-blue-500"
              ></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-(--foreground)">{{
              stats()!.totalUsers
            }}</span>
            <p class="mt-1 text-xs text-(--muted-foreground)">
              {{ stats()!.activeUsers }} actifs
            </p>
          </div>
        </div>

        <!-- Orders -->
        <div
          class="rounded-xl border border-(--border) bg-(--card) p-5"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium uppercase tracking-wider text-(--muted-foreground)">Commandes</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-500/10"
            >
              <lucide-icon
                [img]="OrdersIcon"
                [size]="18"
                class="text-emerald-500"
              ></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-(--foreground)">{{
              stats()!.totalOrders
            }}</span>
          </div>
        </div>

        <!-- Services -->
        <div
          class="rounded-xl border border-(--border) bg-(--card) p-5"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium uppercase tracking-wider text-(--muted-foreground)">Services</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-lg bg-violet-500/10"
            >
              <lucide-icon
                [img]="PackageIcon"
                [size]="18"
                class="text-violet-500"
              ></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-(--foreground)">{{
              catalogStats()?.totalServices || 0
            }}</span>
            <p class="mt-1 text-xs text-(--muted-foreground)">
              {{ catalogStats()?.activeServices || 0 }} actifs · {{ catalogStats()?.featuredServices || 0 }} en vedette
            </p>
          </div>
        </div>

        <!-- Appointments -->
        <div
          class="rounded-xl border border-(--border) bg-(--card) p-5"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium uppercase tracking-wider text-(--muted-foreground)">Rendez-vous</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-500/10"
            >
              <lucide-icon
                [img]="CalendarIcon"
                [size]="18"
                class="text-amber-500"
              ></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-(--foreground)">{{
              stats()!.totalAppointments
            }}</span>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="mt-8">
        <h2
          class="font-display text-lg font-semibold text-(--foreground)"
        >
          Accès rapides
        </h2>
        <div class="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <a
            routerLink="/admin/services"
            class="group flex items-center justify-between rounded-xl border border-(--border) bg-(--card) p-4 transition-all hover:border-violet-500/30 hover:shadow-md"
          >
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-lg bg-violet-500/10 text-violet-500"
              >
                <lucide-icon [img]="PackageIcon" [size]="20"></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-semibold text-(--foreground)">
                  Gérer les Services
                </p>
                <p class="text-xs text-(--muted-foreground)">
                  Créer, modifier, supprimer
                </p>
              </div>
            </div>
            <lucide-icon
              [img]="ArrowRightIcon"
              [size]="16"
              class="text-(--muted-foreground) transition-transform group-hover:translate-x-1"
            ></lucide-icon>
          </a>

          <a
            routerLink="/admin/users"
            class="group flex items-center justify-between rounded-xl border border-(--border) bg-(--card) p-4 transition-all hover:border-blue-500/30 hover:shadow-md"
          >
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-500/10 text-blue-500"
              >
                <lucide-icon [img]="UsersIcon" [size]="20"></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-semibold text-(--foreground)">
                  Gérer les Utilisateurs
                </p>
                <p class="text-xs text-(--muted-foreground)">
                  Comptes, rôles, verrouillage
                </p>
              </div>
            </div>
            <lucide-icon
              [img]="ArrowRightIcon"
              [size]="16"
              class="text-(--muted-foreground) transition-transform group-hover:translate-x-1"
            ></lucide-icon>
          </a>

          <a
            routerLink="/admin/orders"
            class="group flex items-center justify-between rounded-xl border border-(--border) bg-(--card) p-4 transition-all hover:border-emerald-500/30 hover:shadow-md"
          >
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-500"
              >
                <lucide-icon [img]="OrdersIcon" [size]="20"></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-semibold text-(--foreground)">
                  Gérer les Commandes
                </p>
                <p class="text-xs text-(--muted-foreground)">
                  Statuts, suivi, détails
                </p>
              </div>
            </div>
            <lucide-icon
              [img]="ArrowRightIcon"
              [size]="16"
              class="text-(--muted-foreground) transition-transform group-hover:translate-x-1"
            ></lucide-icon>
          </a>

          <a
            routerLink="/admin/appointments"
            class="group flex items-center justify-between rounded-xl border border-(--border) bg-(--card) p-4 transition-all hover:border-amber-500/30 hover:shadow-md"
          >
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-lg bg-amber-500/10 text-amber-500"
              >
                <lucide-icon [img]="CalendarIcon" [size]="20"></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-semibold text-(--foreground)">
                  Gérer les Rendez-vous
                </p>
                <p class="text-xs text-(--muted-foreground)">
                  Consulter, confirmer, annuler
                </p>
              </div>
            </div>
            <lucide-icon
              [img]="ArrowRightIcon"
              [size]="16"
              class="text-(--muted-foreground) transition-transform group-hover:translate-x-1"
            ></lucide-icon>
          </a>
        </div>
      </div>
    }
  `,
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly stats = signal<AdminDashboardStats | null>(null);
  readonly catalogStats = signal<CatalogStats | null>(null);
  readonly loading = signal(true);

  readonly UsersIcon = Users;
  readonly OrdersIcon = ShoppingCart;
  readonly PackageIcon = Package;
  readonly CalendarIcon = Calendar;
  readonly TrendingUpIcon = TrendingUp;
  readonly RefreshCwIcon = RefreshCw;
  readonly Loader2Icon = Loader2;
  readonly ArrowRightIcon = ArrowRight;

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading.set(true);
    this.adminService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.adminService.getCatalogStats().subscribe({
      next: (data) => this.catalogStats.set(data),
    });
  }
}
