package com.lmp.support.exec;

import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Regex catalogues feeding {@link CommandValidator}.
 * Precedence (in the validator): denylist → high-risk hints → allowlist → default MEDIUM.
 *
 * Patterns are PowerShell-flavoured but case-insensitive so cmd.exe spellings match too.
 */
@Configuration
public class CommandAllowlistConfig {

    /** Auto-allowed (LOW risk, no approval). Read-only or trivially safe. */
    public List<Pattern> lowRiskAllowlist() {
        return List.of(
            Pattern.compile("^(Get-Process|Get-Service|Get-ChildItem|Get-Item)( .*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(ipconfig|ipconfig /all|ipconfig /flushdns)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(ping|nslookup|tracert)\\s+[\\w.-]+$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Get-NetAdapter( .*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Get-NetIPAddress( .*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Get-NetRoute( .*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Get-NetTCPConnection( .*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Get-EventLog( .*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Get-WinEvent( .*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Get-ComputerInfo$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Get-Date( .*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^systeminfo$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^whoami( /.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^hostname$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Test-NetConnection\\s+[\\w.-]+( .*)?$", Pattern.CASE_INSENSITIVE)
        );
    }

    /** Hard-blocked. Backend rejects regardless of any approval. */
    public List<Pattern> denylist() {
        return List.of(
            // code execution via string evaluation
            Pattern.compile("\\bInvoke-Expression\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?<![\\w-])iex(?![\\w-])", Pattern.CASE_INSENSITIVE),
            // download-then-execute
            Pattern.compile("\\bInvoke-WebRequest\\b.*\\|.*\\b(iex|Invoke-Expression)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(curl|wget|Invoke-WebRequest)\\b.*\\|.*\\bsh\\b", Pattern.CASE_INSENSITIVE),
            // boot config
            Pattern.compile("\\bbcdedit\\b", Pattern.CASE_INSENSITIVE),
            // disk wipe / format
            Pattern.compile("\\bformat\\s+[a-zA-Z]:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcipher\\s+/w(:|\\s)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bvssadmin\\s+delete\\b", Pattern.CASE_INSENSITIVE),
            // WMI remote / WinRM
            Pattern.compile("\\bwmic\\s+/node\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bInvoke-WmiMethod\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bInvoke-Command\\s+-ComputerName\\b", Pattern.CASE_INSENSITIVE),
            // critical registry roots
            Pattern.compile("\\bReg\\s+add\\s+(?:HKLM|HKEY_LOCAL_MACHINE)\\\\(SECURITY|SAM|System\\\\CurrentControlSet\\\\Services)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bSet-ItemProperty\\b.*HKLM:\\\\(SECURITY|SAM)\\b", Pattern.CASE_INSENSITIVE),
            // service / user creation
            Pattern.compile("\\bNet\\s+user\\b.*/add\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bNet\\s+localgroup\\b.*/add\\b", Pattern.CASE_INSENSITIVE),
            // power state
            Pattern.compile("\\b(shutdown|Restart-Computer|Stop-Computer)\\b", Pattern.CASE_INSENSITIVE),
            // BitLocker disable
            Pattern.compile("\\bmanage-bde\\s+-off\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bDisable-BitLocker\\b", Pattern.CASE_INSENSITIVE),
            // Defender disable
            Pattern.compile("\\bSet-MpPreference\\b.*-DisableRealtimeMonitoring", Pattern.CASE_INSENSITIVE),
            // filesystem nukes targeting system paths
            Pattern.compile("\\bRemove-Item\\b.*-Recurse\\b.*\\b(C:\\\\Windows|C:\\\\Users)\\b", Pattern.CASE_INSENSITIVE)
        );
    }

    /** Patterns that auto-classify a cmd as HIGH (still require approval, but flagged dangerous). */
    public List<Pattern> highRiskHints() {
        return List.of(
            Pattern.compile("\\bRemove-Item\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bSet-Service\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bNew-Service\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bUninstall-WindowsFeature\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bStop-Service\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bDism\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bSet-ExecutionPolicy\\b.*Unrestricted", Pattern.CASE_INSENSITIVE)
        );
    }
}
