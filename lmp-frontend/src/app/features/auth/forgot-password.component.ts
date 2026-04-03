import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmLabel } from '@spartan-ng/helm/label';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'lmp-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink, HlmButton, HlmInput, HlmLabel],
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
            Nous vous enverrons un lien sécurisé pour choisir un nouveau mot de passe. Le lien expire après
            une heure.
          </p>
        </div>
        <div class="p-8 pt-0 flex items-center gap-6">
          <a routerLink="/privacy" class="text-xs text-(--muted-foreground) hover:text-(--foreground) transition-colors"
            >Politique de confidentialité</a
          >
          <a routerLink="/terms" class="text-xs text-(--muted-foreground) hover:text-(--foreground) transition-colors"
            >Conditions d'utilisation</a
          >
        </div>
      </div>

      <div class="flex w-full flex-col lg:w-1/2">
        <div class="flex items-center justify-between px-6 py-4 sm:px-8">
          <a routerLink="/login" class="flex items-center gap-2 text-sm text-(--muted-foreground) hover:text-(--foreground) transition-colors">
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Retour à la connexion
          </a>
        </div>

        <div class="flex flex-1 items-center justify-center px-4 py-8">
          <div class="w-full max-w-md">
            <h1
              class="font-display text-[clamp(1.875rem,4.5vw,2.5rem)] font-semibold leading-[1.12] tracking-[-0.03em] text-(--foreground)"
            >
              Mot de passe oublié
            </h1>
            <p class="mt-1.5 text-sm text-(--muted-foreground)">
              Saisissez l’adresse e-mail de votre compte. Si elle est reconnue, vous recevrez un lien de
              réinitialisation.
            </p>

            @if (!successMessage()) {
              <form (ngSubmit)="onSubmit()" class="mt-8 space-y-5">
                <div class="space-y-2">
                  <label hlmLabel class="font-semibold">Adresse email</label>
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
                        d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75"
                      />
                    </svg>
                    <input
                      hlmInput
                      type="email"
                      placeholder="name&#64;company.com"
                      [(ngModel)]="form.email"
                      name="email"
                      required
                      autocomplete="email"
                      class="pl-10"
                    />
                  </div>
                </div>

                @if (errorMessage()) {
                  <div class="rounded-sm bg-(--destructive)/10 px-3 py-2 text-sm text-(--destructive)">
                    {{ errorMessage() }}
                  </div>
                }

                <button hlmBtn variant="default" type="submit" class="w-full cursor-pointer" [disabled]="submitting()">
                  {{ submitting() ? 'Envoi…' : 'Envoyer le lien' }}
                </button>
              </form>
            } @else {
              <div class="mt-8 rounded-sm border border-(--border) bg-(--muted)/30 px-4 py-3 text-sm text-(--foreground)">
                {{ successMessage() }}
              </div>
              <a routerLink="/login" hlmBtn variant="outline" class="mt-6 inline-flex w-full justify-center">
                Retour à la connexion
              </a>
            }
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ForgotPasswordComponent {
  private readonly http = inject(HttpClient);

  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  readonly form = { email: '' };

  onSubmit(): void {
    if (!this.form.email?.trim()) {
      this.errorMessage.set('Veuillez saisir votre adresse e-mail.');
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set('');

    this.http
      .post<{ success?: boolean; message?: string }>(
        `${environment.apiUrl}/api/v1/auth/forgot-password`,
        { email: this.form.email.trim() },
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          this.successMessage.set(
            res.message ??
              'Si un compte existe pour cette adresse, un e-mail de réinitialisation a été envoyé.',
          );
          this.submitting.set(false);
        },
        error: (err) => {
          this.errorMessage.set(err.error?.message ?? 'Impossible d’envoyer la demande. Réessayez plus tard.');
          this.submitting.set(false);
        },
      });
  }
}
