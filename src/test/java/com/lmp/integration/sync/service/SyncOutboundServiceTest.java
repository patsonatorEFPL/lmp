package com.lmp.integration.sync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncProperties;
import com.lmp.integration.sync.SyncStatus;
import com.lmp.integration.sync.domain.SyncEvent;
import com.lmp.integration.sync.monitoring.SyncErrorClassifier;
import com.lmp.integration.sync.monitoring.SyncMetricsService;
import com.lmp.integration.sync.repository.SyncEventRepository;
import com.lmp.integration.sync.verification.SyncVerificationService;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.shared.config.site.SiteConfigManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du gate runtime ERP sync dans enqueue().
 * Spec : docs/superpowers/specs/2026-05-29-erp-sync-toggle-design.md
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SyncOutboundServiceTest {

    @Mock private ExternalSystemClient externalClient;
    @Mock private SyncEventRepository syncEventRepository;
    @Mock private SyncProperties syncProperties;
    @Mock private SyncCallbackService syncCallbackService;
    @Mock private SyncVerificationService verificationService;
    @Mock private ObjectMapper objectMapper;
    @Mock private SyncErrorClassifier errorClassifier;
    @Mock private SyncMetricsService metricsService;
    @Mock private SiteConfigManager siteConfigManager;

    @InjectMocks private SyncOutboundService service;

    private void stubRetry() {
        SyncProperties.Retry retry = new SyncProperties.Retry();
        when(syncProperties.getRetry()).thenReturn(retry);
    }

    @Test
    void enqueue_runtimeDisabled_skipsEverything() {
        when(syncProperties.isEnabled()).thenReturn(true);
        when(siteConfigManager.getBoolean(SyncOutboundService.RUNTIME_ENABLED_KEY, true)).thenReturn(false);

        service.enqueue(SyncEntityType.CUSTOMER, "CREATED", UUID.randomUUID(), null, Map.of("k", "v"));

        verify(syncEventRepository, never()).save(any());
    }

    @Test
    void enqueue_runtimeEnabled_enqueuesQueued() {
        stubRetry();
        when(syncProperties.isEnabled()).thenReturn(true);
        when(siteConfigManager.getBoolean(SyncOutboundService.RUNTIME_ENABLED_KEY, true)).thenReturn(true);

        UUID id = UUID.randomUUID();
        service.enqueue(SyncEntityType.CUSTOMER, "CREATED", id, null, Map.of("k", "v"));

        ArgumentCaptor<SyncEvent> captor = ArgumentCaptor.forClass(SyncEvent.class);
        verify(syncEventRepository).save(captor.capture());
        assertEquals(SyncStatus.QUEUED, captor.getValue().getStatus());
        assertEquals(id, captor.getValue().getLocalEntityId());
    }

    @Test
    void enqueue_staticDisabled_skipsBeforeRuntimeCheck() {
        when(syncProperties.isEnabled()).thenReturn(false);

        service.enqueue(SyncEntityType.CUSTOMER, "CREATED", UUID.randomUUID(), null, Map.of());

        verify(syncEventRepository, never()).save(any());
        verifyNoInteractions(siteConfigManager);
    }

    @Test
    void syncEntity_aliasGoesThroughGate() {
        when(syncProperties.isEnabled()).thenReturn(true);
        when(siteConfigManager.getBoolean(SyncOutboundService.RUNTIME_ENABLED_KEY, true)).thenReturn(false);

        service.syncEntity(SyncEntityType.CUSTOMER, "CREATED", UUID.randomUUID(), null, Map.of());

        verify(syncEventRepository, never()).save(any());
    }
}
