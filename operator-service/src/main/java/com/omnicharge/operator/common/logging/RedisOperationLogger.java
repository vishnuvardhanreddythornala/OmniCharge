package com.omnicharge.operator.common.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AOP aspect that automatically logs all Redis cache operations.
 * Intercepts RedisTemplate operations to log:
 * - Cache hits and misses (get operations)
 * - Cache writes (set operations)
 * - Cache deletions
 * - Connection errors
 * 
 * This provides comprehensive Redis operation visibility without modifying business code.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOperationLogger {
    private static final String UNKNOWN = "unknown";

    private static final String LOGGER_NAME = "RedisOperationLogger";
    private static final String EVENT_TYPE_REDIS = "REDIS";
    private static final String LEVEL_DEBUG = "DEBUG";
    private static final String LEVEL_ERROR = "ERROR";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String ERROR_SEPARATOR = " | Error: ";

    private final LogEventPublisher logEventPublisher;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    /**
     * Intercepts Redis get operations to log cache hits and misses.
     */
    @Around("execution(* org.springframework.data.redis.core.ValueOperations.get(..))")
    public Object logRedisGet(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String key = args.length > 0 ? String.valueOf(args[0]) : UNKNOWN;
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        String status = "HIT";
        String errorMessage = null;
        
        try {
            result = joinPoint.proceed();
            if (result == null) {
                status = "MISS";
            }
            return result;
        } catch (Exception e) {
            status = LEVEL_ERROR;
            errorMessage = e.getMessage();
            log.error("Redis: Error getting key: {}", key, e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            String message = String.format(
                "[REDIS-GET] Key: %s | Status: %s | Duration: %dms%s",
                key,
                status,
                duration,
                errorMessage != null ? ERROR_SEPARATOR + errorMessage : ""
            );
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(status.equals(LEVEL_ERROR) ? LEVEL_ERROR : LEVEL_DEBUG)
                    .eventType(EVENT_TYPE_REDIS)
                    .logger(LOGGER_NAME)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .stackTrace(errorMessage)
                    .build();
            
            logEventPublisher.publish(logEvent);
        }
    }

    /**
     * Intercepts Redis set operations to log cache writes.
     */
    @Around("execution(* org.springframework.data.redis.core.ValueOperations.set(..))")
    public Object logRedisSet(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String key = args.length > 0 ? String.valueOf(args[0]) : UNKNOWN;
        
        long startTime = System.currentTimeMillis();
        String status = STATUS_SUCCESS;
        String errorMessage = null;
        
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            status = STATUS_FAILED;
            errorMessage = e.getMessage();
            log.error("Redis: Error setting key: {}", key, e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            String message = String.format(
                "[REDIS-SET] Key: %s | Status: %s | Duration: %dms%s",
                key,
                status,
                duration,
                errorMessage != null ? ERROR_SEPARATOR + errorMessage : ""
            );
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(status.equals(STATUS_FAILED) ? LEVEL_ERROR : LEVEL_DEBUG)
                    .eventType(EVENT_TYPE_REDIS)
                    .logger(LOGGER_NAME)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .stackTrace(errorMessage)
                    .build();
            
            logEventPublisher.publish(logEvent);
        }
    }

    /**
     * Intercepts Redis delete operations to log cache evictions.
     */
    @Around("execution(* org.springframework.data.redis.core.RedisTemplate.delete(..))")
    public Object logRedisDelete(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String key = args.length > 0 ? String.valueOf(args[0]) : UNKNOWN;
        
        long startTime = System.currentTimeMillis();
        String status = STATUS_SUCCESS;
        String errorMessage = null;
        
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            status = STATUS_FAILED;
            errorMessage = e.getMessage();
            log.error("Redis: Error deleting key: {}", key, e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            String message = String.format(
                "[REDIS-DELETE] Key: %s | Status: %s | Duration: %dms%s",
                key,
                status,
                duration,
                errorMessage != null ? ERROR_SEPARATOR + errorMessage : ""
            );
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(status.equals(STATUS_FAILED) ? LEVEL_ERROR : LEVEL_DEBUG)
                    .eventType(EVENT_TYPE_REDIS)
                    .logger(LOGGER_NAME)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .stackTrace(errorMessage)
                    .build();
            
            logEventPublisher.publish(logEvent);
        }
    }
}
