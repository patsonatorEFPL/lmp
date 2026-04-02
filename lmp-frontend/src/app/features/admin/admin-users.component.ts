import { Component, DestroyRef, inject, OnInit, signal, effect, untracked } from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import {
  LucideAngularModule,
  Users,
  Search,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Shield,
  Lock,
  Unlock,
  Mail,
  Loader2,
  Pencil,
  X,
  Check,
  Save,
  User,
  Trash2,
  AlertTriangle,
  Info,
  MailWarning,
  Ban,
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AdminSseService } from '../../core/services/admin-sse.service';
import { AuthService } from '../../core/services/auth.service';
import { VisiblePollService } from '../../core/services/visible-poll.service';
import { createListFetchLoading } from '../../core/utils/list-fetch-loading';

interface UserItem {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  displayName: string;
  roles: string[];
  emailVerified: boolean;
  accountLocked: boolean;
  status: string;
  registrationDate: string;
  lastLoginDate: string | null;
  phone?: string;
  city?: string;
  country?: string;
  companyName?: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

@Component({
  selector: 'lmp-admin-users',
  standalone: true,
  imports: [NgClass, DatePipe, FormsModule, LucideAngularModule, HlmButton],
  template: `
    <!-- Header -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-(--foreground)">
          Gestion des Utilisateurs
        </h1>
        <p class="mt-1 text-sm text-(--muted-foreground)">
          {{ totalUsers() }} utilisateurs enregistrés
        </p>
      </div>
      <div class="flex items-center gap-2">
        <button
          hlmBtn variant="ghost" size="icon" class="cursor-pointer"
          (click)="loadUsers()"
        >
          <lucide-icon
            [img]="RefreshCwIcon" [size]="18"
            [ngClass]="{ 'animate-spin': loading() }"
          ></lucide-icon>
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center">
      <div class="relative flex-1">
        <lucide-icon
          [img]="SearchIcon" [size]="16"
          class="absolute top-1/2 left-3 -translate-y-1/2 text-(--muted-foreground)"
        ></lucide-icon>
        <input
          type="text"
          [(ngModel)]="searchQuery"
          (input)="filterUsers()"
          placeholder="Rechercher par nom ou email..."
          class="w-full rounded-sm border border-(--border) bg-(--background) py-2.5 pr-4 pl-10 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
        />
      </div>
      <select
        [(ngModel)]="statusFilter"
        (change)="loadUsers()"
        class="rounded-sm border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none cursor-pointer"
      >
        <option value="">Tous les statuts</option>
        <option value="ACTIVE">Actifs</option>
        <option value="INACTIVE">Inactifs</option>
        <option value="DELETED">Supprimés (soft)</option>
      </select>
    </div>

    <!-- Users table -->
    <div class="mt-6 overflow-x-auto rounded-sm border border-(--border) bg-(--card)">
      @if (loading()) {
        <div class="flex items-center justify-center py-12">
          <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-(--muted-foreground)"></lucide-icon>
        </div>
      } @else {
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-(--border) text-left">
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Utilisateur</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Email</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Rôle</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Statut</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Inscription</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Dernière connexion</th>
              <th class="px-4 py-3 font-medium text-(--muted-foreground)">Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (user of filteredUsers(); track user.id) {
              <tr class="border-b border-(--border) last:border-0 transition-colors hover:bg-(--accent)/50">
                <td class="px-4 py-3">
                  <div class="flex items-center gap-3">
                    <div
                      class="flex h-9 w-9 items-center justify-center rounded-full text-xs font-bold text-white"
                      [ngClass]="user.roles.includes('ADMIN') ? 'bg-zinc-700' : 'bg-blue-500'"
                    >
                      {{ getInitials(user) }}
                    </div>
                    <span class="font-medium text-(--foreground)">
                      {{ user.displayName || (user.firstName + ' ' + user.lastName) }}
                    </span>
                  </div>
                </td>
                <td class="px-4 py-3 text-(--muted-foreground)">
                  <div class="flex items-center gap-1.5">
                    {{ user.email }}
                    @if (user.emailVerified) {
                      <span class="text-green-500" title="Email vérifié">✓</span>
                    }
                  </div>
                </td>
                <td class="px-4 py-3">
                  @for (role of user.roles; track role) {
                    <span
                      class="inline-flex rounded-xs px-2 py-0.5 text-xs font-medium"
                      [ngClass]="role === 'ADMIN'
                        ? 'bg-(--muted) text-(--primary)'
                        : 'bg-(--muted) text-(--foreground)'"
                    >
                      {{ role }}
                    </span>
                  }
                </td>
                <td class="px-4 py-3">
                  <div class="flex flex-col gap-1">
                    <div class="flex items-center gap-1.5">
                      <span
                        class="inline-flex rounded-xs px-2 py-0.5 text-xs font-medium"
                        [ngClass]="getStatusClass(user.status)"
                      >
                        {{ getStatusLabel(user.status) }}
                      </span>
                      @if (user.accountLocked) {
                        <span
                          class="inline-flex items-center gap-1 rounded-full bg-red-500/10 px-2 py-0.5 text-xs font-medium text-red-500"
                          title="Compte verrouillé pour raisons de sécurité — la connexion est bloquée"
                        >
                          <lucide-icon [img]="LockIcon" [size]="10"></lucide-icon>
                          Verrouillé
                        </span>
                      }
                    </div>
                  </div>
                </td>
                <td class="px-4 py-3 text-(--muted-foreground)">
                  {{ user.registrationDate | date:'dd/MM/yyyy' }}
                </td>
                <td class="px-4 py-3 text-(--muted-foreground)">
                  {{ user.lastLoginDate ? (user.lastLoginDate | date:'dd/MM/yyyy HH:mm') : '—' }}
                </td>
                <td class="px-4 py-3">
                  <button
                    hlmBtn variant="ghost" size="icon"
                    class="h-8 w-8 cursor-pointer"
                    (click)="openEditUser(user)"
                    title="Modifier"
                  >
                    <lucide-icon [img]="PencilIcon" [size]="14" class="text-(--muted-foreground)"></lucide-icon>
                  </button>
                  <button
                    hlmBtn variant="ghost" size="icon"
                    class="h-8 w-8 cursor-pointer"
                    (click)="softDeleteUser(user)"
                    title="Désactiver"
                  >
                    <lucide-icon [img]="Trash2Icon" [size]="14" class="text-(--foreground)"></lucide-icon>
                  </button>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="7" class="px-4 py-12 text-center text-(--muted-foreground)">
                  Aucun utilisateur trouvé
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>

    <!-- Pagination -->
    @if (totalPages() > 1) {
      <div class="mt-4 flex items-center justify-between">
        <p class="text-xs text-(--muted-foreground)">
          Page {{ currentPage() + 1 }} sur {{ totalPages() }}
        </p>
        <div class="flex items-center gap-1">
          <button
            hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
            [disabled]="currentPage() === 0"
            (click)="changePage(currentPage() - 1)"
          >
            <lucide-icon [img]="ChevronLeftIcon" [size]="16"></lucide-icon>
          </button>
          <button
            hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
            [disabled]="currentPage() >= totalPages() - 1"
            (click)="changePage(currentPage() + 1)"
          >
            <lucide-icon [img]="ChevronRightIcon" [size]="16"></lucide-icon>
          </button>
        </div>
      </div>
    }

    <!-- Edit User Modal -->
    @if (showEditModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-end justify-center bg-black/50 p-0 backdrop-blur-sm sm:items-center sm:p-4"
        (click)="closeEditModal()"
        role="dialog"
        aria-modal="true"
        aria-labelledby="edit-user-title"
      >
        <div
          class="flex max-h-[min(92dvh,760px)] w-full max-w-xl flex-col rounded-t-lg border border-(--border) bg-(--card) shadow-2xl sm:rounded-lg"
          (click)="$event.stopPropagation()"
        >
          <!-- Modal Header -->
          <div class="shrink-0 border-b border-(--border) px-5 py-4 sm:px-6">
            <div class="flex items-start justify-between gap-3">
              <div class="flex min-w-0 flex-1 items-center gap-3">
                <div
                  class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-sm font-bold text-white"
                  [ngClass]="editForm.admin ? 'bg-zinc-700' : 'bg-blue-600'"
                >
                  {{ editingUser ? getInitials(editingUser) : '' }}
                </div>
                <div class="min-w-0">
                  <h3 id="edit-user-title" class="text-lg font-semibold tracking-tight text-(--foreground)">
                    Modifier l'utilisateur
                  </h3>
                  <p class="truncate text-xs text-(--muted-foreground)">{{ editingUser?.email }}</p>
                  <div class="mt-2 flex flex-wrap items-center gap-2">
                    <span
                      class="inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide"
                      [ngClass]="
                        editForm.admin
                          ? 'bg-zinc-500/15 text-zinc-700 dark:text-zinc-300'
                          : 'bg-blue-500/10 text-blue-700 dark:text-blue-300'
                      "
                    >
                      {{ editForm.admin ? 'Rôle admin' : 'Utilisateur standard' }}
                    </span>
                    @if (editForm.admin && editingUser?.roles?.includes('ADMIN') === false) {
                      <span class="text-[11px] text-amber-600 dark:text-amber-400">Non enregistré</span>
                    }
                  </div>
                </div>
              </div>
              <button
                hlmBtn variant="ghost" size="icon" class="h-9 w-9 shrink-0 cursor-pointer"
                type="button"
                (click)="closeEditModal()"
                aria-label="Fermer"
              >
                <lucide-icon [img]="XIcon" [size]="18"></lucide-icon>
              </button>
            </div>
          </div>

          <!-- Modal Body -->
          <div class="min-h-0 flex-1 space-y-6 overflow-y-auto overscroll-contain px-5 py-5 sm:px-6">
            <!-- Profil (lecture seule) -->
            <div class="rounded-lg border border-(--border)/80 bg-(--muted)/20 px-4 py-3">
              <p class="text-[11px] font-semibold uppercase tracking-wider text-(--muted-foreground)">
                Profil
              </p>
              <div class="mt-2 grid gap-3 sm:grid-cols-2">
                <div>
                  <p class="text-xs text-(--muted-foreground)">Nom affiché</p>
                  <p class="mt-0.5 text-sm font-medium text-(--foreground)">
                    {{ editingUser?.displayName || ((editingUser?.firstName || '') + ' ' + (editingUser?.lastName || '')) || '—' }}
                  </p>
                </div>
                <div>
                  <p class="text-xs text-(--muted-foreground)">Inscription</p>
                  <p class="mt-0.5 text-sm text-(--foreground)">
                    {{ editingUser?.registrationDate | date:'dd/MM/yyyy' }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Email -->
            <div>
              <label for="edit-user-email" class="mb-1.5 block text-sm font-medium text-(--foreground)">
                Adresse e-mail
              </label>
              <input
                id="edit-user-email"
                [(ngModel)]="editForm.email"
                type="email"
                autocomplete="email"
                class="w-full rounded-md border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none transition-shadow focus:border-(--primary)/50 focus:ring-2 focus:ring-(--primary)/20"
              />
              @if (editForm.email !== editingUser?.email) {
                <div
                  class="mt-2 flex gap-2 rounded-md border border-amber-500/25 bg-amber-500/8 px-3 py-2 text-xs text-amber-950 dark:text-amber-100"
                  role="status"
                >
                  <lucide-icon [img]="MailWarningIcon" [size]="14" class="mt-0.5 shrink-0 text-amber-600 dark:text-amber-400"></lucide-icon>
                  <span>Des notifications seront envoyées à l’ancienne et à la nouvelle adresse après enregistrement.</span>
                </div>
              }
            </div>

            <div class="h-px bg-(--border)"></div>

            <p class="text-[11px] font-semibold uppercase tracking-wider text-(--muted-foreground)">
              Droits et accès
            </p>

            <!-- Rôle administrateur -->
            <div class="space-y-2">
              <div class="flex items-center gap-2">
                <lucide-icon [img]="ShieldIcon" [size]="16" class="text-(--primary)"></lucide-icon>
                <span class="text-sm font-semibold text-(--foreground)">Niveau d’accès</span>
              </div>
              <p class="text-xs leading-relaxed text-(--muted-foreground)">
                Les administrateurs gèrent commandes, utilisateurs et contenus. Le rôle « utilisateur » reste toujours actif en arrière-plan.
              </p>
              <div [class]="segWrap" role="group" aria-label="Niveau d’accès">
                <button
                  type="button"
                  [class]="segBtn + ' ' + (editForm.admin ? segOn : segOff)"
                  [attr.aria-pressed]="editForm.admin"
                  (click)="editForm.admin = true"
                >
                  <lucide-icon [img]="ShieldIcon" [size]="15"></lucide-icon>
                  Admin
                </button>
                <button
                  type="button"
                  [class]="segBtn + ' ' + (!editForm.admin ? segOn : segOff)"
                  [attr.aria-pressed]="!editForm.admin"
                  [disabled]="cannotRevokeOwnAdmin()"
                  (click)="editForm.admin = false"
                >
                  <lucide-icon [img]="UserIcon" [size]="15"></lucide-icon>
                  Standard
                </button>
              </div>
              @if (cannotRevokeOwnAdmin() && editForm.admin) {
                <div class="flex gap-2 rounded-md border border-(--border) bg-(--muted)/30 px-3 py-2 text-xs text-(--muted-foreground)">
                  <lucide-icon [img]="InfoIcon" [size]="14" class="mt-0.5 shrink-0 text-(--foreground)"></lucide-icon>
                  <span>Vous ne pouvez pas retirer votre propre rôle administrateur depuis cette fiche. Promouvoir un autre admin avant, si besoin.</span>
                </div>
              }
            </div>

            <!-- Statut + verrouillage (carte unique) -->
            <div class="rounded-lg border border-(--border) bg-(--background) p-4">
              <p class="text-[11px] font-semibold uppercase tracking-wider text-(--muted-foreground)">
                État du compte
              </p>

              <div class="mt-4 space-y-5">
                <div class="space-y-2">
                  <div class="flex items-center justify-between gap-2">
                    <span class="text-sm font-medium text-(--foreground)">Statut</span>
                    <span class="hidden text-[11px] text-(--muted-foreground) sm:inline">Soft-delete si inactif</span>
                  </div>
                  <p class="text-xs text-(--muted-foreground)">
                    Compte actif ou désactivé (l’utilisateur ne peut plus se connecter si inactif).
                  </p>
                  <div [class]="segWrap" role="group" aria-label="Statut du compte">
                    <button
                      type="button"
                      [class]="segBtn + ' ' + (editForm.status === 'ACTIVE' ? segOn : segOff)"
                      [attr.aria-pressed]="editForm.status === 'ACTIVE'"
                      (click)="editForm.status = 'ACTIVE'"
                    >
                      <lucide-icon [img]="CheckIcon" [size]="15"></lucide-icon>
                      Actif
                    </button>
                    <button
                      type="button"
                      [class]="segBtn + ' ' + (editForm.status === 'INACTIVE' ? segOn : segOff)"
                      [attr.aria-pressed]="editForm.status === 'INACTIVE'"
                      (click)="editForm.status = 'INACTIVE'"
                    >
                      <lucide-icon [img]="UserIcon" [size]="15"></lucide-icon>
                      Inactif
                    </button>
                  </div>
                </div>

                <div class="h-px bg-(--border)"></div>

                <div class="space-y-2">
                  <div class="flex items-center justify-between gap-2">
                    <span class="text-sm font-medium text-(--foreground)">Verrouillage sécurité</span>
                  </div>
                  <p class="text-xs text-(--muted-foreground)">
                    Indépendant du statut : bloque la connexion (tentatives, fraude, etc.).
                  </p>
                  <div [class]="segWrap" role="group" aria-label="Verrouillage du compte">
                    <button
                      type="button"
                      [class]="segBtn + ' ' + (!editForm.locked ? segOn : segOff)"
                      [attr.aria-pressed]="!editForm.locked"
                      (click)="editForm.locked = false"
                    >
                      <lucide-icon [img]="UnlockIcon" [size]="15"></lucide-icon>
                      Déverrouillé
                    </button>
                    <button
                      type="button"
                      [class]="segBtn + ' ' + (editForm.locked ? segOn + ' text-red-600 dark:text-red-400' : segOff)"
                      [attr.aria-pressed]="editForm.locked"
                      (click)="editForm.locked = true"
                    >
                      <lucide-icon [img]="LockIcon" [size]="15"></lucide-icon>
                      Verrouillé
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Synthèse connexion -->
            <div
              class="flex gap-3 rounded-lg border-l-4 px-4 py-3"
              [ngClass]="accessSummary().boxClass"
              role="status"
            >
              <lucide-icon
                [img]="accessSummary().icon"
                [size]="18"
                class="mt-0.5 shrink-0"
                [ngClass]="accessSummary().iconClass"
              ></lucide-icon>
              <div>
                <p class="text-[11px] font-semibold uppercase tracking-wider text-(--muted-foreground)">
                  Résumé connexion
                </p>
                <p class="mt-1 text-sm font-medium leading-snug" [ngClass]="accessSummary().textClass">
                  {{ accessSummary().message }}
                </p>
              </div>
            </div>
          </div>

          <!-- Modal Footer -->
          <div
            class="shrink-0 border-t border-(--border) bg-(--card) px-5 py-4 sm:px-6"
          >
            <div class="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
              <button
                hlmBtn variant="ghost" size="sm"
                class="cursor-pointer gap-1.5 text-red-600 hover:bg-red-500/10 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                type="button"
                (click)="hardDeleteUser()"
              >
                <lucide-icon [img]="AlertTriangleIcon" [size]="14"></lucide-icon>
                Supprimer définitivement
              </button>
              <div class="flex w-full justify-end gap-2 sm:w-auto">
                <button
                  hlmBtn variant="outline" size="sm" class="min-h-10 flex-1 cursor-pointer sm:flex-initial"
                  type="button"
                  (click)="closeEditModal()"
                >
                  Annuler
                </button>
                <button
                  hlmBtn variant="default" size="sm" class="min-h-10 min-w-[7.5rem] flex-1 cursor-pointer gap-2 sm:flex-initial"
                  type="button"
                  [disabled]="saving()"
                  (click)="saveUser()"
                >
                  @if (saving()) {
                    <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                  } @else {
                    <lucide-icon [img]="SaveIcon" [size]="14"></lucide-icon>
                  }
                  Enregistrer
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    }

    <!-- Toast -->
    @if (toast()) {
      <div
        class="fixed right-4 bottom-4 z-[200] flex items-center gap-2 rounded-sm border px-4 py-3 shadow-xs"
        [ngClass]="{
          'border-emerald-500/30 bg-(--muted) text-(--foreground)': toast()!.type === 'success',
          'border-red-500/30 bg-red-500/10 text-red-500': toast()!.type === 'error',
        }"
      >
        @if (toast()!.type === 'success') {
          <lucide-icon [img]="CheckIcon" [size]="16"></lucide-icon>
        } @else {
          <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
        }
        <span class="text-sm font-medium">{{ toast()!.message }}</span>
      </div>
    }
  `,
})
export class AdminUsersComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly adminSse = inject(AdminSseService);
  readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly visiblePoll = inject(VisiblePollService);

  constructor() {
    effect(() => {
      const badge = this.adminSse.badgeUsers();
      if (badge > 0) {
        untracked(() => this.loadUsers({ silent: true }));
      }
    });
  }

  readonly UsersIcon = Users;
  readonly SearchIcon = Search;
  readonly RefreshCwIcon = RefreshCw;
  readonly ChevronLeftIcon = ChevronLeft;
  readonly ChevronRightIcon = ChevronRight;
  readonly ShieldIcon = Shield;
  readonly LockIcon = Lock;
  readonly UnlockIcon = Unlock;
  readonly MailIcon = Mail;
  readonly Loader2Icon = Loader2;
  readonly PencilIcon = Pencil;
  readonly XIcon = X;
  readonly CheckIcon = Check;
  readonly SaveIcon = Save;
  readonly UserIcon = User;
  readonly Trash2Icon = Trash2;
  readonly AlertTriangleIcon = AlertTriangle;
  readonly InfoIcon = Info;
  readonly MailWarningIcon = MailWarning;
  readonly BanIcon = Ban;

  /** Segmented control : bouton actif / inactif (modale édition) */
  readonly segWrap = 'flex w-full rounded-md border border-(--border) bg-(--muted)/25 p-0.5 gap-0.5 sm:inline-flex sm:w-auto';
  readonly segBtn =
    'flex min-h-11 flex-1 cursor-pointer items-center justify-center gap-2 rounded px-3 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-(--primary)/35 sm:flex-initial sm:min-w-[7.5rem]';
  readonly segOn =
    'bg-(--background) text-(--foreground) shadow-sm ring-1 ring-(--border) ring-inset';
  readonly segOff =
    'text-(--muted-foreground) hover:bg-(--muted)/60 hover:text-(--foreground)';

  readonly loading = signal(false);
  private readonly listFetch = createListFetchLoading(this.loading);
  readonly saving = signal(false);
  readonly users = signal<UserItem[]>([]);
  readonly filteredUsers = signal<UserItem[]>([]);
  readonly totalUsers = signal(0);
  readonly totalPages = signal(0);
  readonly currentPage = signal(0);
  readonly showEditModal = signal(false);
  readonly toast = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  searchQuery = '';
  statusFilter = '';
  editingUser: UserItem | null = null;
  editForm = { status: 'ACTIVE', locked: false, email: '', admin: false };

  ngOnInit(): void {
    this.loadUsers();
    this.visiblePoll.subscribeWhileVisible(
      this.destroyRef,
      environment.dashboardPollIntervalMs,
      () => this.loadUsers({ silent: true }),
    );
  }

  loadUsers(options?: { silent?: boolean }): void {
    const silent = options?.silent === true;
    this.listFetch.beforeFetch(silent);
    const params: Record<string, string> = {
      page: this.currentPage().toString(),
      size: '20',
    };
    if (this.statusFilter) params['status'] = this.statusFilter;

    this.http
      .get<ApiResponse<PageResponse<UserItem>>>(
        `${environment.apiUrl}/api/v1/admin/users`,
        { params, withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          const page = res.data!;
          this.users.set(page.content);
          this.filteredUsers.set(page.content);
          this.totalUsers.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.listFetch.afterFetch();
        },
        error: () => {
          this.listFetch.afterFetch();
        },
      });
  }

  filterUsers(): void {
    const q = this.searchQuery.toLowerCase();
    if (!q) {
      this.filteredUsers.set(this.users());
      return;
    }
    this.filteredUsers.set(
      this.users().filter(
        (u) =>
          u.email.toLowerCase().includes(q) ||
          (u.firstName + ' ' + u.lastName).toLowerCase().includes(q) ||
          (u.displayName || '').toLowerCase().includes(q),
      ),
    );
  }

  changePage(page: number): void {
    this.currentPage.set(page);
    this.loadUsers();
  }

  getInitials(user: UserItem): string {
    return (
      (user.firstName?.[0] || '') + (user.lastName?.[0] || '')
    ).toUpperCase() || user.email[0].toUpperCase();
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-green-500/10 text-green-500';
      case 'INACTIVE': return 'bg-gray-500/10 text-gray-400';
      case 'DELETED': return 'bg-red-500/10 text-red-500';
      default: return 'bg-gray-500/10 text-gray-400';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'Actif';
      case 'INACTIVE': return 'Inactif';
      case 'DELETED': return 'Supprimé';
      default: return status;
    }
  }

  // ========== Edit User ==========

  openEditUser(user: UserItem): void {
    this.editingUser = user;
    this.editForm = {
      status: user.status || 'ACTIVE',
      locked: user.accountLocked === true,
      email: user.email,
      admin: user.roles?.includes('ADMIN') ?? false,
    };
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.editingUser = null;
  }

  saveUser(): void {
    if (!this.editingUser) return;
    this.saving.set(true);

    const payload: Record<string, unknown> = {
      status: this.editForm.status,
      locked: this.editForm.locked,
      admin: this.editForm.admin,
    };

    // Include email only if changed
    if (this.editForm.email && this.editForm.email !== this.editingUser.email) {
      payload['email'] = this.editForm.email;
    }

    this.http
      .put<ApiResponse<void>>(
        `${environment.apiUrl}/api/v1/admin/users/${this.editingUser.id}`,
        payload,
        { withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.showToast('success', 'Utilisateur mis à jour');
          this.closeEditModal();
          this.saving.set(false);
          this.loadUsers();
        },
        error: (err) => {
          const msg =
            err?.error?.message ||
            (typeof err?.error === 'string' ? err.error : null) ||
            'Erreur lors de la mise à jour';
          this.showToast('error', msg);
          this.saving.set(false);
        },
      });
  }

  cannotRevokeOwnAdmin(): boolean {
    const me = this.authService.user();
    return !!me && this.editingUser?.id === me.id;
  }

  softDeleteUser(user: UserItem): void {
    if (!confirm(`Désactiver le compte de ${user.email} ? (soft delete)`)) return;

    this.http
      .put<ApiResponse<void>>(
        `${environment.apiUrl}/api/v1/admin/users/${user.id}/soft-delete`,
        {},
        { withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.showToast('success', 'Utilisateur désactivé');
          this.loadUsers();
        },
        error: () => this.showToast('error', 'Erreur lors de la désactivation'),
      });
  }

  hardDeleteUser(): void {
    if (!this.editingUser) return;
    const confirmMsg = `⚠️ SUPPRESSION DÉFINITIVE de ${this.editingUser.email}\n\nCette action est IRRÉVERSIBLE.\nLes commandes et avis seront anonymisés.\n\nÊtes-vous sûr ?`;
    if (!confirm(confirmMsg)) return;

    this.http
      .delete<ApiResponse<void>>(
        `${environment.apiUrl}/api/v1/admin/users/${this.editingUser.id}`,
        { withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.showToast('success', 'Utilisateur supprimé définitivement');
          this.closeEditModal();
          this.loadUsers();
        },
        error: () => this.showToast('error', 'Erreur lors de la suppression'),
      });
  }

  accessSummary(): {
    message: string;
    boxClass: string;
    textClass: string;
    iconClass: string;
    icon: typeof Check | typeof Lock | typeof Ban;
  } {
    if (this.editForm.status === 'INACTIVE') {
      return {
        message:
          'Connexion impossible : le compte est désactivé. Aucune connexion n’est possible, même sans verrouillage.',
        boxClass: 'border-red-500/65 bg-red-500/[0.06]',
        textClass: 'text-red-800 dark:text-red-200',
        iconClass: 'text-red-600 dark:text-red-400',
        icon: this.BanIcon,
      };
    }
    if (this.editForm.locked) {
      return {
        message:
          'Connexion bloquée : le compte est verrouillé pour sécurité. Cela s’applique même si le statut reste « actif ».',
        boxClass: 'border-amber-500/65 bg-amber-500/[0.07]',
        textClass: 'text-amber-950 dark:text-amber-100',
        iconClass: 'text-amber-600 dark:text-amber-400',
        icon: this.LockIcon,
      };
    }
    return {
      message: 'Connexion autorisée : compte actif et déverrouillé.',
      boxClass: 'border-emerald-500/65 bg-emerald-500/[0.06]',
      textClass: 'text-emerald-900 dark:text-emerald-100',
      iconClass: 'text-emerald-600 dark:text-emerald-400',
      icon: this.CheckIcon,
    };
  }

  private showToast(type: 'success' | 'error', message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => this.toast.set(null), 3000);
  }
}
