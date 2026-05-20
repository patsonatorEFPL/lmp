package com.lmp.support.meshcentral.event;

import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.meshcentral.MeshCentralResponse;
import com.lmp.support.meshcentral.MeshCentralWebSocketClient.RawMeshCentralEvent;
import com.lmp.support.repository.SupportSessionRepository;
import com.lmp.support.service.SupportSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeshCentralEventListenerTest {

    private SupportSessionRepository repo;
    private SupportSessionService sessionService;
    private ApplicationEventPublisher pub;
    private MeshCentralEventListener listener;

    @BeforeEach
    void setup() {
        repo = mock(SupportSessionRepository.class);
        sessionService = mock(SupportSessionService.class);
        pub = mock(ApplicationEventPublisher.class);
        listener = new MeshCentralEventListener(repo, sessionService, pub);
    }

    @Test
    void nodeConnectTransitionsConsentWaitSessionToActive() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession();
        s.setId(sid);
        s.setMeshCentralGroupId("mesh//abc");
        s.setStatus(SessionStatus.CONSENT_WAIT);
        when(repo.findByMeshCentralGroupIdAndStatus("mesh//abc", SessionStatus.CONSENT_WAIT))
            .thenReturn(Optional.of(s));

        MeshCentralResponse r = new MeshCentralResponse();
        r.setAction("nodeconnect");
        r.setExtra("meshid", "mesh//abc");
        r.setExtra("nodeid", "node//xyz");

        listener.onRawEvent(new RawMeshCentralEvent(r));

        verify(sessionService).attachMeshNode(eq(sid), eq("node//xyz"));
        verify(sessionService).transition(eq(sid), eq(SessionStatus.ACTIVE), eq(null));
        verify(pub).publishEvent(any(AgentConnectedEvent.class));
    }

    @Test
    void nodeConnectIgnoredWhenNoMatchingSession() {
        when(repo.findByMeshCentralGroupIdAndStatus(any(), any())).thenReturn(Optional.empty());

        MeshCentralResponse r = new MeshCentralResponse();
        r.setAction("nodeconnect");
        r.setExtra("meshid", "mesh//unknown");

        listener.onRawEvent(new RawMeshCentralEvent(r));

        verify(sessionService, never()).transition(any(), any(), any());
        verify(pub, never()).publishEvent(any());
    }

    @Test
    void nodeDisconnectPublishesEventButDoesNotTransition() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession();
        s.setId(sid);
        s.setMeshCentralGroupId("mesh//abc");
        s.setStatus(SessionStatus.ACTIVE);
        when(repo.findByMeshCentralGroupIdAndStatus("mesh//abc", SessionStatus.ACTIVE))
            .thenReturn(Optional.of(s));

        MeshCentralResponse r = new MeshCentralResponse();
        r.setAction("nodedisconnect");
        r.setExtra("meshid", "mesh//abc");
        r.setExtra("nodeid", "node//xyz");

        listener.onRawEvent(new RawMeshCentralEvent(r));

        verify(pub).publishEvent(any(AgentDisconnectedEvent.class));
        verify(sessionService, never()).transition(any(), any(), any());
    }

    @Test
    void unknownActionIgnored() {
        MeshCentralResponse r = new MeshCentralResponse();
        r.setAction("ping");

        listener.onRawEvent(new RawMeshCentralEvent(r));

        verify(sessionService, never()).transition(any(), any(), any());
        verify(pub, never()).publishEvent(any());
    }

    @Test
    void nullActionIgnored() {
        MeshCentralResponse r = new MeshCentralResponse();
        // no action set
        listener.onRawEvent(new RawMeshCentralEvent(r));

        verify(sessionService, never()).transition(any(), any(), any());
    }
}
