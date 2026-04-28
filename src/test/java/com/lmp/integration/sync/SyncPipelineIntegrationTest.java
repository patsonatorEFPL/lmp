package com.lmp.integration.sync;

import com.lmp.integration.sync.client.CircuitBreakerExternalClient;
import com.lmp.integration.sync.domain.SyncEvent;
import com.lmp.integration.sync.repository.SyncEventRepository;
import com.lmp.integration.sync.service.SyncOutboundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test d'intégration du pipeline de synchronisation.
 * <p>
 * Vérifie que {@link SyncOutboundService#enqueue} crée correctement un
 * {@link SyncEvent} en base, et que le processeur de file met à jour le statut
 * après un appel au système externe (mocké).
 */
@SpringBootTest
@ActiveProfiles("test")
class SyncPipelineIntegrationTest {

    @Autowired
    private SyncOutboundService syncOutboundService;

    @Autowired
    private SyncEventRepository syncEventRepository;

    @MockitoBean
    private CircuitBreakerExternalClient externalClient;

    @Test
    void enqueue_shouldCreateQueuedEvent() {
        UUID entityId = UUID.randomUUID();
        syncOutboundService.enqueue(SyncEntityType.CUSTOMER, "CREATED", entityId, null,
                Map.of("customer_name", "Test Customer"));

        SyncEvent event = syncEventRepository.findByLocalEntityIdOrderByCreatedAtDesc(entityId).get(0);
        assertThat(event).isNotNull();
        assertThat(event.getStatus()).isEqualTo(SyncStatus.QUEUED);
        assertThat(event.getDirection()).isEqualTo(SyncDirection.OUTBOUND);
        assertThat(event.getEntityType()).isEqualTo(SyncEntityType.CUSTOMER);
        assertThat(event.getEventType()).isEqualTo("CREATED");
        assertThat(event.getMapperVersion()).isEqualTo("1");
    }

    @Test
    void processEvent_shouldTransitionToSuccess_whenExternalCallSucceeds() {
        when(externalClient.createEntity(any(), any()))
                .thenReturn(ExternalResponse.created("EXT-123", Map.of("name", "EXT-123")));

        UUID entityId = UUID.randomUUID();
        syncOutboundService.enqueue(SyncEntityType.CUSTOMER, "CREATED", entityId, null,
                Map.of("customer_name", "Test Customer"));

        SyncEvent queued = syncEventRepository.findByLocalEntityIdOrderByCreatedAtDesc(entityId).get(0);
        assertThat(queued.getStatus()).isEqualTo(SyncStatus.QUEUED);

        syncOutboundService.processEvent(queued);

        SyncEvent processed = syncEventRepository.findById(queued.getId()).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(processed.getExternalEntityId()).isEqualTo("EXT-123");
        assertThat(processed.getProcessedAt()).isNotNull();
    }

    @Test
    void processEvent_shouldTransitionToFailed_whenExternalCallFails() {
        when(externalClient.createEntity(any(), any()))
                .thenReturn(ExternalResponse.failure("ERP timeout", 504));

        UUID entityId = UUID.randomUUID();
        syncOutboundService.enqueue(SyncEntityType.CUSTOMER, "CREATED", entityId, null,
                Map.of("customer_name", "Test Customer"));

        SyncEvent queued = syncEventRepository.findByLocalEntityIdOrderByCreatedAtDesc(entityId).get(0);
        syncOutboundService.processEvent(queued);

        SyncEvent processed = syncEventRepository.findById(queued.getId()).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(processed.getRetryCount()).isEqualTo(1);
        assertThat(processed.getErrorMessage()).contains("ERP timeout");
    }
}
