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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsentControllerTest {

    private ConsentService consentService;
    private SupportSessionService sessionService;
    private UserService userService;
    private ConsentController controller;

    private UUID clientId;
    private UUID techId;

    @BeforeEach
    void setup() {
        consentService = mock(ConsentService.class);
        sessionService = mock(SupportSessionService.class);
        userService = mock(UserService.class);
        controller = new ConsentController(consentService, sessionService, userService);

        clientId = UUID.randomUUID();
        techId = UUID.randomUUID();

        User client = new User(); client.setId(clientId);
        User tech = new User(); tech.setId(techId);
        when(userService.findByLogin("client@lmp.ca")).thenReturn(Optional.of(client));
        when(userService.findByLogin("tech@lmp.ca")).thenReturn(Optional.of(tech));
    }

    private SupportSession invitedSession(UUID id) {
        SupportSession s = new SupportSession();
        s.setId(id);
        s.setTechUserId(techId);
        s.setClientUserId(clientId);
        s.setStatus(SessionStatus.INVITED);
        return s;
    }

    private Authentication auth(String login) {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn(login);
        return a;
    }

    private HttpServletRequest req(String xff, String remote, String ua) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getHeader("X-Forwarded-For")).thenReturn(xff);
        when(r.getRemoteAddr()).thenReturn(remote);
        when(r.getHeader("User-Agent")).thenReturn(ua);
        return r;
    }

    @Test
    void acceptedConsentDelegatesToServiceAndReturns201() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid)).thenReturn(invitedSession(sid));

        ConsentRecord rec = new ConsentRecord();
        rec.setId(UUID.randomUUID());
        rec.setSessionId(sid);
        rec.setAccepted(true);
        rec.setOccurredAt(LocalDateTime.now());
        when(consentService.record(eq(sid), eq(true), any(), any(), any(), eq("hash123")))
            .thenReturn(rec);

        ConsentRequest body = new ConsentRequest(
            true, List.of("screen", "voice"), "hash123");
        ResponseEntity<ConsentResponse> resp = controller.submit(
            sid, body, auth("client@lmp.ca"),
            req("203.0.113.5", "127.0.0.1", "Mozilla/5.0"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().accepted()).isTrue();
        assertThat(resp.getBody().sessionId()).isEqualTo(sid);

        verify(consentService).record(eq(sid), eq(true),
            eq(List.of("screen", "voice")),
            eq("203.0.113.5"),  // XFF first hop wins over remoteAddr
            eq("Mozilla/5.0"),
            eq("hash123"));
    }

    @Test
    void refusedConsentDelegatesWithFalse() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid)).thenReturn(invitedSession(sid));

        ConsentRecord rec = new ConsentRecord();
        rec.setId(UUID.randomUUID());
        rec.setSessionId(sid);
        rec.setAccepted(false);
        rec.setOccurredAt(LocalDateTime.now());
        when(consentService.record(eq(sid), eq(false), any(), any(), any(), eq("h")))
            .thenReturn(rec);

        ResponseEntity<ConsentResponse> resp = controller.submit(
            sid, new ConsentRequest(false, List.of(), "h"),
            auth("client@lmp.ca"), req(null, "127.0.0.1", "ua"));

        assertThat(resp.getBody().accepted()).isFalse();
    }

    @Test
    void techCannotSubmitConsentForClient() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid)).thenReturn(invitedSession(sid));

        assertThatThrownBy(() -> controller.submit(
            sid, new ConsentRequest(true, List.of(), "h"),
            auth("tech@lmp.ca"), req(null, "x", "ua")))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void rejects409WhenSessionNotInvited() {
        UUID sid = UUID.randomUUID();
        SupportSession s = invitedSession(sid);
        s.setStatus(SessionStatus.ACTIVE);
        when(sessionService.findById(sid)).thenReturn(s);

        assertThatThrownBy(() -> controller.submit(
            sid, new ConsentRequest(true, List.of(), "h"),
            auth("client@lmp.ca"), req(null, "x", "ua")))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void rejects400WhenBodyMissing() {
        UUID sid = UUID.randomUUID();
        assertThatThrownBy(() -> controller.submit(
            sid, null, auth("client@lmp.ca"), req(null, "x", "ua")))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejects400WhenConsentTextHashBlank() {
        UUID sid = UUID.randomUUID();
        assertThatThrownBy(() -> controller.submit(
            sid, new ConsentRequest(true, List.of(), "  "),
            auth("client@lmp.ca"), req(null, "x", "ua")))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejects401WhenAuthenticationNull() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid)).thenReturn(invitedSession(sid));

        assertThatThrownBy(() -> controller.submit(
            sid, new ConsentRequest(true, List.of(), "h"),
            null, req(null, "x", "ua")))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void xffWithMultipleHopsTakesFirst() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid)).thenReturn(invitedSession(sid));
        ConsentRecord rec = new ConsentRecord();
        rec.setId(UUID.randomUUID());
        rec.setSessionId(sid);
        rec.setAccepted(true);
        rec.setOccurredAt(LocalDateTime.now());
        when(consentService.record(any(), any(boolean.class), any(), any(), any(), any())).thenReturn(rec);

        controller.submit(sid, new ConsentRequest(true, List.of(), "h"),
            auth("client@lmp.ca"),
            req("203.0.113.5, 10.0.0.1, 10.0.0.2", "127.0.0.1", "ua"));

        verify(consentService).record(eq(sid), eq(true), any(), eq("203.0.113.5"), any(), eq("h"));
    }

    @Test
    void fallsBackToRemoteAddrWhenNoXff() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid)).thenReturn(invitedSession(sid));
        ConsentRecord rec = new ConsentRecord();
        rec.setId(UUID.randomUUID());
        rec.setSessionId(sid);
        rec.setAccepted(true);
        rec.setOccurredAt(LocalDateTime.now());
        when(consentService.record(any(), any(boolean.class), any(), any(), any(), any())).thenReturn(rec);

        controller.submit(sid, new ConsentRequest(true, List.of(), "h"),
            auth("client@lmp.ca"),
            req(null, "192.168.1.42", "ua"));

        verify(consentService).record(eq(sid), eq(true), any(), eq("192.168.1.42"), any(), eq("h"));
    }
}
