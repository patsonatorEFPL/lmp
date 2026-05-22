package com.lmp.notification.mail.dispatch;

import com.lmp.notification.mail.queue.EmailQueueEvent;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envoi direct via {@link JavaMailSender} (SMTP). Comportement legacy LMP.
 */
@Component
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

        List<String> ccList = event.getCcList();
        if (!ccList.isEmpty()) {
            helper.setCc(ccList.toArray(new String[0]));
        }
        List<String> bccList = event.getBccList();
        if (!bccList.isEmpty()) {
            helper.setBcc(bccList.toArray(new String[0]));
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
