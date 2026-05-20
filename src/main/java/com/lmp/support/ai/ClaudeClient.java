package com.lmp.support.ai;

/**
 * Port over the Anthropic API. Production binding will use the Anthropic Java SDK
 * with prompt caching + tool_use (per LMP {@code claude-api} skill). The port lets
 * {@link ClaudeAdapter} unit-test the orchestration + cost math without a live HTTP
 * call.
 */
public interface ClaudeClient {

    /**
     * Asks Claude to propose ONE PowerShell command via the {@code powershell_exec}
     * tool. Implementations MUST force structured output via tool_use so the response
     * always has command/risk/rationale fields.
     */
    RawSuggestion proposeCommand(String userPrompt, String sessionContext);

    /** Raw decoded tool_use payload + usage; cost is computed downstream. */
    record RawSuggestion(
        String command,
        String llmRiskLevel,   // "LOW" | "MEDIUM" | "HIGH"  (string so unknown values can be detected)
        String rationale,
        int tokensInput,
        int tokensOutput,
        String model
    ) {}
}
