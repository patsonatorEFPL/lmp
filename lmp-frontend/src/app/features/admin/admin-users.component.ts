import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  effect,
  untracked,
  computed,
} from '@angular/core';
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
  KeyRound,
  Eye,
  EyeOff,
  Filter,
  ArrowUpDown,
  Columns3,
  Phone,
  MoreHorizontal,
  Plus,
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AdminSseService } from '../../core/services/admin-sse.service';
import { AdminService } from '../../core/services/admin.service';
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
    <div class="crm-list-view flex h-full flex-col overflow-hidden bg-white">
    <!-- Barre de filtres inline (style CRM) -->
    <div class="flex items-center justify-between gap-2 px-5 py-4">
      <div class="flex items-center"></div>
      <!-- Actions droite -->
      <div class="flex items-center gap-0.5">
        <button
          hlmBtn variant="ghost" size="icon" type="button"
          class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
          title="Actualiser"
          (click)="loadUsers()"
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
        <div class="relative">
          <button
            hlmBtn variant="ghost" size="sm" type="button"
            class="h-7 cursor-pointer gap-1.5 px-2"
            [ngClass]="showSortMenu() ? 'bg-zinc-100 text-zinc-800 dark:bg-zinc-800 dark:text-zinc-200' : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-800 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-200'"
            (click)="showSortMenu.set(!showSortMenu())"
          >
            <lucide-icon [img]="ArrowUpDownIcon" [size]="14"></lucide-icon>
            <span class="text-sm">Sort</span>
          </button>
          @if (showSortMenu()) {
            <div class="absolute right-0 top-full z-50 mt-1 w-48 rounded-lg border border-zinc-200 bg-white py-1 shadow-lg dark:border-zinc-700 dark:bg-zinc-900">
              @for (opt of sortOptions; track opt.key) {
                <button
                  type="button"
                  class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm transition-colors hover:bg-zinc-50 dark:hover:bg-zinc-800"
                  [ngClass]="currentSort() === opt.key ? 'text-zinc-900 font-medium dark:text-zinc-100' : 'text-zinc-600 dark:text-zinc-400'"
                  (click)="applySort(opt.key)"
                >
                  @if (currentSort() === opt.key) {
                    <lucide-icon [img]="CheckIcon" [size]="14" class="text-zinc-900 dark:text-zinc-100"></lucide-icon>
                  } @else {
                    <span class="w-3.5"></span>
                  }
                  {{ opt.label }}
                  @if (currentSort() === opt.key) {
                    <span class="ml-auto text-xs text-zinc-400">{{ sortDirection() === 'asc' ? '↑' : '↓' }}</span>
                  }
                </button>
              }
            </div>
          }
        </div>
        <button
          hlmBtn variant="default" size="sm" type="button"
          class="ml-1 h-7 cursor-pointer gap-1.5 px-2.5"
          (click)="openCreateUserModal()"
        >
          <lucide-icon [img]="PlusIcon" [size]="14"></lucide-icon>
          <span class="text-sm">Ajouter</span>
        </button>
      </div>
    </div>

    <!-- Panneau de filtres (toggle) -->
    @if (showFilterPanel()) {
      <div class="flex items-center gap-2 border-b border-zinc-100 px-5 pb-3 dark:border-zinc-800">
        <input
          type="text"
          [(ngModel)]="searchQuery"
          (input)="filterUsers()"
          placeholder="Rechercher par email ou nom…"
          class="h-8 w-56 rounded-lg border border-zinc-200 bg-zinc-50 px-3 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
        />
        <input
          type="text"
          [(ngModel)]="phoneFilter"
          (input)="filterUsers()"
          placeholder="Téléphone"
          class="hidden h-8 w-40 rounded-lg border border-zinc-200 bg-zinc-50 px-3 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none focus:border-zinc-300 focus:bg-white sm:block dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
        />
        <input
          type="text"
          [(ngModel)]="orgFilter"
          (input)="filterUsers()"
          placeholder="Organisation"
          class="hidden h-8 w-40 rounded-lg border border-zinc-200 bg-zinc-50 px-3 text-sm text-zinc-700 placeholder:text-zinc-400 outline-none focus:border-zinc-300 focus:bg-white md:block dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
        />
        <select
          [(ngModel)]="statusFilter"
          (change)="currentPage.set(0); loadUsers()"
          class="h-8 w-36 cursor-pointer rounded-lg border border-zinc-200 bg-zinc-50 px-2 text-sm text-zinc-700 outline-none focus:border-zinc-300 focus:bg-white dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-300 dark:focus:border-zinc-600 dark:focus:bg-zinc-900"
        >
          <option value="">Tous les statuts</option>
          <option value="ACTIVE">Actif</option>
          <option value="INACTIVE">Inactif</option>
        </select>
        @if (hasActiveFilters()) {
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
    }

    <!-- Liste (style CRM - pas de bordure extérieure) -->
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
        <!-- En-tête colonnes (style CRM - fond gris arrondi, mb-2) -->
        <div class="mb-2 flex items-center rounded-lg bg-zinc-100 py-1.5 text-sm font-normal leading-none text-zinc-500 dark:bg-zinc-800/70 dark:text-zinc-400">
          <div class="flex w-10 shrink-0 items-center justify-center">
            <input
              type="checkbox"
              class="h-3.5 w-3.5 cursor-pointer rounded-xs border-zinc-400 text-zinc-600 focus:ring-zinc-400"
              [checked]="allRowsSelected()"
              (change)="toggleSelectAll($event)"
            />
          </div>
          <div class="w-64 shrink-0 px-2">Email</div>
          <div class="hidden w-48 shrink-0 px-2 text-center sm:block">Phone</div>
          <div class="hidden w-48 shrink-0 px-2 text-center md:block">Organisation</div>
          <div class="w-32 shrink-0 px-2 text-center">Last Modified</div>
        </div>

        <!-- Lignes (style CRM - cliquables, sans bordures visibles entre les lignes) -->
        <div>
          @for (user of filteredUsers(); track user.id) {
            <div
              class="group flex h-10 cursor-pointer items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
              (click)="openEditUser(user)"
            >
              <div class="flex w-10 shrink-0 items-center justify-center" (click)="$event.stopPropagation()">
                <input
                  type="checkbox"
                  class="h-3.5 w-3.5 cursor-pointer rounded-xs border-zinc-400 text-zinc-600 focus:ring-zinc-400"
                  [checked]="selectedUserIds().has(user.id)"
                  (change)="toggleUserSelected(user.id)"
                />
              </div>
              <div class="w-64 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">
                {{ user.email }}
              </div>
              <div class="hidden w-48 shrink-0 truncate px-2 text-center text-sm leading-none text-zinc-600 sm:block dark:text-zinc-400">
                {{ user.phone || '' }}
              </div>
              <div class="hidden w-48 shrink-0 truncate px-2 text-center text-sm leading-none text-zinc-600 md:block dark:text-zinc-400">
                {{ organizationLabel(user) !== '—' ? organizationLabel(user) : '' }}
              </div>
              <div class="w-32 shrink-0 px-2 text-center text-sm leading-none text-zinc-500 dark:text-zinc-400">
                {{ formatRelativeTimeFr(user.lastLoginDate) }}
              </div>
            </div>
          } @empty {
            <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
              Aucun utilisateur trouvé
            </div>
          }
        </div>
      }
    </div>

    <!-- Footer pagination (style CRM - bordered button group) -->
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
        <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ filteredUsers().length }}</span>
        <span>of</span>
        <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ totalUsers() }}</span>
      </div>
    </div>

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
                  <h3 id="edit-user-title" class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">
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
            <div class="rounded-lg border border-outline-gray-2 border-(--border)/80 bg-(--muted)/20 px-4 py-3">
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
            <div class="rounded-lg border border-outline-gray-2 border-(--border) bg-(--background) p-4">
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

            <!-- Changement mot de passe -->
            <div class="rounded-lg border border-outline-gray-2 border-(--border) bg-(--background) p-4">
              <div class="flex cursor-pointer items-center justify-between" (click)="showPwdSection.set(!showPwdSection())">
                <div class="flex items-center gap-2">
                  <lucide-icon [img]="KeyRoundIcon" [size]="16" class="text-(--primary)"></lucide-icon>
                  <span class="text-sm font-semibold text-(--foreground)">Changer le mot de passe</span>
                </div>
                <span class="text-xs text-(--muted-foreground)">{{ showPwdSection() ? 'Masquer' : 'Afficher' }}</span>
              </div>

              @if (showPwdSection()) {
                <div class="mt-4 space-y-3">
                  @if (userPwdSuccess()) {
                    <div class="flex items-center gap-2 rounded-sm border border-emerald-500/20 bg-(--muted) p-2.5 text-xs text-(--foreground)">
                      <lucide-icon [img]="CheckIcon" [size]="14"></lucide-icon>
                      {{ userPwdSuccess() }}
                    </div>
                  }
                  @if (userPwdError()) {
                    <div class="flex items-center gap-2 rounded-sm border border-red-500/20 bg-red-500/10 p-2.5 text-xs text-red-400">
                      <lucide-icon [img]="XIcon" [size]="14"></lucide-icon>
                      {{ userPwdError() }}
                    </div>
                  }
                  <p class="text-xs text-(--muted-foreground)">
                    Définissez un nouveau mot de passe pour cet utilisateur. Il sera déconnecté de toutes ses sessions.
                  </p>
                  <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <div>
                      <label class="text-xs font-medium text-(--foreground)">Nouveau mot de passe</label>
                      <div class="relative mt-1">
                        <input
                          [type]="showUserNewPwd() ? 'text' : 'password'"
                          [(ngModel)]="userPwdForm.newPassword"
                          placeholder="Min. 8 caractères"
                          autocomplete="new-password"
                          class="w-full rounded-sm border border-(--border) bg-(--card) px-3 py-2 pr-9 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
                        />
                        <button
                          type="button"
                          class="absolute right-2 top-1/2 -translate-y-1/2 cursor-pointer text-(--muted-foreground) hover:text-(--foreground)"
                          (click)="showUserNewPwd.set(!showUserNewPwd())"
                        >
                          <lucide-icon [img]="showUserNewPwd() ? EyeOffIcon : EyeIcon" [size]="14"></lucide-icon>
                        </button>
                      </div>
                    </div>
                    <div>
                      <label class="text-xs font-medium text-(--foreground)">Confirmer</label>
                      <input
                        [type]="showUserNewPwd() ? 'text' : 'password'"
                        [(ngModel)]="userPwdForm.confirmPassword"
                        placeholder="Confirmer"
                        autocomplete="new-password"
                        class="mt-1 w-full rounded-sm border border-(--border) bg-(--card) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
                      />
                    </div>
                  </div>
                  <div class="flex justify-end">
                    <button
                      hlmBtn variant="outline" size="sm" class="cursor-pointer gap-1.5"
                      (click)="changeUserPassword()"
                      [disabled]="changingUserPwd()"
                    >
                      @if (changingUserPwd()) {
                        <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                      } @else {
                        <lucide-icon [img]="KeyRoundIcon" [size]="14"></lucide-icon>
                      }
                      Appliquer
                    </button>
                  </div>
                </div>
              }
            </div>

            <!-- Synthèse connexion -->
            <div
              class="flex gap-3 rounded-lg border border-outline-gray-2-l-4 px-4 py-3"
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

    <!-- Create User Modal -->
    @if (showCreateUserModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        (click)="closeCreateUserModal()"
      >
        <div
          class="mx-4 w-full max-w-md rounded-sm border border-(--border) bg-(--card) shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          <div class="flex items-center justify-between border-b border-(--border) px-6 py-4">
            <h3 class="text-base font-medium tracking-[0.02em] text-zinc-500 dark:text-zinc-400">Ajouter un utilisateur</h3>
            <button
              hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
              (click)="closeCreateUserModal()"
            >
              <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
            </button>
          </div>

          <div class="space-y-4 px-6 py-5">
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Email *</label>
              <input
                [(ngModel)]="newUserForm.email"
                type="email"
                autocomplete="off"
                class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="email&#64;example.com"
              />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Prénom</label>
                <input
                  [(ngModel)]="newUserForm.firstName"
                  type="text"
                  class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                  placeholder="Prénom"
                />
              </div>
              <div>
                <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Nom</label>
                <input
                  [(ngModel)]="newUserForm.lastName"
                  type="text"
                  class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                  placeholder="Nom"
                />
              </div>
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Mot de passe *</label>
              <div class="relative">
                <input
                  [(ngModel)]="newUserForm.password"
                  [type]="showCreatePwd() ? 'text' : 'password'"
                  autocomplete="new-password"
                  class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 pr-9 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                  placeholder="Min. 8 caractères"
                />
                <button
                  type="button"
                  class="absolute right-2 top-1/2 -translate-y-1/2 cursor-pointer text-(--muted-foreground) hover:text-(--foreground)"
                  (click)="showCreatePwd.set(!showCreatePwd())"
                >
                  <lucide-icon [img]="showCreatePwd() ? EyeOffIcon : EyeIcon" [size]="14"></lucide-icon>
                </button>
              </div>
              <p class="mt-1 text-xs text-(--muted-foreground)">Majuscule, minuscule, chiffre, caractère spécial</p>
            </div>
            <div class="flex items-center gap-2">
              <input
                type="checkbox"
                [(ngModel)]="newUserForm.admin"
                class="h-3.5 w-3.5 cursor-pointer rounded-xs border-zinc-400 text-zinc-600 focus:ring-zinc-400"
              />
              <label class="text-sm text-(--foreground)">Rôle administrateur</label>
            </div>
          </div>

          <div class="flex items-center justify-end gap-2 border-t border-(--border) px-6 py-4">
            <button hlmBtn variant="outline" size="sm" class="cursor-pointer" (click)="closeCreateUserModal()">
              Annuler
            </button>
            <button
              hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
              [disabled]="creatingUser()"
              (click)="createUser()"
            >
              @if (creatingUser()) {
                <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
              } @else {
                <lucide-icon [img]="PlusIcon" [size]="14"></lucide-icon>
              }
              Créer
            </button>
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
  private readonly adminService = inject(AdminService);
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
  readonly KeyRoundIcon = KeyRound;
  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;
  readonly FilterIcon = Filter;
  readonly ArrowUpDownIcon = ArrowUpDown;
  readonly Columns3Icon = Columns3;
  readonly PhoneIcon = Phone;
  readonly MoreHorizontalIcon = MoreHorizontal;
  readonly PlusIcon = Plus;

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

  // CRM-style pagination
  readonly pageSizes = [20, 50, 100];
  readonly pageSize = signal(20);

  // Filter & Sort
  readonly showFilterPanel = signal(false);
  readonly showSortMenu = signal(false);
  readonly currentSort = signal<string>('lastLoginDate');
  readonly sortDirection = signal<'asc' | 'desc'>('desc');
  readonly sortOptions = [
    { key: 'email', label: 'Email' },
    { key: 'registrationDate', label: 'Date d\'inscription' },
    { key: 'lastLoginDate', label: 'Dernière connexion' },
    { key: 'displayName', label: 'Nom' },
  ];

  // Create user
  readonly showCreateUserModal = signal(false);
  readonly creatingUser = signal(false);
  readonly showCreatePwd = signal(false);
  newUserForm = { email: '', firstName: '', lastName: '', password: '', admin: false };

  searchQuery = '';
  statusFilter = '';
  phoneFilter = '';
  orgFilter = '';
  editingUser: UserItem | null = null;
  editForm = { status: 'ACTIVE', locked: false, email: '', admin: false };

  // User password change (inside edit modal)
  readonly showPwdSection = signal(false);
  readonly changingUserPwd = signal(false);
  readonly userPwdSuccess = signal<string | null>(null);
  readonly userPwdError = signal<string | null>(null);
  readonly showUserNewPwd = signal(false);
  userPwdForm = { newPassword: '', confirmPassword: '' };

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
      size: this.pageSize().toString(),
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
          this.selectedUserIds.set(new Set());
          this.listFetch.afterFetch();
        },
        error: () => {
          this.listFetch.afterFetch();
        },
      });
  }

  filterUsers(): void {
    const q = this.searchQuery.toLowerCase();
    const phone = this.phoneFilter.toLowerCase();
    
    let filtered = this.users();
    
    if (q) {
      filtered = filtered.filter(
        (u) =>
          u.email.toLowerCase().includes(q) ||
          (u.firstName + ' ' + u.lastName).toLowerCase().includes(q) ||
          (u.displayName || '').toLowerCase().includes(q),
      );
    }
    
    if (phone) {
      filtered = filtered.filter(
        (u) => (u.phone || '').toLowerCase().includes(phone),
      );
    }

    const org = this.orgFilter.toLowerCase();
    if (org) {
      filtered = filtered.filter(
        (u) => this.organizationLabel(u).toLowerCase().includes(org),
      );
    }

    this.filteredUsers.set(filtered);
  }

  changePage(page: number): void {
    this.currentPage.set(page);
    this.selectedUserIds.set(new Set());
    this.loadUsers();
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadUsers();
  }

  /** Sélection multiple (cases à cocher) */
  readonly selectedUserIds = signal<Set<string>>(new Set());
  readonly filterPanelOpen = signal(true);

  readonly allRowsSelected = computed(() => {
    const list = this.filteredUsers();
    if (list.length === 0) return false;
    const sel = this.selectedUserIds();
    return list.every((u) => sel.has(u.id));
  });

  toggleSelectAll(ev: Event): void {
    const checked = (ev.target as HTMLInputElement).checked;
    const list = this.filteredUsers();
    if (checked) {
      this.selectedUserIds.set(new Set(list.map((u) => u.id)));
    } else {
      this.selectedUserIds.set(new Set());
    }
  }

  toggleUserSelected(id: string): void {
    const next = new Set(this.selectedUserIds());
    if (next.has(id)) next.delete(id);
    else next.add(id);
    this.selectedUserIds.set(next);
  }

  organizationLabel(user: UserItem): string {
    if (user.companyName?.trim()) return user.companyName.trim();
    const parts = [user.city, user.country].filter(Boolean);
    return parts.length ? parts.join(', ') : '—';
  }

  formatRelativeTimeFr(iso: string | null | undefined): string {
    if (!iso) return '—';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '—';
    const diffMs = Date.now() - d.getTime();
    const sec = Math.floor(diffMs / 1000);
    if (sec < 45) return 'À l’instant';
    const min = Math.floor(sec / 60);
    const hours = Math.floor(min / 60);
    const days = Math.floor(hours / 24);
    if (min < 60) return min <= 1 ? 'Il y a 1 min' : `Il y a ${min} min`;
    if (hours < 24) return hours <= 1 ? 'Il y a 1 h' : `Il y a ${hours} h`;
    if (days < 7) return days === 1 ? 'Il y a 1 jour' : `Il y a ${days} j`;
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
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
    this.resetPwdSection();
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.editingUser = null;
    this.resetPwdSection();
  }

  private resetPwdSection(): void {
    this.showPwdSection.set(false);
    this.userPwdForm = { newPassword: '', confirmPassword: '' };
    this.userPwdSuccess.set(null);
    this.userPwdError.set(null);
    this.showUserNewPwd.set(false);
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

  changeUserPassword(): void {
    this.userPwdSuccess.set(null);
    this.userPwdError.set(null);

    if (!this.editingUser) return;

    if (!this.userPwdForm.newPassword) {
      this.userPwdError.set('Veuillez saisir un nouveau mot de passe.');
      return;
    }
    if (this.userPwdForm.newPassword !== this.userPwdForm.confirmPassword) {
      this.userPwdError.set('Les mots de passe ne correspondent pas.');
      return;
    }

    const pwd = this.userPwdForm.newPassword;
    if (pwd.length < 8 || !/[A-Z]/.test(pwd) || !/[a-z]/.test(pwd) || !/\d/.test(pwd) || !/[^A-Za-z0-9]/.test(pwd)) {
      this.userPwdError.set('Le mot de passe doit contenir au moins 8 caractères, incluant majuscules, minuscules, chiffres et caractères spéciaux.');
      return;
    }

    this.changingUserPwd.set(true);
    this.adminService
      .changeUserPassword(this.editingUser.id, this.userPwdForm)
      .subscribe({
        next: () => {
          this.userPwdSuccess.set('Mot de passe modifié — l\'utilisateur a été déconnecté.');
          this.userPwdForm = { newPassword: '', confirmPassword: '' };
          this.changingUserPwd.set(false);
          setTimeout(() => this.userPwdSuccess.set(null), 5000);
        },
        error: (err: any) => {
          this.userPwdError.set(
            err?.error?.message || err?.message || 'Erreur lors du changement de mot de passe.',
          );
          this.changingUserPwd.set(false);
        },
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

  // ========== Filter helpers ==========

  hasActiveFilters(): boolean {
    return !!(this.searchQuery || this.phoneFilter || this.orgFilter || this.statusFilter);
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.phoneFilter = '';
    this.orgFilter = '';
    this.statusFilter = '';
    this.currentPage.set(0);
    this.loadUsers();
  }

  // ========== Sort ==========

  applySort(key: string): void {
    if (this.currentSort() === key) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.currentSort.set(key);
      this.sortDirection.set('asc');
    }
    this.showSortMenu.set(false);
    this.applySortToList();
  }

  private applySortToList(): void {
    const key = this.currentSort();
    const dir = this.sortDirection() === 'asc' ? 1 : -1;
    const sorted = [...this.filteredUsers()].sort((a, b) => {
      let va = '';
      let vb = '';
      switch (key) {
        case 'email': va = a.email || ''; vb = b.email || ''; break;
        case 'displayName': va = a.displayName || ''; vb = b.displayName || ''; break;
        case 'registrationDate': va = a.registrationDate || ''; vb = b.registrationDate || ''; break;
        case 'lastLoginDate': va = a.lastLoginDate || ''; vb = b.lastLoginDate || ''; break;
      }
      return va.localeCompare(vb, 'fr', { sensitivity: 'base' }) * dir;
    });
    this.filteredUsers.set(sorted);
  }

  // ========== Create User ==========

  openCreateUserModal(): void {
    this.newUserForm = { email: '', firstName: '', lastName: '', password: '', admin: false };
    this.showCreatePwd.set(false);
    this.showCreateUserModal.set(true);
  }

  closeCreateUserModal(): void {
    this.showCreateUserModal.set(false);
  }

  createUser(): void {
    const email = this.newUserForm.email.trim();
    if (!email) {
      this.showToast('error', 'L\'adresse email est obligatoire');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      this.showToast('error', 'Adresse email invalide');
      return;
    }
    const pwd = this.newUserForm.password;
    if (!pwd || pwd.length < 8) {
      this.showToast('error', 'Le mot de passe doit contenir au moins 8 caractères');
      return;
    }
    if (!/[A-Z]/.test(pwd) || !/[a-z]/.test(pwd) || !/\d/.test(pwd) || !/[^A-Za-z0-9]/.test(pwd)) {
      this.showToast('error', 'Le mot de passe doit contenir majuscules, minuscules, chiffres et caractères spéciaux');
      return;
    }

    this.creatingUser.set(true);
    this.http
      .post<ApiResponse<unknown>>(
        `${environment.apiUrl}/api/v1/admin/users`,
        {
          email,
          firstName: this.newUserForm.firstName.trim() || null,
          lastName: this.newUserForm.lastName.trim() || null,
          password: pwd,
          admin: this.newUserForm.admin,
        },
        { withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.showToast('success', 'Utilisateur créé : ' + email);
          this.closeCreateUserModal();
          this.creatingUser.set(false);
          this.loadUsers();
        },
        error: (err: { error?: { message?: string } }) => {
          this.showToast('error', err.error?.message || 'Erreur lors de la création');
          this.creatingUser.set(false);
        },
      });
  }

  private showToast(type: 'success' | 'error', message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => this.toast.set(null), 3000);
  }
}
