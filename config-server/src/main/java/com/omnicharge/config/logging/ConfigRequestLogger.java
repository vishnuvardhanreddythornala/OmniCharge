package com.omnicharge.config.logging;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Logs configuration requests served by Config Server.
 * 
 * Production-grade logging for infrastructure service:
 * - Logs when services fetch configuration
 * - Includes client name and profile
 * - Minimal overhead, INFO level only
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigRequestLogger {

    private final LogEventPublisher logEventPublisher;

    /**
     * Logs configuration requests using Spring's request context.
     * Called via AOP or manual invocation from controllers.
     */
    public void logConfigRequest(String application, String profile, String label) {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            String clientIp = "unknown";
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                clientIp = request.getRemoteAddr();
            }
            
            String message = String.format(
                "[CONFIG-REQUEST] Application: %s | Profile: %s | Label: %s | ClientIP: %s",
                application, profile != null ? profile : "default", 
                label != null ? label : "master", clientIp
            );
            
            Map<String, Object> context = new HashMap<>();
            context.put("application", application);
            context.put("profile", profile != null ? profile : "default");
            context.put("label", label != null ? label : "master");
            context.put("clientIp", clientIp);
            
            LogEvent logEvent = LogEvent.builder()
                    .serviceName("config-server")
                    .level("INFO")
                    .eventType("CONFIG_REQUEST")
                    .logger(ConfigRequestLogger.class.getName())
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .context(context)
                    .build();
            
            logEventPublisher.publish(logEvent);
            log.info(message);
            
        } catch (Exception e) {
            // Don't let logging errors break config serving
            log.error("Error logging config request: {}", e.getMessage());
        }
    }
}
