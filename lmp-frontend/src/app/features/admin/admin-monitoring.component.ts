import { isPlatformBrowser } from '@angular/common';
import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  PLATFORM_ID,
  computed,
  resource,
  signal,
} from '@angular/core';
import {
  LucideAngularModule,
  Activity,
  CheckCircle,
  AlertTriangle,
  XCircle,
  Clock,
  Zap,
  Globe,
  Shield,
  CreditCard,
  Mail,
  DollarSign,
  Database,
  Loader2,
  PlayCircle,
} from 'lucide-angular';
import {
  ApiHealthService,
  ApiHealthSnapshot,
  ApiHealthEntry,
  ApiStatus,
} from '../../core/services/api-health.service';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { environment } from '../../../environments/environment';
import { firstValueFrom } from 'rxjs';

/** Map API name → category icon */
const API_ICONS: Record<string, typeof Activity> = {
  'ip-api.com': Globe,
  'GetIPIntel': Shield,
  'IPHub': Shield,
  'ipwho.is': Globe,
  'VIES': Globe,
  'Stripe': CreditCard,
  'Mailtrap': Mail,
  'FX Rates': DollarSign,
};

@Component({
  selector: 'lmp-admin-monitoring',
  standalone: true,
  imports: [LucideAngularModule],
  template: `
    <div class="p-4 sm:p-5">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2.5">
          <div
            class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)"
          >
            <lucide-icon
              [img]="ActivityIcon"
              [size]="18"
              class="text-(--foreground)"
            ></lucide-icon>
          </div>
          <div>
            <h1 class="text-base font-semibold text-(--foreground)">
              Monitoring API
            </h1>
            @if (snapshot()) {
              <p class="text-xs text-(--muted-foreground)">
                Mis à jour : {{ formatTimestamp(snapshot()!.timestamp) }}
              </p>
            }
          </div>
        </div>
        <button
          type="button"
          class="inline-flex items-center gap-1.5 rounded border border-zinc-200 bg-white px-3 py-1.5 text-xs font-medium text-(--foreground) shadow-xs transition-colors hover:bg-zinc-50 disabled:pointer-events-none disabled:opacity-50 dark:border-zinc-700 dark:bg-zinc-800 dark:hover:bg-zinc-700"
          [disabled]="probing()"
          (click)="onProbeAll()"
        >
          <lucide-icon
            [img]="PlayCircleIcon"
            [size]="14"
            [class.animate-spin]="probing()"
          ></lucide-icon>
          {{ probing() ? 'Test en cours…' : 'Tester les APIs' }}
        </button>
      </div>

      @if (blockingLoader()) {
        <div class="flex items-center justify-center py-16">
          <lucide-icon
            [img]="Loader2Icon"
            [size]="32"
            class="animate-spin text-(--primary)"
          ></lucide-icon>
        </div>
      }

      @if (!blockingLoader() && snapshot()) {
        <!-- Summary pills -->
        <div class="mt-5 flex flex-wrap gap-3">
          <div
            class="flex items-center gap-2 rounded-full border border-green-200 bg-green-50 px-3.5 py-1.5 dark:border-green-900/50 dark:bg-green-950/30"
          >
            <span class="h-2 w-2 rounded-full bg-green-500"></span>
            <span class="text-xs font-medium text-green-700 dark:text-green-400"
              >{{ upCount() }} Opérationnel{{ upCount() > 1 ? 's' : '' }}</span
            >
          </div>
          @if (degradedCount() > 0) {
            <div
              class="flex items-center gap-2 rounded-full border border-amber-200 bg-amber-50 px-3.5 py-1.5 dark:border-amber-900/50 dark:bg-amber-950/30"
            >
              <span class="h-2 w-2 rounded-full bg-amber-500"></span>
              <span
                class="text-xs font-medium text-amber-700 dark:text-amber-400"
                >{{ degradedCount() }} Dégradé{{ degradedCount() > 1 ? 's' : '' }}</span
              >
            </div>
          }
          @if (downCount() > 0) {
            <div
              class="flex items-center gap-2 rounded-full border border-red-200 bg-red-50 px-3.5 py-1.5 dark:border-red-900/50 dark:bg-red-950/30"
            >
              <span class="h-2 w-2 rounded-full bg-red-500"></span>
              <span class="text-xs font-medium text-red-700 dark:text-red-400"
                >{{ downCount() }} Hors service</span
              >
            </div>
          }
          @if (unknownCount() > 0) {
            <div
              class="flex items-center gap-2 rounded-full border border-zinc-200 bg-zinc-50 px-3.5 py-1.5 dark:border-zinc-700 dark:bg-zinc-800/30"
            >
              <span class="h-2 w-2 rounded-full bg-zinc-400"></span>
              <span
                class="text-xs font-medium text-zinc-600 dark:text-zinc-400"
                >{{ unknownCount() }} Aucune donnée</span
              >
            </div>
          }
        </div>

        <!-- API Cards Grid -->
        <div
          class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3"
        >
          @for (api of snapshot()!.apis; track api.name) {
            <div
              class="rounded border border-zinc-200/90 bg-white p-5 shadow-sm dark:border-zinc-800 dark:bg-zinc-900/50"
            >
              <!-- Card header: icon + name + status dot -->
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2.5">
                  <div
                    class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted)"
                  >
                    <lucide-icon
                      [img]="getApiIcon(api.name)"
                      [size]="15"
                      class="text-(--foreground)"
                    ></lucide-icon>
                  </div>
                  <span
                    class="text-sm font-semibold text-(--foreground)"
                    >{{ api.name }}</span
                  >
                </div>
                <span
                  class="h-2.5 w-2.5 rounded-full"
                  [class]="statusDotClass(api.status)"
                  [title]="statusLabel(api.status)"
                ></span>
              </div>

              <!-- Metrics -->
              <div class="mt-4 grid grid-cols-3 gap-3">
                <div>
                  <p
                    class="text-[10px] font-medium tracking-wider text-(--muted-foreground) uppercase"
                  >
                    Latence
                  </p>
                  <p class="mt-0.5 text-sm font-semibold text-(--foreground)">
                    {{ api.totalCalls > 0 ? api.avgLatencyMs + ' ms' : '—' }}
                  </p>
                </div>
                <div>
                  <p
                    class="text-[10px] font-medium tracking-wider text-(--muted-foreground) uppercase"
                  >
                    Succès
                  </p>
                  <p
                    class="mt-0.5 text-sm font-semibold"
                    [class]="successRateClass(api.successRate, api.totalCalls)"
                  >
                    {{
                      api.totalCalls > 0
                        ? api.successRate + '%'
                        : '—'
                    }}
                  </p>
                </div>
                <div>
                  <p
                    class="text-[10px] font-medium tracking-wider text-(--muted-foreground) uppercase"
                  >
                    Appels
                  </p>
                  <p class="mt-0.5 text-sm font-semibold text-(--foreground)">
                    {{ api.totalCalls }}
                  </p>
                </div>
              </div>

              <!-- Footer: last call + last error -->
              <div
                class="mt-3.5 border-t border-zinc-100 pt-3 dark:border-zinc-800"
              >
                <div class="flex items-center gap-1.5 text-xs text-(--muted-foreground)">
                  <lucide-icon
                    [img]="ClockIcon"
                    [size]="12"
                    class="shrink-0"
                  ></lucide-icon>
                  <span>{{ api.lastCallAt ? timeAgo(api.lastCallAt) : 'Jamais' }}</span>
                </div>
                @if (api.lastError) {
                  <p
                    class="mt-1.5 truncate text-xs text-red-500 dark:text-red-400"
                    [title]="api.lastError"
                  >
                    {{ api.lastError }}
                  </p>
                }
              </div>
            </div>
          }
        </div>

        <!-- Infrastructure -->
        <div class="mt-8">
          <h2
            class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400"
          >
            Infrastructure
          </h2>
          <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <!-- Database -->
            <div
              class="flex items-center gap-3.5 rounded border border-zinc-200/90 bg-white p-4 shadow-sm dark:border-zinc-800 dark:bg-zinc-900/50"
            >
              <div
                class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)"
              >
                <lucide-icon
                  [img]="DatabaseIcon"
                  [size]="16"
                  class="text-(--foreground)"
                ></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-semibold text-(--foreground)">
                  Base de données
                </p>
                <p class="mt-0.5 text-xs" [class]="infraStatusClass(snapshot()!.infra.db)">
                  {{ infraStatusLabel(snapshot()!.infra.db) }}
                </p>
              </div>
              <span
                class="ml-auto h-2.5 w-2.5 rounded-full"
                [class]="statusDotClass(normalizeInfraStatus(snapshot()!.infra.db))"
              ></span>
            </div>

            <!-- Disk -->
            <div
              class="flex items-center gap-3.5 rounded border border-zinc-200/90 bg-white p-4 shadow-sm dark:border-zinc-800 dark:bg-zinc-900/50"
            >
              <div
                class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)"
              >
                <lucide-icon
                  [img]="ZapIcon"
                  [size]="16"
                  class="text-(--foreground)"
                ></lucide-icon>
              </div>
              <div class="min-w-0 flex-1">
                <p class="text-sm font-semibold text-(--foreground)">
                  Espace disque
                </p>
                @if (snapshot()!.infra.diskTotal) {
                  <p class="mt-0.5 text-xs text-(--muted-foreground)">
                    {{ formatBytes(snapshot()!.infra.diskFree!) }} libre sur {{ formatBytes(snapshot()!.infra.diskTotal!) }}
                  </p>
                  <div class="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-zinc-100 dark:bg-zinc-800">
                    <div
                      class="h-full rounded-full transition-all"
                      [class]="diskBarClass(snapshot()!.infra.diskUsagePercent!)"
                      [style.width.%]="snapshot()!.infra.diskUsagePercent"
                    ></div>
                  </div>
                  <p class="mt-1 text-[10px] text-(--muted-foreground)">
                    {{ snapshot()!.infra.diskUsagePercent }}% utilisé
                  </p>
                } @else {
                  <p class="mt-0.5 text-xs" [class]="infraStatusClass(snapshot()!.infra.diskSpace)">
                    {{ infraStatusLabel(snapshot()!.infra.diskSpace) }}
                  </p>
                }
              </div>
              <span
                class="ml-auto h-2.5 w-2.5 shrink-0 rounded-full"
                [class]="statusDotClass(normalizeInfraStatus(snapshot()!.infra.diskSpace))"
              ></span>
            </div>
          </div>
        </div>
      }
    </div>
  `,
})
export class AdminMonitoringComponent implements OnInit {
  private readonly apiHealthService = inject(ApiHealthService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroyRef = inject(DestroyRef);
  private readonly visiblePoll = inject(VisiblePollService);

  // Icons
  readonly ActivityIcon = Activity;
  readonly CheckCircleIcon = CheckCircle;
  readonly AlertTriangleIcon = AlertTriangle;
  readonly XCircleIcon = XCircle;
  readonly ClockIcon = Clock;
  readonly ZapIcon = Zap;
  readonly DatabaseIcon = Database;
  readonly Loader2Icon = Loader2;
  readonly PlayCircleIcon = PlayCircle;

  readonly healthResource = resource<ApiHealthSnapshot | null, { browser: boolean }>({
    params: () => ({ browser: isPlatformBrowser(this.platformId) }),
    loader: async ({ params }) => {
      if (!params.browser) return null;
      try {
        const snap = await firstValueFrom(this.apiHealthService.getHealthSnapshot());
        this.nowMs.set(Date.now());
        return snap;
      } catch {
        return null;
      }
    },
  });

  readonly snapshot = computed(() => this.healthResource.value() ?? null);

  /**
   * Timestamp capturé à chaque chargement du snapshot.
   * Évite NG0100 : timeAgo() utilise ce signal stable au lieu de Date.now().
   */
  private readonly nowMs = signal(Date.now());

  readonly blockingLoader = computed(
    () => this.healthResource.status() === 'loading' && !this.healthResource.hasValue(),
  );

  readonly probing = signal(false);

  readonly upCount = computed(
    () => this.snapshot()?.apis.filter((a) => a.status === 'UP').length ?? 0,
  );
  readonly degradedCount = computed(
    () => this.snapshot()?.apis.filter((a) => a.status === 'DEGRADED').length ?? 0,
  );
  readonly downCount = computed(
    () => this.snapshot()?.apis.filter((a) => a.status === 'DOWN').length ?? 0,
  );
  readonly unknownCount = computed(
    () => this.snapshot()?.apis.filter((a) => a.status === 'UNKNOWN').length ?? 0,
  );

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.visiblePoll.subscribeWhileVisible(
        this.destroyRef,
        environment.dashboardPollIntervalMs,
        () => this.healthResource.reload(),
      );
    }
  }

  onProbeAll(): void {
    this.probing.set(true);
    this.apiHealthService.probeAll().subscribe({
      next: () => {
        // Reload snapshot after probe populates data
        this.healthResource.reload();
        this.probing.set(false);
      },
      error: () => this.probing.set(false),
    });
  }

  getApiIcon(apiName: string) {
    return API_ICONS[apiName] ?? Activity;
  }

  statusDotClass(status: ApiStatus | string): string {
    switch (status) {
      case 'UP':
        return 'bg-green-500';
      case 'DEGRADED':
        return 'bg-amber-500';
      case 'DOWN':
        return 'bg-red-500';
      default:
        return 'bg-zinc-400';
    }
  }

  statusLabel(status: ApiStatus): string {
    switch (status) {
      case 'UP':
        return 'Opérationnel';
      case 'DEGRADED':
        return 'Dégradé';
      case 'DOWN':
        return 'Hors service';
      default:
        return 'Aucune donnée';
    }
  }

  successRateClass(rate: number, totalCalls: number): string {
    if (totalCalls === 0) return 'text-(--muted-foreground)';
    if (rate >= 90) return 'text-green-600 dark:text-green-400';
    if (rate >= 50) return 'text-amber-600 dark:text-amber-400';
    return 'text-red-600 dark:text-red-400';
  }

  normalizeInfraStatus(actuatorStatus: string): ApiStatus {
    if (actuatorStatus === 'UP') return 'UP';
    if (actuatorStatus === 'UNKNOWN') return 'UNKNOWN';
    return 'DOWN';
  }

  infraStatusLabel(status: string): string {
    switch (status) {
      case 'UP':
        return 'Opérationnel';
      case 'DOWN':
        return 'Hors service';
      default:
        return 'Inconnu';
    }
  }

  infraStatusClass(status: string): string {
    switch (status) {
      case 'UP':
        return 'text-green-600 dark:text-green-400';
      case 'DOWN':
        return 'text-red-600 dark:text-red-400';
      default:
        return 'text-(--muted-foreground)';
    }
  }

  formatTimestamp(iso: string): string {
    try {
      return new Date(iso).toLocaleTimeString('fr-FR', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } catch {
      return iso;
    }
  }

  timeAgo(iso: string): string {
    const diff = this.nowMs() - new Date(iso).getTime();
    if (diff < 0) return 'à l\'instant';
    const seconds = Math.floor(diff / 1000);
    if (seconds < 60) return `il y a ${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `il y a ${minutes} min`;
    const hours = Math.floor(minutes / 60);
    return `il y a ${hours}h`;
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 o';
    const units = ['o', 'Ko', 'Mo', 'Go', 'To'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    const value = bytes / Math.pow(1024, i);
    return `${value < 10 ? value.toFixed(1) : Math.round(value)} ${units[i]}`;
  }

  diskBarClass(usagePercent: number): string {
    if (usagePercent >= 90) return 'bg-red-500';
    if (usagePercent >= 75) return 'bg-amber-500';
    return 'bg-green-500';
  }
}
