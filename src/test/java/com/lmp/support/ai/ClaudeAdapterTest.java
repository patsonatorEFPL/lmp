package com.lmp.support.ai;

import com.lmp.support.exec.CommandRiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClaudeAdapterTest {

    private ClaudeClient client;
    private CostCalculator costCalculator;
    private ClaudeAdapter adapter;

    @BeforeEach
    void setup() {
        client = mock(ClaudeClient.class);
        costCalculator = new CostCalculator();
        adapter = new ClaudeAdapter(client, costCalculator);
    }

    @Test
    void enrichesRawSuggestionWithCost() {
        when(client.proposeCommand(any(), any())).thenReturn(new ClaudeClient.RawSuggestion(
            "Get-Process", "LOW", "Lists running processes",
            120, 80, "claude-opus-4-7"));

        AiSuggestion s = adapter.suggest("Show processes", "ctx");

        assertThat(s.suggestedCommand()).isEqualTo("Get-Process");
        assertThat(s.llmRiskLevel()).isEqualTo(CommandRiskLevel.LOW);
        assertThat(s.rationale()).isEqualTo("Lists running processes");
        assertThat(s.tokensInput()).isEqualTo(120);
        assertThat(s.tokensOutput()).isEqualTo(80);
        // 120*15/1M + 80*75/1M = 0.0018 + 0.006 = 0.0078
        assertThat(s.costUsd()).isEqualByComparingTo(new BigDecimal("0.007800"));
    }

    @Test
    void unknownRiskLabelFallsBackToMedium() {
        when(client.proposeCommand(any(), any())).thenReturn(new ClaudeClient.RawSuggestion(
            "Get-Process", "WHATEVER", "ok", 10, 10, "claude-sonnet-4-6"));

        AiSuggestion s = adapter.suggest("p", "c");
        assertThat(s.llmRiskLevel()).isEqualTo(CommandRiskLevel.MEDIUM);
    }

    @Test
    void llmCannotSelfDeclareBlocked() {
        when(client.proposeCommand(any(), any())).thenReturn(new ClaudeClient.RawSuggestion(
            "format C:", "BLOCKED", "destructive", 10, 10, "claude-sonnet-4-6"));

        // Backend owns BLOCKED — LLM claiming it gets normalised to HIGH
        AiSuggestion s = adapter.suggest("p", "c");
        assertThat(s.llmRiskLevel()).isEqualTo(CommandRiskLevel.HIGH);
    }

    @Test
    void caseInsensitiveRiskLabel() {
        when(client.proposeCommand(any(), any())).thenReturn(new ClaudeClient.RawSuggestion(
            "Stop-Service", "high", "kills service", 10, 10, "claude-sonnet-4-6"));

        AiSuggestion s = adapter.suggest("p", "c");
        assertThat(s.llmRiskLevel()).isEqualTo(CommandRiskLevel.HIGH);
    }

    @Test
    void nullRationaleNormalisedToEmpty() {
        when(client.proposeCommand(any(), any())).thenReturn(new ClaudeClient.RawSuggestion(
            "Get-Date", "LOW", null, 5, 5, "claude-haiku-4-5-20251001"));

        AiSuggestion s = adapter.suggest("time?", "c");
        assertThat(s.rationale()).isEmpty();
    }

    @Test
    void rejectsBlankUserPrompt() {
        assertThatThrownBy(() -> adapter.suggest("", "c"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.suggest("  ", "c"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.suggest(null, "c"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullClientResponse() {
        when(client.proposeCommand(any(), any())).thenReturn(null);
        assertThatThrownBy(() -> adapter.suggest("p", "c"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsBlankCommandFromClient() {
        when(client.proposeCommand(any(), any())).thenReturn(new ClaudeClient.RawSuggestion(
            "", "LOW", "r", 1, 1, "claude-sonnet-4-6"));
        assertThatThrownBy(() -> adapter.suggest("p", "c"))
            .isInstanceOf(IllegalStateException.class);
    }
}
