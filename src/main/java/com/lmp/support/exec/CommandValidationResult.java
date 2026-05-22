package com.lmp.support.exec;

public record CommandValidationResult(
    CommandRiskLevel riskLevel,
    String reason,
    boolean requiresApproval,
    boolean blocked
) {
    public static CommandValidationResult blocked(String reason) {
        return new CommandValidationResult(CommandRiskLevel.BLOCKED, reason, false, true);
    }
    public static CommandValidationResult low(String reason) {
        return new CommandValidationResult(CommandRiskLevel.LOW, reason, false, false);
    }
    public static CommandValidationResult medium(String reason) {
        return new CommandValidationResult(CommandRiskLevel.MEDIUM, reason, true, false);
    }
    public static CommandValidationResult high(String reason) {
        return new CommandValidationResult(CommandRiskLevel.HIGH, reason, true, false);
    }
}
