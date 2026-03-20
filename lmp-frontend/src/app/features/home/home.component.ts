import {
  Component,
  signal,
  AfterViewInit,
  OnDestroy,
  OnInit,
  Inject,
  PLATFORM_ID,
  inject,
} from '@angular/core';
import { isPlatformBrowser, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SeoService } from '../../core/services/seo.service';
import {
  LucideAngularModule,
  ArrowRight,
  Check,
  Calendar,
  ShoppingCart,
  Search,
  BarChart3,
  Globe,
  Megaphone,
  Target,
  Layers,
  Rocket,
  ChevronDown,
  Quote,
  Shield,
  Users,
  Award,
  Zap,
  TrendingUp,
  Star,
} from 'lucide-angular';
import { AppointmentModalComponent } from '../../shared/modals/appointment-modal.component';
import { CatalogService, ServiceItem } from '../../core/services/catalog.service';

@Component({
  selector: 'lmp-home',
  standalone: true,
  imports: [RouterLink, LucideAngularModule, AppointmentModalComponent, CurrencyPipe],
  template: `
    <!-- ===== HERO SECTION ===== -->
    <section class="relative overflow-hidden min-h-screen -mt-16 pt-16 flex items-center">
      <!-- Background image with overlay -->
      <div class="absolute inset-0">
        <img
          src="/images/hero-bg-office.jpg"
          alt="Modern corporate office — Photo by Mikael Blomkvist"
          class="absolute inset-0 w-full h-full object-cover"
        />
        <div class="absolute inset-0 bg-gradient-to-b from-black/80 via-black/70 to-black/90"></div>
      </div>

      <!-- Subtle dot pattern -->
      <div
        class="pointer-events-none absolute inset-0 opacity-20"
        style="background-image: radial-gradient(circle, rgba(16,185,129,0.3) 1px, transparent 1px); background-size: 40px 40px;"
      ></div>

      <div class="relative z-10 mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8 w-full">
        <div class="max-w-3xl">
          <!-- Service pills -->
          <div class="scroll-animate anim-fade-up mb-8 flex flex-wrap gap-2">
            @for (tag of heroTags; track tag) {
              <span class="rounded-full border border-white/15 bg-white/5 backdrop-blur-sm px-4 py-1.5 text-xs font-medium tracking-wider text-white/70 uppercase">
                {{ tag }}
              </span>
            }
          </div>

          <!-- Main heading -->
          <h1 class="scroll-animate anim-fade-up delay-100 font-display tracking-tight text-white leading-[1.1]">
            <span class="block text-5xl sm:text-6xl lg:text-7xl font-light italic text-emerald-400">
              Propulsez
            </span>
            <span class="block text-5xl sm:text-6xl lg:text-7xl font-bold mt-1">
              Votre Visibilité
            </span>
            <span class="block text-5xl sm:text-6xl lg:text-7xl font-bold">
              Digitale<span class="text-emerald-400">.</span>
            </span>
          </h1>

          <p class="scroll-animate anim-fade-up delay-200 mt-6 max-w-xl text-base leading-relaxed text-white/60 sm:text-lg font-light">
            Technologie de pointe et expertise humaine pour dominer
            votre marché local. Plus de 500 entreprises nous font confiance.
          </p>

          <!-- CTA buttons -->
          <div class="scroll-animate anim-fade-up delay-300 mt-10 flex flex-wrap gap-4">
            <button
              (click)="showAppointment.set(true)"
              class="group inline-flex items-center gap-2.5 rounded-full bg-emerald-500 px-7 py-3.5 text-sm font-semibold text-white transition-all duration-300 hover:bg-emerald-400 hover:shadow-lg hover:shadow-emerald-500/25 cursor-pointer"
            >
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
              Réserver un audit gratuit
              <lucide-icon [img]="ArrowRightIcon" [size]="16" class="transition-transform group-hover:translate-x-0.5"></lucide-icon>
            </button>
            <a
              routerLink="/services"
              class="inline-flex items-center gap-2 rounded-full border border-white/20 px-7 py-3.5 text-sm font-medium text-white/90 transition-all duration-300 hover:bg-white/10 hover:border-white/30 cursor-pointer"
            >
              Découvrir nos services
            </a>
          </div>

          <!-- Trust strip -->
          <div class="scroll-animate anim-fade-up delay-400 mt-12 flex flex-wrap items-center gap-6 text-xs text-white/40 font-medium uppercase tracking-wider">
            <span class="flex items-center gap-2">
              <lucide-icon [img]="ShieldIcon" [size]="14" class="text-emerald-500"></lucide-icon>
              Paiement sécurisé
            </span>
            <span class="flex items-center gap-2">
              <lucide-icon [img]="ZapIcon" [size]="14" class="text-emerald-500"></lucide-icon>
              Mise en place rapide
            </span>
            <span class="flex items-center gap-2">
              <lucide-icon [img]="CheckIcon" [size]="14" class="text-emerald-500"></lucide-icon>
              Pas de frais cachés
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== PARTNER TICKER ===== -->
    <section class="border-y border-(--border) bg-(--card)/50 overflow-hidden">
      <div class="py-5">
        <div class="marquee-track">
          @for (partner of partnerLogos.concat(partnerLogos); track $index) {
            <span class="flex items-center gap-2 px-8 text-sm font-medium tracking-wider uppercase whitespace-nowrap text-(--muted-foreground)/60">
              <span class="h-1.5 w-1.5 rounded-full bg-(--primary)"></span>
              {{ partner }}
            </span>
          }
        </div>
      </div>
    </section>

    <!-- ===== SERVICES SECTION ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <!-- Section header -->
        <div class="flex flex-col lg:flex-row lg:items-end lg:justify-between mb-16">
          <div class="scroll-animate anim-fade-up">
            <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
              Ce Que Nous Faisons
            </span>
            <h2 class="font-display text-3xl sm:text-4xl lg:text-5xl font-bold text-(--foreground) leading-tight">
              Des solutions qui transforment<br />
              <span class="italic font-light text-(--primary)">votre présence digitale</span>
            </h2>
          </div>
          <div class="mt-6 lg:mt-0 scroll-animate anim-fade-right delay-200">
            <a
              routerLink="/services"
              class="inline-flex items-center gap-2 text-sm font-medium text-(--primary) hover:underline underline-offset-4 cursor-pointer"
            >
              Voir tous les services
              <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
            </a>
          </div>
        </div>

        <!-- Skeleton loading -->
        @if (loadingFeatured()) {
          <div class="grid grid-cols-1 gap-5 sm:grid-cols-2">
            @for (skeleton of [1,2,3,4]; track skeleton) {
              <div class="flex flex-col rounded-xl border border-(--border) bg-(--card) p-7 animate-pulse">
                <div class="h-4 w-12 rounded bg-(--muted) mb-4"></div>
                <div class="h-6 w-3/4 rounded bg-(--muted) mb-3"></div>
                <div class="h-4 w-full rounded bg-(--muted) mb-2"></div>
                <div class="h-4 w-5/6 rounded bg-(--muted)"></div>
              </div>
            }
          </div>
        }

        <!-- Service cards from API -->
        <div class="grid grid-cols-1 gap-5 sm:grid-cols-2">
          @for (service of featuredServices(); track service.id; let i = $index) {
            <a
              routerLink="/services"
              [fragment]="service.slug"
              class="group relative flex flex-col rounded-xl border border-(--border) bg-(--card) p-7 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 cursor-pointer scroll-animate anim-fade-up"
              [class]="'group relative flex flex-col rounded-xl border border-(--border) bg-(--card) p-7 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 cursor-pointer scroll-animate anim-fade-up delay-' + ((i % 4) + 1) + '00'"
            >
              <!-- Number -->
              <span class="font-mono text-xs font-semibold text-(--muted-foreground)/40 mb-4">
                /{{ (i + 1).toString().padStart(2, '0') }}
              </span>
              <div class="flex items-start justify-between gap-4 mb-3">
                <h3 class="font-display text-lg font-semibold text-(--foreground) group-hover:text-(--primary) transition-colors">
                  {{ service.title }}
                </h3>
                <span class="shrink-0 rounded-full bg-(--primary)/10 px-2.5 py-0.5 text-xs font-medium text-(--primary)">
                  {{ service.categoryName }}
                </span>
              </div>
              <p class="text-sm leading-relaxed text-(--muted-foreground) flex-1">
                {{ service.description }}
              </p>
              <ul class="mt-4 space-y-1.5">
                @for (benefit of service.benefits?.slice(0, 3); track benefit) {
                  <li class="flex items-start gap-2 text-xs text-(--muted-foreground)">
                    <lucide-icon [img]="CheckIcon" [size]="12" class="mt-0.5 text-(--primary) shrink-0"></lucide-icon>
                    {{ benefit }}
                  </li>
                }
              </ul>
              <div class="mt-5 flex items-end justify-between border-t border-(--border) pt-4">
                <div>
                  @if (service.currentOffer) {
                    <span class="font-display text-xl font-bold text-(--primary)">
                      {{ service.currentOffer.price | currency:'EUR':'symbol':'1.2-2':'fr' }}
                    </span>
                    <span class="text-xs text-(--muted-foreground) ml-1">
                      {{ service.currentOffer.durationType === 'ONE_TIME' ? 'unique' :
                         service.currentOffer.durationType === 'MONTHLY' ? '/ mois' :
                         service.currentOffer.durationType === 'YEARLY' ? '/ an' : '' }}
                    </span>
                  } @else {
                    <span class="font-display text-lg font-bold text-(--primary)">Sur devis</span>
                  }
                </div>
                <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary) transition-colors group-hover:bg-(--primary)/20">
                  <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
                </div>
              </div>
            </a>
          }
        </div>

        <!-- Fallback static services -->
        @if (featuredServices().length === 0 && !loadingFeatured()) {
          <div class="grid grid-cols-1 gap-5 sm:grid-cols-2">
            @for (service of fallbackServices; track service.subtitle; let i = $index) {
              <a
                routerLink="/services"
                class="group relative flex flex-col rounded-xl border border-(--border) bg-(--card) p-7 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 cursor-pointer scroll-animate anim-fade-up"
              >
                <span class="font-mono text-xs font-semibold text-(--muted-foreground)/40 mb-4">
                  /{{ (i + 1).toString().padStart(2, '0') }}
                </span>
                <h3 class="font-display text-lg font-semibold text-(--foreground) group-hover:text-(--primary) transition-colors mb-2">
                  {{ service.subtitle }}
                </h3>
                <p class="text-sm leading-relaxed text-(--muted-foreground) flex-1">
                  {{ service.description }}
                </p>
                <div class="mt-5 flex items-end justify-between border-t border-(--border) pt-4">
                  <span class="font-display text-xl font-bold text-(--primary)">{{ service.price }}</span>
                  <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary)">
                    <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
                  </div>
                </div>
              </a>
            }
          </div>
        }
      </div>
    </section>

    <!-- ===== ABOUT / STATS SECTION ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <!-- Left: Statement -->
          <div class="scroll-animate anim-fade-up">
            <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-4 block">
              Notre Histoire
            </span>
            <h2 class="font-display text-3xl sm:text-4xl font-bold text-(--foreground) leading-snug">
              Nous aidons les marques ambitieuses à
              <span class="italic font-light text-(--primary)">dominer</span>
              leur marché local depuis plus de 10 ans.
            </h2>
            <p class="mt-6 text-(--muted-foreground) leading-relaxed">
              Notre approche combine analyse de données, expertise humaine et compréhension
              profonde des enjeux locaux. Chaque stratégie est taillée sur mesure pour maximiser
              votre retour sur investissement.
            </p>
          </div>

          <!-- Right: Stats -->
          <div class="grid grid-cols-3 gap-4 scroll-animate anim-scale-in delay-200">
            @for (stat of aboutStats; track stat.label) {
              <div class="text-center rounded-xl border border-(--border) bg-(--card) p-6">
                <div class="font-display text-3xl sm:text-4xl font-bold text-(--primary)">{{ stat.value }}</div>
                <div class="mt-2 text-xs font-medium uppercase tracking-wider text-(--muted-foreground)">{{ stat.label }}</div>
              </div>
            }
          </div>
        </div>
      </div>
    </section>

    <!-- ===== PROCESS SECTION ===== -->
    <section class="border-b border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <div class="text-center mb-16 scroll-animate anim-fade-up">
          <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
            Notre Processus
          </span>
          <h2 class="font-display text-3xl sm:text-4xl font-bold text-(--foreground)">
            De l'audit à la croissance,
            <span class="italic font-light text-(--primary)">en 4 étapes</span>
          </h2>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          @for (step of processSteps; track step.title; let i = $index) {
            <div
              class="scroll-animate anim-fade-up relative rounded-xl border border-(--border) bg-(--card) p-6"
              [class]="'scroll-animate anim-fade-up relative rounded-xl border border-(--border) bg-(--card) p-6 delay-' + (i + 1) + '00'"
            >
              <!-- Step number -->
              <div class="font-mono text-5xl font-bold text-(--primary)/10 absolute top-4 right-5">
                {{ (i + 1).toString().padStart(2, '0') }}
              </div>
              <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary) mb-4">
                <lucide-icon [img]="step.icon" [size]="20"></lucide-icon>
              </div>
              <h3 class="font-display text-base font-semibold text-(--foreground) mb-2">{{ step.title }}</h3>
              <p class="text-sm leading-relaxed text-(--muted-foreground)">{{ step.description }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== TESTIMONIALS SECTION ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <div class="flex flex-col sm:flex-row sm:items-end sm:justify-between mb-16">
          <div class="scroll-animate anim-fade-up">
            <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
              Ce Qu'ils Disent
            </span>
            <h2 class="font-display text-3xl sm:text-4xl font-bold text-(--foreground)">
              Ils ont transformé<br />
              <span class="italic font-light text-(--primary)">leur visibilité</span>
            </h2>
          </div>
          <div class="mt-4 sm:mt-0 flex items-center gap-2 scroll-animate anim-fade-right delay-200">
            <button
              class="flex h-10 w-10 items-center justify-center rounded-full border border-(--border) text-(--muted-foreground) transition-colors hover:text-(--foreground) hover:border-(--primary)/30 cursor-pointer"
              (click)="prevTestimonial()"
              aria-label="Témoignage précédent"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button
              class="flex h-10 w-10 items-center justify-center rounded-full bg-(--primary) text-white transition-colors hover:bg-emerald-600 cursor-pointer"
              (click)="nextTestimonial()"
              aria-label="Témoignage suivant"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 gap-6 sm:grid-cols-3">
          @for (t of visibleTestimonials(); track t.name; let i = $index) {
            <div
              class="rounded-xl border border-(--border) bg-(--card) p-6 scroll-animate anim-fade-up"
              [class]="'rounded-xl border border-(--border) bg-(--card) p-6 scroll-animate anim-fade-up delay-' + (i + 1) + '00'"
            >
              <!-- Stars -->
              <div class="flex gap-0.5 text-amber-400 mb-4">
                @for (s of [1,2,3,4,5]; track s) {
                  <lucide-icon [img]="StarIcon" [size]="14" class="fill-current"></lucide-icon>
                }
              </div>
              <p class="text-sm leading-relaxed text-(--muted-foreground) italic">
                "{{ t.quote }}"
              </p>
              <div class="mt-5 flex items-center gap-3 border-t border-(--border) pt-4">
                <img
                  [src]="t.avatar"
                  [alt]="t.name"
                  class="h-10 w-10 rounded-full object-cover"
                />
                <div>
                  <div class="text-sm font-semibold text-(--foreground)">{{ t.name }}</div>
                  <div class="text-xs text-(--muted-foreground)">{{ t.role }}</div>
                </div>
              </div>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== FAQ SECTION ===== -->
    <section class="border-b border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 lg:grid-cols-5 gap-12">
          <!-- Left: Title -->
          <div class="lg:col-span-2 scroll-animate anim-fade-up">
            <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
              Questions Fréquentes
            </span>
            <h2 class="font-display text-3xl sm:text-4xl font-bold text-(--foreground) leading-snug">
              Tout ce que vous devez
              <span class="italic font-light text-(--primary)">savoir</span>
            </h2>
            <p class="mt-4 text-sm text-(--muted-foreground)">
              Vous ne trouvez pas votre réponse ?
              <a href="mailto:lmp.assistance@gmail.com" class="text-(--primary) hover:underline">Écrivez-nous</a>
            </p>
          </div>

          <!-- Right: Accordion -->
          <div class="lg:col-span-3 space-y-3 scroll-animate anim-fade-up delay-200">
            @for (faq of faqs; track faq.question; let i = $index) {
              <div class="rounded-xl border border-(--border) bg-(--card) overflow-hidden transition-colors"
                   [class.border-emerald-500/20]="openFaq() === i">
                <button
                  class="flex w-full items-center justify-between px-6 py-4 text-left cursor-pointer"
                  (click)="toggleFaq(i)"
                >
                  <span class="text-sm font-semibold text-(--foreground) pr-4">{{ faq.question }}</span>
                  <lucide-icon
                    [img]="ChevronDownIcon"
                    [size]="16"
                    class="shrink-0 text-(--muted-foreground) transition-transform duration-300"
                    [class.rotate-180]="openFaq() === i"
                  ></lucide-icon>
                </button>
                @if (openFaq() === i) {
                  <div class="px-6 pb-4">
                    <p class="text-sm leading-relaxed text-(--muted-foreground)">{{ faq.answer }}</p>
                  </div>
                }
              </div>
            }
          </div>
        </div>
      </div>
    </section>

    <!-- ===== CTA SECTION ===== -->
    <section class="relative overflow-hidden">
      <!-- Emerald gradient background -->
      <div class="absolute inset-0 bg-gradient-to-br from-emerald-600 via-emerald-500 to-teal-500"></div>
      <div
        class="pointer-events-none absolute inset-0 opacity-10"
        style="background-image: radial-gradient(circle, white 1px, transparent 1px); background-size: 32px 32px;"
      ></div>

      <div class="relative z-10 mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="mx-auto max-w-2xl text-center scroll-animate anim-fade-up">
          <h2 class="font-display text-3xl sm:text-4xl lg:text-5xl font-bold text-white leading-tight">
            Prêt à transformer<br />
            <span class="italic font-light">votre visibilité ?</span>
          </h2>
          <p class="mt-4 text-white/70 text-base">
            Discutons de vos objectifs. L'audit initial est offert et sans engagement.
          </p>
          <div class="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
            <button
              (click)="showAppointment.set(true)"
              class="group inline-flex items-center gap-2 rounded-full bg-white px-7 py-3.5 text-sm font-semibold text-emerald-700 transition-all duration-300 hover:bg-white/90 hover:shadow-lg cursor-pointer"
            >
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
              Réserver mon audit gratuit
              <lucide-icon [img]="ArrowRightIcon" [size]="16" class="transition-transform group-hover:translate-x-0.5"></lucide-icon>
            </button>
            <a
              href="mailto:lmp.assistance@gmail.com"
              class="inline-flex items-center gap-2 rounded-full border border-white/30 px-7 py-3.5 text-sm font-medium text-white transition-colors hover:bg-white/10 cursor-pointer"
            >
              Écrivez-nous →
            </a>
          </div>
        </div>
      </div>
    </section>

    <!-- Appointment Modal -->
    <lmp-appointment-modal
      [isOpen]="showAppointment()"
      (closed)="showAppointment.set(false)"
    />
  `,
})
export class HomeComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly catalogService = inject(CatalogService);
  private readonly seo = inject(SeoService);

  // Lucide Icons
  readonly ArrowRightIcon = ArrowRight;
  readonly CheckIcon = Check;
  readonly CalendarIcon = Calendar;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly SearchIcon = Search;
  readonly BarChart3Icon = BarChart3;
  readonly GlobeIcon = Globe;
  readonly MegaphoneIcon = Megaphone;
  readonly TargetIcon = Target;
  readonly LayersIcon = Layers;
  readonly RocketIcon = Rocket;
  readonly ChevronDownIcon = ChevronDown;
  readonly QuoteIcon = Quote;
  readonly ShieldIcon = Shield;
  readonly UsersIcon = Users;
  readonly AwardIcon = Award;
  readonly ZapIcon = Zap;
  readonly TrendingUpIcon = TrendingUp;
  readonly StarIcon = Star;

  readonly showAppointment = signal(false);
  readonly featuredServices = signal<ServiceItem[]>([]);
  readonly loadingFeatured = signal(true);
  readonly totalFeaturedCount = signal(0);
  readonly openFaq = signal<number | null>(null);
  readonly testimonialPage = signal(0);

  private scrollObserver?: IntersectionObserver;
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  // ── Hero ──
  readonly heroTags = ['SEO', 'Google Ads', 'Web', 'Google My Business', 'Branding'];

  // ── Partner Ticker ──
  readonly partnerLogos = [
    'Google Partner',
    'Meta Business',
    'Stripe Certified',
    'Trustpilot Verified',
    'HubSpot Partner',
    'Semrush Certified',
    'Google Analytics',
    'WordPress VIP',
  ];

  // ── About Stats ──
  readonly aboutStats = [
    { value: '500+', label: 'Clients Satisfaits' },
    { value: '10+', label: "Années d'Expérience" },
    { value: '98%', label: 'Satisfaction Client' },
  ];

  // ── Process Steps ──
  readonly processSteps = [
    { icon: Search, title: 'Audit & Analyse', description: 'Nous analysons votre présence digitale actuelle et identifions les opportunités de croissance.' },
    { icon: Target, title: 'Stratégie Sur Mesure', description: "Un plan d'action personnalisé basé sur vos objectifs et votre marché spécifique." },
    { icon: Rocket, title: 'Exécution & Optimisation', description: 'Mise en œuvre technique avec suivi continu et ajustements pour maximiser les résultats.' },
    { icon: TrendingUp, title: 'Résultats & Croissance', description: 'Rapports transparents et croissance mesurable de votre visibilité en ligne.' },
  ];

  // ── Testimonials ──
  readonly testimonials = [
    {
      quote: 'LMP a complètement changé notre approche locale. En 3 mois, nos appels entrants ont doublé. Le tableau de bord est clair et le support ultra réactif.',
      name: 'Marie Lefevre',
      role: 'Restauratrice, Bruxelles',
      avatar: 'https://i.pravatar.cc/80?u=marie-lefevre',
    },
    {
      quote: "Le service de gestion des avis m'a sauvé un temps précieux. Ma note moyenne est passée de 3.8 à 4.7 en quelques semaines. Indispensable pour mon activité.",
      name: 'Thomas Dubois',
      role: 'Institut de Beauté, Liège',
      avatar: 'https://i.pravatar.cc/80?u=thomas-dubois',
    },
    {
      quote: "L'équipe technique est impressionnante. Ils ont réglé un problème de fiche suspendue en 48h que je traînais depuis des mois. Merci pour l'efficacité.",
      name: 'Claire Vandenberghe',
      role: 'Cabinet Dentaire, Namur',
      avatar: 'https://i.pravatar.cc/80?u=claire-vandenberghe',
    },
    {
      quote: "Depuis que LMP gère notre référencement, nous sommes premiers sur Google Maps dans notre zone. Le retour sur investissement est exceptionnel.",
      name: 'Antoine Mercier',
      role: 'Garage Automobile, Charleroi',
      avatar: 'https://i.pravatar.cc/80?u=antoine-mercier',
    },
    {
      quote: "La création de notre site web a dépassé toutes nos attentes. L'intégration avec notre profil Google est parfaite et les résultats sont immédiats.",
      name: 'Sophie Renard',
      role: 'Fleuriste, Waterloo',
      avatar: 'https://i.pravatar.cc/80?u=sophie-renard',
    },
    {
      quote: "Un accompagnement personnalisé qui fait vraiment la différence. Pas de robots, des vraies réponses à nos questions et une stratégie sur mesure.",
      name: 'David Claessens',
      role: 'Boulangerie, Uccle',
      avatar: 'https://i.pravatar.cc/80?u=david-claessens',
    },
  ];

  visibleTestimonials = signal(this.testimonials.slice(0, 3));

  prevTestimonial(): void {
    const page = this.testimonialPage();
    const newPage = page === 0 ? Math.ceil(this.testimonials.length / 3) - 1 : page - 1;
    this.testimonialPage.set(newPage);
    this.visibleTestimonials.set(this.testimonials.slice(newPage * 3, newPage * 3 + 3));
  }

  nextTestimonial(): void {
    const page = this.testimonialPage();
    const maxPage = Math.ceil(this.testimonials.length / 3) - 1;
    const newPage = page >= maxPage ? 0 : page + 1;
    this.testimonialPage.set(newPage);
    this.visibleTestimonials.set(this.testimonials.slice(newPage * 3, newPage * 3 + 3));
  }

  // ── FAQ ──
  readonly faqs = [
    {
      question: 'Combien de temps faut-il pour voir des résultats SEO ?',
      answer: 'Les premiers résultats sont généralement visibles entre 4 et 8 semaines, selon votre secteur et la concurrence locale. Nous fournissons des rapports détaillés pour suivre votre progression en temps réel.',
    },
    {
      question: 'Est-ce que la consultation initiale est vraiment gratuite ?',
      answer: "Oui, l'audit initial est 100% gratuit et sans engagement. Nous analysons votre présence digitale actuelle et vous présentons les opportunités d'amélioration avec un plan d'action concret.",
    },
    {
      question: 'Quels types d\'entreprises accompagnez-vous ?',
      answer: 'Nous accompagnons tous types d\'entreprises locales : restaurants, commerces, cabinets médicaux, garages, instituts de beauté, artisans, et bien d\'autres. Notre expertise couvre toute la Belgique et l\'Europe francophone.',
    },
    {
      question: 'Comment fonctionne le paiement ?',
      answer: 'Nous proposons des paiements sécurisés via Stripe. Vous pouvez choisir un paiement unique ou un abonnement mensuel selon le service. Aucun frais caché — tout est transparent dès le départ.',
    },
    {
      question: 'Puis-je annuler mon abonnement à tout moment ?',
      answer: "Oui, tous nos abonnements sont sans engagement de durée. Vous pouvez annuler à tout moment depuis votre tableau de bord ou en nous contactant directement.",
    },
  ];

  toggleFaq(index: number): void {
    this.openFaq.set(this.openFaq() === index ? null : index);
  }

  // ── Fallback Services ──
  readonly fallbackServices = [
    {
      subtitle: 'Sécurisations et Accès Google My Business',
      description: 'Protégez et sécurisez votre profil Google My Business avec un accès propriétaire garanti.',
      price: '353,89 €',
    },
    {
      subtitle: 'Référencement Optimale VIP+',
      description: 'Service premium exclusif avec garantie de résultats exceptionnels et mots clés garantis.',
      price: '750,79 €',
    },
    {
      subtitle: 'Gestion des Avis',
      description: 'Améliorez votre réputation en ligne. Soyez le mieux noté de votre secteur.',
      price: '747,43 €',
    },
    {
      subtitle: 'Création Site Web',
      description: 'Site web professionnel avec protocole SSL et référencement optimisé.',
      price: '550,00 €',
    },
  ] as const;

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'LMP Digital Services — Marketing Digital & Référencement Local',
      description: 'Propulsez votre visibilité au sommet. Expertise en marketing digital, référencement SEO, Google My Business et création de sites web. 500+ clients satisfaits.',
      url: '/',
      keywords: 'marketing digital, référencement SEO, Google My Business, création site web, publicité en ligne, LMP, référencement local, Belgique, Bruxelles',
    });
    this.loadFeaturedServices();
  }

  private loadFeaturedServices(): void {
    this.loadingFeatured.set(true);
    this.catalogService.getFeaturedServices().subscribe({
      next: (services) => {
        const activeServices = services.filter(s => s.active);
        this.totalFeaturedCount.set(activeServices.length);
        this.featuredServices.set(activeServices.slice(0, 6));
        this.loadingFeatured.set(false);
        if (this.isBrowser) {
          setTimeout(() => {
            document.querySelectorAll('.scroll-animate:not(.animate-visible)').forEach((el) => {
              this.scrollObserver?.observe(el);
            });
          }, 50);
        }
      },
      error: () => {
        this.loadingFeatured.set(false);
        if (this.isBrowser) {
          setTimeout(() => {
            document.querySelectorAll('.scroll-animate:not(.animate-visible)').forEach((el) => {
              this.scrollObserver?.observe(el);
            });
          }, 50);
        }
      },
    });
  }

  ngAfterViewInit(): void {
    if (!this.isBrowser) return;

    // Scroll animation observer
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
  }
}
