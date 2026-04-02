package com.omnicharge.config.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect to intercept Spring Cloud Config Server requests.
 * 
 * Intercepts calls to EnvironmentController to log configuration requests.
 * Production-grade: minimal overhead, non-blocking, fail-safe.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigRequestLoggingAspect {

    private final ConfigRequestLogger configRequestLogger;

    /**
     * Intercepts environment controller methods that serve configuration.
     * Logs the application name, profile, and label being requested.
     */
    @Around("execution(* org.springframework.cloud.config.server.environment.EnvironmentController..*(..))")
    public Object logConfigRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object[] args = joinPoint.getArgs();
            
            // Extract application, profile, label from method arguments
            String application = args.length > 0 ? String.valueOf(args[0]) : "unknown";
            String profile = args.length > 1 ? String.valueOf(args[1]) : null;
            String label = args.length > 2 ? String.valueOf(args[2]) : null;
            
            // Log the config request
            configRequestLogger.logConfigRequest(application, profile, label);
            
        } catch (Exception e) {
            // Don't let logging errors break config serving
            log.debug("Error in config request logging aspect: {}", e.getMessage());
        }
        
        // Proceed with the actual config serving
        return joinPoint.proceed();
    }
}
