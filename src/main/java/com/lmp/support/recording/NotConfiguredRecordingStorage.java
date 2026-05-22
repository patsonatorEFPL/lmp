package com.lmp.support.recording;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

/**
 * Default {@link RecordingStorage} used until a live binding (AWS SDK against R2) is wired.
 * Throws on every call so the app starts cleanly but recording chunk upload + mux are
 * disabled. The live impl will replace this via {@code @ConditionalOnMissingBean} ordering.
 */
@Configuration
public class NotConfiguredRecordingStorage {

    @Bean
    @ConditionalOnMissingBean(RecordingStorage.class)
    RecordingStorage stubRecordingStorage() {
        return new RecordingStorage() {
            @Override
            public void putChunk(String key, InputStream data, long contentLength) {
                throw notConfigured();
            }

            @Override
            public void putFinalWithObjectLock(String key, InputStream data, long contentLength, Instant retainUntil) {
                throw notConfigured();
            }

            @Override
            public InputStream get(String key) {
                throw notConfigured();
            }

            @Override
            public URL presign(String key, Duration ttl) {
                throw notConfigured();
            }

            @Override
            public void delete(String key) {
                throw notConfigured();
            }

            private IllegalStateException notConfigured() {
                return new IllegalStateException(
                    "RecordingStorage not configured. Provide an implementation backed by AWS SDK "
                    + "(set R2_ENDPOINT, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY and bind a "
                    + "RecordingStorage bean).");
            }
        };
    }
}
