package com.lmp.support.web;

import com.lmp.support.ai.AiSuggestion;
import com.lmp.support.ai.AiSuggestionOutcome;
import com.lmp.support.ai.AiSuggestionService;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.exec.CommandRiskLevel;
import com.lmp.support.exec.CommandValidationResult;
import com.lmp.support.service.SupportSessionService;
import com.lmp.support.web.dto.AiSuggestRequest;
import com.lmp.support.web.dto.AiSuggestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiExecControllerTest {

    private AiSuggestionService suggestionService;
    private SupportSessionService sessionService;
    private AiExecController controller;

    @BeforeEach
    void setup() {
        suggestionService = mock(AiSuggestionService.class);
        sessionService = mock(SupportSessionService.class);
        controller = new AiExecController(suggestionService, sessionService);
    }

    private SupportSession activeSession(UUID id) {
        SupportSession s = new SupportSession();
        s.setId(id);
        s.setStatus(SessionStatus.ACTIVE);
        return s;
    }

    @Test
    void suggestForwardsToServiceAndMapsAccepted() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid)).thenReturn(activeSession(sid));

        AiSuggestion sug = new AiSuggestion(
            "Get-Process", CommandRiskLevel.LOW, "lists processes",
            120, 80, new BigDecimal("0.0078"));
        AiSuggestionOutcome outcome = AiSuggestionOutcome.accepted(
            sug, CommandValidationResult.low("matches allowlist"));
        when(suggestionService.suggest(eq(sid), eq("show processes"), any()))
            .thenReturn(outcome);

        AiSuggestResponse resp = controller.suggest(sid, new AiSuggestRequest("show processes"));

        assertThat(resp.denied()).isFalse();
        assertThat(resp.suggestedCommand()).isEqualTo("Get-Process");
        assertThat(resp.llmRiskLevel()).isEqualTo("LOW");
        assertThat(resp.backendRiskLevel()).isEqualTo("LOW");
        assertThat(resp.backendBlocked()).isFalse();
        assertThat(resp.requiresApproval()).isFalse();
        assertThat(resp.tokensInput()).isEqualTo(120);
        assertThat(resp.costUsd()).isEqualByComparingTo("0.0078");
        verify(suggestionService).suggest(eq(sid), eq("show processes"), any());
    }

    @Test
    void suggestMapsBackendBlockedOverridingLlm() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid)).thenReturn(activeSession(sid));

        // LLM says MEDIUM, backend says BLOCKED
        AiSuggestion sug = new AiSuggestion(
            "bcdedit /set testsigning on", CommandRiskLevel.MEDIUM, "boot config",
            100, 50, new BigDecimal("0.005"));
        AiSuggestionOutcome outcome = AiSuggestionOutcome.accepted(
            sug, CommandValidationResult.blocked("matches denylist"));
        when(suggestionService.suggest(any(), any(), any())).thenReturn(outcome);

        AiSuggestResponse resp = controller.suggest(sid, new AiSuggestRequest("boot test mode"));

        assertThat(resp.denied()).isFalse();
        assertThat(resp.llmRiskLevel()).isEqualTo("MEDIUM");
        assertThat(resp.backendBlocked()).isTrue();
        assertThat(resp.backendRiskLevel()).isEqualTo("BLOCKED");
    }

    @Test
    void suggestMapsDeniedOutcome() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid)).thenReturn(activeSession(sid));
        when(suggestionService.suggest(any(), any(), any()))
            .thenReturn(AiSuggestionOutcome.denied("budget exhausted"));

        AiSuggestResponse resp = controller.suggest(sid, new AiSuggestRequest("any"));

        assertThat(resp.denied()).isTrue();
        assertThat(resp.denialReason()).contains("budget");
        assertThat(resp.suggestedCommand()).isNull();
        assertThat(resp.backendRiskLevel()).isNull();
    }

    @Test
    void rejectsBlankPrompt() {
        UUID sid = UUID.randomUUID();

        assertThatThrownBy(() -> controller.suggest(sid, new AiSuggestRequest("")))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> controller.suggest(sid, new AiSuggestRequest("   ")))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsNullRequest() {
        UUID sid = UUID.randomUUID();
        assertThatThrownBy(() -> controller.suggest(sid, null))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsNullPrompt() {
        UUID sid = UUID.randomUUID();
        assertThatThrownBy(() -> controller.suggest(sid, new AiSuggestRequest(null)))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void requires409WhenSessionNotActive() {
        UUID sid = UUID.randomUUID();
        SupportSession s = activeSession(sid);
        s.setStatus(SessionStatus.CONSENT_WAIT);
        when(sessionService.findById(sid)).thenReturn(s);

        assertThatThrownBy(() -> controller.suggest(sid, new AiSuggestRequest("p")))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void requires409WhenSessionEnded() {
        UUID sid = UUID.randomUUID();
        SupportSession s = activeSession(sid);
        s.setStatus(SessionStatus.ARCHIVED);
        when(sessionService.findById(sid)).thenReturn(s);

        assertThatThrownBy(() -> controller.suggest(sid, new AiSuggestRequest("p")))
            .isInstanceOfSatisfying(ResponseStatusException.class, e ->
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
