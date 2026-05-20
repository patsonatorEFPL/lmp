package com.lmp.support.ai;

import com.lmp.support.exec.CommandAllowlistConfig;
import com.lmp.support.exec.CommandRiskLevel;
import com.lmp.support.exec.CommandValidationResult;
import com.lmp.support.exec.CommandValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSuggestionServiceTest {

    private AiBudgetService budget;
    private ClaudeAdapter adapter;
    private CommandValidator validator;
    private AiSuggestionService service;

    @BeforeEach
    void setup() {
        budget = mock(AiBudgetService.class);
        adapter = mock(ClaudeAdapter.class);
        validator = new CommandValidator(new CommandAllowlistConfig());
        service = new AiSuggestionService(budget, adapter, validator);
    }

    @Test
    void happyPathReturnsBothSuggestionAndValidation() {
        UUID sid = UUID.randomUUID();
        when(budget.tryConsume(eq(sid), anyInt())).thenReturn(true);
        AiSuggestion fake = new AiSuggestion(
            "Get-Process", CommandRiskLevel.LOW, "lists processes",
            100, 50, new BigDecimal("0.0030"));
        when(adapter.suggest(any(), any())).thenReturn(fake);

        AiSuggestionOutcome out = service.suggest(sid, "show processes", "ctx");

        assertThat(out.denied()).isFalse();
        assertThat(out.suggestion()).isSameAs(fake);
        assertThat(out.backendValidation().riskLevel()).isEqualTo(CommandRiskLevel.LOW);
        assertThat(out.backendValidation().blocked()).isFalse();
    }

    @Test
    void budgetRejectionShortCircuitsBeforeClaudeCall() {
        UUID sid = UUID.randomUUID();
        when(budget.tryConsume(eq(sid), anyInt())).thenReturn(false);

        AiSuggestionOutcome out = service.suggest(sid, "anything", "ctx");

        assertThat(out.denied()).isTrue();
        assertThat(out.denialReason()).contains("budget");
        assertThat(out.suggestion()).isNull();
        assertThat(out.backendValidation()).isNull();
        verify(adapter, never()).suggest(any(), any());
    }

    @Test
    void llmSuggestsDangerousCommandBackendBlocksIt() {
        UUID sid = UUID.randomUUID();
        when(budget.tryConsume(eq(sid), anyInt())).thenReturn(true);
        // LLM mislabels a denylisted command as MEDIUM — backend overrides
        AiSuggestion fake = new AiSuggestion(
            "bcdedit /set testsigning on", CommandRiskLevel.MEDIUM, "boot config",
            100, 50, new BigDecimal("0.0030"));
        when(adapter.suggest(any(), any())).thenReturn(fake);

        AiSuggestionOutcome out = service.suggest(sid, "boot test mode", "ctx");

        assertThat(out.denied()).isFalse();
        assertThat(out.suggestion().llmRiskLevel()).isEqualTo(CommandRiskLevel.MEDIUM);
        // Ground truth: BLOCKED regardless of LLM claim
        assertThat(out.backendValidation().blocked()).isTrue();
        assertThat(out.backendValidation().riskLevel()).isEqualTo(CommandRiskLevel.BLOCKED);
    }

    @Test
    void highRiskCommandFlaggedRequiringApproval() {
        UUID sid = UUID.randomUUID();
        when(budget.tryConsume(eq(sid), anyInt())).thenReturn(true);
        AiSuggestion fake = new AiSuggestion(
            "Stop-Service -Name Spooler", CommandRiskLevel.MEDIUM, "stops print spooler",
            100, 50, new BigDecimal("0.0030"));
        when(adapter.suggest(any(), any())).thenReturn(fake);

        AiSuggestionOutcome out = service.suggest(sid, "stop spooler", "ctx");

        assertThat(out.backendValidation().riskLevel()).isEqualTo(CommandRiskLevel.HIGH);
        assertThat(out.backendValidation().requiresApproval()).isTrue();
        assertThat(out.backendValidation().blocked()).isFalse();
    }

    @Test
    void tokenEstimateScalesWithPromptLength() {
        UUID sid = UUID.randomUUID();
        when(budget.tryConsume(eq(sid), anyInt())).thenReturn(true);
        AiSuggestion fake = new AiSuggestion(
            "Get-Date", CommandRiskLevel.LOW, "r", 1, 1, BigDecimal.ZERO);
        when(adapter.suggest(any(), any())).thenReturn(fake);

        String shortPrompt = "a";
        String longPrompt = "x".repeat(4000);  // ~1000 tokens
        service.suggest(sid, shortPrompt, "");
        service.suggest(sid, longPrompt, "");

        // Capture both consumption calls
        org.mockito.ArgumentCaptor<Integer> captor = org.mockito.ArgumentCaptor.forClass(Integer.class);
        verify(budget, org.mockito.Mockito.times(2)).tryConsume(eq(sid), captor.capture());
        java.util.List<Integer> consumed = captor.getAllValues();
        assertThat(consumed.get(1)).isGreaterThan(consumed.get(0));
    }

    @Test
    void rejectsBlankPrompt() {
        UUID sid = UUID.randomUUID();
        assertThatThrownBy(() -> service.suggest(sid, "", "ctx"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.suggest(sid, "  ", "ctx"))
            .isInstanceOf(IllegalArgumentException.class);
        verify(budget, never()).tryConsume(any(), anyInt());
    }

    @Test
    void rejectsNullSessionId() {
        assertThatThrownBy(() -> service.suggest(null, "p", "c"))
            .isInstanceOf(IllegalArgumentException.class);
        verify(budget, never()).tryConsume(any(), anyInt());
    }

    @Test
    void nullContextHandled() {
        UUID sid = UUID.randomUUID();
        when(budget.tryConsume(eq(sid), anyInt())).thenReturn(true);
        AiSuggestion fake = new AiSuggestion(
            "Get-Process", CommandRiskLevel.LOW, "r", 1, 1, BigDecimal.ZERO);
        when(adapter.suggest(any(), any())).thenReturn(fake);

        AiSuggestionOutcome out = service.suggest(sid, "show", null);

        assertThat(out.denied()).isFalse();
    }
}
