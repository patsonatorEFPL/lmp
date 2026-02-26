package com.lmp.web.controller.user;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.User;
import com.lmp.repository.OrderRepository;
import com.lmp.repository.UserRepository;
import com.lmp.service.invoice.InvoiceCacheService;
import com.lmp.service.invoice.InvoicePdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

/**
 * Contrôleur pour le téléchargement des factures PDF.
 * Les factures sont générées dynamiquement et mises en cache temporairement.
 */
@Controller
@RequestMapping("/dashboard/orders")
@PreAuthorize("isAuthenticated()")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoicePdfService invoicePdfService;
    private final InvoiceCacheService invoiceCacheService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public InvoiceController(InvoicePdfService invoicePdfService,
            InvoiceCacheService invoiceCacheService,
            OrderRepository orderRepository,
            UserRepository userRepository) {
        this.invoicePdfService = invoicePdfService;
        this.invoiceCacheService = invoiceCacheService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    /**
     * Télécharge la facture PDF pour une commande.
     * La facture est mise en cache pendant 5 minutes pour éviter de surcharger le
     * serveur.
     * 
     * @param orderId        L'ID de la commande
     * @param authentication L'authentification de l'utilisateur connecté
     * @return Le PDF de la facture ou une erreur
     */
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long orderId, Authentication authentication) {
        log.info("Demande de téléchargement de facture pour la commande #{}", orderId);

        // Récupérer l'utilisateur connecté
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.warn("Utilisateur non trouvé: {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userOpt.get();

        // Récupérer la commande
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            log.warn("Commande #{} non trouvée", orderId);
            return ResponseEntity.notFound().build();
        }

        Order order = orderOpt.get();

        // Vérifier que la commande appartient à l'utilisateur ou qu'il est admin
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && (order.getUser() == null || !order.getUser().getId().equals(user.getId()))) {
            log.warn("Utilisateur {} n'est pas autorisé à accéder à la facture de la commande #{}",
                    email, orderId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Vérifier que la commande est éligible pour une facture
        if (!isInvoiceEligible(order)) {
            log.warn("La commande #{} n'est pas éligible pour une facture (statut: {})",
                    orderId, order.getStatus());
            return ResponseEntity.badRequest().build();
        }

        try {
            // Vérifier le cache
            byte[] pdfData = invoiceCacheService.get(orderId);
            String invoiceNumber;

            if (pdfData != null) {
                // Cache hit
                log.info("Facture pour la commande #{} trouvée dans le cache", orderId);
                invoiceNumber = invoiceCacheService.getInvoiceNumber(orderId);
            } else {
                // Cache miss - générer le PDF
                log.info("Génération de la facture pour la commande #{}", orderId);
                pdfData = invoicePdfService.generateInvoicePdf(order, user);
                invoiceNumber = invoicePdfService.generateInvoiceNumber(order);

                // Mettre en cache
                invoiceCacheService.put(orderId, pdfData, invoiceNumber);
            }

            // Préparer la réponse
            String filename = "Facture_" + invoiceNumber + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfData.length);

            log.info("Facture {} téléchargée avec succès ({} bytes)", filename, pdfData.length);

            return new ResponseEntity<>(pdfData, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Erreur lors de la génération de la facture pour la commande #{}: {}",
                    orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Vérifie si une commande est éligible pour générer une facture.
     * Seules les commandes payées/confirmées/complétées/livrées sont éligibles.
     */
    private boolean isInvoiceEligible(Order order) {
        String status = order.getStatus().name();
        return "CONFIRMED".equals(status) ||
                "COMPLETED".equals(status) ||
                "DELIVERED".equals(status) ||
                "PROCESSING".equals(status) ||
                "IN_PROGRESS".equals(status);
    }
}
