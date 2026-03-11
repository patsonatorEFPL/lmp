import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
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

@Component({
  selector: 'lmp-admin-settings',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div>
      <h1 class="font-display text-2xl font-bold text-(--foreground)">
        Paramètres du système
      </h1>
      <p class="mt-1 text-sm text-(--muted-foreground)">
        Configurez les paramètres globaux de la plateforme.
      </p>
    </div>

    <div class="mt-8 space-y-6">
      <!-- General Settings -->
      <div class="rounded-xl border border-(--border) bg-(--card) p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-500/10">
            <lucide-icon [img]="GlobeIcon" [size]="18" class="text-blue-500"></lucide-icon>
          </div>
          <div>
            <h2 class="font-display text-sm font-semibold text-(--foreground)">Général</h2>
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
                class="mt-1 w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              />
            </div>
            <div>
              <label class="text-xs font-medium text-(--foreground)">Email de support</label>
              <input
                type="email"
                [(ngModel)]="settings.supportEmail"
                class="mt-1 w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              />
            </div>
          </div>
          <div>
            <label class="text-xs font-medium text-(--foreground)">URL du site</label>
            <input
              type="url"
              [(ngModel)]="settings.siteUrl"
              class="mt-1 w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
            />
          </div>
        </div>
      </div>

      <!-- Notification Settings -->
      <div class="rounded-xl border border-(--border) bg-(--card) p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-500/10">
            <lucide-icon [img]="BellIcon" [size]="18" class="text-amber-500"></lucide-icon>
          </div>
          <div>
            <h2 class="font-display text-sm font-semibold text-(--foreground)">Notifications</h2>
            <p class="text-xs text-(--muted-foreground)">Configurer les notifications email</p>
          </div>
        </div>
        <div class="space-y-3">
          @for (notif of notificationOptions; track notif.key) {
            <div class="flex items-center justify-between rounded-lg border border-(--border) px-4 py-3">
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
      <div class="rounded-xl border border-(--border) bg-(--card) p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-red-500/10">
            <lucide-icon [img]="ShieldIcon" [size]="18" class="text-red-500"></lucide-icon>
          </div>
          <div>
            <h2 class="font-display text-sm font-semibold text-(--foreground)">Sécurité</h2>
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
                class="mt-1 w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              />
            </div>
            <div>
              <label class="text-xs font-medium text-(--foreground)">Durée de session (minutes)</label>
              <input
                type="number"
                [(ngModel)]="settings.sessionTimeout"
                class="mt-1 w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
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
          {{ saving() ? 'Enregistrement...' : 'Enregistrer les paramètres' }}
        </button>
      </div>
    </div>
  `,
})
export class AdminSettingsComponent {
  readonly GlobeIcon = Globe;
  readonly BellIcon = Bell;
  readonly ShieldIcon = Shield;
  readonly MailIcon = Mail;
  readonly SaveIcon = Save;

  readonly saving = signal(false);

  settings = {
    companyName: 'LMP Services',
    supportEmail: 'lmp.assistance@gmail.com',
    siteUrl: 'https://lmp-services.ca',
    maxLoginAttempts: 5,
    sessionTimeout: 30,
  };

  notificationOptions = [
    { key: 'newOrder', label: 'Nouvelle commande', description: 'Recevoir un email pour chaque nouvelle commande', enabled: true },
    { key: 'newUser', label: 'Nouvel utilisateur', description: 'Notification lors d\'une nouvelle inscription', enabled: true },
    { key: 'newAppointment', label: 'Nouveau rendez-vous', description: 'Notification pour chaque demande de rendez-vous', enabled: true },
    { key: 'contactForm', label: 'Formulaire de contact', description: 'Email lorsqu\'un message est envoyé via le formulaire', enabled: true },
  ];

  saveSettings(): void {
    this.saving.set(true);
    // TODO: Connect to backend settings API
    setTimeout(() => {
      this.saving.set(false);
    }, 1000);
  }
}
