$uuid = [guid]::NewGuid().ToString().Substring(0, 5)
$emailA = "testA_$uuid@test.com"
$emailB = "testB_$uuid@test.com"

Write-Output "Registering Admin A and B..."
Invoke-RestMethod -Uri "http://localhost:8081/api/admin/register" -Method Post -Body (@{email = $emailA; password = "password"; name = "Admin A" } | ConvertTo-Json) -ContentType "application/json" | Out-Null
Invoke-RestMethod -Uri "http://localhost:8081/api/admin/register" -Method Post -Body (@{email = $emailB; password = "password"; name = "Admin B" } | ConvertTo-Json) -ContentType "application/json" | Out-Null

Write-Output "Logging in..."
$loginA = Invoke-RestMethod -Uri "http://localhost:8081/api/admin/login" -Method Post -Body (@{email = $emailA; password = "password" } | ConvertTo-Json) -ContentType "application/json"
$tokenA = $loginA.token

$loginB = Invoke-RestMethod -Uri "http://localhost:8081/api/admin/login" -Method Post -Body (@{email = $emailB; password = "password" } | ConvertTo-Json) -ContentType "application/json"
$tokenB = $loginB.token

$teachersA1 = try { Invoke-RestMethod -Uri "http://localhost:8081/api/admin/teachers" -Headers @{Authorization = "Bearer $tokenA" } } catch { @() }
Write-Output "Initial Admin A teachers count: $(@($teachersA1).Count)"

$teacherEmail = "teachA_$uuid@test.com"
try {
    $r = Invoke-RestMethod -Uri "http://localhost:8081/api/teachers/register" -Method Post -Body (@{name = "Teacher A"; email = $teacherEmail; password = "pass" } | ConvertTo-Json) -ContentType "application/json" -Headers @{Authorization = "Bearer $tokenA" }
    Write-Output "Teacher A created successfully."
}
catch {
    Write-Output "ERROR CREATING TEACHER:"
    Write-Output $_.Exception.Message
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Output $reader.ReadToEnd()
    }
}

$teachersA2 = try { Invoke-RestMethod -Uri "http://localhost:8081/api/admin/teachers" -Headers @{Authorization = "Bearer $tokenA" } } catch { @() }
Write-Output "Admin A teachers count after creation: $(@($teachersA2).Count)"

$teachersB = try { Invoke-RestMethod -Uri "http://localhost:8081/api/admin/teachers" -Headers @{Authorization = "Bearer $tokenB" } } catch { @() }
Write-Output "Admin B teachers count: $(@($teachersB).Count)"

if (@($teachersA2).Count -gt 0 -and @($teachersB).Count -eq 0) {
    Write-Output "SUCCESS: Data isolation worked. Admin B cannot see Admin A's teacher."
}
else {
    Write-Output "FAILED: Isolation did not work."
}
