package com.lmp.integration.sync.web;

import com.lmp.billing.domain.Order;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.service.admin.OrderAdminService;
import com.lmp.integration.event.LmpBusinessEvent;
import com.lmp.shared.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

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
    private final ApplicationEventPublisher eventPublisher;

    public DevSyncController(OrderAdminService orderAdminService,
                             OrderRepository orderRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.orderAdminService = orderAdminService;
        this.orderRepository = orderRepository;
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
