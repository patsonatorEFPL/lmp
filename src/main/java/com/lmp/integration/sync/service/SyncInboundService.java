package com.lmp.integration.sync.service;

import com.lmp.auth.repository.UserRepository;
import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceCategory;
import com.lmp.catalog.repository.ServiceCategoryRepository;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.integration.sync.*;
import com.lmp.integration.sync.client.EntityTypeMapping;
import com.lmp.integration.sync.domain.SyncEvent;
import com.lmp.integration.sync.mapper.*;
import com.lmp.integration.sync.repository.SyncEventRepository;
import com.lmp.project.domain.Project;
import com.lmp.project.domain.ProjectTask;
import com.lmp.project.repository.ProjectRepository;
import com.lmp.project.repository.ProjectTaskRepository;
import com.lmp.support.domain.Ticket;
import com.lmp.support.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.sentry.Sentry;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Gère les événements entrants (système externe → LMP).
 * <p>
 * Appelé par {@code InboundWebhookController} après validation HMAC.
 * Route vers les services métier appropriés selon le type d'entité.
 */
@Component
public class SyncInboundService {

    private static final Logger log = LoggerFactory.getLogger(SyncInboundService.class);

    private final SyncEventRepository syncEventRepository;
    private final EntityTypeMapping entityTypeMapping;
    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ItemSyncMapper itemSyncMapper;
    private final ItemGroupSyncMapper itemGroupSyncMapper;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ProjectSyncMapper projectSyncMapper;
    private final TaskSyncMapper taskSyncMapper;
    private final TicketSyncMapper ticketSyncMapper;

    public SyncInboundService(SyncEventRepository syncEventRepository,
                              EntityTypeMapping entityTypeMapping,
                              ServiceRepository serviceRepository,
                              ServiceCategoryRepository serviceCategoryRepository,
                              ItemSyncMapper itemSyncMapper,
                              ItemGroupSyncMapper itemGroupSyncMapper,
                              ProjectRepository projectRepository,
                              ProjectTaskRepository projectTaskRepository,
                              TicketRepository ticketRepository,
                              UserRepository userRepository,
                              ProjectSyncMapper projectSyncMapper,
                              TaskSyncMapper taskSyncMapper,
                              TicketSyncMapper ticketSyncMapper) {
        this.syncEventRepository = syncEventRepository;
        this.entityTypeMapping = entityTypeMapping;
        this.serviceRepository = serviceRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.itemSyncMapper = itemSyncMapper;
        this.itemGroupSyncMapper = itemGroupSyncMapper;
        this.projectRepository = projectRepository;
        this.projectTaskRepository = projectTaskRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.projectSyncMapper = projectSyncMapper;
        this.taskSyncMapper = taskSyncMapper;
        this.ticketSyncMapper = ticketSyncMapper;
    }

    /**
     * Traite un payload entrant depuis un webhook.
     */
    public void processInbound(InboundSyncPayload payload) {
        SyncEntityType entityType = entityTypeMapping.fromExternalDocType(payload.entityType());
        if (entityType == null) {
            log.warn("⚠️ [SYNC INBOUND] Unknown entity type: {} — ignoring", payload.entityType());
            return;
        }

        SyncEvent syncEvent = new SyncEvent();
        syncEvent.setDirection(SyncDirection.INBOUND);
        syncEvent.setEntityType(entityType);
        syncEvent.setExternalEntityId(payload.entityId());
        syncEvent.setEventType(payload.event());
        syncEvent.setStatus(SyncStatus.PENDING);
        syncEventRepository.save(syncEvent);

        try {
            routeToHandler(entityType, payload);
            syncEvent.setStatus(SyncStatus.SUCCESS);
            syncEvent.setProcessedAt(LocalDateTime.now());
            log.info("✅ [SYNC IN] {} {} processed — externalId={}",
                    entityType, payload.event(), payload.entityId());
        } catch (Exception e) {
            syncEvent.setStatus(SyncStatus.FAILED);
            syncEvent.setErrorMessage(e.getMessage());
            log.error("❌ [SYNC IN] Failed to process {} {}: {}",
                    entityType, payload.event(), e.getMessage(), e);

            // Envoi à Sentry — toutes les erreurs de sync inbound avec contexte riche
            try {
                Sentry.withScope(scope -> {
                    scope.setTag("sync.direction", "INBOUND");
                    scope.setTag("sync.entity_type", entityType.name());
                    scope.setTag("sync.event_type", payload.event());
                    scope.setTag("sync.status", "FAILED");
                    scope.setContexts("sync_event", Map.of(
                            "eventId", syncEvent.getId().toString(),
                            "externalEntityId", payload.entityId() != null ? payload.entityId() : "null",
                            "errorMessage", syncEvent.getErrorMessage() != null ? syncEvent.getErrorMessage() : "null"
                    ));
                    Sentry.captureException(e);
                });
            } catch (Exception sentryEx) {
                log.debug("🔇 [SYNC IN] Sentry capture failed: {}", sentryEx.getMessage());
            }
        }

        syncEventRepository.save(syncEvent);
    }

    /**
     * Route le payload vers le handler métier approprié.
     * Les événements "on_trash" (suppression côté externe) sont routés vers les handlers de suppression.
     */
    private void routeToHandler(SyncEntityType entityType, InboundSyncPayload payload) {
        // external CRM émet "on_trash" quand un document est supprimé
        if ("on_trash".equalsIgnoreCase(payload.event())) {
            handleInboundDelete(entityType, payload);
            return;
        }

        switch (entityType) {
            case ITEM -> handleItemSync(payload);
            case ITEM_GROUP -> handleItemGroupSync(payload);
            case CUSTOMER -> handleCustomerSync(payload);
            case PROJECT -> handleProjectSync(payload);
            case TASK -> handleTaskSync(payload);
            case ISSUE -> handleIssueSync(payload);
            case COMMUNICATION -> handleCommunicationSync(payload);
            default -> log.info("📥 [SYNC IN] Entity type {} — handler not yet implemented", entityType);
        }
    }

    /**
     * Gère la suppression d'une entité côté externe → suppression ou désactivation côté LMP.
     * Lookup par external_*_id, puis suppression logique (nullification du lien).
     */
    @Transactional
    protected void handleInboundDelete(SyncEntityType entityType, InboundSyncPayload payload) {
        String externalId = payload.entityId();
        log.info("🗑️ [SYNC IN] Delete {} '{}' from external system", entityType, externalId);

        switch (entityType) {
            case ITEM -> serviceRepository.findByExternalItemCode(externalId).ifPresentOrElse(
                    service -> {
                        service.setExternalItemCode(null);
                        serviceRepository.save(service);
                        log.info("🗑️ [SYNC IN] Unlinked Service '{}' (external Item '{}' deleted)",
                                service.getTitle(), externalId);
                    },
                    () -> log.debug("📋 [SYNC IN] No Service found for external Item '{}' — ignoring", externalId)
            );
            case ITEM_GROUP -> serviceCategoryRepository.findByExternalGroupId(externalId).ifPresentOrElse(
                    category -> {
                        category.setExternalGroupId(null);
                        serviceCategoryRepository.save(category);
                        log.info("🗑️ [SYNC IN] Unlinked Category '{}' (external ItemGroup '{}' deleted)",
                                category.getName(), externalId);
                    },
                    () -> log.debug("📋 [SYNC IN] No Category found for external ItemGroup '{}' — ignoring", externalId)
            );
            case PROJECT -> projectRepository.findByExternalProjectId(externalId).ifPresentOrElse(
                    project -> {
                        project.setExternalProjectId(null);
                        projectRepository.save(project);
                        log.info("🗑️ [SYNC IN] Unlinked Project '{}' (external '{}' deleted)",
                                project.getTitle(), externalId);
                    },
                    () -> log.debug("📋 [SYNC IN] No Project found for external '{}' — ignoring", externalId)
            );
            case TASK -> projectTaskRepository.findByExternalTaskId(externalId).ifPresentOrElse(
                    task -> {
                        task.setExternalTaskId(null);
                        projectTaskRepository.save(task);
                        log.info("🗑️ [SYNC IN] Unlinked Task '{}' (external '{}' deleted)",
                                task.getTitle(), externalId);
                    },
                    () -> log.debug("📋 [SYNC IN] No Task found for external '{}' — ignoring", externalId)
            );
            case ISSUE -> ticketRepository.findByExternalIssueId(externalId).ifPresentOrElse(
                    ticket -> {
                        ticket.setExternalIssueId(null);
                        ticketRepository.save(ticket);
                        log.info("🗑️ [SYNC IN] Unlinked Ticket '{}' (external Issue '{}' deleted)",
                                ticket.getSubject(), externalId);
                    },
                    () -> log.debug("📋 [SYNC IN] No Ticket found for external Issue '{}' — ignoring", externalId)
            );
            case CUSTOMER -> userRepository.findByExternalCustomerId(externalId).ifPresentOrElse(
                    user -> {
                        user.setExternalCustomerId(null);
                        userRepository.save(user);
                        log.info("🗑️ [SYNC IN] Unlinked User '{}' (external Customer '{}' deleted)",
                                user.getEmail(), externalId);
                    },
                    () -> log.debug("📋 [SYNC IN] No User found for external Customer '{}' — ignoring", externalId)
            );
            default -> log.info("📥 [SYNC IN] Delete for {} not implemented — ignoring", entityType);
        }
    }

    // ==================== Phase 3 — Item & ItemGroup Handlers ====================

    /**
     * Upsert un Service LMP à partir d'un Item externe.
     * <p>
     * Stratégie : last-write-wins basé sur le champ {@code updatedAt}.
     * Lookup par {@code external_item_code}.
     */
    @Transactional
    protected void handleItemSync(InboundSyncPayload payload) {
        Map<String, Object> data = payload.data();
        if (data == null || data.isEmpty()) {
            log.warn("⚠️ [SYNC IN] Item payload is empty — ignoring");
            return;
        }

        String externalId = payload.entityId();

        // Lookup existing service by external item code
        Optional<Service> existingOpt = serviceRepository.findByExternalItemCode(externalId);

        if (existingOpt.isPresent()) {
            // UPDATE — merge fields
            Service service = existingOpt.get();
            itemSyncMapper.updateServiceFromPayload(service, data);
            serviceRepository.save(service);
            log.info("🔄 [SYNC IN] Updated Service '{}' from external Item '{}'",
                    service.getTitle(), externalId);
        } else {
            // CREATE — resolve category first
            ServiceCategory category = resolveCategory(data);
            Service service = itemSyncMapper.toNewService(data, category);

            // Avoid slug collision
            ensureUniqueSlug(service);

            serviceRepository.save(service);
            log.info("🆕 [SYNC IN] Created Service '{}' from external Item '{}'",
                    service.getTitle(), externalId);
        }
    }

    /**
     * Upsert une ServiceCategory LMP à partir d'un Item Group externe.
     */
    @Transactional
    protected void handleItemGroupSync(InboundSyncPayload payload) {
        Map<String, Object> data = payload.data();
        if (data == null || data.isEmpty()) {
            log.warn("⚠️ [SYNC IN] ItemGroup payload is empty — ignoring");
            return;
        }

        String externalId = payload.entityId();

        // Lookup existing category by external group id
        Optional<ServiceCategory> existingOpt = serviceCategoryRepository.findByExternalGroupId(externalId);

        if (existingOpt.isPresent()) {
            // UPDATE
            ServiceCategory category = existingOpt.get();
            itemGroupSyncMapper.updateCategoryFromPayload(category, data);
            serviceCategoryRepository.save(category);
            log.info("🔄 [SYNC IN] Updated Category '{}' from external ItemGroup '{}'",
                    category.getName(), externalId);
        } else {
            // CREATE
            ServiceCategory category = itemGroupSyncMapper.toNewCategory(data);

            // Avoid slug collision
            ensureUniqueCategorySlug(category);

            serviceCategoryRepository.save(category);
            log.info("🆕 [SYNC IN] Created Category '{}' from external ItemGroup '{}'",
                    category.getName(), externalId);
        }
    }

    // ==================== Helpers ====================

    /**
     * Résout la catégorie d'un Item depuis le payload.
     * Cherche par external_group_id ou par nom. Fallback sur une catégorie "Général".
     */
    private ServiceCategory resolveCategory(Map<String, Object> data) {
        String itemGroup = (String) data.get("item_group");
        if (itemGroup != null && !itemGroup.isBlank()) {
            // Try by external ID first
            Optional<ServiceCategory> byExternal = serviceCategoryRepository.findByExternalGroupId(itemGroup);
            if (byExternal.isPresent()) return byExternal.get();

            // Try by name
            Optional<ServiceCategory> byName = serviceCategoryRepository.findByName(itemGroup);
            if (byName.isPresent()) return byName.get();
        }

        // Fallback — get or create "Général" category
        return serviceCategoryRepository.findBySlug("general")
                .orElseGet(() -> {
                    ServiceCategory general = new ServiceCategory("Général", "general",
                            "Catégorie par défaut", null, 999);
                    return serviceCategoryRepository.save(general);
                });
    }

    /**
     * S'assure que le slug du Service est unique.
     */
    private void ensureUniqueSlug(Service service) {
        String baseSlug = service.getSlug();
        String slug = baseSlug;
        int counter = 1;
        while (serviceRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter++;
        }
        service.setSlug(slug);
    }

    /**
     * S'assure que le slug de la ServiceCategory est unique.
     */
    private void ensureUniqueCategorySlug(ServiceCategory category) {
        String baseSlug = category.getSlug();
        String slug = baseSlug;
        int counter = 1;
        while (serviceCategoryRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter++;
        }
        category.setSlug(slug);
    }

    // ==================== Phase 4 — Project Handler ====================

    /**
     * Upsert un Project LMP à partir d'un Project externe.
     * Lookup par {@code external_project_id}. Résout le customer par external_customer_id.
     */
    @Transactional
    protected void handleProjectSync(InboundSyncPayload payload) {
        Map<String, Object> data = payload.data();
        if (data == null || data.isEmpty()) {
            log.warn("⚠️ [SYNC IN] Project payload is empty — ignoring");
            return;
        }

        String externalId = payload.entityId();
        Optional<Project> existingOpt = projectRepository.findByExternalProjectId(externalId);

        if (existingOpt.isPresent()) {
            Project project = existingOpt.get();
            projectSyncMapper.updateProjectFromPayload(project, data);
            projectRepository.save(project);
            log.info("🔄 [SYNC IN] Updated Project '{}' from external '{}'", project.getTitle(), externalId);
        } else {
            Project project = projectSyncMapper.toNewProject(data);
            // Résoudre le customer si présent dans le payload
            resolveProjectCustomer(project, data);
            projectRepository.save(project);
            log.info("🆕 [SYNC IN] Created Project '{}' from external '{}'", project.getTitle(), externalId);
        }
    }

    /**
     * Résout le customer d'un Project à partir du payload externe.
     */
    private void resolveProjectCustomer(Project project, Map<String, Object> data) {
        String customerName = (String) data.get("customer");
        if (customerName != null && !customerName.isBlank()) {
            userRepository.findByExternalCustomerId(customerName).ifPresent(project::setCustomer);
        }
    }

    // ==================== Phase 4 — Task Handler ====================

    /**
     * Upsert une ProjectTask LMP à partir d'une Task externe.
     * Résout le projet parent par {@code external_project_id}.
     */
    @Transactional
    protected void handleTaskSync(InboundSyncPayload payload) {
        Map<String, Object> data = payload.data();
        if (data == null || data.isEmpty()) {
            log.warn("⚠️ [SYNC IN] Task payload is empty — ignoring");
            return;
        }

        String externalId = payload.entityId();
        Optional<ProjectTask> existingOpt = projectTaskRepository.findByExternalTaskId(externalId);

        if (existingOpt.isPresent()) {
            ProjectTask task = existingOpt.get();
            taskSyncMapper.updateTaskFromPayload(task, data);
            projectTaskRepository.save(task);
            log.info("🔄 [SYNC IN] Updated Task '{}' from external '{}'", task.getTitle(), externalId);
        } else {
            // Résoudre le projet parent
            String externalProjectId = (String) data.get("project");
            if (externalProjectId == null || externalProjectId.isBlank()) {
                log.warn("⚠️ [SYNC IN] Task '{}' has no project reference — ignoring", externalId);
                return;
            }

            Optional<Project> projectOpt = projectRepository.findByExternalProjectId(externalProjectId);
            if (projectOpt.isEmpty()) {
                log.warn("⚠️ [SYNC IN] Task '{}' references unknown project '{}' — ignoring",
                        externalId, externalProjectId);
                return;
            }

            ProjectTask task = taskSyncMapper.toNewTask(data);
            task.setProject(projectOpt.get());
            projectTaskRepository.save(task);
            log.info("🆕 [SYNC IN] Created Task '{}' for Project '{}' from external '{}'",
                    task.getTitle(), projectOpt.get().getTitle(), externalId);
        }
    }

    // ==================== Phase 4 — Issue/Ticket Handler ====================

    /**
     * Upsert un Ticket LMP à partir d'un Issue externe.
     * Résout le customer par {@code external_customer_id}.
     */
    @Transactional
    protected void handleIssueSync(InboundSyncPayload payload) {
        Map<String, Object> data = payload.data();
        if (data == null || data.isEmpty()) {
            log.warn("⚠️ [SYNC IN] Issue payload is empty — ignoring");
            return;
        }

        String externalId = payload.entityId();
        Optional<Ticket> existingOpt = ticketRepository.findByExternalIssueId(externalId);

        if (existingOpt.isPresent()) {
            Ticket ticket = existingOpt.get();
            ticketSyncMapper.updateTicketFromPayload(ticket, data);
            ticketRepository.save(ticket);
            log.info("🔄 [SYNC IN] Updated Ticket '{}' from external '{}'", ticket.getSubject(), externalId);
        } else {
            Ticket ticket = ticketSyncMapper.toNewTicket(data);
            // Résoudre le customer
            String customerName = (String) data.get("customer");
            if (customerName != null && !customerName.isBlank()) {
                userRepository.findByExternalCustomerId(customerName).ifPresent(ticket::setCustomer);
            }
            ticketRepository.save(ticket);
            log.info("🆕 [SYNC IN] Created Ticket '{}' from external '{}'", ticket.getSubject(), externalId);
        }
    }

    // ==================== Remaining Stubs ====================

    private void handleCustomerSync(InboundSyncPayload payload) {
        log.info("📥 [SYNC IN — STUB] Customer sync: id={}, event={}", payload.entityId(), payload.event());
    }

    private void handleCommunicationSync(InboundSyncPayload payload) {
        log.info("📥 [SYNC IN — STUB] Communication sync: id={}, event={}", payload.entityId(), payload.event());
    }
}
