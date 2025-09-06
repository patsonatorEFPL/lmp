# Script de test pour le créneau disponible du 29 septembre à 16:00
$baseUrl = "http://localhost:8080"

# Date et heure spécifiques avec créneau disponible
$futureDate = "2025-09-29"
$timeSlot = "16:00"

# Fonction pour extraire le token CSRF depuis les meta-balises
function Get-CSRFToken {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/" -SessionVariable session
        $csrfToken = $response.Content | Select-String -Pattern '<meta name="_csrf" content="([^"]*)"' | ForEach-Object { $_.Matches[0].Groups[1].Value }
        
        return @{
            Token = $csrfToken
            Session = $session
        }
    } catch {
        Write-Host "Erreur lors de la recuperation du token CSRF: $($_.Exception.Message)"
        return $null
    }
}

# Fonction pour tester la creation de rendez-vous
function Test-AppointmentCreation {
    $csrfInfo = Get-CSRFToken
    if (-not $csrfInfo -or -not $csrfInfo.Token) {
        Write-Host "Impossible de recuperer le token CSRF"
        return $false
    }

    $headers = @{
        "Content-Type" = "application/json"
        "X-Requested-With" = "XMLHttpRequest"
        "X-CSRF-TOKEN" = $csrfInfo.Token
    }

    $body = @{
        name = "Test Utilisateur"
        email = "test.user@example.com"
        phone = "+33-1-23-45-67-89"
        service = "Consultation"
        date = $futureDate
        time = $timeSlot
        _csrf = $csrfInfo.Token
    } | ConvertTo-Json

    try {
        Write-Host "Test de creation de rendez-vous..."
        Write-Host "Date: $futureDate"
        Write-Host "Heure: $timeSlot"
        Write-Host "Token CSRF: $($csrfInfo.Token.Substring(0, 20))..."
        Write-Host "Donnees envoyees:"
        Write-Host $body
        
        $response = Invoke-WebRequest -Uri "$baseUrl/appointments/create" -Method POST -Body $body -Headers $headers -WebSession $csrfInfo.Session -ErrorAction Stop
        
        Write-Host "`n=== SUCCES ==="
        Write-Host "Status: $($response.StatusCode)"
        Write-Host "Response: $($response.Content)"
        Write-Host "=============="
        
        return $true
        
    } catch {
        $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode } else { "Unknown" }
        Write-Host "`n=== ERREUR ==="
        Write-Host "Status: $statusCode"
        Write-Host "Message: $($_.Exception.Message)"
        
        if ($_.Exception.Response) {
            try {
                $errorContent = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($errorContent)
                $errorBody = $reader.ReadToEnd()
                Write-Host "Error Body: $errorBody"
            } catch {
                Write-Host "Impossible de lire le contenu de l'erreur"
            }
        }
        Write-Host "============="
        return $false
    }
}

# Fonction pour vérifier les créneaux disponibles
function Test-AvailableSlots {
    $csrfInfo = Get-CSRFToken
    if (-not $csrfInfo) {
        Write-Host "Impossible de recuperer le token CSRF pour les creneaux"
        return @()
    }

    try {
        Write-Host "Verification des creneaux disponibles pour: $futureDate"
        $response = Invoke-WebRequest -Uri "$baseUrl/appointments/available-slots?date=$futureDate" -Method GET -WebSession $csrfInfo.Session -ErrorAction Stop
        
        Write-Host "Status: $($response.StatusCode)"
        $slots = $response.Content | ConvertFrom-Json
        Write-Host "Creneaux disponibles: $($response.Content)"
        
        # Vérifier si le créneau 16:00 est disponible
        if ($slots -contains $timeSlot) {
            Write-Host "✓ Creneau $timeSlot confirme comme disponible"
        } else {
            Write-Host "✗ Creneau $timeSlot non trouve dans les disponibilites"
        }
        
        return $slots
        
    } catch {
        Write-Host "Erreur lors de la verification des creneaux - Status: $($_.Exception.Response.StatusCode)"
        Write-Host "Message: $($_.Exception.Message)"
        return @()
    }
}

Write-Host "=========================================="
Write-Host "TEST RENDEZ-VOUS - 29 SEPTEMBRE 16:00"
Write-Host "=========================================="

# Étape 1 : Vérifier que le créneau est disponible
Write-Host "`n--- ETAPE 1: Verification des creneaux ---"
$availableSlots = Test-AvailableSlots

# Étape 2 : Tenter de créer le rendez-vous
Write-Host "`n--- ETAPE 2: Creation du rendez-vous ---"
if ($availableSlots -contains $timeSlot) {
    $success = Test-AppointmentCreation
    
    if ($success) {
        Write-Host "`n🎉 RENDEZ-VOUS CREE AVEC SUCCES !"
        Write-Host "Date: $futureDate"
        Write-Host "Heure: $timeSlot"
    } else {
        Write-Host "`n❌ ECHEC DE LA CREATION DU RENDEZ-VOUS"
    }
} else {
    Write-Host "`n⚠️ Le creneau $timeSlot n'est pas disponible le $futureDate"
    Write-Host "Creneaux disponibles actuellement: $($availableSlots -join ', ')"
}

Write-Host "`n=========================================="
