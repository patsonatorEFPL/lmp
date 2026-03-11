package com.lmp.portal.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de réponse pour les statistiques du tableau de bord utilisateur.
 */
public record DashboardStatsResponse(
        long totalOrders,
        long completedOrders,
        long inProgressOrders,
        long totalReviews,
        long upcomingAppointments,
        BigDecimal totalSpent,
        List<RecentOrderDto> recentOrders,
        List<RecentReviewDto> recentReviews,
        List<UpcomingAppointmentDto> upcomingAppointmentsList
) {

    public record RecentOrderDto(
            String id,
            String serviceName,
            String status,
            BigDecimal totalAmount,
            String currency,
            String createdAt
    ) {}

    public record RecentReviewDto(
            String id,
            int rating,
            String comment,
            boolean approved,
            String createdAt
    ) {}

    public record UpcomingAppointmentDto(
            String id,
            String subject,
            String status,
            String appointmentDate,
            int durationMinutes
    ) {}
}
