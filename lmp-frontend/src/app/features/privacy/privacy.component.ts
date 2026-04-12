import { Component, OnInit, inject, AfterViewInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'lmp-privacy',
  standalone: true,
  template: `
    <section class="mx-auto max-w-4xl px-4 py-20 sm:px-6 lg:px-8">
      <div class="text-center">
        <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-4 block">
          Légal
        </span>
        <h1 class="text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl">
          Politique de Confidentialité
        </h1>
        <p class="mt-4 text-lg text-(--muted-foreground)">
          Comment nous protégeons et utilisons vos données personnelles
        </p>
      </div>

      <!-- Info banner -->
      <div class="mt-10 rounded-sm border border-(--primary)/20 bg-(--primary)/5 p-6 scroll-animate anim-fade-up delay-100">
        <p class="text-sm font-semibold text-(--primary)">Dernière mise à jour : {{ lastUpdated }}</p>
        <p class="mt-2 text-sm text-(--muted-foreground)">
          Cette politique de confidentialité explique comment
          <strong class="text-(--foreground)">LMP</strong>
          collecte, utilise et protège vos informations personnelles.
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
export class PrivacyComponent implements OnInit, AfterViewInit, OnDestroy {
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
      title: 'Présentation',
      content: `Le site web <strong class="text-(--foreground)">LMP</strong> est détenu par
        <strong class="text-(--foreground)">LMP</strong>, qui est responsable du traitement de vos
        données personnelles. Nous avons adopté cette politique de confidentialité qui détermine comment nous traitons
        les informations collectées par LMP, et qui explique également pourquoi nous devons collecter
        certaines données personnelles vous concernant.<br/><br/>
        Nous prenons soin de vos données personnelles et nous nous engageons à garantir leur confidentialité et leur sécurité.`,
    },
    {
      title: 'Informations personnelles que nous collectons',
      content: `Lorsque vous visitez LMP, nous collectons automatiquement certaines informations sur votre
        appareil, notamment des informations sur votre navigateur web, votre adresse IP, votre fuseau horaire et
        certains des cookies installés sur votre appareil. De plus, lorsque vous naviguez sur le site, nous collectons
        des informations sur les pages web individuelles ou les produits que vous consultez, les sites web ou les termes
        de recherche qui vous ont redirigé vers le site.`,
    },
    {
      title: 'Géolocalisation',
      content: `Lors du processus de paiement, nous pouvons vous demander d'autoriser l'accès à votre position
        géographique via votre navigateur. Cette demande est <strong class="text-(--foreground)">entièrement facultative</strong>
        — vous pouvez la refuser sans aucune incidence sur votre achat.<br/><br/>
        Si vous l'acceptez, <strong class="text-(--foreground)">seul votre pays</strong> est déterminé et enregistré.
        Nous ne conservons ni vos coordonnées GPS, ni votre adresse exacte, ni aucune donnée de localisation précise.
        Cette information est utilisée exclusivement pour la <strong class="text-(--foreground)">prévention de la fraude</strong>
        et la vérification de cohérence fiscale (TVA), conformément à notre intérêt légitime au sens de l'article 6(1)(f) du RGPD.<br/><br/>
        Le pays détecté est conservé uniquement le temps nécessaire au traitement de votre commande et à nos obligations légales.`,
    },
    {
      title: 'Utilisation des données',
      content: `Nous utilisons les informations que nous collectons pour optimiser notre site web, améliorer nos services,
        prévenir la fraude lors des paiements, et communiquer avec vous lorsque cela est nécessaire.
        Nous ne partageons pas vos informations personnelles avec des tiers sans votre consentement explicite.`,
    },
    {
      title: 'Vos droits (RGPD)',
      content: `Si vous êtes un résident européen, vous disposez des droits suivants relatifs à vos données personnelles :
        le droit d'être informé, le droit d'accès, le droit de rectification, le droit à l'effacement,
        le droit de restreindre le traitement, le droit à la portabilité des données, le droit d'opposition.`,
    },
    {
      title: 'Contact',
      content: `Pour toute question concernant cette politique de confidentialité, vous pouvez nous contacter à
        <a href="mailto:lmp.assistance@gmail.com" class="text-(--primary) hover:underline">lmp.assistance&#64;gmail.com</a>.`,
    },
  ];

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'Politique de Confidentialité',
      description: 'Politique de confidentialité de LMP Digital Services. Découvrez comment nous protégeons et utilisons vos données personnelles conformément au RGPD.',
      url: '/privacy',
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
