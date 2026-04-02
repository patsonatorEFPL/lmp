import { Component, ElementRef, OnDestroy, PLATFORM_ID, ViewChild, inject, signal } from '@angular/core';
import { isPlatformBrowser, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { loadStripe, Stripe, StripeElements, StripePaymentElement } from '@stripe/stripe-js';

import { environment } from '../../../environments/environment';
import { AuthService, UserInfo } from '../../core/services/auth.service';

interface ApiOk<T> {
  success: boolean;
  data?: T;
  message?: string;
}

@Component({
  selector: 'lmp-payment-guest',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  template: `
    <div class="flex min-h-screen flex-col bg-(--background) px-4 py-10">
      <div class="mx-auto w-full max-w-lg">
        <h1 class="text-2xl font-bold text-(--foreground)">Finaliser votre commande</h1>
        <p class="mt-2 text-sm text-(--muted-foreground)">
          Créez votre compte puis payez en toute sécurité.
        </p>

        @if (loadError()) {
          <p class="mt-6 rounded-sm border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-600">
            {{ loadError() }}
          </p>
        } @else if (preview()) {
          <div class="mt-6 rounded-sm border border-(--border) bg-(--card) p-4">
            <p class="text-sm font-medium text-(--foreground)">{{ preview()!.serviceName }}</p>
            <p class="mt-1 text-lg font-semibold text-(--primary)">
              {{ preview()!.totalAmount | number: '1.2-2' }} {{ preview()!.currency }}
            </p>
          </div>
        }

        @if (preview() && !payReady()) {
          <form class="mt-8 space-y-4" (ngSubmit)="onRegisterAndPrepare()">
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Email *</label>
              <input
                type="email"
                [(ngModel)]="reg.email"
                name="email"
                required
                class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Mot de passe *</label>
              <input
                type="password"
                [(ngModel)]="reg.password"
                name="password"
                required
                minlength="6"
                class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">Confirmer le mot de passe *</label>
              <input
                type="password"
                [(ngModel)]="reg.confirmPassword"
                name="confirmPassword"
                required
                class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm"
              />
            </div>
            <div class="flex items-start gap-2">
              <input type="checkbox" id="vatRev" [(ngModel)]="vatReverseCharge" name="vatRev" class="mt-1" />
              <label for="vatRev" class="text-sm text-(--foreground)">
                Autoliquidation TVA (entreprise UE, hors France)
              </label>
            </div>
            @if (vatReverseCharge) {
              <div>
                <label class="mb-1 block text-xs font-medium text-(--muted-foreground)">N° TVA intracommunautaire *</label>
                <input
                  type="text"
                  [(ngModel)]="vatNumber"
                  name="vatNumber"
                  class="w-full rounded-sm border border-(--border) bg-(--background) px-3 py-2 text-sm"
                  placeholder="ex. DE123456789"
                />
              </div>
            }
            <div class="flex items-start gap-2">
              <input type="checkbox" id="terms" [(ngModel)]="reg.acceptTerms" name="terms" class="mt-1" required />
              <label for="terms" class="text-sm text-(--foreground)">J’accepte les conditions d’utilisation *</label>
            </div>
            @if (formError()) {
              <p class="text-sm text-red-500">{{ formError() }}</p>
            }
            <button
              type="submit"
              class="w-full rounded-sm bg-(--primary) py-3 text-sm font-medium text-(--primary-foreground) disabled:opacity-50 cursor-pointer"
              [disabled]="preparing()"
            >
              @if (preparing()) {
                Préparation…
              } @else {
                Continuer vers le paiement
              }
            </button>
          </form>
        }

        @if (payReady()) {
          <div class="mt-8 rounded-sm border border-(--border) bg-(--card) p-6">
            <h2 class="text-lg font-semibold text-(--foreground)">Paiement</h2>
            <div #stripeHost class="mt-4 min-h-[120px]"></div>
            @if (payError()) {
              <p class="mt-3 text-sm text-red-500">{{ payError() }}</p>
            }
            <button
              type="button"
              class="mt-6 w-full rounded-sm bg-(--primary) py-3 text-sm font-medium text-(--primary-foreground) disabled:opacity-50 cursor-pointer"
              [disabled]="paySubmitting() || !stripeMounted()"
              (click)="confirmPay()"
            >
              @if (paySubmitting()) {
                Traitement…
              } @else {
                Payer {{ preview()?.totalAmount | number: '1.2-2' }} {{ preview()?.currency }}
              }
            </button>
          </div>
        }
      </div>
    </div>
  `,
})
export class PaymentGuestComponent implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  @ViewChild('stripeHost') stripeHost?: ElementRef<HTMLDivElement>;

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private paymentElement: StripePaymentElement | null = null;

  private clientSecret: string | null = null;
  private publishableKey: string | null = null;
  private orderId: string | null = null;
  private mountStripeAttempts = 0;
  private resumeTriggered = false;
  checkoutToken = '';

  readonly preview = signal<{
    orderId: string;
    serviceName: string;
    totalAmount: number;
    currency: string;
  } | null>(null);
  readonly loadError = signal<string | null>(null);
  readonly formError = signal<string | null>(null);
  readonly preparing = signal(false);
  readonly payReady = signal(false);
  readonly payError = signal<string | null>(null);
  readonly paySubmitting = signal(false);
  readonly stripeMounted = signal(false);

  vatReverseCharge = false;
  vatNumber = '';

  reg = {
    email: '',
    password: '',
    confirmPassword: '',
    acceptTerms: false,
  };

  constructor() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.route.queryParamMap
      .pipe(
        map((q) => (q.get('t') || '').trim()),
        distinctUntilChanged(),
      )
      .subscribe((t) => {
        this.checkoutToken = t;
        if (!t) {
          this.resetGuestPaymentState();
          this.preview.set(null);
          this.loadError.set('Lien invalide : paramètre manquant.');
          return;
        }
        this.resetGuestPaymentState();
        this.preview.set(null);
        this.loadError.set(null);
        this.fetchPreview(t);
      });
  }

  private guestResumeStorageKey(token: string): string {
    return `lmp_guest_resume_${token}`;
  }

  private resetGuestPaymentState(): void {
    this.resumeTriggered = false;
    this.paymentElement?.unmount();
    this.paymentElement = null;
    this.elements = null;
    this.stripe = null;
    this.clientSecret = null;
    this.publishableKey = null;
    this.orderId = null;
    this.mountStripeAttempts = 0;
    this.payReady.set(false);
    this.payError.set(null);
    this.stripeMounted.set(false);
  }

  private fetchPreview(token: string): void {
    this.http
      .get<ApiOk<{ orderId: string; serviceName: string; totalAmount: number; currency: string }>>(
        `${environment.apiUrl}/api/v1/payments/guest-order/preview/${encodeURIComponent(token)}`,
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.preview.set(res.data);
            this.tryResumePaymentAfterPreview(token, res.data.orderId);
          } else {
            this.loadError.set(res.message || 'Lien invalide ou expiré.');
          }
        },
        error: (err) => {
          this.loadError.set(err.error?.message || 'Impossible de charger la commande.');
        },
      });
  }

  private tryResumePaymentAfterPreview(token: string, orderIdFromPreview: string): void {
    if (!isPlatformBrowser(this.platformId) || this.resumeTriggered) {
      return;
    }
    const stored = sessionStorage.getItem(this.guestResumeStorageKey(token));
    if (!stored || stored !== String(orderIdFromPreview)) {
      return;
    }
    this.resumeTriggered = true;
    this.http
      .post<ApiOk<{ orderId: string; clientSecret: string; publishableKey: string }>>(
        `${environment.apiUrl}/api/v1/payments/checkout-order/${encodeURIComponent(orderIdFromPreview)}/payment-element`,
        {},
        { withCredentials: true },
      )
      .subscribe({
        next: (r) => {
          if (!r.success || !r.data?.clientSecret) {
            this.resumeTriggered = false;
            return;
          }
          this.clientSecret = r.data.clientSecret;
          this.publishableKey = r.data.publishableKey;
          this.orderId = String(r.data.orderId);
          this.mountStripeAttempts = 0;
          this.payReady.set(true);
          setTimeout(() => void this.mountStripe(), 200);
        },
        error: () => {
          this.resumeTriggered = false;
        },
      });
  }

  onRegisterAndPrepare(): void {
    this.formError.set(null);
    if (this.reg.password !== this.reg.confirmPassword) {
      this.formError.set('Les mots de passe ne correspondent pas.');
      return;
    }
    if (!this.reg.acceptTerms) {
      this.formError.set('Vous devez accepter les conditions d’utilisation.');
      return;
    }
    if (this.vatReverseCharge && !this.vatNumber.trim()) {
      this.formError.set('Indiquez votre numéro de TVA pour l’autoliquidation.');
      return;
    }

    this.preparing.set(true);
    const body = {
      checkoutToken: this.checkoutToken,
      vatReverseCharge: this.vatReverseCharge,
      vatNumber: this.vatNumber.trim(),
      registration: {
        email: this.reg.email.trim(),
        password: this.reg.password,
        confirmPassword: this.reg.confirmPassword,
        acceptTerms: true,
      },
    };

    this.http
      .post<
        ApiOk<{
          orderId: string;
          clientSecret: string;
          publishableKey: string;
          user: UserInfo;
        }>
      >(`${environment.apiUrl}/api/v1/payments/guest-order/prepare`, body, { withCredentials: true })
      .subscribe({
        next: (res) => {
          this.preparing.set(false);
          if (!res.success || !res.data?.clientSecret) {
            this.formError.set(res.message || 'Inscription impossible.');
            return;
          }
          this.clientSecret = res.data.clientSecret;
          this.publishableKey = res.data.publishableKey;
          this.orderId = String(res.data.orderId);
          try {
            sessionStorage.setItem(this.guestResumeStorageKey(this.checkoutToken), this.orderId);
          } catch {
            /* ignore quota / private mode */
          }
          if (res.data.user) {
            this.auth.setUser(res.data.user as UserInfo);
          }
          this.mountStripeAttempts = 0;
          this.payReady.set(true);
          setTimeout(() => void this.mountStripe(), 200);
        },
        error: (err) => {
          this.preparing.set(false);
          this.formError.set(err.error?.message || 'Erreur lors de l’inscription.');
        },
      });
  }

  private async mountStripe(): Promise<void> {
    if (!isPlatformBrowser(this.platformId) || !this.payReady()) {
      return;
    }
    if (!this.clientSecret || !this.publishableKey) {
      return;
    }
    if (this.paymentElement) {
      return;
    }

    this.mountStripeAttempts++;
    if (this.mountStripeAttempts > 30) {
      this.payError.set('Impossible d’afficher le formulaire de paiement. Rechargez la page.');
      return;
    }

    let host = this.stripeHost?.nativeElement;
    if (!host) {
      setTimeout(() => void this.mountStripe(), 80);
      return;
    }
    if (host.childElementCount > 0) {
      return;
    }
    try {
      this.stripe = await loadStripe(this.publishableKey);
      if (!this.stripe) {
        this.payError.set('Impossible de charger Stripe.');
        return;
      }
      this.elements = this.stripe.elements({
        clientSecret: this.clientSecret,
        appearance: { theme: 'stripe' },
      });
      this.paymentElement = this.elements.create('payment');
      this.paymentElement.mount(host);
      this.mountStripeAttempts = 0;
      this.stripeMounted.set(true);
    } catch {
      this.payError.set('Erreur d’initialisation du paiement.');
    }
  }

  async confirmPay(): Promise<void> {
    if (!this.stripe || !this.elements || !this.orderId) {
      return;
    }
    this.payError.set(null);
    this.paySubmitting.set(true);
    const origin = window.location.origin;
    const { error } = await this.stripe.confirmPayment({
      elements: this.elements,
      confirmParams: {
        return_url: `${origin}/payment/success?orderId=${encodeURIComponent(this.orderId)}`,
      },
    });
    this.paySubmitting.set(false);
    if (error) {
      this.payError.set(error.message ?? 'Paiement refusé.');
    }
  }

  ngOnDestroy(): void {
    this.resetGuestPaymentState();
  }
}
