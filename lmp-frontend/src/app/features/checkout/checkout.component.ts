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
  effect,
} from '@angular/core';
import { isPlatformBrowser, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { loadStripe, Stripe, StripeElements, StripePaymentElement, StripeAddressElement } from '@stripe/stripe-js';
import {
  LucideAngularModule,
  ArrowLeft,
  Loader2,
  ShieldCheck,
  CreditCard,
  Building2,
  Check,
  MapPin,
} from 'lucide-angular';

import { environment } from '../../../environments/environment';
import { paymentApiUrls } from '../../core/api/payment-api.paths';
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

/** Shape of GET /api/v1/orders/:id response */
interface OrderDetail {
  id: string;
  serviceName: string;
  totalAmount: number;
  currency: string;
  status: string;
  billingName: string | null;
  billingAddress: string | null;
  billingCity: string | null;
  billingPostalCode: string | null;
  billingCountry: string | null;
  vatReverseCharge: boolean | null;
  customerVatNumber: string | null;
  amountBaseEur: number | null;
}

interface GeoCheckResult {
  ipCountry: string | null;
  vpnScore: number;
  vpnDetected: boolean;
  vpnSources: string;
}

interface FraudCheckResult {
  score: number;
  alert: boolean;
  flags: string[];
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

          <!-- ═══ Different billing name ═══ -->
          <label class="mt-4 flex cursor-pointer items-center gap-3 rounded-lg border border-[#2a2a2e] bg-[#1a1a1d] px-5 py-4 text-sm text-[#ccc] transition-colors hover:border-[#3a3a3e]">
            <input
              type="checkbox"
              [checked]="useDifferentBillingName()"
              (change)="onDifferentBillingNameToggle($event)"
              class="h-4 w-4 cursor-pointer rounded border-[#444] bg-transparent accent-blue-500"
            />
            <span>Utiliser un nom différent sur les factures</span>
          </label>

          @if (useDifferentBillingName()) {
            <div class="mt-2 rounded-lg border border-[#2a2a2e] bg-[#1a1a1d] px-5 py-4">
              <label class="mb-1 block text-xs font-medium text-[#999]">Nom sur la facture</label>
              <input
                type="text"
                [ngModel]="customBillingName()"
                (ngModelChange)="customBillingName.set($event)"
                placeholder="ex. Nom de l'entreprise ou nom complet"
                class="w-full rounded-lg border border-[#333] bg-[#111] px-3 py-2.5 text-sm text-white outline-none transition-colors placeholder:text-[#555] focus:border-blue-500/60"
                autocomplete="organization"
              />
            </div>
          }

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

          <!-- ═══ Billing Address ═══ -->
          <div class="mt-4 rounded-lg border border-[#2a2a2e] bg-[#1a1a1d] p-5">
            <div class="mb-4 flex items-center gap-2 text-sm font-semibold text-white">
              <lucide-icon [img]="MapPinIcon" [size]="16" class="text-[#777]"></lucide-icon>
              Adresse de facturation
            </div>

            <div
              #addressHost
              class="min-h-[1px] transition-opacity duration-300"
              [class.opacity-0]="stripeLoading()"
              [class.opacity-100]="!stripeLoading()"
            ></div>
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
  readonly MapPinIcon = MapPin;

  @ViewChild('stripeHost') stripeHost!: ElementRef<HTMLDivElement>;
  @ViewChild('addressHost') addressHost!: ElementRef<HTMLDivElement>;

  // ── State ────────────────────────────────────────────
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly preview = signal<CheckoutPreview | null>(null);

  readonly useDifferentBillingName = signal(false);
  readonly customBillingName = signal('');

  readonly vatReverseCharge = signal(false);
  readonly vatNumber = signal('');
  readonly vatError = signal<string | null>(null);

  readonly stripeLoading = signal(true);
  readonly stripeError = signal<string | null>(null);
  readonly stripeReady = signal(false);
  readonly submitting = signal(false);

  // ── Fraud / geo signals ─────────────────────────────
  private geoCheck = signal<GeoCheckResult | null>(null);
  private geoCountry = signal<string | null>(null);
  private geoLocationDenied = signal(false);
  private autoSaveTimer: ReturnType<typeof setTimeout> | null = null;
  private beforeUnloadHandler: (() => void) | null = null;

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private paymentElement: StripePaymentElement | null = null;
  private addressElement: StripeAddressElement | null = null;
  private orderId: string | null = null;

  /** true when paying an existing order (route /checkout/order/:orderId) */
  private existingOrderMode = false;

  // ── Computed ─────────────────────────────────────────
  readonly displayVatAmount = computed(() => {
    const p = this.preview();
    if (!p) return 0;
    if (this.vatReverseCharge()) return 0;
    return Math.round(p.amountHt * p.vatRate) / 100;
  });

  readonly displayTotal = computed(() => {
    const p = this.preview();
    if (!p) return 0;
    if (this.vatReverseCharge()) return p.amountHt;
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
    this.stripeReady() && !this.submitting()
  );

  constructor() {
    afterNextRender(() => {
      // Detect mode: /checkout/order/:orderId  vs  /checkout/:offerId
      const existingOrderId = this.route.snapshot.paramMap.get('orderId');
      const offerId = this.route.snapshot.paramMap.get('offerId');

      // Init VAT from user profile as default
      const user = this.authService.user();
      if (user) {
        this.vatReverseCharge.set(!!user.vatReverseCharge);
        this.vatNumber.set(user.vatNumber ?? '');
      }

      if (existingOrderId) {
        // ── Existing order mode ──
        this.existingOrderMode = true;
        this.orderId = existingOrderId;
        this.loadExistingOrder(existingOrderId);
      } else if (offerId) {
        // ── New order mode ──
        this.loadPreview(offerId);
      } else {
        this.loadError.set('Offre introuvable.');
        this.loading.set(false);
      }

      // ── Geo check (VPN detection) ──
      this.runGeoCheck();

      // ── Auto-save setup ──
      this.setupAutoSave();
    });
  }

  // ── Data Loading ─────────────────────────────────────

  /** Load checkout preview for a new order from an offer */
  private loadPreview(offerId: string): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.http
      .get<ApiResponse<CheckoutPreview>>(
        paymentApiUrls.checkoutPreview(),
        { params: { offerId }, withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.preview.set(res.data);
            this.loading.set(false);
            this.initNewOrderPayment(offerId);
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

  /** Load an existing order's data and restore all form fields */
  private loadExistingOrder(orderId: string): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.http
      .get<ApiResponse<OrderDetail>>(
        `${environment.apiUrl}/api/v1/orders/${orderId}`,
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          if (res.success && res.data) {
            const order = res.data;

            if (order.status !== 'PAYMENT_PENDING') {
              this.loadError.set('Cette commande n\'est pas en attente de paiement.');
              this.loading.set(false);
              return;
            }

            // Restore form fields from order snapshot
            this.restoreFormFromOrder(order);

            // Restore draft from localStorage (overrides order data with latest user input)
            this.restoreDraft();

            // Build a preview from the order data
            const amountHt = order.amountBaseEur ?? order.totalAmount;
            const vatRate = (order.vatReverseCharge) ? 0 : 20;
            this.preview.set({
              serviceName: order.serviceName,
              amountHt,
              vatAmount: order.vatReverseCharge ? 0 : Math.round(amountHt * vatRate) / 100,
              totalAmount: order.totalAmount,
              vatRate,
              reverseCharge: !!order.vatReverseCharge,
              currency: order.currency ?? 'EUR',
              durationType: 'ONE_TIME',
              offerId: '',
            });

            this.loading.set(false);
            this.initExistingOrderPayment(orderId);
          } else {
            this.loadError.set(res.message ?? 'Commande introuvable.');
            this.loading.set(false);
          }
        },
        error: (err) => {
          this.loadError.set(
            err.error?.message ?? 'Erreur lors du chargement de la commande.',
          );
          this.loading.set(false);
        },
      });
  }

  /** Restore all user-entered data from order fields */
  private restoreFormFromOrder(order: OrderDetail): void {
    // Billing name
    if (order.billingName) {
      this.useDifferentBillingName.set(true);
      this.customBillingName.set(order.billingName);
    }

    // VAT / Reverse charge — prefer order snapshot, fall back to user profile
    if (order.vatReverseCharge != null) {
      this.vatReverseCharge.set(order.vatReverseCharge);
    }
    if (order.customerVatNumber) {
      this.vatNumber.set(order.customerVatNumber);
    }
  }

  // ── Payment init ────────────────────────────────────

  /** Create a PaymentIntent for a new order (from offer) */
  private initNewOrderPayment(offerId: string): void {
    this.stripeLoading.set(true);
    const currency = this.preview()?.currency ?? 'EUR';

    this.http
      .post<ApiResponse<PaymentElementResult>>(
        paymentApiUrls.checkoutPaymentElement(),
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

  /** Create/refresh a PaymentIntent for an existing order */
  private initExistingOrderPayment(orderId: string): void {
    this.stripeLoading.set(true);

    this.http
      .post<ApiResponse<PaymentElementResult>>(
        paymentApiUrls.checkoutOrderPaymentElement(orderId),
        {},
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          if (res.success && res.data) {
            void this.mountStripe(res.data.clientSecret, res.data.publishableKey);
          } else {
            this.stripeError.set(res.message ?? 'Impossible de préparer le paiement.');
            this.stripeLoading.set(false);
          }
        },
        error: (err) => {
          if (err.status === 409) {
            this.loadError.set('Paiement déjà pris en compte pour cette commande.');
            this.stripeLoading.set(false);
            return;
          }
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

      // Address Element — always collects billing address
      const addressDefaults = this.getRestoredAddressDefaults();
      this.addressElement = this.elements.create('address', {
        mode: 'billing',
        ...(addressDefaults ? { defaultValues: addressDefaults } : {}),
      } as any);
      this.addressElement.mount(this.addressHost.nativeElement);

      // Payment Element — address handled by Address Element above
      this.paymentElement = this.elements.create('payment', {
        layout: 'tabs',
        fields: {
          billingDetails: {
            address: 'never',
          },
        },
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

  // ── Geo / Fraud ─────────────────────────────────────

  private runGeoCheck(): void {
    this.http
      .get<ApiResponse<GeoCheckResult>>(
        paymentApiUrls.geoCheck(),
        { withCredentials: true },
      )
      .subscribe({
        next: (res) => {
          if (res.success && res.data) {
            this.geoCheck.set(res.data);
            console.debug('[FRAUD-DEBUG] geo-check result:', res.data);
            if (res.data.vpnDetected) {
              this.requestGeolocation();
            }
          }
        },
        error: (err) => console.warn('[FRAUD-DEBUG] geo-check failed:', err),
      });
  }

  private requestGeolocation(): void {
    if (!navigator.geolocation) {
      this.geoLocationDenied.set(true);
      console.debug('[FRAUD-DEBUG] Geolocation API not available');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        console.debug('[FRAUD-DEBUG] Geolocation granted:', pos.coords.latitude, pos.coords.longitude);
        // Reverse geocode via Nominatim (free, no key)
        fetch(`https://nominatim.openstreetmap.org/reverse?lat=${pos.coords.latitude}&lon=${pos.coords.longitude}&format=json`)
          .then(r => r.json())
          .then(data => {
            const cc = data?.address?.country_code?.toUpperCase() ?? null;
            this.geoCountry.set(cc);
            console.debug('[FRAUD-DEBUG] Reverse geocode country:', cc);
          })
          .catch(err => console.warn('[FRAUD-DEBUG] Reverse geocode failed:', err));
      },
      (err) => {
        this.geoLocationDenied.set(true);
        console.debug('[FRAUD-DEBUG] Geolocation denied:', err.message);
      },
      { timeout: 10000, enableHighAccuracy: false },
    );
  }

  private async sendFraudSignals(): Promise<void> {
    if (!this.orderId) return;

    // Get billing country from Stripe Address Element
    let billingCountry: string | null = null;
    if (this.addressElement) {
      try {
        const addrValue = await this.addressElement.getValue();
        billingCountry = addrValue?.value?.address?.country ?? null;
        console.debug('[FRAUD-DEBUG] Stripe address country:', billingCountry);
      } catch (err) {
        console.warn('[FRAUD-DEBUG] Could not get address value:', err);
      }
    }

    const browserTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    console.debug('[FRAUD-DEBUG] Sending fraud signals: tz=%s geo=%s billing=%s geoDenied=%s',
      browserTimezone, this.geoCountry(), billingCountry, this.geoLocationDenied());

    try {
      await new Promise<void>((resolve, reject) => {
        this.http
          .patch<ApiResponse<FraudCheckResult>>(
            paymentApiUrls.updateFraudSignals(this.orderId!),
            {
              browserTimezone,
              geoCountry: this.geoCountry(),
              billingCountry,
              geoLocationDenied: this.geoLocationDenied(),
            },
            { withCredentials: true },
          )
          .subscribe({
            next: (res) => {
              console.debug('[FRAUD-DEBUG] Fraud signals result:', res.data);
              resolve();
            },
            error: (err) => {
              console.warn('[FRAUD-DEBUG] Fraud signals failed:', err);
              resolve(); // Non-blocking
            },
          });
      });
    } catch {
      // Non-blocking
    }
  }

  // ── LocalStorage Auto-save ──────────────────────────

  private get draftKey(): string {
    return this.orderId ? `checkout-draft:${this.orderId}` : '';
  }

  private setupAutoSave(): void {
    // Debounced auto-save via effect
    effect(() => {
      // Track all form signals
      const _bn = this.useDifferentBillingName();
      const _cn = this.customBillingName();
      const _vr = this.vatReverseCharge();
      const _vn = this.vatNumber();

      // Debounce
      if (this.autoSaveTimer) clearTimeout(this.autoSaveTimer);
      this.autoSaveTimer = setTimeout(() => this.saveDraft(), 1500);
    });

    // Also save on beforeunload
    this.beforeUnloadHandler = () => this.saveDraft();
    window.addEventListener('beforeunload', this.beforeUnloadHandler);
  }

  private saveDraft(): void {
    if (!this.draftKey) return;
    try {
      const draft: Record<string, unknown> = {
        useDifferentBillingName: this.useDifferentBillingName(),
        customBillingName: this.customBillingName(),
        vatReverseCharge: this.vatReverseCharge(),
        vatNumber: this.vatNumber(),
        timestamp: Date.now(),
      };

      // Try to get Stripe address values synchronously (best effort)
      if (this.addressElement) {
        this.addressElement.getValue().then(v => {
          if (v?.value) {
            draft["addressName"] = v.value.name;
            draft["addressCountry"] = v.value.address?.country;
            draft["addressLine1"] = v.value.address?.line1;
            draft["addressLine2"] = v.value.address?.line2;
            draft["addressCity"] = v.value.address?.city;
            draft["addressPostal"] = v.value.address?.postal_code;
            draft["addressState"] = v.value.address?.state;
          }
          localStorage.setItem(this.draftKey, JSON.stringify(draft));
          console.debug('[CHECKOUT-DRAFT] Saved draft:', this.draftKey);
        }).catch(() => {
          localStorage.setItem(this.draftKey, JSON.stringify(draft));
        });
      } else {
        localStorage.setItem(this.draftKey, JSON.stringify(draft));
      }
    } catch (e) {
      console.warn('[CHECKOUT-DRAFT] Save failed:', e);
    }
  }

  private restoreDraft(): void {
    if (!this.draftKey) return;
    try {
      const raw = localStorage.getItem(this.draftKey);
      if (!raw) return;
      const draft = JSON.parse(raw);

      // Check staleness (7 days)
      if (draft.timestamp && Date.now() - draft.timestamp > 7 * 24 * 60 * 60 * 1000) {
        localStorage.removeItem(this.draftKey);
        console.debug('[CHECKOUT-DRAFT] Removed stale draft');
        return;
      }

      // Restore form fields
      if (draft.useDifferentBillingName != null) this.useDifferentBillingName.set(draft.useDifferentBillingName);
      if (draft.customBillingName) this.customBillingName.set(draft.customBillingName);
      if (draft.vatReverseCharge != null) this.vatReverseCharge.set(draft.vatReverseCharge);
      if (draft.vatNumber) this.vatNumber.set(draft.vatNumber);

      console.debug('[CHECKOUT-DRAFT] Restored draft:', this.draftKey, draft);
    } catch (e) {
      console.warn('[CHECKOUT-DRAFT] Restore failed:', e);
    }
  }

  private getRestoredAddressDefaults(): Record<string, unknown> | null {
    if (!this.draftKey) return null;
    try {
      const raw = localStorage.getItem(this.draftKey);
      if (!raw) return null;
      const draft = JSON.parse(raw);
      if (!draft.addressCountry) return null;

      // Check if IP country changed (don't restore geo-dependent data)
      const geoCheck = this.geoCheck();
      if (geoCheck?.ipCountry && draft.addressCountry
          && geoCheck.ipCountry !== draft.addressCountry) {
        console.debug('[CHECKOUT-DRAFT] IP country changed (%s -> %s), skipping address restore',
          draft.addressCountry, geoCheck.ipCountry);
        return null;
      }

      return {
        name: draft.addressName ?? '',
        address: {
          country: draft.addressCountry,
          line1: draft.addressLine1 ?? '',
          line2: draft.addressLine2 ?? '',
          city: draft.addressCity ?? '',
          postal_code: draft.addressPostal ?? '',
          state: draft.addressState ?? '',
        },
      };
    } catch {
      return null;
    }
  }

  private clearDraft(): void {
    if (this.draftKey) {
      localStorage.removeItem(this.draftKey);
      console.debug('[CHECKOUT-DRAFT] Cleared draft:', this.draftKey);
    }
  }

    // ── Actions ──────────────────────────────────────────
  onDifferentBillingNameToggle(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.useDifferentBillingName.set(checked);
    if (!checked) {
      this.customBillingName.set('');
    }
  }

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
      if (!/^[A-Z]{2}\d{4,}/.test(vat.toUpperCase())) {
        this.vatError.set('Format de TVA invalide (ex: FR12345678901).');
        return;
      }
    }
    this.vatError.set(null);
    this.stripeError.set(null);

    this.submitting.set(true);

    // Save VAT preferences before payment
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

    // Save custom billing name on the order before confirming payment
    if (this.orderId && this.useDifferentBillingName() && this.customBillingName().trim()) {
      try {
        await new Promise<void>((resolve, reject) => {
          this.http
            .patch(
              paymentApiUrls.updateBillingName(this.orderId!),
              { billingName: this.customBillingName().trim() },
              { withCredentials: true },
            )
            .subscribe({ next: () => resolve(), error: reject });
        });
      } catch {
        // Non-blocking — billing name save failure shouldn't block payment
      }
    }

    // Send fraud signals (non-blocking)
    await this.sendFraudSignals();

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
    } else {
      this.clearDraft();
    }
  }

  goBack(): void {
    if (this.existingOrderMode) {
      void this.router.navigate(['/dashboard/orders']);
    } else {
      void this.router.navigate(['/services']);
    }
  }

  ngOnDestroy(): void {
    if (this.autoSaveTimer) clearTimeout(this.autoSaveTimer);
    if (this.beforeUnloadHandler) window.removeEventListener('beforeunload', this.beforeUnloadHandler);
    this.addressElement?.unmount();
    this.addressElement = null;
    this.paymentElement?.unmount();
    this.paymentElement = null;
    this.elements = null;
    this.stripe = null;
  }
}
