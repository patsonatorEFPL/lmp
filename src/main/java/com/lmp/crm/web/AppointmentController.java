package com.lmp.crm.web;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lmp.crm.dto.AppointmentForm;
import com.lmp.crm.dto.AppointmentRequest;
import com.lmp.crm.domain.Appointment;
import com.lmp.auth.domain.User;
import com.lmp.crm.domain.AppointmentStatus;
import com.lmp.crm.service.AppointmentService;
import com.lmp.auth.service.UserService;

import jakarta.validation.Valid;

/**
 * Contrôleur pour la gestion des rendez-vous professionnels.
 * 
 * Gère les endpoints pour la prise, modification, annulation et consultation
 * des rendez-vous avec validation des créneaux horaires et notifications automatiques.
 */
@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserService userService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

    public AppointmentController(AppointmentService appointmentService, UserService userService) {
        this.appointmentService = appointmentService;
        this.userService = userService;
    }

    // ================================
    // ENDPOINTS POUR LES UTILISATEURS
    // ================================

    /**
     * Créer un nouveau rendez-vous
     */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createAppointment(@Valid @RequestBody AppointmentRequest request, 
                                             BindingResult result, 
                                             Principal principal,
                                             HttpServletRequest httpRequest) {
        try {
            // Protection anti-spam : Vérifier l'origine de la requête
            String userAgent = httpRequest.getHeader("User-Agent");
            String referer = httpRequest.getHeader("Referer");
            
            // Bloquer les requêtes sans User-Agent (bots simples)
            if (userAgent == null || userAgent.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Requête invalide", null)
                );
            }
            
            // Validation des données du formulaire
            if (result.hasErrors()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Données invalides", result.getAllErrors())
                );
            }

            Appointment appointment;
            
            if (principal != null) {
                // Utilisateur connecté - création normale
                User user = userService.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                appointment = appointmentService.createAppointment(request.toAppointmentForm(), user);
            } else {
                // Utilisateur anonyme - création sans compte
                appointment = appointmentService.createAnonymousAppointment(request);
            }

            return ResponseEntity.ok(new ApiResponse(true, 
                "Rendez-vous créé avec succès. Un email de confirmation vous a été envoyé.", 
                appointment.getId()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, e.getMessage(), null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse(false, "Erreur lors de la création du rendez-vous", null)
            );
        }
    }

    /**
     * Obtenir les créneaux disponibles pour une date donnée
     */
    @GetMapping("/available-slots")
    @ResponseBody
    public ResponseEntity<List<String>> getAvailableSlots(@RequestParam("date") String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            List<LocalDateTime> availableSlots = appointmentService.getAvailableTimeSlots(date);
            
            // Convertir les LocalDateTime en format HH:mm pour le frontend
            List<String> timeSlots = availableSlots.stream()
                .map(dateTime -> dateTime.toLocalTime().toString()) // Convertit en HH:mm
                .toList();
                
            return ResponseEntity.ok(timeSlots);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obtenir les rendez-vous de l'utilisateur connecté
     */
    @GetMapping("/my-appointments")
    @ResponseBody
    public ResponseEntity<Page<Appointment>> getMyAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appointmentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) AppointmentStatus status,
            Principal principal) {
        
        try {
            User user = userService.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            
            Page<Appointment> appointments;
            if (status != null) {
                appointments = appointmentService.findByUserAndStatus(user, status, pageRequest);
            } else {
                appointments = appointmentService.findByUser(user, pageRequest);
            }

            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtenir un rendez-vous spécifique de l'utilisateur
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Appointment> getAppointment(@PathVariable java.util.UUID id, Principal principal) {
        try {
            User user = userService.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Appointment appointment = appointmentService.findById(id);

            // Vérifier que l'utilisateur est propriétaire du rendez-vous
            if (!appointment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Modifier un rendez-vous existant
     */
    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> updateAppointment(@PathVariable java.util.UUID id,
                                             @Valid @RequestBody AppointmentForm form,
                                             BindingResult result,
                                             Principal principal) {
        try {
            if (result.hasErrors()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse(false, "Données invalides", result.getAllErrors())
                );
            }

            User user = userService.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Appointment updatedAppointment = appointmentService.updateAppointment(id, form, user);

            return ResponseEntity.ok(new ApiResponse(true, 
                "Rendez-vous modifié avec succès", updatedAppointment.getId()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, e.getMessage(), null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse(false, "Erreur lors de la modification du rendez-vous", null)
            );
        }
    }

    /**
     * Annuler un rendez-vous
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> cancelAppointment(@PathVariable java.util.UUID id,
                                             @RequestParam(required = false) String reason,
                                             Principal principal) {
        try {
            User user = userService.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            appointmentService.cancelAppointment(id, reason != null ? reason : "Annulé par le client", user);

            return ResponseEntity.ok(new ApiResponse(true, 
                "Rendez-vous annulé avec succès", null));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, e.getMessage(), null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse(false, "Erreur lors de l'annulation du rendez-vous", null)
            );
        }
    }

    // =================================
    // ENDPOINTS POUR LES ADMINISTRATEURS
    // =================================

    /**
     * Redirige l'ancienne page d'administration des rendez-vous vers Angular.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAppointments() {
        return "redirect:" + frontendUrl + "/admin/appointments";
    }

    /**
     * Liste tous les rendez-vous (admin)
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<Page<Appointment>> getAllAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "appointmentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            
            LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
            LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;
            
            Page<Appointment> appointments = appointmentService.findAppointments(
                status, start, end, pageRequest);

            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Confirmer un rendez-vous (admin)
     */
    @PutMapping("/admin/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> confirmAppointment(@PathVariable java.util.UUID id) {
        try {
            appointmentService.confirmAppointment(id);
            return ResponseEntity.ok(new ApiResponse(true, 
                "Rendez-vous confirmé avec succès", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, e.getMessage(), null)
            );
        }
    }

    /**
     * Démarrer un rendez-vous (admin)
     */
    @PutMapping("/admin/{id}/start")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> startAppointment(@PathVariable java.util.UUID id) {
        try {
            appointmentService.startAppointment(id);
            return ResponseEntity.ok(new ApiResponse(true, 
                "Rendez-vous démarré avec succès", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, e.getMessage(), null)
            );
        }
    }

    /**
     * Terminer un rendez-vous (admin)
     */
    @PutMapping("/admin/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> completeAppointment(@PathVariable java.util.UUID id) {
        try {
            appointmentService.completeAppointment(id, "Rendez-vous terminé via interface admin");
            return ResponseEntity.ok(new ApiResponse(true, 
                "Rendez-vous terminé avec succès", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, e.getMessage(), null)
            );
        }
    }

    /**
     * Ajouter des notes administratives
     */
    @PutMapping("/admin/{id}/notes")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> updateAdminNotes(@PathVariable java.util.UUID id, 
                                            @RequestBody String notes) {
        try {
            appointmentService.updateAdminNotes(id, notes);
            return ResponseEntity.ok(new ApiResponse(true, 
                "Notes mises à jour avec succès", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse(false, e.getMessage(), null)
            );
        }
    }

    // =================================
    // CLASSES DE RÉPONSE API
    // =================================

    /**
     * Classe pour structurer les réponses API
     */
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters et setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}