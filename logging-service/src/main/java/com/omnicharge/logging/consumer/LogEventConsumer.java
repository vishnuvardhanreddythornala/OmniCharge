package com.omnicharge.logging.consumer;

import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LoggingConstants;
import com.omnicharge.logging.service.LogFileWriterService;
import com.omnicharge.logging.service.LogPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes log events from the logging queue and dispatches them
 * to both the file writer and the database persistence service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogEventConsumer {

    private final LogPersistenceService logPersistenceService;
    private final LogFileWriterService logFileWriterService;

    @RabbitListener(queues = LoggingConstants.LOGGING_QUEUE)
    public void consumeLogEvent(LogEvent event) {
        try {
            // Write to per-service log file + combined log
            logFileWriterService.writeToFile(event);

            // Persist to MySQL
            logPersistenceService.save(event);
        } catch (Exception e) {
            log.error("Failed to process log event from {}: {}",
                    event.getServiceName(), e.getMessage(), e);
        }
    }
}
