package com.lmp.notification.service;

import com.lmp.notification.config.MailAddressConfig;
import com.lmp.notification.mail.queue.EmailQueueEvent;
import com.lmp.notification.mail.queue.EmailQueueRepository;
import com.lmp.notification.mail.queue.MailQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Vérifie que NotificationService enqueue les emails via MailQueueService,
 * et que MailAddressConfig est utilisé pour les adresses From.
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

    @Mock
    private EmailQueueRepository emailQueueRepository;

    private MailAddressConfig mailAddressConfig;
    private MailQueueService mailQueueService;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        mailAddressConfig = new MailAddressConfig();
        ReflectionTestUtils.setField(mailAddressConfig, "noreply", "noreply@example.com");
        ReflectionTestUtils.setField(mailAddressConfig, "support", "support@example.com");
        ReflectionTestUtils.setField(mailAddressConfig, "replyToSupport", "noreply@example.com");
        ReflectionTestUtils.setField(mailAddressConfig, "name", "LMP Digital Services");

        when(emailQueueRepository.save(any(EmailQueueEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mailQueueService = new MailQueueService(emailQueueRepository);
        notificationService = new NotificationService(mailSender, templateEngine, mailAddressConfig, mailQueueService);
    }

    @Test
    void sendTestEmailEnqueuesWithNoreplyFrom() {
        notificationService.sendTestEmail("test@example.com");

        ArgumentCaptor<EmailQueueEvent> captor = ArgumentCaptor.forClass(EmailQueueEvent.class);
        verify(emailQueueRepository).save(captor.capture());
        EmailQueueEvent event = captor.getValue();

        assertEquals("noreply@example.com", event.getSender());
        assertEquals("LMP Digital Services", event.getSenderName());
        assertEquals("test@example.com", event.getRecipient());
        assertTrue(event.getSubject().startsWith("Test Email"));
        assertNotNull(event.getBodyHtml());
    }

    @Test
    void testConnectivityReturnsTrueWhenMailSenderCreatesMessage() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        assertTrue(notificationService.testEmailConnectivity());
        verify(mailSender).createMimeMessage();
    }

    @Test
    void testConnectivityReturnsFalseOnException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Connection failed"));
        assertFalse(notificationService.testEmailConnectivity());
    }

    @Test
    void sendTestWelcomeEmailEnqueuesWithRenderedTemplate() {
        when(templateEngine.process(eq("emails/welcome-minimal-clean"), any()))
                .thenReturn("<html>Welcome!</html>");

        notificationService.sendTestWelcomeEmail("welcome@example.com");

        ArgumentCaptor<EmailQueueEvent> captor = ArgumentCaptor.forClass(EmailQueueEvent.class);
        verify(emailQueueRepository).save(captor.capture());
        EmailQueueEvent event = captor.getValue();

        assertEquals("welcome@example.com", event.getRecipient());
        assertEquals("<html>Welcome!</html>", event.getBodyHtml());
        verify(templateEngine).process(eq("emails/welcome-minimal-clean"), any());
    }

    @Test
    void mailAddressConfigStrategyIsCoherent() {
        assertTrue(mailAddressConfig.getAppropriateFromAddress(true).contains("noreply"));
        assertTrue(mailAddressConfig.getAppropriateReplyTo(true).contains("noreply"));
        assertTrue(mailAddressConfig.getAppropriateFromAddress(false).contains("support"));
        assertTrue(mailAddressConfig.getAppropriateReplyTo(false).contains("support"));
    }
}
