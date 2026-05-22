package com.lmp.support.ai;

import com.lmp.support.exec.CommandRiskLevel;
import org.springframework.stereotype.Service;

/**
 * Orchestrates one AI-assisted command suggestion: delegates the live API call
 * to a {@link ClaudeClient} port, then enriches the raw response with cost.
 *
 * Backend re-validation of the suggested command happens upstream of this class
 * (see {@code CommandValidator}). This adapter does NOT trust the LLM's
 * self-reported risk — it only normalises it for storage.
 */
@Service
public class ClaudeAdapter {

    private final ClaudeClient client;
    private final CostCalculator costCalculator;

    public ClaudeAdapter(ClaudeClient client, CostCalculator costCalculator) {
        this.client = client;
        this.costCalculator = costCalculator;
    }

    public AiSuggestion suggest(String userPrompt, String sessionContext) {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("userPrompt required");
        }
        ClaudeClient.RawSuggestion raw = client.proposeCommand(userPrompt, sessionContext);
        if (raw == null) {
            throw new IllegalStateException("ClaudeClient returned null");
        }
        if (raw.command() == null || raw.command().isBlank()) {
            throw new IllegalStateException("LLM did not produce a command");
        }
        CommandRiskLevel level = parseRisk(raw.llmRiskLevel());
        var cost = costCalculator.compute(raw.tokensInput(), raw.tokensOutput(), raw.model());
        return new AiSuggestion(
            raw.command(),
            level,
            raw.rationale() == null ? "" : raw.rationale(),
            raw.tokensInput(),
            raw.tokensOutput(),
            cost
        );
    }

    private static CommandRiskLevel parseRisk(String raw) {
        if (raw == null) return CommandRiskLevel.MEDIUM;
        try {
            CommandRiskLevel parsed = CommandRiskLevel.valueOf(raw.trim().toUpperCase());
            // LLM cannot self-declare BLOCKED — backend owns that classification
            return parsed == CommandRiskLevel.BLOCKED ? CommandRiskLevel.HIGH : parsed;
        } catch (IllegalArgumentException e) {
            // unknown label → conservative default
            return CommandRiskLevel.MEDIUM;
        }
    }
}
