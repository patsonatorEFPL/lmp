package com.lmp.auth.dto;

import com.lmp.auth.domain.StaffInvitation;

import java.time.LocalDateTime;
import java.util.UUID;

public record StaffInvitationResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String status,
        UUID invitedBy,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt
) {
    public static StaffInvitationResponse from(StaffInvitation inv) {
        return new StaffInvitationResponse(
                inv.getId(),
                inv.getEmail(),
                inv.getFirstName(),
                inv.getLastName(),
                inv.getStatus().name(),
                inv.getInvitedBy(),
                inv.getCreatedAt(),
                inv.getExpiresAt(),
                inv.getAcceptedAt(),
                inv.getRevokedAt()
        );
    }
}
