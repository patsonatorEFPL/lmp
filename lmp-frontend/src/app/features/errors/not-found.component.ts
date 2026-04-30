import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, ArrowLeft, Home, Search, LifeBuoy } from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';

@Component({
  selector: 'lmp-not-found',
  standalone: true,
  imports: [RouterLink, LucideAngularModule, HlmButton],
  template: `
    <section class="relative isolate overflow-hidden min-h-[calc(100vh-3.5rem)] flex items-center bg-(--background)">
      <!-- Aurora backdrop (clin d'œil au hero home) -->
      <div class="pointer-events-none absolute inset-0 -z-10" aria-hidden="true">
        <div class="absolute -top-32 -left-24 h-[28rem] w-[28rem] rounded-full blur-3xl opacity-40"
             style="background: radial-gradient(closest-side, oklch(0.62 0.22 258 / 0.55), transparent 70%);"></div>
        <div class="absolute -bottom-40 -right-24 h-[32rem] w-[32rem] rounded-full blur-3xl opacity-40"
             style="background: radial-gradient(closest-side, oklch(0.66 0.20 285 / 0.45), transparent 70%);"></div>
        <div class="absolute inset-0"
             style="background-image: linear-gradient(to right, color-mix(in oklab, var(--border) 60%, transparent) 1px, transparent 1px), linear-gradient(to bottom, color-mix(in oklab, var(--border) 60%, transparent) 1px, transparent 1px); background-size: 56px 56px; mask-image: radial-gradient(ellipse at center, black 40%, transparent 80%);"></div>
      </div>

      <div class="mx-auto w-full max-w-3xl px-4 sm:px-6 lg:px-8 py-20">
        <div class="text-center">
          <!-- Badge -->
          <div class="inline-flex items-center gap-2 rounded-full border border-(--border) bg-(--card) px-3 py-1 text-xs font-medium text-(--muted-foreground)">
            <span class="h-1.5 w-1.5 rounded-full bg-(--destructive)"></span>
            Erreur 404 — Page introuvable
          </div>

          <!-- Code XXL -->
          <h1
            class="mt-6 font-[family-name:var(--font-display)] text-[7rem] sm:text-[10rem] font-bold leading-none tracking-tight bg-clip-text text-transparent"
            style="background-image: linear-gradient(135deg, oklch(0.62 0.22 258), oklch(0.66 0.20 285) 60%, oklch(0.7 0.18 320));"
          >
            404
          </h1>

          <h2 class="mt-2 font-[family-name:var(--font-display)] text-2xl sm:text-3xl font-semibold text-(--foreground)">
            Cette page s'est égarée.
          </h2>
          <p class="mx-auto mt-3 max-w-xl text-sm sm:text-base leading-relaxed text-(--muted-foreground)">
            Le lien est peut-être obsolète, ou l'adresse contient une faute de frappe.
            Voici quelques pistes pour repartir du bon pied.
          </p>

          <!-- Actions -->
          <div class="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
            <a routerLink="/" hlmBtn>
              <lucide-angular [img]="HomeIcon" class="size-4 mr-2"></lucide-angular>
              Retour à l'accueil
            </a>
            <button type="button" hlmBtn variant="outline" (click)="goBack()">
              <lucide-angular [img]="BackIcon" class="size-4 mr-2"></lucide-angular>
              Page précédente
            </button>
          </div>

          <!-- Suggestions -->
          <div class="mt-12 grid grid-cols-1 sm:grid-cols-3 gap-3 text-left">
            <a
              routerLink="/services"
              class="group rounded-lg border border-(--border) bg-(--card) p-4 transition-colors hover:border-(--ring)"
            >
              <div class="flex items-center gap-2 text-(--foreground)">
                <lucide-angular [img]="SearchIcon" class="size-4 text-(--muted-foreground) group-hover:text-(--foreground) transition-colors"></lucide-angular>
                <span class="text-sm font-medium">Nos services</span>
              </div>
              <p class="mt-1 text-xs text-(--muted-foreground)">SEO, Google My Business, sites web…</p>
            </a>
            <a
              routerLink="/blog"
              class="group rounded-lg border border-(--border) bg-(--card) p-4 transition-colors hover:border-(--ring)"
            >
              <div class="flex items-center gap-2 text-(--foreground)">
                <lucide-angular [img]="SearchIcon" class="size-4 text-(--muted-foreground) group-hover:text-(--foreground) transition-colors"></lucide-angular>
                <span class="text-sm font-medium">Articles & guides</span>
              </div>
              <p class="mt-1 text-xs text-(--muted-foreground)">Conseils marketing local et SEO.</p>
            </a>
            <a
              routerLink="/contact"
              class="group rounded-lg border border-(--border) bg-(--card) p-4 transition-colors hover:border-(--ring)"
            >
              <div class="flex items-center gap-2 text-(--foreground)">
                <lucide-angular [img]="SupportIcon" class="size-4 text-(--muted-foreground) group-hover:text-(--foreground) transition-colors"></lucide-angular>
                <span class="text-sm font-medium">Contacter l'équipe</span>
              </div>
              <p class="mt-1 text-xs text-(--muted-foreground)">On vous répond rapidement.</p>
            </a>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: `
    :host { display: block; }
  `,
})
export class NotFoundComponent {
  protected readonly HomeIcon = Home;
  protected readonly BackIcon = ArrowLeft;
  protected readonly SearchIcon = Search;
  protected readonly SupportIcon = LifeBuoy;

  constructor(private router: Router) {}

  goBack(): void {
    if (typeof history !== 'undefined' && history.length > 1) {
      history.back();
      return;
    }
    this.router.navigateByUrl('/');
  }
}
