# Complete Flow Test for Operator Service Logging
# This script tests the complete operator service flow and verifies logging

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Operator Service Complete Flow Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8083/api"

# Test 1: Create an Operator
Write-Host "Test 1: Creating a new operator..." -ForegroundColor Yellow
$operatorData = @{
    name = "Test Operator"
    code = "TEST"
    category = "PREPAID"
    logoUrl = "https://example.com/logo.png"
    isActive = $true
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/operators" -Method Post -Body $operatorData -ContentType "application/json"
    Write-Host "   ✓ Operator created successfully" -ForegroundColor Green
    Write-Host "   Operator ID: $($response.id)" -ForegroundColor White
    $operatorId = $response.id
    
    # Wait for log to be written
    Start-Sleep -Seconds 2
    
    # Check logs
    $logFile = "logging-service/logs/operator-service.log"
    if (Test-Path $logFile) {
        $createLog = Select-String -Path $logFile -Pattern "OPERATOR_CREATED" | Select-Object -Last 1
        if ($createLog) {
            Write-Host "   ✓ Found OPERATOR_CREATED log entry" -ForegroundColor Green
        } else {
            Write-Host "   ✗ OPERATOR_CREATED log entry not found" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "   ✗ Failed to create operator: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 2: Update the Operator
Write-Host "Test 2: Updating the operator..." -ForegroundColor Yellow
if ($operatorId) {
    $updateData = @{
        name = "Updated Test Operator"
        code = "TEST"
        category = "PREPAID"
        logoUrl = "https://example.com/new-logo.png"
        isActive = $true
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/operators/$operatorId" -Method Put -Body $updateData -ContentType "application/json"
        Write-Host "   ✓ Operator updated successfully" -ForegroundColor Green
        
        # Wait for log to be written
        Start-Sleep -Seconds 2
        
        # Check logs
        if (Test-Path $logFile) {
            $updateLog = Select-String -Path $logFile -Pattern "OPERATOR_UPDATED" | Select-Object -Last 1
            if ($updateLog) {
                Write-Host "   ✓ Found OPERATOR_UPDATED log entry" -ForegroundColor Green
            } else {
                Write-Host "   ✗ OPERATOR_UPDATED log entry not found" -ForegroundColor Red
            }
        }
    } catch {
        Write-Host "   ✗ Failed to update operator: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# Test 3: Detect Operator
Write-Host "Test 3: Detecting operator for mobile number..." -ForegroundColor Yellow
$mobileNumber = "9876543210"

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/operators/detect/$mobileNumber" -Method Get
    Write-Host "   ✓ Operator detection completed" -ForegroundColor Green
    if ($response.operatorName) {
        Write-Host "   Detected Operator: $($response.operatorName)" -ForegroundColor White
    }
    
    # Wait for log to be written
    Start-Sleep -Seconds 2
    
    # Check logs
    if (Test-Path $logFile) {
        $detectionLog = Select-String -Path $logFile -Pattern "OPERATOR_DETECTION" | Select-Object -Last 1
        if ($detectionLog) {
            Write-Host "   ✓ Found OPERATOR_DETECTION log entry" -ForegroundColor Green
        } else {
            Write-Host "   ✗ OPERATOR_DETECTION log entry not found" -ForegroundColor Red
        }
        
        $numverifyLog = Select-String -Path $logFile -Pattern "NUMVERIFY_API_CALL" | Select-Object -Last 1
        if ($numverifyLog) {
            Write-Host "   ✓ Found NUMVERIFY_API_CALL log entry" -ForegroundColor Green
        } else {
            Write-Host "   ⚠ NUMVERIFY_API_CALL log entry not found (may be cached)" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "   ✗ Failed to detect operator: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 4: Create a Plan
Write-Host "Test 4: Creating a plan for the operator..." -ForegroundColor Yellow
if ($operatorId) {
    $planData = @{
        operatorId = $operatorId
        planName = "Test Plan"
        price = 299
        validityDays = 28
        dataLimit = "1.5GB/day"
        callBenefit = "Unlimited"
        smsBenefit = "100/day"
        category = "DATA"
        isActive = $true
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/plans" -Method Post -Body $planData -ContentType "application/json"
        Write-Host "   ✓ Plan created successfully" -ForegroundColor Green
        Write-Host "   Plan ID: $($response.id)" -ForegroundColor White
        $planId = $response.id
    } catch {
        Write-Host "   ✗ Failed to create plan: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# Test 5: Activate/Deactivate Plan
Write-Host "Test 5: Testing plan activation/deactivation..." -ForegroundColor Yellow
if ($planId) {
    try {
        # Deactivate
        Invoke-RestMethod -Uri "$baseUrl/plans/$planId/deactivate" -Method Put
        Write-Host "   ✓ Plan deactivated" -ForegroundColor Green
        
        Start-Sleep -Seconds 2
        
        # Check logs
        if (Test-Path $logFile) {
            $deactivateLog = Select-String -Path $logFile -Pattern "PLAN_DEACTIVATED" | Select-Object -Last 1
            if ($deactivateLog) {
                Write-Host "   ✓ Found PLAN_DEACTIVATED log entry" -ForegroundColor Green
            } else {
                Write-Host "   ✗ PLAN_DEACTIVATED log entry not found" -ForegroundColor Red
            }
        }
        
        # Activate
        Invoke-RestMethod -Uri "$baseUrl/plans/$planId/activate" -Method Put
        Write-Host "   ✓ Plan activated" -ForegroundColor Green
        
        Start-Sleep -Seconds 2
        
        # Check logs
        if (Test-Path $logFile) {
            $activateLog = Select-String -Path $logFile -Pattern "PLAN_ACTIVATED" | Select-Object -Last 1
            if ($activateLog) {
                Write-Host "   ✓ Found PLAN_ACTIVATED log entry" -ForegroundColor Green
            } else {
                Write-Host "   ✗ PLAN_ACTIVATED log entry not found" -ForegroundColor Red
            }
        }
    } catch {
        Write-Host "   ✗ Failed to activate/deactivate plan: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Check the following log files for details:" -ForegroundColor Yellow
Write-Host "1. operator-service/logs/operator-service.log" -ForegroundColor White
Write-Host "2. logging-service/logs/operator-service.log" -ForegroundColor White
Write-Host "3. logging-service/logs/all-services.log" -ForegroundColor White
Write-Host ""
Write-Host "Run verify-operator-service-db-logs.sql to check database logs" -ForegroundColor Yellow
Write-Host ""
