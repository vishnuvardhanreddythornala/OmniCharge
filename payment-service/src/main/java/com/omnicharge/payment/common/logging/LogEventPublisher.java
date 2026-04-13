package com.omnicharge.payment.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
 * Publishes structured LogEvent messages to the centralized logging exchange.
 * 
 * Strategy:
 *   PRIMARY  → RabbitMQ (async, non-blocking)
 *   FALLBACK → Local file via FallbackLogWriter (when RabbitMQ is unavailable)
 */
@Component
@Slf4j
public class LogEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final FallbackLogWriter fallbackLogWriter;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Autowired(required = false)
    public LogEventPublisher(RabbitTemplate rabbitTemplate, FallbackLogWriter fallbackLogWriter) {
        this.rabbitTemplate = rabbitTemplate;
        this.fallbackLogWriter = fallbackLogWriter;
    }

    /*
     * Publishes a log event to RabbitMQ. Falls back to local file on failure.
     */
    public void publish(LogEvent event) {
        if (event.getServiceName() == null) {
            event.setServiceName(serviceName);
        }

        try {
            rabbitTemplate.convertAndSend(
                    LoggingConstants.LOGGING_EXCHANGE,
                    "log." + event.getServiceName(),
                    event
            );
        } catch (Exception e) {
            // RabbitMQ is down — use fallback
            fallbackLogWriter.writeToFallbackFile(event);
        }
    }
}
