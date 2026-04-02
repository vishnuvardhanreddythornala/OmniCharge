package com.omnicharge.logging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LoggingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scheduled service that replays buffered log events from fallback files.
 * 
 * When services cannot reach RabbitMQ, they write logs to fallback-buffer-*.log files.
 * This service periodically scans for these files, parses the JSON lines, republishes
 * them to RabbitMQ, and deletes successfully replayed files.
 * 
 * Requirements 12.2, 12.3: Fallback event replay and cleanup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FallbackReplayService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${logging.fallback.directory:logs}")
    private String fallbackDir;

    /**
     * Scans for fallback files every 60 seconds and replays them.
     * Runs only if RabbitMQ is available.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void replayFallbackLogs() {
        try {
            Path fallbackDirectory = Paths.get(fallbackDir);
            
            if (!Files.exists(fallbackDirectory)) {
                return; // No fallback directory yet
            }

            // Find all fallback-buffer-*.log files
            try (Stream<Path> files = Files.list(fallbackDirectory)) {
                files.filter(path -> path.getFileName().toString().startsWith("fallback-buffer-"))
                     .filter(path -> path.getFileName().toString().endsWith(".log"))
                     .forEach(this::replayFile);
            }
        } catch (IOException e) {
            log.error("Failed to scan fallback directory: {}", e.getMessage());
        }
    }

    /**
     * Replays a single fallback file by parsing JSON lines and republishing to RabbitMQ.
     */
    private void replayFile(Path fallbackFile) {
        log.info("Replaying fallback file: {}", fallbackFile.getFileName());
        
        int successCount = 0;
        int failureCount = 0;

        try {
            List<String> lines = Files.readAllLines(fallbackFile);
            
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    // Parse JSON line to LogEvent
                    LogEvent event = objectMapper.readValue(line, LogEvent.class);
                    
                    // Republish to RabbitMQ
                    rabbitTemplate.convertAndSend(
                            LoggingConstants.LOGGING_EXCHANGE,
                            "log." + event.getServiceName(),
                            event
                    );
                    
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to replay log line from {}: {}", 
                            fallbackFile.getFileName(), e.getMessage());
                    failureCount++;
                }
            }
            
            // If all events were successfully replayed, delete the fallback file
            if (failureCount == 0 && successCount > 0) {
                Files.delete(fallbackFile);
                log.info("Successfully replayed and deleted fallback file: {} ({} events)", 
                        fallbackFile.getFileName(), successCount);
            } else if (successCount > 0) {
                log.warn("Partially replayed fallback file: {} ({} success, {} failures). File retained for retry.",
                        fallbackFile.getFileName(), successCount, failureCount);
            }
            
        } catch (IOException e) {
            log.error("Failed to read fallback file {}: {}", 
                    fallbackFile.getFileName(), e.getMessage());
        }
    }
}
