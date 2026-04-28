package com.lmp.integration.sync.monitoring;

import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Métriques Micrometer pour le sous-système de synchronisation ERP.
 * <p>
 * Compteurs par {@code entityType} / {@code status} + gauges pour la profondeur
 * de file et la disponibilité ERP.
 */
@Component
public class SyncMetricsService {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger queueDepth = new AtomicInteger(0);
    private final AtomicInteger erpAvailable = new AtomicInteger(0);

    public SyncMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        Gauge.builder("sync.queue.depth", queueDepth, AtomicInteger::get)
                .description("Number of events currently queued for sync")
                .register(meterRegistry);

        Gauge.builder("sync.erp.availability", erpAvailable, AtomicInteger::get)
                .description("ERP availability (1 = up, 0 = down)")
                .register(meterRegistry);
    }

    public void recordEventProcessed(SyncEntityType entityType, SyncStatus status) {
        Counter.builder("sync.events.total")
                .tag("entityType", entityType.name())
                .tag("status", status.name())
                .description("Total sync events processed")
                .register(meterRegistry)
                .increment();
    }

    public void recordEventRetried(SyncEntityType entityType) {
        Counter.builder("sync.events.retried")
                .tag("entityType", entityType.name())
                .description("Total sync events retried")
                .register(meterRegistry)
                .increment();
    }

    public void recordEventDead(SyncEntityType entityType) {
        Counter.builder("sync.events.dead")
                .tag("entityType", entityType.name())
                .description("Total sync events marked dead")
                .register(meterRegistry)
                .increment();
    }

    public void setQueueDepth(int depth) {
        queueDepth.set(depth);
    }

    public void setErpAvailable(boolean available) {
        erpAvailable.set(available ? 1 : 0);
    }
}
