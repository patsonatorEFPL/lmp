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
  afterNextRender,
} from '@angular/core';
import { isPlatformBrowser, NgClass, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Check, ArrowRight, Loader2, ShoppingCart, Filter, Save } from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { Router, ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { SeoService } from '../../core/services/seo.service';
import {
  CatalogService,
  ServiceItem,
} from '../../core/services/catalog.service';
import { AuthService } from '../../core/services/auth.service';
import { SiteConfigService } from '../../core/services/site-config.service';
import { ProfileService } from '../../core/services/profile.service';

@Component({
  selector: 'lmp-services',
  standalone: true,
  imports: [LucideAngularModule, HlmButton, NgClass, CurrencyPipe, FormsModule],
  template: `
    <section class="relative">
      <div class="relative mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <!-- Header -->
        <div class="mx-auto max-w-3xl text-center">
          <h1 class="text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl">
            Nos services
          </h1>
          <p class="mt-4 text-base text-(--muted-foreground)">
            Une gamme complète d'outils et de services pour dominer votre
            marché local et national.
          </p>
        </div>

        <!-- Category Filter Tabs (underline style, not pills) -->
        @if (!loading() && !error() && categories().length > 0) {
          <div class="mt-8 flex flex-wrap items-center justify-center gap-1 border-b border-(--border) scroll-animate">
            <button
              class="px-4 py-2.5 text-sm font-medium transition-colors duration-150 cursor-pointer border-b-2 -mb-px"
              [ngClass]="selectedCategory() === null
                ? 'border-(--primary) text-(--foreground)'
                : 'border-transparent text-(--muted-foreground) hover:text-(--foreground)'"
              (click)="selectedCategory.set(null)"
            >
              Tous ({{ services().length }})
            </button>
            @for (cat of categories(); track cat.slug) {
              <button
                class="px-4 py-2.5 text-sm font-medium transition-colors duration-150 cursor-pointer border-b-2 -mb-px"
                [ngClass]="selectedCategory() === cat.slug
                  ? 'border-(--primary) text-(--foreground)'
                  : 'border-transparent text-(--muted-foreground) hover:text-(--foreground)'"
                (click)="selectedCategory.set(cat.slug)"
              >
                {{ cat.name }} ({{ getCategoryCount(cat.slug) }})
              </button>
            }
          </div>
        }



        <!-- Loading state -->
        @if (loading()) {
          <div class="mt-12 flex flex-col items-center justify-center py-12">
            <lucide-icon
              [img]="Loader2Icon"
              [size]="24"
              class="animate-spin text-(--primary)"
            ></lucide-icon>
            <p class="mt-3 text-sm text-(--muted-foreground)">
              Chargement des services…
            </p>
          </div>
        }

        <!-- Error state -->
        @if (error()) {
          <div
            class="mt-12 rounded-sm border border-(--destructive)/20 bg-(--destructive)/5 p-6 text-center"
          >
            <p class="text-sm text-(--destructive)">{{ error() }}</p>
            <button
              hlmBtn
              variant="outline"
              size="sm"
              class="mt-3 cursor-pointer"
              (click)="loadServices()"
            >
              Réessayer
            </button>
          </div>
        }

        <!-- Services Grid -->
        @if (!loading() && !error()) {
          @if (filteredServices().length === 0) {
            <div class="mt-12 py-12 text-center">
              <lucide-icon [img]="FilterIcon" [size]="24" class="mx-auto text-(--muted-foreground)"></lucide-icon>
              <h3 class="mt-3 text-base font-semibold text-(--foreground)">Aucun service trouvé</h3>
              <p class="mt-1 text-sm text-(--muted-foreground)">Aucun service disponible dans cette catégorie.</p>
              <button
                hlmBtn
                variant="outline"
                size="sm"
                class="mt-3 cursor-pointer"
                (click)="selectedCategory.set(null)"
              >
                Voir tous les services
              </button>
            </div>
          }

          <div class="mt-10 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (service of filteredServices(); track service.id; let i = $index) {
              <div
                [id]="service.slug"
                class="group relative flex flex-col rounded-sm border bg-(--card) p-5 transition-colors duration-150 scroll-animate"
                [ngClass]="{
                  'border-(--primary) ring-1 ring-(--primary)/30': highlightedSlug() === service.slug,
                  'border-(--border) hover:border-(--primary)/30': highlightedSlug() !== service.slug
                }"
                [style.transition-delay.ms]="(i % 3) * 100"
              >
                <!-- Category -->
                <div class="mb-3 flex items-start justify-between">
                  <span class="text-xs font-mono text-(--muted-foreground)">
                    {{ (i + 1).toString().padStart(2, '0') }}
                  </span>
                  <span class="rounded-sm bg-(--muted) px-2 py-0.5 text-xs font-medium text-(--muted-foreground)">
                    {{ service.categoryName }}
                  </span>
                </div>

                <h3 class="text-sm font-semibold text-(--foreground)">
                  {{ service.title }}
                </h3>

                <p class="mt-2 text-sm leading-relaxed text-(--muted-foreground)">
                  {{ service.description }}
                </p>

                <ul class="mt-3 flex-1 space-y-1.5">
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

                <div class="mt-5 flex items-end justify-between border-t border-(--border) pt-3">
                  <div>
                    @if (service.currentOffer) {
                      <span class="text-lg font-bold text-(--foreground)">
                        {{
                          service.currentOffer.price
                            | currency
                              : (service.currentOffer.currency || 'EUR')
                              : 'symbol'
                              : '1.2-2'
                              : 'fr'
                        }}
                      </span>
                      @if (
                        service.currentOffer.originalPrice &&
                        service.currentOffer.originalPrice > service.currentOffer.price
                      ) {
                        <span class="ml-1.5 text-sm text-(--muted-foreground) line-through">
                          {{
                            service.currentOffer.originalPrice
                              | currency
                                : (service.currentOffer.currency || 'EUR')
                                : 'symbol'
                                : '1.2-2'
                                : 'fr'
                          }}
                        </span>
                      }
                      <div class="text-xs text-(--muted-foreground)">
                        {{ service.currentOffer.durationType === 'ONE_TIME' ? 'Paiement unique' :
                           service.currentOffer.durationType === 'MONTHLY' ? '/ mois' :
                           service.currentOffer.durationType === 'YEARLY' ? '/ an' : 'Paiement unique' }}
                      </div>
                    } @else {
                      <span class="text-base font-semibold text-(--foreground)">Sur devis</span>
                    }
                  </div>
                  <button
                    class="flex h-8 cursor-pointer items-center gap-1.5 rounded-sm bg-(--muted) px-3 text-sm font-medium text-(--foreground) transition-colors duration-150 hover:bg-(--accent) disabled:opacity-50 disabled:cursor-not-allowed"
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
        <section class="mt-16 scroll-animate rounded-sm border border-(--border) bg-(--card) p-8 sm:p-12 text-center">
          <h2 class="text-2xl font-bold text-(--foreground) sm:text-3xl">
            Prêt à démarrer ?
          </h2>
          <p class="mx-auto mt-3 max-w-xl text-sm text-(--muted-foreground)">
            Ne laissez pas vos concurrents prendre l'avantage. Nos experts
            vous accompagnent vers le sommet.
          </p>
          <div class="mt-6 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
            <a
              hlmBtn
              variant="default"
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
  readonly authService = inject(AuthService);

  readonly CheckIcon = Check;
  readonly ArrowRightIcon = ArrowRight;
  readonly Loader2Icon = Loader2;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly FilterIcon = Filter;
  readonly SaveIcon = Save;

  private readonly catalogService = inject(CatalogService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly siteConfig = inject(SiteConfigService);
  private readonly seo = inject(SeoService);
  private readonly profileService = inject(ProfileService);

  checkoutTaxForm = {
    vatReverseCharge: false,
    vatNumber: '',
  };

  readonly services = signal<ServiceItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly checkoutLoading = signal<string | null>(null);
  readonly highlightedSlug = signal<string | null>(null);
  readonly selectedCategory = signal<string | null>(null);
  readonly vatSaveStatus = signal<'idle' | 'saving' | 'saved' | 'error'>('idle');
  readonly vatSaveError = signal<string>('');

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

    afterNextRender(() => this.loadServices());

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

    this.seo.setJsonLd({
      '@context': 'https://schema.org',
      '@type': 'ItemList',
      name: 'Services LMP Digital Services',
      url: `${this.seo.baseUrl}/services`,
      itemListElement: [
        {
          '@type': 'ListItem',
          position: 1,
          item: {
            '@type': 'Service',
            name: 'Référencement SEO',
            description: 'Optimisation technique et sémantique pour dominer les résultats Google.',
            provider: { '@type': 'Organization', name: 'LMP Digital Services' },
          },
        },
        {
          '@type': 'ListItem',
          position: 2,
          item: {
            '@type': 'Service',
            name: 'Gestion Google My Business',
            description: 'Optimisation complète de votre fiche établissement Google.',
            provider: { '@type': 'Organization', name: 'LMP Digital Services' },
          },
        },
        {
          '@type': 'ListItem',
          position: 3,
          item: {
            '@type': 'Service',
            name: 'Création de Site Web',
            description: 'Sites web professionnels avec SSL, référencement optimisé et design responsive.',
            provider: { '@type': 'Organization', name: 'LMP Digital Services' },
          },
        },
        {
          '@type': 'ListItem',
          position: 4,
          item: {
            '@type': 'Service',
            name: 'Publicité en Ligne',
            description: 'Campagnes Google Ads et Meta Ads avec ciblage géolocalisé.',
            provider: { '@type': 'Organization', name: 'LMP Digital Services' },
          },
        },
        {
          '@type': 'ListItem',
          position: 5,
          item: {
            '@type': 'Service',
            name: 'Gestion des Avis',
            description: 'Stratégie de collecte et gestion de la e-réputation.',
            provider: { '@type': 'Organization', name: 'LMP Digital Services' },
          },
        },
      ],
    });

    const u = this.authService.user();
    if (u) {
      this.checkoutTaxForm = {
        vatReverseCharge: !!u.vatReverseCharge,
        vatNumber: u.vatNumber ?? '',
      };
    }

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
          } else {
            entry.target.classList.remove('animate-visible');
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
    clearTimeout(this.vatSaveTimer);
  }

  private vatSaveTimer?: ReturnType<typeof setTimeout>;

  saveVatPreferences(): void {
    if (
      this.checkoutTaxForm.vatReverseCharge &&
      !this.checkoutTaxForm.vatNumber.trim()
    ) {
      this.vatSaveStatus.set('error');
      this.vatSaveError.set('Veuillez saisir votre numéro de TVA.');
      return;
    }

    this.vatSaveStatus.set('saving');

    this.profileService
      .updateProfile({
        vatReverseCharge: this.checkoutTaxForm.vatReverseCharge,
        vatNumber: this.checkoutTaxForm.vatReverseCharge
          ? this.checkoutTaxForm.vatNumber.trim()
          : '',
      })
      .subscribe({
        next: (updated) => {
          this.authService.setUser(updated);
          this.vatSaveStatus.set('saved');
          clearTimeout(this.vatSaveTimer);
          this.vatSaveTimer = setTimeout(() => this.vatSaveStatus.set('idle'), 4000);
        },
        error: (err) => {
          this.vatSaveStatus.set('error');
          this.vatSaveError.set(
            err.error?.message || 'Impossible d\'enregistrer vos préférences.',
          );
        },
      });
  }

  onCheckout(service: ServiceItem): void {
    if (!this.authService.isLoggedIn()) {
      const target = service.currentOffer?.id
        ? `/checkout/${service.currentOffer.id}`
        : '/services';
      this.siteConfig.goToLogin(target);
      return;
    }

    if (!service.currentOffer) return;

    if (
      this.checkoutTaxForm.vatReverseCharge &&
      !this.checkoutTaxForm.vatNumber.trim()
    ) {
      alert(
        'Veuillez saisir votre numéro de TVA lorsque l’autoliquidation (auto-reverse) est activée.',
      );
      return;
    }

    this.checkoutLoading.set(service.id);

    this.profileService
      .updateProfile({
        vatReverseCharge: this.checkoutTaxForm.vatReverseCharge,
        vatNumber: this.checkoutTaxForm.vatReverseCharge
          ? this.checkoutTaxForm.vatNumber.trim()
          : '',
      })
      .subscribe({
        next: (updated) => {
          this.authService.setUser(updated);
          this.startCheckoutSession(service);
        },
        error: (err) => {
          this.checkoutLoading.set(null);
          alert(
            err.error?.message ||
              err.message ||
              'Impossible d’enregistrer vos informations de TVA.',
          );
        },
      });
  }

  private startCheckoutSession(service: ServiceItem): void {
    if (!service.currentOffer) {
      this.checkoutLoading.set(null);
      return;
    }

    this.checkoutLoading.set(null);
    void this.router.navigate(['/checkout', service.currentOffer.id]);
  }
}
