package com.lmp.billing.dto;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatus;

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
        String guestPaymentLink
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
                computeGuestPaymentLink(order, frontendBaseUrl));
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
