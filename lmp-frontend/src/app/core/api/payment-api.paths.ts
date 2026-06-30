import { environment } from '../../../environments/environment';

/** Préfixe REST paiements (v1). */
const PAYMENTS_V1_BASE = '/api/v1/payments';

const root = (): string => `${environment.apiUrl}${PAYMENTS_V1_BASE}`;

/**
 * URLs complètes des endpoints paiement côté backend.
 * Point unique pour renommer des chemins ou changer de base API.
 */
export const paymentApiUrls = {
  paymentStatus: (orderId: string): string => `${root()}/status/${orderId}`,

  /** Réconciliation immédiate avec le prestataire (polling de secours après retour checkout). */
  paymentReconcile: (orderId: string): string => `${root()}/status/${orderId}/reconcile`,

  checkoutPreview: (): string => `${root()}/checkout-preview`,

  /** Detect existing pending/active orders for this user+offer before navigating to checkout. */
  checkoutAvailability: (): string => `${root()}/checkout-availability`,

  checkoutPaymentElement: (): string => `${root()}/checkout/payment-element`,

  checkoutOrderPaymentElement: (orderId: string): string =>
    `${root()}/checkout-order/${encodeURIComponent(orderId)}/payment-element`,

  updateBillingName: (orderId: string): string =>
    `${root()}/orders/${encodeURIComponent(orderId)}/billing-name`,

  guestOrderPreview: (token: string): string =>
    `${root()}/guest-order/preview/${encodeURIComponent(token)}`,

  guestOrderAttach: (): string => `${root()}/guest-order/attach`,

  guestOrderPrepare: (): string => `${root()}/guest-order/prepare`,

  /** Geo + VPN check for the current client IP. */
  geoCheck: (): string => `${root()}/geo-check`,

  /** Record fraud signals (timezone, geolocation, billing country) on an order. */
  updateFraudSignals: (orderId: string): string =>
    `${root()}/orders/${encodeURIComponent(orderId)}/fraud-signals`,

  /** Validate a VAT number via VIES. */
  vatValidate: (): string => `${root()}/vat/validate`,

  /** Finalize checkout: re-apply VAT snapshot and update PaymentIntent amount. */
  finalizeCheckout: (orderId: string): string =>
    `${root()}/orders/${encodeURIComponent(orderId)}/finalize-checkout`,
} as const;
