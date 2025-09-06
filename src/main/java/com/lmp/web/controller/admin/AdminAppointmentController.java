package com.lmp.web.controller.admin;

import com.lmp.domain.dto.AppointmentForm;
import com.lmp.domain.entity.Appointment;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.AppointmentStatus;
import com.lmp.service.AppointmentService;
import com.lmp.service.user.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserService userService;

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

            // Ajout des données au modèle
            model.addAttribute("appointments", appointments);
            model.addAttribute("statusCounts", statusCounts);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDirection", sortDirection);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("search", search);
            model.addAttribute("allStatuses", Arrays.asList(AppointmentStatus.values()));

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
    public String viewAppointment(@PathVariable Long id, Model model, Authentication authentication) {
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
            @RequestParam Long userId,
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
    public String editAppointmentForm(@PathVariable Long id, Model model, Authentication authentication) {
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
            @PathVariable Long id,
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
     * Suppression d'un rendez-vous (soft delete via statut CANCELLED)
     */
    @PostMapping("/{id}/delete")
    public String deleteAppointment(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        logger.info("Admin deleting appointment {} by: {}", id, adminEmail);

        try {
            appointmentService.cancelAppointment(id, "Supprimé par l'administrateur");
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Rendez-vous supprimé avec succès");
            
            auditLogger.warn("Appointment {} deleted by admin: {}", id, adminEmail);
            
            return "redirect:/admin/appointments";

        } catch (Exception e) {
            logger.error("Error deleting appointment {} by admin {}: {}", id, adminEmail, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la suppression : " + e.getMessage());
            return "redirect:/admin/appointments";
        }
    }

    // ======== ACTIONS DE CHANGEMENT DE STATUT ========

    /**
     * Confirme un rendez-vous
     */
    @PostMapping("/{id}/confirm")
    @ResponseBody
    public Map<String, Object> confirmAppointment(@PathVariable Long id, Authentication authentication) {
        String adminEmail = authentication.getName();
        
        try {
            Appointment appointment = appointmentService.confirmAppointment(id);
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
            @PathVariable Long id,
            @RequestParam(defaultValue = "Annulé par l'administrateur") String reason,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        
        try {
            Appointment appointment = appointmentService.cancelAppointment(id, reason);
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
    public Map<String, Object> startAppointment(@PathVariable Long id, Authentication authentication) {
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
            @PathVariable Long id,
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
    public Map<String, Object> markAsNoShow(@PathVariable Long id, Authentication authentication) {
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
                writer.printf("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\",\"%d\",\"%s\",\"%s\"%n",
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
