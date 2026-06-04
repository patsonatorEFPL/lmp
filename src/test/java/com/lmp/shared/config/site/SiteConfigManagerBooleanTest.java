package com.lmp.shared.config.site;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour SiteConfigManager.getBoolean (fail-open).
 * Partial mock : on stubbe getString, getBoolean appelle la vraie méthode.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SiteConfigManagerBooleanTest {

    private SiteConfigManager partialMock() {
        SiteConfigManager mgr = mock(SiteConfigManager.class);
        when(mgr.getBoolean(anyString(), anyBoolean())).thenCallRealMethod();
        return mgr;
    }

    @Test
    void getBoolean_valueTrue_returnsTrue() {
        SiteConfigManager mgr = partialMock();
        when(mgr.getString("k")).thenReturn("true");
        assertTrue(mgr.getBoolean("k", false));
    }

    @Test
    void getBoolean_valueFalse_returnsFalse() {
        SiteConfigManager mgr = partialMock();
        when(mgr.getString("k")).thenReturn("false");
        assertFalse(mgr.getBoolean("k", true));
    }

    @Test
    void getBoolean_absent_returnsDefault() {
        SiteConfigManager mgr = partialMock();
        when(mgr.getString("k")).thenReturn(null);
        assertTrue(mgr.getBoolean("k", true));
        assertFalse(mgr.getBoolean("k", false));
    }

    @Test
    void getBoolean_readFailure_failOpen_returnsDefault() {
        SiteConfigManager mgr = partialMock();
        when(mgr.getString("k")).thenThrow(new RuntimeException("db down"));
        assertTrue(mgr.getBoolean("k", true));
    }

    @Test
    void getBoolean_whitespaceAndCase_parsed() {
        SiteConfigManager mgr = partialMock();
        when(mgr.getString("k")).thenReturn("  TRUE ");
        assertTrue(mgr.getBoolean("k", false));
    }
}
