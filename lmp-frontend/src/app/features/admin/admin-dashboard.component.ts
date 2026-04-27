import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
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
  Users,
  ShoppingCart,
  Package,
  Calendar,
  TrendingUp,
  Loader2,
  Download,
  Plus,
  Activity,
  Shield,
  FileText,
  CreditCard,
  RefreshCw,
  Filter,
  Server,
  ArrowUp,
  ArrowDown,
} from 'lucide-angular';
import { firstValueFrom } from 'rxjs';

import {
  AdminService,
  AdminDashboardStats,
  CatalogStats,
} from '../../core/services/admin.service';
import { AdminSseService } from '../../core/services/admin-sse.service';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { environment } from '../../../environments/environment';
import { StatCardComponent } from '../../shared/ui/stat-card.component';
import { QuickActionComponent } from '../../shared/ui/quick-action.component';
import { PageHeadComponent } from '../../shared/ui/page-head.component';
import { LineChartComponent } from '../../shared/ui/line-chart.component';

interface RecentUser {
  id: string;
  name: string;
  email: string;
  role: string;
  statusLabel: string;
  statusTone: string;
  ordersCount: number;
  spent: string;
  lastSeen: string;
}

interface TopService {
  name: string;
  orders: number;
  revenue: string;
  growth: number;
}

interface MonitoringRow {
  name: string;
  uptime: string;
  latency: string;
  status: string;
  tone: 'is-ok' | 'is-warn' | 'is-danger';
  /** Indices (0..29) où la barre d'uptime est dégradée. */
  warnIndices: number[];
}

interface AdminUserPageItem {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  displayName: string;
  roles: string[];
  status: string;
  accountLocked: boolean;
  registrationDate: string;
  lastLoginDate: string | null;
}

interface AdminDashboardPayload {
  stats: AdminDashboardStats | null;
  catalog: CatalogStats | null;
  users: RecentUser[];
  topServices: TopService[];
}

type RevenuePeriod = 'week' | 'month' | 'quarter';

const REVENUE_SERIES = {} as Record<RevenuePeriod, { current: number[]; previous: number[]; labels: string[] }>;

const SPARK = {};

const MONITORING_DEMO: MonitoringRow[] = [];

interface AdminUsersResponse {
  success: boolean;
  data?: {
    content: AdminUserPageItem[];
    totalElements: number;
  };
}

@Component({
  selector: 'lmp-admin-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    LucideAngularModule,
    StatCardComponent,
    QuickActionComponent,
    PageHeadComponent,
    LineChartComponent,
  ],
  template: `
    <div class="p-4 sm:p-5">
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
        <lmp-page-head
          [title]="headerTitle()"
          [subtitle]="headerSubtitle()"
        >
          <div actions>
            <button type="button" class="lmpd-btn" (click)="reload()">
              <lucide-icon [img]="RefreshIcon" [size]="13"></lucide-icon>
              Rafraîchir
            </button>
            <a routerLink="/admin/services" class="lmpd-btn">
              <lucide-icon [img]="DownloadIcon" [size]="13"></lucide-icon>
              Rapport
            </a>
            <a routerLink="/admin/services" class="lmpd-btn is-accent">
              <lucide-icon [img]="PlusIcon" [size]="13"></lucide-icon>
              Créer un service
            </a>
          </div>
        </lmp-page-head>

        <!-- 4 KPIs -->
        <div class="lmpd-stat-grid">
          <lmp-stat-card
            label="Utilisateurs"
            [value]="formatNumber(s.totalUsers)"
            [icon]="UsersIcon"
          />
          <lmp-stat-card
            label="Revenus · 30j"
            [value]="revenueDisplay()"
            [icon]="CreditCardIcon"
            [accent]="true"
          />
          <lmp-stat-card
            label="Services"
            [value]="catalogStats()?.totalServices || 0"
            [icon]="PackageIcon"
            [footer]="catalogFooter()"
          />
          <lmp-stat-card
            label="Rendez-vous"
            [value]="formatNumber(s.totalAppointments)"
            [icon]="CalendarIcon"
          />
        </div>

        <!-- Revenus 16 semaines + Accès rapides -->
        <div class="lmpd-two-col-admin">
          <section class="lmpd-panel">
            <header class="lmpd-panel-head">
              <div>
                <h3>Revenus · {{ revenueRangeLabel() }}</h3>
                <div style="font-size:11.5px;color:var(--lmpd-fg-mute);margin-top:2px">
                  Total des commandes sur la période
                </div>
              </div>
              <div class="lmpd-tabs">
                <button type="button" [class.is-on]="revenuePeriod() === 'week'" (click)="setRevenuePeriod('week')">Hebdo</button>
                <button type="button" [class.is-on]="revenuePeriod() === 'month'" (click)="setRevenuePeriod('month')">Mens.</button>
                <button type="button" [class.is-on]="revenuePeriod() === 'quarter'" (click)="setRevenuePeriod('quarter')">Trim.</button>
              </div>
            </header>
            <div class="lmpd-chart-wrap">
              <lmp-line-chart
                [data]="revenueSeries().current"
                [secondary]="revenueSeries().previous"
                [labels]="revenueSeries().labels"
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

          <div class="flex flex-col gap-4">
            <div>
              <div class="lmpd-sec-head">
                <h2>Accès rapides</h2>
              </div>
              <div class="lmpd-qa-grid">
                <lmp-quick-action
                  [icon]="PackageIcon"
                  title="Gérer les services"
                  description="Créer, modifier, supprimer"
                  route="/admin/services"
                />
                <lmp-quick-action
                  [icon]="UsersIcon"
                  title="Gérer les utilisateurs"
                  description="Comptes, rôles, verrouillage"
                  route="/admin/users"
                />
                <lmp-quick-action
                  [icon]="OrdersIcon"
                  title="Gérer les commandes"
                  description="Statuts, suivi, détails"
                  route="/admin/orders"
                />
                <lmp-quick-action
                  [icon]="CalendarIcon"
                  title="Gérer les rendez-vous"
                  description="Consulter, confirmer, annuler"
                  route="/admin/appointments"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Utilisateurs récents + Top services -->
        <div class="lmpd-two-col-admin">
          <section class="lmpd-panel">
            <header class="lmpd-panel-head">
              <h3>Utilisateurs récents</h3>
              <div style="display:flex; gap:8px">
                <button type="button" class="lmpd-btn" style="padding:4px 9px;font-size:11.5px">
                  <lucide-icon [img]="FilterIcon" [size]="12"></lucide-icon>
                  Filtrer
                </button>
                <a routerLink="/admin/users" class="lmpd-btn" style="padding:4px 9px;font-size:11.5px">
                  Voir tous →
                </a>
              </div>
            </header>
            @if (recentUsers().length === 0) {
              <div class="flex flex-col items-center justify-center py-10 text-center px-6">
                <div class="flex h-12 w-12 items-center justify-center rounded-full bg-zinc-100 dark:bg-zinc-800">
                  <lucide-icon
                    [img]="UsersIcon"
                    [size]="20"
                    class="text-zinc-500 dark:text-zinc-400"
                  ></lucide-icon>
                </div>
                <p class="mt-3 text-sm font-medium text-zinc-900 dark:text-zinc-100">
                  Aucun utilisateur
                </p>
                <p class="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
                  Les nouveaux comptes s'afficheront ici.
                </p>
              </div>
            } @else {
              <table class="lmpd-table">
                <thead>
                  <tr>
                    <th>Utilisateur</th>
                    <th>Rôle</th>
                    <th>Statut</th>
                    <th class="lmpd-num text-right">Commandes</th>
                    <th class="lmpd-num text-right">Dépensé</th>
                    <th class="lmpd-num text-right">Dernier</th>
                  </tr>
                </thead>
                <tbody>
                  @for (u of recentUsers(); track u.id) {
                    <tr>
                      <td>
                        <div class="lmpd-user-cell">
                          <span
                            class="flex items-center justify-center rounded-full text-white font-semibold"
                            [style.width.px]="26"
                            [style.height.px]="26"
                            [style.fontSize.px]="10.5"
                            [style.background]="avatarBg(u.name)"
                          >{{ initials(u.name) }}</span>
                          <div>
                            <div class="lmpd-nm">{{ u.name }}</div>
                            <div class="lmpd-em">{{ u.email }}</div>
                          </div>
                        </div>
                      </td>
                      <td><span class="lmpd-badge is-muted">{{ u.role }}</span></td>
                      <td><span [class]="'lmpd-badge ' + u.statusTone">{{ u.statusLabel }}</span></td>
                      <td class="lmpd-num text-right">{{ u.ordersCount }}</td>
                      <td class="lmpd-num text-right font-medium">{{ u.spent }}</td>
                      <td class="lmpd-num text-right" style="color:var(--lmpd-fg-mute)">{{ u.lastSeen }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </section>

          <div class="flex flex-col gap-4">
            <section class="lmpd-panel">
              <header class="lmpd-panel-head">
                <h3>Top services</h3>
                <a routerLink="/admin/services" class="lmpd-link">Tout →</a>
              </header>
              @if (topServices().length === 0) {
                <div class="px-4 py-6 text-xs text-(--lmpd-fg-mute)">
                  Aucun service au catalogue.
                </div>
              } @else {
                @for (svc of topServices(); track svc.name; let last = $last) {
                  <div
                    [style.padding]="'12px 16px'"
                    [style.borderBottom]="last ? 'none' : '1px solid var(--lmpd-border)'"
                    style="display:grid;grid-template-columns:1fr auto;gap:8px;align-items:center"
                  >
                    <div>
                      <div style="font-weight:500;font-size:13px;color:var(--lmpd-fg)">{{ svc.name }}</div>
                      <div style="font-size:11px;color:var(--lmpd-fg-mute);margin-top:2px">
                        {{ svc.orders }} commandes · {{ svc.revenue }}
                      </div>
                    </div>
                    <span [class]="'lmpd-delta ' + (svc.growth >= 0 ? 'is-up' : 'is-down')">
                      <lucide-icon [img]="svc.growth >= 0 ? ArrowUpIcon : ArrowDownIcon" [size]="10"></lucide-icon>
                      {{ absoluteValue(svc.growth) }}%
                    </span>
                  </div>
                }
              }
            </section>
          </div>
        </div>

        @if (monitoring.length > 0) {
        <section class="lmpd-panel">
          <header class="lmpd-panel-head">
            <h3>
              Santé des services ·
              <span class="lmpd-mono" style="color:var(--lmpd-fg-mute);font-size:11px">uptime 30 jours</span>
            </h3>
            <span class="lmpd-badge is-ok">{{ monitoringOkCount() }} OK</span>
          </header>
          <div>
            @for (m of monitoring; track m.name) {
              <div class="lmpd-svc-row">
                <div class="lmpd-svc-name">
                  <span
                    [style.width.px]="7"
                    [style.height.px]="7"
                    [style.borderRadius]="'50%'"
                    [style.background]="dotColor(m.tone)"
                    [style.boxShadow]="dotHalo(m.tone)"
                  ></span>
                  {{ m.name }}
                </div>
                <div class="lmpd-svc-uptime-bar">
                  @for (i of uptimeRange; track $index) {
                    <span [class.is-w]="m.warnIndices.includes(i)"></span>
                  }
                </div>
                <div class="lmpd-mono" style="color:var(--lmpd-fg-mute);min-width:60px;text-align:right">
                  {{ m.latency }}
                </div>
                <div><span [class]="'lmpd-badge ' + m.tone">{{ m.status }}</span></div>
              </div>
            }
          </div>
        </section>
        }

        <!-- Bandeau temps réel -->
        <div class="lmpd-insight">
          <span class="lmpd-insight-ic">
            <lucide-icon [img]="TrendingUpIcon" [size]="14"></lucide-icon>
          </span>
          <div class="lmpd-insight-tx">
            <b>Temps réel actif · </b>
            Les commandes, utilisateurs et rendez-vous se mettent à jour automatiquement.
          </div>
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
  private readonly http = inject(HttpClient);

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
        return { stats: null, catalog: null, users: [], topServices: [] };
      }
      const [stats, catalog, users, topServices] = await Promise.all([
        this.fetchStats(),
        this.fetchCatalog(),
        this.fetchRecentUsers(),
        this.fetchTopServices(),
      ]);
      return { stats, catalog, users, topServices };
    },
  });

  private async fetchStats(): Promise<AdminDashboardStats | null> {
    try {
      return await firstValueFrom(this.adminService.getDashboardStats());
    } catch {
      return null;
    }
  }

  private async fetchCatalog(): Promise<CatalogStats | null> {
    try {
      return await firstValueFrom(this.adminService.getCatalogStats());
    } catch {
      return null;
    }
  }

  private async fetchRecentUsers(): Promise<RecentUser[]> {
    try {
      const url = `${environment.apiUrl}/api/v1/admin/users`;
      const res = await firstValueFrom(
        this.http.get<AdminUsersResponse>(url, {
          params: { page: '0', size: '6' },
          withCredentials: true,
        }),
      );
      const content = res?.data?.content ?? [];
      return content.map((u) => this.toRecentUser(u));
    } catch {
      return [];
    }
  }

  private async fetchTopServices(): Promise<TopService[]> {
    return [];
  }

  private toRecentUser(u: AdminUserPageItem): RecentUser {
    const name = u.displayName?.trim() || `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || u.email;
    const role = (u.roles?.[0] ?? 'CLIENT').replace('ROLE_', '');
    let statusLabel = 'Actif';
    let statusTone: 'is-ok' | 'is-warn' | 'is-danger' = 'is-ok';
    if (u.accountLocked) {
      statusLabel = 'Verrouillé';
      statusTone = 'is-danger';
    } else if ((u.status ?? '').toUpperCase() === 'PENDING') {
      statusLabel = 'En attente';
      statusTone = 'is-warn';
    } else if ((u.status ?? '').toUpperCase() === 'INACTIVE') {
      statusLabel = 'Inactif';
      statusTone = 'is-warn';
    }
    return {
      id: u.id,
      name,
      email: u.email,
      role: role === 'ADMIN' ? 'Admin' : role === 'PRO' ? 'Pro' : 'Client',
      statusLabel,
      statusTone,
      ordersCount: 0,
      spent: '—',
      lastSeen: this.formatRelative(u.lastLoginDate ?? u.registrationDate),
    };
  }

  readonly stats = computed(() => this.dashboardResource.value()?.stats ?? null);
  readonly catalogStats = computed(() => this.dashboardResource.value()?.catalog ?? null);
  readonly recentUsers = computed(() => this.dashboardResource.value()?.users ?? []);
  readonly topServices = computed(() => this.dashboardResource.value()?.topServices ?? []);

  readonly blockingLoader = computed(
    () => this.dashboardResource.status() === 'loading' && !this.dashboardResource.hasValue(),
  );

  readonly headerTitle = computed(() => {
    const now = new Date();
    const month = now.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
    return `Aperçu · ${month}`;
  });

  readonly headerSubtitle = computed(() => 'Pilotage global de la plateforme.');

  readonly completedPercent = computed(() => {
    const s = this.stats();
    if (!s || !s.totalOrders) return 0;
    const completed =
      (s.ordersByStatus?.['COMPLETED']?.count ?? 0) +
      (s.ordersByStatus?.['DELIVERED']?.count ?? 0);
    return Math.round((completed / s.totalOrders) * 100);
  });

  /** Affichage des revenus 30j — agrégés depuis ordersByStatus si disponible. */
  readonly revenueDisplay = computed(() => {
    const s = this.stats();
    if (!s?.ordersByStatus) return '0 ' + this.currencySymbol();
    const total = Object.values(s.ordersByStatus).reduce(
      (sum, v) => sum + (v?.revenue ?? 0),
      0,
    );
    return `${this.formatNumber(Math.round(total))} ${this.currencySymbol()}`;
  });

  readonly catalogFooter = computed(() => {
    const c = this.catalogStats();
    if (!c) return 'Catalogue non disponible';
    return `${c.activeServices} actifs · ${c.featuredServices} en vedette`;
  });

  readonly revenuePeriod = signal<RevenuePeriod>('week');
  setRevenuePeriod(p: RevenuePeriod) {
    this.revenuePeriod.set(p);
  }
  readonly revenueSeries = computed(() => REVENUE_SERIES[this.revenuePeriod()] ?? { current: [], previous: [], labels: [] });
  readonly revenueRangeLabel = computed(() => {
    switch (this.revenuePeriod()) {
      case 'week':
        return '16 dernières semaines';
      case 'month':
        return '12 derniers mois';
      case 'quarter':
        return '4 derniers trimestres';
    }
  });

  readonly monitoring = MONITORING_DEMO;
  readonly uptimeRange = Array.from({ length: 30 }, (_, i) => i);
  readonly monitoringOkCount = computed(
    () => this.monitoring.filter((m) => m.tone === 'is-ok').length,
  );

  readonly UsersIcon = Users;
  readonly OrdersIcon = ShoppingCart;
  readonly PackageIcon = Package;
  readonly CalendarIcon = Calendar;
  readonly TrendingUpIcon = TrendingUp;
  readonly Loader2Icon = Loader2;
  readonly DownloadIcon = Download;
  readonly PlusIcon = Plus;
  readonly ActivityIcon = Activity;
  readonly ShieldIcon = Shield;
  readonly FileTextIcon = FileText;
  readonly CreditCardIcon = CreditCard;
  readonly RefreshIcon = RefreshCw;
  readonly FilterIcon = Filter;
  readonly ServerIcon = Server;
  readonly ArrowUpIcon = ArrowUp;
  readonly ArrowDownIcon = ArrowDown;

  readonly sparkUsers = undefined;
  readonly sparkRevenue = undefined;
  readonly sparkServices = undefined;
  readonly sparkAppointments = undefined;

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.visiblePoll.subscribeWhileVisible(
        this.destroyRef,
        environment.dashboardPollIntervalMs,
        () => this.dashboardResource.reload(),
      );
    }
  }

  reload(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.dashboardResource.reload();
    }
  }

  formatNumber(v: number): string {
    return new Intl.NumberFormat('fr-FR').format(v);
  }

  currencySymbol(code: string = environment.defaultCurrency): string {
    const map: Record<string, string> = { EUR: '€', USD: '$', GBP: '£', CAD: '$' };
    return map[code] ?? code;
  }

  absoluteValue(v: number): number {
    return Math.abs(v);
  }

  initials(name: string): string {
    if (!name) return '·';
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('');
  }

  /** Couleur d'avatar dérivée du nom (palette stable par hash). */
  avatarBg(name: string): string {
    let h = 0;
    for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0;
    const hues = [10, 30, 60, 150, 200, 230, 260, 290, 320, 350];
    const hue = hues[h % hues.length];
    return `linear-gradient(135deg, oklch(0.72 0.15 ${hue}), oklch(0.6 0.2 ${(hue + 40) % 360}))`;
  }

  dotColor(tone: 'is-ok' | 'is-warn' | 'is-danger'): string {
    if (tone === 'is-warn') return 'var(--lmpd-warning)';
    if (tone === 'is-danger') return 'var(--lmpd-danger)';
    return 'var(--lmpd-success)';
  }

  dotHalo(tone: 'is-ok' | 'is-warn' | 'is-danger'): string {
    if (tone === 'is-warn') return '0 0 0 3px oklch(0.78 0.16 80 / 0.2)';
    if (tone === 'is-danger') return '0 0 0 3px oklch(0.64 0.22 25 / 0.2)';
    return '0 0 0 3px oklch(0.68 0.17 150 / 0.2)';
  }

  formatRelative(iso: string | null): string {
    if (!iso) return '—';
    const d = Date.parse(iso);
    if (!d) return '—';
    const diffMs = Date.now() - d;
    if (diffMs < 0) return "à l'instant";
    const min = Math.round(diffMs / 60_000);
    if (min < 1) return "à l'instant";
    if (min < 60) return `${min} min`;
    const h = Math.round(min / 60);
    if (h < 24) return `${h} h`;
    const days = Math.round(h / 24);
    if (days === 1) return '1 j';
    if (days < 30) return `${days} j`;
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }
}
