package com.lmp.integration.sync.web;

import com.lmp.auth.repository.UserRepository;
import com.lmp.billing.domain.*;
import com.lmp.billing.repository.OrderInstallmentRepository;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.QuotationRepository;
import com.lmp.billing.service.QuotationService;
import com.lmp.billing.service.admin.OrderAdminService;
import com.lmp.billing.util.InstallmentCalculator;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.integration.event.LmpBusinessEvent;
import com.lmp.shared.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Temporary dev-only controller to trigger sync-stripe from the API filter chain.
 * Available only in the 'dev' profile. Remove after Phase 4 validation.
 */
@RestController
@RequestMapping("/api/v1/dev/sync")
@Profile("dev")
public class DevSyncController {

    private static final Logger log = LoggerFactory.getLogger(DevSyncController.class);

    private final OrderAdminService orderAdminService;
    private final OrderRepository orderRepository;
    private final OrderInstallmentRepository installmentRepository;
    private final QuotationRepository quotationRepository;
    private final QuotationService quotationService;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DevSyncController(OrderAdminService orderAdminService,
                              OrderRepository orderRepository,
                              OrderInstallmentRepository installmentRepository,
                              QuotationRepository quotationRepository,
                              QuotationService quotationService,
                              ServiceRepository serviceRepository,
                              UserRepository userRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.orderAdminService = orderAdminService;
        this.orderRepository = orderRepository;
        this.installmentRepository = installmentRepository;
        this.quotationRepository = quotationRepository;
        this.quotationService = quotationService;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/orders/{orderId}/sync-stripe")
    public ResponseEntity<?> triggerSyncStripe(@PathVariable UUID orderId) {
        log.warn("🔧 [DEV] Manual sync-stripe trigger for order {}", orderId);
        try {
            var result = orderAdminService.syncWithStripe(orderId);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            log.error("🔧 [DEV] Sync-stripe failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DEV-only: Force-publish an ORDER_CONFIRMED event to trigger the full sync pipeline
     * (Sales Order + Sales Invoice + Payment Entry).
     */
    @PostMapping("/orders/{orderId}/trigger-confirmed")
    public ResponseEntity<?> triggerOrderConfirmed(@PathVariable UUID orderId) {
        log.warn("🔧 [DEV] Manual ORDER_CONFIRMED trigger for order {}", orderId);
        try {
            eventPublisher.publishEvent(LmpBusinessEvent.of(
                    LmpBusinessEvent.EventType.ORDER_CONFIRMED, "billing", orderId, Map.of()
            ));
            return ResponseEntity.ok(ApiResponse.ok("ORDER_CONFIRMED event published for " + orderId));
        } catch (Exception e) {
            log.error("🔧 [DEV] Trigger failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DEV-only: Force-publish an ORDER_CREATED event to trigger SO + SINV creation (unpaid).
     */
    @PostMapping("/orders/{orderId}/trigger-created")
    public ResponseEntity<?> triggerOrderCreated(@PathVariable UUID orderId) {
        log.warn("🔧 [DEV] Manual ORDER_CREATED trigger for order {}", orderId);
        try {
            eventPublisher.publishEvent(LmpBusinessEvent.of(
                    LmpBusinessEvent.EventType.ORDER_CREATED, "billing", orderId, Map.of()
            ));
            return ResponseEntity.ok(ApiResponse.ok("ORDER_CREATED event published for " + orderId));
        } catch (Exception e) {
            log.error("🔧 [DEV] Trigger failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DEV-only: Force-publish a PAYMENT_RECEIVED event to trigger Payment Entry creation.
     */
    @PostMapping("/orders/{orderId}/trigger-payment")
    public ResponseEntity<?> triggerPaymentReceived(@PathVariable UUID orderId) {
        log.warn("🔧 [DEV] Manual PAYMENT_RECEIVED trigger for order {}", orderId);
        try {
            eventPublisher.publishEvent(LmpBusinessEvent.of(
                    LmpBusinessEvent.EventType.PAYMENT_RECEIVED, "billing", orderId, Map.of()
            ));
            return ResponseEntity.ok(ApiResponse.ok("PAYMENT_RECEIVED event published for " + orderId));
        } catch (Exception e) {
            log.error("🔧 [DEV] Trigger failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DEV-only: Crée une commande de test avec paiement en plusieurs fois
     * et déclenche la synchronisation complète vers l'ERP.
     * <p>
     * Params: installments (2,3,4), amount (total TTC), paidInstallments (combien sont déjà payées)
     */
    @PostMapping("/orders/create-installment-test")
    public ResponseEntity<?> createInstallmentTestOrder(
            @RequestParam(defaultValue = "3") int installments,
            @RequestParam(defaultValue = "1200.00") BigDecimal amount,
            @RequestParam(defaultValue = "1") int paidInstallments) {
        log.warn("🔧 [DEV] Creating installment test order: {}x, amount={}, paid={}",
                installments, amount, paidInstallments);
        try {
            // Résoudre le template external ERP
            String templateName = "Paiement en " + installments + "x";

            // Associer un utilisateur existant qui a un externalCustomerId (pour le lien ERP)
            var adminUser = userRepository.findByEmail("admin@lmp.ca").orElse(null);

            // Créer la commande
            Order order = new Order();
            order.setUser(adminUser);
            order.setServiceName("Consultation Premium");
            order.setTotalAmount(amount);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setCurrency("EUR");
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setPaidAt(paidInstallments > 0 ? LocalDateTime.now() : null);
            order.setPaymentMethod("card");
            order.setPaymentStatus(paidInstallments >= installments ? "paid" : "partially_paid");
            order.setInstallmentCount(installments);
            order.setPaymentTermsTemplate(templateName);
            order.setAppliedVatRate(new BigDecimal("0.2100"));
            order.setBillingName(adminUser != null ? "Proximus" : "Client Test");

            order = orderRepository.save(order);

            // Créer les échéances — calcul identique à external ERP (percentage × grand_total / 100)
            List<InstallmentCalculator.Installment> calculated = InstallmentCalculator.calculate(amount, installments);

            for (InstallmentCalculator.Installment calc : calculated) {
                OrderInstallment inst = new OrderInstallment();
                inst.setOrder(order);
                inst.setInstallmentNumber(calc.number());
                inst.setPaymentTerm(calc.number() + (calc.number() == 1 ? "ère" : "ème") + " échéance");
                inst.setInvoicePortion(calc.invoicePortion());
                inst.setAmount(calc.paymentAmount());

                inst.setDueDate(LocalDate.now().plusDays((long) (calc.number() - 1) * 30));
                inst.setStatus(calc.number() <= paidInstallments ? InstallmentStatus.PAID : InstallmentStatus.PENDING);
                inst.setPaidAt(calc.number() <= paidInstallments ? LocalDateTime.now() : null);
                inst.setStripePaymentIntentId(calc.number() <= paidInstallments
                        ? "pi_test_inst_" + calc.number() + "_" + UUID.randomUUID().toString().substring(0, 8) : null);

                installmentRepository.save(inst);
            }

            // Publier l'événement PAYMENT_RECEIVED pour déclencher la chaîne SO→SINV→PE
            eventPublisher.publishEvent(LmpBusinessEvent.of(
                    LmpBusinessEvent.EventType.PAYMENT_RECEIVED, "billing", order.getId(), Map.of()
            ));

            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "orderId", order.getId(),
                    "installments", installments,
                    "amount", amount,
                    "paidInstallments", paidInstallments,
                    "template", templateName,
                    "message", "Order created and PAYMENT_RECEIVED event published"
            )));
        } catch (Exception e) {
            log.error("🔧 [DEV] Create installment order failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DEV-only: Simule le paiement d'une échéance spécifique et déclenche la synchro PE.
     */
    @PostMapping("/orders/{orderId}/pay-installment/{installmentNumber}")
    public ResponseEntity<?> payInstallment(@PathVariable UUID orderId,
                                            @PathVariable int installmentNumber) {
        log.warn("🔧 [DEV] Manual installment payment: order={}, installment={}", orderId, installmentNumber);
        try {
            var instOpt = installmentRepository.findByOrderIdAndInstallmentNumber(orderId, installmentNumber);
            if (instOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            OrderInstallment inst = instOpt.get();
            inst.setStatus(InstallmentStatus.PAID);
            inst.setPaidAt(LocalDateTime.now());
            inst.setStripePaymentIntentId("pi_test_manual_" + UUID.randomUUID().toString().substring(0, 8));
            installmentRepository.save(inst);

            // Déclencher la synchro
            eventPublisher.publishEvent(LmpBusinessEvent.of(
                    LmpBusinessEvent.EventType.PAYMENT_RECEIVED, "billing", orderId, Map.of()
            ));

            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "orderId", orderId,
                    "installmentNumber", installmentNumber,
                    "status", "PAID",
                    "message", "Installment marked as paid and PAYMENT_RECEIVED published"
            )));
        } catch (Exception e) {
            log.error("🔧 [DEV] Pay installment failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Quotation E2E Test ====================

    /**
     * DEV-only: Crée un devis de test, l'envoie, puis l'accepte pour tester le flux complet :
     * DRAFT → SENT (sync Quotation) → ACCEPTED (make_sales_order → Order).
     */
    @PostMapping("/quotations/create-test")
    public ResponseEntity<?> createQuotationTest(
            @RequestParam(defaultValue = "500.00") BigDecimal amount) {
        log.warn("🔧 [DEV] Creating quotation test: amount={}", amount);
        try {
            // Trouver un user et un service existants
            var adminUser = userRepository.findByEmail("admin@lmp.ca").orElse(null);
            if (adminUser == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("User admin@lmp.ca not found"));
            }

            // Trouver le premier service actif
            var services = serviceRepository.findAll();
            if (services.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("No services found in database"));
            }
            var service = services.get(0);

            // Créer le devis
            var itemRequest = new QuotationService.QuotationItemRequest(
                    service.getId(), 1, amount, "Test quotation item - " + service.getTitle()
            );

            Quotation quotation = quotationService.createDraft(
                    adminUser,
                    "Devis Test E2E - " + service.getTitle(),
                    List.of(itemRequest),
                    new BigDecimal("0.2100"),
                    false,
                    LocalDateTime.now().plusDays(30),
                    "Devis de test Phase 2",
                    adminUser.getFirstName() + " " + adminUser.getLastName()
            );

            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "quotationId", quotation.getId(),
                    "status", quotation.getStatus(),
                    "totalAmount", quotation.getTotalAmount(),
                    "message", "Quotation DRAFT created. Use /send then /accept to test full flow."
            )));
        } catch (Exception e) {
            log.error("🔧 [DEV] Create quotation test failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DEV-only: Envoie un devis (DRAFT → SENT) — déclenche la sync externe.
     */
    @PostMapping("/quotations/{quotationId}/send")
    public ResponseEntity<?> sendQuotation(@PathVariable UUID quotationId) {
        log.warn("🔧 [DEV] Sending quotation {}", quotationId);
        try {
            Quotation quotation = quotationService.send(quotationId);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "quotationId", quotation.getId(),
                    "status", quotation.getStatus(),
                    "message", "Quotation SENT — sync event published"
            )));
        } catch (Exception e) {
            log.error("🔧 [DEV] Send quotation failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DEV-only: Accepte un devis (SENT → ACCEPTED) — convertit en Order + make_sales_order.
     */
    @PostMapping("/quotations/{quotationId}/accept")
    public ResponseEntity<?> acceptQuotation(@PathVariable UUID quotationId) {
        log.warn("🔧 [DEV] Accepting quotation {}", quotationId);
        try {
            Order order = quotationService.accept(quotationId);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "quotationId", quotationId,
                    "orderId", order.getId(),
                    "orderStatus", order.getStatus(),
                    "message", "Quotation ACCEPTED → Order created. make_sales_order event published."
            )));
        } catch (Exception e) {
            log.error("🔧 [DEV] Accept quotation failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DEV-only: Rejette un devis (SENT → REJECTED).
     */
    @PostMapping("/quotations/{quotationId}/reject")
    public ResponseEntity<?> rejectQuotation(@PathVariable UUID quotationId) {
        log.warn("🔧 [DEV] Rejecting quotation {}", quotationId);
        try {
            Quotation quotation = quotationService.reject(quotationId);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "quotationId", quotation.getId(),
                    "status", quotation.getStatus(),
                    "message", "Quotation REJECTED — declare_order_lost event published"
            )));
        } catch (Exception e) {
            log.error("🔧 [DEV] Reject quotation failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DEV-only: Statut sync d'un devis.
     */
    @GetMapping("/quotations/{quotationId}/status")
    public ResponseEntity<?> getQuotationSyncStatus(@PathVariable UUID quotationId) {
        return quotationRepository.findById(quotationId)
                .map(q -> ResponseEntity.ok(Map.of(
                        "id", q.getId(),
                        "status", q.getStatus(),
                        "externalQuotationId", q.getExternalQuotationId() != null ? q.getExternalQuotationId() : "null",
                        "convertedOrderId", q.getConvertedOrder() != null ? q.getConvertedOrder().getId().toString() : "null",
                        "totalAmount", q.getTotalAmount()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/orders/{orderId}/status")
    public ResponseEntity<?> getOrderSyncStatus(@PathVariable UUID orderId) {
        return orderRepository.findById(orderId)
                .map(order -> ResponseEntity.ok(Map.of(
                        "id", order.getId(),
                        "status", order.getStatus(),
                        "externalOrderId", order.getExternalOrderId() != null ? order.getExternalOrderId() : "null",
                        "externalInvoiceId", order.getExternalInvoiceId() != null ? order.getExternalInvoiceId() : "null",
                        "externalPaymentId", order.getExternalPaymentId() != null ? order.getExternalPaymentId() : "null"
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
