<#
.SYNOPSIS
  Affiche état tunnel : DNS Cloudflare (multi-zones), processes locaux,
  HEAD test sur chaque hostname.
#>

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath  = Join-Path $repoRoot 'bin\cloudflared\tunnel.env'

if (-not (Test-Path $envPath)) {
    Write-Error "Missing $envPath."
}

$env_ = @{}
Get-Content $envPath | ForEach-Object {
    if ($_ -match '^\s*#') { return }
    if ($_ -match '^\s*$') { return }
    if ($_ -match '^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$') {
        $env_[$Matches[1]] = $Matches[2].Trim()
    }
}

if (-not $env_.TUNNEL_HOSTS) { Write-Error "TUNNEL_HOSTS missing in $envPath" }
$hosts_ = $env_.TUNNEL_HOSTS.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ }

$cfHeaders = @{
    'Authorization' = "Bearer $($env_.CF_API_TOKEN)"
    'Content-Type'  = 'application/json'
}

$script:zoneCache = $null
function Get-CfZoneId {
    param([string]$Hostname)
    if ($null -eq $script:zoneCache) {
        $script:zoneCache = @{}
        $page = 1
        do {
            $url = "https://api.cloudflare.com/client/v4/zones?per_page=50&page=$page"
            $resp = Invoke-RestMethod -Headers $cfHeaders -Uri $url -Method Get
            if (-not $resp.success) { return $null }
            foreach ($z in $resp.result) { $script:zoneCache[$z.name] = $z.id }
            $totalPages = [int]$resp.result_info.total_pages
            $page++
        } while ($page -le $totalPages)
    }
    $best = $null
    foreach ($zoneName in $script:zoneCache.Keys) {
        if ($Hostname -eq $zoneName -or $Hostname.EndsWith(".$zoneName")) {
            if ($null -eq $best -or $zoneName.Length -gt $best.Length) {
                $best = $zoneName
            }
        }
    }
    if ($null -eq $best) { return $null }
    return @{ ZoneId = $script:zoneCache[$best]; ZoneName = $best }
}

function Get-CfDnsRecord {
    param([string]$Hostname)
    $z = Get-CfZoneId -Hostname $Hostname
    if ($null -eq $z) { return @{ error = "no zone match" } }
    $listUrl = "https://api.cloudflare.com/client/v4/zones/$($z.ZoneId)/dns_records?name=$Hostname"
    try {
        $list = Invoke-RestMethod -Headers $cfHeaders -Uri $listUrl -Method Get
        if ($list.result.Count -eq 0) { return $null }
        $rec = $list.result[0]
        $rec | Add-Member -NotePropertyName ZoneName -NotePropertyValue $z.ZoneName -Force
        return $rec
    } catch {
        return @{ error = $_.Exception.Message }
    }
}

Write-Host "=========================================================="
Write-Host " DNS Cloudflare ($($hosts_.Count) hostnames)"
Write-Host "=========================================================="
foreach ($host_ in $hosts_) {
    $rec = Get-CfDnsRecord -Hostname $host_
    if ($null -eq $rec) {
        Write-Host "  $host_  →  (no record)"
    } elseif ($rec.error) {
        Write-Host "  $host_  →  ERROR: $($rec.error)"
    } else {
        $proxied = if ($rec.proxied) { ' (proxied)' } else { '' }
        Write-Host "  [$($rec.ZoneName)] $host_  →  $($rec.type) $($rec.content)$proxied"
    }
}

Write-Host ""
Write-Host "=========================================================="
Write-Host " Processes locaux"
Write-Host "=========================================================="
$cf = Get-Process -Name cloudflared -ErrorAction SilentlyContinue
if ($cf) {
    foreach ($p in $cf) { Write-Host "  cloudflared    PID $($p.Id)  STARTED $($p.StartTime)" }
} else {
    Write-Host "  cloudflared    NOT RUNNING"
}

$sshPidFile = Join-Path $repoRoot 'bin\cloudflared\.ssh-pg.pid'
if (Test-Path $sshPidFile) {
    $sshPid = Get-Content $sshPidFile | Select-Object -First 1
    if ($sshPid) {
        $sp = Get-Process -Id $sshPid -ErrorAction SilentlyContinue
        if ($sp) {
            Write-Host "  ssh (Postgres) PID $sshPid  STARTED $($sp.StartTime)"
        } else {
            Write-Host "  ssh (Postgres) PID $sshPid  (process gone)"
        }
    }
} else {
    Write-Host "  ssh (Postgres) NOT RUNNING"
}

Write-Host ""
Write-Host "=========================================================="
Write-Host " Tests rapides"
Write-Host "=========================================================="
foreach ($host_ in $hosts_) {
    try {
        $r = Invoke-WebRequest -Uri "https://$host_/" -Method Head -TimeoutSec 5 -SkipHttpErrorCheck -SkipCertificateCheck
        $server = $r.Headers['server']
        if ($server -is [array]) { $server = $server[0] }
        Write-Host "  HEAD https://$host_/   →  $($r.StatusCode)  server=$server"
    } catch {
        Write-Host "  HEAD https://$host_/   →  ERROR: $($_.Exception.Message)"
    }
}
