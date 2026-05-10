package com.lmp.notification.mail.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Recovery des rows {@code SENDING} orphelins (réplica crash mid-send).
 * <p>
 * Sans recovery, claim atomique = trou noir : un email claimé puis abandonné
 * reste {@code SENDING} ad vitam — invisible à la queue ({@code NOT_SENT}) et
 * au retry handler ({@code ERROR}). Pattern identique à {@code SyncRetryScheduler}.
 */
@Component
@ConditionalOnProperty(name = "lmp.mail.queue.enabled", havingValue = "true", matchIfMissing = true)
public class MailQueueRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(MailQueueRecoveryScheduler.class);

    private final EmailQueueRepository repository;

    @Value("${lmp.mail.queue.recovery.stale-after-minutes:5}")
    private int staleAfterMinutes;

    public MailQueueRecoveryScheduler(EmailQueueRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${lmp.mail.queue.recovery.delay-ms:60000}")
    @Transactional
    public void recoverOrphanSending() {
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(staleAfterMinutes);
        int recovered = repository.recoverStaleSending(staleBefore);
        if (recovered > 0) {
            log.warn("⚕️ [MAIL RECOVERY] Reset {} stale SENDING -> NOT_SENT (older than {} min)",
                    recovered, staleAfterMinutes);
        }
    }
}
