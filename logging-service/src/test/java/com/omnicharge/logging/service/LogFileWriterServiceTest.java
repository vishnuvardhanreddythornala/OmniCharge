package com.omnicharge.logging.service;

import com.omnicharge.common.logging.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LogFileWriterServiceTest {

    private LogFileWriterService logFileWriterService;
    private Path tempLogDir;

    @BeforeEach
    void setUp() throws IOException {
        logFileWriterService = new LogFileWriterService();
        tempLogDir = Files.createTempDirectory("test_logs");
        ReflectionTestUtils.setField(logFileWriterService, "logBaseDir", tempLogDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(tempLogDir);
    }

    @Test
    void writeToFile_CreatesServiceLogFile() {
        LogEvent event = LogEvent.builder()
                .serviceName("test-service")
                .level("INFO")
                .logger("TestLogger")
                .message("Hello Logging")
                .traceId("tr-123")
                .timestamp(LocalDateTime.now())
                .build();

        logFileWriterService.writeToFile(event);

        Path serviceLog = tempLogDir.resolve("test-service").resolve("test-service.log");

        assertTrue(Files.exists(serviceLog), "Service log file should be created");
        
        try {
            String serviceLogContent = Files.readString(serviceLog);
            assertTrue(serviceLogContent.contains("Hello Logging"));
            assertTrue(serviceLogContent.contains("test-service"));
            assertTrue(serviceLogContent.contains("INFO"));
            assertTrue(serviceLogContent.contains("tr-123"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    void writeToFile_CriticalEventWrittenToAllServicesLog() {
        LogEvent errorEvent = LogEvent.builder()
                .serviceName("test-service")
                .level("ERROR")
                .logger("TestLogger")
                .message("Critical error occurred")
                .traceId("tr-456")
                .timestamp(LocalDateTime.now())
                .build();

        logFileWriterService.writeToFile(errorEvent);

        Path serviceLog = tempLogDir.resolve("test-service").resolve("test-service.log");
        Path combinedLog = tempLogDir.resolve("all-services.log");

        assertTrue(Files.exists(serviceLog), "Service log file should be created");
        assertTrue(Files.exists(combinedLog), "Combined log file should be created for ERROR level");
        
        try {
            String serviceLogContent = Files.readString(serviceLog);
            assertTrue(serviceLogContent.contains("Critical error occurred"));
            
            String combinedLogContent = Files.readString(combinedLog);
            assertTrue(combinedLogContent.contains("Critical error occurred"),
                    "ERROR level event should appear in all-services.log");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    void writeToFile_NonCriticalEventNotInAllServicesLog() {
        LogEvent infoEvent = LogEvent.builder()
                .serviceName("test-service")
                .level("INFO")
                .logger("TestLogger")
                .message("Non-critical info message")
                .traceId("tr-789")
                .timestamp(LocalDateTime.now())
                .build();

        logFileWriterService.writeToFile(infoEvent);

        Path serviceLog = tempLogDir.resolve("test-service").resolve("test-service.log");
        Path combinedLog = tempLogDir.resolve("all-services.log");

        assertTrue(Files.exists(serviceLog), "Service log file should be created");
        
        try {
            String serviceLogContent = Files.readString(serviceLog);
            assertTrue(serviceLogContent.contains("Non-critical info message"),
                    "INFO event should appear in per-service log");
            
            // all-services.log should not exist or should not contain the INFO message
            if (Files.exists(combinedLog)) {
                String combinedLogContent = Files.readString(combinedLog);
                assertTrue(!combinedLogContent.contains("Non-critical info message"),
                        "INFO level event should NOT appear in all-services.log");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
