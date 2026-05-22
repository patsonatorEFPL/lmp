package com.lmp.notification.mail.dispatch;

import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.notification.mail.queue.EmailQueueEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envoi des emails via l'API du framework externe (relais Email Queue distante).
 *
 * <p>Méthode appelée : {@code frappe.core.doctype.communication.email.make} avec
 * {@code send_email=1} pour déclencher l'envoi immédiat (sans queue distante
 * supplémentaire — LMP a déjà sa propre queue).</p>
 */
@Component
public class ExternalCrmMailDispatcher implements MailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ExternalCrmMailDispatcher.class);

    private final ExternalSystemClient externalClient;

    public ExternalCrmMailDispatcher(ExternalSystemClient externalClient) {
        this.externalClient = externalClient;
    }

    @Override
    public void send(EmailQueueEvent event) throws Exception {
        if (!externalClient.isAvailable()) {
            throw new IllegalStateException(
                    "ERPNext dispatcher selected but ExternalSystemClient unavailable (lmp.sync.enabled=false?)");
        }

        Map<String, Object> args = new HashMap<>();

        List<String> recipients = new ArrayList<>();
        recipients.add(event.getRecipient());
        args.put("recipients", recipients);

        List<String> ccList = event.getCcList();
        if (!ccList.isEmpty()) args.put("cc", ccList);
        List<String> bccList = event.getBccList();
        if (!bccList.isEmpty()) args.put("bcc", bccList);

        args.put("subject", event.getSubject());

        if (event.getBodyHtml() != null && !event.getBodyHtml().isBlank()) {
            args.put("content", event.getBodyHtml());
            args.put("content_type", "HTML");
        } else {
            args.put("content", event.getBodyText() != null ? event.getBodyText() : "");
            args.put("content_type", "text/plain");
        }

        if (event.getSender() != null && !event.getSender().isBlank()) {
            args.put("sender", event.getSender());
        }
        if (event.getSenderName() != null && !event.getSenderName().isBlank()) {
            args.put("sender_full_name", event.getSenderName());
        }
        if (event.getReplyTo() != null && !event.getReplyTo().isBlank()) {
            args.put("read_receipt", 0);
            args.put("recipients_email", event.getRecipient());
        }

        args.put("doctype", "Communication");
        args.put("send_email", 1);
        args.put("communication_medium", "Email");

        ExternalResponse response = externalClient.callMethod(
                "frappe.core.doctype.communication.email.make", args);

        if (response == null || !response.success()) {
            String errorMsg = response != null ? response.errorMessage() : "null response";
            throw new IllegalStateException("External framework email send failed: " + errorMsg);
        }

        log.debug("📤 [MAIL EXT-CRM] dispatched id={} to={} via external framework", event.getId(), event.getRecipient());
    }
}
