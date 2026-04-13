package com.omnicharge.recharge.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/*
 * Fallback writer that stores log events to a local file
 * when RabbitMQ is unavailable. This ensures zero log loss.
 * The fallback file is written in JSON-lines format (one JSON object per line)
 * so the recovery mechanism can parse and replay them later.
 */
@Component
@Slf4j
public class FallbackLogWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Value("${logging.fallback.directory:logs}")
    private String fallbackDir;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    /*
     * Writes a log event to a local fallback file.
     * Called when RabbitMQ is unavailable.
     */
    public void writeToFallbackFile(LogEvent event) {
        try {
            Path dir = Paths.get(fallbackDir);
            Files.createDirectories(dir);
            Path fallbackFile = dir.resolve("fallback-buffer-" + serviceName + ".log");

            String jsonLine = MAPPER.writeValueAsString(event) + System.lineSeparator();

            Files.writeString(fallbackFile, jsonLine,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Last resort — at least print to console (which the user wants preserved)
            log.error("CRITICAL: Cannot write to fallback log file. Log event lost: {}", event.getMessage(), e);
        }
    }
}
