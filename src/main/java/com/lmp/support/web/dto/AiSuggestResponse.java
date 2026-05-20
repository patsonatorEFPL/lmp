package com.lmp.support.web.dto;

import com.lmp.support.ai.AiSuggestionOutcome;

import java.math.BigDecimal;

/**
 * Tech UI payload for one AI suggestion.
 *
 * Backend ground-truth fields ({@code backendRiskLevel}, {@code backendBlocked},
 * {@code requiresApproval}, {@code backendReason}) must drive the UI's
 * execute/approve button logic — {@code llmRiskLevel} is informational.
 */
public record AiSuggestResponse(
    boolean denied,
    String denialReason,

    String suggestedCommand,
    String llmRiskLevel,
    String rationale,
    int tokensInput,
    int tokensOutput,
    BigDecimal costUsd,

    String backendRiskLevel,
    boolean backendBlocked,
    boolean requiresApproval,
    String backendReason
) {
    public static AiSuggestResponse from(AiSuggestionOutcome out) {
        if (out.denied()) {
            return new AiSuggestResponse(
                true, out.denialReason(),
                null, null, null, 0, 0, null,
                null, false, false, null
            );
        }
        return new AiSuggestResponse(
            false, null,
            out.suggestion().suggestedCommand(),
            out.suggestion().llmRiskLevel().name(),
            out.suggestion().rationale(),
            out.suggestion().tokensInput(),
            out.suggestion().tokensOutput(),
            out.suggestion().costUsd(),
            out.backendValidation().riskLevel().name(),
            out.backendValidation().blocked(),
            out.backendValidation().requiresApproval(),
            out.backendValidation().reason()
        );
    }
}
