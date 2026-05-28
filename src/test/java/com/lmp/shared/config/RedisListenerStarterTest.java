package com.lmp.shared.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.TaskScheduler;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Structural tests for {@link RedisListenerStarter}. The actual startup race is
 * timing-dependent (overlay network attach) and not reproducible in a unit test;
 * these verify the retry/give-up/observability behaviour instead. The starter now
 * drives every {@link RedisMessageListenerContainer} bean (the app's own and Spring
 * Session's), so the tests cover the multi-container fan-out too.
 */
class RedisListenerStarterTest {

    private RedisMessageListenerContainer container;
    private TaskScheduler scheduler;
    private RedisListenerStarter starter;
    private ListAppender<ILoggingEvent> logs;
    private Logger logbackLogger;

    @BeforeEach
    void setUp() {
        container = mock(RedisMessageListenerContainer.class);
        scheduler = mock(TaskScheduler.class);
        starter = new RedisListenerStarter(singleContainer(container), scheduler);

        logbackLogger = (Logger) LoggerFactory.getLogger(RedisListenerStarter.class);
        logs = new ListAppender<>();
        logs.start();
        logbackLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logbackLogger.detachAppender(logs);
    }

    private static Map<String, RedisMessageListenerContainer> singleContainer(RedisMessageListenerContainer c) {
        Map<String, RedisMessageListenerContainer> m = new LinkedHashMap<>();
        m.put("redisMessageListenerContainer", c);
        return m;
    }

    @Test
    void startsContainerOnFirstSuccessfulAttempt() {
        doNothing().when(container).start();

        starter.tryStart();

        assertThat(starter.isStarted()).isTrue();
        assertThat(starter.hasGivenUp()).isFalse();
        assertThat(starter.attempts()).isEqualTo(1);
        // No retry scheduled on success.
        verify(scheduler, never()).schedule(any(Runnable.class), any(Instant.class));
        assertThat(infoMessages()).anyMatch(m -> m.contains("started on attempt"));
    }

    @Test
    void reschedulesRetryWhenStartThrows() {
        doThrow(new RuntimeException("Unable to connect to Redis")).when(container).start();

        starter.tryStart();

        assertThat(starter.isStarted()).isFalse();
        assertThat(starter.hasGivenUp()).isFalse();
        assertThat(starter.attempts()).isEqualTo(1);
        // One retry scheduled.
        verify(scheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
        assertThat(warnMessages()).anyMatch(m -> m.contains("start attempt 1"));
    }

    @Test
    void givesUpAfterMaxAttemptsAndLogsError() {
        doThrow(new RuntimeException("Unable to connect to Redis")).when(container).start();

        for (int i = 0; i < RedisListenerStarter.MAX_ATTEMPTS; i++) {
            starter.tryStart();
        }

        assertThat(starter.isStarted()).isFalse();
        assertThat(starter.hasGivenUp()).isTrue();
        assertThat(starter.attempts()).isEqualTo(RedisListenerStarter.MAX_ATTEMPTS);
        // Reschedules only on attempts 1..MAX-1; the final attempt gives up without scheduling.
        verify(scheduler, times(RedisListenerStarter.MAX_ATTEMPTS - 1))
                .schedule(any(Runnable.class), any(Instant.class));
        assertThat(errorMessages()).anyMatch(m -> m.contains("Gave up after"));
    }

    @Test
    void noOpWhenAlreadyStarted() {
        doNothing().when(container).start();
        starter.tryStart();

        starter.tryStart(); // second call

        assertThat(starter.attempts()).isEqualTo(1);
        verify(container, times(1)).start();
    }

    @Test
    void applicationReadySchedulesImmediateFirstAttempt() {
        starter.onApplicationReady();

        verify(scheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void retriesOnlyTheFailedContainerAfterPartialFailure() {
        RedisMessageListenerContainer ok = mock(RedisMessageListenerContainer.class);
        RedisMessageListenerContainer flaky = mock(RedisMessageListenerContainer.class);
        doNothing().when(ok).start();
        // Fails on the first attempt, succeeds on the second.
        doThrow(new RuntimeException("Unable to connect to Redis")).doNothing().when(flaky).start();

        Map<String, RedisMessageListenerContainer> two = new LinkedHashMap<>();
        two.put("redisMessageListenerContainer", ok);
        two.put("springSessionRedisMessageListenerContainer", flaky);
        RedisListenerStarter multi = new RedisListenerStarter(two, scheduler);

        multi.tryStart(); // attempt 1: ok starts, flaky throws → reschedule
        assertThat(multi.isStarted()).isFalse();
        verify(scheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));

        multi.tryStart(); // attempt 2: ok skipped (already started), flaky starts
        assertThat(multi.isStarted()).isTrue();
        assertThat(multi.attempts()).isEqualTo(2);

        // ok started exactly once (not retried), flaky started twice (retried).
        verify(ok, times(1)).start();
        verify(flaky, times(2)).start();
    }

    private java.util.List<String> infoMessages() {
        return messagesAt(Level.INFO);
    }

    private java.util.List<String> warnMessages() {
        return messagesAt(Level.WARN);
    }

    private java.util.List<String> errorMessages() {
        return messagesAt(Level.ERROR);
    }

    private java.util.List<String> messagesAt(Level level) {
        return logs.list.stream()
                .filter(e -> e.getLevel() == level)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
