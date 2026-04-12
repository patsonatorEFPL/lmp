import {
  Component,
  ElementRef,
  OnDestroy,
  PLATFORM_ID,
  ViewChild,
  afterNextRender,
  inject,
  signal,
  computed,
} from '@angular/core';
import { isPlatformBrowser, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { loadStripe, Stripe, StripeElements, StripePaymentElement } from '@stripe/stripe-js';
import {
  LucideAngularModule,
  ArrowLeft,
  Loader2,
  ShieldCheck,
  CreditCard,
  Building2,
  Check,
} from 'lucide-angular';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';

// ── API types ──────────────────────────────────────────────
interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
}

interface CheckoutPreview {
  serviceName: string;
  amountHt: number;
  vatAmount: number;
  totalAmount: number;
  vatRate: number;
  reverseCharge: boolean;
  currency: string;
  durationType: string;
  offerId: string;
}

interface PaymentElementResult {
  orderId: string;
  clientSecret: string;
  publishableKey: string;
}

@Component({
  selector: 'lmp-checkout',
  standalone: true,
  imports: [LucideAngularModule, DecimalPipe, FormsModule],
  template: `
    <!-- Force dark theme on this page -->
    <div class="checkout-page dark min-h-screen bg-[#0e0e10] px-4 py-8 sm:py-12">
      <div class="mx-auto w-full max-w-[520px]">

        <!-- Back button -->
        <button
          type="button"
          class="mb-6 flex cursor-pointer items-center gap-2 text-sm text-[#999] transition-colors hover:text-white"
          (click)="goBack()"
        >
          <lucide-icon [img]="ArrowLeftIcon" [size]="18"></lucide-icon>
          <span>Retour</span>
        </button>

        <!-- Loading skeleton -->
        @if (loading()) {
          <div class="space-y-4 animate-pulse">
            <div class="h-8 w-48 rounded bg-[#222]"></div>
            <div class="h-40 rounded-lg bg-[#1a1a1d]"></div>
            <div class="h-60 rounded-lg bg-[#1a1a1d]"></div>
          </div>
        }

        <!-- Error state -->
        @if (loadError()) {
          <div class="rounded-lg border border-red-500/30 bg-red-500/10 p-6 text-center">
            <p class="text-sm text-red-400">{{ loadError() }}</p>
            <button
              type="button"
              class="mt-4 cursor-pointer rounded-lg bg-[#222] px-4 py-2 text-sm text-white hover:bg-[#2a2a2d]"
              (click)="goBack()"
            >
              Retour aux services
            </button>
          </div>
        }

        <!-- Main checkout content -->
        @if (!loading() && !loadError() && preview()) {
          <!-- Title -->
          <h1 class="mb-6 text-2xl font-bold text-white">
            {{ preview()!.serviceName }}
          </h1>

          <!-- ═══ Order Summary Card ═══ -->
          <div class="rounded-lg border border-[#2a2a2e] bg-[#1a1a1d] p-5">
            <h2 class="mb-4 text-sm font-semibold text-white">
              Détails de la commande
            </h2>

            <!-- Line item -->
            <div class="flex items-start justify-between border-b border-[#2a2a2e] pb-3">
              <div>
                <p class="text-sm font-medium text-[#e5e5e5]">{{ preview()!.serviceName }}</p>
                <p class="mt-0.5 text-xs text-[#777]">{{ durationLabel() }}</p>
              </div>
              <p class="text-sm font-medium text-[#e5e5e5]">
                {{ preview()!.amountHt | number: '1.2-2' }}&nbsp;{{ currencySymbol() }}
              </p>
            </div>

            <!-- Subtotal -->
            <div class="mt-3 flex justify-between text-sm">
              <span class="text-[#999]">Sous-total</span>
              <span class="text-[#e5e5e5]">{{ preview()!.amountHt | number: '1.2-2' }}&nbsp;{{ currencySymbol() }}</span>
            </div>

            <!-- TVA -->
            <div class="mt-2 flex justify-between text-sm">
              <span class="text-[#999]">
                @if (vatReverseCharge()) {
                  TVA — autoliquidation
                } @else {
                  TVA ({{ preview()!.vatRate }}&nbsp;%)
                }
              </span>
              <span class="text-[#e5e5e5]">
                {{ displayVatAmount() | number: '1.2-2' }}&nbsp;{{ currencySymbol() }}
              </span>
            </div>

            <!-- Total -->
            <div class="mt-3 flex justify-between border-t border-[#2a2a2e] pt-3">
              <span class="text-sm font-semibold text-white">Total dû aujourd'hui</span>
              <span class="text-base font-bold text-white">
                {{ displayTotal() | number: '1.2-2' }}&nbsp;{{ currencySymbol() }}
              </span>
            </div>
          </div>

          <!-- ═══ Tax ID / Reverse Charge (optional) ═══ -->
          <div class="mt-4 rounded-lg border border-[#2a2a2e] bg-[#1a1a1d] p-5">
            <div class="flex items-center gap-2 text-sm font-semibold text-white">
              <lucide-icon [img]="Building2Icon" [size]="16" class="text-[#777]"></lucide-icon>
              Numéro fiscal des sociétés (facultatif)
            </div>
            <p class="mt-1.5 text-xs leading-relaxed text-[#777]">
              Si vous fournissez un numéro de TVA, la facturation sera au nom de votre entreprise.
            </p>

            <label class="mt-4 flex cursor-pointer items-start gap-3 text-sm text-[#ccc]">
              <input
                type="checkbox"
                [checked]="vatReverseCharge()"
                (change)="onReverseChargeToggle($event)"
                class="mt-0.5 h-4 w-4 cursor-pointer rounded border-[#444] bg-transparent accent-blue-500"
              />
              <span>Autoliquidation TVA (reverse charge)</span>
            </label>

            @if (vatReverseCharge()) {
              <div class="mt-3">
                <label class="mb-1 block text-xs font-medium text-[#999]">N° TVA intracommunautaire</label>
                <input
                  type="text"
                  [ngModel]="vatNumber()"
                  (ngModelChange)="vatNumber.set($event)"
                  placeholder="ex. FR12345678901"
                  class="w-full rounded-lg border border-[#333] bg-[#111] px-3 py-2.5 text-sm text-white outline-none transition-colors placeholder:text-[#555] focus:border-blue-500/60"
                  autocomplete="off"
                />
                @if (vatError()) {
                  <p class="mt-1.5 text-xs text-red-400">{{ vatError() }}</p>
                }
              </div>
            }
          </div>

          <!-- ═══ Payment Form ═══ -->
          <div class="mt-4 rounded-lg border border-[#2a2a2e] bg-[#1a1a1d] p-5">
            <div class="mb-4 flex items-center gap-2 text-sm font-semibold text-white">
              <lucide-icon [img]="CreditCardIcon" [size]="16" class="text-[#777]"></lucide-icon>
              Mode de paiement
            </div>

            @if (stripeLoading()) {
              <div class="flex items-center justify-center py-8">
                <lucide-icon [img]="Loader2Icon" [size]="20" class="animate-spin text-[#777]"></lucide-icon>
                <span class="ml-2 text-sm text-[#777]">Chargement du formulaire…</span>
              </div>
            }

            <div
              #stripeHost
              class="min-h-[1px] transition-opacity duration-300"
              [class.opacity-0]="stripeLoading()"
              [class.opacity-100]="!stripeLoading()"
            ></div>

            @if (stripeError()) {
              <p class="mt-3 text-sm text-red-400">{{ stripeError() }}</p>
            }
          </div>

          <!-- ═══ Submit Button ═══ -->
          <button
            type="button"
            class="mt-6 flex w-full cursor-pointer items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-3.5 text-sm font-semibold text-white transition-all duration-150 hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
            [disabled]="!canSubmit()"
            (click)="submitPayment()"
          >
            @if (submitting()) {
              <lucide-icon [img]="Loader2Icon" [size]="16" class="animate-spin"></lucide-icon>
              Paiement en cours…
            } @else {
              <lucide-icon [img]="ShieldCheckIcon" [size]="16"></lucide-icon>
              Payer {{ displayTotal() | number: '1.2-2' }}&nbsp;{{ currencySymbol() }}
            }
          </button>

          <!-- Security note -->
          <p class="mt-3 text-center text-xs text-[#666]">
            <lucide-icon [img]="ShieldCheckIcon" [size]="12" class="mr-1 inline-block align-[-2px]"></lucide-icon>
            Paiement sécurisé par Stripe. Vos données bancaires ne transitent pas par nos serveurs.
          </p>
        }
      </div>
    </div>
  `,
  styles: `
    .checkout-page {
      /* Override Stripe Elements for dark theme */
      --colorPrimary: #3b82f6;
    }
  `,
})
export class CheckoutComponent implements OnDestroy {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);

  // Icons
  readonly ArrowLeftIcon = ArrowLeft;
  readonly Loader2Icon = Loader2;
  readonly ShieldCheckIcon = ShieldCheck;
  readonly CreditCardIcon = CreditCard;
  readonly Building2Icon = Building2;
  readonly CheckIcon = Check;

  @ViewChild('stripeHost') stripeHost!: ElementRef<HTMLDivElement>;

  // ── State ────────────────────────────────────────────
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly preview = signal<CheckoutPreview | null>(null);

  readonly vatReverseCharge = signal(false);
  readonly vatNumber = signal('');
  readonly vatError = signal<string | null>(null);

  readonly stripeLoading = signal(true);
  readonly stripeError = signal<string | null>(null);
  readonly stripeReady = signal(false);
  readonly submitting = signal(false);

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private paymentElement: StripePaymentElement | null = null;
  private orderId: string | null = null;
  private offerId: string | null = null;

  // ── Computed ─────────────────────────────────────────
  readonly displayVatAmount = computed(() => {
    const p = this.preview();
    if (!p) return 0;
    if (this.vatReverseCharge()) return 0;
    // Compute client-side: amountHt × vatRate / 100
    return Math.round(p.amountHt * p.vatRate) / 100;
  });

  readonly displayTotal = computed(() => {
    const p = this.preview();
    if (!p) return 0;
    if (this.vatReverseCharge()) return p.amountHt;
    // HT + TVA computed client-side
    return Math.round((p.amountHt + p.amountHt * p.vatRate / 100) * 100) / 100;
  });

  readonly durationLabel = computed(() => {
    const p = this.preview();
    if (!p) return '';
    switch (p.durationType) {
      case 'MONTHLY': return 'Abonnement mensuel';
      case 'YEARLY': return 'Abonnement annuel';
      default: return 'Paiement unique';
    }
  });

  readonly currencySymbol = computed(() => {
    const p = this.preview();
    if (!p) return '€';
    switch (p.currency.toUpperCase()) {
      case 'EUR': return '€';
      case 'USD': return '$';
      case 'GBP': return '£';
      default: return p.currency;
    }
  });

  readonly canSubmit = computed(() =>
    this.stripeReady() && !this.submitting() && !this.stripeError()
  );

  constructor() {
    afterNextRender(() => {
      this.offerId = this.route.snapshot.paramMap.get('offerId');
      if (!this.offerId) {
        this.loadError.set('Offre introuvable.');
        this.loading.set(false);
        return;
      }

      // Init VAT from user profile
      const user = this.authService.user();
      if (user) {
        this.vatReverseCharge.set(!!user.vatReverseCharge);
        this.vatNumber.set(user.vatNumber ?? '');
      }

      this.loadPreview(this.offerId);
    });
  }

  // ── Data Loading ─────────────────────────────────────
  private loadPreview(offerId: string): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.http
      .get<ApiResponse<CheckoutPreview>>(
        `${environment.apiUrl}/api/v1/payments/checkout-preview`,
        { params: { offerId }, withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.preview.set(res.data);
            this.loading.set(false);
            this.initPayment(offerId);
          } else {
            this.loadError.set(res.message ?? 'Offre introuvable.');
            this.loading.set(false);
          }
        },
        error: (err) => {
          this.loadError.set(
            err.error?.message ?? 'Erreur lors du chargement de l\'offre.',
          );
          this.loading.set(false);
        },
      });
  }

  private initPayment(offerId: string): void {
    this.stripeLoading.set(true);
    const currency = this.preview()?.currency ?? 'EUR';

    this.http
      .post<ApiResponse<PaymentElementResult>>(
        `${environment.apiUrl}/api/v1/payments/checkout/payment-element`,
        { offerId, currency },
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.orderId = res.data.orderId;
            void this.mountStripe(res.data.clientSecret, res.data.publishableKey);
          } else {
            this.stripeError.set(res.message ?? 'Impossible de préparer le paiement.');
            this.stripeLoading.set(false);
          }
        },
        error: (err) => {
          this.stripeError.set(
            err.error?.message ?? 'Erreur lors de la préparation du paiement.',
          );
          this.stripeLoading.set(false);
        },
      });
  }

  // ── Stripe ───────────────────────────────────────────
  private async mountStripe(clientSecret: string, publishableKey: string): Promise<void> {
    try {
      this.stripe = await loadStripe(publishableKey);
      if (!this.stripe) {
        this.stripeError.set('Impossible de charger Stripe.');
        this.stripeLoading.set(false);
        return;
      }

      this.elements = this.stripe.elements({
        clientSecret,
        appearance: {
          theme: 'night',
          variables: {
            colorPrimary: '#3b82f6',
            colorBackground: '#111113',
            colorText: '#e5e5e5',
            colorDanger: '#ef4444',
            fontFamily: 'Inter, system-ui, sans-serif',
            borderRadius: '8px',
            spacingUnit: '4px',
          },
          rules: {
            '.Input': {
              backgroundColor: '#111113',
              border: '1px solid #333',
            },
            '.Input:focus': {
              border: '1px solid rgba(59, 130, 246, 0.6)',
              boxShadow: '0 0 0 1px rgba(59, 130, 246, 0.3)',
            },
            '.Label': {
              color: '#999',
              fontSize: '13px',
            },
          },
        },
      });

      this.paymentElement = this.elements.create('payment', {
        layout: 'tabs',
      });
      this.paymentElement.on('ready', () => {
        this.stripeLoading.set(false);
        this.stripeReady.set(true);
      });
      this.paymentElement.mount(this.stripeHost.nativeElement);
    } catch {
      this.stripeError.set('Erreur d\'initialisation du paiement.');
      this.stripeLoading.set(false);
    }
  }

  // ── Actions ──────────────────────────────────────────
  onReverseChargeToggle(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.vatReverseCharge.set(checked);
    this.vatError.set(null);
  }

  async submitPayment(): Promise<void> {
    if (!isPlatformBrowser(this.platformId) || !this.stripe || !this.elements) {
      return;
    }

    // Validate VAT if reverse charge
    if (this.vatReverseCharge()) {
      const vat = this.vatNumber().trim();
      if (!vat) {
        this.vatError.set('Veuillez saisir votre numéro de TVA.');
        return;
      }
      // Basic EU VAT format check: 2 letters + digits
      if (!/^[A-Z]{2}\d{4,}/.test(vat.toUpperCase())) {
        this.vatError.set('Format de TVA invalide (ex: FR12345678901).');
        return;
      }
    }
    this.vatError.set(null);

    // Save VAT preferences before payment
    this.submitting.set(true);

    try {
      await new Promise<void>((resolve, reject) => {
        this.profileService
          .updateProfile({
            vatReverseCharge: this.vatReverseCharge(),
            vatNumber: this.vatReverseCharge() ? this.vatNumber().trim() : '',
          })
          .subscribe({ next: (u) => { this.authService.setUser(u); resolve(); }, error: reject });
      });
    } catch {
      // Non-blocking — VAT save failure shouldn't block payment
    }

    const origin = window.location.origin;
    const { error } = await this.stripe.confirmPayment({
      elements: this.elements,
      confirmParams: {
        return_url: `${origin}/payment/success?orderId=${encodeURIComponent(this.orderId ?? '')}`,
      },
    });

    this.submitting.set(false);
    if (error) {
      this.stripeError.set(error.message ?? 'Le paiement a échoué.');
    }
  }

  goBack(): void {
    void this.router.navigate(['/services']);
  }

  ngOnDestroy(): void {
    this.paymentElement?.unmount();
    this.paymentElement = null;
    this.elements = null;
    this.stripe = null;
  }
}
