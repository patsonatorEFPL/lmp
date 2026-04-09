import { Component, computed, DestroyRef, inject, OnInit, signal, effect, untracked } from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import {
  LucideAngularModule,
  Calendar,
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
  User,
  Mail,
  Phone,
  MessageSquare,
  Trash2,
  Filter,
  ArrowUpDown,
  AlertTriangle,
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AdminSseService } from '../../core/services/admin-sse.service';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { createListFetchLoading } from '../../core/utils/list-fetch-loading';

interface AppointmentItem {
  id: string;
  clientName: string;
  clientEmail: string;
  clientPhone: string;
  appointmentDate: string;
  subject: string;
  description: string | null;
  status: string;
  durationMinutes: number;
  priority: number;
  createdAt: string;
  confirmedAt: string | null;
}

interface AppointmentDetail {
  id: string;
  clientName: string;
  clientEmail: string;
  clientPhone: string;
  appointmentDate: string;
  subject: string;
  description: string | null;
  status: string;
  adminNotes: string | null;
  durationMinutes: number;
  priority: number;
  createdAt: string;
  confirmedAt: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  isAnonymous: boolean;
  userId: string | null;
  userName: string | null;
  userEmail: string | null;
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

@Component({
  selector: 'lmp-admin-appointments',
  standalone: true,
  imports: [NgClass, DatePipe, FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="crm-list-view flex h-full flex-col overflow-hidden bg-white">
    <!-- Toolbar -->
    <div class="flex items-center justify-between gap-2 px-5 py-4">
      <div class="flex items-center"></div>
      <!-- Actions droite -->
      <div class="flex items-center gap-0.5">
        <button
          hlmBtn variant="ghost" size="icon" type="button"
          class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
          title="Actualiser"
          (click)="loadAppointments()"
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

    <!-- Panneau de filtres (toggle) -->
    @if (showFilterPanel()) {
      <div class="flex items-center gap-2 border-b border-zinc-100 px-5 pb-3 dark:border-zinc-800">
        <input
          type="text"
          [(ngModel)]="emailFilter"
          (input)="filterAppointments()"
          placeholder="Rechercher par email ou nom…"
          class="h-8 w-56 rounded-lg border border-zinc-200 bg-zinc-50 px-3 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
        />
        <input
          type="text"
          [(ngModel)]="subjectFilter"
          (input)="filterAppointments()"
          placeholder="Service / sujet"
          class="hidden h-8 w-40 rounded-lg border border-zinc-200 bg-zinc-50 px-3 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none focus:border-zinc-300 focus:bg-white sm:block dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
        />
        <select
          [(ngModel)]="statusFilter"
          (change)="currentPage.set(0); loadAppointments()"
          class="h-8 w-36 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
        >
          <option value="">Tous les statuts</option>
          <option value="PENDING">En attente</option>
          <option value="CONFIRMED">Confirmés</option>
          <option value="IN_PROGRESS">En cours</option>
          <option value="COMPLETED">Terminés</option>
          <option value="CANCELLED">Annulés</option>
          <option value="NO_SHOW">Absences</option>
        </select>
        @if (hasActiveFilters()) {
          <button
            type="button"
            class="flex h-8 cursor-pointer items-center gap-1 rounded-lg px-2 text-sm text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800"
            (click)="clearFilters()"
          >
            <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
            Effacer
          </button>
        }
      </div>
    }

    <!-- Liste -->
    <div class="flex-1 overflow-auto px-3 sm:px-5">
      @if (loading()) {
        <div class="flex items-center justify-center py-16">
          <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-zinc-400"></lucide-icon>
        </div>
      } @else {
        <!-- En-tête colonnes -->
        <div class="mb-2 flex items-center rounded-lg bg-zinc-100 py-1.5 text-sm font-normal leading-none text-zinc-500 dark:bg-zinc-800/70 dark:text-zinc-400">
          <div class="flex w-10 shrink-0 items-center justify-center">
            <input
              type="checkbox"
              class="h-3.5 w-3.5 cursor-pointer rounded-xs border-zinc-400 text-zinc-600 focus:ring-zinc-400"
              [checked]="allRowsSelected()"
              (change)="toggleSelectAll($event)"
            />
          </div>
          <div class="w-64 shrink-0 px-2">Email</div>
          <div class="hidden w-48 shrink-0 px-2 sm:block">Service</div>
          <div class="hidden w-40 shrink-0 px-2 md:block">Date</div>
          <div class="hidden w-28 shrink-0 px-2 md:block">Statut</div>
          <div class="w-32 shrink-0 px-2 text-right">Last Modified</div>
        </div>

        <!-- Lignes -->
        <div>
          @for (appt of filteredAppointments(); track appt.id) {
            <div
              class="group flex h-10 cursor-pointer items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
              (click)="viewDetail(appt.id)"
            >
              <div class="flex w-10 shrink-0 items-center justify-center" (click)="$event.stopPropagation()">
                <input
                  type="checkbox"
                  class="h-3.5 w-3.5 cursor-pointer rounded-xs border-zinc-400 text-zinc-600 focus:ring-zinc-400"
                  [checked]="selectedApptIds().has(appt.id)"
                  (change)="toggleApptSelected(appt.id)"
                />
              </div>
              <div class="w-64 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">
                {{ appt.clientEmail || '—' }}
              </div>
              <div class="hidden w-48 shrink-0 truncate px-2 text-sm leading-none text-zinc-600 sm:block dark:text-zinc-400">
                {{ appt.subject }}
              </div>
              <div class="hidden w-40 shrink-0 truncate px-2 text-sm leading-none text-zinc-600 md:block dark:text-zinc-400">
                {{ appt.appointmentDate | date:'dd/MM/yyyy HH:mm' }}
              </div>
              <div class="hidden w-28 shrink-0 px-2 md:block">
                <span
                  class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                  [ngClass]="getStatusClass(appt.status)"
                >
                  {{ getStatusLabel(appt.status) }}
                </span>
              </div>
              <div class="w-32 shrink-0 px-2 text-right text-sm leading-none text-zinc-500 dark:text-zinc-400">
                {{ formatRelativeTimeFr(appt.createdAt) }}
              </div>
            </div>
          } @empty {
            <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
              Aucun rendez-vous trouvé
            </div>
          }
        </div>
      }
    </div>

    <!-- Footer pagination -->
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
        <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ filteredAppointments().length }}</span>
        <span>of</span>
        <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ totalAppointments() }}</span>
      </div>
    </div>

    <!-- Detail Modal -->
    @if (showDetailModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-end justify-center bg-black/50 p-0 backdrop-blur-sm sm:items-center sm:p-4"
        (click)="closeDetailModal()"
        role="dialog"
        aria-modal="true"
        aria-labelledby="detail-appt-title"
      >
        <div
          class="flex max-h-[min(92dvh,760px)] w-full max-w-xl flex-col rounded-t-lg border border-(--border) bg-(--card) shadow-2xl sm:rounded-lg"
          (click)="$event.stopPropagation()"
        >
          @if (loadingDetail()) {
            <div class="flex items-center justify-center py-16">
              <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-(--primary)"></lucide-icon>
            </div>
          } @else if (detail()) {
            <!-- Modal Header -->
            <div class="shrink-0 border-b border-(--border) px-5 py-4 sm:px-6">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <h3 id="detail-appt-title" class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">
                    Détail du rendez-vous
                  </h3>
                  <p class="truncate text-xs text-(--muted-foreground)">{{ detail()!.subject }}</p>
                </div>
                <button
                  hlmBtn variant="ghost" size="icon" class="h-9 w-9 shrink-0 cursor-pointer"
                  type="button"
                  (click)="closeDetailModal()"
                  aria-label="Fermer"
                >
                  <lucide-icon [img]="XIcon" [size]="18"></lucide-icon>
                </button>
              </div>
            </div>

            <!-- Modal Body -->
            <div class="min-h-0 flex-1 space-y-5 overflow-y-auto overscroll-contain px-5 py-5 sm:px-6">
              <!-- Client info -->
              <div class="rounded-lg border border-(--border)/80 bg-(--muted)/20 px-4 py-3">
                <p class="text-[11px] font-semibold uppercase tracking-wider text-(--muted-foreground)">Client</p>
                <div class="mt-2 space-y-2">
                  <div class="flex items-center gap-2">
                    <lucide-icon [img]="UserIcon" [size]="14" class="text-(--muted-foreground)"></lucide-icon>
                    <span class="text-sm font-medium text-(--foreground)">{{ detail()!.clientName || '—' }}</span>
                    @if (detail()!.isAnonymous) {
                      <span class="rounded-full bg-(--muted) px-2 py-0.5 text-[10px] font-medium text-(--foreground)">Anonyme</span>
                    }
                  </div>
                  <div class="flex items-center gap-2">
                    <lucide-icon [img]="MailIcon" [size]="14" class="text-(--muted-foreground)"></lucide-icon>
                    <span class="text-sm text-(--foreground)">{{ detail()!.clientEmail || '—' }}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <lucide-icon [img]="PhoneIcon" [size]="14" class="text-(--muted-foreground)"></lucide-icon>
                    <span class="text-sm text-(--foreground)">{{ detail()!.clientPhone || '—' }}</span>
                  </div>
                </div>
              </div>

              <!-- Date + Durée -->
              <div class="grid grid-cols-2 gap-3">
                <div class="rounded-lg border border-(--border)/80 bg-(--muted)/20 p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Date & Heure</p>
                  <p class="mt-1 text-sm font-medium text-(--foreground)">
                    {{ detail()!.appointmentDate | date:'dd/MM/yyyy HH:mm' }}
                  </p>
                </div>
                <div class="rounded-lg border border-(--border)/80 bg-(--muted)/20 p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Durée</p>
                  <p class="mt-1 text-sm font-medium text-(--foreground)">{{ detail()!.durationMinutes }} min</p>
                </div>
              </div>

              @if (detail()!.description) {
                <div>
                  <p class="text-xs font-medium text-(--muted-foreground) mb-1">Message du client</p>
                  <p class="text-sm text-(--foreground) rounded-lg border border-(--border)/80 bg-(--muted)/20 p-3">
                    {{ detail()!.description }}
                  </p>
                </div>
              }

              <div class="h-px bg-(--border)"></div>

              <!-- Editable fields -->
              <div class="space-y-3">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-(--foreground)">Statut</label>
                  <select
                    [(ngModel)]="editForm.status"
                    class="w-full rounded-md border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none transition-shadow focus:border-(--primary)/50 focus:ring-2 focus:ring-(--primary)/20 cursor-pointer"
                  >
                    <option value="PENDING">En attente</option>
                    <option value="CONFIRMED">Confirmé</option>
                    <option value="IN_PROGRESS">En cours</option>
                    <option value="COMPLETED">Terminé</option>
                    <option value="CANCELLED">Annulé</option>
                    <option value="NO_SHOW">Absence</option>
                  </select>
                </div>

                <div>
                  <label class="mb-1.5 block text-sm font-medium text-(--foreground)">
                    <lucide-icon [img]="MessageSquareIcon" [size]="12" class="inline mr-1"></lucide-icon>
                    Notes admin
                  </label>
                  <textarea
                    [(ngModel)]="editForm.adminNotes"
                    rows="3"
                    class="w-full rounded-md border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none transition-shadow focus:border-(--primary)/50 focus:ring-2 focus:ring-(--primary)/20"
                    placeholder="Notes internes..."
                  ></textarea>
                </div>

                @if (editForm.status === 'CANCELLED') {
                  <div>
                    <label class="mb-1.5 block text-sm font-medium text-(--foreground)">Raison d'annulation</label>
                    <input
                      [(ngModel)]="editForm.cancellationReason"
                      type="text"
                      class="w-full rounded-md border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none transition-shadow focus:border-(--primary)/50 focus:ring-2 focus:ring-(--primary)/20"
                      placeholder="Raison..."
                    />
                  </div>
                }
              </div>
            </div>

            <!-- Modal Footer -->
            <div class="shrink-0 border-t border-(--border) bg-(--card) px-5 py-4 sm:px-6">
              <div class="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
                <button
                  hlmBtn variant="ghost" size="sm"
                  class="cursor-pointer gap-1.5 text-red-600 hover:bg-red-500/10 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                  type="button"
                  (click)="deleteAppointment()"
                >
                  <lucide-icon [img]="AlertTriangleIcon" [size]="14"></lucide-icon>
                  Supprimer définitivement
                </button>
                <div class="flex w-full justify-end gap-2 sm:w-auto">
                  <button
                    hlmBtn variant="outline" size="sm" class="min-h-10 flex-1 cursor-pointer sm:flex-initial"
                    type="button"
                    (click)="closeDetailModal()"
                  >
                    Annuler
                  </button>
                  <button
                    hlmBtn variant="default" size="sm" class="min-h-10 min-w-[7.5rem] flex-1 cursor-pointer gap-2 sm:flex-initial"
                    type="button"
                    [disabled]="saving()"
                    (click)="saveAppointment()"
                  >
                    @if (saving()) {
                      <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                    } @else {
                      <lucide-icon [img]="SaveIcon" [size]="14"></lucide-icon>
                    }
                    Enregistrer
                  </button>
                </div>
              </div>
            </div>
          }
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
export class AdminAppointmentsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly adminSse = inject(AdminSseService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly visiblePoll = inject(VisiblePollService);

  constructor() {
    effect(() => {
      const badge = this.adminSse.badgeAppointments();
      if (badge > 0) {
        untracked(() => this.loadAppointments({ silent: true }));
      }
    });
  }

  readonly CalendarIcon = Calendar;
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
  readonly UserIcon = User;
  readonly MailIcon = Mail;
  readonly PhoneIcon = Phone;
  readonly MessageSquareIcon = MessageSquare;
  readonly Trash2Icon = Trash2;
  readonly FilterIcon = Filter;
  readonly ArrowUpDownIcon = ArrowUpDown;
  readonly AlertTriangleIcon = AlertTriangle;

  readonly loading = signal(false);
  private readonly listFetch = createListFetchLoading(this.loading);
  readonly loadingDetail = signal(false);
  readonly saving = signal(false);
  readonly appointments = signal<AppointmentItem[]>([]);
  readonly filteredAppointments = signal<AppointmentItem[]>([]);
  readonly totalAppointments = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly showDetailModal = signal(false);
  readonly detail = signal<AppointmentDetail | null>(null);
  readonly toast = signal<{ type: 'success' | 'error'; message: string } | null>(null);
  readonly selectedApptIds = signal<Set<string>>(new Set());

  readonly pageSizes = [20, 50, 100];
  readonly pageSize = signal(20);

  // Filter & Sort
  readonly showFilterPanel = signal(false);
  readonly showSortMenu = signal(false);
  readonly currentSort = signal<string>('appointmentDate');
  readonly sortDirection = signal<'asc' | 'desc'>('desc');
  readonly sortOptions = [
    { key: 'clientEmail', label: 'Email' },
    { key: 'subject', label: 'Service / Sujet' },
    { key: 'appointmentDate', label: 'Date du rendez-vous' },
    { key: 'createdAt', label: 'Date de création' },
    { key: 'status', label: 'Statut' },
  ];

  emailFilter = '';
  subjectFilter = '';
  statusFilter = '';
  editForm = { status: 'PENDING', adminNotes: '', cancellationReason: '' };

  readonly allRowsSelected = computed(() => {
    const list = this.filteredAppointments();
    if (list.length === 0) return false;
    const sel = this.selectedApptIds();
    return list.every((a) => sel.has(a.id));
  });

  ngOnInit(): void {
    this.loadAppointments();
    this.visiblePoll.subscribeWhileVisible(
      this.destroyRef,
      environment.dashboardPollIntervalMs,
      () => this.loadAppointments({ silent: true }),
    );
  }

  loadAppointments(options?: { silent?: boolean }): void {
    const silent = options?.silent === true;
    this.listFetch.beforeFetch(silent);
    const params: Record<string, string> = {
      page: this.currentPage().toString(),
      size: this.pageSize().toString(),
    };
    if (this.statusFilter) params['status'] = this.statusFilter;

    this.http
      .get<ApiResponse<PageResponse<AppointmentItem>>>(
        `${environment.apiUrl}/api/v1/admin/appointments`,
        { params, withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          const page = res.data!;
          this.appointments.set(page.content);
          this.selectedApptIds.set(new Set());
          this.filterAppointments();
          this.totalAppointments.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.listFetch.afterFetch();
        },
        error: () => {
          this.listFetch.afterFetch();
        },
      });
  }

  filterAppointments(): void {
    const q = this.emailFilter.toLowerCase();
    const subj = this.subjectFilter.toLowerCase();

    let filtered = this.appointments();

    if (q) {
      filtered = filtered.filter(
        (a) =>
          (a.clientEmail || '').toLowerCase().includes(q) ||
          (a.clientName || '').toLowerCase().includes(q),
      );
    }

    if (subj) {
      filtered = filtered.filter(
        (a) => (a.subject || '').toLowerCase().includes(subj),
      );
    }

    this.filteredAppointments.set(filtered);
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadAppointments();
  }

  toggleSelectAll(ev: Event): void {
    const checked = (ev.target as HTMLInputElement).checked;
    const list = this.filteredAppointments();
    if (checked) {
      this.selectedApptIds.set(new Set(list.map((a) => a.id)));
    } else {
      this.selectedApptIds.set(new Set());
    }
  }

  toggleApptSelected(id: string): void {
    const next = new Set(this.selectedApptIds());
    if (next.has(id)) next.delete(id);
    else next.add(id);
    this.selectedApptIds.set(next);
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
    this.loadAppointments();
  }

  viewDetail(id: string): void {
    this.loadingDetail.set(true);
    this.showDetailModal.set(true);

    this.http
      .get<ApiResponse<AppointmentDetail>>(
        `${environment.apiUrl}/api/v1/admin/appointments/${id}`,
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          const d = res.data!;
          this.detail.set(d);
          this.editForm = {
            status: d.status || 'PENDING',
            adminNotes: d.adminNotes || '',
            cancellationReason: d.cancellationReason || '',
          };
          this.loadingDetail.set(false);
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
    this.detail.set(null);
  }

  saveAppointment(): void {
    const d = this.detail();
    if (!d) return;

    this.saving.set(true);
    const payload: Record<string, unknown> = {
      status: this.editForm.status,
      adminNotes: this.editForm.adminNotes,
    };
    if (this.editForm.status === 'CANCELLED' && this.editForm.cancellationReason) {
      payload['cancellationReason'] = this.editForm.cancellationReason;
    }

    this.http
      .put<ApiResponse<void>>(
        `${environment.apiUrl}/api/v1/admin/appointments/${d.id}`,
        payload,
        { withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.showToast('success', 'Rendez-vous mis à jour');
          this.saving.set(false);
          this.closeDetailModal();
          this.loadAppointments();
        },
        error: () => {
          this.showToast('error', 'Erreur lors de la mise à jour');
          this.saving.set(false);
        },
      });
  }

  deleteAppointment(): void {
    const d = this.detail();
    if (!d) return;

    if (!confirm('Supprimer définitivement ce rendez-vous ?')) return;

    this.http
      .delete<ApiResponse<void>>(
        `${environment.apiUrl}/api/v1/admin/appointments/${d.id}`,
        { withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.showToast('success', 'Rendez-vous supprimé');
          this.closeDetailModal();
          this.loadAppointments();
        },
        error: () => this.showToast('error', 'Erreur lors de la suppression'),
      });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'bg-(--muted) text-(--foreground)';
      case 'COMPLETED': return 'bg-green-500/10 text-green-500';
      case 'IN_PROGRESS': return 'bg-(--muted) text-(--foreground)';
      case 'PENDING': return 'bg-yellow-500/10 text-yellow-500';
      case 'CANCELLED': return 'bg-red-500/10 text-red-500';
      case 'NO_SHOW': return 'bg-gray-500/10 text-gray-400';
      default: return 'bg-gray-500/10 text-gray-400';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'Confirmé';
      case 'COMPLETED': return 'Terminé';
      case 'IN_PROGRESS': return 'En cours';
      case 'PENDING': return 'En attente';
      case 'CANCELLED': return 'Annulé';
      case 'NO_SHOW': return 'Absence';
      default: return status;
    }
  }

  // ========== Filter helpers ==========

  hasActiveFilters(): boolean {
    return !!(this.emailFilter || this.subjectFilter || this.statusFilter);
  }

  clearFilters(): void {
    this.emailFilter = '';
    this.subjectFilter = '';
    this.statusFilter = '';
    this.currentPage.set(0);
    this.loadAppointments();
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
    this.applySortToList();
  }

  private applySortToList(): void {
    const key = this.currentSort();
    const dir = this.sortDirection() === 'asc' ? 1 : -1;
    const sorted = [...this.filteredAppointments()].sort((a, b) => {
      let va = '';
      let vb = '';
      switch (key) {
        case 'clientEmail': va = a.clientEmail || ''; vb = b.clientEmail || ''; break;
        case 'subject': va = a.subject || ''; vb = b.subject || ''; break;
        case 'appointmentDate': va = a.appointmentDate || ''; vb = b.appointmentDate || ''; break;
        case 'createdAt': va = a.createdAt || ''; vb = b.createdAt || ''; break;
        case 'status': va = a.status || ''; vb = b.status || ''; break;
      }
      return va.localeCompare(vb, 'fr', { sensitivity: 'base' }) * dir;
    });
    this.filteredAppointments.set(sorted);
  }

  private showToast(type: 'success' | 'error', message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => this.toast.set(null), 3000);
  }
}
