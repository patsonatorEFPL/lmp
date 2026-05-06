import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import {
  LucideAngularModule,
  RefreshCw,
  Save,
  Server,
  X,
  Pencil,
  Building2,
  Mail,
  Link2,
  Coins,
  RefreshCcw,
  KeyRound,
  Settings,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';

interface ConfigItem {
  key: string;
  value: string;
  originalValue: string;
  isEditing: boolean;
  category: string;
}

interface CategoryDef {
  id: string;
  label: string;
  icon: any;
  prefixes: string[];
  description: string;
}

const CATEGORIES: CategoryDef[] = [
  {
    id: 'branding',
    label: 'Marque & entreprise',
    icon: Building2,
    prefixes: ['app.name', 'company.'],
    description: 'Nom du site, raison sociale, adresse, logo',
  },
  {
    id: 'email',
    label: 'Emails',
    icon: Mail,
    prefixes: ['mail.from.', 'mail.replyto.', 'mail.domain'],
    description: 'Adresses expéditrices et reply-to',
  },
  {
    id: 'crm-sso',
    label: 'CRM & SSO Frappe',
    icon: Link2,
    prefixes: ['lmp.crm.', 'app.oauth2.erp.', 'app.oauth2.external.', 'lmp.sync.'],
    description: 'Frappe/ERPNext SSO, sync bidirectionnelle',
  },
  {
    id: 'pricing',
    label: 'Pricing & VAT',
    icon: Coins,
    prefixes: ['pricing.', 'geoip.'],
    description: 'TVA, taux de change, géolocalisation',
  },
  {
    id: 'oauth-public',
    label: 'OAuth2 public',
    icon: KeyRound,
    prefixes: ['spring.security.oauth2.client.registration.google.client-id',
               'spring.security.oauth2.client.registration.microsoft.client-id'],
    description: 'Client IDs Google/Microsoft (secrets restent en env vars)',
  },
  {
    id: 'site',
    label: 'Site (URLs canoniques)',
    icon: Server,
    prefixes: ['app.base.url', 'app.frontend.url', 'app.oauth2.issuer-uri',
               'app.cors.allowed-origins', 'lmp.site.url'],
    description: 'Dérivé de SITE_URL — modifier avec précaution',
  },
  {
    id: 'misc',
    label: 'Divers',
    icon: Settings,
    prefixes: [],
    description: 'Autres clés non catégorisées',
  },
];

function categorizeKey(key: string): string {
  for (const cat of CATEGORIES) {
    if (cat.prefixes.some(p => key === p || key.startsWith(p))) {
      return cat.id;
    }
  }
  return 'misc';
}

@Component({
  selector: 'lmp-admin-site-config',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="space-y-4">
      <div class="rounded-sm border border-(--border) bg-(--card) p-6">
        <div class="flex items-start justify-between">
          <div class="flex items-center gap-3">
            <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)">
              <lucide-icon [img]="ServerIcon" [size]="18" class="text-(--foreground)"></lucide-icon>
            </div>
            <div>
              <h2 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">Configuration du site</h2>
              <p class="text-xs text-(--muted-foreground)">Variables persistées en DB — modifications immédiates sans redémarrage</p>
            </div>
          </div>
          <div class="flex gap-2">
            <button hlmBtn variant="outline" size="sm" (click)="load()" [disabled]="loading()">
              <lucide-icon [img]="RefreshCwIcon" [size]="14" class="mr-1"></lucide-icon>
              Recharger
            </button>
            <button hlmBtn variant="outline" size="sm" (click)="reloadAll()" [disabled]="reloading()">
              <lucide-icon [img]="RefreshCcwIcon" [size]="14" class="mr-1"></lucide-icon>
              Reset cache
            </button>
          </div>
        </div>

        @if (error()) {
          <div class="mt-4 rounded-sm border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
            {{ error() }}
          </div>
        }
        @if (success()) {
          <div class="mt-4 rounded-sm border border-green-200 bg-green-50 p-3 text-sm text-green-700 dark:border-green-900 dark:bg-green-950 dark:text-green-300">
            {{ success() }}
          </div>
        }
      </div>

      @for (cat of categoriesWithItems(); track cat.id) {
        <div class="rounded-sm border border-(--border) bg-(--card) p-6">
          <div class="mb-4 flex items-center gap-3">
            <div class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted)">
              <lucide-icon [img]="cat.icon" [size]="16" class="text-(--foreground)"></lucide-icon>
            </div>
            <div>
              <h3 class="text-sm font-medium text-(--foreground)">{{ cat.label }}</h3>
              <p class="text-xs text-(--muted-foreground)">{{ cat.description }}</p>
            </div>
            <span class="ml-auto text-xs text-(--muted-foreground)">{{ cat.items.length }} clé(s)</span>
          </div>

          <div class="space-y-2">
            @for (item of cat.items; track item.key) {
              <div class="flex items-center gap-2 rounded-sm border border-(--border) bg-(--background) px-3 py-2">
                <div class="w-64 shrink-0">
                  <span class="text-xs font-mono text-(--muted-foreground)">{{ item.key }}</span>
                </div>
                @if (item.isEditing) {
                  <input
                    [type]="inputType(item.key)"
                    [(ngModel)]="item.value"
                    class="flex-1 rounded-sm border border-(--border) bg-(--card) px-2 py-1 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
                  />
                  <button hlmBtn variant="default" size="sm" (click)="save(item)" title="Enregistrer">
                    <lucide-icon [img]="SaveIcon" [size]="14"></lucide-icon>
                  </button>
                  <button hlmBtn variant="outline" size="sm" (click)="cancel(item)" title="Annuler">
                    <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
                  </button>
                } @else {
                  <div class="flex-1 truncate text-sm text-(--foreground)">{{ item.value || '—' }}</div>
                  <button hlmBtn variant="outline" size="sm" (click)="item.isEditing = true" title="Modifier">
                    <lucide-icon [img]="PencilIcon" [size]="14"></lucide-icon>
                  </button>
                }
              </div>
            }
          </div>
        </div>
      } @empty {
        <div class="rounded-sm border border-(--border) bg-(--card) p-12 text-center text-sm text-(--muted-foreground)">
          Cliquez sur "Recharger" pour afficher la configuration.
        </div>
      }
    </div>
  `,
})
export class AdminSiteConfigComponent {
  private readonly http = inject(HttpClient);

  readonly items = signal<ConfigItem[]>([]);
  readonly loading = signal(false);
  readonly reloading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly RefreshCwIcon = RefreshCw;
  readonly RefreshCcwIcon = RefreshCcw;
  readonly SaveIcon = Save;
  readonly ServerIcon = Server;
  readonly XIcon = X;
  readonly PencilIcon = Pencil;

  readonly categoriesWithItems = computed(() => {
    const grouped = new Map<string, ConfigItem[]>();
    for (const item of this.items()) {
      const list = grouped.get(item.category) ?? [];
      list.push(item);
      grouped.set(item.category, list);
    }
    return CATEGORIES
      .map(cat => ({ ...cat, items: (grouped.get(cat.id) ?? []).sort((a, b) => a.key.localeCompare(b.key)) }))
      .filter(cat => cat.items.length > 0);
  });

  constructor() {
    this.load();
  }

  inputType(key: string): string {
    if (key.startsWith('mail.')) return 'email';
    if (key.includes('.url') || key.includes('issuer-uri') || key === 'company.website') return 'url';
    if (key.includes('rate') || key.includes('margin') || key.includes('size') || key.includes('ttl')) return 'number';
    return 'text';
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<any>('/api/v1/admin/site-config').subscribe({
      next: (res) => {
        const data = res.data ?? {};
        this.items.set(
          Object.entries(data).map(([key, value]) => ({
            key,
            value: String(value),
            originalValue: String(value),
            isEditing: false,
            category: categorizeKey(key),
          }))
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Erreur lors du chargement');
        this.loading.set(false);
      },
    });
  }

  save(item: ConfigItem): void {
    this.error.set(null);
    this.http.put<any>(`/api/v1/admin/site-config/${item.key}`, { value: item.value }).subscribe({
      next: () => {
        item.originalValue = item.value;
        item.isEditing = false;
        this.success.set(`${item.key} mis à jour`);
        setTimeout(() => this.success.set(null), 3000);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Erreur lors de la sauvegarde');
      },
    });
  }

  cancel(item: ConfigItem): void {
    item.value = item.originalValue;
    item.isEditing = false;
  }

  reloadAll(): void {
    this.reloading.set(true);
    this.http.post<any>('/api/v1/admin/site-config/reload-all', {}).subscribe({
      next: () => {
        this.success.set('Cache configuration vidé');
        this.reloading.set(false);
        this.load();
        setTimeout(() => this.success.set(null), 3000);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Erreur reload');
        this.reloading.set(false);
      },
    });
  }
}
