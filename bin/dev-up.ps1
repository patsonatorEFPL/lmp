# Démarre PostgreSQL local (docker-compose.dev.yml) et attend que la base accepte les connexions.
# Usage : depuis la racine du dépôt — .\bin\dev-up.ps1
# Enregistrer ce fichier en UTF-8 avec BOM si les accents s’affichent mal dans la console.

$ErrorActionPreference = "Stop"
try {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $OutputEncoding = [Console]::OutputEncoding
} catch { }
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$ComposeFile = Join-Path $Root "docker-compose.dev.yml"
if (-not (Test-Path $ComposeFile)) {
    Write-Error "Fichier introuvable : $ComposeFile"
    exit 1
}

Write-Host "Démarrage de PostgreSQL (docker compose)..." -ForegroundColor Cyan
docker compose -f $ComposeFile up -d
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$ready = $false
for ($i = 0; $i -lt 45; $i++) {
    docker compose -f $ComposeFile exec -T lmp-dev-db pg_isready -U lmp_dev -d lmp_db 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $ready = $true
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $ready) {
    Write-Error "PostgreSQL n'est pas devenu prêt à temps. Vérifiez : docker compose -f docker-compose.dev.yml logs lmp-dev-db"
    exit 1
}

Write-Host "PostgreSQL est prêt (volume persistant lmp-dev-pgdata)." -ForegroundColor Green
Write-Host ""
Write-Host "Lancer le backend (profil dev) :" -ForegroundColor Yellow
Write-Host '  $env:SPRING_PROFILES_ACTIVE=''dev''; mvn spring-boot:run'
Write-Host "Ou exécuter la classe com.lmp.LmpApplication depuis l'IDE."
Write-Host ""
Write-Host "Mot de passe BDD par défaut (aligner application-secrets) : lmp_dev_local" -ForegroundColor DarkGray
