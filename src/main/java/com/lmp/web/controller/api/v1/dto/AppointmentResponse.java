package com.lmp.web.controller.api.v1.dto;

import com.lmp.domain.entity.Appointment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour un rendez-vous.
 */
public record AppointmentResponse(
        UUID id,
        String clientName,
        String clientEmail,
        String clientPhone,
        LocalDateTime appointmentDate,
        String subject,
        String description,
        String status,
        Integer durationMinutes,
        Integer priority,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getClientName(),
                appointment.getClientEmail(),
                appointment.getClientPhone(),
                appointment.getAppointmentDate(),
                appointment.getSubject(),
                appointment.getDescription(),
                appointment.getStatus() != null ? appointment.getStatus().name() : null,
                appointment.getDurationMinutes(),
                appointment.getPriority(),
                appointment.getCreatedAt(),
                appointment.getConfirmedAt());
    }
}
