import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { getDashboardStats1 } from '../../../app/generated/fn/admin/get-dashboard-stats-1';
import { getCatalogStats } from '../../../app/generated/fn/admin-services/get-catalog-stats';
import { getCategories } from '../../../app/generated/fn/admin-services/get-categories';
import { createCategory } from '../../../app/generated/fn/admin-services/create-category';
import { updateCategory } from '../../../app/generated/fn/admin-services/update-category';
import { deleteCategory } from '../../../app/generated/fn/admin-services/delete-category';
import { reorderServices } from '../../../app/generated/fn/admin-services/reorder-services';
import { getAllServices } from '../../../app/generated/fn/admin-services/get-all-services';
import { getService } from '../../../app/generated/fn/admin-services/get-service';
import { createService } from '../../../app/generated/fn/admin-services/create-service';
import { updateService } from '../../../app/generated/fn/admin-services/update-service';
import { deleteService } from '../../../app/generated/fn/admin-services/delete-service';
import { syncBenefits } from '../../../app/generated/fn/admin-services/sync-benefits';
import { createOffer } from '../../../app/generated/fn/admin-services/create-offer';
import { updateOffer } from '../../../app/generated/fn/admin-services/update-offer';
import { deleteOffer } from '../../../app/generated/fn/admin-services/delete-offer';

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

/** Adresse / ville-province (factures PDF, éditable admin) */
export interface CompanyAddressPayload {
  addressLine: string;
  cityRegion: string;
}

const COMPANY_PROFILE_PATH = '/api/v1/admin/company-profile';

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  // ========== Dashboard Stats ==========

  getDashboardStats(): Observable<AdminDashboardStats> {
    return this.http
      .get<ApiResponse<AdminDashboardStats>>(getDashboardStats1.PATH)
      .pipe(map((res) => res.data!));
  }

  // ========== Catalog Stats ==========

  getCatalogStats(): Observable<CatalogStats> {
    return this.http
      .get<ApiResponse<CatalogStats>>(getCatalogStats.PATH)
      .pipe(map((res) => res.data!));
  }

  // ========== Categories ==========

  getCategories(): Observable<CategoryItem[]> {
    return this.http
      .get<ApiResponse<CategoryItem[]>>(getCategories.PATH)
      .pipe(map((res) => res.data ?? []));
  }

  createCategory(data: Partial<CategoryItem>): Observable<ApiResponse<unknown>> {
    return this.http.post<ApiResponse<unknown>>(createCategory.PATH, data);
  }

  updateCategory(id: string, data: Partial<CategoryItem>): Observable<ApiResponse<void>> {
    const path = updateCategory.PATH.replace('{id}', id);
    return this.http.put<ApiResponse<void>>(path, data);
  }

  deleteCategory(id: string): Observable<ApiResponse<void>> {
    const path = deleteCategory.PATH.replace('{id}', id);
    return this.http.delete<ApiResponse<void>>(path);
  }

  // ========== Reorder ==========

  reorderServices(serviceIds: string[]): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(reorderServices.PATH, { serviceIds });
  }

  // ========== Services ==========

  getServices(): Observable<ServiceItem[]> {
    return this.http
      .get<ApiResponse<ServiceItem[]>>(getAllServices.PATH)
      .pipe(map((res) => res.data ?? []));
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  getService(id: string): Observable<any> {
    const path = getService.PATH.replace('{id}', id);
    return this.http
      .get<ApiResponse<any>>(path)
      .pipe(map((res) => res.data));
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  createService(data: any): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(createService.PATH, data);
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  updateService(id: string, data: any): Observable<ApiResponse<void>> {
    const path = updateService.PATH.replace('{id}', id);
    return this.http.put<ApiResponse<void>>(path, data);
  }

  deleteService(id: string): Observable<ApiResponse<void>> {
    const path = deleteService.PATH.replace('{id}', id);
    return this.http.delete<ApiResponse<void>>(path);
  }

  // ========== Benefits ==========

  syncBenefits(serviceId: string, benefits: string[]): Observable<ApiResponse<void>> {
    const path = syncBenefits.PATH.replace('{serviceId}', serviceId);
    return this.http.post<ApiResponse<void>>(path, { benefits });
  }

  // ========== Offers ==========

  createOffer(serviceId: string, data: unknown): Observable<ApiResponse<unknown>> {
    const path = createOffer.PATH.replace('{serviceId}', serviceId);
    return this.http.post<ApiResponse<unknown>>(path, data);
  }

  updateOffer(offerId: string, data: unknown): Observable<ApiResponse<void>> {
    const path = updateOffer.PATH.replace('{offerId}', offerId);
    return this.http.put<ApiResponse<void>>(path, data);
  }

  deleteOffer(offerId: string): Observable<ApiResponse<void>> {
    const path = deleteOffer.PATH.replace('{offerId}', offerId);
    return this.http.delete<ApiResponse<void>>(path);
  }

  // ========== Profil entreprise (adresse facturation) ==========

  getCompanyAddress(): Observable<CompanyAddressPayload> {
    return this.http
      .get<ApiResponse<CompanyAddressPayload>>(COMPANY_PROFILE_PATH)
      .pipe(map((res) => res.data!));
  }

  updateCompanyAddress(body: CompanyAddressPayload): Observable<CompanyAddressPayload> {
    return this.http
      .put<ApiResponse<CompanyAddressPayload>>(COMPANY_PROFILE_PATH, body)
      .pipe(map((res) => res.data!));
  }
}
