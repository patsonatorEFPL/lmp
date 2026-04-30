<#
.SYNOPSIS
  Bascule des hostnames Dokploy vers laptop local via Cloudflare Tunnel
  (multi-zones). Démarre cloudflared + SSH tunnel Postgres staging.

.DESCRIPTION
  1. Lit bin/cloudflared/tunnel.env (créé depuis tunnel.env.sample)
  2. Résout dynamiquement la Cloudflare zone de chaque hostname (suffixe)
  3. Patche les enregistrements DNS (A → CNAME vers <TUNNEL_ID>.cfargotunnel.com)
  4. Génère bin/cloudflared/config.yml avec un ingress par hostname
  5. Démarre cloudflared en background
  6. Démarre SSH tunnel Postgres en background
  7. Affiche les commandes pour lancer Spring Boot

.NOTES
  Pour revenir à Dokploy : .\bin\dev-tunnel-off.ps1
#>

[CmdletBinding()]
param(
    [switch]$SkipDb,
    [switch]$SkipDns
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath  = Join-Path $repoRoot 'bin\cloudflared\tunnel.env'
$cfgPath  = Join-Path $repoRoot 'bin\cloudflared\config.yml'

if (-not (Test-Path $envPath)) {
    Write-Error "Missing $envPath. Copy tunnel.env.sample to tunnel.env and fill in values."
}

# Load env file (KEY=VALUE per line)
$env_ = @{}
Get-Content $envPath | ForEach-Object {
    if ($_ -match '^\s*#') { return }
    if ($_ -match '^\s*$') { return }
    if ($_ -match '^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$') {
        $env_[$Matches[1]] = $Matches[2].Trim()
    }
}

$required = @('CF_API_TOKEN','TUNNEL_ID','TUNNEL_CREDENTIALS','TUNNEL_HOSTS',
              'DOKPLOY_IP','DOKPLOY_SSH_HOST','DOKPLOY_SSH_USER',
              'LOCAL_PG_PORT','REMOTE_PG_HOST','REMOTE_PG_PORT')
foreach ($k in $required) {
    if (-not $env_[$k]) { Write-Error "Missing $k in $envPath" }
}

$hosts_ = $env_.TUNNEL_HOSTS.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ }
if ($hosts_.Count -eq 0) { Write-Error "TUNNEL_HOSTS is empty" }

$tunnelTarget = "$($env_.TUNNEL_ID).cfargotunnel.com"

$cfHeaders = @{
    'Authorization' = "Bearer $($env_.CF_API_TOKEN)"
    'Content-Type'  = 'application/json'
}

# --- Cache zones (one API call) and resolve per hostname by longest suffix ---
$script:zoneCache = $null
function Get-CfZoneId {
    param([string]$Hostname)
    if ($null -eq $script:zoneCache) {
        $script:zoneCache = @{}
        $page = 1
        do {
            $url = "https://api.cloudflare.com/client/v4/zones?per_page=50&page=$page"
            $resp = Invoke-RestMethod -Headers $cfHeaders -Uri $url -Method Get
            if (-not $resp.success) {
                Write-Error "Cloudflare zones fetch failed: $($resp | ConvertTo-Json -Depth 6)"
            }
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

function Update-CfDnsRecord {
    param([string]$Hostname, [string]$Target)
    $z = Get-CfZoneId -Hostname $Hostname
    $listUrl = "https://api.cloudflare.com/client/v4/zones/$($z.ZoneId)/dns_records?name=$Hostname"
    $list = Invoke-RestMethod -Headers $cfHeaders -Uri $listUrl -Method Get

    $body = @{
        type    = 'CNAME'
        name    = $Hostname
        content = $Target
        proxied = $true
        ttl     = 1
    } | ConvertTo-Json -Compress

    if ($list.result.Count -eq 0) {
        $createUrl = "https://api.cloudflare.com/client/v4/zones/$($z.ZoneId)/dns_records"
        $resp = Invoke-RestMethod -Headers $cfHeaders -Uri $createUrl -Method Post -Body $body
        if (-not $resp.success) { Write-Error "Create DNS failed for $Hostname : $($resp | ConvertTo-Json -Depth 6)" }
        Write-Host "  CREATE  [$($z.ZoneName)] $Hostname → CNAME $Target (proxied)"
    } else {
        $rid = $list.result[0].id
        $patchUrl = "https://api.cloudflare.com/client/v4/zones/$($z.ZoneId)/dns_records/$rid"
        $resp = Invoke-RestMethod -Headers $cfHeaders -Uri $patchUrl -Method Put -Body $body
        if (-not $resp.success) { Write-Error "Update DNS failed for $Hostname : $($resp | ConvertTo-Json -Depth 6)" }
        Write-Host "  UPDATE  [$($z.ZoneName)] $Hostname → CNAME $Target (proxied)"
    }
}

if (-not $SkipDns) {
    Write-Host "==> Flipping DNS to Cloudflare Tunnel ($($hosts_.Count) hostnames)"
    foreach ($h in $hosts_) { Update-CfDnsRecord -Hostname $h -Target $tunnelTarget }
} else {
    Write-Host "==> Skipping DNS flip (-SkipDns)"
}

# --- Generate config.yml dynamically ---
Write-Host "==> Generating $cfgPath"
$ingressLines = @()
foreach ($h in $hosts_) {
    $ingressLines += "  - hostname: $h"
    $ingressLines += "    service: http://localhost:8080"
}
$ingressLines += "  - service: http_status:404"
$cfg = @"
# Auto-generated by dev-tunnel-on.ps1. DO NOT EDIT.
tunnel: $($env_.TUNNEL_ID)
credentials-file: $($env_.TUNNEL_CREDENTIALS)

loglevel: info
transport-loglevel: warn

originRequest:
  connectTimeout: 30s
  tlsTimeout: 30s
  noTLSVerify: true

ingress:
$($ingressLines -join "`n")
"@
Set-Content -Path $cfgPath -Value $cfg -Encoding utf8

# Stop existing cloudflared
Get-Process -Name cloudflared -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "  Stopping existing cloudflared PID $($_.Id)"
    Stop-Process -Id $_.Id -Force
}

# Locate cloudflared binary :
#   1. ~/.cloudflared/cloudflared.exe (where cert + tunnel credentials live)
#   2. PATH (winget install)
$cfBin = Join-Path $env:USERPROFILE '.cloudflared\cloudflared.exe'
if (-not (Test-Path $cfBin)) {
    $inPath = Get-Command cloudflared -ErrorAction SilentlyContinue
    if ($inPath) { $cfBin = $inPath.Source }
    else { Write-Error "cloudflared not found (checked $cfBin and PATH)" }
}

# Start cloudflared as background process
Write-Host "==> Starting cloudflared tunnel ($cfBin)"
$cfLog = Join-Path $repoRoot 'tunnel-cloudflared.log'
$cfProc = Start-Process -FilePath $cfBin `
    -ArgumentList @('tunnel', '--config', $cfgPath, 'run') `
    -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput $cfLog `
    -RedirectStandardError "$cfLog.err"
Write-Host "  cloudflared PID: $($cfProc.Id) (log: $cfLog)"
$cfProc.Id | Out-File -FilePath (Join-Path $repoRoot 'bin\cloudflared\.cloudflared.pid') -Encoding ascii

if (-not $SkipDb) {
    $sshPidFile = Join-Path $repoRoot 'bin\cloudflared\.ssh-pg.pid'
    if (Test-Path $sshPidFile) {
        $oldPid = Get-Content $sshPidFile | Select-Object -First 1
        if ($oldPid) {
            Get-Process -Id $oldPid -ErrorAction SilentlyContinue | ForEach-Object {
                Write-Host "  Stopping existing SSH tunnel PID $oldPid"
                Stop-Process -Id $oldPid -Force
            }
        }
    }

    Write-Host "==> Starting SSH tunnel for Postgres ($($env_.LOCAL_PG_PORT) → $($env_.REMOTE_PG_HOST):$($env_.REMOTE_PG_PORT))"
    $sshLog = Join-Path $repoRoot 'tunnel-ssh-pg.log'
    $forward = "$($env_.LOCAL_PG_PORT):$($env_.REMOTE_PG_HOST):$($env_.REMOTE_PG_PORT)"
    $sshProc = Start-Process -FilePath 'ssh' `
        -ArgumentList @('-N', '-L', $forward,
                        '-o', 'ServerAliveInterval=30',
                        '-o', 'ServerAliveCountMax=3',
                        "$($env_.DOKPLOY_SSH_USER)@$($env_.DOKPLOY_SSH_HOST)") `
        -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput $sshLog `
        -RedirectStandardError "$sshLog.err"
    Write-Host "  ssh PID: $($sshProc.Id) (log: $sshLog)"
    $sshProc.Id | Out-File -FilePath $sshPidFile -Encoding ascii
}

Start-Sleep -Seconds 3

Write-Host ""
Write-Host "=========================================================="
Write-Host " Tunnel ON — DNS pointe vers ton laptop."
Write-Host "=========================================================="
Write-Host " Hostnames :"
foreach ($h in $hosts_) { Write-Host "   - https://$h" }
Write-Host ""
Write-Host " Lance Spring Boot avec :"
Write-Host ""
Write-Host "   `$env:DB_URL = 'jdbc:postgresql://localhost:$($env_.LOCAL_PG_PORT)/lmp_test_db'"
Write-Host "   `$env:DB_USERNAME = '<staging-user>'"
Write-Host "   `$env:DB_PASSWORD = '<staging-password>'"
Write-Host "   `$env:SITE_URL = 'https://$($hosts_[0])'"
Write-Host "   `$env:SPRING_PROFILES_ACTIVE = 'staging'"
Write-Host "   .\mvnw spring-boot:run"
Write-Host ""
Write-Host " Status : .\bin\dev-tunnel-status.ps1"
Write-Host " Stop   : .\bin\dev-tunnel-off.ps1"
Write-Host "=========================================================="
