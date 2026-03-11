import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserInfo } from './auth.service';

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  companyName?: string;
  city?: string;
  country?: string;
  address?: string;
  postalCode?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  updateProfile(request: UpdateProfileRequest): Observable<UserInfo> {
    return this.http
      .put<ApiResponse<UserInfo>>(
        `${environment.apiUrl}/api/v1/dashboard/profile`,
        request,
        { withCredentials: true },
      )
      .pipe(
        map((res) => {
          if (res.success && res.data) {
            return res.data;
          }
          throw new Error(res.message ?? 'Erreur lors de la mise à jour');
        }),
      );
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http
      .put<ApiResponse<void>>(
        `${environment.apiUrl}/api/v1/dashboard/password`,
        request,
        { withCredentials: true },
      )
      .pipe(
        map((res) => {
          if (!res.success) {
            throw new Error(res.message ?? 'Erreur lors du changement de mot de passe');
          }
        }),
      );
  }
}
