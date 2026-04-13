package com.omnicharge.operator.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FallbackLogWriterTest {

    @InjectMocks
    private FallbackLogWriter fallbackLogWriter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fallbackLogWriter, "fallbackDir", tempDir.toString());
        ReflectionTestUtils.setField(fallbackLogWriter, "serviceName", "test-service");
    }

    @Test
    void writeToFallbackFile_Success() throws IOException {
        LogEvent event = new LogEvent();
        event.setServiceName("test-service");
        event.setLevel("ERROR");
        event.setMessage("Test error message");
        event.setTimestamp(LocalDateTime.now());

        fallbackLogWriter.writeToFallbackFile(event);

        Path fallbackFile = tempDir.resolve("fallback-buffer-test-service.log");
        assertTrue(Files.exists(fallbackFile));
        String content = Files.readString(fallbackFile);
        assertTrue(content.contains("Test error message"));
    }

    @Test
    void writeToFallbackFile_MultipleWrites() throws IOException {
        LogEvent event1 = new LogEvent();
        event1.setServiceName("test-service");
        event1.setMessage("Line 1");
        event1.setTimestamp(LocalDateTime.now());

        LogEvent event2 = new LogEvent();
        event2.setServiceName("test-service");
        event2.setMessage("Line 2");
        event2.setTimestamp(LocalDateTime.now());

        fallbackLogWriter.writeToFallbackFile(event1);
        fallbackLogWriter.writeToFallbackFile(event2);

        Path fallbackFile = tempDir.resolve("fallback-buffer-test-service.log");
        String content = Files.readString(fallbackFile);
        assertTrue(content.contains("Line 1"));
        assertTrue(content.contains("Line 2"));
    }

    @Test
    void writeToFallbackFile_InvalidDir_HandlesGracefully() {
        ReflectionTestUtils.setField(fallbackLogWriter, "fallbackDir", "NUL\\\\invalid");

        LogEvent event = new LogEvent();
        event.setServiceName("test-service");
        event.setMessage("Lost message");
        event.setTimestamp(LocalDateTime.now());

        // Should not throw - logs error internally
        assertDoesNotThrow(() -> fallbackLogWriter.writeToFallbackFile(event));
    }
}
