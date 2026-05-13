package com.lmp.integration.sync.service;

import com.lmp.auth.domain.User;
import com.lmp.auth.repository.UserRepository;
import com.lmp.billing.domain.InstallmentStatus;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderInstallment;
import com.lmp.billing.repository.OrderInstallmentRepository;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.QuotationRepository;
import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceCategory;
import com.lmp.catalog.repository.ServiceCategoryRepository;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.mapper.AddressSyncMapper;
import com.lmp.integration.sync.mapper.OrderSyncMapper;
import com.lmp.integration.sync.mapper.PaymentSyncMapper;
import com.lmp.project.repository.ProjectRepository;
import com.lmp.project.repository.ProjectTaskRepository;
import com.lmp.support.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Met à jour les entités LMP après une synchronisation réussie avec le système externe.
 * <p>
 * Stocke l'identifiant externe sur l'entité locale pour le lien bidirectionnel.
 */
@Component
public class SyncCallbackService {

    private static final Logger log = LoggerFactory.getLogger(SyncCallbackService.class);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderInstallmentRepository installmentRepository;
    private final QuotationRepository quotationRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final TicketRepository ticketRepository;
    private final ExternalSystemClient externalClient;
    private final OrderSyncMapper orderSyncMapper;
    private final PaymentSyncMapper paymentSyncMapper;
    private final AddressSyncMapper addressSyncMapper;
    private final SyncOutboundService syncOutboundService;

    public SyncCallbackService(UserRepository userRepository,
                               OrderRepository orderRepository,
                               OrderInstallmentRepository installmentRepository,
                               QuotationRepository quotationRepository,
                               ServiceRepository serviceRepository,
                               ServiceCategoryRepository serviceCategoryRepository,
                               ProjectRepository projectRepository,
                               ProjectTaskRepository projectTaskRepository,
                               TicketRepository ticketRepository,
                               ExternalSystemClient externalClient,
                               OrderSyncMapper orderSyncMapper,
                               PaymentSyncMapper paymentSyncMapper,
                               AddressSyncMapper addressSyncMapper,
                               @Lazy SyncOutboundService syncOutboundService) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.installmentRepository = installmentRepository;
        this.quotationRepository = quotationRepository;
        this.serviceRepository = serviceRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.projectRepository = projectRepository;
        this.projectTaskRepository = projectTaskRepository;
        this.ticketRepository = ticketRepository;
        this.externalClient = externalClient;
        this.orderSyncMapper = orderSyncMapper;
        this.paymentSyncMapper = paymentSyncMapper;
        this.addressSyncMapper = addressSyncMapper;
        this.syncOutboundService = syncOutboundService;
    }

    /**
     * Nettoie les identifiants externes après une suppression réussie côté système externe.
     *
     * @param entityType      type d'entité supprimée
     * @param localEntityId   UUID de l'entité locale (peut être le userId lors d'une cascade)
     * @param externalEntityId ID externe supprimé — utilisé comme fallback pour trouver
     *                         l'entité locale quand localEntityId ne correspond pas (ex: cascade user delete)
     */
    @Transactional
    public void onDeleteSuccess(SyncEntityType entityType, UUID localEntityId, String externalEntityId) {
        switch (entityType) {
            case CUSTOMER -> clearUserExternalCustomerId(localEntityId);
            case CONTACT -> clearUserExternalContactId(localEntityId);
            case SALES_ORDER -> clearOrderExternalOrderId(localEntityId, externalEntityId);
            case SALES_INVOICE -> clearOrderExternalInvoiceId(localEntityId, externalEntityId);
            case PAYMENT -> clearOrderExternalPaymentId(localEntityId, externalEntityId);
            case ITEM -> clearServiceExternalItemCode(localEntityId);
            case ITEM_GROUP -> clearCategoryExternalGroupId(localEntityId);
            case PROJECT -> clearProjectExternalId(localEntityId);
            case TASK -> clearTaskExternalId(localEntityId);
            case ISSUE -> clearTicketExternalId(localEntityId);
            case QUOTATION -> clearQuotationExternalId(localEntityId);
            case ADDRESS -> clearUserExternalAddressId(localEntityId);
            case ERP_USER -> clearUserExternalErpUserId(localEntityId);
            default -> log.debug("📋 [CALLBACK] No cleanup needed for {}", entityType);
        }
    }

    /**
     * Met à jour l'entité locale avec l'identifiant externe reçu après sync.
     *
     * @param responseData données retournées par le système externe (peut contenir grand_total, etc.)
     */
    @Transactional
    public void onSyncSuccess(SyncEntityType entityType, UUID localEntityId, String externalId,
                               Map<String, Object> responseData) {
        switch (entityType) {
            case CUSTOMER -> updateUserExternalCustomerId(localEntityId, externalId);
            case CONTACT -> updateUserExternalContactId(localEntityId, externalId);
            case ERP_USER -> updateUserExternalErpUserId(localEntityId, externalId);
            case SALES_ORDER -> updateOrderExternalOrderId(localEntityId, externalId);
            case SALES_INVOICE -> updateOrderExternalInvoiceId(localEntityId, externalId, responseData);
            case PAYMENT -> updateOrderExternalPaymentId(localEntityId, externalId);
            case ITEM -> updateServiceExternalItemCode(localEntityId, externalId);
            case ITEM_GROUP -> updateCategoryExternalGroupId(localEntityId, externalId);
            case PROJECT -> updateProjectExternalId(localEntityId, externalId);
            case TASK -> updateTaskExternalId(localEntityId, externalId);
            case ISSUE -> updateTicketExternalId(localEntityId, externalId);
            case QUOTATION -> updateQuotationExternalId(localEntityId, externalId);
            default -> log.debug("📋 [CALLBACK] No local update needed for {}", entityType);
        }
    }

    private void updateUserExternalCustomerId(UUID userId, String externalId) {
        userRepository.findById(userId).ifPresentOrElse(
                user -> {
                    user.setExternalCustomerId(externalId);
                    userRepository.save(user);
                    log.info("🔗 [CALLBACK] User {} linked to external Customer {}", userId, externalId);

                    // Enchaîner la création de l'Address si le user a une adresse
                    if (addressSyncMapper.hasAddressData(user)) {
                        createAddressForUser(user, externalId);
                    }
                },
                () -> log.warn("⚠️ [CALLBACK] User {} not found for customer link", userId)
        );
    }

    private void createAddressForUser(User user, String customerExternalId) {
        try {
            Map<String, Object> payload = addressSyncMapper.toCreatePayload(user, customerExternalId);
            ExternalResponse response = externalClient.createEntity(SyncEntityType.ADDRESS, payload);
            if (response.success() && response.externalId() != null) {
                user.setExternalAddressId(response.externalId());
                userRepository.save(user);
                log.info("🔗 [CALLBACK] User {} linked to external Address {}", user.getId(), response.externalId());
            } else {
                log.warn("⚠️ [CALLBACK] Failed to create Address for User {}: {}",
                        user.getId(), response.errorMessage());
            }
        } catch (Exception e) {
            log.error("❌ [CALLBACK] Error creating Address for User {}: {}", user.getId(), e.getMessage());
        }
    }

    private void updateUserExternalContactId(UUID userId, String externalId) {
        userRepository.findById(userId).ifPresentOrElse(
                user -> {
                    user.setExternalContactId(externalId);
                    userRepository.save(user);
                    log.info("🔗 [CALLBACK] User {} linked to external Contact {}", userId, externalId);
                },
                () -> log.warn("⚠️ [CALLBACK] User {} not found for contact link", userId)
        );
    }

    /**
     * Pour le DocType externalErp "User", la clé primaire est l'email lui-même.
     * On stocke donc cet email comme externalErpUserId pour pouvoir cibler le bon document
     * lors d'un update / delete ultérieur.
     */
    private void updateUserExternalErpUserId(UUID userId, String externalId) {
        userRepository.findById(userId).ifPresentOrElse(
                user -> {
                    user.setExternalErpUserId(externalId);
                    userRepository.save(user);
                    log.info("🔗 [CALLBACK] User {} linked to external externalErp User {}", userId, externalId);
                },
                () -> log.warn("⚠️ [CALLBACK] User {} not found for externalErp User link", userId)
        );
    }

    private void updateOrderExternalOrderId(UUID orderId, String externalId) {
        orderRepository.findByIdWithUserAndItems(orderId).ifPresentOrElse(
                order -> {
                    order.setExternalOrderId(externalId);
                    orderRepository.save(order);
                    log.info("🔗 [CALLBACK] Order {} linked to external Order {}", orderId, externalId);

                    // Chaîne SO → SINV : maintenant qu'on a l'externalOrderId,
                    // on peut créer la SINV avec le bon lien sales_order sur les items
                    enqueueInvoiceIfNeeded(order);
                },
                () -> log.warn("⚠️ [CALLBACK] Order {} not found for order link", orderId)
        );
    }

    /**
     * Enqueue la création d'une Sales Invoice si la commande n'en a pas encore.
     * Appelé après la création réussie du Sales Order pour que la SINV
     * référence le bon SO dans ses items (champ sales_order).
     */
    @SuppressWarnings("unchecked")
    private void enqueueInvoiceIfNeeded(Order order) {
        if (order.getExternalInvoiceId() != null) {
            log.debug("📋 [CALLBACK] Order {} already has SINV — skipping", order.getId());
            return;
        }

        try {
            Map<String, Object> siPayload = orderSyncMapper.toSalesInvoicePayload(order);

            // Fetch SO item row names from ERP pour renseigner so_detail sur chaque ligne SINV.
            // Sans so_detail, externalErp ne met pas à jour per_billed sur le Sales Order.
            enrichWithSoDetail(siPayload, order.getExternalOrderId());

            syncOutboundService.enqueue(
                    SyncEntityType.SALES_INVOICE, "CREATED",
                    order.getId(), null, siPayload
            );
            log.info("📤 [CALLBACK] Enqueued SINV for Order {} → SO {}", order.getId(), order.getExternalOrderId());
        } catch (Exception e) {
            log.error("❌ [CALLBACK] Failed to enqueue SINV for Order {}: {}",
                    order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Enrichit le payload SINV avec les so_detail (noms des lignes SO)
     * pour que externalErp puisse calculer per_billed sur le Sales Order.
     */
    @SuppressWarnings("unchecked")
    private void enrichWithSoDetail(Map<String, Object> siPayload, String salesOrderId) {
        if (salesOrderId == null) return;
        try {
            ExternalResponse soResponse = externalClient.getEntity(SyncEntityType.SALES_ORDER, salesOrderId);
            if (!soResponse.success() || soResponse.data() == null) {
                log.warn("⚠️ [CALLBACK] Could not fetch SO {} for so_detail", salesOrderId);
                return;
            }
            Map<String, Object> soData = soResponse.data();
            if (soData.containsKey("data") && soData.get("data") instanceof Map) {
                soData = (Map<String, Object>) soData.get("data");
            }
            List<Map<String, Object>> soItems = soData.containsKey("items")
                    ? (List<Map<String, Object>>) soData.get("items") : null;
            if (soItems == null || soItems.isEmpty()) return;

            // Map SO item_code → row name (so_detail)
            Map<String, String> itemCodeToRowName = new java.util.LinkedHashMap<>();
            for (Map<String, Object> soItem : soItems) {
                String itemCode = soItem.get("item_code") != null ? soItem.get("item_code").toString() : null;
                String rowName = soItem.get("name") != null ? soItem.get("name").toString() : null;
                if (itemCode != null && rowName != null) {
                    itemCodeToRowName.put(itemCode, rowName);
                }
            }

            // Apply so_detail to SINV items
            List<Map<String, Object>> sinvItems = siPayload.containsKey("items")
                    ? (List<Map<String, Object>>) siPayload.get("items") : null;
            if (sinvItems == null) return;
            for (Map<String, Object> sinvItem : sinvItems) {
                String itemCode = sinvItem.get("item_code") != null ? sinvItem.get("item_code").toString() : null;
                if (itemCode != null && itemCodeToRowName.containsKey(itemCode)) {
                    sinvItem.put("so_detail", itemCodeToRowName.get(itemCode));
                    log.debug("📋 [CALLBACK] Set so_detail={} for item {}", itemCodeToRowName.get(itemCode), itemCode);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [CALLBACK] Error fetching SO items for so_detail: {}", e.getMessage());
        }
    }

    private void updateOrderExternalInvoiceId(UUID orderId, String externalId,
                                               Map<String, Object> responseData) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    order.setExternalInvoiceId(externalId);
                    orderRepository.save(order);
                    log.info("🔗 [CALLBACK] Order {} linked to external Invoice {}", orderId, externalId);

                    // Extraire le grand_total réel de l'ERP (peut différer de LMP à cause des arrondis TVA)
                    java.math.BigDecimal erpTotal = extractErpGrandTotal(responseData);

                    // Enqueue Payment Entry maintenant qu'on a le nom de la facture
                    enqueuePaymentEntryIfPaid(order, externalId, erpTotal);
                },
                () -> log.warn("⚠️ [CALLBACK] Order {} not found for invoice link", orderId)
        );
    }

    /**
     * Extrait le grand_total depuis la réponse ERP pour éviter les écarts d'arrondi.
     * Retourne null si non disponible — le mapper utilisera order.getTotalAmount() en fallback.
     */
    @SuppressWarnings("unchecked")
    private java.math.BigDecimal extractErpGrandTotal(Map<String, Object> responseData) {
        if (responseData == null) return null;
        // externalErp API returns {"data": {"grand_total": ...}} — navigate the nested structure
        Map<String, Object> dataMap = responseData;
        if (dataMap.containsKey("data") && dataMap.get("data") instanceof Map) {
            dataMap = (Map<String, Object>) dataMap.get("data");
        }
        Object grandTotal = dataMap.get("grand_total");
        if (grandTotal == null) return null;
        try {
            java.math.BigDecimal total = new java.math.BigDecimal(grandTotal.toString());
            log.info("📋 [CALLBACK] Extracted ERP grand_total={}", total);
            return total;
        } catch (NumberFormatException e) {
            log.warn("⚠️ [CALLBACK] Cannot parse grand_total '{}': {}", grandTotal, e.getMessage());
            return null;
        }
    }

    /**
     * Enqueue un ou plusieurs Payment Entry (type Receive) après la création de la SINV.
     * <p>
     * Deux cas :
     * <ul>
     *   <li>Paiement unique : 1 PE pour le montant total (comportement existant)</li>
     *   <li>Paiement en plusieurs fois : 1 PE par échéance payée (status=PAID),
     *       avec {@code payment_term} sur la référence SINV pour que externalErp mette à jour
     *       le bon slot du {@code payment_schedule}</li>
     * </ul>
     *
     * @param erpTotal montant grand_total réel de la facture ERP (pour éviter les écarts d'arrondi)
     */
    private void enqueuePaymentEntryIfPaid(Order order, String salesInvoiceId,
                                            java.math.BigDecimal erpTotal) {
        // Cas installment : créer un PE par échéance payée
        if (order.isInstallmentOrder()) {
            enqueueInstallmentPaymentEntries(order, salesInvoiceId);
            return;
        }

        // Cas paiement unique (existant)
        if (order.getExternalPaymentId() != null) {
            log.debug("📋 [CALLBACK] Order {} already has Payment Entry — skipping", order.getId());
            return;
        }
        if (order.getPaidAt() == null) {
            log.debug("📋 [CALLBACK] Order {} not paid — skipping Payment Entry", order.getId());
            return;
        }

        try {
            Map<String, Object> paymentPayload = paymentSyncMapper.toPaymentEntryPayload(order, salesInvoiceId, erpTotal);
            syncOutboundService.enqueue(
                    SyncEntityType.PAYMENT, "CREATED",
                    order.getId(), null, paymentPayload
            );
            log.info("💳 [CALLBACK] Enqueued Payment Entry for Order {} → SINV {}", order.getId(), salesInvoiceId);
        } catch (Exception e) {
            log.error("❌ [CALLBACK] Failed to enqueue Payment Entry for Order {}: {}",
                    order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Enqueue un Payment Entry pour chaque échéance payée d'une commande en plusieurs fois.
     * <p>
     * Récupère le {@code payment_schedule} de la SINV pour utiliser les montants exacts
     * calculés par externalErp (évite les écarts d'arrondi entre LMP et l'ERP).
     */
    @SuppressWarnings("unchecked")
    private void enqueueInstallmentPaymentEntries(Order order, String salesInvoiceId) {
        List<OrderInstallment> installments = installmentRepository
                .findByOrderIdOrderByInstallmentNumberAsc(order.getId());

        if (installments.isEmpty()) {
            log.warn("⚠️ [CALLBACK] Installment order {} has no installments — skipping PE", order.getId());
            return;
        }

        // Récupérer le payment_schedule réel de l'ERP pour les montants exacts
        Map<String, java.math.BigDecimal> erpAmountByTerm = fetchErpPaymentScheduleAmounts(salesInvoiceId);

        for (OrderInstallment inst : installments) {
            if (inst.getStatus() != InstallmentStatus.PAID) {
                log.debug("📋 [CALLBACK] Installment {}/{} not yet paid — skipping",
                        inst.getInstallmentNumber(), order.getInstallmentCount());
                continue;
            }
            if (inst.getExternalPaymentId() != null) {
                log.debug("📋 [CALLBACK] Installment {}/{} already has PE {} — skipping",
                        inst.getInstallmentNumber(), order.getInstallmentCount(), inst.getExternalPaymentId());
                continue;
            }

            try {
                // Utiliser le montant ERP si disponible (évite les écarts d'arrondi)
                java.math.BigDecimal erpAmount = erpAmountByTerm.get(inst.getPaymentTerm());
                Map<String, Object> pePayload = paymentSyncMapper.toInstallmentPaymentEntryPayload(
                        order, inst, salesInvoiceId, erpAmount);
                syncOutboundService.enqueue(
                        SyncEntityType.PAYMENT, "CREATED",
                        order.getId(), null, pePayload
                );
                log.info("💳 [CALLBACK] Enqueued PE for installment {}/{} → SINV {} (term={}, erpAmount={})",
                        inst.getInstallmentNumber(), order.getInstallmentCount(),
                        salesInvoiceId, inst.getPaymentTerm(), erpAmount);
            } catch (Exception e) {
                log.error("❌ [CALLBACK] Failed to enqueue PE for installment {}/{}: {}",
                        inst.getInstallmentNumber(), order.getInstallmentCount(), e.getMessage(), e);
            }
        }
    }

    /**
     * Récupère les montants du payment_schedule depuis la SINV ERP.
     * Retourne un map payment_term → payment_amount (montant exact calculé par externalErp).
     */
    @SuppressWarnings("unchecked")
    private Map<String, java.math.BigDecimal> fetchErpPaymentScheduleAmounts(String salesInvoiceId) {
        Map<String, java.math.BigDecimal> result = new java.util.LinkedHashMap<>();
        if (salesInvoiceId == null) return result;

        try {
            ExternalResponse response = externalClient.getEntity(SyncEntityType.SALES_INVOICE, salesInvoiceId);
            if (!response.success() || response.data() == null) return result;

            Map<String, Object> data = response.data();
            if (data.containsKey("data") && data.get("data") instanceof Map) {
                data = (Map<String, Object>) data.get("data");
            }

            List<Map<String, Object>> schedule = data.containsKey("payment_schedule")
                    ? (List<Map<String, Object>>) data.get("payment_schedule") : null;
            if (schedule == null) return result;

            for (Map<String, Object> row : schedule) {
                String term = row.get("payment_term") != null ? row.get("payment_term").toString() : null;
                Object amountObj = row.get("payment_amount");
                if (term != null && amountObj != null) {
                    result.put(term, new java.math.BigDecimal(amountObj.toString()));
                    log.debug("📋 [CALLBACK] ERP payment_schedule: term={}, amount={}", term, amountObj);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [CALLBACK] Error fetching payment_schedule for {}: {}", salesInvoiceId, e.getMessage());
        }
        return result;
    }

    private void updateOrderExternalPaymentId(UUID orderId, String externalId) {
        orderRepository.findById(orderId).ifPresentOrElse(
                order -> {
                    if (order.isInstallmentOrder()) {
                        // Pour les installments, stocker l'external ID sur l'échéance
                        // Le PE externe contient le payment_term dans son payload
                        updateInstallmentExternalPaymentId(order, externalId);
                    } else {
                        order.setExternalPaymentId(externalId);
                        orderRepository.save(order);
                    }
                    log.info("🔗 [CALLBACK] Order {} linked to external Payment {}", orderId, externalId);
                },
                () -> log.warn("⚠️ [CALLBACK] Order {} not found for payment link", orderId)
        );
    }

    /**
     * Stocke l'external payment ID sur la bonne échéance.
     * Cherche la première échéance PAID sans external ID.
     */
    private void updateInstallmentExternalPaymentId(Order order, String externalPaymentId) {
        List<OrderInstallment> installments = installmentRepository
                .findByOrderIdOrderByInstallmentNumberAsc(order.getId());

        for (OrderInstallment inst : installments) {
            if (inst.getStatus() == InstallmentStatus.PAID && inst.getExternalPaymentId() == null) {
                inst.setExternalPaymentId(externalPaymentId);
                installmentRepository.save(inst);
                log.info("🔗 [CALLBACK] Installment {}/{} linked to PE {}",
                        inst.getInstallmentNumber(), order.getInstallmentCount(), externalPaymentId);

                // Vérifier si toutes les échéances sont payées ET synchronisées
                long pendingSync = installments.stream()
                        .filter(i -> i.getStatus() == InstallmentStatus.PAID && i.getExternalPaymentId() == null)
                        .count();
                if (pendingSync == 0) {
                    // Toutes les échéances payées sont synchronisées — marquer l'order aussi
                    order.setExternalPaymentId(externalPaymentId + " (installments)");
                    orderRepository.save(order);
                    log.info("✅ [CALLBACK] All installments synced for Order {}", order.getId());
                }
                return;
            }
        }
        log.warn("⚠️ [CALLBACK] No unpaid installment found for PE {} on Order {}",
                externalPaymentId, order.getId());
    }

    private void updateServiceExternalItemCode(UUID serviceId, String externalId) {
        serviceRepository.findById(serviceId).ifPresentOrElse(
                service -> {
                    service.setExternalItemCode(externalId);
                    serviceRepository.save(service);
                    log.info("🔗 [CALLBACK] Service {} linked to external Item {}", serviceId, externalId);
                },
                () -> log.warn("⚠️ [CALLBACK] Service {} not found for item link", serviceId)
        );
    }

    private void updateCategoryExternalGroupId(UUID categoryId, String externalId) {
        serviceCategoryRepository.findById(categoryId).ifPresentOrElse(
                category -> {
                    category.setExternalGroupId(externalId);
                    serviceCategoryRepository.save(category);
                    log.info("🔗 [CALLBACK] Category {} linked to external Group {}", categoryId, externalId);
                },
                () -> log.warn("⚠️ [CALLBACK] Category {} not found for group link", categoryId)
        );
    }

    private void updateProjectExternalId(UUID projectId, String externalId) {
        projectRepository.findById(projectId).ifPresentOrElse(
                project -> {
                    project.setExternalProjectId(externalId);
                    projectRepository.save(project);
                    log.info("🔗 [CALLBACK] Project {} linked to external Project {}", projectId, externalId);
                },
                () -> log.warn("⚠️ [CALLBACK] Project {} not found for project link", projectId)
        );
    }

    private void updateTaskExternalId(UUID taskId, String externalId) {
        projectTaskRepository.findById(taskId).ifPresentOrElse(
                task -> {
                    task.setExternalTaskId(externalId);
                    projectTaskRepository.save(task);
                    log.info("🔗 [CALLBACK] Task {} linked to external Task {}", taskId, externalId);
                },
                () -> log.warn("⚠️ [CALLBACK] Task {} not found for task link", taskId)
        );
    }

    private void updateTicketExternalId(UUID ticketId, String externalId) {
        ticketRepository.findById(ticketId).ifPresentOrElse(
                ticket -> {
                    ticket.setExternalIssueId(externalId);
                    ticketRepository.save(ticket);
                    log.info("🔗 [CALLBACK] Ticket {} linked to external Issue {}", ticketId, externalId);
                },
                () -> log.warn("⚠️ [CALLBACK] Ticket {} not found for issue link", ticketId)
        );
    }

    // ==================== Clear external IDs after delete ====================

    private void clearUserExternalCustomerId(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setExternalCustomerId(null);
            userRepository.save(user);
            log.info("🧹 [CALLBACK] Cleared external Customer ID on User {}", userId);
        });
    }

    private void clearUserExternalContactId(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setExternalContactId(null);
            userRepository.save(user);
            log.info("🧹 [CALLBACK] Cleared external Contact ID on User {}", userId);
        });
    }

    private void clearUserExternalAddressId(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setExternalAddressId(null);
            userRepository.save(user);
            log.info("🧹 [CALLBACK] Cleared external Address ID on User {}", userId);
        });
    }

    private void clearUserExternalErpUserId(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setExternalErpUserId(null);
            userRepository.save(user);
            log.info("🧹 [CALLBACK] Cleared external externalErp User ID on User {}", userId);
        });
    }

    private void clearOrderExternalOrderId(UUID localEntityId, String externalOrderId) {
        // Essayer d'abord par localEntityId (cas normal : l'order lui-même est supprimé)
        Optional<Order> orderOpt = orderRepository.findById(localEntityId);
        // Fallback : chercher par externalOrderId (cas cascade : localEntityId = userId)
        if (orderOpt.isEmpty() && externalOrderId != null && !externalOrderId.isBlank()) {
            orderOpt = orderRepository.findByExternalOrderId(externalOrderId);
        }
        orderOpt.ifPresent(order -> {
            order.setExternalOrderId(null);
            orderRepository.save(order);
            log.info("🧹 [CALLBACK] Cleared external Order ID '{}' on Order {}", externalOrderId, order.getId());
        });
    }

    private void clearOrderExternalInvoiceId(UUID localEntityId, String externalInvoiceId) {
        // Essayer d'abord par localEntityId (cas normal)
        Optional<Order> orderOpt = orderRepository.findById(localEntityId);
        // Fallback : chercher par externalInvoiceId (cas cascade)
        if (orderOpt.isEmpty() && externalInvoiceId != null && !externalInvoiceId.isBlank()) {
            orderOpt = orderRepository.findByExternalInvoiceId(externalInvoiceId);
        }
        orderOpt.ifPresent(order -> {
            order.setExternalInvoiceId(null);
            orderRepository.save(order);
            log.info("🧹 [CALLBACK] Cleared external Invoice ID '{}' on Order {}", externalInvoiceId, order.getId());
        });
    }

    private void clearOrderExternalPaymentId(UUID localEntityId, String externalPaymentId) {
        Optional<Order> orderOpt = orderRepository.findById(localEntityId);
        if (orderOpt.isEmpty() && externalPaymentId != null && !externalPaymentId.isBlank()) {
            orderOpt = orderRepository.findByExternalPaymentId(externalPaymentId);
        }
        orderOpt.ifPresent(order -> {
            order.setExternalPaymentId(null);
            orderRepository.save(order);
            log.info("🧹 [CALLBACK] Cleared external Payment ID '{}' on Order {}", externalPaymentId, order.getId());
        });
    }

    private void clearServiceExternalItemCode(UUID serviceId) {
        serviceRepository.findById(serviceId).ifPresent(service -> {
            service.setExternalItemCode(null);
            serviceRepository.save(service);
            log.info("🧹 [CALLBACK] Cleared external Item Code on Service {}", serviceId);
        });
    }

    private void clearCategoryExternalGroupId(UUID categoryId) {
        serviceCategoryRepository.findById(categoryId).ifPresent(category -> {
            category.setExternalGroupId(null);
            serviceCategoryRepository.save(category);
            log.info("🧹 [CALLBACK] Cleared external Group ID on Category {}", categoryId);
        });
    }

    private void clearProjectExternalId(UUID projectId) {
        projectRepository.findById(projectId).ifPresent(project -> {
            project.setExternalProjectId(null);
            projectRepository.save(project);
            log.info("🧹 [CALLBACK] Cleared external Project ID on Project {}", projectId);
        });
    }

    private void clearTaskExternalId(UUID taskId) {
        projectTaskRepository.findById(taskId).ifPresent(task -> {
            task.setExternalTaskId(null);
            projectTaskRepository.save(task);
            log.info("🧹 [CALLBACK] Cleared external Task ID on Task {}", taskId);
        });
    }

    private void clearTicketExternalId(UUID ticketId) {
        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            ticket.setExternalIssueId(null);
            ticketRepository.save(ticket);
            log.info("🧹 [CALLBACK] Cleared external Issue ID on Ticket {}", ticketId);
        });
    }

    // ==================== Quotation ====================

    private void updateQuotationExternalId(UUID quotationId, String externalId) {
        quotationRepository.findById(quotationId).ifPresentOrElse(
                quotation -> {
                    quotation.setExternalQuotationId(externalId);
                    quotationRepository.save(quotation);
                    log.info("🔗 [CALLBACK] Quotation {} linked to external Quotation {}", quotationId, externalId);
                },
                () -> log.warn("⚠️ [CALLBACK] Quotation {} not found for quotation link", quotationId)
        );
    }

    private void clearQuotationExternalId(UUID quotationId) {
        quotationRepository.findById(quotationId).ifPresent(quotation -> {
            quotation.setExternalQuotationId(null);
            quotationRepository.save(quotation);
            log.info("🧹 [CALLBACK] Cleared external Quotation ID on Quotation {}", quotationId);
        });
    }
}
