-- Operator Service Database Logging Verification Queries
-- Run these queries against the logging_db database to verify operator-service logs

-- 1. Count total log entries from operator-service
SELECT COUNT(*) as total_logs
FROM log_entries
WHERE service_name = 'operator-service';

-- 2. Count logs by level
SELECT level, COUNT(*) as count
FROM log_entries
WHERE service_name = 'operator-service'
GROUP BY level
ORDER BY count DESC;

-- 3. Count logs by event type
SELECT event_type, COUNT(*) as count
FROM log_entries
WHERE service_name = 'operator-service'
  AND event_type IS NOT NULL
GROUP BY event_type
ORDER BY count DESC;

-- 4. Recent operator-service logs (last 50)
SELECT id, timestamp, level, event_type, message
FROM log_entries
WHERE service_name = 'operator-service'
ORDER BY timestamp DESC
LIMIT 50;

-- 5. Business operation logs - Operator Management
SELECT id, timestamp, event_type, message, context_json
FROM log_entries
WHERE service_name = 'operator-service'
  AND event_type IN ('OPERATOR_CREATED', 'OPERATOR_UPDATED', 'OPERATOR_ACTIVATED', 'OPERATOR_DEACTIVATED')
ORDER BY timestamp DESC
LIMIT 20;

-- 6. Business operation logs - Operator Detection
SELECT id, timestamp, event_type, message, context_json
FROM log_entries
WHERE service_name = 'operator-service'
  AND event_type = 'OPERATOR_DETECTION'
ORDER BY timestamp DESC
LIMIT 20;

-- 7. Business operation logs - Plan Management
SELECT id, timestamp, event_type, message, context_json
FROM log_entries
WHERE service_name = 'operator-service'
  AND event_type IN ('PLAN_ACTIVATED', 'PLAN_DEACTIVATED')
ORDER BY timestamp DESC
LIMIT 20;

-- 8. Business operation logs - Numverify API Calls
SELECT id, timestamp, event_type, message, context_json
FROM log_entries
WHERE service_name = 'operator-service'
  AND event_type = 'NUMVERIFY_API_CALL'
ORDER BY timestamp DESC
LIMIT 20;

-- 9. Business operation logs - RabbitMQ Events
SELECT id, timestamp, event_type, message, context_json
FROM log_entries
WHERE service_name = 'operator-service'
  AND event_type = 'RABBITMQ_RECEIVE'
ORDER BY timestamp DESC
LIMIT 20;

-- 10. Error logs from operator-service
SELECT id, timestamp, level, event_type, message, context_json
FROM log_entries
WHERE service_name = 'operator-service'
  AND level = 'ERROR'
ORDER BY timestamp DESC
LIMIT 20;

-- 11. Logs with context data (business operations)
SELECT id, timestamp, event_type, message, context_json
FROM log_entries
WHERE service_name = 'operator-service'
  AND context_json IS NOT NULL
  AND context_json != '{}'
ORDER BY timestamp DESC
LIMIT 30;

-- 12. Operator detection success vs failure
SELECT 
    JSON_EXTRACT(context_json, '$.detectionResult') as result,
    COUNT(*) as count
FROM log_entries
WHERE service_name = 'operator-service'
  AND event_type = 'OPERATOR_DETECTION'
  AND context_json IS NOT NULL
GROUP BY result;

-- 13. Numverify API call success vs failure
SELECT 
    JSON_EXTRACT(context_json, '$.responseStatus') as status,
    COUNT(*) as count
FROM log_entries
WHERE service_name = 'operator-service'
  AND event_type = 'NUMVERIFY_API_CALL'
  AND context_json IS NOT NULL
GROUP BY status;

-- 14. RabbitMQ event processing status
SELECT 
    JSON_EXTRACT(context_json, '$.processingStatus') as status,
    COUNT(*) as count
FROM log_entries
WHERE service_name = 'operator-service'
  AND event_type = 'RABBITMQ_RECEIVE'
  AND context_json IS NOT NULL
GROUP BY status;

-- 15. Average response time for Numverify API calls
SELECT 
    AVG(CAST(JSON_EXTRACT(context_json, '$.responseTimeMs') AS UNSIGNED)) as avg_response_time_ms,
    MIN(CAST(JSON_EXTRACT(context_json, '$.responseTimeMs') AS UNSIGNED)) as min_response_time_ms,
    MAX(CAST(JSON_EXTRACT(context_json, '$.responseTimeMs') AS UNSIGNED)) as max_response_time_ms
FROM log_entries
WHERE service_name = 'operator-service'
  AND event_type = 'NUMVERIFY_API_CALL'
  AND context_json IS NOT NULL
  AND JSON_EXTRACT(context_json, '$.responseTimeMs') IS NOT NULL;
