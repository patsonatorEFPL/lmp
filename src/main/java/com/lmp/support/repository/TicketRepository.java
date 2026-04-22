package com.lmp.support.repository;

import com.lmp.support.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByExternalIssueId(String externalIssueId);

    List<Ticket> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
