package com.omnicharge.common.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Automatically logs service lifecycle events (startup and shutdown).
 * This component is auto-configured via Spring Boot AutoConfiguration.imports
 * and requires no manual wiring in services.
 * 
 * Lifecycle events are marked with level "LIFECYCLE" and eventType "LIFECYCLE"
 * to enable selective routing to all-services.log.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceLifecycleLogger {

    private final LogEventPublisher logEventPublisher;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Service {} is starting up", serviceName);
        
        LogEvent lifecycleEvent = LogEvent.builder()
                .serviceName(serviceName)
                .level("LIFECYCLE")
                .eventType("LIFECYCLE")
                .logger(ServiceLifecycleLogger.class.getName())
                .message(String.format("Service %s STARTED successfully", serviceName))
                .timestamp(LocalDateTime.now())
                .threadName(Thread.currentThread().getName())
                .build();
        
        logEventPublisher.publish(lifecycleEvent);
        
        log.info("Lifecycle STARTING event published for service: {}", serviceName);
    }

    @EventListener(ContextClosedEvent.class)
    public void onApplicationShutdown(ContextClosedEvent event) {
        log.info("Service {} is shutting down", serviceName);
        
        LogEvent lifecycleEvent = LogEvent.builder()
                .serviceName(serviceName)
                .level("LIFECYCLE")
                .eventType("LIFECYCLE")
                .logger(ServiceLifecycleLogger.class.getName())
                .message(String.format("Service %s STOPPING gracefully", serviceName))
                .timestamp(LocalDateTime.now())
                .threadName(Thread.currentThread().getName())
                .build();
        
        logEventPublisher.publish(lifecycleEvent);
        
        log.info("Lifecycle ENDING event published for service: {}", serviceName);
    }
}
