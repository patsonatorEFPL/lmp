import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';

@Component({
  selector: 'lmp-order-modal',
  standalone: true,
  imports: [FormsModule, HlmButton],
  template: `
    @if (isOpen) {
      <!-- Backdrop -->
      <div
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
        (click)="close()"
      >
        <!-- Modal -->
        <div
          class="relative w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-sm border border-(--border) bg-(--card) p-6 sm:p-8 shadow-sm"
          (click)="$event.stopPropagation()"
        >
          <!-- Close button -->
          <button
            class="absolute top-4 right-4 text-(--muted-foreground) hover:text-(--foreground) cursor-pointer"
            (click)="close()"
          >
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>

          <h2 class="text-lg font-bold text-(--foreground)">Commander un service</h2>

          <!-- Registration form -->
          <div class="mt-6 space-y-5">
            <div>
              <h3 class="text-sm font-semibold text-(--foreground) mb-3">Inscription rapide</h3>
              <p class="text-xs text-(--muted-foreground) mb-4">
                Créez votre compte en quelques secondes ! Votre nom d'affichage sera automatiquement généré depuis votre email.
              </p>

              <div class="space-y-3">
                <div>
                  <label class="text-xs font-medium text-(--foreground)">Email *</label>
                  <input
                    type="email"
                    [(ngModel)]="email"
                    placeholder="votre@email.com"
                    class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) placeholder:text-(--muted-foreground) outline-none focus:border-blue-500/50"
                  />
                  <p class="mt-1 text-xs text-(--muted-foreground)">Votre identifiant de connexion</p>
                </div>

                <div>
                  <label class="text-xs font-medium text-(--foreground)">Téléphone</label>
                  <input
                    type="tel"
                    [(ngModel)]="phone"
                    placeholder="+32 ..."
                    class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) placeholder:text-(--muted-foreground) outline-none focus:border-blue-500/50"
                  />
                  <p class="mt-1 text-xs text-(--muted-foreground)">Optionnel pour la commande</p>
                </div>

                <!-- Toggle: Add name -->
                <button
                  class="text-xs text-blue-400 hover:text-blue-300 cursor-pointer"
                  (click)="showNameFields.set(!showNameFields())"
                >
                  + Ajouter prénom et nom (optionnel)
                </button>

                @if (showNameFields()) {
                  <div class="grid grid-cols-2 gap-3">
                    <div>
                      <label class="text-xs font-medium text-(--foreground)">Prénom</label>
                      <input
                        type="text"
                        [(ngModel)]="firstName"
                        class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-blue-500/50"
                      />
                    </div>
                    <div>
                      <label class="text-xs font-medium text-(--foreground)">Nom</label>
                      <input
                        type="text"
                        [(ngModel)]="lastName"
                        class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-blue-500/50"
                      />
                    </div>
                  </div>
                }
              </div>
            </div>

            <!-- Password section -->
            <div>
              <h3 class="text-sm font-semibold text-(--foreground) mb-3">Sécurité du compte</h3>
              <div class="space-y-3">
                <div>
                  <label class="text-xs font-medium text-(--foreground)">Mot de passe *</label>
                  <input
                    type="password"
                    [(ngModel)]="password"
                    class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-blue-500/50"
                  />
                  <p class="mt-1 text-xs text-(--muted-foreground)">Minimum 6 caractères</p>
                </div>
                <div>
                  <label class="text-xs font-medium text-(--foreground)">Confirmer le mot de passe *</label>
                  <input
                    type="password"
                    [(ngModel)]="confirmPassword"
                    class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-blue-500/50"
                  />
                </div>
              </div>
            </div>

            <!-- Billing toggle -->
            <div>
              <h3 class="text-sm font-semibold text-(--foreground) mb-2">Informations de facturation</h3>
              <p class="text-xs text-(--muted-foreground) mb-3">
                Vous pourrez ajouter ces informations plus tard dans votre profil si nécessaire.
              </p>
              <button
                class="text-xs text-blue-400 hover:text-blue-300 cursor-pointer"
                (click)="showBilling.set(!showBilling())"
              >
                + Ajouter une adresse de facturation (optionnel)
              </button>

              @if (showBilling()) {
                <div class="mt-3 space-y-3">
                  <input type="text" placeholder="Adresse" class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none" />
                  <div class="grid grid-cols-2 gap-3">
                    <input type="text" placeholder="Ville" class="rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none" />
                    <input type="text" placeholder="Code postal" class="rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none" />
                  </div>
                  <select class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none cursor-pointer">
                    <option value="">Sélectionner...</option>
                    <option value="CA">Canada</option>
                    <option value="BE">Belgique</option>
                    <option value="FR">France</option>
                    <option value="LU">Luxembourg</option>
                    <option value="CH">Suisse</option>
                    <option value="NL">Pays-Bas</option>
                  </select>
                  <input type="text" placeholder="Nom de l'entreprise (optionnel)" class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none" />
                </div>
              }
            </div>

            <!-- Order summary -->
            <div class="rounded-sm border border-(--border) bg-(--background) p-4">
              <h3 class="text-sm font-semibold text-(--foreground) mb-3">Résumé de la commande</h3>
              <div class="flex items-center justify-between text-sm">
                <span class="text-(--muted-foreground)">{{ serviceName || 'Service sélectionné' }}</span>
                <span class="font-semibold text-(--foreground)">{{ servicePrice || '0,00 EUR' }}</span>
              </div>
            </div>

            <!-- Terms -->
            <label class="flex items-start gap-2 text-xs text-(--muted-foreground) cursor-pointer">
              <input type="checkbox" [(ngModel)]="acceptTerms" class="mt-0.5 cursor-pointer" />
              <span>
                J'accepte les
                <a routerLink="/terms" class="text-blue-400 hover:underline">conditions d'utilisation</a>
                et la
                <a routerLink="/privacy" class="text-blue-400 hover:underline">politique de confidentialité</a> *
              </span>
            </label>

            <!-- Submit -->
            <div class="flex gap-3">
              <button
                hlmBtn
                variant="default"
                class="flex-1 cursor-pointer justify-center"
                [disabled]="!acceptTerms"
              >
                Créer mon compte et payer
              </button>
            </div>
            <button
              class="w-full text-center text-xs text-(--muted-foreground) hover:text-(--foreground) cursor-pointer"
              (click)="close()"
            >
              Annuler
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class OrderModalComponent {
  @Input() isOpen = false;
  @Input() serviceName = '';
  @Input() servicePrice = '';
  @Output() closed = new EventEmitter<void>();

  email = '';
  phone = '';
  firstName = '';
  lastName = '';
  password = '';
  confirmPassword = '';
  acceptTerms = false;

  showNameFields = signal(false);
  showBilling = signal(false);

  close(): void {
    this.closed.emit();
  }
}
