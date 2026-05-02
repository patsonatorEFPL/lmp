import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SiteConfigService } from '../../core/services/site-config.service';

@Component({
  selector: 'lmp-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="border-t border-(--border) bg-(--card)">
      <div class="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 gap-10 sm:grid-cols-2 lg:grid-cols-3 items-start">
          <!-- Brand -->
          <div>
            <a routerLink="/" class="flex items-center gap-3">
              <img src="/images/logo-lmp.webp" alt="LMP Logo" class="h-8 w-auto rounded-xs" />
              <span class="text-base font-semibold text-(--foreground)">LMP</span>
            </a>
            <p class="mt-3 max-w-xs text-sm leading-relaxed text-(--muted-foreground)">
              Votre partenaire de confiance pour la visibilité locale et le
              marketing digital.
            </p>
            <!-- Social Icons -->
            <div class="mt-4 flex items-center gap-2">
              <a
                href="#"
                aria-label="Twitter / X"
                class="flex h-8 w-8 items-center justify-center rounded-sm border border-(--border) text-(--muted-foreground) transition-colors hover:text-(--foreground)"
              >
                <svg class="h-3.5 w-3.5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M22.46 6c-.77.35-1.6.58-2.46.69.88-.53 1.56-1.37 1.88-2.38-.83.5-1.75.85-2.72 1.05C18.37 4.5 17.26 4 16 4c-2.35 0-4.27 1.92-4.27 4.29 0 .34.04.67.11.98C8.28 9.09 5.11 7.38 3 4.79c-.37.63-.58 1.37-.58 2.15 0 1.49.75 2.81 1.91 3.56-.71 0-1.37-.2-1.95-.5v.03c0 2.08 1.48 3.82 3.44 4.21a4.22 4.22 0 0 1-1.93.07 4.28 4.28 0 0 0 4 2.98 8.521 8.521 0 0 1-5.33 1.84c-.34 0-.68-.02-1.02-.06C3.44 20.29 5.7 21 8.12 21 16 21 20.33 14.46 20.33 8.79c0-.19 0-.37-.01-.56.84-.6 1.56-1.36 2.14-2.23z" />
                </svg>
              </a>
              <a
                href="#"
                aria-label="LinkedIn"
                class="flex h-8 w-8 items-center justify-center rounded-sm border border-(--border) text-(--muted-foreground) transition-colors hover:text-(--foreground)"
              >
                <svg class="h-3.5 w-3.5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z" />
                </svg>
              </a>
              <a
                href="#"
                aria-label="Instagram"
                class="flex h-8 w-8 items-center justify-center rounded-sm border border-(--border) text-(--muted-foreground) transition-colors hover:text-(--foreground)"
              >
                <svg class="h-3.5 w-3.5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 1 0 0 12.324 6.162 6.162 0 0 0 0-12.324zM12 16a4 4 0 1 1 0-8 4 4 0 0 1 0 8zm6.406-11.845a1.44 1.44 0 1 0 0 2.881 1.44 1.44 0 0 0 0-2.881z" />
                </svg>
              </a>
            </div>
          </div>

          <!-- Explorer -->
          <div>
            <h3 class="text-sm font-semibold text-(--foreground)">
              Explorer
            </h3>
            <ul class="mt-3 space-y-2">
              @for (link of exploreLinks; track link.path) {
                <li>
                  <a
                    [routerLink]="link.path"
                    class="text-sm text-(--muted-foreground) transition-colors duration-150 hover:text-(--foreground)"
                  >
                    {{ link.label }}
                  </a>
                </li>
              }
            </ul>
          </div>

          <!-- Contact -->
          <div>
            <h3 class="text-sm font-semibold text-(--foreground)">
              Contact
            </h3>
            <ul class="mt-3 space-y-2">
              <li class="flex items-center gap-2 text-sm text-(--muted-foreground)">
                <svg class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" />
                </svg>
                <a [href]="'mailto:' + siteConfig.supportEmail" class="hover:text-(--foreground) transition-colors">
                  {{ siteConfig.supportEmail }}
                </a>
              </li>
              <li class="flex items-start gap-2 text-sm text-(--muted-foreground)">
                <svg class="mt-0.5 h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z" />
                </svg>
                <span>
                  1085 Rue de la Rivière,<br />
                  Québec, QC G1Y 2A3, Canada
                </span>
              </li>
              <li class="flex items-center gap-2 text-sm text-(--muted-foreground)">
                <svg class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>Lun - Ven: 9h00 - 18h00</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- Bottom strip -->
        <div class="mt-8 flex flex-col items-center justify-between gap-3 border-t border-(--border) pt-6 sm:flex-row">
          <p class="text-xs text-(--muted-foreground)">
            © 2026 LMP. Tous droits réservés.
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
          </div>
        </div>
      </div>
    </footer>
  `,
})
export class FooterComponent {
  readonly siteConfig = inject(SiteConfigService);

  readonly exploreLinks = [
    { path: '/', label: 'Accueil' },
    { path: '/services', label: 'Nos Services' },
    { path: '/map', label: 'Carte Interactive' },
    { path: '/about', label: 'À propos' },
  ];
}
