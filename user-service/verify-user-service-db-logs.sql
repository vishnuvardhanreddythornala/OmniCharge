-- SQL Verification Script for user-service Centralized Logging
-- Run this script against the logging_db database

-- 1. Check if user-service logs exist
SELECT 
    'Total user-service logs' as metric,
    COUNT(*) as count
FROM log_entries
WHERE service_name = 'user-service';

-- 2. Check log distribution by level
SELECT 
    level,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM log_entries WHERE service_name = 'user-service'), 2) as percentage
FROM log_entries
WHERE service_name = 'user-service'
GROUP BY level
ORDER BY count DESC;

-- 3. Check log distribution by event type
SELECT 
    event_type,
    COUNT(*) as count
FROM log_entries
WHERE service_name = 'user-service'
AND event_type IS NOT NULL
GROUP BY event_type
ORDER BY count DESC;

-- 4. Check recent LIFECYCLE events
SELECT 
    timestamp,
    level,
    event_type,
    message,
    logger
FROM log_entries
WHERE service_name = 'user-service'
AND event_type = 'LIFECYCLE'
ORDER BY timestamp DESC
LIMIT 5;

-- 5. Check recent USER_REGISTRATION events
SELECT 
    timestamp,
    level,
    message,
    context_json
FROM log_entries
WHERE service_name = 'user-service'
AND event_type = 'USER_REGISTRATION'
ORDER BY timestamp DESC
LIMIT 5;

-- 6. Check recent LOGIN_ATTEMPT events
SELECT 
    timestamp,
    level,
    message,
    context_json
FROM log_entries
WHERE service_name = 'user-service'
AND event_type = 'LOGIN_ATTEMPT'
ORDER BY timestamp DESC
LIMIT 10;

-- 7. Check for failed login attempts
SELECT 
    timestamp,
    message,
    context_json
FROM log_entries
WHERE service_name = 'user-service'
AND event_type = 'LOGIN_ATTEMPT'
AND level = 'WARN'
ORDER BY timestamp DESC
LIMIT 5;

-- 8. Check PASSWORD_RESET events
SELECT 
    timestamp,
    event_type,
    message,
    context_json
FROM log_entries
WHERE service_name = 'user-service'
AND event_type IN ('PASSWORD_RESET_REQUEST', 'PASSWORD_RESET_COMPLETE')
ORDER BY timestamp DESC
LIMIT 5;

-- 9. Check PROFILE_UPDATE events
SELECT 
    timestamp,
    message,
    context_json
FROM log_entries
WHERE service_name = 'user-service'
AND event_type = 'PROFILE_UPDATE'
ORDER BY timestamp DESC
LIMIT 5;

-- 10. Check TOKEN_GENERATION events
SELECT 
    timestamp,
    message,
    context_json
FROM log_entries
WHERE service_name = 'user-service'
AND event_type = 'TOKEN_GENERATION'
ORDER BY timestamp DESC
LIMIT 5;

-- 11. Check for ERROR level logs
SELECT 
    timestamp,
    event_type,
    message,
    logger,
    context_json
FROM log_entries
WHERE service_name = 'user-service'
AND level = 'ERROR'
ORDER BY timestamp DESC
LIMIT 10;

-- 12. Verify context_json is populated
SELECT 
    'Logs with context_json' as metric,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM log_entries WHERE service_name = 'user-service'), 2) as percentage
FROM log_entries
WHERE service_name = 'user-service'
AND context_json IS NOT NULL
AND context_json != '{}';

-- 13. Check logs from the last hour
SELECT 
    event_type,
    level,
    COUNT(*) as count
FROM log_entries
WHERE service_name = 'user-service'
AND timestamp >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY event_type, level
ORDER BY count DESC;

-- 14. Check most recent 20 logs
SELECT 
    timestamp,
    level,
    event_type,
    LEFT(message, 100) as message_preview,
    logger
FROM log_entries
WHERE service_name = 'user-service'
ORDER BY timestamp DESC
LIMIT 20;

-- 15. Verify all expected event types exist
SELECT 
    CASE 
        WHEN EXISTS (SELECT 1 FROM log_entries WHERE service_name = 'user-service' AND event_type = 'LIFECYCLE') THEN '✓'
        ELSE '✗'
    END as LIFECYCLE,
    CASE 
        WHEN EXISTS (SELECT 1 FROM log_entries WHERE service_name = 'user-service' AND event_type = 'USER_REGISTRATION') THEN '✓'
        ELSE '✗'
    END as USER_REGISTRATION,
    CASE 
        WHEN EXISTS (SELECT 1 FROM log_entries WHERE service_name = 'user-service' AND event_type = 'LOGIN_ATTEMPT') THEN '✓'
        ELSE '✗'
    END as LOGIN_ATTEMPT,
    CASE 
        WHEN EXISTS (SELECT 1 FROM log_entries WHERE service_name = 'user-service' AND event_type = 'TOKEN_GENERATION') THEN '✓'
        ELSE '✗'
    END as TOKEN_GENERATION,
    CASE 
        WHEN EXISTS (SELECT 1 FROM log_entries WHERE service_name = 'user-service' AND event_type = 'PASSWORD_RESET_REQUEST') THEN '✓'
        ELSE '✗'
    END as PASSWORD_RESET_REQUEST,
    CASE 
        WHEN EXISTS (SELECT 1 FROM log_entries WHERE service_name = 'user-service' AND event_type = 'PROFILE_UPDATE') THEN '✓'
        ELSE '✗'
    END as PROFILE_UPDATE;
