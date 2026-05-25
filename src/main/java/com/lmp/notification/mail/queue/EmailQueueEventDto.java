package com.lmp.notification.mail.queue;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO léger pour exposer la file d'attente email dans l'API admin.
 */
public record EmailQueueEventDto(
        UUID id,
        String recipient,
        String subject,
        String status,
        int retryCount,
        int maxRetries,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
        String lastError,
        String sender
) {

    public static EmailQueueEventDto fromEntity(EmailQueueEvent event) {
        return new EmailQueueEventDto(
                event.getId(),
                event.getRecipient(),
                event.getSubject(),
                event.getStatus().name(),
                event.getRetryCount(),
                event.getMaxRetries(),
                event.getCreatedAt(),
                event.getSentAt(),
                event.getLastError(),
                event.getSender()
        );
    }
}
