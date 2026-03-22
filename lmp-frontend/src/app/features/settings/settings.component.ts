import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  ArrowLeft,
  Save,
  User,
  Shield,
  Bell,
  Check,
  Loader2,
  Eye,
  EyeOff,
  AlertCircle,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmLabel } from '@spartan-ng/helm/label';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';

@Component({
  selector: 'lmp-settings',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    LucideAngularModule,
    HlmButton,
    HlmInput,
    HlmLabel,
  ],
  template: `
    <div class="min-h-screen bg-(--background)">
      <!-- Header -->
      <header
        class="border-b border-(--border) bg-(--card)"
      >
        <div
          class="mx-auto flex h-16 max-w-4xl items-center gap-4 px-4 sm:px-6"
        >
          <a
            routerLink="/dashboard"
            class="flex items-center gap-2 text-sm text-(--muted-foreground) transition-colors hover:text-(--foreground)"
          >
            <lucide-icon [img]="ArrowLeftIcon" [size]="16"></lucide-icon>
            Retour au dashboard
          </a>
        </div>
      </header>

      <main class="mx-auto max-w-4xl px-4 py-8 sm:px-6">
        <h1 class="text-2xl font-bold text-(--foreground)">
          Paramètres du compte
        </h1>
        <p class="mt-1 text-sm text-(--muted-foreground)">
          Gérez vos informations personnelles et préférences.
        </p>

        <!-- Success / Error Messages -->
        @if (profileSuccess()) {
          <div
            class="mt-4 flex items-center gap-2 rounded-sm border border-emerald-500/20 bg-(--muted) p-3 text-sm text-(--foreground)"
          >
            <lucide-icon [img]="CheckIcon" [size]="16"></lucide-icon>
            {{ profileSuccess() }}
          </div>
        }
        @if (profileError()) {
          <div
            class="mt-4 flex items-center gap-2 rounded-sm border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-400"
          >
            <lucide-icon [img]="AlertCircleIcon" [size]="16"></lucide-icon>
            {{ profileError() }}
          </div>
        }

        <!-- Profile Section -->
        <div class="mt-8 rounded-sm border border-(--border) bg-(--card) p-6">
          <div class="mb-6 flex items-center gap-3">
            <div
              class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--primary)/10 text-(--primary)"
            >
              <lucide-icon [img]="UserIcon" [size]="18"></lucide-icon>
            </div>
            <h2 class="text-lg font-bold text-(--foreground)">
              Profil
            </h2>
          </div>

          <div class="grid grid-cols-1 gap-5 sm:grid-cols-2">
            <div class="space-y-2">
              <label hlmLabel>Prénom</label>
              <input
                hlmInput
                type="text"
                [(ngModel)]="profileForm.firstName"
                placeholder="Votre prénom"
              />
            </div>
            <div class="space-y-2">
              <label hlmLabel>Nom</label>
              <input
                hlmInput
                type="text"
                [(ngModel)]="profileForm.lastName"
                placeholder="Votre nom"
              />
            </div>
            <div class="space-y-2 sm:col-span-2">
              <label hlmLabel>Email</label>
              <input
                hlmInput
                type="email"
                [value]="authService.user()?.email || ''"
                readonly
                class="cursor-not-allowed opacity-60"
              />
              <p class="text-xs text-(--muted-foreground)">
                L'email ne peut pas être modifié.
              </p>
            </div>
            <div class="space-y-2">
              <label hlmLabel>Téléphone</label>
              <input
                hlmInput
                type="tel"
                [(ngModel)]="profileForm.phone"
                placeholder="+33 6 00 00 00 00"
              />
            </div>
            <div class="space-y-2">
              <label hlmLabel>Entreprise</label>
              <input
                hlmInput
                type="text"
                [(ngModel)]="profileForm.companyName"
                placeholder="Nom de l'entreprise"
              />
            </div>
            <div class="space-y-2">
              <label hlmLabel>Ville</label>
              <input
                hlmInput
                type="text"
                [(ngModel)]="profileForm.city"
                placeholder="Votre ville"
              />
            </div>
            <div class="space-y-2">
              <label hlmLabel>Pays</label>
              <input
                hlmInput
                type="text"
                [(ngModel)]="profileForm.country"
                placeholder="France"
              />
            </div>
          </div>

          <div class="mt-6 flex justify-end">
            <button
              hlmBtn
              variant="default"
              class="cursor-pointer gap-2"
              (click)="saveProfile()"
              [disabled]="savingProfile()"
            >
              @if (savingProfile()) {
                <lucide-icon
                  [img]="Loader2Icon"
                  [size]="16"
                  class="animate-spin"
                ></lucide-icon>
                Enregistrement…
              } @else {
                <lucide-icon [img]="SaveIcon" [size]="16"></lucide-icon>
                Enregistrer
              }
            </button>
          </div>
        </div>

        <!-- Security Section -->
        <div class="mt-6 rounded-sm border border-(--border) bg-(--card) p-6">
          <div class="mb-4 flex items-center gap-3">
            <div
              class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--primary)/10 text-(--primary)"
            >
              <lucide-icon [img]="ShieldIcon" [size]="18"></lucide-icon>
            </div>
            <h2 class="text-lg font-bold text-(--foreground)">
              Sécurité
            </h2>
          </div>

          <div class="space-y-4">
            <!-- Email verified status -->
            <div
              class="flex items-center justify-between rounded-sm border border-(--border) p-4"
            >
              <div>
                <p class="text-sm font-medium text-(--foreground)">
                  Email vérifié
                </p>
                <p class="text-xs text-(--muted-foreground)">
                  Statut de vérification de votre email
                </p>
              </div>
              @if (authService.user()?.emailVerified) {
                <span
                  class="rounded-full bg-(--muted) px-3 py-1 text-xs font-medium text-(--foreground)"
                  >Vérifié ✓</span
                >
              } @else {
                <span
                  class="rounded-full bg-(--muted) px-3 py-1 text-xs font-medium text-(--foreground)"
                  >Non vérifié</span
                >
              }
            </div>

            <!-- Password change -->
            <div class="rounded-sm border border-(--border) p-4">
              <p class="text-sm font-medium text-(--foreground)">
                Changer le mot de passe
              </p>
              <p class="mb-4 text-xs text-(--muted-foreground)">
                Entrez votre mot de passe actuel et un nouveau mot de passe.
              </p>

              @if (passwordSuccess()) {
                <div
                  class="mb-4 flex items-center gap-2 rounded-sm border border-emerald-500/20 bg-(--muted) p-3 text-sm text-(--foreground)"
                >
                  <lucide-icon [img]="CheckIcon" [size]="16"></lucide-icon>
                  {{ passwordSuccess() }}
                </div>
              }
              @if (passwordError()) {
                <div
                  class="mb-4 flex items-center gap-2 rounded-sm border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-400"
                >
                  <lucide-icon
                    [img]="AlertCircleIcon"
                    [size]="16"
                  ></lucide-icon>
                  {{ passwordError() }}
                </div>
              }

              <div class="space-y-4">
                <div class="space-y-2">
                  <label hlmLabel>Mot de passe actuel</label>
                  <div class="relative">
                    <input
                      hlmInput
                      [type]="showCurrentPwd() ? 'text' : 'password'"
                      [(ngModel)]="passwordForm.currentPassword"
                      placeholder="••••••••"
                      class="w-full pr-10"
                    />
                    <button
                      type="button"
                      class="absolute right-2 top-1/2 -translate-y-1/2 cursor-pointer text-(--muted-foreground) hover:text-(--foreground)"
                      (click)="showCurrentPwd.set(!showCurrentPwd())"
                    >
                      <lucide-icon
                        [img]="showCurrentPwd() ? EyeOffIcon : EyeIcon"
                        [size]="16"
                      ></lucide-icon>
                    </button>
                  </div>
                </div>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div class="space-y-2">
                    <label hlmLabel>Nouveau mot de passe</label>
                    <input
                      hlmInput
                      [type]="showNewPwd() ? 'text' : 'password'"
                      [(ngModel)]="passwordForm.newPassword"
                      placeholder="Min. 8 caractères"
                    />
                  </div>
                  <div class="space-y-2">
                    <label hlmLabel>Confirmer le nouveau mot de passe</label>
                    <input
                      hlmInput
                      [type]="showNewPwd() ? 'text' : 'password'"
                      [(ngModel)]="passwordForm.confirmPassword"
                      placeholder="Confirmer"
                    />
                  </div>
                </div>
              </div>

              <div class="mt-4 flex justify-end">
                <button
                  hlmBtn
                  variant="outline"
                  class="cursor-pointer gap-2"
                  (click)="changePassword()"
                  [disabled]="changingPassword()"
                >
                  @if (changingPassword()) {
                    <lucide-icon
                      [img]="Loader2Icon"
                      [size]="16"
                      class="animate-spin"
                    ></lucide-icon>
                    Modification…
                  } @else {
                    <lucide-icon [img]="ShieldIcon" [size]="16"></lucide-icon>
                    Modifier le mot de passe
                  }
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  `,
})
export class SettingsComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);

  readonly ArrowLeftIcon = ArrowLeft;
  readonly SaveIcon = Save;
  readonly UserIcon = User;
  readonly ShieldIcon = Shield;
  readonly BellIcon = Bell;
  readonly CheckIcon = Check;
  readonly Loader2Icon = Loader2;
  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;
  readonly AlertCircleIcon = AlertCircle;

  // Profile form
  profileForm = {
    firstName: '',
    lastName: '',
    phone: '',
    companyName: '',
    city: '',
    country: '',
  };

  // Password form
  passwordForm = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  };

  readonly savingProfile = signal(false);
  readonly profileSuccess = signal<string | null>(null);
  readonly profileError = signal<string | null>(null);

  readonly changingPassword = signal(false);
  readonly passwordSuccess = signal<string | null>(null);
  readonly passwordError = signal<string | null>(null);

  readonly showCurrentPwd = signal(false);
  readonly showNewPwd = signal(false);

  ngOnInit(): void {
    const user = this.authService.user();
    if (user) {
      this.profileForm = {
        firstName: user.firstName ?? '',
        lastName: user.lastName ?? '',
        phone: user.phone ?? '',
        companyName: user.companyName ?? '',
        city: user.city ?? '',
        country: user.country ?? '',
      };
    }
  }

  saveProfile(): void {
    this.profileSuccess.set(null);
    this.profileError.set(null);
    this.savingProfile.set(true);

    this.profileService.updateProfile(this.profileForm).subscribe({
      next: (updatedUser) => {
        // Update auth service user with new data
        this.authService.setUser(updatedUser as any);
        this.profileSuccess.set('Profil mis à jour avec succès.');
        this.savingProfile.set(false);
        this.clearMessageAfterDelay('profile');
      },
      error: (err) => {
        this.profileError.set(
          err.error?.message || 'Erreur lors de la mise à jour du profil.',
        );
        this.savingProfile.set(false);
      },
    });
  }

  changePassword(): void {
    this.passwordSuccess.set(null);
    this.passwordError.set(null);

    if (!this.passwordForm.currentPassword || !this.passwordForm.newPassword) {
      this.passwordError.set('Veuillez remplir tous les champs.');
      return;
    }

    if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
      this.passwordError.set('Les mots de passe ne correspondent pas.');
      return;
    }

    if (this.passwordForm.newPassword.length < 8) {
      this.passwordError.set(
        'Le mot de passe doit contenir au moins 8 caractères.',
      );
      return;
    }

    this.changingPassword.set(true);

    this.profileService.changePassword(this.passwordForm).subscribe({
      next: () => {
        this.passwordSuccess.set('Mot de passe modifié avec succès.');
        this.passwordForm = {
          currentPassword: '',
          newPassword: '',
          confirmPassword: '',
        };
        this.changingPassword.set(false);
        this.clearMessageAfterDelay('password');
      },
      error: (err) => {
        this.passwordError.set(
          err.error?.message ||
            'Erreur lors du changement de mot de passe.',
        );
        this.changingPassword.set(false);
      },
    });
  }

  private clearMessageAfterDelay(type: 'profile' | 'password'): void {
    setTimeout(() => {
      if (type === 'profile') {
        this.profileSuccess.set(null);
      } else {
        this.passwordSuccess.set(null);
      }
    }, 5000);
  }
}
