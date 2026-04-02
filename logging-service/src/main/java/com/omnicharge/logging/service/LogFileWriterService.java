package com.omnicharge.logging.service;

import com.omnicharge.common.logging.LogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Writes log events to per-service log files within the centralized logs/ directory.
 * Each service gets its own subdirectory with rolling log files.
 * Also writes to a combined all-services.log for cross-service search.
 * 
 * SELECTIVE FILTERING (Requirements 6.1-6.5):
 * - Per-service logs: ALL events (DEBUG, INFO, WARN, ERROR, LIFECYCLE)
 * - all-services.log: ONLY critical events (ERROR, WARN, LIFECYCLE)
 * 
 * This filtering enables efficient cross-service monitoring while maintaining
 * complete per-service history for debugging.
 */
@Service
@Slf4j
public class LogFileWriterService {

    private static final DateTimeFormatter TIMESTAMP_FMT = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE_FMT = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 10MB limit per file before rolling
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    
    // Critical log levels that should appear in all-services.log
    private static final Set<String> CRITICAL_LEVELS = Set.of("ERROR", "WARN", "LIFECYCLE");

    @Value("${logging.service.log-directory:logs}")
    private String logBaseDir;

    /**
     * Appends a formatted log line to both the per-service file and the combined log.
     * Implements selective filtering for all-services.log (Requirements 6.1-6.5).
     */
    public void writeToFile(LogEvent event) {
        String formattedLine = formatLogLine(event);

        // 1. Write to per-service log file: logs/{serviceName}/{serviceName}.log
        // Per-service logs contain ALL events for complete debugging history
        writeWithRolling(Paths.get(logBaseDir, event.getServiceName()), event.getServiceName() + ".log", formattedLine);

        // 2. Write to combined log: logs/all-services.log
        // SELECTIVE FILTERING: Only ERROR, WARN, and LIFECYCLE events
        // This reduces noise and enables efficient cross-service monitoring
        if (shouldWriteToAllServicesLog(event)) {
            writeWithRolling(Paths.get(logBaseDir), "all-services.log", formattedLine);
        }
    }
    
    /**
     * Determines if an event should be written to all-services.log.
     * 
     * Requirements 6.1-6.5: Selective filtering based on level and eventType
     * - ERROR: Always included (critical failures)
     * - WARN: Always included (potential issues)
     * - LIFECYCLE: Always included (service startup/shutdown)
     * - INFO/DEBUG: Excluded (too verbose for cross-service view)
     */
    private boolean shouldWriteToAllServicesLog(LogEvent event) {
        // Check log level
        if (event.getLevel() != null && CRITICAL_LEVELS.contains(event.getLevel())) {
            return true;
        }
        
        // Check eventType for LIFECYCLE events (may have custom level)
        if ("LIFECYCLE".equals(event.getEventType())) {
            return true;
        }
        
        return false;
    }

    private void writeWithRolling(Path directory, String filename, String line) {
        try {
            Files.createDirectories(directory);
            Path logFile = directory.resolve(filename);

            // Check rolling condition
            if (Files.exists(logFile) && Files.size(logFile) >= MAX_FILE_SIZE_BYTES) {
                rollFile(directory, filename);
            }

            Files.writeString(logFile, line,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to write to log file {}: {}", filename, e.getMessage());
        }
    }

    private void rollFile(Path directory, String baseFilename) throws IOException {
        Path originalFile = directory.resolve(baseFilename);
        String dateStr = LocalDate.now().format(DATE_FMT);
        String nameWithoutExt = baseFilename.substring(0, baseFilename.lastIndexOf('.'));
        
        int counter = 0;
        Path rolledFile;
        do {
            String rolledFilename = nameWithoutExt + "-" + dateStr + "." + counter + ".log";
            rolledFile = directory.resolve(rolledFilename);
            counter++;
        } while (Files.exists(rolledFile));

        Files.move(originalFile, rolledFile);
        
        // Advanced functionality for 30 days / 500MB limits would go here.
        // For current implementation, basic file rolling is sufficient to satisfy 10MB chunking.
    }

    private String formatLogLine(LogEvent event) {
        String timestamp = event.getTimestamp() != null 
                ? event.getTimestamp().format(TIMESTAMP_FMT) 
                : "N/A";

        StringBuilder sb = new StringBuilder();
        sb.append(timestamp)
          .append(" ")
          .append(String.format("%5s", event.getLevel()))
          .append(" [").append(event.getServiceName())
          .append(",").append(event.getTraceId() != null ? event.getTraceId() : "-")
          .append(",").append(event.getSpanId() != null ? event.getSpanId() : "-")
          .append("] [").append(event.getThreadName() != null ? event.getThreadName() : "-")
          .append("] ").append(event.getLogger() != null ? event.getLogger() : "-")
          .append(" : ").append(event.getMessage());

        if (event.getStackTrace() != null && !event.getStackTrace().isEmpty()) {
            sb.append(System.lineSeparator()).append(event.getStackTrace());
        }

        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
