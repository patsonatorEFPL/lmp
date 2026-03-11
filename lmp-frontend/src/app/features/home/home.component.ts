import {
  Component,
  signal,
  AfterViewInit,
  ElementRef,
  ViewChild,
  OnDestroy,
  Inject,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  ArrowRight,
  Check,
  TrendingUp,
  Star,
  Clock,
  Award,
  Calendar,
} from 'lucide-angular';
import { AppointmentModalComponent } from '../../shared/modals/appointment-modal.component';

@Component({
  selector: 'lmp-home',
  standalone: true,
  imports: [RouterLink, LucideAngularModule, AppointmentModalComponent],
  template: `
    <!-- ===== HERO SECTION ===== -->
    <section
      #heroSection
      class="relative overflow-hidden min-h-screen -mt-16 pt-16 flex items-center group cursor-default"
    >
      <!-- Background image (office from login page) with overlay -->
      <div class="absolute inset-0">
        <img
          src="/images/hero-bg-office.jpg"
          alt="Modern corporate office with glass walls"
          class="absolute inset-0 w-full h-full object-cover"
        />
        <div class="absolute inset-0 bg-gradient-to-br from-[#0a0e1a]/90 via-[#0f1629]/85 to-[#1a1040]/80"></div>
      </div>

      <!-- Dot grid pattern -->
      <div
        class="pointer-events-none absolute inset-0"
        style="background-image: radial-gradient(circle, rgba(99,102,241,0.18) 1px, transparent 1px); background-size: 36px 36px; mask-image: radial-gradient(80% 80%, black 40%, transparent 100%);"
      ></div>

      <!-- Animated particles -->
      @for (p of particles; track $index) {
        <div
          class="hero-particle"
          [style.left.%]="p.x"
          [style.top.%]="p.y"
          [style.width.px]="p.size"
          [style.height.px]="p.size"
          [style.animation-duration]="p.duration + 's'"
          [style.animation-delay]="p.delay + 's'"
        ></div>
      }

      <!-- Mouse tracking glow -->
      <div
        #heroGlow
        class="absolute w-[800px] h-[800px] rounded-full blur-[120px] -translate-x-1/2 -translate-y-1/2 pointer-events-none opacity-0 transition-opacity duration-700 group-hover:opacity-100 mix-blend-screen z-0"
        style="background: linear-gradient(to right, rgba(37,99,235,0.3), rgba(99,102,241,0.3));"
      ></div>

      <div class="relative z-10 mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8 w-full">
        <div class="grid grid-cols-1 items-center gap-12 lg:grid-cols-2">
          <!-- Left column: Text -->
          <div>
            <!-- Update badge -->
            <div
              class="scroll-animate anim-fade-up mb-6 inline-flex items-center gap-2 rounded-full border border-blue-500/25 bg-blue-500/10 backdrop-blur-sm px-4 py-1.5 text-xs font-medium text-blue-300"
            >
              <span class="h-2 w-2 rounded-full bg-emerald-400 animate-pulse"></span>
              Nouvelle mise à jour Google 2026
            </div>

            <!-- Main heading with typewriter -->
            <h1
              class="scroll-animate anim-fade-up delay-100 font-display text-4xl font-bold tracking-tight text-white sm:text-5xl lg:text-6xl leading-tight"
            >
              <span #typeSpan1></span>
              <span #typeSpan2 class="hero-gradient-text"></span>
              <br />
              <span class="text-gray-300 font-normal text-3xl sm:text-4xl lg:text-5xl">
                L'excellence locale.
              </span>
            </h1>

            <p
              class="scroll-animate anim-fade-up delay-200 mt-6 max-w-lg text-base leading-relaxed text-gray-400 sm:text-lg"
            >
              Propulsez votre visibilité au sommet. Technologie de pointe et expertise
              humaine pour dominer votre marché local.
            </p>

            <!-- Email input + CTA -->
            <div
              class="scroll-animate anim-fade-up delay-300 mt-8 relative max-w-md"
            >
              <div
                class="absolute -inset-[2px] rounded-2xl bg-gradient-to-r from-blue-600/50 to-purple-600/50 opacity-0 blur-[10px] transition-opacity duration-400 group-hover:opacity-100 -z-10"
              ></div>
              <div class="flex items-center rounded-xl border border-white/10 bg-[#0f172a]/80 backdrop-blur-sm p-1.5">
                <input
                  type="email"
                  placeholder="votre@email.com"
                  class="flex-1 bg-transparent px-4 py-2.5 text-sm text-white placeholder:text-gray-500 outline-none"
                />
                <button
                  class="flex items-center gap-2 rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-blue-700 cursor-pointer"
                >
                  Débuter
                  <svg class="h-4 w-4 transition-transform group-hover:translate-x-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                  </svg>
                </button>
              </div>
            </div>

            <!-- Trust badges -->
            <div class="scroll-animate anim-fade-up delay-400 mt-6 flex flex-wrap items-center gap-5 text-xs text-gray-400">
              <span class="flex items-center gap-1.5">
                <lucide-icon [img]="CheckIcon" [size]="14" class="text-emerald-500"></lucide-icon>
                Pas de frais cachés
              </span>
              <span class="flex items-center gap-1.5">
                <span class="text-blue-400">🛡️</span>
                Paiement sécurisé
              </span>
              <span class="flex items-center gap-1.5">
                <span class="text-gray-300">⚡</span>
                Mise en place rapide
              </span>
            </div>
          </div>

          <!-- Right column: Floating Stats over background -->
          <div class="relative flex items-center justify-center scroll-animate anim-scale-in delay-200">



          </div>
        </div>
      </div>
    </section>

    <!-- ===== STATS SECTION ===== -->
    <section class="relative border-t border-white/5 bg-gradient-to-b from-[#0f1629] to-(--background)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="text-center mb-12 scroll-animate anim-fade-up">
          <div class="inline-flex items-center gap-2 rounded-full border border-blue-500/20 bg-blue-500/10 px-4 py-1.5 text-xs font-medium text-blue-400 mb-4">
            <span class="h-1.5 w-1.5 rounded-full bg-blue-400"></span>
            CHIFFRES CLÉS
          </div>
          <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl">
            Des résultats qui
            <span class="hero-gradient-text">parlent d'eux-mêmes</span>
          </h2>
        </div>
        <div class="grid grid-cols-2 gap-6 sm:grid-cols-4">
          @for (stat of stats; track stat.label; let i = $index) {
            <div
              class="scroll-animate anim-scale-in rounded-xl border border-(--border) bg-(--card) p-6 text-center"
              [class]="'scroll-animate anim-scale-in delay-' + (i + 1) + '00'"
            >
              <div class="flex h-10 w-10 mx-auto items-center justify-center rounded-xl mb-3" [style.background]="stat.iconBg">
                <span class="text-lg">{{ stat.icon }}</span>
              </div>
              <div class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl">{{ stat.value }}</div>
              <div class="mt-1 text-sm font-medium text-(--foreground)">{{ stat.label }}</div>
              <div class="text-xs text-(--muted-foreground)">{{ stat.sub }}</div>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== SERVICES SECTION ===== -->
    <section class="border-t border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="text-center mb-14 scroll-animate anim-fade-up">
          <div class="inline-flex items-center gap-2 rounded-full border border-blue-500/20 bg-blue-500/10 px-4 py-1.5 text-xs font-medium text-blue-400 mb-4">
            <span class="h-1.5 w-1.5 rounded-full bg-blue-400"></span>
            NOS SERVICES
          </div>
          <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl">
            Solutions
            <span class="hero-gradient-text">Premium</span>
          </h2>
          <p class="mt-4 mx-auto max-w-2xl text-(--muted-foreground)">
            Une gamme complète d'outils et de services pour dominer votre marché local et national.
          </p>
        </div>

        <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          @for (service of homeServices; track service.subtitle; let i = $index) {
            <div
              class="group flex flex-col rounded-xl border border-(--border) bg-(--card) p-6 transition-all duration-300 hover:border-blue-500/30 hover:shadow-lg hover:shadow-blue-500/5 scroll-animate anim-fade-up"
              [class]="'group flex flex-col rounded-xl border border-(--border) bg-(--card) p-6 transition-all duration-300 hover:border-blue-500/30 hover:shadow-lg hover:shadow-blue-500/5 scroll-animate anim-fade-up delay-' + ((i % 3) + 1) + '00'"
            >
              <div class="flex items-start justify-between mb-4">
                <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-500/10 text-xl">
                  {{ service.emoji }}
                </div>
                <span class="rounded-full bg-blue-500/10 px-2.5 py-0.5 text-xs font-medium text-blue-400">
                  {{ service.category }}
                </span>
              </div>
              <h3 class="font-display text-base font-semibold text-(--foreground)">
                {{ service.subtitle }}
              </h3>
              <p class="mt-2 flex-1 text-sm leading-relaxed text-(--muted-foreground)">
                {{ service.description }}
              </p>
              <ul class="mt-4 space-y-1.5">
                @for (feat of service.features; track feat) {
                  <li class="flex items-start gap-2 text-xs text-(--muted-foreground)">
                    <lucide-icon [img]="CheckIcon" [size]="12" class="mt-0.5 text-emerald-500 shrink-0"></lucide-icon>
                    {{ feat }}
                  </li>
                }
              </ul>
              <div class="mt-5 flex items-end justify-between border-t border-(--border) pt-4">
                <div>
                  <span class="font-display text-xl font-bold text-(--primary)">{{ service.price }}</span>
                  <div class="text-xs text-(--muted-foreground)">Paiement unique</div>
                </div>
                <button class="flex h-8 w-8 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary) transition-colors hover:bg-(--primary)/20 cursor-pointer">
                  <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
                  </svg>
                </button>
              </div>
            </div>
          }
        </div>

        <div class="mt-10 text-center scroll-animate anim-fade-up">
          <a
            routerLink="/services"
            class="inline-flex items-center gap-2 rounded-xl border border-(--border) bg-(--card) px-6 py-3 text-sm font-medium text-(--foreground) transition-colors hover:bg-(--accent) cursor-pointer"
          >
            Voir tous les services
            <lucide-icon [img]="ArrowRightIcon" [size]="16"></lucide-icon>
          </a>
        </div>
      </div>
    </section>

    <!-- ===== EXPERTISE SECTION ===== -->
    <section class="border-t border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="mx-auto max-w-3xl text-center mb-14 scroll-animate anim-blur-in">
          <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl">
            L'Expertise au service de votre croissance.
          </h2>
          <p class="mt-4 text-(--muted-foreground)">
            Depuis plus de 10 ans, nous redéfinissons les standards du marketing local.
            Une approche data-driven combinée à une compréhension humaine de vos enjeux.
          </p>
        </div>
        <div class="grid grid-cols-1 gap-6 sm:grid-cols-3">
          @for (exp of expertise; track exp.title; let i = $index) {
            <div
              class="rounded-xl border border-(--border) bg-(--card) p-6 scroll-animate anim-slide-rotate"
              [class]="'rounded-xl border border-(--border) bg-(--card) p-6 scroll-animate anim-slide-rotate delay-' + (i + 1) + '00'"
            >
              <h3 class="font-display text-lg font-semibold text-(--foreground)">{{ exp.title }}</h3>
              <p class="mt-3 text-sm leading-relaxed text-(--muted-foreground)">{{ exp.description }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== TESTIMONIALS SECTION ===== -->
    <section class="border-t border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="flex items-end justify-between mb-14">
          <div class="scroll-animate anim-fade-left">
            <div class="inline-flex items-center gap-2 rounded-full border border-blue-500/20 bg-blue-500/10 px-4 py-1.5 text-xs font-medium text-blue-400 mb-4">
              💬 500+ Clients Heureux
            </div>
            <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl">
              Ils ont transformé<br />
              <span class="hero-gradient-text">leur visibilité</span>
            </h2>
          </div>
          <div class="hidden sm:flex items-center gap-2 scroll-animate anim-fade-right">
            <button
              class="flex h-10 w-10 items-center justify-center rounded-full border border-(--border) text-(--muted-foreground) transition-colors hover:text-(--foreground) cursor-pointer"
              (click)="prevTestimonial()"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button
              class="flex h-10 w-10 items-center justify-center rounded-full bg-(--primary) text-white transition-colors hover:bg-(--primary)/90 cursor-pointer"
              (click)="nextTestimonial()"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 gap-6 sm:grid-cols-3">
          @for (t of testimonials; track t.name; let i = $index) {
            <div
              class="rounded-xl border border-(--border) bg-(--card) p-6 scroll-animate anim-flip-y"
              [class]="'rounded-xl border border-(--border) bg-(--card) p-6 scroll-animate anim-flip-y delay-' + (i + 1) + '00'"
            >
              <div class="flex gap-0.5 text-white/80 mb-4">
                @for (s of [1,2,3,4,5]; track s) {
                  <span>★</span>
                }
              </div>
              <p class="text-sm leading-relaxed text-(--muted-foreground)">
                "{{ t.quote }}"
              </p>
              <div class="mt-5 flex items-center gap-3">
                <div
                  class="flex h-10 w-10 items-center justify-center rounded-full text-sm font-bold text-white"
                  [style.background]="t.color"
                >
                  {{ t.initials }}
                </div>
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

    <!-- ===== CTA SECTION ===== -->
    <section class="border-t border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="mx-auto max-w-2xl text-center scroll-animate anim-slide-up">
          <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl lg:text-5xl">
            Prêt pour l'étape suivante ?
          </h2>
          <p class="mt-4 text-(--muted-foreground)">
            Discutons de vos objectifs. L'audit initial est offert et sans engagement.
          </p>
          <div class="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
            <button
              (click)="showAppointment.set(true)"
              class="inline-flex items-center gap-2 rounded-xl bg-(--primary) px-6 py-3 text-sm font-semibold text-white transition-all duration-300 hover:bg-blue-700 hover:scale-105 hover:shadow-lg hover:shadow-blue-500/25 cursor-pointer"
            >
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
              Réserver mon audit
            </button>
            <a
              href="mailto:lmp.assistance@gmail.com"
              class="inline-flex items-center gap-2 rounded-xl border border-(--border) bg-(--card) px-6 py-3 text-sm font-medium text-(--foreground) transition-colors hover:bg-(--accent) cursor-pointer"
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
export class HomeComponent implements AfterViewInit, OnDestroy {
  @ViewChild('heroSection') heroSectionRef!: ElementRef<HTMLElement>;
  @ViewChild('heroGlow') heroGlowRef!: ElementRef<HTMLElement>;
  @ViewChild('typeSpan1') typeSpan1Ref!: ElementRef<HTMLElement>;
  @ViewChild('typeSpan2') typeSpan2Ref!: ElementRef<HTMLElement>;

  readonly ArrowRightIcon = ArrowRight;
  readonly CheckIcon = Check;
  readonly TrendingUpIcon = TrendingUp;
  readonly StarIcon = Star;
  readonly ClockIcon = Clock;
  readonly AwardIcon = Award;
  readonly CalendarIcon = Calendar;

  readonly showAppointment = signal(false);

  private scrollObserver?: IntersectionObserver;
  private mouseMoveHandler?: (e: MouseEvent) => void;
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  // Particles for hero background
  readonly particles = Array.from({ length: 12 }, () => ({
    x: Math.random() * 90 + 5,
    y: Math.random() * 80 + 10,
    size: Math.random() * 4 + 2,
    duration: Math.random() * 4 + 4,
    delay: Math.random() * 3,
  }));

  readonly stats = [
    { value: '499+', label: 'Clients satisfaits', sub: 'en Europe', icon: '📊', iconBg: 'rgba(99,102,241,0.15)' },
    { value: '97%', label: 'Taux de rétention', sub: 'Fidèles depuis 2020', icon: '📈', iconBg: 'rgba(34,197,94,0.15)' },
    { value: '24/7', label: 'Support Expert', sub: 'Disponible à tout moment', icon: '🎧', iconBg: 'rgba(59,130,246,0.15)' },
    { value: '9+', label: "Années d'expérience", sub: 'Certifié Google', icon: '👤', iconBg: 'rgba(99,102,241,0.15)' },
  ];

  readonly homeServices = [
    {
      emoji: '📍',
      category: 'Référencement Local',
      subtitle: 'Sécurisations et Accès Google My Business',
      description: 'Protégez et sécurisez votre profil Google My Business avec un accès propriétaire garanti.',
      features: [
        'Remise des accès propriétaire principal',
        'Synchronisation du profil Google avec votre courriel',
        'Sécurisation du Google My Business',
      ],
      price: '353,89 €',
    },
    {
      emoji: '⭐',
      category: 'Référencement Premium',
      subtitle: 'Référencement Optimale avec Sécurisations Garantie VIP+',
      description: 'Service premium exclusif avec garantie de résultats exceptionnels.',
      features: [
        '8 mots clés garantis',
        '8 zones de services',
        'Remise et sécurisation des accès garanti',
        'Validation du profil Google',
      ],
      price: '750,79 €',
    },
    {
      emoji: '💬',
      category: 'Réputation en Ligne',
      subtitle: 'Gestion des Avis',
      description: 'Améliorez votre réputation en ligne avec notre service de gestion des avis.',
      features: [
        'Soyez le mieux noté de votre secteur',
        'Gérez les avis indésirables',
        'Recevez plus d\'avis positifs',
      ],
      price: '747,43 €',
    },
    {
      emoji: '🏪',
      category: 'Marketing Local',
      subtitle: 'Présence Locales',
      description: 'Boostez votre visibilité locale avec une présence optimisée dans tous les annuaires locaux.',
      features: [
        'Montez dans les résultats de la carte Google',
        'Boostez votre visibilité dans votre ville',
        'Visible dans tous les annuaires locaux',
      ],
      price: '2 200,00 €',
    },
    {
      emoji: '💻',
      category: 'Développement Web',
      subtitle: 'Création Site Web',
      description: 'Création de site web professionnel avec protocole SSL, référencement optimisé.',
      features: [
        'Création + promotion avec protocole SSL',
        'Référencement parmi les meilleurs résultats',
        'Boost des pages (publicité)',
      ],
      price: '550,00 €',
    },
    {
      emoji: '🔍',
      category: 'SEO Avancé',
      subtitle: 'SEO et Référencement Naturel',
      description: 'Service SEO complet pour améliorer votre positionnement sur les moteurs de recherche.',
      features: [
        'Améliorez votre positionnement sur les moteurs',
        'Mise à jour régulière des données GMB',
        'Résultats garantis pour Google Maps',
      ],
      price: '5 500,00 €',
    },
    {
      emoji: '🚀',
      category: 'Google Premium',
      subtitle: 'Mise à jour 2026',
      description: 'Solution complète et révolutionnaire intégrant tous nos services premium.',
      features: [
        'Tous les autres services inclus',
        'Sécurisation garantie',
        'Immatriculation du profil Google',
        'Validation du profil Google',
        'Fusion des profils doubles',
      ],
      price: '1 000,00 €',
    },
  ];

  readonly expertise = [
    {
      title: 'Certifié & Reconnu',
      description: 'Partenaire privilégié des plateformes majeures, nous maîtrisons les algorithmes pour garantir votre conformité.',
    },
    {
      title: 'Focus ROI',
      description: 'Chaque action est mesurée. Des rapports transparents et détaillés pour suivre votre évolution jour après jour.',
    },
    {
      title: 'Humain avant tout',
      description: 'Un expert dédié vous accompagne personnellement. Pas de robots, des vraies réponses et une stratégie sur mesure.',
    },
  ];

  readonly testimonials = [
    {
      quote: 'LMP a complètement changé notre approche locale. En 3 mois, nos appels entrants ont doublé. Le tableau de bord est clair et le support ultra réactif.',
      name: 'Jean Dupont',
      role: 'Restaurateur, Bruxelles',
      initials: 'JD',
      color: '#22c55e',
    },
    {
      quote: "Le service de gestion des avis m'a sauvé un temps précieux. Je ne m'occupe plus de rien, et ma note moyenne est passée de 3.8 à 4.7. Indispensable.",
      name: 'Sophie Martin',
      role: 'Institut de Beauté, Liège',
      initials: 'SM',
      color: '#6366f1',
    },
    {
      quote: "L'équipe technique est impressionnante. Ils ont réglé un problème de fiche suspendue en 48h que je traînais depuis des mois. Merci pour l'efficacité.",
      name: 'Pierre Lambert',
      role: 'Garage Automobile, Namur',
      initials: 'PL',
      color: '#3b82f6',
    },
  ];

  prevTestimonial(): void {}
  nextTestimonial(): void {}

  ngAfterViewInit(): void {
    if (!this.isBrowser) return;

    // 1. Scroll animation observer
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

    // 2. Mouse tracking glow on hero section
    const heroSection = this.heroSectionRef?.nativeElement;
    const heroGlow = this.heroGlowRef?.nativeElement;
    if (heroSection && heroGlow) {
      this.mouseMoveHandler = (e: MouseEvent) => {
        const rect = heroSection.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        requestAnimationFrame(() => {
          heroGlow.style.left = `${x}px`;
          heroGlow.style.top = `${y}px`;
        });
      };
      heroSection.addEventListener('mousemove', this.mouseMoveHandler);
    }

    // 3. Typewriter effect
    this.runTypewriter();
  }

  private runTypewriter(): void {
    const span1 = this.typeSpan1Ref?.nativeElement;
    const span2 = this.typeSpan2Ref?.nativeElement;
    if (!span1 || !span2) return;

    const text1 = 'LMP ';
    const text2 = 'Marketing Digital';
    const cursor = document.createElement('span');
    cursor.className = 'typewriter-cursor';

    span1.appendChild(cursor);
    let i = 0;
    let j = 0;
    let phase1 = true;

    const type = () => {
      if (phase1) {
        if (i < text1.length) {
          span1.insertBefore(document.createTextNode(text1.charAt(i)), cursor);
          i++;
          setTimeout(type, 70 + Math.random() * 50);
        } else {
          phase1 = false;
          span1.removeChild(cursor);
          span2.appendChild(cursor);
          setTimeout(type, 300);
        }
      } else {
        if (j < text2.length) {
          span2.insertBefore(document.createTextNode(text2.charAt(j)), cursor);
          j++;
          setTimeout(type, 70 + Math.random() * 50);
        } else {
          setTimeout(() => {
            cursor.style.display = 'none';
          }, 2000);
        }
      }
    };

    setTimeout(type, 800);
  }

  ngOnDestroy(): void {
    this.scrollObserver?.disconnect();
    if (this.mouseMoveHandler && this.heroSectionRef?.nativeElement) {
      this.heroSectionRef.nativeElement.removeEventListener('mousemove', this.mouseMoveHandler);
    }
  }
}
