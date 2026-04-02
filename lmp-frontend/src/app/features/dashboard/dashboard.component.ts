import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe, CurrencyPipe, NgClass } from '@angular/common';
import {
  LucideAngularModule,
  LayoutDashboard,
  ShoppingCart,
  Calendar,
  Settings,
  LogOut,
  User,
  Bell,
  FileText,
  Star,
  TrendingUp,
  ChevronRight,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Loader2,
  RefreshCw,
  Eye,
  X,
  MessageSquare,
  Download,
  RotateCcw,
  CreditCard,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import {
  DashboardService,
  DashboardStats,
} from '../../core/services/dashboard.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { FormsModule } from '@angular/forms';
import { NotificationPanelComponent } from '../../shared/layout/notification-panel.component';

interface OrderDetailResponse {
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

const USER_ORDER_STEPS = [
  { label: 'Commande reçue', threshold: 0 },
  { label: 'Paiement confirmé', threshold: 10 },
  { label: 'En traitement', threshold: 30 },
  { label: 'En cours', threshold: 50 },
  { label: 'Livraison', threshold: 80 },
  { label: 'Terminée', threshold: 100 },
];

@Component({
  selector: 'lmp-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    LucideAngularModule,
    HlmButton,
    DatePipe,
    CurrencyPipe,
    NgClass,
    FormsModule,
    NotificationPanelComponent,
  ],
  template: `
    <div class="min-h-screen bg-(--background)">
      <!-- Top navbar -->
      <header
        class="sticky top-0 z-40 border-b border-(--border) bg-(--card)/80 backdrop-blur-sm"
      >
        <div
          class="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8"
        >
          <div class="flex items-center gap-4">
            <a routerLink="/" class="flex items-center">
              <img
                src="/images/logo-lmp.webp"
                alt="LMP Logo"
                class="h-10 w-auto"
              />
            </a>
            <span
              class="hidden text-sm font-medium text-(--muted-foreground) sm:inline"
              >/</span
            >
            <span
              class="hidden text-sm font-semibold text-(--foreground) sm:inline"
              >Dashboard</span
            >
          </div>

          <div class="flex items-center gap-3">
            <div class="relative" (click)="$event.stopPropagation()">
              <button
                hlmBtn
                variant="ghost"
                size="icon"
                class="relative cursor-pointer"
                (click)="toggleNotificationPanel()"
              >
                <lucide-icon [img]="BellIcon" [size]="18"></lucide-icon>
                @if (notificationService.unreadCount() > 0) {
                  <span
                    class="absolute -top-0.5 -right-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white ring-2 ring-(--card) bell-badge-pulse"
                  >
                    {{ notificationService.unreadCount() > 9 ? '9+' : notificationService.unreadCount() }}
                  </span>
                }
              </button>

              <!-- Notification Panel Dropdown -->
              <lmp-notification-panel
                [isOpen]="showNotificationPanel()"
                (panelClosed)="showNotificationPanel.set(false)"
              />
            </div>

            <div
              class="flex items-center gap-2 rounded-sm border border-(--border) px-3 py-1.5"
            >
              <div
                class="flex h-7 w-7 items-center justify-center rounded-full bg-(--primary)/10 text-(--primary)"
              >
                <lucide-icon [img]="UserIcon" [size]="14"></lucide-icon>
              </div>
              <span class="text-sm font-medium text-(--foreground)">
                {{
                  authService.user()?.displayName || authService.user()?.email
                }}
              </span>
            </div>

            <button
              hlmBtn
              variant="ghost"
              size="icon"
              class="cursor-pointer text-(--muted-foreground) hover:text-(--destructive)"
              (click)="onLogout()"
            >
              <lucide-icon [img]="LogOutIcon" [size]="18"></lucide-icon>
            </button>
          </div>
        </div>
      </header>

      <!-- Main content -->
      <main class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <!-- Welcome banner -->
        <div
          class="rounded-sm border border-(--border) bg-(--card) p-6 sm:p-8"
        >
          <div class="flex items-center justify-between">
            <div>
              <h1
                class="text-2xl font-bold text-(--foreground) sm:text-3xl"
              >
                Bienvenue,
                {{ authService.user()?.firstName || 'Utilisateur' }} 👋
              </h1>
              <p class="mt-2 text-sm text-(--muted-foreground)">
                Gérez vos services, commandes et rendez-vous depuis votre espace
                personnel.
              </p>
              @if (authService.isAdmin()) {
                <a
                  routerLink="/admin"
                  class="mt-3 inline-flex items-center gap-2 rounded-sm bg-(--muted) px-4 py-2 text-sm font-medium text-(--primary) transition-colors hover:bg-(--primary)/10"
                >
                  🛡️ Accéder au panneau d'administration
                </a>
              }
            </div>
            <button
              hlmBtn
              variant="ghost"
              size="icon"
              class="cursor-pointer"
              (click)="loadStats()"
            >
              <lucide-icon
                [img]="RefreshCwIcon"
                [size]="18"
                [ngClass]="{ 'animate-spin': loading() }"
              ></lucide-icon>
            </button>
          </div>
        </div>

        <!-- Loading -->
        @if (loading()) {
          <div class="mt-8 flex items-center justify-center py-16">
            <lucide-icon
              [img]="Loader2Icon"
              [size]="32"
              class="animate-spin text-(--primary)"
            ></lucide-icon>
          </div>
        }

        @if (!loading() && stats()) {
          <!-- Quick stats -->
          <div
            class="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4"
          >
            <div
              class="rounded-sm border border-(--border) bg-(--card) p-5 transition-colors"
            >
              <div class="flex items-center justify-between">
                <span
                  class="text-xs font-medium text-(--muted-foreground)"
                  >Commandes</span
                >
                <div
                  class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--primary)/10 text-(--primary)"
                >
                  <lucide-icon [img]="ShoppingCartIcon" [size]="16"></lucide-icon>
                </div>
              </div>
              <div class="mt-3">
                <span
                  class="text-2xl font-bold text-(--foreground)"
                  >{{ stats()!.totalOrders }}</span
                >
              </div>
              <p class="mt-1 text-xs text-(--muted-foreground)">
                {{ stats()!.completedOrders }} terminée(s)
              </p>
            </div>

            <div
              class="rounded-sm border border-(--border) bg-(--card) p-5 transition-colors"
            >
              <div class="flex items-center justify-between">
                <span
                  class="text-xs font-medium text-(--muted-foreground)"
                  >En cours</span
                >
                <div
                  class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted) text-(--foreground)"
                >
                  <lucide-icon [img]="ClockIcon" [size]="16"></lucide-icon>
                </div>
              </div>
              <div class="mt-3">
                <span
                  class="text-2xl font-bold text-(--foreground)"
                  >{{ stats()!.inProgressOrders }}</span
                >
              </div>
              <p class="mt-1 text-xs text-(--muted-foreground)">
                Commandes actives
              </p>
            </div>

            <div
              class="rounded-sm border border-(--border) bg-(--card) p-5 transition-colors"
            >
              <div class="flex items-center justify-between">
                <span
                  class="text-xs font-medium text-(--muted-foreground)"
                  >Rendez-vous</span
                >
                <div
                  class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted) text-(--primary)"
                >
                  <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
                </div>
              </div>
              <div class="mt-3">
                <span
                  class="text-2xl font-bold text-(--foreground)"
                  >{{ stats()!.upcomingAppointments }}</span
                >
              </div>
              <p class="mt-1 text-xs text-(--muted-foreground)">À venir</p>
            </div>

            <div
              class="rounded-sm border border-(--border) bg-(--card) p-5 transition-colors"
            >
              <div class="flex items-center justify-between">
                <span
                  class="text-xs font-medium text-(--muted-foreground)"
                  >Avis</span
                >
                <div
                  class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted) text-(--foreground)"
                >
                  <lucide-icon [img]="StarIcon" [size]="16"></lucide-icon>
                </div>
              </div>
              <div class="mt-3">
                <span
                  class="text-2xl font-bold text-(--foreground)"
                  >{{ stats()!.totalReviews }}</span
                >
              </div>
              <p class="mt-1 text-xs text-(--muted-foreground)">Avis donnés</p>
            </div>
          </div>

          <!-- Two-column layout: Recent orders + Quick actions -->
          <div class="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
            <!-- Recent Orders (2/3 width) -->
            <div class="lg:col-span-2">
              <h2
                class="mb-4 text-lg font-bold text-(--foreground)"
              >
                Commandes récentes
              </h2>
              <div class="rounded-sm border border-(--border) bg-(--card)">
                @if (stats()!.recentOrders.length === 0) {
                  <div
                    class="flex flex-col items-center justify-center py-10 text-center"
                  >
                    <div
                      class="flex h-12 w-12 items-center justify-center rounded-full bg-(--muted)"
                    >
                      <lucide-icon
                        [img]="FileTextIcon"
                        [size]="20"
                        class="text-(--muted-foreground)"
                      ></lucide-icon>
                    </div>
                    <p class="mt-3 text-sm font-medium text-(--foreground)">
                      Aucune commande
                    </p>
                    <p class="mt-1 text-xs text-(--muted-foreground)">
                      Vos commandes apparaîtront ici.
                    </p>
                    <a
                      routerLink="/services"
                      hlmBtn
                      variant="default"
                      size="sm"
                      class="mt-4 cursor-pointer"
                    >
                      Découvrir nos services
                    </a>
                  </div>
                } @else {
                  <div class="divide-y divide-(--border)">
                    @for (
                      order of stats()!.recentOrders;
                      track order.id
                    ) {
                      <div
                        class="flex items-center justify-between p-4 transition-colors hover:bg-(--muted)/50"
                      >
                        <div class="flex items-center gap-3">
                          <div
                            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-sm"
                            [ngClass]="getStatusBgClass(order.status)"
                          >
                            <lucide-icon
                              [img]="getStatusIcon(order.status)"
                              [size]="16"
                            ></lucide-icon>
                          </div>
                          <div>
                            <p
                              class="text-sm font-medium text-(--foreground)"
                            >
                              {{ order.serviceName }}
                            </p>
                            <p class="text-xs text-(--muted-foreground)">
                              {{ order.createdAt | date: 'dd MMM yyyy' }}
                            </p>
                          </div>
                        </div>
                        <div class="flex items-center gap-3">
                          <div class="text-right">
                            <p class="text-sm font-semibold text-(--foreground)">
                              {{
                                order.totalAmount
                                  | currency
                                    : (order.currency || 'EUR')
                                    : 'symbol'
                                    : '1.2-2'
                                    : 'fr'
                              }}
                            </p>
                            <span
                              class="inline-block rounded-xs px-2 py-0.5 text-xs font-medium"
                              [ngClass]="getStatusBadgeClass(order.status)"
                            >
                              {{ getStatusLabel(order.status) }}
                            </span>
                          </div>
                          <button
                            hlmBtn variant="ghost" size="icon"
                            class="h-8 w-8 shrink-0 cursor-pointer"
                            (click)="viewOrderDetail(order.id)"
                            title="Voir les détails"
                          >
                            <lucide-icon [img]="EyeIcon" [size]="16" class="text-(--primary)"></lucide-icon>
                          </button>
                        </div>
                      </div>
                    }
                  </div>
                }
              </div>
            </div>

            <!-- Quick actions (1/3 width) -->
            <div>
              <h2
                class="mb-4 text-lg font-bold text-(--foreground)"
              >
                Actions rapides
              </h2>
              <div class="space-y-3">
                @for (action of quickActions; track action.label) {
                  <a
                    [routerLink]="action.route"
                    class="group flex items-center gap-3 rounded-sm border border-(--border) bg-(--card) p-4 transition-all hover:border-(--primary)/30  hover:-translate-y-0.5"
                  >
                    <div
                      class="flex h-10 w-10 shrink-0 items-center justify-center rounded-sm bg-(--primary)/10 text-(--primary) transition-transform 110"
                    >
                      <lucide-icon
                        [img]="action.icon"
                        [size]="18"
                      ></lucide-icon>
                    </div>
                    <div class="flex-1">
                      <h3 class="text-sm font-semibold text-(--foreground)">
                        {{ action.label }}
                      </h3>
                      <p class="text-xs text-(--muted-foreground)">
                        {{ action.description }}
                      </p>
                    </div>
                    <lucide-icon
                      [img]="ChevronRightIcon"
                      [size]="16"
                      class="text-(--muted-foreground) transition-transform "
                    ></lucide-icon>
                  </a>
                }
              </div>
            </div>
          </div>

          <!-- Upcoming Appointments -->
          @if (stats()!.upcomingAppointmentsList.length > 0) {
            <div class="mt-8">
              <h2
                class="mb-4 text-lg font-bold text-(--foreground)"
              >
                Prochains rendez-vous
              </h2>
              <div
                class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"
              >
                @for (
                  appt of stats()!.upcomingAppointmentsList;
                  track appt.id
                ) {
                  <div
                    class="rounded-sm border border-(--border) bg-(--card) p-5"
                  >
                    <div class="flex items-start justify-between">
                      <div
                        class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted) text-(--primary)"
                      >
                        <lucide-icon
                          [img]="CalendarIcon"
                          [size]="16"
                        ></lucide-icon>
                      </div>
                      <span
                        class="rounded-full bg-(--muted) px-2.5 py-0.5 text-xs font-medium text-(--foreground)"
                      >
                        {{ appt.durationMinutes }} min
                      </span>
                    </div>
                    <h3
                      class="mt-3 text-sm font-semibold text-(--foreground)"
                    >
                      {{ appt.subject }}
                    </h3>
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
              <h2
                class="mb-4 text-lg font-bold text-(--foreground)"
              >
                Vos avis récents
              </h2>
              <div class="space-y-3">
                @for (
                  review of stats()!.recentReviews;
                  track review.id
                ) {
                  <div
                    class="rounded-sm border border-(--border) bg-(--card) p-5"
                  >
                    <div class="flex items-center justify-between">
                      <div class="flex items-center gap-1">
                        @for (
                          s of [1, 2, 3, 4, 5];
                          track s
                        ) {
                          <lucide-icon
                            [img]="StarIcon"
                            [size]="14"
                            [ngClass]="{
                              'text-amber-400': s <= review.rating,
                              'text-(--muted)': s > review.rating,
                            }"
                          ></lucide-icon>
                        }
                      </div>
                      @if (review.approved) {
                        <span
                          class="rounded-full bg-(--muted) px-2.5 py-0.5 text-xs font-medium text-(--foreground)"
                          >Approuvé</span
                        >
                      } @else {
                        <span
                          class="rounded-full bg-(--muted) px-2.5 py-0.5 text-xs font-medium text-(--foreground)"
                          >En attente</span
                        >
                      }
                    </div>
                    @if (review.comment) {
                      <p
                        class="mt-2 text-sm text-(--muted-foreground)"
                      >
                        {{ review.comment }}
                      </p>
                    }
                    <p class="mt-2 text-xs text-(--muted-foreground)">
                      {{ review.createdAt | date: 'dd MMM yyyy' }}
                    </p>
                  </div>
                }
              </div>
            </div>
          }
        }
      </main>

      <!-- Order Detail Modal -->
      @if (showOrderDetail()) {
        <div
          class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
          (click)="closeOrderDetail()"
        >
          <div
            class="mx-4 w-full max-w-xl max-h-[85vh] overflow-y-auto rounded-sm border border-(--border) bg-(--card) shadow-2xl"
            (click)="$event.stopPropagation()"
          >
            @if (loadingOrderDetail()) {
              <div class="flex items-center justify-center py-16">
                <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-(--primary)"></lucide-icon>
              </div>
            } @else if (selectedOrder()) {
              <!-- Header -->
              <div class="flex items-center justify-between border-b border-(--border) px-6 py-4">
                <div>
                  <h3 class="text-lg font-bold text-(--foreground)">
                    Détail de la commande
                  </h3>
                  <p class="text-xs text-(--muted-foreground)">{{ selectedOrder()!.serviceName }}</p>
                </div>
                <button
                  hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
                  (click)="closeOrderDetail()"
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

                  <!-- Steps -->
                  <div class="space-y-2">
                    @for (step of orderSteps; track step.threshold) {
                      <div
                        class="flex items-center gap-3 rounded-sm px-3 py-2"
                        [ngClass]="(selectedOrder()!.progressPercentage || 0) >= step.threshold
                          ? 'bg-(--primary)/5'
                          : 'opacity-40'"
                      >
                        <div
                          class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-white"
                          [ngClass]="(selectedOrder()!.progressPercentage || 0) >= step.threshold
                            ? 'bg-(--primary)'
                            : 'bg-(--muted-foreground)'"
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

                <!-- Progress Message -->
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

              <!-- Refunds Section -->
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
                  (click)="closeOrderDetail()"
                >
                  Fermer
                </button>
              </div>
            }
          </div>
        </div>
      }
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  readonly authService = inject(AuthService);
  readonly notificationService = inject(NotificationService);
  private readonly dashboardService = inject(DashboardService);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);

  readonly DashboardIcon = LayoutDashboard;
  readonly UserIcon = User;
  readonly BellIcon = Bell;
  readonly LogOutIcon = LogOut;
  readonly FileTextIcon = FileText;
  readonly ChevronRightIcon = ChevronRight;
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
  readonly XIcon = X;
  readonly TrendingUpIcon = TrendingUp;
  readonly MessageSquareIcon = MessageSquare;
  readonly DownloadIcon = Download;
  readonly RotateCcwIcon = RotateCcw;
  readonly CreditCardIcon = CreditCard;

  readonly stats = signal<DashboardStats | null>(null);
  readonly loading = signal(true);
  readonly showNotificationPanel = signal(false);
  readonly showOrderDetail = signal(false);
  readonly loadingOrderDetail = signal(false);
  readonly selectedOrder = signal<OrderDetailResponse | null>(null);
  readonly orderRefunds = signal<RefundItem[]>([]);
  readonly orderSteps = USER_ORDER_STEPS;

  readonly quickActions = [
    {
      label: 'Voir les services',
      description: 'Parcourir notre catalogue',
      route: '/services',
      icon: ShoppingCart,
    },
    {
      label: 'Prendre rendez-vous',
      description: 'Planifier une consultation',
      route: '/contact',
      icon: Calendar,
    },
    {
      label: 'Paramètres',
      description: 'Gérer votre compte',
      route: '/settings',
      icon: Settings,
    },
  ];

  ngOnInit(): void {
    this.loadStats();
  }

  toggleNotificationPanel(): void {
    this.showNotificationPanel.update((v) => !v);
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
          totalOrders: 0,
          completedOrders: 0,
          inProgressOrders: 0,
          totalReviews: 0,
          upcomingAppointments: 0,
          totalSpent: 0,
          recentOrders: [],
          recentReviews: [],
          upcomingAppointmentsList: [],
        });
        this.loading.set(false);
      },
    });
  }

  onLogout(): void {
    this.notificationService.reset();
    this.authService.logout();
    this.router.navigate(['/']);
  }

  getStatusLabel(status: string): string {
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

  getStatusIcon(status: string) {
    switch (status) {
      case 'COMPLETED':
      case 'DELIVERED':
        return this.CheckCircleIcon;
      case 'CANCELLED':
      case 'REFUNDED':
        return this.XCircleIcon;
      case 'IN_PROGRESS':
      case 'PROCESSING':
        return this.ClockIcon;
      default:
        return this.AlertCircleIcon;
    }
  }

  // ========== Order Detail ==========

  viewOrderDetail(orderId: string): void {
    this.loadingOrderDetail.set(true);
    this.showOrderDetail.set(true);
    this.orderRefunds.set([]);

    this.http
      .get<{ success: boolean; data?: OrderDetailResponse }>(
        `${environment.apiUrl}/api/v1/orders/${orderId}`,
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          this.selectedOrder.set(res.data ?? null);
          this.loadingOrderDetail.set(false);

          // Load refunds for this order
          this.http
            .get<{ success: boolean; data?: RefundItem[] }>(
              `${environment.apiUrl}/api/v1/orders/${orderId}/refunds`,
              { withCredentials: true },
            )
            .subscribe({
              next: (refRes) => this.orderRefunds.set(refRes.data ?? []),
            });
        },
        error: () => {
          this.loadingOrderDetail.set(false);
          this.showOrderDetail.set(false);
        },
      });
  }

  closeOrderDetail(): void {
    this.showOrderDetail.set(false);
    this.selectedOrder.set(null);
    this.orderRefunds.set([]);
  }

  isInvoiceEligible(status: string): boolean {
    return ['CONFIRMED', 'COMPLETED', 'DELIVERED', 'PROCESSING', 'IN_PROGRESS'].includes(status);
  }

  payOrder(order: any): void {
    this.http
      .post<any>(
        `${environment.apiUrl}/api/v1/payments/checkout-order/${order.id}`,
        {},
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          if (res.data?.redirectUrl) {
            window.location.href = res.data.redirectUrl;
          }
        },
        error: () => {
          alert('Erreur lors de la création de la session de paiement.');
        },
      });
  }

  downloadInvoice(orderId: string): void {
    this.http
      .get(`${environment.apiUrl}/api/v1/orders/${orderId}/invoice`, {
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
      });
  }

  getProgressColor(percentage: number): string {
    if (percentage >= 100) return 'bg-emerald-500';
    if (percentage >= 60) return 'bg-blue-500';
    if (percentage >= 30) return 'bg-amber-500';
    return 'bg-orange-500';
  }

  getProgressTextColor(percentage: number): string {
    if (percentage >= 100) return 'text-(--foreground)';
    if (percentage >= 60) return 'text-(--foreground)';
    if (percentage >= 30) return 'text-(--foreground)';
    return 'text-orange-500';
  }
}
