package com.lmp.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Tunables for the HelpDesk feature. Defaults match the spec at
 * {@code docs/superpowers/specs/2026-05-18-helpdesk-design.md} §4.
 */
@ConfigurationProperties(prefix = "lmp.helpdesk")
public record SupportProperties(
    Duration consentWaitTimeout,
    Duration sessionIdleWarn,
    Duration sessionIdleKill,
    Duration sessionMaxDuration,
    Duration runnerTokenTtl
) {
    public SupportProperties {
        if (consentWaitTimeout == null) consentWaitTimeout = Duration.ofMinutes(5);
        if (sessionIdleWarn == null) sessionIdleWarn = Duration.ofMinutes(10);
        if (sessionIdleKill == null) sessionIdleKill = Duration.ofMinutes(20);
        if (sessionMaxDuration == null) sessionMaxDuration = Duration.ofHours(4);
        if (runnerTokenTtl == null) runnerTokenTtl = Duration.ofMinutes(15);
    }
}
