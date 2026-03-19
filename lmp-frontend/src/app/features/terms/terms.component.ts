import { Component, OnInit, inject } from '@angular/core';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'lmp-terms',
  standalone: true,
  template: `
    <section class="mx-auto max-w-4xl px-4 py-20 sm:px-6 lg:px-8">
      <div class="text-center">
        <h1 class="font-display text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl">
          Conditions d'Utilisation
        </h1>
        <p class="mt-4 text-lg text-(--muted-foreground)">
          Règles et réglementations pour l'utilisation de notre site web
        </p>
      </div>

      <!-- Info banner -->
      <div class="mt-10 rounded-xl border border-blue-500/20 bg-blue-500/5 p-6">
        <p class="text-sm font-semibold text-blue-400">Dernière mise à jour : Janvier 2025</p>
        <p class="mt-2 text-sm text-(--muted-foreground)">
          Ces conditions d'utilisation décrivent les règles et réglementations pour l'utilisation du site web de
          <strong class="text-(--foreground)">LMP Local Map Profil</strong>.
        </p>
      </div>

      <div class="mt-10 space-y-10">
        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Acceptation des conditions</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            En accédant à ce site web, nous supposons que vous acceptez ces conditions d'utilisation.
            Si vous n'acceptez pas toutes les conditions d'utilisation énoncées sur cette page, veuillez ne pas
            continuer à utiliser LMP Local Map Profil.
          </p>
        </div>

        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Propriété intellectuelle</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            Sauf indication contraire, LMP Local Map Profil et/ou ses concédants de licence détiennent les droits
            de propriété intellectuelle pour l'ensemble du matériel sur LMP Local Map Profil. Tous les droits de
            propriété intellectuelle sont réservés. Vous pouvez y accéder à partir de LMP Local Map Profil pour
            votre usage personnel, sous réserve des restrictions définies dans ces conditions.
          </p>
        </div>

        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Restrictions</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            Il vous est expressément interdit de : publier du matériel du site web dans un autre média,
            vendre ou commercialiser du matériel du site web, reproduire ou copier le matériel du site web,
            redistribuer le contenu du site web sans notre consentement.
          </p>
        </div>

        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Limitation de responsabilité</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            En aucun cas, LMP Local Map Profil, ni aucun de ses dirigeants, administrateurs et employés, ne sera
            tenu responsable de quoi que ce soit découlant de ou de quelque manière que ce soit lié à votre
            utilisation de ce site web.
          </p>
        </div>

        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Droit applicable</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            Ces conditions seront régies et interprétées conformément aux lois du Royaume de Belgique,
            et vous vous soumettez à la juridiction non exclusive des tribunaux belges pour la résolution
            de tout litige.
          </p>
        </div>

        <div>
          <h2 class="font-display text-2xl font-bold text-(--foreground)">Contact</h2>
          <p class="mt-4 text-sm leading-relaxed text-(--muted-foreground)">
            Pour toute question concernant ces conditions d'utilisation, vous pouvez nous contacter à
            <a href="mailto:lmp.assistance@gmail.com" class="text-blue-400 hover:underline">lmp.assistance&#64;gmail.com</a>.
          </p>
        </div>
      </div>
    </section>
  `,
})
export class TermsComponent implements OnInit {
  private readonly seo = inject(SeoService);

  ngOnInit(): void {
    this.seo.updateMeta({
      title: "Conditions d'Utilisation",
      description: "Conditions d'utilisation du site LMP Digital Services. Règles et réglementations pour l'utilisation de notre plateforme de marketing digital.",
      url: '/terms',
    });
  }
}
