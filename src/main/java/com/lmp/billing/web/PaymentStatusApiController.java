package com.lmp.billing.web;

import com.lmp.billing.domain.Order;
import com.lmp.billing.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller pour le polling du statut de paiement.
 * Utilisé par la page de traitement intermédiaire pour vérifier
 * si le webhook Stripe a confirmé le paiement.
 */
@RestController
@RequestMapping("/api/payment-status")
public class PaymentStatusApiController {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusApiController.class);

    private final OrderRepository orderRepository;

    public PaymentStatusApiController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Endpoint de polling pour vérifier le statut de paiement d'une commande.
     * Retourne le statut actuel et un flag "ready" indiquant si le paiement est
     * confirmé.
     *
     * @param orderId L'ID de la commande
     * @return JSON avec orderId, status, paymentStatus, ready
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable java.util.UUID orderId) {
        log.debug("Polling payment status for order #{}", orderId);

        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            log.warn("Order #{} not found during payment status polling", orderId);
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("orderId", orderId);
            notFound.put("status", "NOT_FOUND");
            notFound.put("paymentStatus", "unknown");
            notFound.put("ready", false);
            return ResponseEntity.ok(notFound);
        }

        Order order = orderOpt.get();
        String status = order.getStatus().name();
        String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus() : "pending";

        // Le paiement est "ready" quand le statut n'est plus PAYMENT_PENDING
        boolean ready = !"PAYMENT_PENDING".equals(status) && !"PENDING".equals(status);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("status", status);
        response.put("paymentStatus", paymentStatus);
        response.put("ready", ready);

        if (ready) {
            log.info("Order #{} payment confirmed - status: {}", orderId, status);
        }

        return ResponseEntity.ok(response);
    }
}
