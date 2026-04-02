import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  PLATFORM_ID,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { loadStripe, Stripe, StripeElements, StripePaymentElement } from '@stripe/stripe-js';

import { PaymentSessionService } from '../../core/services/payment-session.service';

@Component({
  selector: 'lmp-payment-process',
  standalone: true,
  template: `
    <div class="flex min-h-[70vh] flex-col items-center justify-center bg-(--background) px-4 py-10">
      <div class="w-full max-w-lg rounded-sm border border-(--border) bg-(--card) p-6 shadow-sm">
        <h1 class="text-xl font-semibold text-(--foreground)">Paiement sécurisé</h1>
        <p class="mt-1 text-sm text-(--muted-foreground)">
          Saisissez vos informations de carte. Vous serez redirigé après confirmation.
        </p>
        <div #stripeHost class="mt-6 min-h-[120px]"></div>
        @if (errorMessage()) {
          <p class="mt-3 text-sm text-red-500">{{ errorMessage() }}</p>
        }
        <button
          type="button"
          class="mt-6 w-full rounded-sm bg-(--primary) px-4 py-3 text-sm font-medium text-(--primary-foreground) disabled:opacity-50 cursor-pointer"
          [disabled]="submitting() || !ready()"
          (click)="submit()"
        >
          @if (submitting()) {
            Paiement en cours…
          } @else {
            Payer maintenant
          }
        </button>
        <button
          type="button"
          class="mt-3 w-full text-center text-sm text-(--muted-foreground) underline cursor-pointer"
          (click)="cancel()"
        >
          Annuler
        </button>
      </div>
    </div>
  `,
})
export class PaymentProcessComponent implements AfterViewInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly paymentSession = inject(PaymentSessionService);
  private readonly platformId = inject(PLATFORM_ID);

  @ViewChild('stripeHost') stripeHost!: ElementRef<HTMLDivElement>;

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private paymentElement: StripePaymentElement | null = null;

  readonly submitting = signal(false);
  readonly ready = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngAfterViewInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const p = this.paymentSession.peek();
    if (!p?.clientSecret || !p.publishableKey) {
      void this.router.navigate(['/services']);
      return;
    }
    void this.bootstrapStripe(p.clientSecret, p.publishableKey);
  }

  private async bootstrapStripe(clientSecret: string, publishableKey: string): Promise<void> {
    try {
      this.stripe = await loadStripe(publishableKey);
      if (!this.stripe) {
        this.errorMessage.set('Impossible de charger Stripe.');
        return;
      }
      this.elements = this.stripe.elements({
        clientSecret,
        appearance: { theme: 'stripe' },
      });
      this.paymentElement = this.elements.create('payment');
      this.paymentElement.mount(this.stripeHost.nativeElement);
      this.ready.set(true);
    } catch {
      this.errorMessage.set('Erreur d’initialisation du paiement.');
    }
  }

  async submit(): Promise<void> {
    if (!isPlatformBrowser(this.platformId) || !this.stripe || !this.elements) {
      return;
    }
    const p = this.paymentSession.peek();
    if (!p) {
      void this.router.navigate(['/services']);
      return;
    }
    this.errorMessage.set(null);
    this.submitting.set(true);
    const origin = window.location.origin;
    const { error } = await this.stripe.confirmPayment({
      elements: this.elements,
      confirmParams: {
        return_url: `${origin}/payment/success?orderId=${encodeURIComponent(p.orderId)}`,
      },
    });
    this.submitting.set(false);
    if (error) {
      this.errorMessage.set(error.message ?? 'Le paiement a échoué.');
    }
  }

  cancel(): void {
    this.paymentSession.clear();
    void this.router.navigate(['/dashboard/orders']);
  }

  ngOnDestroy(): void {
    this.paymentElement?.unmount();
    this.paymentElement = null;
    this.elements = null;
    this.stripe = null;
  }
}
