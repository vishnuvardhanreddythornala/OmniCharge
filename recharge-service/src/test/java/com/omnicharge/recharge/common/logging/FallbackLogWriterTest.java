package com.omnicharge.recharge.common.logging;

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
        ReflectionTestUtils.setField(fallbackLogWriter, "serviceName", "recharge-service");
    }

    @Test
    void writeToFallbackFile_Success() throws IOException {
        LogEvent event = new LogEvent();
        event.setServiceName("recharge-service");
        event.setLevel("ERROR");
        event.setMessage("Queue failure");
        event.setTimestamp(LocalDateTime.now());

        fallbackLogWriter.writeToFallbackFile(event);

        Path f = tempDir.resolve("fallback-buffer-recharge-service.log");
        assertTrue(Files.exists(f));
        assertTrue(Files.readString(f).contains("Queue failure"));
    }

    @Test
    void writeToFallbackFile_MultipleAppends() throws IOException {
        LogEvent e1 = new LogEvent(); e1.setMessage("L1"); e1.setTimestamp(LocalDateTime.now());
        LogEvent e2 = new LogEvent(); e2.setMessage("L2"); e2.setTimestamp(LocalDateTime.now());

        fallbackLogWriter.writeToFallbackFile(e1);
        fallbackLogWriter.writeToFallbackFile(e2);

        String c = Files.readString(tempDir.resolve("fallback-buffer-recharge-service.log"));
        assertTrue(c.contains("L1"));
        assertTrue(c.contains("L2"));
    }

    @Test
    void writeToFallbackFile_InvalidDir_HandlesGracefully() {
        ReflectionTestUtils.setField(fallbackLogWriter, "fallbackDir", "NUL\\\\invalid");
        LogEvent event = new LogEvent(); event.setMessage("Lost"); event.setTimestamp(LocalDateTime.now());
        assertDoesNotThrow(() -> fallbackLogWriter.writeToFallbackFile(event));
    }
}
