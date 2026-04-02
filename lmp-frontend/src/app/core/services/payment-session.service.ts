import { Injectable, signal } from '@angular/core';

export interface StripePaymentSessionPayload {
  clientSecret: string;
  publishableKey: string;
  orderId: string;
}

/**
 * Transporte en mémoire le clientSecret entre la création du PaymentIntent et la page /payment/process
 * (évite de l’exposer dans l’URL).
 */
@Injectable({ providedIn: 'root' })
export class PaymentSessionService {
  private readonly payload = signal<StripePaymentSessionPayload | null>(null);

  start(p: StripePaymentSessionPayload): void {
    this.payload.set(p);
  }

  peek(): StripePaymentSessionPayload | null {
    return this.payload();
  }

  clear(): void {
    this.payload.set(null);
  }
}
