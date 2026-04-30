# ============================================================
# Configure LMP SSO — Social Login Key dans Frappe CRM
#
# Usage :
#   # Prod LMP (auth.lmp-services.ca → frappe-erp-client) :
#   .\bin\configure-frappe-sso.ps1 `
#     -FrappePassword "..." `
#     -SiteUrl "https://lmp-services.ca" `
#     -ClientId "frappe-erp-client" `
#     -ClientSecret "..."
#
#   # Staging LMP (auth.dev.lmp-services.ca → frappe-erp-client-dev) :
#   .\bin\configure-frappe-sso.ps1 `
#     -FrappePassword "..." `
#     -SiteUrl "https://dev.lmp-services.ca" `
#     -ClientId "frappe-erp-client-dev" `
#     -ClientSecret "..." `
#     -ProviderName "LMP SSO Dev"
#
# Q1A : 1 Frappe partagée → 2 Social Login Keys (1 par env LMP).
# ProviderName est la primary key dans Frappe — DIFFÉRENT par env.
# ============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$FrappePassword,

    # SiteUrl LMP — driver de toutes les autres URLs (sauf override explicite)
    [Parameter(Mandatory=$true)]
    [string]$SiteUrl,

    [Parameter(Mandatory=$true)]
    [string]$ClientId,

    [Parameter(Mandatory=$true)]
    [string]$ClientSecret,

    # Frappe instance — défaut "crm.<host-of-SiteUrl-without-www>"
    [string]$FrappeUrl    = "",
    [string]$FrappeUser   = "patsonator32@gmail.com",

    # Issuer URI dérivé "auth.<host>" sauf override
    [string]$IssuerUri    = "",

    # Provider name = primary key Frappe ; default "LMP SSO" (prod). Staging : "LMP SSO Dev"
    [string]$ProviderName = "LMP SSO"
)

$ErrorActionPreference = "Stop"

# ── Derivation depuis SiteUrl ────────────────────────────────
$siteHost = ([System.Uri]$SiteUrl).Host
$hostNoWww = if ($siteHost.StartsWith("www.")) { $siteHost.Substring(4) } else { $siteHost }
$isLocal = $siteHost -in @("localhost", "127.0.0.1")

if (-not $IssuerUri) {
    $IssuerUri = if ($isLocal) { $SiteUrl } else { "https://auth.$hostNoWww" }
}
if (-not $FrappeUrl) {
    $FrappeUrl = if ($isLocal) { "http://localhost:8000" } else { "https://crm.$hostNoWww" }
}
$RedirectUri = "$FrappeUrl/api/method/frappe.integrations.oauth2_logins.custom/lmp_sso"

Write-Host "`n=== Configuration LMP SSO sur $FrappeUrl ===" -ForegroundColor Cyan
Write-Host "  Site URL      : $SiteUrl"
Write-Host "  Issuer URI    : $IssuerUri"
Write-Host "  Client ID     : $ClientId"
Write-Host "  Provider Name : $ProviderName"
Write-Host "  Redirect URI  : $RedirectUri"

# ── 1. Authentification ──────────────────────────────────────
Write-Host "`n[1/3] Authentification Frappe..." -ForegroundColor Yellow

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

$loginBody = @{
    usr = $FrappeUser
    pwd = $FrappePassword
} | ConvertTo-Json

$loginResp = Invoke-RestMethod `
    -Uri "$FrappeUrl/api/method/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $loginBody `
    -SessionVariable session `
    -UseBasicParsing

Write-Host "  Connecté en tant que $FrappeUser" -ForegroundColor Green

# ── 2. Vérifier si la Social Login Key existe déjà ───────────
Write-Host "`n[2/3] Vérification entrée existante (provider_name=$ProviderName)..." -ForegroundColor Yellow

$existing = $null
try {
    $existing = Invoke-RestMethod `
        -Uri "$FrappeUrl/api/resource/Social Login Key/$ProviderName" `
        -Method GET `
        -WebSession $session `
        -UseBasicParsing
    Write-Host "  Entrée existante trouvée — mise à jour..." -ForegroundColor Yellow
} catch {
    Write-Host "  Aucune entrée existante — création..." -ForegroundColor Yellow
}

# ── 3. Payload Social Login Key ───────────────────────────────
$payload = @{
    provider_name            = $ProviderName
    enable_social_login      = 1
    social_login_provider    = "Custom"
    client_id                = $ClientId
    client_secret            = $ClientSecret
    base_url                 = $IssuerUri
    custom_base_url          = 1
    authorize_url            = "/oauth2/authorize"
    access_token_url         = "/oauth2/token"
    redirect_url             = "/api/method/frappe.integrations.oauth2_logins.custom/lmp_sso"
    api_endpoint             = "/userinfo"
    api_endpoint_args        = '{"response_type": "code", "scope": "openid email profile"}'
    allow_sign_ups           = 1
    sign_ups                 = "Allow"
    user_id_property         = "sub"
    icon                     = ""
} | ConvertTo-Json

Write-Host "`n[3/3] Application de la configuration..." -ForegroundColor Yellow

if ($existing) {
    $resp = Invoke-RestMethod `
        -Uri "$FrappeUrl/api/resource/Social Login Key/$ProviderName" `
        -Method PUT `
        -ContentType "application/json" `
        -Body $payload `
        -WebSession $session `
        -UseBasicParsing
} else {
    $resp = Invoke-RestMethod `
        -Uri "$FrappeUrl/api/resource/Social Login Key" `
        -Method POST `
        -ContentType "application/json" `
        -Body $payload `
        -WebSession $session `
        -UseBasicParsing
}

Write-Host "  Social Login Key '$ProviderName' configurée !" -ForegroundColor Green

# ── Résumé ────────────────────────────────────────────────────
Write-Host "`n=== Résumé ===" -ForegroundColor Cyan
Write-Host "  Provider Name      : $ProviderName"
Write-Host "  Issuer (Base URL)  : $IssuerUri"
Write-Host "  Authorize URL      : $IssuerUri/oauth2/authorize"
Write-Host "  Token URL          : $IssuerUri/oauth2/token"
Write-Host "  UserInfo URL       : $IssuerUri/userinfo"
Write-Host "  Client ID          : $ClientId"
Write-Host "  Redirect URI       : $RedirectUri"
Write-Host ""
Write-Host "=== Côté LMP ===" -ForegroundColor Cyan
Write-Host "  application-<env>.properties OU env vars Dokploy :"
Write-Host "    SITE_URL=$SiteUrl"
Write-Host "    ERP_OAUTH2_CLIENT_ID=$ClientId"
Write-Host "    ERP_OAUTH2_CLIENT_SECRET=<aligné avec ce qui est dans Frappe>"
Write-Host "  L'issuer est dérivé automatiquement par SiteEnvironmentPostProcessor."
Write-Host ""
Write-Host "=== Vérification ===" -ForegroundColor Cyan
Write-Host "  curl $IssuerUri/.well-known/openid-configuration"
Write-Host "  $FrappeUrl/login -> bouton '$ProviderName'"
Write-Host ""
