import {
  Component,
  AfterViewInit,
  OnDestroy,
  OnInit,
  Inject,
  PLATFORM_ID,
  inject,
  signal,
} from '@angular/core';
import { isPlatformBrowser, NgClass, CurrencyPipe } from '@angular/common';
import { LucideAngularModule, Check, ArrowRight, Loader2, ShoppingCart } from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
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
        <!-- Header with anim-fade-up -->
        <div
          class="mx-auto max-w-3xl text-center scroll-animate anim-fade-up"
        >
          <div
            class="mb-4 inline-flex items-center gap-2 rounded-full border border-blue-500/20 bg-blue-500/10 px-4 py-1.5 text-xs font-medium text-blue-400"
          >
            <span
              class="inline-block h-1.5 w-1.5 rounded-full bg-blue-400"
            ></span>
            Nos Solutions
          </div>
          <h1
            class="font-display text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl lg:text-6xl"
          >
            Expertise Digitale
            <span
              class="bg-gradient-to-r from-violet-400 to-fuchsia-500 bg-clip-text text-transparent"
            >
              360°
            </span>
          </h1>
          <p class="mt-6 text-lg text-(--muted-foreground)">
            Une gamme complète d'outils et de services pour dominer votre
            marché local et national.
          </p>
        </div>

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

        <!-- Services Grid with staggered animations -->
        @if (!loading() && !error()) {
          <div
            class="mt-16 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3"
          >
            @for (service of services(); track service.id; let i = $index) {
              <div
                [id]="service.slug"
                class="group relative flex flex-col rounded-xl border bg-(--card) p-6 transition-all duration-300 hover:border-blue-500/30 hover:shadow-lg hover:shadow-blue-500/5 hover:-translate-y-1"
                [ngClass]="getServiceAnimation(i) + (highlightedSlug() === service.slug
                  ? ' service-highlight border-blue-500 ring-2 ring-blue-500/40 shadow-lg shadow-blue-500/20 scale-[1.02] -translate-y-2'
                  : ' border-(--border)')"
              >
                <!-- Selected badge -->
                @if (highlightedSlug() === service.slug) {
                  <div class="absolute -top-3 left-1/2 -translate-x-1/2 z-10 rounded-full bg-blue-600 px-3 py-1 text-xs font-semibold text-white shadow-md whitespace-nowrap">
                    ✨ Service sélectionné
                  </div>
                }

                <!-- Icon + Category -->
                <div class="mb-4 flex items-start justify-between">
                  <div
                    class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-500/10 text-xl transition-transform duration-300 group-hover:scale-110"
                  >
                    {{ service.icon }}
                  </div>
                  <span
                    class="rounded-full bg-blue-500/10 px-3 py-1 text-xs font-medium text-blue-400"
                  >
                    {{ service.categoryName }}
                  </span>
                </div>

                <!-- Title -->
                <h3
                  class="font-display text-lg font-bold leading-snug text-(--foreground)"
                >
                  {{ service.title }}
                </h3>

                <!-- Description -->
                <p
                  class="mt-3 text-sm leading-relaxed text-(--muted-foreground)"
                >
                  {{ service.description }}
                </p>

                <!-- Features / Benefits -->
                <ul class="mt-4 flex-1 space-y-2">
                  @for (benefit of service.benefits; track benefit) {
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

                <!-- Price + Add button -->
                <div
                  class="mt-6 flex items-end justify-between border-t border-(--border) pt-4"
                >
                  <div>
                    @if (service.currentOffer) {
                      <span
                        class="font-display text-2xl font-bold text-(--primary)"
                      >
                        {{
                          service.currentOffer.price
                            | currency: 'EUR' : 'symbol' : '1.2-2' : 'fr'
                        }}
                      </span>
                      @if (
                        service.currentOffer.originalPrice &&
                        service.currentOffer.originalPrice >
                          service.currentOffer.price
                      ) {
                        <span
                          class="ml-2 text-sm text-(--muted-foreground) line-through"
                        >
                          {{
                            service.currentOffer.originalPrice
                              | currency: 'EUR' : 'symbol' : '1.2-2' : 'fr'
                          }}
                        </span>
                      }
                      <div class="text-xs text-(--muted-foreground)">
                        {{
                          service.currentOffer.durationType === 'ONE_TIME'
                            ? 'Paiement unique'
                            : service.currentOffer.durationType === 'MONTHLY'
                              ? '/ mois'
                              : service.currentOffer.durationType === 'YEARLY'
                                ? '/ an'
                                : 'Paiement unique'
                        }}
                      </div>
                    } @else {
                      <span
                        class="font-display text-lg font-bold text-(--primary)"
                        >Sur devis</span
                      >
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

        <!-- CTA Section with anim-slide-up -->
        <section
          class="mt-20 scroll-animate anim-slide-up rounded-2xl border border-(--border) bg-(--card) p-10 text-center sm:p-16"
        >
          <span
            class="text-xs font-semibold uppercase tracking-wider text-(--muted-foreground)"
            >Parlons Business</span
          >
          <h2
            class="mt-3 font-display text-3xl font-bold text-(--foreground) sm:text-4xl"
          >
            Prêt à dominer votre marché ?
          </h2>
          <p
            class="mx-auto mt-4 max-w-xl text-(--muted-foreground)"
          >
            Ne laissez pas vos concurrents prendre l'avantage. Nos experts
            vous accompagnent vers le sommet.
          </p>
          <div
            class="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center"
          >
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

  private readonly catalogService = inject(CatalogService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  readonly services = signal<ServiceItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly checkoutLoading = signal<string | null>(null);
  readonly highlightedSlug = signal<string | null>(null);

  private scrollObserver?: IntersectionObserver;
  private fragmentSub?: Subscription;
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  // Assign different animation classes to each service card
  private readonly animationClasses = [
    'scroll-animate anim-fade-up',
    'scroll-animate anim-scale-in',
    'scroll-animate anim-fade-left',
    'scroll-animate anim-flip-y',
    'scroll-animate anim-blur-in',
    'scroll-animate anim-fade-right',
    'scroll-animate anim-slide-up',
    'scroll-animate anim-clip-reveal',
    'scroll-animate anim-slide-rotate',
  ];

  getServiceAnimation(index: number): string {
    const baseClass =
      this.animationClasses[index % this.animationClasses.length];
    const delay = `delay-${((index % 3) + 1)}00`;
    return `${baseClass} ${delay}`;
  }

  ngOnInit(): void {
    this.loadServices();

    // Listen to URL fragment changes for scroll-to-service navigation
    if (this.isBrowser) {
      this.fragmentSub = this.route.fragment.subscribe((fragment) => {
        if (fragment) {
          this.scrollToService(fragment);
        }
      });
    }
  }

  private scrollToService(slug: string): void {
    // Wait for services to be loaded before scrolling
    const tryScroll = (retries = 0) => {
      const element = document.getElementById(slug);
      if (element) {
        // Scroll with offset for navbar
        setTimeout(() => {
          element.scrollIntoView({ behavior: 'smooth', block: 'center' });
          // Add highlight effect — keep visible for 6s so user clearly sees it
          this.highlightedSlug.set(slug);
          setTimeout(() => this.highlightedSlug.set(null), 6000);
        }, 100);
      } else if (retries < 10) {
        // Retry if services haven't rendered yet
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
        // Filter only active services and sort by displayOrder to avoid grid gaps
        const sorted = data
          .filter((s) => s.active)
          .sort((a, b) => a.displayOrder - b.displayOrder);
        this.services.set(sorted);
        this.loading.set(false);
        // Re-observe after data loads
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
    // Check if user is logged in
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
            // Redirect to Stripe Checkout
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
