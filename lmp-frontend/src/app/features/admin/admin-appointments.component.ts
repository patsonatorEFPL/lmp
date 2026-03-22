import { Component, inject, OnInit, signal } from '@angular/core';
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
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

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
    <!-- Header -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-(--foreground)">
          Gestion des Rendez-vous
        </h1>
        <p class="mt-1 text-sm text-(--muted-foreground)">
          {{ totalAppointments() }} rendez-vous au total
        </p>
      </div>
      <button
        hlmBtn variant="ghost" size="icon" class="cursor-pointer"
        (click)="loadAppointments()"
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
        (change)="currentPage.set(0); loadAppointments()"
        class="rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none cursor-pointer"
      >
        <option value="">Tous les statuts</option>
        <option value="PENDING">En attente</option>
        <option value="CONFIRMED">Confirmés</option>
        <option value="IN_PROGRESS">En cours</option>
        <option value="COMPLETED">Terminés</option>
        <option value="CANCELLED">Annulés</option>
        <option value="NO_SHOW">Absences</option>
      </select>
    </div>

    <!-- Appointments table -->
    <div class="mt-6 overflow-x-auto rounded-sm border border-(--border) bg-(--card)">
      @if (loading()) {
        <div class="flex items-center justify-center py-12">
          <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-(--muted-foreground)"></lucide-icon>
        </div>
      } @else {
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-(--border) text-left">
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Client</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Service</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Date & Heure</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Durée</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Statut</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Créé le</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (appt of appointments(); track appt.id) {
              <tr class="border-b border-(--border) last:border-0 transition-colors hover:bg-(--accent)/50">
                <td class="px-4 py-3">
                  <div>
                    <p class="font-medium text-(--foreground)">{{ appt.clientName || '—' }}</p>
                    <p class="text-xs text-(--muted-foreground)">{{ appt.clientEmail || '—' }}</p>
                  </div>
                </td>
                <td class="px-4 py-3 text-(--foreground)">{{ appt.subject }}</td>
                <td class="px-4 py-3 text-(--foreground)">
                  {{ appt.appointmentDate | date:'dd/MM/yyyy HH:mm' }}
                </td>
                <td class="px-4 py-3 text-(--muted-foreground)">{{ appt.durationMinutes }} min</td>
                <td class="px-4 py-3">
                  <span
                    class="inline-flex rounded-xs px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusClass(appt.status)"
                  >
                    {{ getStatusLabel(appt.status) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-(--muted-foreground)">
                  {{ appt.createdAt | date:'dd/MM/yyyy' }}
                </td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-1">
                    <button
                      hlmBtn variant="ghost" size="icon"
                      class="h-8 w-8 cursor-pointer"
                      (click)="viewDetail(appt.id)"
                      title="Voir les détails"
                    >
                      <lucide-icon [img]="EyeIcon" [size]="16" class="text-(--primary)"></lucide-icon>
                    </button>
                  </div>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="7" class="px-4 py-12 text-center text-(--muted-foreground)">
                  Aucun rendez-vous trouvé
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
  `,
})
export class AdminAppointmentsComponent implements OnInit {
  private readonly http = inject(HttpClient);

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

  readonly loading = signal(false);
  readonly loadingDetail = signal(false);
  readonly saving = signal(false);
  readonly appointments = signal<AppointmentItem[]>([]);
  readonly totalAppointments = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly showDetailModal = signal(false);
  readonly detail = signal<AppointmentDetail | null>(null);
  readonly toast = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  statusFilter = '';
  editForm = { status: 'PENDING', adminNotes: '', cancellationReason: '' };

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    this.loading.set(true);
    const params: Record<string, string> = {
      page: this.currentPage().toString(),
      size: '20',
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
          this.totalAppointments.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
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
