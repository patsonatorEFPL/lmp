package com.lmp.notification.mail.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public API d'enqueue. Persiste un row {@link EmailQueueEvent} en {@code NOT_SENT}.
 * Le {@code MailQueueProcessor} prend en charge le SMTP send asynchrone.
 *
 * <p>Avantages vs send SMTP synchrone direct :</p>
 * <ul>
 *   <li>Durabilité : si l'app crash mid-send, le row reste — recovery scheduler le repick.</li>
 *   <li>Multi-replica safe : claim atomique évite les double-sends.</li>
 *   <li>Retry exponentiel : SMTP outage ne perd pas les emails.</li>
 *   <li>Priority + send_after : transactional &gt; marketing, scheduling natif.</li>
 *   <li>Découplé du thread HTTP : pas de blocage MVC sur SMTP wait.</li>
 * </ul>
 */
@Service
public class MailQueueService {

    private static final Logger log = LoggerFactory.getLogger(MailQueueService.class);

    public static final int PRIORITY_TRANSACTIONAL = 9;  // password reset, order confirmation
    public static final int PRIORITY_NORMAL        = 5;  // welcome email, contact form ack
    public static final int PRIORITY_MARKETING     = 1;  // newsletter, drip campaigns

    private final EmailQueueRepository repository;

    public MailQueueService(EmailQueueRepository repository) {
        this.repository = repository;
    }

    /**
     * Enqueue immédiat à priority normale.
     */
    @Transactional
    public UUID enqueue(String sender, String senderName, String recipient,
                        String subject, String bodyHtml) {
        return enqueue(EmailQueueRequest.builder()
                .sender(sender)
                .senderName(senderName)
                .recipient(recipient)
                .subject(subject)
                .bodyHtml(bodyHtml)
                .priority(PRIORITY_NORMAL)
                .build());
    }

    /**
     * Enqueue avec contrôle complet.
     */
    @Transactional
    public UUID enqueue(EmailQueueRequest req) {
        if (req.recipient() == null || req.recipient().isBlank()) {
            throw new IllegalArgumentException("Email recipient required");
        }
        if (req.subject() == null || req.subject().isBlank()) {
            throw new IllegalArgumentException("Email subject required");
        }

        EmailQueueEvent event = new EmailQueueEvent();
        event.setSender(req.sender());
        event.setSenderName(req.senderName());
        event.setReplyTo(req.replyTo());
        event.setRecipient(req.recipient());
        event.setCc(req.cc());
        event.setBcc(req.bcc());
        event.setSubject(req.subject());
        event.setBodyHtml(req.bodyHtml());
        event.setBodyText(req.bodyText());
        event.setPriority(clamp(req.priority(), 1, 9));
        event.setMaxRetries(req.maxRetries() > 0 ? req.maxRetries() : 3);
        event.setSendAfter(req.sendAfter() != null ? req.sendAfter() : LocalDateTime.now());
        event.setCorrelationId(req.correlationId());

        EmailQueueEvent saved = repository.save(event);
        log.info("📥 [MAIL QUEUE] Enqueued to={} subj=\"{}\" priority={} queueId={}",
                req.recipient(), truncate(req.subject(), 60), event.getPriority(), saved.getId());
        return saved.getId();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len) + "…";
    }
}
