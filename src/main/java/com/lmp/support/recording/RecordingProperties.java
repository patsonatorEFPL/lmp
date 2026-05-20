package com.lmp.support.recording;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * Config for {@code lmp.helpdesk.recording.*}. Production binds against Cloudflare R2;
 * Object Lock retention defaults to 90 days (GDPR / Loi 25 compliance window for
 * session recordings).
 */
@ConfigurationProperties(prefix = "lmp.helpdesk.recording")
public record RecordingProperties(
    String bucket,
    URI endpoint,
    String accessKey,
    String secretKey,
    String region,
    Duration defaultRetention,
    String chunkPrefix,
    String finalPrefix
) {
    public RecordingProperties {
        if (region == null || region.isBlank()) region = "auto";
        if (defaultRetention == null) defaultRetention = Duration.ofDays(90);
        if (chunkPrefix == null || chunkPrefix.isBlank()) chunkPrefix = "chunks/";
        if (finalPrefix == null || finalPrefix.isBlank()) finalPrefix = "final/";
    }

    /** True when bucket+endpoint+keys are all present and non-blank. */
    public boolean isConfigured() {
        return notBlank(bucket) && endpoint != null
            && notBlank(accessKey) && notBlank(secretKey);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
