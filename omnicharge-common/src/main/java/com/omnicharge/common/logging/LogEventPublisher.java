package com.omnicharge.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * Publishes structured LogEvent messages asynchronously to the centralized logging exchange.
 * 
 * Strategy:
 *   PRIMARY  → In-Memory Queue (Zero latency impact to business thread)
 *   SENDER   → Background Daemon Thread sends memory queue to RabbitMQ
 *   FALLBACK → If RabbitMQ is down, Daemon thread writes to Local Disk Outbox
 */
@Component
@Slf4j
public class LogEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final FallbackLogWriter fallbackLogWriter;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    // Buffer up to 5000 logs in memory 
    private final BlockingQueue<LogEvent> logQueue = new ArrayBlockingQueue<>(5000);
    
    // Dedicated daemon thread for sending logs to RabbitMQ so it doesn't block APIs
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "log-publisher-thread");
        t.setDaemon(true);
        return t;
    });

    @Autowired(required = false)
    public LogEventPublisher(RabbitTemplate rabbitTemplate, FallbackLogWriter fallbackLogWriter) {
        this.rabbitTemplate = rabbitTemplate;
        this.fallbackLogWriter = fallbackLogWriter;
    }

    @PostConstruct
    public void startPublisher() {
        executorService.submit(this::processQueue);
    }

    @PreDestroy
    public void stopPublisher() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /*
     * Enqueues log event for async publishing (Instant return, zero latency)
     */
    public void publish(LogEvent event) {
        if (event.getServiceName() == null) {
            event.setServiceName(serviceName);
        }
        
        if (!logQueue.offer(event)) {
            // In the extreme edge case where memory queue is full, bypass and write to disk
            fallbackLogWriter.writeToFallbackFile(event);
        }
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Blocks until log is available
                LogEvent event = logQueue.take();
                
                try {
                    rabbitTemplate.convertAndSend(
                            LoggingConstants.LOGGING_EXCHANGE,
                            "log." + event.getServiceName(),
                            event
                    );
                } catch (Exception e) {
                    // RabbitMQ is down — safely park the log in the Local Disk Outbox
                    fallbackLogWriter.writeToFallbackFile(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
