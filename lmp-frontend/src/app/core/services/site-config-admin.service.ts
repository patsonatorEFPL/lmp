import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SiteConfigEntryDto {
  key: string;
  value: string;
  description?: string;
  updatedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class SiteConfigAdminService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<ApiResponse<Record<string, string>>> {
    return this.http.get<ApiResponse<Record<string, string>>>('/api/v1/admin/site-config');
  }

  update(key: string, value: string, description?: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`/api/v1/admin/site-config/${key}`, { value, description });
  }

  reloadFile(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>('/api/v1/admin/site-config/reload-file', {});
  }

  reloadAll(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>('/api/v1/admin/site-config/reload-all', {});
  }
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}
