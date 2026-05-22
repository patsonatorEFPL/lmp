package com.lmp.notification.mail.dispatch;

import com.lmp.notification.mail.queue.EmailQueueEvent;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envoi direct via {@link JavaMailSender} (SMTP). Comportement legacy LMP.
 * Activé par défaut quand {@code lmp.mail.dispatcher} vaut {@code smtp} ou est absent.
 */
@Component
@ConditionalOnProperty(name = "lmp.mail.dispatcher", havingValue = "smtp", matchIfMissing = true)
public class SmtpMailDispatcher implements MailDispatcher {

    private final JavaMailSender mailSender;

    public SmtpMailDispatcher(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(EmailQueueEvent event) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

        if (event.getSenderName() != null && !event.getSenderName().isBlank()) {
            helper.setFrom(new InternetAddress(event.getSender(), event.getSenderName(), "UTF-8"));
        } else {
            helper.setFrom(event.getSender());
        }
        if (event.getReplyTo() != null && !event.getReplyTo().isBlank()) {
            helper.setReplyTo(event.getReplyTo());
        }
        helper.setTo(event.getRecipient());
        if (event.getCc() != null && !event.getCc().isBlank()) {
            helper.setCc(event.getCc().split("\\s*,\\s*"));
        }
        if (event.getBcc() != null && !event.getBcc().isBlank()) {
            helper.setBcc(event.getBcc().split("\\s*,\\s*"));
        }
        helper.setSubject(event.getSubject());

        if (event.getBodyHtml() != null && !event.getBodyHtml().isBlank()) {
            String text = event.getBodyText() != null ? event.getBodyText() : "";
            helper.setText(text, event.getBodyHtml());
        } else {
            helper.setText(event.getBodyText() != null ? event.getBodyText() : "");
        }

        mailSender.send(msg);
    }
}
