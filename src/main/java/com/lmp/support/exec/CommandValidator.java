package com.lmp.support.exec;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Single source of truth for terminal command safety. Order of checks:
 * <ol>
 *   <li>Empty/blank → BLOCKED</li>
 *   <li>Denylist hit → BLOCKED (even if part of a multi-stmt that also matches allowlist)</li>
 *   <li>High-risk hint → HIGH (requires approval)</li>
 *   <li>Allowlist exact match → LOW (auto-allowed)</li>
 *   <li>Default → MEDIUM (requires manual approval)</li>
 * </ol>
 */
@Component
public class CommandValidator {

    private final CommandAllowlistConfig cfg;

    public CommandValidator(CommandAllowlistConfig cfg) {
        this.cfg = cfg;
    }

    public CommandValidationResult validate(String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) {
            return CommandValidationResult.blocked("empty command");
        }
        String cmd = rawCommand.trim();

        for (Pattern p : cfg.denylist()) {
            if (p.matcher(cmd).find()) {
                return CommandValidationResult.blocked("matches denylist pattern: " + p.pattern());
            }
        }
        for (Pattern p : cfg.highRiskHints()) {
            if (p.matcher(cmd).find()) {
                return CommandValidationResult.high("matches high-risk pattern: " + p.pattern());
            }
        }
        for (Pattern p : cfg.lowRiskAllowlist()) {
            if (p.matcher(cmd).matches()) {
                return CommandValidationResult.low("matches allowlist pattern: " + p.pattern());
            }
        }
        return CommandValidationResult.medium("no pattern match; requires manual review");
    }
}
