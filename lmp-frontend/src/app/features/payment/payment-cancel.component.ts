import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  XCircle,
  ArrowRight,
  ShoppingCart,
  RefreshCw,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';

@Component({
  selector: 'lmp-payment-cancel',
  standalone: true,
  imports: [LucideAngularModule, HlmButton, RouterLink],
  template: `
    <section class="flex min-h-screen items-center justify-center bg-(--background) px-4">
      <div class="w-full max-w-md text-center">
        <div class="flex flex-col items-center gap-6 rounded-sm border border-amber-500/20 bg-(--card) p-10 shadow-xs">
          <div class="flex h-20 w-20 items-center justify-center rounded-full bg-(--muted)">
            <lucide-icon
              [img]="XCircleIcon"
              [size]="40"
              class="text-(--foreground)"
            ></lucide-icon>
          </div>
          <div>
            <h2 class="text-2xl font-bold text-(--foreground)">
              Paiement annulé
            </h2>
            <p class="mt-2 text-sm text-(--muted-foreground)">
              Votre paiement a été annulé. Aucun montant n'a été débité.
              Vous pouvez réessayer à tout moment.
            </p>
          </div>

          <div class="flex w-full flex-col gap-2 sm:flex-row">
            <a
              hlmBtn
              variant="default"
              size="default"
              routerLink="/services"
              class="w-full cursor-pointer gap-2"
            >
              <lucide-icon [img]="ShoppingCartIcon" [size]="16"></lucide-icon>
              Voir nos services
            </a>
            <a
              hlmBtn
              variant="outline"
              size="default"
              routerLink="/"
              class="w-full cursor-pointer gap-2"
            >
              Accueil
              <lucide-icon [img]="ArrowRightIcon" [size]="16"></lucide-icon>
            </a>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class PaymentCancelComponent {
  readonly XCircleIcon = XCircle;
  readonly ArrowRightIcon = ArrowRight;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly RefreshCwIcon = RefreshCw;
}
