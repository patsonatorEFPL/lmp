$baseUrl = "http://localhost:8080"
$futureDate = "2025-09-29"
$timeSlot = "16:00"

Write-Host "Test de creation de rendez-vous pour le $futureDate à $timeSlot"

# 1. Obtenir le token CSRF
$response = Invoke-WebRequest -Uri "$baseUrl/" -SessionVariable session
$csrfPattern = '<meta name="_csrf" content="([^"]*)"'
$match = [regex]::Match($response.Content, $csrfPattern)
$csrfToken = $match.Groups[1].Value

Write-Host "Token CSRF: $($csrfToken.Substring(0, 20))..."

# 2. Vérifier les créneaux disponibles
$slotsUrl = "$baseUrl/appointments/available-slots?date=$futureDate"
$slotsResponse = Invoke-WebRequest -Uri $slotsUrl -Method GET -WebSession $session
Write-Host "Creneaux disponibles: $($slotsResponse.Content)"

$slots = $slotsResponse.Content | ConvertFrom-Json

if ($timeSlot -in $slots) {
    Write-Host "Creneau $timeSlot disponible - tentative de creation"
    
    # 3. Créer le rendez-vous
    $headers = @{
        "Content-Type" = "application/json"
        "X-Requested-With" = "XMLHttpRequest"
        "X-CSRF-TOKEN" = $csrfToken
    }
    
    $appointmentData = @{
        name = "Test User"
        email = "test@example.com"
        phone = "+1234567890"
        service = "Consultation"
        date = $futureDate
        time = $timeSlot
        _csrf = $csrfToken
    }
    
    $body = $appointmentData | ConvertTo-Json
    
    try {
        $createResponse = Invoke-WebRequest -Uri "$baseUrl/appointments/create" -Method POST -Body $body -Headers $headers -WebSession $session
        Write-Host "SUCCES! Status: $($createResponse.StatusCode)"
        Write-Host "Response: $($createResponse.Content)"
    }
    catch {
        Write-Host "ERREUR Status: $($_.Exception.Response.StatusCode)"
        Write-Host "Message: $($_.Exception.Message)"
        
        if ($_.Exception.Response) {
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorBody = $reader.ReadToEnd()
            Write-Host "Erreur detaillee: $errorBody"
        }
    }
}
else {
    Write-Host "Creneau $timeSlot non disponible"
    Write-Host "Creneaux disponibles: $($slots -join ', ')"
}
