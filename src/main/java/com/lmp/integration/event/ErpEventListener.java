package com.lmp.integration.event;

import com.lmp.auth.domain.User;
import com.lmp.auth.repository.UserRepository;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderItem;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.catalog.domain.Service;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncProperties;
import com.lmp.integration.sync.mapper.CustomerSyncMapper;
import com.lmp.integration.sync.mapper.ItemSyncMapper;
import com.lmp.integration.sync.mapper.OrderSyncMapper;
import com.lmp.integration.sync.mapper.PaymentSyncMapper;
import com.lmp.integration.sync.service.SyncOutboundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Listener central pour les événements métier LMP → système externe (intégration sortante).
 *
 * Architecture :
 *   Module (auth, billing, crm…) → ApplicationEventPublisher → LmpBusinessEvent → ErpEventListener
 *
 * Ce listener est le point d'entrée unique pour pousser les événements vers le système externe.
 * Les modules métier ne dépendent pas d'un fournisseur spécifique.
 * <p>
 * Utilise {@code @TransactionalEventListener(AFTER_COMMIT)} pour garantir que la transaction
 * émettrice (checkout, webhook Stripe) est commitée avant de démarrer le traitement asynchrone.
 * Cela élimine les race conditions où le handler ne voit pas l'entité ou ses champs mis à jour.
 * {@code fallbackExecution = true} assure le fonctionnement hors contexte transactionnel (dev, tests).
 */
@Component
public class ErpEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ErpEventListener.class);

    private final SyncOutboundService syncOutboundService;
    private final SyncProperties syncProperties;
    private final ExternalSystemClient externalClient;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ServiceRepository serviceRepository;
    private final CustomerSyncMapper customerSyncMapper;
    private final OrderSyncMapper orderSyncMapper;
    private final ItemSyncMapper itemSyncMapper;
    private final PaymentSyncMapper paymentSyncMapper;

    public ErpEventListener(SyncOutboundService syncOutboundService,
                            SyncProperties syncProperties,
                            ExternalSystemClient externalClient,
                            UserRepository userRepository,
                            OrderRepository orderRepository,
                            ServiceRepository serviceRepository,
                            CustomerSyncMapper customerSyncMapper,
                            OrderSyncMapper orderSyncMapper,
                            ItemSyncMapper itemSyncMapper,
                            PaymentSyncMapper paymentSyncMapper) {
        this.syncOutboundService = syncOutboundService;
        this.syncProperties = syncProperties;
        this.externalClient = externalClient;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.serviceRepository = serviceRepository;
        this.customerSyncMapper = customerSyncMapper;
        this.orderSyncMapper = orderSyncMapper;
        this.itemSyncMapper = itemSyncMapper;
        this.paymentSyncMapper = paymentSyncMapper;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleBusinessEvent(LmpBusinessEvent event) {
        logger.info("📡 [EVENT BUS] {} — module={}, entityId={}, eventId={}",
                event.type(), event.sourceModule(), event.entityId(), event.eventId());

        switch (event.type()) {
            // --- Utilisateurs → CUSTOMER + CONTACT ---
            case USER_REGISTERED -> {
                logEvent("Utilisateur inscrit — provisioning externe", event);
                handleUserProvisioning(event);
            }
            case USER_VERIFIED -> {
                logEvent("Utilisateur vérifié — mise à jour externe", event);
                handleUserUpdate(event);
            }
            case USER_UPDATED -> {
                logEvent("Utilisateur mis à jour — synchronisation externe", event);
                handleUserUpdate(event);
            }
            case USER_DELETED -> {
                logEvent("Utilisateur supprimé — suppression cascade externe", event);
                handleUserDelete(event);
            }

            // --- Commandes → SALES_ORDER ---
            case ORDER_CREATED -> {
                logEvent("Commande créée — synchronisation SO + SINV (impayée)", event);
                handleOrderCreated(event);
            }
            case ORDER_CONFIRMED -> {
                logEvent("Commande confirmée — synchronisation Payment Entry", event);
                handlePaymentReceived(event);
            }
            case ORDER_UPDATED -> {
                logEvent("Commande mise à jour — synchronisation externe", event);
                handleOrderUpdate(event);
            }
            case ORDER_CANCELLED -> {
                logEvent("Commande annulée — synchronisation externe", event);
                handleOrderUpdate(event);
            }
            case ORDER_DELETED -> {
                logEvent("Commande supprimée — suppression externe", event);
                handleOrderDelete(event);
            }

            // --- Paiements → SALES_ORDER + SALES_INVOICE ---
            // PAYMENT_RECEIVED est publié par OrderRealtimeEventPublisher quand le statut
            // passe à CONFIRMED (via webhook Stripe ou réconciliation). C'est le vrai
            // déclencheur de la synchronisation commande + facture vers l'ERP.
            case PAYMENT_RECEIVED -> {
                logEvent("Paiement reçu — synchronisation Payment Entry", event);
                handlePaymentReceived(event);
            }
            case PAYMENT_FAILED -> logEvent("Paiement échoué", event);

            // --- Factures → SALES_INVOICE ---
            case INVOICE_GENERATED -> {
                logEvent("Facture générée — synchronisation externe", event);
                handleInvoiceGenerated(event);
            }

            // --- Remboursements ---
            case REFUND_PROCESSED, REFUND_STATUS_UPDATED ->
                    logEvent("Remboursement — synchronisation externe", event);

            // --- Rendez-vous ---
            case APPOINTMENT_CREATED, APPOINTMENT_CONFIRMED, APPOINTMENT_CANCELLED,
                    APPOINTMENT_UPDATED, APPOINTMENT_DELETED ->
                    logEvent("Rendez-vous — synchronisation externe", event);

            // --- Avis ---
            case REVIEW_CREATED, REVIEW_UPDATED ->
                    logEvent("Avis — synchronisation externe", event);

            // --- Lead / Contact ---
            case CONTACT_FORM_SUBMITTED ->
                    logEvent("Lead — synchronisation externe (CRM)", event);

            // --- Catalogue → ITEM ---
            case SERVICE_CREATED -> {
                logEvent("Service créé — synchronisation externe", event);
                dispatchSync(SyncEntityType.ITEM, "CREATED", event);
            }
            case SERVICE_UPDATED -> {
                logEvent("Service mis à jour — synchronisation externe", event);
                dispatchSync(SyncEntityType.ITEM, "UPDATED", event);
            }
            case SERVICE_DELETED -> {
                logEvent("Service supprimé — suppression externe", event);
                dispatchSync(SyncEntityType.ITEM, "DELETED", event);
            }

            // --- Projets → PROJECT / TASK ---
            case PROJECT_CREATED -> {
                logEvent("Projet créé — synchronisation externe", event);
                dispatchSync(SyncEntityType.PROJECT, "CREATED", event);
            }
            case PROJECT_UPDATED -> {
                logEvent("Projet mis à jour — synchronisation externe", event);
                dispatchSync(SyncEntityType.PROJECT, "UPDATED", event);
            }
            case PROJECT_DELETED -> {
                logEvent("Projet supprimé — suppression externe", event);
                dispatchSync(SyncEntityType.PROJECT, "DELETED", event);
            }
            case TASK_CREATED -> {
                logEvent("Tâche créée — synchronisation externe", event);
                dispatchSync(SyncEntityType.TASK, "CREATED", event);
            }
            case TASK_UPDATED -> {
                logEvent("Tâche mise à jour — synchronisation externe", event);
                dispatchSync(SyncEntityType.TASK, "UPDATED", event);
            }
            case TASK_DELETED -> {
                logEvent("Tâche supprimée — suppression externe", event);
                dispatchSync(SyncEntityType.TASK, "DELETED", event);
            }

            // --- Support → ISSUE ---
            case TICKET_CREATED -> {
                logEvent("Ticket créé — synchronisation externe", event);
                dispatchSync(SyncEntityType.ISSUE, "CREATED", event);
            }
            case TICKET_UPDATED -> {
                logEvent("Ticket mis à jour — synchronisation externe", event);
                dispatchSync(SyncEntityType.ISSUE, "UPDATED", event);
            }
            case TICKET_RESOLVED -> {
                logEvent("Ticket résolu — synchronisation externe", event);
                dispatchSync(SyncEntityType.ISSUE, "UPDATED", event);
            }
            case TICKET_DELETED -> {
                logEvent("Ticket supprimé — suppression externe", event);
                dispatchSync(SyncEntityType.ISSUE, "DELETED", event);
            }

            default -> logger.debug("📡 [EVENT BUS] Événement non routé : {}", event.type());
        }
    }

    /**
     * Provisioning d'un nouveau Customer + Contact pour un User inscrit.
     */
    private void handleUserProvisioning(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isUserProvisioning()) {
            logger.debug("🔇 [SYNC] User provisioning disabled — skipping");
            return;
        }

        Optional<User> userOpt = userRepository.findById(event.entityId());
        if (userOpt.isEmpty()) {
            logger.warn("⚠️ [SYNC] User {} not found for provisioning", event.entityId());
            return;
        }

        User user = userOpt.get();
        Map<String, Object> customerPayload = customerSyncMapper.toCreatePayload(user);

        syncOutboundService.syncEntity(
                SyncEntityType.CUSTOMER,
                "CREATED",
                user.getId(),
                null,
                customerPayload
        );
    }

    /**
     * Mise à jour d'un Customer existant pour un User modifié.
     */
    private void handleUserUpdate(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isUserProvisioning()) return;

        Optional<User> userOpt = userRepository.findById(event.entityId());
        if (userOpt.isEmpty()) {
            logger.warn("⚠️ [SYNC] User {} not found for update", event.entityId());
            return;
        }

        User user = userOpt.get();

        // Si pas encore provisionné, créer au lieu de mettre à jour
        if (user.getExternalCustomerId() == null) {
            Map<String, Object> createPayload = customerSyncMapper.toCreatePayload(user);
            syncOutboundService.syncEntity(SyncEntityType.CUSTOMER, "CREATED", user.getId(), null, createPayload);
            return;
        }

        Map<String, Object> updatePayload = customerSyncMapper.toUpdatePayload(user);
        syncOutboundService.syncEntity(
                SyncEntityType.CUSTOMER,
                "UPDATED",
                user.getId(),
                user.getExternalCustomerId(),
                updatePayload
        );
    }

    /**
     * Commande créée → créer Sales Order + Sales Invoice dans le système externe.
     * La facture sera en statut "Impayé" (pas de Payment Entry).
     * <p>
     * Auto-provisionne les Items manquants avant de créer le Sales Order.
     * <p>
     * Note : pas de @Transactional ici — les méthodes appelées (syncEntity, ensureItemsProvisioned)
     * gèrent leurs propres transactions. On utilise findByIdWithUserAndItems qui charge eagerly
     * pour éviter les LazyInitializationException hors session.
     */
    private void handleOrderCreated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isOrderSync()) {
            logger.debug("🔇 [SYNC] Order sync disabled — skipping");
            return;
        }

        try {
            Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(event.entityId());
            if (orderOpt.isEmpty()) {
                logger.warn("⚠️ [SYNC] Order {} not found for sync", event.entityId());
                return;
            }

            Order order = orderOpt.get();

            // Skip si déjà synchronisée (évite les doublons lors des re-publications)
            if (order.getExternalOrderId() != null) {
                logger.debug("📋 [SYNC] Order {} already has externalOrderId {} — skipping creation",
                        order.getId(), order.getExternalOrderId());
                return;
            }

            // 0. Auto-provisionner les Items manquants sur le système externe
            ensureItemsProvisioned(order);

            // Sales Order — la SINV sera créée automatiquement par le callback
            // (SyncCallbackService.updateOrderExternalOrderId → enqueueInvoiceIfNeeded)
            Map<String, Object> soPayload = orderSyncMapper.toSalesOrderPayload(order);
            syncOutboundService.syncEntity(
                    SyncEntityType.SALES_ORDER, "CREATED",
                    order.getId(), null, soPayload
            );

            logger.info("📤 [SYNC] Order {} enqueued for SO (SINV will chain via callback)", order.getId());
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleOrderCreated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    /**
     * Paiement reçu → créer un Payment Entry pour marquer la facture comme "Payée".
     * <p>
     * Si la commande n'a pas encore été synchronisée (pas d'externalInvoiceId),
     * on crée d'abord SO + SINV puis le Payment Entry sera créé dans le callback
     * de la SINV (via SyncCallbackService.enqueuePaymentEntryIfPaid).
     */
    private void handlePaymentReceived(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isOrderSync()) {
            logger.debug("🔇 [SYNC] Order sync disabled — skipping");
            return;
        }

        try {
            Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(event.entityId());
            if (orderOpt.isEmpty()) {
                logger.warn("⚠️ [SYNC] Order {} not found for payment sync", event.entityId());
                return;
            }

            Order order = orderOpt.get();

            // Skip si un Payment Entry existe déjà
            if (order.getExternalPaymentId() != null) {
                logger.debug("📋 [SYNC] Order {} already has Payment Entry {} — skipping",
                        order.getId(), order.getExternalPaymentId());
                return;
            }

            // Cas 1 : la facture existe déjà côté ERP → créer le Payment Entry directement
            // On récupère le grand_total réel de l'ERP pour éviter les écarts d'arrondi TVA
            // (ex: LMP 1200.00 vs ERP 1199.99 avec included_in_print_rate)
            if (order.getExternalInvoiceId() != null) {
                java.math.BigDecimal erpTotal = fetchErpInvoiceTotal(order.getExternalInvoiceId());
                Map<String, Object> paymentPayload = paymentSyncMapper.toPaymentEntryPayload(
                        order, order.getExternalInvoiceId(), erpTotal);
                syncOutboundService.syncEntity(
                        SyncEntityType.PAYMENT, "CREATED",
                        order.getId(), null, paymentPayload
                );
                logger.info("💳 [SYNC] Payment Entry enqueued for Order {} → SINV {} (erpTotal={})",
                        order.getId(), order.getExternalInvoiceId(), erpTotal);
                return;
            }

            // Cas 2 : pas encore de SINV.
            // Chaîne de callbacks : SO → (callback) → SINV → (callback) → Payment Entry
            // Chaque étape attend le callback de la précédente pour avoir les IDs corrects.

            if (order.getExternalOrderId() != null) {
                // SO existe déjà mais pas de SINV → créer SINV directement
                logger.info("📤 [SYNC] Order {} has SO {} but no SINV — creating SINV",
                        order.getId(), order.getExternalOrderId());
                Map<String, Object> siPayload = orderSyncMapper.toSalesInvoicePayload(order);
                syncOutboundService.syncEntity(
                        SyncEntityType.SALES_INVOICE, "CREATED",
                        order.getId(), null, siPayload
                );
            } else {
                // Ni SO ni SINV → créer le SO, la chaîne fera le reste
                logger.info("📤 [SYNC] Order {} has no SO/SINV — creating SO (chain: SO→SINV→PE)",
                        order.getId());
                ensureItemsProvisioned(order);
                Map<String, Object> soPayload = orderSyncMapper.toSalesOrderPayload(order);
                syncOutboundService.syncEntity(
                        SyncEntityType.SALES_ORDER, "CREATED",
                        order.getId(), null, soPayload
                );
            }
            // SINV + Payment Entry seront créés par les callbacks chaînés
        } catch (Exception e) {
            logger.error("❌ [SYNC] handlePaymentReceived failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    /**
     * Récupère le grand_total réel d'une Sales Invoice depuis le système externe.
     * Permet d'éviter les écarts d'arrondi TVA entre LMP et l'ERP
     * (ex: LMP 1200.00 vs ERP 1199.99 quand included_in_print_rate=1).
     *
     * @return le grand_total ERP, ou null si non récupérable (le mapper utilisera le fallback LMP)
     */
    @SuppressWarnings("unchecked")
    private java.math.BigDecimal fetchErpInvoiceTotal(String salesInvoiceId) {
        try {
            ExternalResponse response = externalClient.getEntity(SyncEntityType.SALES_INVOICE, salesInvoiceId);
            if (response.success() && response.data() != null) {
                // ERPNext API returns {"data": {"grand_total": ...}} — navigate the nested structure
                Map<String, Object> dataMap = response.data();
                if (dataMap.containsKey("data") && dataMap.get("data") instanceof Map) {
                    dataMap = (Map<String, Object>) dataMap.get("data");
                }
                Object grandTotal = dataMap.get("grand_total");
                if (grandTotal != null) {
                    java.math.BigDecimal total = new java.math.BigDecimal(grandTotal.toString());
                    logger.info("📋 [SYNC] Fetched ERP grand_total={} for {}", total, salesInvoiceId);
                    return total;
                }
            }
            logger.warn("⚠️ [SYNC] Could not fetch grand_total for {} — using LMP fallback", salesInvoiceId);
        } catch (Exception e) {
            logger.warn("⚠️ [SYNC] Error fetching grand_total for {}: {}", salesInvoiceId, e.getMessage());
        }
        return null;
    }

    /**
     * S'assure que tous les Items référencés par la commande existent dans le
     * système externe. Si un Service n'a pas d'externalItemCode, l'Item est
     * créé synchronement via l'API externe et le code est stocké localement.
     * <p>
     * Cela évite les LinkValidationError lors de la création du Sales Order.
     */
    private void ensureItemsProvisioned(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            // Commande sans items → le mapper utilisera serviceName comme item_code
            // Provisionner un Item avec ce nom
            ensureSingleItemProvisioned(order.getServiceName(), null);
            return;
        }

        for (OrderItem item : order.getItems()) {
            Service service = item.getService();
            if (service != null && service.getExternalItemCode() == null) {
                ensureServiceItemProvisioned(service);
            }
        }
    }

    /**
     * Provisionne un Item pour un Service LMP spécifique.
     * Appel synchrone à l'API externe, puis sauvegarde l'externalItemCode.
     */
    private void ensureServiceItemProvisioned(Service service) {
        try {
            Map<String, Object> itemPayload = itemSyncMapper.toItemCreatePayload(service);
            ExternalResponse response = externalClient.createEntity(SyncEntityType.ITEM, itemPayload);

            if (response.success() && response.externalId() != null) {
                service.setExternalItemCode(response.externalId());
                serviceRepository.save(service);
                logger.info("🔧 [SYNC] Auto-provisioned Item '{}' for Service '{}'",
                        response.externalId(), service.getTitle());
            } else {
                logger.warn("⚠️ [SYNC] Failed to auto-provision Item for Service '{}': {}",
                        service.getTitle(), response.errorMessage());
            }
        } catch (Exception e) {
            logger.error("❌ [SYNC] Error auto-provisioning Item for Service '{}': {}",
                    service.getTitle(), e.getMessage());
        }
    }

    /**
     * Provisionne un Item générique par nom (fallback quand la commande n'a pas d'OrderItems).
     */
    private void ensureSingleItemProvisioned(String itemName, UUID serviceId) {
        if (itemName == null || itemName.isBlank()) return;

        try {
            Map<String, Object> itemPayload = new java.util.LinkedHashMap<>();
            itemPayload.put("item_code", itemName);
            itemPayload.put("item_name", itemName);
            itemPayload.put("item_group", "Services");
            itemPayload.put("stock_uom", "Nos");
            itemPayload.put("is_stock_item", 0);
            itemPayload.put("is_sales_item", 1);

            ExternalResponse response = externalClient.createEntity(SyncEntityType.ITEM, itemPayload);

            if (response.success()) {
                logger.info("🔧 [SYNC] Auto-provisioned generic Item '{}'", itemName);
            } else {
                // Si l'Item existe déjà (DuplicateEntryError), c'est OK
                if (response.errorMessage() != null && response.errorMessage().contains("DuplicateEntryError")) {
                    logger.debug("📋 [SYNC] Item '{}' already exists — OK", itemName);
                } else {
                    logger.warn("⚠️ [SYNC] Failed to auto-provision Item '{}': {}", itemName, response.errorMessage());
                }
            }
        } catch (Exception e) {
            logger.error("❌ [SYNC] Error auto-provisioning Item '{}': {}", itemName, e.getMessage());
        }
    }

    /**
     * Mise à jour ou annulation d'une commande existante.
     * <p>
     * Si la commande est passée à CANCELLED, on supprime les documents liés
     * dans le système externe (Payment Entry → SINV → SO) dans l'ordre FIFO.
     * Cela gère les cas de réconciliation où le SaaS annule une commande
     * dont le paiement n'a pas abouti.
     */
    private void handleOrderUpdate(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isOrderSync()) return;

        try {
            Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(event.entityId());
            if (orderOpt.isEmpty()) {
                logger.warn("⚠️ [SYNC] Order {} not found for update", event.entityId());
                return;
            }

            Order order = orderOpt.get();

            // Vérifier si c'est une annulation
            Map<String, Object> eventPayload = event.payload();
            String newStatus = eventPayload != null ? (String) eventPayload.get("newStatus") : null;
            boolean isCancellation = "CANCELLED".equals(newStatus)
                    || order.getStatus() == com.lmp.billing.domain.OrderStatus.CANCELLED;

            if (isCancellation) {
                handleOrderCancellation(order);
                return;
            }

            // Mise à jour simple (notes, remarques)
            if (order.getExternalOrderId() == null) {
                logger.debug("⚠️ [SYNC] Order {} has no externalOrderId — skipping update", order.getId());
                return;
            }

            Map<String, Object> payload = orderSyncMapper.toSalesOrderUpdatePayload(order);
            syncOutboundService.syncEntity(
                    SyncEntityType.SALES_ORDER, "UPDATED",
                    order.getId(), order.getExternalOrderId(), payload
            );
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleOrderUpdate failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    /**
     * Annulation d'une commande → suppression cascade des documents ERP.
     * Ordre : Payment Entry → Sales Invoice → Sales Order (enfants d'abord).
     * Les callbacks de suppression nettoient les external IDs sur l'Order local.
     */
    private void handleOrderCancellation(Order order) {
        logger.info("🗑️ [SYNC] Cancelling ERP documents for Order {}", order.getId());

        // 1. Payment Entry (doit être supprimé avant la facture)
        if (order.getExternalPaymentId() != null && !order.getExternalPaymentId().isBlank()) {
            syncOutboundService.enqueue(
                    SyncEntityType.PAYMENT, "DELETED",
                    order.getId(), order.getExternalPaymentId(), Map.of()
            );
        }

        // 2. Sales Invoice
        if (order.getExternalInvoiceId() != null && !order.getExternalInvoiceId().isBlank()) {
            syncOutboundService.enqueue(
                    SyncEntityType.SALES_INVOICE, "DELETED",
                    order.getId(), order.getExternalInvoiceId(), Map.of()
            );
        }

        // 3. Sales Order
        if (order.getExternalOrderId() != null && !order.getExternalOrderId().isBlank()) {
            syncOutboundService.enqueue(
                    SyncEntityType.SALES_ORDER, "DELETED",
                    order.getId(), order.getExternalOrderId(), Map.of()
            );
        }
    }

    /**
     * Facture générée → créer Sales Invoice si pas déjà fait lors de ORDER_CONFIRMED.
     */
    private void handleInvoiceGenerated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isOrderSync()) return;

        try {
            // L'entityId peut être l'orderId — récupérer depuis le payload
            UUID orderId = event.entityId();
            Map<String, Object> eventPayload = event.payload();
            if (eventPayload != null && eventPayload.containsKey("orderId")) {
                orderId = UUID.fromString(eventPayload.get("orderId").toString());
            }

            Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(orderId);
            if (orderOpt.isEmpty()) {
                logger.warn("⚠️ [SYNC] Order {} not found for invoice sync", orderId);
                return;
            }

            Order order = orderOpt.get();

            // Skip si déjà liée à une facture externe
            if (order.getExternalInvoiceId() != null) {
                logger.debug("📋 [SYNC] Order {} already has externalInvoiceId — skipping", order.getId());
                return;
            }

            Map<String, Object> siPayload = orderSyncMapper.toSalesInvoicePayload(order);
            syncOutboundService.syncEntity(
                    SyncEntityType.SALES_INVOICE, "CREATED",
                    order.getId(), null, siPayload
            );
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleInvoiceGenerated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    /**
     * Suppression d'un User → cascade : supprimer d'abord les documents liés
     * dans le système externe (Sales Invoice, Sales Order, Contact, Payment Entry),
     * puis le Customer lui-même.
     * <p>
     * L'ordre FIFO de la queue garantit que les enfants sont traités avant le parent.
     */
    private void handleUserDelete(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isUserProvisioning()) return;

        // Le payload doit contenir l'externalCustomerId (set avant la suppression)
        Map<String, Object> payload = event.payload();
        String externalCustomerId = payload != null ? (String) payload.get("externalCustomerId") : null;

        if (externalCustomerId == null || externalCustomerId.isBlank()) {
            logger.debug("📋 [SYNC] User {} has no externalCustomerId — nothing to delete externally",
                    event.entityId());
            return;
        }

        logger.info("🗑️ [SYNC CASCADE] Deleting Customer '{}' and linked docs for User {}",
                externalCustomerId, event.entityId());

        // Ordre de suppression : enfants d'abord, parent ensuite
        // 1. Payment Entries liés au customer (doivent être supprimés avant les factures)
        enqueueCascadeDeletes(SyncEntityType.PAYMENT, "party", externalCustomerId, event.entityId());

        // 2. Sales Invoices liés au customer
        enqueueCascadeDeletes(SyncEntityType.SALES_INVOICE, "customer", externalCustomerId, event.entityId());

        // 3. Sales Orders liés au customer
        enqueueCascadeDeletes(SyncEntityType.SALES_ORDER, "customer", externalCustomerId, event.entityId());

        // 4. Contacts liés (Dynamic Link)
        enqueueCascadeDeletes(SyncEntityType.CONTACT, "name", null, event.entityId());

        // 5. Enfin le Customer lui-même
        syncOutboundService.enqueue(
                SyncEntityType.CUSTOMER, "DELETED",
                event.entityId(), externalCustomerId, Map.of()
        );
    }

    /**
     * Suppression d'une commande → supprimer Sales Invoice + Sales Order liés.
     */
    private void handleOrderDelete(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isOrderSync()) return;

        Map<String, Object> payload = event.payload();
        String externalOrderId = payload != null ? (String) payload.get("externalOrderId") : null;
        String externalInvoiceId = payload != null ? (String) payload.get("externalInvoiceId") : null;

        // 0. Supprimer le Payment Entry d'abord (dépend de la facture)
        String externalPaymentId = payload != null ? (String) payload.get("externalPaymentId") : null;
        if (externalPaymentId != null && !externalPaymentId.isBlank()) {
            syncOutboundService.enqueue(
                    SyncEntityType.PAYMENT, "DELETED",
                    event.entityId(), externalPaymentId, Map.of()
            );
        }

        // 1. Supprimer la facture d'abord (dépend du SO dans certains cas)
        if (externalInvoiceId != null && !externalInvoiceId.isBlank()) {
            syncOutboundService.enqueue(
                    SyncEntityType.SALES_INVOICE, "DELETED",
                    event.entityId(), externalInvoiceId, Map.of()
            );
        }

        // 2. Puis le Sales Order
        if (externalOrderId != null && !externalOrderId.isBlank()) {
            syncOutboundService.enqueue(
                    SyncEntityType.SALES_ORDER, "DELETED",
                    event.entityId(), externalOrderId, Map.of()
            );
        }
    }

    /**
     * Découvre les entités liées côté système externe et enqueue leur suppression.
     * Utilise la queue FIFO pour garantir l'ordre (enfants supprimés avant parent).
     */
    private void enqueueCascadeDeletes(SyncEntityType childType, String parentField,
                                       String parentId, UUID localEntityId) {
        if (parentId == null) return;

        try {
            List<String> linkedIds = externalClient.listLinkedEntityIds(childType, parentField, parentId);
            for (String linkedId : linkedIds) {
                syncOutboundService.enqueue(childType, "DELETED", localEntityId, linkedId, Map.of());
                logger.info("🗑️ [SYNC CASCADE] Enqueued delete {} '{}'", childType, linkedId);
            }
        } catch (Exception e) {
            logger.warn("⚠️ [SYNC CASCADE] Failed to list linked {} for {}={}: {}",
                    childType, parentField, parentId, e.getMessage());
        }
    }

    /**
     * Dispatch générique vers le SyncOutboundService (pour les entités non-User).
     */
    private void dispatchSync(SyncEntityType entityType, String syncEventType, LmpBusinessEvent event) {
        Map<String, Object> payload = event.payload();
        String externalId = payload != null ? (String) payload.get("externalId") : null;

        syncOutboundService.syncEntity(
                entityType,
                syncEventType,
                event.entityId(),
                externalId,
                payload != null ? payload : Map.of()
        );
    }

    private void logEvent(String description, LmpBusinessEvent event) {
        logger.info("📡 [ERP SYNC] {} | type={} | entityId={}",
                description, event.type(), event.entityId());
    }
}
