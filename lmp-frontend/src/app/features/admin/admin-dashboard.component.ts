import { isPlatformBrowser } from '@angular/common';
import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  PLATFORM_ID,
  computed,
  effect,
  resource,
  untracked,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  Users,
  ShoppingCart,
  Package,
  Calendar,
  TrendingUp,
  Loader2,
  ArrowRight,
} from 'lucide-angular';
import { AdminService, AdminDashboardStats, CatalogStats } from '../../core/services/admin.service';
import { AdminSseService } from '../../core/services/admin-sse.service';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { environment } from '../../../environments/environment';
import { firstValueFrom } from 'rxjs';

type AdminDashboardPayload = {
  stats: AdminDashboardStats | null;
  catalog: CatalogStats | null;
};

@Component({
  selector: 'lmp-admin-dashboard',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  template: `
    <div class="p-4 sm:p-5">
    @if (blockingLoader()) {
      <div class="flex items-center justify-center py-16">
        <lucide-icon
          [img]="Loader2Icon"
          [size]="32"
          class="animate-spin text-(--primary)"
        ></lucide-icon>
      </div>
    }

    <!-- Stats Grid (cartes type vue liste) -->
    @if (!blockingLoader() && stats()) {
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <!-- Users -->
        <div
          class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-950/80"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-(--muted-foreground)">Utilisateurs</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)"
            >
              <lucide-icon
                [img]="UsersIcon"
                [size]="18"
                class="text-(--foreground)"
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
          class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-950/80"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-(--muted-foreground)">Commandes</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)"
            >
              <lucide-icon
                [img]="OrdersIcon"
                [size]="18"
                class="text-(--foreground)"
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
          class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-950/80"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-(--muted-foreground)">Services</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)"
            >
              <lucide-icon
                [img]="PackageIcon"
                [size]="18"
                class="text-(--primary)"
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
          class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-950/80"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-(--muted-foreground)">Rendez-vous</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)"
            >
              <lucide-icon
                [img]="CalendarIcon"
                [size]="18"
                class="text-(--foreground)"
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
          class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400"
        >
          Accès rapides
        </h2>
        <div class="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <a
            routerLink="/admin/services"
            class="group flex items-center justify-between rounded border border-zinc-200/90 bg-white p-4 shadow-sm transition-all hover:bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-950/80 dark:hover:bg-zinc-800/80"
          >
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-sm bg-(--muted) text-(--primary)"
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
              class="text-(--muted-foreground) transition-transform "
            ></lucide-icon>
          </a>

          <a
            routerLink="/admin/users"
            class="group flex items-center justify-between rounded border border-zinc-200/90 bg-white p-4 shadow-sm transition-all hover:bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-950/80 dark:hover:bg-zinc-800/80"
          >
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-sm bg-(--muted) text-(--foreground)"
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
              class="text-(--muted-foreground) transition-transform "
            ></lucide-icon>
          </a>

          <a
            routerLink="/admin/orders"
            class="group flex items-center justify-between rounded border border-zinc-200/90 bg-white p-4 shadow-sm transition-all hover:bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-950/80 dark:hover:bg-zinc-800/80"
          >
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-sm bg-(--muted) text-(--foreground)"
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
              class="text-(--muted-foreground) transition-transform "
            ></lucide-icon>
          </a>

          <a
            routerLink="/admin/appointments"
            class="group flex items-center justify-between rounded border border-zinc-200/90 bg-white p-4 shadow-sm transition-all hover:bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-950/80 dark:hover:bg-zinc-800/80"
          >
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-sm bg-(--muted) text-(--foreground)"
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
              class="text-(--muted-foreground) transition-transform "
            ></lucide-icon>
          </a>
        </div>
      </div>
    }
    </div>
  `,
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly adminSse = inject(AdminSseService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroyRef = inject(DestroyRef);
  private readonly visiblePoll = inject(VisiblePollService);

  constructor() {
    effect(() => {
      const orders = this.adminSse.badgeOrders();
      const users = this.adminSse.badgeUsers();
      const appts = this.adminSse.badgeAppointments();
      if (orders > 0 || users > 0 || appts > 0) {
        if (isPlatformBrowser(this.platformId)) {
          untracked(() => this.dashboardResource.reload());
        }
      }
    });
  }

  readonly dashboardResource = resource<AdminDashboardPayload, { browser: boolean }>({
    params: () => ({ browser: isPlatformBrowser(this.platformId) }),
    loader: async ({ params }) => {
      if (!params.browser) {
        return { stats: null, catalog: null };
      }
      let stats: AdminDashboardStats | null = null;
      try {
        stats = await firstValueFrom(this.adminService.getDashboardStats());
      } catch {
        stats = null;
      }
      let catalog: CatalogStats | null = null;
      try {
        catalog = await firstValueFrom(this.adminService.getCatalogStats());
      } catch {
        catalog = null;
      }
      return { stats, catalog };
    },
  });

  readonly stats = computed(() => this.dashboardResource.value()?.stats ?? null);
  readonly catalogStats = computed(() => this.dashboardResource.value()?.catalog ?? null);

  readonly blockingLoader = computed(
    () => this.dashboardResource.status() === 'loading' && !this.dashboardResource.hasValue(),
  );

  readonly UsersIcon = Users;
  readonly OrdersIcon = ShoppingCart;
  readonly PackageIcon = Package;
  readonly CalendarIcon = Calendar;
  readonly TrendingUpIcon = TrendingUp;
  readonly Loader2Icon = Loader2;
  readonly ArrowRightIcon = ArrowRight;

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.visiblePoll.subscribeWhileVisible(
        this.destroyRef,
        environment.dashboardPollIntervalMs,
        () => this.dashboardResource.reload(),
      );
    }
  }

}
