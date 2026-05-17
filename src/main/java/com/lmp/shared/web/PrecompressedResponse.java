package com.lmp.shared.web;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.zip.GZIPOutputStream;

/**
 * Cached response avec pré-built ResponseEntity instances pour skip
 * BodyBuilder allocations per request.
 *
 * <p>Profile iter28 G1GC @ 7k VU : ResponseEntity/HttpMessageConverter
 * 19% CPU. BodyBuilder allocate header maps + cacheControl + content type
 * per request. Pre-built immutable ResponseEntity = reuse cross-thread,
 * zero per-request allocation.</p>
 *
 * <p>Variants : rawEntity (no gzip), gzipEntity (Content-Encoding gzip),
 * notModifiedEntity (304 vide pour If-None-Match match).</p>
 */
public record PrecompressedResponse(
        byte[] raw,
        byte[] gzip,
        String etag,
        Instant expiresAt,
        ResponseEntity<byte[]> rawEntity,
        ResponseEntity<byte[]> gzipEntity,
        ResponseEntity<byte[]> notModifiedEntity) {

    public static PrecompressedResponse build(byte[] raw, Instant expiresAt) throws IOException {
        return build(raw, expiresAt,
                CacheControl.maxAge(java.time.Duration.ofMinutes(5)).cachePublic(),
                "Accept-Encoding");
    }

    public static PrecompressedResponse build(byte[] raw, Instant expiresAt,
                                                CacheControl cacheControl, String varyHeader) throws IOException {
        byte[] gz;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(raw.length / 2, 256));
             GZIPOutputStream g = new GZIPOutputStream(out)) {
            g.write(raw);
            g.finish();
            gz = out.toByteArray();
        }
        String etag = computeEtag(raw);

        ResponseEntity<byte[]> rawE = ResponseEntity.ok()
                .cacheControl(cacheControl)
                .header("ETag", etag)
                .header("Vary", varyHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(raw);

        ResponseEntity<byte[]> gzipE = ResponseEntity.ok()
                .cacheControl(cacheControl)
                .header("ETag", etag)
                .header("Vary", varyHeader)
                .header("Content-Encoding", "gzip")
                .contentType(MediaType.APPLICATION_JSON)
                .body(gz);

        ResponseEntity<byte[]> notModE = ResponseEntity.status(304)
                .cacheControl(cacheControl)
                .header("ETag", etag)
                .header("Vary", varyHeader)
                .build();

        return new PrecompressedResponse(raw, gz, etag, expiresAt, rawE, gzipE, notModE);
    }

    private static String computeEtag(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return "\"" + HexFormat.of().formatHex(hash, 0, 16) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
