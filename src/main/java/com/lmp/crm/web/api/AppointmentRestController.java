package com.lmp.crm.web.api;

import com.lmp.crm.dto.AppointmentRequest;
import com.lmp.crm.domain.Appointment;
import com.lmp.crm.service.AppointmentService;
import com.lmp.auth.domain.User;
import com.lmp.auth.repository.UserRepository;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.crm.dto.AppointmentResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * API REST pour les rendez-vous.
 * Le POST est public (utilisable depuis le modal de la page d'accueil).
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Gestion des rendez-vous")
public class AppointmentRestController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentRestController.class);

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    public AppointmentRestController(AppointmentService appointmentService,
                                     UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
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
    @Operation(summary = "Créer un rendez-vous", description = "Crée un nouveau rendez-vous (public, fonctionne pour les visiteurs anonymes)")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @RequestBody AppointmentRequest request,
            Authentication authentication) {
        try {
            logger.info("📅 Demande de rendez-vous reçue - Nom: {}, Email: {}, Service: {}, Date: {} {}",
                    request.getName(), request.getEmail(), request.getService(),
                    request.getDate(), request.getTime());

            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

            Appointment appointment;

            if (existingUser.isPresent()) {
                logger.info("✅ Utilisateur existant trouvé pour {}", request.getEmail());
                appointment = appointmentService.createAppointment(
                        request.toAppointmentForm(), request.getEmail());
            } else {
                logger.info("👤 Visiteur anonyme, création via AppointmentService");
                appointment = appointmentService.createAnonymousAppointment(request);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Rendez-vous créé avec succès", AppointmentResponse.from(appointment)));
        } catch (Exception e) {
            logger.error("❌ Erreur création rendez-vous: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
