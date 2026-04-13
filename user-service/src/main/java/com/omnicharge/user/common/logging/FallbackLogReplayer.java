package com.omnicharge.user.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Self-healing background daemon that replays local disk logs to RabbitMQ.
 * Implements the Outbox pattern for zero-data-loss logging.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FallbackLogReplayer {

    private final RabbitTemplate rabbitTemplate;
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Value("${logging.fallback.directory:logs}")
    private String fallbackDir;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "log-replayer-thread");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void init() {
        // Runs cleanly every 30 seconds
        scheduler.scheduleWithFixedDelay(this::replayLogs, 30, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void replayLogs() {
        Path dir = Paths.get(fallbackDir);
        Path fallbackFile = dir.resolve("fallback-buffer-" + serviceName + ".log");
        Path processingFile = dir.resolve("processing-buffer-" + serviceName + ".log");

        try {
            // Priority to existing processing file from previous crashed run
            // Atomic rename prevents publisher thread from writing while we read
            if (!Files.exists(processingFile) && Files.exists(fallbackFile)) {
                Files.move(fallbackFile, processingFile, StandardCopyOption.ATOMIC_MOVE);
            }

            if (!Files.exists(processingFile)) {
                return; // Nothing to do
            }

            log.info("[OUTBOX RECOVERY] Processing failed logs from disk buffer...");

            List<String> remainingLines = new ArrayList<>();
            boolean brokerDown = false;

            try (BufferedReader reader = Files.newBufferedReader(processingFile)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (brokerDown) {
                        remainingLines.add(line);
                        continue;
                    }

                    if (!processLine(line)) { brokerDown = true; remainingLines.add(line); }
                }
            }

            if (brokerDown) {
                // Rewrite remaining un-processed lines back to processing buffer
                Files.write(processingFile, remainingLines, StandardOpenOption.TRUNCATE_EXISTING);
                log.warn("[OUTBOX FAILED] Broker still down. Remaining logs safely preserved on disk.");
            } else {
                // All success, delete the buffer file safely
                Files.delete(processingFile);
                log.info("[OUTBOX SUCCESS] All buffered logs successfully forwarded to RabbitMQ.");
            }

        } catch (Exception e) {
            log.error("[OUTBOX ERROR] Failed to process recovery logs: {}", e.getMessage());
        }
    }

    private boolean processLine(String line) {
        try {
            LogEvent event = MAPPER.readValue(line, LogEvent.class);
            rabbitTemplate.convertAndSend(
                    LoggingConstants.LOGGING_EXCHANGE,
                    "log." + event.getServiceName(),
                    event
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}