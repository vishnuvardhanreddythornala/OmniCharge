# Operator Service Logging Verification Script
# This script verifies that operator-service is properly logging to the centralized logging system

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Operator Service Logging Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if logging-service is running
Write-Host "1. Checking if logging-service is running..." -ForegroundColor Yellow
$loggingService = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*logging-service*" }
if ($loggingService) {
    Write-Host "   ✓ Logging service is running (PID: $($loggingService.Id))" -ForegroundColor Green
} else {
    Write-Host "   ✗ Logging service is NOT running" -ForegroundColor Red
    Write-Host "   Please start logging-service first" -ForegroundColor Yellow
    exit 1
}

# Check if operator-service is running
Write-Host "2. Checking if operator-service is running..." -ForegroundColor Yellow
$operatorService = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*operator-service*" }
if ($operatorService) {
    Write-Host "   ✓ Operator service is running (PID: $($operatorService.Id))" -ForegroundColor Green
} else {
    Write-Host "   ✗ Operator service is NOT running" -ForegroundColor Red
    Write-Host "   Please start operator-service first" -ForegroundColor Yellow
    exit 1
}

# Check operator-service logs directory
Write-Host "3. Checking operator-service logs directory..." -ForegroundColor Yellow
$operatorLogsDir = "operator-service/logs"
if (Test-Path $operatorLogsDir) {
    Write-Host "   ✓ Logs directory exists: $operatorLogsDir" -ForegroundColor Green
    
    # Check for operator-service.log
    $operatorLogFile = "$operatorLogsDir/operator-service.log"
    if (Test-Path $operatorLogFile) {
        $logSize = (Get-Item $operatorLogFile).Length
        Write-Host "   ✓ operator-service.log exists (Size: $logSize bytes)" -ForegroundColor Green
    } else {
        Write-Host "   ✗ operator-service.log not found" -ForegroundColor Red
    }
} else {
    Write-Host "   ✗ Logs directory not found: $operatorLogsDir" -ForegroundColor Red
}

# Check logging-service logs directory
Write-Host "4. Checking logging-service logs directory..." -ForegroundColor Yellow
$loggingLogsDir = "logging-service/logs"
if (Test-Path $loggingLogsDir) {
    Write-Host "   ✓ Logs directory exists: $loggingLogsDir" -ForegroundColor Green
    
    # Check for all-services.log
    $allServicesLog = "$loggingLogsDir/all-services.log"
    if (Test-Path $allServicesLog) {
        $logSize = (Get-Item $allServicesLog).Length
        Write-Host "   ✓ all-services.log exists (Size: $logSize bytes)" -ForegroundColor Green
        
        # Check if operator-service logs are present
        $operatorLogs = Select-String -Path $allServicesLog -Pattern "operator-service" -SimpleMatch
        if ($operatorLogs) {
            Write-Host "   ✓ Found operator-service entries in all-services.log" -ForegroundColor Green
        } else {
            Write-Host "   ✗ No operator-service entries found in all-services.log" -ForegroundColor Red
        }
    } else {
        Write-Host "   ✗ all-services.log not found" -ForegroundColor Red
    }
    
    # Check for operator-service.log in logging-service
    $operatorServiceLog = "$loggingLogsDir/operator-service.log"
    if (Test-Path $operatorServiceLog) {
        $logSize = (Get-Item $operatorServiceLog).Length
        Write-Host "   ✓ operator-service.log exists in logging-service (Size: $logSize bytes)" -ForegroundColor Green
    } else {
        Write-Host "   ✗ operator-service.log not found in logging-service" -ForegroundColor Red
    }
} else {
    Write-Host "   ✗ Logs directory not found: $loggingLogsDir" -ForegroundColor Red
}

# Check for business operation log events
Write-Host "5. Checking for business operation log events..." -ForegroundColor Yellow
if (Test-Path $operatorServiceLog) {
    $businessEvents = @(
        "OPERATOR_CREATED",
        "OPERATOR_UPDATED",
        "OPERATOR_ACTIVATED",
        "OPERATOR_DEACTIVATED",
        "OPERATOR_DETECTION",
        "PLAN_ACTIVATED",
        "PLAN_DEACTIVATED",
        "NUMVERIFY_API_CALL",
        "RABBITMQ_RECEIVE"
    )
    
    foreach ($event in $businessEvents) {
        $found = Select-String -Path $operatorServiceLog -Pattern $event -SimpleMatch -Quiet
        if ($found) {
            Write-Host "   ✓ Found $event events" -ForegroundColor Green
        } else {
            Write-Host "   ✗ No $event events found (may not have occurred yet)" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "   ⚠ Cannot check business events - log file not found" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Verification Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "To test operator-service logging:" -ForegroundColor Yellow
Write-Host "1. Create an operator: POST http://localhost:8083/api/operators" -ForegroundColor White
Write-Host "2. Detect operator: GET http://localhost:8083/api/operators/detect/{mobileNumber}" -ForegroundColor White
Write-Host "3. Check logs in logging-service/logs/operator-service.log" -ForegroundColor White
Write-Host ""
