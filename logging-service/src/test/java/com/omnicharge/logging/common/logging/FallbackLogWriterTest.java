package com.omnicharge.logging.common.logging;

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

    @InjectMocks private FallbackLogWriter fallbackLogWriter;
    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fallbackLogWriter, "fallbackDir", tempDir.toString());
        ReflectionTestUtils.setField(fallbackLogWriter, "serviceName", "logging-service");
    }

    @Test
    void writeToFallbackFile_Success() throws IOException {
        LogEvent event = new LogEvent();
        event.setServiceName("logging-service");
        event.setLevel("ERROR");
        event.setMessage("Queue failure test");
        event.setTimestamp(LocalDateTime.now());

        fallbackLogWriter.writeToFallbackFile(event);

        Path fallbackFile = tempDir.resolve("fallback-buffer-logging-service.log");
        assertTrue(Files.exists(fallbackFile));
        assertTrue(Files.readString(fallbackFile).contains("Queue failure test"));
    }

    @Test
    void writeToFallbackFile_MultipleAppends() throws IOException {
        LogEvent e1 = new LogEvent();
        e1.setServiceName("logging-service");
        e1.setMessage("Event 1");
        e1.setTimestamp(LocalDateTime.now());
        LogEvent e2 = new LogEvent();
        e2.setServiceName("logging-service");
        e2.setMessage("Event 2");
        e2.setTimestamp(LocalDateTime.now());

        fallbackLogWriter.writeToFallbackFile(e1);
        fallbackLogWriter.writeToFallbackFile(e2);

        String content = Files.readString(tempDir.resolve("fallback-buffer-logging-service.log"));
        assertTrue(content.contains("Event 1"));
        assertTrue(content.contains("Event 2"));
    }

    @Test
    void writeToFallbackFile_InvalidDir_HandlesGracefully() {
        ReflectionTestUtils.setField(fallbackLogWriter, "fallbackDir", "NUL\\\\invalid");
        LogEvent event = new LogEvent();
        event.setMessage("Lost");
        event.setTimestamp(LocalDateTime.now());

        assertDoesNotThrow(() -> fallbackLogWriter.writeToFallbackFile(event));
    }
}
