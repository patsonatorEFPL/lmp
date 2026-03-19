import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ServiceItem } from './catalog.service';

export interface CategoryItem {
  id: string;
  name: string;
  slug: string;
  description: string;
  icon: string;
  displayOrder: number;
}

export interface CatalogStats {
  totalCategories: number;
  totalServices: number;
  totalOffers: number;
  activeServices: number;
  featuredServices: number;
}

export interface AdminDashboardStats {
  totalUsers: number;
  activeUsers: number;
  totalOrders: number;
  totalAppointments: number;
  ordersByStatus: Record<string, { count: number; revenue: number }>;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/admin`;

  // ========== Dashboard Stats ==========

  getDashboardStats(): Observable<AdminDashboardStats> {
    return this.http
      .get<ApiResponse<AdminDashboardStats>>(`${this.baseUrl}/stats`, {
        withCredentials: true,
      })
      .pipe(map((res) => res.data!));
  }

  // ========== Catalog Stats ==========

  getCatalogStats(): Observable<CatalogStats> {
    return this.http
      .get<ApiResponse<CatalogStats>>(`${this.baseUrl}/services/stats`, {
        withCredentials: true,
      })
      .pipe(map((res) => res.data!));
  }

  // ========== Categories ==========

  getCategories(): Observable<CategoryItem[]> {
    return this.http
      .get<ApiResponse<CategoryItem[]>>(
        `${this.baseUrl}/services/categories`,
        { withCredentials: true },
      )
      .pipe(map((res) => res.data ?? []));
  }

  createCategory(data: Partial<CategoryItem>): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/services/categories`,
      data,
      { withCredentials: true },
    );
  }

  updateCategory(
    id: string,
    data: Partial<CategoryItem>,
  ): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(
      `${this.baseUrl}/services/categories/${id}`,
      data,
      { withCredentials: true },
    );
  }

  deleteCategory(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.baseUrl}/services/categories/${id}`,
      { withCredentials: true },
    );
  }

  // ========== Reorder ==========

  reorderServices(serviceIds: string[]): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(
      `${this.baseUrl}/services/reorder`,
      { serviceIds },
      { withCredentials: true },
    );
  }

  // ========== Services ==========

  getServices(): Observable<ServiceItem[]> {
    return this.http
      .get<ApiResponse<ServiceItem[]>>(`${this.baseUrl}/services`, {
        withCredentials: true,
      })
      .pipe(map((res) => res.data ?? []));
  }

  getService(id: string): Observable<any> {
    return this.http
      .get<ApiResponse<any>>(`${this.baseUrl}/services/${id}`, {
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  createService(data: any): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/services`,
      data,
      { withCredentials: true },
    );
  }

  updateService(id: string, data: any): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(
      `${this.baseUrl}/services/${id}`,
      data,
      { withCredentials: true },
    );
  }

  deleteService(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.baseUrl}/services/${id}`,
      { withCredentials: true },
    );
  }

  // ========== Benefits ==========

  syncBenefits(
    serviceId: string,
    benefits: string[],
  ): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.baseUrl}/services/${serviceId}/benefits/sync`,
      { benefits },
      { withCredentials: true },
    );
  }

  // ========== Offers ==========

  createOffer(serviceId: string, data: any): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/services/${serviceId}/offers`,
      data,
      { withCredentials: true },
    );
  }

  updateOffer(offerId: string, data: any): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(
      `${this.baseUrl}/services/offers/${offerId}`,
      data,
      { withCredentials: true },
    );
  }

  deleteOffer(offerId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.baseUrl}/services/offers/${offerId}`,
      { withCredentials: true },
    );
  }
}
