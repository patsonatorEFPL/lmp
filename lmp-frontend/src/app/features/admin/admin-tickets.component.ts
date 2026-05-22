import { Component, OnInit, signal } from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import {
  LucideAngularModule,
  RefreshCw,
  Filter,
  Loader2,
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';

interface TicketItem {
  id: string;
  number: string;
  subject: string;
  client: string;
  priority: string;
  type: string;
  status: string;
  date: string;
}

@Component({
  selector: 'lmp-admin-tickets',
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
            placeholder="Rechercher un ticket…"
            class="h-8 w-56 rounded-lg border border-zinc-200 bg-zinc-50 px-3 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
          />
          <select
            [(ngModel)]="statusFilter"
            (change)="applyFilter()"
            class="h-8 w-40 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
          >
            <option value="">Tous les statuts</option>
            <option value="OPEN">Ouvert</option>
            <option value="REPLIED">Répondu</option>
            <option value="ON_HOLD">En attente</option>
            <option value="RESOLVED">Résolu</option>
            <option value="CLOSED">Fermé</option>
          </select>
          @if (hasActiveFilters()) {
            <button
              type="button"
              class="flex h-8 cursor-pointer items-center gap-1 rounded-lg px-2 text-sm text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800"
              (click)="clearFilters()"
            >
              <span>Effacer</span>
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
            <div class="w-20 shrink-0 px-2">N°</div>
            <div class="w-48 shrink-0 px-2">Sujet</div>
            <div class="w-40 shrink-0 px-2 text-center">Client</div>
            <div class="w-28 shrink-0 px-2 text-center">Priorité</div>
            <div class="w-32 shrink-0 px-2 text-center">Type</div>
            <div class="w-28 shrink-0 px-2 text-center">Statut</div>
            <div class="w-36 shrink-0 px-2 text-center">Date</div>
          </div>

          <!-- Data rows -->
          <div>
            @for (ticket of filteredItems(); track ticket.id) {
              <div
                class="group flex h-10 min-w-max cursor-pointer items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
              >
                <div class="w-20 shrink-0 px-2 text-sm font-mono leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ ticket.number }}
                </div>
                <div class="w-48 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">
                  {{ ticket.subject }}
                </div>
                <div class="w-40 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ ticket.client }}
                </div>
                <div class="w-28 shrink-0 px-2 text-center">
                  <span
                    class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getPriorityClass(ticket.priority)"
                  >
                    {{ ticket.priority }}
                  </span>
                </div>
                <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ ticket.type }}
                </div>
                <div class="w-28 shrink-0 px-2 text-center">
                  <span
                    class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                    [ngClass]="getStatusClass(ticket.status)"
                  >
                    {{ getStatusLabel(ticket.status) }}
                  </span>
                </div>
                <div class="w-36 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ ticket.date | date:'dd/MM/yyyy HH:mm' }}
                </div>
              </div>
            } @empty {
              <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
                Aucun ticket trouvé
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
export class AdminTicketsComponent implements OnInit {
  readonly RefreshCwIcon = RefreshCw;
  readonly FilterIcon = Filter;
  readonly Loader2Icon = Loader2;

  readonly loading = signal(false);
  readonly items = signal<TicketItem[]>([]);
  readonly filteredItems = signal<TicketItem[]>([]);
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
      this.items.set([
        {
          id: '1',
          number: '#1024',
          subject: 'Erreur de paiement Stripe',
          client: 'Jean Dupont',
          priority: 'High',
          type: 'Bug',
          status: 'OPEN',
          date: '2026-04-26T09:30:00',
        },
        {
          id: '2',
          number: '#1023',
          subject: 'Demande de remboursement',
          client: 'Marie Curie',
          priority: 'Medium',
          type: 'Billing',
          status: 'REPLIED',
          date: '2026-04-25T14:15:00',
        },
        {
          id: '3',
          number: '#1022',
          subject: 'Problème de connexion SSO',
          client: 'TechCorp',
          priority: 'Urgent',
          type: 'Bug',
          status: 'ON_HOLD',
          date: '2026-04-25T11:00:00',
        },
        {
          id: '4',
          number: '#1021',
          subject: 'Question sur la facturation',
          client: 'Pierre Martin',
          priority: 'Low',
          type: 'Question',
          status: 'RESOLVED',
          date: '2026-04-24T16:45:00',
        },
        {
          id: '5',
          number: '#1020',
          subject: 'Demande de fonctionnalité',
          client: 'Sophie Bernard',
          priority: 'Medium',
          type: 'Feature',
          status: 'CLOSED',
          date: '2026-04-23T08:20:00',
        },
      ]);
      this.applyFilter();
      this.loading.set(false);
    }, 300);
  }

  applyFilter(): void {
    let filtered = this.items();
    const q = this.searchQuery.toLowerCase();
    if (q) {
      filtered = filtered.filter(
        (t) =>
          t.subject.toLowerCase().includes(q) ||
          t.client.toLowerCase().includes(q) ||
          t.number.toLowerCase().includes(q),
      );
    }
    if (this.statusFilter) {
      filtered = filtered.filter((t) => t.status === this.statusFilter);
    }
    this.filteredItems.set(filtered);
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.statusFilter = '';
    this.applyFilter();
  }

  hasActiveFilters(): boolean {
    return !!this.searchQuery || !!this.statusFilter;
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN':
        return 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400';
      case 'REPLIED':
        return 'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400';
      case 'ON_HOLD':
        return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400';
      case 'RESOLVED':
        return 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
      case 'CLOSED':
        return 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-400';
      default:
        return 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-400';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'OPEN':
        return 'Ouvert';
      case 'REPLIED':
        return 'Répondu';
      case 'ON_HOLD':
        return 'En attente';
      case 'RESOLVED':
        return 'Résolu';
      case 'CLOSED':
        return 'Fermé';
      default:
        return status;
    }
  }

  getPriorityClass(priority: string): string {
    switch (priority) {
      case 'Low':
        return 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-400';
      case 'Medium':
        return 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400';
      case 'High':
        return 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
      case 'Urgent':
        return 'animate-pulse bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400';
      default:
        return 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-400';
    }
  }
}
