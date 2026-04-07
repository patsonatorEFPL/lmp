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
import { DatePipe, CurrencyPipe, NgClass } from '@angular/common';
import {
  LucideAngularModule,
  ShoppingCart,
  Calendar,
  Settings,
  Star,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Loader2,
  Eye,
  ChevronRight,
  FileText,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import {
  DashboardService,
  DashboardStats,
} from '../../core/services/dashboard.service';
import { NotificationService } from '../../core/services/notification.service';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { environment } from '../../../environments/environment';
import { firstValueFrom } from 'rxjs';

const EMPTY_DASHBOARD_STATS: DashboardStats = {
  totalOrders: 0,
  completedOrders: 0,
  inProgressOrders: 0,
  totalReviews: 0,
  upcomingAppointments: 0,
  totalSpent: 0,
  recentOrders: [],
  recentReviews: [],
  upcomingAppointmentsList: [],
};

@Component({
  selector: 'lmp-dashboard-overview',
  standalone: true,
  imports: [
    RouterLink,
    LucideAngularModule,
    HlmButton,
    DatePipe,
    CurrencyPipe,
    NgClass,
  ],
  template: `
    @if (blockingLoader()) {
      <div class="flex items-center justify-center py-16">
        <lucide-icon [img]="Loader2Icon" [size]="32" class="animate-spin text-zinc-600 dark:text-zinc-400"></lucide-icon>
      </div>
    }

    @if (!blockingLoader() && stats()) {
      <div class="mb-6 border-b border-zinc-200/90 pb-4 dark:border-zinc-800">
        <p class="text-xs font-medium uppercase tracking-wide text-zinc-500 dark:text-zinc-400">Vue d’ensemble</p>
        <h2 class="mt-1 text-lg font-semibold text-zinc-900 dark:text-zinc-100">Espace client</h2>
      </div>

      <!-- Quick stats (même base que /admin : cartes zinc + ombre légère) -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div
          class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-900/50"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-zinc-500 dark:text-zinc-400">Commandes</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-sm bg-zinc-100 dark:bg-zinc-800/80"
            >
              <lucide-icon [img]="ShoppingCartIcon" [size]="18" class="text-zinc-700 dark:text-zinc-200"></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-zinc-900 dark:text-zinc-100">{{ stats()!.totalOrders }}</span>
          </div>
          <p class="mt-1 text-xs text-zinc-500 dark:text-zinc-400">{{ stats()!.completedOrders }} terminée(s)</p>
        </div>

        <div
          class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-900/50"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-zinc-500 dark:text-zinc-400">En cours</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-sm bg-zinc-100 dark:bg-zinc-800/80"
            >
              <lucide-icon [img]="ClockIcon" [size]="18" class="text-amber-600 dark:text-amber-500"></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-zinc-900 dark:text-zinc-100">{{ stats()!.inProgressOrders }}</span>
          </div>
          <p class="mt-1 text-xs text-zinc-500 dark:text-zinc-400">Commandes actives</p>
        </div>

        <div
          class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-900/50"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-zinc-500 dark:text-zinc-400">Rendez-vous</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-sm bg-zinc-100 dark:bg-zinc-800/80"
            >
              <lucide-icon [img]="CalendarIcon" [size]="18" class="text-zinc-700 dark:text-zinc-200"></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-zinc-900 dark:text-zinc-100">{{ stats()!.upcomingAppointments }}</span>
          </div>
          <p class="mt-1 text-xs text-zinc-500 dark:text-zinc-400">À venir</p>
        </div>

        <div
          class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-900/50"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-zinc-500 dark:text-zinc-400">Avis</span>
            <div
              class="flex h-9 w-9 items-center justify-center rounded-sm bg-zinc-100 dark:bg-zinc-800/80"
            >
              <lucide-icon [img]="StarIcon" [size]="18" class="text-emerald-600 dark:text-emerald-400"></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-zinc-900 dark:text-zinc-100">{{ stats()!.totalReviews }}</span>
          </div>
          <p class="mt-1 text-xs text-zinc-500 dark:text-zinc-400">Avis donnés</p>
        </div>
      </div>

      <!-- Two-column layout: Recent orders + Quick actions -->
      <div class="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <!-- Recent Orders -->
        <div class="lg:col-span-2">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-zinc-900 dark:text-zinc-100">Commandes récentes</h2>
            <a
              routerLink="/dashboard/orders"
              class="text-xs font-semibold text-zinc-600 hover:text-zinc-900 hover:underline dark:text-zinc-400 dark:hover:text-zinc-100"
            >
              Voir tout →
            </a>
          </div>
          <div class="rounded border border-zinc-200/90 bg-white dark:border-zinc-800 dark:bg-zinc-900/50">
            @if (stats()!.recentOrders.length === 0) {
              <div class="flex flex-col items-center justify-center py-10 text-center">
                <div class="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-100 dark:bg-zinc-800">
                  <lucide-icon [img]="FileTextIcon" [size]="20" class="text-zinc-500 dark:text-zinc-400"></lucide-icon>
                </div>
                <p class="mt-3 text-sm font-medium text-zinc-900 dark:text-zinc-100">Aucune commande</p>
                <p class="mt-1 text-xs text-zinc-500 dark:text-zinc-400">Vos commandes apparaîtront ici.</p>
                <a routerLink="/services" hlmBtn variant="default" size="sm" class="mt-4 cursor-pointer">
                  Découvrir nos services
                </a>
              </div>
            } @else {
              <div class="divide-y divide-zinc-200/90 dark:divide-zinc-800">
                @for (order of stats()!.recentOrders; track order.id) {
                  <a
                    [routerLink]="['/dashboard/orders']"
                    [queryParams]="{ open: order.id }"
                    class="flex items-center justify-between p-4 transition-colors hover:bg-zinc-50 dark:hover:bg-zinc-800/60"
                  >
                    <div class="flex items-center gap-3">
                      <div
                        class="flex h-9 w-9 shrink-0 items-center justify-center rounded-sm"
                        [ngClass]="getStatusBgClass(order.status)"
                      >
                        <lucide-icon [img]="getStatusIcon(order.status)" [size]="16"></lucide-icon>
                      </div>
                      <div>
                        <p class="text-sm font-medium text-zinc-900 dark:text-zinc-100">{{ order.serviceName }}</p>
                        <p class="text-xs text-zinc-500 dark:text-zinc-400">{{ order.createdAt | date: 'dd MMM yyyy' }}</p>
                      </div>
                    </div>
                    <div class="flex items-center gap-3">
                      <div class="text-right">
                        <p class="text-sm font-semibold text-zinc-900 dark:text-zinc-100">
                          {{ order.totalAmount | currency:(order.currency || 'EUR'):'symbol':'1.2-2':'fr' }}
                        </p>
                        <span
                          class="inline-block rounded-xs px-2 py-0.5 text-xs font-medium"
                          [ngClass]="getStatusBadgeClass(order.status)"
                        >
                          {{ getStatusLabel(order.status) }}
                        </span>
                      </div>
                      <lucide-icon [img]="ChevronRightIcon" [size]="16" class="text-zinc-400 dark:text-zinc-500"></lucide-icon>
                    </div>
                  </a>
                }
              </div>
            }
          </div>
        </div>

        <!-- Quick actions -->
        <div>
          <h2 class="mb-4 text-lg font-semibold text-zinc-900 dark:text-zinc-100">Actions rapides</h2>
          <div class="space-y-3">
            @for (action of quickActions; track action.label) {
              <a
                [routerLink]="action.route"
                class="group flex items-center gap-3 rounded border border-zinc-200/90 bg-white p-4 shadow-sm transition-all hover:bg-zinc-50 dark:border-zinc-800 dark:bg-zinc-900/50 dark:hover:bg-zinc-800/80"
              >
                <div
                  class="flex h-10 w-10 shrink-0 items-center justify-center rounded-sm bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-100"
                >
                  <lucide-icon [img]="action.icon" [size]="18"></lucide-icon>
                </div>
                <div class="flex-1">
                  <h3 class="text-sm font-semibold text-zinc-900 dark:text-zinc-100">{{ action.label }}</h3>
                  <p class="text-xs text-zinc-500 dark:text-zinc-400">{{ action.description }}</p>
                </div>
                <lucide-icon
                  [img]="ChevronRightIcon" [size]="16"
                  class="text-zinc-400 transition-transform group-hover:translate-x-0.5 dark:text-zinc-500"
                ></lucide-icon>
              </a>
            }
          </div>
        </div>
      </div>

      <!-- Upcoming Appointments -->
      @if (stats()!.upcomingAppointmentsList.length > 0) {
        <div class="mt-8">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold text-zinc-900 dark:text-zinc-100">Prochains rendez-vous</h2>
            <a
              routerLink="/dashboard/appointments"
              class="text-xs font-semibold text-zinc-600 hover:text-zinc-900 hover:underline dark:text-zinc-400 dark:hover:text-zinc-100"
            >
              Voir tout →
            </a>
          </div>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (appt of stats()!.upcomingAppointmentsList; track appt.id) {
              <div
                class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-900/50"
              >
                <div class="flex items-start justify-between">
                  <div
                    class="flex h-9 w-9 items-center justify-center rounded-sm bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200"
                  >
                    <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
                  </div>
                  <span
                    class="rounded-xs bg-zinc-100 px-2 py-0.5 text-xs font-medium text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400"
                  >
                    {{ appt.durationMinutes }} min
                  </span>
                </div>
                <h3 class="mt-3 text-sm font-semibold text-zinc-900 dark:text-zinc-100">{{ appt.subject }}</h3>
                <p class="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
                  {{ appt.appointmentDate | date: 'EEEE dd MMM yyyy à HH:mm' }}
                </p>
              </div>
            }
          </div>
        </div>
      }

      <!-- Recent Reviews -->
      @if (stats()!.recentReviews.length > 0) {
        <div class="mt-8">
          <h2 class="mb-4 text-lg font-semibold text-zinc-900 dark:text-zinc-100">Vos avis récents</h2>
          <div class="space-y-3">
            @for (review of stats()!.recentReviews; track review.id) {
              <div
                class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-900/50"
              >
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-1">
                    @for (s of [1, 2, 3, 4, 5]; track s) {
                      <lucide-icon
                        [img]="StarIcon" [size]="14"
                        [ngClass]="{ 'text-amber-400': s <= review.rating, 'text-zinc-300 dark:text-zinc-600': s > review.rating }"
                      ></lucide-icon>
                    }
                  </div>
                  @if (review.approved) {
                    <span
                      class="rounded-xs bg-emerald-500/10 px-2 py-0.5 text-xs font-medium text-emerald-700 dark:text-emerald-400"
                      >Approuvé</span
                    >
                  } @else {
                    <span
                      class="rounded-xs bg-amber-500/10 px-2 py-0.5 text-xs font-medium text-amber-800 dark:text-amber-400"
                      >En attente</span
                    >
                  }
                </div>
                @if (review.comment) {
                  <p class="mt-2 text-sm text-zinc-600 dark:text-zinc-400">{{ review.comment }}</p>
                }
                <p class="mt-2 text-xs text-zinc-500 dark:text-zinc-500">{{ review.createdAt | date: 'dd MMM yyyy' }}</p>
              </div>
            }
          </div>
        </div>
      }
    }
  `,
})
export class DashboardOverviewComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly visiblePoll = inject(VisiblePollService);
  private readonly platformId = inject(PLATFORM_ID);

  constructor() {
    effect(() => {
      const statsHint = this.notificationService.liveDashboardStatsHint();
      if (statsHint > 0) {
        untracked(() => this.statsResource.reload());
      }
    });
  }

  /** Rechargé quand `browser` passe à true (hydratation) ou via `reload()`. */
  readonly statsResource = resource({
    params: () => ({ browser: isPlatformBrowser(this.platformId) }),
    loader: async ({ params }) => {
      if (!params.browser) {
        return null;
      }
      try {
        return await firstValueFrom(this.dashboardService.getStats());
      } catch {
        return EMPTY_DASHBOARD_STATS;
      }
    },
  });

  readonly stats = computed(() => this.statsResource.value() ?? null);

  readonly blockingLoader = computed(
    () => this.statsResource.status() === 'loading' && !this.statsResource.hasValue(),
  );

  // Icons
  readonly ShoppingCartIcon = ShoppingCart;
  readonly CalendarIcon = Calendar;
  readonly StarIcon = Star;
  readonly ClockIcon = Clock;
  readonly CheckCircleIcon = CheckCircle;
  readonly XCircleIcon = XCircle;
  readonly AlertCircleIcon = AlertCircle;
  readonly Loader2Icon = Loader2;
  readonly EyeIcon = Eye;
  readonly ChevronRightIcon = ChevronRight;
  readonly FileTextIcon = FileText;

  readonly quickActions = [
    { label: 'Voir les services', description: 'Parcourir notre catalogue', route: '/services', icon: ShoppingCart },
    { label: 'Prendre rendez-vous', description: 'Planifier une consultation', route: '/contact', icon: Calendar },
    { label: 'Paramètres', description: 'Gérer votre compte', route: '/dashboard/settings', icon: Settings },
  ];

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.visiblePoll.subscribeWhileVisible(
        this.destroyRef,
        environment.dashboardPollIntervalMs,
        () => this.statsResource.reload(),
      );
    }
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PAYMENT_PENDING: 'Paiement en attente', PENDING: 'En attente',
      CONFIRMED: 'Confirmée', PROCESSING: 'En traitement',
      IN_PROGRESS: 'En cours', SHIPPED: 'Expédiée', DELIVERED: 'Livrée',
      COMPLETED: 'Terminée', UNDER_REVIEW: 'En révision',
      CANCELLED: 'Annulée', REFUNDED: 'Remboursée',
    };
    return labels[status] || status;
  }

  getStatusBadgeClass(status: string): string {
    const classes: Record<string, string> = {
      COMPLETED: 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200',
      DELIVERED: 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200',
      CONFIRMED: 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200',
      IN_PROGRESS: 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200',
      PROCESSING: 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200',
      PENDING: 'bg-zinc-500/10 text-zinc-600 dark:text-zinc-400',
      PAYMENT_PENDING: 'bg-orange-500/10 text-orange-600 dark:text-orange-400',
      CANCELLED: 'bg-red-500/10 text-red-600 dark:text-red-400',
      REFUNDED: 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-300',
    };
    return classes[status] || 'bg-zinc-500/10 text-zinc-600';
  }

  getStatusBgClass(status: string): string {
    return this.getStatusBadgeClass(status);
  }

  getStatusIcon(status: string) {
    switch (status) {
      case 'COMPLETED': case 'DELIVERED': return this.CheckCircleIcon;
      case 'CANCELLED': case 'REFUNDED': return this.XCircleIcon;
      case 'IN_PROGRESS': case 'PROCESSING': return this.ClockIcon;
      default: return this.AlertCircleIcon;
    }
  }
}
