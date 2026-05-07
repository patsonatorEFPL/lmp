package com.lmp.integration.event;

import com.lmp.auth.domain.User;
import com.lmp.auth.repository.UserRepository;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderItem;
import com.lmp.billing.domain.Quotation;
import com.lmp.billing.domain.QuotationItem;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.QuotationRepository;
import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceOffer;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncProperties;
import com.lmp.integration.sync.mapper.AddressSyncMapper;
import com.lmp.integration.sync.mapper.CustomerSyncMapper;
import com.lmp.integration.sync.mapper.ErpUserSyncMapper;
import com.lmp.integration.sync.mapper.ItemPriceSyncMapper;
import com.lmp.integration.sync.mapper.ItemSyncMapper;
import com.lmp.integration.sync.mapper.OrderSyncMapper;
import com.lmp.integration.sync.mapper.PaymentSyncMapper;
import com.lmp.integration.sync.mapper.ProjectSyncMapper;
import com.lmp.integration.sync.mapper.QuotationSyncMapper;
import com.lmp.integration.sync.mapper.TaskSyncMapper;
import com.lmp.integration.sync.mapper.TicketSyncMapper;
import com.lmp.integration.sync.service.SyncOutboundService;
import com.lmp.project.domain.Project;
import com.lmp.project.domain.ProjectTask;
import com.lmp.project.repository.ProjectRepository;
import com.lmp.project.repository.ProjectTaskRepository;
import com.lmp.support.domain.Ticket;
import com.lmp.support.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
 * <p>
 * <b>Contrat Outbox :</b> Le write business (Order, User…) et le publish de l'événement sont
 * dans la même transaction Spring. L'enqueue dans {@code sync_event_log} est effectué par
 * {@code SyncOutboundService.enqueue()} dans le thread @Async du listener, APRÈS le commit.
 * <p>
 * <b>Risque connu :</b> Si le thread @Async crash entre le commit business et l'enqueue,
 * l'événement est perdu. Ce cas est rattrapé par {@code SyncReconciliationService} qui détecte
 * les entités CONFIRMED sans {@code externalOrderId} et ré-enqueue la synchronisation.
 * Ce trade-off (at-least-once avec réconciliation) est accepté pour éviter la complexité
 * d'un vrai Outbox pattern (polling table dédiée).
 */
@Component
public class ErpEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ErpEventListener.class);

    private final SyncOutboundService syncOutboundService;
    private final SyncProperties syncProperties;
    private final ExternalSystemClient externalClient;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final QuotationRepository quotationRepository;
    private final ServiceRepository serviceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final TicketRepository ticketRepository;
    private final CustomerSyncMapper customerSyncMapper;
    private final OrderSyncMapper orderSyncMapper;
    private final ItemSyncMapper itemSyncMapper;
    private final PaymentSyncMapper paymentSyncMapper;
    private final QuotationSyncMapper quotationSyncMapper;
    private final AddressSyncMapper addressSyncMapper;
    private final ProjectSyncMapper projectSyncMapper;
    private final TaskSyncMapper taskSyncMapper;
    private final TicketSyncMapper ticketSyncMapper;
    private final ItemPriceSyncMapper itemPriceSyncMapper;
    private final ErpUserSyncMapper erpUserSyncMapper;

    public ErpEventListener(SyncOutboundService syncOutboundService,
                            SyncProperties syncProperties,
                            ExternalSystemClient externalClient,
                            UserRepository userRepository,
                            OrderRepository orderRepository,
                            QuotationRepository quotationRepository,
                            ServiceRepository serviceRepository,
                            ProjectRepository projectRepository,
                            ProjectTaskRepository projectTaskRepository,
                            TicketRepository ticketRepository,
                            CustomerSyncMapper customerSyncMapper,
                            OrderSyncMapper orderSyncMapper,
                            ItemSyncMapper itemSyncMapper,
                            PaymentSyncMapper paymentSyncMapper,
                            QuotationSyncMapper quotationSyncMapper,
                            AddressSyncMapper addressSyncMapper,
                            ProjectSyncMapper projectSyncMapper,
                            TaskSyncMapper taskSyncMapper,
                            TicketSyncMapper ticketSyncMapper,
                            ItemPriceSyncMapper itemPriceSyncMapper,
                            ErpUserSyncMapper erpUserSyncMapper) {
        this.syncOutboundService = syncOutboundService;
        this.syncProperties = syncProperties;
        this.externalClient = externalClient;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.quotationRepository = quotationRepository;
        this.serviceRepository = serviceRepository;
        this.projectRepository = projectRepository;
        this.projectTaskRepository = projectTaskRepository;
        this.ticketRepository = ticketRepository;
        this.customerSyncMapper = customerSyncMapper;
        this.orderSyncMapper = orderSyncMapper;
        this.itemSyncMapper = itemSyncMapper;
        this.paymentSyncMapper = paymentSyncMapper;
        this.quotationSyncMapper = quotationSyncMapper;
        this.addressSyncMapper = addressSyncMapper;
        this.projectSyncMapper = projectSyncMapper;
        this.taskSyncMapper = taskSyncMapper;
        this.ticketSyncMapper = ticketSyncMapper;
        this.itemPriceSyncMapper = itemPriceSyncMapper;
        this.erpUserSyncMapper = erpUserSyncMapper;
    }

    // @Transactional(readOnly=true) ouvre une session Hibernate pour ce thread async :
    // les `findById` + accès à des collections LAZY (User.roles, Order.items, etc.)
    // marchent. Sans, chaque event explose en LazyInitializationException une fois le
    // commit business terminé (session originale fermée).
    //
    // RestExternalClient a un read-timeout de 8s : Hibernate libère sa connexion entre
    // les SELECT et l'appel HTTP (handling_mode=DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT,
    // défaut JPA). La connexion DB n'est donc PAS bloquée pendant l'attente Frappe.
    @Async
    @Transactional(readOnly = true)
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

            // --- Devis → QUOTATION ---
            case QUOTATION_CREATED, QUOTATION_SENT -> {
                logEvent("Devis créé/envoyé — synchronisation externe", event);
                handleQuotationCreated(event);
            }
            case QUOTATION_ACCEPTED -> {
                logEvent("Devis accepté — conversion via make_sales_order", event);
                handleQuotationAccepted(event);
            }
            case QUOTATION_REJECTED -> {
                logEvent("Devis refusé — mise à jour externe", event);
                handleQuotationRejected(event);
            }
            case QUOTATION_UPDATED -> {
                logEvent("Devis mis à jour — synchronisation externe", event);
                handleQuotationUpdate(event);
            }
            case QUOTATION_DELETED -> {
                logEvent("Devis supprimé — suppression externe", event);
                handleQuotationDelete(event);
            }

            // --- Lead / Contact ---
            case CONTACT_FORM_SUBMITTED ->
                    logEvent("Lead — synchronisation externe (CRM)", event);

            // --- Catalogue → ITEM + ItemPrice ---
            case SERVICE_CREATED -> {
                logEvent("Service créé — synchronisation externe", event);
                handleServiceCreated(event);
            }
            case SERVICE_UPDATED -> {
                logEvent("Service mis à jour — synchronisation externe", event);
                handleServiceUpdated(event);
            }
            case SERVICE_DELETED -> {
                logEvent("Service supprimé — suppression externe", event);
                handleServiceDeleted(event);
            }

            // --- Projets → PROJECT / TASK ---
            case PROJECT_CREATED -> {
                logEvent("Projet créé — synchronisation externe", event);
                handleProjectCreated(event);
            }
            case PROJECT_UPDATED -> {
                logEvent("Projet mis à jour — synchronisation externe", event);
                handleProjectUpdated(event);
            }
            case PROJECT_DELETED -> {
                logEvent("Projet supprimé — suppression externe", event);
                handleProjectDeleted(event);
            }
            case TASK_CREATED -> {
                logEvent("Tâche créée — synchronisation externe", event);
                handleTaskCreated(event);
            }
            case TASK_UPDATED -> {
                logEvent("Tâche mise à jour — synchronisation externe", event);
                handleTaskUpdated(event);
            }
            case TASK_DELETED -> {
                logEvent("Tâche supprimée — suppression externe", event);
                handleTaskDeleted(event);
            }

            // --- Support → ISSUE ---
            case TICKET_CREATED -> {
                logEvent("Ticket créé — synchronisation externe", event);
                handleTicketCreated(event);
            }
            case TICKET_UPDATED -> {
                logEvent("Ticket mis à jour — synchronisation externe", event);
                handleTicketUpdated(event);
            }
            case TICKET_RESOLVED -> {
                logEvent("Ticket résolu — synchronisation externe", event);
                handleTicketResolved(event);
            }
            case TICKET_DELETED -> {
                logEvent("Ticket supprimé — suppression externe", event);
                handleTicketDeleted(event);
            }

            default -> logger.debug("📡 [EVENT BUS] Événement non routé : {}", event.type());
        }
    }

    /**
     * Provisioning d'un nouveau User inscrit.
     * <ul>
     *   <li>STAFF/ADMIN → DocType "User" ERPNext (login)</li>
     *   <li>USER classique → DocType "Customer" ERPNext (compte client)</li>
     * </ul>
     */
    private void handleUserProvisioning(LmpBusinessEvent event) {
        Optional<User> userOpt = userRepository.findById(event.entityId());
        if (userOpt.isEmpty()) {
            logger.warn("⚠️ [SYNC] User {} not found for provisioning", event.entityId());
            return;
        }

        User user = userOpt.get();

        if (user.isStaff()) {
            provisionErpUser(user);
        } else {
            provisionCustomer(user);
        }
    }

    private void provisionCustomer(User user) {
        if (!syncProperties.getFeatures().isUserProvisioning()) {
            logger.debug("🔇 [SYNC] Customer provisioning disabled — skipping");
            return;
        }
        Map<String, Object> customerPayload = customerSyncMapper.toCreatePayload(user);
        syncOutboundService.syncEntity(
                SyncEntityType.CUSTOMER,
                "CREATED",
                user.getId(),
                null,
                customerPayload
        );
    }

    private void provisionErpUser(User user) {
        if (!syncProperties.getFeatures().isStaffProvisioning()) {
            logger.debug("🔇 [SYNC] Staff provisioning disabled — skipping");
            return;
        }
        Map<String, Object> userPayload = erpUserSyncMapper.toCreatePayload(user);
        syncOutboundService.syncEntity(
                SyncEntityType.ERP_USER,
                "CREATED",
                user.getId(),
                null,
                userPayload
        );
    }

    /**
     * Mise à jour d'un User : Customer (clients) ou ERPNext User (collaborateurs).
     */
    private void handleUserUpdate(LmpBusinessEvent event) {
        Optional<User> userOpt = userRepository.findById(event.entityId());
        if (userOpt.isEmpty()) {
            logger.warn("⚠️ [SYNC] User {} not found for update", event.entityId());
            return;
        }

        User user = userOpt.get();

        if (user.isStaff()) {
            updateErpUser(user);
        } else {
            updateCustomer(user);
        }
    }

    private void updateCustomer(User user) {
        if (!syncProperties.getFeatures().isUserProvisioning()) return;

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

        syncUserAddress(user);
    }

    private void updateErpUser(User user) {
        if (!syncProperties.getFeatures().isStaffProvisioning()) return;

        if (user.getExternalErpUserId() == null) {
            Map<String, Object> createPayload = erpUserSyncMapper.toCreatePayload(user);
            syncOutboundService.syncEntity(SyncEntityType.ERP_USER, "CREATED", user.getId(), null, createPayload);
            return;
        }

        Map<String, Object> updatePayload = erpUserSyncMapper.toUpdatePayload(user);
        syncOutboundService.syncEntity(
                SyncEntityType.ERP_USER,
                "UPDATED",
                user.getId(),
                user.getExternalErpUserId(),
                updatePayload
        );
    }

    /**
     * Synchronise l'adresse du User vers ERPNext (création ou mise à jour).
     * Appel synchrone — ne passe pas par la queue car l'Address dépend du Customer
     * qui doit déjà exister.
     */
    private void syncUserAddress(User user) {
        if (!addressSyncMapper.hasAddressData(user)) {
            return;
        }
        if (user.getExternalCustomerId() == null) {
            logger.debug("📋 [SYNC] User {} has no externalCustomerId — skipping address sync", user.getId());
            return;
        }

        try {
            if (user.getExternalAddressId() != null) {
                // Mise à jour
                Map<String, Object> payload = addressSyncMapper.toUpdatePayload(user);
                ExternalResponse response = externalClient.updateEntity(
                        SyncEntityType.ADDRESS, user.getExternalAddressId(), payload);
                if (response.success()) {
                    logger.info("✅ [SYNC] Updated Address {} for User {}",
                            user.getExternalAddressId(), user.getId());
                } else {
                    logger.warn("⚠️ [SYNC] Failed to update Address for User {}: {}",
                            user.getId(), response.errorMessage());
                }
            } else {
                // Création (peut arriver si le Customer existait déjà sans Address)
                Map<String, Object> payload = addressSyncMapper.toCreatePayload(user, user.getExternalCustomerId());
                ExternalResponse response = externalClient.createEntity(SyncEntityType.ADDRESS, payload);
                if (response.success() && response.externalId() != null) {
                    user.setExternalAddressId(response.externalId());
                    userRepository.save(user);
                    logger.info("🔗 [SYNC] Created Address {} for User {}",
                            response.externalId(), user.getId());
                } else {
                    logger.warn("⚠️ [SYNC] Failed to create Address for User {}: {}",
                            user.getId(), response.errorMessage());
                }
            }
        } catch (Exception e) {
            logger.error("❌ [SYNC] Error syncing Address for User {}: {}", user.getId(), e.getMessage());
        }
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
            } else if (service == null) {
                // Fallback : item sans service lié → provisionner par serviceName
                ensureSingleItemProvisioned(order.getServiceName(), null);
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
            } else if (response.errorMessage() != null && response.errorMessage().contains("DuplicateEntryError")) {
                // L'Item existe déjà — chercher son ID exact dans l'ERP et le lier
                String existingId = findExistingItemId(service.getTitle());
                if (existingId != null) {
                    service.setExternalItemCode(existingId);
                    serviceRepository.save(service);
                    logger.info("🔗 [SYNC] Linked existing Item '{}' to Service '{}'",
                            existingId, service.getTitle());
                } else {
                    logger.debug("📋 [SYNC] Item '{}' already exists — OK", service.getTitle());
                }
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
     * Cherche un Item existant dans l'ERP par son item_code (name).
     * @return le name de l'item, ou null si non trouvé
     */
    private String findExistingItemId(String itemCode) {
        try {
            ExternalResponse response = externalClient.getEntity(SyncEntityType.ITEM, itemCode);
            if (response.success() && response.data() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.data();
                if (data.containsKey("data")) {
                    data = (Map<String, Object>) data.get("data");
                }
                Object name = data.get("name");
                return name != null ? name.toString() : null;
            }
        } catch (Exception e) {
            logger.debug("🔍 [SYNC] Could not find existing Item '{}': {}", itemCode, e.getMessage());
        }
        return null;
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
            } else if (response.errorMessage() != null && response.errorMessage().contains("DuplicateEntryError")) {
                logger.debug("📋 [SYNC] Item '{}' already exists — OK", itemName);
            } else {
                logger.warn("⚠️ [SYNC] Failed to auto-provision Item '{}': {}", itemName, response.errorMessage());
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
        Map<String, Object> payload = event.payload();
        String externalErpUserId = payload != null ? (String) payload.get("externalErpUserId") : null;
        String externalCustomerId = payload != null ? (String) payload.get("externalCustomerId") : null;

        // Cas collaborateur : supprimer le User ERPNext s'il existe
        if (externalErpUserId != null && !externalErpUserId.isBlank()
                && syncProperties.getFeatures().isStaffProvisioning()) {
            logger.info("🗑️ [SYNC] Deleting ERPNext User '{}' for User {}",
                    externalErpUserId, event.entityId());
            syncOutboundService.enqueue(
                    SyncEntityType.ERP_USER, "DELETED",
                    event.entityId(), externalErpUserId, Map.of()
            );
        }

        if (!syncProperties.getFeatures().isUserProvisioning()) return;

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

    // ==================== Quotation Handlers ====================

    /**
     * Devis créé ou envoyé → créer Quotation dans le système externe.
     * Auto-provisionne les Items manquants comme pour les commandes.
     */
    private void handleQuotationCreated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isQuotationSync()) {
            logger.debug("🔇 [SYNC] Quotation sync disabled — skipping");
            return;
        }

        try {
            Optional<Quotation> quotationOpt = quotationRepository.findByIdWithUserAndItems(event.entityId());
            if (quotationOpt.isEmpty()) {
                logger.warn("⚠️ [SYNC] Quotation {} not found for sync", event.entityId());
                return;
            }

            Quotation quotation = quotationOpt.get();

            if (quotation.getExternalQuotationId() != null) {
                logger.debug("📋 [SYNC] Quotation {} already has externalId {} — skipping",
                        quotation.getId(), quotation.getExternalQuotationId());
                return;
            }

            // Auto-provisionner les Items manquants
            ensureQuotationItemsProvisioned(quotation);

            Map<String, Object> payload = quotationSyncMapper.toQuotationPayload(quotation);
            syncOutboundService.syncEntity(
                    SyncEntityType.QUOTATION, "CREATED",
                    quotation.getId(), null, payload
            );

            logger.info("📤 [SYNC] Quotation {} enqueued for external creation", quotation.getId());
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleQuotationCreated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    /**
     * Devis accepté → appelle make_sales_order côté ERPNext pour convertir
     * le Quotation en Sales Order. L'external SO ID est ensuite stocké sur
     * l'Order LMP issue de la conversion (via QuotationService.convertToOrder).
     */
    private void handleQuotationAccepted(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isQuotationSync()) return;

        try {
            Optional<Quotation> quotationOpt = quotationRepository.findByIdWithUserAndItems(event.entityId());
            if (quotationOpt.isEmpty()) {
                logger.warn("⚠️ [SYNC] Quotation {} not found for acceptance", event.entityId());
                return;
            }

            Quotation quotation = quotationOpt.get();
            String externalQuotationId = quotation.getExternalQuotationId();

            if (externalQuotationId == null) {
                logger.warn("⚠️ [SYNC] Quotation {} has no externalId — cannot call make_sales_order",
                        quotation.getId());
                return;
            }

            // 0. S'assurer que le Quotation est soumis (docstatus=1)
            submitQuotationIfNeeded(externalQuotationId);

            // 1. Attendre que le submit soit persisté côté ERPNext
            Thread.sleep(1500);

            // 2. Récupérer l'Order depuis l'event (évite LazyInitializationException sur convertedOrder)
            String orderIdStr = event.payload() != null ? (String) event.payload().get("orderId") : null;
            if (orderIdStr == null) {
                logger.error("❌ [SYNC] QUOTATION_ACCEPTED event missing orderId metadata");
                return;
            }
            Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(UUID.fromString(orderIdStr));
            if (orderOpt.isEmpty()) {
                logger.error("❌ [SYNC] Order {} not found for Quotation {}", orderIdStr, quotation.getId());
                return;
            }
            Order order = orderOpt.get();

            // 3. Créer le SO directement à partir de l'Order (évite les problèmes de template
            //    make_sales_order qui contient des champs calculés incompatible avec frappe.client.insert)
            //    Le lien Quotation est injecté automatiquement par OrderSyncMapper.toSalesOrderPayload()
            //    via order.getQuotation().getExternalQuotationId().
            Map<String, Object> soData = orderSyncMapper.toSalesOrderPayload(order);

            ExternalResponse createResponse = externalClient.createEntity(SyncEntityType.SALES_ORDER, soData);

            if (createResponse.success() && createResponse.externalId() != null) {
                String soExternalId = createResponse.externalId();
                logger.info("✅ [SYNC] Quotation {} → SO {} created", quotation.getId(), soExternalId);

                order.setExternalOrderId(soExternalId);
                orderRepository.save(order);
                logger.info("🔗 [SYNC] Order {} linked to SO {} (from Quotation conversion)",
                        order.getId(), soExternalId);
            } else {
                logger.error("❌ [SYNC] Failed to create SO from Quotation {}: {}",
                        quotation.getId(), createResponse.errorMessage());
            }
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleQuotationAccepted failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    /**
     * Soumet un Quotation ERPNext s'il est encore en Draft (docstatus=0).
     * Nécessaire car make_sales_order exige docstatus=1.
     */
    private void submitQuotationIfNeeded(String externalQuotationId) {
        try {
            ExternalResponse docResponse = externalClient.getEntity(SyncEntityType.QUOTATION, externalQuotationId);
            if (docResponse.success() && docResponse.data() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) docResponse.data();
                if (data.containsKey("data") && data.get("data") instanceof Map) {
                    data = (Map<String, Object>) data.get("data");
                }
                Object docstatus = data.get("docstatus");
                int status = (docstatus instanceof Number) ? ((Number) docstatus).intValue() : 0;
                if (status == 0) {
                    // frappe.client.submit nécessite le document complet avec 'modified' pour éviter
                    // TimestampMismatchError ("modified after you have opened it")
                    data.put("docstatus", 1);
                    ExternalResponse submitResponse = externalClient.callMethod("frappe.client.submit",
                            Map.of("doc", data));
                    if (submitResponse.success()) {
                        logger.info("📋 [SYNC] Submitted Quotation '{}' before make_sales_order", externalQuotationId);
                    } else {
                        logger.warn("⚠️ [SYNC] Failed to submit Quotation '{}': {} — proceeding anyway",
                                externalQuotationId, submitResponse.errorMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ [SYNC] Could not submit Quotation '{}': {} — proceeding with make_sales_order anyway",
                    externalQuotationId, e.getMessage());
        }
    }

    /**
     * Devis refusé → déclarer comme "Lost" côté système externe.
     */
    private void handleQuotationRejected(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isQuotationSync()) return;

        try {
            Optional<Quotation> quotationOpt = quotationRepository.findById(event.entityId());
            if (quotationOpt.isEmpty()) return;

            Quotation quotation = quotationOpt.get();
            if (quotation.getExternalQuotationId() == null) {
                logger.debug("📋 [SYNC] Quotation {} has no externalId — skipping reject", quotation.getId());
                return;
            }

            // Appeler declare_order_lost sur ERPNext
            externalClient.callMethod(
                    "erpnext.selling.doctype.quotation.quotation.declare_order_lost",
                    Map.of(
                            "docname", quotation.getExternalQuotationId(),
                            "lost_reasons_list", List.of(Map.of("lost_reason", "Client refusal")),
                            "detailed_reason", "Rejected by client via LMP"
                    )
            );

            logger.info("📋 [SYNC] Quotation {} declared as Lost externally", quotation.getId());
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleQuotationRejected failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    /**
     * Devis mis à jour → synchroniser les modifications.
     */
    private void handleQuotationUpdate(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isQuotationSync()) return;

        try {
            Optional<Quotation> quotationOpt = quotationRepository.findByIdWithUserAndItems(event.entityId());
            if (quotationOpt.isEmpty()) return;

            Quotation quotation = quotationOpt.get();
            if (quotation.getExternalQuotationId() == null) {
                logger.debug("📋 [SYNC] Quotation {} has no externalId — skipping update", quotation.getId());
                return;
            }

            Map<String, Object> payload = quotationSyncMapper.toQuotationUpdatePayload(quotation);
            syncOutboundService.syncEntity(
                    SyncEntityType.QUOTATION, "UPDATED",
                    quotation.getId(), quotation.getExternalQuotationId(), payload
            );
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleQuotationUpdate failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    /**
     * Devis supprimé → supprimer côté système externe.
     */
    private void handleQuotationDelete(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isQuotationSync()) return;

        Map<String, Object> payload = event.payload();
        String externalQuotationId = payload != null ? (String) payload.get("externalQuotationId") : null;

        if (externalQuotationId != null && !externalQuotationId.isBlank()) {
            syncOutboundService.enqueue(
                    SyncEntityType.QUOTATION, "DELETED",
                    event.entityId(), externalQuotationId, Map.of()
            );
        }
    }

    // ==================== Service / ItemPrice Handlers ====================

    private void handleServiceCreated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isCatalogSync()) {
            logger.debug("🔇 [SYNC] Catalog sync disabled — skipping");
            return;
        }
        try {
            Optional<Service> opt = serviceRepository.findByIdWithOffers(event.entityId());
            if (opt.isEmpty()) {
                logger.warn("⚠️ [SYNC] Service {} not found for sync", event.entityId());
                return;
            }
            Service service = opt.get();

            // 1. Sync Item
            Map<String, Object> itemPayload = itemSyncMapper.toItemCreatePayload(service);
            syncOutboundService.syncEntity(SyncEntityType.ITEM, "CREATED", service.getId(), null, itemPayload);

            // 2. Sync Item Prices for active offers
            if (service.getOffers() != null) {
                for (ServiceOffer offer : service.getOffers()) {
                    if (Boolean.TRUE.equals(offer.getActive()) && offer.isCurrentlyValid()) {
                        itemPriceSyncMapper.upsertItemPrice(offer, service);
                    }
                }
            }
            logger.info("📤 [SYNC] Service {} enqueued with {} offers", service.getId(),
                    service.getOffers() != null ? service.getOffers().size() : 0);
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleServiceCreated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    private void handleServiceUpdated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isCatalogSync()) return;
        try {
            Optional<Service> opt = serviceRepository.findByIdWithOffers(event.entityId());
            if (opt.isEmpty()) return;
            Service service = opt.get();

            String externalItemCode = service.getExternalItemCode();
            if (externalItemCode == null || externalItemCode.isBlank()) {
                logger.debug("📋 [SYNC] Service {} has no externalItemCode — skipping update", service.getId());
                return;
            }

            // 1. Update Item
            Map<String, Object> itemPayload = itemSyncMapper.toItemCreatePayload(service);
            syncOutboundService.syncEntity(SyncEntityType.ITEM, "UPDATED", service.getId(), externalItemCode, itemPayload);

            // 2. Upsert Item Prices for active offers
            if (service.getOffers() != null) {
                for (ServiceOffer offer : service.getOffers()) {
                    if (Boolean.TRUE.equals(offer.getActive()) && offer.isCurrentlyValid()) {
                        itemPriceSyncMapper.upsertItemPrice(offer, service);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleServiceUpdated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    private void handleServiceDeleted(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isCatalogSync()) return;
        Map<String, Object> payload = event.payload();
        String externalItemCode = payload != null ? (String) payload.get("externalItemCode") : null;
        if (externalItemCode != null && !externalItemCode.isBlank()) {
            syncOutboundService.enqueue(SyncEntityType.ITEM, "DELETED", event.entityId(), externalItemCode, Map.of());
        }
    }

    // ==================== Project Handlers ====================

    private void handleProjectCreated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isProjectSync()) {
            logger.debug("🔇 [SYNC] Project sync disabled — skipping");
            return;
        }
        try {
            Optional<Project> opt = projectRepository.findById(event.entityId());
            if (opt.isEmpty()) {
                logger.warn("⚠️ [SYNC] Project {} not found for sync", event.entityId());
                return;
            }
            Project project = opt.get();
            if (project.getExternalProjectId() != null) {
                logger.debug("📋 [SYNC] Project {} already has externalProjectId {} — skipping",
                        project.getId(), project.getExternalProjectId());
                return;
            }
            Map<String, Object> payload = projectSyncMapper.toOutboundPayload(project);
            syncOutboundService.syncEntity(SyncEntityType.PROJECT, "CREATED", project.getId(), null, payload);
            logger.info("📤 [SYNC] Project {} enqueued for external creation", project.getId());
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleProjectCreated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    private void handleProjectUpdated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isProjectSync()) return;
        try {
            Optional<Project> opt = projectRepository.findById(event.entityId());
            if (opt.isEmpty()) return;
            Project project = opt.get();
            if (project.getExternalProjectId() == null) {
                logger.debug("📋 [SYNC] Project {} has no externalProjectId — skipping update", project.getId());
                return;
            }
            Map<String, Object> payload = projectSyncMapper.toOutboundPayload(project);
            syncOutboundService.syncEntity(SyncEntityType.PROJECT, "UPDATED",
                    project.getId(), project.getExternalProjectId(), payload);
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleProjectUpdated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    private void handleProjectDeleted(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isProjectSync()) return;
        Map<String, Object> payload = event.payload();
        String externalProjectId = payload != null ? (String) payload.get("externalProjectId") : null;
        if (externalProjectId != null && !externalProjectId.isBlank()) {
            syncOutboundService.enqueue(SyncEntityType.PROJECT, "DELETED", event.entityId(), externalProjectId, Map.of());
        }
    }

    // ==================== Task Handlers ====================

    private void handleTaskCreated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isProjectSync()) {
            logger.debug("🔇 [SYNC] Project sync disabled — skipping task sync");
            return;
        }
        try {
            Optional<ProjectTask> opt = projectTaskRepository.findByIdWithProject(event.entityId());
            if (opt.isEmpty()) {
                logger.warn("⚠️ [SYNC] Task {} not found for sync", event.entityId());
                return;
            }
            ProjectTask task = opt.get();
            if (task.getExternalTaskId() != null) {
                logger.debug("📋 [SYNC] Task {} already has externalTaskId {} — skipping",
                        task.getId(), task.getExternalTaskId());
                return;
            }
            String externalProjectId = task.getProject() != null ? task.getProject().getExternalProjectId() : null;
            if (externalProjectId == null) {
                logger.warn("⚠️ [SYNC] Task {} parent project has no externalProjectId — skipping", task.getId());
                return;
            }
            Map<String, Object> payload = taskSyncMapper.toOutboundPayload(task, externalProjectId);
            syncOutboundService.syncEntity(SyncEntityType.TASK, "CREATED", task.getId(), null, payload);
            logger.info("📤 [SYNC] Task {} enqueued for external creation", task.getId());
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleTaskCreated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    private void handleTaskUpdated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isProjectSync()) return;
        try {
            Optional<ProjectTask> opt = projectTaskRepository.findByIdWithProject(event.entityId());
            if (opt.isEmpty()) return;
            ProjectTask task = opt.get();
            if (task.getExternalTaskId() == null) {
                logger.debug("📋 [SYNC] Task {} has no externalTaskId — skipping update", task.getId());
                return;
            }
            String externalProjectId = task.getProject() != null ? task.getProject().getExternalProjectId() : null;
            Map<String, Object> payload = taskSyncMapper.toOutboundPayload(task, externalProjectId);
            syncOutboundService.syncEntity(SyncEntityType.TASK, "UPDATED",
                    task.getId(), task.getExternalTaskId(), payload);
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleTaskUpdated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    private void handleTaskDeleted(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isProjectSync()) return;
        Map<String, Object> payload = event.payload();
        String externalTaskId = payload != null ? (String) payload.get("externalTaskId") : null;
        if (externalTaskId != null && !externalTaskId.isBlank()) {
            syncOutboundService.enqueue(SyncEntityType.TASK, "DELETED", event.entityId(), externalTaskId, Map.of());
        }
    }

    // ==================== Ticket Handlers ====================

    private void handleTicketCreated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isTicketSync()) {
            logger.debug("🔇 [SYNC] Ticket sync disabled — skipping");
            return;
        }
        try {
            Optional<Ticket> opt = ticketRepository.findByIdWithCustomer(event.entityId());
            if (opt.isEmpty()) {
                logger.warn("⚠️ [SYNC] Ticket {} not found for sync", event.entityId());
                return;
            }
            Ticket ticket = opt.get();
            if (ticket.getExternalIssueId() != null) {
                logger.debug("📋 [SYNC] Ticket {} already has externalIssueId {} — skipping",
                        ticket.getId(), ticket.getExternalIssueId());
                return;
            }
            String externalCustomerId = ticket.getCustomer() != null ? ticket.getCustomer().getExternalCustomerId() : null;
            Map<String, Object> payload = ticketSyncMapper.toOutboundPayload(ticket, externalCustomerId);
            syncOutboundService.syncEntity(SyncEntityType.ISSUE, "CREATED", ticket.getId(), null, payload);
            logger.info("📤 [SYNC] Ticket {} enqueued for external creation", ticket.getId());
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleTicketCreated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    private void handleTicketUpdated(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isTicketSync()) return;
        try {
            Optional<Ticket> opt = ticketRepository.findByIdWithCustomer(event.entityId());
            if (opt.isEmpty()) return;
            Ticket ticket = opt.get();
            if (ticket.getExternalIssueId() == null) {
                logger.debug("📋 [SYNC] Ticket {} has no externalIssueId — skipping update", ticket.getId());
                return;
            }
            String externalCustomerId = ticket.getCustomer() != null ? ticket.getCustomer().getExternalCustomerId() : null;
            Map<String, Object> payload = ticketSyncMapper.toOutboundPayload(ticket, externalCustomerId);
            syncOutboundService.syncEntity(SyncEntityType.ISSUE, "UPDATED",
                    ticket.getId(), ticket.getExternalIssueId(), payload);
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleTicketUpdated failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    private void handleTicketResolved(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isTicketSync()) return;
        try {
            Optional<Ticket> opt = ticketRepository.findByIdWithCustomer(event.entityId());
            if (opt.isEmpty()) return;
            Ticket ticket = opt.get();
            if (ticket.getExternalIssueId() == null) {
                logger.debug("📋 [SYNC] Ticket {} has no externalIssueId — skipping resolve", ticket.getId());
                return;
            }
            String externalCustomerId = ticket.getCustomer() != null ? ticket.getCustomer().getExternalCustomerId() : null;
            Map<String, Object> payload = ticketSyncMapper.toOutboundPayload(ticket, externalCustomerId);
            syncOutboundService.syncEntity(SyncEntityType.ISSUE, "UPDATED",
                    ticket.getId(), ticket.getExternalIssueId(), payload);
        } catch (Exception e) {
            logger.error("❌ [SYNC] handleTicketResolved failed for {}: {}", event.entityId(), e.getMessage(), e);
        }
    }

    private void handleTicketDeleted(LmpBusinessEvent event) {
        if (!syncProperties.getFeatures().isTicketSync()) return;
        Map<String, Object> payload = event.payload();
        String externalIssueId = payload != null ? (String) payload.get("externalIssueId") : null;
        if (externalIssueId != null && !externalIssueId.isBlank()) {
            syncOutboundService.enqueue(SyncEntityType.ISSUE, "DELETED", event.entityId(), externalIssueId, Map.of());
        }
    }

    /**
     * Auto-provisionne les Items référencés par un devis.
     */
    private void ensureQuotationItemsProvisioned(Quotation quotation) {
        if (quotation.getItems() == null || quotation.getItems().isEmpty()) {
            ensureSingleItemProvisioned(quotation.getTitle(), null);
            return;
        }

        for (QuotationItem item : quotation.getItems()) {
            com.lmp.catalog.domain.Service service = item.getService();
            if (service != null && service.getExternalItemCode() == null) {
                ensureServiceItemProvisioned(service);
            } else if (service == null) {
                // Fallback : item sans service lié → provisionner avec le même item_code
                // que le mapper utilisera (quotation.getTitle())
                ensureSingleItemProvisioned(quotation.getTitle(), null);
            }
        }
    }

    private void logEvent(String description, LmpBusinessEvent event) {
        logger.info("📡 [ERP SYNC] {} | type={} | entityId={}",
                description, event.type(), event.entityId());
    }
}
