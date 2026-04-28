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
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="mx-auto max-w-3xl text-center">
          <h1 class="text-4xl sm:text-5xl font-bold tracking-tight text-(--foreground) leading-tight">
            L'excellence locale, depuis 2016
          </h1>
          <p class="mt-4 text-base text-(--muted-foreground) max-w-xl mx-auto leading-relaxed">
            Découvrez la mission, la vision et l'équipe qui propulsent
            des centaines d'entreprises au sommet de leur marché.
          </p>
        </div>
      </div>
    </section>

    <!-- ===== MISSION & VISION ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 items-start gap-12 lg:grid-cols-2">
          <div class="scroll-animate">
            <h2 class="text-2xl font-bold text-(--foreground) sm:text-3xl">
              Connecter les entreprises locales à leur audience
            </h2>
            <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
              <strong class="text-(--foreground)">LMP</strong> est une plateforme qui relie les utilisateurs aux
              solutions et aux acteurs les plus pertinents pour leurs besoins, partout dans le monde. Notre mission est
              de faciliter la découverte et l'accès aux services, aux entreprises et aux opportunités qui comptent pour eux.
            </p>

            <div class="mt-10">
              <h2 class="text-2xl font-bold text-(--foreground) sm:text-3xl">
                Un monde où chaque entreprise rayonne
              </h2>
              <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
                Nous imaginons un monde où chaque personne peut facilement trouver et accéder
                aux ressources qui répondent à ses besoins, créant ainsi des communautés
                plus connectées et prospères.
              </p>
            </div>
          </div>

          <!-- Values cards -->
          <div class="space-y-3 scroll-animate">
            @for (value of values; track value.title; let i = $index) {
              <div
                class="flex items-start gap-3 rounded-sm border border-(--border) bg-(--card) p-4 scroll-animate"
                [style.transition-delay.ms]="(i + 1) * 100"
              >
                <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-sm bg-(--muted)">
                  <lucide-icon [img]="value.icon" [size]="18" class="text-(--primary)"></lucide-icon>
                </div>
                <div>
                  <h3 class="text-sm font-semibold text-(--foreground)">{{ value.title }}</h3>
                  <p class="mt-1 text-sm text-(--muted-foreground) leading-relaxed">{{ value.description }}</p>
                </div>
              </div>
            }
          </div>
        </div>
      </div>
    </section>

    <!-- ===== STATS BAR ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="scroll-animate mb-10">
          <h2 class="text-2xl font-bold text-(--foreground)">
            En chiffres
          </h2>
        </div>

        <div class="grid grid-cols-2 gap-4 sm:grid-cols-4 scroll-animate">
          @for (stat of aboutStats; track stat.label; let i = $index) {
            <div class="text-center rounded-sm border border-(--border) bg-(--card) p-5">
              <div class="text-3xl font-bold text-(--primary)">{{ stat.value }}</div>
              <div class="mt-1 text-sm font-medium text-(--foreground)">{{ stat.label }}</div>
              <div class="text-xs text-(--muted-foreground)">{{ stat.sub }}</div>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== WHY LMP ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="mb-10 scroll-animate">
          <h2 class="text-2xl font-bold text-(--foreground)">
            Ce qui nous rend différents
          </h2>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          @for (diff of differentiators; track diff.title; let i = $index) {
            <div
              class="rounded-sm border border-(--border) bg-(--card) p-6 scroll-animate"
              [style.transition-delay.ms]="(i + 1) * 100"
            >
              <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--muted) text-(--primary) mb-3">
                <lucide-icon [img]="diff.icon" [size]="18"></lucide-icon>
              </div>
              <h3 class="text-sm font-semibold text-(--foreground) mb-1.5">{{ diff.title }}</h3>
              <p class="text-sm leading-relaxed text-(--muted-foreground)">{{ diff.description }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== CTA SECTION ===== -->
    <section class="border-b border-(--border) bg-(--card)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="mx-auto max-w-2xl text-center scroll-animate">
          <h2 class="text-2xl sm:text-3xl font-bold text-(--foreground)">
            Prêt à rejoindre nos clients satisfaits ?
          </h2>
          <p class="mt-3 text-sm text-(--muted-foreground)">
            Contactez-nous pour une consultation gratuite et découvrez comment nous pouvons booster votre visibilité.
          </p>
          <div class="mt-6 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
            <button
              (click)="showAppointment.set(true)"
              class="inline-flex items-center gap-2 rounded-sm bg-(--primary) px-5 py-2.5 text-sm font-medium text-(--primary-foreground) transition-colors hover:opacity-90 cursor-pointer"
            >
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
              Réserver un audit gratuit
            </button>
            <a
              routerLink="/contact"
              class="inline-flex items-center gap-1.5 rounded-sm border border-(--border) px-5 py-2.5 text-sm font-medium text-(--foreground) transition-colors hover:bg-(--accent) cursor-pointer"
            >
              Nous contacter
              <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
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
      keywords: 'à propos LMP, marketing local, expertise SEO, équipe marketing digital, référencement local Québec, Canada',
    });

    this.seo.setJsonLd({
      '@context': 'https://schema.org',
      '@type': 'AboutPage',
      name: 'À Propos — LMP Digital Services',
      url: 'https://lmp-services.ca/about',
      description: 'Découvrez LMP Digital Services : notre mission, notre vision et notre équipe d\'experts en marketing digital et référencement local.',
      mainEntity: {
        '@type': 'Organization',
        name: 'LMP Digital Services',
        url: 'https://lmp-services.ca',
        logo: 'https://lmp-services.ca/images/logo-lmp.webp',
        foundingDate: '2016',
        sameAs: [
          'https://www.facebook.com/lmpservices',
          'https://www.linkedin.com/company/lmp-digital-services',
        ],
      },
    });
  }

  ngAfterViewInit(): void {
    if (!this.isBrowser) return;

    this.scrollObserver = new IntersectionObserver(
      (entries) => entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('animate-visible');
        } else {
          entry.target.classList.remove('animate-visible');
        }
      }),
      { threshold: 0.1, rootMargin: '0px 0px -50px 0px' },
    );

    document.querySelectorAll('.scroll-animate').forEach((el) => this.scrollObserver!.observe(el));
  }

  ngOnDestroy(): void {
    this.scrollObserver?.disconnect();
  }
}
