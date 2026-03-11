package com.lmp.notification.service;

import com.lmp.notification.config.MailAddressConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests d'intégration pour NotificationService
 * Valide que les headers From et Reply-To sont correctement configurés
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceIntegrationTest {

    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private TemplateEngine templateEngine;
    
    @Mock
    private MimeMessage mimeMessage;
    
    private MailAddressConfig mailAddressConfig;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Configuration du MailAddressConfig
        mailAddressConfig = new MailAddressConfig();
        ReflectionTestUtils.setField(mailAddressConfig, "noreply", "noreply@lmp-services.ca");
        ReflectionTestUtils.setField(mailAddressConfig, "support", "support@lmp-services.ca");
        ReflectionTestUtils.setField(mailAddressConfig, "replyToSupport", "noreply@lmp-services.ca");
        ReflectionTestUtils.setField(mailAddressConfig, "name", "LMP Digital Services");
        
        // Création du service avec constructor injection
        notificationService = new NotificationService(mailSender, templateEngine, mailAddressConfig);
        ReflectionTestUtils.setField(notificationService, "mailHost", "smtp.gmail.com");
        ReflectionTestUtils.setField(notificationService, "mailUsername", "lmp.assistance@gmail.com");
        ReflectionTestUtils.setField(notificationService, "mailPassword", "****");
    }

    @Test
    void testSendTestEmailUsesCorrectAddresses() throws Exception {
        // Given
        String testEmail = "test@example.com";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // When
        notificationService.sendTestEmail(testEmail);
        
        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        
        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals("noreply@lmp-services.ca", capturedMessage.getFrom());
        assertEquals("noreply@lmp-services.ca", capturedMessage.getReplyTo());
        assertArrayEquals(new String[]{testEmail}, capturedMessage.getTo());
    }

    @Test
    void testEmailConfigurationLogging() throws Exception {
        // Given
        String testEmail = "test@example.com";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // When
        notificationService.sendTestEmail(testEmail);
        
        // Then - Vérifier que les logs montrent les bonnes adresses
        // (Les logs sont vérifiés par observation dans les tests manuels)
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test 
    void testConnectivityTest() {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        // When
        boolean result = notificationService.testEmailConnectivity();
        
        // Then
        assertTrue(result);
        verify(mailSender).createMimeMessage();
    }

    @Test
    void testConnectivityTestFailure() {
        // Given
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Connection failed"));
        
        // When
        boolean result = notificationService.testEmailConnectivity();
        
        // Then
        assertFalse(result);
    }

    @Test
    void testSendTestWelcomeEmailConfiguration() throws Exception {
        // Given
        String testEmail = "welcome@example.com";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("emails/welcome-minimal-clean"), any())).thenReturn("<html>Welcome!</html>");
        
        // When
        notificationService.sendTestWelcomeEmail(testEmail);
        
        // Then
        verify(mailSender).send(any(MimeMessage.class));
        verify(templateEngine).process(eq("emails/welcome-minimal-clean"), any());
    }

    @Test
    void testMailAddressConfigIntegration() {
        // Test que MailAddressConfig est correctement injecté et utilisé
        assertNotNull(ReflectionTestUtils.getField(notificationService, "mailAddressConfig"));
        
        assertEquals("noreply@lmp-services.ca", mailAddressConfig.getNoreply());
        assertEquals("support@lmp-services.ca", mailAddressConfig.getSupport());
        assertEquals("noreply@lmp-services.ca", mailAddressConfig.getReplyToSupport());
    }

    @Test
    void testEmailAddressStrategy() {
        // Vérifier que la stratégie d'adresses est cohérente
        
        // Emails transactionnels = noreply + reply-to cohérent (noreply)
        assertTrue(mailAddressConfig.getAppropriateFromAddress(true).contains("noreply"));
        assertNotNull(mailAddressConfig.getAppropriateReplyTo(true));
        assertTrue(mailAddressConfig.getAppropriateReplyTo(true).contains("noreply"));
        
        // Emails support = support + reply-to cohérent (support)
        assertTrue(mailAddressConfig.getAppropriateFromAddress(false).contains("support"));
        assertNotNull(mailAddressConfig.getAppropriateReplyTo(false));
        assertTrue(mailAddressConfig.getAppropriateReplyTo(false).contains("support"));
    }
}
