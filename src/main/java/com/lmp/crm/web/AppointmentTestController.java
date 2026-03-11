package com.lmp.crm.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.lmp.crm.service.AppointmentService;
import com.lmp.auth.service.UserService;
import com.lmp.crm.dto.AppointmentForm;
import com.lmp.auth.domain.User;
import com.lmp.crm.domain.Appointment;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.UserRepository;

/**
 * Contrôleur TEMPORAIRE pour tester les rendez-vous sans authentification
 * À SUPPRIMER EN PRODUCTION !
 */
@Controller
@RequestMapping("/appointments-test")
public class AppointmentTestController {
    
        private final AppointmentService appointmentService;
    
        private final UserService userService;
    
        private final UserRepository userRepository;
    

    public AppointmentTestController(AppointmentService appointmentService,
                           UserService userService,
                           UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * Page de test pour le modal de rendez-vous
     */
    @GetMapping("/page")
    public String testPage(Model model) {
        model.addAttribute("title", "Test Modal Rendez-vous");
        return "test-appointment";
    }
    
    /**
     * Page de test enrichie avec statistiques et affichage BD
     */
    @GetMapping("/enhanced")
    public String enhancedTestPage(Model model) {
        model.addAttribute("title", "Test Enrichi - Modal Rendez-vous");
        System.out.println("🧪 Accès à la page de test enrichie des rendez-vous");
        return "test-appointment-enhanced";
    }
    
    /**
     * Créer un rendez-vous de test (sans authentification)
     * PERSISTANCE RÉELLE EN BD
     */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<?> createTestAppointment(@RequestBody Map<String, Object> formData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Récupérer les données du formulaire
            String name = (String) formData.get("name");
            String email = (String) formData.get("email");
            String phone = (String) formData.get("phone");
            String service = (String) formData.get("service");
            String dateStr = (String) formData.get("date");
            String timeStr = (String) formData.get("time");
            String message = (String) formData.get("message");
            
            // Créer le LocalDateTime à partir de la date et de l'heure
            LocalDate date = LocalDate.parse(dateStr);
            String[] timeParts = timeStr.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            LocalDateTime appointmentDateTime = date.atTime(hour, minute);
            
            // Créer l'objet AppointmentForm avec les champs requis
            AppointmentForm form = new AppointmentForm();
            form.setSubject(service + " - " + name); // Le sujet est une combinaison du service et du nom
            form.setDescription("Contact: " + email + " / " + phone + 
                              (message != null && !message.isEmpty() ? "\nMessage: " + message : ""));
            form.setAppointmentDate(appointmentDateTime);
            form.setDurationMinutes(60); // Durée fixe d'1 heure pour les créneaux
            
            // Récupérer ou créer un utilisateur de test
            User testUser = userRepository.findByEmail(email).orElse(null);
            if (testUser == null) {
                // Créer un nouvel utilisateur pour le test
                testUser = new User();
                testUser.setEmail(email);
                
                // Séparer le nom en prénom et nom si possible
                String[] nameParts = name.split(" ", 2);
                if (nameParts.length > 0) {
                    testUser.setFirstName(nameParts[0]);
                }
                if (nameParts.length > 1) {
                    testUser.setLastName(nameParts[1]);
                }
                
                testUser.setPassword("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"); // Mot de passe encodé BCrypt pour "password123"
                testUser.setPhone(phone);
                testUser.setStatus(com.lmp.auth.domain.UserStatus.ACTIVE);
                testUser.setAccountLocked(false);
                testUser.setEmailVerified(true); // Pour les tests, on considère l'email vérifié
                testUser.setRegistrationDate(LocalDateTime.now());
                
                testUser = userRepository.save(testUser);
                System.out.println("👤 Utilisateur de test créé : " + testUser.getEmail());
            }
            
            // Appeler le service pour créer réellement le rendez-vous
            System.out.println("📅 Tentative de création du rendez-vous en BD...");
            System.out.println("  - Date/Heure: " + appointmentDateTime);
            System.out.println("  - Sujet: " + form.getSubject());
            System.out.println("  - Utilisateur: " + testUser.getEmail());
            
            Appointment appointment = appointmentService.createAppointment(form, testUser);
            
            response.put("success", true);
            response.put("message", "✅ Rendez-vous créé avec succès en base de données !");
            response.put("appointmentId", appointment.getId());
            response.put("data", formData);
            
            System.out.println("✅ Rendez-vous créé avec succès - ID: " + appointment.getId());
            
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Erreur de validation : " + e.getMessage());
            response.put("success", false);
            response.put("message", "❌ " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création : " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "❌ Erreur lors de la création du rendez-vous : " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtenir les créneaux disponibles pour une date (CONSULTATION BD RÉELLE)
     */
    @GetMapping("/available-slots")
    @ResponseBody
    public ResponseEntity<List<String>> getTestAvailableSlots(@RequestParam("date") String dateStr) {
        List<String> slots = new ArrayList<>();
        
        try {
            // Parser la date
            LocalDate date = LocalDate.parse(dateStr);
            int dayOfWeek = date.getDayOfWeek().getValue();
            
            // Vérifier si c'est un jour de semaine (lundi=1 à vendredi=5)
            if (dayOfWeek >= 1 && dayOfWeek <= 5) {
                // CONSULTATION DE LA BASE DE DONNÉES
                // Récupérer les créneaux disponibles depuis le service (qui consulte la BD)
                List<LocalDateTime> availableTimeSlots = appointmentService.getAvailableTimeSlots(date);
                
                // Convertir en format heure simple
                for (LocalDateTime slot : availableTimeSlots) {
                    String timeStr = String.format("%02d:%02d", slot.getHour(), slot.getMinute());
                    slots.add(timeStr);
                }
                
                System.out.println("📅 BD - Créneaux disponibles pour " + dateStr + " (depuis BD) : " + slots.size() + " créneaux");
                
                // Si aucun créneau n'est disponible dans la BD, on peut retourner des créneaux par défaut
                if (slots.isEmpty()) {
                    System.out.println("⚠️ Aucun créneau libre dans la BD, utilisation des créneaux par défaut");
                    // Créneaux par défaut si tout est libre
                    slots.add("09:00");
                    slots.add("09:30");
                    slots.add("10:00");
                    slots.add("10:30");
                    slots.add("11:00");
                    slots.add("11:30");
                    slots.add("14:00");
                    slots.add("14:30");
                    slots.add("15:00");
                    slots.add("15:30");
                    slots.add("16:00");
                    slots.add("16:30");
                }
            } else {
                System.out.println("🚫 Week-end détecté pour " + dateStr + " - Aucun créneau disponible");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la consultation de la BD : " + e.getMessage());
            e.printStackTrace();
            // En cas d'erreur, retourner des créneaux par défaut
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
        
        System.out.println("📅 Résultat final : " + slots);
        
        return ResponseEntity.ok(slots);
    }
    
    /**
     * Récupérer tous les rendez-vous existants dans la BD (pour debug)
     */
    @GetMapping("/list-appointments")
    @ResponseBody
    public ResponseEntity<?> listAllAppointments() {
        try {
            List<Map<String, Object>> appointmentsList = new ArrayList<>();
            
            // Récupérer tous les rendez-vous actifs
            List<Appointment> appointments = appointmentService.findActiveAppointments();
            
            for (Appointment apt : appointments) {
                Map<String, Object> aptMap = new HashMap<>();
                aptMap.put("id", apt.getId());
                aptMap.put("date", apt.getAppointmentDate().toString());
                aptMap.put("subject", apt.getSubject());
                aptMap.put("status", apt.getStatus().toString());
                aptMap.put("duration", apt.getDurationMinutes());
                appointmentsList.add(aptMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("total", appointmentsList.size());
            response.put("appointments", appointmentsList);
            response.put("message", "📅 " + appointmentsList.size() + " rendez-vous actifs dans la BD");
            
            System.out.println("📅 BD - Nombre de rendez-vous actifs : " + appointmentsList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des rendez-vous : " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Erreur lors de la récupération des rendez-vous");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
