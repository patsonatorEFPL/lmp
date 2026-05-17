package com.lmp.shared.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.zip.GZIPOutputStream;

/**
 * Cached response : raw bytes + gzip pre-compressed bytes + TTL.
 *
 * <p>Pattern Pinterest/Cloudflare : compression au build (cache write) puis
 * serve byte[] direct selon Accept-Encoding client. Skip CPU compress per
 * request — sous 1k+ req/s même un gzip de 10kB ajoute des ms latency
 * cumulés.</p>
 *
 * <p>Trade-off : 2× mémoire cache (raw + gzip). Pour responses ~5-10KB,
 * négligeable. Pour gros responses (MB), considérer disk-backed cache.</p>
 */
public record PrecompressedResponse(byte[] raw, byte[] gzip, Instant expiresAt) {

    public static PrecompressedResponse build(byte[] raw, Instant expiresAt) throws IOException {
        byte[] gz;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(raw.length / 2, 256));
             GZIPOutputStream g = new GZIPOutputStream(out)) {
            g.write(raw);
            g.finish();
            gz = out.toByteArray();
        }
        return new PrecompressedResponse(raw, gz, expiresAt);
    }
}
