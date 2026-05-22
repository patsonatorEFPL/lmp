package com.lmp.integration.sync.scheduler;

import com.lmp.integration.sync.SyncStatus;
import com.lmp.integration.sync.monitoring.SyncErrorPatternRepository;
import com.lmp.integration.sync.monitoring.SyncHealthSnapshotRepository;
import com.lmp.integration.sync.repository.SyncEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Purge périodique des données de synchronisation anciennes.
 * <p>
 * Supprime les événements terminés (SUCCESS, DEAD, SKIPPED) ainsi que les
 * snapshots de monitoring et les patterns d'erreur obsolètes.
 */
@Component
public class SyncPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncPurgeScheduler.class);

    private final SyncEventRepository syncEventRepository;
    private final SyncHealthSnapshotRepository healthSnapshotRepository;
    private final SyncErrorPatternRepository errorPatternRepository;

    private final int retentionDays;

    public SyncPurgeScheduler(SyncEventRepository syncEventRepository,
                              SyncHealthSnapshotRepository healthSnapshotRepository,
                              SyncErrorPatternRepository errorPatternRepository,
                              @Value("${lmp.sync.purge.retention-days:90}") int retentionDays) {
        this.syncEventRepository = syncEventRepository;
        this.healthSnapshotRepository = healthSnapshotRepository;
        this.errorPatternRepository = errorPatternRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${lmp.sync.purge.cron:0 0 3 * * *}")
    @SchedulerLock(name = "SyncPurgeScheduler.purge",
                   lockAtMostFor = "PT15M", lockAtLeastFor = "PT5M")
    @Transactional
    public void purge() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneId.systemDefault()).minusDays(retentionDays);
        log.info("🧹 [SYNC-PURGE] Starting purge for data older than {} days (before {})", retentionDays, cutoff);

        int total = 0;

        for (SyncStatus status : new SyncStatus[]{SyncStatus.SUCCESS, SyncStatus.DEAD, SyncStatus.SKIPPED}) {
            int deleted = syncEventRepository.deleteByStatusAndCreatedAtBefore(status.name(), cutoff);
            total += deleted;
            log.info("🧹 [SYNC-PURGE] Deleted {} {} events", deleted, status);
        }

        int snapshotsDeleted = healthSnapshotRepository.deleteByCreatedAtBefore(cutoff);
        log.info("🧹 [SYNC-PURGE] Deleted {} health snapshots", snapshotsDeleted);

        Instant instantCutoff = cutoff.atZone(ZoneId.systemDefault()).toInstant();
        int patternsDeleted = errorPatternRepository.deleteStaleWithZeroOccurrences(instantCutoff);
        log.info("🧹 [SYNC-PURGE] Deleted {} stale error patterns", patternsDeleted);

        log.info("🧹 [SYNC-PURGE] Completed. Total events deleted: {}", total);
    }
}
