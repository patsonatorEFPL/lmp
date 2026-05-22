package com.lmp.support.exec;

public enum CommandRiskLevel {
    LOW,      // read-only inspections (Get-Process, ipconfig, Get-NetAdapter)
    MEDIUM,   // mutating but reversible (Stop-Service, Set-NetIPAddress)
    HIGH,     // irreversible or destructive (Remove-Item, Stop-Computer)
    BLOCKED   // denylist hit — never executed
}
