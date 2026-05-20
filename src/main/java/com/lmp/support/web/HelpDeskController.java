package com.lmp.support.web;

import com.lmp.auth.domain.User;
import com.lmp.auth.service.UserService;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.domain.Ticket;
import com.lmp.support.meshcentral.MeshCentralService;
import com.lmp.support.repository.TicketRepository;
import com.lmp.support.service.SupportSessionService;
import com.lmp.support.signaling.TurnCredentialResponse;
import com.lmp.support.signaling.TurnCredentialService;
import com.lmp.support.web.dto.CreateSessionRequest;
import com.lmp.support.web.dto.SessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.UUID;

/**
 * Public REST entrypoint for HelpDesk session lifecycle.
 *
 * <ul>
 *   <li>{@code POST /api/v1/support/sessions} — tech creates a session for a ticket;
 *   orchestrates MeshCentral device group + agent invite, transitions DRAFT → INVITED.</li>
 *   <li>{@code GET /api/v1/support/sessions/{id}} — fetch session state.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/support/sessions")
public class HelpDeskController {

    private final SupportSessionService sessionService;
    private final MeshCentralService meshService;
    private final TicketRepository ticketRepository;
    private final UserService userService;
    private final TurnCredentialService turnService;
    private final Duration inviteTtl;

    public HelpDeskController(SupportSessionService sessionService,
                              MeshCentralService meshService,
                              TicketRepository ticketRepository,
                              UserService userService,
                              TurnCredentialService turnService) {
        this(sessionService, meshService, ticketRepository, userService, turnService,
            Duration.ofMinutes(15));
    }

    HelpDeskController(SupportSessionService sessionService,
                       MeshCentralService meshService,
                       TicketRepository ticketRepository,
                       UserService userService,
                       TurnCredentialService turnService,
                       Duration inviteTtl) {
        this.sessionService = sessionService;
        this.meshService = meshService;
        this.ticketRepository = ticketRepository;
        this.userService = userService;
        this.turnService = turnService;
        this.inviteTtl = inviteTtl;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECH','SUPPORT','ADMIN')")
    public ResponseEntity<SessionResponse> create(@Valid @RequestBody CreateSessionRequest req,
                                                  Authentication authentication) {
        if (req == null || req.ticketId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketId required");
        }

        Ticket ticket = ticketRepository.findByIdWithCustomer(req.ticketId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Ticket not found: " + req.ticketId()));
        User customer = ticket.getCustomer();
        if (customer == null || customer.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ticket has no customer attached");
        }

        UUID techUserId = resolveTechUserId(authentication);

        SupportSession draft = sessionService.create(ticket.getId(), techUserId, customer.getId());
        String meshId = meshService.createDeviceGroup(draft.getId());
        sessionService.attachMeshGroup(draft.getId(), meshId);
        SupportSession invited = sessionService.transition(draft.getId(), SessionStatus.INVITED, null);
        String inviteUrl = meshService.generateAgentInvite(meshId, inviteTtl);

        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.of(invited, inviteUrl));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECH','SUPPORT','ADMIN')")
    public SessionResponse get(@PathVariable UUID id) {
        SupportSession s = sessionService.findById(id);
        return SessionResponse.of(s, null);
    }

    /**
     * Issues short-lived TURN credentials scoped to the caller. Used by both the
     * tech browser and the runner WebView2 to populate their {@code RTCPeerConnection}
     * ICE servers list.
     */
    @GetMapping("/{id}/turn-credentials")
    @PreAuthorize("isAuthenticated()")
    public TurnCredentialResponse turnCredentials(@PathVariable UUID id,
                                                  Authentication authentication) {
        SupportSession s = sessionService.findById(id);
        UUID callerId = resolveTechUserId(authentication);
        if (!callerId.equals(s.getTechUserId()) && !callerId.equals(s.getClientUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Not a participant of session " + id);
        }
        return turnService.issue(authentication.getName());
    }

    private UUID resolveTechUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        User actor = userService.findByLogin(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Unknown principal: " + authentication.getName()));
        return actor.getId();
    }
}
