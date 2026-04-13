package com.omnicharge.logging.service;

import com.omnicharge.logging.common.logging.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class LogFileWriterServiceTest {

    @InjectMocks
    private LogFileWriterService logFileWriterService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(logFileWriterService, "logBaseDir", tempDir.toString());
    }

    @Test
    void writeToFile_WritesToServiceLogNotToAllServicesLogIfInfo() throws Exception {
        LogEvent event = LogEvent.builder()
                .serviceName("test-service")
                .level("INFO")
                .message("Normal start")
                .eventType("START")
                .timestamp(LocalDateTime.now())
                .build();

        logFileWriterService.writeToFile(event);

        Path serviceLog = tempDir.resolve("test-service").resolve("test-service.log");
        Path allLog = tempDir.resolve("all-services.log");

        assertTrue(Files.exists(serviceLog));
        assertTrue(Files.notExists(allLog) || Files.size(allLog) == 0);
    }

    @Test
    void writeToFile_WritesToBothIfError() throws Exception {
        LogEvent event = LogEvent.builder()
                .serviceName("test-service")
                .level("ERROR")
                .message("Failure")
                .eventType("CRASH")
                .timestamp(LocalDateTime.now())
                .build();

        logFileWriterService.writeToFile(event);

        Path serviceLog = tempDir.resolve("test-service").resolve("test-service.log");
        Path allLog = tempDir.resolve("all-services.log");

        assertTrue(Files.exists(serviceLog));
        assertTrue(Files.exists(allLog));
        
        String allLogContent = Files.readString(allLog);
        assertTrue(allLogContent.contains("ERROR"));
        assertTrue(allLogContent.contains("Failure"));
    }

    @Test
    void writeToFile_WritesToBothIfLifecycle() throws Exception {
        LogEvent event = LogEvent.builder()
                .serviceName("test-service")
                .level("INFO")
                .message("App started")
                .eventType("LIFECYCLE")
                .timestamp(LocalDateTime.now())
                .build();

        logFileWriterService.writeToFile(event);

        Path serviceLog = tempDir.resolve("test-service").resolve("test-service.log");
        Path allLog = tempDir.resolve("all-services.log");

        assertTrue(Files.exists(serviceLog));
        assertTrue(Files.exists(allLog));
    }
}
