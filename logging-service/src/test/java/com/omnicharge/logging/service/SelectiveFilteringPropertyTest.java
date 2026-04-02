package com.omnicharge.logging.service;

import com.omnicharge.common.logging.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for selective all-services.log filtering.
 * 
 * Validates Properties 11 and 12 from the design document:
 * - Property 11: Critical Event All-Services Routing
 * - Property 12: Non-Critical Event Filtering
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
@Tag("Feature: production-grade-centralized-logging, Property 11: Critical Event All-Services Routing")
@Tag("Feature: production-grade-centralized-logging, Property 12: Non-Critical Event Filtering")
class SelectiveFilteringPropertyTest {

    private LogFileWriterService logFileWriterService;
    private Path tempLogDir;
    
    private static final List<String> CRITICAL_LEVELS = List.of("ERROR", "WARN", "LIFECYCLE");
    private static final List<String> NON_CRITICAL_LEVELS = List.of("INFO", "DEBUG", "TRACE");
    private static final List<String> ALL_LEVELS = new ArrayList<>();
    
    static {
        ALL_LEVELS.addAll(CRITICAL_LEVELS);
        ALL_LEVELS.addAll(NON_CRITICAL_LEVELS);
    }

    @BeforeEach
    void setUp() throws IOException {
        logFileWriterService = new LogFileWriterService();
        tempLogDir = Files.createTempDirectory("test_logs_selective");
        ReflectionTestUtils.setField(logFileWriterService, "logBaseDir", tempLogDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(tempLogDir);
    }

    /**
     * Property 11: Critical Event All-Services Routing
     * 
     * For any log event with level ERROR, WARN, or event type LIFECYCLE,
     * the Logging_Service should write it to the all-services.log file
     * in addition to the per-service log.
     */
    @Test
    void property11_criticalEventsRoutedToAllServicesLog() throws IOException {
        // Run 100 iterations with randomized inputs
        Random random = new Random(42); // Fixed seed for reproducibility
        
        for (int iteration = 0; iteration < 100; iteration++) {
            // Generate random critical event
            String level = CRITICAL_LEVELS.get(random.nextInt(CRITICAL_LEVELS.size()));
            String serviceName = "service-" + random.nextInt(10);
            String message = "Critical event " + iteration;
            
            LogEvent event = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(level)
                    .logger("TestLogger")
                    .message(message)
                    .traceId("trace-" + iteration)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            logFileWriterService.writeToFile(event);
            
            // Verify event appears in all-services.log
            Path allServicesLog = tempLogDir.resolve("all-services.log");
            assertTrue(Files.exists(allServicesLog), 
                    "all-services.log should exist for critical event");
            
            String allServicesContent = Files.readString(allServicesLog);
            assertTrue(allServicesContent.contains(message),
                    String.format("Critical event (level=%s) should appear in all-services.log", level));
            
            // Verify event also appears in per-service log
            Path serviceLog = tempLogDir.resolve(serviceName).resolve(serviceName + ".log");
            assertTrue(Files.exists(serviceLog),
                    "Per-service log should exist");
            
            String serviceContent = Files.readString(serviceLog);
            assertTrue(serviceContent.contains(message),
                    "Event should appear in per-service log");
        }
    }

    /**
     * Property 11 (variant): LIFECYCLE eventType routing
     * 
     * For any log event with eventType=LIFECYCLE (regardless of level),
     * it should be written to all-services.log.
     */
    @Test
    void property11_lifecycleEventTypeRoutedToAllServicesLog() throws IOException {
        Random random = new Random(43);
        
        for (int iteration = 0; iteration < 50; iteration++) {
            // Generate random event with LIFECYCLE eventType
            String level = ALL_LEVELS.get(random.nextInt(ALL_LEVELS.size()));
            String serviceName = "service-" + random.nextInt(5);
            String message = "Lifecycle event " + iteration;
            
            LogEvent event = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(level)
                    .eventType("LIFECYCLE")
                    .logger("TestLogger")
                    .message(message)
                    .traceId("trace-" + iteration)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            logFileWriterService.writeToFile(event);
            
            // Verify event appears in all-services.log
            Path allServicesLog = tempLogDir.resolve("all-services.log");
            String allServicesContent = Files.readString(allServicesLog);
            assertTrue(allServicesContent.contains(message),
                    String.format("LIFECYCLE event (level=%s) should appear in all-services.log", level));
        }
    }

    /**
     * Property 12: Non-Critical Event Filtering
     * 
     * For any log event with level INFO, DEBUG, or TRACE (and not a lifecycle event),
     * the Logging_Service should NOT write it to the all-services.log file.
     */
    @Test
    void property12_nonCriticalEventsFilteredFromAllServicesLog() throws IOException {
        Random random = new Random(44);
        
        for (int iteration = 0; iteration < 100; iteration++) {
            // Generate random non-critical event
            String level = NON_CRITICAL_LEVELS.get(random.nextInt(NON_CRITICAL_LEVELS.size()));
            String serviceName = "service-" + random.nextInt(10);
            String message = "Non-critical event " + iteration;
            
            LogEvent event = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(level)
                    .logger("TestLogger")
                    .message(message)
                    .traceId("trace-" + iteration)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            logFileWriterService.writeToFile(event);
            
            // Verify event appears in per-service log
            Path serviceLog = tempLogDir.resolve(serviceName).resolve(serviceName + ".log");
            assertTrue(Files.exists(serviceLog),
                    "Per-service log should exist");
            
            String serviceContent = Files.readString(serviceLog);
            assertTrue(serviceContent.contains(message),
                    "Non-critical event should appear in per-service log");
            
            // Verify event does NOT appear in all-services.log (if it exists)
            Path allServicesLog = tempLogDir.resolve("all-services.log");
            if (Files.exists(allServicesLog)) {
                String allServicesContent = Files.readString(allServicesLog);
                assertFalse(allServicesContent.contains(message),
                        String.format("Non-critical event (level=%s) should NOT appear in all-services.log", level));
            }
        }
    }

    /**
     * Combined property test: Mixed critical and non-critical events
     * 
     * Validates that when processing a mix of critical and non-critical events,
     * the filtering works correctly for all events.
     */
    @Test
    void property11And12_mixedEventsFilteredCorrectly() throws IOException {
        Random random = new Random(45);
        List<LogEvent> allEvents = new ArrayList<>();
        
        // Generate 200 random events (mix of critical and non-critical)
        for (int i = 0; i < 200; i++) {
            String level = ALL_LEVELS.get(random.nextInt(ALL_LEVELS.size()));
            String serviceName = "service-" + random.nextInt(5);
            String message = "Event-" + i + "-" + level;
            
            LogEvent event = LogEvent.builder()
                    .serviceName(serviceName)
                    .level(level)
                    .logger("TestLogger")
                    .message(message)
                    .traceId("trace-" + i)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            allEvents.add(event);
            logFileWriterService.writeToFile(event);
        }
        
        // Read all-services.log
        Path allServicesLog = tempLogDir.resolve("all-services.log");
        assertTrue(Files.exists(allServicesLog), "all-services.log should exist");
        String allServicesContent = Files.readString(allServicesLog);
        
        // Verify critical events are present
        List<LogEvent> criticalEvents = allEvents.stream()
                .filter(e -> CRITICAL_LEVELS.contains(e.getLevel()))
                .collect(Collectors.toList());
        
        for (LogEvent criticalEvent : criticalEvents) {
            assertTrue(allServicesContent.contains(criticalEvent.getMessage()),
                    String.format("Critical event (level=%s) should be in all-services.log", 
                            criticalEvent.getLevel()));
        }
        
        // Verify non-critical events are absent
        List<LogEvent> nonCriticalEvents = allEvents.stream()
                .filter(e -> NON_CRITICAL_LEVELS.contains(e.getLevel()))
                .collect(Collectors.toList());
        
        for (LogEvent nonCriticalEvent : nonCriticalEvents) {
            assertFalse(allServicesContent.contains(nonCriticalEvent.getMessage()),
                    String.format("Non-critical event (level=%s) should NOT be in all-services.log", 
                            nonCriticalEvent.getLevel()));
        }
        
        // Verify all events are in their respective per-service logs
        Map<String, List<LogEvent>> eventsByService = allEvents.stream()
                .collect(Collectors.groupingBy(LogEvent::getServiceName));
        
        for (Map.Entry<String, List<LogEvent>> entry : eventsByService.entrySet()) {
            String serviceName = entry.getKey();
            List<LogEvent> serviceEvents = entry.getValue();
            
            Path serviceLog = tempLogDir.resolve(serviceName).resolve(serviceName + ".log");
            assertTrue(Files.exists(serviceLog), 
                    String.format("Per-service log for %s should exist", serviceName));
            
            String serviceContent = Files.readString(serviceLog);
            for (LogEvent event : serviceEvents) {
                assertTrue(serviceContent.contains(event.getMessage()),
                        String.format("Event should be in %s per-service log", serviceName));
            }
        }
    }

    /**
     * Edge case: Empty or null eventType with critical level
     */
    @Test
    void property11_criticalLevelWithNullEventType() throws IOException {
        LogEvent event = LogEvent.builder()
                .serviceName("test-service")
                .level("ERROR")
                .eventType(null)
                .logger("TestLogger")
                .message("Error with null eventType")
                .timestamp(LocalDateTime.now())
                .build();
        
        logFileWriterService.writeToFile(event);
        
        Path allServicesLog = tempLogDir.resolve("all-services.log");
        assertTrue(Files.exists(allServicesLog));
        String content = Files.readString(allServicesLog);
        assertTrue(content.contains("Error with null eventType"),
                "ERROR level event should be in all-services.log even with null eventType");
    }

    /**
     * Edge case: Non-critical level with LIFECYCLE eventType
     */
    @Test
    void property11_lifecycleEventTypeWithNonCriticalLevel() throws IOException {
        LogEvent event = LogEvent.builder()
                .serviceName("test-service")
                .level("INFO")
                .eventType("LIFECYCLE")
                .logger("TestLogger")
                .message("INFO level lifecycle event")
                .timestamp(LocalDateTime.now())
                .build();
        
        logFileWriterService.writeToFile(event);
        
        Path allServicesLog = tempLogDir.resolve("all-services.log");
        assertTrue(Files.exists(allServicesLog));
        String content = Files.readString(allServicesLog);
        assertTrue(content.contains("INFO level lifecycle event"),
                "LIFECYCLE eventType should route to all-services.log regardless of level");
    }
}
