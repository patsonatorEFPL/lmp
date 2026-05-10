package com.lmp.notification.mail.queue;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload immutable pour {@link MailQueueService#enqueue(EmailQueueRequest)}.
 * Utilise un builder fluide pour éviter un constructeur à 12+ paramètres.
 */
public record EmailQueueRequest(
        String sender,
        String senderName,
        String replyTo,
        String recipient,
        String cc,
        String bcc,
        String subject,
        String bodyHtml,
        String bodyText,
        int priority,
        int maxRetries,
        LocalDateTime sendAfter,
        UUID correlationId
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String sender;
        private String senderName;
        private String replyTo;
        private String recipient;
        private String cc;
        private String bcc;
        private String subject;
        private String bodyHtml;
        private String bodyText;
        private int priority = 5;
        private int maxRetries = 3;
        private LocalDateTime sendAfter;
        private UUID correlationId;

        public Builder sender(String v)        { this.sender = v; return this; }
        public Builder senderName(String v)    { this.senderName = v; return this; }
        public Builder replyTo(String v)       { this.replyTo = v; return this; }
        public Builder recipient(String v)     { this.recipient = v; return this; }
        public Builder cc(String v)            { this.cc = v; return this; }
        public Builder bcc(String v)           { this.bcc = v; return this; }
        public Builder subject(String v)       { this.subject = v; return this; }
        public Builder bodyHtml(String v)      { this.bodyHtml = v; return this; }
        public Builder bodyText(String v)      { this.bodyText = v; return this; }
        public Builder priority(int v)         { this.priority = v; return this; }
        public Builder maxRetries(int v)       { this.maxRetries = v; return this; }
        public Builder sendAfter(LocalDateTime v) { this.sendAfter = v; return this; }
        public Builder correlationId(UUID v)   { this.correlationId = v; return this; }

        public EmailQueueRequest build() {
            return new EmailQueueRequest(sender, senderName, replyTo, recipient,
                    cc, bcc, subject, bodyHtml, bodyText,
                    priority, maxRetries, sendAfter, correlationId);
        }
    }
}
