import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { updateProfile } from '../../../app/generated/fn/user-dashboard/update-profile';
import { changePassword } from '../../../app/generated/fn/user-dashboard/change-password';
import { ApiResponseUserResponse } from '../../../app/generated/models/api-response-user-response';
import { ApiResponseVoid } from '../../../app/generated/models/api-response-void';
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
  vatReverseCharge?: boolean;
  vatNumber?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  updateProfile(request: UpdateProfileRequest): Observable<UserInfo> {
    return this.http
      .put<ApiResponseUserResponse>(updateProfile.PATH, request)
      .pipe(
        map((res) => {
          if (res.success && res.data) {
            return res.data as UserInfo;
          }
          throw new Error(res.message ?? 'Erreur lors de la mise à jour');
        }),
      );
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http
      .put<ApiResponseVoid>(changePassword.PATH, request)
      .pipe(
        map((res) => {
          if (!res.success) {
            throw new Error(res.message ?? 'Erreur lors du changement de mot de passe');
          }
        }),
      );
  }
}
