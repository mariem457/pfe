$api   = "http://localhost:8081/api/truck-locations"
$token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZ2VudF9QYXJpcyIsInJvbGUiOiJNVU5JQ0lQQUxJVFkiLCJpYXQiOjE3NzI2NTMwNzAsImV4cCI6MTc3MjczOTQ3MH0.Al_UlZ3NXKyMuNHMQUiXtNKXQVKqsObftCIUZmU2dCs"

$headers = @{
  "Content-Type"  = "application/json"
  "Authorization" = "Bearer $token"
}

$trucks = @(
  @{ id = 1;  lat = 48.8460; lng = 2.2950 },
  @{ id = 2;  lat = 48.8445; lng = 2.2920 },
  @{ id = 3;  lat = 48.8425; lng = 2.2970 },
  @{ id = 5;  lat = 48.8405; lng = 2.3000 }
 )

# حدود تقريب لــ 15ème (باش ما يهربوش)
$minLat = 48.8330; $maxLat = 48.8550
$minLng = 2.2650;  $maxLng = 2.3150

Write-Host "Simulation running... CTRL+C to stop."

while ($true) {
  foreach ($t in $trucks) {

    $t.lat = [double]$t.lat + ((Get-Random -Minimum -6 -Maximum 7) * 0.00005)
    $t.lng = [double]$t.lng + ((Get-Random -Minimum -6 -Maximum 7) * 0.00007)

    if ($t.lat -lt $minLat) { $t.lat = $minLat }
    if ($t.lat -gt $maxLat) { $t.lat = $maxLat }
    if ($t.lng -lt $minLng) { $t.lng = $minLng }
    if ($t.lng -gt $maxLng) { $t.lng = $maxLng }

    $payload = @{
      driverId   = $t.id
      lat        = [Math]::Round($t.lat, 6)
      lng        = [Math]::Round($t.lng, 6)
      speedKmh   = (Get-Random -Minimum 10 -Maximum 60)
      headingDeg = (Get-Random -Minimum 0 -Maximum 360)
    } | ConvertTo-Json -Compress

    try {
      Invoke-RestMethod -Method Post -Uri $api -Headers $headers -Body $payload | Out-Null
    } catch {
      Write-Host "POST failed for truck $($t.id): $($_.Exception.Message)"
    }
  }

  Start-Sleep -Milliseconds 800
}