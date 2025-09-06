# Script de test pour l'endpoint de création de rendez-vous - jour ouvrable
$baseUrl = "http://localhost:8080"

# Utiliser une date qui est un jour ouvrable (mardi 9 septembre)
$futureDate = "2025-09-09"

# Fonction pour extraire le token CSRF depuis les méta-balises
function Get-CSRFToken {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/" -SessionVariable session
        $csrfToken = $response.Content | Select-String -Pattern '<meta name="_csrf" content="([^"]*)"' | ForEach-Object { $_.Matches[0].Groups[1].Value }
        
        return @{
            Token = $csrfToken
            Session = $session
        }
    } catch {
        Write-Host "Erreur lors de la récupération du token CSRF: $($_.Exception.Message)"
        return $null
    }
}

# Fonction pour tester la création de rendez-vous
function Test-AppointmentCreation {
    param([string]$timeSlot = "10:00")
    
    $csrfInfo = Get-CSRFToken
    if (-not $csrfInfo -or -not $csrfInfo.Token) {
        Write-Host "❌ Impossible de récupérer le token CSRF"
        return
    }

    $headers = @{
        "Content-Type" = "application/json"
        "X-Requested-With" = "XMLHttpRequest"
        "X-CSRF-TOKEN" = $csrfInfo.Token
    }

    $body = @{
        name = "Jean Dupont"
        email = "jean.dupont@example.com"
        phone = "+1-514-555-1234"
        service = "Consultation"
        date = $futureDate
        time = $timeSlot
        _csrf = $csrfInfo.Token
    } | ConvertTo-Json

    try {
        Write-Host "`n🔄 Test de création de rendez-vous..."
        Write-Host "📅 Date: $futureDate (mardi)"
        Write-Host "⏰ Heure: $timeSlot"
        Write-Host "🔐 Token CSRF: $($csrfInfo.Token.Substring(0, 20))..."
        
        $response = Invoke-WebRequest -Uri "$baseUrl/appointments/create" -Method POST -Body $body -Headers $headers -WebSession $csrfInfo.Session -ErrorAction Stop
        
        Write-Host "✅ SUCCÈS - Status: $($response.StatusCode)"
        Write-Host "📬 Response: $($response.Content)"
        
        return $true
        
    } catch {
        $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode } else { "Unknown" }
        Write-Host "❌ ERREUR - Status: $statusCode"
        Write-Host "💬 Message: $($_.Exception.Message)"
        
        if ($_.Exception.Response) {
            try {
                $errorContent = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($errorContent)
                $errorBody = $reader.ReadToEnd()
                Write-Host "📝 Error Body: $errorBody"
            } catch {
                Write-Host "❓ Impossible de lire le contenu de l'erreur"
            }
        }
        return $false
    }
}

# Fonction pour tester la récupération des créneaux
function Test-AvailableSlots {
    $csrfInfo = Get-CSRFToken
    if (-not $csrfInfo) {
        Write-Host "❌ Impossible de récupérer le token CSRF pour les créneaux"
        return @()
    }

    try {
        Write-Host "🔍 Test de récupération des créneaux disponibles pour: $futureDate"
        $response = Invoke-WebRequest -Uri "$baseUrl/appointments/available-slots?date=$futureDate" -Method GET -WebSession $csrfInfo.Session -ErrorAction Stop
        
        Write-Host "✅ Succès - Status: $($response.StatusCode)"
        $slots = $response.Content | ConvertFrom-Json
        Write-Host "🕐 Créneaux disponibles: $($response.Content)"
        
        return $slots
        
    } catch {
        Write-Host "❌ Erreur - Status: $($_.Exception.Response.StatusCode)"
        Write-Host "Message: $($_.Exception.Message)"
        return @()
    }
}

Write-Host "=========================================="
Write-Host "🧪 TEST COMPLET DES RENDEZ-VOUS"
Write-Host "=========================================="
Write-Host "📅 Date testée: $futureDate (mardi)"

# Étape 1 : Récupérer les créneaux disponibles
$availableSlots = Test-AvailableSlots

# Étape 2 : Essayer de créer un rendez-vous si des créneaux sont disponibles
if ($availableSlots.Count -gt 0) {
    Write-Host "`n✅ $($availableSlots.Count) créneaux disponibles trouvés"
    
    # Essayer avec le premier créneau disponible
    $firstSlot = $availableSlots[0]
    $success = Test-AppointmentCreation -timeSlot $firstSlot
    
    if ($success) {
        Write-Host "`n🎉 RENDEZ-VOUS CRÉÉ AVEC SUCCÈS !"
    } else {
        Write-Host "`n😞 Échec de la création du rendez-vous"
        
        # Si le premier créneau échoue, essayer un autre
        if ($availableSlots.Count -gt 1) {
            Write-Host "`n🔄 Tentative avec un autre créneau..."
            $secondSlot = $availableSlots[1]
            Test-AppointmentCreation -timeSlot $secondSlot
        }
    }
} else {
    Write-Host "`n⚠️  Aucun créneau disponible pour cette date"
    Write-Host "Cela peut indiquer que tous les créneaux sont pris ou que c'est un jour non ouvrable"
}

Write-Host "`n=========================================="
Write-Host "🏁 FIN DES TESTS"
Write-Host "=========================================="
