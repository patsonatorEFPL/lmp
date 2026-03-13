package com.lmp.crm.web.api;

import com.lmp.crm.dto.AppointmentRequest;
import com.lmp.crm.domain.Appointment;
import com.lmp.crm.domain.AppointmentStatus;
import com.lmp.crm.repository.AppointmentRepository;
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
import java.util.stream.Collectors;

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
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public AppointmentRestController(AppointmentService appointmentService,
                                     AppointmentRepository appointmentRepository,
                                     UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.appointmentRepository = appointmentRepository;
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

            // Try to find the user by email (may be an existing registered user)
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

            Appointment appointment;

            if (existingUser.isPresent()) {
                // Registered user → use the full service (with notifications)
                logger.info("✅ Utilisateur existant trouvé pour {}", request.getEmail());
                appointment = appointmentService.createAppointment(
                        request.toAppointmentForm(), request.getEmail());
            } else {
                // Anonymous visitor → create appointment with client fields only
                logger.info("👤 Visiteur anonyme, création directe du rendez-vous");
                appointment = createAnonymousAppointment(request);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Rendez-vous créé avec succès", AppointmentResponse.from(appointment)));
        } catch (Exception e) {
            logger.error("❌ Erreur création rendez-vous: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Creates an appointment for an anonymous visitor (no User entity required).
     * Uses the clientName/clientEmail/clientPhone fields on the Appointment entity.
     */
    private Appointment createAnonymousAppointment(AppointmentRequest request) {
        // Validate required fields
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Le téléphone est obligatoire");
        }
        if (request.getDate() == null || request.getTime() == null) {
            throw new IllegalArgumentException("La date et l'heure sont obligatoires");
        }

        LocalDateTime appointmentDateTime = request.getAppointmentDateTime();

        // Check the date is in the future
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La date du rendez-vous doit être dans le futur");
        }

        Appointment appointment = new Appointment();
        appointment.setClientName(request.getName().trim());
        appointment.setClientEmail(request.getEmail().trim());
        appointment.setClientPhone(request.getPhone().trim());
        appointment.setSubject(request.getService() != null ? request.getService() : "Consultation générale");
        appointment.setDescription(request.getMessage());
        appointment.setAppointmentDate(appointmentDateTime);
        appointment.setDurationMinutes(60);
        appointment.setPriority(5);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setCreatedAt(LocalDateTime.now());

        appointment = appointmentRepository.save(appointment);

        logger.info("✅ Rendez-vous anonyme créé - ID: {}, Client: {}, Date: {}",
                appointment.getId(), request.getName(), appointmentDateTime);

        return appointment;
    }
}
