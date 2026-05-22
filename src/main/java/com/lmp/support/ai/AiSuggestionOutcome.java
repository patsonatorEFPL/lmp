package com.lmp.support.ai;

import com.lmp.support.exec.CommandValidationResult;

/**
 * Combined result of an AI suggestion + the independent backend re-validation.
 *
 * The tech UI surfaces both: Claude's self-reported risk lets the tech see what
 * the model thought, and {@code backendValidation} is the ground truth — if
 * {@code backendValidation.blocked()} is true the command CANNOT be executed,
 * regardless of what the LLM said.
 *
 * If {@code denied} is true the call was rejected before even calling Claude
 * (rate limit / token cap). In that case {@code suggestion} and
 * {@code backendValidation} are null.
 */
public record AiSuggestionOutcome(
    AiSuggestion suggestion,
    CommandValidationResult backendValidation,
    boolean denied,
    String denialReason
) {
    public static AiSuggestionOutcome denied(String reason) {
        return new AiSuggestionOutcome(null, null, true, reason);
    }

    public static AiSuggestionOutcome accepted(AiSuggestion s, CommandValidationResult v) {
        return new AiSuggestionOutcome(s, v, false, null);
    }
}
