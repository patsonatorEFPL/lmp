package com.lmp.billing.dto;

import com.lmp.billing.domain.Order;

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
        String processingNotes
) {
    public static OrderResponse from(Order order) {
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
                order.getProcessingNotes());
    }
}
