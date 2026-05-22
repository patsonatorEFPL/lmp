import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  LucideAngularModule, LifeBuoy, RefreshCw, Filter, X, Plus, Loader2,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { PortalService } from '../../core/services/portal.service';
import { Issue, IssueStatus, IssuePriority } from '../../shared/models/portal.models';

@Component({
  selector: 'lmp-user-tickets',
  standalone: true,
  imports: [DatePipe, NgClass, FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="flex h-full flex-col overflow-hidden">
      <!-- Toolbar -->
      <div class="flex items-center justify-between gap-2 pb-4">
        <div></div>
        <div class="flex items-center gap-0.5">
          <button hlmBtn variant="ghost" size="icon" type="button"
            class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
            title="Actualiser" (click)="load()"
          ><lucide-icon [img]="RefreshCwIcon" [size]="15" [ngClass]="{ 'animate-spin': loading() }"></lucide-icon></button>
          <button hlmBtn variant="ghost" size="sm" type="button"
            class="h-7 cursor-pointer gap-1.5 px-2"
            [ngClass]="showFilter() ? 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200' : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200'"
            (click)="showFilter.set(!showFilter())"
          ><lucide-icon [img]="FilterIcon" [size]="14"></lucide-icon><span class="text-sm">Filtre</span></button>
          <button hlmBtn size="sm" type="button" class="h-7 cursor-pointer gap-1.5 px-2"
            (click)="showCreate.set(true)"
          ><lucide-icon [img]="PlusIcon" [size]="14"></lucide-icon><span class="text-sm">Nouveau</span></button>
        </div>
      </div>

      @if (showFilter()) {
        <div class="flex items-center gap-2 border-b border-zinc-100 pb-3 dark:border-zinc-800">
          <select [(ngModel)]="statusFilter"
            class="h-8 w-44 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
          >
            <option value="">Tous les statuts</option>
            @for (s of STATUS_OPTIONS; track s) { <option [value]="s">{{ s }}</option> }
          </select>
          @if (statusFilter) {
            <button type="button"
              class="flex h-8 cursor-pointer items-center gap-1 rounded-lg px-2 text-sm text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800"
              (click)="statusFilter = ''"
            ><lucide-icon [img]="XIcon" [size]="14"></lucide-icon>Effacer</button>
          }
        </div>
      }

      <!-- CRM-style list -->
      <div class="flex-1 overflow-auto">
        @if (loading()) {
          <div class="flex items-center justify-center py-16">
            <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-zinc-400"></lucide-icon>
          </div>
        } @else {
          <div class="mb-2 flex min-w-max items-center rounded-lg bg-zinc-100 py-1.5 text-sm font-normal leading-none text-zinc-500 dark:bg-zinc-800/70 dark:text-zinc-400">
            <div class="w-32 shrink-0 px-2">Ticket</div>
            <div class="w-80 shrink-0 px-2">Sujet</div>
            <div class="w-32 shrink-0 px-2 text-center">Priorité</div>
            <div class="w-32 shrink-0 px-2 text-center">Statut</div>
            <div class="w-36 shrink-0 px-2 text-center">Ouvert le</div>
            <div class="w-32 shrink-0 px-2 text-center">Last Modified</div>
          </div>

          <div>
            @for (t of paginated(); track t.id) {
              <div
                class="group flex h-10 min-w-max cursor-pointer items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
                (click)="openDetail(t)"
              >
                <div class="w-32 shrink-0 truncate px-2 font-mono text-xs leading-normal text-zinc-700 dark:text-zinc-300">{{ t.id }}</div>
                <div class="w-80 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">{{ t.subject }}</div>
                <div class="w-32 shrink-0 px-2 text-center">
                  <span class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium" [ngClass]="getPriorityClass(t.priority)">
                    {{ t.priority }}
                  </span>
                </div>
                <div class="w-32 shrink-0 px-2 text-center">
                  <span class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium" [ngClass]="getStatusClass(t.status)">
                    {{ t.status }}
                  </span>
                </div>
                <div class="w-36 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ t.openingDate | date: 'dd/MM/yyyy' }}
                </div>
                <div class="w-32 shrink-0 px-2 text-center text-sm leading-normal text-zinc-500 dark:text-zinc-400">
                  {{ formatRelativeTimeFr(t.openingDate) }}
                </div>
              </div>
            } @empty {
              <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
                @if (statusFilter) { Aucun ticket ne correspond à ce filtre. }
                @else {
                  <p>Aucun ticket.</p>
                  <button hlmBtn size="sm" class="mt-4 cursor-pointer" (click)="showCreate.set(true)">
                    <lucide-icon [img]="PlusIcon" [size]="14" class="mr-1"></lucide-icon>
                    Nouveau ticket
                  </button>
                }
              </div>
            }
          </div>
        }
      </div>

      <!-- Footer pagination -->
      <div class="flex items-center justify-between border-t border-zinc-200 py-2 dark:border-zinc-800">
        <div class="inline-flex rounded-md border border-zinc-200 dark:border-zinc-700">
          @for (size of pageSizes; track size; let first = $first; let last = $last) {
            <button type="button"
              class="h-7 min-w-[2.25rem] px-2.5 text-sm font-normal transition-colors"
              [ngClass]="{
                'rounded-l-md': first,
                'rounded-r-md': last,
                'border-r border-zinc-200 dark:border-zinc-700': !last,
                'bg-zinc-100 text-zinc-900 dark:bg-zinc-800 dark:text-zinc-100': pageSize() === size,
                'bg-white text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900 dark:bg-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200': pageSize() !== size
              }"
              (click)="changePageSize(size)"
            >{{ size }}</button>
          }
        </div>
        <div class="flex items-center gap-1 text-sm text-zinc-500 dark:text-zinc-400">
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ filtered().length }}</span>
          <span>of</span>
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ items().length }}</span>
        </div>
      </div>
    </div>

    @if (showCreate()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" (click)="cancelCreate()">
        <div class="w-full max-w-lg rounded-md bg-(--card) shadow-2xl" (click)="$event.stopPropagation()">
          <div class="flex items-center justify-between border-b border-(--border) px-5 py-3">
            <h2 class="text-base font-semibold text-(--foreground)">Nouveau ticket</h2>
            <button class="cursor-pointer rounded-md p-1 text-zinc-500 hover:bg-(--accent)" (click)="cancelCreate()">
              <lucide-icon [img]="XIcon" [size]="18"></lucide-icon>
            </button>
          </div>
          <div class="space-y-4 p-5">
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Sujet</label>
              <input [(ngModel)]="newSubject" type="text"
                class="h-9 w-full rounded-md border border-(--border) bg-(--card) px-3 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="Décrivez brièvement votre demande"
              />
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Priorité</label>
              <select [(ngModel)]="newPriority"
                class="h-9 w-full cursor-pointer rounded-md border border-(--border) bg-(--card) px-3 text-sm text-(--foreground) outline-none focus:border-(--primary)"
              >
                @for (p of PRIORITY_OPTIONS; track p) { <option [value]="p">{{ p }}</option> }
              </select>
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Description</label>
              <textarea [(ngModel)]="newDescription" rows="5"
                class="w-full rounded-md border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="Expliquez votre problème ou votre demande en détail"
              ></textarea>
            </div>
          </div>
          <div class="flex justify-end gap-2 border-t border-(--border) p-4">
            <button hlmBtn variant="ghost" class="cursor-pointer" (click)="cancelCreate()">Annuler</button>
            <button hlmBtn class="cursor-pointer" [disabled]="!newSubject.trim() || !newDescription.trim()"
              (click)="create()"
            >Créer le ticket</button>
          </div>
        </div>
      </div>
    }
  `,
})
export class UserTicketsComponent implements OnInit {
  private readonly portal = inject(PortalService);
  private readonly router = inject(Router);

  readonly STATUS_OPTIONS: IssueStatus[] = ['Open', 'Replied', 'On Hold', 'Resolved', 'Closed'];
  readonly PRIORITY_OPTIONS: IssuePriority[] = ['Low', 'Medium', 'High', 'Urgent'];

  readonly RefreshCwIcon = RefreshCw;
  readonly FilterIcon = Filter;
  readonly XIcon = X;
  readonly PlusIcon = Plus;
  readonly LifeBuoyIcon = LifeBuoy;
  readonly Loader2Icon = Loader2;

  readonly loading = signal(false);
  readonly showFilter = signal(false);
  readonly showCreate = signal(false);
  readonly items = signal<Issue[]>([]);
  statusFilter = '';

  newSubject = '';
  newDescription = '';
  newPriority: IssuePriority = 'Medium';

  readonly pageSize = signal(20);
  readonly currentPage = signal(0);
  readonly pageSizes = [20, 50, 100];

  readonly filtered = computed(() =>
    this.statusFilter ? this.items().filter((i) => i.status === this.statusFilter) : this.items(),
  );

  readonly paginated = computed(() => {
    const start = this.currentPage() * this.pageSize();
    return this.filtered().slice(start, start + this.pageSize());
  });

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.portal.listIssues().subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  openDetail(t: Issue) {
    void this.router.navigate(['/dashboard/tickets', t.id]);
  }

  create() {
    if (!this.newSubject.trim() || !this.newDescription.trim()) return;
    this.portal.createIssue(this.newSubject.trim(), this.newDescription.trim(), this.newPriority)
      .subscribe((created) => {
        this.items.update((list) => [created, ...list]);
        this.cancelCreate();
      });
  }

  cancelCreate() {
    this.showCreate.set(false);
    this.newSubject = '';
    this.newDescription = '';
    this.newPriority = 'Medium';
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
  }

  getStatusClass(status: IssueStatus): string {
    switch (status) {
      case 'Open': return 'bg-blue-500/10 text-blue-500';
      case 'Replied': return 'bg-orange-500/10 text-orange-500';
      case 'On Hold': return 'bg-(--muted) text-(--foreground)';
      case 'Resolved': return 'bg-green-500/10 text-green-500';
      case 'Closed': return 'bg-(--muted) text-(--foreground)';
      default: return 'bg-gray-500/10 text-gray-400';
    }
  }

  getPriorityClass(p: IssuePriority): string {
    switch (p) {
      case 'Urgent': return 'bg-red-500/10 text-red-500';
      case 'High': return 'bg-orange-500/10 text-orange-500';
      case 'Medium': return 'bg-yellow-500/10 text-yellow-500';
      case 'Low': return 'bg-(--muted) text-(--foreground)';
      default: return 'bg-gray-500/10 text-gray-400';
    }
  }

  formatRelativeTimeFr(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '—';
    const diffMs = Date.now() - d.getTime();
    const sec = Math.floor(diffMs / 1000);
    if (sec < 45) return 'À l\'instant';
    const min = Math.floor(sec / 60);
    const hours = Math.floor(min / 60);
    const days = Math.floor(hours / 24);
    if (min < 60) return min <= 1 ? 'Il y a 1 min' : `Il y a ${min} min`;
    if (hours < 24) return hours <= 1 ? 'Il y a 1 h' : `Il y a ${hours} h`;
    if (days < 7) return days === 1 ? 'Il y a 1 jour' : `Il y a ${days} j`;
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }
}
