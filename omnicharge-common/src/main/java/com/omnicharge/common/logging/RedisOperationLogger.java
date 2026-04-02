package com.omnicharge.common.logging;

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

    private final LogEventPublisher logEventPublisher;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    /**
     * Intercepts Redis get operations to log cache hits and misses.
     */
    @Around("execution(* org.springframework.data.redis.core.ValueOperations.get(..))")
    public Object logRedisGet(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String key = args.length > 0 ? String.valueOf(args[0]) : "unknown";
        
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
            status = "ERROR";
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
                errorMessage != null ? " | Error: " + errorMessage : ""
            );
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(status.equals("ERROR") ? "ERROR" : "DEBUG")
                    .eventType("REDIS")
                    .logger("RedisOperationLogger")
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
        String key = args.length > 0 ? String.valueOf(args[0]) : "unknown";
        
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMessage = null;
        
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            status = "FAILED";
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
                errorMessage != null ? " | Error: " + errorMessage : ""
            );
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(status.equals("FAILED") ? "ERROR" : "DEBUG")
                    .eventType("REDIS")
                    .logger("RedisOperationLogger")
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
        String key = args.length > 0 ? String.valueOf(args[0]) : "unknown";
        
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMessage = null;
        
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            status = "FAILED";
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
                errorMessage != null ? " | Error: " + errorMessage : ""
            );
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(status.equals("FAILED") ? "ERROR" : "DEBUG")
                    .eventType("REDIS")
                    .logger("RedisOperationLogger")
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .stackTrace(errorMessage)
                    .build();
            
            logEventPublisher.publish(logEvent);
        }
    }
}
