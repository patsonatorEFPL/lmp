$body = @{
    name = "Test User"
    email = "test@example.com"
    phone = "+1234567890"
    service = "Consultation"
    date = "2025-09-17"
    time = "09:00"
    message = "Test message"
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
    "User-Agent" = "TestBrowser/1.0"
}

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/appointments/create" -Method POST -Body $body -Headers $headers
    Write-Host "Success: $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
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
