import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { getDashboardStats } from '../../../app/generated/fn/user-dashboard/get-dashboard-stats';
import { ApiResponseDashboardStatsResponse } from '../../../app/generated/models/api-response-dashboard-stats-response';

export interface RecentOrder {
  id: string;
  serviceName: string;
  status: string;
  totalAmount: number;
  currency: string;
  createdAt: string;
}

export interface RecentReview {
  id: string;
  rating: number;
  comment: string;
  approved: boolean;
  createdAt: string;
}

export interface UpcomingAppointment {
  id: string;
  subject: string;
  status: string;
  appointmentDate: string;
  durationMinutes: number;
}

export interface DashboardStats {
  totalOrders: number;
  completedOrders: number;
  inProgressOrders: number;
  totalReviews: number;
  upcomingAppointments: number;
  totalSpent: number;
  recentOrders: RecentOrder[];
  recentReviews: RecentReview[];
  upcomingAppointmentsList: UpcomingAppointment[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  getStats(): Observable<DashboardStats> {
    return this.http
      .get<ApiResponseDashboardStatsResponse>(getDashboardStats.PATH)
      .pipe(
        map((res) => {
          if (res.success && res.data) {
            return res.data as DashboardStats;
          }
          throw new Error(res.message ?? 'Failed to load dashboard stats');
        }),
      );
  }
}
