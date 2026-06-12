import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  LucideAngularModule, FolderKanban, RefreshCw, Filter, X, Loader2,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { PortalService } from '../../core/services/portal.service';
import { Project, ProjectStatus, ProjectPriority } from '../../shared/models/portal.models';
import { formatRelativeTimeFr } from '../../core/utils/relative-time';

@Component({
  selector: 'lmp-user-projects',
  standalone: true,
  imports: [DatePipe, NgClass, FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="flex h-full flex-col overflow-hidden">
      <!-- Toolbar -->
      <div class="flex items-center justify-between gap-2 pb-4">
        <div class="flex items-center"></div>
        <div class="flex items-center gap-0.5">
          <button
            hlmBtn variant="ghost" size="icon" type="button"
            class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
            title="Actualiser" (click)="load()"
          >
            <lucide-icon [img]="RefreshCwIcon" [size]="15" [ngClass]="{ 'animate-spin': loading() }"></lucide-icon>
          </button>
          <button
            hlmBtn variant="ghost" size="sm" type="button"
            class="h-7 cursor-pointer gap-1.5 px-2"
            [ngClass]="showFilter() ? 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200' : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200'"
            (click)="showFilter.set(!showFilter())"
          >
            <lucide-icon [img]="FilterIcon" [size]="14"></lucide-icon>
            <span class="text-sm">Filtre</span>
          </button>
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
            >
              <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
              Effacer
            </button>
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
            <div class="w-32 shrink-0 px-2">Projet</div>
            <div class="w-64 shrink-0 px-2">Nom</div>
            <div class="w-32 shrink-0 px-2 text-center">Statut</div>
            <div class="w-28 shrink-0 px-2 text-center">Priorité</div>
            <div class="w-40 shrink-0 px-2 text-center">Avancement</div>
            <div class="w-32 shrink-0 px-2 text-center">Échéance</div>
            <div class="w-32 shrink-0 px-2 text-center">Last Modified</div>
          </div>

          <div>
            @for (p of paginated(); track p.id) {
              <div
                class="group flex h-10 min-w-max cursor-pointer items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
                (click)="openProject(p)"
              >
                <div class="w-32 shrink-0 truncate px-2 font-mono text-xs leading-normal text-zinc-700 dark:text-zinc-300">{{ p.id }}</div>
                <div class="w-64 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">{{ p.projectName }}</div>
                <div class="w-32 shrink-0 px-2 text-center">
                  <span class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium" [ngClass]="getStatusClass(p.status)">
                    {{ getStatusLabel(p.status) }}
                  </span>
                </div>
                <div class="w-28 shrink-0 px-2 text-center">
                  <span class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium" [ngClass]="getPriorityClass(p.priority)">
                    {{ p.priority }}
                  </span>
                </div>
                <div class="w-40 shrink-0 px-2">
                  <div class="flex items-center gap-2">
                    <div class="h-1.5 flex-1 overflow-hidden rounded-full bg-zinc-200 dark:bg-zinc-800">
                      <div class="h-full transition-all" [style.width.%]="p.percentComplete"
                        [ngClass]="p.status === 'Completed' ? 'bg-emerald-500' : p.status === 'Cancelled' ? 'bg-zinc-400' : 'bg-(--primary)'"
                      ></div>
                    </div>
                    <span class="w-9 shrink-0 text-right text-xs leading-normal text-zinc-600 dark:text-zinc-400">{{ p.percentComplete }}%</span>
                  </div>
                </div>
                <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ p.expectedEndDate ? (p.expectedEndDate | date: 'dd/MM/yyyy') : '—' }}
                </div>
                <div class="w-32 shrink-0 px-2 text-center text-sm leading-normal text-zinc-500 dark:text-zinc-400">
                  {{ formatRelativeTimeFr(p.expectedEndDate) }}
                </div>
              </div>
            } @empty {
              <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
                @if (statusFilter) { Aucun projet ne correspond à ce filtre. }
                @else { Aucun projet trouvé. Vos projets apparaîtront ici. }
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
  `,
})
export class UserProjectsComponent implements OnInit {
  private readonly portal = inject(PortalService);
  private readonly router = inject(Router);

  readonly STATUS_OPTIONS: ProjectStatus[] = ['Open', 'Completed', 'Cancelled'];

  readonly RefreshCwIcon = RefreshCw;
  readonly FilterIcon = Filter;
  readonly XIcon = X;
  readonly FolderKanbanIcon = FolderKanban;
  readonly Loader2Icon = Loader2;

  readonly loading = signal(false);
  readonly showFilter = signal(false);
  readonly items = signal<Project[]>([]);
  statusFilter = '';

  readonly pageSize = signal(20);
  readonly currentPage = signal(0);
  readonly pageSizes = [20, 50, 100];

  readonly filtered = computed(() =>
    this.statusFilter ? this.items().filter((p) => p.status === this.statusFilter) : this.items(),
  );

  readonly paginated = computed(() => {
    const start = this.currentPage() * this.pageSize();
    return this.filtered().slice(start, start + this.pageSize());
  });

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.portal.listProjects().subscribe({
      next: (list) => { this.items.set(list); this.loading.set(false); this.currentPage.set(0); },
      error: () => this.loading.set(false),
    });
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
  }

  openProject(p: Project): void {
    this.router.navigate(['/dashboard/projects', p.id]);
  }

  getStatusLabel(s: ProjectStatus): string {
    switch (s) {
      case 'Open': return 'En cours';
      case 'Completed': return 'Terminé';
      case 'Cancelled': return 'Annulé';
      default: return s;
    }
  }

  getStatusClass(s: ProjectStatus): string {
    switch (s) {
      case 'Open': return 'bg-blue-500/10 text-blue-500';
      case 'Completed': return 'bg-green-500/10 text-green-500';
      case 'Cancelled': return 'bg-red-500/10 text-red-500';
      default: return 'bg-(--muted) text-(--foreground)';
    }
  }

  getPriorityClass(p: ProjectPriority): string {
    switch (p) {
      case 'High': return 'bg-red-500/10 text-red-500';
      case 'Medium': return 'bg-yellow-500/10 text-yellow-500';
      case 'Low': return 'bg-(--muted) text-(--foreground)';
      default: return 'bg-(--muted) text-(--foreground)';
    }
  }

  readonly formatRelativeTimeFr = (iso: string | null | undefined) => formatRelativeTimeFr(iso, 30);
}
