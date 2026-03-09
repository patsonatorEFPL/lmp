package com.lmp.web.controller.api.v1;

import com.lmp.domain.dto.AppointmentRequest;
import com.lmp.domain.entity.Appointment;
import com.lmp.service.AppointmentService;
import com.lmp.web.controller.api.v1.dto.ApiResponse;
import com.lmp.web.controller.api.v1.dto.AppointmentResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API REST pour les rendez-vous.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Gestion des rendez-vous")
public class AppointmentRestController {

    private final AppointmentService appointmentService;

    public AppointmentRestController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/available-slots")
    @Operation(summary = "Créneaux disponibles", description = "Retourne les créneaux disponibles pour une date")
    public ResponseEntity<ApiResponse<List<LocalDateTime>>> getAvailableSlots(
            @RequestParam String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            List<LocalDateTime> slots = appointmentService.getAvailableTimeSlots(localDate);
            return ResponseEntity.ok(ApiResponse.ok(slots));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid date format. Use yyyy-MM-dd"));
        }
    }

    @PostMapping
    @Operation(summary = "Créer un rendez-vous", description = "Crée un nouveau rendez-vous")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @RequestBody AppointmentRequest request,
            Authentication authentication) {
        try {
            Appointment appointment = appointmentService.createAppointment(
                    request.toAppointmentForm(), request.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Appointment created", AppointmentResponse.from(appointment)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
