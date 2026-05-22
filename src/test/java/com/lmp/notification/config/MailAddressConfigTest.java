package com.lmp.notification.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour MailAddressConfig
 * Valide la configuration des adresses email avec routing Cloudflare
 */
class MailAddressConfigTest {

    private MailAddressConfig mailAddressConfig;

    @BeforeEach
    void setUp() {
        mailAddressConfig = new MailAddressConfig();
        
        // Configuration des propriétés comme elles seraient chargées depuis application.properties
        ReflectionTestUtils.setField(mailAddressConfig, "noreply", "noreply@localhost");
        ReflectionTestUtils.setField(mailAddressConfig, "support", "support@localhost");
        ReflectionTestUtils.setField(mailAddressConfig, "replyToSupport", "noreply@localhost");
        ReflectionTestUtils.setField(mailAddressConfig, "name", "LMP Digital Services");
    }

    @Test
    void testGetNoreply() {
        assertEquals("noreply@localhost", mailAddressConfig.getNoreply());
    }

    @Test
    void testGetSupport() {
        assertEquals("support@localhost", mailAddressConfig.getSupport());
    }

    @Test
    void testGetReplyToSupport() {
        assertEquals("noreply@localhost", mailAddressConfig.getReplyToSupport());
    }

    @Test
    void testGetName() {
        assertEquals("LMP Digital Services", mailAddressConfig.getName());
    }

    @Test
    void testIsNoReplyAddress() {
        assertTrue(mailAddressConfig.isNoReplyAddress("noreply@localhost"));
        assertTrue(mailAddressConfig.isNoReplyAddress("NOREPLY@localhost")); // Case insensitive
        assertFalse(mailAddressConfig.isNoReplyAddress("support@localhost"));
        assertFalse(mailAddressConfig.isNoReplyAddress("lmp.assistance@gmail.com"));
        assertFalse(mailAddressConfig.isNoReplyAddress(null));
    }

    @Test
    void testIsSupportAddress() {
        assertTrue(mailAddressConfig.isSupportAddress("support@localhost"));
        assertTrue(mailAddressConfig.isSupportAddress("SUPPORT@localhost")); // Case insensitive
        assertFalse(mailAddressConfig.isSupportAddress("noreply@localhost"));
        assertFalse(mailAddressConfig.isSupportAddress("lmp.assistance@gmail.com"));
        assertFalse(mailAddressConfig.isSupportAddress(null));
    }

    @Test
    void testGetAppropriateFromAddress() {
        // Email transactionnel -> noreply
        assertEquals("noreply@localhost", 
                    mailAddressConfig.getAppropriateFromAddress(true));
        
        // Email support -> support
        assertEquals("support@localhost", 
                    mailAddressConfig.getAppropriateFromAddress(false));
    }

    @Test
    void testGetAppropriateReplyTo() {
        // Email transactionnel -> Reply-To cohérent avec From (noreply)
        assertEquals("noreply@localhost", 
                    mailAddressConfig.getAppropriateReplyTo(true));
        
        // Email support -> Reply-To cohérent avec From (support)
        assertEquals("support@localhost", 
                    mailAddressConfig.getAppropriateReplyTo(false));
    }

    @Test
    void testToString() {
        String result = mailAddressConfig.toString();
        assertNotNull(result);
        assertTrue(result.contains("noreply@localhost"));
        assertTrue(result.contains("support@localhost"));
        assertTrue(result.contains("LMP Digital Services"));
    }

    @Test
    void testDefaultValues() {
        // Test avec une instance par défaut
        MailAddressConfig defaultConfig = new MailAddressConfig();
        
        assertEquals("noreply@localhost", defaultConfig.getNoreply());
        assertEquals("support@localhost", defaultConfig.getSupport());
        assertEquals("noreply@localhost", defaultConfig.getReplyToSupport());
        assertEquals("LMP Digital Services", defaultConfig.getName());
    }

    @Test
    void testEmailValidationScenarios() {
        // Test avec différents formats d'email
        String[] validNoReplyEmails = {
            "noreply@localhost",
            "NoReply@localhost", 
            "NOREPLY@localhost"
        };
        
        for (String email : validNoReplyEmails) {
            assertTrue(mailAddressConfig.isNoReplyAddress(email), 
                      "Should validate as noreply: " + email);
        }
        
        String[] validSupportEmails = {
            "support@localhost",
            "Support@localhost",
            "SUPPORT@localhost"
        };
        
        for (String email : validSupportEmails) {
            assertTrue(mailAddressConfig.isSupportAddress(email), 
                      "Should validate as support: " + email);
        }
    }
}
