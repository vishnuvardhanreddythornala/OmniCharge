-- Recharge Service Database Logging Verification
-- This script verifies that recharge-service logs are properly persisted in the database

-- 1. Count total log entries for recharge-service
SELECT 
    'Total recharge-service log entries' AS metric,
    COUNT(*) AS count
FROM log_entries
WHERE service_name = 'recharge-service';

-- 2. Count log entries by event type
SELECT 
    'Log entries by event type' AS metric,
    event_type,
    COUNT(*) AS count
FROM log_entries
WHERE service_name = 'recharge-service'
GROUP BY event_type
ORDER BY count DESC;

-- 3. Count log entries by level
SELECT 
    'Log entries by level' AS metric,
    level,
    COUNT(*) AS count
FROM log_entries
WHERE service_name = 'recharge-service'
GROUP BY level
ORDER BY count DESC;

-- 4. Recent RECHARGE_INITIATED events (last 10)
SELECT 
    'Recent RECHARGE_INITIATED events' AS metric,
    timestamp,
    level,
    message,
    context_json
FROM log_entries
WHERE service_name = 'recharge-service'
  AND event_type = 'RECHARGE_INITIATED'
ORDER BY timestamp DESC
LIMIT 10;

-- 5. Recent RECHARGE_PROCESSING events (last 10)
SELECT 
    'Recent RECHARGE_PROCESSING events' AS metric,
    timestamp,
    level,
    message,
    context_json
FROM log_entries
WHERE service_name = 'recharge-service'
  AND event_type = 'RECHARGE_PROCESSING'
ORDER BY timestamp DESC
LIMIT 10;

-- 6. Recent SAGA_PAYMENT_APPROVED events (last 10)
SELECT 
    'Recent SAGA_PAYMENT_APPROVED events' AS metric,
    timestamp,
    level,
    message,
    context_json
FROM log_entries
WHERE service_name = 'recharge-service'
  AND event_type = 'SAGA_PAYMENT_APPROVED'
ORDER BY timestamp DESC
LIMIT 10;

-- 7. Recent SAGA_PAYMENT_REJECTED events (last 10)
SELECT 
    'Recent SAGA_PAYMENT_REJECTED events' AS metric,
    timestamp,
    level,
    message,
    context_json
FROM log_entries
WHERE service_name = 'recharge-service'
  AND event_type = 'SAGA_PAYMENT_REJECTED'
ORDER BY timestamp DESC
LIMIT 10;

-- 8. Recent SAGA_EVENT_PUBLISHED events (last 10)
SELECT 
    'Recent SAGA_EVENT_PUBLISHED events' AS metric,
    timestamp,
    level,
    message,
    context_json
FROM log_entries
WHERE service_name = 'recharge-service'
  AND event_type = 'SAGA_EVENT_PUBLISHED'
ORDER BY timestamp DESC
LIMIT 10;

-- 9. Recent RECHARGE_EXPIRED events (last 10)
SELECT 
    'Recent RECHARGE_EXPIRED events' AS metric,
    timestamp,
    level,
    message,
    context_json
FROM log_entries
WHERE service_name = 'recharge-service'
  AND event_type = 'RECHARGE_EXPIRED'
ORDER BY timestamp DESC
LIMIT 10;

-- 10. Verify context_json is populated for business operations
SELECT 
    'Business operations with context' AS metric,
    event_type,
    COUNT(*) AS count,
    COUNT(CASE WHEN context_json IS NOT NULL AND context_json != '{}' THEN 1 END) AS with_context,
    ROUND(COUNT(CASE WHEN context_json IS NOT NULL AND context_json != '{}' THEN 1 END) * 100.0 / COUNT(*), 2) AS context_percentage
FROM log_entries
WHERE service_name = 'recharge-service'
  AND event_type IN ('RECHARGE_INITIATED', 'RECHARGE_PROCESSING', 'SAGA_PAYMENT_APPROVED', 'SAGA_PAYMENT_REJECTED', 'SAGA_EVENT_PUBLISHED', 'RECHARGE_EXPIRED')
GROUP BY event_type;

-- 11. Check for trace ID correlation in SAGA flows (sample)
-- This query finds recharge IDs that appear in multiple event types (indicating SAGA flow)
SELECT 
    'SAGA flow trace correlation' AS metric,
    JSON_EXTRACT(context_json, '$.rechargeId') AS recharge_id,
    COUNT(DISTINCT event_type) AS event_types_count,
    GROUP_CONCAT(DISTINCT event_type ORDER BY event_type) AS event_types
FROM log_entries
WHERE service_name = 'recharge-service'
  AND event_type IN ('RECHARGE_INITIATED', 'RECHARGE_PROCESSING', 'SAGA_PAYMENT_APPROVED', 'SAGA_PAYMENT_REJECTED', 'SAGA_EVENT_PUBLISHED')
  AND JSON_EXTRACT(context_json, '$.rechargeId') IS NOT NULL
GROUP BY JSON_EXTRACT(context_json, '$.rechargeId')
HAVING COUNT(DISTINCT event_type) >= 3
ORDER BY timestamp DESC
LIMIT 10;

-- 12. Recent log entries with full details (last 20)
SELECT 
    'Recent log entries (full details)' AS metric,
    timestamp,
    level,
    event_type,
    message,
    context_json
FROM log_entries
WHERE service_name = 'recharge-service'
ORDER BY timestamp DESC
LIMIT 20;

-- 13. Error and warning logs (last 10)
SELECT 
    'Recent errors and warnings' AS metric,
    timestamp,
    level,
    event_type,
    message,
    context_json
FROM log_entries
WHERE service_name = 'recharge-service'
  AND level IN ('ERROR', 'WARN')
ORDER BY timestamp DESC
LIMIT 10;

-- 14. Hourly log volume for recharge-service (last 24 hours)
SELECT 
    'Hourly log volume (last 24 hours)' AS metric,
    DATE_FORMAT(timestamp, '%Y-%m-%d %H:00:00') AS hour,
    COUNT(*) AS log_count
FROM log_entries
WHERE service_name = 'recharge-service'
  AND timestamp >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY DATE_FORMAT(timestamp, '%Y-%m-%d %H:00:00')
ORDER BY hour DESC;

-- 15. Verification summary
SELECT 
    'Verification Summary' AS summary,
    COUNT(*) AS total_logs,
    COUNT(DISTINCT event_type) AS distinct_event_types,
    COUNT(DISTINCT level) AS distinct_levels,
    MIN(timestamp) AS earliest_log,
    MAX(timestamp) AS latest_log
FROM log_entries
WHERE service_name = 'recharge-service';
