# Script de test pour l'endpoint de création de rendez-vous avec CSRF corrigé
$baseUrl = "http://localhost:8080"

# Calculer une date dans le futur (dans 7 jours pour éviter les weekends)
$futureDate = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")

# Fonction pour extraire le token CSRF depuis les méta-balises
function Get-CSRFToken {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/" -SessionVariable session
        
        # Chercher le token CSRF dans les méta-balises
        $csrfToken = $response.Content | Select-String -Pattern '<meta name="_csrf" content="([^"]*)"' | ForEach-Object { $_.Matches[0].Groups[1].Value }
        
        if (-not $csrfToken) {
            # Méthode alternative : chercher dans un champ caché du formulaire
            $csrfToken = $response.Content | Select-String -Pattern 'name="_csrf".*?value="([^"]*)"' | ForEach-Object { $_.Matches[0].Groups[1].Value }
        }
        
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
        name = "Test User"
        email = "test@example.com"
        phone = "+1234567890"
        service = "Consultation"
        date = $futureDate
        time = "10:00"
        _csrf = $csrfInfo.Token
    } | ConvertTo-Json

    try {
        Write-Host "`nTest de création de rendez-vous..."
        Write-Host "Date utilisée: $futureDate"
        Write-Host "Token CSRF: $($csrfInfo.Token.Substring(0, 20))..." # Afficher seulement les premiers caractères
        
        $response = Invoke-WebRequest -Uri "$baseUrl/appointments/create" -Method POST -Body $body -Headers $headers -WebSession $csrfInfo.Session -ErrorAction Stop
        
        Write-Host "✅ Succès - Status: $($response.StatusCode)"
        Write-Host "Response: $($response.Content)"
        
    } catch {
        $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode } else { "Unknown" }
        Write-Host "❌ Erreur - Status: $statusCode"
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
    }
}

# Fonction pour tester la récupération des créneaux
function Test-AvailableSlots {
    $csrfInfo = Get-CSRFToken
    if (-not $csrfInfo) {
        Write-Host "❌ Impossible de récupérer le token CSRF pour les créneaux"
        return
    }

    try {
        Write-Host "Test de récupération des créneaux disponibles pour: $futureDate"
        $response = Invoke-WebRequest -Uri "$baseUrl/appointments/available-slots?date=$futureDate" -Method GET -WebSession $csrfInfo.Session -ErrorAction Stop
        
        Write-Host "✅ Succès - Status: $($response.StatusCode)"
        Write-Host "Créneaux disponibles: $($response.Content)"
        
        # Vérifier s'il y a des créneaux disponibles
        $slots = $response.Content | ConvertFrom-Json
        if ($slots.Count -eq 0) {
            Write-Host "⚠️  Aucun créneau disponible pour cette date"
            
            # Essayer une autre date
            $alternateDate = (Get-Date).AddDays(10).ToString("yyyy-MM-dd")
            Write-Host "Test avec une date alternative: $alternateDate"
            
            $altResponse = Invoke-WebRequest -Uri "$baseUrl/appointments/available-slots?date=$alternateDate" -Method GET -WebSession $csrfInfo.Session -ErrorAction Stop
            Write-Host "Créneaux pour $alternateDate : $($altResponse.Content)"
        }
        
    } catch {
        Write-Host "❌ Erreur - Status: $($_.Exception.Response.StatusCode)"
        Write-Host "Message: $($_.Exception.Message)"
    }
}

Write-Host "=== Test des endpoints de rendez-vous avec CSRF corrigé ==="
Write-Host "Date calculée: $futureDate"
Test-AvailableSlots
Test-AppointmentCreation
Write-Host "`n=== Fin des tests ==="
