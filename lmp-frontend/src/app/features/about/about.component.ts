import { Component, AfterViewInit, OnDestroy, OnInit, Inject, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Lightbulb, Target, Users, Award, TrendingUp } from 'lucide-angular';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'lmp-about',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  template: `
    <section class="relative">
      <div class="relative mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <!-- Hero -->
        <div class="mx-auto max-w-3xl text-center scroll-animate anim-fade-up">
          <div class="mb-4 inline-flex items-center gap-2 rounded-full border border-blue-500/20 bg-blue-500/10 px-4 py-1.5 text-xs font-medium text-blue-400">
            <span class="inline-block h-1.5 w-1.5 rounded-full bg-blue-400"></span>
            Notre Histoire
          </div>
          <h1 class="font-display text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl">
            À Propos de
            <span class="hero-gradient-text">LMP</span>
          </h1>
          <p class="mt-4 text-lg text-(--muted-foreground)">
            Découvrez notre mission, notre vision et l'équipe qui fait la différence.
          </p>
        </div>

        <!-- Mission + Vision grid -->
        <div class="mt-16 grid grid-cols-1 items-start gap-12 lg:grid-cols-2">
          <!-- Left: Mission -->
          <div class="scroll-animate anim-fade-left">
            <h2 class="font-display text-2xl font-bold text-(--foreground) sm:text-3xl">
              Notre Mission Marketing Local
            </h2>
            <p class="mt-4 text-base leading-relaxed text-(--muted-foreground)">
              <span class="font-semibold text-blue-400">LMP</span> (Local Map Profil) est une plateforme innovante qui
              connecte les utilisateurs aux meilleures solutions locales. Notre mission est de faciliter
              la découverte et l'accès aux services, entreprises et opportunités qui vous entourent.
            </p>

            <h2 class="mt-10 font-display text-2xl font-bold text-(--foreground) sm:text-3xl">
              Vision SEO Local
            </h2>
            <p class="mt-4 text-base leading-relaxed text-(--muted-foreground)">
              Nous imaginons un monde où chaque personne peut facilement trouver et accéder
              aux ressources locales qui répondent à ses besoins, créant ainsi des communautés
              plus connectées et prospères.
            </p>
          </div>

          <!-- Right: Values cards -->
          <div class="space-y-4 scroll-animate anim-fade-right">
            @for (value of values; track value.title; let i = $index) {
              <div
                class="flex items-start gap-4 rounded-xl border border-(--border) bg-(--card) p-5 transition-all duration-200 hover:border-blue-500/30 scroll-animate anim-scale-in"
                [class]="'flex items-start gap-4 rounded-xl border border-(--border) bg-(--card) p-5 transition-all duration-200 hover:border-blue-500/30 scroll-animate anim-scale-in delay-' + (i + 1) + '00'"
              >
                <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-blue-500/10">
                  <lucide-icon [img]="value.icon" [size]="20" class="text-blue-400"></lucide-icon>
                </div>
                <div>
                  <h3 class="font-display text-base font-semibold text-(--foreground)">{{ value.title }}</h3>
                  <p class="mt-1.5 text-sm text-(--muted-foreground) leading-relaxed">{{ value.description }}</p>
                </div>
              </div>
            }
          </div>
        </div>

        <!-- Stats bar -->
        <div class="mt-20 rounded-2xl border border-(--border) bg-(--card) p-8 sm:p-12 scroll-animate anim-blur-in">
          <div class="grid grid-cols-2 gap-8 sm:grid-cols-4 text-center">
            @for (stat of aboutStats; track stat.label; let i = $index) {
              <div class="scroll-animate anim-fade-up" [class]="'scroll-animate anim-fade-up delay-' + (i + 1) + '00'">
                <div class="font-display text-3xl font-bold text-(--primary) sm:text-4xl">{{ stat.value }}</div>
                <div class="mt-1 text-sm font-medium text-(--foreground)">{{ stat.label }}</div>
                <div class="text-xs text-(--muted-foreground)">{{ stat.sub }}</div>
              </div>
            }
          </div>
        </div>

        <!-- CTA -->
        <div class="mt-20 text-center scroll-animate anim-slide-up">
          <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl">
            Prêt à rejoindre nos clients satisfaits ?
          </h2>
          <p class="mt-4 mx-auto max-w-xl text-(--muted-foreground)">
            Contactez-nous pour une consultation gratuite et découvrez comment nous pouvons booster votre visibilité.
          </p>
          <div class="mt-8">
            <a
              routerLink="/contact"
              class="inline-flex items-center gap-2 rounded-xl bg-(--primary) px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-blue-700 cursor-pointer"
            >
              Nous contacter
            </a>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class AboutComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly seo = inject(SeoService);

  readonly LightbulbIcon = Lightbulb;
  readonly TargetIcon = Target;
  readonly UsersIcon = Users;
  readonly AwardIcon = Award;
  readonly TrendingUpIcon = TrendingUp;

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

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'À Propos — Expertise Marketing Local depuis 10 ans',
      description: 'Découvrez LMP Digital Services : notre mission, notre vision et notre équipe d\'experts en marketing digital et référencement local. 500+ clients satisfaits en Europe.',
      url: '/about',
      keywords: 'à propos LMP, marketing local, expertise SEO, équipe marketing digital, référencement local Belgique',
    });
  }

  readonly aboutStats = [
    { value: '500+', label: 'Clients', sub: 'en Europe' },
    { value: '98%', label: 'Satisfaction', sub: 'clients fidèles' },
    { value: '10+', label: 'Années', sub: "d'expérience" },
    { value: '24/7', label: 'Support', sub: 'disponible' },
  ];

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
