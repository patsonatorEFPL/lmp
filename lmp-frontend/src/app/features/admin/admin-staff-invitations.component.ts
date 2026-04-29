import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import {
  LucideAngularModule,
  Mail,
  Plus,
  RefreshCw,
  Loader2,
  X,
  Send,
  RotateCcw,
  Trash2,
  Check,
  Clock,
  Ban,
  ChevronLeft,
  ChevronRight,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { environment } from '../../../environments/environment';
import { VisiblePollService } from '../../core/services/visible-poll.service';

interface InvitationItem {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  status: 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'REVOKED';
  invitedBy: string;
  createdAt: string;
  expiresAt: string;
  acceptedAt: string | null;
  revokedAt: string | null;
}

interface PageResponse {
  items: InvitationItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
}

@Component({
  selector: 'lmp-admin-staff-invitations',
  standalone: true,
  imports: [
    NgClass,
    DatePipe,
    FormsModule,
    LucideAngularModule,
    HlmButton,
  ],
  template: `
    <div class="crm-list-view flex h-full flex-col overflow-hidden bg-(--background)">
      <!-- Barre de filtres inline (style CRM) -->
      <div class="flex items-center justify-between gap-2 px-5 py-4">
        <div class="flex items-center"></div>
        <!-- Actions droite -->
        <div class="flex items-center gap-0.5">
          <button
            hlmBtn variant="ghost" size="icon" type="button"
            class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
            title="Actualiser"
            (click)="loadInvitations()"
          >
            <lucide-icon [img]="RefreshCwIcon" [size]="15" [ngClass]="{ 'animate-spin': loading() }"></lucide-icon>
          </button>
          <button
            hlmBtn variant="default" size="sm" type="button"
            class="ml-1 h-7 cursor-pointer gap-1.5 px-2.5"
            (click)="openInviteModal()"
          >
            <lucide-icon [img]="PlusIcon" [size]="14"></lucide-icon>
            <span class="text-sm">Inviter</span>
          </button>
        </div>
      </div>

      <!-- Panneau de filtres -->
      <div class="flex items-center gap-2 border-b border-zinc-100 px-5 pb-3 dark:border-zinc-800">
        <input
          type="text"
          [(ngModel)]="emailFilter"
          (input)="currentPage.set(0); loadInvitations()"
          placeholder="Rechercher par email…"
          class="h-8 w-56 rounded-lg border border-zinc-200 bg-zinc-50 px-3 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
        />
        <select
          [(ngModel)]="statusFilter"
          (change)="currentPage.set(0); loadInvitations()"
          class="h-8 w-40 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
        >
          <option value="">Tous les statuts</option>
          <option value="PENDING">En attente</option>
          <option value="ACCEPTED">Acceptée</option>
          <option value="EXPIRED">Expirée</option>
          <option value="REVOKED">Révoquée</option>
        </select>
        @if (emailFilter() || statusFilter()) {
          <button
            type="button"
            class="flex h-8 cursor-pointer items-center gap-1 rounded-lg px-2 text-sm text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800"
            (click)="clearFilters()"
          >
            <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
            Effacer
          </button>
        }
      </div>

      <!-- Liste (style CRM) -->
      <div class="flex-1 overflow-auto px-3 sm:px-5">
        @if (loading()) {
          <div class="flex items-center justify-center py-16">
            <lucide-icon
              [img]="Loader2Icon"
              [size]="24"
              class="animate-spin text-zinc-400"
            ></lucide-icon>
          </div>
        } @else {
          <!-- En-tête colonnes -->
          <div class="mb-2 flex min-w-max items-center rounded-lg bg-zinc-100 py-1.5 text-sm font-normal leading-none text-zinc-500 dark:bg-zinc-800/70 dark:text-zinc-400">
            <div class="w-64 shrink-0 px-2">Email</div>
            <div class="w-40 shrink-0 px-2">Nom</div>
            <div class="w-28 shrink-0 px-2">Statut</div>
            <div class="w-36 shrink-0 px-2">Envoyée</div>
            <div class="w-32 shrink-0 px-2">Expiration</div>
            <div class="w-24 shrink-0 px-2 text-right">Actions</div>
          </div>

          <!-- Lignes -->
          <div>
            @for (inv of invitations(); track inv.id) {
              <div
                class="group flex h-10 min-w-max items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
              >
                <div class="w-64 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">
                  {{ inv.email }}
                </div>
                <div class="w-40 shrink-0 truncate px-2 text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ displayName(inv) }}
                </div>
                <div class="w-28 shrink-0 px-2">
                  <span
                    class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                    [ngClass]="statusClass(inv.status)"
                  >
                    {{ statusLabel(inv.status) }}
                  </span>
                </div>
                <div class="w-36 shrink-0 px-2 text-sm leading-normal text-zinc-500 dark:text-zinc-400">
                  {{ inv.createdAt | date:'dd/MM/yyyy HH:mm' }}
                </div>
                <div class="w-32 shrink-0 px-2 text-sm leading-normal text-zinc-500 dark:text-zinc-400">
                  {{ inv.expiresAt | date:'dd/MM/yyyy' }}
                </div>
                <div class="flex w-24 shrink-0 items-center justify-end gap-1 px-2">
                  @if (inv.status === 'PENDING') {
                    <button
                      type="button"
                      class="flex h-7 w-7 cursor-pointer items-center justify-center rounded text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200"
                      title="Renvoyer"
                      (click)="resendInvitation(inv.id)"
                    >
                      <lucide-icon [img]="RotateCcwIcon" [size]="14"></lucide-icon>
                    </button>
                    <button
                      type="button"
                      class="flex h-7 w-7 cursor-pointer items-center justify-center rounded text-zinc-500 transition-colors hover:bg-red-50 hover:text-red-600 dark:text-zinc-400 dark:hover:bg-red-900/20 dark:hover:text-red-400"
                      title="Révoquer"
                      (click)="revokeInvitation(inv.id)"
                    >
                      <lucide-icon [img]="Trash2Icon" [size]="14"></lucide-icon>
                    </button>
                  }
                </div>
              </div>
            } @empty {
              <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
                Aucune invitation trouvée
              </div>
            }
          </div>
        }
      </div>

      <!-- Footer pagination (style CRM) -->
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
          <button
            type="button"
            class="flex h-7 w-7 cursor-pointer items-center justify-center rounded text-zinc-500 hover:bg-zinc-100 disabled:cursor-not-allowed disabled:opacity-40 dark:text-zinc-400 dark:hover:bg-zinc-800"
            [disabled]="currentPage() === 0"
            (click)="prevPage()"
          >
            <lucide-icon [img]="ChevronLeftIcon" [size]="14"></lucide-icon>
          </button>
          <span class="px-1">
            <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ currentPage() + 1 }}</span>
            <span>/</span>
            <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ totalPages() || 1 }}</span>
          </span>
          <button
            type="button"
            class="flex h-7 w-7 cursor-pointer items-center justify-center rounded text-zinc-500 hover:bg-zinc-100 disabled:cursor-not-allowed disabled:opacity-40 dark:text-zinc-400 dark:hover:bg-zinc-800"
            [disabled]="currentPage() >= totalPages() - 1"
            (click)="nextPage()"
          >
            <lucide-icon [img]="ChevronRightIcon" [size]="14"></lucide-icon>
          </button>
        </div>
      </div>
    </div>

    <!-- Invite Modal -->
    @if (showInviteModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        (click)="closeInviteModal()"
      >
        <div
          class="mx-4 w-full max-w-md rounded-sm border border-(--border) bg-(--card) shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          <div class="flex items-center justify-between border-b border-(--border) px-6 py-4">
            <h3 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">Inviter un collaborateur</h3>
            <button
              hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
              (click)="closeInviteModal()"
            >
              <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
            </button>
          </div>

          <div class="space-y-4 px-6 py-5">
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Adresse e-mail *</label>
              <input
                [(ngModel)]="inviteForm.email"
                type="email"
                autocomplete="off"
                class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="collegue@entreprise.be"
              />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Prénom</label>
                <input
                  [(ngModel)]="inviteForm.firstName"
                  type="text"
                  class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                  placeholder="Jean"
                />
              </div>
              <div>
                <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Nom</label>
                <input
                  [(ngModel)]="inviteForm.lastName"
                  type="text"
                  class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                  placeholder="Dupont"
                />
              </div>
            </div>

            @if (inviteError()) {
              <div class="rounded-sm bg-red-50 px-3 py-2 text-xs text-red-600 dark:bg-red-900/20 dark:text-red-400">
                {{ inviteError() }}
              </div>
            }
            @if (inviteSuccess()) {
              <div class="rounded-sm bg-green-50 px-3 py-2 text-xs text-green-600 dark:bg-green-900/20 dark:text-green-400">
                {{ inviteSuccess() }}
              </div>
            }
          </div>

          <div class="flex items-center justify-end gap-2 border-t border-(--border) px-6 py-4">
            <button hlmBtn variant="outline" size="sm" class="cursor-pointer" (click)="closeInviteModal()">
              Annuler
            </button>
            <button
              hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
              [disabled]="inviting()"
              (click)="sendInvitation()"
            >
              @if (inviting()) {
                <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                Envoi…
              } @else {
                <lucide-icon [img]="SendIcon" [size]="14"></lucide-icon>
                Envoyer
              }
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class AdminStaffInvitationsComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly visiblePoll = inject(VisiblePollService);

  readonly MailIcon = Mail;
  readonly PlusIcon = Plus;
  readonly RefreshCwIcon = RefreshCw;
  readonly Loader2Icon = Loader2;
  readonly XIcon = X;
  readonly SendIcon = Send;
  readonly RotateCcwIcon = RotateCcw;
  readonly Trash2Icon = Trash2;
  readonly CheckIcon = Check;
  readonly ClockIcon = Clock;
  readonly BanIcon = Ban;
  readonly ChevronLeftIcon = ChevronLeft;
  readonly ChevronRightIcon = ChevronRight;

  readonly loading = signal(false);
  readonly invitations = signal<InvitationItem[]>([]);
  readonly currentPage = signal(0);
  readonly pageSize = signal(20);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly emailFilter = signal('');
  readonly statusFilter = signal('');

  readonly showInviteModal = signal(false);
  readonly inviting = signal(false);
  readonly inviteError = signal('');
  readonly inviteSuccess = signal('');

  readonly inviteForm = {
    email: '',
    firstName: '',
    lastName: '',
  };

  readonly pageSizes = [10, 20, 50];

  ngOnInit(): void {
    this.loadInvitations();
    this.visiblePoll.subscribeWhileVisible(
      this.destroyRef,
      environment.dashboardPollIntervalMs,
      () => this.loadInvitations(),
    );
  }

  loadInvitations(): void {
    this.loading.set(true);
    const params: Record<string, string> = {
      page: this.currentPage().toString(),
      size: this.pageSize().toString(),
    };
    const status = this.statusFilter();
    if (status) params['status'] = status;
    const email = this.emailFilter().trim();
    if (email) params['email'] = email;

    this.http
      .get<ApiResponse<PageResponse>>(
        `${environment.apiUrl}/api/v1/admin/staff-invitations`,
        { params, withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          const page = res.data!;
          this.invitations.set(page.items);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  clearFilters(): void {
    this.emailFilter.set('');
    this.statusFilter.set('');
    this.currentPage.set(0);
    this.loadInvitations();
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadInvitations();
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update((p) => p - 1);
      this.loadInvitations();
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update((p) => p + 1);
      this.loadInvitations();
    }
  }

  openInviteModal(): void {
    this.inviteForm.email = '';
    this.inviteForm.firstName = '';
    this.inviteForm.lastName = '';
    this.inviteError.set('');
    this.inviteSuccess.set('');
    this.showInviteModal.set(true);
  }

  closeInviteModal(): void {
    this.showInviteModal.set(false);
  }

  sendInvitation(): void {
    this.inviteError.set('');
    this.inviteSuccess.set('');

    if (!this.inviteForm.email || !this.inviteForm.email.includes('@')) {
      this.inviteError.set('Veuillez saisir une adresse e-mail valide.');
      return;
    }

    this.inviting.set(true);
    this.http
      .post<ApiResponse<InvitationItem>>(
        `${environment.apiUrl}/api/v1/admin/staff-invitations`,
        {
          email: this.inviteForm.email.trim(),
          firstName: this.inviteForm.firstName.trim() || undefined,
          lastName: this.inviteForm.lastName.trim() || undefined,
        },
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          this.inviting.set(false);
          this.inviteSuccess.set(res.message ?? 'Invitation envoyée avec succès.');
          this.loadInvitations();
          setTimeout(() => this.closeInviteModal(), 1200);
        },
        error: (err) => {
          this.inviting.set(false);
          this.inviteError.set(
            err.error?.message ?? "Impossible d'envoyer l'invitation.",
          );
        },
      });
  }

  resendInvitation(id: string): void {
    if (!confirm('Renvoyer cette invitation ? Le lien précédent ne sera plus valide.')) return;
    this.http
      .post<ApiResponse<void>>(
        `${environment.apiUrl}/api/v1/admin/staff-invitations/${id}/resend`,
        {},
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          alert(res.message ?? 'Invitation renvoyée.');
          this.loadInvitations();
        },
        error: (err) => {
          alert(err.error?.message ?? 'Erreur lors du renvoi.');
        },
      });
  }

  revokeInvitation(id: string): void {
    if (!confirm("Révoquer cette invitation ? Le destinataire ne pourra plus l'utiliser.")) return;
    this.http
      .delete<ApiResponse<void>>(
        `${environment.apiUrl}/api/v1/admin/staff-invitations/${id}`,
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          alert(res.message ?? 'Invitation révoquée.');
          this.loadInvitations();
        },
        error: (err) => {
          alert(err.error?.message ?? 'Erreur lors de la révocation.');
        },
      });
  }

  statusClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'bg-amber-50 text-amber-700 dark:bg-amber-900/20 dark:text-amber-400';
      case 'ACCEPTED':
        return 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400';
      case 'EXPIRED':
        return 'bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400';
      case 'REVOKED':
        return 'bg-red-50 text-red-700 dark:bg-red-900/20 dark:text-red-400';
      default:
        return 'bg-zinc-100 text-zinc-600';
    }
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'En attente';
      case 'ACCEPTED': return 'Acceptée';
      case 'EXPIRED': return 'Expirée';
      case 'REVOKED': return 'Révoquée';
      default: return status;
    }
  }

  displayName(inv: InvitationItem): string {
    const parts = [inv.firstName, inv.lastName].filter((x): x is string => !!x);
    return parts.join(' ') || '—';
  }
}
