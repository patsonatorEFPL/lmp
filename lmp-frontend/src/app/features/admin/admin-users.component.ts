import { Component, inject, OnInit, signal } from '@angular/core';
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
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

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
        <h1 class="font-display text-2xl font-bold text-(--foreground)">
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
          class="w-full rounded-lg border border-(--border) bg-(--background) py-2.5 pr-4 pl-10 text-sm text-(--foreground) outline-none focus:border-(--primary)/50"
        />
      </div>
      <select
        [(ngModel)]="statusFilter"
        (change)="loadUsers()"
        class="rounded-lg border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none cursor-pointer"
      >
        <option value="">Tous les statuts</option>
        <option value="ACTIVE">Actifs</option>
        <option value="INACTIVE">Inactifs</option>
        <option value="DELETED">Supprimés (soft)</option>
      </select>
    </div>

    <!-- Users table -->
    <div class="mt-6 overflow-x-auto rounded-xl border border-(--border) bg-(--card)">
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
                      [ngClass]="user.roles.includes('ADMIN') ? 'bg-violet-500' : 'bg-blue-500'"
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
                      class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                      [ngClass]="role === 'ADMIN'
                        ? 'bg-violet-500/10 text-violet-500'
                        : 'bg-blue-500/10 text-blue-500'"
                    >
                      {{ role }}
                    </span>
                  }
                </td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-1.5">
                    <span
                      class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                      [ngClass]="getStatusClass(user.status)"
                    >
                      {{ getStatusLabel(user.status) }}
                    </span>
                    @if (user.accountLocked) {
                      <span class="inline-flex rounded-full bg-red-500/10 px-2 py-0.5 text-xs font-medium text-red-500">
                        🔒
                      </span>
                    }
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
                    <lucide-icon [img]="Trash2Icon" [size]="14" class="text-amber-500"></lucide-icon>
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
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        (click)="closeEditModal()"
      >
        <div
          class="mx-4 w-full max-w-lg rounded-2xl border border-(--border) bg-(--card) shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          <!-- Modal Header -->
          <div class="flex items-center justify-between border-b border-(--border) px-6 py-4">
            <div class="flex items-center gap-3">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-full text-sm font-bold text-white"
                [ngClass]="editingUser?.roles?.includes('ADMIN') ? 'bg-violet-500' : 'bg-blue-500'"
              >
                {{ editingUser ? getInitials(editingUser) : '' }}
              </div>
              <div>
                <h3 class="font-display text-lg font-bold text-(--foreground)">
                  Modifier l'utilisateur
                </h3>
                <p class="text-xs text-(--muted-foreground)">{{ editingUser?.email }}</p>
              </div>
            </div>
            <button
              hlmBtn variant="ghost" size="icon" class="h-8 w-8 cursor-pointer"
              (click)="closeEditModal()"
            >
              <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
            </button>
          </div>

          <!-- Modal Body -->
          <div class="space-y-5 px-6 py-5">
            <!-- User Info (read-only) -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <p class="text-xs font-medium text-(--muted-foreground)">Nom complet</p>
                <p class="mt-0.5 text-sm font-medium text-(--foreground)">
                  {{ editingUser?.displayName || ((editingUser?.firstName || '') + ' ' + (editingUser?.lastName || '')) }}
                </p>
              </div>
              <div>
                <p class="text-xs font-medium text-(--muted-foreground)">Inscription</p>
                <p class="mt-0.5 text-sm text-(--foreground)">
                  {{ editingUser?.registrationDate | date:'dd/MM/yyyy' }}
                </p>
              </div>
            </div>

            <div class="h-px bg-(--border)"></div>

            <!-- Email -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-(--foreground)">Email</label>
              <input
                [(ngModel)]="editForm.email"
                type="email"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
              />
              @if (editForm.email !== editingUser?.email) {
                <p class="mt-1 text-xs text-amber-500">
                  ⚠️ Un email de notification sera envoyé à l'ancien et au nouvel email.
                </p>
              }
            </div>

            <!-- Status -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-(--foreground)">Statut du compte</label>
              <div class="flex gap-2">
                <button
                  hlmBtn size="sm" class="cursor-pointer gap-1.5"
                  [variant]="editForm.status === 'ACTIVE' ? 'default' : 'outline'"
                  (click)="editForm.status = 'ACTIVE'"
                >
                  <lucide-icon [img]="CheckIcon" [size]="14"></lucide-icon>
                  Actif
                </button>
                <button
                  hlmBtn size="sm" class="cursor-pointer gap-1.5"
                  [variant]="editForm.status === 'INACTIVE' ? 'default' : 'outline'"
                  (click)="editForm.status = 'INACTIVE'"
                >
                  <lucide-icon [img]="UserIcon" [size]="14"></lucide-icon>
                  Inactif
                </button>
              </div>
            </div>

            <!-- Lock -->
            <div>
              <label class="mb-1.5 block text-sm font-medium text-(--foreground)">Verrouillage</label>
              <div class="flex gap-2">
                <button
                  hlmBtn size="sm" class="cursor-pointer gap-1.5"
                  [variant]="!editForm.locked ? 'default' : 'outline'"
                  (click)="editForm.locked = false"
                >
                  <lucide-icon [img]="UnlockIcon" [size]="14"></lucide-icon>
                  Déverrouillé
                </button>
                <button
                  hlmBtn size="sm" class="cursor-pointer gap-1.5"
                  [variant]="editForm.locked ? 'destructive' : 'outline'"
                  (click)="editForm.locked = true"
                >
                  <lucide-icon [img]="LockIcon" [size]="14"></lucide-icon>
                  Verrouillé
                </button>
              </div>
            </div>
          </div>

          <!-- Modal Footer -->
          <div class="flex items-center justify-between border-t border-(--border) px-6 py-4">
            <button
              hlmBtn variant="destructive" size="sm" class="cursor-pointer gap-1.5"
              (click)="hardDeleteUser()"
            >
              <lucide-icon [img]="AlertTriangleIcon" [size]="14"></lucide-icon>
              Supprimer définitivement
            </button>
            <div class="flex items-center gap-2">
            <button
              hlmBtn variant="outline" size="sm" class="cursor-pointer"
              (click)="closeEditModal()"
            >
              Annuler
            </button>
            <button
              hlmBtn variant="default" size="sm" class="cursor-pointer gap-2"
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
    }

    <!-- Toast -->
    @if (toast()) {
      <div
        class="fixed right-4 bottom-4 z-[200] flex items-center gap-2 rounded-lg border px-4 py-3 shadow-lg"
        [ngClass]="{
          'border-emerald-500/30 bg-emerald-500/10 text-emerald-500': toast()!.type === 'success',
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

  readonly loading = signal(false);
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
  editForm = { status: 'ACTIVE', locked: false, email: '' };

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
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
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
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
      status: user.status,
      locked: user.accountLocked ?? false,
      email: user.email,
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
        error: () => {
          this.showToast('error', 'Erreur lors de la mise à jour');
          this.saving.set(false);
        },
      });
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

  private showToast(type: 'success' | 'error', message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => this.toast.set(null), 3000);
  }
}
