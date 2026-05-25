import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, finalize, startWith, switchMap, tap } from 'rxjs/operators';
import {
  LucideAngularModule,
  RefreshCw,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Loader2,
  Inbox,
  XCircle,
  Search,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';

type EmailQueueStatus = 'NOT_SENT' | 'SENDING' | 'SENT' | 'ERROR';
type StatusFilter = EmailQueueStatus | '';

interface EmailQueueItem {
  id: string;
  recipient: string;
  subject: string;
  status: EmailQueueStatus;
  retryCount: number;
  maxRetries: number;
  createdAt: string;
  sentAt: string | null;
  lastError: string;
  sender: string;
}

interface EmailQueueStats {
  total: number;
  notSent: number;
  sent: number;
  error: number;
  sending: number;
}

interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
}

const STATUS_LABELS: Record<EmailQueueStatus, string> = {
  NOT_SENT: 'En attente',
  SENDING: 'En cours',
  SENT: 'Envoyé',
  ERROR: 'Erreur',
};

@Component({
  selector: 'lmp-admin-email-queue',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="p-4 sm:p-5">
      <div class="mb-6 flex items-center justify-between">
        <div>
          <h1 class="text-lg font-semibold tracking-tight text-(--foreground)">File d'attente Email</h1>
          <p class="text-xs text-(--muted-foreground)">Surveillance et gestion des emails en file d'attente</p>
        </div>
        <button hlmBtn variant="outline" class="cursor-pointer gap-2" (click)="loadAll()" [disabled]="loading()">
          <lucide-icon [img]="RefreshCwIcon" [size]="16" [class.animate-spin]="loading()"></lucide-icon>
          Actualiser
        </button>
      </div>

      <!-- Stats cards -->
      <div class="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div class="rounded-sm border border-(--border) bg-(--card) p-4">
          <div class="flex items-center gap-2">
            <div class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted)">
              <lucide-icon [img]="InboxIcon" [size]="16" class="text-(--foreground)"></lucide-icon>
            </div>
            <div>
              <p class="text-xl font-semibold text-(--foreground)">{{ stats()?.total ?? 0 }}</p>
              <p class="text-[11px] text-(--muted-foreground)">Total</p>
            </div>
          </div>
        </div>
        <div class="rounded-sm border border-(--border) bg-(--card) p-4">
          <div class="flex items-center gap-2">
            <div class="flex h-8 w-8 items-center justify-center rounded-sm bg-amber-500/15">
              <lucide-icon [img]="ClockIcon" [size]="16" class="text-amber-500"></lucide-icon>
            </div>
            <div>
              <p class="text-xl font-semibold text-(--foreground)">{{ stats()?.notSent ?? 0 }}</p>
              <p class="text-[11px] text-(--muted-foreground)">En attente</p>
            </div>
          </div>
        </div>
        <div class="rounded-sm border border-(--border) bg-(--card) p-4">
          <div class="flex items-center gap-2">
            <div class="flex h-8 w-8 items-center justify-center rounded-sm bg-emerald-500/15">
              <lucide-icon [img]="CheckCircle2Icon" [size]="16" class="text-emerald-500"></lucide-icon>
            </div>
            <div>
              <p class="text-xl font-semibold text-(--foreground)">{{ stats()?.sent ?? 0 }}</p>
              <p class="text-[11px] text-(--muted-foreground)">Envoyés</p>
            </div>
          </div>
        </div>
        <div class="rounded-sm border border-(--border) bg-(--card) p-4">
          <div class="flex items-center gap-2">
            <div class="flex h-8 w-8 items-center justify-center rounded-sm bg-red-500/15">
              <lucide-icon [img]="AlertTriangleIcon" [size]="16" class="text-red-500"></lucide-icon>
            </div>
            <div>
              <p class="text-xl font-semibold text-(--foreground)">{{ stats()?.error ?? 0 }}</p>
              <p class="text-[11px] text-(--muted-foreground)">Erreurs</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Filters -->
      <div class="mb-4 flex flex-wrap items-center gap-3">
        <select
          [formControl]="filterStatusCtrl"
          class="rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
        >
          <option value="">Tous les statuts</option>
          <option value="NOT_SENT">En attente</option>
          <option value="SENDING">En cours</option>
          <option value="SENT">Envoyé</option>
          <option value="ERROR">Erreur</option>
        </select>
        <div class="relative">
          <lucide-icon [img]="SearchIcon" [size]="14" class="absolute left-2.5 top-1/2 -translate-y-1/2 text-(--muted-foreground)"></lucide-icon>
          <input
            type="text"
            [formControl]="filterSearchCtrl"
            placeholder="Rechercher..."
            class="w-56 rounded-sm border border-(--border) bg-(--background) py-2 pl-8 pr-3 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
          />
        </div>
      </div>

      <!-- Bulk actions -->
      @if (selectedIds().size > 0) {
        <div class="mb-3 flex items-center gap-3">
          <span class="text-xs text-(--muted-foreground)">{{ selectedIds().size }} sélectionné(s)</span>
          <button hlmBtn variant="outline" size="sm" class="cursor-pointer gap-1" (click)="bulkRetry()" [disabled]="bulkBusy()">
            <lucide-icon [img]="RefreshCwIcon" [size]="14"></lucide-icon>
            Relancer la sélection
          </button>
          <button hlmBtn variant="outline" size="sm" class="cursor-pointer gap-1 text-red-500 hover:text-red-600" (click)="bulkDelete()" [disabled]="bulkBusy()">
            <lucide-icon [img]="XCircleIcon" [size]="14"></lucide-icon>
            Supprimer la sélection
          </button>
        </div>
      }
      @if (errorMessage(); as msg) {
        <div data-testid="email-queue-error" class="mb-3 rounded-sm border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs text-red-500">
          {{ msg }}
        </div>
      }

      <!-- Table -->
      <div class="overflow-x-auto rounded-sm border border-(--border)">
        <table class="w-full text-left text-sm">
          <thead class="bg-(--muted) text-xs uppercase text-(--muted-foreground)">
            <tr>
              <th class="px-2 py-3">
                <input type="checkbox" (change)="toggleSelectAll($event)" [checked]="isAllSelected()" class="cursor-pointer" />
              </th>
              <th class="px-4 py-3 font-medium">Destinataire</th>
              <th class="px-4 py-3 font-medium">Sujet</th>
              <th class="px-4 py-3 font-medium">Statut</th>
              <th class="px-4 py-3 font-medium">Tentatives</th>
              <th class="px-4 py-3 font-medium">Date</th>
              <th class="px-4 py-3 font-medium text-right">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-(--border)">
            @if (loading()) {
              <tr>
                <td colspan="7" class="px-4 py-8 text-center text-(--muted-foreground)">
                  <lucide-icon [img]="Loader2Icon" [size]="20" class="mx-auto mb-2 animate-spin"></lucide-icon>
                  Chargement…
                </td>
              </tr>
            } @else if (emails().length === 0) {
              <tr>
                <td colspan="7" class="px-4 py-8 text-center text-(--muted-foreground)">
                  Aucun email dans la file d'attente.
                </td>
              </tr>
            } @else {
              @for (email of emails(); track email.id) {
                <tr class="hover:bg-(--muted)/50">
                  <td class="px-2 py-3">
                    <input type="checkbox" [checked]="selectedIds().has(email.id)" (change)="toggleSelect(email.id)" class="cursor-pointer" />
                  </td>
                  <td class="px-4 py-3 text-(--foreground)">{{ email.recipient }}</td>
                  <td class="max-w-xs truncate px-4 py-3 text-(--foreground)">{{ email.subject }}</td>
                  <td class="px-4 py-3">
                    <span
                      class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium"
                      [class.bg-amber-500\/15]="email.status === 'NOT_SENT'"
                      [class.text-amber-500]="email.status === 'NOT_SENT'"
                      [class.bg-blue-500\/15]="email.status === 'SENDING'"
                      [class.text-blue-500]="email.status === 'SENDING'"
                      [class.bg-emerald-500\/15]="email.status === 'SENT'"
                      [class.text-emerald-500]="email.status === 'SENT'"
                      [class.bg-red-500\/15]="email.status === 'ERROR'"
                      [class.text-red-500]="email.status === 'ERROR'"
                    >
                      {{ statusLabel(email.status) }}
                    </span>
                  </td>
                  <td class="px-4 py-3 text-(--foreground)">{{ email.retryCount }}/{{ email.maxRetries }}</td>
                  <td class="px-4 py-3 text-xs text-(--muted-foreground)">{{ email.createdAt | date:'short' }}</td>
                  <td class="px-4 py-3 text-right">
                    <div class="flex items-center justify-end gap-1">
                      @if (email.status === 'ERROR') {
                        <button
                          hlmBtn variant="ghost" size="sm" class="cursor-pointer gap-1"
                          (click)="retry(email.id)"
                          [disabled]="retryingId() === email.id"
                        >
                          @if (retryingId() === email.id) {
                            <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                          } @else {
                            <lucide-icon [img]="RefreshCwIcon" [size]="14"></lucide-icon>
                          }
                          Relancer
                        </button>
                      }
                      <button
                        hlmBtn variant="ghost" size="sm" class="cursor-pointer text-red-500 hover:text-red-600"
                        (click)="deleteOne(email.id)"
                        [disabled]="deletingId() === email.id"
                      >
                        @if (deletingId() === email.id) {
                          <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                        } @else {
                          <lucide-icon [img]="XCircleIcon" [size]="14"></lucide-icon>
                        }
                      </button>
                    </div>
                  </td>
                </tr>
              }
            }
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      @if (totalPages() > 1) {
        <div class="mt-4 flex items-center justify-between">
          <p class="text-xs text-(--muted-foreground)">
            Page {{ currentPage() + 1 }} sur {{ totalPages() }}
          </p>
          <div class="flex gap-2">
            <button
              hlmBtn variant="outline" size="sm" class="cursor-pointer"
              (click)="prevPage()"
              [disabled]="currentPage() === 0 || loading()"
            >
              Précédent
            </button>
            <button
              hlmBtn variant="outline" size="sm" class="cursor-pointer"
              (click)="nextPage()"
              [disabled]="currentPage() >= totalPages() - 1 || loading()"
            >
              Suivant
            </button>
          </div>
        </div>
      }
    </div>
  `,
})
export class AdminEmailQueueComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  readonly RefreshCwIcon = RefreshCw;
  readonly CheckCircle2Icon = CheckCircle2;
  readonly ClockIcon = Clock;
  readonly AlertTriangleIcon = AlertTriangle;
  readonly Loader2Icon = Loader2;
  readonly InboxIcon = Inbox;
  readonly XCircleIcon = XCircle;
  readonly SearchIcon = Search;

  emails = signal<EmailQueueItem[]>([]);
  stats = signal<EmailQueueStats | null>(null);
  loading = signal(false);
  retryingId = signal<string | null>(null);
  deletingId = signal<string | null>(null);
  bulkBusy = signal(false);
  currentPage = signal(0);
  totalPages = signal(0);
  selectedIds = signal<Set<string>>(new Set());
  errorMessage = signal<string | null>(null);

  filterStatusCtrl = new FormControl<StatusFilter>('', { nonNullable: true });
  filterSearchCtrl = new FormControl<string>('', { nonNullable: true });
  private reload$ = new Subject<void>();

  ngOnInit(): void {
    // Debounce search input + react to status filter + manual reload via single pipe.
    this.filterStatusCtrl.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.currentPage.set(0);
        this.selectedIds.set(new Set());
        this.reload$.next();
      });

    this.filterSearchCtrl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.currentPage.set(0);
        this.selectedIds.set(new Set());
        this.reload$.next();
      });

    this.reload$
      .pipe(
        startWith(undefined),
        switchMap(() => this.fetchPage()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.loadStats();
  }

  loadAll(): void {
    this.reload$.next();
    this.loadStats();
  }

  private fetchPage() {
    this.loading.set(true);
    this.errorMessage.set(null);
    let params = new HttpParams().set('page', this.currentPage()).set('size', 20);
    const status = this.filterStatusCtrl.value;
    const search = this.filterSearchCtrl.value;
    if (status) params = params.set('status', status);
    if (search) params = params.set('search', search);

    return this.http.get<PageResponse<EmailQueueItem>>('/api/v1/admin/email-queue', { params })
      .pipe(
        tap((page) => {
          this.emails.set(page.content ?? []);
          this.totalPages.set(page.totalPages ?? 0);
        }),
        catchError((err) => {
          this.errorMessage.set('Échec chargement : ' + this.errMsg(err));
          this.emails.set([]);
          return of(null);
        }),
        finalize(() => this.loading.set(false)),
      );
  }

  loadStats(): void {
    this.http.get<EmailQueueStats>('/api/v1/admin/email-queue/stats')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (s) => this.stats.set(s),
        error: () => this.stats.set(null),
      });
  }

  retry(id: string): void {
    this.retryingId.set(id);
    this.http.post(`/api/v1/admin/email-queue/${id}/retry`, {})
      .pipe(finalize(() => this.retryingId.set(null)))
      .subscribe({
        next: () => this.loadAll(),
        error: (err) => this.errorMessage.set('Échec relance : ' + this.errMsg(err)),
      });
  }

  deleteOne(id: string): void {
    this.deletingId.set(id);
    this.http.delete(`/api/v1/admin/email-queue/${id}`)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe({
        next: () => this.loadAll(),
        error: (err) => this.errorMessage.set('Échec suppression : ' + this.errMsg(err)),
      });
  }

  bulkRetry(): void {
    const ids = Array.from(this.selectedIds());
    if (ids.length === 0) return;
    this.bulkBusy.set(true);
    this.http.post('/api/v1/admin/email-queue/bulk-retry', ids)
      .pipe(finalize(() => this.bulkBusy.set(false)))
      .subscribe({
        next: () => {
          this.selectedIds.set(new Set());
          this.loadAll();
        },
        error: (err) => this.errorMessage.set('Échec relance groupée : ' + this.errMsg(err)),
      });
  }

  bulkDelete(): void {
    const ids = Array.from(this.selectedIds());
    if (ids.length === 0) return;
    this.bulkBusy.set(true);
    this.http.post('/api/v1/admin/email-queue/bulk-delete', ids)
      .pipe(finalize(() => this.bulkBusy.set(false)))
      .subscribe({
        next: () => {
          this.selectedIds.set(new Set());
          this.loadAll();
        },
        error: (err) => this.errorMessage.set('Échec suppression groupée : ' + this.errMsg(err)),
      });
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update((p) => p - 1);
      this.selectedIds.set(new Set());
      this.reload$.next();
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update((p) => p + 1);
      this.selectedIds.set(new Set());
      this.reload$.next();
    }
  }

  toggleSelect(id: string): void {
    this.selectedIds.update((set) => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  toggleSelectAll(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedIds.set(checked ? new Set(this.emails().map((e) => e.id)) : new Set());
  }

  isAllSelected(): boolean {
    return this.emails().length > 0 && this.emails().every((e) => this.selectedIds().has(e.id));
  }

  statusLabel(status: EmailQueueStatus): string {
    return STATUS_LABELS[status] ?? status;
  }

  private errMsg(err: any): string {
    return err?.error?.message || err?.message || 'Erreur inconnue';
  }
}
