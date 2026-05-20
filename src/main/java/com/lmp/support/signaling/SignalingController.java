package com.lmp.support.signaling;

import com.lmp.auth.domain.User;
import com.lmp.auth.service.UserService;
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
 * STOMP relay for WebRTC signaling. Two gates:
 * <ol>
 *   <li>Session must be in a signaling-capable state ({@code CONSENT_WAIT} or {@code ACTIVE}).</li>
 *   <li>Principal must be the tech or the client for the session — without this any
 *   authenticated user could publish offers/ICE into someone else's session.</li>
 * </ol>
 *
 * <pre>
 *   tech client publishes /app/signaling/{id}/tech-to-client  → /topic/signaling/{id}/client
 *   client side publishes /app/signaling/{id}/client-to-tech  → /topic/signaling/{id}/tech
 * </pre>
 */
@Controller
public class SignalingController {

    private final SimpMessagingTemplate broker;
    private final SupportSessionRepository sessionRepo;
    private final UserService userService;

    public SignalingController(SimpMessagingTemplate broker,
                               SupportSessionRepository sessionRepo,
                               UserService userService) {
        this.broker = broker;
        this.sessionRepo = sessionRepo;
        this.userService = userService;
    }

    @MessageMapping("/signaling/{sessionId}/tech-to-client")
    public void techToClient(@DestinationVariable UUID sessionId,
                             @Payload SignalingMessage msg,
                             Principal principal) {
        SupportSession s = validateSignalingState(sessionId);
        UUID caller = resolveCallerId(principal);
        if (!caller.equals(s.getTechUserId())) {
            throw new SecurityException("Caller is not the tech for session " + sessionId);
        }
        broker.convertAndSend("/topic/signaling/" + sessionId + "/client", msg);
    }

    @MessageMapping("/signaling/{sessionId}/client-to-tech")
    public void clientToTech(@DestinationVariable UUID sessionId,
                             @Payload SignalingMessage msg,
                             Principal principal) {
        SupportSession s = validateSignalingState(sessionId);
        UUID caller = resolveCallerId(principal);
        if (!caller.equals(s.getClientUserId())) {
            throw new SecurityException("Caller is not the client for session " + sessionId);
        }
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

    private UUID resolveCallerId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new SecurityException("Not authenticated");
        }
        User u = userService.findByLogin(principal.getName())
            .orElseThrow(() -> new SecurityException("Unknown principal: " + principal.getName()));
        return u.getId();
    }
}
