import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { LucideAngularModule, Eye, EyeOff } from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmLabel } from '@spartan-ng/helm/label';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'lmp-accept-invitation',
  standalone: true,
  imports: [FormsModule, RouterLink, LucideAngularModule, HlmButton, HlmInput, HlmLabel],
  template: `
    <div class="flex min-h-screen">
      <div class="relative hidden w-1/2 lg:flex flex-col justify-between bg-(--card) border-r border-(--border)">
        <div class="p-8">
          <a routerLink="/" class="flex items-center gap-3">
            <img src="/images/logo-lmp.webp" alt="LMP Logo" class="h-8 w-auto rounded-xs" />
            <span class="text-base font-semibold text-(--foreground)">LMP Digital Services</span>
          </a>
        </div>
        <div class="p-8">
          <p class="text-sm text-(--muted-foreground) max-w-sm leading-relaxed">
            Vous avez été invité à rejoindre l'équipe en tant que collaborateur. Complétez votre profil et définissez
            un mot de passe sécurisé pour activer votre compte.
          </p>
        </div>
        <div class="p-8 pt-0 flex items-center gap-6">
          <a routerLink="/privacy" class="text-xs text-(--muted-foreground) hover:text-(--foreground) transition-colors"
            >Politique de confidentialité</a
          >
        </div>
      </div>

      <div class="flex w-full flex-col lg:w-1/2">
        <div class="flex items-center justify-between px-6 py-4 sm:px-8">
          <a routerLink="/login" class="flex items-center gap-2 text-sm text-(--muted-foreground) hover:text-(--foreground) transition-colors">
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Connexion
          </a>
        </div>

        <div class="flex flex-1 items-center justify-center px-4 py-8">
          <div class="w-full max-w-md">
            @if (loadingPreview()) {
              <div class="flex flex-col items-center justify-center py-12">
                <div class="h-8 w-8 animate-spin rounded-full border-2 border-(--primary) border-t-transparent"></div>
                <p class="mt-4 text-sm text-(--muted-foreground)">Vérification de l'invitation…</p>
              </div>
            } @else if (previewError()) {
              <h1 class="font-display text-[clamp(1.875rem,4.5vw,2.5rem)] font-semibold leading-[1.12] tracking-[-0.03em] text-(--foreground)">
                Invitation invalide
              </h1>
              <p class="mt-1.5 text-sm text-(--muted-foreground)">
                Ce lien a expiré, a déjà été utilisé ou n'est pas valide.
              </p>
              <div class="mt-8 rounded-sm bg-(--destructive)/10 px-3 py-2 text-sm text-(--destructive)">
                {{ previewError() }}
              </div>
              <a routerLink="/login" hlmBtn variant="default" class="mt-6 inline-flex w-full justify-center">
                Se connecter
              </a>
            } @else if (done()) {
              <h1 class="font-display text-[clamp(1.875rem,4.5vw,2.5rem)] font-semibold leading-[1.12] tracking-[-0.03em] text-(--foreground)">
                Compte activé
              </h1>
              <p class="mt-1.5 text-sm text-(--muted-foreground)">
                Votre compte collaborateur est prêt. Connectez-vous pour accéder à votre espace.
              </p>
              <div class="mt-8 rounded-sm border border-(--border) bg-(--muted)/30 px-4 py-3 text-sm text-(--foreground)">
                {{ successMessage() }}
              </div>
              <a routerLink="/login" hlmBtn variant="default" class="mt-6 inline-flex w-full justify-center">
                Se connecter
              </a>
            } @else {
              <h1 class="font-display text-[clamp(1.875rem,4.5vw,2.5rem)] font-semibold leading-[1.12] tracking-[-0.03em] text-(--foreground)">
                Rejoindre l'équipe
              </h1>
              <p class="mt-1.5 text-sm text-(--muted-foreground)">
                Finalisez votre inscription en quelques secondes.
              </p>

              <form (ngSubmit)="onSubmit()" class="mt-8 space-y-5">
                <div class="space-y-2">
                  <label hlmLabel class="font-semibold">Adresse e-mail</label>
                  <input
                    hlmInput
                    type="email"
                    [value]="form.email"
                    disabled
                    class="bg-(--muted)/50 cursor-not-allowed"
                  />
                </div>

                <div class="grid grid-cols-2 gap-4">
                  <div class="space-y-2">
                    <label hlmLabel class="font-semibold">Prénom</label>
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
                    <label hlmLabel class="font-semibold">Nom</label>
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
                  <label hlmLabel class="font-semibold">Téléphone</label>
                  <input
                    hlmInput
                    type="tel"
                    placeholder="+32 470 12 34 56"
                    [(ngModel)]="form.phone"
                    name="phone"
                    autocomplete="tel"
                  />
                </div>

                <div class="space-y-2">
                  <label hlmLabel class="font-semibold">Mot de passe</label>
                  <div class="relative">
                    <svg
                      class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-(--muted-foreground)"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      stroke-width="1.5"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"
                      />
                    </svg>
                    <input
                      hlmInput
                      [type]="showPassword() ? 'text' : 'password'"
                      placeholder="••••••••"
                      [(ngModel)]="form.password"
                      name="password"
                      required
                      autocomplete="new-password"
                      class="pl-10 pr-10"
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
                  <label hlmLabel class="font-semibold">Confirmer le mot de passe</label>
                  <input
                    hlmInput
                    [type]="showPassword2() ? 'text' : 'password'"
                    placeholder="••••••••"
                    [(ngModel)]="form.confirmPassword"
                    name="confirmPassword"
                    required
                    autocomplete="new-password"
                  />
                  <button
                    type="button"
                    class="text-xs text-(--muted-foreground) hover:text-(--foreground)"
                    (click)="showPassword2.set(!showPassword2())"
                  >
                    {{ showPassword2() ? 'Masquer' : 'Afficher' }} la confirmation
                  </button>
                </div>

                @if (errorMessage()) {
                  <div class="rounded-sm bg-(--destructive)/10 px-3 py-2 text-sm text-(--destructive)">
                    {{ errorMessage() }}
                  </div>
                }

                <button hlmBtn variant="default" type="submit" class="w-full cursor-pointer" [disabled]="submitting()">
                  {{ submitting() ? 'Activation…' : 'Activer mon compte' }}
                </button>
              </form>
            }
          </div>
        </div>
      </div>
    </div>
  `,
})
export class AcceptInvitationComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;

  readonly token = signal('');
  readonly loadingPreview = signal(true);
  readonly previewError = signal('');
  readonly showPassword = signal(false);
  readonly showPassword2 = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly done = signal(false);
  readonly successMessage = signal('');

  readonly form = {
    email: '',
    firstName: '',
    lastName: '',
    phone: '',
    password: '',
    confirmPassword: '',
  };

  ngOnInit(): void {
    const t = this.route.snapshot.queryParamMap.get('token');
    if (!t?.trim()) {
      this.loadingPreview.set(false);
      this.previewError.set('Lien invalide ou incomplet.');
      return;
    }
    this.token.set(t.trim());

    this.http
      .get<any>(`${environment.apiUrl}/api/v1/auth/staff-invitations/preview`, {
        params: { token: this.token() },
        withCredentials: true,
      })
      .subscribe({
        next: (res) => {
          const data = res.data ?? res;
          this.form.email = data.email ?? '';
          this.form.firstName = data.firstName ?? '';
          this.form.lastName = data.lastName ?? '';
          this.loadingPreview.set(false);
        },
        error: (err) => {
          this.previewError.set(
            err.error?.message ?? 'Ce lien est invalide ou a expiré.',
          );
          this.loadingPreview.set(false);
        },
      });
  }

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

    if (!this.form.firstName || !this.form.lastName || !this.form.password) {
      this.errorMessage.set('Veuillez remplir tous les champs obligatoires.');
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

    this.submitting.set(true);

    this.http
      .post<any>(
        `${environment.apiUrl}/api/v1/auth/staff-invitations/accept`,
        {
          token: this.token(),
          password: this.form.password,
          confirmPassword: this.form.confirmPassword,
          firstName: this.form.firstName,
          lastName: this.form.lastName,
          phone: this.form.phone || undefined,
        },
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          this.successMessage.set(res.message ?? 'Votre compte a été créé avec succès.');
          this.done.set(true);
          this.submitting.set(false);
          void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: {},
            replaceUrl: true,
          });
        },
        error: (err) => {
          this.errorMessage.set(
            err.error?.message ?? 'Impossible de finaliser l\'inscription. Veuillez réessayer.',
          );
          this.submitting.set(false);
        },
      });
  }
}
