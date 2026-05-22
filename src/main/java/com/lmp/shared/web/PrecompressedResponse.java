package com.lmp.shared.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.zip.GZIPOutputStream;

/**
 * Cached response : raw + gzip pré-compressed + ETag (SHA-256 hash) + TTL.
 *
 * <p>Pattern Pinterest/Cloudflare : compress + hash au cache build, serve
 * direct selon Accept-Encoding + If-None-Match client.</p>
 *
 * <p>ETag (RFC 7232) : permet client de revalider cache local sans
 * re-downloader le payload. Server retourne 304 Not Modified si
 * If-None-Match correspond → response body-less, sub-1ms latence.</p>
 *
 * <p>Trade-off : 2× mémoire (raw + gzip) + ~64 bytes ETag par entrée.
 * Pour responses 5-10KB, négligeable.</p>
 */
public record PrecompressedResponse(byte[] raw, byte[] gzip, String etag, Instant expiresAt) {

    public static PrecompressedResponse build(byte[] raw, Instant expiresAt) throws IOException {
        byte[] gz;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(raw.length / 2, 256));
             GZIPOutputStream g = new GZIPOutputStream(out)) {
            g.write(raw);
            g.finish();
            gz = out.toByteArray();
        }
        String etag = computeEtag(raw);
        return new PrecompressedResponse(raw, gz, etag, expiresAt);
    }

    private static String computeEtag(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            // Strong ETag (RFC 7232 §2.3) : "<hex>"
            return "\"" + HexFormat.of().formatHex(hash, 0, 16) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
