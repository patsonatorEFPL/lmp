package com.lmp.auth.service;

import com.lmp.auth.domain.StaffInvitation;
import com.lmp.auth.domain.User;
import com.lmp.auth.dto.StaffInvitationAcceptRequest;
import com.lmp.auth.dto.StaffInvitationCreateRequest;

import java.util.UUID;

public interface StaffInvitationService {

    StaffInvitation createInvitation(StaffInvitationCreateRequest request, UUID actorId);

    StaffInvitation findValidByToken(String token);

    User acceptInvitation(StaffInvitationAcceptRequest request);

    void revokeInvitation(UUID invitationId, UUID actorId);

    void resendInvitation(UUID invitationId, UUID actorId);

    int markExpired();
}
