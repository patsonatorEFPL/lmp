import { Component, inject, OnInit, signal } from '@angular/core';
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
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="font-display text-2xl font-bold text-(--foreground)">Mes rendez-vous</h1>
        <p class="mt-1 text-sm text-(--muted-foreground)">
          {{ appointments().length }} rendez-vous au total
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

    @if (loading()) {
      <div class="mt-8 flex items-center justify-center py-16">
        <lucide-icon [img]="Loader2Icon" [size]="32" class="animate-spin text-(--primary)"></lucide-icon>
      </div>
    } @else if (appointments().length === 0) {
      <div class="mt-8 flex flex-col items-center justify-center py-16 text-center rounded-xl border border-(--border) bg-(--card)">
        <div class="flex h-14 w-14 items-center justify-center rounded-full bg-(--muted)">
          <lucide-icon [img]="CalendarIcon" [size]="24" class="text-(--muted-foreground)"></lucide-icon>
        </div>
        <p class="mt-4 text-sm font-medium text-(--foreground)">Aucun rendez-vous trouvé</p>
        <p class="mt-1 text-xs text-(--muted-foreground)">Vos rendez-vous apparaîtront ici.</p>
      </div>
    } @else {
      <!-- Upcoming -->
      @if (upcomingAppointments().length > 0) {
        <div class="mt-8">
          <h2 class="font-display mb-4 text-lg font-bold text-(--foreground)">
            Rendez-vous à venir
            <span class="ml-2 rounded-full bg-(--primary)/10 px-2 py-0.5 text-xs font-medium text-(--primary)">
              {{ upcomingAppointments().length }}
            </span>
          </h2>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (appt of upcomingAppointments(); track appt.id) {
              <div class="rounded-xl border border-(--border) bg-(--card) p-5 transition-all hover:shadow-md hover:border-(--primary)/20">
                <div class="flex items-start justify-between">
                  <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-500/10 text-violet-500">
                    <lucide-icon [img]="CalendarIcon" [size]="18"></lucide-icon>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="rounded-full bg-blue-500/10 px-2.5 py-0.5 text-xs font-medium text-blue-500">
                      {{ appt.durationMinutes }} min
                    </span>
                    <span
                      class="rounded-full px-2.5 py-0.5 text-xs font-medium"
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
          <h2 class="font-display mb-4 text-lg font-bold text-(--foreground)">
            Rendez-vous passés
            <span class="ml-2 rounded-full bg-(--muted) px-2 py-0.5 text-xs font-medium text-(--muted-foreground)">
              {{ pastAppointments().length }}
            </span>
          </h2>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (appt of pastAppointments(); track appt.id) {
              <div class="rounded-xl border border-(--border) bg-(--card) p-5 opacity-70">
                <div class="flex items-start justify-between">
                  <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-(--muted) text-(--muted-foreground)">
                    <lucide-icon [img]="CalendarIcon" [size]="18"></lucide-icon>
                  </div>
                  <span
                    class="rounded-full px-2.5 py-0.5 text-xs font-medium"
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

  readonly loading = signal(true);
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
  }

  loadAppointments(): void {
    this.loading.set(true);
    this.http
      .get<ApiResponse<Appointment[]>>(`${environment.apiUrl}/api/v1/dashboard/stats`, { withCredentials: true })
      .subscribe({
        next: (res: any) => {
          // The dashboard stats endpoint returns upcomingAppointmentsList
          const stats = res.data;
          const allAppts = stats?.upcomingAppointmentsList ?? [];
          this.appointments.set(allAppts);
          this.splitAppointments(allAppts);
          this.loading.set(false);
        },
        error: () => {
          this.appointments.set([]);
          this.upcomingAppointments.set([]);
          this.pastAppointments.set([]);
          this.loading.set(false);
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
      PENDING: 'bg-amber-500/10 text-amber-500',
      CONFIRMED: 'bg-blue-500/10 text-blue-500',
      COMPLETED: 'bg-emerald-500/10 text-emerald-500',
      CANCELLED: 'bg-red-500/10 text-red-500',
      NO_SHOW: 'bg-slate-500/10 text-slate-500',
    };
    return classes[status] || 'bg-slate-500/10 text-slate-500';
  }
}
