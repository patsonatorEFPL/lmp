package com.lmp.notification.web.api;

import com.lmp.notification.dto.ContactForm;
import com.lmp.notification.service.ContactService;
import com.lmp.shared.dto.ApiResponse;

import com.lmp.integration.event.LmpBusinessEvent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * API REST pour le formulaire de contact.
 * Endpoint public — pas d'authentification requise.
 */
@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Contact", description = "Formulaire de contact")
public class ContactRestController {

    private static final Logger logger = LoggerFactory.getLogger(ContactRestController.class);

    private final ContactService contactService;
    private final ApplicationEventPublisher eventPublisher;

    public ContactRestController(ContactService contactService, ApplicationEventPublisher eventPublisher) {
        this.contactService = contactService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    @Operation(summary = "Envoyer un message de contact", description = "Traite le formulaire de contact et envoie les emails")
    public ResponseEntity<ApiResponse<Void>> submitContact(@Valid @RequestBody ContactForm contactForm) {
        logger.info("API Contact received from: {}", contactForm.getEmail());

        boolean success = contactService.processContact(contactForm);

        if (success) {
            // Publier événement pour ERPNext (Lead CRM)
            eventPublisher.publishEvent(LmpBusinessEvent.of(
                    LmpBusinessEvent.EventType.CONTACT_FORM_SUBMITTED,
                    "notification",
                    null,
                    Map.of("name", contactForm.getName(), "email", contactForm.getEmail(), "subject", contactForm.getSubject())
            ));
            return ResponseEntity.ok(ApiResponse.ok("Message envoyé avec succès", null));
        } else {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de l'envoi du message"));
        }
    }
}
