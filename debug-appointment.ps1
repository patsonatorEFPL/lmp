# Test script de débogage pour la création de rendez-vous
$baseUrl = "http://localhost:8080"

try {
    # Test des créneaux disponibles d'abord
    Write-Host "=== Test des créneaux disponibles ==="
    $slotsResponse = Invoke-WebRequest -Uri "$baseUrl/appointments/available-slots?date=2025-09-17" -Method GET -UseBasicParsing
    Write-Host "Créneaux disponibles: $($slotsResponse.Content)"
    
    # Obtenir le token CSRF
    Write-Host "`n=== Récupération du token CSRF ==="
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $homeResponse = Invoke-WebRequest -Uri $baseUrl -Method GET -SessionVariable session -UseBasicParsing
    
    $csrfTokenMatch = $homeResponse.Content | Select-String -Pattern 'name="_csrf"\s+content="([^"]+)"'
    if ($csrfTokenMatch) {
        $csrfToken = $csrfTokenMatch.Matches[0].Groups[1].Value
        Write-Host "Token CSRF trouvé: $($csrfToken.Substring(0,10))..."
    } else {
        throw "Token CSRF non trouvé"
    }
    
    # Test avec données très simples
    Write-Host "`n=== Test de création de rendez-vous ==="
    $appointmentData = @{
        name = "Test"
        email = "test@test.com"
        phone = "0123456789"
        service = "consultation"  # Service simple sans accents
        date = "2025-09-17"
        time = "09:00"
        message = "Test simple"
    } | ConvertTo-Json
    
    $headers = @{
        "Content-Type" = "application/json"
        "X-Requested-With" = "XMLHttpRequest"
        "X-CSRF-TOKEN" = $csrfToken
    }
    
    Write-Host "Données envoyées: $appointmentData"
    
    $response = Invoke-WebRequest -Uri "$baseUrl/appointments/create" -Method POST -Body $appointmentData -Headers $headers -WebSession $session -UseBasicParsing
    
    Write-Host "Succès ! Réponse: $($response.Content)"
    
} catch {
    Write-Host "Erreur: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseContent = $reader.ReadToEnd()
        Write-Host "Contenu de l'erreur: $responseContent"
    }
}
