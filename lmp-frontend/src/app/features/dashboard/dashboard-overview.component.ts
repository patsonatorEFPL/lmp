import { isPlatformBrowser, DatePipe, CurrencyPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
  PLATFORM_ID,
  computed,
  effect,
  resource,
  signal,
  untracked,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  ShoppingCart,
  Calendar,
  Settings,
  Star,
  Clock,
  Loader2,
  ChevronRight,
  FileText,
  Box,
  CreditCard,
  Plus,
  LifeBuoy,
  Sparkles,
  CheckCircle2,
  CalendarCheck,
  Download,
  X,
} from 'lucide-angular';
import type { LucideIconData } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import {
  DashboardService,
  DashboardStats,
} from '../../core/services/dashboard.service';
import { NotificationService } from '../../core/services/notification.service';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';
import { StatCardComponent } from '../../shared/ui/stat-card.component';
import { QuickActionComponent } from '../../shared/ui/quick-action.component';
import { PanelComponent } from '../../shared/ui/panel.component';
import { PageHeadComponent } from '../../shared/ui/page-head.component';
import { LineChartComponent } from '../../shared/ui/line-chart.component';
import { DonutChartComponent, DonutSegment } from '../../shared/ui/donut-chart.component';
import { ActivityFeedComponent, ActivityFeedItem } from '../../shared/ui/activity-feed.component';

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

type StatusTone = 'is-ok' | 'is-warn' | 'is-info' | 'is-pending' | 'is-danger' | 'is-muted';
type Period = '7j' | '30j' | '90j' | '12m';
type OrderFilter = 'all' | 'active' | 'done';

const PERIODS: Period[] = ['7j', '30j', '90j', '12m'];
const ORDER_FILTERS: { key: OrderFilter; label: string }[] = [
  { key: 'all', label: 'Toutes' },
  { key: 'active', label: 'En cours' },
  { key: 'done', label: 'Livrées' },
];

/**
 * Séries de démo pour le graphique d'activité (données back non disponibles
 * pour les courbes temps-réel). Conçues pour reproduire fidèlement la maquette.
 */
const ACTIVITY_SERIES: Record<Period, { current: number[]; previous: number[]; labels: string[] }> = {
  '7j': {
    current: [3, 5, 4, 7, 6, 8, 9],
    previous: [2, 3, 4, 5, 4, 6, 7],
    labels: ['L', 'M', 'M', 'J', 'V', 'S', 'D'],
  },
  '30j': {
    current: [14, 18, 22, 19, 28, 32, 30, 38, 42, 40, 48, 52],
    previous: [12, 15, 18, 17, 22, 25, 27, 30, 33, 35, 38, 41],
    labels: ['Jan', '', 'Mar', '', 'Mai', '', 'Juil', '', 'Sep', '', 'Nov', ''],
  },
  '90j': {
    current: [22, 28, 32, 35, 40, 44, 48, 52, 56, 60, 65, 70],
    previous: [18, 22, 26, 28, 32, 35, 38, 42, 46, 50, 54, 58],
    labels: ['S1', '', 'S3', '', 'S5', '', 'S7', '', 'S9', '', 'S11', ''],
  },
  '12m': {
    current: [40, 48, 55, 60, 68, 72, 80, 88, 92, 100, 108, 116],
    previous: [32, 38, 44, 50, 56, 62, 68, 74, 80, 86, 92, 98],
    labels: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc'],
  },
};

/** Sparklines fixes pour les stat cards — purement décoratives. */
const SPARK = {
  orders: [3, 4, 3, 5, 7, 6, 8, 9, 8, 11, 10, 12, 14],
  inProgress: [1, 2, 2, 3, 2, 3, 4, 3, 2, 3, 3, 3, 3],
  appointments: [0, 1, 0, 1, 2, 2, 1, 2, 2, 1, 2, 2, 2],
  reviews: [1, 1, 2, 2, 3, 4, 5, 5, 6, 6, 7, 7, 8],
};

@Component({
  selector: 'lmp-dashboard-overview',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    LucideAngularModule,
    DatePipe,
    CurrencyPipe,
    StatCardComponent,
    QuickActionComponent,
    PanelComponent,
    PageHeadComponent,
    LineChartComponent,
    DonutChartComponent,
    ActivityFeedComponent,
  ],
  template: `
    @if (blockingLoader()) {
      <div class="flex items-center justify-center py-16">
        <lucide-icon
          [img]="Loader2Icon"
          [size]="32"
          class="animate-spin text-zinc-600 dark:text-zinc-400"
        ></lucide-icon>
      </div>
    }

    @if (!blockingLoader() && stats(); as s) {
      <div class="lmpd-page">
        <!-- Salutation personnalisée + actions principales -->
        <lmp-page-head
          [title]="greeting()"
          [subtitle]="headerSubtitle()"
        >
          <div actions>
            <a routerLink="/dashboard/invoices" class="lmpd-btn">
              <lucide-icon [img]="DownloadIcon" [size]="13"></lucide-icon>
              Exporter
            </a>
            <a routerLink="/services" class="lmpd-btn is-accent">
              <lucide-icon [img]="PlusIcon" [size]="13"></lucide-icon>
              Nouvelle commande
            </a>
          </div>
        </lmp-page-head>

        <!-- Bandeau « Recommandation IA » -->
        @if (showInsight()) {
          <div class="lmpd-insight">
            <span class="lmpd-insight-ic">
              <lucide-icon [img]="SparklesIcon" [size]="14"></lucide-icon>
            </span>
            <div class="lmpd-insight-tx">
              <b>{{ insightTitle() }} · </b>{{ insightBody() }}
            </div>
            <a routerLink="/services" class="lmpd-btn">Explorer</a>
            <button
              type="button"
              class="lmpd-btn"
              style="padding:6px 8px"
              (click)="dismissInsight()"
              aria-label="Fermer"
            >
              <lucide-icon [img]="CloseIcon" [size]="13"></lucide-icon>
            </button>
          </div>
        }

        <!-- 4 KPIs avec sparklines (séries de démo : pas de courbes côté back) -->
        <div class="lmpd-stat-grid">
          <lmp-stat-card
            label="Commandes"
            [value]="s.totalOrders"
            [icon]="ShoppingCartIcon"
            [delta]="22"
            [footer]="s.completedOrders + ' terminée(s)'"
            [spark]="sparkOrders"
          />
          <lmp-stat-card
            label="En cours"
            [value]="s.inProgressOrders"
            [icon]="ClockIcon"
            footer="Commandes actives"
            [accent]="true"
            [spark]="sparkInProgress"
          />
          <lmp-stat-card
            label="Rendez-vous"
            [value]="s.upcomingAppointments"
            [icon]="CalendarIcon"
            footer="À venir · 7 jours"
            [spark]="sparkAppointments"
          />
          <lmp-stat-card
            label="Avis donnés"
            [value]="s.totalReviews"
            [icon]="StarIcon"
            [delta]="12"
            footer="Merci pour vos retours"
            [spark]="sparkReviews"
          />
        </div>

        <!-- Activity chart + Quick actions (même grille que la maquette) -->
        <div class="lmpd-two-col">
          <section class="lmpd-panel">
            <header class="lmpd-panel-head">
              <div>
                <h3>Activité de vos commandes</h3>
                <div style="font-size:11.5px;color:var(--lmpd-fg-mute);margin-top:2px">
                  Évolution · trafic &amp; conversions
                </div>
              </div>
              <div class="lmpd-tabs">
                @for (p of periods; track p) {
                  <button
                    type="button"
                    [class.is-on]="period() === p"
                    (click)="setPeriod(p)"
                  >{{ p }}</button>
                }
              </div>
            </header>
            <div class="lmpd-chart-wrap">
              <lmp-line-chart
                [data]="series().current"
                [secondary]="series().previous"
                [labels]="series().labels"
              />
              <div style="display:flex;gap:20px;font-size:11.5px;color:var(--lmpd-fg-mute);padding:8px 4px 0">
                <span style="display:flex;align-items:center;gap:6px">
                  <span style="width:8px;height:8px;border-radius:2px;background:var(--lmpd-accent)"></span>
                  Cette période
                </span>
                <span style="display:flex;align-items:center;gap:6px">
                  <span style="width:10px;height:2px;background:var(--lmpd-fg-mute);opacity:0.5"></span>
                  Période précédente
                </span>
              </div>
            </div>
          </section>

          <div>
            <div class="lmpd-sec-head">
              <h2>Actions rapides</h2>
            </div>
            <div class="lmpd-qa-grid">
              @for (action of quickActions; track action.label) {
                <lmp-quick-action
                  [icon]="action.icon"
                  [title]="action.label"
                  [description]="action.description"
                  [route]="action.route"
                />
              }
            </div>
          </div>
        </div>

        <!-- Orders + (Appointments + Donut) -->
        <div class="lmpd-two-col">
          <section class="lmpd-panel">
            <header class="lmpd-panel-head">
              <h3>Commandes récentes</h3>
              <div class="lmpd-pill-filter">
                @for (f of orderFilters; track f.key) {
                  <button
                    type="button"
                    [class.is-on]="orderFilter() === f.key"
                    (click)="setOrderFilter(f.key)"
                  >{{ f.label }}</button>
                }
              </div>
            </header>
            @if (filteredOrders().length === 0) {
              <div class="flex flex-col items-center justify-center py-10 text-center px-6">
                <div class="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-100 dark:bg-zinc-800">
                  <lucide-icon
                    [img]="FileTextIcon"
                    [size]="20"
                    class="text-zinc-500 dark:text-zinc-400"
                  ></lucide-icon>
                </div>
                <p class="mt-3 text-sm font-medium text-zinc-900 dark:text-zinc-100">
                  Aucune commande
                </p>
                <p class="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
                  Vos commandes apparaîtront ici.
                </p>
                <a routerLink="/services" class="lmpd-btn is-accent mt-4">
                  Découvrir nos services
                </a>
              </div>
            } @else {
              <table class="lmpd-table">
                <thead>
                  <tr>
                    <th>Référence</th>
                    <th>Service</th>
                    <th>Statut</th>
                    <th>Avancement</th>
                    <th class="text-right">Montant</th>
                  </tr>
                </thead>
                <tbody>
                  @for (order of filteredOrders(); track order.id) {
                    <tr
                      class="cursor-pointer"
                      [routerLink]="['/dashboard/orders']"
                      [queryParams]="{ open: order.id }"
                    >
                      <td class="lmpd-mono">#{{ shortId(order.id) }}</td>
                      <td>
                        <div class="font-medium">{{ order.serviceName }}</div>
                        <div class="text-[11px] text-(--lmpd-fg-mute)">
                          {{ order.createdAt | date: 'dd MMM yyyy' }}
                        </div>
                      </td>
                      <td>
                        <span [class]="'lmpd-badge ' + statusTone(order.status)">
                          {{ statusLabel(order.status) }}
                        </span>
                      </td>
                      <td style="min-width:140px">
                        <div style="display:flex;align-items:center;gap:8px">
                          <div class="lmpd-bar" style="flex:1">
                            <span [style.width.%]="progressFor(order.status)"></span>
                          </div>
                          <span class="lmpd-num" style="font-size:11px;color:var(--lmpd-fg-mute)">
                            {{ progressFor(order.status) }}%
                          </span>
                        </div>
                      </td>
                      <td class="lmpd-num text-right font-medium">
                        {{ order.totalAmount | currency: (order.currency || 'EUR'):'symbol':'1.2-2':'fr' }}
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </section>

          <div class="flex flex-col gap-4">
            <lmp-panel title="À venir">
              @if (s.upcomingAppointmentsList.length > 0) {
                @for (appt of s.upcomingAppointmentsList; track appt.id) {
                  <div class="lmpd-appt">
                    <div class="lmpd-appt-date">
                      <span class="lmpd-d">{{ dayOf(appt.appointmentDate) }}</span>
                      <span class="lmpd-m">{{ monthOf(appt.appointmentDate) }}</span>
                    </div>
                    <div>
                      <div class="lmpd-appt-tt">{{ appt.subject }}</div>
                      <div class="lmpd-appt-sub">
                        {{ appt.appointmentDate | date: 'HH:mm' }}
                        · {{ appt.durationMinutes }} min
                      </div>
                    </div>
                  </div>
                }
              } @else {
                <div class="flex flex-col items-center justify-center py-8 text-center px-6">
                  <p class="text-xs text-(--lmpd-fg-mute)">Aucun rendez-vous à venir</p>
                </div>
              }
            </lmp-panel>
          </div>
        </div>


      </div>
    }
  `,
})
export class DashboardOverviewComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly notificationService = inject(NotificationService);
  private readonly authService = inject(AuthService);
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

  readonly greeting = computed(() => {
    const u = this.authService.user();
    const name = u?.firstName?.trim();
    return name ? `Bonjour, ${name} 👋` : 'Bonjour 👋';
  });

  readonly headerSubtitle = computed(() => {
    const s = this.stats();
    if (!s) return 'Voici un résumé de votre activité.';
    const parts: string[] = [];
    if (s.inProgressOrders > 0) {
      parts.push(`${s.inProgressOrders} projet${s.inProgressOrders > 1 ? 's' : ''} avance${s.inProgressOrders > 1 ? 'nt' : ''}`);
    }
    if (s.upcomingAppointments > 0) {
      parts.push(`${s.upcomingAppointments} rendez-vous à venir`);
    }
    return parts.length
      ? `Voici un résumé de votre activité. ${parts.join(', ')}.`
      : 'Voici un résumé de votre activité.';
  });

  /** Bandeau d'astuce — s'inspire de la « Recommandation IA » de la maquette. */
  readonly insightVisible = signal(true);
  readonly showInsight = computed(() => this.insightVisible());
  dismissInsight() {
    this.insightVisible.set(false);
  }

  readonly insightTitle = computed(() => 'Recommandation IA');

  readonly insightBody = computed(
    () =>
      'Vos campagnes Google Ads performent 23 % au-dessus du secteur. Un budget +15 % pourrait générer ~41 leads/mois.',
  );

  /** Période sélectionnée pour la courbe d'activité (UI seulement, séries mock). */
  readonly period = signal<Period>('30j');
  setPeriod(p: Period) {
    this.period.set(p);
  }
  readonly periods = PERIODS;
  readonly series = computed(() => ACTIVITY_SERIES[this.period()]);

  /** Filtre rapide sur le tableau des commandes. */
  readonly orderFilter = signal<OrderFilter>('all');
  setOrderFilter(f: OrderFilter) {
    this.orderFilter.set(f);
  }
  readonly orderFilters = ORDER_FILTERS;

  readonly filteredOrders = computed(() => {
    const orders = this.stats()?.recentOrders ?? [];
    const filter = this.orderFilter();
    if (filter === 'all') return orders;
    if (filter === 'done') {
      return orders.filter((o) => o.status === 'COMPLETED' || o.status === 'DELIVERED');
    }
    return orders.filter(
      (o) => o.status !== 'COMPLETED' && o.status !== 'DELIVERED' && o.status !== 'CANCELLED' && o.status !== 'REFUNDED',
    );
  });

  /** Donut « Répartition » — agrégation des commandes récentes par statut. */
  readonly categorySegments = computed<DonutSegment[]>(() => {
    const orders = this.stats()?.recentOrders ?? [];
    if (orders.length === 0) {
      // Fallback visuel pour rendre le panneau parlant même sans commandes.
      return [
        { label: 'Aucune commande', value: 1, color: 'var(--lmpd-fg-faint)' },
      ];
    }
    const counts = new Map<string, number>();
    for (const o of orders) {
      counts.set(o.status, (counts.get(o.status) ?? 0) + 1);
    }
    return Array.from(counts.entries())
      .map(([status, value]) => ({
        label: this.statusLabel(status),
        value,
        color: this.statusColor(status),
      }))
      .sort((a, b) => b.value - a.value);
  });

  readonly activityItems = computed<ActivityFeedItem[]>(() => {
    const s = this.stats();
    if (!s) return [];
    const items: (ActivityFeedItem & { ts: number })[] = [];

    for (const o of s.recentOrders.slice(0, 5)) {
      items.push({
        icon: this.statusIcon(o.status),
        title: o.serviceName,
        detail: this.statusLabel(o.status),
        meta: this.formatRelative(o.createdAt),
        ts: Date.parse(o.createdAt) || 0,
      });
    }
    for (const a of s.upcomingAppointmentsList.slice(0, 3)) {
      items.push({
        icon: this.CalendarCheckIcon,
        title: a.subject,
        detail: `Rendez-vous · ${a.durationMinutes} min`,
        meta: this.formatRelative(a.appointmentDate),
        ts: Date.parse(a.appointmentDate) || 0,
      });
    }
    for (const r of s.recentReviews.slice(0, 3)) {
      items.push({
        icon: this.StarIcon,
        title: `Avis · ${r.rating}/5`,
        detail: r.comment || (r.approved ? 'Avis approuvé' : 'Avis en attente'),
        meta: this.formatRelative(r.createdAt),
        ts: Date.parse(r.createdAt) || 0,
      });
    }

    return items
      .sort((a, b) => b.ts - a.ts)
      .slice(0, 6)
      .map(({ ts: _ts, ...rest }) => rest);
  });

  readonly ShoppingCartIcon = ShoppingCart;
  readonly CalendarIcon = Calendar;
  readonly StarIcon = Star;
  readonly ClockIcon = Clock;
  readonly Loader2Icon = Loader2;
  readonly ChevronRightIcon = ChevronRight;
  readonly FileTextIcon = FileText;
  readonly BoxIcon = Box;
  readonly PlusIcon = Plus;
  readonly SparklesIcon = Sparkles;
  readonly CheckCircleIcon = CheckCircle2;
  readonly CalendarCheckIcon = CalendarCheck;
  readonly DownloadIcon = Download;
  readonly CloseIcon = X;

  readonly sparkOrders = SPARK.orders;
  readonly sparkInProgress = SPARK.inProgress;
  readonly sparkAppointments = SPARK.appointments;
  readonly sparkReviews = SPARK.reviews;

  readonly quickActions = [
    {
      label: 'Voir les services',
      description: 'Parcourir notre catalogue',
      route: '/services',
      icon: Box,
    },
    {
      label: 'Prendre rendez-vous',
      description: 'Planifier une consultation',
      route: '/contact',
      icon: Calendar,
    },
    {
      label: 'Ouvrir un ticket',
      description: 'Obtenir de l’aide rapidement',
      route: '/dashboard/tickets',
      icon: LifeBuoy,
    },
    {
      label: 'Régler une facture',
      description: '2 factures en attente',
      route: '/dashboard/invoices',
      icon: CreditCard,
    },
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

  shortId(raw: string): string {
    return raw.length > 8 ? raw.slice(0, 8) : raw;
  }

  dayOf(iso: string): string {
    const d = new Date(iso);
    return isNaN(d.valueOf()) ? '--' : String(d.getDate()).padStart(2, '0');
  }

  monthOf(iso: string): string {
    const d = new Date(iso);
    if (isNaN(d.valueOf())) return '';
    return d
      .toLocaleDateString('fr-FR', { month: 'short' })
      .replace('.', '')
      .toUpperCase();
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PAYMENT_PENDING: 'Paiement en attente',
      PENDING: 'En attente',
      CONFIRMED: 'Confirmée',
      PROCESSING: 'En traitement',
      IN_PROGRESS: 'En cours',
      SHIPPED: 'Expédiée',
      DELIVERED: 'Livrée',
      COMPLETED: 'Terminée',
      UNDER_REVIEW: 'En révision',
      CANCELLED: 'Annulée',
      REFUNDED: 'Remboursée',
    };
    return labels[status] || status;
  }

  /** Mappe le statut métier vers un ton de badge du design system (lmpd-badge). */
  statusTone(status: string): StatusTone {
    switch (status) {
      case 'COMPLETED':
      case 'DELIVERED':
        return 'is-ok';
      case 'CONFIRMED':
      case 'IN_PROGRESS':
      case 'PROCESSING':
      case 'SHIPPED':
        return 'is-info';
      case 'UNDER_REVIEW':
        return 'is-warn';
      case 'PENDING':
      case 'PAYMENT_PENDING':
        return 'is-pending';
      case 'CANCELLED':
      case 'REFUNDED':
        return 'is-danger';
      default:
        return 'is-muted';
    }
  }

  /** Couleur de segment donut pour un statut de commande. */
  statusColor(status: string): string {
    switch (status) {
      case 'COMPLETED':
      case 'DELIVERED':
        return 'var(--lmpd-success)';
      case 'IN_PROGRESS':
      case 'PROCESSING':
      case 'SHIPPED':
      case 'CONFIRMED':
        return 'var(--lmpd-info)';
      case 'UNDER_REVIEW':
        return 'var(--lmpd-warning)';
      case 'PENDING':
      case 'PAYMENT_PENDING':
        return 'var(--lmpd-accent)';
      case 'CANCELLED':
      case 'REFUNDED':
        return 'var(--lmpd-danger)';
      default:
        return 'var(--lmpd-fg-faint)';
    }
  }

  statusIcon(status: string): LucideIconData {
    switch (status) {
      case 'COMPLETED':
      case 'DELIVERED':
        return CheckCircle2;
      case 'CONFIRMED':
      case 'IN_PROGRESS':
      case 'PROCESSING':
      case 'SHIPPED':
        return Clock;
      case 'CANCELLED':
      case 'REFUNDED':
        return FileText;
      default:
        return ShoppingCart;
    }
  }

  /**
   * Avancement (%) dérivé du statut — la maquette montre des barres de
   * progression mais le back ne renvoie pas de pourcentage par commande.
   */
  progressFor(status: string): number {
    switch (status) {
      case 'COMPLETED':
      case 'DELIVERED':
        return 100;
      case 'SHIPPED':
        return 90;
      case 'UNDER_REVIEW':
        return 80;
      case 'IN_PROGRESS':
      case 'PROCESSING':
        return 60;
      case 'CONFIRMED':
        return 35;
      case 'PENDING':
      case 'PAYMENT_PENDING':
        return 10;
      case 'CANCELLED':
      case 'REFUNDED':
        return 0;
      default:
        return 25;
    }
  }

  /** Date relative compacte (« il y a 12 min », « hier », etc.). */
  formatRelative(iso: string): string {
    const d = Date.parse(iso);
    if (!d) return '';
    const diffMs = Date.now() - d;
    if (diffMs < 0) {
      const days = Math.round(-diffMs / 86_400_000);
      if (days < 1) return "aujourd'hui";
      if (days === 1) return 'demain';
      return `dans ${days} j`;
    }
    const min = Math.round(diffMs / 60_000);
    if (min < 1) return "à l'instant";
    if (min < 60) return `il y a ${min} min`;
    const h = Math.round(min / 60);
    if (h < 24) return `il y a ${h} h`;
    const days = Math.round(h / 24);
    if (days === 1) return 'hier';
    if (days < 30) return `il y a ${days} j`;
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }
}
