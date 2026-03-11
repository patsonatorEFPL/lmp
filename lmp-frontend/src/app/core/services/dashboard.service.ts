import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

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

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  getStats(): Observable<DashboardStats> {
    return this.http
      .get<ApiResponse<DashboardStats>>(
        `${environment.apiUrl}/api/v1/dashboard/stats`,
        { withCredentials: true },
      )
      .pipe(
        map((res) => {
          if (res.success && res.data) {
            return res.data;
          }
          throw new Error(res.message ?? 'Failed to load dashboard stats');
        }),
      );
  }
}
