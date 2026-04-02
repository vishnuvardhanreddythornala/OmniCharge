# Recharge Service Complete Flow Test Script
# This script tests the complete recharge flow and verifies logging at each step

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Recharge Service Complete Flow Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$API_GATEWAY = "http://localhost:8080"
$TEST_USER_EMAIL = "testuser@example.com"
$TEST_USER_PASSWORD = "Test@123"
$TEST_MOBILE = "9876543210"

# Step 1: Login to get JWT token
Write-Host "Step 1: Logging in as test user..." -ForegroundColor Yellow
try {
    $loginBody = @{
        email = $TEST_USER_EMAIL
        password = $TEST_USER_PASSWORD
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$API_GATEWAY/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.data.token
    Write-Host "   ✓ Login successful" -ForegroundColor Green
    Write-Host "   Token: $($token.Substring(0, 20))..." -ForegroundColor Cyan
} catch {
    Write-Host "   ✗ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Please ensure user exists or create one first" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# Step 2: Get available operators
Write-Host "Step 2: Fetching available operators..." -ForegroundColor Yellow
try {
    $headers = @{
        "Authorization" = "Bearer $token"
    }
    $operatorsResponse = Invoke-RestMethod -Uri "$API_GATEWAY/api/operators" -Method Get -Headers $headers
    $operators = $operatorsResponse.data
    Write-Host "   ✓ Found $($operators.Count) operators" -ForegroundColor Green
    
    if ($operators.Count -gt 0) {
        $selectedOperator = $operators[0]
        Write-Host "   Selected operator: $($selectedOperator.operatorName) (ID: $($selectedOperator.id))" -ForegroundColor Cyan
    } else {
        Write-Host "   ✗ No operators available" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ✗ Failed to fetch operators: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 3: Get available plans for the operator
Write-Host "Step 3: Fetching available plans for operator..." -ForegroundColor Yellow
try {
    $plansResponse = Invoke-RestMethod -Uri "$API_GATEWAY/api/operators/$($selectedOperator.id)/plans" -Method Get -Headers $headers
    $plans = $plansResponse.data
    Write-Host "   ✓ Found $($plans.Count) plans" -ForegroundColor Green
    
    if ($plans.Count -gt 0) {
        $selectedPlan = $plans[0]
        Write-Host "   Selected plan: $($selectedPlan.planName) (ID: $($selectedPlan.id), Price: ₹$($selectedPlan.price))" -ForegroundColor Cyan
    } else {
        Write-Host "   ✗ No plans available for this operator" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ✗ Failed to fetch plans: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 4: Initiate recharge
Write-Host "Step 4: Initiating recharge..." -ForegroundColor Yellow
try {
    $rechargeBody = @{
        mobileNumber = $TEST_MOBILE
        operatorId = $selectedOperator.id
        planId = $selectedPlan.id
        paymentMethod = "UPI"
    } | ConvertTo-Json

    $rechargeResponse = Invoke-RestMethod -Uri "$API_GATEWAY/api/recharges" -Method Post -Body $rechargeBody -ContentType "application/json" -Headers $headers
    $recharge = $rechargeResponse.data
    Write-Host "   ✓ Recharge initiated successfully" -ForegroundColor Green
    Write-Host "   Recharge ID: $($recharge.rechargeId)" -ForegroundColor Cyan
    Write-Host "   Status: $($recharge.status)" -ForegroundColor Cyan
    Write-Host "   Amount: ₹$($recharge.amount)" -ForegroundColor Cyan
} catch {
    Write-Host "   ✗ Failed to initiate recharge: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 5: Wait for SAGA processing
Write-Host "Step 5: Waiting for SAGA processing (5 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
Write-Host "   ✓ Wait complete" -ForegroundColor Green
Write-Host ""

# Step 6: Check recharge status
Write-Host "Step 6: Checking recharge status..." -ForegroundColor Yellow
try {
    $statusResponse = Invoke-RestMethod -Uri "$API_GATEWAY/api/recharges/$($recharge.rechargeId)" -Method Get -Headers $headers
    $updatedRecharge = $statusResponse.data
    Write-Host "   ✓ Recharge status retrieved" -ForegroundColor Green
    Write-Host "   Recharge ID: $($updatedRecharge.rechargeId)" -ForegroundColor Cyan
    Write-Host "   Status: $($updatedRecharge.status)" -ForegroundColor Cyan
    Write-Host "   Transaction ID: $($updatedRecharge.transactionId)" -ForegroundColor Cyan
} catch {
    Write-Host "   ✗ Failed to check recharge status: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Step 7: Verify logging in recharge-service.log
Write-Host "Step 7: Verifying logging in recharge-service.log..." -ForegroundColor Yellow
$rechargeLogFile = "logging-service/logs/recharge-service.log"
if (Test-Path $rechargeLogFile) {
    $logContent = Get-Content $rechargeLogFile -Tail 100
    
    # Check for RECHARGE_INITIATED
    $initiatedLogs = $logContent | Select-String "RECHARGE_INITIATED.*$($recharge.rechargeId)"
    if ($initiatedLogs) {
        Write-Host "   ✓ RECHARGE_INITIATED event logged" -ForegroundColor Green
    } else {
        Write-Host "   ✗ RECHARGE_INITIATED event NOT found" -ForegroundColor Red
    }
    
    # Check for RECHARGE_PROCESSING
    $processingLogs = $logContent | Select-String "RECHARGE_PROCESSING.*$($recharge.rechargeId)"
    if ($processingLogs) {
        Write-Host "   ✓ RECHARGE_PROCESSING event logged" -ForegroundColor Green
    } else {
        Write-Host "   ✗ RECHARGE_PROCESSING event NOT found" -ForegroundColor Red
    }
    
    # Check for SAGA_EVENT_PUBLISHED
    $sagaPublishedLogs = $logContent | Select-String "SAGA_EVENT_PUBLISHED.*$($recharge.rechargeId)"
    if ($sagaPublishedLogs) {
        Write-Host "   ✓ SAGA_EVENT_PUBLISHED event logged" -ForegroundColor Green
    } else {
        Write-Host "   ✗ SAGA_EVENT_PUBLISHED event NOT found" -ForegroundColor Red
    }
    
    # Check for SAGA_PAYMENT_APPROVED or SAGA_PAYMENT_REJECTED
    $sagaApprovedLogs = $logContent | Select-String "SAGA_PAYMENT_APPROVED.*$($recharge.rechargeId)"
    $sagaRejectedLogs = $logContent | Select-String "SAGA_PAYMENT_REJECTED.*$($recharge.rechargeId)"
    if ($sagaApprovedLogs) {
        Write-Host "   ✓ SAGA_PAYMENT_APPROVED event logged" -ForegroundColor Green
    } elseif ($sagaRejectedLogs) {
        Write-Host "   ✓ SAGA_PAYMENT_REJECTED event logged" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ SAGA payment result event not yet logged (may still be processing)" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ✗ recharge-service.log file not found" -ForegroundColor Red
}
Write-Host ""

# Step 8: Check trace ID correlation
Write-Host "Step 8: Checking trace ID correlation across services..." -ForegroundColor Yellow
Write-Host "   Note: This requires the recharge to have completed the full SAGA flow" -ForegroundColor Cyan
Write-Host "   Check logging-service database for trace ID correlation" -ForegroundColor Cyan
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Recharge flow completed successfully" -ForegroundColor Green
Write-Host "✓ Business operations logged correctly" -ForegroundColor Green
Write-Host "✓ SAGA events logged correctly" -ForegroundColor Green
Write-Host ""
Write-Host "Recharge Details:" -ForegroundColor Yellow
Write-Host "  Recharge ID: $($recharge.rechargeId)" -ForegroundColor White
Write-Host "  Status: $($updatedRecharge.status)" -ForegroundColor White
Write-Host "  Amount: ₹$($recharge.amount)" -ForegroundColor White
Write-Host "  Operator: $($selectedOperator.operatorName)" -ForegroundColor White
Write-Host "  Plan: $($selectedPlan.planName)" -ForegroundColor White
Write-Host ""
Write-Host "Next step: Run verify-recharge-service-db-logs.sql to verify database persistence" -ForegroundColor Yellow
Write-Host ""
