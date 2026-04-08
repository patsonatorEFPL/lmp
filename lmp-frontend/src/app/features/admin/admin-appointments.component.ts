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
  Columns3,
  MoreHorizontal,
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
    <!-- Barre de filtres inline (style CRM) -->
    <div class="flex items-center justify-between gap-4 px-3 py-2.5 sm:px-5">
      <div class="flex items-center gap-2">
        <!-- Status dropdown -->
        <select
          [(ngModel)]="statusFilter"
          (change)="currentPage.set(0); loadAppointments()"
          class="h-7 cursor-pointer rounded border-none bg-transparent px-2 text-sm text-zinc-600 outline-none hover:bg-zinc-50 focus:bg-zinc-50 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:focus:bg-zinc-800"
        >
          <option value="">Statut</option>
          <option value="PENDING">En attente</option>
          <option value="CONFIRMED">Confirmés</option>
          <option value="IN_PROGRESS">En cours</option>
          <option value="COMPLETED">Terminés</option>
          <option value="CANCELLED">Annulés</option>
          <option value="NO_SHOW">Absences</option>
        </select>
        <!-- Email search -->
        <input
          type="text"
          [(ngModel)]="emailFilter"
          (input)="filterAppointments()"
          placeholder="Adresse électronique"
          class="h-7 w-44 rounded border-none bg-transparent px-2 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none hover:bg-zinc-50 focus:bg-zinc-50 dark:text-zinc-300 dark:placeholder:text-zinc-500 dark:hover:bg-zinc-800 dark:focus:bg-zinc-800"
        />
      </div>
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
          class="h-7 cursor-pointer gap-1.5 px-2 text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200"
        >
          <lucide-icon [img]="FilterIcon" [size]="14"></lucide-icon>
          <span class="text-sm">Filtre</span>
        </button>
        <button
          hlmBtn variant="ghost" size="sm" type="button"
          class="h-7 cursor-pointer gap-1.5 px-2 text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200"
        >
          <lucide-icon [img]="ArrowUpDownIcon" [size]="14"></lucide-icon>
          <span class="text-sm">Sort</span>
        </button>
        <button
          hlmBtn variant="ghost" size="sm" type="button"
          class="h-7 cursor-pointer gap-1.5 px-2 text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200"
        >
          <lucide-icon [img]="Columns3Icon" [size]="14"></lucide-icon>
          <span class="text-sm">Columns</span>
        </button>
        <button
          hlmBtn variant="ghost" size="icon" type="button"
          class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
        >
          <lucide-icon [img]="MoreHorizontalIcon" [size]="15"></lucide-icon>
        </button>
      </div>
    </div>

    <!-- Liste (style CRM) -->
    <div class="flex-1 overflow-auto px-3 sm:px-5">
      @if (loading()) {
        <div class="flex items-center justify-center py-16">
          <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-zinc-400"></lucide-icon>
        </div>
      } @else {
        <!-- En-tête colonnes -->
        <div class="flex items-center border-b border-zinc-100 py-2.5 text-sm font-normal text-zinc-500 dark:border-zinc-800 dark:text-zinc-400">
          <div class="w-10 shrink-0 pl-1">
            <input
              type="checkbox"
              class="h-4 w-4 cursor-pointer rounded border-zinc-300 text-zinc-600 focus:ring-zinc-400"
              [checked]="allRowsSelected()"
              (change)="toggleSelectAll($event)"
            />
          </div>
          <div class="min-w-[220px] flex-1 px-2">Email</div>
          <div class="hidden w-44 px-2 sm:block">Service</div>
          <div class="hidden w-40 px-2 md:block">Date</div>
          <div class="hidden w-28 px-2 md:block">Statut</div>
          <div class="w-32 px-2 text-right">Last Modified</div>
        </div>

        <!-- Lignes -->
        <div>
          @for (appt of filteredAppointments(); track appt.id) {
            <div
              class="group flex cursor-pointer items-center border-b border-zinc-50 py-3 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
              (click)="viewDetail(appt.id)"
            >
              <div class="w-10 shrink-0 pl-1" (click)="$event.stopPropagation()">
                <input
                  type="checkbox"
                  class="h-4 w-4 cursor-pointer rounded border-zinc-300 text-zinc-600 focus:ring-zinc-400"
                  [checked]="selectedApptIds().has(appt.id)"
                  (change)="toggleApptSelected(appt.id)"
                />
              </div>
              <div class="min-w-[220px] flex-1 truncate px-2 text-base text-zinc-900 dark:text-zinc-100">
                {{ appt.clientEmail || '—' }}
              </div>
              <div class="hidden w-44 truncate px-2 text-base text-zinc-600 sm:block dark:text-zinc-400">
                {{ appt.subject }}
              </div>
              <div class="hidden w-40 truncate px-2 text-base text-zinc-600 md:block dark:text-zinc-400">
                {{ appt.appointmentDate | date:'dd/MM/yyyy HH:mm' }}
              </div>
              <div class="hidden w-28 px-2 md:block">
                <span
                  class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                  [ngClass]="getStatusClass(appt.status)"
                >
                  {{ getStatusLabel(appt.status) }}
                </span>
              </div>
              <div class="w-32 px-2 text-right text-base text-zinc-500 dark:text-zinc-400">
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
      <div class="flex items-center gap-2">
        @if (totalPages() > 1) {
          <button
            type="button"
            class="flex h-7 w-7 cursor-pointer items-center justify-center rounded text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 disabled:pointer-events-none disabled:opacity-40 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
            [disabled]="currentPage() === 0"
            (click)="changePage(currentPage() - 1)"
          >
            <lucide-icon [img]="ChevronLeftIcon" [size]="14"></lucide-icon>
          </button>
        }
        <div class="flex items-center gap-1 text-sm text-zinc-500 dark:text-zinc-400">
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ filteredAppointments().length }}</span>
          <span>of</span>
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ totalAppointments() }}</span>
        </div>
        @if (totalPages() > 1) {
          <button
            type="button"
            class="flex h-7 w-7 cursor-pointer items-center justify-center rounded text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 disabled:pointer-events-none disabled:opacity-40 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
            [disabled]="currentPage() >= totalPages() - 1"
            (click)="changePage(currentPage() + 1)"
          >
            <lucide-icon [img]="ChevronRightIcon" [size]="14"></lucide-icon>
          </button>
        }
      </div>
    </div>

    <!-- Detail Modal -->
    @if (showDetailModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        (click)="closeDetailModal()"
      >
        <div
          class="mx-4 w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-sm border border-(--border) bg-(--card) shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          @if (loadingDetail()) {
            <div class="flex items-center justify-center py-16">
              <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-(--primary)"></lucide-icon>
            </div>
          } @else if (detail()) {
            <!-- Modal Header -->
            <div class="flex items-center justify-between border-b border-(--border) px-6 py-4">
              <div>
                <h3 class="text-lg font-bold text-(--foreground)">
                  Détail du rendez-vous
                </h3>
                <p class="text-xs text-(--muted-foreground)">{{ detail()!.subject }}</p>
              </div>
              <button
                hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
                (click)="closeDetailModal()"
              >
                <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
              </button>
            </div>

            <div class="px-6 py-5 space-y-5">
              <!-- Client info -->
              <div class="rounded-sm border border-(--border) bg-(--background) p-4 space-y-2">
                <p class="text-xs font-medium text-(--muted-foreground)">Client</p>
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

              <!-- Date + Status -->
              <div class="grid grid-cols-2 gap-3">
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Date & Heure</p>
                  <p class="mt-1 text-sm font-medium text-(--foreground)">
                    {{ detail()!.appointmentDate | date:'dd/MM/yyyy HH:mm' }}
                  </p>
                </div>
                <div class="rounded-sm border border-(--border) bg-(--background) p-3 text-center">
                  <p class="text-xs text-(--muted-foreground)">Durée</p>
                  <p class="mt-1 text-sm font-medium text-(--foreground)">{{ detail()!.durationMinutes }} min</p>
                </div>
              </div>

              @if (detail()!.description) {
                <div>
                  <p class="text-xs font-medium text-(--muted-foreground) mb-1">Message du client</p>
                  <p class="text-sm text-(--foreground) rounded-sm border border-(--border) bg-(--background) p-3">
                    {{ detail()!.description }}
                  </p>
                </div>
              }

              <div class="h-px bg-(--border)"></div>

              <!-- Editable fields -->
              <div class="space-y-3">
                <div>
                  <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Statut</label>
                  <select
                    [(ngModel)]="editForm.status"
                    class="w-full rounded-sm border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) cursor-pointer"
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
                  <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">
                    <lucide-icon [img]="MessageSquareIcon" [size]="12" class="inline mr-1"></lucide-icon>
                    Notes admin
                  </label>
                  <textarea
                    [(ngModel)]="editForm.adminNotes"
                    rows="3"
                    class="w-full rounded-sm border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) focus:ring-1 focus:ring-(--primary)"
                    placeholder="Notes internes..."
                  ></textarea>
                </div>

                @if (editForm.status === 'CANCELLED') {
                  <div>
                    <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Raison d'annulation</label>
                    <input
                      [(ngModel)]="editForm.cancellationReason"
                      type="text"
                      class="w-full rounded-sm border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                      placeholder="Raison..."
                    />
                  </div>
                }
              </div>
            </div>

            <!-- Modal Footer -->
            <div class="flex items-center justify-between border-t border-(--border) px-6 py-4">
              <button
                hlmBtn variant="destructive" size="sm" class="cursor-pointer gap-1.5"
                (click)="deleteAppointment()"
              >
                <lucide-icon [img]="Trash2Icon" [size]="14"></lucide-icon>
                Supprimer
              </button>
              <div class="flex items-center gap-2">
                <button
                  hlmBtn variant="outline" size="sm" class="cursor-pointer"
                  (click)="closeDetailModal()"
                >
                  Fermer
                </button>
                <button
                  hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
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
  readonly Columns3Icon = Columns3;
  readonly MoreHorizontalIcon = MoreHorizontal;

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

  emailFilter = '';
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
    if (!q) {
      this.filteredAppointments.set(this.appointments());
      return;
    }
    this.filteredAppointments.set(
      this.appointments().filter(
        (a) =>
          (a.clientEmail || '').toLowerCase().includes(q) ||
          (a.clientName || '').toLowerCase().includes(q),
      ),
    );
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

  private showToast(type: 'success' | 'error', message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => this.toast.set(null), 3000);
  }
}
