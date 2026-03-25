import { Component, OnInit, inject, AfterViewInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'lmp-terms',
  standalone: true,
  template: `
    <section class="mx-auto max-w-4xl px-4 py-20 sm:px-6 lg:px-8">
      <div class="text-center">
        <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-4 block">
          Légal
        </span>
        <h1 class="text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl">
          Conditions d'Utilisation
        </h1>
        <p class="mt-4 text-lg text-(--muted-foreground)">
          Règles et réglementations pour l'utilisation de notre site web
        </p>
      </div>

      <!-- Info banner -->
      <div class="mt-10 rounded-sm border border-(--primary)/20 bg-(--primary)/5 p-6 scroll-animate anim-fade-up delay-100">
        <p class="text-sm font-semibold text-(--primary)">Dernière mise à jour : {{ lastUpdated }}</p>
        <p class="mt-2 text-sm text-(--muted-foreground)">
          Ces conditions d'utilisation décrivent les règles et réglementations pour l'utilisation du site web de
          <strong class="text-(--foreground)">LMP Local Map Profil</strong>.
        </p>
      </div>

      <div class="mt-10 space-y-10">
        @for (section of sections; track section.title; let i = $index) {
          <div class="scroll-animate anim-fade-up" [style.transition-delay.ms]="(i + 1) * 100">
            <h2 class="text-2xl font-bold text-(--foreground)">{{ section.title }}</h2>
            <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)" [innerHTML]="section.content"></p>
          </div>
        }
      </div>
    </section>
  `,
})
export class TermsComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly seo = inject(SeoService);
  private isBrowser: boolean;
  private scrollObserver?: IntersectionObserver;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  readonly lastUpdated = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' })
    .format(new Date())
    .replace(/^./, (c) => c.toUpperCase());

  readonly sections = [
    {
      title: 'Acceptation des conditions',
      content: `En accédant à ce site web, nous supposons que vous acceptez ces conditions d'utilisation.
        Si vous n'acceptez pas toutes les conditions d'utilisation énoncées sur cette page, veuillez ne pas
        continuer à utiliser LMP Local Map Profil.`,
    },
    {
      title: 'Propriété intellectuelle',
      content: `Sauf indication contraire, LMP Local Map Profil et/ou ses concédants de licence détiennent les droits
        de propriété intellectuelle pour l'ensemble du matériel sur LMP Local Map Profil. Tous les droits de
        propriété intellectuelle sont réservés. Vous pouvez y accéder à partir de LMP Local Map Profil pour
        votre usage personnel, sous réserve des restrictions définies dans ces conditions.`,
    },
    {
      title: 'Restrictions',
      content: `Il vous est expressément interdit de : publier du matériel du site web dans un autre média,
        vendre ou commercialiser du matériel du site web, reproduire ou copier le matériel du site web,
        redistribuer le contenu du site web sans notre consentement.`,
    },
    {
      title: 'Limitation de responsabilité',
      content: `En aucun cas, LMP Local Map Profil, ni aucun de ses dirigeants, administrateurs et employés, ne sera
        tenu responsable de quoi que ce soit découlant de ou de quelque manière que ce soit lié à votre
        utilisation de ce site web.`,
    },
    {
      title: 'Droit applicable',
      content: `Ces conditions seront régies et interprétées conformément aux lois du Royaume de Belgique,
        et vous vous soumettez à la juridiction non exclusive des tribunaux belges pour la résolution
        de tout litige.`,
    },
    {
      title: 'Contact',
      content: `Pour toute question concernant ces conditions d'utilisation, vous pouvez nous contacter à
        <a href="mailto:lmp.assistance@gmail.com" class="text-(--primary) hover:underline">lmp.assistance&#64;gmail.com</a>.`,
    },
  ];

  ngOnInit(): void {
    this.seo.updateMeta({
      title: "Conditions d'Utilisation",
      description: "Conditions d'utilisation du site LMP Digital Services. Règles et réglementations pour l'utilisation de notre plateforme de marketing digital.",
      url: '/terms',
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
