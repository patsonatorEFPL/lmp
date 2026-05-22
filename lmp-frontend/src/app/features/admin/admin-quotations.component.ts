import { Component, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import {
  LucideAngularModule,
  RefreshCw,
  Filter,
  Loader2,
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';
import { AppCurrencyPipe } from '../../shared/pipes/app-currency.pipe';

interface QuotationItem {
  id: string;
  number: string;
  client: string;
  subject: string;
  amount: number;
  status: string;
  date: string;
  validUntil: string;
}

@Component({
  selector: 'lmp-admin-quotations',
  standalone: true,
  imports: [DatePipe, NgClass, FormsModule, LucideAngularModule, HlmButton, AppCurrencyPipe],
  template: `
    <div class="flex h-full flex-col overflow-hidden">
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
            class="h-7 cursor-pointer gap-1.5 px-2 text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200"
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
            placeholder="Rechercher…"
            class="h-8 w-56 rounded-lg border border-zinc-200 bg-zinc-50 px-3 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
          />
          <select
            [(ngModel)]="statusFilter"
            (change)="applyFilter()"
            class="h-8 w-44 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
          >
            <option value="">Tous les statuts</option>
            <option value="DRAFT">Brouillon</option>
            <option value="OPEN">Ouvert</option>
            <option value="REPLIED">Répondu</option>
            <option value="ORDERED">Commandé</option>
            <option value="EXPIRED">Expiré</option>
            <option value="LOST">Perdu</option>
            <option value="CANCELLED">Annulé</option>
          </select>
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
            <div class="w-32 shrink-0 px-2">N° devis</div>
            <div class="w-48 shrink-0 px-2">Client</div>
            <div class="w-56 shrink-0 px-2">Objet</div>
            <div class="w-28 shrink-0 px-2 text-right">Montant</div>
            <div class="w-28 shrink-0 px-2 text-center">Statut</div>
            <div class="w-32 shrink-0 px-2 text-center">Date</div>
            <div class="w-36 shrink-0 px-2 text-center">Valide jusqu'au</div>
          </div>

          <!-- Data rows -->
          <div>
            @for (item of filteredItems(); track item.id) {
              <div
                class="group flex h-10 min-w-max cursor-pointer items-center border-b border-zinc-50 hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
              >
                <div class="w-32 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">
                  {{ item.number }}
                </div>
                <div class="w-48 shrink-0 truncate px-2 text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ item.client }}
                </div>
                <div class="w-56 shrink-0 truncate px-2 text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ item.subject }}
                </div>
                <div class="w-28 shrink-0 truncate px-2 text-right text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ item.amount | appCurrency }}
                </div>
                <div class="w-28 shrink-0 px-2 text-center">
                  <span
                    class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusClass(item.status)"
                  >
                    {{ getStatusLabel(item.status) }}
                  </span>
                </div>
                <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ item.date | date:'dd/MM/yyyy' }}
                </div>
                <div class="w-36 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ item.validUntil | date:'dd/MM/yyyy' }}
                </div>
              </div>
            } @empty {
              <div class="py-12 text-center text-sm text-zinc-500">
                Aucun devis trouvé
              </div>
            }
          </div>
        }
      </div>

      <!-- Pagination footer -->
      <div class="flex items-center justify-between border-t border-zinc-200 py-2 px-3 sm:px-5 dark:border-zinc-800">
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
          <span>sur</span>
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ items().length }}</span>
        </div>
      </div>
    </div>
  `,
})
export class AdminQuotationsComponent implements OnInit {
  readonly RefreshCwIcon = RefreshCw;
  readonly FilterIcon = Filter;
  readonly Loader2Icon = Loader2;

  readonly loading = signal(false);
  readonly items = signal<QuotationItem[]>([]);
  readonly filteredItems = signal<QuotationItem[]>([]);
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
      result = result.filter(
        (i) =>
          i.number.toLowerCase().includes(q) ||
          i.client.toLowerCase().includes(q) ||
          i.subject.toLowerCase().includes(q),
      );
    }
    if (this.statusFilter) {
      result = result.filter((i) => i.status === this.statusFilter);
    }
    this.filteredItems.set(result);
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      DRAFT: 'Brouillon',
      OPEN: 'Ouvert',
      REPLIED: 'Répondu',
      ORDERED: 'Commandé',
      EXPIRED: 'Expiré',
      LOST: 'Perdu',
      CANCELLED: 'Annulé',
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'DRAFT':
        return 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-300';
      case 'OPEN':
      case 'REPLIED':
        return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
      case 'ORDERED':
        return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
      case 'EXPIRED':
      case 'LOST':
      case 'CANCELLED':
        return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
      default:
        return 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-300';
    }
  }
}
