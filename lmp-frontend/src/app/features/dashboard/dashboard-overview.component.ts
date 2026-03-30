import { Component, inject, OnInit, signal, effect, untracked } from '@angular/core';
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
  RefreshCw,
  Eye,
  ChevronRight,
  FileText,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AuthService } from '../../core/services/auth.service';
import {
  DashboardService,
  DashboardStats,
} from '../../core/services/dashboard.service';
import { NotificationService } from '../../core/services/notification.service';

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
    <!-- Welcome banner -->
    <div
      class="rounded-sm border border-(--border) bg-(--card) p-6 sm:p-8"
    >
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-(--foreground)">
            Bienvenue, {{ authService.user()?.firstName || 'Utilisateur' }}
          </h1>
          <p class="mt-2 text-sm text-(--muted-foreground)">
            Gérez vos services, commandes et rendez-vous depuis votre espace personnel.
          </p>
        </div>
        <button
          hlmBtn variant="ghost" size="icon" class="cursor-pointer"
          (click)="loadStats()"
        >
          <lucide-icon
            [img]="RefreshCwIcon" [size]="18"
            [ngClass]="{ 'animate-spin': loading() }"
          ></lucide-icon>
        </button>
      </div>
    </div>

    <!-- Loading -->
    @if (loading()) {
      <div class="mt-8 flex items-center justify-center py-16">
        <lucide-icon [img]="Loader2Icon" [size]="32" class="animate-spin text-(--primary)"></lucide-icon>
      </div>
    }

    @if (!loading() && stats()) {
      <!-- Quick stats -->
      <div class="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="rounded-sm border border-(--border) bg-(--card) p-5">
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-(--muted-foreground)">Commandes</span>
            <div class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted) text-(--primary)">
              <lucide-icon [img]="ShoppingCartIcon" [size]="16"></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-(--foreground)">{{ stats()!.totalOrders }}</span>
          </div>
          <p class="mt-1 text-xs text-(--muted-foreground)">{{ stats()!.completedOrders }} terminée(s)</p>
        </div>

        <div class="rounded-sm border border-(--border) bg-(--card) p-5">
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-(--muted-foreground)">En cours</span>
            <div class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted) text-amber-600">
              <lucide-icon [img]="ClockIcon" [size]="16"></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-(--foreground)">{{ stats()!.inProgressOrders }}</span>
          </div>
          <p class="mt-1 text-xs text-(--muted-foreground)">Commandes actives</p>
        </div>

        <div class="rounded-sm border border-(--border) bg-(--card) p-5">
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-(--muted-foreground)">Rendez-vous</span>
            <div class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted) text-(--primary)">
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-(--foreground)">{{ stats()!.upcomingAppointments }}</span>
          </div>
          <p class="mt-1 text-xs text-(--muted-foreground)">À venir</p>
        </div>

        <div class="rounded-sm border border-(--border) bg-(--card) p-5">
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-(--muted-foreground)">Avis</span>
            <div class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted) text-emerald-600">
              <lucide-icon [img]="StarIcon" [size]="16"></lucide-icon>
            </div>
          </div>
          <div class="mt-3">
            <span class="text-2xl font-bold text-(--foreground)">{{ stats()!.totalReviews }}</span>
          </div>
          <p class="mt-1 text-xs text-(--muted-foreground)">Avis donnés</p>
        </div>
      </div>

      <!-- Two-column layout: Recent orders + Quick actions -->
      <div class="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <!-- Recent Orders -->
        <div class="lg:col-span-2">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-bold text-(--foreground)">Commandes récentes</h2>
            <a
              routerLink="/dashboard/orders"
              class="text-xs font-semibold text-(--primary) hover:underline"
            >
              Voir tout →
            </a>
          </div>
          <div class="rounded-sm border border-(--border) bg-(--card)">
            @if (stats()!.recentOrders.length === 0) {
              <div class="flex flex-col items-center justify-center py-10 text-center">
                <div class="flex h-12 w-12 items-center justify-center rounded-full bg-(--muted)">
                  <lucide-icon [img]="FileTextIcon" [size]="20" class="text-(--muted-foreground)"></lucide-icon>
                </div>
                <p class="mt-3 text-sm font-medium text-(--foreground)">Aucune commande</p>
                <p class="mt-1 text-xs text-(--muted-foreground)">Vos commandes apparaîtront ici.</p>
                <a routerLink="/services" hlmBtn variant="default" size="sm" class="mt-4 cursor-pointer">
                  Découvrir nos services
                </a>
              </div>
            } @else {
              <div class="divide-y divide-(--border)">
                @for (order of stats()!.recentOrders; track order.id) {
                  <a
                    [routerLink]="['/dashboard/orders']"
                    [queryParams]="{ open: order.id }"
                    class="flex items-center justify-between p-4 transition-colors hover:bg-(--muted)/50"
                  >
                    <div class="flex items-center gap-3">
                      <div
                        class="flex h-9 w-9 shrink-0 items-center justify-center rounded-sm"
                        [ngClass]="getStatusBgClass(order.status)"
                      >
                        <lucide-icon [img]="getStatusIcon(order.status)" [size]="16"></lucide-icon>
                      </div>
                      <div>
                        <p class="text-sm font-medium text-(--foreground)">{{ order.serviceName }}</p>
                        <p class="text-xs text-(--muted-foreground)">{{ order.createdAt | date: 'dd MMM yyyy' }}</p>
                      </div>
                    </div>
                    <div class="flex items-center gap-3">
                      <div class="text-right">
                        <p class="text-sm font-semibold text-(--foreground)">
                          {{ order.totalAmount | currency:(order.currency || 'EUR'):'symbol':'1.2-2':'fr' }}
                        </p>
                        <span
                          class="inline-block rounded-xs px-2 py-0.5 text-xs font-medium"
                          [ngClass]="getStatusBadgeClass(order.status)"
                        >
                          {{ getStatusLabel(order.status) }}
                        </span>
                      </div>
                      <lucide-icon [img]="ChevronRightIcon" [size]="16" class="text-(--muted-foreground)"></lucide-icon>
                    </div>
                  </a>
                }
              </div>
            }
          </div>
        </div>

        <!-- Quick actions -->
        <div>
          <h2 class="mb-4 text-lg font-bold text-(--foreground)">Actions rapides</h2>
          <div class="space-y-3">
            @for (action of quickActions; track action.label) {
              <a
                [routerLink]="action.route"
                class="group flex items-center gap-3 rounded-sm border border-(--border) bg-(--card) p-4 transition-colors hover:bg-(--accent)"
              >
                <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-sm bg-(--muted) text-(--primary)">
                  <lucide-icon [img]="action.icon" [size]="18"></lucide-icon>
                </div>
                <div class="flex-1">
                  <h3 class="text-sm font-semibold text-(--foreground)">{{ action.label }}</h3>
                  <p class="text-xs text-(--muted-foreground)">{{ action.description }}</p>
                </div>
                <lucide-icon
                  [img]="ChevronRightIcon" [size]="16"
                  class="text-(--muted-foreground)"
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
            <h2 class="text-lg font-bold text-(--foreground)">Prochains rendez-vous</h2>
            <a
              routerLink="/dashboard/appointments"
              class="text-xs font-semibold text-(--primary) hover:underline"
            >
              Voir tout →
            </a>
          </div>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (appt of stats()!.upcomingAppointmentsList; track appt.id) {
              <div class="rounded-sm border border-(--border) bg-(--card) p-5">
                <div class="flex items-start justify-between">
                  <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted) text-(--primary)">
                    <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
                  </div>
                  <span class="rounded-xs bg-(--muted) px-2 py-0.5 text-xs font-medium text-(--muted-foreground)">
                    {{ appt.durationMinutes }} min
                  </span>
                </div>
                <h3 class="mt-3 text-sm font-semibold text-(--foreground)">{{ appt.subject }}</h3>
                <p class="mt-1 text-xs text-(--muted-foreground)">
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
          <h2 class="mb-4 text-lg font-bold text-(--foreground)">Vos avis récents</h2>
          <div class="space-y-3">
            @for (review of stats()!.recentReviews; track review.id) {
              <div class="rounded-sm border border-(--border) bg-(--card) p-5">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-1">
                    @for (s of [1, 2, 3, 4, 5]; track s) {
                      <lucide-icon
                        [img]="StarIcon" [size]="14"
                        [ngClass]="{ 'text-amber-400': s <= review.rating, 'text-(--muted)': s > review.rating }"
                      ></lucide-icon>
                    }
                  </div>
                  @if (review.approved) {
                    <span class="rounded-xs bg-(--muted) px-2 py-0.5 text-xs font-medium text-emerald-600">Approuvé</span>
                  } @else {
                    <span class="rounded-xs bg-(--muted) px-2 py-0.5 text-xs font-medium text-amber-600">En attente</span>
                  }
                </div>
                @if (review.comment) {
                  <p class="mt-2 text-sm text-(--muted-foreground)">{{ review.comment }}</p>
                }
                <p class="mt-2 text-xs text-(--muted-foreground)">{{ review.createdAt | date: 'dd MMM yyyy' }}</p>
              </div>
            }
          </div>
        </div>
      }
    }
  `,
})
export class DashboardOverviewComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  private readonly notificationService = inject(NotificationService);

  constructor() {
    effect(() => {
      const orders = this.notificationService.liveOrderHint();
      const appts = this.notificationService.liveAppointmentHint();
      if (orders > 0 || appts > 0) {
        untracked(() => this.loadStats());
      }
    });
  }

  readonly stats = signal<DashboardStats | null>(null);
  readonly loading = signal(true);

  // Icons
  readonly ShoppingCartIcon = ShoppingCart;
  readonly CalendarIcon = Calendar;
  readonly StarIcon = Star;
  readonly ClockIcon = Clock;
  readonly CheckCircleIcon = CheckCircle;
  readonly XCircleIcon = XCircle;
  readonly AlertCircleIcon = AlertCircle;
  readonly Loader2Icon = Loader2;
  readonly RefreshCwIcon = RefreshCw;
  readonly EyeIcon = Eye;
  readonly ChevronRightIcon = ChevronRight;
  readonly FileTextIcon = FileText;

  readonly quickActions = [
    { label: 'Voir les services', description: 'Parcourir notre catalogue', route: '/services', icon: ShoppingCart },
    { label: 'Prendre rendez-vous', description: 'Planifier une consultation', route: '/contact', icon: Calendar },
    { label: 'Paramètres', description: 'Gérer votre compte', route: '/settings', icon: Settings },
  ];

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading.set(true);
    this.dashboardService.getStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.stats.set({
          totalOrders: 0, completedOrders: 0, inProgressOrders: 0,
          totalReviews: 0, upcomingAppointments: 0, totalSpent: 0,
          recentOrders: [], recentReviews: [], upcomingAppointmentsList: [],
        });
        this.loading.set(false);
      },
    });
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
      COMPLETED: 'bg-(--muted) text-(--foreground)',
      DELIVERED: 'bg-(--muted) text-(--foreground)',
      CONFIRMED: 'bg-(--muted) text-(--foreground)',
      IN_PROGRESS: 'bg-(--muted) text-(--foreground)',
      PROCESSING: 'bg-(--muted) text-(--foreground)',
      PENDING: 'bg-slate-500/10 text-slate-500',
      PAYMENT_PENDING: 'bg-orange-500/10 text-orange-500',
      CANCELLED: 'bg-red-500/10 text-red-500',
      REFUNDED: 'bg-(--muted) text-(--primary)',
    };
    return classes[status] || 'bg-slate-500/10 text-slate-500';
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
