package com.lmp.support.meshcentral;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "lmp.helpdesk.meshcentral")
public record MeshCentralProperties(
    URI httpBase,
    URI wsBase,
    String adminUsername,
    String adminPassword,
    Duration commandTimeout,
    Duration reconnectInitialBackoff,
    Duration reconnectMaxBackoff,
    int reconnectMaxAttempts,
    boolean acceptSelfSigned
) {
    public MeshCentralProperties {
        if (commandTimeout == null) commandTimeout = Duration.ofSeconds(10);
        if (reconnectInitialBackoff == null) reconnectInitialBackoff = Duration.ofSeconds(1);
        if (reconnectMaxBackoff == null) reconnectMaxBackoff = Duration.ofSeconds(30);
        if (reconnectMaxAttempts == 0) reconnectMaxAttempts = Integer.MAX_VALUE;
    }
}
