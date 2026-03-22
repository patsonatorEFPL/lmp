import { Component, inject, OnInit, signal } from '@angular/core';
import { NgClass, DatePipe, CurrencyPipe, SlicePipe } from '@angular/common';
import {
  LucideAngularModule,
  ShoppingCart,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Loader2,
  Eye,
  X,
  Check,
  Save,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  TrendingUp,
  MessageSquare,
  FileText,
  Plus,
  Download,
  RefreshCcw,
  RotateCcw,
  CreditCard,
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface OrderItem {
  id: string;
  customerName: string;
  customerEmail: string;
  serviceName: string;
  amount: number;
  status: string;
  createdAt: string;
  paidAt: string | null;
  progressPercentage?: number;
  progressStatus?: string;
  processingNotes?: string;
}

interface OrderDetail {
  id: string;
  serviceName: string;
  totalAmount: number;
  currency: string;
  status: string;
  paymentStatus: string;
  paymentMethod: string;
  createdAt: string;
  paidAt: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  cancelledAt: string | null;
  notes: string | null;
  adminNotes: string | null;
  processingNotes: string | null;
  progressPercentage: number;
  progressStatus: string | null;
  priority: number;
  stripeSessionId: string | null;
  stripePaymentIntentId: string | null;
  cancellationReason: string | null;
  userEmail: string | null;
  userName: string | null;
  userId: string | null;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

// Order progress steps with thresholds
const ORDER_STEPS = [
  { label: 'Commande reçue', threshold: 0, status: 'PENDING' },
  { label: 'Paiement confirmé', threshold: 10, status: 'CONFIRMED' },
  { label: 'En traitement', threshold: 30, status: 'PROCESSING' },
  { label: 'En cours', threshold: 50, status: 'IN_PROGRESS' },
  { label: 'Livraison', threshold: 80, status: 'SHIPPED' },
  { label: 'Terminée', threshold: 100, status: 'COMPLETED' },
];

@Component({
  selector: 'lmp-admin-orders',
  standalone: true,
  imports: [NgClass, DatePipe, CurrencyPipe, SlicePipe, FormsModule, LucideAngularModule, HlmButton],
  template: `
    <!-- Header -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-(--foreground)">
          Gestion des Commandes
        </h1>
        <p class="mt-1 text-sm text-(--muted-foreground)">
          {{ totalOrders() }} commandes au total
        </p>
      </div>
      <div class="flex items-center gap-2">
        <button
          hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
          (click)="showCreateOrderModal.set(true)"
        >
          <lucide-icon [img]="PlusIcon" [size]="16"></lucide-icon>
          Créer une commande
        </button>
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
    </div>

    <!-- Filters -->
    <div class="mt-6 flex items-center gap-3">
      <select
        [(ngModel)]="statusFilter"
        (change)="currentPage.set(0); loadOrders()"
        class="rounded-lg border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none cursor-pointer"
      >
        <option value="">Tous les statuts</option>
        <option value="PAYMENT_PENDING">Paiement en attente</option>
        <option value="PENDING">En attente</option>
        <option value="CONFIRMED">Confirmées</option>
        <option value="PROCESSING">En traitement</option>
        <option value="IN_PROGRESS">En cours</option>
        <option value="SHIPPED">Expédiées</option>
        <option value="COMPLETED">Terminées</option>
        <option value="CANCELLED">Annulées</option>
        <option value="REFUNDED">Remboursées</option>
      </select>
    </div>

    <!-- Orders table -->
    <div class="mt-6 overflow-x-auto rounded-sm border border-(--border) bg-(--card)">
      @if (loading()) {
        <div class="flex items-center justify-center py-12">
          <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-(--muted-foreground)"></lucide-icon>
        </div>
      } @else {
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-(--border) text-left">
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">ID</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Client</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Service</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Montant</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Statut</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Progression</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Date</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (order of orders(); track order.id) {
              <tr class="border-b border-(--border) last:border-0 transition-colors hover:bg-(--accent)/50">
                <td class="px-4 py-3 font-mono text-xs text-(--muted-foreground)">
                  {{ order.id | slice:0:8 }}...
                </td>
                <td class="px-4 py-3">
                  <div>
                    <p class="font-medium text-(--foreground)">{{ order.customerName || '—' }}</p>
                    <p class="text-xs text-(--muted-foreground)">{{ order.customerEmail || '—' }}</p>
                  </div>
                </td>
                <td class="px-4 py-3 text-(--foreground)">{{ order.serviceName }}</td>
                <td class="px-4 py-3 font-semibold text-(--foreground)">
                  {{ order.amount | currency:'EUR':'symbol':'1.2-2' }}
                </td>
                <td class="px-4 py-3">
                  <span
                    class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusClass(order.status)"
                  >
                    {{ getStatusLabel(order.status) }}
                  </span>
                </td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2">
                    <div class="h-1.5 w-16 overflow-hidden rounded-full bg-(--muted)">
                      <div
                        class="h-full rounded-full transition-all"
                        [ngClass]="getProgressBarClass(order.progressPercentage || 0)"
                        [style.width.%]="order.progressPercentage || 0"
                      ></div>
                    </div>
                    <span class="text-xs text-(--muted-foreground)">{{ order.progressPercentage || 0 }}%</span>
                  </div>
                </td>
                <td class="px-4 py-3 text-(--muted-foreground)">
                  {{ order.createdAt | date:'dd/MM/yyyy HH:mm' }}
                </td>
                <td class="px-4 py-3">
                  <button
                    hlmBtn variant="ghost" size="icon"
                    class="h-8 w-8 cursor-pointer"
                    (click)="viewOrderDetail(order.id)"
                    title="Voir les détails"
                  >
                    <lucide-icon [img]="EyeIcon" [size]="16" class="text-(--primary)"></lucide-icon>
                  </button>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="8" class="px-4 py-12 text-center text-(--muted-foreground)">
                  Aucune commande trouvée
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>

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
            (click)="changePage(currentPage() - 1)"
          >
            <lucide-icon [img]="ChevronLeftIcon" [size]="16"></lucide-icon>
          </button>
          <button
            hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
            [disabled]="currentPage() >= totalPages() - 1"
            (click)="changePage(currentPage() + 1)"
          >
            <lucide-icon [img]="ChevronRightIcon" [size]="16"></lucide-icon>
          </button>
        </div>
      </div>
    }

    <!-- Order Detail Modal -->
    @if (showDetailModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        (click)="closeDetailModal()"
      >
        <div
          class="mx-4 w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl border border-(--border) bg-(--card) shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          @if (loadingDetail()) {
            <div class="flex items-center justify-center py-16">
              <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-(--primary)"></lucide-icon>
            </div>
          } @else if (orderDetail()) {
            <!-- Modal Header -->
            <div class="flex items-center justify-between border-b border-(--border) px-6 py-4">
              <div>
                <h3 class="text-lg font-bold text-(--foreground)">
                  Détail de la commande
                </h3>
                <p class="font-mono text-xs text-(--muted-foreground)">{{ orderDetail()!.id }}</p>
              </div>
              <button
                hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
                (click)="closeDetailModal()"
              >
                <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
              </button>
            </div>

            <!-- Order Info -->
            <div class="px-6 py-5 space-y-6">
              <!-- Summary Cards -->
              <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
                <div class="rounded-lg border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Montant</p>
                  <p class="mt-1 text-lg font-bold text-(--foreground)">
                    {{ orderDetail()!.totalAmount | currency:(orderDetail()!.currency || 'EUR'):'symbol':'1.2-2' }}
                  </p>
                </div>
                <div class="rounded-lg border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Statut</p>
                  <span
                    class="mt-1 inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusClass(orderDetail()!.status)"
                  >
                    {{ getStatusLabel(orderDetail()!.status) }}
                  </span>
                </div>
                <div class="rounded-lg border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Paiement</p>
                  <p class="mt-1 text-sm font-medium text-(--foreground)">
                    {{ orderDetail()!.paymentStatus || '—' }}
                  </p>
                </div>
                <div class="rounded-lg border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Date</p>
                  <p class="mt-1 text-sm text-(--foreground)">
                    {{ orderDetail()!.createdAt | date:'dd/MM/yy' }}
                  </p>
                </div>
              </div>

              <!-- Client Info -->
              <div>
                <p class="text-xs font-medium uppercase tracking-wider text-(--muted-foreground)">Client</p>
                <div class="mt-2 flex items-center gap-3 rounded-lg border border-(--border) bg-(--background) p-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-full bg-(--primary)/10 text-(--primary)">
                    <lucide-icon [img]="ShoppingCartIcon" [size]="16"></lucide-icon>
                  </div>
                  <div>
                    <p class="text-sm font-medium text-(--foreground)">{{ orderDetail()!.userName || '—' }}</p>
                    <p class="text-xs text-(--muted-foreground)">{{ orderDetail()!.userEmail || '—' }}</p>
                  </div>
                </div>
              </div>

              <div class="h-px bg-(--border)"></div>

              <!-- Progress Section -->
              <div>
                <div class="flex items-center gap-2 mb-4">
                  <lucide-icon [img]="TrendingUpIcon" [size]="18" class="text-(--primary)"></lucide-icon>
                  <h4 class="text-sm font-semibold text-(--foreground)">Progression de la commande</h4>
                </div>

                <!-- Progress Bar -->
                <div class="relative mb-6">
                  <div class="h-2 w-full overflow-hidden rounded-full bg-(--muted)">
                    <div
                      class="h-full rounded-full transition-all duration-500"
                      [ngClass]="getProgressBarClass(editProgressForm.percentage)"
                      [style.width.%]="editProgressForm.percentage"
                    ></div>
                  </div>
                  <div class="mt-1 flex items-center justify-between">
                    <span class="text-xs text-(--muted-foreground)">0%</span>
                    <span class="text-sm font-bold" [ngClass]="getProgressTextClass(editProgressForm.percentage)">
                      {{ editProgressForm.percentage }}%
                    </span>
                    <span class="text-xs text-(--muted-foreground)">100%</span>
                  </div>
                </div>

                <!-- Steps Timeline -->
                <div class="space-y-2 mb-4">
                  @for (step of orderSteps; track step.threshold) {
                    <div
                      class="flex items-center gap-3 rounded-lg px-3 py-2 transition-colors"
                      [ngClass]="editProgressForm.percentage >= step.threshold
                        ? 'bg-(--primary)/5'
                        : 'opacity-40'"
                    >
                      <div
                        class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-white transition-all"
                        [ngClass]="editProgressForm.percentage >= step.threshold
                          ? 'bg-(--primary) scale-100'
                          : 'bg-(--muted-foreground) scale-90'"
                      >
                        @if (editProgressForm.percentage >= step.threshold) {
                          <lucide-icon [img]="CheckIcon" [size]="14"></lucide-icon>
                        } @else {
                          <lucide-icon [img]="ClockIcon" [size]="14"></lucide-icon>
                        }
                      </div>
                      <span class="text-sm font-medium text-(--foreground)">{{ step.label }}</span>
                    </div>
                  }
                </div>

                <!-- Editable Fields -->
                <div class="space-y-3 rounded-lg border border-(--border) bg-(--background) p-4">
                  <div>
                    <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                      Progression (%)
                    </label>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      step="5"
                      [(ngModel)]="editProgressForm.percentage"
                      (ngModelChange)="onProgressPercentageChange($event)"
                      class="w-full accent-(--primary) cursor-pointer"
                    />
                  </div>

                  <div>
                    <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                      Statut de la commande
                    </label>
                    <select
                      [(ngModel)]="editProgressForm.status"
                      (ngModelChange)="onStatusChange($event)"
                      class="w-full rounded-lg border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) cursor-pointer"
                    >
                      <option value="PAYMENT_PENDING">Paiement en attente</option>
                      <option value="PENDING">En attente</option>
                      <option value="CONFIRMED">Confirmée</option>
                      <option value="PROCESSING">En traitement</option>
                      <option value="IN_PROGRESS">En cours</option>
                      <option value="SHIPPED">Expédiée</option>
                      <option value="DELIVERED">Livrée</option>
                      <option value="COMPLETED">Terminée</option>
                      <option value="CANCELLED">Annulée</option>
                      <option value="REFUNDED">Remboursée</option>
                    </select>
                  </div>

                  <div>
                    <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                      <lucide-icon [img]="MessageSquareIcon" [size]="12" class="inline mr-1"></lucide-icon>
                      Message de progression (visible par le client)
                    </label>
                    <textarea
                      [(ngModel)]="editProgressForm.progressMessage"
                      rows="2"
                      class="w-full rounded-lg border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) focus:ring-1 focus:ring-(--primary)"
                      placeholder="Ex: Votre projet est en cours de réalisation..."
                    ></textarea>
                  </div>

                  <div>
                    <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                      <lucide-icon [img]="FileTextIcon" [size]="12" class="inline mr-1"></lucide-icon>
                      Notes internes (admin uniquement)
                    </label>
                    <textarea
                      [(ngModel)]="editProgressForm.adminNotes"
                      rows="2"
                      class="w-full rounded-lg border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) focus:ring-1 focus:ring-(--primary)"
                      placeholder="Notes internes..."
                    ></textarea>
                  </div>
                </div>
              </div>
            </div>

            <!-- Refunds Section -->
            @if (orderRefunds().length > 0) {
              <div class="px-6 pb-4">
                <div class="flex items-center gap-2 mb-3">
                  <lucide-icon [img]="RotateCcwIcon" [size]="16" class="text-violet-500"></lucide-icon>
                  <h4 class="text-sm font-semibold text-(--foreground)">Remboursements</h4>
                </div>
                <div class="space-y-2">
                  @for (refund of orderRefunds(); track refund.id) {
                    <div class="flex items-center justify-between rounded-lg border border-(--border) bg-(--background) p-3">
                      <div>
                        <p class="text-sm font-medium text-(--foreground)">
                          {{ refund.amount | currency:(refund.currency || 'EUR'):'symbol':'1.2-2' }}
                        </p>
                        <p class="text-xs text-(--muted-foreground)">
                          {{ refund.createdAt | date:'dd/MM/yyyy HH:mm' }}
                          @if (refund.reason) { · {{ refund.reason }} }
                        </p>
                        @if (refund.stripeRefundId) {
                          <p class="text-xs font-mono text-(--muted-foreground)">{{ refund.stripeRefundId }}</p>
                        }
                      </div>
                      <span
                        class="rounded-full px-2 py-0.5 text-xs font-medium"
                        [ngClass]="refund.status === 'succeeded' ? 'bg-green-500/10 text-green-500' :
                                   refund.status === 'pending' ? 'bg-amber-500/10 text-amber-500' :
                                   refund.status === 'failed' ? 'bg-red-500/10 text-red-500' :
                                   'bg-gray-500/10 text-gray-400'"
                      >
                        {{ refund.status === 'succeeded' ? 'Remboursé' :
                           refund.status === 'pending' ? 'En cours' :
                           refund.status === 'failed' ? 'Échoué' : refund.status }}
                      </span>
                    </div>
                  }
                </div>
              </div>
            }

            <!-- Modal Footer -->
            <div class="flex items-center justify-between border-t border-(--border) px-6 py-4">
              <div class="flex items-center gap-2">
                @if (isInvoiceEligible(orderDetail()!.status)) {
                  <button
                    hlmBtn variant="outline" size="sm" class="cursor-pointer gap-2"
                    (click)="downloadInvoice(orderDetail()!.id)"
                  >
                    <lucide-icon [img]="DownloadIcon" [size]="14"></lucide-icon>
                    Facture PDF
                  </button>
                } @else {
                  <span class="text-xs text-(--muted-foreground) italic">
                    Facture disponible après confirmation
                  </span>
                }
                <button
                  hlmBtn variant="outline" size="sm" class="cursor-pointer gap-2"
                  [disabled]="syncing()"
                  (click)="syncWithStripe(orderDetail()!.id)"
                >
                  @if (syncing()) {
                    <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                  } @else {
                    <lucide-icon [img]="RefreshCcwIcon" [size]="14"></lucide-icon>
                  }
                  Sync Stripe
                </button>
              </div>
              <div class="flex items-center gap-2">
              <button
                hlmBtn variant="outline" size="sm" class="cursor-pointer"
                (click)="closeDetailModal()"
              >
                Fermer
              </button>
              <button
                hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
                [disabled]="savingProgress()"
                (click)="saveOrderProgress()"
              >
                @if (savingProgress()) {
                  <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                } @else {
                  <lucide-icon [img]="SaveIcon" [size]="14"></lucide-icon>
                }
                Enregistrer
              </button>
              </div>
            </div>
          }
        </div>
      </div>
    }

    <!-- Create Order Modal -->
    @if (showCreateOrderModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        (click)="showCreateOrderModal.set(false)"
      >
        <div
          class="mx-4 w-full max-w-md rounded-2xl border border-(--border) bg-(--card) shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          <div class="flex items-center justify-between border-b border-(--border) px-6 py-4">
            <h3 class="text-lg font-bold text-(--foreground)">
              Créer une commande
            </h3>
            <button
              hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
              (click)="showCreateOrderModal.set(false)"
            >
              <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
            </button>
          </div>

          <div class="space-y-4 px-6 py-5">
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                Email de l'utilisateur *
              </label>
              <input
                [(ngModel)]="newOrderForm.userEmail"
                type="email"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="user@example.com"
              />
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                Nom du service *
              </label>
              <input
                [(ngModel)]="newOrderForm.serviceName"
                type="text"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="Consultation SEO..."
              />
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                Montant (EUR) *
              </label>
              <input
                [(ngModel)]="newOrderForm.amount"
                type="number"
                min="0.01"
                step="0.01"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="100.00"
              />
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                Notes (optionnel)
              </label>
              <textarea
                [(ngModel)]="newOrderForm.notes"
                rows="2"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="Description..."
              ></textarea>
            </div>
          </div>

          <div class="flex items-center justify-end gap-2 border-t border-(--border) px-6 py-4">
            <button
              hlmBtn variant="outline" size="sm" class="cursor-pointer"
              (click)="showCreateOrderModal.set(false)"
            >
              Annuler
            </button>
            <button
              hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
              [disabled]="creatingOrder()"
              (click)="createOrder()"
            >
              @if (creatingOrder()) {
                <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
              } @else {
                <lucide-icon [img]="PlusIcon" [size]="14"></lucide-icon>
              }
              Créer
            </button>
          </div>
        </div>
      </div>
    }

    <!-- Toast -->
    @if (toast()) {
      <div
        class="fixed right-4 bottom-4 z-[200] flex items-center gap-2 rounded-lg border px-4 py-3 shadow-lg"
        [ngClass]="{
          'border-emerald-500/30 bg-emerald-500/10 text-emerald-500': toast()!.type === 'success',
          'border-red-500/30 bg-red-500/10 text-red-500': toast()!.type === 'error',
        }"
      >
        @if (toast()!.type === 'success') {
          <lucide-icon [img]="CheckIcon" [size]="16"></lucide-icon>
        } @else {
          <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
        }
        <span class="text-sm font-medium">{{ toast()!.message }}</span>
      </div>
    }
  `,
})
export class AdminOrdersComponent implements OnInit {
  private readonly http = inject(HttpClient);

  readonly RefreshCwIcon = RefreshCw;
  readonly ChevronLeftIcon = ChevronLeft;
  readonly ChevronRightIcon = ChevronRight;
  readonly Loader2Icon = Loader2;
  readonly EyeIcon = Eye;
  readonly XIcon = X;
  readonly CheckIcon = Check;
  readonly SaveIcon = Save;
  readonly ClockIcon = Clock;
  readonly CheckCircleIcon = CheckCircle;
  readonly XCircleIcon = XCircle;
  readonly AlertCircleIcon = AlertCircle;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly TrendingUpIcon = TrendingUp;
  readonly MessageSquareIcon = MessageSquare;
  readonly FileTextIcon = FileText;
  readonly PlusIcon = Plus;
  readonly DownloadIcon = Download;
  readonly RefreshCcwIcon = RefreshCcw;
  readonly RotateCcwIcon = RotateCcw;
  readonly CreditCardIcon = CreditCard;

  readonly loading = signal(false);
  readonly loadingDetail = signal(false);
  readonly savingProgress = signal(false);
  readonly orders = signal<OrderItem[]>([]);
  readonly totalOrders = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly showDetailModal = signal(false);
  readonly orderDetail = signal<OrderDetail | null>(null);
  readonly toast = signal<{ type: 'success' | 'error'; message: string } | null>(null);
  readonly showCreateOrderModal = signal(false);
  readonly creatingOrder = signal(false);
  readonly syncing = signal(false);
  readonly orderRefunds = signal<any[]>([]);

  statusFilter = '';
  readonly orderSteps = ORDER_STEPS;

  newOrderForm = {
    userEmail: '',
    serviceName: '',
    amount: 0,
    notes: '',
  };

  editProgressForm = {
    percentage: 0,
    status: 'PENDING',
    progressMessage: '',
    adminNotes: '',
  };

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    const params: Record<string, string> = {
      page: this.currentPage().toString(),
      size: '20',
    };
    if (this.statusFilter) params['status'] = this.statusFilter;

    this.http
      .get<ApiResponse<PageResponse<OrderItem>>>(
        `${environment.apiUrl}/api/v1/admin/orders`,
        { params, withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          const page = res.data!;
          this.orders.set(page.content.map(o => ({
            ...o,
            customerName: (o as any).userName || (o as any).customerName || '',
            customerEmail: (o as any).userEmail || (o as any).customerEmail || '',
            amount: (o as any).totalAmount || (o as any).amount || 0,
          })));
          this.totalOrders.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  changePage(page: number): void {
    this.currentPage.set(page);
    this.loadOrders();
  }

  // ========== Order Detail ==========

  viewOrderDetail(orderId: string): void {
    this.loadingDetail.set(true);
    this.showDetailModal.set(true);
    this.orderRefunds.set([]);

    this.http
      .get<ApiResponse<OrderDetail>>(
        `${environment.apiUrl}/api/v1/admin/orders/${orderId}`,
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          const detail = res.data!;
          this.orderDetail.set(detail);
          this.editProgressForm = {
            percentage: detail.progressPercentage || 0,
            status: detail.status || 'PENDING',
            progressMessage: detail.processingNotes || '',
            adminNotes: detail.adminNotes || '',
          };
          this.loadingDetail.set(false);

          // Load refunds for this order
          this.http
            .get<ApiResponse<any[]>>(
              `${environment.apiUrl}/api/v1/admin/orders/${orderId}/refunds`,
              { withCredentials: true },
            )
            .subscribe({
              next: (refRes) => this.orderRefunds.set(refRes.data ?? []),
            });
        },
        error: () => {
          this.loadingDetail.set(false);
          this.showToast('error', 'Impossible de charger les détails');
          this.showDetailModal.set(false);
        },
      });
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.orderDetail.set(null);
    this.orderRefunds.set([]);
  }

  saveOrderProgress(): void {
    const detail = this.orderDetail();
    if (!detail) return;

    this.savingProgress.set(true);

    const payload = {
      status: this.editProgressForm.status,
      progressPercentage: this.editProgressForm.percentage,
      progressStatus: this.editProgressForm.progressMessage,
      processingNotes: this.editProgressForm.progressMessage,
      adminNotes: this.editProgressForm.adminNotes,
    };

    this.http
      .put<ApiResponse<void>>(
        `${environment.apiUrl}/api/v1/admin/orders/${detail.id}`,
        payload,
        { withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.showToast('success', 'Commande mise à jour');
          this.savingProgress.set(false);
          this.closeDetailModal();
          this.loadOrders();
        },
        error: () => {
          this.showToast('error', 'Erreur lors de la mise à jour');
          this.savingProgress.set(false);
        },
      });
  }

  // ========== Status ↔ Progress Sync ==========

  /**
   * When the slider moves, find the highest step threshold that the percentage
   * has reached and update the status to match.
   */
  onProgressPercentageChange(percentage: number): void {
    // Find the highest step whose threshold is <= the new percentage
    let matchedStep = ORDER_STEPS[0];
    for (const step of ORDER_STEPS) {
      if (percentage >= step.threshold) {
        matchedStep = step;
      }
    }
    this.editProgressForm.status = matchedStep.status;
  }

  /**
   * When the status dropdown changes, snap the progress bar to the
   * corresponding step's threshold percentage.
   */
  onStatusChange(status: string): void {
    const step = ORDER_STEPS.find((s) => s.status === status);
    if (step) {
      this.editProgressForm.percentage = step.threshold;
    }
    // For statuses not in ORDER_STEPS (PAYMENT_PENDING, DELIVERED, CANCELLED, REFUNDED),
    // keep percentage unchanged.
  }

  // ========== Helpers ==========

  getStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'bg-green-500/10 text-green-500';
      case 'DELIVERED': return 'bg-green-500/10 text-green-500';
      case 'CONFIRMED': return 'bg-blue-500/10 text-blue-500';
      case 'PROCESSING': return 'bg-amber-500/10 text-amber-500';
      case 'IN_PROGRESS': return 'bg-amber-500/10 text-amber-500';
      case 'SHIPPED': return 'bg-indigo-500/10 text-indigo-500';
      case 'PENDING': return 'bg-yellow-500/10 text-yellow-500';
      case 'PAYMENT_PENDING': return 'bg-orange-500/10 text-orange-500';
      case 'CANCELLED': return 'bg-red-500/10 text-red-500';
      case 'REFUNDED': return 'bg-violet-500/10 text-violet-500';
      default: return 'bg-gray-500/10 text-gray-400';
    }
  }

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
      default: return status;
    }
  }

  getProgressBarClass(percentage: number): string {
    if (percentage >= 100) return 'bg-emerald-500';
    if (percentage >= 60) return 'bg-blue-500';
    if (percentage >= 30) return 'bg-amber-500';
    return 'bg-orange-500';
  }

  getProgressTextClass(percentage: number): string {
    if (percentage >= 100) return 'text-emerald-500';
    if (percentage >= 60) return 'text-blue-500';
    if (percentage >= 30) return 'text-amber-500';
    return 'text-orange-500';
  }

  createOrder(): void {
    if (!this.newOrderForm.userEmail || !this.newOrderForm.serviceName || !this.newOrderForm.amount) {
      this.showToast('error', 'Veuillez remplir tous les champs obligatoires');
      return;
    }

    this.creatingOrder.set(true);

    // Find user by email then create order
    this.http
      .get<ApiResponse<PageResponse<{ id: string; email: string }>>>(
        `${environment.apiUrl}/api/v1/admin/users`,
        { params: { size: '100', page: '0' }, withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          const users = res.data?.content || [];
          const found = users.find(
            (u) => u.email.toLowerCase() === this.newOrderForm.userEmail.toLowerCase(),
          );

          if (!found) {
            this.showToast('error', 'Utilisateur non trouvé: ' + this.newOrderForm.userEmail);
            this.creatingOrder.set(false);
            return;
          }

          this.http
            .post<ApiResponse<any>>(
              `${environment.apiUrl}/api/v1/admin/orders`,
              {
                userId: found.id,
                serviceName: this.newOrderForm.serviceName,
                amount: this.newOrderForm.amount,
                notes: this.newOrderForm.notes,
              },
              { withCredentials: true },
            )
            .subscribe({
              next: () => {
                this.showToast('success', 'Commande créée pour ' + this.newOrderForm.userEmail);
                this.showCreateOrderModal.set(false);
                this.creatingOrder.set(false);
                this.newOrderForm = { userEmail: '', serviceName: '', amount: 0, notes: '' };
                this.loadOrders();
              },
              error: (err) => {
                this.showToast('error', err.error?.message || 'Erreur lors de la création');
                this.creatingOrder.set(false);
              },
            });
        },
        error: () => {
          this.showToast('error', 'Impossible de rechercher l\'utilisateur');
          this.creatingOrder.set(false);
        },
      });
  }

  syncWithStripe(orderId: string): void {
    this.syncing.set(true);
    this.http
      .post<ApiResponse<any>>(
        `${environment.apiUrl}/api/v1/admin/orders/${orderId}/stripe-sync`,
        {},
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          const data = res.data;
          if (data?.synced) {
            this.showToast('success', `Synchronisé: ${data.updatedPaymentStatus} / ${data.updatedStatus}`);
            // Refresh the detail modal
            this.viewOrderDetail(orderId);
          } else {
            this.showToast('error', data?.message || 'Impossible de synchroniser');
          }
          this.syncing.set(false);
        },
        error: (err) => {
          this.showToast('error', err.error?.message || 'Erreur de synchronisation Stripe');
          this.syncing.set(false);
        },
      });
  }

  isInvoiceEligible(status: string): boolean {
    return ['CONFIRMED', 'PROCESSING', 'IN_PROGRESS', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'REFUNDED'].includes(status);
  }

  downloadInvoice(orderId: string): void {
    this.http
      .get(`${environment.apiUrl}/api/v1/admin/orders/${orderId}/invoice`, {
        withCredentials: true,
        responseType: 'blob',
      })
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `Facture_${orderId}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        },
        error: () => this.showToast('error', 'Impossible de télécharger la facture'),
      });
  }

  private showToast(type: 'success' | 'error', message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => this.toast.set(null), 3000);
  }
}
