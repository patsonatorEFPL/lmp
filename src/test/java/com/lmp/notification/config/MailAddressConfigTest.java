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
        ReflectionTestUtils.setField(mailAddressConfig, "noreply", "noreply@lmp-services.ca");
        ReflectionTestUtils.setField(mailAddressConfig, "support", "support@lmp-services.ca");
        ReflectionTestUtils.setField(mailAddressConfig, "replyToSupport", "noreply@lmp-services.ca");
        ReflectionTestUtils.setField(mailAddressConfig, "name", "LMP Digital Services");
    }

    @Test
    void testGetNoreply() {
        assertEquals("noreply@lmp-services.ca", mailAddressConfig.getNoreply());
    }

    @Test
    void testGetSupport() {
        assertEquals("support@lmp-services.ca", mailAddressConfig.getSupport());
    }

    @Test
    void testGetReplyToSupport() {
        assertEquals("noreply@lmp-services.ca", mailAddressConfig.getReplyToSupport());
    }

    @Test
    void testGetName() {
        assertEquals("LMP Digital Services", mailAddressConfig.getName());
    }

    @Test
    void testIsNoReplyAddress() {
        assertTrue(mailAddressConfig.isNoReplyAddress("noreply@lmp-services.ca"));
        assertTrue(mailAddressConfig.isNoReplyAddress("NOREPLY@lmp-services.ca")); // Case insensitive
        assertFalse(mailAddressConfig.isNoReplyAddress("support@lmp-services.ca"));
        assertFalse(mailAddressConfig.isNoReplyAddress("lmp.assistance@gmail.com"));
        assertFalse(mailAddressConfig.isNoReplyAddress(null));
    }

    @Test
    void testIsSupportAddress() {
        assertTrue(mailAddressConfig.isSupportAddress("support@lmp-services.ca"));
        assertTrue(mailAddressConfig.isSupportAddress("SUPPORT@lmp-services.ca")); // Case insensitive
        assertFalse(mailAddressConfig.isSupportAddress("noreply@lmp-services.ca"));
        assertFalse(mailAddressConfig.isSupportAddress("lmp.assistance@gmail.com"));
        assertFalse(mailAddressConfig.isSupportAddress(null));
    }

    @Test
    void testGetAppropriateFromAddress() {
        // Email transactionnel -> noreply
        assertEquals("noreply@lmp-services.ca", 
                    mailAddressConfig.getAppropriateFromAddress(true));
        
        // Email support -> support
        assertEquals("support@lmp-services.ca", 
                    mailAddressConfig.getAppropriateFromAddress(false));
    }

    @Test
    void testGetAppropriateReplyTo() {
        // Email transactionnel -> Reply-To cohérent avec From (noreply)
        assertEquals("noreply@lmp-services.ca", 
                    mailAddressConfig.getAppropriateReplyTo(true));
        
        // Email support -> Reply-To cohérent avec From (support)
        assertEquals("support@lmp-services.ca", 
                    mailAddressConfig.getAppropriateReplyTo(false));
    }

    @Test
    void testToString() {
        String result = mailAddressConfig.toString();
        assertNotNull(result);
        assertTrue(result.contains("noreply@lmp-services.ca"));
        assertTrue(result.contains("support@lmp-services.ca"));
        assertTrue(result.contains("LMP Digital Services"));
    }

    @Test
    void testDefaultValues() {
        // Test avec une instance par défaut
        MailAddressConfig defaultConfig = new MailAddressConfig();
        
        assertEquals("noreply@lmp-services.ca", defaultConfig.getNoreply());
        assertEquals("support@lmp-services.ca", defaultConfig.getSupport());
        assertEquals("noreply@lmp-services.ca", defaultConfig.getReplyToSupport());
        assertEquals("LMP Digital Services", defaultConfig.getName());
    }

    @Test
    void testEmailValidationScenarios() {
        // Test avec différents formats d'email
        String[] validNoReplyEmails = {
            "noreply@lmp-services.ca",
            "NoReply@lmp-services.ca", 
            "NOREPLY@lmp-services.ca"
        };
        
        for (String email : validNoReplyEmails) {
            assertTrue(mailAddressConfig.isNoReplyAddress(email), 
                      "Should validate as noreply: " + email);
        }
        
        String[] validSupportEmails = {
            "support@lmp-services.ca",
            "Support@lmp-services.ca",
            "SUPPORT@lmp-services.ca"
        };
        
        for (String email : validSupportEmails) {
            assertTrue(mailAddressConfig.isSupportAddress(email), 
                      "Should validate as support: " + email);
        }
    }
}
