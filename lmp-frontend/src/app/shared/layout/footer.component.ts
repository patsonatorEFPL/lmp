import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'lmp-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="border-t border-(--border) bg-(--card)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <!-- Top grid: Location, Links, Socials -->
        <div class="grid grid-cols-1 gap-12 sm:grid-cols-3">
          <!-- Location + Contact -->
          <div>
            <h3 class="text-xs font-semibold uppercase tracking-wider text-(--muted-foreground)">
              Contact
            </h3>
            <ul class="mt-4 space-y-3">
              <li class="text-sm text-(--foreground)">
                <a href="mailto:lmp.assistance@gmail.com" class="hover:text-(--primary) transition-colors">
                  lmp.assistance&#64;gmail.com
                </a>
              </li>
              <li class="text-sm text-(--muted-foreground)">
                Rue Gatti De Gamond 97,<br />
                1180 Uccle, Belgique
              </li>
              <li class="text-sm text-(--muted-foreground)">
                Lun - Ven: 9h00 - 18h00
              </li>
            </ul>
          </div>

          <!-- Links -->
          <div>
            <h3 class="text-xs font-semibold uppercase tracking-wider text-(--muted-foreground)">
              Liens
            </h3>
            <ul class="mt-4 space-y-3">
              @for (link of exploreLinks; track link.path) {
                <li>
                  <a
                    [routerLink]="link.path"
                    class="text-sm font-medium text-(--foreground) transition-colors hover:text-(--primary)"
                  >
                    {{ link.label }}
                  </a>
                </li>
              }
            </ul>
          </div>

          <!-- Socials -->
          <div>
            <h3 class="text-xs font-semibold uppercase tracking-wider text-(--muted-foreground)">
              Réseaux
            </h3>
            <ul class="mt-4 space-y-3">
              <li>
                <a href="#" class="text-sm font-medium text-(--foreground) transition-colors hover:text-(--primary)">Instagram</a>
              </li>
              <li>
                <a href="#" class="text-sm font-medium text-(--foreground) transition-colors hover:text-(--primary)">LinkedIn</a>
              </li>
              <li>
                <a href="#" class="text-sm font-medium text-(--foreground) transition-colors hover:text-(--primary)">Facebook</a>
              </li>
              <li>
                <a href="#" class="text-sm font-medium text-(--foreground) transition-colors hover:text-(--primary)">X (Twitter)</a>
              </li>
            </ul>
          </div>
        </div>

        <!-- Large brand text -->
        <div class="mt-16 overflow-hidden">
          <h2
            class="font-editorial-italic text-[8rem] sm:text-[12rem] lg:text-[16rem] font-bold leading-none text-(--foreground)/5 select-none tracking-tighter text-center"
          >
            lmp
          </h2>
        </div>

        <!-- Bottom strip -->
        <div class="mt-4 flex flex-col items-center justify-between gap-4 border-t border-(--border) pt-6 pb-2 sm:flex-row">
          <p class="text-xs text-(--muted-foreground)">
            © 2026 LMP (Local Map Profil). Tous droits réservés.
          </p>
          <div class="flex gap-6">
            <a
              routerLink="/privacy"
              class="text-xs text-(--muted-foreground) transition-colors hover:text-(--foreground)"
            >
              Politique de confidentialité
            </a>
            <a
              routerLink="/terms"
              class="text-xs text-(--muted-foreground) transition-colors hover:text-(--foreground)"
            >
              Conditions d'utilisation
            </a>
            <a
              href="#"
              class="text-xs text-(--muted-foreground) transition-colors hover:text-(--foreground)"
            >
              Retour en haut ↑
            </a>
          </div>
        </div>
      </div>
    </footer>
  `,
})
export class FooterComponent {
  readonly exploreLinks = [
    { path: '/', label: 'Accueil' },
    { path: '/services', label: 'Nos Services' },
    { path: '/map', label: 'Carte Interactive' },
    { path: '/about', label: 'À propos' },
    { path: '/contact', label: 'Contact' },
  ];
}
