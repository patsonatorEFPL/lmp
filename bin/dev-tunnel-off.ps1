<#
.SYNOPSIS
  Restaure tous les hostnames TUNNEL_HOSTS vers Dokploy (A record).
  Stoppe cloudflared + SSH tunnel Postgres.

.DESCRIPTION
  Inverse de dev-tunnel-on.ps1. Multi-zones : la zone Cloudflare est
  résolue dynamiquement par suffixe pour chaque hostname.
#>

[CmdletBinding()]
param(
    [switch]$SkipDns
)

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

$required = @('CF_API_TOKEN','TUNNEL_HOSTS','DOKPLOY_IP')
foreach ($k in $required) {
    if (-not $env_[$k]) { Write-Error "Missing $k in $envPath" }
}

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
            if (-not $resp.success) { Write-Error "Cloudflare zones fetch failed" }
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
    if ($null -eq $best) { Write-Error "No Cloudflare zone matches '$Hostname'" }
    return @{ ZoneId = $script:zoneCache[$best]; ZoneName = $best }
}

function Restore-CfDnsRecord {
    param([string]$Hostname, [string]$Ip)
    $z = Get-CfZoneId -Hostname $Hostname
    $listUrl = "https://api.cloudflare.com/client/v4/zones/$($z.ZoneId)/dns_records?name=$Hostname"
    $list = Invoke-RestMethod -Headers $cfHeaders -Uri $listUrl -Method Get

    $body = @{
        type    = 'A'
        name    = $Hostname
        content = $Ip
        proxied = $false
        ttl     = 1
    } | ConvertTo-Json -Compress

    if ($list.result.Count -eq 0) {
        $createUrl = "https://api.cloudflare.com/client/v4/zones/$($z.ZoneId)/dns_records"
        $resp = Invoke-RestMethod -Headers $cfHeaders -Uri $createUrl -Method Post -Body $body
        if (-not $resp.success) { Write-Error "Create DNS failed for $Hostname" }
        Write-Host "  CREATE  [$($z.ZoneName)] $Hostname → A $Ip"
    } else {
        $rid = $list.result[0].id
        $patchUrl = "https://api.cloudflare.com/client/v4/zones/$($z.ZoneId)/dns_records/$rid"
        $resp = Invoke-RestMethod -Headers $cfHeaders -Uri $patchUrl -Method Put -Body $body
        if (-not $resp.success) { Write-Error "Update DNS failed for $Hostname" }
        Write-Host "  UPDATE  [$($z.ZoneName)] $Hostname → A $Ip"
    }
}

# Stop cloudflared
$cfPidFile = Join-Path $repoRoot 'bin\cloudflared\.cloudflared.pid'
if (Test-Path $cfPidFile) {
    $cfPid = Get-Content $cfPidFile | Select-Object -First 1
    if ($cfPid) {
        Get-Process -Id $cfPid -ErrorAction SilentlyContinue | ForEach-Object {
            Write-Host "==> Stopping cloudflared PID $cfPid"
            Stop-Process -Id $cfPid -Force
        }
    }
    Remove-Item $cfPidFile -Force -ErrorAction SilentlyContinue
}
Get-Process -Name cloudflared -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "==> Stopping stray cloudflared PID $($_.Id)"
    Stop-Process -Id $_.Id -Force
}

# Stop SSH tunnel
$sshPidFile = Join-Path $repoRoot 'bin\cloudflared\.ssh-pg.pid'
if (Test-Path $sshPidFile) {
    $sshPid = Get-Content $sshPidFile | Select-Object -First 1
    if ($sshPid) {
        Get-Process -Id $sshPid -ErrorAction SilentlyContinue | ForEach-Object {
            Write-Host "==> Stopping SSH tunnel PID $sshPid"
            Stop-Process -Id $sshPid -Force
        }
    }
    Remove-Item $sshPidFile -Force -ErrorAction SilentlyContinue
}

if (-not $SkipDns) {
    Write-Host "==> Restoring DNS to Dokploy ($($env_.DOKPLOY_IP)) for $($hosts_.Count) hostnames"
    foreach ($h in $hosts_) { Restore-CfDnsRecord -Hostname $h -Ip $env_.DOKPLOY_IP }
} else {
    Write-Host "==> Skipping DNS restore (-SkipDns)"
}

Write-Host ""
Write-Host "=========================================================="
Write-Host " Tunnel OFF — DNS pointe vers Dokploy."
Write-Host " Vérifie : .\bin\dev-tunnel-status.ps1"
Write-Host "=========================================================="
