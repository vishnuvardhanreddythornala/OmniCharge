# Verification Script for user-service Centralized Logging Integration
# This script verifies that user-service properly logs to logging-service

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "USER-SERVICE LOGGING VERIFICATION" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Check if RabbitMQ is running
Write-Host "[1/6] Checking RabbitMQ status..." -ForegroundColor Yellow
try {
    $rabbitResponse = Invoke-WebRequest -Uri "http://localhost:15672" -Method Get -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ RabbitMQ is running on port 15672" -ForegroundColor Green
} catch {
    Write-Host "✗ RabbitMQ is NOT running. Please start RabbitMQ first." -ForegroundColor Red
    Write-Host "  Run: docker start rabbitmq (or start RabbitMQ service)" -ForegroundColor Yellow
    exit 1
}

# Step 2: Check if logging-service is running
Write-Host "[2/6] Checking logging-service status..." -ForegroundColor Yellow
try {
    $loggingResponse = Invoke-WebRequest -Uri "http://localhost:8086/actuator/health" -Method Get -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ logging-service is running on port 8086" -ForegroundColor Green
} catch {
    Write-Host "✗ logging-service is NOT running. Please start logging-service first." -ForegroundColor Red
    Write-Host "  Run: cd logging-service && mvnw.cmd spring-boot:run" -ForegroundColor Yellow
    exit 1
}

# Step 3: Check if user-service is running
Write-Host "[3/6] Checking user-service status..." -ForegroundColor Yellow
try {
    $userResponse = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -Method Get -TimeoutSec 5 -ErrorAction Stop
    Write-Host "✓ user-service is running on port 8081" -ForegroundColor Green
} catch {
    Write-Host "✗ user-service is NOT running. Please start user-service first." -ForegroundColor Red
    Write-Host "  Run: cd user-service && mvnw.cmd spring-boot:run" -ForegroundColor Yellow
    exit 1
}

# Step 4: Check RabbitMQ queue
Write-Host "[4/6] Checking RabbitMQ log.events queue..." -ForegroundColor Yellow
try {
    $queueUrl = "http://localhost:15672/api/queues/%2F/log.events"
    $credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("guest:guest"))
    $headers = @{
        Authorization = "Basic $credentials"
    }
    $queueInfo = Invoke-RestMethod -Uri $queueUrl -Headers $headers -Method Get
    Write-Host "✓ Queue 'log.events' exists" -ForegroundColor Green
    Write-Host "  Messages ready: $($queueInfo.messages_ready)" -ForegroundColor Cyan
    Write-Host "  Messages unacknowledged: $($queueInfo.messages_unacknowledged)" -ForegroundColor Cyan
    Write-Host "  Total messages: $($queueInfo.messages)" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Could not check RabbitMQ queue. Queue may not exist yet." -ForegroundColor Yellow
}

# Step 5: Trigger a test operation (user registration)
Write-Host "[5/6] Triggering test user registration..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$testEmail = "test_$timestamp@example.com"
$registerPayload = @{
    email = $testEmail
    password = "Test@123456"
    fullName = "Test User $timestamp"
    phoneNumber = "+1234567890"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" -Method Post -Body $registerPayload -ContentType "application/json"
    Write-Host "✓ User registration successful" -ForegroundColor Green
    Write-Host "  User ID: $($registerResponse.userId)" -ForegroundColor Cyan
    Write-Host "  Email: $testEmail" -ForegroundColor Cyan
} catch {
    Write-Host "✗ User registration failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  This is expected if the service is not fully configured" -ForegroundColor Yellow
}

# Step 6: Check logging-service logs directory
Write-Host "[6/6] Checking logging-service log files..." -ForegroundColor Yellow
$logsDir = "logging-service\logs"
if (Test-Path $logsDir) {
    Write-Host "✓ Logs directory exists: $logsDir" -ForegroundColor Green
    
    # Check all-services.log
    $allServicesLog = "$logsDir\all-services.log"
    if (Test-Path $allServicesLog) {
        Write-Host "✓ all-services.log exists" -ForegroundColor Green
        $allServicesContent = Get-Content $allServicesLog -Tail 10
        Write-Host "  Last 10 lines:" -ForegroundColor Cyan
        $allServicesContent | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
    } else {
        Write-Host "✗ all-services.log not found" -ForegroundColor Yellow
    }
    
    # Check user-service.log
    $userServiceLog = "$logsDir\user-service.log"
    if (Test-Path $userServiceLog) {
        Write-Host "✓ user-service.log exists" -ForegroundColor Green
        $userServiceContent = Get-Content $userServiceLog -Tail 10
        Write-Host "  Last 10 lines:" -ForegroundColor Cyan
        $userServiceContent | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
    } else {
        Write-Host "✗ user-service.log not found" -ForegroundColor Yellow
    }
} else {
    Write-Host "✗ Logs directory not found: $logsDir" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICATION COMPLETE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Check user-service.log for business operation logs (USER_REGISTRATION)" -ForegroundColor White
Write-Host "2. Check all-services.log for critical events (LIFECYCLE, ERROR, WARN)" -ForegroundColor White
Write-Host "3. Query logging-service database to verify log persistence" -ForegroundColor White
Write-Host "4. Check RabbitMQ management UI for message flow" -ForegroundColor White
