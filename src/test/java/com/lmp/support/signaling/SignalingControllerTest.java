package com.lmp.support.signaling;

import com.lmp.auth.domain.User;
import com.lmp.auth.service.UserService;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.repository.SupportSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignalingControllerTest {

    private SimpMessagingTemplate broker;
    private SupportSessionRepository repo;
    private UserService userService;
    private SignalingController controller;

    private UUID techId;
    private UUID clientId;

    @BeforeEach
    void setup() {
        broker = mock(SimpMessagingTemplate.class);
        repo = mock(SupportSessionRepository.class);
        userService = mock(UserService.class);
        controller = new SignalingController(broker, repo, userService);

        techId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        User tech = new User(); tech.setId(techId);
        User client = new User(); client.setId(clientId);
        when(userService.findByLogin("tech@lmp.ca")).thenReturn(Optional.of(tech));
        when(userService.findByLogin("client@lmp.ca")).thenReturn(Optional.of(client));
    }

    private SupportSession activeSession(UUID id) {
        SupportSession s = new SupportSession();
        s.setId(id);
        s.setStatus(SessionStatus.ACTIVE);
        s.setTechUserId(techId);
        s.setClientUserId(clientId);
        return s;
    }

    private Principal principalFor(String login) {
        Principal p = mock(Principal.class);
        when(p.getName()).thenReturn(login);
        return p;
    }

    @Test
    void techToClientForwardsToClientTopic() {
        UUID sid = UUID.randomUUID();
        when(repo.findById(sid)).thenReturn(Optional.of(activeSession(sid)));

        SignalingMessage msg = new SignalingMessage(sid, "offer", "tech-to-client", "v=0...");
        controller.techToClient(sid, msg, principalFor("tech@lmp.ca"));

        verify(broker).convertAndSend(eq("/topic/signaling/" + sid + "/client"), eq(msg));
    }

    @Test
    void clientToTechForwardsToTechTopic() {
        UUID sid = UUID.randomUUID();
        when(repo.findById(sid)).thenReturn(Optional.of(activeSession(sid)));

        SignalingMessage msg = new SignalingMessage(sid, "answer", "client-to-tech", "v=0...");
        controller.clientToTech(sid, msg, principalFor("client@lmp.ca"));

        verify(broker).convertAndSend(eq("/topic/signaling/" + sid + "/tech"), eq(msg));
    }

    @Test
    void consentWaitIsAlsoSignalingCapable() {
        UUID sid = UUID.randomUUID();
        SupportSession s = activeSession(sid);
        s.setStatus(SessionStatus.CONSENT_WAIT);
        when(repo.findById(sid)).thenReturn(Optional.of(s));

        SignalingMessage msg = new SignalingMessage(sid, "ice-candidate", "tech-to-client", "{}");
        controller.techToClient(sid, msg, principalFor("tech@lmp.ca"));

        verify(broker).convertAndSend(eq("/topic/signaling/" + sid + "/client"), any(Object.class));
    }

    @Test
    void rejectsWhenSessionNotFound() {
        UUID sid = UUID.randomUUID();
        when(repo.findById(sid)).thenReturn(Optional.empty());

        SignalingMessage msg = new SignalingMessage(sid, "offer", "tech-to-client", "x");
        assertThatThrownBy(() -> controller.techToClient(sid, msg, principalFor("tech@lmp.ca")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");

        verify(broker, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void rejectsWhenSessionDraft() {
        UUID sid = UUID.randomUUID();
        SupportSession s = activeSession(sid);
        s.setStatus(SessionStatus.DRAFT);
        when(repo.findById(sid)).thenReturn(Optional.of(s));

        SignalingMessage msg = new SignalingMessage(sid, "offer", "tech-to-client", "x");
        assertThatThrownBy(() -> controller.techToClient(sid, msg, principalFor("tech@lmp.ca")))
            .isInstanceOf(IllegalStateException.class);

        verify(broker, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void rejectsWhenSessionArchived() {
        UUID sid = UUID.randomUUID();
        SupportSession s = activeSession(sid);
        s.setStatus(SessionStatus.ARCHIVED);
        when(repo.findById(sid)).thenReturn(Optional.of(s));

        SignalingMessage msg = new SignalingMessage(sid, "offer", "client-to-tech", "x");
        assertThatThrownBy(() -> controller.clientToTech(sid, msg, principalFor("client@lmp.ca")))
            .isInstanceOf(IllegalStateException.class);

        verify(broker, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void techToClientRejectedWhenCallerIsNotTech() {
        UUID sid = UUID.randomUUID();
        when(repo.findById(sid)).thenReturn(Optional.of(activeSession(sid)));

        // client tries to publish on the tech-to-client topic
        SignalingMessage msg = new SignalingMessage(sid, "offer", "tech-to-client", "x");
        assertThatThrownBy(() -> controller.techToClient(sid, msg, principalFor("client@lmp.ca")))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("not the tech");

        verify(broker, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void clientToTechRejectedWhenCallerIsNotClient() {
        UUID sid = UUID.randomUUID();
        when(repo.findById(sid)).thenReturn(Optional.of(activeSession(sid)));

        SignalingMessage msg = new SignalingMessage(sid, "answer", "client-to-tech", "x");
        assertThatThrownBy(() -> controller.clientToTech(sid, msg, principalFor("tech@lmp.ca")))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("not the client");
    }

    @Test
    void rejectsWhenPrincipalUnknown() {
        UUID sid = UUID.randomUUID();
        when(repo.findById(sid)).thenReturn(Optional.of(activeSession(sid)));
        when(userService.findByLogin("stranger@lmp.ca")).thenReturn(Optional.empty());

        SignalingMessage msg = new SignalingMessage(sid, "offer", "tech-to-client", "x");
        assertThatThrownBy(() -> controller.techToClient(sid, msg, principalFor("stranger@lmp.ca")))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Unknown principal");
    }

    @Test
    void rejectsWhenPrincipalNull() {
        UUID sid = UUID.randomUUID();
        when(repo.findById(sid)).thenReturn(Optional.of(activeSession(sid)));

        SignalingMessage msg = new SignalingMessage(sid, "offer", "tech-to-client", "x");
        assertThatThrownBy(() -> controller.techToClient(sid, msg, null))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void thirdPartyRejectedEvenIfAuthenticated() {
        UUID sid = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        User stranger = new User(); stranger.setId(strangerId);
        when(userService.findByLogin("stranger@lmp.ca")).thenReturn(Optional.of(stranger));
        when(repo.findById(sid)).thenReturn(Optional.of(activeSession(sid)));

        SignalingMessage msg = new SignalingMessage(sid, "offer", "tech-to-client", "x");
        assertThatThrownBy(() -> controller.techToClient(sid, msg, principalFor("stranger@lmp.ca")))
            .isInstanceOf(SecurityException.class);
    }
}
