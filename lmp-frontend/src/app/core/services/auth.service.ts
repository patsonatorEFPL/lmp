import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';

import { getCurrentUser } from '../../../app/generated/fn/authentication/get-current-user';
import { logout as logoutFn } from '../../../app/generated/fn/authentication/logout';
import { ApiResponseUserResponse } from '../../../app/generated/models/api-response-user-response';

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
  /** Déclaration autoliquidation TVA (auto-reverse) côté client */
  vatReverseCharge?: boolean;
  vatNumber?: string;
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
   * Restaure la session depuis le cookie (JSESSIONID).
   * Promesse résolue après /auth/me pour que {@link provideAppInitializer} bloque le bootstrap.
   */
  checkSession(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      // SSR : ne pas passer loading à false sans session — sinon la navbar rend
      // « Connexion » dans le HTML, puis le client restaure l’utilisateur → flash.
      // Garder loading à true (état initial) affiche le squelette jusqu’à l’hydratation + /me.
      return Promise.resolve();
    }

    this._loading.set(true);
    return new Promise<void>((resolve) => {
      this.http
        .get<ApiResponseUserResponse>(getCurrentUser.PATH, {
          withCredentials: true,
        })
        .subscribe({
          next: (response) => {
            if (response.success && response.data) {
              this.currentUser.set(response.data as UserInfo);
            } else {
              this.currentUser.set(null);
            }
            this._loading.set(false);
            resolve();
          },
          error: (_err: HttpErrorResponse) => {
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
    return true;
  }

  logout(): void {
    this.http
      .post(logoutFn.PATH, null)
      .subscribe({
        next: () => this.clearUser(),
        error: () => this.clearUser(),
      });
  }
}
