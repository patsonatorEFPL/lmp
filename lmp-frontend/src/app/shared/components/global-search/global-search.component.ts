import { CommonModule } from '@angular/common';
import {
  Component,
  HostListener,
  OnDestroy,
  OnInit,
  signal,
  computed,
  ViewChild,
  ElementRef,
  inject,
} from '@angular/core';
import { Router } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, switchMap, tap } from 'rxjs/operators';
import { LucideAngularModule, Search, X } from 'lucide-angular';

import {
  SearchService,
  GlobalSearchResult,
} from '../../services/search.service';

type Row =
  | { kind: 'service'; slug: string; title: string; description: string }
  | { kind: 'blog'; slug: string; title: string; excerpt: string };

/**
 * Global header search dialog.
 *
 * <p>Surface: product register. Trigger button always visible in the navbar
 * actions cluster (composed via <lmp-global-search />). Cmd/Ctrl+K opens it
 * from anywhere; Esc closes. The dialog grabs focus on open so the input
 * is type-ready immediately.</p>
 *
 * <p>Why a modal rather than an inline dropdown: the search has to work
 * from any route on the SPA, including the dashboard and the marketing
 * pages, and the trigger lives inside a transparent fixed header. An
 * inline expansion would either cover the page underneath (same as a
 * modal but more awkward) or collide with the header background switch
 * on scroll. Centered dialog is the boring, correct answer.</p>
 *
 * <p>Backdrop = subtle dim, not glass — see PRODUCT.md anti-references
 * ("Animations spectaculaires sans substance"). Motion is the 150ms
 * fade+scale enter that signals "this came from somewhere"; nothing else.</p>
 */
@Component({
  selector: 'lmp-global-search',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  styles: `
    :host {
      display: contents;
    }

    /* Enter animation respects prefers-reduced-motion (.97 → 1 scale, 0 → 1 opacity, 150ms ease-out-quart). */
    @keyframes lmp-global-search-enter {
      from {
        opacity: 0;
        transform: translateY(-4px) scale(0.985);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }
    .lmp-dialog-panel {
      animation: lmp-global-search-enter 150ms cubic-bezier(0.165, 0.84, 0.44, 1);
    }
    @media (prefers-reduced-motion: reduce) {
      .lmp-dialog-panel {
        animation: none;
      }
    }
  `,
  template: `
    <!-- Trigger button: always rendered in the navbar slot. -->
    <button
      type="button"
      (click)="open()"
      class="inline-flex h-9 w-9 items-center justify-center rounded-md text-(--muted-foreground) transition-colors hover:bg-(--accent) hover:text-(--foreground) focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-(--ring)"
      aria-label="Rechercher (Ctrl+K)"
      title="Rechercher (Ctrl+K)"
    >
      <lucide-angular [img]="iconSearch" class="h-4 w-4" aria-hidden="true" />
    </button>

    @if (isOpen()) {
      <!-- Backdrop: dim, not glass. Click closes. -->
      <div
        class="fixed inset-0 z-[1300] bg-(--background)/70 backdrop-blur-[2px]"
        (click)="close()"
        aria-hidden="true"
      ></div>

      <!-- Dialog: top-anchored at 12vh so input lives near the top of the eye, results flow below. -->
      <div
        class="fixed inset-x-0 top-[12vh] z-[1301] mx-auto flex max-w-2xl flex-col gap-0 px-4"
        role="dialog"
        aria-modal="true"
        aria-label="Recherche globale"
        (click)="$event.stopPropagation()"
      >
        <div
          class="lmp-dialog-panel overflow-hidden rounded-lg border border-(--border) bg-(--background) shadow-lg"
        >
          <!-- Input row: large, ghost border-bottom only. Close button on right. -->
          <div class="flex items-center gap-3 border-b border-(--border) px-4 py-3">
            <lucide-angular
              [img]="iconSearch"
              class="h-4 w-4 shrink-0 text-(--muted-foreground)"
              aria-hidden="true"
            />
            <input
              #searchInput
              type="text"
              [value]="query()"
              (input)="onQueryChange($any($event.target).value)"
              placeholder="Rechercher services, articles..."
              class="w-full bg-transparent text-base text-(--foreground) placeholder:text-(--muted-foreground) focus:outline-none"
              autocomplete="off"
              spellcheck="false"
              aria-label="Champ de recherche"
            />
            <button
              type="button"
              (click)="close()"
              class="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded text-(--muted-foreground) hover:bg-(--accent) hover:text-(--foreground) focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-(--ring)"
              aria-label="Fermer"
            >
              <lucide-angular [img]="iconClose" class="h-4 w-4" aria-hidden="true" />
            </button>
          </div>

          <!-- Body: loading / results / suggestions / empty. Capped height so long result sets scroll inside the modal. -->
          <div class="max-h-[60vh] overflow-y-auto">
            @if (query().length < 2) {
              <div class="px-4 py-6 text-sm text-(--muted-foreground)">
                Tapez au moins 2 caractères pour rechercher.
                <span class="ml-2 inline-block rounded border border-(--border) px-1.5 py-0.5 font-mono text-xs">
                  Esc
                </span>
                pour fermer.
              </div>
            } @else if (loading()) {
              <div class="px-4 py-6 text-sm text-(--muted-foreground)">Recherche en cours...</div>
            } @else if (allRows().length > 0) {
              @let svc = result()?.services ?? [];
              @if (svc.length > 0) {
                <div class="px-4 py-2 text-xs font-medium uppercase tracking-wider text-(--muted-foreground)">
                  Services
                </div>
                @for (s of svc; track s.slug; let i = $index) {
                  <a
                    [attr.href]="'/services/' + s.slug"
                    (click)="onRowClick($event, 'service', s.slug ?? '')"
                    class="flex items-start gap-3 px-4 py-2.5 transition-colors"
                    [class.bg-\\[--accent\\]]="activeIndex() === i"
                  >
                    @if (s.icon) {
                      <span class="text-lg leading-none">{{ s.icon }}</span>
                    }
                    <span class="flex min-w-0 flex-col">
                      <span class="truncate text-sm font-medium text-(--foreground)">{{ s.title }}</span>
                      <span class="truncate text-xs text-(--muted-foreground)">{{ s.description }}</span>
                    </span>
                  </a>
                }
              }
              @let blogRows = result()?.blog ?? [];
              @if (blogRows.length > 0) {
                <div
                  class="px-4 py-2 text-xs font-medium uppercase tracking-wider text-(--muted-foreground)"
                  [class.border-t]="svc.length > 0"
                  [class.border-\\[--border\\]]="svc.length > 0"
                  [class.mt-1]="svc.length > 0"
                >
                  Articles
                </div>
                @for (p of blogRows; track p.slug; let i = $index) {
                  <a
                    [attr.href]="'/blog/' + p.slug"
                    (click)="onRowClick($event, 'blog', p.slug)"
                    class="flex flex-col gap-1 px-4 py-2.5 transition-colors"
                    [class.bg-\\[--accent\\]]="activeIndex() === svc.length + i"
                  >
                    <span class="truncate text-sm font-medium text-(--foreground)">{{ p.title }}</span>
                    <span class="line-clamp-1 text-xs text-(--muted-foreground)">{{ p.excerpt }}</span>
                  </a>
                }
              }
            } @else if ((result()?.didYouMean ?? []).length > 0) {
              <div class="flex flex-col gap-3 px-4 py-6">
                <p class="text-sm text-(--muted-foreground)">
                  Aucun résultat pour <span class="font-medium text-(--foreground)">«&nbsp;{{ query() }}&nbsp;»</span>.
                  Vouliez-vous dire&nbsp;:
                </p>
                <div class="flex flex-wrap gap-1.5">
                  @for (suggestion of result()?.didYouMean ?? []; track suggestion) {
                    <button
                      type="button"
                      (click)="applySuggestion(suggestion)"
                      class="rounded-full border border-(--border) px-3 py-1 text-xs text-(--foreground) transition-colors hover:bg-(--accent) focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-(--ring)"
                    >
                      {{ suggestion }}
                    </button>
                  }
                </div>
              </div>
            } @else {
              <div class="px-4 py-6 text-sm text-(--muted-foreground)">
                Aucun résultat pour <span class="font-medium text-(--foreground)">«&nbsp;{{ query() }}&nbsp;»</span>.
                Essayez d'autres mots-clés.
              </div>
            }
          </div>

          <!-- Footer keybinds: discoverability without modal chrome. -->
          <div
            class="flex items-center justify-between border-t border-(--border) bg-(--muted)/30 px-4 py-2 text-xs text-(--muted-foreground)"
          >
            <span class="flex items-center gap-3">
              <span class="flex items-center gap-1">
                <kbd class="rounded border border-(--border) bg-(--background) px-1.5 py-0.5 font-mono text-[10px]">↑↓</kbd>
                naviguer
              </span>
              <span class="flex items-center gap-1">
                <kbd class="rounded border border-(--border) bg-(--background) px-1.5 py-0.5 font-mono text-[10px]">Enter</kbd>
                ouvrir
              </span>
            </span>
            <span class="flex items-center gap-1">
              <kbd class="rounded border border-(--border) bg-(--background) px-1.5 py-0.5 font-mono text-[10px]">Esc</kbd>
              fermer
            </span>
          </div>
        </div>
      </div>
    }
  `,
})
export class GlobalSearchComponent implements OnInit, OnDestroy {
  protected readonly iconSearch = Search;
  protected readonly iconClose = X;

  protected readonly isOpen = signal(false);
  protected readonly query = signal('');
  protected readonly loading = signal(false);
  protected readonly result = signal<GlobalSearchResult | null>(null);
  protected readonly activeIndex = signal(0);

  protected readonly allRows = computed<Row[]>(() => {
    const r = this.result();
    if (!r) return [];
    const services: Row[] = r.services.map((s) => ({
      kind: 'service',
      slug: s.slug ?? '',
      title: s.title ?? '',
      description: s.description ?? '',
    }));
    const blog: Row[] = r.blog.map((p) => ({
      kind: 'blog',
      slug: p.slug,
      title: p.title,
      excerpt: p.excerpt,
    }));
    return [...services, ...blog];
  });

  private readonly query$ = new Subject<string>();
  private sub?: Subscription;

  private readonly router = inject(Router);
  private readonly search = inject(SearchService);

  @ViewChild('searchInput') searchInputRef?: ElementRef<HTMLInputElement>;

  ngOnInit(): void {
    this.sub = this.query$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        tap((q) => this.loading.set(q.length >= 2)),
        filter((q) => q.length >= 2),
        switchMap((q) => this.search.searchAll(q)),
      )
      .subscribe((result) => {
        this.result.set(result);
        this.activeIndex.set(0);
        this.loading.set(false);
      });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  open(): void {
    this.isOpen.set(true);
    // Focus the input on the next tick once it's in the DOM
    queueMicrotask(() => this.searchInputRef?.nativeElement.focus());
  }

  close(): void {
    this.isOpen.set(false);
  }

  onQueryChange(v: string): void {
    this.query.set(v);
    if (v.length < 2) {
      this.result.set(null);
      this.loading.set(false);
    }
    this.query$.next(v);
  }

  applySuggestion(s: string): void {
    this.query.set(s);
    this.query$.next(s);
    queueMicrotask(() => this.searchInputRef?.nativeElement.focus());
  }

  onRowClick(event: MouseEvent, kind: 'service' | 'blog', slug: string): void {
    // Honor cmd/ctrl-click by letting the browser default open in a new tab
    if (event.metaKey || event.ctrlKey || event.button !== 0) return;
    event.preventDefault();
    this.navigateTo(kind, slug);
  }

  private navigateTo(kind: 'service' | 'blog', slug: string): void {
    if (!slug) return;
    const url = kind === 'service' ? `/services/${slug}` : `/blog/${slug}`;
    this.router.navigateByUrl(url);
    this.close();
  }

  // ============================================================
  // Keyboard
  // ============================================================

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    // Ctrl/Cmd+K opens from anywhere — global shortcut
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      if (this.isOpen()) this.close();
      else this.open();
      return;
    }

    if (!this.isOpen()) return;

    if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
      return;
    }

    const rows = this.allRows();
    if (rows.length === 0) return;

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeIndex.set((this.activeIndex() + 1) % rows.length);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeIndex.set((this.activeIndex() - 1 + rows.length) % rows.length);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const row = rows[this.activeIndex()];
      if (row) this.navigateTo(row.kind, row.slug);
    }
  }
}
