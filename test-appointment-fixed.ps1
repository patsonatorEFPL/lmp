# Script de test pour l'endpoint de création de rendez-vous avec date future
$baseUrl = "http://localhost:8080"

# Calculer une date dans le futur (dans 3 jours)
$futureDate = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")

# Fonction pour extraire le token CSRF
function Get-CSRFToken {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/" -SessionVariable session
        $csrfToken = $response.Content | Select-String -Pattern 'name="_csrf".*?value="([^"]*)"' | ForEach-Object { $_.Matches[0].Groups[1].Value }
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
    if (-not $csrfInfo) {
        Write-Host "Impossible de récupérer le token CSRF"
        return
    }

    $headers = @{
        "Content-Type" = "application/json"
        "X-Requested-With" = "XMLHttpRequest"
    }

    $body = @{
        name = "Test User"
        email = "test@example.com"
        phone = "+1234567890"
        service = "Consultation"
        date = $futureDate
        time = "14:00"
        _csrf = $csrfInfo.Token
    } | ConvertTo-Json

    try {
        Write-Host "Test de création de rendez-vous..."
        Write-Host "Date utilisée: $futureDate"
        Write-Host "Token CSRF: $($csrfInfo.Token)"
        
        $response = Invoke-WebRequest -Uri "$baseUrl/appointments/create" -Method POST -Body $body -Headers $headers -WebSession $csrfInfo.Session -ErrorAction Stop
        
        Write-Host "✅ Succès - Status: $($response.StatusCode)"
        Write-Host "Response: $($response.Content)"
        
    } catch {
        Write-Host "❌ Erreur - Status: $($_.Exception.Response.StatusCode)"
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

# Fonction pour tester la récupération des créneaux avec la bonne date
function Test-AvailableSlots {
    $csrfInfo = Get-CSRFToken
    if (-not $csrfInfo) {
        Write-Host "Impossible de récupérer le token CSRF"
        return
    }

    try {
        Write-Host "Test de récupération des créneaux disponibles pour: $futureDate"
        $response = Invoke-WebRequest -Uri "$baseUrl/appointments/available-slots?date=$futureDate" -Method GET -WebSession $csrfInfo.Session -ErrorAction Stop
        
        Write-Host "✅ Succès - Status: $($response.StatusCode)"
        Write-Host "Response: $($response.Content)"
        
    } catch {
        Write-Host "❌ Erreur - Status: $($_.Exception.Response.StatusCode)"
        Write-Host "Message: $($_.Exception.Message)"
    }
}

Write-Host "=== Test des endpoints de rendez-vous avec date future ==="
Write-Host "Date calculée: $futureDate"
Test-AvailableSlots
Test-AppointmentCreation
Write-Host "`n=== Fin des tests ==="
