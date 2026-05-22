package com.lmp.support.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.support.domain.ActorType;
import com.lmp.support.domain.AuditEventType;
import com.lmp.support.domain.AuditLogEntry;
import com.lmp.support.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Append-only audit log writer + chain verifier.
 *
 * <p>Each entry contains a SHA-256 hash computed over the previous entry's
 * hash concatenated with the current row's session id, event type, actor,
 * payload JSON, and microsecond-precision timestamp. Tampering with any
 * stored value breaks the chain on {@link #verifyChain(UUID)}. The DB-level
 * trigger {@code support_audit_log_block_modifications} prevents UPDATE
 * or DELETE at the application role level.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository repo;
    private final ObjectMapper jsonMapper;

    public AuditLogService(AuditLogRepository repo, ObjectMapper jsonMapper) {
        this.repo = repo;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public AuditLogEntry append(UUID sessionId, AuditEventType type, ActorType actor, Map<String, Object> payload) {
        return append(sessionId, type, actor, null, payload);
    }

    @Transactional
    public AuditLogEntry append(UUID sessionId, AuditEventType type, ActorType actor, UUID actorId,
                                Map<String, Object> payload) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setSessionId(sessionId);
        entry.setEventType(type.name());
        entry.setActorType(actor);
        entry.setActorId(actorId);
        entry.setPayload(payload == null ? Map.of() : payload);

        Instant now = Instant.now();
        entry.setOccurredAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC));
        long micros = now.getEpochSecond() * 1_000_000L + now.getNano() / 1_000L;
        entry.setOccurredAtMicros(micros);

        byte[] prevHash = repo.findFirstByOrderByIdDesc()
            .map(AuditLogEntry::getEntryHash)
            .orElse(null);
        entry.setPrevHash(prevHash);
        entry.setEntryHash(computeHash(prevHash, entry));

        return repo.save(entry);
    }

    /**
     * Recompute the hash of every entry for {@code sessionId} and verify that
     * each stored {@code entry_hash} matches. Returns false on any mismatch.
     */
    public boolean verifyChain(UUID sessionId) {
        List<AuditLogEntry> entries = repo.findBySessionIdOrderByOccurredAtAsc(sessionId);
        for (AuditLogEntry e : entries) {
            byte[] computed = computeHash(e.getPrevHash(), e);
            if (!Arrays.equals(computed, e.getEntryHash())) {
                return false;
            }
        }
        return true;
    }

    private byte[] computeHash(byte[] prev, AuditLogEntry entry) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (prev != null) {
                md.update(prev);
            }
            md.update(entry.getSessionId().toString().getBytes(StandardCharsets.UTF_8));
            md.update(entry.getEventType().getBytes(StandardCharsets.UTF_8));
            md.update(entry.getActorType().name().getBytes(StandardCharsets.UTF_8));
            if (entry.getActorId() != null) {
                md.update(entry.getActorId().toString().getBytes(StandardCharsets.UTF_8));
            }
            md.update(jsonMapper.writeValueAsBytes(entry.getPayload()));
            md.update(longToBytes(entry.getOccurredAtMicros()));
            return md.digest();
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new HashChainException("hash compute failed", e);
        }
    }

    private static byte[] longToBytes(long v) {
        byte[] b = new byte[8];
        for (int i = 7; i >= 0; i--) {
            b[i] = (byte) (v & 0xFF);
            v >>= 8;
        }
        return b;
    }
}
