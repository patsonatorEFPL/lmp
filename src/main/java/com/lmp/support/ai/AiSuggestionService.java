package com.lmp.support.ai;

import com.lmp.support.exec.CommandValidationResult;
import com.lmp.support.exec.CommandValidator;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Single entrypoint the tech UI calls to get one AI-assisted command suggestion.
 *
 * Pipeline (each step can short-circuit):
 * <ol>
 *   <li>Reserve budget via {@link AiBudgetService#tryConsume} — token estimate
 *   based on prompt length. If rejected: return DENIED outcome, no Claude call.</li>
 *   <li>Ask Claude via {@link ClaudeAdapter}.</li>
 *   <li>Re-validate the suggested command via {@link CommandValidator} — backend
 *   is the ground truth on safety; LLM's self-reported risk is advisory only.</li>
 *   <li>Return both pieces so the UI can show what the LLM thought AND whether
 *   it's actually allowed.</li>
 * </ol>
 *
 * Note: persistence of the suggestion to {@code support_ai_command} is intentionally
 * deferred — entity + repository belong to the P7 DB tasks that need V44 migration.
 */
@Service
public class AiSuggestionService {

    /** Conservative pre-call estimate: 4 chars ≈ 1 token, plus a fixed overhead for system prompt + tool schema. */
    private static final int SYSTEM_OVERHEAD_TOKENS = 600;
    private static final int CHARS_PER_TOKEN = 4;

    private final AiBudgetService budget;
    private final ClaudeAdapter adapter;
    private final CommandValidator validator;

    public AiSuggestionService(AiBudgetService budget,
                               ClaudeAdapter adapter,
                               CommandValidator validator) {
        this.budget = budget;
        this.adapter = adapter;
        this.validator = validator;
    }

    public AiSuggestionOutcome suggest(UUID sessionId, String userPrompt, String sessionContext) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId required");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("userPrompt required");
        }

        int estimated = estimateTokens(userPrompt, sessionContext);
        if (!budget.tryConsume(sessionId, estimated)) {
            return AiSuggestionOutcome.denied(
                "budget exhausted (rate limit or token cap) for session " + sessionId);
        }

        AiSuggestion suggestion = adapter.suggest(userPrompt, sessionContext);
        CommandValidationResult backend = validator.validate(suggestion.suggestedCommand());
        return AiSuggestionOutcome.accepted(suggestion, backend);
    }

    private int estimateTokens(String prompt, String context) {
        int promptLen = prompt == null ? 0 : prompt.length();
        int ctxLen = context == null ? 0 : context.length();
        return SYSTEM_OVERHEAD_TOKENS + (promptLen + ctxLen) / CHARS_PER_TOKEN;
    }
}
