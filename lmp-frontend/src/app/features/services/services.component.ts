import {
  Component,
  AfterViewInit,
  OnDestroy,
  OnInit,
  Inject,
  PLATFORM_ID,
  inject,
  signal,
  computed,
  effect,
} from '@angular/core';
import { isPlatformBrowser, NgClass, CurrencyPipe } from '@angular/common';
import { LucideAngularModule, Check, ArrowRight, Loader2, ShoppingCart, Filter } from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { SeoService } from '../../core/services/seo.service';
import {
  CatalogService,
  ServiceItem,
} from '../../core/services/catalog.service';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'lmp-services',
  standalone: true,
  imports: [LucideAngularModule, HlmButton, NgClass, CurrencyPipe],
  template: `
    <section class="relative">
      <div class="relative mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <!-- Header -->
        <div class="mx-auto max-w-3xl text-center scroll-animate anim-fade-up">
          <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-4 block">
            Nos Solutions
          </span>
          <h1 class="font-display text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl lg:text-6xl">
            Expertise Digitale
            <span class="italic font-light text-(--primary)">360°</span>
          </h1>
          <p class="mt-6 text-lg text-(--muted-foreground)">
            Une gamme complète d'outils et de services pour dominer votre
            marché local et national.
          </p>
        </div>

        <!-- Category Filter Tabs -->
        @if (!loading() && !error() && categories().length > 0) {
          <div class="mt-10 flex flex-wrap items-center justify-center gap-2 scroll-animate anim-fade-up delay-100">
            <button
              class="rounded-full px-4 py-1.5 text-sm font-medium transition-all duration-200 cursor-pointer"
              [ngClass]="selectedCategory() === null
                ? 'bg-(--primary) text-white shadow-md shadow-(--primary)/25'
                : 'bg-(--card) text-(--muted-foreground) border border-(--border) hover:border-(--primary)/30 hover:text-(--foreground)'"
              (click)="selectedCategory.set(null)"
            >
              Tous
              <span class="ml-1 text-xs opacity-70">({{ services().length }})</span>
            </button>
            @for (cat of categories(); track cat.slug) {
              <button
                class="rounded-full px-4 py-1.5 text-sm font-medium transition-all duration-200 cursor-pointer"
                [ngClass]="selectedCategory() === cat.slug
                  ? 'bg-(--primary) text-white shadow-md shadow-(--primary)/25'
                  : 'bg-(--card) text-(--muted-foreground) border border-(--border) hover:border-(--primary)/30 hover:text-(--foreground)'"
                (click)="selectedCategory.set(cat.slug)"
              >
                {{ cat.name }}
                <span class="ml-1 text-xs opacity-70">({{ getCategoryCount(cat.slug) }})</span>
              </button>
            }
          </div>
        }

        <!-- Loading state -->
        @if (loading()) {
          <div class="mt-16 flex flex-col items-center justify-center py-16">
            <lucide-icon
              [img]="Loader2Icon"
              [size]="32"
              class="animate-spin text-(--primary)"
            ></lucide-icon>
            <p class="mt-4 text-sm text-(--muted-foreground)">
              Chargement des services…
            </p>
          </div>
        }

        <!-- Error state -->
        @if (error()) {
          <div
            class="mt-16 rounded-xl border border-red-500/20 bg-red-500/5 p-8 text-center"
          >
            <p class="text-sm text-red-400">{{ error() }}</p>
            <button
              hlmBtn
              variant="outline"
              size="sm"
              class="mt-4 cursor-pointer"
              (click)="loadServices()"
            >
              Réessayer
            </button>
          </div>
        }

        <!-- Services Grid -->
        @if (!loading() && !error()) {
          <!-- Empty state for filtered category -->
          @if (filteredServices().length === 0) {
            <div class="mt-16 py-16 text-center">
              <div class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-(--primary)/10">
                <lucide-icon [img]="FilterIcon" [size]="24" class="text-(--primary)"></lucide-icon>
              </div>
              <h3 class="font-display text-lg font-semibold text-(--foreground)">Aucun service trouvé</h3>
              <p class="mt-2 text-sm text-(--muted-foreground)">Aucun service disponible dans cette catégorie.</p>
              <button
                hlmBtn
                variant="outline"
                size="sm"
                class="mt-4 cursor-pointer"
                (click)="selectedCategory.set(null)"
              >
                Voir tous les services
              </button>
            </div>
          }

          <div class="mt-16 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            @for (service of filteredServices(); track service.id; let i = $index) {
              <div
                [id]="service.slug"
                class="group relative flex flex-col rounded-xl border bg-(--card) p-6 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 hover:-translate-y-1 scroll-animate anim-fade-up"
                [ngClass]="{
                  'service-highlight border-(--primary) ring-2 ring-(--primary)/40 shadow-lg shadow-emerald-500/20 scale-[1.02] -translate-y-2': highlightedSlug() === service.slug,
                  'border-(--border)': highlightedSlug() !== service.slug
                }"
                [style.transition-delay.ms]="(i % 3) * 100"
              >
                <!-- Selected badge -->
                @if (highlightedSlug() === service.slug) {
                  <div class="absolute -top-3 left-1/2 -translate-x-1/2 z-10 rounded-full bg-(--primary) px-3 py-1 text-xs font-semibold text-white shadow-md whitespace-nowrap">
                    ✨ Service sélectionné
                  </div>
                }

                <!-- Number + Category -->
                <div class="mb-4 flex items-start justify-between">
                  <span class="font-mono text-xs font-semibold text-(--muted-foreground)/40">
                    /{{ (i + 1).toString().padStart(2, '0') }}
                  </span>
                  <span class="rounded-full bg-(--primary)/10 px-3 py-1 text-xs font-medium text-(--primary)">
                    {{ service.categoryName }}
                  </span>
                </div>

                <!-- Title -->
                <h3 class="font-display text-lg font-bold leading-snug text-(--foreground) group-hover:text-(--primary) transition-colors">
                  {{ service.title }}
                </h3>

                <!-- Description -->
                <p class="mt-3 text-sm leading-relaxed text-(--muted-foreground)">
                  {{ service.description }}
                </p>

                <!-- Features / Benefits -->
                <ul class="mt-4 flex-1 space-y-2">
                  @for (benefit of service.benefits; track benefit) {
                    <li class="flex items-start gap-2 text-sm text-(--muted-foreground)">
                      <lucide-icon
                        [img]="CheckIcon"
                        [size]="14"
                        class="mt-0.5 shrink-0 text-(--primary)"
                      ></lucide-icon>
                      {{ benefit }}
                    </li>
                  }
                </ul>

                <!-- Price + Add button -->
                <div class="mt-6 flex items-end justify-between border-t border-(--border) pt-4">
                  <div>
                    @if (service.currentOffer) {
                      <span class="font-display text-2xl font-bold text-(--primary)">
                        {{ service.currentOffer.price | currency: 'EUR' : 'symbol' : '1.2-2' : 'fr' }}
                      </span>
                      @if (
                        service.currentOffer.originalPrice &&
                        service.currentOffer.originalPrice > service.currentOffer.price
                      ) {
                        <span class="ml-2 text-sm text-(--muted-foreground) line-through">
                          {{ service.currentOffer.originalPrice | currency: 'EUR' : 'symbol' : '1.2-2' : 'fr' }}
                        </span>
                      }
                      <div class="text-xs text-(--muted-foreground)">
                        {{ service.currentOffer.durationType === 'ONE_TIME' ? 'Paiement unique' :
                           service.currentOffer.durationType === 'MONTHLY' ? '/ mois' :
                           service.currentOffer.durationType === 'YEARLY' ? '/ an' : 'Paiement unique' }}
                      </div>
                    } @else {
                      <span class="font-display text-lg font-bold text-(--primary)">Sur devis</span>
                    }
                  </div>
                  <button
                    class="flex h-9 cursor-pointer items-center gap-1.5 rounded-lg bg-(--primary)/10 px-3 text-sm font-medium text-(--primary) transition-all duration-200 hover:scale-105 hover:bg-(--primary)/20 disabled:opacity-50 disabled:cursor-not-allowed"
                    [disabled]="!service.currentOffer || checkoutLoading() === service.id"
                    (click)="onCheckout(service)"
                  >
                    @if (checkoutLoading() === service.id) {
                      <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
                    } @else {
                      <lucide-icon [img]="ShoppingCartIcon" [size]="14"></lucide-icon>
                    }
                    Commander
                  </button>
                </div>
              </div>
            }
          </div>
        }

        <!-- CTA Section -->
        <section class="mt-20 scroll-animate anim-fade-up rounded-2xl border border-(--border) bg-(--card) p-10 text-center sm:p-16">
          <span class="text-xs font-semibold uppercase tracking-[0.2em] text-(--primary) block mb-3">
            Parlons Business
          </span>
          <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl">
            Prêt à dominer
            <span class="italic font-light text-(--primary)">votre marché ?</span>
          </h2>
          <p class="mx-auto mt-4 max-w-xl text-(--muted-foreground)">
            Ne laissez pas vos concurrents prendre l'avantage. Nos experts
            vous accompagnent vers le sommet.
          </p>
          <div class="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
            <a
              hlmBtn
              variant="default"
              size="lg"
              href="mailto:lmp.assistance&#64;gmail.com"
              class="cursor-pointer gap-2"
            >
              Démarrer mon projet
              <lucide-icon [img]="ArrowRightIcon" [size]="16"></lucide-icon>
            </a>
            <span class="text-sm text-(--muted-foreground)">Audit Gratuit</span>
          </div>
        </section>
      </div>
    </section>
  `,
})
export class ServicesComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly CheckIcon = Check;
  readonly ArrowRightIcon = ArrowRight;
  readonly Loader2Icon = Loader2;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly FilterIcon = Filter;

  private readonly catalogService = inject(CatalogService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly seo = inject(SeoService);

  readonly services = signal<ServiceItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly checkoutLoading = signal<string | null>(null);
  readonly highlightedSlug = signal<string | null>(null);
  readonly selectedCategory = signal<string | null>(null);

  readonly categories = computed(() => {
    const seen = new Set<string>();
    const cats: { name: string; slug: string }[] = [];
    for (const s of this.services()) {
      if (!seen.has(s.categorySlug)) {
        seen.add(s.categorySlug);
        cats.push({ name: s.categoryName, slug: s.categorySlug });
      }
    }
    return cats;
  });

  readonly filteredServices = computed(() => {
    const cat = this.selectedCategory();
    if (!cat) return this.services();
    return this.services().filter((s) => s.categorySlug === cat);
  });

  getCategoryCount(slug: string): number {
    return this.services().filter((s) => s.categorySlug === slug).length;
  }

  private scrollObserver?: IntersectionObserver;
  private fragmentSub?: Subscription;
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);

    effect(() => {
      this.filteredServices();
      if (this.isBrowser) {
        setTimeout(() => this.setupScrollObserver(), 0);
      }
    });
  }

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'Nos Services — Expertise Digitale 360°',
      description: 'Découvrez notre gamme complète de services de marketing digital : référencement SEO, gestion Google My Business, création de sites web, publicité en ligne et gestion des avis.',
      url: '/services',
      keywords: 'services marketing digital, référencement SEO, Google My Business, création site web, publicité en ligne, gestion avis, présence locale',
    });

    this.loadServices();

    if (this.isBrowser) {
      this.fragmentSub = this.route.fragment.subscribe((fragment) => {
        if (fragment) {
          this.scrollToService(fragment);
        }
      });
    }
  }

  private scrollToService(slug: string): void {
    const tryScroll = (retries = 0) => {
      const element = document.getElementById(slug);
      if (element) {
        setTimeout(() => {
          element.scrollIntoView({ behavior: 'smooth', block: 'center' });
          this.highlightedSlug.set(slug);
          setTimeout(() => this.highlightedSlug.set(null), 6000);
        }, 100);
      } else if (retries < 10) {
        setTimeout(() => tryScroll(retries + 1), 200);
      }
    };
    tryScroll();
  }

  loadServices(): void {
    this.loading.set(true);
    this.error.set(null);

    this.catalogService.getServices().subscribe({
      next: (data) => {
        const sorted = data
          .filter((s) => s.active)
          .sort((a, b) => a.displayOrder - b.displayOrder);
        this.services.set(sorted);
        this.loading.set(false);
        if (this.isBrowser) {
          setTimeout(() => this.setupScrollObserver(), 50);
        }
      },
      error: () => {
        this.error.set(
          'Impossible de charger les services. Veuillez réessayer.',
        );
        this.loading.set(false);
      },
    });
  }

  ngAfterViewInit(): void {
    if (!this.isBrowser) return;
    this.setupScrollObserver();
  }

  private setupScrollObserver(): void {
    this.scrollObserver?.disconnect();

    this.scrollObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('animate-visible');
          }
        });
      },
      { threshold: 0.1, rootMargin: '0px 0px -50px 0px' },
    );

    document.querySelectorAll('.scroll-animate').forEach((el) => {
      this.scrollObserver!.observe(el);
    });
  }

  ngOnDestroy(): void {
    this.scrollObserver?.disconnect();
    this.fragmentSub?.unsubscribe();
  }

  onCheckout(service: ServiceItem): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    if (!service.currentOffer) return;

    this.checkoutLoading.set(service.id);

    this.http
      .post<{
        success: boolean;
        data?: { redirectUrl: string; sessionId: string; orderId: string };
        message?: string;
      }>(
        `${environment.apiUrl}/api/v1/payments/checkout`,
        {
          offerId: service.currentOffer.id,
          currency: 'EUR',
        },
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          this.checkoutLoading.set(null);
          if (res.success && res.data?.redirectUrl) {
            window.location.href = res.data.redirectUrl;
          } else {
            alert(res.message || 'Erreur lors de la création du paiement');
          }
        },
        error: (err) => {
          this.checkoutLoading.set(null);
          if (err.status === 401) {
            this.router.navigate(['/login']);
          } else {
            alert('Erreur lors de la création du paiement. Veuillez réessayer.');
          }
        },
      });
  }
}
