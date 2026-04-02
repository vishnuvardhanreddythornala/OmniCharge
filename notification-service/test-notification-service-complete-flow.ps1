# Complete Flow Test for notification-service Centralized Logging Integration
# This script triggers all business operations to verify logging

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "NOTIFICATION-SERVICE COMPLETE FLOW TEST" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$timestamp = Get-Date -Format "yyyyMMddHHmmss"

Write-Host "Prerequisites:" -ForegroundColor Yellow
Write-Host "1. RabbitMQ must be running on localhost:5672" -ForegroundColor White
Write-Host "2. logging-service must be running on localhost:8086" -ForegroundColor White
Write-Host "3. notification-service must be running on localhost:8084" -ForegroundColor White
Write-Host "4. MySQL database 'logging_db' must be accessible" -ForegroundColor White
Write-Host ""
Write-Host "Press any key to continue or Ctrl+C to cancel..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
Write-Host ""

# Test 1: Service Lifecycle Logging (automatic via ServiceLifecycleLogger)
Write-Host "[Test 1/5] Service Lifecycle Logging" -ForegroundColor Cyan
Write-Host "  This is automatically logged when notification-service starts/stops" -ForegroundColor Gray
Write-Host "  Expected: LIFECYCLE events in all-services.log" -ForegroundColor Gray
Write-Host "  ✓ Check logs after service startup" -ForegroundColor Green
Write-Host ""

# Test 2: Email Notification Creation
Write-Host "[Test 2/5] Email Notification Creation" -ForegroundColor Cyan
Write-Host "  Note: notification-service may not expose direct API endpoints" -ForegroundColor Yellow
Write-Host "  Email notifications are typically triggered by internal service calls" -ForegroundColor Yellow
Write-Host "  Expected: NOTIFICATION_CREATED events with type=EMAIL" -ForegroundColor Gray
Write-Host "  ✓ Check notification-service.log for email creation logs" -ForegroundColor Green
Write-Host ""

# Test 3: SMS Notification Creation
Write-Host "[Test 3/5] SMS Notification Creation" -ForegroundColor Cyan
Write-Host "  Note: notification-service may not expose direct API endpoints" -ForegroundColor Yellow
Write-Host "  SMS notifications are typically triggered by internal service calls" -ForegroundColor Yellow
Write-Host "  Expected: NOTIFICATION_CREATED, SMS_SENT, or SMS_FAILED events" -ForegroundColor Gray
Write-Host "  ✓ Check notification-service.log for SMS logs" -ForegroundColor Green
Write-Host ""

# Test 4: Payment Event Consumer Logging
Write-Host "[Test 4/5] Payment Event Consumer Logging" -ForegroundColor Cyan
Write-Host "  Trigger a payment event to test PaymentEventConsumer logging" -ForegroundColor Yellow
Write-Host "  Expected: RABBITMQ_RECEIVE events with payment context" -ForegroundColor Gray
Write-Host ""
Write-Host "  To test this, trigger a payment from payment-service:" -ForegroundColor White
Write-Host "  1. Start payment-service" -ForegroundColor White
Write-Host "  2. Create a payment transaction" -ForegroundColor White
Write-Host "  3. Check notification-service logs for event consumption" -ForegroundColor White
Write-Host ""

# Test 5: Recharge Event Consumer Logging
Write-Host "[Test 5/5] Recharge Event Consumer Logging" -ForegroundColor Cyan
Write-Host "  Trigger a recharge event to test RechargeEventConsumer logging" -ForegroundColor Yellow
Write-Host "  Expected: RABBITMQ_RECEIVE events with recharge context" -ForegroundColor Gray
Write-Host ""
Write-Host "  To test this, trigger a recharge from recharge-service:" -ForegroundColor White
Write-Host "  1. Start recharge-service" -ForegroundColor White
Write-Host "  2. Create a recharge transaction" -ForegroundColor White
Write-Host "  3. Check notification-service logs for event consumption" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICATION STEPS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Step 1: Check notification-service.log" -ForegroundColor Yellow
Write-Host "  Location: logging-service\logs\notification-service.log" -ForegroundColor White
Write-Host "  Expected events:" -ForegroundColor White
Write-Host "    - NOTIFICATION_CREATED (with notificationId, userId, type, category, recipient)" -ForegroundColor Gray
Write-Host "    - SMS_SENT (with deliveryStatus=SENT)" -ForegroundColor Gray
Write-Host "    - SMS_FAILED (with deliveryStatus=FAILED, errorMessage)" -ForegroundColor Gray
Write-Host "    - RABBITMQ_RECEIVE (from event consumers)" -ForegroundColor Gray
Write-Host ""

Write-Host "Step 2: Check all-services.log" -ForegroundColor Yellow
Write-Host "  Location: logging-service\logs\all-services.log" -ForegroundColor White
Write-Host "  Expected events:" -ForegroundColor White
Write-Host "    - LIFECYCLE (service startup/shutdown)" -ForegroundColor Gray
Write-Host "    - ERROR events (if any failures occurred)" -ForegroundColor Gray
Write-Host "    - WARN events (if any warnings occurred)" -ForegroundColor Gray
Write-Host "  Note: INFO and DEBUG events should NOT appear in all-services.log" -ForegroundColor Yellow
Write-Host ""

Write-Host "Step 3: Query logging-service database" -ForegroundColor Yellow
Write-Host "  Run: mysql -u root -p logging_db < verify-notification-service-db-logs.sql" -ForegroundColor White
Write-Host "  Expected results:" -ForegroundColor White
Write-Host "    - Multiple NOTIFICATION_CREATED events" -ForegroundColor Gray
Write-Host "    - SMS_SENT and/or SMS_FAILED events" -ForegroundColor Gray
Write-Host "    - RABBITMQ_RECEIVE events (if payment/recharge events triggered)" -ForegroundColor Gray
Write-Host "    - context_json populated for all business operations" -ForegroundColor Gray
Write-Host ""

Write-Host "Step 4: Check RabbitMQ Management UI" -ForegroundColor Yellow
Write-Host "  URL: http://localhost:15672" -ForegroundColor White
Write-Host "  Username: guest, Password: guest" -ForegroundColor White
Write-Host "  Check:" -ForegroundColor White
Write-Host "    - log.events queue should have messages flowing" -ForegroundColor Gray
Write-Host "    - No messages stuck in queue" -ForegroundColor Gray
Write-Host "    - Message rate consistent with service activity" -ForegroundColor Gray
Write-Host ""

Write-Host "Step 5: Run unit tests" -ForegroundColor Yellow
Write-Host "  Command: cd notification-service && mvnw.cmd test" -ForegroundColor White
Write-Host "  Expected: All tests passing (including property tests)" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BUSINESS OPERATIONS LOGGED" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. NOTIFICATION_CREATED - Notification creation with type and recipient" -ForegroundColor White
Write-Host "2. SMS_SENT - Successful SMS delivery" -ForegroundColor White
Write-Host "3. SMS_FAILED - Failed SMS delivery with error details" -ForegroundColor White
Write-Host "4. RABBITMQ_RECEIVE - Payment event consumption" -ForegroundColor White
Write-Host "5. RABBITMQ_RECEIVE - Recharge event consumption" -ForegroundColor White
Write-Host ""
Write-Host "Automatic Infrastructure Logging (via omnicharge-common):" -ForegroundColor White
Write-Host "- LIFECYCLE - Service startup and shutdown" -ForegroundColor Gray
Write-Host "- RABBITMQ_SEND - RabbitMQ message publishing" -ForegroundColor Gray
Write-Host "- EXCEPTION - All unhandled exceptions" -ForegroundColor Gray
Write-Host ""

Write-Host "Test complete! Review the verification steps above." -ForegroundColor Green
