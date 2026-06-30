package com.lmp.portal.admin;

import com.lmp.integration.sync.SyncProperties;
import com.lmp.integration.sync.service.SyncOutboundService;
import com.lmp.shared.config.site.SiteConfigManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ErpSyncAdminController (toggle runtime ERP sync).
 */
@ExtendWith(MockitoExtension.class)
class ErpSyncAdminControllerTest {

    @Mock private SiteConfigManager siteConfigManager;
    @Mock private SyncProperties syncProperties;
    @Mock private Authentication authentication;

    @InjectMocks private ErpSyncAdminController controller;

    @Test
    void get_keyAbsent_enabledByDefault_sourceDefault() {
        when(syncProperties.isEnabled()).thenReturn(true);
        when(siteConfigManager.getBoolean(SyncOutboundService.RUNTIME_ENABLED_KEY, true)).thenReturn(true);
        when(siteConfigManager.getString(SyncOutboundService.RUNTIME_ENABLED_KEY)).thenReturn(null);

        var response = controller.get();

        assertEquals(200, response.getStatusCode().value());
        var status = response.getBody().data();
        assertTrue(status.enabled());
        assertTrue(status.staticEnabled());
        assertEquals("default", status.source());
    }

    @Test
    void put_disable_updatesSiteConfigAndReturnsNewState() {
        when(syncProperties.isEnabled()).thenReturn(true);
        when(siteConfigManager.getBoolean(SyncOutboundService.RUNTIME_ENABLED_KEY, true)).thenReturn(false);
        when(siteConfigManager.getString(SyncOutboundService.RUNTIME_ENABLED_KEY)).thenReturn("false");
        when(authentication.getName()).thenReturn("admin@lmp.ca");

        var response = controller.update(new ErpSyncAdminController.UpdateErpSyncRequest(false), authentication);

        verify(siteConfigManager).update(SyncOutboundService.RUNTIME_ENABLED_KEY, "false", "ERP sync runtime toggle");
        assertEquals(200, response.getStatusCode().value());
        assertFalse(response.getBody().data().enabled());
        assertEquals("config", response.getBody().data().source());
    }

    @Test
    void put_nullEnabled_returns400_noUpdate() {
        var response = controller.update(new ErpSyncAdminController.UpdateErpSyncRequest(null), authentication);

        assertEquals(400, response.getStatusCode().value());
        verify(siteConfigManager, never()).update(any(), any(), any());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
    }
}
