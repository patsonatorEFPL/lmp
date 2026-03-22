import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, NgClass } from '@angular/common';
import {
  LucideAngularModule,
  Plus,
  Pencil,
  Trash2,
  X,
  Check,
  Loader2,
  RefreshCw,
  Package,
  FolderOpen,
  Star,
  Eye,
  EyeOff,
  ChevronDown,
  ChevronUp,
  Save,
  ToggleLeft,
  ToggleRight,
  ExternalLink,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import {
  AdminService,
  CategoryItem,
  CatalogStats,
} from '../../core/services/admin.service';
import { ServiceItem } from '../../core/services/catalog.service';

type ModalMode = 'create' | 'edit';

@Component({
  selector: 'lmp-admin-services',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, HlmButton, CurrencyPipe, NgClass],
  template: `
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-(--foreground)">
          Gestion des Services
        </h1>
        <p class="mt-1 text-sm text-(--muted-foreground)">
          Créez, modifiez et gérez votre catalogue de services.
        </p>
      </div>
      <div class="flex items-center gap-2">
        <button
          hlmBtn
          variant="ghost"
          size="icon"
          class="cursor-pointer"
          (click)="loadData()"
        >
          <lucide-icon
            [img]="RefreshCwIcon"
            [size]="18"
            [ngClass]="{ 'animate-spin': loading() }"
          ></lucide-icon>
        </button>
        <button
          hlmBtn
          variant="default"
          size="sm"
          class="cursor-pointer gap-2"
          (click)="openCreateService()"
        >
          <lucide-icon [img]="PlusIcon" [size]="16"></lucide-icon>
          Nouveau service
        </button>
      </div>
    </div>

    <!-- Stats -->
    @if (stats()) {
      <div class="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div
          class="rounded-lg border border-(--border) bg-(--card) px-4 py-3 text-center"
        >
          <p class="text-2xl font-bold text-(--foreground)">
            {{ stats()!.totalCategories }}
          </p>
          <p class="text-xs text-(--muted-foreground)">Catégories</p>
        </div>
        <div
          class="rounded-lg border border-(--border) bg-(--card) px-4 py-3 text-center"
        >
          <p class="text-2xl font-bold text-(--foreground)">
            {{ stats()!.totalServices }}
          </p>
          <p class="text-xs text-(--muted-foreground)">Services</p>
        </div>
        <div
          class="rounded-lg border border-(--border) bg-(--card) px-4 py-3 text-center"
        >
          <p class="text-2xl font-bold text-emerald-500">
            {{ stats()!.activeServices }}
          </p>
          <p class="text-xs text-(--muted-foreground)">Actifs</p>
        </div>
        <div
          class="rounded-lg border border-(--border) bg-(--card) px-4 py-3 text-center"
        >
          <p class="text-2xl font-bold text-amber-500">
            {{ stats()!.featuredServices }}
          </p>
          <p class="text-xs text-(--muted-foreground)">En vedette</p>
        </div>
      </div>
    }

    <!-- Loading -->
    @if (loading()) {
      <div class="mt-8 flex items-center justify-center py-16">
        <lucide-icon
          [img]="Loader2Icon"
          [size]="32"
          class="animate-spin text-(--primary)"
        ></lucide-icon>
      </div>
    }

    <!-- Categories + Services list -->
    @if (!loading()) {
      <!-- Categories management -->
      <div class="mt-8">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold text-(--foreground)">
            Catégories
          </h2>
          <button
            hlmBtn
            variant="outline"
            size="sm"
            class="cursor-pointer gap-2"
            (click)="openCreateCategory()"
          >
            <lucide-icon [img]="PlusIcon" [size]="14"></lucide-icon>
            Catégorie
          </button>
        </div>
        <div
          class="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3"
        >
          @for (cat of categories(); track cat.id) {
            <div
              class="flex items-center justify-between rounded-lg border border-(--border) bg-(--card) px-4 py-3"
            >
              <div class="flex items-center gap-2">
                <span class="text-lg">{{ cat.icon }}</span>
                <span class="text-sm font-medium text-(--foreground)">{{
                  cat.name
                }}</span>
              </div>
              <div class="flex items-center gap-1">
                <button
                  hlmBtn
                  variant="ghost"
                  size="icon"
                  class="h-7 w-7 cursor-pointer"
                  (click)="openEditCategory(cat)"
                >
                  <lucide-icon
                    [img]="PencilIcon"
                    [size]="14"
                    class="text-(--muted-foreground)"
                  ></lucide-icon>
                </button>
                <button
                  hlmBtn
                  variant="ghost"
                  size="icon"
                  class="h-7 w-7 cursor-pointer"
                  (click)="onDeleteCategory(cat)"
                >
                  <lucide-icon
                    [img]="Trash2Icon"
                    [size]="14"
                    class="text-(--destructive)"
                  ></lucide-icon>
                </button>
              </div>
            </div>
          }
        </div>
      </div>

      <!-- Duplicate order warning -->
      @if (hasDuplicateOrders()) {
        <div class="mt-6 flex items-center gap-3 rounded-lg border border-amber-500/30 bg-amber-500/5 px-4 py-3">
          <span class="text-lg">⚠️</span>
          <div>
            <p class="text-sm font-medium text-amber-500">Ordres d'affichage en doublon détectés</p>
            <p class="text-xs text-(--muted-foreground)">Plusieurs services partagent la même valeur d'ordre. Utilisez les flèches pour réordonner.</p>
          </div>
        </div>
      }

      <!-- Services list -->
      <div class="mt-8">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold text-(--foreground)">
            Services ({{ services().length }})
          </h2>
          <span class="text-xs text-(--muted-foreground)">Triés par ordre d'affichage</span>
        </div>
        <div class="mt-3 space-y-3">
          @for (service of services(); track service.id; let i = $index; let first = $first; let last = $last) {
            <div
              class="rounded-sm border bg-(--card) p-5 transition-all hover:border-(--primary)/20"
              [ngClass]="isDuplicateOrder(service) ? 'border-amber-500/40' : 'border-(--border)'"
            >
              <div class="flex items-start justify-between">
                <div class="flex items-start gap-4">
                  <div
                    class="flex h-12 w-12 shrink-0 items-center justify-center rounded-sm bg-(--primary)/10 text-xl"
                  >
                    {{ service.icon }}
                  </div>
                  <div>
                    <div class="flex items-center gap-2">
                      <span
                        class="flex h-6 w-6 shrink-0 items-center justify-center rounded-md text-[10px] font-bold"
                        [ngClass]="isDuplicateOrder(service)
                          ? 'bg-amber-500/20 text-amber-500'
                          : 'bg-(--primary)/10 text-(--primary)'"
                      >
                        {{ service.displayOrder }}
                      </span>
                      <h3
                        class="text-base font-bold text-(--foreground)"
                      >
                        {{ service.title }}
                      </h3>
                      @if (service.featured) {
                        <span
                          class="inline-flex items-center gap-1 rounded-full bg-amber-500/10 px-2 py-0.5 text-[10px] font-semibold text-amber-500"
                        >
                          <lucide-icon
                            [img]="StarIcon"
                            [size]="10"
                          ></lucide-icon>
                          Vedette
                        </span>
                      }
                      @if (service.active) {
                        <span
                          class="rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-500"
                          >Actif</span
                        >
                      } @else {
                        <span
                          class="rounded-full bg-red-500/10 px-2 py-0.5 text-[10px] font-semibold text-red-500"
                          >Inactif</span
                        >
                      }
                    </div>
                    <p
                      class="mt-1 line-clamp-2 max-w-xl text-sm text-(--muted-foreground)"
                    >
                      {{ service.description }}
                    </p>
                    <div class="mt-2 flex items-center gap-3">
                      <span
                        class="rounded bg-(--primary)/10 px-2 py-0.5 text-xs text-(--primary)"
                      >
                        {{ service.categoryName }}
                      </span>
                      @if (service.currentOffer) {
                        <span class="text-sm font-semibold text-(--primary)">
                          {{
                            service.currentOffer.price
                              | currency: 'EUR' : 'symbol' : '1.2-2'
                          }}
                        </span>
                      }
                      <span class="text-xs text-(--muted-foreground)">
                        {{ service.benefits.length }} avantage(s)
                      </span>
                    </div>
                  </div>
                </div>
                <div class="flex shrink-0 items-center gap-1">
                  <!-- Move up -->
                  <button
                    hlmBtn
                    variant="ghost"
                    size="icon"
                    class="h-8 w-8 cursor-pointer"
                    title="Monter"
                    [disabled]="first"
                    (click)="moveService(i, 'up')"
                  >
                    <lucide-icon
                      [img]="ChevronUpIcon"
                      [size]="16"
                      class="text-(--muted-foreground)"
                    ></lucide-icon>
                  </button>
                  <!-- Move down -->
                  <button
                    hlmBtn
                    variant="ghost"
                    size="icon"
                    class="h-8 w-8 cursor-pointer"
                    title="Descendre"
                    [disabled]="last"
                    (click)="moveService(i, 'down')"
                  >
                    <lucide-icon
                      [img]="ChevronDownIcon"
                      [size]="16"
                      class="text-(--muted-foreground)"
                    ></lucide-icon>
                  </button>
                  <div class="mx-1 h-5 w-px bg-(--border)"></div>
                  <button
                    hlmBtn
                    variant="ghost"
                    size="icon"
                    class="h-8 w-8 cursor-pointer"
                    title="Prévisualiser"
                    (click)="openPreview(service)"
                  >
                    <lucide-icon
                      [img]="EyeIcon"
                      [size]="16"
                      class="text-emerald-500"
                    ></lucide-icon>
                  </button>
                  <button
                    hlmBtn
                    variant="ghost"
                    size="icon"
                    class="h-8 w-8 cursor-pointer"
                    title="Activer / Désactiver"
                    (click)="toggleServiceActive(service)"
                  >
                    @if (service.active) {
                      <lucide-icon
                        [img]="ToggleRightIcon"
                        [size]="16"
                        class="text-emerald-500"
                      ></lucide-icon>
                    } @else {
                      <lucide-icon
                        [img]="ToggleLeftIcon"
                        [size]="16"
                        class="text-(--muted-foreground)"
                      ></lucide-icon>
                    }
                  </button>
                  <button
                    hlmBtn
                    variant="ghost"
                    size="icon"
                    class="h-8 w-8 cursor-pointer"
                    title="Modifier"
                    (click)="openEditService(service)"
                  >
                    <lucide-icon
                      [img]="PencilIcon"
                      [size]="16"
                      class="text-(--muted-foreground)"
                    ></lucide-icon>
                  </button>
                  <button
                    hlmBtn
                    variant="ghost"
                    size="icon"
                    class="h-8 w-8 cursor-pointer"
                    title="Supprimer"
                    (click)="onDeleteService(service)"
                  >
                    <lucide-icon
                      [img]="Trash2Icon"
                      [size]="16"
                      class="text-(--destructive)"
                    ></lucide-icon>
                  </button>
                </div>
              </div>
            </div>
          }
        </div>
      </div>
    }

    <!-- Service Modal (Create/Edit) -->
    @if (showServiceModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        (click)="closeServiceModal()"
      >
        <div
          class="mx-4 w-full max-w-lg rounded-2xl border border-(--border) bg-(--card) shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          <!-- Modal Header -->
          <div
            class="flex items-center justify-between border-b border-(--border) px-6 py-4"
          >
            <h3 class="text-lg font-bold text-(--foreground)">
              {{
                serviceModalMode() === 'create'
                  ? 'Nouveau service'
                  : 'Modifier le service'
              }}
            </h3>
            <button
              hlmBtn
              variant="ghost"
              size="icon"
              class="h-8 w-8 cursor-pointer"
              (click)="closeServiceModal()"
            >
              <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
            </button>
          </div>

          <!-- Modal Body -->
          <div class="max-h-[70vh] space-y-4 overflow-y-auto px-6 py-4">
            <!-- Title -->
            <div>
              <label
                class="mb-1 block text-sm font-medium text-(--foreground)"
                >Titre *</label
              >
              <input
                type="text"
                [(ngModel)]="serviceForm.title"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) focus:ring-1 focus:ring-(--primary)"
                placeholder="Nom du service"
              />
            </div>

            <!-- Category -->
            <div>
              <label
                class="mb-1 block text-sm font-medium text-(--foreground)"
                >Catégorie *</label
              >
              <select
                [(ngModel)]="serviceForm.categoryId"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
              >
                <option value="">Sélectionner...</option>
                @for (cat of categories(); track cat.id) {
                  <option [value]="cat.id">{{ cat.icon }} {{ cat.name }}</option>
                }
              </select>
            </div>

            <!-- Description -->
            <div>
              <label
                class="mb-1 block text-sm font-medium text-(--foreground)"
                >Description</label
              >
              <textarea
                [(ngModel)]="serviceForm.description"
                rows="3"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) focus:ring-1 focus:ring-(--primary)"
                placeholder="Description du service..."
              ></textarea>
            </div>

            <!-- Icon + Order -->
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label
                  class="mb-1 block text-sm font-medium text-(--foreground)"
                  >Icône (emoji)</label
                >
                <input
                  type="text"
                  [(ngModel)]="serviceForm.icon"
                  class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                  placeholder="📦"
                />
              </div>
              <div>
                <label
                  class="mb-1 block text-sm font-medium text-(--foreground)"
                  >Ordre d'affichage</label
                >
                <input
                  type="number"
                  [(ngModel)]="serviceForm.displayOrder"
                  class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                />
              </div>
            </div>

            <!-- Toggles -->
            <div class="flex items-center gap-6">
              <label class="flex cursor-pointer items-center gap-2">
                <input
                  type="checkbox"
                  [(ngModel)]="serviceForm.active"
                  class="h-4 w-4 rounded border-(--border) accent-(--primary)"
                />
                <span class="text-sm text-(--foreground)">Actif</span>
              </label>
              <label class="flex cursor-pointer items-center gap-2">
                <input
                  type="checkbox"
                  [(ngModel)]="serviceForm.featured"
                  class="h-4 w-4 rounded border-(--border) accent-(--primary)"
                />
                <span class="text-sm text-(--foreground)">En vedette</span>
              </label>
            </div>

            <!-- Benefits -->
            <div>
              <label
                class="mb-1 block text-sm font-medium text-(--foreground)"
                >Avantages (un par ligne)</label
              >
              <textarea
                [(ngModel)]="serviceForm.benefitsText"
                rows="4"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary) focus:ring-1 focus:ring-(--primary)"
                placeholder="Un avantage par ligne..."
              ></textarea>
            </div>
          </div>

          <!-- Modal Footer -->
          <div
            class="flex items-center justify-end gap-2 border-t border-(--border) px-6 py-4"
          >
            <button
              hlmBtn
              variant="outline"
              size="sm"
              class="cursor-pointer"
              (click)="closeServiceModal()"
            >
              Annuler
            </button>
            <button
              hlmBtn
              variant="default"
              size="sm"
              class="cursor-pointer gap-2"
              [disabled]="saving()"
              (click)="saveService()"
            >
              @if (saving()) {
                <lucide-icon
                  [img]="Loader2Icon"
                  [size]="14"
                  class="animate-spin"
                ></lucide-icon>
              } @else {
                <lucide-icon [img]="SaveIcon" [size]="14"></lucide-icon>
              }
              {{ serviceModalMode() === 'create' ? 'Créer' : 'Enregistrer' }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- Category Modal (Create/Edit) -->
    @if (showCategoryModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
        (click)="closeCategoryModal()"
      >
        <div
          class="mx-4 w-full max-w-md rounded-2xl border border-(--border) bg-(--card) shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          <div
            class="flex items-center justify-between border-b border-(--border) px-6 py-4"
          >
            <h3 class="text-lg font-bold text-(--foreground)">
              {{
                categoryModalMode() === 'create'
                  ? 'Nouvelle catégorie'
                  : 'Modifier la catégorie'
              }}
            </h3>
            <button
              hlmBtn
              variant="ghost"
              size="icon"
              class="h-8 w-8 cursor-pointer"
              (click)="closeCategoryModal()"
            >
              <lucide-icon [img]="XIcon" [size]="16"></lucide-icon>
            </button>
          </div>
          <div class="space-y-4 px-6 py-4">
            <div>
              <label
                class="mb-1 block text-sm font-medium text-(--foreground)"
                >Nom *</label
              >
              <input
                type="text"
                [(ngModel)]="categoryForm.name"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="Nom de la catégorie"
              />
            </div>
            <div>
              <label
                class="mb-1 block text-sm font-medium text-(--foreground)"
                >Description</label
              >
              <input
                type="text"
                [(ngModel)]="categoryForm.description"
                class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                placeholder="Description"
              />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label
                  class="mb-1 block text-sm font-medium text-(--foreground)"
                  >Icône (emoji)</label
                >
                <input
                  type="text"
                  [(ngModel)]="categoryForm.icon"
                  class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                  placeholder="📁"
                />
              </div>
              <div>
                <label
                  class="mb-1 block text-sm font-medium text-(--foreground)"
                  >Ordre</label
                >
                <input
                  type="number"
                  [(ngModel)]="categoryForm.displayOrder"
                  class="w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2 text-sm text-(--foreground) outline-none focus:border-(--primary)"
                />
              </div>
            </div>
          </div>
          <div
            class="flex items-center justify-end gap-2 border-t border-(--border) px-6 py-4"
          >
            <button
              hlmBtn
              variant="outline"
              size="sm"
              class="cursor-pointer"
              (click)="closeCategoryModal()"
            >
              Annuler
            </button>
            <button
              hlmBtn
              variant="default"
              size="sm"
              class="cursor-pointer gap-2"
              [disabled]="saving()"
              (click)="saveCategory()"
            >
              @if (saving()) {
                <lucide-icon
                  [img]="Loader2Icon"
                  [size]="14"
                  class="animate-spin"
                ></lucide-icon>
              } @else {
                <lucide-icon [img]="SaveIcon" [size]="14"></lucide-icon>
              }
              {{ categoryModalMode() === 'create' ? 'Créer' : 'Enregistrer' }}
            </button>
          </div>
        </div>
      </div>
    }

    <!-- Preview Modal -->
    @if (showPreviewModal()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm"
        (click)="closePreview()"
      >
        <div
          class="relative mx-4 w-full max-w-md"
          (click)="$event.stopPropagation()"
        >
          <!-- Close button -->
          <button
            hlmBtn
            variant="ghost"
            size="icon"
            class="absolute -top-12 right-0 h-8 w-8 cursor-pointer text-white/70 hover:text-white"
            (click)="closePreview()"
          >
            <lucide-icon [img]="XIcon" [size]="18"></lucide-icon>
          </button>

          <!-- Service card preview (mirrors public services page) -->
          @if (previewService()) {
            <div
              class="group flex flex-col rounded-sm border border-(--border) bg-(--card) p-6 shadow-2xl"
            >
              <!-- Icon + Category -->
              <div class="mb-4 flex items-start justify-between">
                <div
                  class="flex h-12 w-12 items-center justify-center rounded-sm bg-blue-500/10 text-xl"
                >
                  {{ previewService()!.icon }}
                </div>
                <span
                  class="rounded-full bg-blue-500/10 px-3 py-1 text-xs font-medium text-blue-400"
                >
                  {{ previewService()!.categoryName }}
                </span>
              </div>

              <!-- Title -->
              <h3
                class="text-lg font-bold leading-snug text-(--foreground)"
              >
                {{ previewService()!.title }}
              </h3>

              <!-- Description -->
              <p
                class="mt-3 text-sm leading-relaxed text-(--muted-foreground)"
              >
                {{ previewService()!.description }}
              </p>

              <!-- Benefits -->
              <ul class="mt-4 flex-1 space-y-2">
                @for (
                  benefit of previewService()!.benefits;
                  track benefit
                ) {
                  <li
                    class="flex items-start gap-2 text-sm text-(--muted-foreground)"
                  >
                    <lucide-icon
                      [img]="CheckIcon"
                      [size]="14"
                      class="mt-0.5 shrink-0 text-emerald-500"
                    ></lucide-icon>
                    {{ benefit }}
                  </li>
                }
              </ul>

              <!-- Price + CTA -->
              <div
                class="mt-6 flex items-end justify-between border-t border-(--border) pt-4"
              >
                <div>
                  @if (previewService()!.currentOffer) {
                    <span
                      class="text-2xl font-bold text-(--primary)"
                    >
                      {{
                        previewService()!.currentOffer!.price
                          | currency: 'EUR' : 'symbol' : '1.2-2' : 'fr'
                      }}
                    </span>
                    @if (
                      previewService()!.currentOffer!.originalPrice &&
                      previewService()!.currentOffer!.originalPrice! >
                        previewService()!.currentOffer!.price
                    ) {
                      <span
                        class="ml-2 text-sm text-(--muted-foreground) line-through"
                      >
                        {{
                          previewService()!.currentOffer!.originalPrice
                            | currency: 'EUR' : 'symbol' : '1.2-2' : 'fr'
                        }}
                      </span>
                    }
                    <div class="text-xs text-(--muted-foreground)">
                      {{
                        previewService()!.currentOffer!.durationType ===
                        'ONE_TIME'
                          ? 'Paiement unique'
                          : previewService()!.currentOffer!.durationType ===
                              'MONTHLY'
                            ? '/ mois'
                            : previewService()!.currentOffer!.durationType ===
                                'YEARLY'
                              ? '/ an'
                              : 'Paiement unique'
                      }}
                    </div>
                  } @else {
                    <span
                      class="text-lg font-bold text-(--primary)"
                      >Sur devis</span
                    >
                  }
                </div>
                <div
                  class="flex h-9 items-center gap-1.5 rounded-lg bg-(--primary)/10 px-3 text-sm font-medium text-(--primary)"
                >
                  Commander
                  <lucide-icon
                    [img]="ExternalLinkIcon"
                    [size]="14"
                  ></lucide-icon>
                </div>
              </div>
            </div>

            <!-- Preview label -->
            <p
              class="mt-3 text-center text-xs text-white/50"
            >
              Aperçu tel qu'affiché sur la page Services
            </p>
          }
        </div>
      </div>
    }

    <!-- Toast -->
    @if (toast()) {
      <div
        class="fixed right-4 bottom-4 z-[200] flex items-center gap-2 rounded-lg border px-4 py-3 shadow-lg"
        [ngClass]="{
          'border-emerald-500/30 bg-emerald-500/10 text-emerald-500':
            toast()!.type === 'success',
          'border-red-500/30 bg-red-500/10 text-red-500':
            toast()!.type === 'error',
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
export class AdminServicesComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly services = signal<ServiceItem[]>([]);
  readonly categories = signal<CategoryItem[]>([]);
  readonly stats = signal<CatalogStats | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly toast = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  // Preview modal
  readonly showPreviewModal = signal(false);
  readonly previewService = signal<ServiceItem | null>(null);

  // Service modal
  readonly showServiceModal = signal(false);
  readonly serviceModalMode = signal<ModalMode>('create');
  serviceForm = this.emptyServiceForm();
  editingServiceId: string | null = null;

  // Category modal
  readonly showCategoryModal = signal(false);
  readonly categoryModalMode = signal<ModalMode>('create');
  categoryForm = this.emptyCategoryForm();
  editingCategoryId: string | null = null;

  // Icons
  readonly PlusIcon = Plus;
  readonly PencilIcon = Pencil;
  readonly Trash2Icon = Trash2;
  readonly XIcon = X;
  readonly CheckIcon = Check;
  readonly Loader2Icon = Loader2;
  readonly RefreshCwIcon = RefreshCw;
  readonly PackageIcon = Package;
  readonly FolderIcon = FolderOpen;
  readonly StarIcon = Star;
  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;
  readonly ToggleLeftIcon = ToggleLeft;
  readonly ToggleRightIcon = ToggleRight;
  readonly ExternalLinkIcon = ExternalLink;
  readonly ChevronDownIcon = ChevronDown;
  readonly ChevronUpIcon = ChevronUp;
  readonly SaveIcon = Save;

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.adminService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
    });
    this.adminService.getServices().subscribe({
      next: (svcs) => {
        this.services.set(svcs);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.adminService.getCatalogStats().subscribe({
      next: (s) => this.stats.set(s),
    });
  }

  // ========== Preview ==========

  openPreview(service: ServiceItem): void {
    this.previewService.set(service);
    this.showPreviewModal.set(true);
  }

  closePreview(): void {
    this.showPreviewModal.set(false);
  }

  // ========== Service CRUD ==========

  openCreateService(): void {
    this.serviceForm = this.emptyServiceForm();
    this.editingServiceId = null;
    this.serviceModalMode.set('create');
    this.showServiceModal.set(true);
  }

  openEditService(service: ServiceItem): void {
    this.editingServiceId = service.id;
    this.serviceModalMode.set('edit');

    // Load full service details to get categoryId before showing modal
    this.adminService.getService(service.id).subscribe({
      next: (data) => {
        this.serviceForm = {
          title: service.title,
          description: service.description,
          icon: service.icon,
          categoryId: data?.categoryId ? String(data.categoryId) : '',
          displayOrder: service.displayOrder,
          active: service.active,
          featured: service.featured,
          benefitsText: service.benefits.join('\n'),
        };
        this.showServiceModal.set(true);
      },
      error: () => {
        // Fallback: open modal without categoryId
        this.serviceForm = {
          title: service.title,
          description: service.description,
          icon: service.icon,
          categoryId: '',
          displayOrder: service.displayOrder,
          active: service.active,
          featured: service.featured,
          benefitsText: service.benefits.join('\n'),
        };
        this.showServiceModal.set(true);
      },
    });
  }

  closeServiceModal(): void {
    this.showServiceModal.set(false);
  }

  saveService(): void {
    if (!this.serviceForm.title || !this.serviceForm.categoryId) {
      this.showToast('error', 'Titre et catégorie requis');
      return;
    }

    this.saving.set(true);
    const benefits = this.serviceForm.benefitsText
      .split('\n')
      .map((b) => b.trim())
      .filter((b) => b.length > 0);

    if (this.serviceModalMode() === 'create') {
      this.adminService
        .createService({
          ...this.serviceForm,
          benefits,
        })
        .subscribe({
          next: () => {
            this.showToast('success', 'Service créé avec succès');
            this.closeServiceModal();
            this.saving.set(false);
            this.loadData();
          },
          error: () => {
            this.showToast('error', 'Erreur lors de la création');
            this.saving.set(false);
          },
        });
    } else {
      const updatePayload = {
        title: this.serviceForm.title,
        description: this.serviceForm.description,
        icon: this.serviceForm.icon,
        categoryId: this.serviceForm.categoryId,
        displayOrder: this.serviceForm.displayOrder,
        active: this.serviceForm.active,
        featured: this.serviceForm.featured,
      };
      this.adminService
        .updateService(this.editingServiceId!, updatePayload)
        .subscribe({
          next: () => {
            // Sync benefits separately
            this.adminService
              .syncBenefits(this.editingServiceId!, benefits)
              .subscribe({
                next: () => {
                  this.showToast('success', 'Service mis à jour');
                  this.closeServiceModal();
                  this.saving.set(false);
                  this.loadData();
                },
                error: () => {
                  this.showToast('success', 'Service mis à jour (avantages non synchronisés)');
                  this.closeServiceModal();
                  this.saving.set(false);
                  this.loadData();
                },
              });
          },
          error: () => {
            this.showToast('error', 'Erreur lors de la mise à jour');
            this.saving.set(false);
          },
        });
    }
  }

  onDeleteService(service: ServiceItem): void {
    if (!confirm(`Supprimer le service "${service.title}" ?`)) return;
    this.adminService.deleteService(service.id).subscribe({
      next: () => {
        this.showToast('success', 'Service supprimé');
        this.loadData();
      },
      error: () => this.showToast('error', 'Impossible de supprimer'),
    });
  }

  toggleServiceActive(service: ServiceItem): void {
    this.adminService
      .updateService(service.id, { active: !service.active })
      .subscribe({
        next: () => {
          this.showToast(
            'success',
            service.active ? 'Service désactivé' : 'Service activé',
          );
          this.loadData();
        },
      });
  }

  // ========== Category CRUD ==========

  openCreateCategory(): void {
    this.categoryForm = this.emptyCategoryForm();
    this.editingCategoryId = null;
    this.categoryModalMode.set('create');
    this.showCategoryModal.set(true);
  }

  openEditCategory(cat: CategoryItem): void {
    this.categoryForm = {
      name: cat.name,
      description: cat.description || '',
      icon: cat.icon || '📁',
      displayOrder: cat.displayOrder,
    };
    this.editingCategoryId = cat.id;
    this.categoryModalMode.set('edit');
    this.showCategoryModal.set(true);
  }

  closeCategoryModal(): void {
    this.showCategoryModal.set(false);
  }

  saveCategory(): void {
    if (!this.categoryForm.name) {
      this.showToast('error', 'Nom requis');
      return;
    }

    this.saving.set(true);

    if (this.categoryModalMode() === 'create') {
      this.adminService.createCategory(this.categoryForm).subscribe({
        next: () => {
          this.showToast('success', 'Catégorie créée');
          this.closeCategoryModal();
          this.saving.set(false);
          this.loadData();
        },
        error: () => {
          this.showToast('error', 'Erreur lors de la création');
          this.saving.set(false);
        },
      });
    } else {
      this.adminService
        .updateCategory(this.editingCategoryId!, this.categoryForm)
        .subscribe({
          next: () => {
            this.showToast('success', 'Catégorie mise à jour');
            this.closeCategoryModal();
            this.saving.set(false);
            this.loadData();
          },
          error: () => {
            this.showToast('error', 'Erreur lors de la mise à jour');
            this.saving.set(false);
          },
        });
    }
  }

  onDeleteCategory(cat: CategoryItem): void {
    if (!confirm(`Supprimer la catégorie "${cat.name}" ?`)) return;
    this.adminService.deleteCategory(cat.id).subscribe({
      next: () => {
        this.showToast('success', 'Catégorie supprimée');
        this.loadData();
      },
      error: () =>
        this.showToast('error', 'Impossible de supprimer (services liés ?)'),
    });
  }

  // ========== Reorder ==========

  hasDuplicateOrders(): boolean {
    const orders = this.services().map((s) => s.displayOrder);
    return new Set(orders).size !== orders.length;
  }

  isDuplicateOrder(service: ServiceItem): boolean {
    return (
      this.services().filter((s) => s.displayOrder === service.displayOrder)
        .length > 1
    );
  }

  moveService(index: number, direction: 'up' | 'down'): void {
    const list = [...this.services()];
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= list.length) return;

    // Swap
    [list[index], list[targetIndex]] = [list[targetIndex], list[index]];
    this.services.set(list);

    // Persist new order via bulk reorder API
    const serviceIds = list.map((s) => s.id);
    this.adminService.reorderServices(serviceIds).subscribe({
      next: () => {
        this.showToast('success', 'Ordre mis à jour');
        this.loadData();
      },
      error: () => this.showToast('error', "Erreur lors de la mise à jour de l'ordre"),
    });
  }

  // ========== Helpers ==========

  private emptyServiceForm() {
    return {
      title: '',
      description: '',
      icon: '📦',
      categoryId: '',
      displayOrder: 0,
      active: true,
      featured: false,
      benefitsText: '',
    };
  }

  private emptyCategoryForm() {
    return {
      name: '',
      description: '',
      icon: '📁',
      displayOrder: 0,
    };
  }

  private showToast(type: 'success' | 'error', message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => this.toast.set(null), 3000);
  }
}
