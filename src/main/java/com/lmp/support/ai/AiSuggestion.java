package com.lmp.support.ai;

import com.lmp.support.exec.CommandRiskLevel;

import java.math.BigDecimal;

/**
 * Structured Claude tool_use response for one HelpDesk command suggestion.
 *
 * <ul>
 *   <li>{@code llmRiskLevel} is what Claude self-reported; backend MUST re-validate
 *   independently via {@code CommandValidator} before allowing execution.</li>
 *   <li>{@code costUsd} is computed by {@code CostCalculator} from token usage and
 *   the model's per-1M pricing tier.</li>
 * </ul>
 */
public record AiSuggestion(
    String suggestedCommand,
    CommandRiskLevel llmRiskLevel,
    String rationale,
    int tokensInput,
    int tokensOutput,
    BigDecimal costUsd
) {}
