# Complete Flow Test for user-service Centralized Logging
# This script triggers all business operations to verify logging

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "USER-SERVICE COMPLETE FLOW TEST" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8081"
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$testEmail = "flowtest_$timestamp@example.com"
$testPassword = "Test@123456"
$newPassword = "NewTest@123456"

# Test 1: User Registration (LOCAL provider)
Write-Host "[Test 1/8] Testing USER_REGISTRATION (LOCAL)..." -ForegroundColor Yellow
$registerPayload = @{
    email = $testEmail
    password = $testPassword
    fullName = "Flow Test User $timestamp"
    phoneNumber = "+1234567890"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $registerPayload -ContentType "application/json"
    Write-Host "✓ Registration successful" -ForegroundColor Green
    Write-Host "  User ID: $($registerResponse.userId)" -ForegroundColor Cyan
    Write-Host "  Expected Log: USER_REGISTRATION with authProvider=LOCAL" -ForegroundColor Gray
    $userId = $registerResponse.userId
} catch {
    Write-Host "✗ Registration failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Start-Sleep -Seconds 2

# Test 2: Successful Login
Write-Host "[Test 2/8] Testing LOGIN_ATTEMPT (SUCCESS)..." -ForegroundColor Yellow
$loginPayload = @{
    email = $testEmail
    password = $testPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginPayload -ContentType "application/json"
    Write-Host "✓ Login successful" -ForegroundColor Green
    Write-Host "  Token received: $($loginResponse.token.Substring(0, 20))..." -ForegroundColor Cyan
    Write-Host "  Expected Log: LOGIN_ATTEMPT with outcome=SUCCESS" -ForegroundColor Gray
    $token = $loginResponse.token
} catch {
    Write-Host "✗ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Start-Sleep -Seconds 2

# Test 3: Failed Login Attempt
Write-Host "[Test 3/8] Testing LOGIN_ATTEMPT (FAILURE)..." -ForegroundColor Yellow
$failedLoginPayload = @{
    email = $testEmail
    password = "WrongPassword123"
} | ConvertTo-Json

try {
    $failedLoginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $failedLoginPayload -ContentType "application/json"
    Write-Host "✗ Login should have failed but succeeded" -ForegroundColor Red
} catch {
    Write-Host "✓ Login failed as expected" -ForegroundColor Green
    Write-Host "  Expected Log: LOGIN_ATTEMPT with outcome=FAILURE, reason=Invalid credentials" -ForegroundColor Gray
}

Start-Sleep -Seconds 2

# Test 4: Token Generation (already logged during login, but verify)
Write-Host "[Test 4/8] Verifying TOKEN_GENERATION..." -ForegroundColor Yellow
Write-Host "✓ Token was generated during login" -ForegroundColor Green
Write-Host "  Expected Log: TOKEN_GENERATION with userId=$userId" -ForegroundColor Gray

Start-Sleep -Seconds 2

# Test 5: Profile Update
Write-Host "[Test 5/8] Testing PROFILE_UPDATE..." -ForegroundColor Yellow
$updatePayload = @{
    fullName = "Updated Flow Test User $timestamp"
    phoneNumber = "+9876543210"
} | ConvertTo-Json

$headers = @{
    Authorization = "Bearer $token"
}

try {
    $updateResponse = Invoke-RestMethod -Uri "$baseUrl/api/users/profile" -Method Put -Body $updatePayload -ContentType "application/json" -Headers $headers
    Write-Host "✓ Profile update successful" -ForegroundColor Green
    Write-Host "  Expected Log: PROFILE_UPDATE with changedFields=[fullName, phoneNumber]" -ForegroundColor Gray
} catch {
    Write-Host "✗ Profile update failed: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 6: Password Reset Request
Write-Host "[Test 6/8] Testing PASSWORD_RESET_REQUEST..." -ForegroundColor Yellow
$resetRequestPayload = @{
    email = $testEmail
} | ConvertTo-Json

try {
    $resetRequestResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/forgot-password" -Method Post -Body $resetRequestPayload -ContentType "application/json"
    Write-Host "✓ Password reset request successful" -ForegroundColor Green
    Write-Host "  Expected Log: PASSWORD_RESET_REQUEST with userId=$userId" -ForegroundColor Gray
} catch {
    Write-Host "✗ Password reset request failed: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 7: Password Change (using current password)
Write-Host "[Test 7/8] Testing PASSWORD_CHANGE..." -ForegroundColor Yellow
$passwordChangePayload = @{
    currentPassword = $testPassword
    newPassword = $newPassword
} | ConvertTo-Json

try {
    $passwordChangeResponse = Invoke-RestMethod -Uri "$baseUrl/api/users/change-password" -Method Post -Body $passwordChangePayload -ContentType "application/json" -Headers $headers
    Write-Host "✓ Password change successful" -ForegroundColor Green
    Write-Host "  Expected Log: PASSWORD_CHANGE with userId=$userId" -ForegroundColor Gray
} catch {
    Write-Host "✗ Password change failed: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 8: Verify new password works
Write-Host "[Test 8/8] Verifying new password..." -ForegroundColor Yellow
$newLoginPayload = @{
    email = $testEmail
    password = $newPassword
} | ConvertTo-Json

try {
    $newLoginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $newLoginPayload -ContentType "application/json"
    Write-Host "✓ Login with new password successful" -ForegroundColor Green
    Write-Host "  Expected Log: LOGIN_ATTEMPT with outcome=SUCCESS" -ForegroundColor Gray
} catch {
    Write-Host "✗ Login with new password failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "All business operations have been triggered." -ForegroundColor Green
Write-Host "Expected log events:" -ForegroundColor Yellow
Write-Host "  1. USER_REGISTRATION (LOCAL provider)" -ForegroundColor White
Write-Host "  2. LOGIN_ATTEMPT (SUCCESS)" -ForegroundColor White
Write-Host "  3. LOGIN_ATTEMPT (FAILURE)" -ForegroundColor White
Write-Host "  4. TOKEN_GENERATION (x2)" -ForegroundColor White
Write-Host "  5. PROFILE_UPDATE" -ForegroundColor White
Write-Host "  6. PASSWORD_RESET_REQUEST" -ForegroundColor White
Write-Host "  7. PASSWORD_CHANGE" -ForegroundColor White
Write-Host "  8. LOGIN_ATTEMPT (SUCCESS with new password)" -ForegroundColor White
Write-Host ""
Write-Host "Verification Steps:" -ForegroundColor Yellow
Write-Host "1. Check user-service.log for all events:" -ForegroundColor White
Write-Host "   Get-Content logging-service\logs\user-service.log -Tail 100" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Check all-services.log for LIFECYCLE events:" -ForegroundColor White
Write-Host "   Get-Content logging-service\logs\all-services.log -Tail 50" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Query database for log entries:" -ForegroundColor White
Write-Host "   mysql -u root -p logging_db < verify-user-service-db-logs.sql" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Check RabbitMQ management UI:" -ForegroundColor White
Write-Host "   http://localhost:15672 (guest/guest)" -ForegroundColor Gray
Write-Host ""
