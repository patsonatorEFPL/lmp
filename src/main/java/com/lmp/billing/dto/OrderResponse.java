package com.lmp.billing.dto;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.shared.pricing.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour une commande.
 */
public record OrderResponse(
        UUID id,
        String serviceName,
        BigDecimal totalAmount,
        String currency,
        String status,
        String paymentStatus,
        String paymentMethod,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        String userEmail,
        String userName,
        Integer progressPercentage,
        String progressStatus,
        String processingNotes,
        /** Lien /payment/guest?t=… pour les commandes invité encore en attente de paiement (admin uniquement). */
        String guestPaymentLink,
        // Facturation
        String billingName,
        String billingAddress,
        String billingCity,
        String billingPostalCode,
        String billingCountry,
        // TVA snapshot
        Boolean vatReverseCharge,
        String customerVatNumber,
        String vatCompanyName,
        /** Taux TVA appliqué au moment de la commande (ex: 0.21). */
        BigDecimal appliedVatRate,
        // FX snapshot
        BigDecimal amountBaseEur,
        /** Montant TTC en EUR (= totalAmount si EUR, sinon amountBaseEur × (1+TVA appliquée)). */
        BigDecimal totalAmountEur,
        // Fraud scoring
        String ipCountry,
        String ipAddress,
        BigDecimal vpnScore,
        String vpnSources,
        String browserTimezone,
        String geoCountry,
        String cardCountry,
        Integer fraudScore,
        String fraudFlags
) {
    public static OrderResponse from(Order order) {
        return forAdmin(order, null);
    }

    /**
     * Réponse admin : inclut {@code guestPaymentLink} lorsque la commande a un token de checkout actif
     * et est en {@link OrderStatus#PAYMENT_PENDING}.
     */
    public static OrderResponse forAdmin(Order order, String frontendBaseUrl) {
        String email = order.getUser() != null ? order.getUser().getEmail() : null;
        String name = order.getUser() != null ? order.getUser().getDisplayName() : null;

        return new OrderResponse(
                order.getId(),
                order.getServiceName(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                order.getCreatedAt(),
                order.getPaidAt(),
                email,
                name,
                order.getProgressPercentage(),
                order.getProgressStatus(),
                order.getProcessingNotes(),
                computeGuestPaymentLink(order, frontendBaseUrl),
                order.getBillingName(),
                order.getBillingAddress(),
                order.getBillingCity(),
                order.getBillingPostalCode(),
                order.getBillingCountry(),
                order.getVatReverseCharge(),
                order.getCustomerVatNumber(),
                order.getVatCompanyName(),
                order.getAppliedVatRate(),
                order.getAmountBaseEur(),
                computeTotalAmountEur(order),
                order.getIpCountry(),
                order.getIpAddress(),
                order.getVpnScore(),
                order.getVpnSources(),
                order.getBrowserTimezone(),
                order.getGeoCountry(),
                order.getCardCountry(),
                order.getFraudScore(),
                order.getFraudFlags());
    }

    /**
     * Calcule le montant TTC en EUR.
     * <ul>
     *   <li>Devise EUR → {@code totalAmount} est déjà TTC EUR</li>
     *   <li>Devise étrangère + reverse-charge → {@code amountBaseEur} (HT = TTC)</li>
     *   <li>Devise étrangère → {@code amountBaseEur × (1 + appliedVatRate)}</li>
     * </ul>
     * Utilise le taux TVA snapshoté sur la commande, avec fallback 0.20 pour les anciennes commandes.
     */
    private static BigDecimal computeTotalAmountEur(Order order) {
        String currency = order.getCurrency();
        if (currency == null || "EUR".equalsIgnoreCase(currency)) {
            return order.getTotalAmount();
        }
        BigDecimal baseEur = order.getAmountBaseEur();
        if (baseEur == null) {
            return order.getTotalAmount();
        }
        if (Boolean.TRUE.equals(order.getVatReverseCharge())) {
            return baseEur;
        }
        // Utiliser le taux snapshoté, fallback 0.20 pour les commandes avant migration
        BigDecimal vatRate = order.getAppliedVatRate();
        if (vatRate == null) {
            vatRate = new BigDecimal("0.20");
        }
        return MoneyUtils.multiply(baseEur, BigDecimal.ONE.add(vatRate));
    }

    /**
     * URL publique de paiement invité (pour détail commande admin).
     */
    public static String computeGuestPaymentLink(Order order, String frontendBaseUrl) {
        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            return null;
        }
        String token = order.getCheckoutToken();
        if (token == null || token.isBlank()) {
            return null;
        }
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return null;
        }
        String base = frontendBaseUrl.replaceAll("/$", "");
        return base + "/payment/guest?t=" + token.trim();
    }
}
