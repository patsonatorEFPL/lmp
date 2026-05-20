package com.lmp.support.web;

import com.lmp.auth.domain.User;
import com.lmp.auth.service.UserService;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.domain.Ticket;
import com.lmp.support.meshcentral.MeshCentralService;
import com.lmp.support.repository.TicketRepository;
import com.lmp.support.service.SupportSessionService;
import com.lmp.support.web.dto.CreateSessionRequest;
import com.lmp.support.web.dto.SessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelpDeskControllerTest {

    private SupportSessionService sessionService;
    private MeshCentralService meshService;
    private TicketRepository ticketRepo;
    private UserService userService;
    private HelpDeskController controller;

    @BeforeEach
    void setup() {
        sessionService = mock(SupportSessionService.class);
        meshService = mock(MeshCentralService.class);
        ticketRepo = mock(TicketRepository.class);
        userService = mock(UserService.class);
        controller = new HelpDeskController(sessionService, meshService, ticketRepo, userService,
            Duration.ofMinutes(15));
    }

    @Test
    void createSessionOrchestratesMeshAndReturnsInvited() {
        UUID techId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User tech = new User();
        tech.setId(techId);
        when(userService.findByLogin("tech@lmp.ca")).thenReturn(Optional.of(tech));

        User customer = new User();
        customer.setId(clientId);
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setCustomer(customer);
        when(ticketRepo.findByIdWithCustomer(ticketId)).thenReturn(Optional.of(ticket));

        SupportSession draft = new SupportSession();
        draft.setId(sessionId);
        draft.setTicketId(ticketId);
        draft.setTechUserId(techId);
        draft.setClientUserId(clientId);
        draft.setStatus(SessionStatus.DRAFT);
        draft.setCreatedAt(LocalDateTime.now());
        when(sessionService.create(ticketId, techId, clientId)).thenReturn(draft);

        when(meshService.createDeviceGroup(sessionId)).thenReturn("mesh//abc123");

        SupportSession invited = new SupportSession();
        invited.setId(sessionId);
        invited.setTicketId(ticketId);
        invited.setTechUserId(techId);
        invited.setClientUserId(clientId);
        invited.setStatus(SessionStatus.INVITED);
        invited.setMeshCentralGroupId("mesh//abc123");
        invited.setCreatedAt(draft.getCreatedAt());
        when(sessionService.attachMeshGroup(sessionId, "mesh//abc123")).thenReturn(invited);
        when(sessionService.transition(sessionId, SessionStatus.INVITED, null)).thenReturn(invited);

        when(meshService.generateAgentInvite(eq("mesh//abc123"), any(Duration.class)))
            .thenReturn("https://mesh.lmp-services.ca/agentinvite?c=abc123");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("tech@lmp.ca");

        ResponseEntity<SessionResponse> response = controller.create(
            new CreateSessionRequest(ticketId), auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SessionResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(sessionId);
        assertThat(body.status()).isEqualTo("INVITED");
        assertThat(body.meshCentralGroupId()).isEqualTo("mesh//abc123");
        assertThat(body.runnerInviteUrl()).contains("agentinvite");
        assertThat(body.techUserId()).isEqualTo(techId);
        assertThat(body.clientUserId()).isEqualTo(clientId);

        // ordering check: create -> mesh group -> attach -> transition -> invite
        verify(sessionService).create(ticketId, techId, clientId);
        verify(meshService).createDeviceGroup(sessionId);
        verify(sessionService).attachMeshGroup(sessionId, "mesh//abc123");
        verify(sessionService).transition(sessionId, SessionStatus.INVITED, null);
        verify(meshService).generateAgentInvite(eq("mesh//abc123"), any(Duration.class));
    }

    @Test
    void createSessionRejectsMissingTicketId() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("tech@lmp.ca");

        assertThatThrownBy(() -> controller.create(new CreateSessionRequest(null), auth))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createSessionThrows404WhenTicketMissing() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepo.findByIdWithCustomer(ticketId)).thenReturn(Optional.empty());
        when(userService.findByLogin(any())).thenReturn(Optional.of(stubUser()));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("tech@lmp.ca");

        assertThatThrownBy(() -> controller.create(new CreateSessionRequest(ticketId), auth))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void createSessionThrows409WhenTicketHasNoCustomer() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        // customer left null
        when(ticketRepo.findByIdWithCustomer(ticketId)).thenReturn(Optional.of(ticket));
        when(userService.findByLogin(any())).thenReturn(Optional.of(stubUser()));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("tech@lmp.ca");

        assertThatThrownBy(() -> controller.create(new CreateSessionRequest(ticketId), auth))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createSessionThrows401WhenAuthenticationIsNull() {
        UUID ticketId = UUID.randomUUID();
        // ticket lookup happens before auth resolution, so stub it
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        User customer = new User(); customer.setId(UUID.randomUUID());
        ticket.setCustomer(customer);
        when(ticketRepo.findByIdWithCustomer(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> controller.create(new CreateSessionRequest(ticketId), null))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void getReturnsSessionState() {
        UUID id = UUID.randomUUID();
        SupportSession s = new SupportSession();
        s.setId(id);
        s.setTicketId(UUID.randomUUID());
        s.setTechUserId(UUID.randomUUID());
        s.setClientUserId(UUID.randomUUID());
        s.setStatus(SessionStatus.ACTIVE);
        s.setCreatedAt(LocalDateTime.now());
        when(sessionService.findById(id)).thenReturn(s);

        SessionResponse resp = controller.get(id);

        assertThat(resp.id()).isEqualTo(id);
        assertThat(resp.status()).isEqualTo("ACTIVE");
        assertThat(resp.runnerInviteUrl()).isNull();
    }

    private User stubUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        return u;
    }
}
