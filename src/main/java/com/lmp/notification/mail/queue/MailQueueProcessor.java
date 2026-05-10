package com.lmp.notification.mail.queue;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Worker poll-based qui consomme {@link EmailQueueEvent} en {@code NOT_SENT},
 * effectue le SMTP send via {@link JavaMailSender}, et marque {@code SENT} ou
 * {@code ERROR}/{@code NOT_SENT} (selon retry budget).
 *
 * <p>Tick par défaut : 5s. external CRM = 4 min, soit 48× plus rapide.</p>
 *
 * <p>Multi-replica safe : claim atomique via UPDATE...RETURNING (cf {@link EmailQueueRepositoryImpl}).
 * 5 replicas se partagent la queue sans race ni double-send.</p>
 */
@Component
@ConditionalOnProperty(name = "lmp.mail.queue.enabled", havingValue = "true", matchIfMissing = true)
public class MailQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(MailQueueProcessor.class);

    private final EmailQueueRepository repository;
    private final JavaMailSender mailSender;

    @Value("${lmp.mail.queue.batch-size:20}")
    private int batchSize;

    @Value("${lmp.mail.queue.retry.base-delay-seconds:60}")
    private long retryBaseDelaySeconds;

    public MailQueueProcessor(EmailQueueRepository repository, JavaMailSender mailSender) {
        this.repository = repository;
        this.mailSender = mailSender;
    }

    @Scheduled(fixedDelayString = "${lmp.mail.queue.poll-interval-ms:5000}")
    public void processNextBatch() {
        List<EmailQueueEvent> batch = repository.claimNextBatch(batchSize);
        if (batch.isEmpty()) {
            return;
        }

        log.info("📤 [MAIL QUEUE] Processing batch of {} emails", batch.size());

        int sent = 0;
        int failed = 0;
        for (EmailQueueEvent event : batch) {
            try {
                sendOne(event);
                sent++;
            } catch (Exception e) {
                handleFailure(event, e);
                failed++;
            }
        }

        log.info("📤 [MAIL QUEUE] Batch complete — {} sent, {} failed", sent, failed);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendOne(EmailQueueEvent event) throws MessagingException, UnsupportedEncodingException {
        EmailQueueEvent fresh = repository.findById(event.getId()).orElse(null);
        if (fresh == null || fresh.getStatus() != EmailQueueStatus.SENDING) {
            log.debug("⏭️ [MAIL QUEUE] {} not in SENDING — skipping", event.getId());
            return;
        }

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

        if (fresh.getSenderName() != null && !fresh.getSenderName().isBlank()) {
            helper.setFrom(new InternetAddress(fresh.getSender(), fresh.getSenderName(), "UTF-8"));
        } else {
            helper.setFrom(fresh.getSender());
        }
        if (fresh.getReplyTo() != null && !fresh.getReplyTo().isBlank()) {
            helper.setReplyTo(fresh.getReplyTo());
        }
        helper.setTo(fresh.getRecipient());
        if (fresh.getCc() != null && !fresh.getCc().isBlank()) {
            helper.setCc(fresh.getCc().split("\\s*,\\s*"));
        }
        if (fresh.getBcc() != null && !fresh.getBcc().isBlank()) {
            helper.setBcc(fresh.getBcc().split("\\s*,\\s*"));
        }
        helper.setSubject(fresh.getSubject());

        if (fresh.getBodyHtml() != null && !fresh.getBodyHtml().isBlank()) {
            // text fallback for clients that don't render HTML
            String text = fresh.getBodyText() != null ? fresh.getBodyText() : "";
            helper.setText(text, fresh.getBodyHtml());
        } else {
            helper.setText(fresh.getBodyText() != null ? fresh.getBodyText() : "");
        }

        mailSender.send(msg);

        fresh.setStatus(EmailQueueStatus.SENT);
        fresh.setSentAt(LocalDateTime.now());
        fresh.setLastError(null);
        repository.save(fresh);
        log.info("✅ [MAIL QUEUE] Sent to={} subj=\"{}\" id={}",
                fresh.getRecipient(),
                truncate(fresh.getSubject(), 60),
                fresh.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(EmailQueueEvent event, Exception cause) {
        EmailQueueEvent fresh = repository.findById(event.getId()).orElse(null);
        if (fresh == null) return;

        int newRetry = fresh.getRetryCount() + 1;
        fresh.setRetryCount(newRetry);
        fresh.setLastError(truncate(safeMessage(cause), 4000));

        if (newRetry >= fresh.getMaxRetries()) {
            fresh.setStatus(EmailQueueStatus.ERROR);
            log.error("💀 [MAIL QUEUE] {} -> ERROR after {} retries: {}",
                    fresh.getId(), newRetry, safeMessage(cause));
        } else {
            // Exponential backoff : base × 2^(retry-1).
            long backoff = retryBaseDelaySeconds * (long) Math.pow(2, newRetry - 1);
            fresh.setSendAfter(LocalDateTime.now().plusSeconds(backoff));
            fresh.setStatus(EmailQueueStatus.NOT_SENT);
            log.warn("⚠️ [MAIL QUEUE] {} retry {}/{} in {}s — {}",
                    fresh.getId(), newRetry, fresh.getMaxRetries(), backoff, safeMessage(cause));
        }
        repository.save(fresh);
    }

    private static String safeMessage(Exception e) {
        if (e == null) return "(null)";
        String m = e.getMessage();
        if (m != null && !m.isBlank()) return e.getClass().getSimpleName() + ": " + m;
        return e.getClass().getName();
    }

    private static String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len) + "…";
    }
}
