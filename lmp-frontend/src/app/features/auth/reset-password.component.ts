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
  selector: 'lmp-reset-password',
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
            Choisissez un mot de passe fort : au moins 8 caractères, avec majuscules, minuscules, chiffres et un
            caractère spécial (!&#64;#$%^&amp;*…).
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
            <h1
              class="font-display text-[clamp(1.875rem,4.5vw,2.5rem)] font-semibold leading-[1.12] tracking-[-0.03em] text-(--foreground)"
            >
              Nouveau mot de passe
            </h1>
            <p class="mt-1.5 text-sm text-(--muted-foreground)">
              Définissez un nouveau mot de passe pour votre compte.
            </p>

            @if (!token()) {
              <div class="mt-8 rounded-sm bg-(--destructive)/10 px-3 py-2 text-sm text-(--destructive)">
                Lien invalide ou incomplet. Demandez un nouveau lien depuis la page « Mot de passe oublié ».
              </div>
              <a routerLink="/forgot-password" hlmBtn variant="default" class="mt-6 inline-flex w-full justify-center">
                Demander un lien
              </a>
            } @else if (!done()) {
              <form (ngSubmit)="onSubmit()" class="mt-8 space-y-5">
                <div class="space-y-2">
                  <label hlmLabel class="font-semibold">Nouveau mot de passe</label>
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
                      [(ngModel)]="form.newPassword"
                      name="newPassword"
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
                  {{ submitting() ? 'Enregistrement…' : 'Enregistrer le mot de passe' }}
                </button>
              </form>
            } @else {
              <div class="mt-8 rounded-sm border border-(--border) bg-(--muted)/30 px-4 py-3 text-sm text-(--foreground)">
                {{ successMessage() }}
              </div>
              <a routerLink="/login" hlmBtn variant="default" class="mt-6 inline-flex w-full justify-center">
                Se connecter
              </a>
            }
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ResetPasswordComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;

  readonly token = signal('');
  readonly showPassword = signal(false);
  readonly showPassword2 = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly done = signal(false);
  readonly successMessage = signal('');

  readonly form = { newPassword: '', confirmPassword: '' };

  ngOnInit(): void {
    const t = this.route.snapshot.queryParamMap.get('token');
    if (t?.trim()) {
      this.token.set(t.trim());
    }
  }

  onSubmit(): void {
    if (!this.form.newPassword || !this.form.confirmPassword) {
      this.errorMessage.set('Veuillez remplir tous les champs.');
      return;
    }
    if (this.form.newPassword !== this.form.confirmPassword) {
      this.errorMessage.set('Les mots de passe ne correspondent pas.');
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set('');

    this.http
      .post<{ message?: string }>(
        `${environment.apiUrl}/api/v1/auth/reset-password`,
        {
          token: this.token(),
          newPassword: this.form.newPassword,
          confirmPassword: this.form.confirmPassword,
        },
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          this.successMessage.set(res.message ?? 'Mot de passe mis à jour.');
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
            err.error?.message ?? 'Impossible de réinitialiser le mot de passe. Le lien est peut-être expiré.',
          );
          this.submitting.set(false);
        },
      });
  }
}
