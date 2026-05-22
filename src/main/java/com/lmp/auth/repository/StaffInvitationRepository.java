package com.lmp.auth.repository;

import com.lmp.auth.domain.StaffInvitation;
import com.lmp.auth.domain.StaffInvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, UUID> {

    Optional<StaffInvitation> findByToken(String token);

    Optional<StaffInvitation> findByEmailAndStatus(String email, StaffInvitationStatus status);

    Page<StaffInvitation> findByStatus(StaffInvitationStatus status, Pageable pageable);

    List<StaffInvitation> findByStatusAndExpiresAtBefore(StaffInvitationStatus status, LocalDateTime cutoff);
}
