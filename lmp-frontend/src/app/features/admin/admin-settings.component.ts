import { ChangeDetectorRef, Component, inject, NgZone, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  LucideAngularModule,
  Settings,
  Save,
  Globe,
  Bell,
  Shield,
  Mail,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AdminService, CompanyAddressPayload } from '../../core/services/admin.service';

@Component({
  selector: 'lmp-admin-settings',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div>
      <h1 class="text-2xl font-bold text-(--foreground)">
        Paramètres du système
      </h1>
      <p class="mt-1 text-sm text-(--muted-foreground)">
        Configurez les paramètres globaux de la plateforme.
      </p>
    </div>

    <div class="mt-8 space-y-6">
      <!-- General Settings -->
      <div class="rounded-sm border border-(--border) bg-(--card) p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)">
            <lucide-icon [img]="GlobeIcon" [size]="18" class="text-(--foreground)"></lucide-icon>
          </div>
          <div>
            <h2 class="text-sm font-semibold text-(--foreground)">Général</h2>
            <p class="text-xs text-(--muted-foreground)">Paramètres de base de la plateforme</p>
          </div>
        </div>
        <div class="space-y-4">
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label class="text-xs font-medium text-(--foreground)">Nom de l'entreprise</label>
              <input
                type="text"
                [(ngModel)]="settings.companyName"
                class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              />
            </div>
            <div>
              <label class="text-xs font-medium text-(--foreground)">Email de support</label>
              <input
                type="email"
                [(ngModel)]="settings.supportEmail"
                class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              />
            </div>
          </div>
          <div>
            <label class="text-xs font-medium text-(--foreground)">URL du site</label>
            <input
              type="url"
              [(ngModel)]="settings.siteUrl"
              class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
            />
          </div>
          <div class="sm:col-span-2">
            <label class="text-xs font-medium text-(--foreground)">Adresse postale (factures PDF)</label>
            <textarea
              rows="2"
              [(ngModel)]="companyAddress.addressLine"
              class="mt-1 w-full resize-y rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              placeholder="Ex. 123 rue Principale, bureau 200"
            ></textarea>
            <p class="mt-1 text-[11px] text-(--muted-foreground)">
              Première ligne sous le nom sur la facture PDF (rue, numéro, CP si besoin). La ligne suivante est la ville ci-dessous.
            </p>
            @if (isAddressStillFlywaySeed()) {
              <p
                class="mt-2 rounded-sm border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-[11px] text-amber-900 dark:text-amber-100"
                role="status"
              >
                <strong>Texte d’exemple encore actif :</strong> cette valeur est toujours affichée sur vos factures tant que vous ne la remplacez pas. Modifiez le champ ci-dessus puis enregistrez.
              </p>
            }
          </div>
          <div class="sm:col-span-2">
            <label class="text-xs font-medium text-(--foreground)">Ville, province / région</label>
            <input
              type="text"
              [(ngModel)]="companyAddress.cityRegion"
              class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              placeholder="Ex. Montréal, QC"
            />
          </div>
        </div>
      </div>

      <!-- Notification Settings -->
      <div class="rounded-sm border border-(--border) bg-(--card) p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)">
            <lucide-icon [img]="BellIcon" [size]="18" class="text-(--foreground)"></lucide-icon>
          </div>
          <div>
            <h2 class="text-sm font-semibold text-(--foreground)">Notifications</h2>
            <p class="text-xs text-(--muted-foreground)">Configurer les notifications email</p>
          </div>
        </div>
        <div class="space-y-3">
          @for (notif of notificationOptions; track notif.key) {
            <div class="flex items-center justify-between rounded-sm border border-(--border) px-4 py-3">
              <div>
                <p class="text-sm font-medium text-(--foreground)">{{ notif.label }}</p>
                <p class="text-xs text-(--muted-foreground)">{{ notif.description }}</p>
              </div>
              <label class="relative inline-flex cursor-pointer items-center">
                <input
                  type="checkbox"
                  [(ngModel)]="notif.enabled"
                  class="peer sr-only"
                />
                <div class="h-6 w-11 rounded-full bg-(--border) transition-colors after:absolute after:left-[2px] after:top-[2px] after:h-5 after:w-5 after:rounded-full after:bg-white after:transition-transform peer-checked:bg-(--primary) peer-checked:after:translate-x-full"></div>
              </label>
            </div>
          }
        </div>
      </div>

      <!-- Security Settings -->
      <div class="rounded-sm border border-(--border) bg-(--card) p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-red-500/10">
            <lucide-icon [img]="ShieldIcon" [size]="18" class="text-red-500"></lucide-icon>
          </div>
          <div>
            <h2 class="text-sm font-semibold text-(--foreground)">Sécurité</h2>
            <p class="text-xs text-(--muted-foreground)">Paramètres de sécurité et d'accès</p>
          </div>
        </div>
        <div class="space-y-4">
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label class="text-xs font-medium text-(--foreground)">Tentatives max avant verrouillage</label>
              <input
                type="number"
                [(ngModel)]="settings.maxLoginAttempts"
                class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              />
            </div>
            <div>
              <label class="text-xs font-medium text-(--foreground)">Durée de session (minutes)</label>
              <input
                type="number"
                [(ngModel)]="settings.sessionTimeout"
                class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Save -->
      <div class="flex justify-end">
        <button
          hlmBtn variant="default" class="cursor-pointer gap-2"
          (click)="saveSettings()"
        >
          <lucide-icon [img]="SaveIcon" [size]="16"></lucide-icon>
          {{ saving() ? 'Enregistrement...' : 'Enregistrer' }}
        </button>
      </div>
    </div>
  `,
})
export class AdminSettingsComponent implements OnInit {
  /** Aligné sur {@code V8__company_profile.sql} (seed Flyway). */
  private static readonly FLYWAY_ADDRESS_SEED =
    '123 Rue Principale, Ville, Province, Code Postal';

  readonly GlobeIcon = Globe;
  readonly BellIcon = Bell;
  readonly ShieldIcon = Shield;
  readonly MailIcon = Mail;
  readonly SaveIcon = Save;

  private readonly adminService = inject(AdminService);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly saving = signal(false);

  companyAddress: CompanyAddressPayload = {
    addressLine: '',
    cityRegion: '',
  };

  settings = {
    companyName: '',
    supportEmail: '',
    siteUrl: '',
    maxLoginAttempts: 5,
    sessionTimeout: 30,
  };

  notificationOptions = [
    { key: 'newOrder', label: 'Nouvelle commande', description: 'Recevoir un email pour chaque nouvelle commande', enabled: true },
    { key: 'newUser', label: 'Nouvel utilisateur', description: 'Notification lors d\'une nouvelle inscription', enabled: true },
    { key: 'newAppointment', label: 'Nouveau rendez-vous', description: 'Notification pour chaque demande de rendez-vous', enabled: true },
    { key: 'contactForm', label: 'Formulaire de contact', description: 'Email lorsqu\'un message est envoyé via le formulaire', enabled: true },
  ];

  isAddressStillFlywaySeed(): boolean {
    return this.companyAddress.addressLine.trim() === AdminSettingsComponent.FLYWAY_ADDRESS_SEED;
  }

  ngOnInit(): void {
    this.adminService.getCompanyAddress().subscribe({
      next: (c) => {
        // withFetch() peut livrer hors zone ; ngModel peut rester visuellement figé sans CD explicite
        this.ngZone.run(() => {
          this.companyAddress.addressLine = c.addressLine;
          this.companyAddress.cityRegion = c.cityRegion;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        /* session ou réseau : champs vides, l’admin peut saisir */
      },
    });
  }

  saveSettings(): void {
    this.saving.set(true);
    this.adminService
      .updateCompanyAddress(this.companyAddress)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (saved) => {
          this.ngZone.run(() => {
            this.companyAddress.addressLine = saved.addressLine;
            this.companyAddress.cityRegion = saved.cityRegion;
            this.cdr.detectChanges();
          });
        },
        error: () => {
          /* erreur validation ou réseau */
        },
      });
  }
}
