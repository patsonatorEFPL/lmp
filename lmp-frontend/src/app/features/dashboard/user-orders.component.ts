import { Component, inject, OnInit, signal, computed, effect, untracked } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
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
  ChevronLeft,
  ChevronRight,
  FileText,
  RefreshCw,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

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

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
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
    <!-- Header -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-(--foreground)">Mes commandes</h1>
        <p class="mt-1 text-sm text-(--muted-foreground)">
          {{ allOrders().length }} commande(s) au total
        </p>
      </div>
      <button
        hlmBtn variant="ghost" size="icon" class="cursor-pointer"
        (click)="loadOrders()"
      >
        <lucide-icon
          [img]="RefreshCwIcon" [size]="18"
          [ngClass]="{ 'animate-spin': loading() }"
        ></lucide-icon>
      </button>
    </div>

    <!-- Filters -->
    <div class="mt-6 flex items-center gap-3">
      <select
        [(ngModel)]="statusFilter"
        (change)="applyFilter()"
        class="rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none cursor-pointer"
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
    </div>

    <!-- Orders list -->
    <div class="mt-6 space-y-3">
      @if (loading()) {
        <div class="flex items-center justify-center py-16">
          <lucide-icon [img]="Loader2Icon" [size]="32" class="animate-spin text-(--primary)"></lucide-icon>
        </div>
      } @else if (filteredOrders().length === 0) {
        <div class="flex flex-col items-center justify-center py-16 text-center rounded-sm border border-(--border) bg-(--card)">
          <div class="flex h-14 w-14 items-center justify-center rounded-full bg-(--muted)">
            <lucide-icon [img]="FileTextIcon" [size]="24" class="text-(--muted-foreground)"></lucide-icon>
          </div>
          <p class="mt-4 text-sm font-medium text-(--foreground)">Aucune commande trouvée</p>
          <p class="mt-1 text-xs text-(--muted-foreground)">
            @if (statusFilter) {
              Essayez un autre filtre.
            } @else {
              Vos commandes apparaîtront ici.
            }
          </p>
        </div>
      } @else {
        @for (order of paginatedOrders(); track order.id) {
          <div
            class="group flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-sm border border-(--border) bg-(--card) p-5 transition-colors hover:border-(--primary)/20 cursor-pointer"
            (click)="viewOrderDetail(order.id)"
          >
            <div class="flex items-center gap-4 flex-1 min-w-0">
              <!-- Status icon -->
              <div
                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-sm transition-transform 105"
                [ngClass]="getStatusBgClass(order.status)"
              >
                <lucide-icon [img]="getStatusIcon(order.status)" [size]="18"></lucide-icon>
              </div>

              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 flex-wrap">
                  <p class="text-sm font-semibold text-(--foreground) truncate">{{ order.serviceName }}</p>
                  <span
                    class="inline-flex rounded-xs px-2 py-0.5 text-xs font-medium shrink-0"
                    [ngClass]="getStatusBadgeClass(order.status)"
                  >
                    {{ getStatusLabel(order.status) }}
                  </span>
                </div>
                <p class="mt-1 text-xs text-(--muted-foreground)">
                  {{ order.createdAt | date: 'dd MMM yyyy à HH:mm' }}
                </p>
                <!-- Progress bar -->
                @if (order.progressPercentage > 0 && !isTerminal(order.status)) {
                  <div class="mt-2 flex items-center gap-2">
                    <div class="h-1.5 w-24 overflow-hidden rounded-full bg-(--muted)">
                      <div
                        class="h-full rounded-full transition-all"
                        [ngClass]="getProgressColor(order.progressPercentage)"
                        [style.width.%]="order.progressPercentage"
                      ></div>
                    </div>
                    <span class="text-[10px] font-medium text-(--muted-foreground)">{{ order.progressPercentage }}%</span>
                  </div>
                }
              </div>
            </div>

            <div class="flex items-center gap-4">
              <p class="text-base font-bold text-(--foreground)">
                {{ order.totalAmount | currency:(order.currency || 'EUR'):'symbol':'1.2-2':'fr' }}
              </p>
              <lucide-icon
                [img]="ChevronRightIcon" [size]="16"
                class="text-(--muted-foreground) transition-transform "
              ></lucide-icon>
            </div>
          </div>
        }

        <!-- Pagination -->
        @if (totalPages() > 1) {
          <div class="mt-4 flex items-center justify-between">
            <p class="text-xs text-(--muted-foreground)">
              Page {{ currentPage() + 1 }} sur {{ totalPages() }}
            </p>
            <div class="flex items-center gap-1">
              <button
                hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
                [disabled]="currentPage() === 0"
                (click)="currentPage.set(currentPage() - 1)"
              >
                <lucide-icon [img]="ChevronLeftIcon" [size]="16"></lucide-icon>
              </button>
              <button
                hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
                [disabled]="currentPage() >= totalPages() - 1"
                (click)="currentPage.set(currentPage() + 1)"
              >
                <lucide-icon [img]="ChevronRightIcon" [size]="16"></lucide-icon>
              </button>
            </div>
          </div>
        }
      }
    </div>

    <!-- Order Detail Modal -->
    @if (showDetail()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        (click)="closeDetail()"
      >
        <div
          class="mx-4 w-full max-w-xl max-h-[85vh] overflow-y-auto rounded-sm border border-(--border) bg-(--card) shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          @if (loadingDetail()) {
            <div class="flex items-center justify-center py-16">
              <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-(--primary)"></lucide-icon>
            </div>
          } @else if (selectedOrder()) {
            <!-- Header -->
            <div class="flex items-center justify-between border-b border-(--border) px-6 py-4">
              <div>
                <h3 class="text-lg font-bold text-(--foreground)">Détail de la commande</h3>
                <p class="text-xs text-(--muted-foreground)">{{ selectedOrder()!.serviceName }}</p>
              </div>
              <button
                hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
                (click)="closeDetail()"
              >
                <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
              </button>
            </div>

            <div class="px-6 py-5 space-y-5">
              <!-- Summary -->
              <div class="grid grid-cols-3 gap-3">
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Montant</p>
                  <p class="mt-1 text-lg font-bold text-(--foreground)">
                    {{ selectedOrder()!.totalAmount | currency:(selectedOrder()!.currency || 'EUR'):'symbol':'1.2-2':'fr' }}
                  </p>
                </div>
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Statut</p>
                  <span
                    class="mt-1 inline-flex rounded-xs px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusBadgeClass(selectedOrder()!.status)"
                  >
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

              <!-- Progress -->
              <div>
                <div class="flex items-center gap-2 mb-3">
                  <lucide-icon [img]="TrendingUpIcon" [size]="16" class="text-(--primary)"></lucide-icon>
                  <h4 class="text-sm font-semibold text-(--foreground)">Progression</h4>
                </div>
                <div class="relative mb-4">
                  <div class="h-2.5 w-full overflow-hidden rounded-full bg-(--muted)">
                    <div
                      class="h-full rounded-full transition-all duration-700"
                      [ngClass]="getProgressColor(selectedOrder()!.progressPercentage || 0)"
                      [style.width.%]="selectedOrder()!.progressPercentage || 0"
                    ></div>
                  </div>
                  <p class="mt-1 text-center text-sm font-bold" [ngClass]="getProgressTextColor(selectedOrder()!.progressPercentage || 0)">
                    {{ selectedOrder()!.progressPercentage || 0 }}%
                  </p>
                </div>
                <div class="space-y-2">
                  @for (step of orderSteps; track step.threshold) {
                    <div
                      class="flex items-center gap-3 rounded-sm px-3 py-2"
                      [ngClass]="(selectedOrder()!.progressPercentage || 0) >= step.threshold ? 'bg-(--primary)/5' : 'opacity-40'"
                    >
                      <div
                        class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-white"
                        [ngClass]="(selectedOrder()!.progressPercentage || 0) >= step.threshold ? 'bg-(--primary)' : 'bg-(--muted-foreground)'"
                      >
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

              <!-- Processing Notes -->
              @if (selectedOrder()!.processingNotes) {
                <div class="rounded-sm border border-(--border) bg-(--primary)/5 p-4">
                  <div class="flex items-center gap-2 mb-1.5">
                    <lucide-icon [img]="MessageSquareIcon" [size]="14" class="text-(--primary)"></lucide-icon>
                    <span class="text-xs font-medium text-(--primary)">Message de l'équipe</span>
                  </div>
                  <p class="text-sm text-(--foreground)">{{ selectedOrder()!.processingNotes }}</p>
                </div>
              }
            </div>

            <!-- Refunds -->
            @if (orderRefunds().length > 0) {
              <div class="px-6 pb-2">
                <div class="flex items-center gap-2 mb-3">
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
                      <span
                        class="rounded-xs px-2 py-0.5 text-xs font-medium"
                        [ngClass]="refund.status === 'succeeded' ? 'bg-green-500/10 text-green-500' :
                                   refund.status === 'pending' ? 'bg-(--muted) text-(--foreground)' :
                                   'bg-red-500/10 text-red-500'"
                      >
                        {{ refund.status === 'succeeded' ? 'Remboursé' : refund.status === 'pending' ? 'En cours' : refund.status }}
                      </span>
                    </div>
                  }
                </div>
              </div>
            }

            <!-- Footer actions -->
            <div class="border-t border-(--border) px-6 py-4 flex gap-2">
              @if (selectedOrder()!.status === 'PAYMENT_PENDING') {
                <button
                  hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
                  (click)="payOrder(selectedOrder()!)"
                >
                  <lucide-icon [img]="CreditCardIcon" [size]="14"></lucide-icon>
                  Payer maintenant
                </button>
              }
              @if (isInvoiceEligible(selectedOrder()!.status)) {
                <button
                  hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
                  (click)="downloadInvoice(selectedOrder()!.id)"
                >
                  <lucide-icon [img]="DownloadIcon" [size]="14"></lucide-icon>
                  Télécharger la facture
                </button>
              }
              <button
                hlmBtn variant="outline" size="sm" class="flex-1 cursor-pointer"
                (click)="closeDetail()"
              >
                Fermer
              </button>
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
  private readonly notificationService = inject(NotificationService);

  constructor() {
    // Auto-reload orders when a new order-related SSE notification arrives
    effect(() => {
      const hint = this.notificationService.liveOrderHint();
      // Only reload when hint changes after initial load (hint > 0)
      if (hint > 0) {
        untracked(() => this.loadOrders());
      }
    });
  }

  readonly loading = signal(true);
  readonly allOrders = signal<OrderItem[]>([]);
  readonly filteredOrders = signal<OrderItem[]>([]);
  readonly currentPage = signal(0);
  readonly pageSize = 10;

  readonly showDetail = signal(false);
  readonly loadingDetail = signal(false);
  readonly selectedOrder = signal<OrderItem | null>(null);
  readonly orderRefunds = signal<RefundItem[]>([]);
  readonly orderSteps = ORDER_STEPS;

  statusFilter = '';

  // Icons
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
  readonly ChevronLeftIcon = ChevronLeft;
  readonly ChevronRightIcon = ChevronRight;
  readonly FileTextIcon = FileText;
  readonly RefreshCwIcon = RefreshCw;

  readonly totalPages = computed(() => Math.ceil(this.filteredOrders().length / this.pageSize) || 1);

  readonly paginatedOrders = computed(() => {
    const start = this.currentPage() * this.pageSize;
    return this.filteredOrders().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.loadOrders();

    // Handle ?open=orderId deep-link
    this.route.queryParams.subscribe((params) => {
      const openId = params['open'];
      if (openId) {
        this.viewOrderDetail(openId);
      }
    });
  }

  loadOrders(): void {
    this.loading.set(true);
    this.http
      .get<ApiResponse<OrderItem[]>>(`${environment.apiUrl}/api/v1/orders`, { withCredentials: true })
      .subscribe({
        next: (res) => {
          const orders = res.data ?? [];
          this.allOrders.set(orders);
          this.applyFilter();
          this.loading.set(false);
        },
        error: () => {
          this.allOrders.set([]);
          this.filteredOrders.set([]);
          this.loading.set(false);
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

  viewOrderDetail(orderId: string): void {
    this.loadingDetail.set(true);
    this.showDetail.set(true);
    this.orderRefunds.set([]);

    this.http
      .get<ApiResponse<OrderItem>>(`${environment.apiUrl}/api/v1/orders/${orderId}`, { withCredentials: true })
      .subscribe({
        next: (res) => {
          this.selectedOrder.set(res.data ?? null);
          this.loadingDetail.set(false);

          // Load refunds
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

  payOrder(order: OrderItem): void {
    this.http
      .post<any>(`${environment.apiUrl}/api/v1/payments/checkout-order/${order.id}`, {}, { withCredentials: true })
      .subscribe({
        next: (res) => {
          if (res.data?.redirectUrl) {
            window.location.href = res.data.redirectUrl;
          }
        },
      });
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

  getProgressColor(pct: number): string {
    if (pct >= 100) return 'bg-emerald-500';
    if (pct >= 60) return 'bg-blue-500';
    if (pct >= 30) return 'bg-amber-500';
    return 'bg-orange-500';
  }

  getProgressTextColor(pct: number): string {
    if (pct >= 100) return 'text-(--foreground)';
    if (pct >= 60) return 'text-(--foreground)';
    if (pct >= 30) return 'text-(--foreground)';
    return 'text-orange-500';
  }
}
