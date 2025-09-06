# Script de test simple pour le 29 septembre à 16:00
$baseUrl = "http://localhost:8080"
$futureDate = "2025-09-29"
$timeSlot = "16:00"

function Get-CSRFToken {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/" -SessionVariable session
        $csrfToken = $response.Content | Select-String -Pattern '<meta name="_csrf" content="([^"]*)"' | ForEach-Object { $_.Matches[0].Groups[1].Value }
        return @{ Token = $csrfToken; Session = $session }
    } catch {
        Write-Host "Erreur CSRF: $($_.Exception.Message)"
        return $null
    }
}

Write-Host "=========================================="
Write-Host "TEST RENDEZ-VOUS - 29 SEPTEMBRE 16:00"
Write-Host "=========================================="

# Etape 1: Verification des creneaux
Write-Host "`nETAPE 1: Verification des creneaux disponibles"
$csrfInfo = Get-CSRFToken
if ($csrfInfo) {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/appointments/available-slots?date=$futureDate" -Method GET -WebSession $csrfInfo.Session
        $slots = $response.Content | ConvertFrom-Json
        Write-Host "Creneaux pour $futureDate : $($response.Content)"
        
        if ($slots -contains $timeSlot) {
            Write-Host "✓ Creneau $timeSlot confirme disponible"
            
            # Etape 2: Creation du rendez-vous
            Write-Host "`nETAPE 2: Creation du rendez-vous"
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
                time = $timeSlot
                _csrf = $csrfInfo.Token
            } | ConvertTo-Json
            
            Write-Host "Token CSRF: $($csrfInfo.Token.Substring(0, 20))..."
            Write-Host "Envoi de la requete..."
            
            try {
                $createResponse = Invoke-WebRequest -Uri "$baseUrl/appointments/create" -Method POST -Body $body -Headers $headers -WebSession $csrfInfo.Session
                Write-Host "SUCCES! Status: $($createResponse.StatusCode)"
                Write-Host "Response: $($createResponse.Content)"
            } catch {
                Write-Host "ERREUR lors de la creation:"
                Write-Host "Status: $($_.Exception.Response.StatusCode)"
                if ($_.Exception.Response) {
                    $errorStream = $_.Exception.Response.GetResponseStream()
                    $reader = New-Object System.IO.StreamReader($errorStream)
                    $errorBody = $reader.ReadToEnd()
                    Write-Host "Error: $errorBody"
                }
            }
        } else {
            Write-Host "✗ Creneau $timeSlot non disponible"
            Write-Host "Creneaux disponibles: $($slots -join ', ')"
        }
    } catch {
        Write-Host "Erreur lors de la verification: $($_.Exception.Message)"
    }
} else {
    Write-Host "Impossible de recuperer le token CSRF"
}

Write-Host "`n=========================================="
