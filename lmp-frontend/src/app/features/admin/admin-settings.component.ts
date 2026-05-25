import { ChangeDetectorRef, Component, DestroyRef, inject, NgZone, OnInit, signal } from '@angular/core';
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
  Eye,
  EyeOff,
  Check,
  Loader2,
  AlertCircle,
  KeyRound,
  Activity,
  Wifi,
  WifiOff,
  RefreshCw,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { AdminService, CompanyAddressPayload } from '../../core/services/admin.service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AdminSiteConfigComponent } from './admin-site-config.component';
import { VisiblePollService } from '../../core/services/visible-poll.service';

type DispatcherStrategy = 'smtp' | 'erpnext';
type HealthStatus = 'UP' | 'DOWN';
interface ErpHealth { status: HealthStatus; latencyMs: number; lastError: string; message: string; }
interface ApiAck { success: boolean; message: string; }

@Component({
  selector: 'lmp-admin-settings',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, HlmButton, AdminSiteConfigComponent],
  template: `
    <div class="p-4 sm:p-5">
    <!-- Titre dans lmp-admin-layout -->
    <div class="space-y-6">
      <lmp-admin-site-config></lmp-admin-site-config>

      <!-- General Settings -->
      <div class="rounded-sm border border-(--border) bg-(--card) p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)">
            <lucide-icon [img]="GlobeIcon" [size]="18" class="text-(--foreground)"></lucide-icon>
          </div>
          <div>
            <h2 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">Général</h2>
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
            <h2 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">Notifications</h2>
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

      <!-- Email Configuration -->
      <div class="rounded-sm border border-(--border) bg-(--card) p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)">
            <lucide-icon [img]="ActivityIcon" [size]="18" class="text-(--foreground)"></lucide-icon>
          </div>
          <div>
            <h2 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">Configuration Email</h2>
            <p class="text-xs text-(--muted-foreground)">État du relais ERPNext</p>
          </div>
        </div>
        <div class="space-y-3">
          @if (erpnextHealth(); as health) {
            <div class="flex items-center justify-between rounded-sm border border-(--border) px-4 py-3">
              <div class="flex items-center gap-3">
                <div class="flex h-8 w-8 items-center justify-center rounded-full"
                     [class.bg-emerald-500\/15]="health.status === 'UP'"
                     [class.text-emerald-500]="health.status === 'UP'"
                     [class.bg-red-500\/15]="health.status === 'DOWN'"
                     [class.text-red-500]="health.status === 'DOWN'">
                  <lucide-icon [img]="health.status === 'UP' ? WifiIcon : WifiOffIcon" [size]="16"></lucide-icon>
                </div>
                <div>
                  <p class="text-sm font-medium text-(--foreground)">Connexion ERPNext Email</p>
                  <p class="text-xs text-(--muted-foreground)">
                    @if (health.status === 'UP') {
                      Latence {{ health.latencyMs }}ms — {{ health.message }}
                    } @else {
                      {{ health.lastError || 'Indisponible' }}
                    }
                  </p>
                </div>
              </div>
              <span data-testid="erpnext-health-badge"
                    class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
                    [class.bg-emerald-500\/15]="health.status === 'UP'"
                    [class.text-emerald-500]="health.status === 'UP'"
                    [class.bg-red-500\/15]="health.status === 'DOWN'"
                    [class.text-red-500]="health.status === 'DOWN'">
                {{ health.status === 'UP' ? 'En ligne' : 'Hors ligne' }}
              </span>
            </div>
          } @else {
            <div class="flex items-center gap-2 text-sm text-(--muted-foreground)">
              <lucide-icon [img]="Loader2Icon" [size]="16" class="animate-spin"></lucide-icon>
              Vérification du relais ERPNext…
            </div>
          }

          <!-- Dispatcher switcher -->
          <div class="mt-4 rounded-sm border border-(--border) px-4 py-3">
            <p class="mb-2 text-sm font-medium text-(--foreground)">Moteur d'envoi actif</p>
            <div class="flex items-center gap-2">
              <button
                class="rounded-sm px-3 py-1.5 text-xs font-medium transition-colors"
                [class.bg-(--primary)]="dispatcherStrategy() === 'smtp'"
                [class.text-(--primary-foreground)]="dispatcherStrategy() === 'smtp'"
                [class.bg-(--muted)]="dispatcherStrategy() !== 'smtp'"
                [class.text-(--muted-foreground)]="dispatcherStrategy() !== 'smtp'"
                (click)="setDispatcher('smtp')"
                [disabled]="changingDispatcher()"
              >
                SMTP (Mailtrap)
              </button>
              <button
                class="rounded-sm px-3 py-1.5 text-xs font-medium transition-colors"
                [class.bg-(--primary)]="dispatcherStrategy() === 'erpnext'"
                [class.text-(--primary-foreground)]="dispatcherStrategy() === 'erpnext'"
                [class.bg-(--muted)]="dispatcherStrategy() !== 'erpnext'"
                [class.text-(--muted-foreground)]="dispatcherStrategy() !== 'erpnext'"
                (click)="setDispatcher('erpnext')"
                [disabled]="changingDispatcher()"
              >
                ERPNext
              </button>
            </div>
            <div class="mt-3 flex items-center gap-2">
              <button
                hlmBtn variant="outline" size="sm" class="cursor-pointer gap-1"
                (click)="testDispatcherEmail()"
              >
                <lucide-icon [img]="MailIcon" [size]="14"></lucide-icon>
                Tester l'envoi
              </button>
              @if (dispatcherTestResult(); as result) {
                <span class="text-xs text-(--foreground)">{{ result }}</span>
              }
            </div>
          </div>
        </div>
      </div>

      <!-- Admin Password Change -->
      <div class="rounded-sm border border-(--border) bg-(--card) p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--primary)/10">
            <lucide-icon [img]="KeyRoundIcon" [size]="18" class="text-(--primary)"></lucide-icon>
          </div>
          <div>
            <h2 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">Mot de passe administrateur</h2>
            <p class="text-xs text-(--muted-foreground)">Modifier votre mot de passe de connexion</p>
          </div>
        </div>

        @if (pwdSuccess()) {
          <div class="mb-4 flex items-center gap-2 rounded-sm border border-emerald-500/20 bg-(--muted) p-3 text-sm text-(--foreground)">
            <lucide-icon [img]="CheckIcon" [size]="16"></lucide-icon>
            {{ pwdSuccess() }}
          </div>
        }
        @if (pwdError()) {
          <div class="mb-4 flex items-center gap-2 rounded-sm border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-400">
            <lucide-icon [img]="AlertCircleIcon" [size]="16"></lucide-icon>
            {{ pwdError() }}
          </div>
        }

        <div class="space-y-4">
          <div>
            <label class="text-xs font-medium text-(--foreground)">Mot de passe actuel</label>
            <div class="relative mt-1">
              <input
                [type]="showCurrentPwd() ? 'text' : 'password'"
                [(ngModel)]="pwdForm.currentPassword"
                placeholder="••••••••"
                autocomplete="current-password"
                class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 pr-10 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              />
              <button
                type="button"
                class="absolute right-2 top-1/2 -translate-y-1/2 cursor-pointer text-(--muted-foreground) hover:text-(--foreground)"
                (click)="showCurrentPwd.set(!showCurrentPwd())"
              >
                <lucide-icon [img]="showCurrentPwd() ? EyeOffIcon : EyeIcon" [size]="16"></lucide-icon>
              </button>
            </div>
          </div>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label class="text-xs font-medium text-(--foreground)">Nouveau mot de passe</label>
              <div class="relative mt-1">
                <input
                  [type]="showNewPwd() ? 'text' : 'password'"
                  [(ngModel)]="pwdForm.newPassword"
                  placeholder="Min. 8 caractères"
                  autocomplete="new-password"
                  class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 pr-10 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
                />
                <button
                  type="button"
                  class="absolute right-2 top-1/2 -translate-y-1/2 cursor-pointer text-(--muted-foreground) hover:text-(--foreground)"
                  (click)="showNewPwd.set(!showNewPwd())"
                >
                  <lucide-icon [img]="showNewPwd() ? EyeOffIcon : EyeIcon" [size]="16"></lucide-icon>
                </button>
              </div>
            </div>
            <div>
              <label class="text-xs font-medium text-(--foreground)">Confirmer le nouveau mot de passe</label>
              <input
                [type]="showNewPwd() ? 'text' : 'password'"
                [(ngModel)]="pwdForm.confirmPassword"
                placeholder="Confirmer"
                autocomplete="new-password"
                class="mt-1 w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              />
            </div>
          </div>
        </div>

        <div class="mt-4 flex justify-end">
          <button
            hlmBtn variant="outline" class="cursor-pointer gap-2"
            (click)="changeOwnPassword()"
            [disabled]="changingPwd()"
          >
            @if (changingPwd()) {
              <lucide-icon [img]="Loader2Icon" [size]="16" class="animate-spin"></lucide-icon>
              Modification…
            } @else {
              <lucide-icon [img]="ShieldIcon" [size]="16"></lucide-icon>
              Modifier le mot de passe
            }
          </button>
        </div>
      </div>

      <!-- Security Settings -->
      <div class="rounded-sm border border-(--border) bg-(--card) p-6">
        <div class="flex items-center gap-3 mb-4">
          <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-red-500/10">
            <lucide-icon [img]="ShieldIcon" [size]="18" class="text-red-500"></lucide-icon>
          </div>
          <div>
            <h2 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">Sécurité</h2>
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
    </div>
  `,
})
export class AdminSettingsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly visiblePoll = inject(VisiblePollService);
  /** Aligné sur {@code V8__company_profile.sql} (seed Flyway). */
  private static readonly FLYWAY_ADDRESS_SEED =
    '123 Rue Principale, Ville, Province, Code Postal';

  readonly GlobeIcon = Globe;
  readonly BellIcon = Bell;
  readonly ShieldIcon = Shield;
  readonly MailIcon = Mail;
  readonly SaveIcon = Save;
  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;
  readonly CheckIcon = Check;
  readonly Loader2Icon = Loader2;
  readonly AlertCircleIcon = AlertCircle;
  readonly KeyRoundIcon = KeyRound;
  readonly ActivityIcon = Activity;
  readonly WifiIcon = Wifi;
  readonly WifiOffIcon = WifiOff;
  readonly RefreshCwIcon = RefreshCw;

  dispatcherStrategy = signal<DispatcherStrategy>('smtp');
  changingDispatcher = signal(false);
  dispatcherTestResult = signal<string | null>(null);
  private healthInflight = false;

  private readonly adminService = inject(AdminService);
  private readonly ngZone = inject(NgZone);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly saving = signal(false);

  // Password change
  pwdForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
  readonly changingPwd = signal(false);
  readonly pwdSuccess = signal<string | null>(null);
  readonly pwdError = signal<string | null>(null);
  readonly showCurrentPwd = signal(false);
  readonly showNewPwd = signal(false);

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

  erpnextHealth = signal<ErpHealth | null>(null);

  ngOnInit(): void {
    this.loadErpnextHealth();
    this.loadDispatcher();
    this.visiblePoll.subscribeWhileVisible(this.destroyRef, 10000, () => this.loadErpnextHealth());

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

  loadErpnextHealth(): void {
    if (this.healthInflight) return;
    this.healthInflight = true;
    this.http.get<ErpHealth>('/admin/email-test/erpnext-health')
      .subscribe({
        next: (data) => {
          this.ngZone.run(() => {
            this.erpnextHealth.set(data);
            this.healthInflight = false;
            this.cdr.detectChanges();
          });
        },
        error: () => {
          this.ngZone.run(() => {
            this.erpnextHealth.set({ status: 'DOWN', latencyMs: -1, lastError: 'Unreachable', message: '' });
            this.healthInflight = false;
            this.cdr.detectChanges();
          });
        },
      });
  }

  loadDispatcher(): void {
    this.http.get<{ strategy: DispatcherStrategy }>('/admin/email-test/dispatcher')
      .subscribe({
        next: (data) => this.dispatcherStrategy.set(data.strategy),
        error: () => this.dispatcherStrategy.set('smtp'),
      });
  }

  setDispatcher(strategy: DispatcherStrategy): void {
    this.changingDispatcher.set(true);
    const params = new HttpParams().set('strategy', strategy);
    this.http.post<ApiAck>('/admin/email-test/dispatcher', null, { params })
      .subscribe({
        next: (res) => {
          this.changingDispatcher.set(false);
          if (res.success) {
            this.dispatcherStrategy.set(strategy);
          }
        },
        error: () => this.changingDispatcher.set(false),
      });
  }

  testDispatcherEmail(): void {
    this.dispatcherTestResult.set('Envoi en cours…');
    const params = new HttpParams().set('testEmail', this.settings.supportEmail);
    this.http.post<ApiAck>('/admin/email-test/test', null, { params })
      .subscribe({
        next: (res) => {
          this.dispatcherTestResult.set(res.success ? '✅ ' + res.message : '❌ ' + res.message);
        },
        error: (err) => {
          this.dispatcherTestResult.set('❌ Échec : ' + (err.error?.message || err.message));
        },
      });
  }

  changeOwnPassword(): void {
    this.pwdSuccess.set(null);
    this.pwdError.set(null);

    if (!this.pwdForm.currentPassword || !this.pwdForm.newPassword) {
      this.pwdError.set('Veuillez remplir tous les champs.');
      return;
    }
    if (this.pwdForm.newPassword !== this.pwdForm.confirmPassword) {
      this.pwdError.set('Les mots de passe ne correspondent pas.');
      return;
    }

    const pwd = this.pwdForm.newPassword;
    if (pwd.length < 8 || !/[A-Z]/.test(pwd) || !/[a-z]/.test(pwd) || !/\d/.test(pwd) || !/[^A-Za-z0-9]/.test(pwd)) {
      this.pwdError.set('Le mot de passe doit contenir au moins 8 caractères, incluant majuscules, minuscules, chiffres et caractères spéciaux.');
      return;
    }

    this.changingPwd.set(true);
    this.adminService.changeOwnPassword(this.pwdForm).subscribe({
      next: () => {
        this.pwdSuccess.set('Mot de passe modifié avec succès.');
        this.pwdForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
        this.changingPwd.set(false);
        setTimeout(() => this.pwdSuccess.set(null), 5000);
      },
      error: (err: any) => {
        this.pwdError.set(
          err?.error?.message || err?.message || 'Erreur lors du changement de mot de passe.',
        );
        this.changingPwd.set(false);
      },
    });
  }
}
