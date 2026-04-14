-- Verification SQL Queries for notification-service Centralized Logging Integration
-- Run these queries against the logging_db database to verify log persistence

-- Query 1: Count total logs from notification-service
SELECT COUNT(*) AS total_logs
FROM log_entries
WHERE service_name = 'notification-service';

-- Query 2: Count logs by level
SELECT level, COUNT(*) AS count
FROM log_entries
WHERE service_name = 'notification-service'
GROUP BY level
ORDER BY FIELD(level, 'ERROR', 'WARN', 'INFO', 'DEBUG'), count DESC;

-- Query 3: Count logs by event type
SELECT event_type, COUNT(*) AS count
FROM log_entries
WHERE service_name = 'notification-service'
GROUP BY event_type
ORDER BY count DESC;

-- Query 4: Verify NOTIFICATION_CREATED events exist
SELECT COUNT(*) AS notification_created_count
FROM log_entries
WHERE service_name = 'notification-service'
  AND event_type = 'NOTIFICATION_CREATED';

-- Query 5: Verify SMS_SENT events exist
SELECT COUNT(*) AS sms_sent_count
FROM log_entries
WHERE service_name = 'notification-service'
  AND event_type = 'SMS_SENT';

-- Query 6: Verify SMS_FAILED events exist
SELECT COUNT(*) AS sms_failed_count
FROM log_entries
WHERE service_name = 'notification-service'
  AND event_type = 'SMS_FAILED';

-- Query 7: Verify RABBITMQ_RECEIVE events exist (from event consumers)
SELECT COUNT(*) AS rabbitmq_receive_count
FROM log_entries
WHERE service_name = 'notification-service'
  AND event_type = 'RABBITMQ_RECEIVE';

-- Query 8: Verify LIFECYCLE events exist
SELECT COUNT(*) AS lifecycle_count
FROM log_entries
WHERE service_name = 'notification-service'
  AND event_type = 'LIFECYCLE';

-- Query 9: Check recent NOTIFICATION_CREATED logs with context
SELECT id, timestamp, message, context_json
FROM log_entries
WHERE service_name = 'notification-service'
  AND event_type = 'NOTIFICATION_CREATED'
ORDER BY timestamp DESC
LIMIT 10;

-- Query 10: Check recent SMS_SENT logs with context
SELECT id, timestamp, message, context_json
FROM log_entries
WHERE service_name = 'notification-service'
  AND event_type = 'SMS_SENT'
ORDER BY timestamp DESC
LIMIT 10;

-- Query 11: Check recent SMS_FAILED logs with context
SELECT id, timestamp, message, context_json
FROM log_entries
WHERE service_name = 'notification-service'
  AND event_type = 'SMS_FAILED'
ORDER BY timestamp DESC
LIMIT 10;

-- Query 12: Check recent RABBITMQ_RECEIVE logs (event consumers)
SELECT id, timestamp, message, context_json
FROM log_entries
WHERE service_name = 'notification-service'
  AND event_type = 'RABBITMQ_RECEIVE'
ORDER BY timestamp DESC
LIMIT 10;

-- Query 13: Verify context_json is populated for business operations
SELECT 
    event_type,
    COUNT(*) AS total_count,
    SUM(CASE WHEN context_json IS NOT NULL AND context_json != '{}' THEN 1 ELSE 0 END) AS with_context_count,
    ROUND(SUM(CASE WHEN context_json IS NOT NULL AND context_json != '{}' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS context_percentage
FROM log_entries
WHERE service_name = 'notification-service'
  AND event_type IN ('NOTIFICATION_CREATED', 'SMS_SENT', 'SMS_FAILED', 'RABBITMQ_RECEIVE')
GROUP BY event_type;

-- Query 14: Check logs from the last hour
SELECT event_type, level, COUNT(*) AS count
FROM log_entries
WHERE service_name = 'notification-service'
  AND timestamp >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY event_type, level
ORDER BY count DESC;

-- Query 15: Sample of all event types with their most recent log
SELECT 
    event_type,
    MAX(timestamp) AS latest_timestamp,
    COUNT(*) AS total_count
FROM log_entries
WHERE service_name = 'notification-service'
GROUP BY event_type
ORDER BY latest_timestamp DESC;

-- Expected Results:
-- - NOTIFICATION_CREATED events should exist with context (notificationId, userId, type, category, recipient, referenceId)
-- - SMS_SENT events should exist with context (userId, recipient, category, referenceId, deliveryStatus=SENT)
-- - SMS_FAILED events should exist with context (userId, recipient, category, referenceId, deliveryStatus=FAILED, errorMessage)
-- - RABBITMQ_RECEIVE events should exist (from PaymentEventConsumer and RechargeEventConsumer)
-- - LIFECYCLE events should exist (service startup/shutdown)
-- - context_json should be populated for all business operations (>95% coverage)
