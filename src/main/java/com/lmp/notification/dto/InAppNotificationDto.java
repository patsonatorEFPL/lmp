package com.lmp.notification.dto;

/**
 * DTO pour les notifications in-app retournées au frontend.
 */
public record InAppNotificationDto(
        String id,
        String type,
        String message,
        String orderId,
        String serviceName,
        Double amount,
        boolean read,
        String timestamp
) {}
