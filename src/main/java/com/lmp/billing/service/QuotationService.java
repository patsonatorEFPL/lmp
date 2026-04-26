package com.lmp.billing.service;

import com.lmp.auth.domain.User;
import com.lmp.billing.domain.*;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.QuotationRepository;
import com.lmp.catalog.domain.Service;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.integration.event.LmpBusinessEvent;
import com.lmp.integration.event.LmpBusinessEvent.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestion du cycle de vie des devis LMP.
 * <p>
 * Cycle : DRAFT → SENT → ACCEPTED/REJECTED/EXPIRED.
 * <p>
 * Un devis ACCEPTED est converti en {@link Order} via {@link #convertToOrder(UUID)}.
 * La conversion publie un QUOTATION_ACCEPTED qui déclenche côté external ERP
 * l'appel à {@code make_sales_order} pour créer un SO lié au Quotation externe.
 */
@org.springframework.stereotype.Service
@Transactional
public class QuotationService {

    private static final Logger log = LoggerFactory.getLogger(QuotationService.class);

    private final QuotationRepository quotationRepository;
    private final OrderRepository orderRepository;
    private final ServiceRepository serviceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public QuotationService(QuotationRepository quotationRepository,
                            OrderRepository orderRepository,
                            ServiceRepository serviceRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.quotationRepository = quotationRepository;
        this.orderRepository = orderRepository;
        this.serviceRepository = serviceRepository;
        this.eventPublisher = eventPublisher;
    }

    // ==================== CRUD ====================

    /**
     * Crée un devis en brouillon.
     */
    public Quotation createDraft(User user, String title, List<QuotationItemRequest> itemRequests,
                                  BigDecimal appliedVatRate, Boolean vatReverseCharge,
                                  LocalDateTime validUntil, String notes, String billingName) {
        Quotation quotation = new Quotation();
        quotation.setUser(user);
        quotation.setTitle(title);
        quotation.setStatus(QuotationStatus.DRAFT);
        quotation.setAppliedVatRate(appliedVatRate);
        quotation.setVatReverseCharge(vatReverseCharge != null ? vatReverseCharge : false);
        quotation.setValidUntil(validUntil);
        quotation.setNotes(notes);
        quotation.setBillingName(billingName);

        BigDecimal total = BigDecimal.ZERO;
        for (QuotationItemRequest req : itemRequests) {
            Service service = serviceRepository.findById(req.serviceId())
                    .orElseThrow(() -> new IllegalArgumentException("Service not found: " + req.serviceId()));

            QuotationItem item = new QuotationItem();
            item.setService(service);
            item.setQuantity(req.quantity() != null ? req.quantity() : 1);
            if (req.price() == null) {
                throw new IllegalArgumentException("Price is required for quotation item (service: " + service.getTitle() + ")");
            }
            item.setPrice(req.price());
            item.setDescription(req.description());
            quotation.addItem(item);

            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Le total stocké est TTC si TVA applicable
        if (!Boolean.TRUE.equals(vatReverseCharge) && appliedVatRate != null
                && appliedVatRate.compareTo(BigDecimal.ZERO) > 0) {
            total = com.lmp.shared.pricing.MoneyUtils.multiply(total, BigDecimal.ONE.add(appliedVatRate));
        }
        quotation.setTotalAmount(com.lmp.shared.pricing.MoneyUtils.round(total));

        quotation = quotationRepository.save(quotation);
        log.info("📝 Quotation {} created (DRAFT) for user {} — total={}",
                quotation.getId(), user.getId(), quotation.getTotalAmount());

        return quotation;
    }

    /**
     * Envoie le devis au client → statut SENT, déclenche la sync externe.
     */
    public Quotation send(UUID quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found: " + quotationId));

        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new IllegalStateException("Cannot send quotation in status " + quotation.getStatus());
        }

        quotation.setStatus(QuotationStatus.SENT);
        quotation = quotationRepository.save(quotation);

        eventPublisher.publishEvent(LmpBusinessEvent.of(
                EventType.QUOTATION_SENT, "billing", quotation.getId(), Map.of()
        ));

        log.info("📤 Quotation {} sent to client", quotation.getId());
        return quotation;
    }

    /**
     * Accepte le devis et le convertit en commande.
     * <p>
     * Étapes :
     * 1. Statut → ACCEPTED, acceptedAt = now
     * 2. Crée un Order à partir des items du devis
     * 3. Publie QUOTATION_ACCEPTED → ErpEventListener appelle make_sales_order sur external ERP
     *
     * @return l'Order créée
     */
    public Order accept(UUID quotationId) {
        Quotation quotation = quotationRepository.findByIdWithUserAndItems(quotationId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found: " + quotationId));

        if (quotation.getStatus() != QuotationStatus.SENT) {
            throw new IllegalStateException("Cannot accept quotation in status " + quotation.getStatus());
        }

        if (quotation.isExpired()) {
            throw new IllegalStateException("Quotation is expired (validUntil=" + quotation.getValidUntil() + ")");
        }

        // 1. Marquer comme accepté
        quotation.setStatus(QuotationStatus.ACCEPTED);
        quotation.setAcceptedAt(LocalDateTime.now());

        // 2. Convertir en commande
        Order order = convertToOrder(quotation);
        quotation.setConvertedOrder(order);
        quotationRepository.save(quotation);

        // 3. Publier l'événement — déclenche make_sales_order côté external ERP
        eventPublisher.publishEvent(LmpBusinessEvent.of(
                EventType.QUOTATION_ACCEPTED, "billing", quotation.getId(),
                Map.of("orderId", order.getId().toString())
        ));

        log.info("✅ Quotation {} accepted → Order {} created", quotation.getId(), order.getId());
        return order;
    }

    /**
     * Rejette le devis.
     */
    public Quotation reject(UUID quotationId) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found: " + quotationId));

        if (quotation.getStatus() != QuotationStatus.SENT) {
            throw new IllegalStateException("Cannot reject quotation in status " + quotation.getStatus());
        }

        quotation.setStatus(QuotationStatus.REJECTED);
        quotation = quotationRepository.save(quotation);

        eventPublisher.publishEvent(LmpBusinessEvent.of(
                EventType.QUOTATION_REJECTED, "billing", quotation.getId(), Map.of()
        ));

        log.info("❌ Quotation {} rejected", quotation.getId());
        return quotation;
    }

    /**
     * Expire les devis SENT dont la date de validité est dépassée.
     * Appelé par un scheduler périodique.
     */
    public int expireOverdueQuotations() {
        List<Quotation> expired = quotationRepository.findExpiredQuotations(LocalDateTime.now());
        for (Quotation q : expired) {
            q.setStatus(QuotationStatus.EXPIRED);
            quotationRepository.save(q);
            log.info("⏰ Quotation {} expired (validUntil={})", q.getId(), q.getValidUntil());
        }
        return expired.size();
    }

    // ==================== Read ====================

    @Transactional(readOnly = true)
    public Optional<Quotation> findById(UUID id) {
        return quotationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Quotation> findByIdWithDetails(UUID id) {
        return quotationRepository.findByIdWithUserAndItems(id);
    }

    @Transactional(readOnly = true)
    public List<Quotation> findByUser(User user) {
        return quotationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // ==================== Private ====================

    /**
     * Convertit un devis accepté en commande (Order).
     * L'Order est créée en statut PAYMENT_PENDING — le paiement Stripe suit.
     */
    private Order convertToOrder(Quotation quotation) {
        Order order = new Order();
        order.setUser(quotation.getUser());
        order.setServiceName(quotation.getTitle());
        order.setTotalAmount(quotation.getTotalAmount());
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setCurrency(quotation.getCurrency());
        order.setBillingName(quotation.getBillingName());
        order.setNotes("Converti depuis devis " + quotation.getId());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Copier les champs TVA
        order.setAppliedVatRate(quotation.getAppliedVatRate());
        order.setVatReverseCharge(quotation.getVatReverseCharge());

        // Copier les items avant le save (cascade ALL)
        for (QuotationItem qItem : quotation.getItems()) {
            OrderItem oItem = new OrderItem();
            oItem.setOrder(order);
            oItem.setService(qItem.getService());
            oItem.setQuantity(qItem.getQuantity());
            oItem.setPrice(qItem.getPrice());
            if (order.getItems() == null) {
                order.setItems(new java.util.HashSet<>());
            }
            order.getItems().add(oItem);
        }

        order = orderRepository.save(order);
        return order;
    }

    // ==================== DTOs ====================

    /**
     * Requête de création d'un item de devis.
     */
    public record QuotationItemRequest(
            UUID serviceId,
            Integer quantity,
            BigDecimal price,
            String description
    ) {}
}
