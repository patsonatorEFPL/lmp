package com.lmp.support.signaling;

import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.repository.SupportSessionRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP relay for WebRTC signaling. Validates session is in a signaling-capable
 * state ({@code CONSENT_WAIT} or {@code ACTIVE}) before forwarding.
 *
 * <pre>
 *   client publishes /app/signaling/{id}/tech-to-client  → broker fans out to /topic/signaling/{id}/client
 *   client publishes /app/signaling/{id}/client-to-tech  → broker fans out to /topic/signaling/{id}/tech
 * </pre>
 */
@Controller
public class SignalingController {

    private final SimpMessagingTemplate broker;
    private final SupportSessionRepository sessionRepo;

    public SignalingController(SimpMessagingTemplate broker,
                               SupportSessionRepository sessionRepo) {
        this.broker = broker;
        this.sessionRepo = sessionRepo;
    }

    @MessageMapping("/signaling/{sessionId}/tech-to-client")
    public void techToClient(@DestinationVariable UUID sessionId,
                             @Payload SignalingMessage msg,
                             Principal principal) {
        validateSignalingState(sessionId);
        broker.convertAndSend("/topic/signaling/" + sessionId + "/client", msg);
    }

    @MessageMapping("/signaling/{sessionId}/client-to-tech")
    public void clientToTech(@DestinationVariable UUID sessionId,
                             @Payload SignalingMessage msg,
                             Principal principal) {
        validateSignalingState(sessionId);
        broker.convertAndSend("/topic/signaling/" + sessionId + "/tech", msg);
    }

    private SupportSession validateSignalingState(UUID sessionId) {
        SupportSession s = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        SessionStatus st = s.getStatus();
        if (st != SessionStatus.ACTIVE && st != SessionStatus.CONSENT_WAIT) {
            throw new IllegalStateException("Session not in signaling state: " + st);
        }
        return s;
    }
}
