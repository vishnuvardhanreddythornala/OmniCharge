package com.omnicharge.common.logging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/*
 * Shared DTO for structured log events published from all services
 * to the centralized logging-service via RabbitMQ.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String serviceName;     // e.g. "recharge-service"
    private String level;           // "INFO", "WARN", "ERROR", "DEBUG", "LIFECYCLE"
    private String logger;          // e.g. "c.o.r.service.RechargeService"
    private String message;         // The log message
    private String traceId;         // Micrometer traceId (propagated across services)
    private String spanId;          // Micrometer spanId
    private String threadName;      // Thread that generated the log
    private String stackTrace;      // Only populated for ERROR level
    private LocalDateTime timestamp;
    
    // New fields for enhanced logging
    private String eventType;       // "LIFECYCLE", "RABBITMQ", "REDIS", "EXCEPTION", "BUSINESS", etc.
    private Map<String, Object> context; // Additional contextual data (e.g., queue name, cache key, etc.)
}
