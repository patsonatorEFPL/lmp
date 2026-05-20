package com.lmp.support.recording;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

/**
 * Port over the object store backing HelpDesk recordings. Production binding will
 * use AWS SDK v2 against Cloudflare R2 with Object Lock GOVERNANCE retention;
 * the port lets {@code RecordingChunkService} and {@code RecordingMuxService}
 * unit-test without spinning S3 mocks.
 */
public interface RecordingStorage {

    /** Upload a session chunk (no retention lock — chunks expire normally). */
    void putChunk(String key, InputStream data, long contentLength);

    /**
     * Upload the final muxed MP4 with Object Lock GOVERNANCE retention.
     * GOVERNANCE (not COMPLIANCE) so an admin can erase for a GDPR Art.17 request.
     */
    void putFinalWithObjectLock(String key, InputStream data, long contentLength, Instant retainUntil);

    InputStream get(String key);

    /** Presigned GET URL for tech-side video.js playback. */
    URL presign(String key, Duration ttl);

    void delete(String key);
}
