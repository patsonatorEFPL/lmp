import { Component, DestroyRef, inject, OnInit, signal, computed, effect, untracked } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DatePipe, CurrencyPipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../../core/services/notification.service';
import {
  LucideAngularModule,
  ShoppingCart,
  Loader2,
  Eye,
  X,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  TrendingUp,
  MessageSquare,
  Download,
  RotateCcw,
  CreditCard,
  ChevronRight,
  FileText,
  RefreshCw,
  Filter,
  ArrowUpDown,
  Check,
  AlertTriangle,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { paymentApiUrls } from '../../core/api/payment-api.paths';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { createListFetchLoading } from '../../core/utils/list-fetch-loading';
import { PaymentSessionService } from '../../core/services/payment-session.service';
import { formatRelativeTimeFr } from '../../core/utils/relative-time';
import { ApiResponse } from '../../shared/models/api.models';

interface OrderItem {
  id: string;
  serviceName: string;
  totalAmount: number;
  currency: string;
  status: string;
  paymentStatus: string;
  createdAt: string;
  progressPercentage: number;
  progressStatus: string | null;
  processingNotes: string | null;
}

interface RefundItem {
  id: string;
  amount: number;
  currency: string;
  status: string;
  reason: string | null;
  createdAt: string;
  processedAt: string | null;
}

const ORDER_STEPS = [
  { label: 'Commande reçue', threshold: 0 },
  { label: 'Paiement confirmé', threshold: 10 },
  { label: 'En traitement', threshold: 30 },
  { label: 'En cours', threshold: 50 },
  { label: 'Livraison', threshold: 80 },
  { label: 'Terminée', threshold: 100 },
];

@Component({
  selector: 'lmp-user-orders',
  standalone: true,
  imports: [DatePipe, CurrencyPipe, NgClass, FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="flex h-full flex-col overflow-hidden">
      <!-- Toolbar (aligned on admin CRM style) -->
      <div class="flex items-center justify-between gap-2 pb-4">
        <div></div>
        <div class="flex items-center gap-0.5">
          <button
            hlmBtn variant="ghost" size="icon" type="button"
            class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
            title="Actualiser"
            (click)="loadOrders()"
          >
            <lucide-icon [img]="RefreshCwIcon" [size]="15" [ngClass]="{ 'animate-spin': loading() }"></lucide-icon>
          </button>
          <button
            hlmBtn variant="ghost" size="sm" type="button"
            class="h-7 cursor-pointer gap-1.5 px-2"
            [ngClass]="showFilterPanel() ? 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200' : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200'"
            (click)="showFilterPanel.set(!showFilterPanel())"
          >
            <lucide-icon [img]="FilterIcon" [size]="14"></lucide-icon>
            <span class="text-sm">Filtre</span>
          </button>
          <div class="relative">
            <button
              hlmBtn variant="ghost" size="sm" type="button"
              class="h-7 cursor-pointer gap-1.5 px-2"
              [ngClass]="showSortMenu() ? 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200' : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200'"
              (click)="showSortMenu.set(!showSortMenu())"
            >
              <lucide-icon [img]="ArrowUpDownIcon" [size]="14"></lucide-icon>
              <span class="text-sm">Sort</span>
            </button>
            @if (showSortMenu()) {
              <div class="absolute right-0 top-full z-50 mt-1 w-48 rounded-lg border border-zinc-200 bg-white py-1 shadow-lg dark:border-zinc-700 dark:bg-zinc-900">
                @for (opt of sortOptions; track opt.key) {
                  <button
                    type="button"
                    class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm transition-colors hover:bg-zinc-50 dark:hover:bg-zinc-800"
                    [ngClass]="currentSort() === opt.key ? 'text-zinc-900 font-medium dark:text-zinc-100' : 'text-zinc-600 dark:text-zinc-400'"
                    (click)="applySort(opt.key)"
                  >
                    @if (currentSort() === opt.key) {
                      <lucide-icon [img]="CheckIcon" [size]="14" class="text-zinc-900 dark:text-zinc-100"></lucide-icon>
                    } @else {
                      <span class="w-3.5"></span>
                    }
                    {{ opt.label }}
                    @if (currentSort() === opt.key) {
                      <span class="ml-auto text-xs text-zinc-400">{{ sortDirection() === 'asc' ? '↑' : '↓' }}</span>
                    }
                  </button>
                }
              </div>
            }
          </div>
        </div>
      </div>

      <!-- Filter panel -->
      @if (showFilterPanel()) {
        <div class="flex items-center gap-2 border-b border-zinc-100 pb-3 dark:border-zinc-800">
          <select
            [(ngModel)]="statusFilter"
            (change)="applyFilter()"
            class="h-8 w-44 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
          >
            <option value="">Tous les statuts</option>
            <option value="PAYMENT_PENDING">Paiement en attente</option>
            <option value="PENDING">En attente</option>
            <option value="CONFIRMED">Confirmées</option>
            <option value="PROCESSING">En traitement</option>
            <option value="IN_PROGRESS">En cours</option>
            <option value="COMPLETED">Terminées</option>
            <option value="CANCELLED">Annulées</option>
            <option value="REFUNDED">Remboursées</option>
          </select>
          @if (statusFilter) {
            <button
              type="button"
              class="flex h-8 cursor-pointer items-center gap-1 rounded-lg px-2 text-sm text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800"
              (click)="statusFilter = ''; applyFilter()"
            >
              <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
              Effacer
            </button>
          }
        </div>
      }

      <!-- CRM-style list -->
      <div class="flex-1 overflow-auto">
        @if (loading()) {
          <div class="flex items-center justify-center py-16">
            <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-zinc-400"></lucide-icon>
          </div>
        } @else {
          <!-- Column header (admin-style) -->
          <div class="mb-2 flex min-w-max items-center rounded-lg bg-zinc-100 py-1.5 text-sm font-normal leading-none text-zinc-500 dark:bg-zinc-800/70 dark:text-zinc-400">
            <div class="w-32 shrink-0 px-2">Commande</div>
            <div class="w-64 shrink-0 px-2">Service</div>
            <div class="w-28 shrink-0 px-2 text-center">Montant</div>
            <div class="w-32 shrink-0 px-2 text-center">Statut</div>
            <div class="w-36 shrink-0 px-2 text-center">Date</div>
            <div class="w-40 shrink-0 px-2 text-center">Progression</div>
            <div class="w-32 shrink-0 px-2 text-center">Last Modified</div>
          </div>

          <!-- Rows -->
          <div>
            @for (order of paginatedOrders(); track order.id) {
              <div
                class="group flex h-10 min-w-max cursor-pointer items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
                (click)="viewOrderDetail(order.id)"
              >
                <div class="w-32 shrink-0 truncate px-2 font-mono text-xs leading-normal text-zinc-700 dark:text-zinc-300">
                  {{ shortId(order.id) }}
                </div>
                <div class="w-64 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">
                  {{ order.serviceName }}
                </div>
                <div class="w-28 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ order.totalAmount | currency:(order.currency || 'EUR'):'symbol':'1.2-2':'fr' }}
                </div>
                <div class="w-32 shrink-0 px-2 text-center">
                  <span
                    class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusClass(order.status)"
                  >
                    {{ getStatusLabel(order.status) }}
                  </span>
                </div>
                <div class="w-36 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ order.createdAt | date:'dd/MM/yyyy HH:mm' }}
                </div>
                <div class="w-40 shrink-0 px-2 text-center">
                  @if (order.progressPercentage > 0 && !isTerminal(order.status)) {
                    <div class="flex items-center justify-center gap-2">
                      <div class="h-1.5 w-20 overflow-hidden rounded-full bg-zinc-200 dark:bg-zinc-800">
                        <div class="h-full rounded-full transition-all"
                          [ngClass]="getProgressColor(order.progressPercentage)"
                          [style.width.%]="order.progressPercentage"></div>
                      </div>
                      <span class="text-xs font-medium text-zinc-600 dark:text-zinc-400">{{ order.progressPercentage }}%</span>
                    </div>
                  } @else {
                    <span class="text-xs text-zinc-400">—</span>
                  }
                </div>
                <div class="w-32 shrink-0 px-2 text-center text-sm leading-normal text-zinc-500 dark:text-zinc-400">
                  {{ formatRelativeTimeFr(order.createdAt) }}
                </div>
              </div>
            } @empty {
              <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
                @if (statusFilter) { Aucune commande ne correspond à ce filtre. }
                @else { Aucune commande trouvée. Vos commandes apparaîtront ici. }
              </div>
            }
          </div>
        }
      </div>

      <!-- Footer pagination (admin-style) -->
      <div class="flex items-center justify-between border-t border-zinc-200 py-2 dark:border-zinc-800">
        <div class="inline-flex rounded-md border border-zinc-200 dark:border-zinc-700">
          @for (size of pageSizes; track size; let first = $first; let last = $last) {
            <button
              type="button"
              class="h-7 min-w-[2.25rem] px-2.5 text-sm font-normal transition-colors"
              [ngClass]="{
                'rounded-l-md': first,
                'rounded-r-md': last,
                'border-r border-zinc-200 dark:border-zinc-700': !last,
                'bg-zinc-100 text-zinc-900 dark:bg-zinc-800 dark:text-zinc-100': pageSize() === size,
                'bg-white text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900 dark:bg-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200': pageSize() !== size
              }"
              (click)="changePageSize(size)"
            >
              {{ size }}
            </button>
          }
        </div>
        <div class="flex items-center gap-1 text-sm text-zinc-500 dark:text-zinc-400">
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ filteredOrders().length }}</span>
          <span>of</span>
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ allOrders().length }}</span>
        </div>
      </div>
    </div>

    <!-- Order Detail Modal -->
    @if (showDetail()) {
      <div
        class="fixed inset-0 z-[100] flex items-end justify-center bg-black/50 p-0 backdrop-blur-sm sm:items-center sm:p-4"
        (click)="closeDetail()"
        role="dialog"
        aria-modal="true"
      >
        <div
          class="flex max-h-[min(92dvh,760px)] w-full max-w-xl flex-col rounded-t-lg border border-(--border) bg-(--card) shadow-2xl sm:rounded-lg"
          (click)="$event.stopPropagation()"
        >
          @if (loadingDetail()) {
            <div class="flex items-center justify-center py-16">
              <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-(--primary)"></lucide-icon>
            </div>
          } @else if (selectedOrder()) {
            <div class="shrink-0 border-b border-(--border) px-5 py-4 sm:px-6">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h3 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">Détail de la commande</h3>
                  <p class="font-mono text-xs text-(--muted-foreground)">{{ selectedOrder()!.id }}</p>
                </div>
                <button hlmBtn variant="ghost" size="icon" class="h-9 w-9 shrink-0 cursor-pointer"
                  type="button" (click)="closeDetail()" aria-label="Fermer">
                  <lucide-icon [img]="XIcon" [size]="18"></lucide-icon>
                </button>
              </div>
            </div>

            <div class="min-h-0 flex-1 space-y-5 overflow-y-auto overscroll-contain px-5 py-5 sm:px-6">
              <div class="grid grid-cols-3 gap-3">
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Montant</p>
                  <p class="mt-1 text-lg font-bold text-(--foreground)">
                    {{ selectedOrder()!.totalAmount | currency:(selectedOrder()!.currency || 'EUR'):'symbol':'1.2-2':'fr' }}
                  </p>
                </div>
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Statut</p>
                  <span class="mt-1 inline-flex rounded-xs px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusClass(selectedOrder()!.status)">
                    {{ getStatusLabel(selectedOrder()!.status) }}
                  </span>
                </div>
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Date</p>
                  <p class="mt-1 text-sm text-(--foreground)">
                    {{ selectedOrder()!.createdAt | date:'dd/MM/yyyy' }}
                  </p>
                </div>
              </div>

              <div>
                <div class="mb-3 flex items-center gap-2">
                  <lucide-icon [img]="TrendingUpIcon" [size]="16" class="text-(--primary)"></lucide-icon>
                  <h4 class="text-sm font-semibold text-(--foreground)">Progression</h4>
                </div>
                <div class="relative mb-4">
                  <div class="h-2.5 w-full overflow-hidden rounded-full bg-(--muted)">
                    <div class="h-full rounded-full transition-all duration-700"
                      [ngClass]="getProgressColor(selectedOrder()!.progressPercentage || 0)"
                      [style.width.%]="selectedOrder()!.progressPercentage || 0"></div>
                  </div>
                  <p class="mt-1 text-center text-sm font-bold text-(--foreground)">
                    {{ selectedOrder()!.progressPercentage || 0 }}%
                  </p>
                </div>
                <div class="space-y-2">
                  @for (step of orderSteps; track step.threshold) {
                    <div class="flex items-center gap-3 rounded-sm px-3 py-2"
                      [ngClass]="(selectedOrder()!.progressPercentage || 0) >= step.threshold ? 'bg-(--primary)/5' : 'opacity-40'">
                      <div class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-white"
                        [ngClass]="(selectedOrder()!.progressPercentage || 0) >= step.threshold ? 'bg-(--primary)' : 'bg-(--muted-foreground)'">
                        @if ((selectedOrder()!.progressPercentage || 0) >= step.threshold) {
                          <lucide-icon [img]="CheckCircleIcon" [size]="12"></lucide-icon>
                        } @else {
                          <lucide-icon [img]="ClockIcon" [size]="12"></lucide-icon>
                        }
                      </div>
                      <span class="text-sm text-(--foreground)">{{ step.label }}</span>
                    </div>
                  }
                </div>
              </div>

              @if (selectedOrder()!.processingNotes) {
                <div class="rounded-sm border border-(--border) bg-(--primary)/5 p-4">
                  <div class="mb-1.5 flex items-center gap-2">
                    <lucide-icon [img]="MessageSquareIcon" [size]="14" class="text-(--primary)"></lucide-icon>
                    <span class="text-xs font-medium text-(--primary)">Message de l'équipe</span>
                  </div>
                  <p class="text-sm text-(--foreground)">{{ selectedOrder()!.processingNotes }}</p>
                </div>
              }

              @if (orderRefunds().length > 0) {
                <div>
                  <div class="mb-3 flex items-center gap-2">
                    <lucide-icon [img]="RotateCcwIcon" [size]="16" class="text-(--primary)"></lucide-icon>
                    <h4 class="text-sm font-semibold text-(--foreground)">Remboursements</h4>
                  </div>
                  <div class="space-y-2">
                    @for (refund of orderRefunds(); track refund.id) {
                      <div class="flex items-center justify-between rounded-sm border border-(--border) bg-(--background) p-3">
                        <div>
                          <p class="text-sm font-medium text-(--foreground)">
                            {{ refund.amount | currency:(refund.currency || 'EUR'):'symbol':'1.2-2':'fr' }}
                          </p>
                          <p class="text-xs text-(--muted-foreground)">
                            {{ refund.createdAt | date:'dd/MM/yyyy' }}
                            @if (refund.reason) { · {{ refund.reason }} }
                          </p>
                        </div>
                        <span class="rounded-xs px-2 py-0.5 text-xs font-medium"
                          [ngClass]="refund.status === 'succeeded' ? 'bg-green-500/10 text-green-500' :
                                     refund.status === 'pending' ? 'bg-(--muted) text-(--foreground)' :
                                     'bg-red-500/10 text-red-500'">
                          {{ refund.status === 'succeeded' ? 'Remboursé' : refund.status === 'pending' ? 'En cours' : refund.status }}
                        </span>
                      </div>
                    }
                  </div>
                </div>
              }
            </div>

            <div class="shrink-0 flex flex-wrap gap-2 border-t border-(--border) bg-(--card) px-5 py-4 sm:px-6">
              @if (canStartPayment(selectedOrder()!)) {
                <button hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
                  (click)="payOrder(selectedOrder()!)">
                  <lucide-icon [img]="CreditCardIcon" [size]="14"></lucide-icon>
                  Payer maintenant
                </button>
              }
              @if (selectedOrder()!.status === 'PAYMENT_PENDING' && !canStartPayment(selectedOrder()!)) {
                <p class="max-w-[20rem] self-center text-xs text-(--muted-foreground)">
                  Paiement reçu côté banque ; confirmation de la commande en cours. Actualisez dans quelques instants.
                </p>
              }
              @if (isInvoiceEligible(selectedOrder()!.status)) {
                <button hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
                  (click)="downloadInvoice(selectedOrder()!.id)">
                  <lucide-icon [img]="DownloadIcon" [size]="14"></lucide-icon>
                  Télécharger la facture
                </button>
              }
              <button hlmBtn variant="outline" size="sm" class="flex-1 cursor-pointer"
                (click)="closeDetail()">Fermer</button>
            </div>
          }
        </div>
      </div>
    }
  `,
})
export class UserOrdersComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly paymentSession = inject(PaymentSessionService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly visiblePoll = inject(VisiblePollService);

  constructor() {
    effect(() => {
      const hint = this.notificationService.liveOrderHint();
      if (hint > 0) {
        untracked(() => this.loadOrders({ silent: true }));
      }
    });
  }

  readonly loading = signal(true);
  private readonly listFetch = createListFetchLoading(this.loading);
  readonly allOrders = signal<OrderItem[]>([]);
  readonly filteredOrders = signal<OrderItem[]>([]);
  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly pageSizes = [20, 50, 100];

  readonly showDetail = signal(false);
  readonly loadingDetail = signal(false);
  readonly selectedOrder = signal<OrderItem | null>(null);
  readonly orderRefunds = signal<RefundItem[]>([]);
  readonly orderSteps = ORDER_STEPS;

  statusFilter = '';

  readonly ShoppingCartIcon = ShoppingCart;
  readonly Loader2Icon = Loader2;
  readonly EyeIcon = Eye;
  readonly XIcon = X;
  readonly ClockIcon = Clock;
  readonly CheckCircleIcon = CheckCircle;
  readonly XCircleIcon = XCircle;
  readonly AlertCircleIcon = AlertCircle;
  readonly TrendingUpIcon = TrendingUp;
  readonly MessageSquareIcon = MessageSquare;
  readonly DownloadIcon = Download;
  readonly RotateCcwIcon = RotateCcw;
  readonly CreditCardIcon = CreditCard;
  readonly ChevronRightIcon = ChevronRight;
  readonly FileTextIcon = FileText;
  readonly RefreshCwIcon = RefreshCw;
  readonly FilterIcon = Filter;
  readonly ArrowUpDownIcon = ArrowUpDown;
  readonly CheckIcon = Check;
  readonly AlertTriangleIcon = AlertTriangle;

  readonly showFilterPanel = signal(false);
  readonly showSortMenu = signal(false);
  readonly currentSort = signal<string>('createdAt');
  readonly sortDirection = signal<'asc' | 'desc'>('desc');
  readonly sortOptions = [
    { key: 'serviceName', label: 'Service' },
    { key: 'createdAt', label: 'Date de création' },
    { key: 'totalAmount', label: 'Montant' },
    { key: 'status', label: 'Statut' },
  ];

  readonly totalPages = computed(() => Math.ceil(this.filteredOrders().length / this.pageSize()) || 1);

  readonly paginatedOrders = computed(() => {
    const start = this.currentPage() * this.pageSize();
    return this.filteredOrders().slice(start, start + this.pageSize());
  });

  ngOnInit(): void {
    this.loadOrders();
    this.visiblePoll.subscribeWhileVisible(
      this.destroyRef,
      environment.dashboardPollIntervalMs,
      () => this.loadOrders({ silent: true }),
    );

    this.route.queryParams.subscribe((params) => {
      const openId = params['open'];
      if (openId) {
        this.viewOrderDetail(openId);
      }
    });
  }

  loadOrders(options?: { silent?: boolean }): void {
    const silent = options?.silent === true;
    this.listFetch.beforeFetch(silent);
    this.http
      .get<ApiResponse<OrderItem[]>>(`${environment.apiUrl}/api/v1/orders`, { withCredentials: true })
      .subscribe({
        next: (res) => {
          const orders = res.data ?? [];
          this.allOrders.set(orders);
          this.applyFilter();
          this.listFetch.afterFetch();
        },
        error: () => {
          this.allOrders.set([]);
          this.filteredOrders.set([]);
          this.listFetch.afterFetch();
        },
      });
  }

  applyFilter(): void {
    const filtered = this.statusFilter
      ? this.allOrders().filter((o) => o.status === this.statusFilter)
      : this.allOrders();
    this.filteredOrders.set(filtered);
    this.currentPage.set(0);
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
  }

  viewOrderDetail(orderId: string): void {
    this.loadingDetail.set(true);
    this.showDetail.set(true);
    this.orderRefunds.set([]);

    this.http
      .get<ApiResponse<OrderItem>>(`${environment.apiUrl}/api/v1/orders/${orderId}`, { withCredentials: true })
      .subscribe({
        next: (res) => {
          const data = res.data ?? null;
          this.selectedOrder.set(data);
          this.loadingDetail.set(false);

          if (data?.status === 'PAYMENT_PENDING') {
            this.http
              .post<ApiResponse<unknown>>(
                paymentApiUrls.paymentReconcile(orderId),
                {},
                { withCredentials: true },
              )
              .subscribe({
                next: () => {
                  this.http
                    .get<ApiResponse<OrderItem>>(`${environment.apiUrl}/api/v1/orders/${orderId}`, { withCredentials: true })
                    .subscribe({ next: (r2) => this.selectedOrder.set(r2.data ?? null) });
                },
              });
          }

          this.http
            .get<ApiResponse<RefundItem[]>>(`${environment.apiUrl}/api/v1/orders/${orderId}/refunds`, { withCredentials: true })
            .subscribe({ next: (r) => this.orderRefunds.set(r.data ?? []) });
        },
        error: () => {
          this.loadingDetail.set(false);
          this.showDetail.set(false);
        },
      });
  }

  closeDetail(): void {
    this.showDetail.set(false);
    this.selectedOrder.set(null);
    this.orderRefunds.set([]);
  }

  canStartPayment(order: OrderItem): boolean {
    if (order.status !== 'PAYMENT_PENDING') {
      return false;
    }
    const ps = (order.paymentStatus || '').toLowerCase();
    return ps !== 'succeeded';
  }

  payOrder(order: OrderItem): void {
    void this.router.navigate(['/checkout/order', order.id]);
  }

  downloadInvoice(orderId: string): void {
    this.http
      .get(`${environment.apiUrl}/api/v1/orders/${orderId}/invoice`, { withCredentials: true, responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `Facture_${orderId}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        },
      });
  }

  isInvoiceEligible(status: string): boolean {
    return ['CONFIRMED', 'COMPLETED', 'DELIVERED', 'PROCESSING', 'IN_PROGRESS'].includes(status);
  }

  isTerminal(status: string): boolean {
    return ['COMPLETED', 'DELIVERED', 'CANCELLED', 'REFUNDED'].includes(status);
  }

  shortId(id: string): string {
    if (!id) return '';
    return id.length > 12 ? id.slice(0, 8).toUpperCase() : id.toUpperCase();
  }

  readonly formatRelativeTimeFr = formatRelativeTimeFr;

  getStatusLabel(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'Terminée';
      case 'DELIVERED': return 'Livrée';
      case 'CONFIRMED': return 'Confirmée';
      case 'PROCESSING': return 'En traitement';
      case 'IN_PROGRESS': return 'En cours';
      case 'SHIPPED': return 'Expédiée';
      case 'PENDING': return 'En attente';
      case 'PAYMENT_PENDING': return 'Paiement en attente';
      case 'CANCELLED': return 'Annulée';
      case 'REFUNDED': return 'Remboursée';
      case 'UNDER_REVIEW': return 'En révision';
      default: return status;
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'bg-green-500/10 text-green-500';
      case 'DELIVERED': return 'bg-green-500/10 text-green-500';
      case 'CONFIRMED': return 'bg-(--muted) text-(--foreground)';
      case 'PROCESSING': return 'bg-(--muted) text-(--foreground)';
      case 'IN_PROGRESS': return 'bg-(--muted) text-(--foreground)';
      case 'SHIPPED': return 'bg-indigo-500/10 text-indigo-500';
      case 'PENDING': return 'bg-yellow-500/10 text-yellow-500';
      case 'PAYMENT_PENDING': return 'bg-orange-500/10 text-orange-500';
      case 'CANCELLED': return 'bg-red-500/10 text-red-500';
      case 'REFUNDED': return 'bg-(--muted) text-(--primary)';
      default: return 'bg-gray-500/10 text-gray-400';
    }
  }

  getProgressColor(pct: number): string {
    if (pct >= 100) return 'bg-emerald-500';
    if (pct >= 60) return 'bg-blue-500';
    if (pct >= 30) return 'bg-amber-500';
    return 'bg-orange-500';
  }

  applySort(key: string): void {
    if (this.currentSort() === key) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.currentSort.set(key);
      this.sortDirection.set('asc');
    }
    this.showSortMenu.set(false);
    this.applySortToList();
  }

  private applySortToList(): void {
    const key = this.currentSort();
    const dir = this.sortDirection() === 'asc' ? 1 : -1;
    const sorted = [...this.filteredOrders()].sort((a, b) => {
      if (key === 'totalAmount') {
        return ((a.totalAmount || 0) - (b.totalAmount || 0)) * dir;
      }
      let va = '';
      let vb = '';
      switch (key) {
        case 'serviceName': va = a.serviceName || ''; vb = b.serviceName || ''; break;
        case 'createdAt': va = a.createdAt || ''; vb = b.createdAt || ''; break;
        case 'status': va = a.status || ''; vb = b.status || ''; break;
      }
      return va.localeCompare(vb, 'fr', { sensitivity: 'base' }) * dir;
    });
    this.filteredOrders.set(sorted);
  }
}
