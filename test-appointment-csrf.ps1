# Récupérer la page d'accueil pour obtenir le token CSRF
Write-Host "Récupération du token CSRF..."
try {
    $homePage = Invoke-WebRequest -Uri "http://localhost:8080/" -SessionVariable "session"
    
    # Extraire le token CSRF depuis les meta tags
    $csrfTokenMatch = $homePage.Content -match '<meta name="_csrf" content="([^"]+)"/>'
    $csrfHeaderMatch = $homePage.Content -match '<meta name="_csrf_header" content="([^"]+)"/>'
    
    if ($csrfTokenMatch -and $csrfHeaderMatch) {
        $csrfToken = $matches[1]
        # Relancer la regex pour récupérer le header
        $homePage.Content -match '<meta name="_csrf_header" content="([^"]+)"/>'
        $csrfHeader = $matches[1]
        
        Write-Host "Token CSRF trouvé: $($csrfToken.Substring(0, 10))..."
        Write-Host "Header CSRF: $csrfHeader"
        
        # Préparer les données du rendez-vous
        $body = @{
            name = "Test User"
            email = "test@example.com"
            phone = "+1234567890"
            service = "Consultation"
            date = "2025-09-17"
            time = "09:00"
            message = "Test message"
        } | ConvertTo-Json
        
        # Préparer les headers avec CSRF
        $headers = @{
            "Content-Type" = "application/json"
            "User-Agent" = "TestBrowser/1.0"
            "X-CSRF-TOKEN" = $csrfToken
        }
        
        Write-Host "Envoi de la requête avec token CSRF..."
        $response = Invoke-WebRequest -Uri "http://localhost:8080/appointments/create" -Method POST -Body $body -Headers $headers -WebSession $session
        Write-Host "Success: $($response.StatusCode)"
        Write-Host "Response: $($response.Content)"
        
    } else {
        Write-Host "Erreur: Token CSRF non trouvé dans la page"
        Write-Host "Contenu de la page (premiers 500 caractères):"
        Write-Host $homePage.Content.Substring(0, [Math]::Min(500, $homePage.Content.Length))
    }
    
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        Write-Host "Status Code: $statusCode"
        
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody"
    }
}
