# ============================================================
# Configure LMP SSO — Social Login Key dans Frappe CRM
# Usage : .\bin\configure-frappe-sso.ps1 -FrappePassword "votre_mot_de_passe"
# ============================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$FrappePassword,

    [string]$FrappeUrl    = "https://crm.lmp-services.ca",
    [string]$FrappeUser   = "patsonator32@gmail.com",
    [string]$IssuerUri    = "https://auth.lmp-services.ca",
    [string]$ClientId     = "frappe-erp-client",
    [string]$ClientSecret = "erp-secret-2026-lmp-sso",
    [string]$RedirectUri  = "https://crm.lmp-services.ca/api/method/frappe.integrations.oauth2_logins.custom/lmp_sso"
)

$ErrorActionPreference = "Stop"

Write-Host "`n=== Configuration LMP SSO sur $FrappeUrl ===" -ForegroundColor Cyan

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

# ── 2. Vérifier si la Social Login Key "LMP SSO" existe déjà ─
Write-Host "`n[2/3] Vérification de l'entrée Social Login Key existante..." -ForegroundColor Yellow

$existing = $null
try {
    $existing = Invoke-RestMethod `
        -Uri "$FrappeUrl/api/resource/Social Login Key/LMP SSO" `
        -Method GET `
        -WebSession $session `
        -UseBasicParsing
    Write-Host "  Entrée existante trouvée — mise à jour..." -ForegroundColor Yellow
} catch {
    Write-Host "  Aucune entrée existante — création..." -ForegroundColor Yellow
}

# ── 3. Payload Social Login Key ───────────────────────────────
$payload = @{
    provider_name            = "LMP SSO"
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
    # PUT pour mise à jour
    $resp = Invoke-RestMethod `
        -Uri "$FrappeUrl/api/resource/Social Login Key/LMP SSO" `
        -Method PUT `
        -ContentType "application/json" `
        -Body $payload `
        -WebSession $session `
        -UseBasicParsing
} else {
    # POST pour création
    $resp = Invoke-RestMethod `
        -Uri "$FrappeUrl/api/resource/Social Login Key" `
        -Method POST `
        -ContentType "application/json" `
        -Body $payload `
        -WebSession $session `
        -UseBasicParsing
}

Write-Host "  Social Login Key configurée avec succès !" -ForegroundColor Green

# ── Résumé ────────────────────────────────────────────────────
Write-Host "`n=== Résumé de la configuration ===" -ForegroundColor Cyan
Write-Host "  Issuer (Base URL)  : $IssuerUri"
Write-Host "  Authorize URL      : $IssuerUri/oauth2/authorize"
Write-Host "  Token URL          : $IssuerUri/oauth2/token"
Write-Host "  UserInfo URL       : $IssuerUri/userinfo"
Write-Host "  Client ID          : $ClientId"
Write-Host "  Redirect URI       : $RedirectUri"
Write-Host ""
Write-Host "=== Prochaine étape ===" -ForegroundColor Cyan
Write-Host "  1. Déployez LMP sur Dokploy avec OAUTH2_ISSUER_URI=https://auth.lmp-services.ca"
Write-Host "  2. Vérifiez : $IssuerUri/.well-known/openid-configuration"
Write-Host "  3. Testez le SSO : $FrappeUrl/login -> 'Login with LMP SSO'"
Write-Host ""
