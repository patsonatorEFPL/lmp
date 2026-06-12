import { Component, DestroyRef, inject, OnInit, signal, computed, effect, untracked } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import {
  LucideAngularModule,
  Calendar,
  Loader2,
  RefreshCw,
  Filter,
  X,
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { NotificationService } from '../../core/services/notification.service';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { createListFetchLoading } from '../../core/utils/list-fetch-loading';
import { formatRelativeTimeFr } from '../../core/utils/relative-time';
import { ApiResponse } from '../../shared/models/api.models';

interface Appointment {
  id: string;
  subject: string;
  status: string;
  appointmentDate: string;
  durationMinutes: number;
  location?: string;
  notes?: string;
}

@Component({
  selector: 'lmp-user-appointments',
  standalone: true,
  imports: [DatePipe, NgClass, FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="flex h-full flex-col overflow-hidden">
      <!-- Toolbar -->
      <div class="flex items-center justify-between gap-2 pb-4">
        <div class="flex items-center"></div>
        <div class="flex items-center gap-0.5">
          <button
            hlmBtn variant="ghost" size="icon" type="button"
            class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
            title="Actualiser" (click)="loadAppointments()"
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
        </div>
      </div>

      @if (showFilterPanel()) {
        <div class="flex items-center gap-2 border-b border-zinc-100 pb-3 dark:border-zinc-800">
          <select
            [(ngModel)]="statusFilter"
            (change)="filterAppointments()"
            class="h-8 w-44 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
          >
            <option value="">Tous les statuts</option>
            <option value="PENDING">En attente</option>
            <option value="CONFIRMED">Confirmés</option>
            <option value="COMPLETED">Terminés</option>
            <option value="CANCELLED">Annulés</option>
            <option value="NO_SHOW">Absences</option>
          </select>
          @if (statusFilter) {
            <button type="button"
              class="flex h-8 cursor-pointer items-center gap-1 rounded-lg px-2 text-sm text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800"
              (click)="statusFilter = ''; filterAppointments()"
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
          <div class="mb-2 flex min-w-max items-center rounded-lg bg-zinc-100 py-1.5 text-sm font-normal leading-none text-zinc-500 dark:bg-zinc-800/70 dark:text-zinc-400">
            <div class="w-32 shrink-0 px-2">Rendez-vous</div>
            <div class="w-72 shrink-0 px-2">Sujet</div>
            <div class="w-32 shrink-0 px-2 text-center">Statut</div>
            <div class="w-24 shrink-0 px-2 text-center">Durée</div>
            <div class="w-48 shrink-0 px-2 text-center">Date</div>
            <div class="w-32 shrink-0 px-2 text-center">Last Modified</div>
          </div>

          <div>
            @for (appt of paginated(); track appt.id) {
              <div
                class="group flex h-10 min-w-max items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
                [ngClass]="{ 'opacity-60': isPast(appt) }"
              >
                <div class="w-32 shrink-0 truncate px-2 font-mono text-xs leading-normal text-zinc-700 dark:text-zinc-300">{{ shortId(appt.id) }}</div>
                <div class="w-72 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">{{ appt.subject }}</div>
                <div class="w-32 shrink-0 px-2 text-center">
                  <span class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium" [ngClass]="getStatusClass(appt.status)">
                    {{ getStatusLabel(appt.status) }}
                  </span>
                </div>
                <div class="w-24 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ appt.durationMinutes }} min
                </div>
                <div class="w-48 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ appt.appointmentDate | date: 'dd/MM/yyyy HH:mm' }}
                </div>
                <div class="w-32 shrink-0 px-2 text-center text-sm leading-normal text-zinc-500 dark:text-zinc-400">
                  {{ formatRelativeTimeFr(appt.appointmentDate) }}
                </div>
              </div>
            } @empty {
              <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
                @if (statusFilter) { Aucun rendez-vous ne correspond à ce filtre. }
                @else { Aucun rendez-vous trouvé. Vos rendez-vous apparaîtront ici. }
              </div>
            }
          </div>
        }
      </div>

      <!-- Footer pagination -->
      <div class="flex items-center justify-between border-t border-zinc-200 py-2 dark:border-zinc-800">
        <div class="inline-flex rounded-md border border-zinc-200 dark:border-zinc-700">
          @for (size of pageSizes; track size; let first = $first; let last = $last) {
            <button type="button"
              class="h-7 min-w-[2.25rem] px-2.5 text-sm font-normal transition-colors"
              [ngClass]="{
                'rounded-l-md': first,
                'rounded-r-md': last,
                'border-r border-zinc-200 dark:border-zinc-700': !last,
                'bg-zinc-100 text-zinc-900 dark:bg-zinc-800 dark:text-zinc-100': pageSize() === size,
                'bg-white text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900 dark:bg-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200': pageSize() !== size
              }"
              (click)="changePageSize(size)"
            >{{ size }}</button>
          }
        </div>
        <div class="flex items-center gap-1 text-sm text-zinc-500 dark:text-zinc-400">
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ filtered().length }}</span>
          <span>of</span>
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ appointments().length }}</span>
        </div>
      </div>
    </div>
  `,
})
export class UserAppointmentsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly visiblePoll = inject(VisiblePollService);

  constructor() {
    effect(() => {
      const hint = this.notificationService.liveAppointmentHint();
      if (hint > 0) {
        untracked(() => this.loadAppointments({ silent: true }));
      }
    });
  }

  readonly loading = signal(true);
  private readonly listFetch = createListFetchLoading(this.loading);
  readonly appointments = signal<Appointment[]>([]);
  readonly filtered = signal<Appointment[]>([]);

  readonly pageSize = signal(20);
  readonly currentPage = signal(0);
  readonly pageSizes = [20, 50, 100];

  readonly paginated = computed(() => {
    const start = this.currentPage() * this.pageSize();
    return this.filtered().slice(start, start + this.pageSize());
  });

  readonly CalendarIcon = Calendar;
  readonly Loader2Icon = Loader2;
  readonly RefreshCwIcon = RefreshCw;
  readonly FilterIcon = Filter;
  readonly XIcon = X;

  readonly showFilterPanel = signal(false);
  statusFilter = '';

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
    this.http
      .get<ApiResponse<Appointment[]>>(`${environment.apiUrl}/api/v1/dashboard/stats`, { withCredentials: true })
      .subscribe({
        next: (res: any) => {
          const stats = res.data;
          const allAppts = stats?.upcomingAppointmentsList ?? [];
          this.appointments.set(allAppts);
          this.filterAppointments();
          this.listFetch.afterFetch();
        },
        error: () => {
          this.appointments.set([]);
          this.filtered.set([]);
          this.listFetch.afterFetch();
        },
      });
  }

  filterAppointments(): void {
    const list = this.statusFilter
      ? this.appointments().filter((a) => a.status === this.statusFilter)
      : this.appointments();
    this.filtered.set(list);
    this.currentPage.set(0);
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
  }

  isPast(appt: Appointment): boolean {
    return new Date(appt.appointmentDate) < new Date();
  }

  shortId(id: string): string {
    if (!id) return '';
    return id.length > 12 ? id.slice(0, 8).toUpperCase() : id.toUpperCase();
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente',
      CONFIRMED: 'Confirmé',
      COMPLETED: 'Terminé',
      CANCELLED: 'Annulé',
      NO_SHOW: 'Absent',
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'bg-green-500/10 text-green-500';
      case 'COMPLETED': return 'bg-(--muted) text-(--foreground)';
      case 'PENDING': return 'bg-yellow-500/10 text-yellow-500';
      case 'CANCELLED': return 'bg-red-500/10 text-red-500';
      case 'NO_SHOW': return 'bg-orange-500/10 text-orange-500';
      default: return 'bg-gray-500/10 text-gray-400';
    }
  }

  readonly formatRelativeTimeFr = formatRelativeTimeFr;
}
