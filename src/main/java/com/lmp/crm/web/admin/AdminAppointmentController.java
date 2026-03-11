package com.lmp.crm.web.admin;

import com.lmp.crm.dto.AppointmentForm;
import com.lmp.crm.domain.Appointment;
import com.lmp.auth.domain.User;
import com.lmp.crm.domain.AppointmentStatus;
import com.lmp.crm.repository.AppointmentRepository;
import com.lmp.crm.service.AppointmentService;
import com.lmp.auth.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur d'administration pour la gestion des rendez-vous.
 * 
 * Fournit toutes les opérations CRUD pour les rendez-vous :
 * - Listage avec filtres et pagination
 * - Création et modification
 * - Changements de statut
 * - Suppression
 * - Export CSV
 */
@Controller
@RequestMapping("/admin/appointments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AdminAppointmentController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + AdminAppointmentController.class.getName());

        private final AppointmentService appointmentService;

        private final UserService userService;
    
        private final AppointmentRepository appointmentRepository;


    public AdminAppointmentController(AppointmentService appointmentService,
                           UserService userService,
                           AppointmentRepository appointmentRepository) {
        this.appointmentService = appointmentService;
        this.userService = userService;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Page principale de gestion des rendez-vous
     */
    @GetMapping
    public String listAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appointmentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String search,
            Model model,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        logger.info("Admin appointments list accessed by: {}", adminEmail);

        try {
            // Configuration de la pagination et du tri
            Sort sort = sortDirection.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            logger.debug("Listing appointments - page: {}, size: {}, sortBy: {}, sortDirection: {}, status: {}, search: {}",
                page, size, sortBy, sortDirection, status, search);

            // Récupération des rendez-vous avec filtres
            Page<Appointment> appointments;
            
            if (search != null && !search.trim().isEmpty()) {
                logger.debug("Searching appointments with keyword: {}", search.trim());
                appointments = appointmentService.searchAppointments(search.trim(), pageable);
            } else {
                logger.debug("Finding appointments with filters - status: {}, startDate: {}, endDate: {}", status, startDate, endDate);
                appointments = appointmentService.findAppointments(status, startDate, endDate, pageable);
            }
            
            logger.info("Found {} appointments (total: {})", appointments.getNumberOfElements(), appointments.getTotalElements());

            // Statistiques par statut
            Map<AppointmentStatus, Long> statusCounts = Map.of(
                AppointmentStatus.PENDING, appointmentService.countAppointmentsByStatus(AppointmentStatus.PENDING),
                AppointmentStatus.CONFIRMED, appointmentService.countAppointmentsByStatus(AppointmentStatus.CONFIRMED),
                AppointmentStatus.IN_PROGRESS, appointmentService.countAppointmentsByStatus(AppointmentStatus.IN_PROGRESS),
                AppointmentStatus.COMPLETED, appointmentService.countAppointmentsByStatus(AppointmentStatus.COMPLETED),
                AppointmentStatus.CANCELLED, appointmentService.countAppointmentsByStatus(AppointmentStatus.CANCELLED),
                AppointmentStatus.NO_SHOW, appointmentService.countAppointmentsByStatus(AppointmentStatus.NO_SHOW)
            );

            // Calcul des statistiques individuelles
            long totalAppointments = appointments.getTotalElements();
            long confirmedCount = statusCounts.get(AppointmentStatus.CONFIRMED);
            long pendingCount = statusCounts.get(AppointmentStatus.PENDING);
            long cancelledCount = statusCounts.get(AppointmentStatus.CANCELLED);
            long completedCount = statusCounts.get(AppointmentStatus.COMPLETED);
            long inProgressCount = statusCounts.get(AppointmentStatus.IN_PROGRESS);
            long noShowCount = statusCounts.get(AppointmentStatus.NO_SHOW);

            // Ajout des données au modèle
            model.addAttribute("appointments", appointments);
            model.addAttribute("statusCounts", statusCounts);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", appointments.getTotalPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDirection", sortDirection);
            model.addAttribute("status", status);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("search", search);
            model.addAttribute("allStatuses", Arrays.asList(AppointmentStatus.values()));
            
            // Statistiques pour les cartes du template
            model.addAttribute("totalAppointments", totalAppointments);
            model.addAttribute("confirmedCount", confirmedCount);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("cancelledCount", cancelledCount);
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("inProgressCount", inProgressCount);
            model.addAttribute("noShowCount", noShowCount);

            auditLogger.info("Admin appointments list viewed by: {} - {} appointments", 
                adminEmail, appointments.getTotalElements());

            return "admin/appointments";

        } catch (Exception e) {
            logger.error("Error loading appointments list for admin {}: {}", adminEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors du chargement des rendez-vous : " + e.getMessage());
            return "admin/appointments";
        }
    }

    /**
     * Affiche les détails d'un rendez-vous
     */
    @GetMapping("/{id}")
    public String viewAppointment(@PathVariable java.util.UUID id, Model model, Authentication authentication) {
        String adminEmail = authentication.getName();
        logger.info("Admin viewing appointment {} by: {}", id, adminEmail);

        try {
            Appointment appointment = appointmentService.findById(id);
            model.addAttribute("appointment", appointment);
            
            auditLogger.info("Appointment {} viewed by admin: {}", id, adminEmail);
            
            return "admin/appointment-details";

        } catch (Exception e) {
            logger.error("Error loading appointment {} for admin {}: {}", id, adminEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Rendez-vous non trouvé ou erreur : " + e.getMessage());
            return "redirect:/admin/appointments";
        }
    }

    /**
     * Formulaire de création d'un nouveau rendez-vous
     */
    @GetMapping("/new")
    public String newAppointmentForm(Model model, Authentication authentication) {
        String adminEmail = authentication.getName();
        logger.info("Admin creating new appointment by: {}", adminEmail);

        try {
            model.addAttribute("appointmentForm", new AppointmentForm());
            model.addAttribute("users", userService.findAll());
            model.addAttribute("allStatuses", Arrays.asList(AppointmentStatus.values()));
            
            return "admin/appointment-form";

        } catch (Exception e) {
            logger.error("Error loading new appointment form for admin {}: {}", adminEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors du chargement du formulaire : " + e.getMessage());
            return "redirect:/admin/appointments";
        }
    }

    /**
     * Sauvegarde d'un nouveau rendez-vous
     */
    @PostMapping("/save")
    public String saveAppointment(
            @Valid @ModelAttribute("appointmentForm") AppointmentForm appointmentForm,
            BindingResult bindingResult,
            @RequestParam java.util.UUID userId,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        logger.info("Admin saving new appointment by: {}", adminEmail);

        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userService.findAll());
            model.addAttribute("allStatuses", Arrays.asList(AppointmentStatus.values()));
            return "admin/appointment-form";
        }

        try {
            User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            Appointment appointment = appointmentService.createAppointment(appointmentForm, user);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Rendez-vous créé avec succès (ID: " + appointment.getId() + ")");
            
            auditLogger.info("New appointment {} created by admin: {}", appointment.getId(), adminEmail);
            
            return "redirect:/admin/appointments";

        } catch (Exception e) {
            logger.error("Error saving appointment by admin {}: {}", adminEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors de la création : " + e.getMessage());
            model.addAttribute("users", userService.findAll());
            model.addAttribute("allStatuses", Arrays.asList(AppointmentStatus.values()));
            return "admin/appointment-form";
        }
    }

    /**
     * Formulaire de modification d'un rendez-vous
     */
    @GetMapping("/{id}/edit")
    public String editAppointmentForm(@PathVariable java.util.UUID id, Model model, Authentication authentication) {
        String adminEmail = authentication.getName();
        logger.info("Admin editing appointment {} by: {}", id, adminEmail);

        try {
            Appointment appointment = appointmentService.findById(id);
            
            // Conversion vers AppointmentForm
            AppointmentForm appointmentForm = new AppointmentForm(
                appointment.getSubject(),
                appointment.getDescription(),
                appointment.getAppointmentDate(),
                appointment.getDurationMinutes(),
                appointment.getPriority()
            );

            model.addAttribute("appointmentForm", appointmentForm);
            model.addAttribute("appointment", appointment);
            model.addAttribute("users", userService.findAll());
            model.addAttribute("allStatuses", Arrays.asList(AppointmentStatus.values()));
            
            return "admin/appointment-form";

        } catch (Exception e) {
            logger.error("Error loading edit form for appointment {} by admin {}: {}", id, adminEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors du chargement : " + e.getMessage());
            return "redirect:/admin/appointments";
        }
    }

    /**
     * Mise à jour d'un rendez-vous existant
     */
    @PostMapping("/{id}/update")
    public String updateAppointment(
            @PathVariable java.util.UUID id,
            @Valid @ModelAttribute("appointmentForm") AppointmentForm appointmentForm,
            BindingResult bindingResult,
            @RequestParam(required = false) String adminNotes,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        logger.info("Admin updating appointment {} by: {}", id, adminEmail);

        if (bindingResult.hasErrors()) {
            try {
                Appointment appointment = appointmentService.findById(id);
                model.addAttribute("appointment", appointment);
                model.addAttribute("users", userService.findAll());
                model.addAttribute("allStatuses", Arrays.asList(AppointmentStatus.values()));
            } catch (Exception e) {
                logger.error("Error reloading appointment {} for validation errors", id, e);
            }
            return "admin/appointment-form";
        }

        try {
            Appointment appointment = appointmentService.updateAppointment(id, appointmentForm);
            
            // Mise à jour des notes administratives si fournies
            if (adminNotes != null && !adminNotes.trim().isEmpty()) {
                appointmentService.updateAdminNotes(id, adminNotes.trim());
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Rendez-vous mis à jour avec succès");
            
            auditLogger.info("Appointment {} updated by admin: {}", id, adminEmail);
            
            return "redirect:/admin/appointments/" + id;

        } catch (Exception e) {
            logger.error("Error updating appointment {} by admin {}: {}", id, adminEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors de la mise à jour : " + e.getMessage());
            try {
                Appointment appointment = appointmentService.findById(id);
                model.addAttribute("appointment", appointment);
                model.addAttribute("users", userService.findAll());
                model.addAttribute("allStatuses", Arrays.asList(AppointmentStatus.values()));
            } catch (Exception ex) {
                logger.error("Error reloading appointment {} after update error", id, ex);
            }
            return "admin/appointment-form";
        }
    }

    /**
     * Annulation d'un rendez-vous (soft delete via statut CANCELLED)
     */
    @PostMapping("/{id}/cancel-soft")
    public String cancelAppointmentSoft(
            @PathVariable java.util.UUID id,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        logger.info("Admin soft-cancelling appointment {} by: {}", id, adminEmail);

        try {
            appointmentService.cancelAppointment(id, "Annulé par l'administrateur", adminEmail);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Rendez-vous annulé avec succès");
            
            auditLogger.info("Appointment {} cancelled (soft delete) by admin: {}", id, adminEmail);
            
            return "redirect:/admin/appointments";

        } catch (Exception e) {
            logger.error("Error cancelling appointment {} by admin {}: {}", id, adminEmail, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de l'annulation : " + e.getMessage());
            return "redirect:/admin/appointments";
        }
    }
    
    /**
     * Suppression définitive d'un rendez-vous (hard delete)
     */
    @PostMapping("/{id}/delete")
    public String deleteAppointmentPermanent(
            @PathVariable java.util.UUID id,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        logger.info("Admin permanently deleting appointment {} by: {}", id, adminEmail);

        try {
            appointmentService.deleteAppointment(id, adminEmail);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Rendez-vous supprimé définitivement avec succès");
            
            auditLogger.warn("Appointment {} permanently deleted by admin: {}", id, adminEmail);
            
            return "redirect:/admin/appointments";

        } catch (Exception e) {
            logger.error("Error permanently deleting appointment {} by admin {}: {}", id, adminEmail, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la suppression définitive : " + e.getMessage());
            return "redirect:/admin/appointments";
        }
    }

    // ======== ACTIONS DE CHANGEMENT DE STATUT ========

    /**
     * Confirme un rendez-vous
     */
    @PostMapping("/{id}/confirm")
    @ResponseBody
    public Map<String, Object> confirmAppointment(@PathVariable java.util.UUID id, Authentication authentication) {
        String adminEmail = authentication.getName();
        
        try {
            Appointment appointment = appointmentService.confirmAppointment(id, adminEmail);
            auditLogger.info("Appointment {} confirmed by admin: {}", id, adminEmail);
            
            return Map.of(
                "success", true,
                "message", "Rendez-vous confirmé avec succès",
                "newStatus", appointment.getStatus().getDisplayName()
            );
        } catch (Exception e) {
            logger.error("Error confirming appointment {} by admin {}: {}", id, adminEmail, e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Erreur : " + e.getMessage()
            );
        }
    }

    /**
     * Annule un rendez-vous
     */
    @PostMapping("/{id}/cancel")
    @ResponseBody
    public Map<String, Object> cancelAppointment(
            @PathVariable java.util.UUID id,
            @RequestParam(defaultValue = "Annulé par l'administrateur") String reason,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        
        try {
            Appointment appointment = appointmentService.cancelAppointment(id, reason, adminEmail);
            auditLogger.info("Appointment {} cancelled by admin: {} - Reason: {}", id, adminEmail, reason);
            
            return Map.of(
                "success", true,
                "message", "Rendez-vous annulé avec succès",
                "newStatus", appointment.getStatus().getDisplayName()
            );
        } catch (Exception e) {
            logger.error("Error cancelling appointment {} by admin {}: {}", id, adminEmail, e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Erreur : " + e.getMessage()
            );
        }
    }

    /**
     * Démarre un rendez-vous
     */
    @PostMapping("/{id}/start")
    @ResponseBody
    public Map<String, Object> startAppointment(@PathVariable java.util.UUID id, Authentication authentication) {
        String adminEmail = authentication.getName();
        
        try {
            Appointment appointment = appointmentService.startAppointment(id);
            auditLogger.info("Appointment {} started by admin: {}", id, adminEmail);
            
            return Map.of(
                "success", true,
                "message", "Rendez-vous démarré avec succès",
                "newStatus", appointment.getStatus().getDisplayName()
            );
        } catch (Exception e) {
            logger.error("Error starting appointment {} by admin {}: {}", id, adminEmail, e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Erreur : " + e.getMessage()
            );
        }
    }

    /**
     * Termine un rendez-vous
     */
    @PostMapping("/{id}/complete")
    @ResponseBody
    public Map<String, Object> completeAppointment(
            @PathVariable java.util.UUID id,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        
        try {
            Appointment appointment = appointmentService.completeAppointment(id, notes);
            auditLogger.info("Appointment {} completed by admin: {}", id, adminEmail);
            
            return Map.of(
                "success", true,
                "message", "Rendez-vous terminé avec succès",
                "newStatus", appointment.getStatus().getDisplayName()
            );
        } catch (Exception e) {
            logger.error("Error completing appointment {} by admin {}: {}", id, adminEmail, e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Erreur : " + e.getMessage()
            );
        }
    }

    /**
     * Marque un rendez-vous comme "no-show"
     */
    @PostMapping("/{id}/no-show")
    @ResponseBody
    public Map<String, Object> markAsNoShow(@PathVariable java.util.UUID id, Authentication authentication) {
        String adminEmail = authentication.getName();
        
        try {
            Appointment appointment = appointmentService.markAsNoShow(id);
            auditLogger.info("Appointment {} marked as no-show by admin: {}", id, adminEmail);
            
            return Map.of(
                "success", true,
                "message", "Rendez-vous marqué comme absence",
                "newStatus", appointment.getStatus().getDisplayName()
            );
        } catch (Exception e) {
            logger.error("Error marking appointment {} as no-show by admin {}: {}", id, adminEmail, e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Erreur : " + e.getMessage()
            );
        }
    }
    
    /**
     * Supprime définitivement un rendez-vous (AJAX)
     */
    @PostMapping("/{id}/delete-permanent")
    @ResponseBody
    public Map<String, Object> deleteAppointmentPermanentAjax(@PathVariable java.util.UUID id, Authentication authentication) {
        String adminEmail = authentication.getName();
        
        try {
            appointmentService.deleteAppointment(id, adminEmail);
            auditLogger.warn("Appointment {} permanently deleted by admin: {}", id, adminEmail);
            
            return Map.of(
                "success", true,
                "message", "Rendez-vous supprimé définitivement avec succès"
            );
        } catch (Exception e) {
            logger.error("Error permanently deleting appointment {} by admin {}: {}", id, adminEmail, e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Erreur : " + e.getMessage()
            );
        }
    }

    /**
     * Test endpoint simple pour vérifier le fonctionnement
     */
    @GetMapping("/test")
    @ResponseBody
    public Map<String, Object> testEndpoint() {
        return Map.of(
            "status", "OK",
            "timestamp", LocalDateTime.now(),
            "message", "AdminAppointmentController is working"
        );
    }

    /**
     * Page de test et diagnostic complète
     */
    @GetMapping("/test-page")
    public String testPage(Model model, Authentication authentication) {
        String adminEmail = authentication.getName();
        logger.info("Accès à la page de test et diagnostic des rendez-vous par: {}", adminEmail);
        return "admin/appointments-test";
    }
    
    /**
     * Test de la logique de détection des conflits
     */
    @GetMapping("/test-conflicts")
    @ResponseBody
    public Map<String, Object> testConflictDetection(
            @RequestParam String startTime,
            @RequestParam Integer duration,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        logger.info("Test de détection des conflits par: {}", adminEmail);
        
        try {
            LocalDateTime appointmentDate = LocalDateTime.parse(startTime);
            LocalDateTime endTime = appointmentDate.plusMinutes(duration);
            
            // Récupérer tous les rendez-vous actifs pour comparaison
            List<Appointment> allActiveAppointments = appointmentService.findActiveAppointments();
            
            // Récupérer les conflits avec la nouvelle logique
            List<Appointment> conflicts = getConflictingAppointmentsForTest(appointmentDate, endTime);
            
            // Créer un rapport détaillé
            List<Map<String, Object>> conflictDetails = conflicts.stream().map(appointment -> {
                LocalDateTime conflictEnd = appointment.getAppointmentDate().plusMinutes(appointment.getDurationMinutes());
                Map<String, Object> details = new HashMap<>();
                details.put("id", appointment.getId());
                details.put("subject", appointment.getSubject());
                details.put("start", appointment.getAppointmentDate().toString());
                details.put("end", conflictEnd.toString());
                details.put("duration", appointment.getDurationMinutes());
                details.put("status", appointment.getStatus().name());
                return details;
            }).collect(Collectors.toList());
            
            List<Map<String, Object>> allAppointments = allActiveAppointments.stream().map(appointment -> {
                LocalDateTime apptEnd = appointment.getAppointmentDate().plusMinutes(appointment.getDurationMinutes());
                Map<String, Object> apptDetails = new HashMap<>();
                apptDetails.put("id", appointment.getId());
                apptDetails.put("subject", appointment.getSubject());
                apptDetails.put("start", appointment.getAppointmentDate().toString());
                apptDetails.put("end", apptEnd.toString());
                apptDetails.put("duration", appointment.getDurationMinutes());
                apptDetails.put("status", appointment.getStatus().name());
                apptDetails.put("isConflict", conflicts.stream().anyMatch(c -> c.getId().equals(appointment.getId())));
                return apptDetails;
            }).collect(Collectors.toList());
            
            return Map.of(
                "success", true,
                "testRequest", Map.of(
                    "start", startTime,
                    "end", endTime.toString(),
                    "duration", duration
                ),
                "conflictsFound", conflicts.size(),
                "conflictDetails", conflictDetails,
                "totalActiveAppointments", allActiveAppointments.size(),
                "allAppointments", allAppointments,
                "message", conflicts.isEmpty() ? 
                    "Aucun conflit détecté - créneau libre" : 
                    conflicts.size() + " conflit(s) détecté(s)"
            );
            
        } catch (Exception e) {
            logger.error("Erreur lors du test de détection des conflits: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Méthode helper pour les tests de conflits
     */
    private List<Appointment> getConflictingAppointmentsForTest(LocalDateTime startTime, LocalDateTime endTime) {
        List<Appointment> activeAppointments = appointmentService.findActiveAppointments();
        List<Appointment> conflicts = new ArrayList<>();
        
        for (Appointment appointment : activeAppointments) {
            LocalDateTime appointmentStart = appointment.getAppointmentDate();
            LocalDateTime appointmentEnd = appointmentStart.plusMinutes(appointment.getDurationMinutes());
            
            // Vérifier le chevauchement
            if (startTime.isBefore(appointmentEnd) && endTime.isAfter(appointmentStart)) {
                conflicts.add(appointment);
            }
        }
        
        return conflicts;
    }
    
    /**
     * Page simple pour le debug
     */
    @GetMapping("/simple")
    public String listAppointmentsSimple(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        logger.info("Admin appointments simple list accessed by: {}", adminEmail);

        try {
            // Configuration simple
            Pageable pageable = PageRequest.of(page, size, Sort.by("appointmentDate").descending());

            // Récupération des rendez-vous sans filtres
            Page<Appointment> appointments = appointmentService.findAppointments(null, null, null, pageable);

            // Statistiques par statut
            Map<AppointmentStatus, Long> statusCounts = Map.of(
                AppointmentStatus.PENDING, appointmentService.countAppointmentsByStatus(AppointmentStatus.PENDING),
                AppointmentStatus.CONFIRMED, appointmentService.countAppointmentsByStatus(AppointmentStatus.CONFIRMED),
                AppointmentStatus.IN_PROGRESS, appointmentService.countAppointmentsByStatus(AppointmentStatus.IN_PROGRESS),
                AppointmentStatus.COMPLETED, appointmentService.countAppointmentsByStatus(AppointmentStatus.COMPLETED),
                AppointmentStatus.CANCELLED, appointmentService.countAppointmentsByStatus(AppointmentStatus.CANCELLED),
                AppointmentStatus.NO_SHOW, appointmentService.countAppointmentsByStatus(AppointmentStatus.NO_SHOW)
            );

            // Ajout des données au modèle
            model.addAttribute("appointments", appointments);
            model.addAttribute("statusCounts", statusCounts);
            model.addAttribute("pageSize", size);

            logger.info("Simple appointments list loaded: {} appointments (total: {})", 
                appointments.getNumberOfElements(), appointments.getTotalElements());

            return "admin/appointments-simple";

        } catch (Exception e) {
            logger.error("Error loading simple appointments list for admin {}: {}", adminEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors du chargement des rendez-vous : " + e.getMessage());
            return "admin/appointments-simple";
        }
    }

    /**
     * Crée des données de test pour les rendez-vous
     */
    @PostMapping("/create-test-data")
    @ResponseBody
    public Map<String, Object> createTestData(Authentication authentication) {
        String adminEmail = authentication.getName();
        logger.info("Creating test appointment data by admin: {}", adminEmail);
        
        try {
            // Récupérer un utilisateur existant
            List<User> users = userService.findAll();
            if (users.isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "Aucun utilisateur trouvé pour créer des rendez-vous de test"
                );
            }
            
            User testUser = users.get(0); // Prendre le premier utilisateur
            int createdCount = 0;
            
            // Créer plusieurs rendez-vous de test
            String[] subjects = {
                "Consultation - Développement web",
                "Réunion - Design graphique", 
                "Formation - Référencement SEO",
                "Audit - Sécurité web",
                "Support - Maintenance site"
            };
            
            AppointmentStatus[] statuses = {
                AppointmentStatus.PENDING,
                AppointmentStatus.CONFIRMED, 
                AppointmentStatus.IN_PROGRESS,
                AppointmentStatus.COMPLETED,
                AppointmentStatus.CANCELLED
            };
            
            for (int i = 0; i < subjects.length; i++) {
                AppointmentForm form = new AppointmentForm();
                form.setSubject(subjects[i]);
                form.setDescription("Rendez-vous de test créé automatiquement pour vérifier le système.");
                form.setAppointmentDate(LocalDateTime.now().plusDays(i + 1).withHour(10).withMinute(0));
                form.setDurationMinutes(60);
                form.setPriority(5);
                
                try {
                    Appointment appointment = appointmentService.createAppointment(form, testUser);
                    // Mettre à jour le statut si nécessaire
                    if (i < statuses.length) {
                        appointment.setStatus(statuses[i]);
                        appointmentService.updateAdminNotes(appointment.getId(), "Statut de test: " + statuses[i].getDisplayName());
                    }
                    createdCount++;
                } catch (Exception e) {
                    logger.warn("Could not create test appointment {}: {}", i, e.getMessage());
                }
            }
            
            return Map.of(
                "success", true,
                "message", createdCount + " rendez-vous de test créés avec succès",
                "createdCount", createdCount,
                "testUser", testUser.getEmail()
            );
            
        } catch (Exception e) {
            logger.error("Error creating test data: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Erreur lors de la création des données de test: " + e.getMessage()
            );
        }
    }

    /**
     * Debug endpoint pour vérifier les données
     */
    @GetMapping("/debug")
    @ResponseBody
    public Map<String, Object> debugAppointments(Authentication authentication) {
        String adminEmail = authentication.getName();
        logger.info("Debug appointments accessed by: {}", adminEmail);
        
        try {
            // Statistiques par statut
            Map<AppointmentStatus, Long> statusCounts = Map.of(
                AppointmentStatus.PENDING, appointmentService.countAppointmentsByStatus(AppointmentStatus.PENDING),
                AppointmentStatus.CONFIRMED, appointmentService.countAppointmentsByStatus(AppointmentStatus.CONFIRMED),
                AppointmentStatus.IN_PROGRESS, appointmentService.countAppointmentsByStatus(AppointmentStatus.IN_PROGRESS),
                AppointmentStatus.COMPLETED, appointmentService.countAppointmentsByStatus(AppointmentStatus.COMPLETED),
                AppointmentStatus.CANCELLED, appointmentService.countAppointmentsByStatus(AppointmentStatus.CANCELLED),
                AppointmentStatus.NO_SHOW, appointmentService.countAppointmentsByStatus(AppointmentStatus.NO_SHOW)
            );
            
            // Récupérer tous les rendez-vous
            Pageable pageable = PageRequest.of(0, 100); // Premier 100
            Page<Appointment> appointments = appointmentService.findAppointments(null, null, null, pageable);
            
            return Map.of(
                "statusCounts", statusCounts,
                "totalAppointments", appointments.getTotalElements(),
                "appointmentsOnPage", appointments.getNumberOfElements(),
                "totalPages", appointments.getTotalPages(),
                "currentPage", appointments.getNumber(),
                "appointments", appointments.getContent().stream().map(apt -> Map.of(
                    "id", apt.getId(),
                    "subject", apt.getSubject(),
                    "status", apt.getStatus(),
                    "clientName", apt.getEffectiveClientName(),
                    "clientEmail", apt.getEffectiveClientEmail(),
                    "appointmentDate", apt.getAppointmentDate().toString()
                )).toList()
            );
        } catch (Exception e) {
            logger.error("Error in debug endpoint: {}", e.getMessage(), e);
            return Map.of(
                "error", e.getMessage(),
                "stackTrace", e.getStackTrace()
            );
        }
    }

    /**
     * Export CSV des rendez-vous
     */
    @GetMapping("/export")
    public void exportAppointments(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String search,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        String adminEmail = authentication.getName();
        logger.info("Admin exporting appointments by: {}", adminEmail);

        // Configuration de la réponse HTTP
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"rendez-vous-" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".csv\"");

        try (PrintWriter writer = response.getWriter()) {
            // En-tête CSV
            writer.println("ID,Client,Email,Téléphone,Sujet,Date,Statut,Durée (min),Priorité,Créé le,Notes Admin");

            // Récupération des données (sans pagination pour l'export)
            Page<Appointment> appointments;
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            
            if (search != null && !search.trim().isEmpty()) {
                appointments = appointmentService.searchAppointments(search.trim(), pageable);
            } else {
                appointments = appointmentService.findAppointments(status, startDate, endDate, pageable);
            }

            // Écriture des données
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            for (Appointment appointment : appointments.getContent()) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\",\"%d\",\"%s\",\"%s\"%n",
                    appointment.getId(),
                    appointment.getEffectiveClientName(),
                    appointment.getEffectiveClientEmail(),
                    appointment.getEffectiveClientPhone() != null ? appointment.getEffectiveClientPhone() : "",
                    appointment.getSubject(),
                    appointment.getAppointmentDate().format(dateFormatter),
                    appointment.getStatus().getDisplayName(),
                    appointment.getDurationMinutes(),
                    appointment.getPriority(),
                    appointment.getCreatedAt().format(dateFormatter),
                    appointment.getAdminNotes() != null ? appointment.getAdminNotes().replace("\"", "\"\"") : ""
                );
            }

            auditLogger.info("Appointments exported by admin: {} - {} records", adminEmail, appointments.getTotalElements());

        } catch (Exception e) {
            logger.error("Error exporting appointments by admin {}: {}", adminEmail, e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur lors de l'export");
        }
    }
}
