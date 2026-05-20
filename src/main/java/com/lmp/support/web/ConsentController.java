package com.lmp.support.web;

import com.lmp.auth.domain.User;
import com.lmp.auth.service.UserService;
import com.lmp.support.domain.ConsentRecord;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.service.ConsentService;
import com.lmp.support.service.SupportSessionService;
import com.lmp.support.web.dto.ConsentRequest;
import com.lmp.support.web.dto.ConsentResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Client/runner-side consent submission. The caller MUST be the session's
 * {@code clientUserId} — techs cannot submit consent on the client's behalf.
 *
 * Only legal on a session currently in {@code INVITED}: the underlying
 * {@code ConsentService.record} call drives the state machine to
 * {@code CONSENT_WAIT} (accepted) or {@code ABORTED} (refused).
 */
@RestController
@RequestMapping("/api/v1/support/sessions")
public class ConsentController {

    private final ConsentService consentService;
    private final SupportSessionService sessionService;
    private final UserService userService;

    public ConsentController(ConsentService consentService,
                             SupportSessionService sessionService,
                             UserService userService) {
        this.consentService = consentService;
        this.sessionService = sessionService;
        this.userService = userService;
    }

    @PostMapping("/{id}/consent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConsentResponse> submit(@PathVariable UUID id,
                                                  @RequestBody ConsentRequest req,
                                                  Authentication authentication,
                                                  HttpServletRequest http) {
        if (req == null || req.consentTextHash() == null || req.consentTextHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "consentTextHash required");
        }
        SupportSession s = sessionService.findById(id);
        UUID caller = resolveCallerId(authentication);
        if (!caller.equals(s.getClientUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the session's client can submit consent");
        }
        if (s.getStatus() != SessionStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Consent only accepted on INVITED session; current=" + s.getStatus());
        }

        String ip = http == null ? null : extractClientIp(http);
        String ua = http == null ? null : http.getHeader("User-Agent");

        ConsentRecord rec = consentService.record(
            id, req.accepted(), req.itemsConsented(),
            ip, ua, req.consentTextHash());

        return ResponseEntity.status(HttpStatus.CREATED).body(ConsentResponse.of(rec));
    }

    private UUID resolveCallerId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        User u = userService.findByLogin(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Unknown principal: " + authentication.getName()));
        return u.getId();
    }

    private static String extractClientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return http.getRemoteAddr();
    }
}
