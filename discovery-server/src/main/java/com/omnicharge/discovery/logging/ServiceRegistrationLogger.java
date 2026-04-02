package com.omnicharge.discovery.logging;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Logs service registration and discovery events from Eureka Server.
 * 
 * Production-grade logging for infrastructure service:
 * - Logs when services register/deregister
 * - Logs heartbeat failures
 * - Minimal overhead, INFO level only
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceRegistrationLogger {

    private final LogEventPublisher logEventPublisher;

    /**
     * Logs service instance registration.
     */
    public void logServiceRegistration(String serviceName, String instanceId, String status) {
        try {
            String message = String.format(
                "[SERVICE-REGISTRATION] Service: %s | InstanceID: %s | Status: %s",
                serviceName, instanceId, status
            );
            
            Map<String, Object> context = new HashMap<>();
            context.put("serviceName", serviceName);
            context.put("instanceId", instanceId);
            context.put("status", status);
            context.put("eventType", "REGISTRATION");
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName("discovery-server")
                    .level("INFO")
                    .eventType("SERVICE_REGISTRATION")
                    .logger(ServiceRegistrationLogger.class.getName())
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .context(context)
                    .build();
            
            logEventPublisher.publish(logEvent);
            
        } catch (Exception e) {
            // Don't let logging errors break service discovery
            log.error("Error logging service registration: {}", e.getMessage());
        }
    }

    /**
     * Logs service instance deregistration/failure.
     */
    public void logServiceFailure(String serviceName, String instanceId, String reason) {
        try {
            String message = String.format(
                "[SERVICE-FAILURE] Service: %s | InstanceID: %s | Reason: %s",
                serviceName, instanceId, reason
            );
            
            Map<String, Object> context = new HashMap<>();
            context.put("serviceName", serviceName);
            context.put("instanceId", instanceId);
            context.put("reason", reason);
            context.put("eventType", "FAILURE");
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName("discovery-server")
                    .level("WARN")
                    .eventType("SERVICE_FAILURE")
                    .logger(ServiceRegistrationLogger.class.getName())
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .context(context)
                    .build();
            
            logEventPublisher.publish(logEvent);
            log.warn(message);
            
        } catch (Exception e) {
            // Don't let logging errors break service discovery
            log.error("Error logging service failure: {}", e.getMessage());
        }
    }

    /**
     * Logs heartbeat failures.
     */
    public void logHeartbeatFailure(String serviceName, String instanceId) {
        try {
            String message = String.format(
                "[HEARTBEAT-FAILURE] Service: %s | InstanceID: %s | Status: Heartbeat missed",
                serviceName, instanceId
            );
            
            Map<String, Object> context = new HashMap<>();
            context.put("serviceName", serviceName);
            context.put("instanceId", instanceId);
            context.put("eventType", "HEARTBEAT_FAILURE");
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName("discovery-server")
                    .level("WARN")
                    .eventType("HEARTBEAT_FAILURE")
                    .logger(ServiceRegistrationLogger.class.getName())
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .context(context)
                    .build();
            
            logEventPublisher.publish(logEvent);
            log.warn(message);
            
        } catch (Exception e) {
            // Don't let logging errors break service discovery
            log.error("Error logging heartbeat failure: {}", e.getMessage());
        }
    }
}
