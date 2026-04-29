import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import {
  LucideAngularModule,
  RefreshCw,
  Save,
  Server,
  Trash2,
  Plus,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';

interface ConfigItem {
  key: string;
  value: string;
  isEditing: boolean;
}

@Component({
  selector: 'lmp-admin-site-config',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="rounded-sm border border-(--border) bg-(--card) p-6">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-3">
          <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted)">
            <lucide-icon [img]="ServerIcon" [size]="18" class="text-(--foreground)"></lucide-icon>
          </div>
          <div>
            <h2 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">Configuration du site</h2>
            <p class="text-xs text-(--muted-foreground)">Variables dérivées de SITE_URL — modifiables à chaud</p>
          </div>
        </div>
        <div class="flex gap-2">
          <button hlmBtn variant="outline" size="sm" (click)="load()" [disabled]="loading()">
            <lucide-icon [img]="RefreshCwIcon" [size]="14" class="mr-1"></lucide-icon>
            Charger DB
          </button>
          <button hlmBtn variant="outline" size="sm" (click)="reloadFile()" [disabled]="reloading()">
            <lucide-icon [img]="RefreshCwIcon" [size]="14" class="mr-1"></lucide-icon>
            Reload JSON
          </button>
          <button hlmBtn variant="outline" size="sm" (click)="reloadAll()" [disabled]="reloading()">
            <lucide-icon [img]="RefreshCwIcon" [size]="14" class="mr-1"></lucide-icon>
            Reload All
          </button>
        </div>
      </div>

      @if (error()) {
        <div class="mb-4 rounded-sm border border-red-200 bg-red-50 p-3 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
          {{ error() }}
        </div>
      }

      @if (success()) {
        <div class="mb-4 rounded-sm border border-green-200 bg-green-50 p-3 text-sm text-green-700 dark:border-green-900 dark:bg-green-950 dark:text-green-300">
          {{ success() }}
        </div>
      }

      <div class="space-y-2">
        @for (item of items(); track item.key) {
          <div class="flex items-center gap-2 rounded-sm border border-(--border) bg-(--background) px-3 py-2">
            <div class="w-48 shrink-0">
              <span class="text-xs font-mono text-(--muted-foreground)">{{ item.key }}</span>
            </div>
            @if (item.isEditing) {
              <input
                type="text"
                [(ngModel)]="item.value"
                class="flex-1 rounded-sm border border-(--border) bg-(--card) px-2 py-1 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
              />
              <button hlmBtn variant="outline" size="sm" (click)="save(item)">
                <lucide-icon [img]="SaveIcon" [size]="14"></lucide-icon>
              </button>
              <button hlmBtn variant="outline" size="sm" (click)="cancel(item)">
                <lucide-icon [img]="Trash2Icon" [size]="14"></lucide-icon>
              </button>
            } @else {
              <div class="flex-1 truncate text-sm text-(--foreground)">{{ item.value }}</div>
              <button hlmBtn variant="outline" size="sm" (click)="item.isEditing = true">
                <lucide-icon [img]="SaveIcon" [size]="14"></lucide-icon>
              </button>
            }
          </div>
        } @empty {
          <div class="text-center py-8 text-sm text-(--muted-foreground)">
            Cliquez sur "Charger DB" pour afficher la configuration.
          </div>
        }
      </div>
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
  readonly SaveIcon = Save;
  readonly ServerIcon = Server;
  readonly Trash2Icon = Trash2;
  readonly PlusIcon = Plus;

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
            isEditing: false,
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
    item.isEditing = false;
    this.load();
  }

  reloadFile(): void {
    this.reloading.set(true);
    this.http.post<any>('/api/v1/admin/site-config/reload-file', {}).subscribe({
      next: () => {
        this.success.set('Fichier site-config.json rechargé');
        this.reloading.set(false);
        setTimeout(() => this.success.set(null), 3000);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Erreur reload');
        this.reloading.set(false);
      },
    });
  }

  reloadAll(): void {
    this.reloading.set(true);
    this.http.post<any>('/api/v1/admin/site-config/reload-all', {}).subscribe({
      next: () => {
        this.success.set('Configuration complète rechargée');
        this.reloading.set(false);
        setTimeout(() => this.success.set(null), 3000);
      },
      error: (err) => {
        this.error.set(err?.error?.message || 'Erreur reload');
        this.reloading.set(false);
      },
    });
  }
}
