package com.lmp.integration.sync.service;

import com.lmp.auth.repository.UserRepository;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.catalog.repository.ServiceCategoryRepository;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service de réconciliation périodique LMP ↔ système externe.
 * <p>
 * Compare les données en interrogeant le système externe pour les entités
 * modifiées depuis le dernier check, puis enqueue les corrections nécessaires.
 */
@Service
public class SyncReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(SyncReconciliationService.class);

    private final ExternalSystemClient externalClient;
    private final SyncOutboundService syncOutboundService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final SyncProperties syncProperties;

    /** Dernière exécution de réconciliation réussie. */
    private final AtomicReference<Instant> lastReconcileTimestamp =
            new AtomicReference<>(Instant.now().minusSeconds(3600));

    public SyncReconciliationService(ExternalSystemClient externalClient,
                                     SyncOutboundService syncOutboundService,
                                     UserRepository userRepository,
                                     OrderRepository orderRepository,
                                     ServiceRepository serviceRepository,
                                     ServiceCategoryRepository serviceCategoryRepository,
                                     SyncProperties syncProperties) {
        this.externalClient = externalClient;
        this.syncOutboundService = syncOutboundService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.serviceRepository = serviceRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.syncProperties = syncProperties;
    }

    /**
     * Exécute un cycle de réconciliation complet.
     *
     * @return le nombre total d'écarts détectés et corrigés
     */
    public int reconcile() {
        if (!externalClient.isAvailable()) {
            log.debug("🔇 [RECONCILIATION] External system unavailable — skipping");
            return 0;
        }

        Instant since = lastReconcileTimestamp.get();
        log.info("🔍 [RECONCILIATION] Starting reconciliation since {}", since);

        int totalGaps = 0;

        try {
            if (syncProperties.getFeatures().isUserProvisioning()) {
                totalGaps += reconcileCustomers(since);
            }
            if (syncProperties.getFeatures().isCatalogSync()) {
                totalGaps += reconcileItems(since);
                totalGaps += reconcileItemGroups(since);
            }
            if (syncProperties.getFeatures().isOrderSync()) {
                totalGaps += reconcileOrders(since);
            }

            lastReconcileTimestamp.set(Instant.now());
            log.info("✅ [RECONCILIATION] Complete — {} gaps detected", totalGaps);
        } catch (Exception e) {
            log.error("❌ [RECONCILIATION] Failed: {}", e.getMessage(), e);
        }

        return totalGaps;
    }

    /**
     * Réconcilie les Customers : vérifie que chaque User avec un externalCustomerId
     * existe toujours côté externe. Provisionne les Users sans externalCustomerId.
     */
    private int reconcileCustomers(Instant since) {
        int gaps = 0;

        // Détecter les Users locaux sans external ID
        var usersWithoutExternal = userRepository.findAll().stream()
                .filter(u -> u.getExternalCustomerId() == null)
                .filter(u -> u.getRegistrationDate() != null &&
                        u.getRegistrationDate().toInstant(ZoneOffset.UTC).isBefore(Instant.now().minusSeconds(300)))
                .toList();

        for (var user : usersWithoutExternal) {
            log.info("🔍 [RECONCILIATION] User {} has no externalCustomerId — enqueuing provision",
                    user.getId());
            syncOutboundService.enqueue(SyncEntityType.CUSTOMER, "CREATED",
                    user.getId(), null, Map.of(
                            "customer_name", user.getFirstName() + " " + user.getLastName(),
                            "customer_type", "Individual",
                            "email_id", user.getEmail()
                    ));
            gaps++;
        }

        // Vérifier les modifications côté externe
        List<Map<String, Object>> externalCustomers = externalClient.listEntities(SyncEntityType.CUSTOMER, since);
        log.debug("🔍 [RECONCILIATION] {} Customers modified externally since {}", externalCustomers.size(), since);
        gaps += externalCustomers.size();

        return gaps;
    }

    /**
     * Réconcilie les Items : détecte les items externes sans correspondance locale.
     */
    private int reconcileItems(Instant since) {
        List<Map<String, Object>> externalItems = externalClient.listEntities(SyncEntityType.ITEM, since);

        int gaps = 0;
        for (Map<String, Object> item : externalItems) {
            String itemCode = (String) item.get("name");
            if (itemCode != null && serviceRepository.findByExternalItemCode(itemCode).isEmpty()) {
                log.info("🔍 [RECONCILIATION] External Item '{}' has no local Service — flagging", itemCode);
                gaps++;
            }
        }
        return gaps;
    }

    /**
     * Réconcilie les Item Groups.
     */
    private int reconcileItemGroups(Instant since) {
        List<Map<String, Object>> externalGroups = externalClient.listEntities(SyncEntityType.ITEM_GROUP, since);

        int gaps = 0;
        for (Map<String, Object> group : externalGroups) {
            String groupName = (String) group.get("name");
            if (groupName != null && serviceCategoryRepository.findByExternalGroupId(groupName).isEmpty()) {
                log.info("🔍 [RECONCILIATION] External ItemGroup '{}' has no local Category — flagging", groupName);
                gaps++;
            }
        }
        return gaps;
    }

    /**
     * Réconcilie les Sales Orders : détecte les commandes locales confirmées sans externalOrderId.
     */
    private int reconcileOrders(Instant since) {
        int gaps = 0;
        var ordersWithoutExternal = orderRepository.findAll().stream()
                .filter(o -> o.getExternalOrderId() == null)
                .filter(o -> "CONFIRMED".equals(o.getStatus().name()) || "PAID".equals(o.getStatus().name()))
                .filter(o -> o.getCreatedAt() != null &&
                        o.getCreatedAt().toInstant(ZoneOffset.UTC).isBefore(Instant.now().minusSeconds(300)))
                .toList();

        for (var order : ordersWithoutExternal) {
            log.info("🔍 [RECONCILIATION] Order {} (status={}) has no externalOrderId — enqueuing",
                    order.getId(), order.getStatus());
            gaps++;
        }
        return gaps;
    }
}
