import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule, MapPin, Plus, Pencil, Trash2, X, Loader2, Star,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { PortalService } from '../../core/services/portal.service';
import { Address, AddressType } from '../../shared/models/portal.models';

@Component({
  selector: 'lmp-user-addresses',
  standalone: true,
  imports: [NgClass, FormsModule, LucideAngularModule, HlmButton],
  template: `
    <div class="flex h-full flex-col overflow-hidden">
      <!-- Toolbar -->
      <div class="flex items-center justify-between gap-2 pb-4">
        <div></div>
        <button hlmBtn size="sm" type="button" class="h-8 cursor-pointer gap-1.5 px-2.5" (click)="openCreate()">
          <lucide-icon [img]="PlusIcon" [size]="14"></lucide-icon>
          <span class="text-sm">Nouvelle adresse</span>
        </button>
      </div>

      <!-- CRM-style list -->
      <div class="flex-1 overflow-auto">
        @if (loading()) {
          <div class="flex items-center justify-center py-16">
            <lucide-icon [img]="Loader2Icon" [size]="24" class="animate-spin text-zinc-400"></lucide-icon>
          </div>
        } @else {
          <div class="mb-2 flex min-w-max items-center rounded-lg bg-zinc-100 py-1.5 text-sm font-normal leading-none text-zinc-500 dark:bg-zinc-800/70 dark:text-zinc-400">
            <div class="w-48 shrink-0 px-2">Titre</div>
            <div class="w-28 shrink-0 px-2 text-center">Type</div>
            <div class="w-72 shrink-0 px-2">Adresse</div>
            <div class="w-32 shrink-0 px-2 text-center">Ville</div>
            <div class="w-28 shrink-0 px-2 text-center">Pays</div>
            <div class="w-36 shrink-0 px-2 text-center">Téléphone</div>
            <div class="w-24 shrink-0 px-2 text-center">Actions</div>
          </div>

          <div>
            @for (a of paginated(); track a.id) {
              <div
                class="group flex h-10 min-w-max cursor-pointer items-center border-b border-zinc-50 transition-colors hover:bg-zinc-50 dark:border-zinc-800/30 dark:hover:bg-zinc-900/50"
                (click)="openEdit(a)"
              >
                <div class="w-48 shrink-0 truncate px-2 text-sm leading-normal text-zinc-900 dark:text-zinc-100">
                  <span class="inline-flex items-center gap-1.5">
                    @if (a.isPrimaryAddress) {
                      <lucide-icon [img]="StarIcon" [size]="12" class="shrink-0 text-amber-500"></lucide-icon>
                    }
                    <span class="truncate">{{ a.addressTitle }}</span>
                  </span>
                </div>
                <div class="w-28 shrink-0 px-2 text-center">
                  <span class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium" [ngClass]="getTypeClass(a.addressType)">
                    {{ a.addressType }}
                  </span>
                </div>
                <div class="w-72 shrink-0 truncate px-2 text-sm leading-normal text-zinc-700 dark:text-zinc-300">
                  {{ a.addressLine1 }}@if (a.addressLine2) {, {{ a.addressLine2 }}}
                </div>
                <div class="w-32 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ a.pincode }} {{ a.city }}
                </div>
                <div class="w-28 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-600 dark:text-zinc-400">
                  {{ a.country }}
                </div>
                <div class="w-36 shrink-0 truncate px-2 text-center text-sm leading-normal text-zinc-500 dark:text-zinc-400">
                  {{ a.phone || '—' }}
                </div>
                <div class="w-24 shrink-0 px-2 text-center" (click)="$event.stopPropagation()">
                  <div class="flex items-center justify-center gap-0.5">
                    <button hlmBtn variant="ghost" size="icon" type="button" title="Modifier"
                      class="h-7 w-7 cursor-pointer text-zinc-500 hover:bg-zinc-100 hover:text-zinc-700 dark:hover:bg-zinc-800 dark:hover:text-zinc-300"
                      (click)="openEdit(a)">
                      <lucide-icon [img]="PencilIcon" [size]="13"></lucide-icon>
                    </button>
                    <button hlmBtn variant="ghost" size="icon" type="button" title="Supprimer"
                      class="h-7 w-7 cursor-pointer text-red-500 hover:bg-red-50 dark:hover:bg-red-950/40"
                      (click)="remove(a)">
                      <lucide-icon [img]="Trash2Icon" [size]="13"></lucide-icon>
                    </button>
                  </div>
                </div>
              </div>
            } @empty {
              <div class="py-12 text-center text-sm text-zinc-500 dark:text-zinc-400">
                Aucune adresse enregistrée. Ajoutez une adresse pour pré-remplir vos commandes.
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
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ items().length }}</span>
          <span>of</span>
          <span class="font-medium text-zinc-700 dark:text-zinc-300">{{ items().length }}</span>
        </div>
      </div>
    </div>

    <!-- Modal -->
    @if (showModal()) {
      <div class="fixed inset-0 z-[100] flex items-end justify-center bg-black/50 p-0 backdrop-blur-sm sm:items-center sm:p-4"
        (click)="closeModal()" role="dialog" aria-modal="true">
        <div class="flex max-h-[min(92dvh,760px)] w-full max-w-lg flex-col rounded-t-lg border border-(--border) bg-(--card) shadow-2xl sm:rounded-lg"
          (click)="$event.stopPropagation()">
          <div class="shrink-0 border-b border-(--border) px-5 py-4">
            <div class="flex items-start justify-between gap-3">
              <h3 class="text-base font-medium text-(--foreground)">
                {{ draft().id ? 'Modifier l\\'adresse' : 'Nouvelle adresse' }}
              </h3>
              <button hlmBtn variant="ghost" size="icon" class="h-8 w-8 shrink-0 cursor-pointer"
                type="button" (click)="closeModal()" aria-label="Fermer">
                <lucide-icon [img]="XIcon" [size]="18"></lucide-icon>
              </button>
            </div>
          </div>

          <form (submit)="save($event)" class="flex min-h-0 flex-1 flex-col">
            <div class="min-h-0 flex-1 space-y-3 overflow-y-auto px-5 py-4">
              <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <label class="block">
                  <span class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Titre *</span>
                  <input type="text" [(ngModel)]="draft().addressTitle" name="addressTitle" required
                    class="mt-1 h-9 w-full rounded-md border border-(--border) bg-(--background) px-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60" />
                </label>
                <label class="block">
                  <span class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Type *</span>
                  <select [(ngModel)]="draft().addressType" name="addressType" required
                    class="mt-1 h-9 w-full cursor-pointer rounded-md border border-(--border) bg-(--background) px-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60">
                    @for (t of TYPES; track t) { <option [value]="t">{{ t }}</option> }
                  </select>
                </label>
              </div>

              <label class="block">
                <span class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Ligne 1 *</span>
                <input type="text" [(ngModel)]="draft().addressLine1" name="addressLine1" required
                  class="mt-1 h-9 w-full rounded-md border border-(--border) bg-(--background) px-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60" />
              </label>
              <label class="block">
                <span class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Ligne 2</span>
                <input type="text" [ngModel]="draft().addressLine2 ?? ''"
                  (ngModelChange)="draft().addressLine2 = $event || null" name="addressLine2"
                  class="mt-1 h-9 w-full rounded-md border border-(--border) bg-(--background) px-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60" />
              </label>

              <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <label class="block">
                  <span class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Code postal *</span>
                  <input type="text" [(ngModel)]="draft().pincode" name="pincode" required
                    class="mt-1 h-9 w-full rounded-md border border-(--border) bg-(--background) px-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60" />
                </label>
                <label class="block">
                  <span class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Ville *</span>
                  <input type="text" [(ngModel)]="draft().city" name="city" required
                    class="mt-1 h-9 w-full rounded-md border border-(--border) bg-(--background) px-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60" />
                </label>
                <label class="block">
                  <span class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Région/État</span>
                  <input type="text" [(ngModel)]="draft().state" name="state"
                    class="mt-1 h-9 w-full rounded-md border border-(--border) bg-(--background) px-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60" />
                </label>
              </div>

              <label class="block">
                <span class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Pays *</span>
                <input type="text" [(ngModel)]="draft().country" name="country" required
                  class="mt-1 h-9 w-full rounded-md border border-(--border) bg-(--background) px-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60" />
              </label>

              <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <label class="block">
                  <span class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Téléphone</span>
                  <input type="tel" [ngModel]="draft().phone ?? ''"
                    (ngModelChange)="draft().phone = $event || null" name="phone"
                    class="mt-1 h-9 w-full rounded-md border border-(--border) bg-(--background) px-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60" />
                </label>
                <label class="block">
                  <span class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Email</span>
                  <input type="email" [ngModel]="draft().emailId ?? ''"
                    (ngModelChange)="draft().emailId = $event || null" name="emailId"
                    class="mt-1 h-9 w-full rounded-md border border-(--border) bg-(--background) px-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60" />
                </label>
              </div>

              <div class="flex items-center gap-4 pt-1">
                <label class="inline-flex cursor-pointer items-center gap-2 text-sm text-(--foreground)">
                  <input type="checkbox" [(ngModel)]="draft().isPrimaryAddress" name="isPrimaryAddress"
                    class="h-4 w-4 cursor-pointer accent-(--primary)" />
                  Adresse principale
                </label>
                <label class="inline-flex cursor-pointer items-center gap-2 text-sm text-(--foreground)">
                  <input type="checkbox" [(ngModel)]="draft().isShippingAddress" name="isShippingAddress"
                    class="h-4 w-4 cursor-pointer accent-(--primary)" />
                  Adresse de livraison
                </label>
              </div>
            </div>

            <div class="shrink-0 border-t border-(--border) px-5 py-3 flex justify-end gap-2">
              <button hlmBtn variant="outline" size="sm" type="button" class="cursor-pointer"
                (click)="closeModal()">Annuler</button>
              <button hlmBtn size="sm" type="submit" class="cursor-pointer gap-1.5" [disabled]="saving()">
                @if (saving()) {
                  <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                }
                Enregistrer
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
})
export class UserAddressesComponent implements OnInit {
  private readonly portal = inject(PortalService);

  readonly TYPES: AddressType[] = ['Billing', 'Shipping', 'Office', 'Personal', 'Other'];

  readonly MapPinIcon = MapPin;
  readonly PlusIcon = Plus;
  readonly PencilIcon = Pencil;
  readonly Trash2Icon = Trash2;
  readonly XIcon = X;
  readonly Loader2Icon = Loader2;
  readonly StarIcon = Star;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly items = signal<Address[]>([]);
  readonly showModal = signal(false);
  readonly draft = signal<Address>(this.blankAddress());

  readonly pageSize = signal(20);
  readonly currentPage = signal(0);
  readonly pageSizes = [20, 50, 100];

  readonly paginated = computed(() => {
    const start = this.currentPage() * this.pageSize();
    return this.items().slice(start, start + this.pageSize());
  });

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.portal.listAddresses().subscribe({
      next: (l) => { this.items.set(l); this.loading.set(false); this.currentPage.set(0); },
      error: () => this.loading.set(false),
    });
  }

  changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
  }

  openCreate() {
    this.draft.set(this.blankAddress());
    this.showModal.set(true);
  }

  openEdit(a: Address) {
    this.draft.set({ ...a });
    this.showModal.set(true);
  }

  closeModal() { this.showModal.set(false); }

  save(e: Event) {
    e.preventDefault();
    this.saving.set(true);
    const d = this.draft();
    const toSave: Address = d.id ? d : { ...d, id: `ADDR-${Date.now()}` };
    this.portal.saveAddress(toSave).subscribe({
      next: () => {
        this.saving.set(false);
        this.closeModal();
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  remove(a: Address) {
    if (!confirm(`Supprimer l'adresse « ${a.addressTitle} » ?`)) return;
    this.portal.deleteAddress(a.id).subscribe({ next: () => this.load() });
  }

  getTypeClass(type: AddressType): string {
    switch (type) {
      case 'Billing': return 'bg-blue-500/10 text-blue-500';
      case 'Shipping': return 'bg-green-500/10 text-green-500';
      case 'Office': return 'bg-orange-500/10 text-orange-500';
      case 'Personal': return 'bg-yellow-500/10 text-yellow-500';
      default: return 'bg-(--muted) text-(--foreground)';
    }
  }

  private blankAddress(): Address {
    return {
      id: '', addressTitle: '', addressType: 'Billing',
      addressLine1: '', addressLine2: null,
      city: '', state: '', pincode: '', country: 'Canada',
      phone: null, emailId: null,
      isPrimaryAddress: false, isShippingAddress: false,
      externalCrmId: null, syncStatus: 'PENDING', lastSyncedAt: null,
    };
  }
}
