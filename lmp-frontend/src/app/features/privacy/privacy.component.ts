import { Component, OnInit, inject } from '@angular/core';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'lmp-privacy',
  standalone: true,
  template: `
    <section class="mx-auto max-w-4xl px-4 py-20 sm:px-6 lg:px-8">
      <div class="text-center">
        <h1 class="font-display text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl">
          Politique de Confidentialité
        </h1>
        <p class="mt-4 text-lg text-(--muted-foreground)">
          Comment nous protégeons et utilisons vos données personnelles
        </p>
      </div>

      <!-- Info banner -->
      <div class="mt-10 rounded-xl border border-blue-500/20 bg-blue-500/5 p-6">
        <p class="text-sm font-semibold text-blue-400">Dernière mise à jour : Janvier 2025</p>
        <p class="mt-2 text-sm text-(--muted-foreground)">
          Cette politique de confidentialité explique comment
          <strong class="text-(--foreground)">LMP Local Map Profil</strong>
          collecte, utilise et protège vos informations personnelles.
        </p>
      </div>

      <div class="mt-10 space-y-10">
        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Présentation</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            Le site web <strong class="text-(--foreground)">LMP Local Map Profil</strong> est détenu par
            <strong class="text-(--foreground)">LMP Local Map Profil</strong>, qui est responsable du traitement de vos
            données personnelles. Nous avons adopté cette politique de confidentialité qui détermine comment nous traitons
            les informations collectées par LMP Local Map Profil, et qui explique également pourquoi nous devons collecter
            certaines données personnelles vous concernant.
          </p>
          <p class="mt-3 text-sm leading-relaxed text-(--muted-foreground)">
            Nous prenons soin de vos données personnelles et nous nous engageons à garantir leur confidentialité et leur sécurité.
          </p>
        </div>

        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Informations personnelles que nous collectons</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            Lorsque vous visitez LMP Local Map Profil, nous collectons automatiquement certaines informations sur votre
            appareil, notamment des informations sur votre navigateur web, votre adresse IP, votre fuseau horaire et
            certains des cookies installés sur votre appareil. De plus, lorsque vous naviguez sur le site, nous collectons
            des informations sur les pages web individuelles ou les produits que vous consultez, les sites web ou les termes
            de recherche qui vous ont redirigé vers le site.
          </p>
        </div>

        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Utilisation des données</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            Nous utilisons les informations que nous collectons pour optimiser notre site web, améliorer nos services,
            et communiquer avec vous lorsque cela est nécessaire. Nous ne partageons pas vos informations personnelles
            avec des tiers sans votre consentement explicite.
          </p>
        </div>

        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Vos droits (RGPD)</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            Si vous êtes un résident européen, vous disposez des droits suivants relatifs à vos données personnelles :
            le droit d'être informé, le droit d'accès, le droit de rectification, le droit à l'effacement,
            le droit de restreindre le traitement, le droit à la portabilité des données, le droit d'opposition.
          </p>
        </div>

        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Contact</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            Pour toute question concernant cette politique de confidentialité, vous pouvez nous contacter à
            <a href="mailto:lmp.assistance@gmail.com" class="text-blue-400 hover:underline">lmp.assistance&#64;gmail.com</a>.
          </p>
        </div>
      </div>
    </section>
  `,
})
export class PrivacyComponent implements OnInit {
  private readonly seo = inject(SeoService);

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'Politique de Confidentialité',
      description: 'Politique de confidentialité de LMP Digital Services. Découvrez comment nous protégeons et utilisons vos données personnelles conformément au RGPD.',
      url: '/privacy',
    });
  }
}
