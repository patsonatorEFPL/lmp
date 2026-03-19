import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface UserInfo {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  displayName: string;
  roles: string[];
  emailVerified: boolean;
  companyName?: string;
  phone?: string;
  city?: string;
  country?: string;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly currentUser = signal<UserInfo | null>(null);
  private readonly _loading = signal(true);

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly loading = this._loading.asReadonly();

  /**
   * Called on app startup to restore session from JSESSIONID cookie.
   * Returns a Promise so APP_INITIALIZER waits for completion.
   */
  checkSession(): Promise<void> {
    // During SSR/prerender, skip session check — no cookies available
    if (!isPlatformBrowser(this.platformId)) {
      this._loading.set(false);
      return Promise.resolve();
    }

    this._loading.set(true);
    return new Promise<void>((resolve) => {
      this.http
        .get<ApiResponse<UserInfo>>(`${environment.apiUrl}/api/v1/auth/me`, {
          withCredentials: true,
        })
        .subscribe({
          next: (response) => {
            if (response.success && response.data) {
              this.currentUser.set(response.data);
            } else {
              this.currentUser.set(null);
            }
            this._loading.set(false);
            resolve();
          },
          error: () => {
            this.currentUser.set(null);
            this._loading.set(false);
            resolve();
          },
        });
    });
  }

  isLoggedIn(): boolean {
    return this.currentUser() !== null;
  }

  hasRole(role: string): boolean {
    return this.currentUser()?.roles?.includes(role) ?? false;
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  setUser(user: UserInfo | null): void {
    this.currentUser.set(user);
  }

  clearUser(): void {
    this.currentUser.set(null);
  }

  hasModule(_module: string): boolean {
    // TODO: implement module-based access control
    return true;
  }

  logout(): void {
    this.http
      .post(`${environment.apiUrl}/api/v1/auth/logout`, null, {
        withCredentials: true,
      })
      .subscribe({
        next: () => this.clearUser(),
        error: () => this.clearUser(),
      });
  }
}
