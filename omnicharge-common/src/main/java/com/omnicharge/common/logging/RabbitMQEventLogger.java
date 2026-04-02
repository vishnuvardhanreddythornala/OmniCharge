package com.omnicharge.common.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * AOP aspect that automatically logs all RabbitMQ message publishing and consumption.
 * Intercepts:
 * - @RabbitListener annotated methods (message consumption)
 * - RabbitTemplate.convertAndSend calls (message publishing)
 * 
 * This provides comprehensive SAGA event logging without modifying business code.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQEventLogger {

    private final LogEventPublisher logEventPublisher;


    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    /**
     * Intercepts all @RabbitListener annotated methods to log message consumption.
     */
    @Around("@annotation(rabbitListener)")
    public Object logConsumption(ProceedingJoinPoint joinPoint, RabbitListener rabbitListener) throws Throwable {
        if ("logging-service".equalsIgnoreCase(serviceName)) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();
        
        // Extract queue names from annotation
        String[] queues = rabbitListener.queues();
        String queueInfo = queues.length > 0 ? String.join(", ", queues) : "unknown";
        
        if (queueInfo.contains(LoggingConstants.LOGGING_QUEUE)) {
            return joinPoint.proceed();
        }

        // Get event argument (usually first parameter)
        Object[] args = joinPoint.getArgs();
        String eventType = args.length > 0 ? args[0].getClass().getSimpleName() : "Unknown";
        
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMessage = null;
        
        try {
            log.debug("RabbitMQ: Consuming message from queue(s): {}", queueInfo);
            Object result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            log.error("RabbitMQ: Failed to consume message from queue(s): {}", queueInfo, e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            String message = String.format(
                "[RABBITMQ-CONSUME] Queue: %s | Consumer: %s.%s | EventType: %s | Status: %s | Duration: %dms%s",
                queueInfo,
                className,
                methodName,
                eventType,
                status,
                duration,
                errorMessage != null ? " | Error: " + errorMessage : ""
            );
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(status.equals("FAILED") ? "ERROR" : "INFO")
                    .eventType("RABBITMQ")
                    .logger(className)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .stackTrace(errorMessage)
                    .build();
            
            logEventPublisher.publish(logEvent);
        }
    }

    private static final ThreadLocal<Boolean> IS_LOGGING = ThreadLocal.withInitial(() -> false);

    /**
     * Intercepts RabbitTemplate.convertAndSend to log message publishing.
     * Excludes calls from LogEventPublisher to prevent infinite recursion.
     */
    @Around("""
    execution(* org.springframework.amqp.rabbit.core.RabbitTemplate.convertAndSend(..)) 
""")
    public Object logPublishing(ProceedingJoinPoint joinPoint) throws Throwable {
        if ("logging-service".equalsIgnoreCase(serviceName)) {
            return joinPoint.proceed();
        }
        
        if (IS_LOGGING.get()) {
            return joinPoint.proceed();
        }
        IS_LOGGING.set(true);
        try {
            Object[] args = joinPoint.getArgs();
            
            // Extract exchange, routing key, and message from arguments
            String exchange = args.length > 0 && args[0] instanceof String ? (String) args[0] : "default";
            String routingKey = args.length > 1 && args[1] instanceof String ? (String) args[1] : "unknown";
            Object message = args.length > 2 ? args[2] : (args.length > 1 ? args[1] : "unknown");
            
            if (LoggingConstants.LOGGING_EXCHANGE.equals(exchange)) {
                return joinPoint.proceed();
            }

            String messageType = message != null ? message.getClass().getSimpleName() : "null";
            
            long startTime = System.currentTimeMillis();
            String status = "SUCCESS";
            String errorMessage = null;
            
            try {
                log.debug("RabbitMQ: Publishing message to exchange: {}, routingKey: {}", exchange, routingKey);
                return joinPoint.proceed();
            } catch (Exception e) {
                status = "FAILED";
                errorMessage = e.getMessage();
                log.error("RabbitMQ: Failed to publish message to exchange: {}, routingKey: {}", exchange, routingKey, e);
                throw e;
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                
                String logMessage = String.format(
                    "[RABBITMQ-PUBLISH] Exchange: %s | RoutingKey: %s | MessageType: %s | Status: %s | Duration: %dms%s",
                    exchange,
                    routingKey,
                    messageType,
                    status,
                    duration,
                    errorMessage != null ? " | Error: " + errorMessage : ""
                );
                
                LogEvent logEvent = LogEvent.builder()
                        .serviceName(serviceName)
                        .level(status.equals("FAILED") ? "ERROR" : "INFO")
                        .eventType("RABBITMQ")
                        .logger("RabbitMQEventLogger")
                        .message(logMessage)
                        .timestamp(LocalDateTime.now())
                        .threadName(Thread.currentThread().getName())
                        .stackTrace(errorMessage)
                        .build();
                
                logEventPublisher.publish(logEvent);
            }
        } finally {
            IS_LOGGING.set(false);
        }
    }
}
