import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, Eye, EyeOff } from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmLabel } from '@spartan-ng/helm/label';
import { HlmSeparator } from '@spartan-ng/helm/separator';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';
import { switchMap } from 'rxjs';

@Component({
  selector: 'lmp-register',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    LucideAngularModule,
    HlmButton,
    HlmInput,
    HlmLabel,
    HlmSeparator,
  ],
  template: `
    <div class="flex min-h-screen items-center justify-center px-4 py-12">


      <div class="relative w-full max-w-md">
        <!-- Logo -->
        <div class="mb-8 flex flex-col items-center">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-sm bg-(--primary) text-(--primary-foreground)"
          >
            <span class="text-lg font-bold">L</span>
          </div>
          <h1 class="mt-4 text-2xl font-bold text-(--foreground)">
            Créer votre compte
          </h1>
          <p class="mt-1.5 text-sm text-(--muted-foreground)">
            Commencez gratuitement en quelques secondes
          </p>
        </div>

        <!-- Card -->
        <div class="rounded-sm border border-(--border) bg-(--card) p-6 sm:p-8">
          <form (ngSubmit)="onSubmit()" class="space-y-4">
            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-2">
                <label hlmLabel>Prénom</label>
                <input
                  hlmInput
                  type="text"
                  placeholder="Jean"
                  [(ngModel)]="form.firstName"
                  name="firstName"
                  required
                  autocomplete="given-name"
                />
              </div>
              <div class="space-y-2">
                <label hlmLabel>Nom</label>
                <input
                  hlmInput
                  type="text"
                  placeholder="Dupont"
                  [(ngModel)]="form.lastName"
                  name="lastName"
                  required
                  autocomplete="family-name"
                />
              </div>
            </div>

            <div class="space-y-2">
              <label hlmLabel>Adresse e-mail</label>
              <input
                hlmInput
                type="email"
                placeholder="votre&#64;email.be"
                [(ngModel)]="form.email"
                name="email"
                required
                autocomplete="email"
              />
            </div>

            <div class="space-y-2">
              <label hlmLabel>Mot de passe</label>
              <div class="relative">
                <input
                  hlmInput
                  [type]="showPassword() ? 'text' : 'password'"
                  placeholder="••••••••"
                  [(ngModel)]="form.password"
                  name="password"
                  required
                  autocomplete="new-password"
                  class="pr-10"
                />
                <button
                  type="button"
                  class="absolute top-1/2 right-3 -translate-y-1/2 cursor-pointer text-(--muted-foreground) hover:text-(--foreground)"
                  (click)="showPassword.set(!showPassword())"
                >
                  @if (showPassword()) {
                    <lucide-icon [img]="EyeOffIcon" [size]="16"></lucide-icon>
                  } @else {
                    <lucide-icon [img]="EyeIcon" [size]="16"></lucide-icon>
                  }
                </button>
              </div>
              <!-- Password strength -->
              @if (form.password) {
                <div class="flex gap-1">
                  @for (i of [0, 1, 2, 3]; track i) {
                    <div
                    class="h-1 flex-1 rounded-xs transition-colors duration-200"
                      [class]="i < passwordStrength() ? strengthColor() : 'bg-(--border)'"
                    ></div>
                  }
                </div>
                <p class="text-xs text-(--muted-foreground)">{{ strengthLabel() }}</p>
              }
            </div>

            <div class="space-y-2">
              <label hlmLabel>Confirmer le mot de passe</label>
              <input
                hlmInput
                [type]="showPassword() ? 'text' : 'password'"
                placeholder="••••••••"
                [(ngModel)]="form.confirmPassword"
                name="confirmPassword"
                required
                autocomplete="new-password"
              />
            </div>

            <!-- Terms -->
            <div class="flex items-start gap-2">
              <input
                type="checkbox"
                id="terms"
                [(ngModel)]="form.acceptTerms"
                name="acceptTerms"
                class="mt-1 h-4 w-4 cursor-pointer rounded border border-(--border) accent-(--primary)"
              />
              <label for="terms" class="text-sm text-(--muted-foreground)">
                J'accepte les
                <a routerLink="/terms" class="text-(--primary) hover:underline">
                  conditions d'utilisation
                </a>
                et la
                <a routerLink="/privacy" class="text-(--primary) hover:underline">
                  politique de confidentialité
                </a>
              </label>
            </div>

            @if (errorMessage()) {
              <div class="rounded-sm bg-(--destructive)/10 px-3 py-2 text-sm text-(--destructive)">
                {{ errorMessage() }}
              </div>
            }

            @if (successMessage()) {
              <div class="rounded-sm bg-(--success)/10 px-3 py-2 text-sm text-(--success)">
                {{ successMessage() }}
              </div>
            }

            <button
              hlmBtn
              variant="default"
              type="submit"
              class="w-full cursor-pointer"
              [disabled]="submitting()"
            >
              {{ submitting() ? 'Création...' : "S'inscrire" }}
            </button>
          </form>

          <!-- Divider -->
          <div class="relative my-6 flex items-center">
            <hlm-separator class="flex-1" />
            <span class="px-3 text-xs text-(--muted-foreground)">ou continuer avec</span>
            <hlm-separator class="flex-1" />
          </div>

          <!-- Social -->
          <div class="grid grid-cols-2 gap-3">
            <a
              hlmBtn
              variant="outline"
              href="/oauth2/authorization/google"
              class="cursor-pointer gap-2"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              Google
            </a>
            <a
              hlmBtn
              variant="outline"
              href="/oauth2/authorization/microsoft"
              class="cursor-pointer gap-2"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24">
                <path fill="#F25022" d="M1 1h10v10H1z"/>
                <path fill="#00A4EF" d="M1 13h10v10H1z"/>
                <path fill="#7FBA00" d="M13 1h10v10H13z"/>
                <path fill="#FFB900" d="M13 13h10v10H13z"/>
              </svg>
              Microsoft
            </a>
          </div>
        </div>

        <!-- Login link -->
        <p class="mt-6 text-center text-sm text-(--muted-foreground)">
          Déjà un compte ?
          <a routerLink="/login" class="font-medium text-(--primary) hover:underline">
            Se connecter
          </a>
        </p>
      </div>
    </div>
  `,
})
export class RegisterComponent {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;

  readonly showPassword = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  readonly form = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
    acceptTerms: false,
  };

  passwordStrength = () => {
    const p = this.form.password;
    let strength = 0;
    if (p.length >= 8) strength++;
    if (/[A-Z]/.test(p)) strength++;
    if (/[0-9]/.test(p)) strength++;
    if (/[^A-Za-z0-9]/.test(p)) strength++;
    return strength;
  };

  strengthColor = () => {
    const s = this.passwordStrength();
    if (s <= 1) return 'bg-(--destructive)';
    if (s <= 2) return 'bg-(--warning)';
    return 'bg-(--success)';
  };

  strengthLabel = () => {
    const s = this.passwordStrength();
    if (s <= 1) return 'Faible';
    if (s <= 2) return 'Moyen';
    if (s <= 3) return 'Fort';
    return 'Très fort';
  };

  onSubmit(): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (!this.form.firstName || !this.form.lastName || !this.form.email || !this.form.password) {
      this.errorMessage.set('Veuillez remplir tous les champs.');
      return;
    }
    if (this.form.password !== this.form.confirmPassword) {
      this.errorMessage.set('Les mots de passe ne correspondent pas.');
      return;
    }
    if (this.form.password.length < 8) {
      this.errorMessage.set('Le mot de passe doit contenir au moins 8 caractères.');
      return;
    }
    if (!this.form.acceptTerms) {
      this.errorMessage.set("Veuillez accepter les conditions d'utilisation.");
      return;
    }

    this.submitting.set(true);

    this.http
      .post<any>(`${environment.apiUrl}/api/v1/auth/register`, {
        firstName: this.form.firstName,
        lastName: this.form.lastName,
        email: this.form.email,
        password: this.form.password,
        confirmPassword: this.form.confirmPassword,
        acceptTerms: this.form.acceptTerms,
      }, { withCredentials: true })
      .pipe(
        switchMap(() =>
          this.http.post<any>(
            `${environment.apiUrl}/api/v1/auth/login`,
            { email: this.form.email, password: this.form.password },
            { withCredentials: true },
          ),
        ),
      )
      .subscribe({
        next: (response: any) => {
          const user = response.data ?? response;
          this.authService.setUser({
            id: user.id,
            email: user.email,
            firstName: user.firstName,
            lastName: user.lastName,
            displayName: user.displayName ?? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim(),
            roles: Array.isArray(user.roles) ? user.roles : [],
            emailVerified: user.emailVerified ?? false,
            companyName: user.companyName,
            phone: user.phone,
            city: user.city,
            country: user.country,
          });
          this.submitting.set(false);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          // Registration may have succeeded but auto-login failed
          if (err.url?.includes('/login')) {
            this.successMessage.set('Compte créé ! Connectez-vous pour accéder à votre espace.');
            this.submitting.set(false);
            this.router.navigate(['/login']);
          } else {
            this.errorMessage.set(
              err.error?.message || 'Une erreur est survenue. Veuillez réessayer.',
            );
            this.submitting.set(false);
          }
        },
      });
  }
}
