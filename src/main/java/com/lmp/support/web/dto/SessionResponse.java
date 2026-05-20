package com.lmp.support.web.dto;

import com.lmp.support.domain.SupportSession;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
    UUID id,
    UUID ticketId,
    String status,
    UUID techUserId,
    UUID clientUserId,
    String meshCentralGroupId,
    String runnerInviteUrl,  // populated only at creation time
    LocalDateTime createdAt
) {
    public static SessionResponse of(SupportSession s, String inviteUrl) {
        return new SessionResponse(
            s.getId(),
            s.getTicketId(),
            s.getStatus().name(),
            s.getTechUserId(),
            s.getClientUserId(),
            s.getMeshCentralGroupId(),
            inviteUrl,
            s.getCreatedAt()
        );
    }
}
