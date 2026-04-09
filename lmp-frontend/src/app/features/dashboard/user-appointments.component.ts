import { Component, DestroyRef, inject, OnInit, signal, effect, untracked } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import {
  LucideAngularModule,
  Calendar,
  Clock,
  CheckCircle,
  XCircle,
  Loader2,
  RefreshCw,
  MapPin,
  Video,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { NotificationService } from '../../core/services/notification.service';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { createListFetchLoading } from '../../core/utils/list-fetch-loading';

interface Appointment {
  id: string;
  subject: string;
  status: string;
  appointmentDate: string;
  durationMinutes: number;
  location?: string;
  notes?: string;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
}

@Component({
  selector: 'lmp-user-appointments',
  standalone: true,
  imports: [DatePipe, NgClass, LucideAngularModule, HlmButton],
  template: `
    <!-- Résumé + action (titre dans lmp-dashboard-layout) -->
    <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <p class="text-sm text-zinc-500 dark:text-zinc-400">
        {{ appointments().length }} rendez-vous au total
      </p>
      <button
        hlmBtn variant="ghost" size="icon" class="cursor-pointer sm:ml-auto"
        (click)="loadAppointments()"
      >
        <lucide-icon
          [img]="RefreshCwIcon" [size]="18"
          [ngClass]="{ 'animate-spin': loading() }"
        ></lucide-icon>
      </button>
    </div>

    @if (loading()) {
      <div class="mt-4 flex items-center justify-center py-16">
        <lucide-icon [img]="Loader2Icon" [size]="32" class="animate-spin text-(--primary)"></lucide-icon>
      </div>
    } @else if (appointments().length === 0) {
      <div class="mt-4 flex flex-col items-center justify-center py-16 text-center rounded-sm border border-(--border) bg-(--card)">
        <div class="flex h-14 w-14 items-center justify-center rounded-full bg-(--muted)">
          <lucide-icon [img]="CalendarIcon" [size]="24" class="text-(--muted-foreground)"></lucide-icon>
        </div>
        <p class="mt-4 text-sm font-medium text-(--foreground)">Aucun rendez-vous trouvé</p>
        <p class="mt-1 text-xs text-(--muted-foreground)">Vos rendez-vous apparaîtront ici.</p>
      </div>
    } @else {
      <!-- Upcoming -->
      @if (upcomingAppointments().length > 0) {
        <div class="mt-6">
          <h2 class="mb-4 text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">
            Rendez-vous à venir
            <span class="ml-2 rounded-full bg-(--primary)/10 px-2 py-0.5 text-xs font-medium text-(--primary)">
              {{ upcomingAppointments().length }}
            </span>
          </h2>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (appt of upcomingAppointments(); track appt.id) {
              <div class="rounded-sm border border-(--border) bg-(--card) p-5 transition-colors hover:border-(--primary)/20">
                <div class="flex items-start justify-between">
                  <div class="flex h-10 w-10 items-center justify-center rounded-sm bg-(--muted) text-(--primary)">
                    <lucide-icon [img]="CalendarIcon" [size]="18"></lucide-icon>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="rounded-full bg-(--muted) px-2.5 py-0.5 text-xs font-medium text-(--foreground)">
                      {{ appt.durationMinutes }} min
                    </span>
                    <span
                      class="rounded-xs px-2.5 py-0.5 text-xs font-medium"
                      [ngClass]="getStatusClass(appt.status)"
                    >
                      {{ getStatusLabel(appt.status) }}
                    </span>
                  </div>
                </div>
                <h3 class="mt-3 text-sm font-semibold text-(--foreground)">{{ appt.subject }}</h3>
                <div class="mt-2 flex items-center gap-1.5 text-xs text-(--muted-foreground)">
                  <lucide-icon [img]="ClockIcon" [size]="12"></lucide-icon>
                  {{ appt.appointmentDate | date: 'EEEE dd MMM yyyy à HH:mm' }}
                </div>
                @if (appt.notes) {
                  <p class="mt-2 text-xs text-(--muted-foreground) line-clamp-2">{{ appt.notes }}</p>
                }
              </div>
            }
          </div>
        </div>
      }

      <!-- Past -->
      @if (pastAppointments().length > 0) {
        <div class="mt-8">
          <h2 class="mb-4 text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">
            Rendez-vous passés
            <span class="ml-2 rounded-full bg-(--muted) px-2 py-0.5 text-xs font-medium text-(--muted-foreground)">
              {{ pastAppointments().length }}
            </span>
          </h2>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (appt of pastAppointments(); track appt.id) {
              <div class="rounded-sm border border-(--border) bg-(--card) p-5 opacity-70">
                <div class="flex items-start justify-between">
                  <div class="flex h-10 w-10 items-center justify-center rounded-sm bg-(--muted) text-(--muted-foreground)">
                    <lucide-icon [img]="CalendarIcon" [size]="18"></lucide-icon>
                  </div>
                  <span
                    class="rounded-xs px-2.5 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusClass(appt.status)"
                  >
                    {{ getStatusLabel(appt.status) }}
                  </span>
                </div>
                <h3 class="mt-3 text-sm font-semibold text-(--foreground)">{{ appt.subject }}</h3>
                <div class="mt-2 flex items-center gap-1.5 text-xs text-(--muted-foreground)">
                  <lucide-icon [img]="ClockIcon" [size]="12"></lucide-icon>
                  {{ appt.appointmentDate | date: 'EEEE dd MMM yyyy à HH:mm' }}
                </div>
              </div>
            }
          </div>
        </div>
      }
    }
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
  readonly upcomingAppointments = signal<Appointment[]>([]);
  readonly pastAppointments = signal<Appointment[]>([]);

  // Icons
  readonly CalendarIcon = Calendar;
  readonly ClockIcon = Clock;
  readonly CheckCircleIcon = CheckCircle;
  readonly XCircleIcon = XCircle;
  readonly Loader2Icon = Loader2;
  readonly RefreshCwIcon = RefreshCw;
  readonly MapPinIcon = MapPin;
  readonly VideoIcon = Video;

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
          // The dashboard stats endpoint returns upcomingAppointmentsList
          const stats = res.data;
          const allAppts = stats?.upcomingAppointmentsList ?? [];
          this.appointments.set(allAppts);
          this.splitAppointments(allAppts);
          this.listFetch.afterFetch();
        },
        error: () => {
          this.appointments.set([]);
          this.upcomingAppointments.set([]);
          this.pastAppointments.set([]);
          this.listFetch.afterFetch();
        },
      });
  }

  private splitAppointments(appts: Appointment[]): void {
    const now = new Date();
    const upcoming: Appointment[] = [];
    const past: Appointment[] = [];

    for (const appt of appts) {
      const date = new Date(appt.appointmentDate);
      if (date >= now) {
        upcoming.push(appt);
      } else {
        past.push(appt);
      }
    }

    this.upcomingAppointments.set(upcoming);
    this.pastAppointments.set(past);
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
    const classes: Record<string, string> = {
      PENDING: 'bg-(--muted) text-(--foreground)',
      CONFIRMED: 'bg-(--muted) text-(--foreground)',
      COMPLETED: 'bg-(--muted) text-(--foreground)',
      CANCELLED: 'bg-red-500/10 text-red-500',
      NO_SHOW: 'bg-slate-500/10 text-slate-500',
    };
    return classes[status] || 'bg-slate-500/10 text-slate-500';
  }
}
