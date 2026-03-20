import { Component, AfterViewInit, OnDestroy, OnInit, Inject, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Lightbulb, Target, Users, Award, TrendingUp, ArrowRight, Calendar } from 'lucide-angular';
import { SeoService } from '../../core/services/seo.service';
import { AppointmentModalComponent } from '../../shared/modals/appointment-modal.component';
import { signal } from '@angular/core';

@Component({
  selector: 'lmp-about',
  standalone: true,
  imports: [RouterLink, LucideAngularModule, AppointmentModalComponent],
  template: `
    <!-- ===== HERO SECTION ===== -->
    <section class="relative overflow-hidden border-b border-(--border)">
      <div
        class="pointer-events-none absolute inset-0 opacity-[0.03]"
        style="background-image: radial-gradient(circle, currentColor 1px, transparent 1px); background-size: 32px 32px;"
      ></div>

      <div class="relative mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8 sm:py-32">
        <div class="mx-auto max-w-3xl text-center scroll-animate anim-fade-up">
          <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-4 block">
            Notre Histoire
          </span>
          <h1 class="font-display text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight text-(--foreground) leading-tight">
            L'excellence locale,
            <span class="italic font-light text-(--primary)">depuis 2016</span>
          </h1>
          <p class="mt-6 text-lg text-(--muted-foreground) max-w-xl mx-auto leading-relaxed">
            Découvrez la mission, la vision et l'équipe qui propulsent
            des centaines d'entreprises au sommet de leur marché.
          </p>
        </div>
      </div>
    </section>

    <!-- ===== MISSION & VISION ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 items-start gap-16 lg:grid-cols-2">
          <!-- Left: Mission & Vision -->
          <div class="scroll-animate anim-fade-up">
            <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
              /01 — Mission
            </span>
            <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl leading-snug">
              Connecter les entreprises locales à
              <span class="italic font-light text-(--primary)">leur audience</span>
            </h2>
            <p class="mt-6 text-base leading-relaxed text-(--muted-foreground)">
              <strong class="text-(--foreground)">LMP</strong> (Local Map Profil) est une plateforme innovante qui
              connecte les utilisateurs aux meilleures solutions locales. Notre mission est de faciliter
              la découverte et l'accès aux services, entreprises et opportunités qui vous entourent.
            </p>

            <div class="mt-12">
              <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
                /02 — Vision
              </span>
              <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl leading-snug">
                Un monde où chaque entreprise
                <span class="italic font-light text-(--primary)">rayonne</span>
              </h2>
              <p class="mt-6 text-base leading-relaxed text-(--muted-foreground)">
                Nous imaginons un monde où chaque personne peut facilement trouver et accéder
                aux ressources locales qui répondent à ses besoins, créant ainsi des communautés
                plus connectées et prospères.
              </p>
            </div>
          </div>

          <!-- Right: Values cards -->
          <div class="space-y-4 scroll-animate anim-fade-up delay-200">
            <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-4 block">
              /03 — Nos Valeurs
            </span>
            @for (value of values; track value.title; let i = $index) {
              <div
                class="flex items-start gap-4 rounded-xl border border-(--border) bg-(--card) p-5 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 scroll-animate anim-scale-in"
                [class]="'flex items-start gap-4 rounded-xl border border-(--border) bg-(--card) p-5 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 scroll-animate anim-scale-in delay-' + (i + 1) + '00'"
              >
                <!-- Number -->
                <div class="flex flex-col items-center gap-2">
                  <span class="font-mono text-xs font-semibold text-(--muted-foreground)/40">
                    /{{ (i + 1).toString().padStart(2, '0') }}
                  </span>
                  <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-(--primary)/10">
                    <lucide-icon [img]="value.icon" [size]="20" class="text-(--primary)"></lucide-icon>
                  </div>
                </div>
                <div>
                  <h3 class="font-display text-base font-semibold text-(--foreground)">{{ value.title }}</h3>
                  <p class="mt-1.5 text-sm text-(--muted-foreground) leading-relaxed">{{ value.description }}</p>
                </div>
              </div>
            }
          </div>
        </div>
      </div>
    </section>

    <!-- ===== STATS BAR ===== -->
    <section class="border-b border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="scroll-animate anim-fade-up mb-12 text-center">
          <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
            En Chiffres
          </span>
          <h2 class="font-display text-3xl sm:text-4xl font-bold text-(--foreground)">
            Des résultats qui
            <span class="italic font-light text-(--primary)">parlent</span>
          </h2>
        </div>

        <div class="grid grid-cols-2 gap-4 sm:grid-cols-4 scroll-animate anim-scale-in delay-200">
          @for (stat of aboutStats; track stat.label; let i = $index) {
            <div
              class="text-center rounded-xl border border-(--border) bg-(--card) p-6 transition-all duration-300 hover:border-(--primary)/30"
            >
              <div class="font-display text-3xl sm:text-4xl font-bold text-(--primary)">{{ stat.value }}</div>
              <div class="mt-2 text-sm font-medium text-(--foreground)">{{ stat.label }}</div>
              <div class="text-xs text-(--muted-foreground)">{{ stat.sub }}</div>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== WHY LMP ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <div class="text-center mb-16 scroll-animate anim-fade-up">
          <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
            Pourquoi LMP
          </span>
          <h2 class="font-display text-3xl sm:text-4xl font-bold text-(--foreground)">
            Ce qui nous rend
            <span class="italic font-light text-(--primary)">différents</span>
          </h2>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-6">
          @for (diff of differentiators; track diff.title; let i = $index) {
            <div
              class="group rounded-xl border border-(--border) bg-(--card) p-7 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 scroll-animate anim-fade-up"
              [class]="'group rounded-xl border border-(--border) bg-(--card) p-7 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 scroll-animate anim-fade-up delay-' + (i + 1) + '00'"
            >
              <div class="font-mono text-5xl font-bold text-(--primary)/10 mb-4">
                {{ (i + 1).toString().padStart(2, '0') }}
              </div>
              <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary) mb-4">
                <lucide-icon [img]="diff.icon" [size]="20"></lucide-icon>
              </div>
              <h3 class="font-display text-lg font-semibold text-(--foreground) mb-2">{{ diff.title }}</h3>
              <p class="text-sm leading-relaxed text-(--muted-foreground)">{{ diff.description }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== CTA SECTION ===== -->
    <section class="relative overflow-hidden">
      <div class="absolute inset-0 bg-gradient-to-br from-emerald-600 via-emerald-500 to-teal-500"></div>
      <div
        class="pointer-events-none absolute inset-0 opacity-10"
        style="background-image: radial-gradient(circle, white 1px, transparent 1px); background-size: 32px 32px;"
      ></div>

      <div class="relative z-10 mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="mx-auto max-w-2xl text-center scroll-animate anim-fade-up">
          <h2 class="font-display text-3xl sm:text-4xl lg:text-5xl font-bold text-white leading-tight">
            Prêt à rejoindre nos<br />
            <span class="italic font-light">clients satisfaits ?</span>
          </h2>
          <p class="mt-4 text-white/70 text-base">
            Contactez-nous pour une consultation gratuite et découvrez comment nous pouvons booster votre visibilité.
          </p>
          <div class="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
            <button
              (click)="showAppointment.set(true)"
              class="group inline-flex items-center gap-2 rounded-full bg-white px-7 py-3.5 text-sm font-semibold text-emerald-700 transition-all duration-300 hover:bg-white/90 hover:shadow-lg cursor-pointer"
            >
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
              Réserver un audit gratuit
              <lucide-icon [img]="ArrowRightIcon" [size]="16" class="transition-transform group-hover:translate-x-0.5"></lucide-icon>
            </button>
            <a
              routerLink="/contact"
              class="inline-flex items-center gap-2 rounded-full border border-white/30 px-7 py-3.5 text-sm font-medium text-white transition-colors hover:bg-white/10 cursor-pointer"
            >
              Nous contacter →
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
export class AboutComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly seo = inject(SeoService);
  readonly showAppointment = signal(false);

  readonly ArrowRightIcon = ArrowRight;
  readonly CalendarIcon = Calendar;

  private scrollObserver?: IntersectionObserver;
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  readonly values = [
    { icon: Lightbulb, title: 'Innovation Continue', description: 'Nous adoptons les dernières technologies et tendances pour garder nos clients en avance sur la concurrence.' },
    { icon: Target, title: 'Résultats Mesurables', description: 'Chaque stratégie est pilotée par les données. Nous mesurons et optimisons chaque action pour maximiser votre ROI.' },
    { icon: Users, title: 'Approche Humaine', description: 'Un expert dédié à votre projet, disponible et réactif. Nous construisons des relations de confiance durables.' },
    { icon: Award, title: 'Excellence Certifiée', description: 'Partenaire certifié Google et des principales plateformes digitales. Notre expertise est reconnue et validée.' },
  ];

  readonly aboutStats = [
    { value: '500+', label: 'Clients', sub: 'en Europe' },
    { value: '98%', label: 'Satisfaction', sub: 'clients fidèles' },
    { value: '10+', label: 'Années', sub: "d'expérience" },
    { value: '24/7', label: 'Support', sub: 'disponible' },
  ];

  readonly differentiators = [
    { icon: TrendingUp, title: 'Focus ROI', description: 'Chaque euro investi est tracé et optimisé. Nous nous engageons sur des résultats mesurables et un retour sur investissement concret.' },
    { icon: Award, title: 'Certifié & Reconnu', description: 'Partenaire certifié Google, Meta Business et des principales plateformes. Notre expertise est validée par les leaders du marché.' },
    { icon: Users, title: 'Humain Avant Tout', description: 'Pas de robots, pas de réponses automatiques. Un expert dédié qui connaît votre marché et comprend vos enjeux.' },
  ];

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'À Propos — Expertise Marketing Local depuis 10 ans',
      description: 'Découvrez LMP Digital Services : notre mission, notre vision et notre équipe d\'experts en marketing digital et référencement local. 500+ clients satisfaits en Europe.',
      url: '/about',
      keywords: 'à propos LMP, marketing local, expertise SEO, équipe marketing digital, référencement local Belgique',
    });
  }

  ngAfterViewInit(): void {
    if (!this.isBrowser) return;

    this.scrollObserver = new IntersectionObserver(
      (entries) => entries.forEach((entry) => {
        if (entry.isIntersecting) entry.target.classList.add('animate-visible');
      }),
      { threshold: 0.1, rootMargin: '0px 0px -50px 0px' },
    );

    document.querySelectorAll('.scroll-animate').forEach((el) => this.scrollObserver!.observe(el));
  }

  ngOnDestroy(): void {
    this.scrollObserver?.disconnect();
  }
}
