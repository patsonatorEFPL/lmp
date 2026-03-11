package com.lmp.notification.web;

import com.lmp.notification.dto.ContactForm;
import com.lmp.notification.service.ContactService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur pour la page Contact
 * 
 * Ce contrôleur gère l'affichage du formulaire de contact et le traitement
 * des soumissions avec validation et délégation au service métier.
 */
@Controller
public class ContactController {
    
    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    
        private final ContactService contactService;


    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Affiche la page de contact avec un formulaire vierge ou prérempli
     *
     * @param model L'objet Model pour passer des données à la vue
     * @param email Email prérempli (optionnel)
     * @param service Type de service (optionnel)
     * @return Le nom du template à utiliser
     */
    @GetMapping("/contact")
    public String contact(Model model,
                         @RequestParam(value = "email", required = false) String email,
                         @RequestParam(value = "service", required = false) String service) {
        
        // LOG DE DIAGNOSTIC : Vérifier que la page de contact est bien accédée
        logger.info("CONTACT_DEBUG - Page contact accédée via GET avec email: {} et service: {}", email, service);
        
        // Créer le formulaire avec ou sans préremplissage
        ContactForm contactForm = new ContactForm();
        
        // Préremplir l'email si fourni
        if (email != null && !email.trim().isEmpty()) {
            contactForm.setEmail(email.trim());
            logger.info("CONTACT_DEBUG - Email prérempli: {}", email);
        }
        
        // Préremplir le sujet selon le service si fourni
        if (service != null && !service.trim().isEmpty()) {
            String subjectPrefix = "Demande de consultation - " + service.trim();
            contactForm.setSubject(subjectPrefix);
            logger.info("CONTACT_DEBUG - Sujet prérempli: {}", subjectPrefix);
        }
        
        model.addAttribute("contactForm", contactForm);
        
        model.addAttribute("title", "Contactez-nous");
        model.addAttribute("subtitle", "Nous sommes là pour vous aider");
        
        // Informations de contact
        model.addAttribute("contactInfo", new Object[][]{
            {"Adresse", "123 Rue Principale, Montréal, QC H1A 1A1"},
            {"Téléphone", "+1 (514) 555-0123"},
            {"Email", "contact@lmp.ca"},
            {"Heures d'ouverture", "Lun-Ven: 9h-17h, Sam: 10h-15h"}
        });
        
        model.addAttribute("currentPage", "contact");
        
        return "contact";
    }
    
    /**
     * Traite la soumission du formulaire de contact avec validation
     * 
     * @param contactForm Le formulaire de contact avec validation
     * @param bindingResult Les résultats de validation
     * @param model L'objet Model pour passer des données à la vue
     * @param redirectAttributes Attributs pour la redirection
     * @return Le nom du template ou une redirection
     */
    @PostMapping("/contact")
    public String submitContact(
            @Valid @ModelAttribute ContactForm contactForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        // LOG DE DIAGNOSTIC : Confirmer que le POST arrive au serveur
        logger.info("CONTACT_DEBUG - POST /contact reçu ! Email: {}, Nom: {}, Sujet: '{}'",
                   contactForm.getEmail(), contactForm.getName(), contactForm.getSubject());
        
        logger.info("Réception d'un formulaire de contact de : {}", contactForm.getEmail());
        
        // Vérification des erreurs de validation
        if (bindingResult.hasErrors()) {
            logger.warn("Erreurs de validation dans le formulaire de contact : {}", bindingResult.getAllErrors());
            
            // Retour au formulaire avec les erreurs
            model.addAttribute("title", "Contactez-nous");
            model.addAttribute("subtitle", "Nous sommes là pour vous aider");
            model.addAttribute("contactInfo", new Object[][]{
                {"Adresse", "123 Rue Principale, Montréal, QC H1A 1A1"},
                {"Téléphone", "+1 (514) 555-0123"},
                {"Email", "contact@lmp.ca"},
                {"Heures d'ouverture", "Lun-Ven: 9h-17h, Sam: 10h-15h"}
            });
            model.addAttribute("currentPage", "contact");
            
            return "contact";
        }
        
        // Traitement du contact via le service
        boolean success = contactService.processContact(contactForm);
        
        if (success) {
            // Succès : redirection vers la page de confirmation
            redirectAttributes.addFlashAttribute("contactForm", contactForm);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Votre message a été envoyé avec succès ! Nous vous répondrons dans les plus brefs délais.");
            
            return "redirect:/contact/success";
        } else {
            // Erreur : retour au formulaire avec message d'erreur
            logger.error("Échec du traitement du contact pour : {}", contactForm.getEmail());
            
            model.addAttribute("title", "Contactez-nous");
            model.addAttribute("subtitle", "Nous sommes là pour vous aider");
            model.addAttribute("contactInfo", new Object[][]{
                {"Adresse", "123 Rue Principale, Montréal, QC H1A 1A1"},
                {"Téléphone", "+1 (514) 555-0123"},
                {"Email", "contact@lmp.ca"},
                {"Heures d'ouverture", "Lun-Ven: 9h-17h, Sam: 10h-15h"}
            });
            model.addAttribute("currentPage", "contact");
            model.addAttribute("errorMessage", 
                "Une erreur s'est produite lors de l'envoi de votre message. Veuillez réessayer.");
            
            return "contact";
        }
    }
    
    /**
     * Affiche la page de confirmation après envoi réussi
     * 
     * @param model L'objet Model pour passer des données à la vue
     * @return Le nom du template à utiliser
     */
    @GetMapping("/contact/success")
    public String contactSuccess(Model model) {
        model.addAttribute("title", "Message Envoyé");
        model.addAttribute("subtitle", "Merci de nous avoir contacté");
        model.addAttribute("currentPage", "contact");
        
        return "contact-success";
    }
} 