package com.lmp.support.service;

import com.lmp.support.domain.Ticket;
import com.lmp.support.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service métier pour la gestion des tickets de support.
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<Ticket> getTicketsByCustomer(UUID customerId) {
        return ticketRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Optional<Ticket> getTicket(UUID id) {
        return ticketRepository.findById(id);
    }

    public Optional<Ticket> getByExternalId(String externalIssueId) {
        return ticketRepository.findByExternalIssueId(externalIssueId);
    }

    @Transactional
    public Ticket save(Ticket ticket) {
        return ticketRepository.save(ticket);
    }
}
