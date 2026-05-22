package com.lmp.support.web.dto;

import com.lmp.support.domain.ConsentRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentResponse(
    UUID id,
    UUID sessionId,
    boolean accepted,
    LocalDateTime recordedAt
) {
    public static ConsentResponse of(ConsentRecord r) {
        return new ConsentResponse(
            r.getId(),
            r.getSessionId(),
            r.isAccepted(),
            r.getOccurredAt()
        );
    }
}
