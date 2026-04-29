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
        ReflectionTestUtils.setField(mailAddressConfig, "noreply", "noreply@example.com");
        ReflectionTestUtils.setField(mailAddressConfig, "support", "support@example.com");
        ReflectionTestUtils.setField(mailAddressConfig, "replyToSupport", "noreply@example.com");
        ReflectionTestUtils.setField(mailAddressConfig, "name", "LMP Digital Services");
    }

    @Test
    void testGetNoreply() {
        assertEquals("noreply@example.com", mailAddressConfig.getNoreply());
    }

    @Test
    void testGetSupport() {
        assertEquals("support@example.com", mailAddressConfig.getSupport());
    }

    @Test
    void testGetReplyToSupport() {
        assertEquals("noreply@example.com", mailAddressConfig.getReplyToSupport());
    }

    @Test
    void testGetName() {
        assertEquals("LMP Digital Services", mailAddressConfig.getName());
    }

    @Test
    void testIsNoReplyAddress() {
        assertTrue(mailAddressConfig.isNoReplyAddress("noreply@example.com"));
        assertTrue(mailAddressConfig.isNoReplyAddress("NOREPLY@example.com")); // Case insensitive
        assertFalse(mailAddressConfig.isNoReplyAddress("support@example.com"));
        assertFalse(mailAddressConfig.isNoReplyAddress("lmp.assistance@gmail.com"));
        assertFalse(mailAddressConfig.isNoReplyAddress(null));
    }

    @Test
    void testIsSupportAddress() {
        assertTrue(mailAddressConfig.isSupportAddress("support@example.com"));
        assertTrue(mailAddressConfig.isSupportAddress("SUPPORT@example.com")); // Case insensitive
        assertFalse(mailAddressConfig.isSupportAddress("noreply@example.com"));
        assertFalse(mailAddressConfig.isSupportAddress("lmp.assistance@gmail.com"));
        assertFalse(mailAddressConfig.isSupportAddress(null));
    }

    @Test
    void testGetAppropriateFromAddress() {
        // Email transactionnel -> noreply
        assertEquals("noreply@example.com", 
                    mailAddressConfig.getAppropriateFromAddress(true));
        
        // Email support -> support
        assertEquals("support@example.com", 
                    mailAddressConfig.getAppropriateFromAddress(false));
    }

    @Test
    void testGetAppropriateReplyTo() {
        // Email transactionnel -> Reply-To cohérent avec From (noreply)
        assertEquals("noreply@example.com", 
                    mailAddressConfig.getAppropriateReplyTo(true));
        
        // Email support -> Reply-To cohérent avec From (support)
        assertEquals("support@example.com", 
                    mailAddressConfig.getAppropriateReplyTo(false));
    }

    @Test
    void testToString() {
        String result = mailAddressConfig.toString();
        assertNotNull(result);
        assertTrue(result.contains("noreply@example.com"));
        assertTrue(result.contains("support@example.com"));
        assertTrue(result.contains("LMP Digital Services"));
    }

    @Test
    void testDefaultValues() {
        // Test avec une instance par défaut
        MailAddressConfig defaultConfig = new MailAddressConfig();
        
        assertEquals("noreply@example.com", defaultConfig.getNoreply());
        assertEquals("support@example.com", defaultConfig.getSupport());
        assertEquals("noreply@example.com", defaultConfig.getReplyToSupport());
        assertEquals("LMP Digital Services", defaultConfig.getName());
    }

    @Test
    void testEmailValidationScenarios() {
        // Test avec différents formats d'email
        String[] validNoReplyEmails = {
            "noreply@example.com",
            "NoReply@example.com", 
            "NOREPLY@example.com"
        };
        
        for (String email : validNoReplyEmails) {
            assertTrue(mailAddressConfig.isNoReplyAddress(email), 
                      "Should validate as noreply: " + email);
        }
        
        String[] validSupportEmails = {
            "support@example.com",
            "Support@example.com",
            "SUPPORT@example.com"
        };
        
        for (String email : validSupportEmails) {
            assertTrue(mailAddressConfig.isSupportAddress(email), 
                      "Should validate as support: " + email);
        }
    }
}
