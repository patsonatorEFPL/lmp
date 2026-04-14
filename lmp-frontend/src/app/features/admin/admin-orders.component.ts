import {
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  OnInit,
  signal,
  untracked,
} from '@angular/core';
import { NgClass, DatePipe, CurrencyPipe, DecimalPipe, SlicePipe } from '@angular/common';
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
  Link2,
  Trash2,
  Filter,
  ArrowUpDown,
  MapPin,
  ShieldAlert,
  ChevronDown,
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AdminSseService } from '../../core/services/admin-sse.service';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { createListFetchLoading } from '../../core/utils/list-fetch-loading';

interface OrderItem {
  id: string;
  customerName: string;
  customerEmail: string;
  serviceName: string;
  amount: number;
  currency: string;
  /** Montant TTC en EUR (calculé côté backend). */
  totalAmountEur?: number | null;
  status: string;
  createdAt: string;
  /** Présent dans la réponse API liste admin (OrderResponse). */
  paidAt?: string | null;
  progressPercentage?: number;
  progressStatus?: string;
  processingNotes?: string;
  billingName?: string | null;
  billingAddress?: string | null;
  billingCity?: string | null;
  billingPostalCode?: string | null;
  billingCountry?: string | null;
  customerVatNumber?: string | null;
  vatCompanyName?: string | null;
  ipCountry?: string | null;
  ipAddress?: string | null;
  fraudScore?: number | null;
}

interface OrderDetail {
  id: string;
  serviceName: string;
  totalAmount: number;
  currency: string;
  /** Montant TTC en EUR (calculé côté backend). */
  totalAmountEur?: number | null;
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
  guestPaymentLink?: string | null;
  billingName: string | null;
  billingAddress: string | null;
  billingCity: string | null;
  billingPostalCode: string | null;
  billingCountry: string | null;
  vatReverseCharge: boolean | null;
  customerVatNumber: string | null;
  vatCompanyName: string | null;
  ipCountry: string | null;
  ipAddress: string | null;
  vpnScore: number | null;
  vpnSources: string | null;
  browserTimezone: string | null;
  geoCountry: string | null;
  cardCountry: string | null;
  fraudScore: number | null;
  fraudFlags: string | null;
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

/** Valeur du select « Autre (saisie libre) » pour création de commande admin. */
const CREATE_ORDER_SERVICE_OTHER = '__other__';

interface AdminCatalogServiceRow {
  id: string;
  title: string;
  currentOffer?: { price?: number } | null;
}

interface CreateOrderCatalogOption {
  id: string;
  title: string;
  defaultPrice: number;
}

/** Ligne utilisateur admin (liste / autocomplétion création de commande). */
interface CreateOrderAdminUser {
  id: string;
  email: string;
  displayName?: string | null;
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
  imports: [NgClass, DatePipe, CurrencyPipe, DecimalPipe, SlicePipe, FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="crm-list-view flex h-full flex-col overflow-hidden bg-(--background)">
    <!-- Barre de filtres inline (style CRM) -->
    <div class="flex items-center justify-between gap-2 px-5 py-4">
      <div class="flex items-center"></div>
      <!-- Actions droite -->
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
              @for (opt of orderSortOptions; track opt.key) {
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
        <button
          hlmBtn variant="default" size="sm" type="button"
          class="ml-1 h-7 cursor-pointer gap-1.5 px-2.5"
          (click)="openCreateOrderModal()"
        >
          <lucide-icon [img]="PlusIcon" [size]="14"></lucide-icon>
          <span class="text-sm">Créer</span>
        </button>
      </div>
    </div>

    <!-- Panneau de filtres (toggle) -->
    @if (showFilterPanel()) {
      <div class="flex items-center gap-2 border-b border-zinc-100 px-5 pb-3 dark:border-zinc-800">
        <input
          type="text"
          [(ngModel)]="emailFilter"
          (input)="filterOrders()"
          placeholder="Rechercher par email…"
          class="h-8 w-56 rounded-lg border border-zinc-200 bg-zinc-50 px-3 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
        />
        <select
          [(ngModel)]="statusFilter"
          (change)="currentPage.set(0); loadOrders()"
          class="h-8 w-44 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
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
        @if (hasActiveOrderFilters()) {
          <button
            type="button"
            class="flex h-8 cursor-pointer items-center gap-1 rounded-lg px-2 text-sm text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800"
            (click)="clearOrderFilters()"
          >
            <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
            Effacer
          </button>
        }
      </div>
    }

    <!-- Liste (style CRM) -->
    <div class="flex-1 overflow-auto px-3 sm:px-5">
      @if (loading()) {
        <div class="flex items-center justify-center py-16">
          <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-zinc-400"></lucide-icon>
        </div>
      } @else {
        <!-- En-tête colonnes (style CRM - fond gris arrondi, mb-2) -->
        <div class="mb-2 flex min-w-max items-center rounded-lg bg-zinc-100 py-1.5 text-sm font-normal leading-none text-zinc-500 dark:bg-zinc-800/70 dark:text-zinc-400">
          <div class="flex w-10 shrink-0 items-center justify-center">
            <input
              type="checkbox"
              class="h-3.5 w-3.5 cursor-pointer rounded-xs border-zinc-400 text-zinc-600 focus:ring-zinc-400"
              [checked]="allRowsSelected()"
              (change)="toggleSelectAll($event)"
            />
          </div>
          <div class="w-64 shrink-0 px-2">Email</div>
          <div class="w-40 shrink-0 px-2 text-center">Service</div>
          <div class="w-28 shrink-0 px-2 text-center">Montant</div>
          <div class="w-32 shrink-0 px-2 text-center">Statut</div>
          <div class="w-36 shrink-0 px-2 text-center">Date</div>
          <div class="w-40 shrink-0 px-2 text-center">Nom facture</div>
          <div class="w-44 shrink-0 px-2 text-center">Adresse</div>
          <div class="w-28 shrink-0 px-2 text-center">Ville</div>
          <div class="w-24 shrink-0 px-2 text-center">Code postal</div>
          <div class="w-20 shrink-0 px-2 text-center">Pays</div>
          <div class="w-32 shrink-0 px-2 text-center">IP</div>
          <div class="w-32 shrink-0 px-2 text-center">N° TVA</div>
          <div class="w-36 shrink-0 px-2 text-center">Société</div>
          <div class="w-20 shrink-0 px-2 text-center">Score</div>
          <div class="w-32 shrink-0 px-2 text-center">Last Modified</div>
        </div>

        <!-- Lignes (style CRM - cliquables, sans bordures visibles entre les lignes) -->
        <div>
          @for (order of filteredOrders(); track order.id) {
            <div
              class="group flex h-10 min-w-max cursor-pointer items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
              (click)="viewOrderDetail(order.id)"
            >
              <div class="flex w-10 shrink-0 items-center justify-center" (click)="$event.stopPropagation()">
                <input
                  type="checkbox"
                  class="h-3.5 w-3.5 cursor-pointer rounded-xs border-zinc-400 text-zinc-600 focus:ring-zinc-400"
                  [checked]="selectedOrderIds().has(order.id)"
                  (change)="toggleOrderSelected(order.id)"
                />
              </div>
              <div class="w-64 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">
                {{ order.customerEmail || '—' }}
              </div>
              <div class="w-40 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                {{ order.serviceName }}
              </div>
              <div class="w-28 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400" [title]="order.currency !== 'EUR' ? (order.amount | currency:order.currency:'symbol':'1.0-2') + ' (devise client)' : ''">
                {{ (order.totalAmountEur ?? order.amount) | currency:'EUR':'symbol':'1.2-2' }}
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
              <div class="w-40 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                {{ order.billingName || '—' }}
              </div>
              <div class="w-44 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                {{ order.billingAddress || '—' }}
              </div>
              <div class="w-28 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                {{ order.billingCity || '—' }}
              </div>
              <div class="w-24 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                {{ order.billingPostalCode || '—' }}
              </div>
              <div class="w-20 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                {{ order.billingCountry || '—' }}
              </div>
              <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                @if (order.ipAddress) {
                  <span class="font-mono text-xs">{{ order.ipAddress }}</span>
                  @if (order.ipCountry) {
                    <span class="ml-1 text-xs">({{ order.ipCountry }})</span>
                  }
                } @else {
                  —
                }
              </div>
              <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                <span class="font-mono text-xs">{{ order.customerVatNumber || '—' }}</span>
              </div>
              <div class="w-36 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                {{ order.vatCompanyName || '—' }}
              </div>
              <div class="w-20 shrink-0 px-2 text-center">
                @if (order.fraudScore != null) {
                  <span
                    class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                    [ngClass]="{
                      'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400': order.fraudScore! >= 80,
                      'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400': order.fraudScore! >= 60 && order.fraudScore! < 80,
                      'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400': order.fraudScore! < 60
                    }"
                  >
                    {{ order.fraudScore }}
                  </span>
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
              Aucune commande trouvée
            </div>
          }
        </div>
      }
    </div>

    <!-- Footer pagination (style CRM) -->
    <div class="flex items-center justify-between border-t border-zinc-200 px-3 py-2 sm:px-5 dark:border-zinc-800">
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
        <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ totalOrders() }}</span>
      </div>
    </div>

    <!-- Order Detail Modal -->
    @if (showDetailModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        (click)="closeDetailModal()"
      >
        <div
          class="mx-4 w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-sm border border-(--border) bg-(--card) shadow-2xl"
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
                <h3 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">
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
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Montant</p>
                  <p class="mt-1 text-lg font-bold text-(--foreground)">
                    {{ (orderDetail()!.totalAmountEur ?? orderDetail()!.totalAmount) | currency:'EUR':'symbol':'1.2-2' }}
                  </p>
                  @if (orderDetail()!.currency && orderDetail()!.currency !== 'EUR') {
                    <p class="mt-0.5 text-xs text-(--muted-foreground)">
                      Client : {{ orderDetail()!.totalAmount | currency:orderDetail()!.currency:'symbol':'1.0-2' }}
                    </p>
                  }
                </div>
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Statut</p>
                  <span
                    class="mt-1 inline-flex rounded-xs px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusClass(orderDetail()!.status)"
                  >
                    {{ getStatusLabel(orderDetail()!.status) }}
                  </span>
                </div>
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Paiement</p>
                  <p class="mt-1 text-sm font-medium text-(--foreground)">
                    {{ orderDetail()!.paymentStatus || '—' }}
                  </p>
                </div>
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Date</p>
                  <p class="mt-1 text-sm text-(--foreground)">
                    {{ orderDetail()!.createdAt | date:'dd/MM/yy' }}
                  </p>
                </div>
              </div>

              <!-- Client Info -->
              <div>
                <p class="text-xs font-medium text-(--muted-foreground)">Client</p>
                <div class="mt-2 flex items-center gap-3 rounded-sm border border-(--border) bg-(--background) p-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-full bg-(--primary)/10 text-(--primary)">
                    <lucide-icon [img]="ShoppingCartIcon" [size]="16"></lucide-icon>
                  </div>
                  <div>
                    <p class="text-sm font-medium text-(--foreground)">{{ orderDetail()!.userName || '—' }}</p>
                    <p class="text-xs text-(--muted-foreground)">{{ orderDetail()!.userEmail || '—' }}</p>
                  </div>
                </div>
              </div>

              <!-- Billing Address -->
              @if (orderDetail()!.billingName || orderDetail()!.billingAddress || orderDetail()!.billingCity || orderDetail()!.billingPostalCode || orderDetail()!.billingCountry) {
                <div>
                  <p class="text-xs font-medium text-(--muted-foreground)">Facturation</p>
                  <div class="mt-2 flex items-start gap-3 rounded-sm border border-(--border) bg-(--background) p-3">
                    <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-(--primary)/10 text-(--primary)">
                      <lucide-icon [img]="MapPinIcon" [size]="16"></lucide-icon>
                    </div>
                    <div class="text-sm text-(--foreground)">
                      @if (orderDetail()!.billingName) {
                        <p class="font-medium">{{ orderDetail()!.billingName }}</p>
                      }
                      @if (orderDetail()!.billingAddress) {
                        <p>{{ orderDetail()!.billingAddress }}</p>
                      }
                      <p>
                        @if (orderDetail()!.billingPostalCode) {
                          <span>{{ orderDetail()!.billingPostalCode }}</span>
                        }
                        @if (orderDetail()!.billingCity) {
                          <span>{{ orderDetail()!.billingPostalCode ? ' ' : '' }}{{ orderDetail()!.billingCity }}</span>
                        }
                      </p>
                      @if (orderDetail()!.billingCountry) {
                        <p class="text-xs text-(--muted-foreground)">{{ orderDetail()!.billingCountry }}</p>
                      }
                    </div>
                  </div>
                </div>
              }

              <div>
                <p class="text-xs font-medium text-(--muted-foreground)">Notes (création / client)</p>
                <div
                  class="mt-2 rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground)"
                >
                  @if (orderDetail()!.notes?.trim()) {
                    <p class="whitespace-pre-wrap">{{ orderDetail()!.notes }}</p>
                  } @else {
                    <p class="text-(--muted-foreground) italic">Aucune note sur cette commande.</p>
                  }
                </div>
              </div>

              @if (orderDetail()!.guestPaymentLink) {
                <div class="rounded-sm border border-emerald-500/30 bg-emerald-500/5 p-4">
                  <p class="text-xs font-medium text-(--foreground)">Lien de paiement invité</p>
                  <p class="mt-1 text-xs text-(--muted-foreground)">
                    À envoyer au client tant que le paiement n’est pas confirmé.
                  </p>
                  <input
                    readonly
                    class="mt-2 w-full rounded-sm border border-(--border) bg-(--background) px-2 py-2 font-mono text-xs text-(--foreground)"
                    [value]="orderDetail()!.guestPaymentLink!"
                  />
                  <button
                    type="button"
                    hlmBtn variant="outline" size="sm" class="mt-2 cursor-pointer gap-2"
                    (click)="copyGuestPaymentLink(orderDetail()!.guestPaymentLink!)"
                  >
                    <lucide-icon [img]="Link2Icon" [size]="14"></lucide-icon>
                    Copier le lien
                  </button>
                </div>
              }

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
                      class="flex items-center gap-3 rounded-sm px-3 py-2 transition-colors"
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
                <div class="space-y-3 rounded-sm border border-(--border) bg-(--background) p-4">
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
                      class="w-full rounded-sm border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) cursor-pointer"
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
                      class="w-full rounded-sm border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) focus:ring-1 focus:ring-(--primary)"
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
                      class="w-full rounded-sm border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) focus:ring-1 focus:ring-(--primary)"
                      placeholder="Notes internes..."
                    ></textarea>
                  </div>
                </div>
              </div>
            </div>

            <!-- Fraud Audit Section -->
            @if (orderDetail()!.fraudScore != null) {
              <div class="px-6 pb-4">
                <button
                  type="button"
                  class="flex w-full cursor-pointer items-center justify-between rounded-sm border border-(--border) bg-(--background) px-4 py-3 text-left transition-colors hover:bg-(--muted)"
                  (click)="fraudAuditOpen.set(!fraudAuditOpen())"
                >
                  <div class="flex items-center gap-2">
                    <lucide-icon
                      [img]="ShieldAlertIcon" [size]="16"
                      [ngClass]="orderDetail()!.fraudScore! < 60 ? 'text-red-500' : orderDetail()!.fraudScore! < 80 ? 'text-amber-500' : 'text-green-500'"
                    ></lucide-icon>
                    <span class="text-sm font-semibold text-(--foreground)">Audit Fraude</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span
                      class="inline-flex min-w-[2rem] items-center justify-center rounded-xs px-2 py-0.5 text-xs font-bold"
                      [ngClass]="{
                        'bg-green-500/10 text-green-600 dark:text-green-400': orderDetail()!.fraudScore! >= 80,
                        'bg-amber-500/10 text-amber-600 dark:text-amber-400': orderDetail()!.fraudScore! >= 60 && orderDetail()!.fraudScore! < 80,
                        'bg-red-500/10 text-red-600 dark:text-red-400': orderDetail()!.fraudScore! < 60
                      }"
                    >
                      {{ orderDetail()!.fraudScore }}/100
                    </span>
                    <lucide-icon
                      [img]="ChevronDownIcon" [size]="14"
                      class="text-(--muted-foreground) transition-transform duration-200"
                      [ngClass]="{ 'rotate-180': fraudAuditOpen() }"
                    ></lucide-icon>
                  </div>
                </button>

                @if (fraudAuditOpen()) {
                  <div class="mt-2 space-y-3 rounded-sm border border-(--border) bg-(--background) p-4 animate-in fade-in slide-in-from-top-1 duration-200">

                    <!-- Signal Details Grid -->
                    <div class="grid grid-cols-2 gap-x-4 gap-y-1.5 text-xs sm:grid-cols-3">
                      @if (orderDetail()!.ipCountry || orderDetail()!.ipAddress) {
                        <div>
                          <span class="text-(--muted-foreground)">IP :</span>
                          <span class="ml-1 font-medium text-(--foreground)">
                            {{ orderDetail()!.ipAddress || '—' }}
                            @if (orderDetail()!.ipCountry) { ({{ orderDetail()!.ipCountry }}) }
                          </span>
                        </div>
                      }
                      @if (orderDetail()!.vpnScore != null) {
                        <div>
                          <span class="text-(--muted-foreground)">VPN Score :</span>
                          <span
                            class="ml-1 font-medium"
                            [ngClass]="orderDetail()!.vpnScore! > 0.6 ? 'text-red-500' : orderDetail()!.vpnScore! > 0.4 ? 'text-amber-500' : 'text-(--foreground)'"
                          >
                            {{ orderDetail()!.vpnScore | number:'1.2-2' }}
                          </span>
                        </div>
                      }
                      @if (orderDetail()!.browserTimezone) {
                        <div>
                          <span class="text-(--muted-foreground)">Timezone :</span>
                          <span class="ml-1 font-medium text-(--foreground)">{{ orderDetail()!.browserTimezone }}</span>
                        </div>
                      }
                      @if (orderDetail()!.geoCountry) {
                        <div>
                          <span class="text-(--muted-foreground)">Géoloc :</span>
                          <span class="ml-1 font-medium text-(--foreground)">{{ orderDetail()!.geoCountry }}</span>
                        </div>
                      }
                      @if (orderDetail()!.billingCountry) {
                        <div>
                          <span class="text-(--muted-foreground)">Facturation :</span>
                          <span class="ml-1 font-medium text-(--foreground)">{{ orderDetail()!.billingCountry }}</span>
                        </div>
                      }
                      @if (orderDetail()!.cardCountry) {
                        <div>
                          <span class="text-(--muted-foreground)">Carte :</span>
                          <span class="ml-1 font-medium text-(--foreground)">{{ orderDetail()!.cardCountry }}</span>
                        </div>
                      }
                      @if (orderDetail()!.customerVatNumber) {
                        <div>
                          <span class="text-(--muted-foreground)">TVA :</span>
                          <span class="ml-1 font-mono font-medium text-(--foreground)">{{ orderDetail()!.customerVatNumber }}</span>
                        </div>
                      }
                      @if (orderDetail()!.vatCompanyName) {
                        <div>
                          <span class="text-(--muted-foreground)">Nom VIES :</span>
                          <span class="ml-1 font-medium text-(--foreground)">{{ orderDetail()!.vatCompanyName }}</span>
                        </div>
                      }
                      @if (orderDetail()!.billingName) {
                        <div>
                          <span class="text-(--muted-foreground)">Nom facturation :</span>
                          <span class="ml-1 font-medium text-(--foreground)">{{ orderDetail()!.billingName }}</span>
                        </div>
                      }
                    </div>

                    <!-- Fraud Flags Breakdown -->
                    @if (parsedFraudFlags().length > 0) {
                      <div class="h-px bg-(--border)"></div>
                      <div class="space-y-1.5">
                        @for (flag of parsedFraudFlags(); track flag.key) {
                          <div class="flex items-start justify-between gap-2 rounded-xs px-2 py-1.5"
                            [ngClass]="flag.penalty < 0 ? 'bg-red-500/5' : 'bg-green-500/5'"
                          >
                            <div class="flex items-start gap-2">
                              <lucide-icon
                                [img]="flag.penalty < 0 ? AlertCircleIcon : CheckCircleIcon"
                                [size]="14"
                                class="mt-0.5 shrink-0"
                                [ngClass]="flag.penalty < 0 ? 'text-red-500' : 'text-green-500'"
                              ></lucide-icon>
                              <div>
                                <p class="text-xs font-medium text-(--foreground)">{{ flag.label }}</p>
                                <p class="text-[11px] text-(--muted-foreground)">{{ flag.key }}</p>
                              </div>
                            </div>
                            <span
                              class="shrink-0 text-xs font-bold"
                              [ngClass]="flag.penalty < 0 ? 'text-red-500' : 'text-green-500'"
                            >
                              {{ flag.penalty > 0 ? '+' : '' }}{{ flag.penalty }}
                            </span>
                          </div>
                        }
                      </div>
                    } @else {
                      <p class="text-xs italic text-(--muted-foreground)">Aucun signal de fraude détecté.</p>
                    }
                  </div>
                }
              </div>
            }

            <!-- Refunds Section -->
            @if (orderRefunds().length > 0) {
              <div class="px-6 pb-4">
                <div class="flex items-center gap-2 mb-3">
                  <lucide-icon [img]="RotateCcwIcon" [size]="16" class="text-(--primary)"></lucide-icon>
                  <h4 class="text-sm font-semibold text-(--foreground)">Remboursements</h4>
                </div>
                <div class="space-y-2">
                  @for (refund of orderRefunds(); track refund.id) {
                    <div class="flex items-center justify-between rounded-sm border border-(--border) bg-(--background) p-3">
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
                        class="rounded-xs px-2 py-0.5 text-xs font-medium"
                        [ngClass]="refund.status === 'succeeded' ? 'bg-green-500/10 text-green-500' :
                                   refund.status === 'pending' ? 'bg-(--muted) text-(--foreground)' :
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
                @if (canDeleteOrderDetail()) {
                  <button
                    hlmBtn variant="destructive" size="sm" class="cursor-pointer gap-2"
                    [disabled]="deletingOrder()"
                    (click)="confirmAndDeleteOrder(orderDetail()!.id, { closeModal: true })"
                  >
                    @if (deletingOrder()) {
                      <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                    } @else {
                      <lucide-icon [img]="Trash2Icon" [size]="14"></lucide-icon>
                    }
                    Supprimer
                  </button>
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
                [disabled]="savingProgress() || deletingOrder()"
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
        (click)="closeCreateOrderModal()"
      >
        <div
          class="mx-4 w-full max-w-md rounded-sm border border-(--border) bg-(--card) shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          <div class="flex items-center justify-between border-b border-(--border) px-6 py-4">
            <h3 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">
              Créer une commande
            </h3>
            <button
              hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
              (click)="closeCreateOrderModal()"
            >
              <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
            </button>
          </div>

          <div class="space-y-4 px-6 py-5">
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                Email du client *
              </label>
              <p class="mb-2 text-xs text-(--muted-foreground)">
                S’il existe un compte avec cet email, la commande y est rattachée. Sinon, une commande « invité » est créée avec un lien de paiement (et une invitation est envoyée à cette adresse si elle est valide).
              </p>
              <p class="mb-2 text-xs text-(--muted-foreground)">
                Suggestions : comptes récents au chargement ; tapez au moins 2 caractères pour chercher dans la base.
              </p>
              <div class="relative">
                <input
                  [(ngModel)]="newOrderForm.userEmail"
                  name="userEmail"
                  (input)="onUserEmailTyped()"
                  type="email"
                  autocomplete="off"
                  list="admin-create-order-user-emails"
                  class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 pr-24 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                  placeholder="email@client.com"
                />
                @if (createOrderUsersLoading()) {
                  <span
                    class="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-xs text-(--muted-foreground)"
                  >
                    Liste…
                  </span>
                } @else if (userEmailSuggestLoading()) {
                  <span
                    class="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-xs text-(--muted-foreground)"
                  >
                    Recherche…
                  </span>
                }
              </div>
              <datalist id="admin-create-order-user-emails">
                @for (opt of createOrderUserDatalistOptions(); track opt.email) {
                  <option [value]="opt.email">{{ opt.label }}</option>
                }
              </datalist>
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                Service *
              </label>
              <select
                [(ngModel)]="newOrderForm.serviceSelection"
                (ngModelChange)="onCreateOrderServiceSelectionChange($event)"
                name="serviceSelection"
                class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) cursor-pointer"
              >
                <option value="">— Choisir un service —</option>
                @for (opt of catalogServicesForCreate(); track opt.id) {
                  <option [value]="opt.id">
                    {{ opt.title }}
                    @if (opt.defaultPrice > 0) {
                      ({{ opt.defaultPrice | number: '1.2-2' }} €)
                    }
                  </option>
                }
                <option [value]="createOrderServiceOther">Autre (saisie libre)</option>
              </select>
              @if (newOrderForm.serviceSelection === createOrderServiceOther) {
                <input
                  [(ngModel)]="newOrderForm.serviceName"
                  name="customServiceName"
                  type="text"
                  class="mt-2 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                  placeholder="Intitulé du service ou prestation…"
                />
              }
              @if (
                newOrderForm.serviceSelection &&
                newOrderForm.serviceSelection !== createOrderServiceOther
              ) {
                <p class="mt-2 text-xs text-(--muted-foreground)">
                  Libellé facturé :
                  <span class="font-medium text-(--foreground)">{{ newOrderForm.serviceName }}</span>
                </p>
              }
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                Montant (EUR) *
                <span class="block font-normal text-(--muted-foreground)">Rempli automatiquement depuis le catalogue ; vous pouvez l’ajuster.</span>
              </label>
              <input
                [(ngModel)]="newOrderForm.amount"
                name="orderAmount"
                type="number"
                min="0.01"
                step="0.01"
                class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
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
                class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="Description..."
              ></textarea>
            </div>

            @if (guestPaymentLink()) {
              <div class="rounded-sm border border-emerald-500/30 bg-emerald-500/5 p-4">
                <p class="text-xs font-medium text-(--foreground)">Lien à transmettre au client</p>
                <input
                  readonly
                  class="mt-2 w-full rounded-sm border border-(--border) bg-(--background) px-2 py-2 font-mono text-xs text-(--foreground)"
                  [value]="guestPaymentLink()!"
                />
                <button
                  type="button"
                  hlmBtn variant="outline" size="sm" class="mt-2 cursor-pointer"
                  (click)="copyGuestLink()"
                >
                  Copier le lien
                </button>
              </div>
            }
          </div>

          <div class="flex items-center justify-end gap-2 border-t border-(--border) px-6 py-4">
            <button
              hlmBtn variant="outline" size="sm" class="cursor-pointer"
              (click)="closeCreateOrderModal()"
            >
              {{ guestPaymentLink() ? 'Fermer' : 'Annuler' }}
            </button>
            @if (!guestPaymentLink()) {
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
            }
          </div>
        </div>
      </div>
    }

    <!-- Toast -->
    @if (toast()) {
      <div
        class="fixed right-4 bottom-4 z-[200] flex items-center gap-2 rounded-sm border px-4 py-3 shadow-xs"
        [ngClass]="{
          'border-emerald-500/30 bg-(--muted) text-(--foreground)': toast()!.type === 'success',
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
    </div>
  `,
})
export class AdminOrdersComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly adminSse = inject(AdminSseService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly visiblePoll = inject(VisiblePollService);

  constructor() {
    effect(() => {
      const badge = this.adminSse.badgeOrders();
      if (badge > 0) {
        untracked(() => this.loadOrders({ silent: true }));
      }
    });
    this.destroyRef.onDestroy(() => {
      if (this.userEmailSuggestTimer != null) {
        clearTimeout(this.userEmailSuggestTimer);
        this.userEmailSuggestTimer = null;
      }
    });
  }

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
  readonly Link2Icon = Link2;
  readonly Trash2Icon = Trash2;
  readonly FilterIcon = Filter;
  readonly ArrowUpDownIcon = ArrowUpDown;
  readonly MapPinIcon = MapPin;
  readonly ShieldAlertIcon = ShieldAlert;
  readonly ChevronDownIcon = ChevronDown;


  readonly loading = signal(false);
  private readonly listFetch = createListFetchLoading(this.loading);
  readonly loadingDetail = signal(false);
  readonly savingProgress = signal(false);
  readonly orders = signal<OrderItem[]>([]);
  readonly filteredOrders = signal<OrderItem[]>([]);
  readonly totalOrders = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly showDetailModal = signal(false);
  readonly orderDetail = signal<OrderDetail | null>(null);
  readonly toast = signal<{ type: 'success' | 'error'; message: string } | null>(null);
  readonly showCreateOrderModal = signal(false);
  readonly guestPaymentLink = signal<string | null>(null);
  readonly creatingOrder = signal(false);
  readonly syncing = signal(false);
  readonly orderRefunds = signal<any[]>([]);
  readonly deletingOrder = signal(false);
  readonly fraudAuditOpen = signal(false);
  readonly selectedOrderIds = signal<Set<string>>(new Set());

  readonly pageSizes = [20, 50, 100];
  readonly pageSize = signal(20);

  // Filter & Sort
  readonly showFilterPanel = signal(false);
  readonly showSortMenu = signal(false);
  readonly currentSort = signal<string>('createdAt');
  readonly sortDirection = signal<'asc' | 'desc'>('desc');
  readonly orderSortOptions = [
    { key: 'customerEmail', label: 'Email' },
    { key: 'serviceName', label: 'Service' },
    { key: 'amount', label: 'Montant' },
    { key: 'status', label: 'Statut' },
    { key: 'createdAt', label: 'Date de création' },
  ];

  emailFilter = '';

  readonly allRowsSelected = computed(() => {
    const list = this.filteredOrders();
    if (list.length === 0) return false;
    const sel = this.selectedOrderIds();
    return list.every((o) => sel.has(o.id));
  });
  /** Options catalogue pour le modal « Créer une commande » (GET /api/v1/admin/services). */
  readonly catalogServicesForCreate = signal<CreateOrderCatalogOption[]>([]);
  /** Exposé au template pour comparer avec la valeur du select « Autre ». */
  readonly createOrderServiceOther = CREATE_ORDER_SERVICE_OTHER;

  readonly createOrderUsersList = signal<CreateOrderAdminUser[]>([]);
  readonly createOrderUsersLoading = signal(false);
  readonly userEmailSuggestRows = signal<CreateOrderAdminUser[]>([]);
  readonly userEmailSuggestLoading = signal(false);

  readonly createOrderUserDatalistOptions = computed(() => {
    const byEmail = new Map<string, { email: string; label: string }>();
    const add = (u: CreateOrderAdminUser) => {
      const key = u.email.toLowerCase();
      if (!byEmail.has(key)) {
        byEmail.set(key, { email: u.email, label: this.formatCreateOrderUserLabel(u) });
      }
    };
    this.createOrderUsersList().forEach(add);
    this.userEmailSuggestRows().forEach(add);
    return Array.from(byEmail.values());
  });

  private userEmailSuggestTimer: ReturnType<typeof setTimeout> | null = null;

  /** Fraud flags parsed from the fraudFlags string on orderDetail. */
  readonly parsedFraudFlags = computed(() => {
    const detail = this.orderDetail();
    if (!detail?.fraudFlags) return [];
    const raw = detail.fraudFlags.split(',').map((f: string) => f.trim()).filter(Boolean);
    return raw.map((key: string) => ({
      key,
      label: this.getFraudFlagLabel(key),
      penalty: this.getFraudFlagPenalty(key),
    }));
  });

  statusFilter = '';
  readonly orderSteps = ORDER_STEPS;

  newOrderForm = {
    userEmail: '',
    /** UUID du service catalogue, vide si non choisi, ou {@link CREATE_ORDER_SERVICE_OTHER}. */
    serviceSelection: '',
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
    this.visiblePoll.subscribeWhileVisible(
      this.destroyRef,
      environment.dashboardPollIntervalMs,
      () => this.loadOrders({ silent: true }),
    );
  }

  loadOrders(options?: { silent?: boolean }): void {
    const silent = options?.silent === true;
    this.listFetch.beforeFetch(silent);
    const params: Record<string, string> = {
      page: this.currentPage().toString(),
      size: this.pageSize().toString(),
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
          const mapped = page.content.map((raw) => {
            const o = raw as OrderItem & {
              userName?: string;
              userEmail?: string;
              totalAmount?: number;
            };
            return {
              ...o,
              customerName: o.userName || o.customerName || '',
              customerEmail: o.userEmail || o.customerEmail || '',
              amount: o.totalAmount ?? o.amount ?? 0,
              currency: o.currency || 'EUR',
              totalAmountEur: o.totalAmountEur ?? null,
              paidAt: o.paidAt ?? null,
            };
          });
          this.orders.set(mapped);
          this.selectedOrderIds.set(new Set());
          this.filterOrders();
          this.totalOrders.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.listFetch.afterFetch();
        },
        error: () => {
          this.listFetch.afterFetch();
        },
      });
  }

  filterOrders(): void {
    const q = this.emailFilter.toLowerCase();
    if (!q) {
      this.filteredOrders.set(this.orders());
      return;
    }
    this.filteredOrders.set(
      this.orders().filter(
        (o) =>
          (o.customerEmail || '').toLowerCase().includes(q) ||
          (o.customerName || '').toLowerCase().includes(q),
      ),
    );
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadOrders();
  }

  toggleSelectAll(ev: Event): void {
    const checked = (ev.target as HTMLInputElement).checked;
    const list = this.filteredOrders();
    if (checked) {
      this.selectedOrderIds.set(new Set(list.map((o) => o.id)));
    } else {
      this.selectedOrderIds.set(new Set());
    }
  }

  toggleOrderSelected(id: string): void {
    const next = new Set(this.selectedOrderIds());
    if (next.has(id)) next.delete(id);
    else next.add(id);
    this.selectedOrderIds.set(next);
  }

  formatRelativeTimeFr(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '—';
    const diffMs = Date.now() - d.getTime();
    const sec = Math.floor(diffMs / 1000);
    if (sec < 45) return 'À l\'instant';
    const min = Math.floor(sec / 60);
    const hours = Math.floor(min / 60);
    const days = Math.floor(hours / 24);
    if (min < 60) return min <= 1 ? 'Il y a 1 min' : `Il y a ${min} min`;
    if (hours < 24) return hours <= 1 ? 'Il y a 1 h' : `Il y a ${hours} h`;
    if (days < 7) return days === 1 ? 'Il y a 1 jour' : `Il y a ${days} j`;
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
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
    this.fraudAuditOpen.set(false);

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

  canDeleteOrderRow(order: OrderItem): boolean {
    if (order.paidAt) {
      return false;
    }
    return order.status === 'PAYMENT_PENDING' || order.status === 'CANCELLED';
  }

  canDeleteOrderDetail(): boolean {
    const d = this.orderDetail();
    if (!d || d.paidAt) {
      return false;
    }
    if (d.status !== 'PAYMENT_PENDING' && d.status !== 'CANCELLED') {
      return false;
    }
    if (this.orderRefunds().length > 0) {
      return false;
    }
    return true;
  }

  confirmAndDeleteOrder(orderId: string, options?: { closeModal?: boolean }): void {
    const msg =
      'Supprimer définitivement cette commande et les données associées (lignes, historique) ? Cette action est irréversible.';
    if (!confirm(msg)) {
      return;
    }
    this.deletingOrder.set(true);
    this.http
      .delete<ApiResponse<void>>(`${environment.apiUrl}/api/v1/admin/orders/${orderId}`, {
        withCredentials: true,
      })
      .subscribe({
        next: (res) => {
          this.deletingOrder.set(false);
          this.showToast('success', res.message ?? 'Commande supprimée');
          if (options?.closeModal) {
            this.closeDetailModal();
          }
          this.loadOrders();
        },
        error: (err: { error?: { message?: string } }) => {
          this.deletingOrder.set(false);
          this.showToast('error', err.error?.message ?? 'Impossible de supprimer la commande');
        },
      });
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
    if (percentage >= 100) return 'text-(--foreground)';
    if (percentage >= 60) return 'text-(--foreground)';
    if (percentage >= 30) return 'text-(--foreground)';
    return 'text-orange-500';
  }

  openCreateOrderModal(): void {
    this.guestPaymentLink.set(null);
    if (this.userEmailSuggestTimer != null) {
      clearTimeout(this.userEmailSuggestTimer);
      this.userEmailSuggestTimer = null;
    }
    this.userEmailSuggestRows.set([]);
    this.newOrderForm = {
      userEmail: '',
      serviceSelection: '',
      serviceName: '',
      amount: 0,
      notes: '',
    };
    this.showCreateOrderModal.set(true);
    this.loadCatalogServicesForCreateOrder();
    this.loadCreateOrderUsersForPicker();
  }

  private loadCreateOrderUsersForPicker(): void {
    this.createOrderUsersLoading.set(true);
    this.http
      .get<ApiResponse<PageResponse<CreateOrderAdminUser>>>(`${environment.apiUrl}/api/v1/admin/users`, {
        params: { page: '0', size: '300' },
        withCredentials: true,
      })
      .subscribe({
        next: (res) => {
          this.createOrderUsersList.set(res.data?.content ?? []);
          this.createOrderUsersLoading.set(false);
        },
        error: () => {
          this.createOrderUsersList.set([]);
          this.createOrderUsersLoading.set(false);
          this.showToast('error', 'Impossible de charger la liste des utilisateurs');
        },
      });
  }

  formatCreateOrderUserLabel(u: CreateOrderAdminUser): string {
    const d = (u.displayName ?? '').trim();
    if (d.length > 0) {
      return `${d} (${u.email})`;
    }
    return u.email;
  }

  onUserEmailTyped(): void {
    const q = (this.newOrderForm.userEmail ?? '').trim();
    if (this.userEmailSuggestTimer != null) {
      clearTimeout(this.userEmailSuggestTimer);
      this.userEmailSuggestTimer = null;
    }
    if (q.length < 2) {
      this.userEmailSuggestRows.set([]);
      return;
    }
    this.userEmailSuggestTimer = setTimeout(() => {
      this.userEmailSuggestTimer = null;
      this.fetchCreateOrderUserSuggestions(q);
    }, 280);
  }

  private fetchCreateOrderUserSuggestions(q: string): void {
    this.userEmailSuggestLoading.set(true);
    this.http
      .get<ApiResponse<PageResponse<CreateOrderAdminUser>>>(`${environment.apiUrl}/api/v1/admin/users`, {
        params: { page: '0', size: '30', search: q },
        withCredentials: true,
      })
      .subscribe({
        next: (res) => {
          this.userEmailSuggestRows.set(res.data?.content ?? []);
          this.userEmailSuggestLoading.set(false);
        },
        error: () => {
          this.userEmailSuggestRows.set([]);
          this.userEmailSuggestLoading.set(false);
        },
      });
  }

  private loadCatalogServicesForCreateOrder(): void {
    this.http
      .get<ApiResponse<AdminCatalogServiceRow[]>>(
        `${environment.apiUrl}/api/v1/admin/services`,
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          const rows = res.data ?? [];
          const opts: CreateOrderCatalogOption[] = rows
            .map((s) => {
              const p = s.currentOffer?.price;
              const defaultPrice =
                p !== undefined && p !== null && !Number.isNaN(Number(p)) ? Number(p) : 0;
              return { id: String(s.id), title: s.title ?? '', defaultPrice };
            })
            .filter((o) => o.title.length > 0)
            .sort((a, b) => a.title.localeCompare(b.title, 'fr', { sensitivity: 'base' }));
          this.catalogServicesForCreate.set(opts);
        },
        error: () => {
          this.catalogServicesForCreate.set([]);
          this.showToast('error', 'Impossible de charger le catalogue des services');
        },
      });
  }

  onCreateOrderServiceSelectionChange(value: string): void {
    if (!value) {
      this.newOrderForm.serviceName = '';
      this.newOrderForm.amount = 0;
      return;
    }
    if (value === CREATE_ORDER_SERVICE_OTHER) {
      this.newOrderForm.serviceName = '';
      this.newOrderForm.amount = 0;
      return;
    }
    const opt = this.catalogServicesForCreate().find((o) => o.id === value);
    if (opt) {
      this.newOrderForm.serviceName = opt.title;
      this.newOrderForm.amount = opt.defaultPrice > 0 ? opt.defaultPrice : 0;
    }
  }

  closeCreateOrderModal(): void {
    this.showCreateOrderModal.set(false);
    this.guestPaymentLink.set(null);
    if (this.userEmailSuggestTimer != null) {
      clearTimeout(this.userEmailSuggestTimer);
      this.userEmailSuggestTimer = null;
    }
    this.userEmailSuggestRows.set([]);
  }

  copyGuestLink(): void {
    const link = this.guestPaymentLink();
    if (link) {
      this.copyGuestPaymentLink(link);
    }
  }

  copyGuestPaymentLink(link: string): void {
    if (!link || typeof navigator === 'undefined' || !navigator.clipboard) {
      return;
    }
    void navigator.clipboard.writeText(link).then(() => {
      this.showToast('success', 'Lien de paiement copié dans le presse-papiers');
    });
  }

  createOrder(): void {
    if (!this.newOrderForm.serviceSelection) {
      this.showToast('error', 'Choisissez un service dans la liste ou « Autre »');
      return;
    }
    if (this.newOrderForm.serviceSelection === CREATE_ORDER_SERVICE_OTHER) {
      if (!this.newOrderForm.serviceName?.trim()) {
        this.showToast('error', 'Indiquez le nom du service pour l’option « Autre »');
        return;
      }
    } else if (!this.newOrderForm.serviceName?.trim()) {
      this.showToast('error', 'Service invalide — rechargez la liste');
      return;
    }
    if (!this.newOrderForm.amount || this.newOrderForm.amount <= 0) {
      this.showToast('error', 'Montant obligatoire (supérieur à 0)');
      return;
    }

    const emailTrim = this.newOrderForm.userEmail.trim();
    if (!emailTrim) {
      this.showToast('error', 'Indiquez l’email du client');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailTrim)) {
      this.showToast('error', 'Adresse e-mail invalide');
      return;
    }

    this.creatingOrder.set(true);

    const cached = this.findCreateOrderUserByEmail(emailTrim);
    if (cached) {
      this.postAdminCreateOrderForUser(cached.id, cached.email);
      return;
    }

    this.http
      .get<ApiResponse<PageResponse<CreateOrderAdminUser>>>(`${environment.apiUrl}/api/v1/admin/users`, {
        params: { search: emailTrim, size: '50', page: '0' },
        withCredentials: true,
      })
      .subscribe({
        next: (res) => {
          const users = res.data?.content ?? [];
          const found = users.find((u) => u.email.toLowerCase() === emailTrim.toLowerCase());
          if (found) {
            this.postAdminCreateOrderForUser(found.id, found.email);
          } else {
            this.postAdminGuestOrderForEmail(emailTrim);
          }
        },
        error: () => {
          this.showToast('error', 'Impossible de vérifier l’utilisateur');
          this.creatingOrder.set(false);
        },
      });
  }

  /** Commande sans compte : lien invité ; {@code clientEmail} sert à l’invitation e-mail côté API. */
  private postAdminGuestOrderForEmail(clientEmail: string): void {
    this.http
      .post<ApiResponse<{ paymentLink?: string }>>(
        `${environment.apiUrl}/api/v1/admin/orders/guest`,
        {
          serviceName: this.newOrderForm.serviceName.trim(),
          amount: this.newOrderForm.amount,
          currency: 'EUR',
          notes: this.newOrderForm.notes,
          guestEmail: clientEmail,
        },
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          this.creatingOrder.set(false);
          const link = (res.data as { paymentLink?: string } | undefined)?.paymentLink;
          if (res.success && link) {
            this.guestPaymentLink.set(link);
            const data = res.data as {
              guestEmailSendFailed?: boolean;
              guestEmailInvalid?: boolean;
            } | undefined;
            const variant =
              data?.guestEmailSendFailed || data?.guestEmailInvalid ? 'error' : 'success';
            this.showToast(variant, res.message || 'Commande invité créée.');
            this.loadOrders();
          } else {
            this.showToast('error', res.message || 'Erreur lors de la création');
          }
        },
        error: (err: { error?: { message?: string } }) => {
          this.creatingOrder.set(false);
          this.showToast('error', err.error?.message || 'Erreur lors de la création');
        },
      });
  }

  /** Correspondance exacte (insensible à la casse) dans la liste récente + dernières suggestions. */
  private findCreateOrderUserByEmail(email: string): CreateOrderAdminUser | undefined {
    const want = email.trim().toLowerCase();
    if (!want) {
      return undefined;
    }
    const pool = [...this.createOrderUsersList(), ...this.userEmailSuggestRows()];
    return pool.find((u) => u.email.toLowerCase() === want);
  }

  private postAdminCreateOrderForUser(userId: string, displayEmail: string): void {
    this.http
      .post<ApiResponse<unknown>>(
        `${environment.apiUrl}/api/v1/admin/orders`,
        {
          userId,
          serviceName: this.newOrderForm.serviceName.trim(),
          amount: this.newOrderForm.amount,
          notes: this.newOrderForm.notes,
        },
        { withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.showToast('success', 'Commande créée pour ' + displayEmail);
          this.closeCreateOrderModal();
          this.creatingOrder.set(false);
          this.newOrderForm = {
            userEmail: '',
            serviceSelection: '',
            serviceName: '',
            amount: 0,
            notes: '',
          };
          this.loadOrders();
        },
        error: (err: { error?: { message?: string } }) => {
          this.showToast('error', err.error?.message || 'Erreur lors de la création');
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

  // ========== Filter helpers ==========

  hasActiveOrderFilters(): boolean {
    return !!(this.emailFilter || this.statusFilter);
  }

  clearOrderFilters(): void {
    this.emailFilter = '';
    this.statusFilter = '';
    this.currentPage.set(0);
    this.loadOrders();
  }

  // ========== Sort ==========

  applySort(key: string): void {
    if (this.currentSort() === key) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.currentSort.set(key);
      this.sortDirection.set('asc');
    }
    this.showSortMenu.set(false);
    this.applySortToOrders();
  }

  private applySortToOrders(): void {
    const key = this.currentSort();
    const dir = this.sortDirection() === 'asc' ? 1 : -1;
    const sorted = [...this.filteredOrders()].sort((a, b) => {
      if (key === 'amount') {
        return ((a.amount || 0) - (b.amount || 0)) * dir;
      }
      let va = '';
      let vb = '';
      switch (key) {
        case 'customerEmail': va = a.customerEmail || ''; vb = b.customerEmail || ''; break;
        case 'serviceName': va = a.serviceName || ''; vb = b.serviceName || ''; break;
        case 'status': va = a.status || ''; vb = b.status || ''; break;
        case 'createdAt': va = a.createdAt || ''; vb = b.createdAt || ''; break;
      }
      return va.localeCompare(vb, 'fr', { sensitivity: 'base' }) * dir;
    });
    this.filteredOrders.set(sorted);
  }

  // ========== Fraud Audit Helpers ==========

  private readonly FRAUD_FLAG_META: Record<string, { label: string; penalty: number }> = {
    VPN_DETECTED_HIGH:          { label: 'VPN détecté (haute confiance)',              penalty: -30 },
    VPN_DETECTED_MEDIUM:        { label: 'VPN détecté (confiance moyenne)',            penalty: -20 },
    VPN_SUSPECTED:              { label: 'VPN suspecté',                               penalty: -10 },
    TZ_MISMATCH:                { label: 'Timezone incohérente avec le pays IP',       penalty: -10 },
    BILLING_IP_MISMATCH:        { label: 'Pays facturation ≠ pays IP',                 penalty: -15 },
    GEO_BILLING_MISMATCH:       { label: 'Géolocalisation ≠ pays facturation',         penalty: -20 },
    GEO_IP_MISMATCH:            { label: 'Géolocalisation ≠ pays IP',                  penalty: -10 },
    GEO_DENIED_WITH_VPN:        { label: 'Géolocalisation refusée + VPN suspecté',     penalty: -15 },
    GEO_DENIED_WITH_MISMATCH:   { label: 'Géolocalisation refusée + pays incohérents', penalty: -10 },
    CARD_BILLING_MISMATCH:      { label: 'Pays carte ≠ pays facturation',              penalty: -15 },
    VAT_BILLING_COUNTRY_MISMATCH: { label: 'Pays TVA ≠ pays facturation',              penalty: -15 },
    VAT_IP_COUNTRY_MISMATCH:    { label: 'Pays TVA ≠ pays IP',                         penalty: -10 },
    VAT_NAME_MISMATCH:          { label: 'Nom VIES ≠ nom facturation',                 penalty: -20 },
    VAT_NAME_PARTIAL_MATCH:     { label: 'Nom VIES partiellement similaire',           penalty: -5  },
    VAT_NAME_WEAK_MATCH:        { label: 'Nom VIES faiblement similaire',              penalty: -5  },
    VIES_UNAVAILABLE:           { label: 'Service VIES indisponible',                  penalty: -10 },
  };

  getFraudFlagLabel(key: string): string {
    return this.FRAUD_FLAG_META[key]?.label ?? key;
  }

  getFraudFlagPenalty(key: string): number {
    return this.FRAUD_FLAG_META[key]?.penalty ?? 0;
  }

  private showToast(type: 'success' | 'error', message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => this.toast.set(null), 3000);
  }
}
