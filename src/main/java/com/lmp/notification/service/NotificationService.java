package com.lmp.notification.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.lmp.notification.config.MailAddressConfig;
import com.lmp.notification.mail.queue.MailQueueService;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatus;

/**
 * Service pour l'envoi de notifications automatiques par email.
 * Toutes les notifications passent par {@link MailQueueService} (durable, async, multi-replica safe).
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final String COMPANY_NAME = "LMP Digital Services";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailAddressConfig mailAddressConfig;
    private final MailQueueService mailQueueService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

    public NotificationService(JavaMailSender mailSender,
                               TemplateEngine templateEngine,
                               MailAddressConfig mailAddressConfig,
                               MailQueueService mailQueueService) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailAddressConfig = mailAddressConfig;
        this.mailQueueService = mailQueueService;
    }

    public void sendOrderStatusNotification(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        enqueueOrderNotification(order, "statut",
                () -> buildStatusChangeSubject(order, newStatus),
                () -> buildStatusChangeHtmlContent(order, oldStatus, newStatus));
    }

    public void sendOrderCancellationNotification(Order order, String reason) {
        enqueueOrderNotification(order, "annulation",
                () -> "Annulation de votre commande #" + order.getId(),
                () -> buildCancellationHtmlContent(order, reason));
    }

    public void sendRefundNotification(Order order, String refundAmount, String refundId) {
        enqueueOrderNotification(order, "remboursement",
                () -> "Remboursement traité pour votre commande #" + order.getId(),
                () -> buildRefundHtmlContent(order, refundAmount, refundId));
    }

    public void sendOrderConfirmationNotification(Order order) {
        enqueueOrderNotification(order, "confirmation",
                () -> "Confirmation de votre commande #" + order.getId(),
                () -> buildConfirmationHtmlContent(order));
    }

    public void sendShippingNotification(Order order, String trackingNumber) {
        enqueueOrderNotification(order, "expédition",
                () -> "Votre commande #" + order.getId() + " a été expédiée",
                () -> buildShippingHtmlContent(order, trackingNumber));
    }

    private void enqueueOrderNotification(Order order, String label,
                                          Supplier<String> subject, Supplier<String> htmlContent) {
        if (order.getUser() == null || order.getUser().getEmail() == null) {
            logger.warn("Skip notification {} commande {} — email client manquant", label, order.getId());
            return;
        }
        try {
            mailQueueService.enqueue(
                    mailAddressConfig.getNoreply(),
                    mailAddressConfig.getName(),
                    order.getUser().getEmail(),
                    subject.get(),
                    htmlContent.get());
            logger.info("Notification {} enqueued pour commande {}", label, order.getId());
        } catch (Exception e) {
            logger.error("Erreur enqueue notification {} pour commande {}: {}",
                    label, order.getId(), e.getMessage(), e);
        }
    }

    public void sendAdminNotification(String adminEmail, String subject, String message, Order order) {
        try {
            String htmlContent = buildAdminNotificationContent(subject, message, order);
            mailQueueService.enqueue(
                    mailAddressConfig.getNoreply(),
                    mailAddressConfig.getName(),
                    adminEmail,
                    "[ADMIN] " + subject,
                    htmlContent);
            logger.info("Notification admin enqueued à {} pour commande {}", adminEmail, order.getId());
        } catch (Exception e) {
            logger.error("Erreur enqueue notification admin: {}", e.getMessage(), e);
        }
    }

    public void sendAdminAlert(String adminEmail, String subject, String plainTextBody) {
        try {
            String alertBody = "[ALERTE] " + subject + "\n\n" + plainTextBody
                    + "\n\n— Alerte automatique LMP "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String htmlContent = renderPlainWithFooter("[ALERTE LMP] " + subject, alertBody);

            mailQueueService.enqueue(
                    mailAddressConfig.getNoreply(),
                    mailAddressConfig.getName(),
                    adminEmail,
                    "[ALERTE LMP] " + subject,
                    htmlContent);
            logger.info("Alerte admin enqueued à {} : {}", adminEmail, subject);
        } catch (Exception e) {
            logger.error("Erreur enqueue alerte admin: {}", e.getMessage(), e);
        }
    }

    private String buildStatusChangeSubject(Order order, OrderStatus newStatus) {
        return switch (newStatus) {
            case CONFIRMED   -> "Votre commande #" + order.getId() + " a été confirmée";
            case PROCESSING  -> "Votre commande #" + order.getId() + " est en préparation";
            case SHIPPED     -> "Votre commande #" + order.getId() + " a été expédiée";
            case DELIVERED   -> "Votre commande #" + order.getId() + " a été livrée";
            case CANCELLED   -> "Votre commande #" + order.getId() + " a été annulée";
            case REFUNDED    -> "Votre commande #" + order.getId() + " a été remboursée";
            default          -> "Mise à jour de votre commande #" + order.getId();
        };
    }

    private String buildStatusChangeHtmlContent(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        Context context = baseOrderContext(order);
        context.setVariable("oldStatus", getStatusDisplayName(oldStatus));
        context.setVariable("newStatus", getStatusDisplayName(newStatus));
        return templateEngine.process("emails/order-status-change", context);
    }

    private String buildCancellationHtmlContent(Order order, String reason) {
        Context context = baseOrderContext(order);
        context.setVariable("reason", reason);
        return templateEngine.process("emails/order-cancellation", context);
    }

    private String buildRefundHtmlContent(Order order, String refundAmount, String refundId) {
        Context context = baseOrderContext(order);
        context.setVariable("refundAmount", refundAmount);
        context.setVariable("refundId", refundId);
        return templateEngine.process("emails/order-refund", context);
    }

    private String buildConfirmationHtmlContent(Order order) {
        return templateEngine.process("emails/order-confirmation", baseOrderContext(order));
    }

    private String buildShippingHtmlContent(Order order, String trackingNumber) {
        Context context = baseOrderContext(order);
        context.setVariable("trackingNumber", trackingNumber);
        return templateEngine.process("emails/order-shipping", context);
    }

    private String buildAdminNotificationContent(String subject, String message, Order order) {
        Context context = baseOrderContext(order);
        context.setVariable("subject", subject);
        context.setVariable("message", message);
        return templateEngine.process("emails/admin-notification", context);
    }

    private Context baseOrderContext(Order order) {
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("customerName", getCustomerName(order));
        context.setVariable("companyName", COMPANY_NAME);
        context.setVariable("companyEmail", mailAddressConfig.getSupport());
        context.setVariable("frontendUrl", frontendUrl);
        context.setVariable("currentDate", LocalDateTime.now().format(DATE_FORMAT));
        return context;
    }

    private String getCustomerName(Order order) {
        if (order.getUser() != null) {
            String firstName = order.getUser().getFirstName();
            String lastName = order.getUser().getLastName();
            if (firstName != null && lastName != null) return firstName + " " + lastName;
            if (firstName != null) return firstName;
            if (lastName != null) return lastName;
        }
        return "Cher client";
    }

    private String getStatusDisplayName(OrderStatus status) {
        if (status == null) return "Inconnu";
        return switch (status) {
            case PENDING         -> "En attente";
            case PAYMENT_PENDING -> "Paiement en attente";
            case CONFIRMED       -> "Confirmée";
            case PROCESSING, IN_PROGRESS -> "En préparation";
            case SHIPPED         -> "Expédiée";
            case DELIVERED       -> "Livrée";
            case COMPLETED       -> "Terminée";
            case UNDER_REVIEW    -> "En révision";
            case CANCELLED       -> "Annulée";
            case REFUNDED        -> "Remboursée";
        };
    }

    public boolean testEmailConnectivity() {
        try {
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            logger.error("Test connectivité email échoué: {}", e.getMessage());
            return false;
        }
    }

    /** Test path — enqueues a plain email to verify the queue + active dispatcher are wired. */
    public void sendTestEmail(String toEmail) {
        String subject = "Test Email — " + COMPANY_NAME;
        String content = "Email de test enqueued le "
                + LocalDateTime.now().format(DATE_FORMAT)
                + ". Si vous recevez cet email, la queue et le dispatcher actif fonctionnent.";
        mailQueueService.enqueue(
                mailAddressConfig.getNoreply(),
                mailAddressConfig.getName(),
                toEmail,
                subject,
                renderPlainWithFooter(subject, content));
        logger.info("Test email enqueued à {}", toEmail);
    }

    private String renderPlainWithFooter(String subject, String bodyText) {
        Context ctx = new Context();
        ctx.setVariable("subject", subject);
        ctx.setVariable("bodyText", bodyText);
        ctx.setVariable("companyName", COMPANY_NAME);
        ctx.setVariable("companyEmail", mailAddressConfig.getSupport());
        ctx.setVariable("companyWebsite", frontendUrl);
        return templateEngine.process("emails/plain-with-footer", ctx);
    }

    /** Test path — bienvenue template via queue. */
    public void sendTestWelcomeEmail(String toEmail) {
        Context context = new Context();
        Map<String, Object> testUser = new HashMap<>();
        testUser.put("firstName", "Utilisateur Test");
        testUser.put("email", toEmail);
        context.setVariable("user", testUser);
        context.setVariable("companyName", COMPANY_NAME);
        context.setVariable("frontendUrl", frontendUrl);
        context.setVariable("baseUrl", frontendUrl);

        String htmlContent = templateEngine.process("emails/welcome-minimal-clean", context);
        mailQueueService.enqueue(
                mailAddressConfig.getNoreply(),
                mailAddressConfig.getName(),
                toEmail,
                "🎉 Test Email de Bienvenue — " + COMPANY_NAME,
                htmlContent);
        logger.info("Test welcome email enqueued à {}", toEmail);
    }
}
