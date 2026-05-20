package com.lmp.support.meshcentral.event;

import com.lmp.support.domain.SessionStatus;
import com.lmp.support.meshcentral.MeshCentralResponse;
import com.lmp.support.meshcentral.MeshCentralWebSocketClient.RawMeshCentralEvent;
import com.lmp.support.repository.SupportSessionRepository;
import com.lmp.support.service.SupportSessionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges MeshCentral agent lifecycle events into the LMP session state machine.
 *
 * <pre>
 *   nodeconnect    + session CONSENT_WAIT  → transition to ACTIVE + publish AgentConnectedEvent
 *   nodedisconnect + session ACTIVE        → publish AgentDisconnectedEvent (no auto-transition;
 *                                            scheduler / controller decides whether to END)
 * </pre>
 */
@Component
public class MeshCentralEventListener {

    private final SupportSessionRepository sessionRepo;
    private final SupportSessionService sessionService;
    private final ApplicationEventPublisher publisher;

    public MeshCentralEventListener(SupportSessionRepository sessionRepo,
                                    SupportSessionService sessionService,
                                    ApplicationEventPublisher publisher) {
        this.sessionRepo = sessionRepo;
        this.sessionService = sessionService;
        this.publisher = publisher;
    }

    @EventListener
    public void onRawEvent(RawMeshCentralEvent raw) {
        MeshCentralResponse r = raw.response();
        String action = r.getAction();
        if (action == null) return;

        Object meshIdObj = r.getExtra().get("meshid");
        Object nodeIdObj = r.getExtra().get("nodeid");
        String meshId = meshIdObj == null ? null : meshIdObj.toString();
        String nodeId = nodeIdObj == null ? null : nodeIdObj.toString();

        switch (action) {
            case "nodeconnect"    -> handleConnect(meshId, nodeId);
            case "nodedisconnect" -> handleDisconnect(meshId, nodeId);
            default               -> { /* ignore */ }
        }
    }

    private void handleConnect(String meshId, String nodeId) {
        if (meshId == null) return;
        sessionRepo.findByMeshCentralGroupIdAndStatus(meshId, SessionStatus.CONSENT_WAIT)
            .ifPresent(s -> {
                if (nodeId != null) {
                    sessionService.attachMeshNode(s.getId(), nodeId);
                }
                sessionService.transition(s.getId(), SessionStatus.ACTIVE, null);
                publisher.publishEvent(new AgentConnectedEvent(s.getId(), meshId, nodeId));
            });
    }

    private void handleDisconnect(String meshId, String nodeId) {
        if (meshId == null) return;
        sessionRepo.findByMeshCentralGroupIdAndStatus(meshId, SessionStatus.ACTIVE)
            .ifPresent(s -> publisher.publishEvent(new AgentDisconnectedEvent(s.getId(), meshId, nodeId)));
    }
}
