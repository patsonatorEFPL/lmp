package com.lmp.support.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Throttles AI-assisted command suggestions per HelpDesk session: short-window rate limit
 * (commands/min and commands/5min) plus a cumulative Claude-token cap.
 *
 * Atomic semantics: a successful {@link #tryConsume(UUID, int)} both reserves a slot in
 * the rate bucket and adds to the token counter; a failed call mutates neither.
 */
@Service
public class AiBudgetService {

    private final int perMinute;
    private final int per5Minutes;
    private final long tokenCapPerSession;

    private final ConcurrentMap<UUID, AtomicLong> tokensConsumed = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, RateBucket> rateBuckets = new ConcurrentHashMap<>();

    public AiBudgetService(
        @Value("${lmp.helpdesk.ai.rate-per-minute:30}") int perMinute,
        @Value("${lmp.helpdesk.ai.rate-per-5min:100}") int per5Minutes,
        @Value("${lmp.helpdesk.ai.tokens-per-session:50000}") long tokenCap
    ) {
        this.perMinute = perMinute;
        this.per5Minutes = per5Minutes;
        this.tokenCapPerSession = tokenCap;
    }

    public boolean tryConsume(UUID sessionId, int tokens) {
        AtomicLong tc = tokensConsumed.computeIfAbsent(sessionId, k -> new AtomicLong());
        RateBucket rb = rateBuckets.computeIfAbsent(sessionId, k -> new RateBucket());

        synchronized (rb) {
            long now = Instant.now().getEpochSecond();
            rb.prune(now);
            if (rb.countSince(now, 60) >= perMinute) return false;
            if (rb.countSince(now, 300) >= per5Minutes) return false;
            if (tc.get() + (long) tokens > tokenCapPerSession) return false;

            rb.record(now);
            tc.addAndGet(tokens);
            return true;
        }
    }

    public long tokensUsed(UUID sessionId) {
        AtomicLong v = tokensConsumed.get(sessionId);
        return v == null ? 0L : v.get();
    }

    public void reset(UUID sessionId) {
        tokensConsumed.remove(sessionId);
        rateBuckets.remove(sessionId);
    }

    private static final class RateBucket {
        private final Deque<Long> stamps = new ArrayDeque<>();

        void record(long now) {
            stamps.addLast(now);
        }

        void prune(long now) {
            while (!stamps.isEmpty() && now - stamps.peekFirst() > 300) {
                stamps.pollFirst();
            }
        }

        int countSince(long now, long windowSeconds) {
            int c = 0;
            for (Long t : stamps) {
                if (now - t <= windowSeconds) c++;
            }
            return c;
        }
    }
}
