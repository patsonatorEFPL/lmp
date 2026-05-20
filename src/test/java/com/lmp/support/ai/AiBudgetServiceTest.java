package com.lmp.support.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiBudgetServiceTest {

    private AiBudgetService svc;

    @BeforeEach
    void setup() {
        svc = new AiBudgetService(30, 100, 50_000);
    }

    @Test
    void allowsUpToRateLimitThenRejects() {
        UUID sid = UUID.randomUUID();
        for (int i = 0; i < 30; i++) {
            assertThat(svc.tryConsume(sid, 100)).as("call %d", i).isTrue();
        }
        assertThat(svc.tryConsume(sid, 100)).isFalse();
    }

    @Test
    void enforcesTokenCap() {
        UUID sid = UUID.randomUUID();
        assertThat(svc.tryConsume(sid, 49_999)).isTrue();
        assertThat(svc.tryConsume(sid, 100)).isFalse();  // would exceed 50_000
    }

    @Test
    void rejectionDoesNotMutateBudget() {
        UUID sid = UUID.randomUUID();
        svc.tryConsume(sid, 49_999);
        long before = svc.tokensUsed(sid);
        svc.tryConsume(sid, 100);  // rejected
        assertThat(svc.tokensUsed(sid)).isEqualTo(before);
    }

    @Test
    void differentSessionsHaveIndependentBuckets() {
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        for (int i = 0; i < 30; i++) {
            svc.tryConsume(s1, 100);
        }
        // s1 is rate-capped; s2 fresh
        assertThat(svc.tryConsume(s2, 100)).isTrue();
        assertThat(svc.tryConsume(s1, 100)).isFalse();
    }

    @Test
    void resetClearsBudget() {
        UUID sid = UUID.randomUUID();
        svc.tryConsume(sid, 49_999);
        svc.reset(sid);
        assertThat(svc.tokensUsed(sid)).isZero();
        assertThat(svc.tryConsume(sid, 100)).isTrue();
    }

    @Test
    void tokensUsedReflectsAccepted() {
        UUID sid = UUID.randomUUID();
        svc.tryConsume(sid, 1500);
        svc.tryConsume(sid, 2500);
        assertThat(svc.tokensUsed(sid)).isEqualTo(4000);
    }

    @Test
    void per5MinLimitCapsBeyondPerMinute() {
        // perMinute=30, per5Min=100 — fill 30 cmds (mockable timing not used here,
        // so we hit per-minute cap first); test ensures both gates exist
        AiBudgetService strict = new AiBudgetService(1000, 5, 10_000_000);
        UUID sid = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            assertThat(strict.tryConsume(sid, 1)).isTrue();
        }
        assertThat(strict.tryConsume(sid, 1)).isFalse();  // 5-min cap reached
    }
}
