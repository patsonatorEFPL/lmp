package com.lmp.support.signaling;

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
    private SignalingController controller;
    private Principal principal;

    @BeforeEach
    void setup() {
        broker = mock(SimpMessagingTemplate.class);
        repo = mock(SupportSessionRepository.class);
        controller = new SignalingController(broker, repo);
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("tech@lmp.ca");
    }

    @Test
    void techToClientForwardsToClientTopicOnActiveSession() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession();
        s.setId(sid);
        s.setStatus(SessionStatus.ACTIVE);
        when(repo.findById(sid)).thenReturn(Optional.of(s));

        SignalingMessage msg = new SignalingMessage(sid, "offer", "tech-to-client", "v=0...");
        controller.techToClient(sid, msg, principal);

        verify(broker).convertAndSend(eq("/topic/signaling/" + sid + "/client"), eq(msg));
    }

    @Test
    void clientToTechForwardsToTechTopic() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession();
        s.setId(sid);
        s.setStatus(SessionStatus.ACTIVE);
        when(repo.findById(sid)).thenReturn(Optional.of(s));

        SignalingMessage msg = new SignalingMessage(sid, "answer", "client-to-tech", "v=0...");
        controller.clientToTech(sid, msg, principal);

        verify(broker).convertAndSend(eq("/topic/signaling/" + sid + "/tech"), eq(msg));
    }

    @Test
    void consentWaitIsAlsoSignalingCapable() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession();
        s.setId(sid);
        s.setStatus(SessionStatus.CONSENT_WAIT);
        when(repo.findById(sid)).thenReturn(Optional.of(s));

        SignalingMessage msg = new SignalingMessage(sid, "ice-candidate", "tech-to-client", "{}");
        controller.techToClient(sid, msg, principal);

        verify(broker).convertAndSend(eq("/topic/signaling/" + sid + "/client"), any(Object.class));
    }

    @Test
    void rejectsWhenSessionNotFound() {
        UUID sid = UUID.randomUUID();
        when(repo.findById(sid)).thenReturn(Optional.empty());

        SignalingMessage msg = new SignalingMessage(sid, "offer", "tech-to-client", "x");
        assertThatThrownBy(() -> controller.techToClient(sid, msg, principal))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");

        verify(broker, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void rejectsWhenSessionDraft() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession();
        s.setId(sid);
        s.setStatus(SessionStatus.DRAFT);
        when(repo.findById(sid)).thenReturn(Optional.of(s));

        SignalingMessage msg = new SignalingMessage(sid, "offer", "tech-to-client", "x");
        assertThatThrownBy(() -> controller.techToClient(sid, msg, principal))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not in signaling state");

        verify(broker, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void rejectsWhenSessionArchived() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession();
        s.setId(sid);
        s.setStatus(SessionStatus.ARCHIVED);
        when(repo.findById(sid)).thenReturn(Optional.of(s));

        SignalingMessage msg = new SignalingMessage(sid, "offer", "client-to-tech", "x");
        assertThatThrownBy(() -> controller.clientToTech(sid, msg, principal))
            .isInstanceOf(IllegalStateException.class);

        verify(broker, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
