import {
  Component,
  OnInit,
  signal,
} from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import {
  LucideAngularModule,
  Shield,
  RefreshCw,
  Filter,
  Loader2,
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';

interface SecurityItem {
  id: string;
  user: string;
  role: string;
  lastLogin: string;
  ip: string;
  status: 'Actif' | 'Inactif' | 'Bloqué';
}

@Component({
  selector: 'lmp-admin-security',
  standalone: true,
  imports: [DatePipe, NgClass, FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="flex h-full flex-col overflow-hidden bg-(--background)">
      <!-- Toolbar -->
      <div class="flex items-center justify-between gap-2 px-5 py-4">
        <div class="flex items-center"></div>
        <div class="flex items-center gap-0.5">
          <button
            hlmBtn variant="ghost" size="icon" type="button"
            class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
            title="Actualiser"
            (click)="load()"
          >
            <lucide-icon [img]="RefreshCwIcon" [size]="15" [ngClass]="{ 'animate-spin': loading() }"></lucide-icon>
          </button>
          <button
            hlmBtn variant="ghost" size="sm" type="button"
            class="h-7 cursor-pointer gap-1.5 px-2"
            [ngClass]="showFilterPanel() ? 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200' : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200'"
            (click)="showFilterPanel.set(!showFilterPanel())"
          >
            <lucide-icon [img]="FilterIcon" [size]="14"></lucide-icon>
            <span class="text-sm">Filtre</span>
          </button>
        </div>
      </div>

      <!-- Filter panel -->
      @if (showFilterPanel()) {
        <div class="flex items-center gap-2 border-b border-zinc-100 px-5 pb-3 dark:border-zinc-800">
          <input
            type="text"
            [(ngModel)]="searchQuery"
            (input)="applyFilter()"
            placeholder="Rechercher un utilisateur…"
            class="h-8 w-56 rounded-lg border border-zinc-200 bg-zinc-50 px-3 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
          />
          <select
            [(ngModel)]="statusFilter"
            (change)="applyFilter()"
            class="h-8 w-36 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
          >
            <option value="">Tous les statuts</option>
            <option value="Actif">Actif</option>
            <option value="Inactif">Inactif</option>
            <option value="Bloqué">Bloqué</option>
          </select>
          @if (searchQuery || statusFilter) {
            <button
              type="button"
              class="flex h-8 cursor-pointer items-center gap-1 rounded-lg px-2 text-sm text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800"
              (click)="clearFilters()"
            >
              <lucide-icon [img]="RefreshCwIcon" [size]="14"></lucide-icon>
              Effacer
            </button>
          }
        </div>
      }

      <!-- Data table -->
      <div class="flex-1 overflow-auto px-3 sm:px-5">
        @if (loading()) {
          <div class="flex items-center justify-center py-16">
            <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-zinc-400"></lucide-icon>
          </div>
        } @else {
          <!-- Header row -->
          <div class="mb-2 flex min-w-max items-center rounded-lg bg-zinc-100 py-1.5 text-sm font-normal leading-none text-zinc-500 dark:bg-zinc-800/70 dark:text-zinc-400">
            <div class="w-48 shrink-0 px-2">Utilisateur</div>
            <div class="w-32 shrink-0 px-2 text-center">Rôle</div>
            <div class="w-40 shrink-0 px-2 text-center">Dernière connexion</div>
            <div class="w-32 shrink-0 px-2 text-center">IP</div>
            <div class="w-28 shrink-0 px-2 text-center">Statut</div>
            <div class="w-24 shrink-0 px-2 text-center">Actions</div>
          </div>

          <!-- Data rows -->
          <div>
            @for (item of filteredItems(); track item.id) {
              <div
                class="group flex h-10 min-w-max cursor-pointer items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
              >
                <div class="w-48 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">
                  {{ item.user }}
                </div>
                <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ item.role }}
                </div>
                <div class="w-40 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ item.lastLogin | date:'dd/MM/yyyy HH:mm' }}
                </div>
                <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  <span class="font-mono text-xs">{{ item.ip }}</span>
                </div>
                <div class="w-28 shrink-0 px-2 text-center">
                  <span
                    class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusClass(item.status)"
                  >
                    {{ item.status }}
                  </span>
                </div>
                <div class="w-24 shrink-0 px-2 text-center">
                  <button
                    hlmBtn variant="ghost" size="icon" type="button"
                    class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
                    title="Actions"
                  >
                    <lucide-icon [img]="ShieldIcon" [size]="14"></lucide-icon>
                  </button>
                </div>
              </div>
            } @empty {
              <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
                Aucune donnée de sécurité trouvée
              </div>
            }
          </div>
        }
      </div>

      <!-- Pagination footer -->
      <div class="flex items-center justify-between border-t border-zinc-200 px-3 py-2 sm:px-5 dark:border-zinc-800">
        <div class="inline-flex rounded-md border border-zinc-200 dark:border-zinc-700">
          @for (size of pageSizes; track size; let first = $first; let last = $last) {
            <button
              type="button"
              class="h-7 min-w-[2.25rem] px-2.5 text-sm font-normal transition-colors"
              [ngClass]="{
                'rounded-l-md': first,
                'rounded-r-md': last,
                'border-r border-zinc-200 dark:border-zinc-700': !last,
                'bg-zinc-100 text-zinc-900 dark:bg-zinc-800 dark:text-zinc-100': pageSize() === size,
                'bg-white text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900 dark:bg-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200': pageSize() !== size
              }"
              (click)="changePageSize(size)"
            >
              {{ size }}
            </button>
          }
        </div>
        <div class="flex items-center gap-1 text-sm text-zinc-500 dark:text-zinc-400">
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ filteredItems().length }}</span>
          <span>of</span>
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ items().length }}</span>
        </div>
      </div>
    </div>
  `,
})
export class AdminSecurityComponent implements OnInit {
  readonly ShieldIcon = Shield;
  readonly RefreshCwIcon = RefreshCw;
  readonly FilterIcon = Filter;
  readonly Loader2Icon = Loader2;

  readonly loading = signal(false);
  readonly items = signal<SecurityItem[]>([]);
  readonly filteredItems = signal<SecurityItem[]>([]);
  readonly showFilterPanel = signal(false);
  readonly pageSize = signal(20);
  readonly pageSizes = [20, 50, 100];

  searchQuery = '';
  statusFilter = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    setTimeout(() => {
      this.items.set([]);
      this.filteredItems.set([]);
      this.loading.set(false);
    }, 300);
  }

  applyFilter(): void {
    let result = this.items();
    const q = this.searchQuery.toLowerCase();
    if (q) {
      result = result.filter((i) => i.user.toLowerCase().includes(q) || i.role.toLowerCase().includes(q));
    }
    if (this.statusFilter) {
      result = result.filter((i) => i.status === this.statusFilter);
    }
    this.filteredItems.set(result);
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.statusFilter = '';
    this.filteredItems.set(this.items());
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Actif':
        return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400';
      case 'Inactif':
        return 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-400';
      case 'Bloqué':
        return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
      default:
        return 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-400';
    }
  }
}
