package com.lmp.support.web.dto;

import com.lmp.support.domain.ActorType;
import com.lmp.support.domain.AuditLogEntry;

import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Audit log row payload for the tech UI compliance view.
 *
 * Hashes are exposed as lowercase hex so the UI can show them for forensic
 * inspection without dealing with byte arrays.
 */
public record AuditEntryResponse(
    Long id,
    UUID sessionId,
    String eventType,
    ActorType actorType,
    UUID actorId,
    Map<String, Object> payload,
    LocalDateTime occurredAt,
    long occurredAtMicros,
    String prevHashHex,
    String entryHashHex
) {
    public static AuditEntryResponse of(AuditLogEntry e) {
        return new AuditEntryResponse(
            e.getId(),
            e.getSessionId(),
            e.getEventType(),
            e.getActorType(),
            e.getActorId(),
            e.getPayload(),
            e.getOccurredAt(),
            e.getOccurredAtMicros(),
            e.getPrevHash() == null ? null : HexFormat.of().formatHex(e.getPrevHash()),
            e.getEntryHash() == null ? null : HexFormat.of().formatHex(e.getEntryHash())
        );
    }
}
